package com.freya02.slingshot;

import com.freya02.slingshot.settings.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AOT {
	private static final boolean USE_TEST_DLL = false;

	private static final byte[] LOADER_DLL_BYTES;
	private static final byte[] MAIN_DLL_BYTES;
	private static final byte[] ZLIB_BYTES;
	private static final byte[] LIBCURL_BYTES;

	private static final List<String> backgrounds;
	private static final List<String> allBackgrounds;
	private static boolean init = false;

	static {
		try (InputStream loaderDllStream = AOT.class.getResourceAsStream("JNISlingshotLoader.dll");
		     InputStream mainDllStream = AOT.class.getResourceAsStream("JNISlingshot.dll");
		     InputStream libcurlDllStream = AOT.class.getResourceAsStream("libcurl.dll");
		     InputStream zlibDllStream = AOT.class.getResourceAsStream("zlib1.dll")) {

			LOADER_DLL_BYTES = loaderDllStream.readAllBytes();
			MAIN_DLL_BYTES = mainDllStream.readAllBytes();
			LIBCURL_BYTES = libcurlDllStream.readAllBytes();
			ZLIB_BYTES = zlibDllStream.readAllBytes();

			System.out.println("Preloaded DLLs");

			//Should be [Project]/target/classes
			allBackgrounds = Files.walk(Path.of(SlingshotController.class.getProtectionDomain().getCodeSource().getLocation().toURI()).resolve("com\\freya02\\slingshot\\backgrounds"))
					.filter(Files::isRegularFile)
					.map(p -> p.getFileName().toString())
					.collect(Collectors.toList());

			backgrounds = allBackgrounds.stream().filter(s -> !s.startsWith("cat")).collect(Collectors.toList());

			System.out.println("Loaded " + allBackgrounds.size() + " backgrounds");
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private static native void addDllPath(String dllPath);

	public synchronized static void init() throws IOException {
		if (init) return;
		init = true;

		final Path tempDllFolder = Files.createTempDirectory("JNISlingshot");
		final String loaderDllPath = writeTempDll(tempDllFolder, "JNISlingshotLoader.dll", LOADER_DLL_BYTES);
		final String mainDllPath = writeTempDll(tempDllFolder, "JNISlingshot.dll", MAIN_DLL_BYTES);
		writeTempDll(tempDllFolder, "libcurl.dll", LIBCURL_BYTES);
		writeTempDll(tempDllFolder, "zlib1.dll", ZLIB_BYTES);

		System.load(loaderDllPath);
		addDllPath(tempDllFolder.toString());

		System.load(USE_TEST_DLL ? Path.of("JNISlingshot\\cmake-build-release\\JNISlingshot.dll").toAbsolutePath().toString() : mainDllPath);
	}

	private static String writeTempDll(Path tempDllDir, String dllName, byte[] bytes) throws IOException {
		final Path tempDll = tempDllDir.resolve(dllName);
		Files.write(tempDll, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
		return tempDll.toString();
	}

	public static String getRandomBackgroundUrl() {
		List<String> backgroundSource = Settings.getInstance().isNSFW() ? allBackgrounds : backgrounds;
		final String backgroundName = backgroundSource.get(new Random().nextInt(backgroundSource.size()));
		return SlingshotController.class.getResource("backgrounds/" + backgroundName).toString();
	}
}
