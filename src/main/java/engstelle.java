
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.nvbw.base.NVBWLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.base.Applicationconfiguration;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "engstelle", 
			urlPatterns = {"/engstelle/*"}
		)
public class engstelle extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(engstelle.class);
	private static Applicationconfiguration configuration = new Applicationconfiguration();
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public engstelle() {
        super();
    }

    /**
     * initialization on servlet startup
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
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren");
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		} catch (Exception e1) {
			LOG.severe("Exception aufgetreten in engstelle doGet, " + e1.toString());
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
			System.out.println("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramObjektid = Long.parseLong(request.getParameter("dhid"));
		} else {
			System.out.println("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			System.out.println("requesturi ===" + requesturi + "===");
			if(requesturi.contains("/engstelle")) {
				int startpos = requesturi.indexOf("/engstelle");
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
			+ "CASE WHEN NOT korr.objektart IS NULL THEN korr.objektart ELSE erf.objektart END as objektart, "
			+ "erf.osmids, erf.osmlon, erf.osmlat, osmimportiert "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "m.name, m.wert, m.typ, o.dhid, o.objektart, "
			+ "osm.osmid AS osmids, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_X(osm.koordinate) END AS osmlon, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_Y(osm.koordinate) END AS osmlat, "
			+ "osmimport.vollstaendig AS osmimportiert "
			+ "FROM merkmal AS m JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
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

			int anzahldatensaetze = 0;
			boolean falscheobjektart = false;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				String objektart = selectMerkmaleRS.getString("objektart");
				String dhid = selectMerkmaleRS.getString("dhid");
				String name = selectMerkmaleRS.getString("name");
				String wert = selectMerkmaleRS.getString("wert");
				String typ = selectMerkmaleRS.getString("typ");

				String osmids = selectMerkmaleRS.getString("osmids");
				double osmlon = selectMerkmaleRS.getDouble("osmlon");
				double osmlat = selectMerkmaleRS.getDouble("osmlat");
				boolean osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");

				if(!objektart.equals("Engstelle")) {
					falscheobjektart = true;
					continue;
				}

                switch (name) {
                    case "OBJ_Engstelle_Durchgangsbreite_cm_D2080" ->
                            merkmaleJsonObject.put("durchgangsbreite_cm", (int) Double.parseDouble(wert));
                    case "OBJ_Engstelle_Bewegflaeche_cm_D2081" ->
                            merkmaleJsonObject.put("bewegflaeche_cm", (int) Double.parseDouble(wert));
                    case "OBJ_Engstelle_Foto" -> merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, dhid));
                    case "OBJ_Engstelle_Weg1_Foto" ->
                            merkmaleJsonObject.put("richtung1_Foto", Bild.getBildUrl(wert, dhid));
                    case "OBJ_Engstelle_Weg2_Foto" ->
                            merkmaleJsonObject.put("richtung2_Foto", Bild.getBildUrl(wert, dhid));
                    default -> LOG.warning("in Servlet " + this.getServletName()
                            + " nicht verarbeitetes Merkmal Name '" + name + "'"
                            + ", Wert '" + wert + "'");
                }

				if(osmids != null) {
					merkmaleJsonObject.put("koordinatenqualitaet", "validierte-Position");
					merkmaleJsonObject.put("lon", osmlon);
					merkmaleJsonObject.put("lat", osmlat);
					List<String> osmlinksArray = OpenStreetMap.getHyperlinksAsArray(osmids);
					JSONArray osmlinksJA = new JSONArray();
                    for (String s : osmlinksArray) osmlinksJA.put(s);
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
			ergebnisJsonObject.put("fehlertext", "SQL-DB Fehler aufgetreten, bitte Administrator informieren");
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

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /engstelle ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
}
