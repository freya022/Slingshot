package com.freya02.slingshot.auth;

import com.freya02.slingshot.Logger;
import com.freya02.ui.UILib;
import com.freya02.ui.window.CloseHandler;
import com.freya02.ui.window.LazyWindow;
import com.freya02.ui.window.WindowBuilder;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXPasswordField;
import com.jfoenix.controls.JFXSpinner;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.input.MouseEvent;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthController extends LazyWindow {
	private static final String INVALID_TEXT_ID = "invalidText";
	private static final Pattern usernamePattern = Pattern.compile("[\\w]{3,16}");
	private static final Pattern emailPattern = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])");
	private static final Matcher usernameMatcher = usernamePattern.matcher("");
	private static final Matcher emailMatcher = emailPattern.matcher("");

	@FXML private JFXTextField identifierField;
	@FXML private JFXPasswordField passwordField;
	@FXML private JFXSpinner spinner;
	@FXML private JFXButton logButton;

	private final SimpleBooleanProperty working = new SimpleBooleanProperty();

	@Override
	protected void onInitialized() {
		identifierField.disableProperty().bind(working);
		passwordField.disableProperty().bind(working);
		spinner.visibleProperty().bind(working);

		final SimpleBooleanProperty usernameIncorrectProperty = new SimpleBooleanProperty(true);
		final SimpleBooleanProperty isNotEmailProperty = new SimpleBooleanProperty(true);
		final SimpleBooleanProperty passwordIncorrectProperty = new SimpleBooleanProperty(false);
		logButton.disableProperty().bind(working
				.or(usernameIncorrectProperty)
				.or(passwordIncorrectProperty));
		passwordField.disableProperty().bind(isNotEmailProperty);

		identifierField.textProperty().addListener((x, y, newStr) -> {
			usernameMatcher.reset(newStr);

			final boolean emailMatch = emailMatcher.reset(newStr).matches();
			isNotEmailProperty.set(!emailMatch);

			final boolean isCorrect = usernameMatcher.matches() || emailMatch;
			usernameIncorrectProperty.set(!isCorrect);

			identifierField.setId(isCorrect ? null : INVALID_TEXT_ID);
		});

		passwordField.textProperty().addListener((x, y, newStr) -> {
			final boolean isCorrect = newStr.length() >= 6 || newStr.length() == 0;
			passwordIncorrectProperty.set(!isCorrect);

			passwordField.setId(isCorrect ? null : INVALID_TEXT_ID);
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

	public static native String getUuid0(String username);
	private static native String authenticate0(String username, String password, String clientToken) throws IOException;

	@FXML private void onLogClicked(MouseEvent event) {
		working.set(true);

		new Thread(() -> {
			try {
				final Credentials credentials = Credentials.getInstance();
				if (emailMatcher.reset(identifierField.getText()).matches()) {
					final String info = authenticate0(identifierField.getText(), passwordField.getText(), UUID.randomUUID().toString());
					final String[] split = info.split(";");

					credentials.setUsername(split[0]);
					credentials.setUuid(split[1]);
					credentials.setClientToken(split[2]);
					credentials.setAccessToken(split[3]);
				} else {
					credentials.setUsername(identifierField.getText());
					credentials.setUuid(getUuid0(identifierField.getText()));
					credentials.setClientToken("0");
					credentials.setAccessToken("0");
				}

				credentials.save();

				Platform.runLater(() -> {
					getWindow().close();
					Platform.exitNestedEventLoop(this, null);
				});
			} catch (IOException ignored) {

			} catch (Exception e) {
				Logger.handleError(e);
			} finally {
				working.set(false);
			}
		}).start();
	}
}
