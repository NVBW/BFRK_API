package de.nvbw.bfrk.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.nvbw.base.NVBWLogger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;

import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.base.BFRKFeld.Name;
import de.nvbw.bfrk.base.Excelfeld;
import de.nvbw.bfrk.base.Excelfeld.Datentyp;
import de.nvbw.bilddb.imports.BildDBCsvReader;

public class ExportNachEYEvisObjektpruefung {
	private static final Logger LOG = NVBWLogger.getLogger(ExportNachEYEvisObjektpruefung.class);

	final static String EYEvisBilderWurzelverzeichnis = "D:\\EYEvis-Bilder-NBSEI";
	final static String MentzBilderWurzelverzeichnisV1 = "D:\\BFRK-Server\\Bild-DB-Backup\\imagemanagement";
	final static String MentzBilderWurzelverzeichnisV2 = "D:\\BFRK-Server\\Bild-DBV2-Backup\\imagemanagement";
	
	private static final String INTRANET_URL = "http://10.70.190.131:8080";
	private static final String INTERNET_URL = "https://mobidata-bw.de";

	public static enum Bildquellenart {lokal, downloadpublic, downloadprivateundpublic};

	private static boolean bilderkopieren = false;
	private static Workbook workbook; 
	private static String bilderKopierzielverzeichnis = null;
	private static BildDBCsvReader mentzbilddbcsvreader = null;
	private static String eyevisvorlage = "";

	static Map<Name, String> fototitelMap = new HashMap<>();


	private static Bildquellenart bildquelle = Bildquellenart.lokal;
	private static String bilddownloadverzeichnis = "";
	
	public static CellStyle dummyFarbstyle = null;
	public static CellStyle unsicherFarbstyle = null;
	public static CellStyle okFarbstyle = null;

	
	public ExportNachEYEvisObjektpruefung(
		boolean bilderkopieren,
		Bildquellenart bildquellenart, 
		Workbook workbookExceldatei) throws Throwable {

		ExportNachEYEvisObjektpruefung.workbook = workbookExceldatei;
		ExportNachEYEvisObjektpruefung.bilderkopieren = bilderkopieren;

		byte[] rot = hexStringToByteArray("faa29e");
		byte[] gruen = hexStringToByteArray("c3fa9e");
		byte[] gelb = hexStringToByteArray("faf99e");
		Color dummyFarbe = new XSSFColor(rot);
		Color unsicherFarbe = new XSSFColor(gelb);
		Color okFarbe = new XSSFColor(gruen);

		dummyFarbstyle = workbook.createCellStyle();
		//dummyFarbstyle.setFillBackgroundColor(dummyFarbe);
		dummyFarbstyle.setFillForegroundColor(dummyFarbe);
		dummyFarbstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		unsicherFarbstyle = workbook.createCellStyle();
		//unsicherFarbstyle.setFillBackgroundColor(unsicherFarbe);
		unsicherFarbstyle.setFillForegroundColor(unsicherFarbe);
		unsicherFarbstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		
		okFarbstyle = workbook.createCellStyle();
		//okFarbstyle.setFillBackgroundColor(dummyFarbe);
		okFarbstyle.setFillForegroundColor(okFarbe);
		okFarbstyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

		bildquelle = bildquellenart;

		if(		bilderkopieren
			&& 	(bildquelle == Bildquellenart.lokal)) {
			File bilddirhandle = new File(EYEvisBilderWurzelverzeichnis);
			if(	!bilddirhandle.exists() ||
				!bilddirhandle.isDirectory()) {
				LOG.severe("im Konstruktor von ErhebungsObjektpruefung wurde festgestellt, "
					+ "das das EYEvis-Bildverzeichnis nicht verfügbar ist, ABRUCH");
				throw(new Throwable("im Konstruktor von ErhebungsObjektpruefung wurde festgestellt, "
					+ "das das EYEvis-Bildverzeichnis nicht verfügbar ist, ABRUCH"));
			}
		}
	}

	
	public void setBildDownloadverzeichnis(String verzeichnis) {
		bilddownloadverzeichnis = verzeichnis;
	}

	public void setBilderZielverzeichnis(String zielordner) {
		ExportNachEYEvisObjektpruefung.bilderKopierzielverzeichnis = zielordner;
	}
	
	public void setMentzBildMetadaten(BildDBCsvReader bilddbcsvreader) {
		ExportNachEYEvisObjektpruefung.mentzbilddbcsvreader = bilddbcsvreader;
	}

	public void setEYEvisVorlage(String paramEYEvisVorlage) {
		ExportNachEYEvisObjektpruefung.eyevisvorlage = paramEYEvisVorlage;
		
	}

	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}

	public static String Textausgabe(String text) {
		if((text == null) || text.isEmpty())
			return "";

		String output = text;
		output = output.replace(";", ",");
		output = output.replace("\r?\n", "");
		output = output.replace("\"", "'");
		if(!text.equals(output))
			LOG.finest("Textausgabe korrigiert: von '" + text + "' nach '" + output + "'");
		return output;
	}

	public String generateEYEvisLatLon(double lon, double lat) {
		String output = "lat=";
		if(lat != 0.0)
			output += ("" + lat).replace(".", ",");
		else
			output += "0,0";
		output += ";lon=";
		if(lon != 0.0)
			output += ("" + lon).replace(".", ",");
		else
			output += "0,0";

		return output;
	}


	private void addprotokolliereBildNichtDownloadbar(String dhid, String bildname) {
		PrintWriter csvOutput = null;
		try {
			csvOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream("Bilder_nicht_downloadbar.txt",true),StandardCharsets.UTF_8)));
			csvOutput.println(dhid + ";" + bildname);
			csvOutput.close();
		} catch (IOException ioe) {
			LOG.severe("Fehler bei Ausgabe in Datei " + "Bilder_nicht_downloadbar.txt");
		}
	}


	private String downloadBild(String bildurl) {
		final int BUFFER_SIZE = 4096;

		String gespeichertedateipfadundname = "";

		URL url;
		try {
			url = new URL(bildurl);
			LOG.info("Url-Anfrage ===" + bildurl + "=== ...");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", "NVBW OpenDataExport");
			conn.setRequestMethod("GET");
		
			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) conn).getResponseCode();
			LOG.info("" + responseCode); // Should be 200
			// ===================================================================================================================


			long contentlength = 0;
			Integer headeri = 1;
			LOG.info("Header-Fields Ausgabe ...");
			while(((HttpURLConnection) conn).getHeaderFieldKey(headeri) != null) {
				LOG.info("  Header # "+headeri+":  [" 
					+ ((HttpURLConnection) conn).getHeaderFieldKey(headeri)+"] ==="
					+ ((HttpURLConnection) conn).getHeaderField(headeri)+"===");
				if(((HttpURLConnection) conn).getHeaderFieldKey(headeri).equals("Content-Length")) {
					contentlength = Integer.parseInt(((HttpURLConnection) conn).getHeaderField(headeri));
				}
				headeri++;
			}

			if((responseCode == HttpURLConnection.HTTP_OK) && (contentlength > 10000)) {
				LOG.info("ok, http-code 200 und Dateilänge > 10KB ...");
				
				InputStream inputStream = conn.getInputStream();
				String dateiname = bildurl;
				if(dateiname.indexOf("/") != -1)
					dateiname = dateiname.substring(dateiname.lastIndexOf("/") + 1);
				
	            gespeichertedateipfadundname = bilddownloadverzeichnis + File.separator + dateiname;
	            LOG.info("Bild wurde heruntergeladen und wird jetzt gespeichert in: " + gespeichertedateipfadundname);
	            // opens an output stream to save into file
	            FileOutputStream outputStream = new FileOutputStream(gespeichertedateipfadundname);
	 
	            int bytesRead = -1;
	            byte[] buffer = new byte[BUFFER_SIZE];
	            while ((bytesRead = inputStream.read(buffer)) != -1) {
	                outputStream.write(buffer, 0, bytesRead);
	            }
	 
	            outputStream.close();
	            inputStream.close();
	 
	            System.out.println("File downloaded");

			} else {
				
				if(responseCode == HttpURLConnection.HTTP_OK)
					LOG.info("Bild zwar vorhanden, aber Content-Length zu gering: " + contentlength
						+ ", Bild-Url war ===" + bildurl + "===");
				else {
					LOG.info("HTTP-Response Code nicht 200, sondern " + responseCode
						+ ", für Bild-Url ===" + bildurl + "===");
					if(bildurl.indexOf("bfrk_nvbw_intern/") != -1) {
						String dhid = bildurl;
						if(dhid.indexOf("/bilder/") != -1) {
							dhid = dhid.substring(dhid.indexOf("/bilder/") + 8);
							if(dhid.indexOf("/") != -1) {
								dhid = dhid.substring(0, dhid.indexOf("/"));
								String bildname = bildurl;
								bildname = bildname.substring(bildname.lastIndexOf("/") + 1);
								addprotokolliereBildNichtDownloadbar(dhid, bildname);
							}
						}
					}
				}
			}

		} catch (MalformedURLException e) {
			LOG.warning("Bilddatei kann nicht heruntergeladen werden (MalformedURLException)" + "\t"
				+ bildurl + "\t" + e.toString());
		} catch (ProtocolException e) {
			LOG.warning("Bilddatei kann nicht heruntergeladen werden (ProtocolException)" + "\t"
				+ bildurl + "\t" + e.toString());
		} catch (IOException e) {
			LOG.warning("Bilddatei kann nicht heruntergeladen werden (IOException)" + "\t"
				+ "\t" + e.toString());
		}
		return gespeichertedateipfadundname;
	}


	private List<String> getBild(String dhid, String dateiname) {
		List<String> outputListe = new ArrayList<>();

		if(		(bildquelle != Bildquellenart.downloadpublic)
			&&	(bildquelle != Bildquellenart.downloadprivateundpublic)) {
			LOG.severe("Methode getBild wurde aufgerufen, aber bildquellenart ist nicht für Download eingestellt, daher Abbruc getBild");
			return outputListe;
		}
		
		LOG.fine("Beginn Methode getBildurl, dateiname ==="
			+ dateiname + "===");
		Date startzeit = new Date();

		if((dateiname == null) || dateiname.equals(""))
			return outputListe;
	
			// bei EYEvis kann historisch eine Liste von Dateiname mit , (Komma) getrennt vorkommmen, auf Pipe-Zeichen ändern
		if(dateiname.indexOf(",") != -1)
			dateiname = dateiname.replace(",","|");
	
			// in dateinamen können auch mehrere Fotos, per | (Pipe) getrennt, vorkommen
		String[] dateinamenliste = dateiname.split("\\|",-1);
	
		
			// Verarbeitung aller Dateinamen (ggfs. mehrere)
		for(int bildindex = 0; bildindex < dateinamenliste.length; bildindex++) {
			String aktdateiname = dateinamenliste[bildindex];
		
			String bildurl = INTRANET_URL + "/bfrk/haltestelle/bilder/" + dhid + "/" + aktdateiname;

			String bildpfadundname = downloadBild(bildurl);
			if(!bildpfadundname.equals("")) {

				File bildhandle = new File(bildpfadundname);
				if(bildhandle.exists()) {
					outputListe.add(bildpfadundname);
					continue;
				}
			} else {
				if(bildquelle != Bildquellenart.downloadprivateundpublic) {
					LOG.warning("Es wird in getBild nicht nach intern gespeichertem Bild gesucht, weil die Einstellung für Bildquelle nur public vorsieht");
					continue;
				}

				if(bildurl.indexOf("/bfrk/haltestelle") == -1) {
					LOG.severe("Fehler in getBild, Url hat nicht /bfrk/haltestelle, da wurde was verändert und nicht im Programmcode angepasst: "
						+ bildurl);
					continue;
				}
				bildurl = bildurl.replace("/bfrk/haltestelle", "/bfrk_nvbw_intern/haltestelle");

				bildpfadundname = downloadBild(bildurl);
				if(!bildpfadundname.equals("")) {

					File bildhandle = new File(bildpfadundname);
					if(bildhandle.exists()) {
						outputListe.add(bildpfadundname);
						continue;
					}
				} else {
					if(bildurl.indexOf("/bfrk/haltestelle") != -1)
						bildurl = bildurl.replace("/bfrk/haltestelle", "/bfrk_nvbw_intern/haltestelle");
					System.out.println("offenbar noch nicht publik: " + bildurl);
				}
			}

		}
		Date endzeit = new Date();
		LOG.info("Ende Methode getBildurl, Dauer in msek: "
			+ (endzeit.getTime() - startzeit.getTime()));

		return outputListe;
	}

	public void copyBild(String dhid, String bildname) {
		List<String> bildListe = new ArrayList<>();

		List<String> bildpfadundnamenListe = null;
		if(		(bildquelle == Bildquellenart.downloadprivateundpublic)
			||	(bildquelle == Bildquellenart.downloadpublic))
			bildpfadundnamenListe = getBild(dhid, bildname);
		if((bildpfadundnamenListe == null) || (bildpfadundnamenListe.isEmpty())) {
			return;
		}

		for(int bildnameindex = 0; bildnameindex < bildpfadundnamenListe.size(); bildnameindex++) {
			String aktname = bildpfadundnamenListe.get(bildnameindex);
			
			if(aktname.equals(""))
				continue;
		
			if(ExportNachEYEvisObjektpruefung.bilderkopieren && (bilderKopierzielverzeichnis != null)) {
				String zielbilddateiname = bilderKopierzielverzeichnis + File.separator 
					+ aktname.substring(aktname.lastIndexOf(File.separator) + 1);
				Path quellpath = (Path)Paths.get(aktname);
				Path zielpath = (Path)Paths.get(zielbilddateiname);
				try {
					Files.copy(quellpath, zielpath, StandardCopyOption.REPLACE_EXISTING);
					bildListe.add(aktname);
				} catch (IOException e) {
					LOG.warning("Datei-Copy fehlgeschlagen: Quelle: "
						+ quellpath.toString() + ", Ziel: " + zielpath.toString());
					e.printStackTrace();
				}
			} else {
				bildListe.add(aktname);
			}
		}
	}


		// =============================================================
		//    Bahnhof    (nicht Bus-Haltestelle !!)
		// =============================================================

	public Map<String, Excelfeld> generateBahnhof(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}

		felder.put("Objekt", new Excelfeld(Datentyp.Text, 
			"0", okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.HST_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		} else {
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

		
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Bahnhof", 
			okFarbstyle));


		if(objektDaten.containsKey(BFRKFeld.Name.HST_Totale_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Totale_Foto).getTextWert();
			felder.put("HST_Totale_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_SitzeOderUnterstand_Art)) {
			String art = objektDaten.get(BFRKFeld.Name.HST_SitzeOderUnterstand_Art).getTextWert();
			felder.put("HST_SitzeoderUnterstand_Auswahl", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche)) {
			String art = objektDaten.get(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert();
			felder.put("HST_Unterstand_Flaeche_Rollstuhlfahrer", new Excelfeld(Datentyp.Text, 
				art, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_SitzeoderUnterstand_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_SitzeoderUnterstand_Foto).getTextWert();
			felder.put("HST_SitzeoderUnterstand_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_SitzeoderUnterstand_Umgebung_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_SitzeoderUnterstand_Umgebung_Foto).getTextWert();
			felder.put("HST_SitzeoderUnterstand_Umgebung_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130).getBooleanWert();

			felder.put("HST_Fahrplananzeigetafel_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Fahrplananzeigetafel_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Fahrplananzeigetafel_Foto).getTextWert();
			felder.put("HST_Fahrplananzeigetafel_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Akustisch_D1131)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Akustisch_D1131).getBooleanWert();
			felder.put("HST_Fahrplananzeigetafel_Akustisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Ansagen_Vorhanden_D1150)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Ansagen_Vorhanden_D1150).getBooleanWert();
			felder.put("HST_Ansagenautomatisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Defi_vorhanden)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Defi_vorhanden).getBooleanWert();
			felder.put("HST_Defi_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Defi_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Defi_Foto).getTextWert();
			felder.put("HST_Defi_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Defi_Lagebeschreibung)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_Defi_Lagebeschreibung).getTextWert();
			felder.put("HST_Defi_Lageschreibung", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Vorhanden_D1090)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Vorhanden_D1090).getBooleanWert();
			felder.put("HST_Gepaeckaufbewahrung_jn_DLF1090", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Foto).getTextWert();
			felder.put("HST_Gepaeckaufbewahrung_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.HST_Gepaecktransport_Vorhanden_D1100)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Gepaecktransport_Vorhanden_D1100).getBooleanWert();
			felder.put("HST_Gepaecktransport_jn_DLF1100", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160).getBooleanWert();
			felder.put("HST_InduktHoeranlage_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InduktiveHoeranlage_Standort_D1161)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Standort_D1161).getTextWert();
			felder.put("HST_InduktHoeranlage_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule)) {
			String art = objektDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert();
			felder.put("HST_InfoNotrufsaeule_Auswahl", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule_Foto).getTextWert();
			felder.put("HST_InfoNotrufsaeule_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.HST_Mission_vorhanden)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Mission_vorhanden).getBooleanWert();
			felder.put("HST_Mission_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Mission_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Mission_Foto).getTextWert();
			felder.put("HST_Mission_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Mission_Oeffnungszeiten_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Mission_Oeffnungszeiten_Foto).getTextWert();
			felder.put("HST_Mission_Oeffnungszeiten_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_sonstige_Bildanzahl)) {
			int anzahlbilder = objektDaten.get(BFRKFeld.Name.HST_sonstige_Bildanzahl).getIntWert();
			String outputWert = "";
			if(anzahlbilder== 0)
				outputWert = "nein";
			else if(anzahlbilder == 1)
				outputWert = "foto1";
			else if(anzahlbilder == 2)
				outputWert = "foto2";
			else if(anzahlbilder == 3)
				outputWert = "foto3";
			else {
				LOG.warning("in generateBahnhof, Merkmal HST_sonstige_Bildanzahl: "
					+ "Wert unerwartet ===" + anzahlbilder + "===");
			}
			felder.put("HST_weitereBilder_Auswahl", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_weitereBilder1_Foto).getTextWert();
			felder.put("HST_weitereBilder1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder_Foto1_Kommentar)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_weitereBilder_Foto1_Kommentar).getTextWert();
			felder.put("HST_weitereBilder1_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_weitereBilder2_Foto).getTextWert();
			felder.put("HST_weitereBilder2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder_Foto2_Kommentar)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_weitereBilder_Foto2_Kommentar).getTextWert();
			felder.put("HST_weitereBilder2_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder3_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_weitereBilder3_Foto).getTextWert();
			felder.put("HST_weitereBilder3_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder_Foto3_Kommentar)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_weitereBilder_Foto3_Kommentar).getTextWert();
			felder.put("HST_weitereBilder3_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		return felder;
	}


		// =============================================================
		//    Bahnsteig
		// =============================================================

	public Map<String, Excelfeld> generateBahnsteig(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();
		
		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, 
				okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		} else {
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		String latlonEYEvisText = generateEYEvisLatLon(lon, lat);
		felder.put("BSTG_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

		
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Bahnsteig", okFarbstyle));


		if(objektDaten.containsKey(BFRKFeld.Name.STG_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Foto).getTextWert();
			felder.put("BSTG_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_2_Foto).getTextWert();
			felder.put("BSTG_2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}


		if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenbelag_Art)) {
			String belag = objektDaten.get(BFRKFeld.Name.STG_Bodenbelag_Art).getTextWert();
			felder.put("BSTG_Bodenbelag", new Excelfeld(Datentyp.Text, 
				belag, okFarbstyle));
		}

		boolean gegenueberVorhanden = false;
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Gegenueber_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Gegenueber_Foto).getTextWert();
			gegenueberVorhanden = true;
			felder.put("BSTG_Gegenueber_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		felder.put("BSTG_Gegenueber_jn", new Excelfeld(Datentyp.Text, 
			"" + gegenueberVorhanden, okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Steigbreite_cm_D1180)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Steigbreite_cm_D1180).getZahlWert();
			felder.put("BSTG_Steigbreite_cm_DLF1180", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Breite_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Breite_Foto).getTextWert();
			felder.put("BSTG_Breite_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_SteigbreiteMinimum)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_SteigbreiteMinimum).getZahlWert();
			felder.put("BSTG_lichteBreite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Engstelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Engstelle_Foto).getTextWert();
			felder.put("BSTG_Engstelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Laengsneigung)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Laengsneigung).getZahlWert();
			felder.put("BSTG_Laengsneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Querneigung)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Querneigung).getZahlWert();
			felder.put("BSTG_Querneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Vorhanden_D2070)) {
			boolean indkatorvorhanden = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Vorhanden_D2070).getBooleanWert();
			felder.put("BSTG_Bodenindikatoren_jn_DLF2070", new Excelfeld(Datentyp.Standard, 
				"" + indkatorvorhanden, okFarbstyle));

			if(indkatorvorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich).getBooleanWert();
					felder.put("BSTG_Bodenindikator_Einstiegsbereich_DLF2071", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, okFarbstyle));
				}

				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich_Foto).getTextWert();
					felder.put("BSTG_Einstieg_Indikator_Foto", new Excelfeld(Datentyp.Text, 
						dateiname, okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_D2072)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_D2072).getBooleanWert();
					felder.put("BSTG_Steigleitstreifen_DLF2072", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, okFarbstyle));
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_Foto).getTextWert();
					felder.put("BSTG_Bodenindikator_Leitstreifen_Foto", new Excelfeld(Datentyp.Text, 
						dateiname, okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		} else {
			felder.put("BSTG_Bodenindikatoren_jn_DLF2070", new Excelfeld(Datentyp.Standard, 
					"" + false, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_SitzeOderUnterstand_Art)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_SitzeOderUnterstand_Art).getTextWert();
			felder.put("BSTG_SitzeoderUnterstand_Auswahl", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert();
			felder.put("BSTG_Unterstand_Flaeche_Rollstuhlfahrer", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_SitzeoderUnterstand_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_SitzeoderUnterstand_Foto).getTextWert();
			felder.put("BSTG_SitzeoderUnterstand_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_WaendebisBodennaehe_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Unterstand_WaendebisBodennaehe_jn).getBooleanWert();
			felder.put("BSTG_Unterstand_WaendebisBodennaehe_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_Kontrastelemente_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Unterstand_Kontrastelemente_jn).getBooleanWert();
			felder.put("BSTG_Unterstand_Kontrastelemente_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Bahnsteig_Sitzplaetz_Summe)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Bahnsteig_Sitzplaetz_Summe).getZahlWert();
			felder.put("BSTG_Unterstand_Sitzplaetze_Steigsumme", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo).getTextWert();
			felder.put("BSTG_Fahrgastinfo", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_inHoehe_100_160cm)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_inHoehe_100_160cm).getBooleanWert();
			if(eyevisvorlage.startsWith("DING")) {
				if(wertBoolean)
					felder.put("DING_STG_Fahrgastinfo_inHoehe_100_160cm_Liste", new Excelfeld(Datentyp.Standard, 
						"alle", okFarbstyle));
				else
					felder.put("DING_STG_Fahrgastinfo_inHoehe_100_160cm_Liste", new Excelfeld(Datentyp.Standard, 
						"keine" + wertBoolean, okFarbstyle));
					
			} else
				felder.put("BSTG_Fahrgastinfo_inHoehe_100_160cm", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_freierreichbar_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_freierreichbar_jn).getBooleanWert();
			if(eyevisvorlage.startsWith("DING")) {
				if(wertBoolean)
					felder.put("DING_STG_Fahrgastinfo_freierreichbar_Liste", new Excelfeld(Datentyp.Standard, 
						"alle", okFarbstyle));
				else
					felder.put("DING_STG_Fahrgastinfo_freierreichbar_Liste", new Excelfeld(Datentyp.Standard, 
						"keine" + wertBoolean, okFarbstyle));
					
			} else
				felder.put("BSTG_Fahrgastinfo_freierreichbar_jn", new Excelfeld(Datentyp.Standard, 
					"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_nichtbarrierefrei_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_nichtbarrierefrei_Foto).getTextWert();
			felder.put("BSTG_Fahrgastinfo_nichtbarrierefrei_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_D1040)) {
			boolean automatVorhanden = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_D1040).getBooleanWert();
			felder.put("BSTG_Fahrkartenautomat_jn", new Excelfeld(Datentyp.Standard, 
				"" + automatVorhanden, okFarbstyle));

			if(automatVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Foto).getTextWert();
					felder.put("BSTG_Fahrkartenautomat_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}

				lon = 0.0;
				lat = 0.0;
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Lat).getZahlWert();
				}
				latlonEYEvisText = generateEYEvisLatLon(lon, lat);
				felder.put("BSTG_Fahrkartenautomat_Pos", new Excelfeld(Datentyp.Text, 
					latlonEYEvisText, okFarbstyle));

				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_ID)) {
					String outputWert = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_ID).getTextWert();
					felder.put("BSTG_Fahrkartenautomat_ID", new Excelfeld(Datentyp.Text, 
						outputWert, okFarbstyle));
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140)) {
			boolean fahrtanzeigerVorhanden = objektDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140).getBooleanWert();
			felder.put("BSTG_dynFahrtzielanzeiger_jn", new Excelfeld(Datentyp.Standard, 
				"" + fahrtanzeigerVorhanden, okFarbstyle));

			if(fahrtanzeigerVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Akustisch_D1141)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Akustisch_D1141).getBooleanWert();
					felder.put("BSTG_dynFahrtzielanzeiger_Akustisch_jn", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, okFarbstyle));
				}
				
				if(objektDaten.containsKey(BFRKFeld.Name.STG_dynFahrtzielanzeiger_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_dynFahrtzielanzeiger_Foto).getTextWert();
					felder.put("BSTG_dynFahrtzielanzeiger_Foto", new Excelfeld(Datentyp.Text, 
						dateiname, okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Steiglaenge)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Steiglaenge).getZahlWert();
			felder.put("BSTG_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Abfallbehaelter)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Abfallbehaelter).getBooleanWert();
			felder.put("BSTG_Abfallbehaelter", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Bahnsteig_Uhr_vorhanden)) {
			boolean uhrVorhanden = objektDaten.get(BFRKFeld.Name.STG_Bahnsteig_Uhr_vorhanden).getBooleanWert();
			felder.put("BSTG_Uhr_vorhanden", new Excelfeld(Datentyp.Standard, 
				"" + uhrVorhanden, okFarbstyle));
			
			if(uhrVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Uhr_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Uhr_Foto).getTextWert();
					felder.put("BSTG_Uhr_Foto", new Excelfeld(Datentyp.Text, 
						dateiname, okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beleuchtung)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Beleuchtung).getTextWert();
			felder.put("BSTG_Beleuchtung_Auswahl", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Ansagen_Vorhanden_D1150)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Ansagen_Vorhanden_D1150).getBooleanWert();
			felder.put("BSTG_Ansagenautomatisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_InfoNotrufsaeule)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_InfoNotrufsaeule).getTextWert();
			felder.put("BSTG_InfoNotrufsaeule_Auswahl", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_InfoNotrufsaeule_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_InfoNotrufsaeule_Foto).getTextWert();
			felder.put("BSTG_InfoNotrufsaeule_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_vorhanden_D1210)) {
			boolean einstiegsrampeVorhanden = objektDaten.get(BFRKFeld.Name.STG_Einstiegrampe_vorhanden_D1210).getBooleanWert();
			felder.put("BSTG_Einstiegrampe_jn", new Excelfeld(Datentyp.Standard, 
				"" + einstiegsrampeVorhanden, okFarbstyle));

			if(einstiegsrampeVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_Laenge_cm_D1211)) {
					double wert = objektDaten.get(BFRKFeld.Name.STG_Einstiegrampe_Laenge_cm_D1211).getZahlWert();
					felder.put("BSTG_Einstiegrampe_Laenge_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
						wert, unsicherFarbstyle));
				}

				if(objektDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_Tragfaehigkeit_kg_D1212)) {
					double wert = objektDaten.get(BFRKFeld.Name.STG_Einstiegrampe_Tragfaehigkeit_kg_D1212).getZahlWert();
					felder.put("BSTG_Einstiegrampe_Tragfaehigkeit_kg", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
						wert, unsicherFarbstyle));
				}

				if(objektDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Einstiegrampe_Foto).getTextWert();
					felder.put("BSTG_Einstiegrampe_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						unsicherFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_vorhanden_D1220)) {
			boolean einstieghubliftVorhanden = objektDaten.get(BFRKFeld.Name.STG_EinstiegHublift_vorhanden_D1220).getBooleanWert();
			felder.put("BSTG_EinstiegHublift_jn", new Excelfeld(Datentyp.Standard, 
				"" + einstieghubliftVorhanden, okFarbstyle));

			if(einstieghubliftVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_Laenge_cm_D1221)) {
					double wert = objektDaten.get(BFRKFeld.Name.STG_EinstiegHublift_Laenge_cm_D1221).getZahlWert();
					felder.put("BSTG_EinstiegHublift_LaengeStellflaeche_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
						wert, unsicherFarbstyle));
				}

				if(objektDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_Tragfaehigkeit_kg_D1222)) {
					double wert = objektDaten.get(BFRKFeld.Name.STG_EinstiegHublift_Tragfaehigkeit_kg_D1222).getZahlWert();
					felder.put("BSTG_EinstiegHublift_Tragfaehigkeit_kg", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
						wert, unsicherFarbstyle));
				}

				if(objektDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_EinstiegHublift_Foto).getTextWert();
					felder.put("BSTG_EinstiegHublift_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						unsicherFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		}

//			BSTG_Abschnitt_jn	BSTG_Abschnitt_Foto	BSTG_Abschnitt2_jn	BSTG_Abschnitt2_Foto	BSTG_Abschnitt3_jn	BSTG_Abschnitt3_Foto	BSTG_Abschnitt4_jn	BSTG_Abschnitt4_Foto	
//			BSTG_Abschnitt5_jn	BSTG_Abschnitt5_Foto	BSTG_Abschnitt6_jn	BSTG_Abschnitt6_Foto	BSTG_Abschnitt7_jn	BSTG_Abschnitt7_Foto	BSTG_Abschnitt8_jn	BSTG_Abschnitt8_Foto
		
//			BSTG_Haltepunkt_jn	BSTG_Haltepunkt_Foto	BSTG_Haltepunkt_Pos	BSTG_Haltepunkt2_jn	BSTG_Haltepunkt2_Foto	BSTG_Haltepunkt2_Pos	
//			BSTG_Haltepunkt3_jn	BSTG_Haltepunkt3_Foto	BSTG_Haltepunkt3_Pos	BSTG_Haltepunkt4_jn	BSTG_Haltepunkt4_Foto	BSTG_Haltepunkt4_Pos	

//			BSTG_Notiz	
		
		
		return felder;
	}


		// =============================================================
		//    Bus-Haltestelle
		// =============================================================
	
	public Map<String, Excelfeld> generateHaltestelle(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();
	
		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				unsicherFarbstyle));
		}

		felder.put("Objekt", new Excelfeld(Datentyp.Text, 
			"0", 
			unsicherFarbstyle));

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				unsicherFarbstyle));
		} else {
			if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
				felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
					"HST: " + Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
					unsicherFarbstyle));
			}
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat)) {
			felder.put("lat", new Excelfeld(Datentyp.Text, 
				"" + haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert(), 
				unsicherFarbstyle));
		}
		
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon)) {
			felder.put("lon", new Excelfeld(Datentyp.Text, 
				"" + haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert(), 
				unsicherFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				unsicherFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				unsicherFarbstyle));
		}

		
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Haltestelle", 
			okFarbstyle));


		if(objektDaten.containsKey(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130).getBooleanWert();
			felder.put("HST_Fahrplananzeigetafel_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Akustisch_D1131)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Akustisch_D1131).getBooleanWert();
			felder.put("HST_Fahrplananzeigetafel_Akustisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Fahrplananzeigetafel_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Fahrplananzeigetafel_Foto).getTextWert();
			felder.put("HST_Fahrplananzeigetafel_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Ansagen_Vorhanden_D1150)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Ansagen_Vorhanden_D1150).getBooleanWert();
			felder.put("HST_Ansagenautomatisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_SitzeOderUnterstand_Art)) {
			String art = objektDaten.get(BFRKFeld.Name.HST_SitzeOderUnterstand_Art).getTextWert();
			felder.put("HST_SitzeoderUnterstand_Auswahl", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche)) {
			String art = objektDaten.get(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert();
				// Mentz-Merkmalwert unterschied nicht zw. vor Infokasten oder frei, daher schlechtere Wahl nehmen
			if(art.equals("Flaeche_1m5_1m5_vorhanden"))
				art = "Flaeche_1m5_1m5_vorhanden_vorInfokasten";
			felder.put("HST_Unterstand_Flaeche_Rollstuhlfahrer", new Excelfeld(Datentyp.Text, 
				art,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_SitzeoderUnterstand_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_SitzeoderUnterstand_Foto).getTextWert();
			felder.put("HST_SitzeoderUnterstand_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_SitzeoderUnterstand_Umgebung_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_SitzeoderUnterstand_Umgebung_Foto).getTextWert();
			felder.put("HST_SitzeoderUnterstand_Umgebung_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Defi_vorhanden)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_Defi_vorhanden).getBooleanWert();
			felder.put("HST_Defi_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Defi_Lagebeschreibung)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_Defi_Lagebeschreibung).getTextWert();
			felder.put("HST_Defi_Lageschreibung", new Excelfeld(Datentyp.Text, 
				outputWert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_Defi_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_Defi_Foto).getTextWert();
			felder.put("HST_Defi_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160).getBooleanWert();
			felder.put("HST_InduktHoeranlage_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InduktiveHoeranlage_Standort_D1161)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Standort_D1161).getTextWert();
			felder.put("HST_InduktHoeranlage_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule)) {
			String art = objektDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert();
			felder.put("HST_InfoNotrufsaeule_Auswahl", new Excelfeld(Datentyp.Text, 
				art,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule_Foto).getTextWert();
			felder.put("HST_InfoNotrufsaeule_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		String weitereBilder = "nein";
		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder1_Foto)) {
			weitereBilder = "foto1";
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_weitereBilder1_Foto).getTextWert();
			felder.put("HST_weitereBilder1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);

			if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder_Foto1_Kommentar)) {
				String outputWert = objektDaten.get(BFRKFeld.Name.HST_weitereBilder_Foto1_Kommentar).getTextWert();
				felder.put("HST_weitereBilder1_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
					outputWert,
					unsicherFarbstyle));
			}
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder2_Foto)) {
			weitereBilder = "foto2";
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_weitereBilder2_Foto).getTextWert();
			felder.put("HST_weitereBilder2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);

			if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder_Foto2_Kommentar)) {
				String outputWert = objektDaten.get(BFRKFeld.Name.HST_weitereBilder_Foto2_Kommentar).getTextWert();
				felder.put("HST_weitereBilder2_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
					outputWert,
					unsicherFarbstyle));
			}
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder3_Foto)) {
			weitereBilder = "foto3";
			String dateiname = objektDaten.get(BFRKFeld.Name.HST_weitereBilder3_Foto).getTextWert();
			felder.put("HST_weitereBilder3_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);

			if(objektDaten.containsKey(BFRKFeld.Name.HST_weitereBilder_Foto3_Kommentar)) {
				String outputWert = objektDaten.get(BFRKFeld.Name.HST_weitereBilder_Foto3_Kommentar).getTextWert();
				felder.put("HST_weitereBilder3_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
					outputWert,
					unsicherFarbstyle));
			}
		}

		felder.put("HST_weitereBilder_Auswahl", new Excelfeld(Datentyp.Text, 
			weitereBilder,
			okFarbstyle));


		return felder;
	}


		// =============================================================
		//    Bus-Haltesteig
		// =============================================================

	/*
	 * am 12.05.2025 vollständig codiert, ab jetzt zu prüfen
	 */
	public Map<String, Excelfeld> generateHaltesteig(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();
		
		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
		}

		String steig_dhid = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			steig_dhid = objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat)) {
			felder.put("lat", new Excelfeld(Datentyp.Text, 
				"" + objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert(), 
				okFarbstyle));
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon)) {
			felder.put("lon", new Excelfeld(Datentyp.Text, 
				"" + objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

		
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Steig", 
			okFarbstyle));


		if(objektDaten.containsKey(BFRKFeld.Name.STG_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Foto).getTextWert();
			felder.put("STG_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		} else {
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String latlonEYEvisText = generateEYEvisLatLon(lon, lat);
		felder.put("STG_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, okFarbstyle));

		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Steigtyp)) {
			String typ = objektDaten.get(BFRKFeld.Name.STG_Steigtyp).getTextWert();
			felder.put("STG_ZUS_Steigtyp", new Excelfeld(Datentyp.Text, 
				typ,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_ZUS_sonstigerSteigtyp_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_ZUS_sonstigerSteigtyp_Foto).getTextWert();
			felder.put("STG_ZUS_sonstigerSteigtyp_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Steiglaenge)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Steiglaenge).getZahlWert();
			LOG.info("DHID: " + steig_dhid + ", Merkmal STG_Steiglänge: " + wert);
			felder.put("STG_ZUS_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_ZUS_Buchtlaenge_m)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_ZUS_Buchtlaenge_m).getZahlWert();
			felder.put("STG_ZUS_Buchtlaenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenbelag_Art)) {
			String belag = objektDaten.get(BFRKFeld.Name.STG_Bodenbelag_Art).getTextWert();
			felder.put("STG_Bodenbelag", new Excelfeld(Datentyp.Text, 
				belag,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Hochbord_vorhanden)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Hochbord_vorhanden).getBooleanWert();
			felder.put("STG_Hochbord_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}		
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Hochbord_Art)) {
			String text = objektDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert();
			felder.put("STG_Hochbordart", new Excelfeld(Datentyp.Text, 
				text,
				okFarbstyle));
		}		
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Hochbord_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Hochbord_Foto).getTextWert();
			felder.put("STG_Hochbord_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Steighoehe_cm_D1170)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Steighoehe_cm_D1170).getZahlWert();
			if(wert < 0.0)
				wert = 0.0;
			felder.put("STG_Bordsteinhoehe_cm_DLF1170", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Steigbreite_cm_D1180)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Steigbreite_cm_D1180).getZahlWert();
			felder.put("STG_Steigbreite_cm_DLF1180", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Breite_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Breite_Foto).getTextWert();
			felder.put("STG_Breite_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_SteigbreiteMinimum)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_SteigbreiteMinimum).getZahlWert();
			felder.put("STG_lichteBreite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Engstelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Engstelle_Foto).getTextWert();
			felder.put("STG_Engstelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Laengsneigung)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Laengsneigung).getZahlWert();
			felder.put("STG_Laengsneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Querneigung)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Querneigung).getZahlWert();
			felder.put("STG_Querneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Vorhanden_D2070)) {
			boolean indkatorvorhanden = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Vorhanden_D2070).getBooleanWert();
			felder.put("STG_Bodenindikatoren_jn_DLF2070", new Excelfeld(Datentyp.Standard, 
				"" + indkatorvorhanden, 
				okFarbstyle));

			if(indkatorvorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich).getBooleanWert();
					felder.put("STG_Bodenindikator_Einstiegsbereich_DLF2071", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, 
						okFarbstyle));
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich_Foto).getTextWert();
					felder.put("STG_Einstieg_Indikator_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen).getBooleanWert();
					felder.put("STG_Bodenindikator_Auffindestreifen", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, 
						unsicherFarbstyle));
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen_Foto).getTextWert();
					felder.put("STG_Bodenindikator_Auffindestreifen_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_D2072)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_D2072).getBooleanWert();
					felder.put("STG_Steigleitstreifen_DLF2072", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, 
						okFarbstyle));
				}
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_Foto).getTextWert();
					felder.put("STG_Bodenindikator_Leitstreifen_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						okFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Tuer2_Laenge_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Tuer2_Laenge_cm).getZahlWert();
			felder.put("STG_Tuer2_Laenge_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Tuer2_Breite_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Tuer2_Breite_cm).getZahlWert();
			felder.put("STG_Tuer2_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_SitzeOderUnterstand_Art)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_SitzeOderUnterstand_Art).getTextWert();
			felder.put("STG_SitzeoderUnterstand_Auswahl", new Excelfeld(Datentyp.Text, 
				wert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert();
				// Mentz-Merkmalwert unterschied nicht zw. vor Infokasten oder frei, daher schlechtere Wahl nehmen
			if(wert.equals("Flaeche_1m5_1m5_vorhanden"))
				wert = "Flaeche_1m5_1m5_vorhanden_vorInfokasten";
			felder.put("STG_Unterstand_Flaeche_Rollstuhlfahrer", new Excelfeld(Datentyp.Text, 
				wert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_WaendebisBodennaehe_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Unterstand_WaendebisBodennaehe_jn).getBooleanWert();
			felder.put("STG_ZUS_Unterstand_WaendebisBodennaehe_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_Kontrastelemente_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Unterstand_Kontrastelemente_jn).getBooleanWert();
			felder.put("STG_ZUS_Unterstand_Kontrastelemente_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Unterstand_offiziell_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Unterstand_offiziell_jn).getBooleanWert();
			felder.put("STG_ZUS_Unterstand_offiziell_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_ZUS_Unterstandnichtofiziell_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_ZUS_Unterstandnichtofiziell_Foto).getTextWert();
			felder.put("STG_ZUS_Unterstandnichtofiziell_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_SitzeoderUnterstand_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_SitzeoderUnterstand_Foto).getTextWert();
			felder.put("STG_SitzeoderUnterstand_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Haltestellenmast_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Haltestellenmast_Foto).getTextWert();
			felder.put("STG_ZUS_Haltestellenmast_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo).getTextWert();
			felder.put("STG_Fahrgastinfo", new Excelfeld(Datentyp.Text, 
				wert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_inHoehe_100_160cm)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_inHoehe_100_160cm).getBooleanWert();
			felder.put("STG_Fahrgastinfo_inHoehe_100_160cm", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_freierreichbar_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_freierreichbar_jn).getBooleanWert();
			felder.put("STG_Fahrgastinfo_freierreichbar_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_nichtbarrierefrei_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_nichtbarrierefrei_Foto).getTextWert();
			felder.put("STG_Fahrgastinfo_nichtbarrierefrei_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beleuchtung)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Beleuchtung).getTextWert();
			felder.put("STG_ZUS_Beleuchtung_Auswahl", new Excelfeld(Datentyp.Text, 
				wert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Abfallbehaelter)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Abfallbehaelter).getBooleanWert();
			felder.put("STG_ZUS_Abfallbehaelter", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Lageinnerorts)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Lageinnerorts).getTextWert();
			felder.put("STG_ZUS_Lageinnerorts", new Excelfeld(Datentyp.Text, 
				wert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Gegenueber_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Gegenueber_Foto).getTextWert();
			felder.put("STG_Gegenueber_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_D1040)) {
			boolean automatVorhanden = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_D1040).getBooleanWert();
			felder.put("STG_Fahrkartenautomat_jn", new Excelfeld(Datentyp.Standard, 
				"" + automatVorhanden, 
				unsicherFarbstyle));

			if(automatVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Foto).getTextWert();
					felder.put("STG_Fahrkartenautomat_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						unsicherFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}

				lon = 0.0;
				lat = 0.0;
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Lat).getZahlWert();
				}
				latlonEYEvisText = generateEYEvisLatLon(lon, lat);
				felder.put("STG_Fahrkartenautomat_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, unsicherFarbstyle));

				if(objektDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_ID)) {
					String outputWert = objektDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_ID).getTextWert();
					felder.put("STG_Fahrkartenautomat_ID", new Excelfeld(Datentyp.Text, 
						outputWert,
						unsicherFarbstyle));
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140)) {
			boolean fahrtanzeigerVorhanden = objektDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140).getBooleanWert();
			felder.put("STG_dynFahrtzielanzeiger_jn", new Excelfeld(Datentyp.Standard, 
				"" + fahrtanzeigerVorhanden, 
				okFarbstyle));

			if(fahrtanzeigerVorhanden) {
				if(objektDaten.containsKey(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Akustisch_D1141)) {
					boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Akustisch_D1141).getBooleanWert();
					felder.put("STG_dynFahrtzielanzeiger_Akustisch_jn", new Excelfeld(Datentyp.Standard, 
						"" + wertBoolean, 
						okFarbstyle));
				}
				
				if(objektDaten.containsKey(BFRKFeld.Name.STG_dynFahrtzielanzeiger_Foto)) {
					String dateiname = objektDaten.get(BFRKFeld.Name.STG_dynFahrtzielanzeiger_Foto).getTextWert();
					felder.put("STG_dynFahrtzielanzeiger_Foto", new Excelfeld(Datentyp.Text, 
						dateiname,
						unsicherFarbstyle));
					if(ExportNachEYEvisObjektpruefung.bilderkopieren)
						copyBild(hst_dhid, dateiname);
				}
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_ZUS_Uhr_jn)) {
			boolean uhrVorhanden = objektDaten.get(BFRKFeld.Name.STG_ZUS_Uhr_jn).getBooleanWert();
			felder.put("STG_ZUS_Uhr_vorhanden", new Excelfeld(Datentyp.Standard, 
				"" + uhrVorhanden, 
				okFarbstyle));
			
			if(objektDaten.containsKey(BFRKFeld.Name.STG_Uhr_Foto)) {
				String dateiname = objektDaten.get(BFRKFeld.Name.STG_Uhr_Foto).getTextWert();
				felder.put("STG_ZUS_Uhr_Foto", new Excelfeld(Datentyp.Text, 
					dateiname,
					unsicherFarbstyle));
				if(ExportNachEYEvisObjektpruefung.bilderkopieren)
					copyBild(hst_dhid, dateiname);
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Ansagen_Vorhanden_D1150)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Ansagen_Vorhanden_D1150).getBooleanWert();
			felder.put("STG_Ansagenautomatisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_InfoNotrufsaeule)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_InfoNotrufsaeule).getTextWert();
			felder.put("STG_InfoNotrufsaeule_Auswahl", new Excelfeld(Datentyp.Text, 
				wert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_InfoNotrufsaeule_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_InfoNotrufsaeule_Foto).getTextWert();
			felder.put("STG_InfoNotrufsaeule_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

//TODO prüfen, ob Notiz hier vorkommt (wird in BFRK-DB extra gespeichert, nicht in Tabelle Merkmal
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Notiz)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Notiz).getTextWert();
			felder.put("STG_ZUS_Notiz", new Excelfeld(Datentyp.Text, 
				wert,
				okFarbstyle));
		}

			// ----------------------------------------------------------------------
			// Zuweg 1 (= Zuweg von) Merkmale 
			// ----------------------------------------------------------------------
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Zugangstyp)) {
			String zuweg1zugangstyp = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Zugangstyp).getTextWert();
			felder.put("STG_Zuweg1_Zugangstyp", new Excelfeld(Datentyp.Text, 
				zuweg1zugangstyp,
				unsicherFarbstyle));
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_direkt_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_direkt_Foto).getTextWert();
			felder.put("STG_Zuweg1_direkt_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_weniger2Prozent_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_weniger2Prozent_jn).getBooleanWert();
			felder.put("STG_Zuweg1_weniger2Prozent_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_eben_Laenge_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_eben_Laenge_cm).getZahlWert() / 100.0;
			felder.put("STG_Zuweg1_eben_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_eben_Breite_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_eben_Breite_cm).getZahlWert();
			felder.put("STG_Zuweg1_eben_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_eben_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_eben_Foto).getTextWert();
			felder.put("STG_Zuweg1_eben_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Rampe_Laenge_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Rampe_Laenge_cm).getZahlWert() / 100.0;
			felder.put("STG_Zuweg1_Rampe_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Rampe_Breite_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Rampe_Breite_cm).getZahlWert();
			felder.put("STG_Zuweg1_Rampe_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Rampe_Neigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Rampe_Neigung_prozent).getZahlWert();
			felder.put("STG_Zuweg1_Rampe_Neigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Rampe_Querneigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Rampe_Querneigung_prozent).getZahlWert();
			felder.put("STG_Zuweg1_Rampe_Querneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Rampe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Rampe_Foto).getTextWert();
			felder.put("STG_Zuweg1_Rampe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Weg_Stufenhoehe_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Weg_Stufenhoehe_cm).getZahlWert();
			felder.put("STG_Zuweg1_Weg_Stufenhoehe_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Weg_Stufe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Weg_Stufe_Foto).getTextWert();
			felder.put("STG_Zuweg1_Weg_Stufe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Notiz)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_Notiz).getTextWert();
			felder.put("STG_Zuweg1_sonstiges_Notiz", new Excelfeld(Datentyp.Text, 
				wert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_sonstiges_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg1_sonstiges_Foto).getTextWert();
			felder.put("STG_Zuweg1_sonstiges_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}


			// ----------------------------------------------------------------------
			// Zuweg 2 (= Zuweg nach) Merkmale
			// ----------------------------------------------------------------------
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Zugangstyp)) {
			String Zuweg2zugangstyp = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Zugangstyp).getTextWert();
			felder.put("STG_Zuweg2_Zugangstyp", new Excelfeld(Datentyp.Text, 
				Zuweg2zugangstyp,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_direkt_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_direkt_Foto).getTextWert();
			felder.put("STG_Zuweg2_direkt_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_weniger2Prozent_jn)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_weniger2Prozent_jn).getBooleanWert();
			felder.put("STG_Zuweg2_weniger2Prozent_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_eben_Laenge_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_eben_Laenge_cm).getZahlWert() / 100.0;
			felder.put("STG_Zuweg2_eben_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_eben_Breite_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_eben_Breite_cm).getZahlWert();
			felder.put("STG_Zuweg2_eben_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_eben_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_eben_Foto).getTextWert();
			felder.put("STG_Zuweg2_eben_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Rampe_Laenge_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Rampe_Laenge_cm).getZahlWert() / 100.0;
			felder.put("STG_Zuweg2_Rampe_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Rampe_Breite_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Rampe_Breite_cm).getZahlWert();
			felder.put("STG_Zuweg2_Rampe_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Rampe_Neigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Rampe_Neigung_prozent).getZahlWert();
			felder.put("STG_Zuweg2_Rampe_Neigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Rampe_Querneigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Rampe_Querneigung_prozent).getZahlWert();
			felder.put("STG_Zuweg2_Rampe_Querneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Rampe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Rampe_Foto).getTextWert();
			felder.put("STG_Zuweg2_Rampe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Weg_Stufenhoehe_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Weg_Stufenhoehe_cm).getZahlWert();
			felder.put("STG_Zuweg2_Weg_Stufenhoehe_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, unsicherFarbstyle));
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Weg_Stufe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Weg_Stufe_Foto).getTextWert();
			felder.put("STG_Zuweg2_Weg_Stufe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Notiz)) {
			String wert = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_Notiz).getTextWert();
			felder.put("STG_Zuweg2_sonstiges_Notiz", new Excelfeld(Datentyp.Text, 
				wert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_sonstiges_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.STG_Zuweg2_sonstiges_Foto).getTextWert();
			felder.put("STG_Zuweg2_sonstiges_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		return felder;
	}

	
		// =============================================================
		//    Aufzug
		// =============================================================

	public Map<String, Excelfeld> generateAufzug(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

		
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Aufzug", okFarbstyle));

		
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Aufzug_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Foto).getTextWert();
			felder.put("OBJ_Aufzug_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Tuerbreite_cm_D2091)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Tuerbreite_cm_D2091).getZahlWert();
			felder.put("OBJ_Aufzug_Tuerbreite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenlaenge_cm_D2093)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenlaenge_cm_D2093).getZahlWert();
			felder.put("OBJ_Aufzug_Grundflaeche_Laenge_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenbreite_cm_D2094)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenbreite_cm_D2094).getZahlWert();
			felder.put("OBJ_Aufzug_Grundflaeche_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Verbindungsfunktion_D2095)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Verbindungsfunktion_D2095).getTextWert();
			felder.put("OBJ_Aufzug_Verbindung_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_ID_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_ID_Foto).getTextWert();
			felder.put("OBJ_Aufzug_ID_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Bedienelemente_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Bedienelemente_Foto).getTextWert();
			felder.put("OBJ_Aufzug_Bedienelemente_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_StoerungKontakt_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_StoerungKontakt_Foto).getTextWert();
			felder.put("OBJ_Aufzug_StoerungKontakt_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Ebene1_Foto).getTextWert();
			felder.put("OBJ_Aufzug_Ebene1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Ebene2_Foto).getTextWert();
			felder.put("OBJ_Aufzug_Ebene2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene3_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Aufzug_Ebene3_Foto).getTextWert();
			if(!dateiname.isEmpty()) {
				felder.put("OBJ_Aufzug_Ebene3_jn", new Excelfeld(Datentyp.Standard, 
					"" + true, okFarbstyle));
	
				felder.put("OBJ_Aufzug_Ebene3_Foto", new Excelfeld(Datentyp.Text, 
					dateiname,
					unsicherFarbstyle));
				if(ExportNachEYEvisObjektpruefung.bilderkopieren)
					copyBild(hst_dhid, dateiname);
			}
		} else {
			felder.put("OBJ_Aufzug_Ebene3_jn", new Excelfeld(Datentyp.Standard, 
				"" + false, okFarbstyle));
		}

		return felder;
	}


		// =============================================================
		//    BuR (Bike & Ride)
		// =============================================================

	public Map<String, Excelfeld> generateBikeRide(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lon", new Excelfeld(Datentyp.Zahl_Double, lon, okFarbstyle));
		felder.put("lat", new Excelfeld(Datentyp.Zahl_Double, lat, okFarbstyle));

		String latlonEYEvisText = generateEYEvisLatLon(lon, lat);
		felder.put("OBJ_BuR_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}


		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"BuR", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_BuR_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Anlagentyp)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Anlagentyp).getTextWert();
			felder.put("OBJ_BuR_Anlagentyp", new Excelfeld(Datentyp.Text, 
				wert, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Stellplatzanzahl)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Stellplatzanzahl).getZahlWert();
			felder.put("OBJ_BuR_Stellplatzanzahl", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Ueberdacht)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Ueberdacht).getBooleanWert();
			felder.put("OBJ_BuR_ueberdacht_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Beleuchtet)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Beleuchtet).getBooleanWert();
			felder.put("OBJ_BuR_beleuchtet_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Kostenpflichtig)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Kostenpflichtig).getBooleanWert();
			felder.put("OBJ_BuR_kostenpflichtig_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_KostenpflichtigNotiz)) {
			felder.put("OBJ_BuR_Betreiber_oder_Kontakt", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.OBJ_BuR_KostenpflichtigNotiz).getTextWert(), 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Foto).getTextWert();
			felder.put("OBJ_BuR_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Weg_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Weg_Foto).getTextWert();
			felder.put("OBJ_BuR_Weg_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_WegZurAnlageAnfahrbar)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_BuR_WegZurAnlageAnfahrbar).getBooleanWert();
			felder.put("OBJ_BuR_Hindernisfreie_Zufahrt_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Hinderniszufahrt_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Hinderniszufahrt_Foto).getTextWert();
			felder.put("OBJ_BuR_Hinderniszufahrt_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Hinderniszufahrt_Beschreibung)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Hinderniszufahrt_Beschreibung).getTextWert();
			felder.put("OBJ_BuR_Hinderniszufahrt_Beschreibung", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Notiz)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Notiz).getTextWert();
			felder.put("OBJ_BuR_Notizen", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_BuR_Besonderheiten_Foto)) {
			felder.put("OBJ_BuR_Besonderheiten_Foto_jn", new Excelfeld(Datentyp.Standard, 
					"" + true, okFarbstyle));

			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_BuR_Besonderheiten_Foto).getTextWert();
			felder.put("OBJ_BuR_Besonderheiten_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		} else {
			felder.put("OBJ_BuR_Besonderheiten_Foto_jn", new Excelfeld(Datentyp.Standard, 
				"" + false, okFarbstyle));
		}

		return felder;
	}

	
		// =============================================================
		//    Engstelle
		// =============================================================
	
	public Map<String, Excelfeld> generateEngstelle(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Engstelle", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Engstelle_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Engstelle_Durchgangsbreite_cm_D2080)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Engstelle_Durchgangsbreite_cm_D2080).getZahlWert();
			felder.put("OBJ_Engstelle_Durchgangsbreite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Engstelle_Bewegflaeche_cm_D2081)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Engstelle_Bewegflaeche_cm_D2081).getZahlWert();
			felder.put("OBJ_Engstelle_Bewegungsflaeche_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Engstelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Engstelle_Foto).getTextWert();
			felder.put("OBJ_Engstelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Engstelle_Weg1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Engstelle_Weg1_Foto).getTextWert();
			felder.put("OBJ_Engstelle_Weg1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Engstelle_Weg2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Engstelle_Weg2_Foto).getTextWert();
			felder.put("OBJ_Engstelle_Weg2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		return felder;
	}
	
	
		// =============================================================
		//    Fahrkartenautomat
		// =============================================================
	
	public Map<String, Excelfeld> generateFahrkartenautomat(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Lon) &&
				(objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Lat) &&
				(objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lon", new Excelfeld(Datentyp.Zahl_Double, lon, okFarbstyle));
		felder.put("lat", new Excelfeld(Datentyp.Zahl_Double, lat, okFarbstyle));

		String latlonEYEvisText = generateEYEvisLatLon(lon, lat);
		felder.put("OBJ_Kartenautomat_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Fahrkartenautomat", okFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Kartenautomat_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_Foto).getTextWert();
			felder.put("OBJ_Kartenautomat_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_ID)) {
			felder.put("OBJ_Kartenautomat_ID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_ID).getTextWert(), 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Entwerter)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_Entwerter).getBooleanWert();
			felder.put("OBJ_Kartenautomat_TicketValidator_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_TicketValidator_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Kartenautomat_TicketValidator_Foto).getTextWert();
			felder.put("OBJ_Kartenautomat_TicketValidator_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
			// fix am Ende 1. Automat signalisieren, das kein zweiter Automat folgt (wird als extra Objekt bereitgestellt)
		felder.put("OBJ_Kartenautomat2_jn", new Excelfeld(Datentyp.Text, 
			"" + false, 
			okFarbstyle));

		return felder;
	}


		// =============================================================
		//    Gleisquerung
		// =============================================================
	
	public Map<String, Excelfeld> generateGleisquerung(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				unsicherFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, 
				unsicherFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				unsicherFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), 
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				unsicherFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, unsicherFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, unsicherFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				unsicherFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				unsicherFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Gleisquerung", 
			unsicherFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Querung_objektID", new Excelfeld(Datentyp.Text, 
				outputWert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Gleisquerung_Foto).getTextWert();
			felder.put("OBJ_Querung_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Weg1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Gleisquerung_Weg1_Foto).getTextWert();
			felder.put("OBJ_Querung_Weg1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Weg2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Gleisquerung_Weg2_Foto).getTextWert();
			felder.put("OBJ_Querung_Weg2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Verbindungsfunktion)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Gleisquerung_Verbindungsfunktion).getTextWert();
			felder.put("OBJ_Querung_Verbindung_Bereiche", new Excelfeld(Datentyp.Text, 
				outputWert, 
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Breite_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Gleisquerung_Breite_cm).getZahlWert();
			felder.put("OBJ_Querung_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				unsicherFarbstyle));
		}
	
		return felder;
	}


		// =============================================================
		//    Informationsstelle
		// =============================================================
	
	public Map<String, Excelfeld> generateInformationsstelle(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Informationsstelle", okFarbstyle));
		
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Infostelle_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Infostelle_Art_D1031)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_Infostelle_Art_D1031).getTextWert();
			felder.put("OBJ_Infostelle_Art_DLF1031", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Infostelle_Stufenfrei_D1032)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Infostelle_Stufenfrei_D1032).getBooleanWert();
			felder.put("OBJ_Infostelle_stufenfrei_jn_DLF1032", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Infostelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Infostelle_Foto).getTextWert();
			felder.put("OBJ_Infostelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Infostelle_Oeffnungszeiten_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Infostelle_Oeffnungszeiten_Foto).getTextWert();
			felder.put("OBJ_Infostelle_Oeffnungszeiten_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Infostelle_EingangzuInfostelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Infostelle_EingangzuInfostelle_Foto).getTextWert();
			felder.put("OBJ_Infostelle_EingangzuInfostelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Infostelle_WegzuInfostelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Infostelle_WegzuInfostelle_Foto).getTextWert();
			felder.put("OBJ_Infostelle_WegzuInfostelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		return felder;
	}


		// =============================================================
		//    Leihfahrradanlage
		// =============================================================
	
	public Map<String, Excelfeld> generateLeihfahrradanlage(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Leihfahrradanlage", okFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Leihrad_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Art)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Leihradanlage_Art).getTextWert();
			felder.put("OBJ_Leihrad_Ausleihart", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Notizen)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Leihradanlage_Notizen).getTextWert();
			felder.put("OBJ_Leihrad_Beschreibung", new Excelfeld(Datentyp.Standard, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Leihradanlage_Foto).getTextWert();
			felder.put("OBJ_Leihrad_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_lon)) {
			String latlonEYEvisText = generateEYEvisLatLon(
				objektDaten.get(BFRKFeld.Name.OBJ_Leihradanlage_lon).getZahlWert(),
				objektDaten.get(BFRKFeld.Name.OBJ_Leihradanlage_lat).getZahlWert());
			felder.put("OBJ_Leihrad_Pos", new Excelfeld(Datentyp.Text, 
				latlonEYEvisText, okFarbstyle));	
		}

		return felder;
	}


		// =============================================================
		//    PuR (Parkplatz)
		// =============================================================

	public Map<String, Excelfeld> generateParkplatz(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}


		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"PuR", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Parkplatz_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_oeffentlichVorhanden_D1050)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_oeffentlichVorhanden_D1050).getBooleanWert();
			felder.put("OBJ_Parkplatz_jn_DLF1050", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Eigentuemer)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Eigentuemer).getTextWert();
			if(!wert.isEmpty()) {
				felder.put("OBJ_Parkplatz_Eigentuemer", new Excelfeld(Datentyp.Text, 
					wert, okFarbstyle));
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert();
			felder.put("OBJ_Parkplatz_Art_DLF1051", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Bedingungen_D1052)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Bedingungen_D1052).getTextWert();
			if(!wert.isEmpty()) {
				felder.put("OBJ_Parkplatz_Nutzungsbedingungen", new Excelfeld(Datentyp.Text, 
					wert, okFarbstyle));
			}
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Kapazitaet)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Kapazitaet).getZahlWert();
			felder.put("OBJ_Parkplatz_Kapazitaet", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_BehindertenplaetzeKapazitaet)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_BehindertenplaetzeKapazitaet).getZahlWert();
			felder.put("OBJ_Parkplatz_KapazitaetBehindertenplaetze", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Foto).getTextWert();
			felder.put("OBJ_Parkplatz_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Foto_Kommentar)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Foto_Kommentar).getTextWert();
			felder.put("OBJ_Parkplatz_Foto (Kommentar)", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Oeffnungszeiten_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Oeffnungszeiten_Foto).getTextWert();
			felder.put("OBJ_Parkplatz_Oeffnungszeiten_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Nutzungsbedingungen_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Nutzungsbedingungen_Foto).getTextWert();
			felder.put("OBJ_Parkplatz_Nutzungsbedingungen_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		lon = 0.0;
		lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		} else {
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Lon) &&
				(objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Lat))
					lat = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lon", new Excelfeld(Datentyp.Zahl_Double, lon, okFarbstyle));
		felder.put("lat", new Excelfeld(Datentyp.Zahl_Double, lat, okFarbstyle));

		String latlonEYEvisText = generateEYEvisLatLon(lon, lat);
		felder.put("OBJ_Parkplatz_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Foto).getTextWert();
			felder.put("OBJ_Parkplatz_Behindertenplaetze_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		lon = 0.0;
		lat = 0.0;
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lon) &&
				(objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lat))
					lat = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		if(lon != 0.0) {
			latlonEYEvisText = generateEYEvisLatLon(lon, lat);
			felder.put("OBJ_Parkplatz_Behindertenplaetze_Pos", new Excelfeld(Datentyp.Text, 
				latlonEYEvisText, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Parkplatz_WegzuHaltestelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Parkplatz_WegzuHaltestelle_Foto).getTextWert();
			felder.put("OBJ_Parkplatz_WegzuHaltestelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		return felder;
	}


		// =============================================================
		//    Rampe
		// =============================================================

	public Map<String, Excelfeld> generateRampe(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Rampe", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Rampe_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Laenge_cm_D2122)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Laenge_cm_D2122).getZahlWert();
				// Umrechnung von cm nach m
			if(wert != 0)
				wert = wert / 100.0;
			felder.put("OBJ_Rampe_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Breite_cm_D2123)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Breite_cm_D2123).getZahlWert();
			felder.put("OBJ_Rampe_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Neigung_prozent_D2124)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Neigung_prozent_D2124).getZahlWert();
			felder.put("OBJ_Rampe_Neigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Querneigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Querneigung_prozent).getZahlWert();
			felder.put("OBJ_Rampe_Querneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Verbindungsfunktion_D2121)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Verbindungsfunktion_D2121).getTextWert();
			felder.put("OBJ_Rampe_Verbindung_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Foto).getTextWert();
			felder.put("OBJ_Rampe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Richtung1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Richtung1_Foto).getTextWert();
			felder.put("OBJ_Rampe_Richtung1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rampe_Richtung2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rampe_Richtung2_Foto).getTextWert();
			felder.put("OBJ_Rampe_Richtung2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		return felder;
	}


		// =============================================================
		//    Rolltreppe
		// =============================================================
	
	public Map<String, Excelfeld> generateRolltreppe(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				unsicherFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, 
				unsicherFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				unsicherFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), 
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				unsicherFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, unsicherFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, unsicherFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				unsicherFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				unsicherFarbstyle));
		}
	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Rolltreppe", 
			okFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Rolltreppe_objektID", new Excelfeld(Datentyp.Text, 
				outputWert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert();
			if(		art.equals("nur_abwaerts")
				||	art.equals("nur_aufwaerts")
				||	art.equals("hauptrichtung_abwaerts")
				||	art.equals("hauptrichtung_aufwaerts")
				||	art.equals("beide_richtungen")) {
				felder.put("OBJ_Rolltreppe_Fahrtrichtung", new Excelfeld(Datentyp.Text, 
					art, 
					unsicherFarbstyle));
			} else {
				LOG.warning("in Objekt Rolltreppe bei Merkmal OBJ_Rolltreppe_Fahrtrichtung_D2132 unerwarteter Datenwert " + art);
			}
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Laufzeit_sek_D2134)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_Laufzeit_sek_D2134).getZahlWert();
			felder.put("OBJ_Rolltreppe_Laufzeit_sek", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, 
				unsicherFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Verbindungsfunktion_D2131)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_Verbindungsfunktion_D2131).getTextWert();
			felder.put("OBJ_Rolltreppe_Verbindung_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert,
				unsicherFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_Foto).getTextWert();
			felder.put("OBJ_Rolltreppe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_ID_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_ID_Foto).getTextWert();
			felder.put("OBJ_Rolltreppe_ID_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Richtung1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_Richtung1_Foto).getTextWert();
			felder.put("OBJ_Rolltreppe_Richtung1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Richtung2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Rolltreppe_Richtung2_Foto).getTextWert();
			felder.put("OBJ_Rolltreppe_Richtung2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				unsicherFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		return felder;
	}
	
	
		// =============================================================
		//    Stationsplan
		// =============================================================
	
	public Map<String, Excelfeld> generateStationsplan(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		String latlonEYEvisText = generateEYEvisLatLon(
			objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_Lon).getZahlWert(),
			objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_Lat).getZahlWert());
		felder.put("OBJ_Stationsplan_Pos", new Excelfeld(Datentyp.Text, 
			latlonEYEvisText, okFarbstyle));

		
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Stationsplan", okFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Stationsplan_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Bodenindikatorart)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_Bodenindikatorart).getTextWert();
			felder.put("OBJ_Stationsplan_Bodenindikatoren_Auswahl", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Plantaktil)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_Plantaktil).getBooleanWert();
			felder.put("OBJ_Stationsplan_taktil_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Fahrplanakustisch)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_Fahrplanakustisch).getBooleanWert();
			felder.put("OBJ_Stationsplan_Fahrplanakustisch_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_Foto).getTextWert();
			felder.put("OBJ_Stationsplan_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Stationsplan_2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Stationsplan_2_Foto).getTextWert();
			felder.put("OBJ_Stationsplan_2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		return felder;
	}
	
	
		// =============================================================
		//    Taxi
		// =============================================================
	
	public Map<String, Excelfeld> generateTaxi(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		String latlonEYEvisText = generateEYEvisLatLon(
			objektDaten.get(BFRKFeld.Name.OBJ_Taxistand_Lon).getZahlWert(),
			objektDaten.get(BFRKFeld.Name.OBJ_Taxistand_Lat).getZahlWert());
		felder.put("OBJ_Taxi_Pos", new Excelfeld(Datentyp.Text, latlonEYEvisText, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Taxi", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Taxi_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Taxistand_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Taxistand_Foto).getTextWert();
			felder.put("OBJ_Taxi_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Taxistand_WegzurHaltestelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Taxistand_WegzurHaltestelle_Foto).getTextWert();
			felder.put("OBJ_Taxi_WegzuBahnhof_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		return felder;
	}
	
	
		// =============================================================
		//    Toilette
		// =============================================================
	
	public Map<String, Excelfeld> generateToilette(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Toilette", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Toilette_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich).getTextWert();
			felder.put("OBJ_Toilette_Rollstuhltauglich", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Lokalschluessel_Notiz)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Lokalschluessel_Notiz).getTextWert();
			felder.put("OBJ_Toilette_Lokalschluessel_Notiz", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Beschreibung_D1075)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Beschreibung_D1075).getTextWert();
			felder.put("OBJ_Toilette_Oeffnungszeiten_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Foto).getTextWert();
			felder.put("OBJ_Toilette_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Foto).getTextWert();
			felder.put("OBJ_Toilette_Oeffnungszeiten_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Nutzungsbedingungen_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Nutzungsbedingungen_Foto).getTextWert();
			felder.put("OBJ_Toilette_Nutzungsbedingungen_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Umgebung1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Umgebung1_Foto).getTextWert();
			felder.put("OBJ_Toilette_Umgebung1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Toilette_Umgebung2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Toilette_Umgebung2_Foto).getTextWert();
			felder.put("OBJ_Toilette_Umgebung2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		return felder;
	}
	
	
		// =============================================================
		//    Treppe
		// =============================================================
	
	public Map<String, Excelfeld> generateTreppe(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}
	
		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, 
				okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}
	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Treppe", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Treppe_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Verbindungsfunktion_D2111)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Verbindungsfunktion_D2111).getTextWert();
			felder.put("OBJ_Treppe_Verbindung_Beschreibung", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Stufenanzahl_D2113)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Stufenanzahl_D2113).getZahlWert();
			felder.put("OBJ_Treppe_Stufenanzahl", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Stufenhoehe_cm_D2112)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Stufenhoehe_cm_D2112).getZahlWert();
			felder.put("OBJ_Treppe_Stufenhoehe_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Handlauf_links)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Handlauf_links).getBooleanWert();
			felder.put("OBJ_Treppe_ZUS_Handlauf_links", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Handlauf_rechts)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Handlauf_rechts).getBooleanWert();
			felder.put("OBJ_Treppe_ZUS_Handlauf_rechts", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Handlauf_mittig)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Handlauf_mittig).getBooleanWert();
			felder.put("OBJ_Treppe_ZUS_Handlauf_mittig", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Radschieberille)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Radschieberille).getBooleanWert();
			felder.put("OBJ_Treppe_ZUS_RadSchieberille_jn", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, 
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Treppe_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Treppe_Foto).getTextWert();
			felder.put("OBJ_Treppe_Foto", new Excelfeld(Datentyp.Text, 
				dateiname,
				okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		return felder;
	}
	
	
		// =============================================================
		//    Tür (Eingang)
		// =============================================================
	
	public Map<String, Excelfeld> generateTuer(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();
	
		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Eingang", okFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Tuer_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Tuer_Art_D2032)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Tuer_Art_D2032).getTextWert();
			felder.put("OBJ_Tuer_Art_DLF2032", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Tuer_Oeffnungsart_D2033)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Tuer_Oeffnungsart_D2033).getTextWert();
			felder.put("OBJ_Tuer_Oeffnungsart_DLF2033", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Tuer_Breite_cm_D2034)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Tuer_Breite_cm_D2034).getZahlWert();
			felder.put("OBJ_Tuer_Breite_cm_DLF2034", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Tuer_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Tuer_Foto).getTextWert();
			felder.put("OBJ_Tuer_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		return felder;
	}
	
	
		// =============================================================
		//    Verkaufsstelle
		// =============================================================
	
	public Map<String, Excelfeld> generateVerkaufsstelle(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

		
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Verkaufsstelle", okFarbstyle));
	
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Verkaufstelle_objektID", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_Art_D1021)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.OBJ_Verkaufsstelle_Art_D1021).getTextWert();
			felder.put("OBJ_Verkaufstelle_Art_DLF1021", new Excelfeld(Datentyp.Text, 
				outputWert, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_stufenfrei_D1022)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Verkaufsstelle_stufenfrei_D1022).getBooleanWert();
			felder.put("OBJ_Verkaufstelle_stufenfrei_jn_DLF1022", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Verkaufsstelle_Foto).getTextWert();
			felder.put("OBJ_Verkaufstelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
			
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_Oeffnungszeiten_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Verkaufsstelle_Oeffnungszeiten_Foto).getTextWert();
			felder.put("OBJ_Verkaufstelle_Oeffnungszeiten_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_EingangzuVerkaufsstelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Verkaufsstelle_EingangzuVerkaufsstelle_Foto).getTextWert();
			felder.put("OBJ_Verkaufstelle_EingangzuVerkaufstelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_WegzuVerkaufsstelle_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Verkaufsstelle_WegzuVerkaufsstelle_Foto).getTextWert();
			felder.put("OBJ_Verkaufstelle_WegzuVerkaufstelle_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}

		return felder;
	}
	
	
		// =============================================================
		//    Weg
		// =============================================================
	
	public Map<String, Excelfeld> generateWeg(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektDaten) {
		Map<String, Excelfeld> felder = new HashMap<>();

		if(haltestelleDaten.containsKey(BFRKFeld.Name.BF_Name)) {
			felder.put("Name", new Excelfeld(Datentyp.Text, 
				Textausgabe(haltestelleDaten.get(BFRKFeld.Name.BF_Name).getTextWert()), 
				okFarbstyle));
		}

		String hst_dhid = "";
		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID)) {
			hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				hst_dhid, okFarbstyle));
		}
		if(objektDaten.containsKey(BFRKFeld.Name.STG_DHID)) {
			felder.put("DHID", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.STG_DHID).getTextWert(), 
				okFarbstyle));
		}

		String objektname = "";
		if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Steig)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Soll_Steig).getTextWert();
		} else if(objektDaten.containsKey(BFRKFeld.Name.STG_Name)) {
			objektname = objektDaten.get(BFRKFeld.Name.STG_Name).getTextWert();
		}
		if(!objektname.isEmpty()) {
			felder.put("Objekt", new Excelfeld(Datentyp.Text, 
				objektname.trim(), okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.STG_Beschreibung)) {
			felder.put("Beschreibung", new Excelfeld(Datentyp.Text, 
				Textausgabe(objektDaten.get(BFRKFeld.Name.STG_Beschreibung).getTextWert()), 
				okFarbstyle));
		}

		double lon = 0.0;
		double lat = 0.0;
		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					lon = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
				}
				if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					lat = objektDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
				}
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert() != 0.0)) {
				lon = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			}
			if(objektDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat) &&
				(objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert() != 0.0)) {
				lat = objektDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = objektDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					lat = objektDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(lon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				LOG.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				lon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					lat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		felder.put("lat", new Excelfeld(Datentyp.Text, lat, okFarbstyle));
		felder.put("lon", new Excelfeld(Datentyp.Text, lon, okFarbstyle));

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Gemeinde)) {
			felder.put("Gemeinde", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert(), 
				okFarbstyle));
		}

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Ortsteil)) {
			felder.put("Ortsteil", new Excelfeld(Datentyp.Text, 
				haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert(), 
				okFarbstyle));
		}

	
		felder.put("Objektart", new Excelfeld(Datentyp.Text, 
			"Weg", okFarbstyle));

		if(objektDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String outputWert = objektDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			felder.put("OBJ_Weg_objektID", new Excelfeld(Datentyp.Text, 
				outputWert,
				okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Art)) {
			String art = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Art).getTextWert();
			felder.put("OBJ_Weg_Art", new Excelfeld(Datentyp.Text, 
				art, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_beleuchtet)) {
			boolean wertBoolean = objektDaten.get(BFRKFeld.Name.OBJ_Weg_beleuchtet).getBooleanWert();
			felder.put("OBJ_Weg_beleuchtet", new Excelfeld(Datentyp.Standard, 
				"" + wertBoolean, okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Laenge_cm_D2020)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Laenge_cm_D2020).getZahlWert();
				// Umrechnung von cm nach m
			if(wert != 0)
				wert = wert / 100.0;
			felder.put("OBJ_Weg_Laenge_m", new Excelfeld(Datentyp.Zahl_Nachkomma2, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Breite_cm_D2021)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Breite_cm_D2021).getZahlWert();
			felder.put("OBJ_Weg_Breite_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_ueberdacht)) {
			felder.put("OBJ_Weg_Ueberdachung", new Excelfeld(Datentyp.Text, 
				objektDaten.get(BFRKFeld.Name.OBJ_Weg_ueberdacht).getTextWert(), 
				okFarbstyle));
		}
	
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Hoehe_cm)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Hoehe_cm).getZahlWert();
			felder.put("OBJ_Weg_Hoehe_cm", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Neigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Neigung_prozent).getZahlWert();
			felder.put("OBJ_Weg_Neigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Querneigung_prozent)) {
			double wert = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Querneigung_prozent).getZahlWert();
			felder.put("OBJ_Weg_Querneigung_Prozent", new Excelfeld(Datentyp.Zahl_Nachkomma1, 
				wert, okFarbstyle));
		}

		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Verbindungsfunktion)) {
			String wert = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Verbindungsfunktion).getTextWert();
			felder.put("OBJ_Weg_Verbindung_Beschreibung", new Excelfeld(Datentyp.Text, 
				wert, okFarbstyle));
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Foto).getTextWert();
			felder.put("OBJ_Weg_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Richtung1_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Richtung1_Foto).getTextWert();
			felder.put("OBJ_Weg_Richtung1_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
		
		if(objektDaten.containsKey(BFRKFeld.Name.OBJ_Weg_Richtung2_Foto)) {
			String dateiname = objektDaten.get(BFRKFeld.Name.OBJ_Weg_Richtung2_Foto).getTextWert();
			felder.put("OBJ_Weg_Richtung2_Foto", new Excelfeld(Datentyp.Text, 
				dateiname, okFarbstyle));
			if(ExportNachEYEvisObjektpruefung.bilderkopieren)
				copyBild(hst_dhid, dateiname);
		}
	
		return felder;
	}


	
//TODO noch zu klären, ob auch SEVBussteig zu implementieren ist

//TODO noch zu klären, ob auch Notizobjekt zu implementieren ist
}
