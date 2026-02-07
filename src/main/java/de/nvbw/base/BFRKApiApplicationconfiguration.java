package de.nvbw.base;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Properties;
import java.util.logging.Level;

public class BFRKApiApplicationconfiguration extends Applicationconfiguration {

	public String authorisierungsid = "";

	public BFRKApiApplicationconfiguration () {
		this("../bfrkapi.properties");
	}

	public BFRKApiApplicationconfiguration (String path) {
		boolean debugoutput = true;

		String configuration_filename = "";

		System.out.println("path to configuration file as method start ===" + path + "===");
		String userdir = System.getProperty("user.dir");
		System.out.println("current dir, is it good?   ===" + userdir);
		
		if(File.separator.equals("\\"))
			configuration_filename = "C:\\Users\\SEI\\IdeaProjects\\BFRK_API\\bfrk_api-STAGING.properties";
		else
			configuration_filename = "/daten/NVBWAdmin/bfrk_api_home-STAGING/bfrk_api.properties";

		if(debugoutput)
			System.out.println("configuration_filename ===" + configuration_filename+ "===");

		try {
			Reader reader = new FileReader( configuration_filename );
			Properties prop = new Properties();
			prop.load( reader );
			prop.list( System.out );
		

			if( prop.getProperty("authorisierungsid") != null)
				this.authorisierungsid = prop.getProperty("authorisierungsid");


			if(debugoutput) {
				System.out.println(" .servername                              ==="+this.servername+"===");
				System.out.println(" .application_homedir                     ==="+this.application_homedir+"===");
				System.out.println(" .application_datadir                     ==="+this.application_datadir+"===");
				System.out.println(" .db_application_url                      ==="+this.db_application_url+"===");
				System.out.println(" .db_application_username                 ==="+this.db_application_username+"===");
				System.out.println(" .db_application_password                 ==="+this.db_application_password+"===");
				System.out.println(" .logging_filename                        ==="+this.logging_filename +"===");
				System.out.println(" .logging_console_level                   ==="+this.logging_console_level.toString() +"===");
				System.out.println(" .logging_file_level                      ==="+this.logging_file_level.toString() +"===");
			}

			if(debugoutput) {
				System.out.println(" .authorisierungsid                      ===" + this.authorisierungsid + "===");
			}
		} catch (Exception e) {
			System.out.println("ERROR: failed to read file ==="+configuration_filename+"===");
			e.printStackTrace();
			return;
		}
	}
}
