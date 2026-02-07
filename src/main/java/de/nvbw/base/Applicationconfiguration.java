package de.nvbw.base;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

public class Applicationconfiguration {

	public String servername = "";
	public String application_homedir = "";
	public String application_datadir = "";
	public String divaexportverzeichnis = "";
	public String db_application_url = "";
	public String db_application_username = "";
	public String db_application_password = "";
	public String logging_filename = "";
	public String logging_path = "";
	public Level logging_console_level = Level.FINEST;
	public Level logging_file_level = Level.FINEST;
	
	public Applicationconfiguration () {
		this("../bfrkapi.properties");
	}

	public Applicationconfiguration (String path) {
		boolean debugoutput = true;

		String configuration_filename = "";

		System.out.println("path to configuration file as method start ===" + path + "===");
		String userdir = System.getProperty("user.dir");
		System.out.println("current dir, is it good?   ===" + userdir);
		
		// get some configuration infos
		if(File.separator.equals("\\"))
			configuration_filename = "C:\\Users\\SEI\\IdeaProjects\\BFRK_API\\bfrk_api.properties";
		else
			configuration_filename = "/daten/NVBWAdmin/bfrk_api_home-STAGING/bfrk_api.properties";

		if(debugoutput)
			System.out.println("configuration_filename ===" + configuration_filename+ "===");

		try {
			Reader reader = new FileReader( configuration_filename );
			Properties prop = new Properties();
			prop.load( reader );
				// iterate over all properties and remove in-line comments in property values
			for (Entry<Object, Object> entry : prop.entrySet()) {
				if(entry.getValue().toString().indexOf("#") != -1) {
					String tempentry = entry.getValue().toString().substring(0, entry.getValue().toString().indexOf("#"));
					tempentry = tempentry.trim();
					prop.setProperty(entry.getKey().toString(),  tempentry);
				}
			}
			prop.list( System.out );
		

			if( prop.getProperty("servername") != null)
				this.servername = prop.getProperty("servername");
			if( prop.getProperty("application_homedir") != null)
				this.application_homedir = prop.getProperty("application_homedir");
			if( prop.getProperty("application_datadir") != null)
				this.application_datadir = prop.getProperty("application_datadir");
			if( prop.getProperty("divaexportdir") != null)
				this.divaexportverzeichnis = prop.getProperty("divaexportdir");
			if( prop.getProperty("db_application_url") != null)
				this.db_application_url = prop.getProperty("db_application_url");
			if( prop.getProperty("db_application_username") != null)
				this.db_application_username = prop.getProperty("db_application_username");
			//if( prop.getProperty("db_application_password") != null)
			//	this.db_application_password = prop.getProperty("db_application_password");
			if( prop.getProperty("logging_filename") != null)
				this.logging_filename = prop.getProperty("logging_filename");
			if( prop.getProperty("logging_path") != null)
				this.logging_path = prop.getProperty("logging_path");
			if( prop.getProperty("logging_console_level") != null)
				this.logging_console_level = Level.parse(prop.getProperty("logging_console_level"));
			if( prop.getProperty("logging_file_level") != null)
				this.logging_file_level = Level.parse(prop.getProperty("logging_file_level"));

			
			System.out.println("Info:  .servername                              ==="+this.servername+"===");
			System.out.println("Info:  .application_homedir                     ==="+this.application_homedir+"===");
			System.out.println("Info:  .application_datadir                     ==="+this.application_datadir+"===");
			System.out.println("Info:  .divaexportverzeichnis                   ==="+this.divaexportverzeichnis+"===");
			System.out.println("Info:  .db_application_url                      ==="+this.db_application_url+"===");
			System.out.println("Info:  .db_application_username                 ==="+this.db_application_username+"===");
			System.out.println("Info:  .db_application_password                 ==="+this.db_application_password+"===");
			System.out.println("Info:  .logging_filename                        ==="+this.logging_filename +"===");
			System.out.println("Info:  .logging_path                            ==="+this.logging_path +"===");
			System.out.println("Info:  .logging_console_level                   ==="+this.logging_console_level.toString() +"===");
			System.out.println("Info:  .logging_file_level                      ==="+this.logging_file_level.toString() +"===");

		} catch (Exception e) {
			System.out.println("FEHLER: Programm-Konfigurationsdatei kann nicht gelesen werden: ==="
				+ configuration_filename + "===");

			System.out.println("Info: current dir, is it good?   ===" + userdir);

			e.printStackTrace();
			return;
		}
	}
}
