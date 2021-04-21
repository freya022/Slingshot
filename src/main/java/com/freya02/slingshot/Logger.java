package com.freya02.slingshot;

import com.freya02.ui.ErrorScene;
import com.freya02.ui.UILib;
import javafx.application.Platform;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class Logger {
	private static final DateTimeFormatter TIME_PATTERN = DateTimeFormatter.ofPattern("d/MM/yy - HH:mm:ss");
	private static final PrintStream oldOut = System.out;
	private static PrintStream writer;

	public static void setupLog(Path logsPath) throws IOException {
		final OutputStream out = Files.newOutputStream(logsPath, CREATE, TRUNCATE_EXISTING);
		writer = new PrintStream(out, true);

		final PrintStream sysPrinter = new PrintStream(out, true) {
			private final int extraSkip = 2;

			@Override
			public void print(boolean b) {
				info(b, extraSkip);
			}

			@Override
			public void print(char c) {
				info(c, extraSkip);
			}

			@Override
			public void print(int i) {
				info(i, extraSkip);
			}

			@Override
			public void print(long l) {
				info(l, extraSkip);
			}

			@Override
			public void print(float f) {
				info(f, extraSkip);
			}

			@Override
			public void print(double d) {
				info(d, extraSkip);
			}

			@Override
			public void print(char[] s) {
				info(new String(s), extraSkip);
			}

			@Override
			public void print(@Nullable String s) {
				info(s, extraSkip);
			}

			@Override
			public void print(@Nullable Object obj) {
				if (obj != null) {
					info(obj, extraSkip);
				} else info("null", extraSkip);
			}
		};

		System.setOut(sysPrinter);
		System.setErr(sysPrinter);
	}

	private static synchronized void doLog(String level, String message, int extraSkip) {
		final StackWalker.StackFrame frame = StackWalker.getInstance().walk(s -> s.skip(2 + extraSkip).findFirst()).orElseThrow();

		final String format = String.format("[%s] [%s] [%s] : [%s#%s:%s] : %s%n",
				TIME_PATTERN.format(LocalDateTime.now()),
				level,
				Thread.currentThread().getName(),
				frame.getClassName(),
				frame.getMethodName(),
				frame.getLineNumber(),
				message);

		if (writer != null) writer.print(format);
		oldOut.print(format);
	}

	public static void info(String message) { doLog("Info", message, 0); }
	public static void error(String message) { doLog("Error", message, 0); }
	public static void warn(String message) { doLog("Warn", message, 0); }

	public static void info(Object message) { doLog("Info", message.toString(), 0); }
	public static void error(Object message) { doLog("Error", message.toString(), 0); }
	public static void warn(Object message) { doLog("Warn", message.toString(), 0); }

	public static void info(String message, int extraSkip) { doLog("Info", message, extraSkip); }
	public static void error(String message, int extraSkip) { doLog("Error", message, extraSkip); }
	public static void warn(String message, int extraSkip) { doLog("Warn", message, extraSkip); }

	public static void info(Object message, int extraSkip) { doLog("Info", message.toString(), extraSkip); }
	public static void error(Object message, int extraSkip) { doLog("Error", message.toString(), extraSkip); }
	public static void warn(Object message, int extraSkip) { doLog("Warn", message.toString(), extraSkip); }

	public static void handleError(Throwable t) {
		final ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		final PrintStream printStream = new PrintStream(out);
		t.printStackTrace(printStream);

		final String stackTrace = new String(out.toByteArray());
		doLog("Error", stackTrace, 0);

		final Runnable code = () -> new ErrorScene("An unexpected exception occurred", stackTrace).showAndWait();

		if (Platform.isFxApplicationThread()) {
			code.run(); //Blocking call, but JFX thread still available due to event loop caused by Stage#showAndWait()
		} else {
			UILib.runAndWait(code); //Blocking call because of runAndWait() (event loop too)
		}
	}
}
