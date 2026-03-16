
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "aufzug", 
			urlPatterns = {"/aufzug/*"}
		)
public class aufzug extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(stopmodell.class);
	private static Connection bfrkConn = null;

	private static DateFormat date_rfc3339_formatter = new SimpleDateFormat("yyyy-MM-dd");


    /**
     * @see HttpServlet#HttpServlet()
     */
    public aufzug() {
        super();
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
				LOG.warning("keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					LOG.severe("es konnte keine DB-Verbindung herstellt werden, in aufzug doGet");
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren");
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		} catch (SQLException e1) {
			LOG.severe("SQLException aufgetreten in aufzug doGet, " + e1.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e1) {
			LOG.severe("IOException aufgetreten in aufzug doGet, " + e1.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unbekannter Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		long paramObjektid = 0;
		if(request.getParameter("dhid") != null) {
			LOG.info("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramObjektid = Long.parseLong(request.getParameter("dhid"));
		} else {
			LOG.info("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			LOG.info("requesturi ===" + requesturi + "===");
			if(requesturi.contains("/aufzug")) {
				int startpos = requesturi.indexOf("/aufzug");
				LOG.info("startpos #1: " + startpos);
				if(requesturi.indexOf("/",startpos + 1) != -1) {
					paramObjektid = Long.parseLong(requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1));
					LOG.info("Versuch, Objektid zu extrahieren ===" + paramObjektid + "===");
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
			+ "CASE WHEN NOT korr.objektart IS NULL THEN korr.objektart ELSE erf.objektart END as objektart, "
			+ "CASE WHEN NOT erf.osm_lon IS NULL THEN 'koord_osm' ELSE 'koord_objekt' END AS koord_art, "
			+ "CASE WHEN NOT erf.osm_lon IS NULL THEN erf.osm_lon ELSE erf.o_lon END AS lon, "
			+ "CASE WHEN NOT erf.osm_lat IS NULL THEN erf.osm_lat ELSE erf.o_lat END AS lat, "
			+ "erf.osmids, osmimportiert, erf.erfassungsdatum "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "m.name, m.wert, m.typ, o.dhid, o.objektart, o.erfassungsdatum, "
			+ "osm.osmid AS osmids, "
			+ "ST_X(o.koordinate) AS o_lon, ST_Y(o.koordinate) AS o_lat, "
			+ "osmimport.vollstaendig AS osmimportiert "
			+ "FROM merkmal AS m JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "LEFT JOIN osmobjektbezug AS osm on o.id = osm.objekt_id "
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
			LOG.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			int anzahldatensaetze = 0;
			long objektid = 0;
			boolean osmimportiert = false;
			String objektart = "";
			String dhid = "";
			String name = "";
			String wert = "";
			String typ = "";
			String osmids = null;
			Date erfassungsdatum = null;
			double lon = 0.0;
			double lat = 0.0;
			boolean falscheobjektart = false;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				objektid = selectMerkmaleRS.getLong("objekt_id");

				objektart = selectMerkmaleRS.getString("objektart");
				dhid = selectMerkmaleRS.getString("dhid");
				name = selectMerkmaleRS.getString("name");
				wert = selectMerkmaleRS.getString("wert");
				typ = selectMerkmaleRS.getString("typ");
				erfassungsdatum = selectMerkmaleRS.getDate("erfassungsdatum");
				osmids = selectMerkmaleRS.getString("osmids");
				lon = selectMerkmaleRS.getDouble("lon");
				lat = selectMerkmaleRS.getDouble("lat");
				osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");

				if(!objektart.equals("Aufzug")) {
					falscheobjektart = true;
					continue;
				}

				merkmaleJsonObject.put("objektid", objektid);
				merkmaleJsonObject.put("lon", lon);
				merkmaleJsonObject.put("lat", lat);

				if(erfassungsdatum != null)
					merkmaleJsonObject.put("erfassungsdatum", date_rfc3339_formatter.format(erfassungsdatum));

				if(name.equals("OBJ_Aufzug_Tuerbreite_cm_D2091"))
					merkmaleJsonObject.put("tuerbreite_cm", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Aufzug_Grundflaechenlaenge_cm_D2093"))
					merkmaleJsonObject.put("grundflaechenlaenge_cm", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Aufzug_Grundflaechenbreite_cm_D2094"))
					merkmaleJsonObject.put("grundflaechenbreite_cm", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Aufzug_Verbindungsfunktion_D2095"))
					merkmaleJsonObject.put("verbindungsfunktion", wert);
				else if(name.equals("OBJ_Aufzug_OSMID"))
					merkmaleJsonObject.put("OSMId", wert);
				
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
					LOG.warning("in Servlet " + this.getServletName() 
						+ " nicht verarbeitetes Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");

				if(osmids != null) {
					merkmaleJsonObject.put("koordinatenqualitaet", "validierte-Position");
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
			LOG.severe("SQLException::: " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten, bitte Administrator informieren: "
				+ e.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		response.getWriter().append(merkmaleJsonObject.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOG.info("Request angekommen in /aufzug doPost ...");

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /aufzug ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
}
