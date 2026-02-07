package de.nvbw.bfrk.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

import de.nvbw.base.Applicationconfiguration;
import de.nvbw.base.NVBWLogger;

public class DBVerbindung {
    private static Connection bfrkConn = null;
	private static Applicationconfiguration configuration = null;

	private static Date dbVerbindungsaufbauzeitpunkt = null;
	private static String dbname = "";
	private static String dbnameoeffentlich = "";

	public DBVerbindung() {
		System.out.println("bin im DBVerbindung/constructor zu Beginn ...");
		internGetDBVerbindung();
	}

	private static void internGetDBVerbindung() {
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
				internGetDBName();
			}
		} 
		catch(ClassNotFoundException e) {
			System.out.println("ClassNotFoundException happened within init(), details follows"
				+ " " + e.toString());
			return;
		}
		catch( SQLException e) {
			System.out.println("SQLException happened within init(), Details: " +
					e.toString());
			return;
		}    
		return;
	}	

	public static Connection getDBVerbindung() {
		System.out.println("in Methode getDBVerbindung ...");
		if(bfrkConn == null)
			internGetDBVerbindung();
		System.out.println(" ist die Variable bfrkConn: " + bfrkConn.toString());
		return bfrkConn;
	}

	public static Date getDBVerbindungsaufbauzeitpunkt() {
		return dbVerbindungsaufbauzeitpunkt;
	}
	
	private static void internGetDBName() {
		String schluessel = "";
		String wert = "";
		String selectNameSql = "SELECT schluessel, wert FROM metadaten WHERE "
			+ "schluessel in ('dbnameöffentlich', 'dbname');";
		try {
			Statement statement = bfrkConn.createStatement();
			System.out.println("innternGetDBName: " + statement.toString());
			ResultSet resultset = statement.executeQuery(selectNameSql);
			while(resultset.next()) {
				schluessel = resultset.getString("schluessel");
				wert = resultset.getString("wert");
				if(schluessel.equals("dbname"))
					dbname = wert;
				if(schluessel.equals("dbnameöffentlich"))
					dbnameoeffentlich = wert;
			}
			resultset.close();
			statement.close();
		} catch (SQLException e) {
			System.out.println("in internGetDBName: SQLException aufgetreten, Details: " + e.toString());
		}
		return;
	}

	public static String getDBName() {
		return dbname;
	}

	public static String getDbnameoeffentlich() {
		return dbnameoeffentlich;
	}

	public static double getDBActiveTime() {
		double activeTime = 0;
		String selectActiveTimeSql = "SELECT active_time from pg_stat_database WHERE datname = ?;";
		try {
			PreparedStatement selectActiveTimeStmt = bfrkConn.prepareStatement(selectActiveTimeSql);
			int stmtindex = 1;
			selectActiveTimeStmt.setString(stmtindex++, dbname);

			ResultSet selectActiveTimeRs = selectActiveTimeStmt.executeQuery();
			if(selectActiveTimeRs.next()) {
				activeTime = selectActiveTimeRs.getDouble("active_time");
			}
			selectActiveTimeRs.close();
			selectActiveTimeStmt.close();
		} catch (SQLException e) {
			System.out.println("in getDBActiveTime: SQLException aufgetreten, Details: " + e.toString());
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
				System.out.println("in gethoechsteTabellenID: SQLException aufgetreten, Details: " + e.toString());
			}
		}
		return id;
	}
}
