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

import static com.freya02.slingshot.Main.ASSETS_PATH;
import static com.freya02.slingshot.Main.MINECRAFT_PATH;

public class SlingshotAssetsSubtask extends SlingshotFileSubtask {
	private final Path assetsFolder;
	private final Path assetsCheckListPath;

	private CheckResults checkResults;

	SlingshotAssetsSubtask(SlingshotTask task, Path assetsFolder) {
		super(task);

		this.assetsFolder = assetsFolder;
		this.assetsCheckListPath = MINECRAFT_PATH.resolve("AssetsCheckList.fchecks");
	}

	@Override
	@NotNull
	Thread startChecklistDownload() {
		final Thread assetsCheckThread = new Thread(() -> {
			try {
				downloadFile0("/AssetsCheckList.fchecks", assetsCheckListPath.toString());
			} catch (IOException e) {
				Logger.handleError(e);
			}
		});
		assetsCheckThread.start();

		return assetsCheckThread;
	}

	@Override
	void checkFiles() throws IOException {
		setState("Checking assets files...");

		checkResults = FileChecks.create(assetsFolder, assetsCheckListPath, CORES).check();
		Files.deleteIfExists(assetsCheckListPath);
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
		return (workDone, totalWork) -> "Downloading assets files... " + formatDouble(workDone) + " / " + formatDouble(totalWork) + " MB";
	}

	@Override
	void downloadFiles() throws InterruptedException {
		final List<Path> damagedFiles = checkResults.getDamagedFiles();
		Logger.info("Damaged assets files :");
		for (Path damagedFile : damagedFiles) {
			Logger.info(damagedFile);
		}

		setState("Downloading assets file...");
		downloadAssetsFiles();
	}

	private void downloadAssetsFiles() throws InterruptedException {
		final ExecutorService es = Executors.newFixedThreadPool(THREADS * 3);
		for (Path osPath : checkResults.getDamagedFiles()) {
			final String relativeOsPath = ASSETS_PATH.relativize(osPath).toString();
			final String dropboxPath = "/assets/" + relativeOsPath.replace('\\', '/');

			es.submit(() -> {
				try {
					downloadFileFromDropbox(osPath, dropboxPath, checkResults.getDate(osPath.toString()));
				} catch (IOException e) {
					Logger.handleError(e);
				}
			});
		}

		es.shutdown();
		es.awaitTermination(1, TimeUnit.DAYS);
	}
}
