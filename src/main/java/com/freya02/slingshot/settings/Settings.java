package com.freya02.slingshot.settings;

import com.sun.management.OperatingSystemMXBean;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.List;

import static com.freya02.slingshot.Main.SETTINGS_PATH;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/*
 * Max RAM = Physical RAM - 2
 * Recommended RAM = Math.min(Max RAM, 3/4 * Physical RAM)
 */
public class Settings {
	public static final int MAX_RAM = 12;

	private static Settings instance;
	private boolean NSFW = false;
	//Perhaps specific to Hotspot, might not work on OpenJ9 ?
	private String ram = String.valueOf(Math.min(MAX_RAM, getMaxRam() - getMaxRam() / 4)); //GB

	private Settings() throws IOException {
		if (Files.notExists(SETTINGS_PATH)) return;

		final List<String> strings = Files.readAllLines(SETTINGS_PATH);
		if (strings.size() == 2) {
			load(strings);
		} else {
			Files.deleteIfExists(SETTINGS_PATH);
			System.err.println("WARN : Invalid credentials file, deleting file");
		}
	}

	/**
	 * @return Physical RAM size, in GB
	 */
	static long getMaxRam() {
		final long maxRamBytes = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
		return Math.round(maxRamBytes / 1073741824.0);
	}

	public static Settings getInstance() {
		if (instance == null) {
			try {
				instance = new Settings();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return instance;
	}

	public boolean isNSFW() {
		return NSFW;
	}

	public void setNSFW(boolean NSFW) {
		this.NSFW = NSFW;
	}

	public String getRam() {
		return ram;
	}

	public void setRam(String ram) {
		this.ram = ram;
	}

	private void load(List<String> strings) {
		NSFW = Boolean.parseBoolean(strings.get(0));
		ram = strings.get(1);
	}

	public void save() throws IOException {
		Files.writeString(SETTINGS_PATH, String.join("\n", String.valueOf(NSFW), ram), CREATE, TRUNCATE_EXISTING);
	}
}
