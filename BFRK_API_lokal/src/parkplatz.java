
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

import de.nvbw.base.BFRKApiApplicationconfiguration;
import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;
import de.nvbw.bfrk.util.ReaderBase;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "parkplatz", 
			urlPatterns = {"/parkplatz/*"}
		)
public class parkplatz extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static BFRKApiApplicationconfiguration bfrkapiconfiguration = null;
    private static Connection bfrkConn = null;

    

    /**
     * @see HttpServlet#HttpServlet()
     */
    public parkplatz() {
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

		long paramObjektid = 0;
		if(request.getParameter("dhid") != null) {
			System.out.println("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramObjektid = Long.parseLong(request.getParameter("dhid"));
		} else {
			System.out.println("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			System.out.println("requesturi ===" + requesturi + "===");
			if(requesturi.indexOf("/parkplatz") != -1) {
				int startpos = requesturi.indexOf("/parkplatz");
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
			double lon = 0.0;
			double lat = 0.0;
			String osmids = null;
			boolean osmimportiert = false;
			StringBuffer osmTags = new StringBuffer();
			String objektart = "";
			String dhid = "";
			String name = "";
			String wert = "";
			String typ = "";
			while(selectMerkmaleRS.next()) {
				if(anzahldatensaetze == 0) {
					objektart = selectMerkmaleRS.getString("objektart");
					dhid = selectMerkmaleRS.getString("dhid");

					osmids = selectMerkmaleRS.getString("osmids");
					lon = selectMerkmaleRS.getDouble("osmlon");
					lat = selectMerkmaleRS.getDouble("osmlat");
					osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");
				}
				anzahldatensaetze++;

				name = selectMerkmaleRS.getString("name");
				wert = selectMerkmaleRS.getString("wert");
				typ = selectMerkmaleRS.getString("typ");

				if(!objektart.equals("Parkplatz")) {
					falscheobjektart = true;
					continue;
				}

				if(name.equals("OBJ_Parkplatz_Art_D1051")) {
					if(wert.equals("behindertenplaetze")) {
						wert = "Behindertenplätze";
						osmTags.append("<tag k='park_ride' v='yes'></tag>\r\n");
					} else if(wert.equals("kurzzeitplaetze")) {
						wert = "Kurzzeit";
						osmTags.append("<tag k='park_ride' v='no'></tag>\r\n");
					} else if(wert.equals("park_ride")) {
						wert = "Park+Ride";
						osmTags.append("<tag k='park_ride' v='yes'></tag>\r\n");
					} else if(wert.equals("Parkhaus")) {
						wert = "Parkhaus";
						osmTags.append("<tag k='parking' v='multi-storey'></tag>\r\n");
					} else if(wert.equals("parkplatz_ohne_parkride")) {
						wert = "Parkplatz_ohne_Park+Ride";
					}
					merkmaleJsonObject.put("art", wert);
				} else if(name.equals("OBJ_Parkplatz_oeffentlichVorhanden_D1050"))
					merkmaleJsonObject.put("oeffentlichvorhanden", wert.equals("true"));
				else if(name.equals("OBJ_Parkplatz_Kapazitaet")) {
					merkmaleJsonObject.put("stellplaetzgesamt", (int) Double.parseDouble(wert));
					osmTags.append("<tag k='capacity' v='" + (int) Double.parseDouble(wert) + "'></tag>\r\n");
				} else if(name.equals("OBJ_Parkplatz_BehindertenplaetzeKapazitaet")) {
					merkmaleJsonObject.put("behindertenstellplaetze", (int) Double.parseDouble(wert));
					osmTags.append("<tag k='capacity:disabled' v='" + (int) Double.parseDouble(wert) + "'></tag>\r\n");
				} else if(name.equals("OBJ_Parkplatz_Bedingungen_D1052"))
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
				
				else if(name.equals("OBJ_Parkplatz_Foto"))
					merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Parkplatz_Behindertenplaetze_Foto"))
					merkmaleJsonObject.put("behindertenplaetze_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Parkplatz_Nutzungsbedingungen_Foto"))
					merkmaleJsonObject.put("nutzungsbedingungen_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Parkplatz_Oeffnungszeiten_Foto"))
					merkmaleJsonObject.put("oeffnungszeiten_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_Parkplatz_WegzuHaltestelle_Foto"))
					merkmaleJsonObject.put("wegzuhaltestelle_Foto", Bild.getBildUrl(wert, dhid));
				else
					NVBWLogger.warning("in Servlet " + this.getServletName() 
						+ " nicht verarbeitetes Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");
			} // end of Schleife über alle Datensatzmerkmale

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

			StringBuffer osmObjekt = new StringBuffer();
			osmObjekt.append("<?xml version='1.0' encoding='UTF-8'?>\r\n");
			osmObjekt.append("<osm version='0.6' generator='NVBW Haltestellenimport' upload='never' download='never'>\r\n");
			osmObjekt.append("<node id='-1' "
					+ "lat='" + lat + "' lon='" + lon + "'>\r\n");
			osmObjekt.append("<tag k='amenity' v='parking'></tag>\r\n");
			osmObjekt.append(osmTags);
			osmObjekt.append("</node>\r\n");
			osmObjekt.append("</osm>\r\n");
			merkmaleJsonObject.put("osmtagging", osmObjekt.toString());

			String josmlink = "http://localhost:8111/load_data?&new_layer=false&"
				+ "download_policy=never&upload_policy=never&data=" 
				+ URLEncoder.encode(osmObjekt.toString(), StandardCharsets.UTF_8.toString());
			merkmaleJsonObject.put("josmimportlink", josmlink);
			
			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			if(falscheobjektart) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setCharacterEncoding("UTF-8");
				response.getWriter().append("Parameter objekt_id passt nicht zum Objekttyp Parkplatz");
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
