package com.freya02.slingshot;

import com.freya02.io.CheckResults;
import com.freya02.io.FileChecks;
import com.freya02.io.zip.Unzipper;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.freya02.slingshot.Main.MINECRAFT_PATH;

public class SlingshotJreSubtask extends SlingshotFileSubtask {
	private final Path jreFolder;
	private final Path jreCheckListPath;
	private long jreSize;

	private CheckResults checkResults;

	SlingshotJreSubtask(SlingshotTask task, Path jreFolder) {
		super(task);

		this.jreFolder = jreFolder;
		this.jreCheckListPath = MINECRAFT_PATH.resolve("JreCheckList.fchecks");
	}

	@Override
	@NotNull
	Thread startChecklistDownload() {
		final Thread gameCheckThread = new Thread(() -> downloadFile0("/JreCheckList.fchecks", jreCheckListPath.toString()));
		gameCheckThread.start();

		return gameCheckThread;
	}

	@Override
	void checkFiles() throws IOException {
		setState("Checking JRE files...");

		checkResults = FileChecks.create(jreFolder, jreCheckListPath, CORES).check();
		Files.deleteIfExists(jreCheckListPath);

		if (hasDamagedFiles()) {
			jreSize = getJreSize();
		}
	}

	@Override
	boolean hasDamagedFiles() {
		return checkResults.getDamagedSize() > 0;
	}

	@Override
	long requiredDownloadSize() {
		return Math.min(checkResults.getDamagedSize(), jreSize);
	}

	private long getJreSize() {
		return getDownloadSize0("/jre.zip");
	}

	@Override
	@NotNull
	ProgressFormatter formatter() {
		return (workDone, totalWork) -> "Downloading JRE files... " + formatDouble(workDone) + " / " + formatDouble(totalWork) + " MB";
	}

	@Override
	void downloadFiles() throws InterruptedException, IOException {
		final List<Path> damagedFiles = checkResults.getDamagedFiles();
		Logger.info("Damaged JRE files :");
		for (Path damagedFile : damagedFiles) {
			Logger.info(damagedFile);
		}

		setState("Downloading JRE file...");
		downloadJreFiles(damagedFiles, checkResults, checkResults.getDamagedSize());
	}

	private void downloadJreFiles(List<Path> files, CheckResults infos, long jreSizeToDownload) throws InterruptedException, IOException {
		//If files to download heavier than entire ZIP, download ZIP and unzip, else, download file by file
		if (jreSizeToDownload > jreSize) {
			final Path jreZip = Files.createTempFile("jre", null);
			downloadFile0("/jre.zip", jreZip.toString());

			Unzipper.createUnzipper(jreZip, jreFolder).unzip();
			Files.deleteIfExists(jreZip);
		} else {
			final ExecutorService es = Executors.newFixedThreadPool(THREADS);
			for (Path osPath : files) {
				final String relativeOsPath = jreFolder.relativize(osPath).toString();
				final String dropboxPath = "/jre/" + relativeOsPath.replace('\\', '/');

				es.submit(() -> {
					try {
						downloadFileFromDropbox(osPath, dropboxPath, infos.getDate(osPath.toString()));
					} catch (IOException e) {
						Logger.handleError(e);
					}
				});
			}

			es.shutdown();
			es.awaitTermination(1, TimeUnit.DAYS);
		}
	}
}
