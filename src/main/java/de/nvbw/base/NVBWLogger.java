package de.nvbw.base;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;



public class NVBWLogger {
    static Logger logger;
    private static Level CONSOLE_LOGGING_LEVEL = Level.FINER;
    private static Level FILE_LOGGING_LEVEL = Level.FINE;
	
    private NVBWLogger(String logdateiname, Level consoleLevel, Level fileLevel) throws IOException {
	    logger = Logger.getLogger(NVBWLogger.class.getName());
	    logger.setUseParentHandlers(true);
	    ConsoleHandler consolehandler = new ConsoleHandler();
	    consolehandler.setFormatter(new SimpleFormatter() {
	        //private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s%n";
	        private static final String format = "[%1$-7s] %2$s%n";

	        @Override
	        public synchronized String format(LogRecord lr) {
	            return String.format(format,
	                    lr.getLevel().getLocalizedName(),
	                    lr.getMessage()
	            );
	        }
	    });
	    consolehandler.setLevel(consoleLevel);
	    logger.addHandler(consolehandler);

	    try {
	        FileHandler filehandler = new FileHandler(logdateiname, true);
	        filehandler.setEncoding(StandardCharsets.UTF_8.toString());
	        filehandler.setFormatter(new SimpleFormatter() {
	            private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s%n";

	            @Override
	            public synchronized String format(LogRecord lr) {
	                return String.format(format,
	                        new Date(lr.getMillis()),
	                        lr.getLevel().getLocalizedName(),
	                        lr.getMessage()
	                );
	            }
	        });
	        filehandler.setLevel(fileLevel);
	        logger.addHandler(filehandler);
	    } catch(IOException ioerr) {
	    	ioerr.printStackTrace();
	    }		
    }
 
	private NVBWLogger() throws IOException {
		this("NVBW.log", CONSOLE_LOGGING_LEVEL, FILE_LOGGING_LEVEL);
	}

	private static Logger getLogger() {
			// wenn logger nicht definiert ist 
			// ODER kein Handler vorhanden ist (dann ist offenbar ein Fehler passiert, verursacht durch Geotools Exception?)
			// dann Logger initialisieren
	    if((logger == null) || (logger.getHandlers().length == 0)) {
	        try {
	            new NVBWLogger();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	    } else {
	    	/*
	    	System.out.println(logger.getHandlers().length);
	    	for(int handleri = 0; handleri < logger.getHandlers().length; handleri++) {
	    		Handler akthandler = logger.getHandlers()[handleri];
	    		System.out.println("Loglevel: " + akthandler.getLevel());
	    		System.out.println("isloggable: " + akthandler.isLoggable(null));
	    	}
	    	*/
	    }
	    return logger;
	}

	public static void init(String dateiname, Level consoleLevel, Level fileLevel) {
        try {
            new NVBWLogger(dateiname, consoleLevel, fileLevel);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	public static void init(Level consoleLevel, Level fileLevel) {
		try {
			Applicationconfiguration configuration = new Applicationconfiguration();
			String dateiname = "dummy.log";
			if(configuration.logging_filename.contains(File.separator))
				dateiname = configuration.logging_filename;
			else
				dateiname = configuration.logging_path + File.separator + configuration.logging_filename;
			new NVBWLogger(dateiname, consoleLevel, fileLevel);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void log(Level level, String msg){
	    getLogger().log(level, msg);
	}

	public static void severe(String msg){
	    getLogger().log(Level.SEVERE, msg);
	}

	public static void warning(String msg){
	    getLogger().log(Level.WARNING, msg);
	}

	public static void info(String msg){
	    getLogger().log(Level.INFO, msg);
	}

	public static void fine(String msg){
	    getLogger().log(Level.FINE, msg);
	}

	public static void finer(String msg){
	    getLogger().log(Level.FINER, msg);
	}

	public static void finest(String msg){
	    getLogger().log(Level.FINEST, msg);
	}
}
