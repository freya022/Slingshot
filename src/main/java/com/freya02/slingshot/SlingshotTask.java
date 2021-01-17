package com.freya02.slingshot;

import com.freya02.io.IOOperation;
import com.freya02.io.IOUtils;
import com.freya02.slingshot.auth.Credentials;
import com.freya02.slingshot.settings.Settings;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.freya02.slingshot.Main.MINECRAFT_PATH;

public class SlingshotTask extends IOOperation {
	private final Path assetsFolder;
	private final Path jreFolder;
	private final Path gameFolderPath;

	private final Path javawPath;

	private final String modpackName, targetVersion;
	private final String dropboxGameFolder;

	private final Object mutex = new Object();

	SlingshotTask(String modpackName, String targetVersion) throws IOException {
		this.modpackName = modpackName;
		this.targetVersion = targetVersion;

		this.assetsFolder = MINECRAFT_PATH.resolve("assets");
		this.jreFolder = MINECRAFT_PATH.resolve("jre");
		this.gameFolderPath = Main.getGamePath(modpackName);

		this.javawPath = jreFolder.resolve("bin").resolve("javaw.exe");

		dropboxGameFolder = getDropboxGameFolder(targetVersion);

		AOT.init(); //Shouldn't have to do this but heh, it should be basically free and is a safety
	}

	@NotNull
	private String getDropboxGameFolder(String version) {
		return "/Versions/" + modpackName + '/' + version;
	}

	private ProgressFormatter progressFormatter = (x, y) -> "Checking version";
	@SuppressWarnings("unused") //Used by JNI
	private void onWrite(int writtenBytes) {
		synchronized (mutex) {
			setWorkDone(getWorkDone() + writtenBytes);
			setState(progressFormatter.format(getWorkDone() / 1048576.0, getTotalWork() / 1048576.0));
		}
	}

	static native void downloadFile0(String dropboxPath, String osPath, Object oThis);
	static native long getDownloadSize0(String dropboxPath);

	private static native void launchGame0(String javaw, String workingDir, String commandline);

	private String createCommandLine() throws IOException {
		final Credentials credentials = Credentials.getInstance();
		return Files.readString(gameFolderPath.resolve("CommandLine.txt"))
				.replace("[JAVAW_PATH]", javawPath.toString())
				.replace("[ASSETS_FOLDER]", assetsFolder.toString())
				.replace("[GAME_FOLDER]", gameFolderPath.toString())
				.replace("[USERNAME]", credentials.getUsername())
				.replace("[UUID]", credentials.getUuid())
				.replace("[ACCESS_TOKEN]", credentials.getAccessToken())
				.replace("[MEMORY]", Settings.getInstance().getRam() + "G");
	}

	public void start() {
		new Thread(() -> {
			try {
				run();
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
		}).start();
	}

	@Override
	public void setState(String state) {
		super.setState(state);
	}

	@Override
	public void incrementProgress() {
		super.incrementProgress();
	}

	@Override
	public void setTotalWork(double totalWork) {
		super.setTotalWork(totalWork);
	}

	@Override
	public void setWorkDone(double workDone) {
		super.setWorkDone(workDone);
	}

	private void run() throws IOException, InterruptedException {
		setWorkDone(1);

		Files.createDirectories(gameFolderPath);
		Files.createDirectories(jreFolder);

		checkVersionChanges();

		setState("Downloading check lists...");
		this.progressFormatter = (x, y) -> "Downloading check lists...";

		final SlingshotAssetsSubtask assetsSubtask = new SlingshotAssetsSubtask(this, assetsFolder);
		final SlingshotJreSubtask jreSubtask = new SlingshotJreSubtask(this, jreFolder);
		final SlingshotGameSubtask gameSubtask = new SlingshotGameSubtask(this, gameFolderPath, dropboxGameFolder);

		final Thread assetsCheckThread = assetsSubtask.startChecklistDownload();
		final Thread gameCheckThread = gameSubtask.startChecklistDownload();
		final Thread jreCheckThread = jreSubtask.startChecklistDownload();

		assetsCheckThread.join();
		gameCheckThread.join();
		jreCheckThread.join();

		assetsSubtask.checkFiles();
		jreSubtask.checkFiles();
		gameSubtask.checkFiles();

		final boolean hasAssetsDamagedFiles = assetsSubtask.hasDamagedFiles();
		final boolean hasJreDamagedFiles = jreSubtask.hasDamagedFiles();
		final boolean hasGameDamagedFiles = gameSubtask.hasDamagedFiles();

		if (hasGameDamagedFiles || hasJreDamagedFiles || hasAssetsDamagedFiles) setState("Gathering file download info");

		long sizeToBeDownloaded =  assetsSubtask.requiredDownloadSize() + jreSubtask.requiredDownloadSize() + gameSubtask.requiredDownloadSize();

		setWorkDone(0);
		setTotalWork(sizeToBeDownloaded);

		if (hasAssetsDamagedFiles) {
			progressFormatter = assetsSubtask.formatter();
			assetsSubtask.downloadFiles();
		}

		if (hasJreDamagedFiles) {
			progressFormatter = jreSubtask.formatter();
			jreSubtask.downloadFiles();
		}

		if (hasGameDamagedFiles) {
			progressFormatter = gameSubtask.formatter();
			gameSubtask.downloadFiles();
		}

		setState("Tweaking Rich Presence...");
		final Path modsPath = Main.getModsPath(modpackName);
		final Optional<Path> mod = Files.walk(modsPath, 1).filter(p -> p.toString().contains("slingshot-rich-presence")).findFirst();
		if (mod.isPresent()) {
			final Path srpPath = mod.get();
			if (Settings.getInstance().doesIntegrateDiscord()) {
				Files.move(srpPath, IOUtils.replaceExtension(srpPath, "jar"), StandardCopyOption.ATOMIC_MOVE);
			} else {
				Files.move(srpPath, IOUtils.replaceExtension(srpPath, "disabled"), StandardCopyOption.ATOMIC_MOVE);
			}
		}

		setState("Starting game...");

		final String commandLine = createCommandLine();
		launchGame0(javawPath.toString(), gameFolderPath.toString(), commandLine);

		Platform.exit();

		Files.writeString(Main.LAST_USE_PATH, modpackName + '\n' + targetVersion, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		Main.setGameVersion(modpackName, targetVersion);

		System.exit(0);
	}

	private List<String> getFileList(Path path) throws IOException {
		final byte[] s = Files.readAllBytes(path);
		final List<String> strings = new ArrayList<>(1400);

		int inc = s[16] == 0 ? 8 : 32;
		for (int i = 17; i < s.length; i += 9 + inc) {
			int length = 0;
			while (s[i] != '|') {
				length++;
				i++;
			}

			strings.add(new String(s, i - length, length));
		}

		return strings;
	}

	private void checkVersionChanges() throws IOException {
		setState("Getting current version...");
		final String currentGameVersion = Main.getGameVersion(modpackName, "");
		if (!currentGameVersion.isBlank() && !currentGameVersion.equals(targetVersion)) {
			final Path oldTempFile = Files.createTempFile("OldCheckList", ".fchecks");

			setState("Downloading old checklist...");
			this.progressFormatter = (x, y) -> "Downloading old checklist...";
			downloadFile0(getDropboxGameFolder(currentGameVersion) + "/CheckList.fchecks", oldTempFile.toString(), this);

			setState("Calculating changes...");
			final List<String> oldList = getFileList(oldTempFile);

			System.out.println(oldList.size() + " files to remove :");
			oldList.forEach(System.out::println);

			setState("Deleting old version...");
			setWorkDone(0); //Reset progress from download
			setTotalWork(oldList.size());
			for (String toDelete : oldList) {
				Files.deleteIfExists(gameFolderPath.resolve(toDelete));
				incrementProgress();
			}
		}
	}
}