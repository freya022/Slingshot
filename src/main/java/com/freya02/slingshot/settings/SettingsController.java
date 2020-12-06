package com.freya02.slingshot.settings;

import com.jfoenix.controls.JFXDialog;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXToggleButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.io.IOException;

public class SettingsController extends JFXDialog {
	@FXML private JFXToggleButton nsfwToggle;
	@FXML private JFXSlider ramSlider;

	@FXML private void initialize() {
		setOverlayClose(false);
		setBackground(new Background(new BackgroundFill(Color.rgb(25, 25, 25, 0.35), CornerRadii.EMPTY, Insets.EMPTY)));
		setTransitionType(DialogTransition.TOP);

		ramSlider.setMax(Settings.MAX_RAM);

		final Settings settings = Settings.getInstance();
		nsfwToggle.setSelected(settings.isNSFW());
		ramSlider.setValue(Double.parseDouble(settings.getRam()));
	}

	public static SettingsController createController() throws IOException {
		final SettingsController controller = new SettingsController();

		final FXMLLoader fxmlLoader = new FXMLLoader(SettingsController.class.getResource("Settings.fxml"));
		fxmlLoader.setController(controller);
		final Region root = fxmlLoader.load();

		controller.setContent(root);

		return controller;
	}

	@FXML private void onCancelClicked(MouseEvent event) {
		close();
	}

	@FXML private void onSaveClicked(MouseEvent event) {
		final Settings settings = Settings.getInstance();
		settings.setNSFW(nsfwToggle.isSelected());
		settings.setRam(Integer.toString((int) ramSlider.getValue()));

		try {
			settings.save();
		} catch (IOException e) {
			e.printStackTrace();
		}

		close();
	}
}
