package de.nvbw.bfrk;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFHyperlink;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
//import org.onebusaway.csv_entities.schema.DefaultEntitySchemaFactory;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.base.Excelfeld;
import de.nvbw.bfrk.base.BFRKFeld.Datentyp;
import de.nvbw.bfrk.base.BFRKFeld.Name;
import de.nvbw.bfrk.util.ExportNachEYEvisObjektpruefung;
import de.nvbw.bfrk.util.ExportNachEYEvisObjektpruefung.Bildquellenart;
import de.nvbw.bfrk.util.ReaderBase.Objektart;
import de.nvbw.base.Applicationconfiguration;

import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;

/**
 * Erstellung von EYEvis Projektdatei im csv-Format und ggfs. Bereitstellung 
 * schon früher erfasste BFRK-Daten zur vor Ort Prüfung und Anpassung in Form einer zip
 * @author SEI
 *
 */
public class ExportNachEYEvis {
	private static final int EYEvisNameMaxlength = 50;
	private static final int EYEvisBeschreibungMaxlength = 50;
	private static final int EYEvisObjektidMaxlength = 20;
	
	public static enum ERHEBUNGSART {EYEvisApp, MentzApp, CSVImport, Ungesetzt};

	static Applicationconfiguration configuration = new Applicationconfiguration();
	static Connection bfrkConn = null;

	static DateFormat datetime_de_formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private static Workbook workbook = null;
	private static Map<String, Sheet> blattMap = new HashMap<>();
	private static Map<String, Integer> naechsteZeilennrMap = new HashMap<>();

	private static void writetoFile(StringBuffer outputbuffer, String dateiname, Charset charset) {
		
		PrintWriter csvOutput = null;

		try {
			csvOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(dateiname),charset)));
			csvOutput.print(outputbuffer.toString());
			csvOutput.close();
		} catch (IOException ioe) {
			NVBWLogger.severe("Fehler bei Ausgabe in Datei " + dateiname);
		}
	}

	private static void initExceldatei() {
	    //Blank workbook
	    workbook = new XSSFWorkbook(); 
	    blattMap = new HashMap<>();
	}

	private static boolean openExceldatei(String dateipfadundname) {
		InputStream inp = null;
		try {
			inp = new FileInputStream(dateipfadundname);
			workbook = WorkbookFactory.create(inp);
		} catch (EncryptedDocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

		int anzahlkorrekteStrukturen = 0;
		for(int sheetindex = 0; sheetindex < workbook.getNumberOfSheets(); sheetindex++) {
			Sheet aktsheet = workbook.getSheetAt(sheetindex);
			String aktsheetname = aktsheet.getSheetName();
			blattMap.put(aktsheetname, aktsheet);
			System.out.println("Sheet # " + sheetindex + "'" + aktsheet.getSheetName() + "'");

			int letzteRowNr = aktsheet.getLastRowNum();
			if(aktsheetname.equals("Erfassung")) {
				Row row = aktsheet.getRow(0);
				Cell aktivezelle0 = row.getCell(0);
				Cell aktivezelle1 = row.getCell(1);
				Cell aktivezelle4 = row.getCell(4);
				Cell aktivezelle6 = row.getCell(6);
				Cell aktivezelle8 = row.getCell(8);
				Cell aktivezelle9 = row.getCell(9);
				if(		(aktivezelle0.getStringCellValue().equals("DHID"))
					&& 	(aktivezelle6.getStringCellValue().equals("lat"))
					&& 	(aktivezelle8.getStringCellValue().equals("Objektart"))
					&&	(aktivezelle9.getStringCellValue().equals("BSTG_Foto"))) {
					anzahlkorrekteStrukturen++;
				} else if(	(aktivezelle1.getStringCellValue().equals("DHID"))
						&& 	(aktivezelle4.getStringCellValue().equals("lat"))
						&& 	(aktivezelle8.getStringCellValue().equals("Objektart"))
						&&	(aktivezelle9.getStringCellValue().equals("HST_Fahrplananzeigetafel_jn"))) {
					anzahlkorrekteStrukturen++;
				}
			} else {
				NVBWLogger.severe("Das notwendige Blatt 'Erfassung' wurde nicht gefunden.");
				NVBWLogger.warning("unerwarteter Blattname '" + aktsheetname + "', bitte prüfen, ob ok");
				return false;
			}
		}
		if(anzahlkorrekteStrukturen < 1) {
			NVBWLogger.severe("Das notwendige Blatt 'Erfassung' hat nicht die richtige Struktur");
			return false;
		}

		return true;
	}


	private static void closeExceldatei(String dateipfadundname, boolean speichern) {
		if(!speichern) {
            try {
            	if(workbook != null)
            		workbook.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            workbook = null;
            blattMap = null;
			return;
		}

		try
        {
            //Write the workbook in file system
            FileOutputStream out = new FileOutputStream(new File(dateipfadundname));
            workbook.write(out);
            out.close();
            System.out.println("Exceldatei " + dateipfadundname + " erfolgreich gepeichert");
            workbook.close();
            workbook = null;
        } 
        catch (Exception e) 
        {
            e.printStackTrace();
        }
		
	}


	/**
	 * Schreibe die Daten in daten in die Zeile zeilenr (mit 0 beginnend)
	 * @param blatt
	 * @param zeilenr
	 * @param daten
	 */
	private static void schreibeExcelZeile(String blatt, int zeilenr, Map<String, Excelfeld> daten) {
        Sheet sheet = null;
		if(blattMap.containsKey(blatt))
			sheet = blattMap.get(blatt);
		else {
			NVBWLogger.severe("Blatt existiert nicht mit Namen '" + blatt + "', daher wird das Blatt nicht ausgefüllt");
			return;
		}

		Row ueberschriftzeile = sheet.getRow(0);
		if(ueberschriftzeile == null) {
			NVBWLogger.severe("In Excelvorlage fehlt in Zeile 1 die Überschriftszeile, ABBRUCH");
			return;
		}

		Row zielzeile = sheet.getRow(zeilenr);
		if(zielzeile == null)
			zielzeile = sheet.createRow(zeilenr);

		int startspalte = ueberschriftzeile.getFirstCellNum();
		int endespalte = ueberschriftzeile.getLastCellNum();
		for(int spaltenindex = startspalte; spaltenindex <= endespalte; spaltenindex++) {

			Cell ueberschriftzelle = ueberschriftzeile.getCell(spaltenindex);
			if(ueberschriftzelle == null)
				continue;

			Cell zielzelle = zielzeile.getCell(spaltenindex);
			if(zielzelle == null)
				zielzelle = zielzeile.createCell(spaltenindex);
			String ueberschriftzelleninhalt = ueberschriftzelle.getStringCellValue();
			System.out.println(ueberschriftzelleninhalt);

			if(daten.containsKey(ueberschriftzelleninhalt)) {
				Excelfeld aktfeld = daten.get(ueberschriftzelleninhalt);

				if(ueberschriftzelleninhalt.endsWith("_Foto")) {
					// wenn mehrere Fotos vorhanden sind, nur das erste nehmen
					if(!aktfeld.getTextWert().isEmpty() && aktfeld.getTextWert().indexOf("|") != -1)
						aktfeld.setInhalt(aktfeld.getTextWert().substring(0, aktfeld.getTextWert().indexOf("|")));
				}

				CreationHelper createHelper = workbook.getCreationHelper();
				CellStyle style = null;
				DataFormat format = workbook.createDataFormat();
	        	if(aktfeld.getStyle() != null)
	        		style = aktfeld.getStyle();
	        	else
	        		style = workbook.createCellStyle();
	        	if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Text) {
	        		zielzelle.setCellValue(aktfeld.getTextWert());
	        		if(style != null)
	                    zielzelle.setCellStyle(style);
	        	} else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Standard) {
		        	zielzelle.setCellValue(aktfeld.getTextWert());
		        	if(style != null)
		        		zielzelle.setCellStyle(style);
	        	} else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Zahl_Integer) {
	                zielzelle.setCellValue(aktfeld.getZahlWert());
	                style.setDataFormat(format.getFormat("#0"));
	                zielzelle.setCellStyle(style);
	        	} else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Zahl_Nachkomma1) {
	                zielzelle.setCellValue(aktfeld.getZahlWert());
	                style.setDataFormat(format.getFormat("#0.0"));
	                zielzelle.setCellStyle(style);
	        	} else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Zahl_Nachkomma2) {
	                zielzelle.setCellValue(aktfeld.getZahlWert());
	                style.setDataFormat(format.getFormat("#0.00"));
	                zielzelle.setCellStyle(style);
	        	} else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Zahl) {
	                zielzelle.setCellValue(aktfeld.getZahlWert());
	            } else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Hyperlink_Bild) {
	            	zielzelle.setCellValue("Bild ↗");
	            	CellStyle hlinkstyle = workbook.createCellStyle();
					Font hlinkfont = workbook.createFont();
					hlinkfont.setUnderline(XSSFFont.U_SINGLE);
					hlinkfont.setColor(IndexedColors.BLUE.index);
					hlinkstyle.setFont(hlinkfont);
	            	XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(HyperlinkType.URL);
	                link.setAddress(aktfeld.getHyperlink());
	                zielzelle.setHyperlink((XSSFHyperlink) link);
	                zielzelle.setCellStyle(hlinkstyle);            	
	            } else if(aktfeld.getFeldtyp() == Excelfeld.Datentyp.Hyperlink_Karte) {
	            	zielzelle.setCellValue("Karte ↗");
	            	CellStyle hlinkstyle = workbook.createCellStyle();
					Font hlinkfont = workbook.createFont();
					hlinkfont.setUnderline(XSSFFont.U_SINGLE);
					hlinkfont.setColor(IndexedColors.BLUE.index);
					hlinkstyle.setFont(hlinkfont);
	            	XSSFHyperlink link = (XSSFHyperlink)createHelper.createHyperlink(HyperlinkType.URL);
	                link.setAddress(aktfeld.getHyperlink());
	                zielzelle.setHyperlink((XSSFHyperlink) link);
	                zielzelle.setCellStyle(hlinkstyle);            	
	        	} else {
	        		zielzelle.setCellValue(aktfeld.getTextWert());
	        		NVBWLogger.warning("in Klasse ExportFuerImport, Methode schreibeExcelSpalte unerwarteter Enum-Wert in ExcelFeld ===" + aktfeld.getFeldtyp().name() + "===");
	        	}
			} else {
				NVBWLogger.warning("Blatt " + blatt + ", Zelle " + zielzelle.getAddress().toString()
					+ ": hierfür gibt es keinen Zellinhalt");
			}
		}
	}


	/**
	 * Schreibe die Daten in daten in die Spalte spaltenr (mit 0 beginnend)
	 * @param blatt
	 * @param spaltenr
	 * @param daten
	 */

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


	private static Map<Long, Map<Name, BFRKFeld>> holHaltestelle(String paramDhid, String oevart, String paramStatus) {
		Map<Long, Map<Name, BFRKFeld>> hstdatensaetze = new HashMap<>();
		List<String> dhidListe = new ArrayList<>();

		String haltestellenartBahnhof = "Bahnhof";
		String haltestellenartHaltestelle = "Haltestelle";
			
		if(oevart.equals("S")) {
			haltestellenartBahnhof = "Bahnhof";
			haltestellenartHaltestelle = "Bahnhof";
		} else if(oevart.equals("O")) {
			haltestellenartBahnhof = "Haltestelle";
			haltestellenartHaltestelle = "Haltestelle";
		}

		String haltestelleSelectSql = "SELECT objekt.id, objekt.dhid, objekt.objektart, objekt.steig, objekt.name, "
			+ "ST_X(koordinate) AS lon, ST_Y(koordinate) AS lat, gemeinde, ortsteil, "
			+ "datenlieferant, dateipfad, dateiname, objektworkflow.status, "
			+ "ds100, dbkategorie, "
			+ "CASE WHEN bahnhofsliste.name != null THEN bahnhofsliste.name ELSE objekt.name END as bahnhofname, "
			+ "bfnr, eiu, bfmanagement, "
			+ "bahnsteigoberkante, offiziellebahnsteiglaenge "
			+ "FROM objekt JOIN importdatei ON objekt.importdatei_id = importdatei.id "
			+ "LEFT JOIN bahnhofsliste ON objekt.dhid = bahnhofsliste.dhid "
			+ "LEFT JOIN objektworkflow ON objekt.dhid = objektworkflow.dhid "
			+ "AND objekt.objektart = objektworkflow.objektart "
			+ "AND objekt.oevart = objektworkflow.oevart "
			+ "WHERE (objekt.objektart = ? OR objekt.objektart = ?) AND "
			+ "objekt.dhid like ? AND objekt.oevart like ? AND "
//			+ "(eyevismast = '0') "
			+ "(eyevismast = '0' OR eyevismast IS NULL) "
			+ "ORDER BY dhid;";

		try {
			Date startquery = new Date();
			PreparedStatement haltestelleSelectStmt = bfrkConn.prepareStatement(haltestelleSelectSql);

			int stmtindex = 1;
			haltestelleSelectStmt.setString(stmtindex++, haltestellenartBahnhof);
			haltestelleSelectStmt.setString(stmtindex++, haltestellenartHaltestelle);
			haltestelleSelectStmt.setString(stmtindex++, paramDhid);
			haltestelleSelectStmt.setString(stmtindex++, oevart);
			NVBWLogger.info("SQL-select Statement zum holen Haltestellen '"
				+  haltestelleSelectStmt.toString() + "'");
	

			ResultSet haltestelleSelectRs = haltestelleSelectStmt.executeQuery();
			int bruttoanzahl = 0;
NVBWLogger.info("in holhaltestellen for DB-Datesatzschleife ...");
			while( haltestelleSelectRs.next() ) {
				bruttoanzahl++;
				Map<Name, BFRKFeld> hstdatensatz = new HashMap<>();

				Long dbid = haltestelleSelectRs.getLong("id");
				String dhid = haltestelleSelectRs.getString("dhid");
				Objektart objektart = Objektart.valueOf(haltestelleSelectRs.getString("objektart"));
				String sollSteig = haltestelleSelectRs.getString("steig");
				String name = haltestelleSelectRs.getString("name");
				double lon = haltestelleSelectRs.getDouble("lon");
				double lat = haltestelleSelectRs.getDouble("lat");
				//TODO wenn lon/lat hier als Sollwerte fehlen, könnten diese alternativ über Mittelwerte über alle Bahnsteig-Istpositionen geholt werden. Beispiel: select avg(m_lon.wert::numeric), avg(m_lat.wert::numeric) from merkmal as m_lon join merkmal as m_lat on m_lon.objekt_id = m_lat.objekt_id where m_lon.name like 'STG_IstPos_Lon%' and m_lon.objekt_id in (select id from objekt where dhid like 'de:08125:1565%') and m_lat.name like 'STG_IstPos_Lat' and m_lat.objekt_id in (select id from objekt where dhid like 'de:08125:1565%') and m_lon.wert != '0.0' and m_lat.wert != '0.0';
				String gemeinde = haltestelleSelectRs.getString("gemeinde");
				String ortsteil = haltestelleSelectRs.getString("ortsteil");
				String dateipfad = haltestelleSelectRs.getString("dateipfad");
				String dateiname = haltestelleSelectRs.getString("dateiname");
				String datenlieferant = haltestelleSelectRs.getString("datenlieferant");
				String status = haltestelleSelectRs.getString("status");
NVBWLogger.info("Objekt-Id: " + dbid + ", DHID: " + dhid + ", Objektart: " + objektart.toString());
				String ds100 = haltestelleSelectRs.getString("ds100");
				int dbkategorie = haltestelleSelectRs.getInt("dbkategorie");
				String bahnhofname = haltestelleSelectRs.getString("bahnhofname");
				int bahnhofnr = haltestelleSelectRs.getInt("bfnr");
				String eiu = haltestelleSelectRs.getString("eiu");
				String bahnhofmanagement = haltestelleSelectRs.getString("bfmanagement");
				int bahnsteigoberkante = haltestelleSelectRs.getInt("bahnsteigoberkante");
				int bahnsteiglaenge = haltestelleSelectRs.getInt("offiziellebahnsteiglaenge");
				BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.HST_DHID.typ(), BFRKFeld.Name.HST_DHID, dhid);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
//TODO hier prüfen, ob mehrfache die DHID vorkommt, dann Fehler melden

				feld = new BFRKFeld(BFRKFeld.Name.HST_Name.typ(), BFRKFeld.Name.HST_Name, name);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Soll_Steig.typ(), BFRKFeld.Name.HST_Soll_Steig, sollSteig);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Soll_Lon.typ(), BFRKFeld.Name.HST_Soll_Lon, "" + lon);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Soll_Lat.typ(), BFRKFeld.Name.HST_Soll_Lat, "" + lat);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Gemeinde.typ(), BFRKFeld.Name.HST_Gemeinde, gemeinde);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Ortsteil.typ(), BFRKFeld.Name.HST_Ortsteil, ortsteil);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Importdateipfad.typ(), BFRKFeld.Name.HST_Importdateipfad, dateipfad);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Importdateiname.typ(), BFRKFeld.Name.HST_Importdateiname, dateiname);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.HST_Datenlieferant.typ(), BFRKFeld.Name.HST_Datenlieferant, datenlieferant);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_DS100.typ(), BFRKFeld.Name.BF_DS100, ds100);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_DBKategorie.typ(), BFRKFeld.Name.BF_DBKategorie, "" + dbkategorie);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_Nr.typ(), BFRKFeld.Name.BF_Nr, "" + bahnhofnr);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_Name.typ(), BFRKFeld.Name.BF_Name, bahnhofname);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_EIU.typ(), BFRKFeld.Name.BF_EIU, eiu);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_Management.typ(), BFRKFeld.Name.BF_Management, bahnhofmanagement);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_Bahnsteigoberkante.typ(), BFRKFeld.Name.BF_Bahnsteigoberkante, "" + bahnsteigoberkante);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				feld = new BFRKFeld(BFRKFeld.Name.BF_Bahnsteiglaenge.typ(), BFRKFeld.Name.BF_Bahnsteiglaenge, "" + bahnsteiglaenge);
				hstdatensatz.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

				boolean datensatzFiltern = false;

				if(	!paramStatus.equals("") &&
					!paramStatus.equals("%")) {
					if((status == null) || !status.equals(paramStatus))
						datensatzFiltern = true;
				}

				if(dhidListe.contains(dhid)) {
					datensatzFiltern = true;
					NVBWLogger.warning("Doppelte DHID gefunden, wird ignoriert. Dhid: " + dhid + ", DB-Id: " + dbid);
				}
				
				if(!datensatzFiltern) {
					hstdatensaetze.put(dbid, hstdatensatz);
					dhidListe.add(dhid);
				}
			}
NVBWLogger.info("in holhaltestellen nach DB-Datesatzschleife");
			haltestelleSelectRs.close();
			haltestelleSelectStmt.close();

			Date endquery = new Date();
			NVBWLogger.info("Anzahl Brutto-Datensätze: " + bruttoanzahl + ",  netto nach filtern: " + hstdatensaetze.size());
			NVBWLogger.info("Dauer holHaltstelle Query in sek: "
				+ (endquery.getTime() - startquery.getTime())/1000);
			NVBWLogger.info("DB-Query war ===" + haltestelleSelectStmt.toString() + "===");
		} catch (SQLException e1) {
			NVBWLogger.severe(e1.toString());
		}
	
		return hstdatensaetze;
	}


	private static Map<Objektart, Map<Long, Map<Name, BFRKFeld>>> holMerkmale(Long dbid) {
		Map<Objektart, Map<Long, Map<Name, BFRKFeld>>> objektliste = new HashMap<>();

		Date holstart = new Date();

		List<Long> objektidList = new ArrayList<>();
		Map<Long, Objektart> objektartMap = new HashMap<>();
		Map<Long, String> dhidMap = new HashMap<>();
		Map<Long, String> nameMap = new HashMap<>();
		Map<Long, String> infraidtempMap = new HashMap<>();
		Map<Long, String> steigMap = new HashMap<>();
		Map<Long, String> beschreibungMap = new HashMap<>();
		Map<Long, String> osmidMap = new HashMap<>();
		Map<Long, Double> lonMap = new HashMap<>();
		Map<Long, Double> latMap = new HashMap<>();
		Map<Long, Double> osmlonMap = new HashMap<>();
		Map<Long, Double> osmlatMap = new HashMap<>();
		

		
		
		String objektSelectSql = "SELECT o.id AS objekt_id, "
			+ "o.objektart, o.dhid, o.name AS objektname, "
			+ "o.steig AS objektsteig, o.beschreibung, "
			+ "ST_X(o.koordinate) AS lon, ST_Y(o.koordinate) AS lat, "
			+ "infraidtemp, osmid, "
			+ "ST_X(osm.koordinate) AS osmlon, ST_Y(osm.koordinate) AS osmlat "
			+ "FROM objekt AS o LEFT JOIN osmobjektbezug AS osm ON "
			+ "o.id = osm.objekt_id WHERE "
			+ "(parent_id = ? OR o.id = ?) ORDER by o.id, objekt_id;";
		PreparedStatement objektSelectStmt = null;
		try {
			objektSelectStmt = bfrkConn.prepareStatement(objektSelectSql);

			int stmtindex = 1;
			objektSelectStmt.setLong(stmtindex++, dbid);
			objektSelectStmt.setLong(stmtindex++, dbid);
			NVBWLogger.info("SQL-select Statement zum holen Haltestellen-Objekte aus Objekt-Tabelle '"
				+  objektSelectStmt.toString() + "'");

			ResultSet objektSelectRs = objektSelectStmt.executeQuery();
			while( objektSelectRs.next() ) {
				
				Long objekt_id = objektSelectRs.getLong("objekt_id");
				Objektart objektart = null;
				try {
					objektart = Objektart.valueOf(objektSelectRs.getString("objektart"));
				} catch(Exception e) {
					NVBWLogger.severe("Die Objektart in der BFRK-DB "
						+ objektSelectRs.getString("objektart")
						+ " kann nicht in die Programm-Objektart überführt werden.");
				}
				String dhid = objektSelectRs.getString("dhid");
				String infraidtemp = objektSelectRs.getString("infraidtemp");
				String steig = objektSelectRs.getString("objektsteig");
				String beschreibung = objektSelectRs.getString("beschreibung");
				String osmid = objektSelectRs.getString("osmid");
				String objektname = objektSelectRs.getString("objektname");
				double lon = objektSelectRs.getDouble("lon");
				double lat = objektSelectRs.getDouble("lat");
				double osmlon = objektSelectRs.getDouble("osmlon");
				double osmlat = objektSelectRs.getDouble("osmlat");

				
				
				if(osmid != null) {
					if(osmid.indexOf(",") != -1)
						osmid = osmid.replace(",", "|");
					osmidMap.put(objekt_id,  osmid);
				}
				objektidList.add(objekt_id);
				objektartMap.put(objekt_id, objektart);
				dhidMap.put(objekt_id, dhid);
				nameMap.put(objekt_id, objektname);
				infraidtempMap.put(objekt_id, infraidtemp);
				steigMap.put(objekt_id, steig);
				beschreibungMap.put(objekt_id, beschreibung);
				lonMap.put(objekt_id, lon);
				latMap.put(objekt_id, lat);
				osmlonMap.put(objekt_id, osmlon);
				osmlatMap.put(objekt_id, osmlat);
			}
			objektSelectRs.close();
			objektSelectStmt.close();
	
		} catch (SQLException e1) {
			NVBWLogger.severe("Fehler bei holMerkmale, Objekt-Query: " + objektSelectStmt.toString());
			NVBWLogger.severe(e1.toString());
		}
		
		String merkmaleSelectSql = "SELECT m.id AS merkmal_id, "
			+ "m.name, m.typ, "
			+ "CASE WHEN k.wert IS NULL THEN m.wert ELSE k.wert END AS wert, "
			+ "CASE WHEN k.wert IS NULL THEN false ELSE true END AS korrekturwert, "
			+ "bildname, ST_X(bild.koordinate) as bildlon, ST_Y(bild.koordinate) as bildlat "
			+ "FROM merkmal AS m LEFT JOIN merkmalkorrektur AS k "
			+ "ON m.objekt_id = k.objekt_id AND m.name = k.name "
			+ "LEFT JOIN bild ON m.wert = bildname "
			+ "WHERE m.objekt_id = ?;";

		try {
			PreparedStatement merkmaleSelectStmt = bfrkConn.prepareStatement(merkmaleSelectSql);

			for(int oindex = 0; oindex < objektidList.size(); oindex++) {
				long objekt_id = objektidList.get(oindex);

				int stmtindex = 1;
				merkmaleSelectStmt.setLong(stmtindex++, objekt_id);
				NVBWLogger.info("SQL-select Statement zum holen Haltestellen-Merkmale '"
					+  merkmaleSelectStmt.toString() + "'");
		
	
				ResultSet merkmaleSelectRs = merkmaleSelectStmt.executeQuery();
				while( merkmaleSelectRs.next() ) {
	
					Objektart objektart = objektartMap.get(objekt_id);
					String dhid = dhidMap.get(objekt_id);
					String objektname = nameMap.get(objekt_id);
					String infraidtemp = infraidtempMap.get(objekt_id);
					String steig = steigMap.get(objekt_id);
					String beschreibung = beschreibungMap.get(objekt_id);
					String osmid = osmidMap.get(objekt_id);
					double lon = lonMap.get(objekt_id);
					double lat = latMap.get(objekt_id);
					double osmlon = osmlonMap.get(objekt_id);
					double osmlat = osmlatMap.get(objekt_id);

					String feldname = merkmaleSelectRs.getString("name");
					String feldwert = merkmaleSelectRs.getString("wert");
					String typ = merkmaleSelectRs.getString("typ");
					String bildname = merkmaleSelectRs.getString("bildname");
					double bildlon = merkmaleSelectRs.getDouble("bildlon");
					double bildlat = merkmaleSelectRs.getDouble("bildlat");
	
					Datentyp datentyp = Datentyp.unset;
					if(typ.equals("Boolean"))
						datentyp = Datentyp.Boolean;
					else if(typ.equals("String"))
						datentyp = Datentyp.String;
					else if(typ.equals("Numeric"))
						datentyp = Datentyp.Numeric;
					
					BFRKFeld merkmal = new BFRKFeld(datentyp, feldname, feldwert);
					try {
						Map<Long, Map<Name, BFRKFeld>> objekte = new HashMap<>();
						if(objektliste.containsKey(objektart))
							objekte = objektliste.get(objektart);
						Map<Name, BFRKFeld> merkmalliste = new HashMap<>();
						if(objekte.containsKey(objekt_id))
							merkmalliste = objekte.get(objekt_id);
						merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);
						objekte.put(objekt_id,  merkmalliste);
	
							// zusätzlich aus vorheriger Objekt-Query einige weitere Merkmale bereitstellen
						if((dhid != null) && !dhid.isEmpty()) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_DHID.typ(), BFRKFeld.Name.STG_DHID, dhid);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}
						
						if((objektname != null) && !objektname.isEmpty()) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_Name.typ(), BFRKFeld.Name.STG_Name, objektname);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if((steig != null) && !steig.isEmpty()) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_Soll_Steig.typ(), BFRKFeld.Name.STG_Soll_Steig, steig);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if((beschreibung != null) && !beschreibung.isEmpty()) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_Beschreibung.typ(), BFRKFeld.Name.STG_Beschreibung, beschreibung);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if((objektname != null) && !objektname.isEmpty()) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_Name.typ(), BFRKFeld.Name.STG_Name, objektname);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if(lon != 0.0) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_Soll_Lon.typ(), BFRKFeld.Name.STG_Soll_Lon, "" + lon);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if(lat != 0.0) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.STG_Soll_Lat.typ(), BFRKFeld.Name.STG_Soll_Lat, "" + lat);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if((infraidtemp != null) && !infraidtemp.isEmpty()) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP.typ(), BFRKFeld.Name.ZUSATZ_INFRAIDTEMP, infraidtemp);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if(osmid != null) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_OSM_Id.typ(), BFRKFeld.Name.ZUSATZ_OSM_Id, osmid);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

							feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_OSM_Lon.typ(), 
								BFRKFeld.Name.ZUSATZ_OSM_Lon, "" + osmlon);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

							feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_OSM_Lat.typ(), 
								BFRKFeld.Name.ZUSATZ_OSM_Lat, "" + osmlat);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}

						if(bildname != null) {
							BFRKFeld feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_Bild_Url.typ(), BFRKFeld.Name.ZUSATZ_Bild_Url, osmid);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

							feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_Bild_Lon.typ(), 
								BFRKFeld.Name.ZUSATZ_Bild_Lon, "" + bildlon);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);

							feld = new BFRKFeld(BFRKFeld.Name.ZUSATZ_Bild_Lat.typ(), 
								BFRKFeld.Name.ZUSATZ_Bild_Lat, "" + bildlat);
							merkmalliste.put(BFRKFeld.Name.valueOf(feld.getFeldname()), feld);
						}
						objektliste.put(objektart, objekte);
					} catch (Exception err) {
	//TODO Fehler protokollieren
						System.out.println("unbekannter Feldname aus DB ===" + feldname + "===");
						System.out.println(err.toString());
	
						NVBWLogger.warning("DHID\t" + dhid + "\tObjektart\t" + objektart + "\tMerkmal-Name\t" + feldname
							+ "\tFeldwert\t" + feldwert + "\tkann nicht in BFRKFeld-Element überführt werden");
					}
				}
				merkmaleSelectRs.close();
			}
			merkmaleSelectStmt.close();

			Date holEnde = new Date();
			NVBWLogger.info("Dauer holMerkmale in sek: " + (holEnde.getTime() - holstart.getTime())/1000.0);
		} catch (SQLException e1) {
			NVBWLogger.severe(e1.toString());
		}

		return objektliste;
	}


	private static Map<Long, Map<Name, BFRKFeld>> holNotizen(String kreisschluessel, 
		String dhid, Objektart objektart) {
		Map<Long, Map<Name, BFRKFeld>> objekte = new HashMap<>();

		String notizobjektSelectSql = "SELECT id, "
			+ "titel, text, ST_X(koordinate) AS lon, "
			+ "ST_Y(koordinate) AS lat "
			+ "FROM notizobjekt WHERE "
			+ "kreisschluessel = ? AND dhid = ? AND objektart = ?;";

		try {
			PreparedStatement notizobjektSelectStmt = bfrkConn.prepareStatement(notizobjektSelectSql);

			int stmtindex = 1;
			notizobjektSelectStmt.setString(stmtindex++, kreisschluessel);
			notizobjektSelectStmt.setString(stmtindex++, dhid);
			notizobjektSelectStmt.setString(stmtindex, objektart.toString());
			NVBWLogger.finer("SQL-select Statement zum holen DIVA-Notizobjekte '"
				+  notizobjektSelectStmt.toString() + "'");
	

			Date resultstart = new Date();
			ResultSet notizobjektSelectRs = notizobjektSelectStmt.executeQuery();
			Date resultfinish = new Date();
			NVBWLogger.finest("holnotizen: DB-Query Dauer in msek: " + (resultfinish.getTime() - resultstart.getTime()));

			
			while( notizobjektSelectRs.next() ) {

				long dbid = notizobjektSelectRs.getLong("id");
				String titel = notizobjektSelectRs.getString("titel");
				String text = notizobjektSelectRs.getString("text");
				double lon = notizobjektSelectRs.getDouble("lon");
				double lat = notizobjektSelectRs.getDouble("lat");

				Map<Name, BFRKFeld> merkmalliste = new HashMap<>();
				
				BFRKFeld merkmal = new BFRKFeld(Datentyp.String, BFRKFeld.Name.DIVANOTIZ_Kreisschluessel, kreisschluessel);
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				merkmal = new BFRKFeld(Datentyp.String, BFRKFeld.Name.DIVANOTIZ_dhid, dhid);
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				merkmal = new BFRKFeld(Datentyp.String, BFRKFeld.Name.DIVANOTIZ_Titel, titel);
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				merkmal = new BFRKFeld(Datentyp.String, BFRKFeld.Name.DIVANOTIZ_Text, text);
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				merkmal = new BFRKFeld(Datentyp.String, BFRKFeld.Name.DIVANOTIZ_Objektart, objektart.toString());
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				merkmal = new BFRKFeld(Datentyp.Numeric, BFRKFeld.Name.DIVANOTIZ_Lon, "" + lon);
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				merkmal = new BFRKFeld(Datentyp.Numeric, BFRKFeld.Name.DIVANOTIZ_Lat, "" + lat);
				merkmalliste.put(BFRKFeld.Name.valueOf(merkmal.getFeldname()), merkmal);

				objekte.put(dbid,  merkmalliste);
				
			}
			notizobjektSelectRs.close();
			notizobjektSelectStmt.close();
		
		} catch (SQLException e1) {
			NVBWLogger.severe(e1.toString());
		}
		return objekte;
	}

	
	private static String generateEindeutigeObjektId(String objektid, Objektart objektart, List<String> objektIdListe) {
		String wunschidprefix = "";

		if((objektid != null) && !objektid.equals(""))
wunschidprefix = objektid;
		if(wunschidprefix.equals("")) {
			if(objektart == Objektart.Bahnhof)
				wunschidprefix = "Bahnhof";
			else if(objektart == Objektart.Haltestelle)
				wunschidprefix = "Hst";
			else if(objektart == Objektart.Bahnsteig)
				wunschidprefix = "Bstg";
			else if(objektart == Objektart.Haltesteig)
				wunschidprefix = "Steig";
			else if(objektart == Objektart.Aufzug)
				wunschidprefix = "Aufzug";
			else if(objektart == Objektart.BuR)
				wunschidprefix = "BuR";
			else if(objektart == Objektart.Kartenautomat)
				wunschidprefix = "Automat";
			else if(objektart == Objektart.Engstelle)
				wunschidprefix = "Engstelle";
			else if(objektart == Objektart.Fahrplananzeigetafel)
				wunschidprefix = "Fahrplantafel";
			else if(objektart == Objektart.Gleisquerung)
				wunschidprefix = "Querung";
			else if(objektart == Objektart.Informationsstelle)
				wunschidprefix = "Infostelle";
			else if(objektart == Objektart.Leihradanlage)
				wunschidprefix = "Leihrad";
			else if(objektart == Objektart.Parkplatz)
				wunschidprefix = "PuR";
			else if(objektart == Objektart.Rampe)
				wunschidprefix = "Rampe";
			else if(objektart == Objektart.Rolltreppe)
				wunschidprefix = "Rolltreppe";
			else if(objektart == Objektart.Stationsplan)
				wunschidprefix = "Plan";
			else if(objektart == Objektart.Taxistand)
				wunschidprefix = "Taxi";
			else if(objektart == Objektart.Toilette)
				wunschidprefix = "Toilette";
			else if(objektart == Objektart.Treppe)
				wunschidprefix = "Treppe";
			else if(objektart == Objektart.Tuer)
				wunschidprefix = "Tür";
			else if(objektart == Objektart.Verkaufsstelle)
				wunschidprefix = "Verkaufsstelle";
			else if(objektart == Objektart.Weg)
				wunschidprefix = "Weg";
			else {
				NVBWLogger.severe("Für Objektart wurde noch kein Kürzel definiert: " + objektart.name());
				return null;
			}
		}
		if(wunschidprefix.equals("")) {
			NVBWLogger.severe("Für Objektart wurde noch kein Kürzel definiert: " + objektart.name());
			return null;
		}
		
		
		for(int wert = 0; wert < 26; wert++) {
			String wunschid = wunschidprefix;
//			if((wert >= 0) && (wert < 10))
//				wunschid += wert;
//			else {
//				wunschid += (char) (65 + wert - 10);
				wunschid += (char) (65 + wert);
//			}

			if(!objektIdListe.contains(wunschid)) {
				return wunschid;
			}
		}
		// TODO Auto-generated method stub
		return null;
	}



	public static int execute(String[] args) {

		String paramKreisschluessel = "";
		String paramOevart = "%";
		String paramRootdir = "c:\\users\\sei\\temp";
		String paramStatus = "";
		String paramEYEvisVorlage = "";
		String paramAusgabedateiPrefix = "";
		boolean paramBilderkopieren = false;
		String paramBilderzielverzeichnis = "";
		String sheetname = "Erfassung";
		List<String> objektIdListe = new ArrayList<>();
		List<String> paramLinienHaltestellenListe = new ArrayList<>();
		List<Objektart> paramObjektartenListe = new ArrayList<>();
		Map<Long, Objektart> paramObjektIDListe = new HashMap<>();

		NVBWLogger.init("/home/NVBWAdmin/tomcat-deployment/bfrk_api_home/eyevisprojektdaten/ExportNachEYEvis.log", configuration.logging_console_level, configuration.logging_file_level);

		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-workflowstatus Lieferant_Final|%: Default Lieferant_Final");
			System.out.println("-oevart S|O - wenn beides, dann nicht angeben. Default beides");
			System.out.println("-kreisschluessel 08111. Default alle Daten");
			System.out.println("-eyevisvorlage xy - Name der Exceldatei, die aus einem EYEvis-Projektexport stammt");
			System.out.println("-bilderkopieren ja|nein - Sollen die Bilder in einen Zielordner kopiert werden (Default. nein)");
			System.out.println("-bilderzielverzeichnis - Angabe eines Verzeichnisses, in das Bilder hin kopiert werden sollen, wenn auch -bilderkopieren = ja gesetzt wurde");
			System.out.println("-dhids - Liste der Bahnhof|Haltstellen-DHIDs, bitte in doppelte Anführungszeichen setzen. Mehrere Werte durch Komma trennen");
			System.out.println("-objektarten - Beschränkung auf einzelne Objektarten, bitte in doppelte Anführungszeichen setzen. Mehrere Werte durch Komma trennen. "
				+ "Gültig sind Aufzug|Bahnhof|Bahnsteig|BuR|Engstelle|Gleisquerung|Haltesteig|Haltestelle|Informationsstelle|"
				+ "Kartenautomat|Leihradanlage|Parkplatz|Rampe|Rolltreppe|SEVHaltesteig|Stationsplan|Taxistand|Toilette|"
				+ "Treppe|Tuer|Verkaufsstelle|Weg");
			return -1;
		}
		
		if(args.length >= 1) {
			int args_ok_count = 0;
			for(int argsi=0;argsi<args.length;argsi+=2) {
				NVBWLogger.info(" args pair analysing #: "+argsi+"  ==="+args[argsi]+"===");
				if(args.length > argsi+1)
					NVBWLogger.info("  args #+1: "+(argsi+1)+"   ==="+args[argsi+1]+"===");
				NVBWLogger.info("");
				if(args[argsi].equals("-workflowstatus")) {
					paramStatus = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-oevart")) {
					paramOevart = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-rootdir")) {
					paramRootdir = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-dhids")) {
					if(args[argsi+1] != null && !args[argsi+1].equals("")) {
						String tempdhids[] = args[argsi+1].split(",",-1);
						for(int index = 0; index < tempdhids.length; index++) {
							String akttext = tempdhids[index].trim();
							paramLinienHaltestellenListe.add(akttext);
						}
					}
					args_ok_count += 2;
				} else if(args[argsi].equals("-objektarten")) {
					boolean fehlervorhanden = false;
					if(args[argsi+1] != null && !args[argsi+1].equals("")) {
						Pattern pattern = Pattern.compile("^([A-Za-zÄÖÜäöü]+)(\\([^\\)]+\\))?(,)?");

						Objektart aktobjektart = Objektart.unbekannt;
						String aktobjektlisteString = "";

						int anzahlverarbeiteteZeichen = 0;
						String objektartenString = args[argsi+1];
						Matcher matcher = pattern.matcher(objektartenString);
						while(matcher.find()) {
							System.out.println("Anzahl groupCount: " + matcher.groupCount());
							if(matcher.groupCount() >= 1) {
								System.out.println("gruppe 1 content ===" + matcher.group(1) + "===");
								anzahlverarbeiteteZeichen += matcher.group(1).length();
								try {
									aktobjektart = Objektart.valueOf(matcher.group(1));
									paramObjektartenListe.add(aktobjektart);
								} catch(Exception e) {
									fehlervorhanden = true;
									NVBWLogger.severe("Die Objektart ist ungültig: " + matcher.group(1));
								}
							}
							if((matcher.groupCount() >= 2) && (matcher.group(2) != null)) {
								System.out.println("gruppe 2 content ===" + matcher.group(2) + "===");
								anzahlverarbeiteteZeichen += matcher.group(2).length();

								String objektidlistenString = matcher.group(2);
								if(objektidlistenString.substring(0,1).equals("("))
									objektidlistenString = objektidlistenString.substring(1);
								if(objektidlistenString.substring(objektidlistenString.length() - 1,objektidlistenString.length()).equals(")"))
									objektidlistenString = objektidlistenString.substring(0,objektidlistenString.length() - 1);
								
								String aktobjektliste[] = objektidlistenString.split(",",-1);
								for(int index = 0; index < aktobjektliste.length; index++) {
									long aktobjektid = Long.parseLong(aktobjektliste[index]);
									paramObjektIDListe.put(aktobjektid, aktobjektart);
								}
							}
							if((matcher.groupCount() == 3) && (matcher.group(3) != null)) {
								System.out.println("gruppe 3 content ===" + matcher.group(3) + "===");
								anzahlverarbeiteteZeichen += matcher.group(3).length();
							}

							objektartenString = objektartenString.substring(anzahlverarbeiteteZeichen);
							if(objektartenString.length() == 0)
								break;
							anzahlverarbeiteteZeichen = 0;
							matcher = pattern.matcher(objektartenString);
						}

						if(fehlervorhanden) {
							NVBWLogger.severe("ungültige Objektarten, PROGRAMM WIRD ANGEHALTEN. Bitte neu aufrufen mit korrekten Werten");
							return -3;
						}
					}
					args_ok_count += 2;
				} else if(args[argsi].equals("-bilderkopieren")) {
					if(args[argsi+1].toUpperCase().equals("JA"))
						paramBilderkopieren = true;
					else
						paramBilderkopieren = false;
					args_ok_count += 2;
				} else if(args[argsi].equals("-bilderzielverzeichnis")) {
					paramBilderzielverzeichnis = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-eyevisvorlage")) {
					paramEYEvisVorlage = args[argsi+1];
					args_ok_count += 2;
				} else if(args[argsi].equals("-ausgabedateiprefix")) {
					paramAusgabedateiPrefix = args[argsi+1];
					args_ok_count += 2;
				} else {
					NVBWLogger.info("Unbekannter Programmparameter ===" + args[argsi] + "===");
					System.out.println("Unbekannter Programmparameter ===" + args[argsi] + "===");
					return -4;
				}
			}
			if(args_ok_count != args.length) {
				NVBWLogger.info("ERROR: not all programm parameters were valid, STOP");
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return -5;
			}
		}

		if(paramObjektartenListe.size() > 0) {
			if(paramLinienHaltestellenListe.size() > 1) {
				NVBWLogger.severe("Es wurden zu mindestens einer Objektarten Objekt-Ids angegeben, "
					+ "dann darf nur eine einzige DHID, also keine Liste von DHID angegeben werden, PROGRAMMABBRUCH");
				return -6;
			}
		}
		
				
		if(!connectDB()) {
			NVBWLogger.severe("es konnte keine DB-Verbindung hergestellt werden, PROGRAMMABBRUCH");
			return -7;
		}

		if(paramBilderkopieren && paramBilderzielverzeichnis.isEmpty()) {
			NVBWLogger.severe("Der Kommandozeilenparameter -bilderzielverzeichnis muß gesetzt werden, "
				+ "weil Kommandozeilenparameter -bilderkopieren = JA gesetzt ist, PRGORAMMABBRUCH");
			return -9;
		}

		if(paramLinienHaltestellenListe.size() == 0) {
			NVBWLogger.severe("Der Kommandozeilenparameter -dhids muß gesetzt werden, "
				+ "PRGORAMMABBRUCH");
			return -10;
		}

		Date programmstart = new Date();
		NVBWLogger.info("Programmstart um: " + datetime_de_formatter.format(programmstart));

		if(paramEYEvisVorlage.isEmpty()) {
			NVBWLogger.severe("Der Programmparameter -eyevisvorlage fehlt und ist Pflichtangabe.  PRORGRAMMABBRUCH");
			return -11;
		}

		String quellexceldatei = paramRootdir + File.separator + paramEYEvisVorlage + ".xlsx";
		File quellexcelhandle = new File(quellexceldatei);
		if((quellexcelhandle == null) || !quellexcelhandle.exists()) {
			NVBWLogger.severe("Die Exceldatei " + quellexceldatei + " konnte nicht geöffnet werden, PROGORAMMABBRUCH");
			return -21;
		}

		String zielexceldatei = paramBilderzielverzeichnis + File.separator
			+ paramAusgabedateiPrefix + ".xlsx";
		
		StringBuffer eyevisCSVOutput = new StringBuffer();
		eyevisCSVOutput.append("DHID;Gemeinde;Ortsteil;Name;Objekt;Beschreibung;lat;lon\r\n");

		if(!openExceldatei(quellexceldatei)) {
			NVBWLogger.severe("Die Exceldatei " + quellexceldatei + " konnte nicht geöffnet werden, PROGORAMMABBRUCH");
			return -12;
		}
		naechsteZeilennrMap.put(sheetname, 1);

		for(int haltestellenindex = 0; haltestellenindex < paramLinienHaltestellenListe.size(); haltestellenindex++) {
			
			Map<Long, Map<Name, BFRKFeld>> hstdatensaetze = holHaltestelle(paramLinienHaltestellenListe.get(haltestellenindex), 
				paramOevart, paramStatus);


			String hst_dhid = "";
			String hst_name = "";
			String hst_beschreibung = "";
			boolean haltestellenobjektVorhanden = false;
			int lfdnr = 0;
			
NVBWLogger.info("Vor Schleife über die Haltestellen-Objekte ...");

				// Schleife über die Haltestellen-Objekte
			for(Map.Entry<Long, Map<Name, BFRKFeld>> hstentry: hstdatensaetze.entrySet()) {
			
				objektIdListe.clear();
				
				Long hstdbnr = hstentry.getKey();
				Map<Name, BFRKFeld> hstMerkmale = hstentry.getValue();
	
				hst_dhid = hstMerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
NVBWLogger.info("Objekt-Id: " + hstdbnr + ", DHID: " + hst_dhid);
if(hstMerkmale.containsKey(BFRKFeld.Name.Objektart))
	NVBWLogger.info(", Objektart: " + hstMerkmale.get(BFRKFeld.Name.Objektart).getTextWert());
				lfdnr++;
				if((lfdnr % 100) == 0)
					NVBWLogger.info("Anzahl Haltestellen bisher: " + lfdnr);
	
				
								
				
				ExportNachEYEvisObjektpruefung exportNachEYEvisPruefung = null;
				try {
					exportNachEYEvisPruefung = new ExportNachEYEvisObjektpruefung(
						paramBilderkopieren,
						Bildquellenart.downloadprivateundpublic, workbook);
					exportNachEYEvisPruefung.setBildDownloadverzeichnis(paramBilderzielverzeichnis + File.separator + "temp");
					exportNachEYEvisPruefung.setBilderZielverzeichnis(paramBilderzielverzeichnis);
					exportNachEYEvisPruefung.setEYEvisVorlage(paramEYEvisVorlage);
				} catch (Throwable e1) {
					NVBWLogger.severe("Abbruch in Erhebungspruefung, weil der Bildbestand nicht verfügbar ist");
					NVBWLogger.severe(e1.toString());
					return -13;
				}
				
				
					// Hol alle Objekte (Steig, BuR, PuR, etc.) zur aktuellen Haltestelle
				Map<Objektart, Map<Long, Map<Name, BFRKFeld>>> objektliste = holMerkmale(hstdbnr);
	
				
					// Schleife über die Objekte (Steig, BuR, PuR, etc.) zur aktuellen Haltestelle
				for(Map.Entry<Objektart, Map<Long, Map<Name, BFRKFeld>>> objektlistentry: objektliste.entrySet()) {
					Objektart objektart = objektlistentry.getKey();
					Map<Long, Map<Name, BFRKFeld>> objekte = objektlistentry.getValue();


						// Schleife über alle Objekte des Bahnhofs / der Haltestelle
					for(Map.Entry<Long, Map<Name, BFRKFeld>> objektentry: objekte.entrySet()) {
						Long objektdbid = objektentry.getKey();
						Map<Name, BFRKFeld> objekt = objektentry.getValue();

						boolean filtern_geprueft = false;
							// Wenn es zu einzelnen Objektarten eine ObjektID-Liste gibt ...
						if(paramObjektIDListe.size() > 0) {
								// und wenn es zur aktuellen Objektart eine ObjektID-Liste gibt ... 
							if(paramObjektIDListe.containsValue(objektart)) {
								filtern_geprueft = true;
									// und wenn die aktuelle ObjektID aus der DB NICHT in der Liste der aktuellen Objektart vorkommt
									// dann diesen Datensatz ignorieren
								if(!paramObjektIDListe.containsKey(objektdbid)) {
									NVBWLogger.info("konkretes Objekt mit DB-ID " + objektdbid + " wird ignoriert, weil es für diese Objektart eine ObjektID-Liste mitgegeben wurde, die aktuelle ID aber nicht dabei war");
									continue;
								} else {
									paramObjektIDListe.remove(objektdbid, objektart);
								}
							}
						}

							// wenn eben bei expliziten Objektids noch keine Filterung erfolgte
						if(!filtern_geprueft) {
								// Wenn nicht alle Objekte exportiert werden sollen, also eine Liste von Objektarten
								// angegeben wurden, die nicht gewollten hier übergehen
								// Aber Bahnhof oder Haltestelle wird immer gebraucht, auch wenn nicht angegeben
							if(paramObjektartenListe.size() > 0) {
								if(		(objektart != Objektart.Bahnhof)
									&&	(objektart != Objektart.Haltestelle)
									&& 	!paramObjektartenListe.contains(objektart)) {
									NVBWLogger.info("Objektart wird ignoriert, wird nicht gebraucht: " + objektart.name());
									continue;
								}
							}
						}

						Map<String, Excelfeld> eyevisWerte = new HashMap<>();

						if(objektart == Objektart.Bahnhof) {
							eyevisWerte = exportNachEYEvisPruefung.generateBahnhof(hstMerkmale, objekt);
							haltestellenobjektVorhanden = true;
						}
		
						if(objektart == Objektart.Bahnsteig) {
							eyevisWerte = exportNachEYEvisPruefung.generateBahnsteig(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Haltestelle) {
NVBWLogger.info("bei Bearbeitung Haltstelle - Objekt-Id: " + hstdbnr + ", DHID: " + hst_dhid);
if(hstMerkmale.containsKey(BFRKFeld.Name.Objektart))
	NVBWLogger.info(", Objektart: " + hstMerkmale.get(BFRKFeld.Name.Objektart).getTextWert());

							eyevisWerte = exportNachEYEvisPruefung.generateHaltestelle(hstMerkmale, objekt);
							haltestellenobjektVorhanden = true;
						}
		
						if(objektart == Objektart.Haltesteig) {
							eyevisWerte = exportNachEYEvisPruefung.generateHaltesteig(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Aufzug) {
							eyevisWerte = exportNachEYEvisPruefung.generateAufzug(hstMerkmale, objekt);
						}
		
						if(objektart == Objektart.BuR) {
							eyevisWerte = exportNachEYEvisPruefung.generateBikeRide(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Engstelle) {
							eyevisWerte = exportNachEYEvisPruefung.generateEngstelle(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Kartenautomat) {
							eyevisWerte = exportNachEYEvisPruefung.generateFahrkartenautomat(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Gleisquerung) {
							eyevisWerte = exportNachEYEvisPruefung.generateGleisquerung(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Informationsstelle) {
							eyevisWerte = exportNachEYEvisPruefung.generateInformationsstelle(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Leihradanlage) {
							eyevisWerte = exportNachEYEvisPruefung.generateLeihfahrradanlage(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Parkplatz) {
							eyevisWerte = exportNachEYEvisPruefung.generateParkplatz(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Rampe) {
							eyevisWerte = exportNachEYEvisPruefung.generateRampe(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Rolltreppe) {
							eyevisWerte = exportNachEYEvisPruefung.generateRolltreppe(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Stationsplan) {
							eyevisWerte = exportNachEYEvisPruefung.generateStationsplan(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Taxistand) {
							eyevisWerte = exportNachEYEvisPruefung.generateTaxi(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Toilette) {
							eyevisWerte = exportNachEYEvisPruefung.generateToilette(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Treppe) {
							eyevisWerte = exportNachEYEvisPruefung.generateTreppe(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Tuer) {
							eyevisWerte = exportNachEYEvisPruefung.generateTuer(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Verkaufsstelle) {
							eyevisWerte = exportNachEYEvisPruefung.generateVerkaufsstelle(hstMerkmale, objekt);
						}

						if(objektart == Objektart.Weg) {
							eyevisWerte = exportNachEYEvisPruefung.generateWeg(hstMerkmale, objekt);
						}


						if(		(eyevisWerte != null) 
							&& 	(eyevisWerte.size() > 0)) {

							if(eyevisWerte.containsKey("Name"))
								hst_name = eyevisWerte.get("Name").getTextWert();
							
							String objektid = "";
							if(eyevisWerte.containsKey("Objekt"))
								objektid = eyevisWerte.get("Objekt").getTextWert();
							if(		(objektid == null)
								||	objektid.equals("")) {
								objektid = generateEindeutigeObjektId(objektid, objektart,objektIdListe);
								if(objektid == null) {
									NVBWLogger.severe("es konnte keine eindeutige Objektid erzeugt werden für Objektart " 
										+ objektart.name() + " und die vorhandene objektidListe: " 
										+ objektIdListe.toString());
									continue;
								}
								if(!objektid.isEmpty() && objektid.length() > EYEvisObjektidMaxlength) {
									NVBWLogger.info("Objekt-Inhalt war länger als " + EYEvisObjektidMaxlength + " Zeichen ===" + objektid + "===");
									objektid = objektid.replace(" / ", "/");
									if(objektid.length() > EYEvisObjektidMaxlength)
										objektid = objektid.substring(0,EYEvisObjektidMaxlength - 1);
									NVBWLogger.info("Objekt-Inhalt wurde gekürzt auf " + EYEvisObjektidMaxlength 
										+ " Zeichen ===" + objektid + "=== (Zeile 1416)");
								}
								Excelfeld objektfeld = eyevisWerte.get("Objekt");
								objektfeld.setInhalt(objektid);
								eyevisWerte.put("Objekt", objektfeld);
								objektIdListe.add(objektid);
							} else {
								if(objektIdListe.contains(objektid)) {
									objektid = generateEindeutigeObjektId(objektid, objektart,objektIdListe);
									if(objektid == null) {
										NVBWLogger.severe("es konnte keine eindeutige Objektid erzeugt werden für Objektart " 
											+ objektart.name() + " und die vorhandene objektidListe: " 
											+ objektIdListe.toString());
										continue;
									}
									if(!objektid.isEmpty() && objektid.length() > EYEvisObjektidMaxlength) {
										NVBWLogger.info("Objekt-Inhalt war länger als " + EYEvisObjektidMaxlength 
											+ " Zeichen ===" + objektid + "===");
										objektid = objektid.replace(" / ", "/");
										if(objektid.length() > EYEvisObjektidMaxlength)
											objektid = objektid.substring(0,EYEvisObjektidMaxlength - 1);
										NVBWLogger.info("Objekt-Inhalt wurde gekürzt auf " + EYEvisObjektidMaxlength
											+ " Zeichen ===" + objektid + "=== (Zeile 1435)");
									}
									Excelfeld objektfeld = eyevisWerte.get("Objekt");
									objektfeld.setInhalt(objektid);
									eyevisWerte.put("Objekt", objektfeld);
									objektIdListe.add(objektid);
								} else {
									if(!objektid.isEmpty() && objektid.length() > EYEvisObjektidMaxlength) {
										NVBWLogger.info("Objekt-Inhalt war länger als " + EYEvisObjektidMaxlength
											+ " Zeichen ===" + objektid + "===");
										objektid = objektid.replace(" / ", "/");
										if(objektid.length() > EYEvisObjektidMaxlength)
											objektid = objektid.substring(0,EYEvisObjektidMaxlength - 1);
										NVBWLogger.info("Objekt-Inhalt wurde gekürzt auf " + EYEvisObjektidMaxlength 
											+ " Zeichen ===" + objektid + "=== (Zeile 1448)");
									}
									objektIdListe.add(objektid);
									Excelfeld objektfeld = eyevisWerte.get("Objekt");
									objektfeld.setInhalt(objektid);
									eyevisWerte.put("Objekt", objektfeld);
								}
							}


							schreibeExcelZeile(sheetname, naechsteZeilennrMap.get(sheetname), eyevisWerte);
							naechsteZeilennrMap.put(sheetname, 1 + naechsteZeilennrMap.get(sheetname));


							if(eyevisWerte.containsKey("DHID"))
								eyevisCSVOutput.append(eyevisWerte.get("DHID").getTextWert());
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("Gemeinde"))
								eyevisCSVOutput.append(eyevisWerte.get("Gemeinde").getTextWert());
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("Ortsteil"))
								eyevisCSVOutput.append(eyevisWerte.get("Ortsteil").getTextWert());
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("Name")) {
								String text = eyevisWerte.get("Name").getTextWert();
								if(!text.isEmpty() && text.length() > EYEvisNameMaxlength) {
									text = text.replace(" / ", "/");
									if(text.length() > EYEvisNameMaxlength)
									text = text.substring(0,EYEvisNameMaxlength - 1);
								}
								eyevisCSVOutput.append(text);
							}
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("Objekt")) {
								String text = eyevisWerte.get("Objekt").getTextWert();
								if(!text.isEmpty() && text.length() > EYEvisObjektidMaxlength) {
									NVBWLogger.info("Objekt-Inhalt war länger als " + EYEvisObjektidMaxlength
										+ " Zeichen ===" + text + "===");
									text = text.replace(" / ", "/");
									if(text.length() > EYEvisObjektidMaxlength)
									text = text.substring(0,EYEvisObjektidMaxlength - 1);
									NVBWLogger.info("Objekt-Inhalt wurde gekürzt auf " + EYEvisObjektidMaxlength
										+ " Zeichen ===" + text + "=== (Zeile 1478)");
								}
								eyevisCSVOutput.append(text);
							}
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("Beschreibung")) {
								String text = eyevisWerte.get("Beschreibung").getTextWert();
								eyevisCSVOutput.append(text);
							}
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("lat"))
								eyevisCSVOutput.append(eyevisWerte.get("lat").getTextWert());
							eyevisCSVOutput.append(";");
							if(eyevisWerte.containsKey("lon"))
								eyevisCSVOutput.append(eyevisWerte.get("lon").getTextWert());
							eyevisCSVOutput.append("\r\n");
						}
					}	// Ende Schleife über alle Objekte des Bahnhofs / der Haltestelle					
				}	// Schleife über die Objekte (Steig, BuR, PuR, etc.) zur aktuellen Haltestelle

					// wenn es zum aktuellen (nicht-Hauptobjekt) kein Hauptobjekt (Haltestelle oder Bahnhof) gibt,
					// dann hier vorab ein Hauptobjekt erstellen und in Excel speichern
				if(!haltestellenobjektVorhanden) {
					NVBWLogger.info("Es gab kein Hauptobjekt für die DHID: " + hst_dhid 
						+ ", daher wird jetzt noch ein Not-Hauptobjekt erstellt");
					if(hstMerkmale != null)
						NVBWLogger.info("hstMerkmale-Properties: " + hstMerkmale.toString());
					Map<String, Excelfeld> hstExcelwerte = new HashMap<>();
	
					StringBuffer hstcsvzeile = new StringBuffer();
	
					if((hstMerkmale != null) && hstMerkmale.containsKey(BFRKFeld.Name.HST_DHID)) {
						hstExcelwerte.put("DHID", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
							ExportNachEYEvisObjektpruefung.Textausgabe(hstMerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()), 
							ExportNachEYEvisObjektpruefung.unsicherFarbstyle));
						hstcsvzeile.append(ExportNachEYEvisObjektpruefung.Textausgabe(hstMerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
					}
					hstcsvzeile.append(";");
	
					if(hstMerkmale.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
						hstExcelwerte.put("Gemeinde", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
							hstMerkmale.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
							ExportNachEYEvisObjektpruefung.unsicherFarbstyle));
						hstcsvzeile.append(ExportNachEYEvisObjektpruefung.Textausgabe(hstMerkmale.get(BFRKFeld.Name.HST_Gemeinde).getTextWert()));
					}
					hstcsvzeile.append(";");
	
					if(hstMerkmale.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
						hstExcelwerte.put("Ortsteil", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
							hstMerkmale.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
							ExportNachEYEvisObjektpruefung.unsicherFarbstyle));
						hstcsvzeile.append(ExportNachEYEvisObjektpruefung.Textausgabe(hstMerkmale.get(BFRKFeld.Name.HST_Ortsteil).getTextWert()));
					}
					hstcsvzeile.append(";");
					if(hstMerkmale.containsKey(BFRKFeld.Name.BF_Name)) {
						String text = hstMerkmale.get(BFRKFeld.Name.BF_Name).getTextWert();
						if(!text.isEmpty() && text.length() > EYEvisNameMaxlength) {
							text = text.replace(" / ", "/");
							if(text.length() > EYEvisNameMaxlength)
							text = text.substring(0,EYEvisNameMaxlength - 1);
						}
						hstExcelwerte.put("Name", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
							ExportNachEYEvisObjektpruefung.Textausgabe(text), 
							ExportNachEYEvisObjektpruefung.unsicherFarbstyle));
						hstcsvzeile.append(ExportNachEYEvisObjektpruefung.Textausgabe(text));
					}
					hstcsvzeile.append(";");
	
					hstExcelwerte.put("Objekt", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
						"0", 
						ExportNachEYEvisObjektpruefung.unsicherFarbstyle));
					hstcsvzeile.append("0;");
	
					if(hstMerkmale.containsKey(BFRKFeld.Name.BF_Name)) {
						String text = "HST: " + ExportNachEYEvisObjektpruefung.Textausgabe(hstMerkmale.get(BFRKFeld.Name.BF_Name).getTextWert());
						if(!text.isEmpty() && text.length() > EYEvisBeschreibungMaxlength) {
							text = text.replace(" / ", "/");
							if(text.length() > EYEvisBeschreibungMaxlength)
							text = text.substring(0,EYEvisBeschreibungMaxlength - 1);
						}
						hstExcelwerte.put("Beschreibung", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
							text, 
							ExportNachEYEvisObjektpruefung.unsicherFarbstyle));
						hstcsvzeile.append(text);
					}
					hstcsvzeile.append(";");
	
					hstExcelwerte.put("lat", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
							ExportNachEYEvisObjektpruefung.Textausgabe("" + hstMerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert()), 
							ExportNachEYEvisObjektpruefung.okFarbstyle));
					hstcsvzeile.append(ExportNachEYEvisObjektpruefung.Textausgabe("" + hstMerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert()));
					hstcsvzeile.append(";");
	
					hstExcelwerte.put("lon", new Excelfeld(de.nvbw.bfrk.base.Excelfeld.Datentyp.Text, 
						ExportNachEYEvisObjektpruefung.Textausgabe("" + hstMerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert()), 
						ExportNachEYEvisObjektpruefung.okFarbstyle));
					hstcsvzeile.append(ExportNachEYEvisObjektpruefung.Textausgabe("" + hstMerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert()));
	
					schreibeExcelZeile(sheetname, naechsteZeilennrMap.get(sheetname), hstExcelwerte);
					naechsteZeilennrMap.put(sheetname, 1 + naechsteZeilennrMap.get(sheetname));
					haltestellenobjektVorhanden = true;

					eyevisCSVOutput.append(hstcsvzeile.toString() + "\r\n");
				}
			
			} // Ende über alle Haltstellenobjekte (normalerweise genau 1)


/*			String eyevisCSVDateiname = paramBilderzielverzeichnis + File.separator + hst_dhid.replace(":", "-") + "_" + hst_name_filesystemok + ".csv";
			writetoFile(eyevisCSVOutput, eyevisCSVDateiname, StandardCharsets.ISO_8859_1);

			String eyeviszipdateiname = paramBilderzielverzeichnis + File.separator + hst_dhid.replace(":", "-") + "_" + hst_name_filesystemok + ".zip";
			try {
				zipforEyevis(eyeviszipdateiname, paramBilderzielverzeichnis);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
*/
		}	// Ende über Array der Haltestellen zur Linie
		closeExceldatei(zielexceldatei, true);

		StringBuffer metaoutput = new StringBuffer();

		String eyevisCSVDateiname = paramBilderzielverzeichnis + File.separator
			+ paramAusgabedateiPrefix + ".csv";
		String publiceyevisCSVDateiname = paramBilderzielverzeichnis + File.separator
			+ "public" + File.separator + paramAusgabedateiPrefix + ".csv";
		writetoFile(eyevisCSVOutput, eyevisCSVDateiname, StandardCharsets.ISO_8859_1);

		File dateihandle = new File(eyevisCSVDateiname);
		metaoutput.append("csvdatei=" + dateihandle.getName() + "\r\n");
		metaoutput.append("csvdateigroesse=" + dateihandle.length() + "\r\n");

		String eyeviszipdateiname = paramBilderzielverzeichnis + File.separator + paramAusgabedateiPrefix + ".zip";
		String publiceyevisZipDateiname = paramBilderzielverzeichnis + File.separator 
			+ "public" + File.separator + paramAusgabedateiPrefix + ".zip";
		try {
			zipforEyevis(eyeviszipdateiname, paramBilderzielverzeichnis);
			dateihandle = new File(eyeviszipdateiname);
			metaoutput.append("zipdatei=" + dateihandle.getName() + "\r\n");
			metaoutput.append("zipdateigroesse=" + dateihandle.length() + "\r\n");

			//			File ziphandle = new File(eyeviszipdateiname);
//			if((ziphandle != null) && ziphandle.isFile()) {
//				File zippublichandle = new File(eyeviszippublicdateiname);
//				ziphandle.renameTo(zippublichandle);
//			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		closeDB();

		String metaDateiname = paramBilderzielverzeichnis + File.separator
				+ paramAusgabedateiPrefix + "_auftragsausgabe.txt";
		writetoFile(metaoutput, metaDateiname, StandardCharsets.ISO_8859_1);

		NVBWLogger.info("Verschieben der csv-Projektdatei und der zip-Datei in den public-Zielbereich ...");
		dateihandle = new File(eyevisCSVDateiname);
		dateihandle.renameTo(new File(publiceyevisCSVDateiname));
		NVBWLogger.info("csv-Datei wurde gerade umgenannt, von ...");
		NVBWLogger.info(" von " + eyevisCSVDateiname);
		NVBWLogger.info(" nach" + publiceyevisCSVDateiname);
		try {
			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.OTHERS_READ);
			Files.setPosixFilePermissions(new File(publiceyevisCSVDateiname).toPath(), perms);
		} catch (IOException ioe) {
			System.out.println("Fehler bei Permission-Settings an Datei " + publiceyevisCSVDateiname);
			System.out.println(ioe.toString());
		}
		
		
		dateihandle = new File(eyeviszipdateiname);
		dateihandle.renameTo(new File(publiceyevisZipDateiname));
		NVBWLogger.info("zip-Datei wurde gerade umgenannt, von ...");
		NVBWLogger.info(" von " + eyeviszipdateiname);
		NVBWLogger.info(" nach" + publiceyevisZipDateiname);
		try {
			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			perms.add(PosixFilePermission.GROUP_READ);
			perms.add(PosixFilePermission.OTHERS_READ);
			Files.setPosixFilePermissions(new File(publiceyevisZipDateiname).toPath(), perms);
		} catch (IOException ioe) {
			System.out.println("Fehler bei Permission-Settings an Datei " + publiceyevisZipDateiname);
			System.out.println(ioe.toString());
		}

		NVBWLogger.info("Verschieben der csv-Projektdatei und der zip-Datei in den public-Zielbereich erledigt");

		if(paramObjektIDListe.size() > 0) {
			NVBWLogger.severe("ACHTUNG: es wurden eine oder mehrere ObjektIds angegeben, die nicht verarbeitet wurden, "
				+ "das ist relativ sicher ein Fehler, bitte die folgenden ObjektIds nochmal überprüfen");
			for(Map.Entry<Long, Objektart> paramObjektIDListeEntry: paramObjektIDListe.entrySet()) {
				long aktobjektid = paramObjektIDListeEntry.getKey();
				Objektart aktobjektart = paramObjektIDListeEntry.getValue();
				NVBWLogger.severe("Objektart " + aktobjektart + ": " + aktobjektid);
			}
		}
		
		Date programmende = new Date();
		NVBWLogger.info("Programmende um " + datetime_de_formatter.format(programmende) 
			+ ",   Dauer " + (programmende.getTime() - programmstart.getTime())/1000 + " sek");

		return 0;
	}


	public static void main(String[] args) {

		int returncode = ExportNachEYEvis.execute(args);
		NVBWLogger.info("ExportNachEYEvis.execute ===" + returncode + "===");
	}


	private static void zipforEyevis(String eyeviszipdateiname, String paramBilderzielverzeichnis) throws IOException {

		FileOutputStream fos = new FileOutputStream(eyeviszipdateiname);
		ZipOutputStream zipOut = new ZipOutputStream(fos);


		File verzeichnishandle = new File(paramBilderzielverzeichnis);
		File[] files = verzeichnishandle.listFiles();

		NVBWLogger.info("Start Zippen in Archiv " + eyeviszipdateiname + ", brutto " + files.length + " Dateien ...");

		for (File file : files) {
	        if (file.isDirectory()) {
	        } else {
	            String name = file.getName();
	            if(name.endsWith(".xlsx") || name.endsWith(".jpg")) {
		            ZipEntry zipEntry = new ZipEntry(name);
		            zipOut.putNextEntry(zipEntry);

		            FileInputStream fis = new FileInputStream(file);
		            byte[] bytes = new byte[1024];
		            int length;
		            while((length = fis.read(bytes)) >= 0) {
		                zipOut.write(bytes, 0, length);
		            }
		            fis.close();
		            file.delete();
	            }
	        }
	    }
	    zipOut.close();
	    fos.close();

		NVBWLogger.info("Ende Zippen in Archiv " + eyeviszipdateiname);
	}
}
