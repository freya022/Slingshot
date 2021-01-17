package com.freya02.slingshot;

import com.freya02.io.CheckResults;
import com.freya02.io.FileChecks;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SlingshotGameSubtask extends SlingshotFileSubtask {
	private final Path gameFolder;
	private final String dropboxGameFolder;
	private final Path checkListPath;

	private final String checklistDropboxPath;

	private CheckResults checkResults;

	SlingshotGameSubtask(SlingshotTask task, Path gameFolder, String dropboxGameFolder) {
		super(task);

		this.gameFolder = gameFolder;
		this.dropboxGameFolder = dropboxGameFolder;
		this.checkListPath = gameFolder.resolve("CheckList.fchecks");
		this.checklistDropboxPath = dropboxGameFolder + "/CheckList.fchecks";
	}

	@Override
	@NotNull
	Thread startChecklistDownload() {
		final Thread assetsCheckThread = new Thread(() -> downloadFile0(checklistDropboxPath, checkListPath.toString()));
		assetsCheckThread.start();

		return assetsCheckThread;
	}

	@Override
	void checkFiles() throws IOException {
		setState("Checking assets files...");

		checkResults = FileChecks.create(gameFolder, checkListPath, CORES).check();
		Files.deleteIfExists(checkListPath);
	}

	@Override
	boolean hasDamagedFiles() {
		return checkResults.getDamagedSize() > 0;
	}

	@Override
	long requiredDownloadSize() {
		return checkResults.getDamagedSize();
	}

	@Override
	@NotNull
	ProgressFormatter formatter() {
		return (workDone, totalWork) -> "Downloading game files... " + formatDouble(workDone) + " / " + formatDouble(totalWork) + " MB";
	}

	@Override
	void downloadFiles() throws InterruptedException {
		final List<Path> damagedFiles = checkResults.getDamagedFiles();
		System.out.println("Damaged game files :");
		for (Path damagedFile : damagedFiles) {
			System.out.println(damagedFile);
		}

		setState("Downloading game file...");
		downloadGameFiles(checkResults.getDamagedFiles(), checkResults);
	}

	private void downloadGameFiles(List<Path> files, CheckResults infos) throws InterruptedException {
		final ExecutorService es = Executors.newFixedThreadPool(THREADS);
		for (Path osPath : files) {
			final String relativeOsPath = gameFolder.relativize(osPath).toString();
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
}
