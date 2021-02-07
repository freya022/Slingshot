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
 * Max RAM = 8 GB (for minecraft itself, java compiler and profiler is not counted in, probably not native MC mem too)
 * Recommended RAM = Math.min(Max RAM, 3/4 * Physical RAM)
 */
public class Settings {
	public static final int MAX_RAM = 8;

	private static Settings instance;
	private boolean NSFW = false;
	private boolean integrateDiscord = true;

	private String ram = String.valueOf(Math.min(MAX_RAM, getMaxRam() - getMaxRam() / 4)); //GB

	private Settings() throws IOException {
		if (Files.notExists(SETTINGS_PATH)) return;

		final List<String> strings = Files.readAllLines(SETTINGS_PATH);
		load(strings);
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

	public boolean doesIntegrateDiscord() {
		return integrateDiscord;
	}

	public void setNSFW(boolean NSFW) {
		this.NSFW = NSFW;
	}

	public void setIntegrateDiscord(boolean integrateDiscord) {
		this.integrateDiscord = integrateDiscord;
	}

	public String getRam() {
		return ram;
	}

	public void setRam(String ram) {
		this.ram = ram;
	}

	private void load(List<String> strings) {
		if (strings.size() >= 1) NSFW = Boolean.parseBoolean(strings.get(0));
		if (strings.size() >= 2) ram = strings.get(1);
		if (strings.size() >= 3) integrateDiscord = Boolean.parseBoolean(strings.get(2));
	}

	public void save() throws IOException {
		Files.writeString(SETTINGS_PATH, String.join("\n", String.valueOf(NSFW), ram, String.valueOf(integrateDiscord)), CREATE, TRUNCATE_EXISTING);
	}
}
