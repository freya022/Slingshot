package com.freya02.slingshot;

import com.freya02.slingshot.auth.Credentials;
import com.freya02.slingshot.settings.SettingsController;
import com.freya02.ui.UILib;
import com.freya02.ui.window.LazyWindow;
import com.freya02.ui.window.WindowBuilder;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXProgressBar;
import com.jfoenix.controls.JFXSpinner;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.freya02.slingshot.Main.PROFILE_PICTURE_PATH;
import static com.freya02.slingshot.Main.PROFILE_USERNAME_PATH;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/*
 * Access token sharing :
 *
 * Getting the access token :
 *   -> Enter username
 *   -> Try to download details file from server
 *   -> Try to decrypt -> if first line (after decryption ofc) is the password hash then it is correct
 *   -> If login successful then
 *   -> -> decrypt 3rd line for access token (and then check for token, if invalid then activate password field)
 *   -> else
 *   -> -> disable access token, delete file
 *
 *
 * Enabling token sharing :
 *   -> Enter username
 *   -> Check UUID
 *   -> Ask for password
 *   -> Try to download details file from server
 *   -> Try to decrypt -> if first line (after decryption ofc) is the password hash then it is correct
 *   -> If login successful then
 *   -> -> Create shared_login_[USERNAME].txt and add the hash
 *   -> else
 *   -> -> Create a file with name being the username
 *   -> -> Add the password and access token : encrypt asap
 *   -> -> Upload file to server
 *
 *   -> Server file (contains access token) encrypted with AES
 *   -> Client file contains hashed pass
 *   -> Pass is key of server file
 *
 * try to do shared tokens ?
 * nah
 */
public class SlingshotController extends LazyWindow {
	@FXML private JFXButton playButton, modsButton, settingsButton;
	@FXML private VBox progressBox;
	@FXML private Label progressText, usernameLabel;
	@FXML private JFXProgressBar progressBar;
	@FXML private ChoiceBox<String> modpackChoiceBox, versionChoiceBox;
	@FXML private JFXSpinner spinner;
	@FXML private ImageView backgroundView, headView;
	@FXML private HBox profileBox;

	private final SimpleBooleanProperty showButtons = new SimpleBooleanProperty(true);

	private final Map<String, String[]> modpackToVersionMap = new HashMap<>();

	@Override
	protected void onInitialized() {
		new Thread(this::waitForDropbox).start();

		backgroundView.setImage(new Image(AOT.getRandomBackgroundUrl()));

		final SimpleBooleanProperty modsFolderNotExistsProperty = new SimpleBooleanProperty(true);

		playButton.disableProperty().bind(modpackChoiceBox.valueProperty().isNull().or(versionChoiceBox.valueProperty().isNull()));
		modsButton.disableProperty().bind(modpackChoiceBox.valueProperty().isNull().or(modsFolderNotExistsProperty));

		playButton.visibleProperty().bind(showButtons);
		modpackChoiceBox.visibleProperty().bind(showButtons);
		versionChoiceBox.visibleProperty().bind(showButtons);
		modsButton.visibleProperty().bind(showButtons);
		settingsButton.visibleProperty().bind(showButtons);
		profileBox.visibleProperty().bind(showButtons);

		progressBox.visibleProperty().bind(showButtons.not());

		modpackChoiceBox.valueProperty().addListener((x, y, newV) -> {
			if (newV != null) {
				final Path modsPath = Main.getModsPath(newV);

				modsFolderNotExistsProperty.set(!Files.exists(modsPath));
			}
		});

		updateDetails();
	}

	public static void createController() throws IOException {
		final SlingshotController controller = new SlingshotController();

		try {
			UILib.runAndWait(() -> {
				new WindowBuilder("Slingshot.fxml", "Slingshot")
						.icon("Icon.png")
						.onClose(stage -> {
							Platform.exit();
							System.exit(0);
						})
						.create(controller);

				return null;
			});
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@FXML private void onLogoutClicked(MouseEvent event) {
		getWindow().close();

		try {
			Credentials.getInstance().reset();

			Main.startLauncher();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML private void onOpenModsClicked(MouseEvent event) {
		final Path modsPath = Main.getModsPath(modpackChoiceBox.getValue());
		openFolder0(modsPath.toString());
	}

	@FXML private void onSettingsClicked(MouseEvent event) {
		try {
			final SettingsController controller = SettingsController.createController();
			controller.show((StackPane) this.getWindow().getRoot());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML private void onPlayClicked(MouseEvent event) {
		showButtons.set(false);

		try {
			final SlingshotTask slingshotTask = new SlingshotTask(modpackChoiceBox.getValue(), versionChoiceBox.getValue());

			progressText.textProperty().bind(slingshotTask.stateProperty());
			progressBar.progressProperty().bind(slingshotTask.progressProperty());

			slingshotTask.start();
		} catch (Exception e) {
			e.printStackTrace();
			showButtons.set(true);
		}
	}

	private static native byte[] getSkinImage0(String uuid);
	private static native void openFolder0(String path);

	/**
	 */
	private static native void saveImage0(String path, int[] pixels, int width, int height);

	private void updateDetails() {
		if (Files.exists(PROFILE_USERNAME_PATH) && Files.exists(PROFILE_PICTURE_PATH)) {
			try {
				final String username = Files.readString(PROFILE_USERNAME_PATH);

				usernameLabel.setText(username);
				headView.setImage(new Image(PROFILE_PICTURE_PATH.toUri().toURL().toString(), true));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		new Thread(this::refreshUser).start();
	}

	private void refreshUser() {
		final byte[] bytes = getSkinImage0(Credentials.getInstance().getUuid());
		int index = -1;
		for (int i = 0, bytesLength = bytes.length; i < bytesLength; i++) {
			byte b = bytes[i];

			if (b == ';') {
				index = i;
				break;
			}
		}

		final String username = new String(Arrays.copyOfRange(bytes, 0, index));
		final byte[] imgBytes = Arrays.copyOfRange(bytes, index + 1 /* skip ; */, bytes.length);

		try (ByteArrayInputStream byteStream = new ByteArrayInputStream(imgBytes)) {
			final Image image = new Image(byteStream);
			final WritableImage writableImage = new WritableImage(32, 32);

			final PixelReader reader = image.getPixelReader();
			final PixelWriter writer = writableImage.getPixelWriter();

			for (int y = 0; y < 8; y++) {
				for (int x = 0; x < 8; x++) {
					final Color color = reader.getColor(8 + x, 8 + y); //64 times
					final int baseX = x * 4;
					final int baseY = y * 4;
					for (int xOffset = 0; xOffset < 4; xOffset++) {
						for (int yOffset = 0; yOffset < 4; yOffset++) {
							writer.setColor(baseX + xOffset, baseY + yOffset, color); //1024 times
						}
					}
				}
			}

			Platform.runLater(() -> {
				usernameLabel.setText(username);
				headView.setImage(writableImage);
			});

			Files.writeString(PROFILE_USERNAME_PATH, username, CREATE, TRUNCATE_EXISTING);

			int[] pixels = new int[1024];
			for (int y = 0; y < 32; y++) {
				for (int x = 0; x < 32; x++) {
					pixels[x + y * 32] = writableImage.getPixelReader().getArgb(x, y);
				}
			}

			saveImage0(PROFILE_PICTURE_PATH.toString(), pixels, 32, 32);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static native String[] listFolder0(String dropboxPath);

	private void waitForDropbox() {
		try {
			AOT.init();

			final ExecutorService es = Executors.newCachedThreadPool();
			final String[] modpackNames = es.submit(() -> {
				final String[] modpacks = listFolder0("/Versions");

				for (String modpackName : modpacks) {
					es.submit(() -> {
						modpackToVersionMap.put(modpackName, listFolder0("/Versions/" + modpackName));
					});
				}

				es.shutdown();

				return modpacks;
			}).get();

			es.awaitTermination(1, TimeUnit.DAYS);

			String lastModpack = null, lastVersion = null;
			if (Files.exists(Main.LAST_USE_PATH)) {
				final List<String> strings = Files.readAllLines(Main.LAST_USE_PATH);
				if (strings.size() == 2 && !strings.get(0).isBlank() && !strings.get(1).isBlank()) {
					lastModpack = strings.get(0);
					lastVersion = strings.get(1);
				}
			}

			String finalLastModpack = lastModpack, finalLastVersion = lastVersion;
			Platform.runLater(() -> {
				((AnchorPane) spinner.getParent()).getChildren().remove(spinner);

				modpackChoiceBox.valueProperty().addListener((x, y, newV) -> {
					final String[] versions = modpackToVersionMap.get(newV);

					if (versions != null) {
						final ObservableList<String> items = versionChoiceBox.getItems();
						items.setAll(versions);

						try {
							final String versionName = Main.getGameVersion(newV, items.get(items.size() - 1));

							if (items.contains(versionName)) {
								versionChoiceBox.setValue(versionName);
							} else {
								versionChoiceBox.setValue(versions[versions.length - 1]);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						System.err.println("Modpack versions is null for " + newV);
					}
				});

				modpackChoiceBox.setDisable(false);
				versionChoiceBox.setDisable(false);

				modpackChoiceBox.getItems().addAll(modpackNames);

				if (modpackNames.length > 0) {
					modpackChoiceBox.setValue(modpackNames[0]);

					final String[] versions = modpackToVersionMap.get(modpackNames[0]);
					if (versions != null && versions.length > 0) {
						versionChoiceBox.setValue(versions[0]);
					}
				}

				if (finalLastModpack != null) {
					for (String modpackName : modpackNames) {
						if (modpackName.equals(finalLastModpack)) {
							modpackChoiceBox.setValue(modpackName);

							if (finalLastVersion != null) {
								for (String versionName : modpackToVersionMap.get(modpackName)) {
									if (versionName.equals(finalLastVersion)) {
										versionChoiceBox.setValue(versionName);
										break;
									}
								}
							}

							break;
						}
					}
				}
			});
		} catch (IOException | InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
}
