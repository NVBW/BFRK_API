package de.nvbw.bfrk.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.imaging.ImagingException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;


import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.base.BFRKFeld.Datentyp;
import de.nvbw.bfrk.base.BFRKFeld.Name;
import de.nvbw.gtfs.model.Coordinate;


/*
 * fehlend: Notiz-Objekt evtl. veröffentlichen
 */



/**
 * 
 * @author SEI
 *
 */
public class OpenDataCSVExportwriter{
	private static DateFormat datum_de_formatter = new SimpleDateFormat("dd.MM.yyyy");

	public static final String EXPORT_BOOLEAN_TRUE = "ja";
	public static final String EXPORT_BOOLEAN_FALSE = "nein";
	private static final String FELDSEPARATOR = ";";

	private static final String INTRANET_URL = "http://10.70.190.131:8080";
	private static final String INTERNET_URL = "https://mobidata-bw.de";

	private static Map<String, String> bilderpublikcacheMap = new HashMap<>();
	private static Map<String, Boolean> bildernonpublikcacheMap = new HashMap<>();

	private static HttpURLConnection conn;


	private static String Textausgabe(String text) {
		String output = text;
		output = output.replace(";", ",");
		output = output.replace("\r?\n", "");
		output = output.replace("\"", "'");
		if(!text.equals(output))
			NVBWLogger.finest("Textausgabe korrigiert: von '" + text + "' nach '" + output + "'");
		return output.toString();
	}

	private String DatenlieferantOpenData(String textWert, String dhid) {
		String returntext = "Unbekannt";

		if(textWert.equals("SPNV"))
			returntext = "NVBW";
		else if(textWert.equals("bodo")) {
			if(dhid.startsWith("de:08436"))
				returntext = "Landratsamt Ravensburg";
			else if(dhid.startsWith("de:08435"))
				returntext = "Landratsamt Ravensburg";
			else {
				returntext = "";
				NVBWLogger.warning("in DatenlieferantOpenData: unerwarteter Landkreisschlüssel für " + textWert + " in DHID ===" + dhid + "===");
			}
		}
		else if(textWert.equals("CalwLK"))
			returntext = "Landratsamt Calw";
		else if(textWert.equals("ding")) {
			if(dhid.startsWith("de:08421"))
				returntext = "Stadtkreis Ulm";
			else if(dhid.startsWith("de:08425"))
				returntext = "Landratsamt Alb-Donau-Kreis";
			else if(dhid.startsWith("de:08426"))
				returntext = "Landratsamt Biberach";
			else {
				returntext = "";
				NVBWLogger.warning("in DatenlieferantOpenData: unerwarteter Landkreisschlüssel für " + textWert + " in DHID ===" + dhid + "===");
			}
		}
		else if(textWert.equals("FreudenstadtLK"))
			returntext = "Landratsamt Freudenstadt";
		else if(textWert.equals("HeidenheimLK"))
			returntext = "Landratsamt Heidenheim";
		else if(textWert.equals("HeilbronnLK"))
			returntext = "Landratsamt Heilbronn";
		else if(textWert.equals("Hohenlohekreis"))
			returntext = "Landratsamt Hohenlohekreis";
		else if(textWert.equals("KVV"))
			returntext = "Karlsruher Verkehrsverbund";
		else if(textWert.equals("Ortenaukreis"))
			returntext = "Landratsamt Ortenaukreis";
		else if(textWert.equals("Ostalbkreis"))
			returntext = "Landratsamt Ostalbkreis";
		else if(textWert.equals("PforzheimSK"))
			returntext = "Stadt Pforzheim";
		else if(textWert.equals("ReutlingenLK"))
			returntext = "Landratsamt Reutlingen";
		else if(textWert.equals("RottweilLK"))
			returntext = "Landratsamt Rottweil";
		else if(textWert.equals("SigmaringenLK"))
			returntext = "Landratsamt Sigmaringen";
		else if(textWert.equals("TübingenLK"))
			returntext = "Landratsamt Tübingen";
		else if(textWert.equals("KonstanzLK"))
			returntext = "Landratsamt Konstanz";
		else if(textWert.equals("Enzkreis"))
			returntext = "Landratsamt Enzkreis";
		else if(textWert.equals("VRN"))
			returntext = "Verkehrsverbund Rhein-Neckar";
		else if(textWert.equals("ZRF")) {
			if(dhid.startsWith("de:08311"))
				returntext = "Stadtkreis Freiburg";
			else if(dhid.startsWith("de:08315"))
				returntext = "Landratsamt Breisgau-Hochschwarzwald";
			else if(dhid.startsWith("de:08316"))
				returntext = "Landratsamt Emmendingen";
			else {
				returntext = "";
				NVBWLogger.warning("in DatenlieferantOpenData: unerwarteter Landkreisschlüssel für " + textWert + " in DHID ===" + dhid + "===");
			}
		}
		else {
			NVBWLogger.warning("in DatenlieferantOpenData: unbekannter textWert ===" + textWert + "===");
		}
		return returntext;
	}


	public void setBilderCachePublik(String csvdateiname) {
		BufferedReader filereader = null;
		try {
			filereader = new BufferedReader(new InputStreamReader(
				new FileInputStream(csvdateiname), 
				StandardCharsets.UTF_8));

			String line = "";
			while ((line = filereader.readLine()) != null) {
				String[] spalten = line.split(";", -1);
				bilderpublikcacheMap.put(spalten[0],  spalten[1]);
			}
			filereader.close();
			NVBWLogger.info("Anzahl eingelesene Bildereinträge für Bilder-Publik-Cache: " + bilderpublikcacheMap.size());
		} catch (FileNotFoundException e) {
	    	NVBWLogger.severe("Fehler beim öffnen BilderPublikCache-Datei: " + e.toString());
			return;
		} catch (IOException e) {
	    	NVBWLogger.severe("Fehler beim readline-Lesen BilderPublikCache-Datei: " + e.toString());
	    	return;
		}
	}


	public void setBilderCacheNonPublik(String csvdateiname) {
		BufferedReader filereader = null;
		try {
			filereader = new BufferedReader(new InputStreamReader(
				new FileInputStream(csvdateiname), 
				StandardCharsets.ISO_8859_1));

			String line = "";
			while ((line = filereader.readLine()) != null) {
				bildernonpublikcacheMap.put(line,  true);
			}
			filereader.close();
			NVBWLogger.info("Anzahl eingelesene Bildereinträge für Bilder-NonPublik-Cache: " + bildernonpublikcacheMap.size());
		} catch (FileNotFoundException e) {
	    	NVBWLogger.severe("Fehler beim öffnen BilderNonPublikcache-Datei: " + e.toString());
			return;
		} catch (IOException e) {
	    	NVBWLogger.severe("Fehler beim readline-Lesen BilderNonPublikcache-Datei: " + e.toString());
	    	return;
		}
	}


	public String getBilderPublikCacheAsCSV() {
		StringBuffer output = new StringBuffer();
		
		for(Map.Entry<String, String> bildercacheentry: bilderpublikcacheMap.entrySet()) {
			String key = bildercacheentry.getKey();
			String value = bildercacheentry.getValue();
			output.append(key + ";" + value + "\r\n");
		}
		return output.toString();
	}
	

	public String getBilderNonPublikCacheAsCSV() {
		StringBuffer output = new StringBuffer();
		
		for(Map.Entry<String, Boolean> bildernonpublikcacheentry: bildernonpublikcacheMap.entrySet()) {
			String key = bildernonpublikcacheentry.getKey();
			output.append(key + "\r\n");
		}
		return output.toString();
	}
	

	private String getBildurl(String bildpfad, String dateiname, String dhid) {
		String outputtext = "";

		NVBWLogger.fine("Beginn Methode getBildurl, dateiname ==="
			+ dateiname + "===");
		Date startzeit = new Date();
		
		if((dateiname == null) || dateiname.equals(""))
			return outputtext;
	
			// bei EYEvis kann historisch eine Liste von Dateiname mit , (Komma) getrennt vorkommmen, auf Pipe-Zeichen ändern
		if(dateiname.indexOf(",") != -1)
			dateiname = dateiname.replace(",","|");
	
			// in dateinamen können auch mehrere Fotos, per | (Pipe) getrennt, vorkommen
		String[] dateinamenliste = dateiname.split("\\|",-1);
	
		
		if(bilderpublikcacheMap.containsKey(dateiname)) {
			Date endzeit = new Date();
			NVBWLogger.info("Ende Methode getBildurl, Dauer in msek (PublikCache-Variante): "
				+ (endzeit.getTime() - startzeit.getTime()));
			for(int dateiindex = 0; dateiindex < dateinamenliste.length; dateiindex++) {
				addOpenDataListeneintrag(bildpfad, dhid, dateinamenliste[dateiindex], "erfolgreich");
			}
			
			return bilderpublikcacheMap.get(dateiname);
		}

		if(bildernonpublikcacheMap.containsKey(dateiname)) {
			Date endzeit = new Date();
			NVBWLogger.info("Ende Methode getBildurl, Dauer in msek (NonPublikCache-Variante): "
				+ (endzeit.getTime() - startzeit.getTime()));
			for(int dateiindex = 0; dateiindex < dateinamenliste.length; dateiindex++) {
				addOpenDataZipDateieintrag(bildpfad, dhid, dateinamenliste[dateiindex]);
				addOpenDataListeneintrag(bildpfad, dhid, dateinamenliste[dateiindex], "nonpublicCache");
			}
			return "";
		}


			// Verarbeitung aller Dateinamen (ggfs. mehrere)
		String aktion = "";
		for(int bildindex = 0; bildindex < dateinamenliste.length; bildindex++) {
			String aktdateiname = dateinamenliste[bildindex];
		
			String bildurl = INTRANET_URL + "/bfrk/haltestelle/bilder/" + dhid + "/" + aktdateiname;

			URL url;
			try {
				url = new URL(bildurl);
				NVBWLogger.fine("Url-Anfrage ===" + bildurl + "=== ...");

				if(conn == null)
					conn = (HttpURLConnection) url.openConnection();
				
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", "NVBW OpenDataExport");
				conn.setRequestMethod("GET");
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
				// Connection is lazily executed whenever you request any status.
				int responseCode = ((HttpURLConnection) conn).getResponseCode();
				NVBWLogger.fine("" + responseCode); // Should be 200
				// ===================================================================================================================
	
	
				long contentlength = 0;
				Integer headeri = 1;
				NVBWLogger.fine("Header-Fields Ausgabe ...");
				while(((HttpURLConnection) conn).getHeaderFieldKey(headeri) != null) {
					NVBWLogger.fine("  Header # "+headeri+":  [" 
						+ ((HttpURLConnection) conn).getHeaderFieldKey(headeri)+"] ==="
						+ ((HttpURLConnection) conn).getHeaderField(headeri)+"===");
					if(((HttpURLConnection) conn).getHeaderFieldKey(headeri).equals("Content-Length")) {
						contentlength = Integer.parseInt(((HttpURLConnection) conn).getHeaderField(headeri));
					}
					headeri++;
				}

				if((responseCode == 200) && (contentlength > 10000)) {
					aktion = "erfolgreich";
					NVBWLogger.info("Bild zwar erfolgreich gefunden, Bild-Url war ===" + bildurl + "===");
				} else {
					
					if(responseCode == 200)
						NVBWLogger.info("Bild zwar vorhanden, aber Content-Length zu gering: " + contentlength
							+ ", Bild-Url war ===" + bildurl + "===");
					else {
						NVBWLogger.info("HTTP-Response Code nicht 200, sondern " + responseCode
							+ ", für Bild-Url ===" + bildurl + "===");
					}
						// keine Url zurückgeben, weil nicht alles in Ordnung
					bildurl = "";
					aktion = "Inhaltsfehler";
				}
	
				rd.close();
			} catch (FileNotFoundException e) {
				NVBWLogger.info("Bilddatei wurde nicht gefunden (FileNotFoundException)" + "\t"
						+ bildurl + "\t" + e.toString());
					bildurl = "";
					aktion = "FileNotFoundException";
			} catch (MalformedURLException e) {
				NVBWLogger.info("Bilddatei kann nicht heruntergeladen werden (MalformedURLException)" + "\t"
					+ bildurl + "\t" + e.toString());
				bildurl = "";
				aktion = "MalformedURLException";
			} catch (ProtocolException e) {
				NVBWLogger.info("Bilddatei kann nicht heruntergeladen werden (ProtocolException)" + "\t"
					+ bildurl + "\t" + e.toString());
				bildurl = "";
				aktion = "ProtocolException";
			} catch (IOException e) {

				String bilddateiname = bildpfad + File.separator + aktdateiname;
				File bildFile = new File ( bilddateiname);
				PrintWriter zipOutput = null;

				addOpenDataZipDateieintrag(bildpfad, dhid, aktdateiname);
				
				NVBWLogger.info("Bilddatei kann nicht heruntergeladen werden (IOException)" + "\t"
					+ "\t" + e.toString());
				bildurl = "";
				aktion = "IOException";
			}

			addOpenDataListeneintrag(bildpfad, dhid, aktdateiname, aktion);
			
			if(!bildurl.equals("")) {
				if(bildurl.indexOf(INTRANET_URL) == 0)
					bildurl = bildurl.replace(INTRANET_URL, INTERNET_URL);
				else
					bildurl = "";

				if(!outputtext.equals(""))
					outputtext += "|";
				outputtext += bildurl;
			}
		}
		Date endzeit = new Date();
		NVBWLogger.info("Ende Methode getBildurl, Dauer in msek: "
			+ (endzeit.getTime() - startzeit.getTime()) + ",   " + aktion);

		if(!outputtext.equals(""))
			bilderpublikcacheMap.put(dateiname, outputtext);
		else
			bildernonpublikcacheMap.put(dateiname, true);

		return outputtext;
	}


	private Coordinate getOSMKoordinate(String osmidmitprefix) {
		Coordinate returnkoordinate = new Coordinate(0.0, 0.0);

		String osmids[] = osmidmitprefix.split("\\|", -1);
//if(osmids.length > 1)
//	System.out.println("achtung, prüfen echt mehrere osm-ids");
		double summeLon = 0;
		double summeLat = 0;
		for(int osmindex = 0; osmindex < osmids.length; osmindex++) {
			String osmid = osmids[osmindex];

			String osmtyp = "";
			if(osmid.startsWith("n"))
				osmtyp = "node";
			else if(osmid.startsWith("w"))
				osmtyp = "way";
			else if(osmid.startsWith("r"))
				osmtyp = "relation";
			else {
				NVBWLogger.warning("in getOSMKoordinate fehlt in OSM-Id der Typ-Prefix, ABBRUCH");
				return returnkoordinate;
			}

			String aktion = "";
			String osmidnetto = osmid.substring(1);

			String requestcontent = "[out:csv(::id,::lon,::lat;false;\";\")];\n"
				+ "\n"
				+ "(\n"
				+ "  " + osmtyp + "(" + osmidnetto + ");>;\n"
				+ ");\n"
				+ "out center;";

			String overpassrequest = "";
			URL url;
			try {
				overpassrequest = "https://overpass-api.de/api/interpreter?data="
					+ URLEncoder.encode(requestcontent,
					StandardCharsets.UTF_8.toString());

				url = new URL(overpassrequest);
				NVBWLogger.fine("Overpass-Anfrage ===" + overpassrequest + "=== ...");
	
				if(conn == null)
					conn = (HttpURLConnection) url.openConnection();
				
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", "NVBW dietmar.seifert@nvbw.de");
				conn.setRequestMethod("GET");
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
				// Connection is lazily executed whenever you request any status.
				int responseCode = ((HttpURLConnection) conn).getResponseCode();
				NVBWLogger.fine("" + responseCode); // Should be 200
				// ===================================================================================================================
	
	
				long contentlength = 0;
				Integer headeri = 1;
				NVBWLogger.info("Header-Fields Ausgabe ...");
				while(((HttpURLConnection) conn).getHeaderFieldKey(headeri) != null) {
					NVBWLogger.info("  Header # "+headeri+":  [" 
						+ ((HttpURLConnection) conn).getHeaderFieldKey(headeri)+"] ==="
						+ ((HttpURLConnection) conn).getHeaderField(headeri)+"===");
					if(((HttpURLConnection) conn).getHeaderFieldKey(headeri).equals("Content-Length")) {
						contentlength = Integer.parseInt(((HttpURLConnection) conn).getHeaderField(headeri));
					}
					headeri++;
				}
	
				if(responseCode == HttpURLConnection.HTTP_OK) {
					aktion = "erfolgreich";

					String inputLine;
					StringBuffer response = new StringBuffer();
					while ((inputLine = rd.readLine()) != null) {
						response.append(inputLine + "\n");
						if(inputLine.startsWith(osmidnetto + ";")) {
							String felder[] = inputLine.split(";",-1);
							if(felder.length >= 3) {
								summeLon += Double.parseDouble(felder[1]);
								summeLat += Double.parseDouble(felder[2]);
							}
						}
					}
					System.out.println("Content  ===" + response.toString() + "===");
				} else {
					NVBWLogger.info("HTTP-Response Code nicht 200, sondern " + responseCode
						+ ", für Bild-Url ===" + overpassrequest + "===");
						// keine Url zurückgeben, weil nicht alles in Ordnung
					overpassrequest = "";
					aktion = "Inhaltsfehler";
				}
	
				rd.close();
			} catch (FileNotFoundException e) {
				NVBWLogger.warning("Overpass-Request wurde nicht gefunden (FileNotFoundException)" + "\t"
						+ overpassrequest + "\t" + e.toString());
					overpassrequest = "";
					aktion = "FileNotFoundException";
			} catch (MalformedURLException e) {
				NVBWLogger.warning("Overpass-Request kann nicht heruntergeladen werden (MalformedURLException)" + "\t"
					+ overpassrequest + "\t" + e.toString());
				overpassrequest = "";
				aktion = "MalformedURLException";
			} catch (ProtocolException e) {
				NVBWLogger.warning("Overpass-Request kann nicht heruntergeladen werden (ProtocolException)" + "\t"
					+ overpassrequest + "\t" + e.toString());
				overpassrequest = "";
				aktion = "ProtocolException";
			} catch (IOException e) {
				NVBWLogger.warning("Overpass-Request kann nicht heruntergeladen werden (ProtocolException)" + "\t"
					+ overpassrequest + "\t" + e.toString());
				overpassrequest = "";
				aktion = "IOException";
			}
		} // End of Schleife über alle Input OSMid
		if(osmids.length > 0) {
			double lon = summeLon / osmids.length;
			double lat = summeLat / osmids.length;
			return new Coordinate(lon, lat);
		}
		return new Coordinate(0.47, 0.11);
	}

	
	private void addOpenDataListeneintrag(String bildpfad, String dhid, String aktdateiname, String aktion) {
		PrintWriter csvOutput = null;
		try {
			csvOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream("OpenData-Bildliste.txt",true),StandardCharsets.UTF_8)));
			csvOutput.println(bildpfad + ";" + dhid + ";" + aktdateiname + ";" + aktion);
			csvOutput.close();
		} catch (IOException ioe) {
			NVBWLogger.severe("Fehler bei Ausgabe in Datei " + "OpenData-Bildliste.txt");
		}
	}

	private static boolean opendatazipdateibereitserstellt = false;
	private void addOpenDataZipDateieintrag(String bildpfad, String dhid, String aktdateiname) {
		try {
			PrintWriter zipOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream("OpenData-zip.bat",true),StandardCharsets.ISO_8859_1)));
			if(!opendatazipdateibereitserstellt) {
				zipOutput.println("cmd.exe /c chcp 1252");
				opendatazipdateibereitserstellt = true;
			}
			zipOutput.println("mkdir \"fotos\\" + dhid.replace(":", "!") + "\"");
			zipOutput.println("copy \"" + bildpfad + File.separator + aktdateiname 
				+ "\"" + " \"fotos\\" + dhid.replace(":","!") + "\\" + aktdateiname + "\"");
			zipOutput.close();
		} catch (IOException ioe) {
			NVBWLogger.severe("Fehler bei Ausgabe in Datei " + "OpenData-zip.bat");
		}
	}


	public static List<Double> getEXIFGPSKoordinaten(final File file) throws ImagingException,
    IOException {
	    List<Double> returnArray = new ArrayList<>();
	
	    // get all metadata stored in EXIF format (ie. from JPEG or TIFF).
	    final ImageMetadata metadata = Imaging.getMetadata(file);
	
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
	
	
	public String printHaltestelleHeader() {
		String output = "ID;HST_DHID;HST_Name;Gemeinde;Ortsteil;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Sitzplätze;Unterstand;RollstuhlflächeImUnterstand;"
			+ "Fahrplananzeigetafel;Fahrplananzeigetafel_akustisch;Ansagen_vorhanden;"
			+ "Defibrillator;Defibrillator_Lagebeschreibung;Gepäckaufbewahrung;"
			+ "Gepäcktransport;InduktiveHöranlage;InduktiveHöranlageStandort;"
			+ "InfoNotrufsäule;Bahnhofsmission;"
			+ "HaltestelleTotale_Foto;SitzeOderUnterstand_Foto;SitzeOderUnterstandUmgebung_Foto;"
			+ "Fahrplananzeigetafel_Foto;Defibrillator_Foto;Gepäckaufbewahrung_Foto;"
			+ "InfoNotrufsäule_Foto;Bahnhofsmission_Foto;BahnhofsmissionWeg_Foto;"
			+ "BahnhofsmissionÖffnungszeiten_Foto;WeitereBilder1_Foto;"
			+ "WeitereBilder2_Foto;WeitereBilder3_Foto;Notiz_Foto;Erfassungsdatum";

		return output.toString();
	}


	public String printHaltestelle(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();

		String hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
		
		String bildpfad = "";
		if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!haltestelleDaten.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = haltestelleDaten.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		output.append(haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert() + FELDSEPARATOR);

		output.append(haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert() + FELDSEPARATOR);

		output.append(haltestelleDaten.get(BFRKFeld.Name.HST_Name).getTextWert() + FELDSEPARATOR);

		output.append(haltestelleDaten.get(BFRKFeld.Name.HST_Gemeinde).getTextWert() + FELDSEPARATOR);

		output.append(haltestelleDaten.get(BFRKFeld.Name.HST_Ortsteil).getTextWert() + FELDSEPARATOR);

		output.append(DatenlieferantOpenData(haltestelleDaten.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String objektkoordinatenqualitaet = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale != null) {
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
				if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
					if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
						objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
						objektkoordinatenqualitaet = "validierte-Position";
					}
					if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
						objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
						objektkoordinatenqualitaet = "validierte-Position";
					}
				}
			} else if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
				}
			}
		}
		if(objektLon == 0.0) {
			if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenqualitaet = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID));
				objektLon = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = haltestelleDaten.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenqualitaet);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(	(objektmerkmale != null)
			&&	objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

			// Sitzgelegenheit oder Unterstand
		if(	(objektmerkmale != null) && 
			objektmerkmale.containsKey(BFRKFeld.Name.HST_WartegelegenheitSitzplatz_Vorhanden_D1120) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_SitzeOderUnterstand_Art)) {
			String unterstandart = objektmerkmale.get(BFRKFeld.Name.HST_SitzeOderUnterstand_Art).getTextWert();
			if(	unterstandart.equals("nur_Sitzplaetze") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand_Windschutz"))
				output.append("ja");
			else
				output.append("nein");
			output.append(FELDSEPARATOR);
			if(	unterstandart.equals("nur_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand_Windschutz"))
				output.append("ja");
			else
				output.append("nein");
			output.append(FELDSEPARATOR);
			if(unterstandart.equals("nur_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand_Windschutz")) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche)) {
					if(objektmerkmale.get(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_1m5_1m5_vorhanden_nichtvorInfokasten"))
						output.append("ausreichend im Unterstand");
					else if(objektmerkmale.get(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_1m5_1m5_vorhanden_vorInfokasten"))
						output.append("ausreichend for Infokasten im Unterstand");
					else if(objektmerkmale.get(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_zuklein"))
						output.append("Fläche zu klein");
					else {
						output.append("unbekannt");
						NVBWLogger.warning("Methode printHaltestelle, Merkmal HST_Unterstand_RollstuhlfahrerFreieFlaeche unerwarteter Wert "
							+ objektmerkmale.get(BFRKFeld.Name.HST_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert());
					}
					output.append(FELDSEPARATOR);
				} else
					output.append(FELDSEPARATOR);
			} else {
				output.append("nein" + FELDSEPARATOR);
			}
		} else {
			output.append("nein" + FELDSEPARATOR);
			output.append("nein" + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}
		
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130) &&
			(objektmerkmale.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130).getBooleanWertalsJN().equals("ja"))) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Vorhanden_D1130).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

			if(	objektmerkmale.containsKey(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Akustisch_D1131))
				output.append(objektmerkmale.get(BFRKFeld.Name.HST_DynFahrplananzeigetafel_Akustisch_D1131).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
		}

			// automatische Ansagen
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Ansagen_Vorhanden_D1150)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_Ansagen_Vorhanden_D1150).getBooleanWertalsJN());
		} else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);
	
			// Defi vorhanden?
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Defi_vorhanden) &&
			(objektmerkmale.get(BFRKFeld.Name.HST_Defi_vorhanden).getBooleanWertalsJN().equals("ja"))) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_Defi_vorhanden).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

				// Defi Lagebeschreibung
			if(	objektmerkmale.containsKey(BFRKFeld.Name.HST_Defi_Lagebeschreibung)) {
				output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.HST_Defi_Lagebeschreibung).getTextWert()));
			}
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}
	
			// Gepäckaufbewahrung
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Vorhanden_D1090)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Vorhanden_D1090).getBooleanWertalsJN());
		} else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);

			// Gepäcktransport
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Gepaecktransport_Vorhanden_D1100)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_Gepaecktransport_Vorhanden_D1100).getBooleanWertalsJN());
		} else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);
	
			// Induktive Höranlage
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160) &&
			(objektmerkmale.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160).getBooleanWertalsJN().equals("ja"))) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Vorhanden_D1160).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

			if(	objektmerkmale.containsKey(BFRKFeld.Name.HST_InduktiveHoeranlage_Standort_D1161)) {
				output.append(objektmerkmale.get(BFRKFeld.Name.HST_InduktiveHoeranlage_Standort_D1161).getTextWert());
			}
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}
		
			// Info- oder Notrufsäule
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule)) {
			if(	objektmerkmale.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("notruf"))
				output.append("Notrufsäule");
			else if(objektmerkmale.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("info"))
				output.append("Informationssäule");
			else if(objektmerkmale.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("notruf_und_info"))
				output.append("Notruf- und Informationssäule");
			else if(objektmerkmale.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("nein"))
				output.append("keine");
			else {
				output.append("keine");
				NVBWLogger.warning("printHaltestelle-Methode, Merkmal HST_InfoNotrufsaeule hat unerwarteten Wert "
					+ objektmerkmale.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert());
			}
		} else
			output.append("keine");
		output.append(FELDSEPARATOR);
	
			// Bahnhofsmission
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Mission_vorhanden)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.HST_Mission_vorhanden).getBooleanWertalsJN());
		} else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);
			
			// Haltestelle Totale-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Totale_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Totale_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_SitzeoderUnterstand_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_SitzeoderUnterstand_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_SitzeoderUnterstand_Umgebung_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_SitzeoderUnterstand_Umgebung_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Fahrplananzeigetafel-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Fahrplananzeigetafel_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Fahrplananzeigetafel_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
			// Defi-Lagefoto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Defi_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Defi_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
			// Gepäckaufbewahrung-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Gepaeckaufbewahrung_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Info- und Notrufsäule-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_InfoNotrufsaeule_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
			// Banhofsmission Eingang-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Mission_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Mission_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
			// Bahnhofsmission Weg-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Mission_Weg_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Mission_Weg_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
			// Bahnhofsmission Öffnungszeiten-Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Mission_Oeffnungszeiten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Mission_Oeffnungszeiten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
			// Weiteres Foto 1
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_weitereBilder1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_weitereBilder1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
			// Weiteres Foto 2
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_weitereBilder2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_weitereBilder2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
			// Weiteres Foto 3
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_weitereBilder3_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_weitereBilder3_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Notiz Foto
		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.HST_Notiz_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.HST_Notiz_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}

		return output.toString();
	}


	public String printHaltesteigHeader() {
		String output = "ID;HST_DHID;HST_Name;STEIG_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Lage_innerorts;Bodenbelag;Steigtyp;Hochbordart;"
			+ "Steiglaenge_m;Steigbreite_cm;Steigbreite_Engstelle_cm;"
			+ "Steighoehe_cm;Laengsneigung;Querneigung;"
			+ "Bodind_Einstiegsbereich;Bodind_Leitstreifen;Bodind_Auffindestreifen;"
			+ "Beleuchtung_am_Steig;Sitzplätze;Unterstand;"
			+ "RollstuhlflächeImUnterstand;Unterstand_WaendeBodennah;"
			+ "Unterstand_Kontrastelemente;Unterstand_offiziell;SitzplatzSumme;Abfallbehaelter;Uhr;"
			+ "Tuer2_freieLaenge_cm;Tuer2_freieBreite_cm;"
			+ "Fahrtzielanzeiger;Fahrtzielanzeiger_akustisch;"
			+ "Fahrkartenautomat;Fahrkartenautomat_ID;Fahrkartenautomat_Lon;Fahrkartenautomat_Lat;"
			+ "Fahrgastinfoart;Fahrgastinfo_korrektehoehe;Fahrgastinfo_barrierefrei;"
			+ "Ansagen_vorhanden;InfoNotrufsäule;"
			+ "MobileRampe;MobileRampe_Länge_cm;MobileRampe_Tragfähigkeit_kg;"
			+ "Hublift;Hublift_Stellfläche_cm;Hublift_Tragfähigkeit_kg;"
			+ "Steig_Foto;Steig2_Foto;sonstigerSteigtyp_Foto;SteigGegenüber_Foto;"
			+ "HochbordartSonstiges_Foto;Steigbreite_Foto;Steigbreite_Engstelle_Foto;"
			+ "Bodind_Einstiegsbereich_Foto;Bodind_Leitstreifen_Foto;Bodind_Auffindestreifen_Foto;"
			+ "Unterstand_Foto;Unterstand_Nichtoffiziell_Foto;"
			+ "Uhr_Foto;Haltesteigmast_Foto;Fahrtzielanzeger_Foto;Fahrkartenautomat_Foto;"
			+ "Fahrgastinfo_nichtbarrierefrei_Foto;Steig_Uhr_Foto;"
			+ "InfoNotrufsäule_Foto;MobileRampeLage_Foto;HubliftLage_Foto;"
			+ "Zuwegung_von_Foto;Zuwegung_von_eben_Foto;Zuwegung_von_stufe_Foto;"
			+ "Zuwegung_von_Rampe_Foto;"
			+ "Zuwegung_von_sonstiges_Foto;"
			+ "Zuwegung_nach_Foto;Zuwegung_nach_eben_Foto;Zuwegung_nach_stufe_Foto;"
			+ "Zuwegung_nach_Rampe_Foto;"
			+ "Zuwegung_nach_sonstiges_Foto;Steig_Notiz_Foto;"
			+ "Erfassungsdatum;Fahrkartenentwerter_Foto";

		return output.toString();
	}

	
	public String printHaltesteig(Map<Name, BFRKFeld> haltestelleDaten, Map<Name, BFRKFeld> steigDaten) {
		StringBuffer output = new StringBuffer();

		String hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String bildpfad = "";
		if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!haltestelleDaten.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = haltestelleDaten.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		if(steigDaten.containsKey(BFRKFeld.Name.STG_DHID))
			output.append(steigDaten.get(BFRKFeld.Name.STG_DHID).getTextWert());
		else {
			NVBWLogger.warning("printHaltesteig: ohne STG_DHID, Objekt ===" + steigDaten.toString() + "===");
		}
		output.append(FELDSEPARATOR);

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID))
			output.append(haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert());
		else {
			NVBWLogger.warning("printHaltesteig: ohne HST_DHID, Objekt ===" + haltestelleDaten.toString() + "===");
		}
		output.append(FELDSEPARATOR);

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Name))
			output.append(haltestelleDaten.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Name))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(haltestelleDaten.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
				haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!steigDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = steigDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = steigDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(	steigDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon) &&
			(!steigDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWertalsText().equals("0,0"))) {
			if(steigDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lon))
				objektLon = steigDaten.get(BFRKFeld.Name.STG_IstPos_Lon).getZahlWert();
			if(steigDaten.containsKey(BFRKFeld.Name.STG_IstPos_Lat))
				objektLat = steigDaten.get(BFRKFeld.Name.STG_IstPos_Lat).getZahlWert();
		} else 	if(steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = steigDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = steigDaten.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	steigDaten.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(steigDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + haltestelleDaten.get(BFRKFeld.Name.STG_DHID));
				objektLon = steigDaten.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(steigDaten.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = steigDaten.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!steigDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(steigDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Lageinnerorts)) {
			output.append(steigDaten.get(BFRKFeld.Name.STG_Lageinnerorts).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenbelag_Art)) {
			String belag = steigDaten.get(BFRKFeld.Name.STG_Bodenbelag_Art).getTextWert();
			if(	belag.equals("befestigt Asphalt") ||
				belag.equals("befestigt_Asphalt"))
				output.append("befestigt Asphalt");
			else if(belag.equals("befestigt Betonsteine") ||
					belag.equals("befestigt_Betonsteine"))
				output.append("befestigt Betonsteine");
			else if(belag.equals("befestigt sonstige Oberfläche") ||
					belag.equals("befestigt_sonstige_Oberflaeche") ||
					belag.equals("befestigt_sonstiges") ||	// Mentz-Wert
					belag.equals("befestigt sonstige Oberflaeche"))
				output.append("befestigt sonstige Oberfläche");
			else if(belag.equals("unbefestigt_Gras"))
				output.append("unbefestigt Gras");
			else if(belag.equals("unbefestigt_Kies"))
				output.append("unbefestigt Kies");
			else if(belag.equals("unbefestigt_sonstige_Oberflaeche") ||
					belag.equals("unbefestigt_sonstiges"))
				output.append("unbefestigt sonstige Oberfläche");
			else {
				output.append("befestigt Betonsteine");
				NVBWLogger.warning("in printHaltesteig, Merkmal STG_Bodenbelag_Art: "
					+ "Wert unerwartet ===" + belag + "===");
				output.append("unbekannt");
			}
		} else
			output.append("unbekannt");
		output.append(FELDSEPARATOR);

		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Steigtyp))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Steigtyp).getTextWert());
		output.append(FELDSEPARATOR);

		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Hochbord_Art)) {
			if(	steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("Hochbord_ohne_Spurfuehrung") |
				steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("delfi_1202_ja"))
				output.append("Hochbord ohne Spurführung");
			else if(steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("Hochbord_mit_Spurfuehrung") |
					steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("delfi_1200_ja"))
				output.append("Hochbord mit Spurführung");
			else if(steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("Hochbord_Spurfuehrung_doppelteHohlkehle"))
				output.append("Hochbord mit Spurführung und doppelter Hohlkehle");
			else if(steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("Kombibord_mit_Spurfuehrung"))
				output.append("Kombibord mit Spurführung");
			else if(steigDaten.get(BFRKFeld.Name.STG_Hochbord_Art).getTextWert().equals("sonstiges_Hochbord"))
				output.append("sonstiges Hochbord");
			else {
				output.append("sonstiges Hochbord");
				String temploggingoutput = "";
				temploggingoutput = "printHaltesteig, bei Merkmal Hochbordart unerwartet "
					+ "keine Angaben für DELFI 1200 bis 1203";
				if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID))
					temploggingoutput += "; HST_DHID: " + haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();
				if(steigDaten.containsKey(BFRKFeld.Name.STG_DHID))
					temploggingoutput += "; STG_DHID: " + steigDaten.get(BFRKFeld.Name.STG_DHID).getTextWert();
				NVBWLogger.warning(temploggingoutput);
			}
//TODO offen: im ÖPNV else-Fall => "kein Hochbord"
		} else if ((steigDaten.containsKey(BFRKFeld.Name.STG_Hochbord_vorhanden)
				&&	steigDaten.get(BFRKFeld.Name.STG_Hochbord_vorhanden).getBooleanWert() == false)) {
			output.append("kein Hochbord");
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Steiglaenge))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Steiglaenge).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Steigbreite_cm_D1180))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Steigbreite_cm_D1180).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_SteigbreiteMinimum))
			output.append(steigDaten.get(BFRKFeld.Name.STG_SteigbreiteMinimum).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Steighoehe_cm_D1170))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Steighoehe_cm_D1170).getZahlWertalsText());
		output.append(FELDSEPARATOR);
		
		if(steigDaten.containsKey(BFRKFeld.Name.STG_Laengsneigung))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Laengsneigung).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Querneigung))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Querneigung).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_D2072))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_D2072).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);


		if(steigDaten.containsKey(BFRKFeld.Name.STG_Beleuchtung))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Beleuchtung).getTextWert());
		output.append(FELDSEPARATOR);
		
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_WartegelegenheitSitzplatz_Vorhanden_D1120) &&
			steigDaten.containsKey(BFRKFeld.Name.STG_SitzeOderUnterstand_Art)) {
			String unterstandart = steigDaten.get(BFRKFeld.Name.STG_SitzeOderUnterstand_Art).getTextWert();
			if(	unterstandart.equals("nur_Sitzplaetze") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand_Windschutz") ||
				unterstandart.equals("Sitzplaetze_ueberdacht_Windschutz") ||
				unterstandart.equals("Sitzplaetze_ueberdacht"))
				output.append("ja");
			else
				output.append("nein");
			output.append(FELDSEPARATOR);

			if(	unterstandart.equals("nur_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand_Windschutz") ||
				unterstandart.equals("Sitzplaetze_ueberdacht_Windschutz") ||
				unterstandart.equals("Sitzplaetze_ueberdacht") ||
				unterstandart.equals("nur_Windschutz") ||
				unterstandart.equals("nur_ueberdacht"))
				output.append("ja");
			else
				output.append("nein");
			output.append(FELDSEPARATOR);
			if(	unterstandart.equals("nur_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand") ||
				unterstandart.equals("Sitzplaetze_und_Unterstand_Windschutz") ||
				unterstandart.equals("Sitzplaetze_ueberdacht_Windschutz") ||
				unterstandart.equals("Sitzplaetze_ueberdacht") ||
				unterstandart.equals("nur_Windschutz") ||
				unterstandart.equals("nur_ueberdacht")) {
				if(steigDaten.containsKey(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche)) {
					if(steigDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_1m5_1m5_vorhanden_nichtvorInfokasten"))
						output.append("ausreichend im Unterstand");
					else if(steigDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_1m5_1m5_vorhanden_vorInfokasten"))
						output.append("ausreichend for Infokasten im Unterstand");
					else if(steigDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_zuklein"))
						output.append("Fläche zu klein");
						// Mentz-Wert
					else if(steigDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert().equals("Flaeche_1m5_1m5_vorhanden"))
						output.append("ausreichend im Unterstand");
					else {
						output.append("unbekannt");
						NVBWLogger.warning("Methode printHaltesteig, Merkmal STG_Unterstand_RollstuhlfahrerFreieFlaeche unerwarteter Wert "
							+ steigDaten.get(BFRKFeld.Name.STG_Unterstand_RollstuhlfahrerFreieFlaeche).getTextWert());
					}
				}
				output.append(FELDSEPARATOR);

				if(steigDaten.containsKey(BFRKFeld.Name.STG_Unterstand_WaendebisBodennaehe_jn))
					output.append(steigDaten.get(BFRKFeld.Name.STG_Unterstand_WaendebisBodennaehe_jn).getBooleanWertalsJN());
				output.append(FELDSEPARATOR);

				if(steigDaten.containsKey(BFRKFeld.Name.STG_Unterstand_Kontrastelemente_jn))
					output.append(steigDaten.get(BFRKFeld.Name.STG_Unterstand_Kontrastelemente_jn).getBooleanWertalsJN());
				output.append(FELDSEPARATOR);
				
				if(steigDaten.containsKey(BFRKFeld.Name.STG_Unterstand_offiziell_jn))
					output.append(steigDaten.get(BFRKFeld.Name.STG_Unterstand_offiziell_jn).getBooleanWertalsJN());
				output.append(FELDSEPARATOR);
			} else {
				output.append(FELDSEPARATOR);
				output.append(FELDSEPARATOR);
				output.append(FELDSEPARATOR);
				output.append(FELDSEPARATOR);
			}
		} else {
			output.append("nein" + FELDSEPARATOR);
			output.append("nein" + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}
		
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Bahnsteig_Sitzplaetz_Summe)) {
			output.append(steigDaten.get(BFRKFeld.Name.STG_Bahnsteig_Sitzplaetz_Summe).getIntWert());
		}
		output.append(FELDSEPARATOR);
		
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Abfallbehaelter))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Abfallbehaelter).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);

		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Bahnsteig_Uhr_vorhanden))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Bahnsteig_Uhr_vorhanden).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Tuer2_Laenge_cm))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Tuer2_Laenge_cm).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Tuer2_Breite_cm))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Tuer2_Breite_cm).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(	steigDaten.containsKey(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140) &&
			(steigDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140).getBooleanWertalsJN().equals("ja"))) {

			output.append(steigDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Vorhanden_D1140).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

			if(steigDaten.containsKey(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Akustisch_D1141))
				output.append(steigDaten.get(BFRKFeld.Name.STG_DynFahrtzielanzeiger_Akustisch_D1141).getBooleanWertalsJN());
			else
				output.append(EXPORT_BOOLEAN_FALSE);
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
		}

		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_D1040) &&
			(steigDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_D1040).getBooleanWertalsJN().equals("ja"))) {

			output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_D1040).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

			if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_ID))
				output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_ID).getTextWert());
			output.append(FELDSEPARATOR);

			if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Lon))
				output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Lon).getZahlWertalsText());
			output.append(FELDSEPARATOR);
			
			if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Lat))
				output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Lat).getZahlWertalsText());
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo)) {
			output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrgastinfo).getTextWert());
			output.append(FELDSEPARATOR);

			if( !steigDaten.get(BFRKFeld.Name.STG_Fahrgastinfo).getTextWert().equals("ohne")) {
				if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_inHoehe_100_160cm))
					output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_inHoehe_100_160cm).getBooleanWertalsJN());
				else
					output.append("unbekannt");
				output.append(FELDSEPARATOR);
				if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_freierreichbar_jn))
					output.append(steigDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_freierreichbar_jn).getBooleanWertalsJN());
				else
					output.append("unbekannt");
				output.append(FELDSEPARATOR);
			} else {
				output.append("unbekannt" + FELDSEPARATOR);
				output.append("unbekannt" + FELDSEPARATOR);
			}
		} else {
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}

			// automatische Ansagen
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Ansagen_Vorhanden_D1150))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Ansagen_Vorhanden_D1150).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);
	
			// Info- oder Notrufsäule
		if(	steigDaten.containsKey(BFRKFeld.Name.HST_InfoNotrufsaeule)) {
			if(	steigDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("notruf"))
				output.append("Notrufsäule");
			else if(steigDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("info"))
				output.append("Informationssäule");
			else if(steigDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("notruf_und_info"))
				output.append("Notruf- und Informationssäule");
			else if(steigDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert().equals("nein"))
				output.append("keine");
			else {
				output.append("keine");
				NVBWLogger.warning("printHaltestelle-Methode, Merkmal HST_InfoNotrufsaeule hat unerwarteten Wert "
					+ steigDaten.get(BFRKFeld.Name.HST_InfoNotrufsaeule).getTextWert());
			}
		} else
			output.append("keine");
		output.append(FELDSEPARATOR);
		
			// mobile Rampe
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_vorhanden_D1210) &&
			(steigDaten.get(BFRKFeld.Name.STG_Einstiegrampe_vorhanden_D1210).getBooleanWertalsJN().equals("ja"))) {
			
			output.append(steigDaten.get(BFRKFeld.Name.STG_Einstiegrampe_vorhanden_D1210).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

			if(	steigDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_Laenge_cm_D1211))
				output.append(steigDaten.get(BFRKFeld.Name.STG_Einstiegrampe_Laenge_cm_D1211).getIntWert());
			output.append(FELDSEPARATOR);

			if(	steigDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_Tragfaehigkeit_kg_D1212))
				output.append(steigDaten.get(BFRKFeld.Name.STG_Einstiegrampe_Tragfaehigkeit_kg_D1212).getIntWert());
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}

			// Hublift
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_vorhanden_D1220) &&
			(steigDaten.get(BFRKFeld.Name.STG_EinstiegHublift_vorhanden_D1220).getBooleanWertalsJN().equals("ja"))) {
			
			output.append(steigDaten.get(BFRKFeld.Name.STG_EinstiegHublift_vorhanden_D1220).getBooleanWertalsJN());
			output.append(FELDSEPARATOR);

			if(	steigDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_Laenge_cm_D1221))
				output.append(steigDaten.get(BFRKFeld.Name.STG_EinstiegHublift_Laenge_cm_D1221).getIntWert());
			output.append(FELDSEPARATOR);

			if(	steigDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_Tragfaehigkeit_kg_D1222))
				output.append(steigDaten.get(BFRKFeld.Name.STG_EinstiegHublift_Tragfaehigkeit_kg_D1222).getIntWert());
			output.append(FELDSEPARATOR);
		} else {
			output.append(EXPORT_BOOLEAN_FALSE + FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}
				
			// Fotos
		
		if(	steigDaten.containsKey(BFRKFeld.Name.STG_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	steigDaten.containsKey(BFRKFeld.Name.STG_2_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_ZUS_sonstigerSteigtyp_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_ZUS_sonstigerSteigtyp_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(steigDaten.containsKey(BFRKFeld.Name.STG_RT_sonstigerSteigtyp_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_RT_sonstigerSteigtyp_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Gegenueber_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Gegenueber_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(steigDaten.containsKey(BFRKFeld.Name.STG_RT_Gegenueber_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_RT_Gegenueber_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Hochbord_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Hochbord_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(steigDaten.containsKey(BFRKFeld.Name.STG_Breite_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Breite_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Engstelle_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Engstelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Bodenindikator_Einstiegsbereich_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Bodenindikator_Leitstreifen_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Bodenindikator_Auffindestreifen_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(steigDaten.containsKey(BFRKFeld.Name.STG_SitzeoderUnterstand_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_SitzeoderUnterstand_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(steigDaten.containsKey(BFRKFeld.Name.STG_ZUS_Unterstandnichtofiziell_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_ZUS_Unterstandnichtofiziell_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(steigDaten.containsKey(BFRKFeld.Name.STG_Uhr_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Uhr_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(steigDaten.containsKey(BFRKFeld.Name.STG_ZUS_Uhr_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_ZUS_Uhr_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Haltestellenmast_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Haltestellenmast_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_dynFahrtzielanzeiger_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_dynFahrtzielanzeiger_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrkartenautomat_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Fahrkartenautomat_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Fahrgastinfo_nichtbarrierefrei_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Fahrgastinfo_nichtbarrierefrei_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(steigDaten.containsKey(BFRKFeld.Name.STG_InfoNotrufsaeule_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_InfoNotrufsaeule_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_ZUS_Uhr_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_ZUS_Uhr_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Einstiegrampe_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Einstiegrampe_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		if(steigDaten.containsKey(BFRKFeld.Name.STG_EinstiegHublift_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_EinstiegHublift_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuwegung_von_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuwegung_von_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_direkt_Foto)) {
				String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg1_direkt_Foto).getTextWert();
				output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_eben_Foto)) {
		String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg1_eben_Foto).getTextWert();
		output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Weg_Stufe_Foto)) {
		String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg1_Weg_Stufe_Foto).getTextWert();
		output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_Rampe_Foto)) {
		String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg1_Rampe_Foto).getTextWert();
		output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg1_sonstiges_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg1_sonstiges_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuwegung_nach_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuwegung_nach_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_direkt_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg2_direkt_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_eben_Foto)) {
		String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg2_eben_Foto).getTextWert();
		output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Weg_Stufe_Foto)) {
		String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg2_Weg_Stufe_Foto).getTextWert();
		output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_Rampe_Foto)) {
		String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg2_Rampe_Foto).getTextWert();
		output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Zuweg2_sonstiges_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Zuweg2_sonstiges_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Notiz_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_Notiz_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(steigDaten != null) &&
			steigDaten.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(steigDaten.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = steigDaten.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_TicketValidator_Foto)) {
			String dateiname = steigDaten.get(BFRKFeld.Name.STG_TicketValidator_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}

		return output.toString();
	}


		// =========================================================================================
		//                              Bahnsteigelement (primär Haltepunkte und Bahnsteigabschnitte)
		// =========================================================================================

	public String printBahnsteigelementHeader() {
		String output = "ID;STEIG_ID;HST_ID;HST_Name;STEIG_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Nummer;Elementart;Entfernung_m;"
			+ "Element_Foto";

		return output.toString();
	}

	
	public String printBahnsteigelement(Map<Name, BFRKFeld> haltestelleDaten, 
		Map<Name, BFRKFeld> steigDaten,
		Map<Name, BFRKFeld> bahnsteigelementDaten) {
		StringBuffer output = new StringBuffer();


		String hst_dhid = haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String bildpfad = "";
		if(	haltestelleDaten.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!haltestelleDaten.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = haltestelleDaten.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektid = "";
		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = bahnsteigelementDaten.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_DHID))
			output.append(steigDaten.get(BFRKFeld.Name.STG_DHID).getTextWert());
		else {
			NVBWLogger.warning("printHaltesteig: ohne STG_DHID, Objekt ===" + steigDaten.toString() + "===");
		}
		output.append(FELDSEPARATOR);

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_DHID))
			output.append(haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert());
		else {
			NVBWLogger.warning("printHaltesteig: ohne HST_DHID, Objekt ===" + haltestelleDaten.toString() + "===");
		}
		output.append(FELDSEPARATOR);

		if(haltestelleDaten.containsKey(BFRKFeld.Name.HST_Name))
			output.append(haltestelleDaten.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		if(steigDaten.containsKey(BFRKFeld.Name.STG_Name))
			output.append(steigDaten.get(BFRKFeld.Name.STG_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(haltestelleDaten.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			haltestelleDaten.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Lon)) {
			if(!bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Lon).getZahlWertalsText().equals("0,0")) {
				if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Lon)) {
					objektLon = bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Lon).getZahlWert();
				}
				if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Lat)) {
					objektLat = bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Lat).getZahlWert();
				}
			}
		}

		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Koordinatenquelle)) {
			output.append(bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Koordinatenquelle).getTextWert());
		}
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!bahnsteigelementDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(Textausgabe(bahnsteigelementDaten.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert()));
		}
		output.append(FELDSEPARATOR);

		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Nummer))
			output.append(bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Nummer).getIntWert());
		output.append(FELDSEPARATOR);

		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Art))
			output.append(bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Art).getTextWert());
		output.append(FELDSEPARATOR);

		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Entfernung_m))
			output.append(bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Entfernung_m).getZahlWert());
		output.append(FELDSEPARATOR);

		if(bahnsteigelementDaten.containsKey(BFRKFeld.Name.STG_Element_Foto)) {
			String dateiname = bahnsteigelementDaten.get(BFRKFeld.Name.STG_Element_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}

		return output.toString();
	}



		// =========================================================================================
		//                              Aufzug
		// =========================================================================================
	
	public String printAufzugHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Tuerweite_cm;Kabinenbreite_cm;Kabinenlaenge_cm;Verbindungsfunktion;"
			+ "Aufzug_Foto;Aufzug_ID_Foto;Aufzug_Stoerungkontakt_Foto;"
			+ "Aufzug_Ebene1_Foto;Aufzug_Ebene2_Foto;Aufzug_Ebene3_Foto;"
			+ "Aufzug_Bedienelemente_Foto;Erfassungsdatum;"
			+ "Laufzeit_sek;Aufzug_Ebene1Weg2_Foto;Aufzug_Ebene2Weg2_Foto;Aufzug_Ebene3Weg2_Foto;"
			+ "DB-FaSta-ID";

		return output.toString();
	}


	public String printAufzug(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuffer output = new StringBuffer();

		
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "AUFZUG" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}
		
		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else {
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Tuerbreite_cm_D2091)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Tuerbreite_cm_D2091).getIntWert() != 50)
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Tuerbreite_cm_D2091).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenbreite_cm_D2094)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenbreite_cm_D2094).getIntWert() != 50)
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenbreite_cm_D2094).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenlaenge_cm_D2093)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenlaenge_cm_D2093).getIntWert() != 50)
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenlaenge_cm_D2093).getIntWert());
		}
		output.append(FELDSEPARATOR);


		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Verbindungsfunktion_D2095)) {
			if(!objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Verbindungsfunktion_D2095).getTextWert().equals(""))
				output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Verbindungsfunktion_D2095).getTextWert()));
		}
		output.append(FELDSEPARATOR);

			// Aufzug-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Aufzug-ID-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_ID_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_ID_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// StoerungKontakt-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_StoerungKontakt_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_StoerungKontakt_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
			// Ebene1-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Ebene1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Ebene2-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Ebene2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Ebene3-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene3_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Ebene3_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Bedienelemente-Foto
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Bedienelemente_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Bedienelemente_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Laufzeit_sek)) {
			int laufzeitSek = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Laufzeit_sek).getIntWert();
			output.append(laufzeitSek);
		}
		output.append(FELDSEPARATOR);

			// Ebene1-Weg2-Foto
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene1Weg2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Ebene1Weg2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Ebene2-Weg2-Foto
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene2Weg2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Ebene2Weg2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

			// Ebene3-Weg2-Foto

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_Ebene3Weg2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_Ebene3Weg2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Aufzug_FastaID)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Aufzug_FastaID).getTextWert()));
		}

		return output.toString();
	}
		// =========================================================================================
		//                              Bike and Ride
		// =========================================================================================

	public String printBikeRideHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;"
			+ "OSM_ID;Anlagentyp;Stellplatzanzahl;ueberdacht;beleuchtet;kostenpflichtig;"
			+ "Notiz_kostenpflichtig;WegZurAnlage_anfahrbar;Notizen;Anlage_Foto;WegzurAnlage_Foto;"
			+ "Hinderniszufahrt_Foto;Besonderheiten_Foto;Erfassungsdatum;Buegelabstand_cm";

		return output.toString();
	}


	/**
	 * Stand: an Merkmale vom 7.9. angepasst; Felder definiert; Open-Data Dokumentation aktuell
	 * @param
	 * @return
	 */
	public String printBikeRide(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
		
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}
		
		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "FAHRRADANLAGE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Lon) &&
			(!objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Lon).getZahlWertalsText().equals("0,0"))) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Lon).getZahlWert();

			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Lat) &&
				(!objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Lat).getZahlWertalsText().equals("0,0"))) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Lat).getZahlWert();
			}
			NVBWLogger.warning("BuR-Anlage notdürftig mit Haltestellen-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);
		
		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Anlagentyp)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Anlagentyp).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Stellplatzanzahl))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Stellplatzanzahl).getIntWert());
		output.append(FELDSEPARATOR);

		String anlagentyp = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Anlagentyp)) {
			anlagentyp = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Anlagentyp).getTextWert();
			if(	anlagentyp.equals("Vorderradhalter")
				|| anlagentyp.equals("doppelstoeckig")
				|| anlagentyp.equals("Anlehnbuegel")) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Ueberdacht))
					output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Ueberdacht).getBooleanWertalsJN());
			} else if(anlagentyp.equals("automatischesParksystem")
				|| anlagentyp.equals("Fahrradboxen")
				|| anlagentyp.equals("Fahrradparkhaus")
				|| anlagentyp.equals("Fahrradsammelanlage")) {
				output.append(OpenDataCSVExportwriter.EXPORT_BOOLEAN_TRUE);
			} else {
				output.append(OpenDataCSVExportwriter.EXPORT_BOOLEAN_FALSE);
			}
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Beleuchtet))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Beleuchtet).getBooleanWertalsJN());
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Kostenpflichtig))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Kostenpflichtig).getBooleanWertalsJN());
		output.append(FELDSEPARATOR);

		if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Kostenpflichtig) &&
			(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Kostenpflichtig).getBooleanWertalsJN().equals("ja")) &&
			objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_KostenpflichtigNotiz)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_KostenpflichtigNotiz).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_WegZurAnlageAnfahrbar))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_WegZurAnlageAnfahrbar).getBooleanWertalsJN());
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Notiz))
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Notiz).getTextWert()));
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Weg_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Weg_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Hinderniszufahrt_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Hinderniszufahrt_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Besonderheiten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Besonderheiten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		output.append(FELDSEPARATOR);

		if(		anlagentyp.equals("Anlehnbuegel")
			&&	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_BuR_Buegelabstand_cm))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_BuR_Buegelabstand_cm).getIntWert());

		return output.toString();
	}

	

		// =========================================================================================
		//                              Defibrillator
		// =========================================================================================
	
	public String printDefibrillatorHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Lagebeschreibung;Foto;Erfassungsdatum";
	
		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printDefibrillator(Map<Name, BFRKFeld> hstmerkmale) {
		StringBuilder output = new StringBuilder();
			
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
/*		if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = hstmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "DEFI" + "-" + "DUMMY";
		}
*/		// vorerst (02/2024) noch keine Infraidtemp vergeben, weil kein eigenständiges Objekt: output.append(objektid;
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!hstmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = hstmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = hstmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else {
			if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				objektLon = hstmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLon = hstmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!hstmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(hstmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Defi_Lagebeschreibung))
			output.append(Textausgabe(hstmerkmale.get(BFRKFeld.Name.HST_Defi_Lagebeschreibung).getTextWert()));
		output.append(FELDSEPARATOR);
	
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Defi_Foto)) {
			String dateiname = hstmerkmale.get(BFRKFeld.Name.HST_Defi_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(hstmerkmale != null) &&
			hstmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(hstmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = hstmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
		return output.toString();
	}

	
		// =========================================================================================
		//                              Engstelle
		// =========================================================================================
	
	public String printEngstelleHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Durchgangsbreite_cm;Bewegungsflaeche_cm;Engstelle_Foto;"
			+ "Weg1_Foto;Weg2_Foto;Erfassungsdatum";

		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printEngstelle(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();

		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "ENGSTELLE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;

		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else {
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
				// bei diesem Objekt ist die Soll-Koordinate in Hst-Objekt
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Engstelle_Durchgangsbreite_cm_D2080)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Durchgangsbreite_cm_D2080).getIntWert() != 50)
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Durchgangsbreite_cm_D2080).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Engstelle_Bewegflaeche_cm_D2081)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Bewegflaeche_cm_D2081).getIntWert() != 50)
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Bewegflaeche_cm_D2081).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Engstelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Engstelle_Weg1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Weg1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Engstelle_Weg2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Engstelle_Weg2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}

		return output.toString();
	}

		
		// =========================================================================================
		//                              Fahrkartenautomat
		// =========================================================================================
	
	public String printFahrkartenautomatHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Kartenautomat_ID;Entwerter_vorhanden;Kartenautomat_Foto;"
			+ "Ticketvalidator_Foto;Erfassungsdatum";
	
		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printFahrkartenautomat(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "FAHRKARTENAUTOMAT" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Lon)) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.OBJ_Kartenautomat_Lon).getZahlWert();
			}
			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.OBJ_Kartenautomat_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_ID))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Kartenautomat_ID).getTextWert());
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Entwerter)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Kartenautomat_Entwerter).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_Foto)) {
 			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Kartenautomat_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Kartenautomat_TicketValidator_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Kartenautomat_TicketValidator_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}

		return output.toString();
	}


		// =========================================================================================
		//                              Gleisquerung
		// =========================================================================================
	
	public String printGleisquerungHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Breite_cm;Verbindungsfunktion;Foto;Weg1_Foto;Weg2_Foto;"
			+ "Erfassungsdatum";

		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printGleisquerung(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "GLEISQUERUNG" + "-" + "DUMMY";
		}

		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Breite_cm)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Gleisquerung_Breite_cm).getIntWert());
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Verbindungsfunktion)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Gleisquerung_Verbindungsfunktion).getTextWert()));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Gleisquerung_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Weg1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Gleisquerung_Weg1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Gleisquerung_Weg2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Gleisquerung_Weg2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
		return output.toString();
	}

	
		// =========================================================================================
		//                              Informationsstelle
		// =========================================================================================
	
	public String printInfostelleHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "InfostelleName;stufenfrei;Infostelle_Foto;InfostelleEingang_Foto;"
			+ "InfostelleWeg_Foto;InfostelleOeffnungszeiten_Foto;Erfassungsdatum";
	
		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printInfostelle(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "INFORMATIONSSTELLE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();

			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Infostelle_Art_D1031))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Infostelle_Art_D1031).getTextWert());
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Infostelle_Stufenfrei_D1032)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Infostelle_Stufenfrei_D1032).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Infostelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Infostelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Infostelle_EingangzuInfostelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Infostelle_EingangzuInfostelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Infostelle_WegzuInfostelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Infostelle_WegzuInfostelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Infostelle_Oeffnungszeiten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Infostelle_Oeffnungszeiten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}

		return output.toString();
	}
	
	
		// =========================================================================================
		//                              Leihradanlage
		// =========================================================================================
	
	public String printLeihradanlageHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Anlage_Art;Notizen;Foto;Kontaktdaten_Foto;Erfassungsdatum";
	
		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printLeihradanlage(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "LEIHRADANLAGE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_lon).getZahlWert();

			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_lat).getZahlWert();
			}
		} else {
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			}
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Art))
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Art).getTextWert().equals("oeffentliche_Leihraeder"))
				output.append("Öffentliche Leihräder");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Art).getTextWert().equals("touristische_Ausleihe"))
				output.append("Touristische Ausleihe");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Art).getTextWert().equals("Sonstiges"))
				output.append("Sonstiges");
			else {
				NVBWLogger.warning("Ausgabe Leihradanlage: Art hat unerwarteten Wert "
					+ objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Art).getTextWert());
			}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Notizen)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Notizen).getTextWert()));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Leihradanlage_Kontaktdaten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Leihradanlage_Kontaktdaten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}

		return output.toString();
	}

		// =========================================================================================
		//                              Parkplatz
		// =========================================================================================

	public String printParkplatzHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Art;Eigentuemer;Nutzungsbedingungen;Stellplatzanzahl_insgesamt;Stellplatzanzahl_Behinderte;"
			+ "Behindertenparkplaetze_Longitude;Behindertenparkplaetze_Latitude;"
			+ "Parkplatz_Foto;LageBehindertenplaetze_Foto;Oeffnungszeiten_Foto;Nutzungsbedingungen_Foto;"
			+ "ParkplatzWegZurHaltestelle_Foto;Erfassungsdatum;"
			+ "Behindertenstellplatz_Laenge_cm;Kommentar_Behindertenstellplatz_Laenge_cm;"
			+ "Behindertenstellplatz_Breite_cm;Kommentar_Behindertenstellplatz_Breite_cm;"
			+ "Bauart;Orientierung;KapazitaetFrauenplaetze;KapazitaetFamilienplaetze;"
			+ "Frauenplaetze_Foto;Familienplaetze_Foto;"
			+ "MaxParkdauer_min;offen247;Gebuehrenpflichtig;"
			+ "Tarife;OeffnungszeitenOSM";

		return output.toString();
	}


	public String printParkplatz(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_oeffentlichVorhanden_D1050)
			&& (objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_oeffentlichVorhanden_D1050).getBooleanWert() == false)) {
			NVBWLogger.info("Parkplatzdatensatz wird gefiltert, weil nicht öffentlich.");
			return output.toString();
		}

		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
					
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "PARKPLATZ" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
		
		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Lon) &&
			(!objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Lon).getZahlWertalsText().equals("0,0"))) {
				objektLon = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Lon).getZahlWert();
	
			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Lat) &&
				(!objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Lat).getZahlWertalsText().equals("0,0")))
				objektLat = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Lat).getZahlWert();
		} else if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);
		
		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert().equals("kurzzeitplaetze"))
				output.append("Kurzzeitparkplätze");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert().equals("park_ride"))
				output.append("Park-and-Ride Parkplatz");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert().equals("behindertenplaetze"))
				output.append("Behindertenparkplatz");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert().equals("Parkhaus"))
				output.append("Parkhaus");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert().equals("parkplatz_ohne_parkride"))
				output.append("Parkplatz (kein Park-and-Ride)");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert().equals(""))
				output.append("unbekannt");
			else {
				NVBWLogger.warning("Ausgabe Parkplatz: Art hat unerwarteten Wert "
					+ objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051).getTextWert());
			}
		} else {
			NVBWLogger.warning("Ausgabe Parkplatz: OBJ_Parkplatz_Art_D1051 fehlt");
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Eigentuemer))
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Eigentuemer).getTextWert()));
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Bedingungen_D1052))
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Bedingungen_D1052).getTextWert()));
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Kapazitaet))
			output.append(Math.round(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Kapazitaet).getIntWert()));
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_BehindertenplaetzeKapazitaet))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_BehindertenplaetzeKapazitaet).getIntWert());
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lon))
			if(!objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lon).getZahlWertalsText().equals("0,0"))
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lon).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lat))
			if(!objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lat).getZahlWertalsText().equals("0,0"))
				output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Lat).getZahlWertalsText());
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Oeffnungszeiten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Oeffnungszeiten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Nutzungsbedingungen_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Nutzungsbedingungen_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_WegzuHaltestelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_WegzuHaltestelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Laenge_cm)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Laenge_cm).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Laenge_cm_Kommentar)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Laenge_cm_Kommentar).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Breite_cm)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Breite_cm).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Breite_cm_Kommentar)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Behindertenplaetze_Breite_cm_Kommentar).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Bauart)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Bauart).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Orientierung)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Orientierung).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_KapazitaetFrauenplaetze)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_KapazitaetFrauenplaetze).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_KapazitaetFamilienplaetze)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_KapazitaetFamilienplaetze).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Frauenplaetze_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Frauenplaetze_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Familienplaetze_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Familienplaetze_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_MaxParkdauer_min)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_MaxParkdauer_min).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_offen247_jn)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_offen247_jn).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Gebuehrenpflichtig_jn)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Gebuehrenpflichtig_jn).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_Tarife)) {
			output.append("\"" + objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_Tarife).getTextWert() + "\"");
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Parkplatz_OeffnungszeitenOSM)) {
			output.append("\"" + objektmerkmale.get(BFRKFeld.Name.OBJ_Parkplatz_OeffnungszeitenOSM).getTextWert() + "\"");
		}

		return output.toString();
	}

	
		// =========================================================================================
		//                              Rampe
		// =========================================================================================
	
	public String printRampeHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Laenge_cm;Breite_cm;Laengsneigung_prozent;Querneigung_prozent;"
			+ "Verbindungsfunktion;Rampe_Foto;Richtung1_Foto;Richtung2_Foto;"
			+ "Erfassungsdatum";
	
		return output.toString();
	}
	
	
	public String printRampe(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "RAMPE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

	//TODO in Beispiel-Datensatz ist PuR DHID-Bereichs-ID enthalten, statt Infrastruktur-ID !!!

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
		
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Laenge_cm_D2122)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Laenge_cm_D2122).getIntWert());
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Breite_cm_D2123)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Breite_cm_D2123).getIntWert());
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Neigung_prozent_D2124)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Neigung_prozent_D2124).getZahlWertalsText());
		}

		output.append(FELDSEPARATOR);
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Querneigung_prozent)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Querneigung_prozent).getZahlWertalsText());
		}

		output.append(FELDSEPARATOR);
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Verbindungsfunktion_D2121)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Verbindungsfunktion_D2121).getTextWert()));
		}

		output.append(FELDSEPARATOR);
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}

		output.append(FELDSEPARATOR);
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}

		output.append(FELDSEPARATOR);
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rampe_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rampe_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		
		return output.toString();
	}

	
		// =========================================================================================
		//                              Rolltreppe
		// =========================================================================================
	
	public String printRolltreppeHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Transportrichtung;Wechselnde_Richtung;Laufzeit_sek;Verbindungsfunktion;"
			+ "Rolltreppe_Foto;Rolltreppe_ID_Foto;Richtung1_Foto;Richtung2_Foto;"
			+ "Erfassungsdatum";
	
		return output.toString();
	}
	
	
	public String printRolltreppe(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "ROLLTREPPE" + "-" + "DUMMY";
		}
if(objektid.indexOf("INFRA-de:08111:6008-ROLLTREPPE-1") != -1)
	System.out.println("bitte prüfen");
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
				// bei diesem Objekt ist die Soll-Koordinate in Hst-Objekt
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132)) {
			if(	objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("nur_abwaerts") ||
				objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("hauptrichtung_abwaerts"))
				output.append("abwärts");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("nur_aufwaerts") ||
				objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("hauptrichtung_aufwaerts"))
				output.append("aufwärts");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("beide_richtungen"))
				output.append("beide Richtungen");
			else {
				NVBWLogger.warning("Objektart Rolltreppe, Merkmal OBJ_Rolltreppe_Fahrtrichtung_D2132 hat"
					+ " unerwarteten Wert ===" + objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert());
			}
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132)) {
			if(	objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("nur_abwaerts") ||
				objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("nur_aufwaerts"))
				output.append("nein");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("hauptrichtung_abwaerts") ||
				objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("hauptrichtung_aufwaerts") ||
				objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132).getTextWert().equals("beide_richtungen"))
				output.append("ja");
			else {
				NVBWLogger.warning("Beim Export Rolltreppe für Feld 'wechselne Richtung' unerwarteten Wert gefunden: '"
					+ objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Fahrtrichtung_D2132) + "'");
			}
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Laufzeit_sek_D2134)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Laufzeit_sek_D2134).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Verbindungsfunktion_D2131)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Verbindungsfunktion_D2131).getTextWert()));
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_ID_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_ID_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Rolltreppe_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Rolltreppe_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}

		return output.toString();
	}


		// =========================================================================================
		//                              Stationsplan
		// =========================================================================================

	
	public String printStationsplanHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "PlanTaktil;akustischeAusgabe;Bodenindikatorart;Foto;FotoUmgebung;"
			+ "Erfassungsdatum";

		return output.toString();
	}
	
	
	public String printStationsplan(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "STATIONSPLAN" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Lon).getZahlWert();

			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Lat).getZahlWert();
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Plantaktil)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Plantaktil).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Fahrplanakustisch)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Fahrplanakustisch).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Bodenindikatorart) &&
			(!objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Bodenindikatorart).equals(""))) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Bodenindikatorart).getTextWert());
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Stationsplan_2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Stationsplan_2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
		return output.toString();
	}


		// =========================================================================================
		//                              Taxi
		// =========================================================================================
	
	public String printTaxiHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
				+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
				+ "Taxistand_Foto;TaxistandWegzurHaltestelle_Foto;"
				+ "Erfassungsdatum";

		return output.toString();
	}


	public String printTaxi(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "TAXI" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Taxistand_Lon) &&
			(!objektmerkmale.get(BFRKFeld.Name.OBJ_Taxistand_Lon).getZahlWertalsText().equals("0,0"))) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.OBJ_Taxistand_Lon).getZahlWert();

			if(	objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Taxistand_Lat) &&
				(!objektmerkmale.get(BFRKFeld.Name.OBJ_Taxistand_Lat).getZahlWertalsText().equals("0,0"))) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.OBJ_Taxistand_Lat).getZahlWert();
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Taxistand_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Taxistand_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Taxistand_WegzurHaltestelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Taxistand_WegzurHaltestelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
		return output.toString();
	}


		// =========================================================================================
		//                              Toilette
		// =========================================================================================
	
	public String printToiletteHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
				+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
				+ "rollstuhltauglich;rollstuhltauglichAlle;SchluesselEurokey;SchluesselLokal;"
				+ "Oeffnungszeiten;Lokalschluessel_Notiz;Toilette_Foto;SchluesselLokal_Foto;"
				+ "Oeffnungszeiten_Foto;Nutzungsbedingungen_Foto;"
				+ "Umgebung1_Foto;Umgebung2_Foto;Erfassungsdatum";

		return output.toString();
	}


	public String printToilette(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "TOILETTE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich).getTextWert().equals("ja_nurrollstuhlfahrer"))
				output.append(EXPORT_BOOLEAN_TRUE);
			else
				output.append(EXPORT_BOOLEAN_FALSE);
			output.append(FELDSEPARATOR);
			
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich).getTextWert().equals("ja_allereisenden"))
				output.append(EXPORT_BOOLEAN_TRUE);
			else
				output.append(EXPORT_BOOLEAN_FALSE);
			output.append(FELDSEPARATOR);

			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich).getTextWert().equals("ja_euroschluessel"))
				output.append(EXPORT_BOOLEAN_TRUE);
			else
				output.append(EXPORT_BOOLEAN_FALSE);
			output.append(FELDSEPARATOR);

			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Rollstuhltauglich).getTextWert().equals("ja_lokalschluessel"))
				output.append(EXPORT_BOOLEAN_TRUE);
			else
				output.append(EXPORT_BOOLEAN_FALSE);
			output.append(FELDSEPARATOR);
		} else {
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
			output.append(FELDSEPARATOR);
		}
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Beschreibung_D1075)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Beschreibung_D1075).getTextWert()));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Lokalschluessel_Notiz)) {
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Lokalschluessel_Notiz).getTextWert()));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_LokalerSchluessel_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_LokalerSchluessel_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Oeffnungszeiten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Nutzungsbedingungen_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Nutzungsbedingungen_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Umgebung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Umgebung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Toilette_Umgebung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Toilette_Umgebung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
		return output.toString();
	}


		// =========================================================================================
		//                             	  Treppe
		// =========================================================================================
	
	public String printTreppeHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "AnzahlStufen;Stufenhoehe_cm;Handlauf_links;"
			+ "Handlauf_rechts;Handlauf_mittig;Fahrradrille;"
			+ "Verbindungsfunktion;Treppe_Foto;"
			+ "Treppe_Richtung1_Foto;Treppe_Richtung2_Foto;"
			+ "Erfassungsdatum;"
			+ "Handlauf_Zielangabe_links;Handlauf_Zielangabe_rechts;"
			+ "Handlauf_Zielangabe_mittig;"
			+ "oben_Richtung1_Foto;oben_Richtung2_Foto";

		return output.toString();
	}
	
	
	public String printTreppe(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "TREPPE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Stufenanzahl_D2113)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Stufenanzahl_D2113).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Stufenhoehe_cm_D2112)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Stufenhoehe_cm_D2112).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Handlauf_links)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Handlauf_links).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Handlauf_rechts)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Handlauf_rechts).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Handlauf_mittig)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Handlauf_mittig).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Radschieberille)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Radschieberille).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Verbindungsfunktion_D2111)) {
			if(!objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Verbindungsfunktion_D2111).getTextWert().equals(""))
				output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Verbindungsfunktion_D2111).getTextWert()));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_unten_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_unten_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		} else if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_unten_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_unten_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_ZielBlinde_links)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_ZielBlinde_links).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_ZielBlinde_rechts)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_ZielBlinde_rechts).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_ZielBlinde_mittig)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_ZielBlinde_mittig).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_oben_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_oben_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Treppe_oben_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Treppe_oben_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}

		return output.toString();
	}


		// =========================================================================================
		//                              Tür
		// =========================================================================================
	
	public String printTuerHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Tuerart;Oeffnungsart;Tuerbreite_cm;Foto;Erfassungsdatum;"
			+ "Richtung1_Foto;Richtung2_Foto";

		return output.toString();
	}
	
	
	public String printTuer(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert() + "-";
			objektid += "EINGANG" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);

		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);

		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);

		//Datenstatus
//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Tuer_Art_D2032)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Art_D2032).getTextWert().equals("drehfluegeltuer"))
				output.append("Anschlagtür");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Art_D2032).getTextWert().equals("pendeltuer"))
					output.append("Pendeltür");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Art_D2032).getTextWert().equals("karusseltuer"))
					output.append("Rotationstür");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Art_D2032).getTextWert().equals("schiebetuer"))
					output.append("Schiebetür");
			else {
	//			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Art_D2032).getTextWert();
	//TODO Fehlermeldung erstellen
			}
		}
		output.append(FELDSEPARATOR);			
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Tuer_Oeffnungsart_D2033)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Oeffnungsart_D2033).getTextWert());
		}
		output.append(FELDSEPARATOR);			
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Tuer_Breite_cm_D2034)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Breite_cm_D2034).getIntWert());
		}
		output.append(FELDSEPARATOR);			

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Tuer_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
		output.append(FELDSEPARATOR);			

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Tuer_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);			

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Tuer_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Tuer_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		
		return output.toString();
	}

	
		// =========================================================================================
		//                              Verkaufsstelle
		// =========================================================================================
	
	public String printVerkaufsstelleHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "VerkaufsstelleName;stufenfrei;Verkaufsstelle_Foto;VerkaufsstelleEingang_Foto;"
			+ "VerkaufsstelleOeffnungszeiten_Foto;VerkaufsstelleWeg_Foto;Erfassungsdatum";
	
		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printVerkaufsstelle(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
		
		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "VERKAUFSSTELLE" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);
	
		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_Art_D1021))
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Verkaufsstelle_Art_D1021).getTextWert()));
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_stufenfrei_D1022)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Verkaufsstelle_stufenfrei_D1022).getBooleanWertalsJN());
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Verkaufsstelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_EingangzuVerkaufsstelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Verkaufsstelle_EingangzuVerkaufsstelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_Oeffnungszeiten_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Verkaufsstelle_Oeffnungszeiten_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Verkaufsstelle_WegzuVerkaufsstelle_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Verkaufsstelle_WegzuVerkaufsstelle_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
	
		return output.toString();
	}
	
	
		// =========================================================================================
		//                              Weg
		// =========================================================================================
	
	public String printWegHeader() {
		String output = "ID;HST_DHID;HST_Name;Datenquelle;Datenstatus;"
			+ "Longitude;Latitude;Koordinatenqualität;OSM_ID;"
			+ "Wegart;Laenge_cm;Breite_cm;Laengsneigung_Prozent;"
			+ "Querneigung_Prozent;lichteHöhe_cm;beleuchtet;überdacht;Verbindungsfunktion;"
			+ "Foto;Richtung1_Foto;Richtung2_Foto;Erfassungsdatum";
	
		return output.toString();
	}
	
	
	/**
	 * Stand: 
	 * @param
	 * @return
	 */
	public String printWeg(Map<Name, BFRKFeld> hstmerkmale, Map<Name, BFRKFeld> objektmerkmale) {
		StringBuilder output = new StringBuilder();
	
		String hst_dhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();

		String haltestelledhid = "";
		if(hstmerkmale.containsKey(BFRKFeld.Name.HST_DHID))
			haltestelledhid = hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert();
	
		String objektid = "";
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP)) {
			String infraidtemp = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_INFRAIDTEMP).getTextWert();
			if((infraidtemp != null) && !infraidtemp.isEmpty()) {
				objektid = infraidtemp;
			}
		}
		if(objektid.isEmpty()) {
			objektid = "INFRA-" + haltestelledhid + "-";
			objektid += "WEG" + "-" + "DUMMY";
		}
		output.append(objektid);
		output.append(FELDSEPARATOR);
	
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert());
		output.append(FELDSEPARATOR);
		output.append(hstmerkmale.get(BFRKFeld.Name.HST_Name).getTextWert());
		output.append(FELDSEPARATOR);
	
		output.append(DatenlieferantOpenData(hstmerkmale.get(BFRKFeld.Name.HST_Datenlieferant).getTextWert(),
			hstmerkmale.get(BFRKFeld.Name.HST_DHID).getTextWert()));
		output.append(FELDSEPARATOR);
	
		//Datenstatus
	//TODO dynamisch setzen, wenn in DB vorhanden
		output.append("Rohdaten");
		output.append(FELDSEPARATOR);

		String bildpfad = "";
		if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Importdateipfad) && 
			!hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert().equals("")) {
			bildpfad = hstmerkmale.get(BFRKFeld.Name.HST_Importdateipfad).getTextWert();
		}

		String objektkoordinatenquelle = "Objekt-Rohposition";
		double objektLon = 0.0;
		double objektLat = 0.0;
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty()) {
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lon)) {
					objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lon).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
				if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Lat)) {
					objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Lat).getZahlWert();
					objektkoordinatenquelle = "validierte-Position";
				}
			}
		} else 	if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lon)) {
			objektLon = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lon).getZahlWert();
			if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Bild_Lat)) {
				objektLat = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Bild_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lon) &&
				(objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lon).getZahlWert();
	
				if(objektmerkmale.containsKey(BFRKFeld.Name.STG_Soll_Lat))
					objektLat = objektmerkmale.get(BFRKFeld.Name.STG_Soll_Lat).getZahlWert();
			}
		}
		if(objektLon == 0.0) {
			if(	hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lon) &&
				(hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert() != 0.0)) {
				objektkoordinatenquelle = "Haltestellen-Sollposition";
				NVBWLogger.warning("Objekt notdürftig mit DIVA-Koordinate besetzt, DHID:  " + hstmerkmale.get(BFRKFeld.Name.HST_DHID));
				objektLon = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lon).getZahlWert();
	
				if(hstmerkmale.containsKey(BFRKFeld.Name.HST_Soll_Lat))
					objektLat = hstmerkmale.get(BFRKFeld.Name.HST_Soll_Lat).getZahlWert();
			}
		}
		String objektLonString = "";
		if(objektLon != 0.0) {
			objektLonString = "" + objektLon;
			objektLonString = objektLonString.replace(".", ",");
		}
		String objektLatString = "";
		if(objektLat != 0.0) {
			objektLatString = "" + objektLat;
			objektLatString = objektLatString.replace(".", ",");
		}
		output.append(objektLonString + FELDSEPARATOR);
		output.append(objektLatString + FELDSEPARATOR);

		//Koordinatenquelle
		output.append(objektkoordinatenquelle);
		output.append(FELDSEPARATOR);

		// OSM_ID
		if(objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_OSM_Id)) {
			if(!objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert().isEmpty())
				output.append(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_OSM_Id).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Art)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Art).getTextWert().equals("weg"))
				output.append("Weg");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Art).getTextWert().equals("unterfuehrung"))
				output.append("Unterführung");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Art).getTextWert().equals("ueberfuehrung"))
				output.append("Überführung oder Brücke");
			else {
				NVBWLogger.warning("Methode printWeg, Merkmal OBJ_Weg_Art hat unerwarteten Wert "
					+ objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Art).getTextWert());
			}
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Laenge_cm_D2020)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Laenge_cm_D2020).getIntWert());
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Breite_cm_D2021)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Breite_cm_D2021).getIntWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Neigung_prozent)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Neigung_prozent).getZahlWertalsText());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Querneigung_prozent)) {
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Querneigung_prozent).getZahlWertalsText());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Hoehe_cm))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Hoehe_cm).getIntWert());
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_beleuchtet))
			output.append(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_beleuchtet).getBooleanWertalsJN());
		else
			output.append(EXPORT_BOOLEAN_FALSE);
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_ueberdacht)) {
			if(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_ueberdacht).getTextWert().equals("ohne_dach"))
				output.append("ohne Dach");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_ueberdacht).getTextWert().equals("dach_hoch"))
				output.append("Dach hoch");
			else if(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_ueberdacht).getTextWert().equals("dach_niedrig"))
				output.append("Dach niedrig");
			else
				NVBWLogger.warning("printWeg, Objektausgabe OBJ_Weg_ueberdacht, unerwarteter Wert ==="
					+ objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_ueberdacht).getTextWert());
		}
		output.append(FELDSEPARATOR);

		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Verbindungsfunktion))
			output.append(Textausgabe(objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Verbindungsfunktion).getTextWert()));
		output.append(FELDSEPARATOR);
		
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Richtung1_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Richtung1_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);
	
		if(objektmerkmale.containsKey(BFRKFeld.Name.OBJ_Weg_Richtung2_Foto)) {
			String dateiname = objektmerkmale.get(BFRKFeld.Name.OBJ_Weg_Richtung2_Foto).getTextWert();
			output.append(getBildurl(bildpfad, dateiname, hst_dhid));
		}
		output.append(FELDSEPARATOR);

		if(	(objektmerkmale != null) &&
			objektmerkmale.containsKey(BFRKFeld.Name.ZUSATZ_Erfassungsdatum)) {
			if(objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum) != null) {
				Date datum = objektmerkmale.get(BFRKFeld.Name.ZUSATZ_Erfassungsdatum).getDatumWert();
				if(datum != null)
					output.append(datum_de_formatter.format(datum));
			}
		}
	
		return output.toString();
	}
}
