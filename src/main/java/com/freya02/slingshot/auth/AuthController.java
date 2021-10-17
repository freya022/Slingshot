package com.freya02.slingshot.auth;

import com.freya02.slingshot.Logger;
import com.freya02.ui.UILib;
import com.freya02.ui.window.CloseHandler;
import com.freya02.ui.window.LazyWindow;
import com.freya02.ui.window.WindowBuilder;
import io.github.palexdev.materialfx.controls.MFXButton;
import io.github.palexdev.materialfx.controls.MFXProgressSpinner;
import io.github.palexdev.materialfx.controls.MFXTextField;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthController extends LazyWindow {
	private static final String INVALID_TEXT_ID = "invalidText";
	private static final Pattern usernamePattern = Pattern.compile("[\\w]{3,16}");
	private static final Matcher usernameMatcher = usernamePattern.matcher("");

	@FXML private MFXTextField identifierField;
	@FXML private MFXProgressSpinner spinner;
	@FXML private MFXButton logButton;

	private final SimpleBooleanProperty working = new SimpleBooleanProperty();

	@Override
	protected void onInitialized() {
		identifierField.disableProperty().bind(working);
		spinner.visibleProperty().bind(working);

		final SimpleBooleanProperty usernameIncorrectProperty = new SimpleBooleanProperty(true);
		logButton.disableProperty().bind(working.or(usernameIncorrectProperty));

		identifierField.textProperty().addListener((x, y, newStr) -> {
			usernameMatcher.reset(newStr);

			final boolean isCorrect = usernameMatcher.matches();
			usernameIncorrectProperty.set(!isCorrect);

			identifierField.setId(isCorrect ? null : INVALID_TEXT_ID);
		});
	}

	public static void createController() throws IOException {
		final AuthController controller = new AuthController();

		try {
			UILib.runAndWait(() -> {
				new WindowBuilder("Authentication.fxml", "Slingshot Authentication")
						.icon("../Icon.png")
						.onClose(CloseHandler.SYSTEM_EXIT)
						.create(controller);

				return Platform.enterNestedEventLoop(controller);
			});
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public static native String getUuid0(String username) throws IOException;
	private static native String[] authenticate0(String username, String password, String clientToken) throws IOException; //TODO cleanup

	@FXML private void onLoginWithMicrosoftAction(ActionEvent event) {
		try {
			final AuthenticationData authenticationData = MSAuthController.tryAuthenticate(); //modal

			if (authenticationData != null) {
				final Credentials credentials = Credentials.getInstance();

				credentials.setUsername(authenticationData.getUsername());
				credentials.setUuid(authenticationData.getUuid());
				credentials.setClientToken("42");
				credentials.setAccessToken(authenticationData.getAccessToken());

				credentials.save();

				getWindow().close();
				Platform.exitNestedEventLoop(this, null);
			}
		} catch (IOException e) {
			Logger.handleError(e);
		}
	}

	@FXML private void onLogClicked(MouseEvent event) {
		working.set(true);

		new Thread(() -> {
			try {
				final Credentials credentials = Credentials.getInstance();
				credentials.setUsername(identifierField.getText());
				credentials.setUuid(getUuid0(identifierField.getText()));
				credentials.setClientToken("0");
				credentials.setAccessToken("0");

				credentials.save();

				Platform.runLater(() -> {
					getWindow().close();
					Platform.exitNestedEventLoop(this, null);
				});
			} catch (Exception e) {
				Logger.handleError(e);
			} finally {
				working.set(false);
			}
		}).start();
	}
}
