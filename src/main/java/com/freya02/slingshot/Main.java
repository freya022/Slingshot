package com.freya02.slingshot;

import com.freya02.slingshot.auth.AuthController;
import com.freya02.slingshot.auth.Credentials;
import com.freya02.ui.UILib;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class Main {
	public static final Path MINECRAFT_PATH = Path.of(System.getenv("appdata"), "Slingshot");
	public static final Path ASSETS_PATH = Path.of(System.getenv("appdata"), "Slingshot", "assets");
	public static final Path LAST_USE_PATH = MINECRAFT_PATH.resolve("lastuse.txt");
	public static final Path CREDENTIALS_PATH = MINECRAFT_PATH.resolve("creds.txt");
	public static final Path PROFILE_USERNAME_PATH = MINECRAFT_PATH.resolve("username.txt");
	public static final Path PROFILE_PICTURE_PATH = MINECRAFT_PATH.resolve("picture.png");
	public static final Path SETTINGS_PATH = MINECRAFT_PATH.resolve("settings.txt");

	static {
		try {
			Files.createDirectories(MINECRAFT_PATH);
			Logger.setupLog(MINECRAFT_PATH.resolve("logs.log"));
		} catch (IOException e) {
			UILib.displayError(e);
			System.exit(-9);
		}
	}

	public static Path getGamePath(String modpackName) {
		return MINECRAFT_PATH.resolve("Versions").resolve(modpackName);
	}

	public static Path getModsPath(String modpackName) {
		return Main.getGamePath(modpackName).resolve("mods");
	}

	public static String getGameVersion(String modpackName, String defaultValue) throws IOException {
		final Path path = getGamePath(modpackName).resolve("Version.txt");
		if (Files.exists(path)) {
			return Files.readString(path);
		} else {
			return defaultValue;
		}
	}

	public static void setGameVersion(String modpackName, String version) throws IOException {
		Files.writeString(getGamePath(modpackName).resolve("Version.txt"), version, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
	}

	public static void main(String[] args) {
		try {
			Thread.setDefaultUncaughtExceptionHandler((t, e) -> Logger.handleError(e));

			Platform.setImplicitExit(false);

			AOT.init();

			startLauncher();
		} catch (IOException e) {
			Logger.handleError(e);
			System.exit(-1);
		}
	}

	static void startLauncher() throws IOException {
		final Credentials credentials = Credentials.getInstance();
		if (!credentials.isValid()) {
			AuthController.createController();
		}

		SlingshotController.createController();
	}
}
