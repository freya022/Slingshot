package com.freya02.slingshot;

import com.freya02.io.CheckResults;
import com.freya02.io.FileChecks;
import com.freya02.io.IOOperation;
import com.freya02.io.zip.Unzipper;
import com.freya02.misc.Benchmark;
import com.freya02.slingshot.auth.Credentials;
import com.freya02.slingshot.settings.Settings;
import javafx.application.Platform;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.freya02.slingshot.Main.ASSETS_PATH;
import static com.freya02.slingshot.Main.MINECRAFT_PATH;

public class SlingshotTask extends IOOperation {
	private final Path assetsFolder;
	private final Path jreFolder;
	private final Path gameFolderPath;
	private final Path jreCheckListPath;
	private final Path assetsCheckListPath;
	private final Path checkListPath;

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
		this.assetsCheckListPath = MINECRAFT_PATH.resolve("AssetsCheckList.fchecks");
		this.checkListPath = gameFolderPath.resolve("CheckList.fchecks");
		this.jreCheckListPath = MINECRAFT_PATH.resolve("JreCheckList.fchecks");

		this.javawPath = jreFolder.resolve("bin").resolve("javaw.exe");

		dropboxGameFolder = getDropboxGameFolder(targetVersion);

		AOT.init(); //Shouldn't have to do this but heh, it should be basically free and is a safety
	}

	@NotNull
	private String getDropboxGameFolder(String version) {
		return "/Versions/" + modpackName + '/' + version;
	}

	private ProgressFormatter progressFormatter = SlingshotTask::formatJreProgress;
	@SuppressWarnings("unused") //Used by JNI
	private void onWrite(int writtenBytes) {
		synchronized (mutex) {
			setWorkDone(getWorkDone() + writtenBytes);
			setState(progressFormatter.format(getWorkDone() / 1048576.0, getTotalWork() / 1048576.0));
		}
	}

	private static native void downloadFile0(String dropboxPath, String osPath, Object oThis);
	private static native long getDownloadSize0(String dropboxPath);

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
			} catch (IOException | InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
		}).start();
	}

	private void run() throws IOException, InterruptedException, ExecutionException {
		setWorkDone(1);

		Files.createDirectories(gameFolderPath);
		Files.createDirectories(jreFolder);

		checkVersionChanges();

		setState("Downloading check lists...");
		this.progressFormatter = (x, y) -> "Downloading check lists...";

		final Thread assetsCheckThread = new Thread(() -> downloadFile0("/AssetsCheckList.fchecks", assetsCheckListPath.toString(), this));
		final Thread gameCheckThread = new Thread(() -> downloadFile0(dropboxGameFolder + "/CheckList.fchecks", checkListPath.toString(), this));
		final Thread jreCheckThread = new Thread(() -> downloadFile0("/JreCheckList.fchecks", jreCheckListPath.toString(), this));

		assetsCheckThread.start();
		gameCheckThread.start();
		jreCheckThread.start();

		assetsCheckThread.join();
		gameCheckThread.join();
		jreCheckThread.join();

		final CheckResults assetsChecker = Benchmark.run("checkAssetsFiles", this::checkAssetsFiles).get();
		final CheckResults jreChecker = Benchmark.run("checkJreFiles", this::checkJreFiles).get();
		final CheckResults gameChecker = Benchmark.run("checkGameFiles", this::checkGameFiles).get();
		final boolean hasAssetsDamagedFiles = !assetsChecker.getDamagedFiles().isEmpty();
		final boolean hasJreDamagedFiles = !jreChecker.getDamagedFiles().isEmpty();
		final boolean hasGameDamagedFiles = !gameChecker.getDamagedFiles().isEmpty();

		if (hasGameDamagedFiles || hasJreDamagedFiles || hasAssetsDamagedFiles) setState("Gathering file download info");

		long sizeToBeDownloaded = 0;
		long jreSizeToDownload = 0;
		if (hasAssetsDamagedFiles) {
			sizeToBeDownloaded += assetsChecker.getDamagedSize();
		}

		if (hasJreDamagedFiles) {
			jreSizeToDownload = Math.min(jreChecker.getDamagedSize(), getJreSize());
			sizeToBeDownloaded += jreSizeToDownload;
		}

		if (hasGameDamagedFiles) {
			sizeToBeDownloaded += gameChecker.getDamagedSize();
		}

		setWorkDone(0);
		setTotalWork(sizeToBeDownloaded);

		if (hasAssetsDamagedFiles) {
			final List<Path> damagedFiles = assetsChecker.getDamagedFiles();
			System.out.println("Damaged assets files :");
			for (Path damagedFile : damagedFiles) {
				System.out.println(damagedFile);
			}

			setState("Downloading assets file...");
			downloadAssetsFiles(damagedFiles, assetsChecker);
		}

		if (hasJreDamagedFiles) {
			final List<Path> damagedFiles = jreChecker.getDamagedFiles();
			System.out.println("Damaged JRE files :");
			for (Path damagedFile : damagedFiles) {
				System.out.println(damagedFile);
			}

			setState("Downloading JRE file...");
			downloadJreFiles(damagedFiles, jreChecker, jreSizeToDownload);
		}

		if (hasGameDamagedFiles) {
			final List<Path> damagedFiles = gameChecker.getDamagedFiles();
			System.out.println("Damaged game files :");
			for (Path damagedFile : damagedFiles) {
				System.out.println(damagedFile);
			}

			setState("Downloading game file...");
			downloadGameFiles(gameChecker.getDamagedFiles(), gameChecker);
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

	private CheckResults checkAssetsFiles() throws IOException, ExecutionException {
		setState("Checking assets files...");

		CheckResults checkResults = Benchmark.run("checkResults", () -> FileChecks.create(assetsFolder, assetsCheckListPath, 8).check()).get();
		Files.deleteIfExists(assetsCheckListPath);

		return checkResults;
	}

	private CheckResults checkGameFiles() throws IOException, ExecutionException {
		setState("Checking game files...");

		CheckResults checkResults = Benchmark.run("checkResults", () -> FileChecks.create(gameFolderPath, checkListPath, 8).check()).get();
		Files.deleteIfExists(checkListPath);

		return checkResults;
	}

	private CheckResults checkJreFiles() throws IOException, ExecutionException {
		setState("Checking JRE files...");

		CheckResults checkResults = Benchmark.run("checkResults", () -> FileChecks.create(jreFolder, jreCheckListPath, 8).check()).get();
		Files.deleteIfExists(jreCheckListPath);

		return checkResults;
	}

	private void downloadJreFiles(List<Path> files, CheckResults infos, long jreSizeToDownload) throws InterruptedException, IOException {
		progressFormatter = SlingshotTask::formatJreProgress;

		//If files to download heavier than entire ZIP, download ZIP and unzip, else, download file by file
		if (jreSizeToDownload > getJreSize()) {
			final Path jreZip = Files.createTempFile("jre", null);
			downloadFile0("/jre.zip", jreZip.toString(), this);

			Unzipper.createUnzipper(jreZip, jreFolder).unzip();
			Files.deleteIfExists(jreZip);
		} else {
			final ExecutorService es = Executors.newFixedThreadPool(16);
			for (Path osPath : files) {
				final String relativeOsPath = jreFolder.relativize(osPath).toString();
				final String dropboxPath = "/jre/" + relativeOsPath.replace('\\', '/');

				es.submit(() -> {
					try {
						downloadFileFromDropbox(osPath, dropboxPath, infos.getDate(osPath.toString()));
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			}

			es.shutdown();
			es.awaitTermination(1, TimeUnit.DAYS);
		}
	}

	private void downloadFileFromDropbox(Path osPath, String dropboxPath, long fileTime) throws IOException {
		Files.createDirectories(osPath.getParent());
		downloadFile0(dropboxPath, osPath.toString(), this);
		Files.setLastModifiedTime(osPath, FileTime.fromMillis(fileTime));
	}

	private long getJreSize() {
		return getDownloadSize0("/jre.zip");
	}

	private void downloadAssetsFiles(List<Path> files, CheckResults infos) throws InterruptedException {
		progressFormatter = SlingshotTask::formatAssetsProgress;

		final ExecutorService es = Executors.newFixedThreadPool(16);
		for (Path osPath : files) {
			final String relativeOsPath = ASSETS_PATH.relativize(osPath).toString();
			final String dropboxPath = "/assets/" + relativeOsPath.replace('\\', '/');

			es.submit(() -> {
				try {
					downloadFileFromDropbox(osPath, dropboxPath, infos.getDate(osPath.toString()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.DAYS);
	}

	private void downloadGameFiles(List<Path> files, CheckResults infos) throws InterruptedException {
		progressFormatter = SlingshotTask::formatGameProgress;

		final ExecutorService es = Executors.newFixedThreadPool(16);
		for (Path osPath : files) {
			final String relativeOsPath = gameFolderPath.relativize(osPath).toString();
			final String dropboxPath = dropboxGameFolder + "/Files/" + relativeOsPath.replace('\\', '/');

			es.submit(() -> {
				try {
					downloadFileFromDropbox(osPath, dropboxPath, infos.getDate(osPath.toString()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.DAYS);
	}

	private static String formatDouble(double d) {
		int pow = 10;
		for (int i = 1; i < 2; i++)
			pow *= 10;
		double tmp = d * pow;
		return String.valueOf(( (double) ( (int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp) ) ) / pow);
	}

	private static String formatJreProgress(double workDone, double totalWork) {
		@SuppressWarnings("StringBufferReplaceableByString") final StringBuilder progressBuilder = new StringBuilder(39); //StringBuilder is still faster
		progressBuilder.append("Downloading JRE files... ").append(formatDouble(workDone)).append(" / ").append(formatDouble(totalWork)).append(" MB");
		return progressBuilder.toString();
	}

	private static String formatGameProgress(double workDone, double totalWork) {
		@SuppressWarnings("StringBufferReplaceableByString") final StringBuilder progressBuilder = new StringBuilder(40); //StringBuilder is still faster
		progressBuilder.append("Downloading game files... ").append(formatDouble(workDone)).append(" / ").append(formatDouble(totalWork)).append(" MB");
		return progressBuilder.toString();
	}

	private static String formatAssetsProgress(double workDone, double totalWork) {
		@SuppressWarnings("StringBufferReplaceableByString") final StringBuilder progressBuilder = new StringBuilder(40); //StringBuilder is still faster
		progressBuilder.append("Downloading assets files... ").append(formatDouble(workDone)).append(" / ").append(formatDouble(totalWork)).append(" MB");
		return progressBuilder.toString();
	}
}
