
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
@WebServlet(name = "rolltreppe", 
			urlPatterns = {"/rolltreppe/*"}
		)
public class rolltreppe extends HttpServlet {
	private static final long serialVersionUID = 1L;

    private static Connection bfrkConn = null;    

    /**
     * @see HttpServlet#HttpServlet()
     */
    public rolltreppe() {
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

		long paramObjektid = 0;
		if(request.getParameter("dhid") != null) {
			System.out.println("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramObjektid = Long.parseLong(request.getParameter("dhid"));
		} else {
			System.out.println("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			System.out.println("requesturi ===" + requesturi + "===");
			if(requesturi.indexOf("/rolltreppe") != -1) {
				int startpos = requesturi.indexOf("/rolltreppe");
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
			+ "erf.osmids, erf.osmlon, erf.osmlat "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "m.name, m.wert, m.typ, o.dhid, o.objektart, "
			+ "osm.osmid AS osmids, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_X(osm.koordinate) END AS osmlon, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_Y(osm.koordinate) END AS osmlat "
			+ "FROM merkmal AS m JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "LEFT JOIN osmobjektbezug AS osm on osm.objekt_id = o.id "
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
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				String dhid = selectMerkmaleRS.getString("dhid");
				String name = selectMerkmaleRS.getString("name");
				String wert = selectMerkmaleRS.getString("wert");

				String osmids = selectMerkmaleRS.getString("osmids");
				double osmlon = selectMerkmaleRS.getDouble("osmlon");
				double osmlat = selectMerkmaleRS.getDouble("osmlat");

				if(name.equals("OBJ_Rolltreppe_Fahrtrichtung_D2132")) {
					if(wert.equals("nur_aufwaerts")) {
						merkmaleJsonObject.put("fahrtrichtung", "aufwärts");
						merkmaleJsonObject.put("wechselndeRichtung", false);
					} else if(wert.equals("nur_abwaerts")) {
						merkmaleJsonObject.put("fahrtrichtung", "abwärts");
						merkmaleJsonObject.put("wechselndeRichtung", false);
					} else if(	wert.equals("hauptrichtung_aufwaerts")) {
						merkmaleJsonObject.put("fahrtrichtung", "aufwärts");
						merkmaleJsonObject.put("wechselndeRichtung", true);
					} else if(wert.equals("hauptrichtung_abwaerts")) {
						merkmaleJsonObject.put("fahrtrichtung", "abwärts");
						merkmaleJsonObject.put("beide_richtungen", true);
					} else if(wert.equals("beide_richtungen")) {				
						merkmaleJsonObject.put("fahrtrichtung", "aufwärts");
						merkmaleJsonObject.put("wechselndeRichtung", true);
					} else {
						NVBWLogger.warning("in Servlet " + this.getServletName() 
						+ " unerwarteter Wert bei Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");
					}
				} else if(name.equals("OBJ_Rolltreppe_Laufzeit_sek_D2134"))
					merkmaleJsonObject.put("laufzeit_sek", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_Rolltreppe_Verbindungsfunktion_D2131"))
					merkmaleJsonObject.put("verbindungsfunktion", wert);
				else if(name.equals("OBJ_Rolltreppe_Vorhanden_D2130")) {
					//nichts zu tun
				}
				else if(name.equals("OBJ_Rolltreppe_Foto"))
					merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Rolltreppe_ID_Foto"))
					merkmaleJsonObject.put("id_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Rolltreppe_Richtung1_Foto"))
					merkmaleJsonObject.put("richtung1_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Rolltreppe_Richtung2_Foto"))
					merkmaleJsonObject.put("richtung2_Foto", Bild.getBildUrl(wert, dhid));
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
