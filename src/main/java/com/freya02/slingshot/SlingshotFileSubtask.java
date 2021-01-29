package com.freya02.slingshot;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public abstract class SlingshotFileSubtask {
	protected static final int CORES = Runtime.getRuntime().availableProcessors() / 2;
	protected static final int THREADS = Runtime.getRuntime().availableProcessors();

	private final SlingshotTask task;

	SlingshotFileSubtask(SlingshotTask task) {
		this.task = task;
	}

	protected final void setState(String state) {
		task.setState(state);
	}

	protected final void incrementProgress() {
		task.incrementProgress();
	}

	protected final void setTotalWork(double totalWork) {
		task.setTotalWork(totalWork);
	}

	protected final void downloadFile0(String dropboxPath, String osPath) {
		SlingshotTask.downloadFile0(dropboxPath, osPath, task);
	}

	protected final long getDownloadSize0(String dropboxPath) {
		return SlingshotTask.getDownloadSize0(dropboxPath);
	}

	protected final void setWorkDone(double workDone) {
		task.setWorkDone(workDone);
	}

	protected final String formatDouble(double d) {
		final int pow = 100;
		final double tmp = d * pow;
		return String.valueOf(((double) ((int) ((tmp - (int) tmp) >= 0.5f ? tmp + 1 : tmp))) / pow);
	}

	protected final void downloadFileFromDropbox(Path osPath, String dropboxPath, long fileTime) throws IOException {
		Logger.info("Downloading " + dropboxPath + " to " + osPath);
		Files.createDirectories(osPath.getParent());
		downloadFile0(dropboxPath, osPath.toString());
		Files.setLastModifiedTime(osPath, FileTime.fromMillis(fileTime));
	}

	@NotNull
	abstract Thread startChecklistDownload();

	abstract void checkFiles() throws IOException;

	abstract boolean hasDamagedFiles();

	abstract long requiredDownloadSize();

	@NotNull
	abstract ProgressFormatter formatter();

	abstract void downloadFiles() throws InterruptedException, IOException;
}
