package de.nvbw.base;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.security.CodeSource;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;

public class BFRKApiApplicationconfiguration {

	public String servername = "";
	public String application_homedir = "";
	public String application_datadir = "";
	public String eyevisdownloadurl = "";
	public String eyevisdownloaddir = "";
	public String divaexportverzeichnis = "";
	public String db_application_url = "";
	public String db_application_username = "";
	public String db_application_password = "";
	public String logging_filename = "";
	public Level logging_console_level = Level.FINEST;
	public Level logging_file_level = Level.FINEST;

	public static String getPropertiesFullPathAndName() {
		try {
			CodeSource codeSource =
					BFRKApiApplicationconfiguration.class
							.getProtectionDomain()
							.getCodeSource();

			if (codeSource == null) {
				return null;
			}


			// z.B.
			// /var/lib/tomcat9/webapps/bfrk_api-DEVELOP/WEB-INF/classes
			File dir = new File(codeSource.getLocation().toURI());
			System.out.println("von file dir komm: " + dir.toString());
			String gefundenerpfad = dir.getAbsolutePath();
			if(gefundenerpfad.endsWith("WEB-INF/classes")) {
				gefundenerpfad = gefundenerpfad.substring(0, gefundenerpfad.indexOf("/WEB-INF/classes"));
				System.out.println("gefundenerpfad netto ===" + gefundenerpfad + "===");
				gefundenerpfad = gefundenerpfad.substring(gefundenerpfad.lastIndexOf(File.separator) + 1);
				System.out.println("hoffentlich netto warfile ===" + gefundenerpfad + "===");

				if(gefundenerpfad.equals("bfrk_api-DEVELOP"))
					gefundenerpfad = "/daten/NVBWAdmin/bfrk_api_home-DEVELOP/bfrk_api-DEVELOP.properties";
				else if(gefundenerpfad.equals("bfrk_api-STAGING"))
					gefundenerpfad = "/daten/NVBWAdmin/bfrk_api_home-STAGING/bfrk_api-STAGING.properties";
				else if(gefundenerpfad.equals("bfrk_api"))
					gefundenerpfad = "/daten/NVBWAdmin/bfrk_api_home/bfrk_api.properties";
				else {
					System.out.println("ich gebe auf, die Appliacationsconfig zu lesen ===" + gefundenerpfad);
					return null;
				}
				return gefundenerpfad;
			} else
				return null;
		} catch (Exception e) {
			System.out.println("Exception aufgetreten in BFRKApiApplicationconfiguration, Details: "
					+ e.toString());
		}
		return null;
	}

	public BFRKApiApplicationconfiguration() {

		String configuration_filename = "";

		String userdir = System.getProperty("user.dir");
		System.out.println("current dir, is it good?   ===" + userdir);

		configuration_filename = getPropertiesFullPathAndName();
		if(configuration_filename == null) {
			System.out.println("von getPropertiesFullPathAndName kam null zurück ABBRUCH ===");
			return;
		}
		System.out.println("configuration_filename gesetzt: ===" + configuration_filename + "===");

		File gibtespropertiesfile = new File(configuration_filename);
		System.out.println("Gibt es das properties file: " + gibtespropertiesfile.exists());
		System.out.println("ist das properties file eine Datei: " + gibtespropertiesfile.isFile());


		// get some configuration infos
		if(File.separator.equals("\\"))
			configuration_filename = "C:\\Users\\SEI\\IdeaProjects\\BFRK_API\\bfrk_api.properties";

		System.out.println("configuration_filename ===" + configuration_filename+ "===");

		try {
			Reader reader = new FileReader( configuration_filename );
			Properties prop = new Properties();
			prop.load( reader );
				// iterate over all properties and remove in-line comments in property values
			for (Entry<Object, Object> entry : prop.entrySet()) {
				if(entry.getValue().toString().contains("#")) {
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

			if( prop.getProperty("eyevisdownloadurl") != null)
				this.eyevisdownloadurl = prop.getProperty("eyevisdownloadurl");
			if( prop.getProperty("eyevisdownloaddir") != null)
				this.eyevisdownloaddir = prop.getProperty("eyevisdownloaddir");
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
			if( prop.getProperty("logging_console_level") != null)
				this.logging_console_level = Level.parse(prop.getProperty("logging_console_level"));
			if( prop.getProperty("logging_file_level") != null)
				this.logging_file_level = Level.parse(prop.getProperty("logging_file_level"));


			System.out.println("Info:  .servername                              ==="+this.servername+"===");
			System.out.println("Info:  .application_homedir                     ==="+this.application_homedir+"===");
			System.out.println("Info:  .application_datadir                     ==="+this.application_datadir+"===");

			System.out.println("Info:  .eyevisdownloadurl                      ==="+this.eyevisdownloadurl+"===");
			System.out.println("Info:  .eyevisdownloaddir                     ==="+this.eyevisdownloaddir+"===");
			System.out.println("Info:  .divaexportverzeichnis                   ==="+this.divaexportverzeichnis+"===");
			System.out.println("Info:  .db_application_url                      ==="+this.db_application_url+"===");
			System.out.println("Info:  .db_application_username                 ==="+this.db_application_username+"===");
			System.out.println("Info:  .db_application_password                 ==="+this.db_application_password+"===");
			System.out.println("Info:  .logging_filename                        ==="+this.logging_filename +"===");
			System.out.println("Info:  .logging_console_level                   ==="+this.logging_console_level.toString() +"===");
			System.out.println("Info:  .logging_file_level                      ==="+this.logging_file_level.toString() +"===");

		} catch (Exception e) {
			System.out.println("FEHLER: Programm-Konfigurationsdatei kann nicht gelesen werden: ==="
				+ configuration_filename + "===");

			System.out.println("Info: current dir, is it good?   ===" + userdir);

			System.out.println(e.toString());
			return;
		}
	}


}
