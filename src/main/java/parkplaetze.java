
import java.io.IOException;
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

import de.nvbw.base.BFRKApiApplicationconfiguration;
import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "parkplaetze", 
			urlPatterns = {"/parkplaetze"}
		)
public class parkplaetze extends HttpServlet {
	private static DateFormat datetime_de_formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private static final long serialVersionUID = 1L;

	private static BFRKApiApplicationconfiguration bfrkapiconfiguration = null;
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public parkplaetze() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * initialization on servlett startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
		bfrkapiconfiguration = new BFRKApiApplicationconfiguration();
    	bfrkConn = DBVerbindung.getDBVerbindung();
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Date requestStart = new Date();
		Date requestEnde = null;

		NVBWLogger.info("parkplaetze Request-Beginn: " + datetime_de_formatter.format(requestStart));

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				NVBWLogger.warning("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
					response.setCharacterEncoding("UTF-8");
					response.getWriter().append("Datenbankverbindung verloren, bitte nochmal versuchen");
					return;
				}
			}
		} catch (Exception e1) {
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append("Datenbankverbindung verloren, bitte nochmal versuchen");
			return;
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
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.dhid ELSE erf.dhid END AS aktdhid, "
			+ "CASE WHEN NOT korr.objektart IS NULL THEN korr.objektart ELSE erf.objektart END as objektart, "
			+ "erf.objekt_id, erf.osmids, erf.lon, erf.lat, erf.gemeinde, erf.ortsteil, osmimportiert, "
			+ "erf.infraidtemp, "
			+ "hst.dhid AS hst_dhid, erf.merkmal_id "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "o.gemeinde, o.ortsteil, o.infraidtemp, "
			+ "m.name, m.wert, m.typ, o.dhid, o.objektart, "
			+ "osm.osmid AS osmids, "
			+ "ST_X(o.koordinate) AS lon, "
			+ "ST_Y(o.koordinate) AS lat, "
			+ "osmimport.vollstaendig AS osmimportiert "
			+ "FROM merkmal AS m JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "LEFT JOIN osmobjektbezug AS osm on osm.objekt_id = o.id "
			+ "LEFT JOIN osmimport on o.id = osmimport.objekt_id) AS erf "
			+ "FULL OUTER JOIN (SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, "
			+ "o.oevart, o.beschreibung, m.name, m.wert, m.typ, o.dhid, o.objektart "
			+ "FROM merkmalkorrektur AS m  JOIN objekt AS o "
			+ "ON m.objekt_id = o.id) AS korr "
			+ "ON erf.objekt_id = korr.objekt_id AND korr.name = erf.name "
			+ "JOIN objekt as hst ON erf.parent_id = hst.id "
			+ "WHERE erf.objektart = 'Parkplatz' "
			+ "ORDER BY aktdhid, erf.objekt_id;";

		JSONArray objektarray = new JSONArray();

		JSONObject merkmaleJsonObject = new JSONObject();
		//merkmaleJsonObject.put("objektid", paramObjektid);		// evtl. in API in Response weglassen

		int anzahlobjekte = 0;
		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			NVBWLogger.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			int anzahldatensaetze = 0;
			boolean falscheobjektart = false;
			long objektid = 0;
			long merkmal_id = 0;
			long vorherigeobjektid = 0;
			String objektart = "";
			String dhid = "";
			String name = "";
			String wert = "";
			String typ = "";
			String hstdhid = "";
			String gemeinde = "";
			String ortsteil = "";
			String infraid = "";
			String osmids = null;
			int maxparkdauer = -1;
			int frauenstellplaetze = 0;
			int familienstellplaetze = 0;
			String gebuehrenpflichtig = "unbekannt";
			double lon = 0.0;
			double lat = 0.0;
			boolean osmimportiert = false;
			while(selectMerkmaleRS.next()) {
				objektid = selectMerkmaleRS.getLong("objekt_id");
	
					// beim wechsel zum nächsten Objekt speichern des bisherigen Objekts
				if((objektid != vorherigeobjektid) && (vorherigeobjektid != 0)) {
					merkmaleJsonObject.put("lon", lon);
					merkmaleJsonObject.put("lat", lat);
					merkmaleJsonObject.put("osmimportiert", osmimportiert);
					if(osmids == null) {
						merkmaleJsonObject.put("koordinatenqualitaet", "Objekt-Rohposition");
					} else {
						merkmaleJsonObject.put("koordinatenqualitaet", "validierte-Position");
						List<String> osmlinksArray = OpenStreetMap.getHyperlinksAsArray(osmids);
						JSONArray osmlinksJA = new JSONArray();
						for(int osmlinkindex = 0; osmlinkindex < osmlinksArray.size(); osmlinkindex++)
							osmlinksJA.put(osmlinksArray.get(osmlinkindex));
						merkmaleJsonObject.put("osmlinks", osmlinksJA);
					}
					merkmaleJsonObject.put("maxparkdauer_min", maxparkdauer);
					merkmaleJsonObject.put("frauenstellplaetze", frauenstellplaetze);
					merkmaleJsonObject.put("familienstellplaetze", familienstellplaetze);
					merkmaleJsonObject.put("gebuehrenpflichtig", gebuehrenpflichtig);
	
					NVBWLogger.info("Länge merkmaleJsonObject: " + merkmaleJsonObject.toString().length());
					NVBWLogger.info("Inhalt merkmaleJsonObject: " + merkmaleJsonObject.toString());
					objektarray.put(merkmaleJsonObject);
					NVBWLogger.info("Objektarray-Länge nach Erweiterung: " + objektarray.toString().length());
					merkmaleJsonObject = new JSONObject();
					lon = 0.0;
					lat = 0.0;
					osmimportiert = false;
					osmids = "";
					maxparkdauer = -1;
					frauenstellplaetze = 0;
					familienstellplaetze = 0;
					gebuehrenpflichtig = "unbekannt";
				}
	
				// bei jedem neuen Objekt zuerst diese Felder füllen
				if((objektid != vorherigeobjektid) || (vorherigeobjektid == 0)) {
					anzahlobjekte++;

					objektart = selectMerkmaleRS.getString("objektart");
					dhid = selectMerkmaleRS.getString("aktdhid");
					hstdhid = selectMerkmaleRS.getString("hst_dhid");
					gemeinde = selectMerkmaleRS.getString("gemeinde");
					ortsteil = selectMerkmaleRS.getString("ortsteil");
					infraid = selectMerkmaleRS.getString("infraidtemp");
					osmids = selectMerkmaleRS.getString("osmids");
					lon = selectMerkmaleRS.getDouble("lon");
					lat = selectMerkmaleRS.getDouble("lat");
					osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");

					merkmaleJsonObject.put("objekt_dhid", dhid);
					merkmaleJsonObject.put("hst_dhid", hstdhid);
					merkmaleJsonObject.put("objektid", objektid);
					merkmaleJsonObject.put("gemeinde", gemeinde);
					merkmaleJsonObject.put("ortsteil", ortsteil);
					merkmaleJsonObject.put("infraid", infraid);
				}
				anzahldatensaetze++;

				merkmal_id = selectMerkmaleRS.getLong("merkmal_id");
				name = selectMerkmaleRS.getString("name");
				wert = selectMerkmaleRS.getString("wert");
				typ = selectMerkmaleRS.getString("typ");
				NVBWLogger.info("objektid: " + objektid + ", Name: " + name + ", Wert: " + wert + ", Typ: " + typ);

				if(!objektart.equals("Parkplatz")) {
					falscheobjektart = true;
					vorherigeobjektid = objektid;
					continue;
				}

				if(name.equals("OBJ_Parkplatz_Art_D1051")) {

					if(wert.equals("behindertenplaetze")) {
						wert = "Behindertenplätze";
					} else if(wert.equals("kurzzeitplaetze")) {
						wert = "Kurzzeit";
					} else if(wert.equals("park_ride")) {
						wert = "Park+Ride";
					} else if(wert.equals("Parkhaus"))
						wert = "Parkhaus";
					else if(wert.equals("parkplatz_ohne_parkride"))
						wert = "Parkplatz_ohne_Park+Ride";
					merkmaleJsonObject.put("art", wert);
				} else if(name.equals("OBJ_Parkplatz_oeffentlichVorhanden_D1050"))
					merkmaleJsonObject.put("oeffentlichvorhanden", wert.equals("true"));
				else if(name.equals("OBJ_Parkplatz_Kapazitaet"))
					merkmaleJsonObject.put("stellplaetzegesamt", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Parkplatz_BehindertenplaetzeKapazitaet"))
					merkmaleJsonObject.put("behindertenstellplaetze", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Parkplatz_Bedingungen_D1052"))
					merkmaleJsonObject.put("bedingungen", wert);
				else if(name.equals("OBJ_Parkplatz_Eigentuemer"))
					merkmaleJsonObject.put("eigentuemer", wert);
				else if(name.equals("OBJ_Parkplatz_Foto_Kommentar"))
					merkmaleJsonObject.put("fotokommentar", wert);
				else if(name.equals("OBJ_Parkplatz_Lon")) {
					if(lon == 0.0)
						lon = Double.parseDouble(wert);
				} else if(name.equals("OBJ_Parkplatz_Lat")) {
					if(lat == 0.0)
						lat = Double.parseDouble(wert);
				} else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Lon"))
					merkmaleJsonObject.put("behindertenplaetze_lon", Double.parseDouble(wert));
				else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Lat"))
					merkmaleJsonObject.put("behindertenplaetze_lat", Double.parseDouble(wert));

				else if(name.equals("OBJ_Parkplatz_Bauart"))
					merkmaleJsonObject.put("bauart", wert);
				else if(name.equals("OBJ_Parkplatz_Orientierung"))
					merkmaleJsonObject.put("orientierung", wert);
				else if(name.equals("OBJ_Parkplatz_MaxParkdauer_min")) {
					// Parkdauer hier erstmal in Variable speichern, beim Wechsel des Objekts rausschreiben
					try {
						maxparkdauer = (int) Double.parseDouble(wert);
						NVBWLogger.info("Merkmal: " + name + ", Max-Parkdauer: " + maxparkdauer);
					} catch(NumberFormatException e) {
						NVBWLogger.warning("Merkmal OBJ_Parkplatz_MaxParkdauer_min, DB-ID: " 
							+ merkmal_id + ", Wert nicht parsebar '" + wert + "'");
						maxparkdauer = -1;
						NVBWLogger.info("Merkmal: " + name + ", Max-Parkdauer: " + maxparkdauer + " in NumberFormatException!");
					}
				} else if(name.equals("OBJ_Parkplatz_KapazitaetFrauenplaetze")) {
					// Frauenstellplätze hier erstmal in Variable speichern, beim Wechsel des Objekts rausschreiben
					try {
						frauenstellplaetze = (int) Double.parseDouble(wert);
					} catch(NumberFormatException e) {
						NVBWLogger.warning("Merkmal OBJ_Parkplatz_KapazitaetFrauenplaetze, DB-ID: "
							+ merkmal_id + ", Wert nicht parsebar '" + wert + "'");
						frauenstellplaetze = 0;
					}
				} else if(name.equals("OBJ_Parkplatz_KapazitaetFamilienplaetze")) {
					// Familienstellplätze hier erstmal in Variable speichern, beim Wechsel des Objekts rausschreiben
					try {
						familienstellplaetze = (int) Double.parseDouble(wert);
					} catch(NumberFormatException e) {
						NVBWLogger.warning("Merkmal OBJ_Parkplatz_KapazitaetFamilienplaetze, DB-ID: "
							+ merkmal_id + ", Wert nicht parsebar '" + wert + "'");
						familienstellplaetze = 0;
					}
				} else if(name.equals("OBJ_Parkplatz_Gebuehrenpflichtig_jn")) {
					// Gebührenpflichtig hier erstmal in Variable speichern, beim Wechsel des Objekts rausschreiben
					if(wert.equals("true"))
						gebuehrenpflichtig = "ja";
					else
						gebuehrenpflichtig = "nein";
				} else if(name.equals("OBJ_Parkplatz_Tarife")) {
					String wertString = wert;
					if((wertString != null) && !wertString.isEmpty())
						wertString = wertString.replace("€", "EUR");
					merkmaleJsonObject.put("gebuehrenbeispiele", wertString);
				} else if(name.equals("OBJ_Parkplatz_OeffnungszeitenOSM")) {
					merkmaleJsonObject.put("oeffnungszeiten", wert);
				} else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Laenge_cm")) {
					try {
						merkmaleJsonObject.put("behindertenstellplaetze_laenge_cm", (int) Double.parseDouble(wert));
					} catch(NumberFormatException e) {
						NVBWLogger.warning("Merkmal OBJ_Parkplatz_Behindertenplaetze_Laenge_cm, DB-ID: " 
							+ merkmal_id + ", Wert nicht parsebar '" + wert + "'");
					}
				} else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Breite_cm")) {
					try {
						merkmaleJsonObject.put("behindertenstellplaetze_breite_cm", (int) Double.parseDouble(wert));
					} catch(NumberFormatException e) {
						NVBWLogger.warning("Merkmal OBJ_Parkplatz_Behindertenplaetze_Breite_cm, DB-ID: " 
							+ merkmal_id + ", Wert nicht parsebar '" + wert + "'");
					}
				} else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Laenge_cm_Kommentar")) {
					merkmaleJsonObject.put("behindertenstellplaetze_laenge_kommentar", wert);
				} else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Breite_cm_Kommentar")) {
					merkmaleJsonObject.put("behindertenstellplaetze_breite_kommentar", wert);
				} else if(name.equals("OBJ_Parkplatz_offen247_jn")) {
					merkmaleJsonObject.put("offen_24_7", wert.equals("true"));

				} else if(name.equals("OBJ_Parkplatz_Foto"))
					merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, hstdhid));
				else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Foto"))
					merkmaleJsonObject.put("behindertenplaetze_Foto", Bild.getBildUrl(wert, hstdhid));
				else if(name.equals("OBJ_Parkplatz_Nutzungsbedingungen_Foto"))
					merkmaleJsonObject.put("nutzungsbedingungen_Foto", Bild.getBildUrl(wert, hstdhid));
				else if(name.equals("OBJ_Parkplatz_Oeffnungszeiten_Foto"))
					merkmaleJsonObject.put("oeffnungszeiten_Foto", Bild.getBildUrl(wert, hstdhid));
				else if(name.equals("OBJ_Parkplatz_WegzuHaltestelle_Foto"))
					merkmaleJsonObject.put("wegzuhaltestelle_Foto", Bild.getBildUrl(wert, hstdhid));
				else if(name.equals("OBJ_Parkplatz_Frauenplaetze_Foto"))
					merkmaleJsonObject.put("frauenstellplaetze_Foto", Bild.getBildUrl(wert, hstdhid));
				else if(name.equals("OBJ_Parkplatz_Familienplaetze_Foto"))
					merkmaleJsonObject.put("familienstellplaetze_Foto", Bild.getBildUrl(wert, hstdhid));
				
				else {
					NVBWLogger.warning("in Servlet " + this.getServletName() 
						+ " nicht verarbeitetes Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");
				}

				vorherigeobjektid = objektid;
			} // end of Schleife über alle Datensatzmerkmale

				// letzten Datensatz noch ergänzen, wenn gefüllt
			if(objektid != 0) {
				anzahlobjekte++;

				merkmaleJsonObject.put("lon", lon);
				merkmaleJsonObject.put("lat", lat);
				merkmaleJsonObject.put("osmimportiert", osmimportiert);
				if(osmids == null) {
					merkmaleJsonObject.put("koordinatenqualitaet", "Objekt-Rohposition");
				} else {
					merkmaleJsonObject.put("koordinatenqualitaet", "Final");
					List<String> osmlinksArray = OpenStreetMap.getHyperlinksAsArray(osmids);
					JSONArray osmlinksJA = new JSONArray();
					for(int osmlinkindex = 0; osmlinkindex < osmlinksArray.size(); osmlinkindex++)
						osmlinksJA.put(osmlinksArray.get(osmlinkindex));
					merkmaleJsonObject.put("osmlinks", osmlinksJA);
				}
				merkmaleJsonObject.put("maxparkdauer_min", maxparkdauer);
				merkmaleJsonObject.put("frauenstellplaetze", frauenstellplaetze);
				merkmaleJsonObject.put("familienstellplaetze", familienstellplaetze);
				merkmaleJsonObject.put("gebuehrenpflichtig", gebuehrenpflichtig);

				NVBWLogger.info("Länge merkmaleJsonObject: " + merkmaleJsonObject.toString().length());
				objektarray.put(merkmaleJsonObject);
				NVBWLogger.info("Objektarray-Länge nach Erweiterung: " + objektarray.toString().length());
			}


			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			requestEnde = new Date();

			if(anzahldatensaetze == 0) {
				NVBWLogger.info("keine Datensätze gefunden, ENDE Request: " 
					+ datetime_de_formatter.format(requestEnde));
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if(falscheobjektart) {
				NVBWLogger.warning("falsche Objektart, ENDE Request: " 
					+ datetime_de_formatter.format(requestEnde));
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setCharacterEncoding("UTF-8");
				response.getWriter().append("Parameter objekt_id passt nicht zum Objekttyp Parkplatz");
				return;
			}
		} catch (SQLException e) {
			NVBWLogger.severe("SQLException::: " + e.toString());
		}
		response.getWriter().append(objektarray.toString());
		NVBWLogger.info("Objektarray-Länge am Ende: " + objektarray.toString().length()
			+ ", " + datetime_de_formatter.format(requestEnde));
		NVBWLogger.info("Am Ende Anzahl Objekte in objektarray: " + anzahlobjekte);
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NVBWLogger.info("Request /parkplaetze, doPost ...");

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append("POST Request ist nicht erlaubt");
		return;
	}
}
