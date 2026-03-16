package de.nvbw.bfrk.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.List;
import java.util.logging.Logger;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.Coordinate;

public class OpenStreetMap {

	private static final Logger LOG = NVBWLogger.getLogger(OpenStreetMap.class);

	private static HttpURLConnection conn;
	private static DateFormat datetime_osm_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	private boolean abruferfolgt = false;
	private int version = 0;
	private long uid = 0;
	private String user = "";
	private double lon = 0.0;
	private double lat = 0.0;
	private Date objektzeitstempel = null;

	public static List<String> getHyperlinksAsArray(String osmids) {
		List<String> returnArray = new ArrayList<>();

		if((osmids == null) || osmids.isEmpty())
			return returnArray;

		String osmidListe[] = osmids.split(",",-1);
		for(int index = 0; index < osmidListe.length; index++) {
			String typShort = osmidListe[index];
			String typLong = "";
			if(!typShort.isEmpty()) {
				typShort = typShort.substring(0,1);
				String osmid = osmidListe[index].substring(1);
				if(typShort.equals("n"))
					typLong = "node";
				else if(typShort.equals("w"))
					typLong = "way";
				else if(typShort.equals("r"))
					typLong = "relation";
				if(!typLong.isEmpty()) {
					returnArray.add("https://www.openstreetmap.org/" + typLong + "/" + osmid);
				}
			}
		}
		return returnArray;
	}

	public static String getHyperlinksAsString(String osmids) {
		String returnString = "";
		
		List<String> linkArray = getHyperlinksAsArray(osmids);

		for(int index = 0; index < linkArray.size(); index++) {
			if(!returnString.isEmpty())
				returnString += "|";
			returnString = returnString + linkArray.get(index);
		}
		return returnString;
	}

	public static Coordinate getOSMKoordinate(String osmidmitprefix) {
		Coordinate returnkoordinate = new Coordinate(0.0, 0.0);

		String osmids[] = osmidmitprefix.split(",", -1);
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
				LOG.warning("in getOSMKoordinate fehlt in OSM-Id der Typ-Prefix, ABBRUCH");
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
				LOG.fine("Overpass-Anfrage ===" + overpassrequest + "=== ...");
	
				if(conn == null)
					conn = (HttpURLConnection) url.openConnection();
				
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestProperty("User-Agent", "NVBW dietmar.seifert@nvbw.de");
				conn.setRequestMethod("GET");
				BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			
				// Connection is lazily executed whenever you request any status.
				int responseCode = ((HttpURLConnection) conn).getResponseCode();
				LOG.fine("" + responseCode); // Should be 200
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
					LOG.info("HTTP-Response Code nicht 200, sondern " + responseCode
						+ ", für Bild-Url ===" + overpassrequest + "===");
						// keine Url zurückgeben, weil nicht alles in Ordnung
					overpassrequest = "";
					aktion = "Inhaltsfehler";
				}
	
				rd.close();
			} catch (FileNotFoundException e) {
				LOG.finest("Overpass-Request wurde nicht gefunden (FileNotFoundException)" + "\t"
						+ overpassrequest + "\t" + e.toString());
					overpassrequest = "";
					aktion = "FileNotFoundException";
			} catch (MalformedURLException e) {
				LOG.info("Overpass-Request kann nicht heruntergeladen werden (MalformedURLException)" + "\t"
					+ overpassrequest + "\t" + e.toString());
				overpassrequest = "";
				aktion = "MalformedURLException";
			} catch (ProtocolException e) {
				LOG.info("Overpass-Request kann nicht heruntergeladen werden (ProtocolException)" + "\t"
					+ overpassrequest + "\t" + e.toString());
				overpassrequest = "";
				aktion = "ProtocolException";
			} catch (IOException e) {
				LOG.info("Overpass-Request kann nicht heruntergeladen werden (ProtocolException)" + "\t"
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

	

	public int getVersion() {
		if(this.abruferfolgt) {
			return this.version;
		}
		return 0;
	}

	public long getUid() {
		if(this.abruferfolgt) {
			return this.uid;
		}
		return 0;
	}

	public String getUser() {
		if(this.abruferfolgt) {
			return this.user;
		}
		return "";
	}

	public double getLon() {
		if(this.abruferfolgt) {
			return this.lon;
		}
		return 0.0;
	}

	public double getLat() {
		if(this.abruferfolgt) {
			return this.lat;
		}
		return 0.0;
	}

	public Date getObjektZeitstempel() {
		if(this.abruferfolgt) {
			return this.objektzeitstempel;
		}
		return null;
	}

	public boolean getObjekt(String osmidmitprefix) {
		boolean abruferfolgreich = false;

		String osmtyp = "";
		if(osmidmitprefix.startsWith("n"))
			osmtyp = "node";
		else if(osmidmitprefix.startsWith("w"))
			osmtyp = "way";
		else if(osmidmitprefix.startsWith("r"))
			osmtyp = "relation";
		else {
			LOG.warning("in getOSMKoordinate fehlt in OSM-Id der Typ-Prefix, ABBRUCH");
			return abruferfolgreich;
		}

		String osmidmitprefixnetto = osmidmitprefix.substring(1);

		String requestcontent = "[out:csv(::id,::version,::uid,::user,::lon,::lat,::timestamp;true;\";\")];\n"
			+ "\n"
			+ "(\n"
			+ "  " + osmtyp + "(" + osmidmitprefixnetto + ");>;\n"
			+ ");\n"
			+ "out meta center;";

		String overpassrequest = "";
		URL url;
		try {
			overpassrequest = "https://overpass-api.de/api/interpreter?data="
				+ URLEncoder.encode(requestcontent,
				StandardCharsets.UTF_8.toString());

			url = new URL(overpassrequest);
			LOG.fine("Overpass-Anfrage ===" + overpassrequest + "=== ...");

			if(conn == null)
				conn = (HttpURLConnection) url.openConnection();
			
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestProperty("User-Agent", "NVBW dietmar.seifert@nvbw.de");
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		
			// Connection is lazily executed whenever you request any status.
			int responseCode = ((HttpURLConnection) conn).getResponseCode();
			LOG.fine("" + responseCode); // Should be 200
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

			if(responseCode == HttpURLConnection.HTTP_OK) {

				String inputLine;
				StringBuffer response = new StringBuffer();
				boolean headerkorrekt = false;
				int lfdnr = 0;
				while ((inputLine = rd.readLine()) != null) {
					lfdnr++;
					if(lfdnr == 1) {
						if(inputLine.equals("@id;@version;@uid;@user;@lon;@lat;@timestamp")) {
							headerkorrekt = true;
							continue;
						}
					}
					response.append(inputLine + "\n");
					if(inputLine.startsWith(osmidmitprefixnetto + ";")) {
						String felder[] = inputLine.split(";",-1);
						this.version = Integer.parseInt(felder[1]);
						this.uid = Long.parseLong(felder[2]);
						this.user = felder[3];
						this.lon = Double.parseDouble(felder[4]);
						this.lat = Double.parseDouble(felder[5]);
						try {
							this.objektzeitstempel = datetime_osm_formatter.parse(felder[6]);
						} catch (Exception e) {
							LOG.warning("OSM-Zeitstempel ist nicht parseable, Content ==="
								+ felder[6] + "===");
						}
					}
				}
				System.out.println("Content  ===" + response.toString() + "===");
				abruferfolgreich = true;
				this.abruferfolgt = true;
			} else {
				LOG.info("HTTP-Response Code nicht 200, sondern " + responseCode
					+ ", für Bild-Url ===" + overpassrequest + "===");
					// keine Url zurückgeben, weil nicht alles in Ordnung
				overpassrequest = "";
				abruferfolgreich = false;
			}

			rd.close();
		} catch (FileNotFoundException e) {
			LOG.finest("Overpass-Request wurde nicht gefunden (FileNotFoundException)" + "\t"
					+ overpassrequest + "\t" + e.toString());
				overpassrequest = "";
				abruferfolgreich = false;
		} catch (MalformedURLException e) {
			LOG.info("Overpass-Request kann nicht heruntergeladen werden (MalformedURLException)" + "\t"
				+ overpassrequest + "\t" + e.toString());
			overpassrequest = "";
			abruferfolgreich = false;
		} catch (ProtocolException e) {
			LOG.info("Overpass-Request kann nicht heruntergeladen werden (ProtocolException)" + "\t"
				+ overpassrequest + "\t" + e.toString());
			overpassrequest = "";
			abruferfolgreich = false;
		} catch (IOException e) {
			LOG.info("Overpass-Request kann nicht heruntergeladen werden (ProtocolException)" + "\t"
				+ overpassrequest + "\t" + e.toString());
			overpassrequest = "";
			abruferfolgreich = false;
		}
		return abruferfolgreich;
	}
}
