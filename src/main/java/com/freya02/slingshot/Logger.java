package com.freya02.slingshot;

import com.freya02.ui.ErrorScene;
import com.freya02.ui.UILib;
import javafx.application.Platform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class Logger {
	private static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("d/MM/yy - HH:mm:ss");
	private static PrintStream writer;

	public static void setupLog(Path logsPath) throws IOException {
		writer = new PrintStream(Files.newOutputStream(logsPath, CREATE, TRUNCATE_EXISTING), true);
	}

	private static void doLog(String level, String message) {
		final StackWalker.StackFrame frame = StackWalker.getInstance().walk(s -> s.skip(2).findFirst()).orElseThrow();

		final String format = String.format("[%s] [%s] [%s] : [%s#%s:%s] : %s%n",
				TIME_PATTERN.format(LocalDateTime.now()),
				level,
				Thread.currentThread().getName(),
				frame.getClassName(),
				frame.getMethodName(),
				frame.getLineNumber(),
				message);

		if (writer != null) writer.print(format);
		System.out.print(format);
	}

	public static void info(String message) { doLog("Info", message); }
	public static void error(String message) { doLog("Error", message); }
	public static void warn(String message) { doLog("Warn", message); }

	public static void info(Object message) { doLog("Info", message.toString()); }
	public static void error(Object message) { doLog("Error", message.toString()); }
	public static void warn(Object message) { doLog("Warn", message.toString()); }

	public static void handleError(Throwable t) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		final PrintStream printStream = new PrintStream(out);
		t.printStackTrace(printStream);

		final String stackTrace = new String(out.toByteArray());
		writer.println(stackTrace);

		final Runnable code = () -> new ErrorScene("An unexpected exception occurred", stackTrace).showAndWait();

		if (Platform.isFxApplicationThread()) {
			code.run(); //Blocking call, but JFX thread still available due to event loop caused by Stage#showAndWait()
		} else {
			UILib.runAndWait(code); //Blocking call because of runAndWait() (event loop too)
		}
	}
}
