
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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
@WebServlet(name = "filter", 
			urlPatterns = {"/filter/*"}
		)
public class filter extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static DateFormat date_rfc3339_formatter = new SimpleDateFormat("yyyy-MM-dd");

    private static Connection bfrkConn = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public filter() {
        super();
        // TODO Auto-generated constructor stub
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

		JSONObject resultObjectJson = new JSONObject();
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String filtertext = "";
		if(		(request.getParameter("filter") != null)
			&&	(!request.getParameter("filter").isEmpty())) {
			System.out.println("url-Parameter filter vorhanden ===" + request.getParameter("filter"));
			filtertext = request.getParameter("filter");
		} else {
			System.out.println("kein zulässiger Parameter vorhanden ...");
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request parameter filter fehlt");
			errorObjektJson.put("message", "Der Parameter filter ist nicht vorhanden oder leer");
			errorObjektJson.put("messageId", 9994711);
			resultObjectJson.put("error", errorObjektJson);
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		//"StopArea":"" , "ID" :"de:08111:2235" , "StopPointID":""

//TODO die 3 Parameter analysieren
String paramstopDHID = "de:08116:7800";
String paramstoppointDHID = "%";
		
		String selectHaltestelleSql = "SELECT CASE WHEN NOT korr.merkmal_id IS NULL THEN 'korrektur' ELSE 'erfassung' END AS quelle, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.merkmal_id ELSE erf.merkmal_id END as merkmal_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.objekt_id ELSE erf.objekt_id END as objekt_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.parent_id ELSE erf.parent_id END AS parent_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.name ELSE erf.merkmalname END AS merkmalname, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.beschreibung ELSE erf.beschreibung END AS beschreibung, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.wert ELSE erf.merkmalwert END AS merkmalwert, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.typ ELSE erf.merkmaltyp END AS merkmaltyp, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.dhid ELSE erf.objekt_dhid END AS objekt_dhid, "
			+ "CASE WHEN NOT korr.objektart IS NULL THEN korr.objektart ELSE erf.objektart END as objektart, "
			+ "erf.osmids, erf.osmlon, erf.osmlat, osmimportiert, erf.erfassungsdatum "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, "
			+ "hst.id AS hst_id, hst.dhid AS hst_dhid, "
			+ "o.oevart, o.beschreibung, "
			+ "m.name AS merkmalname, m.wert AS merkmalwert, m.typ AS merkmaltyp, o.dhid AS objekt_dhid, o.objektart, o.erfassungsdatum, "
			+ "osm.osmid AS osmids, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_X(osm.koordinate) END AS osmlon, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_Y(osm.koordinate) END AS osmlat, "
			+ "osmimport.vollstaendig AS osmimportiert "
			+ "FROM merkmal AS m "
			+ "JOIN objekt AS o ON m.objekt_id = o.id "
			+ "JOIN objekt AS hst ON o.parent_id = hst.id "
			+ "LEFT JOIN osmobjektbezug AS osm on o.id = osm.objekt_id "
			+ "LEFT JOIN osmimport on o.id = osmimport.objekt_id "
			+ "WHERE "
			+ "hst.dhid like ?"
			+ "AND o.dhid like ? "
			+ ") AS erf "
			+ "FULL OUTER JOIN (SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, "
			+ "o.oevart, o.beschreibung, m.name, m.wert, m.typ, o.dhid, o.objektart "
			+ "FROM merkmalkorrektur AS m  JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "WHERE o.id = ?) AS korr "
			+ "ON erf.objekt_id = korr.objekt_id AND korr.name = erf.name "
//TODO order muss noch anders gesetzt werden
			+ "ORDER BY erf.hst_dhid, erf.objektart, erf.objekt_id;";


		JSONArray resultJsonArray = new JSONArray();
		JSONObject resultJsonObjekt = new JSONObject();
		JSONArray aufzuegeJsonArray = new JSONArray();
		JSONObject aufzugJsonObjekt = new JSONObject();

		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			selectHaltestelleStmt.setString(1, paramstopDHID);
			selectHaltestelleStmt.setString(2, paramstoppointDHID);
			System.out.println("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

int anzahldatensaetze = 0;
			long aktuelleHstObjektID = 0;
			long aktuelleObjektID = 0;
			long vorherigeHstObjektID = 0;
			long vorherigeObjektID = 0;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				aktuelleHstObjektID = selectMerkmaleRS.getLong("hst_id");
				aktuelleObjektID = selectMerkmaleRS.getLong("objekt_id");
				String objektart = selectMerkmaleRS.getString("objektart");
				String hstDHID = selectMerkmaleRS.getString("hst_dhid");
				String steigDHID = selectMerkmaleRS.getString("objekt_dhid");
				String merkmalname = selectMerkmaleRS.getString("merkmalname");
				String merkmalwert = selectMerkmaleRS.getString("merkmalwert");
				String merkmaltyp = selectMerkmaleRS.getString("merkmaltyp");
				Date erfassungsdatum = selectMerkmaleRS.getDate("erfassungsdatum");
				String osmids = selectMerkmaleRS.getString("osmids");
				double osmlon = selectMerkmaleRS.getDouble("osmlon");
				double osmlat = selectMerkmaleRS.getDouble("osmlat");
				boolean osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");

				if((aktuelleHstObjektID != vorherigeHstObjektID) && (vorherigeHstObjektID != -1)) {
					resultJsonArray.put(resultJsonObjekt);
					resultJsonObjekt = new JSONObject();
					// alle Objekte und Array innerhalb resultJsonObjekt initialisieren
					aufzuegeJsonArray = new JSONArray();
					aufzugJsonObjekt = new JSONObject();
				}

				if(objektart.equals("Aufzug")) {
					if((aktuelleObjektID != vorherigeObjektID) && (vorherigeObjektID != -1)) {
						aufzuegeJsonArray.put(aufzugJsonObjekt);
						aufzugJsonObjekt = new JSONObject();
					}

					if(osmids != null) {
						aufzugJsonObjekt.put("CoordX", osmlon);
						aufzugJsonObjekt.put("CoordY", osmlat);
					}

					if(merkmalname.equals("OBJ_Aufzug_Tuerbreite_cm_D2091"))
						aufzugJsonObjekt.put("Width", (int) Double.parseDouble(merkmalwert));
					else if(merkmalname.equals("OBJ_Aufzug_Grundflaechenlaenge_cm_D2093"))
						aufzugJsonObjekt.put("Length", (int) Double.parseDouble(merkmalwert));
					else if(merkmalname.equals("OBJ_Aufzug_Grundflaechenbreite_cm_D2094"))
						aufzugJsonObjekt.put("Depth", (int) Double.parseDouble(merkmalwert));
					else if(merkmalname.equals("OBJ_Aufzug_Verbindungsfunktion_D2095"))
						aufzugJsonObjekt.put("Description", merkmalwert);
					else if(merkmalname.equals("OBJ_Aufzug_OSMID"))
						aufzugJsonObjekt.put("OsmId", merkmalwert);
					else if(merkmalname.equals("OBJ_Aufzug_Foto"))
						aufzugJsonObjekt.put("aufzug_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else if(merkmalname.equals("OBJ_Aufzug_Ebene1_Foto"))
						aufzugJsonObjekt.put("ebene1_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else if(merkmalname.equals("OBJ_Aufzug_Ebene2_Foto"))
						aufzugJsonObjekt.put("ebene2_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else if(merkmalname.equals("OBJ_Aufzug_Ebene3_Foto"))
						aufzugJsonObjekt.put("ebene3_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else if(merkmalname.equals("OBJ_Aufzug_Bedienelemente_Foto"))
						aufzugJsonObjekt.put("bedienelemente_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else if(merkmalname.equals("OBJ_Aufzug_ID_Foto"))
						aufzugJsonObjekt.put("schild_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else if(merkmalname.equals("OBJ_Aufzug_StoerungKontakt_Foto"))
						aufzugJsonObjekt.put("stoerungkontakt_Foto", Bild.getBildUrl(merkmalwert, steigDHID));
					else
						NVBWLogger.warning("in Servlet " + this.getServletName() 
							+ " nicht verarbeitetes Merkmal Name '" + merkmalname + "'" 
							+ ", Wert '" + merkmalwert + "'");
				}

				vorherigeHstObjektID = aktuelleHstObjektID;
				vorherigeObjektID = aktuelleObjektID;

			}
			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException::: " + e.toString());
		}
		response.getWriter().append(aufzugJsonObjekt.toString());
	}
}
