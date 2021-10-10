package com.freya02.slingshot.settings;

import com.freya02.slingshot.Logger;
import io.github.palexdev.materialfx.controls.MFXDialog;
import io.github.palexdev.materialfx.controls.MFXSlider;
import io.github.palexdev.materialfx.controls.MFXToggleButton;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.IOException;

public class SettingsController extends MFXDialog {
	@FXML private MFXToggleButton nsfwToggle, discordToggle, updateToggle;
	@FXML private MFXSlider ramSlider;

	@FXML private void initialize() {
		setVisible(false); //Visible by default as added in the stackpane directly
		setOverlayClose(false);
		setBackground(new Background(new BackgroundFill(Color.rgb(25, 25, 25, 0.35), CornerRadii.EMPTY, Insets.EMPTY)));

		//enable "animations"
		setAnimateIn(true);
		setAnimateOut(true);
		setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE); //Do not use bigger sizes

		setScrimOpacity(.5);

		setOnClosed(e -> ((Pane) getParent()).getChildren().remove(this)); //Remove ourselves after closing, could circumvent by using a singleton but eh

		ramSlider.setMax(Settings.MAX_RAM);

		final Settings settings = Settings.getInstance();
		nsfwToggle.setSelected(settings.isNSFW());
		ramSlider.setValue(Double.parseDouble(settings.getRam()));
		discordToggle.setSelected(settings.doesIntegrateDiscord());
		updateToggle.setSelected(settings.isUpdateEnabled());
	}

	public static SettingsController createController(StackPane stackPane) throws IOException {
		final SettingsController controller = new SettingsController();

		final FXMLLoader fxmlLoader = new FXMLLoader(SettingsController.class.getResource("Settings.fxml"));
		fxmlLoader.setController(controller);
		fxmlLoader.setRoot(controller);
		fxmlLoader.load();

		stackPane.getChildren().add(controller); //The dialog needs to be added to a **stack pane**

		return controller;
	}

	@FXML private void onCancelClicked(MouseEvent event) {
		close();
	}

	@FXML private void onSaveClicked(MouseEvent event) {
		final Settings settings = Settings.getInstance();
		settings.setNSFW(nsfwToggle.isSelected());
		settings.setRam(Integer.toString((int) ramSlider.getValue()));
		settings.setIntegrateDiscord(discordToggle.isSelected());
		settings.setUpdateEnabled(updateToggle.isSelected());

		try {
			settings.save();
		} catch (IOException e) {
			Logger.handleError(e);
		}

		close();
	}
}
