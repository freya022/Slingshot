package com.freya02.slingshot;

import com.freya02.io.IOUtils;
import com.freya02.slingshot.settings.Settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class AOT {
	private static final boolean USE_DEBUG_DLL = false;

	private static final Map<String, DLL> DLLS = new HashMap<>(5);

	private static final List<ByteArrayInputStream> backgroundBytes;
	private static final int nsfwOffset;
	private static boolean init = false;

	static {
		try {
			if (USE_DEBUG_DLL) {
				Logger.warn("Using Slingshot debug DLLs, PID: " + IOUtils.getPID());
			}

			preloadDll("JNISlingshotLoader", "JNISlingshotLoader.dll");
			preloadDll("JNISlingshot", "JNISlingshot.dll");
			preloadDll("libcurl", USE_DEBUG_DLL ? "libcurl-d.dll" : "libcurl.dll");
			preloadDll("zlib", USE_DEBUG_DLL ? "zlibd1.dll" : "zlib1.dll");
			preloadDll("fmt", USE_DEBUG_DLL ? "fmtd.dll" : "fmt.dll");

			System.out.println("Preloaded DLLs");

			//Should be [Project]/target/classes
			int offset = 0;
			final List<Path> paths = Files.walk(getProjectPath("ExternalResources\\backgrounds"))
					.filter(Files::isRegularFile)
					.collect(Collectors.toList());
			backgroundBytes = new ArrayList<>(paths.size());
			for (Path path : paths) {
				final byte[] bytes = Files.readAllBytes(path);

				backgroundBytes.add(backgroundBytes.size() - offset, new ByteArrayInputStream(bytes));

				if (path.getFileName().toString().startsWith("cat")) {
					offset++;
				}
			}

			nsfwOffset = offset;

			System.out.println("Loaded " + backgroundBytes.size() + " backgrounds");
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static void preloadDll(String libName, String dllName) throws IOException, URISyntaxException {
		try (InputStream loaderDllStream = getDllStream(dllName)) {
			DLLS.put(libName, new DLL(dllName, loaderDllStream.readAllBytes()));
		}
	}

	private static InputStream getDllStream(String dllName) throws URISyntaxException, IOException {
		return Files.newInputStream(getProjectPath("JNISlingshot\\cmake-build-" + (USE_DEBUG_DLL ? "debug" : "release") + "\\" + dllName));
	}

	private static Path getProjectPath() throws URISyntaxException {
		return Path.of(SlingshotController.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolve("..\\..");
	}

	private static Path getProjectPath(String toResolve) throws URISyntaxException {
		return getProjectPath().resolve(toResolve);
	}

	private static native void addDllPath(String dllPath);

	public synchronized static void init() throws IOException {
		if (init) return;
		init = true;

		final Path tempDllFolder = Files.createTempDirectory("JNISlingshot");
		for (Map.Entry<String, DLL> entry : DLLS.entrySet()) {
			DLL dll = entry.getValue();
			writeTempDll(tempDllFolder, dll.dllName, dll.bytes);
			Logger.info("Loading " + entry.getKey() + " in " + (USE_DEBUG_DLL ? "debug" : "release") + " mode");
		}
		final String loaderDllPath = tempDllFolder.resolve(DLLS.get("JNISlingshotLoader").dllName).toString();
		final String mainDllPath = tempDllFolder.resolve(DLLS.get("JNISlingshot").dllName).toString();

		System.load(loaderDllPath);
		addDllPath(tempDllFolder.toString());

		System.load(mainDllPath);
	}

	private static void writeTempDll(Path tempDllDir, String dllName, byte[] bytes) throws IOException {
		final Path tempDll = tempDllDir.resolve(dllName);
		Files.write(tempDll, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
	}

	public static ByteArrayInputStream getRandomBackgroundBytes() {
		int max = Settings.getInstance().isNSFW() ? backgroundBytes.size() : backgroundBytes.size() - nsfwOffset;
		final ByteArrayInputStream inputStream = backgroundBytes.get(new Random().nextInt(max));
		inputStream.reset();
		return inputStream;
	}

	private static class DLL {
		private final String dllName;
		private final byte[] bytes;

		private DLL(String dllName, byte[] bytes) {
			this.dllName = dllName;
			this.bytes = bytes;
		}
	}
}
