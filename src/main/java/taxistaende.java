
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
@WebServlet(name = "taxistaende", 
			urlPatterns = {"/taxistaende"}
		)
public class taxistaende extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static DateFormat datetime_de_formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	private static DateFormat date_rfc3339_formatter = new SimpleDateFormat("yyyy-MM-dd");


	private static BFRKApiApplicationconfiguration bfrkapiconfiguration = null;
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public taxistaende() {
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

		System.out.println("Request-Beginn: " + datetime_de_formatter.format(requestStart));

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				System.out.println("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
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
			+ "erf.infraidtemp, erf.erfassungsdatum, "
			+ "hst.dhid AS hst_dhid "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "o.gemeinde, o.ortsteil, o.infraidtemp, o.erfassungsdatum, "
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
			+ "WHERE erf.objektart = 'Taxistand' "
			+ "ORDER BY aktdhid, erf.objekt_id;";

		JSONArray objektarray = new JSONArray();

		JSONObject merkmaleJsonObject = new JSONObject();
		//merkmaleJsonObject.put("objektid", paramObjektid);		// evtl. in API in Response weglassen

		int anzahlobjekte = 0;
		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			System.out.println("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			long objektid = 0;
			int anzahldatensaetze = 0;
			boolean falscheobjektart = false;
			StringBuffer osmTags = new StringBuffer();
			double lon = 0.0;
			double lat = 0.0;
			String osmaccess = "";
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
			Date erfassungsdatum = null;
			boolean osmimportiert = false;
			while(selectMerkmaleRS.next()) {
				objektid = selectMerkmaleRS.getLong("objekt_id");
NVBWLogger.info("jetzt kommt objektid: " + objektid + ", vorherigeobjektid ist: " + vorherigeobjektid);

					// Bei Objektwechsel zuerst die Daten in JsonObject sammeln und ins JsonArray ergänzen
				if((objektid != vorherigeobjektid) && (vorherigeobjektid != 0)) {
				NVBWLogger.info(" ok objektid hat sich geändert, lon ist: " + lon + ", lat ist: " + lat + " vor speicher in jsonobject");
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
				
					System.out.println("Länge merkmaleJsonObject: " + merkmaleJsonObject.toString().length());
					objektarray.put(merkmaleJsonObject);
					System.out.println("objektarray nach Erweiterung: " + objektarray.toString().length());
					merkmaleJsonObject = new JSONObject();
					lon = 0.0;
					lat = 0.0;
					osmimportiert = false;
					osmids = "";
				}

					// bei jedem neuen Objekt zuerst einige Stammfelder füllen
				if((objektid != vorherigeobjektid) || (vorherigeobjektid == 0)) {
					anzahlobjekte++;
					
					objektart = selectMerkmaleRS.getString("objektart");
					dhid = selectMerkmaleRS.getString("aktdhid");
					hstdhid = selectMerkmaleRS.getString("hst_dhid");
					gemeinde = selectMerkmaleRS.getString("gemeinde");
					ortsteil = selectMerkmaleRS.getString("ortsteil");
					infraid = selectMerkmaleRS.getString("infraidtemp");
					osmids = selectMerkmaleRS.getString("osmids");
					erfassungsdatum = selectMerkmaleRS.getDate("erfassungsdatum");
					lon = selectMerkmaleRS.getDouble("lon");
					lat = selectMerkmaleRS.getDouble("lat");
					osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");

					merkmaleJsonObject.put("objekt_dhid", dhid);
					merkmaleJsonObject.put("hst_dhid", hstdhid);
					merkmaleJsonObject.put("objektid", objektid);
					merkmaleJsonObject.put("gemeinde", gemeinde);
					merkmaleJsonObject.put("ortsteil", ortsteil);
					merkmaleJsonObject.put("infraid", infraid);
					if(erfassungsdatum != null)
						merkmaleJsonObject.put("erfassungsdatum", date_rfc3339_formatter.format(erfassungsdatum));

					NVBWLogger.info("ok, bin bei Objekt-Erstbefüllung, lon: " + lon + ", lat: " + lat);
					System.out.println("Neues Objekt #" + anzahlobjekte + " gefunden, gefunden HST-DHID: " + hstdhid 
					+ ", Objekt-DHID: " + dhid + ", Objektart: " + objektart
					+ ", Objekt-ID: " + objektid + ", OSM-Importiert? " + osmimportiert);
				}
				anzahldatensaetze++;

				name = selectMerkmaleRS.getString("name");
				wert = selectMerkmaleRS.getString("wert");
				typ = selectMerkmaleRS.getString("typ");

				if(!objektart.equals("Taxistand")) {
					falscheobjektart = true;
					vorherigeobjektid = objektid;
					continue;
				}

				System.out.println("Merkmal name zu verarbeiten: " + name + ", mit Wert ===" + wert + "===");
				if(name.equals("OBJ_Taxistand_Lon")) {
					if(lon == 0.0) {
						lon = Double.parseDouble(wert);
						NVBWLogger.info("bei Merkmalbearbeitung ..Lon, lon war noch 0.0, wird jetzt gesetzt mit: " + lon);
					}
				} else if(name.equals("OBJ_Taxistand_Lat")) {
					if(lat == 0.0) {
						lat = Double.parseDouble(wert);
						NVBWLogger.info("bei Merkmalbearbeitung ..Lat, lat war noch 0.0, wird jetzt gesetzt mit: " + lat);
					}
				} else if(name.equals("OBJ_Taxistand_Foto")) {
					merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, hstdhid));
				} else if(name.equals("OBJ_Taxistand_WegzurHaltestelle_Foto")) {
					merkmaleJsonObject.put("wegzuhaltestelle_Foto", Bild.getBildUrl(wert, hstdhid));
				} else {
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
					merkmaleJsonObject.put("koordinatenqualitaet", "validierte-Position");
					List<String> osmlinksArray = OpenStreetMap.getHyperlinksAsArray(osmids);
					JSONArray osmlinksJA = new JSONArray();
					for(int osmlinkindex = 0; osmlinkindex < osmlinksArray.size(); osmlinkindex++)
						osmlinksJA.put(osmlinksArray.get(osmlinkindex));
					merkmaleJsonObject.put("osmlinks", osmlinksJA);
				}
	
				System.out.println("Länge merkmaleJsonObject: " + merkmaleJsonObject.toString().length() + " Bytes");
				objektarray.put(merkmaleJsonObject);
				System.out.println("objektarray nach Erweiterung: " + objektarray.toString().length() + " Bytes");
				System.out.println("Am Ende Anzahl Objekte in objektarray: " + anzahlobjekte);
			}
	

/*			StringBuffer osmObjekt = new StringBuffer();
			osmObjekt.append("<?xml version='1.0' encoding='UTF-8'?>\r\n");
			osmObjekt.append("<osm version='0.6' generator='NVBW Haltestellenimport' upload='never' download='never'>\r\n");
			osmObjekt.append("<node id='-1' "
					+ "lat='" + lat + "' lon='" + lon + "'>\r\n");
			osmObjekt.append("<tag k='amenity' v='taxi'></tag>\r\n");
			osmObjekt.append(osmTags);
			if(!osmaccess.isEmpty())
				osmObjekt.append("<tag k='access' v='" + osmaccess + "'></tag>\r\n");
			osmObjekt.append("</node>\r\n");
			osmObjekt.append("</osm>\r\n");
			merkmaleJsonObject.put("osmtagging", osmObjekt.toString());

			String josmlink = "http://localhost:8111/load_data?&new_layer=false&"
				+ "download_policy=never&upload_policy=never&data=" 
				+ URLEncoder.encode(osmObjekt.toString(), StandardCharsets.UTF_8.toString());
			merkmaleJsonObject.put("josmimportlink", josmlink);
*/
			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			requestEnde = new Date();

			if(anzahldatensaetze == 0) {
				System.out.println("keine Datensätze gefunden, ENDE Request: " 
					+ datetime_de_formatter.format(requestEnde));
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if(falscheobjektart) {
				System.out.println("falsche Objektart, ENDE Request: " 
					+ datetime_de_formatter.format(requestEnde));
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setCharacterEncoding("UTF-8");
				response.getWriter().append("Parameter objekt_id passt nicht zum Objekttyp Taxistand");
				return;
			}
		} catch (SQLException e) {
			System.out.println("SQLException::: " + e.toString());
		}
		response.getWriter().append(objektarray.toString());
		System.out.println("objektarray am Ende: " + objektarray.toString().length()
			+ ", " + datetime_de_formatter.format(requestEnde));
		System.out.println("Am Ende Anzahl Objekte in objektarray: " + anzahlobjekte);
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Request /taxistaende, doPost ...");

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append("POST Request ist nicht erlaubt");
		return;
	}
}
