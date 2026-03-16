package de.nvbw.bfrk.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import de.nvbw.base.NVBWLogger;


public class Bild {
	private static final Logger LOG = NVBWLogger.getLogger(Bild.class);

	private static final String INTRANET_URL = "http://10.70.190.131:80";

    private static Connection bfrkConn = null;

    public Bild() {
    	if(bfrkConn == null)
    		bfrkConn = DBVerbindung.getDBVerbindung();
    }

	public static void setDBConnection(Connection connection) {
		bfrkConn = connection;
	}
	

    public static String getBildUrl(String bildname, String dhid) {
    	return getBildUrl(bildname, dhid, true);
    }

    public static String getBildUrl(String bildname, String dhid, boolean nuroeffentlich) {
    	String outputurl = "";

    	String hstDHID = "";
    	if((dhid != null) || !dhid.equals("")) {
    		String[] dhidteile = dhid.split(":", -1);
    		if(dhidteile.length >= 3)
    			hstDHID = dhidteile[0] + ":" + dhidteile[1] + ":" + dhidteile[2];
    	}
//    	System.out.println("orig-dhid war ===" + dhid + "===, Hst-DHID ist ===" + hstDHID + "===");
    			
		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				LOG.warning("FEHLER: keine DB-Verbindung offen in Klasse Bild, Methode getBildUrl");
				return outputurl;
			}
		} catch (SQLException e1) {
			LOG.warning("FEHLER: keine DB-Verbindung offen in Klasse Bild, Methode getBildUrl, bei SQLException " + e1.toString());
			return outputurl;
		}

    	String selectBildSql = "SELECT url, oeffentlich FROM bild WHERE bildname = ?;";

		PreparedStatement selectBildStmt;
		try {
			selectBildStmt = bfrkConn.prepareStatement(selectBildSql);
			selectBildStmt.setString(1, bildname);
//			LOG.fine("Haltestelle query: " + selectBildStmt.toString() + "===");

			ResultSet selectBildRS = selectBildStmt.executeQuery();

			if(selectBildRS.next()) {
				String url = selectBildRS.getString("url");
				boolean oeffentlich = selectBildRS.getBoolean("oeffentlich");
				if(nuroeffentlich == true) {
					if(oeffentlich)
						outputurl = url;
				} else {
					if(!hstDHID.equals(""))
						outputurl = INTRANET_URL + "/bfrk/haltestelle/bilder/" + hstDHID + "/" + bildname;
				}
			}
			selectBildRS.close();
			selectBildStmt.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.severe("SQLException::: " + e.toString());
		}    	
    	return outputurl;
    }
}
