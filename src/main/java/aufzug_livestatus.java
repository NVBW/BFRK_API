
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;
import de.nvbw.bfrk.util.ReaderBase;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "aufzug_livestatus", 
			urlPatterns = {"/aufzug_livestatus/*"}
		)
public class aufzug_livestatus extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final DateFormat datetime_iso8601_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    private static Connection bfrkConn = null;
   
	private static HttpURLConnection conn = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public aufzug_livestatus() {
        super();
        // TODO Auto-generated constructor stub
    }

    private String getFastaAufzugzustand(long fastaid) {
    	String aufzugstatus = "REQUESTERROR";
 
    	String fastaurl = "https://apis.deutschebahn.com/db-api-marketplace/apis/fasta/v2/facilities/" + fastaid;


		URL url;
		try {
			url = new URL(fastaurl);
			NVBWLogger.fine("Url-Anfrage ===" + fastaurl + "=== ...");

			if(conn == null)
				conn = (HttpURLConnection) url.openConnection();
			
			conn = (HttpURLConnection) url.openConnection();
				// Anwendung BFRK-API auf https://developers.deutschebahn.com/
			conn.setRequestProperty("DB-Client-Id", "80124701564e7145e373fd04bb6b7826");
			conn.setRequestProperty("DB-Api-Key", "04aa859f57ad99b96011b799cb654695");
			conn.setRequestProperty("accept", "application/json");
			conn.setRequestProperty("User-Agent", "NVBW");
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

			if(responseCode == HttpURLConnection.HTTP_OK) {

				String inputLine;
				StringBuffer response = new StringBuffer();
				while ((inputLine = rd.readLine()) != null) {
					response.append(inputLine + "\n");
				}
				System.out.println("Content  ===" + response.toString() + "===");

				if(response.length() > 0) {
					String inhalt = response.toString();
					String suchstring = "\"state\"";
					int startpos = inhalt.indexOf(suchstring);
					if(startpos != -1) {
						startpos += suchstring.length();
						int findstartpos = inhalt.indexOf("\"", startpos) + 1;
						if(findstartpos != -1) {
							int endpos = inhalt.indexOf("\"", findstartpos);
							if(endpos != -1) {
								aufzugstatus = inhalt.substring(findstartpos, endpos);
							} else {
								NVBWLogger.warning("String-Endepos von Property state wurde nicht gefunden, "
									+ "ab Pos " + startpos + ", " + inhalt.substring(startpos));
							}
						} else {
							NVBWLogger.warning("String-Startpos von Property state wurde nicht gefunden, "
								+ "ab Pos " + startpos + ", " + inhalt.substring(startpos));
						}
					} else {
						NVBWLogger.warning("Property state kann im Content nicht gefunden werden");
					}
					suchstring = "\"stateExplanation\"";
					startpos = inhalt.indexOf(suchstring);
					if(startpos != -1) {
						startpos += suchstring.length();
						int findstartpos = inhalt.indexOf("\"", startpos) + 1;
						if(findstartpos != -1) {
							int endpos = inhalt.indexOf("\"", findstartpos);
							if(endpos != -1) {
								aufzugstatus += "|" + inhalt.substring(findstartpos, endpos);
							} else {
								NVBWLogger.warning("String-Endepos von Property stateExplanation wurde nicht gefunden, "
									+ "ab Pos " + startpos + ", " + inhalt.substring(startpos));
							}
						} else {
							NVBWLogger.warning("String-Startpos von Property stateExplanation wurde nicht gefunden, "
								+ "ab Pos " + startpos + ", " + inhalt.substring(startpos));
						}
					} else {
						NVBWLogger.warning("Property stateExplanation kann im Content nicht gefunden werden");
					}
				} else {
					NVBWLogger.warning("Response-Content ist leer.");
				}
			
			} else {
				NVBWLogger.warning("HTTP-Response Code nicht 200, sondern " + responseCode
					+ ", für Bild-Url ===" + fastaurl + "===");
					// keine Url zurückgeben, weil nicht alles in Ordnung
			}
			rd.close();
			NVBWLogger.info("getFastaAufzugzustand liefert zurück: " + aufzugstatus);
		} catch (FileNotFoundException e) {
			NVBWLogger.finest("Bilddatei wurde nicht gefunden (FileNotFoundException)" + "\t"
					+ fastaurl + "\t" + e.toString());
				fastaurl = "";
		} catch (MalformedURLException e) {
			NVBWLogger.info("Fasta-API Zugriff war nicht erfolgreich  werden (MalformedURLException)" + "\t"
				+ fastaurl + "\t" + e.toString());
			fastaurl = "";
		} catch (ProtocolException e) {
			NVBWLogger.info("Fasta-API Zugriff war nicht erfolgreich  werden (ProtocolException)" + "\t"
				+ fastaurl + "\t" + e.toString());
			fastaurl = "";
		} catch (IOException e) {
			NVBWLogger.info("Fasta-API Zugriff war nicht erfolgreich  werden (ProtocolException)" + "\t"
					+ fastaurl + "\t" + e.toString());
		}
		return aufzugstatus;
    }
    
    /**
     * initialization on servlett startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
    	bfrkConn = DBVerbindung.getDBVerbindung();
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				System.out.println("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					response.getWriter().append("FEHLER: keine DB-Verbindung offen");
					return;
				}
			}
		} catch (SQLException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei SQLException " + e1.toString());
			return;
		} catch (IOException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei IOException " + e1.toString());
			return;
		}

		long paramObjektid = 0;
		if(request.getParameter("dhid") != null) {
			System.out.println("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramObjektid = Long.parseLong(request.getParameter("dhid"));
		} else {
			System.out.println("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			System.out.println("requesturi ===" + requesturi + "===");
			if(requesturi.indexOf("/aufzug") != -1) {
				int startpos = requesturi.indexOf("/aufzug");
				System.out.println("startpos #1: " + startpos);
				if(requesturi.indexOf("/",startpos + 1) != -1) {
					paramObjektid = Long.parseLong(requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1));
					System.out.println("Versuch, Objektid zu extrahieren ===" + paramObjektid + "===");
				}
			}
		}

		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		
		String selectHaltestelleSql = "SELECT CASE WHEN NOT korr.merkmal_id IS NULL THEN 'korrektur' ELSE 'erfassung' END AS quelle, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.merkmal_id ELSE erf.merkmal_id END as merkmal_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.objekt_id ELSE erf.objekt_id END as objekt_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.parent_id ELSE erf.parent_id END AS parent_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.name ELSE erf.name END AS name, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.beschreibung ELSE erf.beschreibung END AS beschreibung, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.wert ELSE erf.wert END AS wert, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.typ ELSE erf.typ END AS typ, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.dhid ELSE erf.dhid END AS dhid, "
			+ "CASE WHEN NOT korr.objektart IS NULL THEN korr.objektart ELSE erf.objektart END as objektart,"
			+ "fastaid, fastaosmid, "
			+ "erf.osmids, erf.osmlon, erf.osmlat, osmimportiert "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "m.name, m.wert, m.typ, o.dhid, o.objektart, fasta.fastaid, fasta.osmid AS fastaosmid, "
			+ "osm.osmid AS osmids, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_X(osm.koordinate) END AS osmlon, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_Y(osm.koordinate) END AS osmlat, "
			+ "osmimport.vollstaendig AS osmimportiert "
			+ "FROM merkmal AS m JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "LEFT JOIN aufzugfastaosmbezug AS fasta on o.id = fasta.objekt_id "
			+ "LEFT JOIN osmobjektbezug AS osm on osm.objekt_id = o.id "
			+ "LEFT JOIN osmimport on o.id = osmimport.objekt_id "
			+ "WHERE o.id = ?) AS erf "
			+ "FULL OUTER JOIN (SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, "
			+ "o.oevart, o.beschreibung, m.name, m.wert, m.typ, o.dhid, o.objektart "
			+ "FROM merkmalkorrektur AS m  JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "WHERE o.id = ?) AS korr "
			+ "ON erf.objekt_id = korr.objekt_id AND korr.name = erf.name;";

//		String selectHaltestelleSql = "SELECT * FROM merkmal_view WHERE objekt_id = ?;";

		JSONObject merkmaleJsonObject = new JSONObject();
		//merkmaleJsonObject.put("objektid", paramObjektid);		// evtl. in API in Response weglassen

		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			selectHaltestelleStmt.setLong(1, paramObjektid);
			selectHaltestelleStmt.setLong(2, paramObjektid);
			System.out.println("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			long fastaid = 0;
			
			int anzahldatensaetze = 0;
			boolean falscheobjektart = false;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				String objektart = selectMerkmaleRS.getString("objektart");
				String dhid = selectMerkmaleRS.getString("dhid");
				String name = selectMerkmaleRS.getString("name");
				String wert = selectMerkmaleRS.getString("wert");
				String typ = selectMerkmaleRS.getString("typ");
				fastaid = selectMerkmaleRS.getLong("fastaid");
				String fastaosmid = selectMerkmaleRS.getString("fastaosmid");
				String osmids = selectMerkmaleRS.getString("osmids");
				double osmlon = selectMerkmaleRS.getDouble("osmlon");
				double osmlat = selectMerkmaleRS.getDouble("osmlat");
				boolean osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");

				if(!objektart.equals("Aufzug")) {
					falscheobjektart = true;
					continue;
				}

					// übernehmen der OSM-Id aus Tabelle AufzugFastaOSM
				if((fastaosmid != null) && !fastaosmid.isEmpty())
					merkmaleJsonObject.put("OSMId", fastaosmid);

				if(name.equals("OBJ_Aufzug_Tuerbreite_cm_D2091"))
					merkmaleJsonObject.put("tuerbreite_cm", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Aufzug_Grundflaechenlaenge_cm_D2093"))
					merkmaleJsonObject.put("grundflaechenlaenge_cm", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Aufzug_Grundflaechenbreite_cm_D2094"))
					merkmaleJsonObject.put("grundflaechenbreite_cm", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Aufzug_Verbindungsfunktion_D2095"))
					merkmaleJsonObject.put("verbindungsfunktion", wert);
					// übernehmen als Attribut zum Aufzug-Objekt
				//else if(name.equals("OBJ_Aufzug_OSMID"))
				//	merkmaleJsonObject.put("OSMId", wert);
				
				else if(name.equals("OBJ_Aufzug_Foto"))
					merkmaleJsonObject.put("aufzug_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Aufzug_Ebene1_Foto"))
					merkmaleJsonObject.put("ebene1_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Aufzug_Ebene2_Foto"))
					merkmaleJsonObject.put("ebene2_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Aufzug_Ebene3_Foto"))
					merkmaleJsonObject.put("ebene3_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Aufzug_Bedienelemente_Foto"))
					merkmaleJsonObject.put("bedienelemente_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Aufzug_ID_Foto"))
					merkmaleJsonObject.put("schild_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Aufzug_StoerungKontakt_Foto"))
					merkmaleJsonObject.put("stoerungkontakt_Foto", Bild.getBildUrl(wert, dhid));
				else
					NVBWLogger.warning("in Servlet " + this.getServletName() 
						+ " nicht verarbeitetes Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");
				if(osmids != null) {
					merkmaleJsonObject.put("koordinatenqualitaet", "validierte-Position");
					merkmaleJsonObject.put("lon", osmlon);
					merkmaleJsonObject.put("lat", osmlat);
					List<String> osmlinksArray = OpenStreetMap.getHyperlinksAsArray(osmids);
					JSONArray osmlinksJA = new JSONArray();
					for(int osmlinkindex = 0; osmlinkindex < osmlinksArray.size(); osmlinkindex++)
						osmlinksJA.put(osmlinksArray.get(osmlinkindex));
					merkmaleJsonObject.put("osmlinks", osmlinksJA);
				} else
					merkmaleJsonObject.put("koordinatenqualitaet", "Objekt-Rohposition");
				merkmaleJsonObject.put("osmimportiert", osmimportiert);
			}
			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			if(fastaid != 0) {
				String aufzugstatus = getFastaAufzugzustand(fastaid);
				if((aufzugstatus != null) && !aufzugstatus.isEmpty()) {
					if(aufzugstatus.indexOf("|") != -1) {
						String statusteile[] = aufzugstatus.split("\\|",-1);
						if(statusteile.length > 0) {
							merkmaleJsonObject.put("aufzugstatus", statusteile[0]);
							merkmaleJsonObject.put("aufzugstatuserklaerung", statusteile[1]);
						}
					} else {
					}
					merkmaleJsonObject.put("aufzugstatuszeitpunkt", datetime_iso8601_formatter.format(new Date()));
				}
			}
			
			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if(falscheobjektart) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.addHeader("Application-Note", "Parameter objekt_id doesn't fit to object type");
				return;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException::: " + e.toString());
		}
		response.getWriter().append(merkmaleJsonObject.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append("POST Request ist nicht erlaubt");
		return;
	}

}
