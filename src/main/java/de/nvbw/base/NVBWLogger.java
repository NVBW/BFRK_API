package de.nvbw.base;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;


public final class NVBWLogger {

	private static final AtomicBoolean initialized = new AtomicBoolean(false);
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(NVBWLogger.class);

	private static Level consoleLevel = Level.FINER;
	private static Level fileLevel = Level.FINE;
	private static String logFileName = "NVBW.log";

	private NVBWLogger() {
		// verhindern, dass instanziiert wird
	}

	/**
	 * Optional manuelle Initialisierung (z.B. beim Start der WebApp)
	 */
	public static void init(String fileName, Level consoleLvl, Level fileLvl) {
		logFileName = fileName;
		consoleLevel = consoleLvl;
		fileLevel = fileLvl;
		configureRootLogger();
	}

	/**
	 * Liefert einen Logger für die angegebene Klasse
	 */
	public static Logger getLogger(Class<?> clazz) {
		if (!initialized.get()) {
			configureRootLogger();
		}
		return Logger.getLogger(clazz.getName());
	}

	/**
	 * Zentrale Logger-Konfiguration (nur einmal)
	 */
	private static synchronized void configureRootLogger() {
		if (initialized.get()) {
			return;
		}

		Applicationconfiguration applicationconfiguration = new Applicationconfiguration();
		consoleLevel = applicationconfiguration.logging_console_level;
		fileLevel = applicationconfiguration.logging_file_level;
		logFileName = applicationconfiguration.logging_filename;

		Logger rootLogger = Logger.getLogger("");

		// Vorhandene Handler entfernen (wichtig bei Tomcat-Reload)
		for (Handler handler : rootLogger.getHandlers()) {
			rootLogger.removeHandler(handler);
		}

		rootLogger.setLevel(Level.ALL);

		// ----- Console Handler -----
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(consoleLevel);
		consoleHandler.setFormatter(new Formatter() {
			@Override
			public synchronized String format(LogRecord lr) {
				return String.format("[%1$-7s] %2$s%n",
						lr.getLevel().getLocalizedName(),
						lr.getMessage());
			}
		});
		rootLogger.addHandler(consoleHandler);

		// ----- File Handler -----
		try {
			FileHandler fileHandler = new FileHandler(logFileName, true);
			fileHandler.setEncoding(StandardCharsets.UTF_8.toString());
			fileHandler.setLevel(fileLevel);

			fileHandler.setFormatter(new Formatter() {

				@Override
				public synchronized String format(LogRecord lr) {
					String className = lr.getSourceClassName();
					//if (className != null && className.contains(".")) {
					//	className = className.substring(className.lastIndexOf('.') + 1);
					//}
					String methodName = lr.getSourceMethodName();

					return String.format("[%1$tF %1$tT] [%2$-7s] [%3$s/%4$s] %5$s%n",
							new Date(lr.getMillis()),
							lr.getLevel().getLocalizedName(),
							className,
							methodName,
							lr.getMessage());
				}
			});

			rootLogger.addHandler(fileHandler);

			rootLogger.info("Log Console-Level aus Konfigurationsdatei: " + applicationconfiguration.logging_console_level);
			rootLogger.info("Log File-Level aus Konfigurationsdatei: " + applicationconfiguration.logging_file_level);
		} catch (IOException e) {
			e.printStackTrace();
		}

		initialized.set(true);
	}
}
