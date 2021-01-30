package com.freya02.slingshot;

import com.freya02.slingshot.settings.Settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AOT {
	private static final boolean USE_TEST_DLL = false;

	private static final byte[] LOADER_DLL_BYTES;
	private static final byte[] MAIN_DLL_BYTES;
	private static final byte[] ZLIB_BYTES;
	private static final byte[] LIBCURL_BYTES;

	private static final List<ByteArrayInputStream> backgroundBytes;
	private static final int nsfwOffset;
	private static boolean init = false;

	static {
		try (InputStream loaderDllStream = getDllStream("JNISlingshotLoader.dll");
		     InputStream mainDllStream = getDllStream("JNISlingshot.dll");
		     InputStream libcurlDllStream = getDllStream("libcurl.dll");
		     InputStream zlibDllStream = getDllStream("zlib1.dll")) {

			LOADER_DLL_BYTES = loaderDllStream.readAllBytes();
			MAIN_DLL_BYTES = mainDllStream.readAllBytes();
			LIBCURL_BYTES = libcurlDllStream.readAllBytes();
			ZLIB_BYTES = zlibDllStream.readAllBytes();

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

	private static InputStream getDllStream(String dllName) throws URISyntaxException, IOException {
		return Files.newInputStream(getProjectPath("JNISlingshot\\cmake-build-release\\" + dllName));
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

	public static ByteArrayInputStream getRandomBackgroundBytes() {
		int max = Settings.getInstance().isNSFW() ? backgroundBytes.size() : backgroundBytes.size() - nsfwOffset;
		final ByteArrayInputStream inputStream = backgroundBytes.get(new Random().nextInt(max));
		inputStream.reset();
		return inputStream;
	}
}
