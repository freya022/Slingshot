package com.freya02.slingshot;

import com.freya02.slingshot.auth.Credentials;
import com.freya02.slingshot.settings.SettingsController;
import com.freya02.ui.ImageUtil;
import com.freya02.ui.UILib;
import com.freya02.ui.window.LazyWindow;
import com.freya02.ui.window.WindowBuilder;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressBar;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
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
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

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
	@FXML private MFXButton playButton, modsButton, settingsButton;
	@FXML private VBox progressBox;
	@FXML private Label progressText, usernameLabel;
	@FXML private MFXProgressBar progressBar;
	@FXML private ChoiceBox<String> modpackChoiceBox, versionChoiceBox;
	@FXML private MFXProgressSpinner spinner;
	@FXML private ImageView backgroundView, headView;
	@FXML private HBox profileBox;

	private final SimpleBooleanProperty showButtons = new SimpleBooleanProperty(true);

	private final Map<String, List<String>> modpackToVersionMap = new TreeMap<>();

	@Override
	protected void onInitialized() {
		new Thread(this::waitForDropbox).start();

		backgroundView.setImage(new Image(AOT.getRandomBackgroundBytes()));

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
			Logger.handleError(e);
		}
	}

	@FXML private void onOpenModsClicked(MouseEvent event) {
		final Path modsPath = Main.getModsPath(modpackChoiceBox.getValue());
		openFolder0(modsPath.toString());
	}

	@FXML private void onSettingsClicked(MouseEvent event) {
		try {
			SettingsController.createController((StackPane) this.getWindow().getRoot()).show();
		} catch (IOException e) {
			Logger.handleError(e);
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
			Logger.handleError(e);
			showButtons.set(true);
		}
	}

	/** Returns mojang username and skin of an <b>existing</b> player
	 *
	 * @param uuid UUID of the player
	 * @return [0]: String, [1]: byte[]
	 */
	private static native Object[] getSkinImage0(String uuid) throws IOException;
	private static native void openFolder0(String path);

	private void updateDetails() {
		if (Files.exists(PROFILE_USERNAME_PATH) && Files.exists(PROFILE_PICTURE_PATH)) {
			try {
				final String username = Files.readString(PROFILE_USERNAME_PATH);

				usernameLabel.setText(username);
				headView.setImage(new Image(PROFILE_PICTURE_PATH.toUri().toURL().toString(), true));
			} catch (IOException e) {
				Logger.handleError(e);
			}
		}

		new Thread(() -> {
			try {
				refreshUser();
			} catch (IOException e) {
				Logger.handleError(e);
			}
		}).start();
	}

	private void refreshUser() throws IOException {
		final Credentials credentials = Credentials.getInstance();
		final String username;
		final byte[] imgBytes;
		if (credentials.getUuid().equals("0")) {
			username = credentials.getUsername();
			try (InputStream stream = new URL("http://assets.mojang.com/SkinTemplates/steve.png").openStream()) {
				imgBytes = stream.readAllBytes();
			}
		} else {
			final Object[] bytes = getSkinImage0(credentials.getUuid());

			username = (String) bytes[0];
			imgBytes = (byte[]) bytes[1];
		}

		try (ByteArrayInputStream byteStream = new ByteArrayInputStream(imgBytes)) {
			final Image image = new Image(byteStream);
			final WritableImage writableImage = new WritableImage(32, 32);

			final PixelReader reader = image.getPixelReader();
			final PixelWriter writer = writableImage.getPixelWriter();

			//No filtering x4 scaling
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

			ImageUtil.saveImage(writableImage, PROFILE_PICTURE_PATH);
		}
	}

	private static native String[] searchModpacks0() throws IOException;

	private void waitForDropbox() {
		try {
			AOT.init();

			final String[] searchResults = searchModpacks0();
			for (String s : searchResults) {
				if (!s.startsWith("/Versions/")) {
					continue;
				}

				//10 is the length of "/Versions/"
				final int index = s.indexOf('/', 10);
				final String modpackName = s.substring(10, index);
				final String version = s.substring(index + 1, s.indexOf('/', index + 1));

				modpackToVersionMap.computeIfAbsent(modpackName, x -> new ArrayList<>()).add(version);
			}

			String[] modpackNames = modpackToVersionMap.keySet().toArray(new String[0]);
			modpackToVersionMap.values().forEach(Collections::sort);

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
					final List<String> versions = modpackToVersionMap.get(newV);

					if (versions != null) {
						final ObservableList<String> items = versionChoiceBox.getItems();
						items.setAll(versions);

						try {
							final String versionName = Main.getGameVersion(newV, items.get(items.size() - 1));

							if (items.contains(versionName)) {
								versionChoiceBox.setValue(versionName);
							} else {
								versionChoiceBox.setValue(versions.get(versions.size() - 1));
							}
						} catch (IOException e) {
							Logger.handleError(e);
						}
					} else {
						Logger.error("Modpack versions is null for " + newV);
					}
				});

				modpackChoiceBox.setDisable(false);
				versionChoiceBox.setDisable(false);

				modpackChoiceBox.getItems().addAll(modpackNames);

				if (modpackNames.length > 0) {
					modpackChoiceBox.setValue(modpackNames[0]);

					final List<String> versions = modpackToVersionMap.get(modpackNames[0]);
					if (versions != null && versions.size() > 0) {
						versionChoiceBox.setValue(versions.get(0));
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
		} catch (IOException e) {
			Logger.handleError(e);
		}
	}
}
