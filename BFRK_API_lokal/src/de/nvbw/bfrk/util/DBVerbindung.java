package de.nvbw.bfrk.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import javax.servlet.ServletConfig;

import de.nvbw.base.Applicationconfiguration;

public class DBVerbindung {
    private static Connection bfrkConn = null;
	private static Applicationconfiguration configuration = null;

	private static Date dbVerbindungsaufbauzeitpunkt = null;
	
	public static Connection getDBVerbindung() {
		System.out.println("in DBVerbindung Constructor zu Beginn: " + new Date());
	
		configuration = new Applicationconfiguration();
	
		try {
			System.out.println("Vor Aufruf postgresl-Treiber ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("Nach Aufruf postgresl-Treiber!");
			
			String bfrkUrl = configuration.db_application_url;
			bfrkConn = DriverManager.getConnection(bfrkUrl, configuration.db_application_username, configuration.db_application_password);
			System.out.println("Verbindungsaufbau mit Url ===" + bfrkUrl + "==="
				+ ", Username: " + configuration.db_application_username
				+ ", Passwort: " + configuration.db_application_password);
			if(bfrkConn == null)
				System.out.println("DB-Connection fehlgeschlagen");
			else {
				System.out.println("Status DB-Connection is valid? " + bfrkConn.isValid(0));
				ReaderBase.setDBConnection(bfrkConn);
				Bild.setDBConnection(bfrkConn);
				dbVerbindungsaufbauzeitpunkt = new Date();
			}
		} 
		catch(ClassNotFoundException e) {
			System.out.println("ClassNotFoundException happend within init(), details follows"
				+ " " + e.toString());
			return null;
		}
		catch( SQLException e) {
			System.out.println("SQLException happened within init(), details follows ...");
			System.out.println(e.toString());
			return null;
		}    
		return bfrkConn;
	}	

	public static Date getDBVerbindungsaufbauzeitpunkt() {
		return dbVerbindungsaufbauzeitpunkt;
	}
	
	public static double getDBActiveTime() {
		double activeTime = 0;
		String selectActiveTimeSql = "SELECT active_time from pg_stat_database WHERE datname = 'bfrk';";
		try {
			Statement statement = bfrkConn.createStatement();
			ResultSet resultset = statement.executeQuery(selectActiveTimeSql);
			if(resultset.next()) {
				activeTime = resultset.getDouble("active_time");
			}
			resultset.close();
			statement.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return activeTime;
	}

	public static long gethoechsteTabellenID(String tabellenname) {
		long id = 0;

		if(		tabellenname.equals("Objekt")
			||	tabellenname.equals("Importdatei")
			||	tabellenname.equals("Merkmal")
			||	tabellenname.equals("Osmimport")
			||	tabellenname.equals("Osmobjektbezug")) {
			
			String selecthoechsteIDSql = "SELECT id FROM " + tabellenname + " ORDER BY ID DESC LIMIT 1;";
			try {
				Statement selectmaxIDStmt = bfrkConn.createStatement();
				ResultSet resultset = selectmaxIDStmt.executeQuery(selecthoechsteIDSql);
				if(resultset.next()) {
					id = resultset.getLong("id");
				}
				resultset.close();
				selectmaxIDStmt.close();
			} catch (SQLException e) {
				System.out.println("SQL-Fehler: " + e.toString());
			}
		}
		return id;
	}
}
