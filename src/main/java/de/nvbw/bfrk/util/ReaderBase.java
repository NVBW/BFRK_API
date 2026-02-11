package de.nvbw.bfrk.util;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.ImageMetadata.ImageMetadataItem;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.base.Applicationconfiguration;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;

public class ReaderBase {
	public static enum Datentyp {Boolean, Numeric, String};

	static Applicationconfiguration configuration = new Applicationconfiguration();
	public static Connection bfrkConn = null;

	public static enum Objektzustand_Typ {neu, vorhandenveraltet, vorhandenaktuell, nichtvorhanden, ungesetzt};
	public static enum Bahnsteigelementart_Typ {bahnsteig_beginn, haltepunkttafel,
		abschnittsbereichtafel, ueberdachung_beginn, ueberdachung_ende,
		treppenzugang, bahnsteighoehenaenderung, bahnsteig_ende, ungesetzt};
	public static enum Koordinatenquelle_Typ {Rohdaten, Pseudogenau, Genau, ungesetzt};

	public static enum Objektart {Bahnhof, Bahnsteig, Bahnsteigelement, Haltestelle, 
		Haltesteig, Aufzug, BuR, Engstelle, Fahrplananzeigetafel,
		Gleisquerung, Informationsstelle, Kartenautomat, Leihradanlage, Parkplatz, Rampe, 
		Rolltreppe, Stationsplan, Taxistand, Toilette, Tuer, Treppe, Verkaufsstelle, Weg, 
		SEVHaltesteig, Notiz, unbekannt};

	public static enum DHID_Typ {Haltestelle, Bereich, Haltesteig, ungueltig};

	private static boolean updateszulaessig = false;

	private static DateFormat date_de_formatter = new SimpleDateFormat("dd.MM.yyyy");
	
	private static Boolean sollFelderkoennenbeliebigsein = false;
	private static Objektzustand_Typ objektzustand = Objektzustand_Typ.ungesetzt;
	
	public static void setDBConnection(Connection connection) {
		bfrkConn = connection;
	}

	public static void setUpdatesZulaessig(boolean wert) {
		updateszulaessig = wert;
	}

	public static void setSollFelderkoennenbeliebigsein(boolean wert) {
		sollFelderkoennenbeliebigsein = wert;
	}

	public static void setObjektzustand(Objektzustand_Typ objektZustand) {
		objektzustand = objektZustand;
	}

	/**
	 * es wird die DHID haltestellenweit zurückgegeben, also de:landkreis:laufendenr
	 * Wenn die DHID nicht erkannt wird, wird leerer String zurückgegeben
	 */
	public static String normierteDHID(String dhid) {
		String dhidnormiert = "";

		// verarbeite normale DHIDs (de:lkr:....)
		Pattern pattern = Pattern.compile("^(de):([01][0-9][0-9][0-9][0-9]):([^:]*)(:([^:]*))?(:([^:]*))?(:(.*))?$");
		Matcher matcher = pattern.matcher(dhid);
		if (matcher.find() ) {
			if ( matcher.groupCount() >= 2 ) {
				dhidnormiert = matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3);
			}
		} else {
			pattern = Pattern.compile("^([A-Za-z][A-Za-z]):([^:]+):([^:]+)(:([^:]*))?(:([^:]*))?(:(.*))?$");
			matcher = pattern.matcher(dhid);
			if (matcher.find() ) {
				if ( matcher.groupCount() >= 2 ) {
					dhidnormiert = matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3);
				}
			} else {
				NVBWLogger.warning("DHID '" + dhid + "' kann nicht normiert werden");
			}
		}
		return dhidnormiert;
	}


	public static DHID_Typ getDHIDTyp(String dhid) {
		String dhidnormiert = "";

		// verarbeite normale DHIDs (de:lkr:....)
		Pattern pattern = Pattern.compile("^(de):([01][0-9][0-9][0-9][0-9]):([^:]*):?([^:]*)?:?([^:]*)?(.*)?$");
		Matcher matcher = pattern.matcher(dhid);
		if (matcher.find() ) {
			for(int index = 1; index <= matcher.groupCount(); index++)
				System.out.println("index: " + index + " ===" + matcher.group(index) + "===");
			if ( matcher.groupCount() == 3 ) {
				return DHID_Typ.Haltestelle;
			} else if(( matcher.groupCount() >= 5 ) && matcher.group(5).equals("")) {
				return DHID_Typ.Bereich;
			} else if(( matcher.groupCount() >= 6 ) && matcher.group(6).equals("")) {
				return DHID_Typ.Haltesteig;
			} else
				return DHID_Typ.ungueltig;
		} else {
			pattern = Pattern.compile("^(ch|de):([^:]+):([^:]+):?([^:]*)?:?([^:]*)?(.*)?$");
			matcher = pattern.matcher(dhid);
			if (matcher.find() ) {
				for(int index = 1; index <= matcher.groupCount(); index++)
					System.out.println("index: " + index + " ===" + matcher.group(index) + "===");
				if ( matcher.groupCount() == 3 ) {
					return DHID_Typ.Haltestelle;
				} else if(( matcher.groupCount() >= 5 ) && matcher.group(5).equals("")) {
					return DHID_Typ.Bereich;
				} else if(( matcher.groupCount() >= 6 ) && matcher.group(6).equals("")) {
					return DHID_Typ.Haltesteig;
				} else
					return DHID_Typ.ungueltig;
			} else {
				NVBWLogger.warning("DHID '" + dhid + "' kann nicht normiert werden");
				return DHID_Typ.ungueltig;
			}
		}
	}


	public static Connection connectDBandgetConnection() {
		String bfrkUrl = configuration.db_application_url;
		String username = configuration.db_application_username;
		String password = configuration.db_application_password;

		try {
			Class.forName("org.postgresql.Driver");
		}
		catch(ClassNotFoundException e) {
			NVBWLogger.severe("Exception ClassNotFoundException, Details ...");
			NVBWLogger.severe(e.toString());
			return null;
		}
		try {
			bfrkConn = DriverManager.getConnection(bfrkUrl, username, password);
		} 	catch( SQLException e) {
			NVBWLogger.severe("SQLException occured, details ...");
			NVBWLogger.severe(e.toString());
			return null;
		}
		return bfrkConn;
	}

		
	public static boolean connectDB() {
		String bfrkUrl = configuration.db_application_url;
		String username = configuration.db_application_username;
		String password = configuration.db_application_password;

		try {
			Class.forName("org.postgresql.Driver");
		}
		catch(ClassNotFoundException e) {
			NVBWLogger.severe("Exception ClassNotFoundException, Details ...");
			NVBWLogger.severe(e.toString());
			return false;
		}
		try {
			bfrkConn = DriverManager.getConnection(bfrkUrl, username, password);
		} 	catch( SQLException e) {
			NVBWLogger.severe("SQLException occured, details ...");
			NVBWLogger.severe(e.toString());
			return false;
		}
		return true;
	}


	public static void closeDB() {
		try {
			bfrkConn.close();
		} catch (SQLException e) {
			NVBWLogger.severe("ERROR: konnte DB-Verbindung nicht schliessen");
			NVBWLogger.severe(e.toString());
		}
	}

	public static void ergaenzeObjektInfraIDtemp(String hstdhid, long objektid, Objektart objektart) {

		if(		(objektart == Objektart.Bahnhof)
			||	(objektart == Objektart.Bahnsteig)
			||	(objektart == Objektart.Haltestelle)
			||	(objektart == Objektart.Haltesteig)
			||	(objektart == Objektart.Notiz)
			||	(objektart == Objektart.SEVHaltesteig))
			return;

		String selectInfraidtempSql = "SELECT o.infraidtemp, hst.dhid as hstdhid FROM "
			+ "objekt AS o JOIN objekt AS hst on o.parent_id = hst.id "
			+ "WHERE hst.dhid = ? AND o.objektart = ?;";

		PreparedStatement selectInfaidtempStmt = null;
		try {
			selectInfaidtempStmt = bfrkConn.prepareStatement(selectInfraidtempSql);
			int stmtindex = 1;
			selectInfaidtempStmt.setString(stmtindex++, hstdhid);
			selectInfaidtempStmt.setString(stmtindex++, objektart.toString());

			ResultSet selectInfraidtempRs = selectInfaidtempStmt.executeQuery();

			int hoechstenummer = 0;
			String aktinfraidtemp = "";
			while (selectInfraidtempRs.next()) {
				aktinfraidtemp = selectInfraidtempRs.getString("infraidtemp");
				if((aktinfraidtemp != null) && !aktinfraidtemp.isEmpty()) {
					int startpos = aktinfraidtemp.lastIndexOf("-");
					String nummerString = aktinfraidtemp.substring(startpos + 1);
					int gefnummer = Integer.parseInt(nummerString);
					if(gefnummer > hoechstenummer)
						hoechstenummer = gefnummer;
				}
			}
			String infraidtemp = "INFRA-" + hstdhid + "-";
			if(		(objektart == Objektart.Aufzug)
				||	(objektart == Objektart.Bahnsteigelement)
				||	(objektart == Objektart.Engstelle)
				||	(objektart == Objektart.Gleisquerung)
				||	(objektart == Objektart.Informationsstelle)
				||	(objektart == Objektart.Leihradanlage)
				||	(objektart == Objektart.Parkplatz)
				||	(objektart == Objektart.Rampe)
				||	(objektart == Objektart.Rolltreppe)
				||	(objektart == Objektart.Stationsplan)
				||	(objektart == Objektart.Toilette)
				||	(objektart == Objektart.Treppe)
				||	(objektart == Objektart.Verkaufsstelle)
				||	(objektart == Objektart.Weg))
				infraidtemp += objektart.toString().toUpperCase();
			else if(objektart == Objektart.BuR)
				infraidtemp += "FAHRRADANLAGE";
			else if(objektart == Objektart.Kartenautomat)
				infraidtemp += "FAHRKARTENAUTOMAT";
			else if(objektart == Objektart.Tuer)
				infraidtemp += "EINGANG";
			else if(objektart == Objektart.Taxistand)
				infraidtemp += "TAXI";
			else {
				NVBWLogger.warning("Aufruf Methode ergaenzeObjektInfraIDtemp mit unerwartetem Wert "
					+ objektart.toString() +", es wird keine infraidtemp ergänzt");
				return;
			}
			infraidtemp += "-" + (hoechstenummer + 1);

			String updateobjektSql = "UPDATE objekt SET infraidtemp = ? WHERE "
				+ "id = ? AND infraidtemp IS NULL;";

			PreparedStatement updateobjektStmt = null;
			try {
				updateobjektStmt = bfrkConn.prepareStatement(updateobjektSql);

				stmtindex = 1;
				updateobjektStmt.setString(stmtindex++, infraidtemp);
				updateobjektStmt.setLong(stmtindex++, objektid);
				NVBWLogger.fine("SQL-update Statement zum speichern infraidtemp in Objekt '"
					+  updateobjektStmt.toString() + "'");

				updateobjektStmt.executeUpdate();
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Update Fehler, als die infraidtemp Tabelle Objekt upgedated werden sollte. " 
					+ "Statement war '" + updateobjektStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
			}
			
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Select zum holen der höchsten infraidtemp Nummer in Tabelle objekt" + "\t" 
				+ "Statement war '" + selectInfaidtempStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}

	/**
	 * am 05.02.2025 von BFRK-Repo Klasse EYEvisBilderMetadaten2BFRKDB Klasse geholt
	 * @param
	 * @return lon und lat in einem Array[2], 7stellig Nachkomma-gekürzt. Wenn ein Fehler aufgetreten ist, wird null zurückgegeben
	 * @throws ImagingException
	 * @throws IOException
	 */
	public static List<Double> getBildEXIFGPSKoordinaten(String dateiname) throws ImagingException, IOException {
	    List<Double> returnArray = new ArrayList<>();
	
	    File file = new File(dateiname);
		if(!file.exists()) {
	    	NVBWLogger.warning("in getBildEXIFGPSKoordinaten, Datei existiert nicht: " + dateiname + ", Abbruch");
			return null;
		}

		String dateiextension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
		if(dateiextension.isEmpty())
			return null;
	    if(!dateiextension.toLowerCase().equals("jpg")) {
	    	NVBWLogger.info("in getBildEXIFItem, unerwartete Dateiextension aufgetreten, Datei wird ignoriert, "
	    		+ file.getName() + ", Dateiextension: " + dateiextension);
	    	return null;
	    }
    	ImageMetadata metadata = null;
	    // get all metadata stored in EXIF format (ie. from JPEG or TIFF).
	    try {
	    	metadata = Imaging.getMetadata(file);
	    } catch (Error e) {
	    	NVBWLogger.severe("in getEXIFGPSKoordinaten, Exception aufgetreten, ABBRUCH, Details folgen ...");
	    	NVBWLogger.severe(e.toString());
	    	return null;
	    }
	    // System.out.println(metadata);
	
	    if (metadata instanceof JpegImageMetadata) {
	        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
	
	        // Jpeg EXIF metadata is stored in a TIFF-based directory structure
	        // and is identified with TIFF tags.
	        // Here we look for the "x resolution" tag, but
	        // we could just as easily search for any other tag.
	        //
	        // see the TiffConstants file for a list of TIFF tags.
		
	        // simple interface to GPS data
	        final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
	        if (null != exifMetadata) {
	            final TiffImageMetadata.GpsInfo gpsInfo = exifMetadata.getGpsInfo();
	            if (null != gpsInfo) {
	                double longitude = gpsInfo.getLongitudeAsDegreesEast();
	                double latitude = gpsInfo.getLatitudeAsDegreesNorth();

	                // Genauigkeit auf 7 Stellen nach dem Komma reduzieren
	                longitude = Math.round(longitude * 10000000) / 10000000.0;
	                latitude = Math.round(latitude * 10000000) / 10000000.0;
	                
	                returnArray.add(longitude);
	                returnArray.add(latitude);
	            }
	        }
	    }
	    return returnArray;
	}
	

		/**
		 * am 05.02.2025 von BFRK-Repo Klasse EYEvisBilderMetadaten2BFRKDB Klasse geholt
		 * @param
		 * @return
		 * @throws ImagingException
		 * @throws IOException
		 */
	public static String getBildEXIFItem(String dateiname, String tag) throws ImagingException,
    IOException {
		
		File file = new File(dateiname);
		if(!file.exists()) {
	    	NVBWLogger.warning("in getBildEXIFItem, Datei existiert nicht: " + dateiname + ", Abbruch");
			return null;
		}

		String dateiextension = file.getName().substring(file.getName().lastIndexOf(".") + 1);
		if(dateiextension.isEmpty())
			return null;
	    if(!dateiextension.toLowerCase().equals("jpg")) {
	    	NVBWLogger.info("in getBildEXIFItem, unerwartete Dateiextension aufgetreten, Datei wird ignoriert, "
	    		+ file.getName() + ", Dateiextension: " + dateiextension);
	    	return "";
	    }
    	ImageMetadata metadata = null;
	    // get all metadata stored in EXIF format (ie. from JPEG or TIFF).
	    try {
	    	metadata = Imaging.getMetadata(file);
	    } catch (Error e) {
	    	NVBWLogger.severe("in getBildEXIFItem, Exception aufgetreten, ABBRUCH, Details folgen ...");
	    	NVBWLogger.severe(e.toString());
	    	return "";
	    }
	
	    if (metadata instanceof JpegImageMetadata) {
	        final JpegImageMetadata jpegMetadata = (JpegImageMetadata) metadata;
	
	        // Jpeg EXIF metadata is stored in a TIFF-based directory structure
	        // and is identified with TIFF tags.
	        // Here we look for the "x resolution" tag, but
	        // we could just as easily search for any other tag.
	        //
	        // see the TiffConstants file for a list of TIFF tags.
		
	        // simple interface to GPS data
	        final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
	        if (null != exifMetadata) {
	            final List<ImageMetadata.ImageMetadataItem> itemsList = (List<ImageMetadataItem>) exifMetadata.getItems();
	            for(int itemindex = 0; itemindex < itemsList.size(); itemindex++) {
	            	String aktitem = itemsList.get(itemindex).toString();
	            	//System.out.println("Item # " + itemindex + " ===" + aktitem + "===");
	            	if(aktitem.startsWith(tag + ":")) {
	            		int valuestartpos = aktitem.indexOf("'");
	            		String value = aktitem.substring(valuestartpos + 1, aktitem.indexOf("'",valuestartpos + 1));
	            			return value;
	            	}
	            }
	        }
	    }
	    return null;
	}


	public static void bilderLoeschen(long objektid) {
		String deleteMerkmalSql = "DELETE FROM merkmal WHERE objekt_id = ? AND name LIKE '%Foto';";

		PreparedStatement deleteMerkmalStmt = null;
		try {
			deleteMerkmalStmt = bfrkConn.prepareStatement(deleteMerkmalSql);

			int stmtindex = 1;
			deleteMerkmalStmt.setLong(stmtindex++, objektid);
			NVBWLogger.fine("SQL-delete Statement zum löschen Bilder zu einem Objekt '"
				+  deleteMerkmalStmt.toString() + "'");
	
			deleteMerkmalStmt.execute();
			NVBWLogger.info("Löschen aller Bilder zu einem Objekt mit objekt-id: " + objektid);
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Delete Fehler, als alle Bilder zu einem Objekt gelöscht werden sollten. " 
				+ "Statement war '" + deleteMerkmalStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString() + ", Code: " + e1.getSQLState());
		}
	}


	public static void addstore(long dbrecordid, String feldname, String feldwert, Datentyp datentyp) {
		NVBWLogger.info("addstore" + "\t" + feldname + "\t" 
				+ feldwert + "\t" + datentyp);

		String selectMerkmalSql = "select id, wert FROM merkmal WHERE "
				+ "objekt_id = ? AND name = ? AND typ = ?;";

		PreparedStatement selectMerkmalStmt = null;
		long vorhandeneMerkmalDBId = 0;
		String vorhandenerWert = "";
		try {
			selectMerkmalStmt = bfrkConn.prepareStatement(selectMerkmalSql);

			int stmtindex = 1;
			selectMerkmalStmt.setLong(stmtindex++, dbrecordid);
			selectMerkmalStmt.setString(stmtindex++, feldname);
			selectMerkmalStmt.setString(stmtindex++, datentyp.name());
			NVBWLogger.fine("SQL-select Statement zum vorab holen Merkmal '"
				+  selectMerkmalStmt.toString() + "'");
	
			ResultSet selectMerkmalRs = selectMerkmalStmt.executeQuery();
			if (selectMerkmalRs.next()) {
				vorhandeneMerkmalDBId = selectMerkmalRs.getLong("id");
				vorhandenerWert = selectMerkmalRs.getString("wert");
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Select Fehler, als ein Merkmal vorab gelesen werden sollte." 
				+ "Statement war '" + selectMerkmalStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString() + ", Code: " + e1.getSQLState());
		}

		if(vorhandeneMerkmalDBId == 0) {
			NVBWLogger.severe("Fehler bei addstore. Es sollte ein vorhandenes Merkmal "
				+ "um einen weiteren Wert ergänzt werden, aber das Merkmal wurde nicht gefunden. "
				+ "Die DB-Anfrage dazu war ===" + selectMerkmalStmt.toString() + "===");
			return;
		}
		List<String> dublettenListe = new ArrayList<>();
		StringBuffer neuerwertBuffer = new StringBuffer();
		if(		(vorhandenerWert != null) 
			&&	!vorhandenerWert.isEmpty()) {
			if(vorhandenerWert.indexOf("|") == -1) {
				neuerwertBuffer.append(vorhandenerWert);
				if(!vorhandenerWert.equals(feldwert)) {
					if(neuerwertBuffer.length() > 0)
						neuerwertBuffer.append("|");
					neuerwertBuffer.append(feldwert);
				} else {
					NVBWLogger.info("neuer Feldwert wird nicht ergänzt, weil schon als Wert vorhanden gewesen ===" + feldwert + "===");
				}
			} else {
				String werte[] = vorhandenerWert.split("\\|", -1);
				for(int wertindex = 0; wertindex < werte.length; wertindex++) {
					String aktwert = werte[wertindex];
					if(dublettenListe.contains(aktwert)) {
						NVBWLogger.info("Bei Merkmal wird vorhandene Dublette gefiltert ===" + aktwert + "===");
						continue;
					}
					if(neuerwertBuffer.length() > 0)
						neuerwertBuffer.append("|");
					neuerwertBuffer.append(aktwert);
					dublettenListe.add(aktwert);
				}
				if(!dublettenListe.contains(feldwert)) {
					if(neuerwertBuffer.length() > 0)
						neuerwertBuffer.append("|");
					neuerwertBuffer.append(feldwert);
				} else {
					NVBWLogger.info("neuer Feldwert wird nicht ergänzt, weil schon als Wert vorhanden gewesen ===" + feldwert + "===");
				}
			}
		}
		
		String updateMerkmalSql = "UPDATE merkmal SET wert = ?, "
			+ "zeitstempel = NOW() "
			+ "WHERE id = ? RETURNING id";

		PreparedStatement updateMerkmalStmt = null;
		try {
			updateMerkmalStmt = bfrkConn.prepareStatement(updateMerkmalSql);

			int stmtindex = 1;
			updateMerkmalStmt.setString(stmtindex++, neuerwertBuffer.toString());
			updateMerkmalStmt.setLong(stmtindex++, vorhandeneMerkmalDBId);
			NVBWLogger.fine("SQL-update Statement zum speichern Merkmal '"
				+  updateMerkmalStmt.toString() + "'");
	
			//updateMerkmalStmt.executeUpdate();
			long dbid = 0;
			ResultSet updateMerkmalRs = updateMerkmalStmt.executeQuery();
			if (updateMerkmalRs.next()) {
				dbid = updateMerkmalRs.getLong("id");
			}
			NVBWLogger.info("Erweiterung erfolgt: Merkmal-ID: " + dbid + ""
				+ ",  Objekt-ID: " + dbrecordid + ",  [" + feldname + "] ===" + feldwert + "===");
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Update Fehler, als ein Merkmal in der Tabelle aktualisiert werden sollte." 
				+ "Statement war '" + updateMerkmalStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString() + ", Code: " + e1.getSQLState());
		}
	}


	public static void store(long dbrecordid, String feldname, String feldwert, Datentyp datentyp) {
		//NVBWLogger.info("store" + "\t" + feldname + "\t" 
		//		+ feldwert + "\t" + datentyp);
		
		String insertMerkmalSql = "INSERT INTO merkmal (objekt_id, name, wert, typ) VALUES (?, ?, ?, ?) RETURNING id;";

		PreparedStatement insertMerkmalStmt = null;
		try {
			insertMerkmalStmt = bfrkConn.prepareStatement(insertMerkmalSql);

			int stmtindex = 1;
			insertMerkmalStmt.setLong(stmtindex++, dbrecordid);
			insertMerkmalStmt.setString(stmtindex++, feldname);
			insertMerkmalStmt.setString(stmtindex++, feldwert);
			insertMerkmalStmt.setString(stmtindex++, datentyp.name());
			NVBWLogger.fine("SQL-insert Statement zum speichern Merkmal '"
				+  insertMerkmalStmt.toString() + "'");
	
			//insertMerkmalStmt.executeUpdate();
			long dbid = 0;
			ResultSet insertMerkmalRs = insertMerkmalStmt.executeQuery();
			if (insertMerkmalRs.next()) {
				dbid = insertMerkmalRs.getLong("id");
			}

			NVBWLogger.info("Speicherung erfolgt: Merkmal-ID: " + dbid + ""
				+ ",  Objekt-ID: " + dbrecordid + ",  [" + feldname + "] ===" + feldwert + "===");
		} catch (SQLException e1) {
				// am 13.09.2024 Reihenfolge geändert: vorher zuerst ob updateszulaessig
			if(e1.getSQLState().equals("23505") && feldname.endsWith("Foto")) {
				if(objektzustand == Objektzustand_Typ.vorhandenveraltet)
					update(dbrecordid, feldname, feldwert, datentyp);
				else
					addstore(dbrecordid, feldname, feldwert, datentyp);
			} else if(e1.getSQLState().equals("23505") && updateszulaessig) {
				NVBWLogger.info("in Methode store kam ein SQL -Unique Fehler, also Merkmal schon vorhanden, "
					+ "wird jetzt über Update aktualisiert, die Parameter waren: "
					+ "Objekt_id: " + dbrecordid + ", Feldname: " + feldname
					+ ", Feldwert: " + feldwert + ", Datentyp: " + datentyp.toString());
				update(dbrecordid, feldname, feldwert, datentyp);
			} else {
				NVBWLogger.severe("SQL-Insert Fehler, als ein Merkmal in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertMerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString() + ", Code: " + e1.getSQLState());
			}
		}
	}


	public static void store(long dbrecordid, BFRKFeld.Name datentyp, boolean booleanwert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.Boolean) {
			store(dbrecordid, datentyp.dbname(), "" + booleanwert, Datentyp.Boolean);
		} else {
			NVBWLogger.severe("Methode store für boolean Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + datentyp.name());
		}
	}


	public static void store(long dbrecordid, BFRKFeld.Name datentyp, String textwert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.String) {
			store(dbrecordid, datentyp.dbname(), "" + textwert, Datentyp.String);
		} else {
			NVBWLogger.severe("Methode store für String Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + datentyp.name());
		}
	}


	public static void store(long dbrecordid, BFRKFeld.Name datentyp, double gleitkommawert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.Numeric) {
			store(dbrecordid, datentyp.dbname(), "" + gleitkommawert, Datentyp.Numeric);
		} else {
			NVBWLogger.severe("Methode store für double Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + "Wert: ===" + gleitkommawert + "===, Datentyp: " + datentyp.name());
		}
	}


	public static void storeKorrektur(long objekt_id, String feldname, String feldwert, Datentyp datentyp) {
		NVBWLogger.info("storeKorrektur" + "\t" + feldname + "\t" 
				+ feldwert + "\t" + datentyp);
		
		String insertMerkmalSql = "INSERT INTO merkmalkorrektur (objekt_id, name, wert, typ) VALUES (?, ?, ?, ?);";

		PreparedStatement insertMerkmalStmt = null;
		try {
			insertMerkmalStmt = bfrkConn.prepareStatement(insertMerkmalSql);

			int stmtindex = 1;
			insertMerkmalStmt.setLong(stmtindex++, objekt_id);
			insertMerkmalStmt.setString(stmtindex++, feldname);
			insertMerkmalStmt.setString(stmtindex++, feldwert);
			insertMerkmalStmt.setString(stmtindex++, datentyp.name());
			NVBWLogger.fine("SQL-insert Statement zum speichern Merkmalkorrektur '"
				+  insertMerkmalStmt.toString() + "'");
	
			insertMerkmalStmt.executeUpdate();
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Insert Fehler, als ein Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
				+ "Statement war '" + insertMerkmalStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}

	
	public static void storeKorrektur(long objekt_id, BFRKFeld.Name datentyp, boolean booleanwert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.Boolean) {
			storeKorrektur(objekt_id, datentyp.dbname(), "" + booleanwert, Datentyp.Boolean);
		} else {
			NVBWLogger.severe("Methode store für boolean Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + datentyp.name());
		}
	}
	
	public static void storeKorrektur(long objekt_id, BFRKFeld.Name datentyp, String textwert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.String) {
			storeKorrektur(objekt_id, datentyp.dbname(), "" + textwert, Datentyp.String);
		} else {
			NVBWLogger.severe("Methode store für String Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + datentyp.name());
		}
	}

	public static void storeKorrektur(long objekt_id, BFRKFeld.Name datentyp, double gleitkommawert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.Numeric) {
			storeKorrektur(objekt_id, datentyp.dbname(), "" + gleitkommawert, Datentyp.Numeric);
		} else {
			NVBWLogger.severe("Methode store für double Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + "Wert: ===" + gleitkommawert + "===, Datentyp: " + datentyp.name());
		}
	}


	public static void deleteImportdatei(String kreisschluessel, String datenlieferant, String importdatei, String oevart) {

		String selectImportdateiSql = "SELECT id from importdatei WHERE "
				+ "oevart = ? AND datenlieferant = ? AND kreisschluessel = ? AND dateiname = ?;";

		PreparedStatement selectImportdateiStmt = null;
		try {
			selectImportdateiStmt = bfrkConn.prepareStatement(selectImportdateiSql);
			int stmtindex = 1;
			selectImportdateiStmt.setString(stmtindex++, oevart);
			selectImportdateiStmt.setString(stmtindex++, datenlieferant);
			selectImportdateiStmt.setString(stmtindex++, kreisschluessel);
			selectImportdateiStmt.setString(stmtindex++, importdatei);
			NVBWLogger.fine("SQL-select Statement zur Prüfung, ob Importdatei schon früher importiert wurde"
				+  "\t" + selectImportdateiStmt.toString());
	
			ResultSet selectImportdateiRs = selectImportdateiStmt.executeQuery();
			if (selectImportdateiRs.next()) {
				long vorhandeneImportdateiDBId = selectImportdateiRs.getLong("id");


				String deleteDatenSql = "DELETE FROM merkmal WHERE objekt_id IN"
					+ "(SELECT objekt.id FROM objekt JOIN importdatei ON importdatei_id = importdatei.id"
					+ " WHERE importdatei.id = ?);";
				PreparedStatement deleteDatenStmt = null;
				try {
					deleteDatenStmt = bfrkConn.prepareStatement(deleteDatenSql);
					stmtindex = 1;
					deleteDatenStmt.setLong(stmtindex++, vorhandeneImportdateiDBId);
					NVBWLogger.fine("SQL-Delete Statement objekt-Datensätze in Tabelle merkmal" + "\t"
						+  "\t" + deleteDatenStmt.toString());
					deleteDatenStmt.execute();
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Delete Statement für objekt-Datensätze in Tabelle merkmal" + "\t" 
						+ "Statement war '" + deleteDatenStmt.toString() 
						+ "' Details folgen ...\n" + e1.toString());
				}

				
				deleteDatenSql = "DELETE FROM merkmalkorrektur WHERE objekt_id IN"
					+ "(SELECT objekt.id FROM objekt JOIN importdatei ON importdatei_id = importdatei.id"
					+ " WHERE importdatei.id = ?);";
				deleteDatenStmt = null;
				try {
					deleteDatenStmt = bfrkConn.prepareStatement(deleteDatenSql);
					stmtindex = 1;
					deleteDatenStmt.setLong(stmtindex++, vorhandeneImportdateiDBId);
					NVBWLogger.fine("SQL-Delete Statement objekt-Datensätze in Tabelle merkmal" + "\t"
						+  "\t" + deleteDatenStmt.toString());
					deleteDatenStmt.execute();
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Delete Statement für objekt-Datensätze in Tabelle merkmal" + "\t" 
						+ "Statement war '" + deleteDatenStmt.toString() 
						+ "' Details folgen ...\n" + e1.toString());
				}


				deleteDatenSql = "DELETE FROM objekt WHERE id  IN (SELECT objekt.id FROM objekt "
					+ "JOIN importdatei ON importdatei_id = importdatei.id where importdatei.id = ?);";
				deleteDatenStmt = null;
				try {
					deleteDatenStmt = bfrkConn.prepareStatement(deleteDatenSql);
					stmtindex = 1;
					deleteDatenStmt.setLong(stmtindex++, vorhandeneImportdateiDBId);
					NVBWLogger.fine("SQL-Delete Statement Datensätze in Tabelle objekt" + "\t"
						+  "\t" + deleteDatenStmt.toString());
					deleteDatenStmt.execute();
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Delete Statement für Datensätze in Tabelle objekt" + "\t" 
						+ "Statement war '" + deleteDatenStmt.toString() 
						+ "' Details folgen ...\n" + e1.toString());
				}	

//TODO bei Mentz-Datenimport prüfen, ob mittlerweile der Dateiname mit gepeichert wird, damit die Notizen aus früheren sasimp Versionen bestehen bleiben
				deleteDatenSql = "DELETE FROM notizobjekt WHERE id  IN (SELECT notizobjekt.id FROM notizobjekt "
					+ "JOIN importdatei ON importdatei_id = importdatei.id where importdatei.id = ?);";
				deleteDatenStmt = null;
				try {
					deleteDatenStmt = bfrkConn.prepareStatement(deleteDatenSql);
					stmtindex = 1;
					deleteDatenStmt.setLong(stmtindex++, vorhandeneImportdateiDBId);
					NVBWLogger.fine("SQL-Delete Statement Datensätze in Tabelle notizobjekt" + "\t"
						+  "\t" + deleteDatenStmt.toString());
					deleteDatenStmt.execute();
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Delete Statement für Datensätze in Tabelle notizobjekt" + "\t" 
						+ "Statement war '" + deleteDatenStmt.toString() 
						+ "' Details folgen ...\n" + e1.toString());
				}	

				deleteDatenSql = "DELETE FROM objektkorrektur WHERE objekt_id  IN (SELECT objekt.id FROM objekt "
					+ "JOIN importdatei ON importdatei_id = importdatei.id where importdatei.id = ?);";
				deleteDatenStmt = null;
				try {
					deleteDatenStmt = bfrkConn.prepareStatement(deleteDatenSql);
					stmtindex = 1;
					deleteDatenStmt.setLong(stmtindex++, vorhandeneImportdateiDBId);
					NVBWLogger.fine("SQL-Delete Statement Datensätze in Tabelle objekt" + "\t"
						+  "\t" + deleteDatenStmt.toString());
					deleteDatenStmt.execute();
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Delete Statement für Datensätze in Tabelle objekt" + "\t" 
						+ "Statement war '" + deleteDatenStmt.toString() 
						+ "' Details folgen ...\n" + e1.toString());
				}	
					

				deleteDatenSql = "DELETE FROM importdatei WHERE id = ?;";
				deleteDatenStmt = null;
				try {
					deleteDatenStmt = bfrkConn.prepareStatement(deleteDatenSql);
					stmtindex = 1;
					deleteDatenStmt.setLong(stmtindex++, vorhandeneImportdateiDBId);
					NVBWLogger.fine("SQL-Delete Statement Datensatz in Tabelle importdatei" + "\t"
						+  "\t" + deleteDatenStmt.toString());
					deleteDatenStmt.execute();
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Delete Statement für Datensatz in Tabelle importdatei" + "\t" 
						+ "Statement war '" + deleteDatenStmt.toString() 
						+ "' Details folgen ...\n" + e1.toString());
				}	
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-select Statement zum ermitteln, ob Importdatei schon früher importiert wurde" 
				+ "\t" + "Statement war '" + selectImportdateiStmt.toString()
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}


	public static long insertObjekt(String kreisschluessel, Objektart objektart, String name, 
		String dhid, String steigHauptobjekt, String beschreibungHauptobjekt, String oevart, 
		long haltestelleDBId, long importdateiDBId, String gemeinde, String ortsteil, 
		double lon, double lat, Date erfassungsdatum) {

		String insertObjektSql = "INSERT INTO objekt (kreisschluessel, objektart, name, dhid, steig, "
			+ "beschreibung, oevart, parent_id, importdatei_id, gemeinde, ortsteil, erfassungsdatum, "
			+ "koordinate) "
			+ "VALUES (?, ?, ?, ?, ?,"
			+ " ?, ?, ?, ?, ?, ?, ?, ST_SetSrid(ST_MakePoint(?, ?), 4326)) RETURNING id;";

		PreparedStatement insertHauptobjektStmt = null;
		try {
			insertHauptobjektStmt = bfrkConn.prepareStatement(insertObjektSql);
			int stmtindex = 1;
			insertHauptobjektStmt.setString(stmtindex++, kreisschluessel);
			insertHauptobjektStmt.setString(stmtindex++, objektart.toString());
			insertHauptobjektStmt.setString(stmtindex++, name);
			insertHauptobjektStmt.setString(stmtindex++, dhid);
			insertHauptobjektStmt.setString(stmtindex++, steigHauptobjekt);
			insertHauptobjektStmt.setString(stmtindex++, beschreibungHauptobjekt);
			insertHauptobjektStmt.setString(stmtindex++, oevart);
			insertHauptobjektStmt.setLong(stmtindex++, haltestelleDBId);
			insertHauptobjektStmt.setLong(stmtindex++, importdateiDBId);
			insertHauptobjektStmt.setString(stmtindex++, gemeinde);
			insertHauptobjektStmt.setString(stmtindex++, ortsteil);
			if(erfassungsdatum != null)
				insertHauptobjektStmt.setDate(stmtindex++, new java.sql.Date(erfassungsdatum.getTime()));
			else
				insertHauptobjektStmt.setDate(stmtindex++, null);
			insertHauptobjektStmt.setDouble(stmtindex++, lon);
			insertHauptobjektStmt.setDouble(stmtindex++, lat);
			NVBWLogger.fine("SQL-insert Statement zu erzeugen Haltestellen Objekt '"
				+  insertHauptobjektStmt.toString() + "'");
	
			ResultSet insertHauptobjektRs = insertHauptobjektStmt.executeQuery();
			if (insertHauptobjektRs.next()) {
				long objektID = insertHauptobjektRs.getLong("id");
				if(objektart != Objektart.Notiz)
					ergaenzeObjektInfraIDtemp(normierteDHID(dhid), objektID, objektart);
				return objektID;
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Insert zum ergänzen Objekt (Art: " + objektart + " in Tabelle objekt" + "\t" 
				+ "Statement war '" + insertHauptobjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}


	public static long insertKorrekturobjekt(String kreisschluessel, Objektart objektart, String name, String dhid,
		String steigHauptobjekt, String beschreibungHauptobjekt, String oevart, long haltestelleDBId, long importdateiDBId, String gemeinde,
		String ortsteil, double lon, double lat,
		String korrekturosmid, Date osmImportdatum, String osmImportperson) {

		String insertObjektSql = "INSERT INTO objektkorrektur (kreisschluessel, objektart, name, dhid, steig,"
			+ " beschreibung, oevart, objekt_id, parent_id, importdatei_id, gemeinde, ortsteil, koordinate ) "
			+ "VALUES (?, ?, ?, ?, ?,"
			+ " ?, ?, ?, ?, ?, ?, ?, ST_SetSrid(ST_MakePoint(?, ?), 4326)) RETURNING id;";

		PreparedStatement insertHauptobjektStmt = null;
		try {
			insertHauptobjektStmt = bfrkConn.prepareStatement(insertObjektSql);
			int stmtindex = 1;
			insertHauptobjektStmt.setString(stmtindex++, kreisschluessel);
			insertHauptobjektStmt.setString(stmtindex++, objektart.toString());
			insertHauptobjektStmt.setString(stmtindex++, name);
			insertHauptobjektStmt.setString(stmtindex++, dhid);
			insertHauptobjektStmt.setString(stmtindex++, steigHauptobjekt);
			insertHauptobjektStmt.setString(stmtindex++, beschreibungHauptobjekt);
			insertHauptobjektStmt.setString(stmtindex++, oevart);
			insertHauptobjektStmt.setLong(stmtindex++, haltestelleDBId);
			insertHauptobjektStmt.setLong(stmtindex++, -1);	// Parent
			insertHauptobjektStmt.setLong(stmtindex++, importdateiDBId);
			insertHauptobjektStmt.setString(stmtindex++, gemeinde);
			insertHauptobjektStmt.setString(stmtindex++, ortsteil);
			insertHauptobjektStmt.setDouble(stmtindex++, lon);
			insertHauptobjektStmt.setDouble(stmtindex++, lat);

			NVBWLogger.fine("SQL-insert Statement zu erzeugen Haltestellen Objekt '"
				+  insertHauptobjektStmt.toString() + "'");
	
			ResultSet insertHauptobjektRs = insertHauptobjektStmt.executeQuery();
			if (insertHauptobjektRs.next()) {

				if((korrekturosmid != null) && !korrekturosmid.equals(""))
					ReaderBase.storeKorrektur(haltestelleDBId, BFRKFeld.Name.KorrekturOsmId, korrekturosmid);
				if(osmImportdatum != null)
					ReaderBase.storeKorrektur(haltestelleDBId, BFRKFeld.Name.KorrekturImportdatum, date_de_formatter.format(osmImportdatum));
				if((osmImportperson != null) && !osmImportperson.equals(""))
					ReaderBase.storeKorrektur(haltestelleDBId, BFRKFeld.Name.KorrekturImportperson, osmImportperson);

				return insertHauptobjektRs.getLong("id");
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Insert zum ergänzen Objekt (Art: " + objektart + " in Tabelle objekt" + "\t" 
				+ "Statement war '" + insertHauptobjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}


	public static long insertImportdatei(String kreisschluessel, String datenlieferant, String importdatei, String dateipfad, String oevart) {

		String insertImportdateiSql = "INSERT INTO importdatei "
				+ "(oevart, datenlieferant, kreisschluessel, dateiname, dateipfad, zeitstempel) "
				+ "VALUES (?, ?, ?, ?, ?, ?) RETURNING id;";

		Date jetzt = new Date();

		PreparedStatement insertImportdateiStmt = null;
		try {
			insertImportdateiStmt = bfrkConn.prepareStatement(insertImportdateiSql);
			int stmtindex = 1;
			insertImportdateiStmt.setString(stmtindex++, oevart);
			insertImportdateiStmt.setString(stmtindex++, datenlieferant);
			if(oevart.equals("O"))
				insertImportdateiStmt.setString(stmtindex++, kreisschluessel);
			else
				insertImportdateiStmt.setString(stmtindex++, "");
			insertImportdateiStmt.setString(stmtindex++, importdatei);
			insertImportdateiStmt.setString(stmtindex++, dateipfad);
			insertImportdateiStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(jetzt.getTime()));
			NVBWLogger.fine("SQL-insert Statement zu erzeugen Haltestellen Objekt '"
				+  insertImportdateiStmt.toString() + "'");

			ResultSet insertImportdateiRs = insertImportdateiStmt.executeQuery();
			if (insertImportdateiRs.next()) {
				return insertImportdateiRs.getLong("id");
			}
	
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Insert Fehler, als ein Haltstelle in die Tabelle eingetragen werden sollte." 
				+ "Statement war '" + insertImportdateiStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}

	public static void updateObjekt(long objektid, double lon, double lat, Date erfassdatum) {
			updateObjekt(objektid, null, null, null, null, null, lon, lat, erfassdatum);
	}
	
	public static void updateObjekt(long objektid, String steig, String name, String beschreibung,
			String gemeinde, String ortsteil, double lon, double lat, Date erfassdatum) {
		String updateObjektSql = "UPDATE objekt SET ";
			if((steig != null) && !steig.isEmpty())
				updateObjektSql += "steig = ?, ";
			if((name != null) && !name.isEmpty())
				updateObjektSql += "name = ?, ";
			if((beschreibung != null) && !beschreibung.isEmpty())
				updateObjektSql += "beschreibung = ?, ";
			if((gemeinde != null) && !gemeinde.isEmpty())
				updateObjektSql += "gemeinde = ?, ";
			if((ortsteil != null) && !ortsteil.isEmpty())
				updateObjektSql += "ortsteil = ?, ";
			updateObjektSql += "koordinate = ST_SetSrid(ST_MakePoint(?, ?), 4326), "
			+ "erfassungsdatum = ? WHERE id = ?;";

		PreparedStatement updateObjektStmt = null;
		try {
			updateObjektStmt = bfrkConn.prepareStatement(updateObjektSql);
			int stmtindex = 1;
			if((steig != null) && !steig.isEmpty())
				updateObjektStmt.setString(stmtindex++, steig);
			if((name != null) && !name.isEmpty())
				updateObjektStmt.setString(stmtindex++, name);
			if((beschreibung != null) && !beschreibung.isEmpty())
				updateObjektStmt.setString(stmtindex++, beschreibung);
			if((gemeinde != null) && !gemeinde.isEmpty())
				updateObjektStmt.setString(stmtindex++, gemeinde);
			if((ortsteil != null) && !ortsteil.isEmpty())
				updateObjektStmt.setString(stmtindex++, ortsteil);
			updateObjektStmt.setDouble(stmtindex++, lon);
			updateObjektStmt.setDouble(stmtindex++, lat);
			if(erfassdatum != null)
				updateObjektStmt.setDate(stmtindex++, new java.sql.Date(erfassdatum.getTime()));
			else
				updateObjektStmt.setDate(stmtindex++, null);
			updateObjektStmt.setLong(stmtindex++, objektid);
			NVBWLogger.fine("SQL-update Statement zur Aktualisierung Objekt '"
				+  updateObjektStmt.toString() + "'");

			updateObjektStmt.execute();
	
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Update Fehler, als ein Objekt aktualisiert werden sollte." 
				+ "Statement war '" + updateObjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}


	public static long storeDivaNote(long importdateiid, String kreisschluessel, Objektart objektart,
		String dhid, String titel, String text, double lon, double lat,
		String datenlieferant, String dateipfad) {

		String insertObjektSql = "INSERT INTO notizobjekt (kreisschluessel, objektart, dhid, "
			+ "titel, text, koordinate, quelle, datenlieferant, dateipfad, importdatei_id) "
			+ "VALUES (?, ?, ?, ?, ?, ST_SetSrid(ST_MakePoint(?, ?), 4326), "
			+ "?, ?, ?, ?) RETURNING id;";

		PreparedStatement insertNotizobjektStmt = null;
		try {
			insertNotizobjektStmt = bfrkConn.prepareStatement(insertObjektSql);
			int stmtindex = 1;
			insertNotizobjektStmt.setString(stmtindex++, kreisschluessel);
			insertNotizobjektStmt.setString(stmtindex++, objektart.toString());
			insertNotizobjektStmt.setString(stmtindex++, dhid);
			insertNotizobjektStmt.setString(stmtindex++, titel);
			insertNotizobjektStmt.setString(stmtindex++, text);
			insertNotizobjektStmt.setDouble(stmtindex++, lon);
			insertNotizobjektStmt.setDouble(stmtindex++, lat);
			insertNotizobjektStmt.setString(stmtindex++, "MentzErfassungsapp");
			insertNotizobjektStmt.setString(stmtindex++, datenlieferant);
			insertNotizobjektStmt.setString(stmtindex++, dateipfad);
			insertNotizobjektStmt.setLong(stmtindex++, importdateiid);
			

			NVBWLogger.fine("SQL-insert Statement zu erzeugen Diva Notizobjekt '"
				+  insertNotizobjektStmt.toString() + "'");
	
			ResultSet insertNotizobjektRs = insertNotizobjektStmt.executeQuery();
			if (insertNotizobjektRs.next()) {
				return insertNotizobjektRs.getLong("id");
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Insert zum ergänzen Diva Notizobjekt (Art: " + objektart + " in Tabelle notizobjekt" + "\t" 
				+ "Statement war '" + insertNotizobjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}

	
	public static long storeEYEvisNote(long importdateiid, String kreisschluessel, Objektart objektart,
		String dhid, String titel, String text, 
		String datenlieferant, String dateipfad) {

		String insertObjektSql = "INSERT INTO notizobjekt (importdatei_id, kreisschluessel, objektart, dhid, "
			+ "titel, text, quelle, datenlieferant, dateipfad) "
			+ "VALUES (?, ?, ?, ?, ?, ?, "
			+ "?, ?, ?) RETURNING id;";

		PreparedStatement insertNotizobjektStmt = null;
		try {
			insertNotizobjektStmt = bfrkConn.prepareStatement(insertObjektSql);
			int stmtindex = 1;
			insertNotizobjektStmt.setLong(stmtindex++, importdateiid);
			insertNotizobjektStmt.setString(stmtindex++, kreisschluessel);
			insertNotizobjektStmt.setString(stmtindex++, objektart.toString());
			insertNotizobjektStmt.setString(stmtindex++, dhid);
			insertNotizobjektStmt.setString(stmtindex++, titel);
			insertNotizobjektStmt.setString(stmtindex++, text);
			insertNotizobjektStmt.setString(stmtindex++, "EYEvis-App");
			insertNotizobjektStmt.setString(stmtindex++, datenlieferant);
			insertNotizobjektStmt.setString(stmtindex++, dateipfad);
			

			NVBWLogger.fine("SQL-insert Statement zu erzeugen EYEvis Notizobjekt '"
				+  insertNotizobjektStmt.toString() + "'");
	
			ResultSet insertNotizobjektRs = insertNotizobjektStmt.executeQuery();
			if (insertNotizobjektRs.next()) {
				return insertNotizobjektRs.getLong("id");
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Insert zum ergänzen EYEvis Notizobjekt (Art: " + objektart + " in Tabelle notizobjekt" + "\t" 
				+ "Statement war '" + insertNotizobjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}


	/**
	 * 
	 * @param kreisschluessel
	 * @param datenlieferant
	 * @param oevart
	 * @param dhid
	 * @return positive Id, wenn genau ein Importdatei-Datensatz gefunden wurde.<br>
	 * 			0, wenn kein Datensatz gefunden wurde<br>
	 * 			negative Id, wenn mehr als ein Datensatz gefunden wurde.
	 * 			Es wird dann ein beliebiger Treffer mit dem negativen Wert der Datensatz-ID zurückgegeben.
	 */
	public static long getImportdatei(String kreisschluessel, String datenlieferant, 
		String oevart, String dhid, String importdateiname) {

		String selectImportdateiSql = "SELECT imp.id FROM importdatei AS imp "
				+ "JOIN objekt AS o ON imp.id = o.importdatei_id "
				+ "WHERE imp.oevart = ? AND datenlieferant = ? and o.kreisschluessel = ? "
				+ "AND o.dhid = ? AND o.objektart in ('Bahnhof', 'Haltestelle') "
				+ "AND imp.dateiname = ?;";	// am 04.02.2025 ergänzt, weil sonst der alte Dateiname auf Dauer verwendet wird

		Date jetzt = new Date();

		PreparedStatement selectImportdateiStmt = null;
		try {
			selectImportdateiStmt = bfrkConn.prepareStatement(selectImportdateiSql);
			int stmtindex = 1;
			selectImportdateiStmt.setString(stmtindex++, oevart);
			selectImportdateiStmt.setString(stmtindex++, datenlieferant);
			selectImportdateiStmt.setString(stmtindex++, kreisschluessel);
			selectImportdateiStmt.setString(stmtindex++, dhid);
			selectImportdateiStmt.setString(stmtindex++, importdateiname);
			NVBWLogger.info("SQL-select Statement zu holen Importdatei DBid '"
				+  selectImportdateiStmt.toString() + "'");

			ResultSet selectImportdateiRs = selectImportdateiStmt.executeQuery();
			int anzahltreffer = 0;
			long gefdbid = 0;
			while (selectImportdateiRs.next()) {
				anzahltreffer++;
				gefdbid = selectImportdateiRs.getLong("id");
			}

			if(anzahltreffer == 1)
				return gefdbid;
			else if(anzahltreffer == 0)
				return 0;
			else {
				NVBWLogger.severe("es konnte keine eindeutige Importdatei-DBid ermittelt werden. Anzahl Treffer: " + anzahltreffer
					+ ", das SQL-Statement war " + selectImportdateiStmt.toString());
				return -1 * gefdbid;
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Select Fehler, als ein DBid aus der Tabelle Importdatei geholt werden sollte." 
				+ "Statement war '" + selectImportdateiStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}


	public static long getImportdateiByDateiname(String kreisschluessel, String datenlieferant, 
			String oevart, String dateiname) {

			String selectImportdateiSql = "SELECT imp.id FROM importdatei AS imp "
					+ "WHERE imp.oevart = ? AND datenlieferant = ? and kreisschluessel = ? "
					+ "AND dateiname = ?;";

			Date jetzt = new Date();

			PreparedStatement selectImportdateiStmt = null;
			try {
				selectImportdateiStmt = bfrkConn.prepareStatement(selectImportdateiSql);
				int stmtindex = 1;
				selectImportdateiStmt.setString(stmtindex++, oevart);
				selectImportdateiStmt.setString(stmtindex++, datenlieferant);
				if(oevart.equals("O"))
					selectImportdateiStmt.setString(stmtindex++, kreisschluessel);
				else
					selectImportdateiStmt.setString(stmtindex++, "");
				selectImportdateiStmt.setString(stmtindex++, dateiname);
				NVBWLogger.info("SQL-select Statement zu holen Importdatei DBid '"
					+  selectImportdateiStmt.toString() + "'");

				ResultSet selectImportdateiRs = selectImportdateiStmt.executeQuery();
				int anzahltreffer = 0;
				long gefdbid = 0;
				while (selectImportdateiRs.next()) {
					anzahltreffer++;
					gefdbid = selectImportdateiRs.getLong("id");
				}

				if(anzahltreffer == 1)
					return gefdbid;
				else if(anzahltreffer == 0)
					return 0;
				else {
					NVBWLogger.severe("es konnte keine eindeutige Importdatei-DBid ermittelt werden. Anzahl Treffer: " + anzahltreffer
						+ ", das SQL-Statement war " + selectImportdateiStmt.toString());
					return -1 * gefdbid;
				}
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Select Fehler, als ein DBid aus der Tabelle Importdatei geholt werden sollte." 
					+ "Statement war '" + selectImportdateiStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
			}
			return -1;
		}


	public static long getObjekt(String kreisschluessel, Objektart objektart, String name, 
		String dhid, String steigHauptobjekt, String beschreibungHauptobjekt, 
		String oevart, long haltestelleDBId, long importdateiDBId, String infraid, String gemeinde,
		String ortsteil, double lon, double lat) {

		if(sollFelderkoennenbeliebigsein) {
			if(name.equals(""))
				name = "%";
			if(gemeinde.equals(""))
				gemeinde = "%";
			if(ortsteil.equals(""))
				ortsteil = "%";
		}

		String selectObjektSql = "SELECT id, name, gemeinde, ortsteil FROM objekt "
			+ "WHERE kreisschluessel = ? AND objektart = ? " // AND name like ? "
			+ "AND dhid = ? AND oevart = ? "
			+ "AND parent_id = ?";
//			+ "AND gemeinde like ? AND ortsteil like ?";
		if((sollFelderkoennenbeliebigsein) && (importdateiDBId != -1)) {
			// importdatei_id Feld nicht berücksichtigen
		} else
			selectObjektSql += " AND importdatei_id = ?";
		if((infraid != null) && !infraid.isEmpty())
			selectObjektSql += " AND infraidtemp = ?";
		selectObjektSql += ";";

		PreparedStatement selectHauptobjektStmt = null;
		try {
			selectHauptobjektStmt = bfrkConn.prepareStatement(selectObjektSql);
			int stmtindex = 1;
			selectHauptobjektStmt.setString(stmtindex++, kreisschluessel);
			selectHauptobjektStmt.setString(stmtindex++, objektart.toString());
//			selectHauptobjektStmt.setString(stmtindex++, name);
			selectHauptobjektStmt.setString(stmtindex++, dhid);
			// nicht steig berücksichtigen in Query
			selectHauptobjektStmt.setString(stmtindex++, oevart);
			selectHauptobjektStmt.setLong(stmtindex++, haltestelleDBId);
//			selectHauptobjektStmt.setString(stmtindex++, gemeinde);
//			selectHauptobjektStmt.setString(stmtindex++, ortsteil);
			if((sollFelderkoennenbeliebigsein) && (importdateiDBId != -1)) {
				// importdatei_id Feld nicht berücksichtigen
			} else
				selectHauptobjektStmt.setLong(stmtindex++, importdateiDBId);
			NVBWLogger.fine("SQL-select Statement zu holen Haltestellen Objekt '"
				+  selectHauptobjektStmt.toString() + "'");
			if((infraid != null) && !infraid.isEmpty())
				selectHauptobjektStmt.setString(stmtindex++, infraid);
	
			ResultSet selectHauptobjektRs = selectHauptobjektStmt.executeQuery();
			int anzahltreffer = 0;
			int anzahlexakttreffer = 0;
			long gefdbid = 0;
			String aktname = null;
			String aktgemeinde = null;
			String aktortsteil = null;
			while (selectHauptobjektRs.next()) {
				anzahltreffer++;
				gefdbid = selectHauptobjektRs.getLong("id");
				aktname = selectHauptobjektRs.getString("name");
				aktgemeinde = selectHauptobjektRs.getString("gemeinde");
				aktortsteil = selectHauptobjektRs.getString("ortsteil");
				if((aktname != null) && (aktortsteil != null)) { // && (aktgemeinde != null)
					if(		aktname.equals(name) 
//						&& 	aktgemeinde.equals(gemeinde)		deaktiviert, weil uralter EYEvis Exportfehler, das Gemeinde bei Excel-Export nicht gesetzt ist 
						&&	aktortsteil.equals(ortsteil)) {
						anzahlexakttreffer++;
					}
				}
			}

			if(anzahlexakttreffer == 1) {
				NVBWLogger.info("in Methode getObjekt ein exaktes Objekt gefunden, wird genommen, DB Tabelle Objekt, ID: " + gefdbid);
				return gefdbid;
			} else if(anzahltreffer == 1) {
				NVBWLogger.info("in Methode getObjekt zwar ein Objekt gefunden, aber nicht exakt, wird aber genommen. "
					+ "Sollfelder - Name. " + name + ", Gemeinde: " + gemeinde + ", Ortsteil: " + ortsteil + ", "
					+ "Istfelder - Name: " + aktname + ", Gemeinde: " + aktgemeinde + ", Ortsteil: " + aktortsteil);
				return gefdbid;
			} else if(anzahltreffer == 0) {
				NVBWLogger.info("in Methode getObjekt kein Objekt gefunden, deshalb wird jetzt insertObjekt aufgerufen ...");
				gefdbid = insertObjekt(kreisschluessel, objektart, name, dhid, steigHauptobjekt, 
					beschreibungHauptobjekt, oevart, haltestelleDBId, importdateiDBId, gemeinde,
					ortsteil, lon, lat, null);
				return gefdbid;
			} else {
				NVBWLogger.severe("es konnte keine eindeutige Objekt-DBid ermittelt werden. Anzahl Treffer: " 
					+ anzahltreffer + ", DB-Query war: " + selectHauptobjektStmt.toString());
				return -1;
			}
		
		
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Select zum holen der DB-id für Objekt (Art: " + objektart + " in Tabelle objekt" + "\t" 
				+ "Statement war '" + selectHauptobjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
		return -1;
	}


	public static void update(long dbrecordid, String feldname, String feldwert, Datentyp datentyp) {
		NVBWLogger.info("update " + "\t" + feldname + "\t" 
				+ feldwert + "\t" + datentyp);
		
		String updateMerkmalSql = "UPDATE merkmal SET wert = ?, "
			+ "zeitstempel = NOW() "
			+ "WHERE objekt_id = ? AND name = ? AND typ = ? RETURNING id";

		PreparedStatement updateMerkmalStmt = null;
		try {
			updateMerkmalStmt = bfrkConn.prepareStatement(updateMerkmalSql);

			int stmtindex = 1;
			updateMerkmalStmt.setString(stmtindex++, feldwert);
			updateMerkmalStmt.setLong(stmtindex++, dbrecordid);
			updateMerkmalStmt.setString(stmtindex++, feldname);
			updateMerkmalStmt.setString(stmtindex++, datentyp.name());
			NVBWLogger.fine("SQL-update Statement zum speichern Merkmal '"
				+  updateMerkmalStmt.toString() + "'");
	
			//updateMerkmalStmt.executeUpdate();
			long dbid = 0;
			ResultSet updateMerkmalRs = updateMerkmalStmt.executeQuery();
			if (updateMerkmalRs.next()) {
				dbid = updateMerkmalRs.getLong("id");
			}
			NVBWLogger.info("Update erfolgt: Merkmal-ID: " + dbid + ""
				+ ",  Objekt-ID: " + dbrecordid + ",  [" + feldname + "] ===" + feldwert + "===");
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Update Fehler, als ein Merkmal in der Tabelle upgedated werden sollte." 
				+ "Statement war '" + updateMerkmalStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}


	public static void update(long dbrecordid, BFRKFeld.Name datentyp, double gleitkommawert) {
		if(datentyp.typ() == BFRKFeld.Datentyp.Numeric) {
			update(dbrecordid, datentyp.dbname(), "" + gleitkommawert, Datentyp.Numeric);
		} else {
			NVBWLogger.severe("Methode update für double Art wurde aufgerufen, aber Typ ist falsch" 
				+ "\t" + datentyp.typ() + "\t" + "Wert: ===" + gleitkommawert + "===, Datentyp: " + datentyp.name());
		}
	}


	public static void deleteMerkmale(long dbobjektid) {

		String deleteMerkmaleSql = "DELETE FROM merkmal WHERE objekt_id = ?;";

		PreparedStatement deleteMerkmaleStmt = null;
		try {
			deleteMerkmaleStmt = bfrkConn.prepareStatement(deleteMerkmaleSql);

			int stmtindex = 1;
			deleteMerkmaleStmt.setLong(stmtindex++, dbobjektid);
			NVBWLogger.info("SQL-delete Statement zum löschen aller Merkmale zu einem Objekt '"
				+  deleteMerkmaleStmt.toString() + "'");
	
			int anzahldatensaetze = deleteMerkmaleStmt.executeUpdate();
			NVBWLogger.info("Löschen der Merkmale erfolgt: Objekt-ID: " + dbobjektid + ",  Anzahl: " + anzahldatensaetze);
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-delete Fehler, als alle Merkmale zu einem Objekt in der Tabelle gelöscht werden sollten. " 
				+ "Statement war '" + deleteMerkmaleStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}
			
	public static void deleteObjekt(long dbobjektid) {

//TODO noch in diversen, abhängigen, Tabellen ggfs. einen Datensatz löschen, z.B. osmobjektbezug, aufzugfastaosmbezug, ... 

		deleteMerkmale(dbobjektid);

		
		String deleteObjektSql = "DELETE FROM objekt WHERE id = ?;";

		PreparedStatement deleteObjektStmt = null;
		try {
			deleteObjektStmt = bfrkConn.prepareStatement(deleteObjektSql);

			int stmtindex = 1;
			deleteObjektStmt.setLong(stmtindex++, dbobjektid);
			NVBWLogger.info("SQL-delete Statement zum löschen eines Objekts '"
				+  deleteObjektStmt.toString() + "'");
	
			int anzahldatensaetze = deleteObjektStmt.executeUpdate();
			NVBWLogger.info("Löschen des Objekts erfolgt: ID: " + dbobjektid + ",  Anzahl: " + anzahldatensaetze);
		} catch (SQLException e1) {
			NVBWLogger.severe("SQL-Delete Fehler, als ein Objekt in der Tabelle gelöscht werden sollte. " 
				+ "Statement war '" + deleteObjektStmt.toString() 
				+ "' Details folgen ...");
			NVBWLogger.severe(e1.toString());
		}
	}

}
