package com.freya02.slingshot.auth;

import com.freya02.misc.Benchmark;
import com.freya02.slingshot.Logger;
import com.freya02.ui.window.CloseHandler;
import com.freya02.ui.window.LazyWindow;
import com.freya02.ui.window.Window;
import com.freya02.ui.window.WindowBuilder;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class MSAuthController extends LazyWindow {
	@FXML private Label label;

	private final MSAuth auth;

	public MSAuthController() throws IOException {
		this.auth = new MSAuth();
	}

	@Nullable
	public static AuthenticationData tryAuthenticate() {
		try {
			final MSAuthController controller = new MSAuthController();

			final Window window = new WindowBuilder("MSWait.fxml", "Waiting for MS Authentication")
					.initModality(Modality.APPLICATION_MODAL)
					.onClose(CloseHandler.SYSTEM_EXIT)
					.create(controller);

			new Thread(() -> {
				try {
					controller.auth.doFullLogin();
				} catch (Exception e) {
					Logger.handleError(e);

					Platform.exitNestedEventLoop(controller.auth, null);
				}
			}).start();

			Benchmark.start("Full login");
			final AuthenticationData data = (AuthenticationData) Platform.enterNestedEventLoop(controller.auth);
			Benchmark.stop();

			window.close();

			return data;
		} catch (IOException e) {
			throw new RuntimeException("Failed to authenticate", e);
		}
	}

	@Override
	protected void onInitialized() {
		label.textProperty().bind(auth.stateProperty());
	}
}
