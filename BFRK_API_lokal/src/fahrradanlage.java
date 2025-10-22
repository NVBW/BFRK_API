
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
@WebServlet(name = "fahrradanlage", 
			urlPatterns = {"/fahrradanlage/*"}
		)
public class fahrradanlage extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static BFRKApiApplicationconfiguration bfrkapiconfiguration = null;
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public fahrradanlage() {
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

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject resultObjectJson = new JSONObject();

		NVBWLogger.info("===== Request /fahrradanlage GET ...");

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				System.out.println("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					JSONObject errorObjektJson = new JSONObject();
					errorObjektJson.put("subject", "Datenbankverbindung nicht möglich, bitte später nochmal versuchen");
					errorObjektJson.put("message", "DB-Verbindung fehlt");
					errorObjektJson.put("messageId", 123);
					resultObjectJson.put("error", errorObjektJson);
					response.setCharacterEncoding("UTF-8");
					response.getWriter().append(resultObjectJson.toString());
					response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
					return;
				}
			}
		} catch (Exception e1) {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Exception bei Aufbau Datenbankverbindung aufgetreten, bitte später nochmal versuchen");
			errorObjektJson.put("message", "DB-Verbindung Exception");
			errorObjektJson.put("messageId", 124);
			resultObjectJson.put("error", errorObjektJson);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			return;
		}
		
		long paramObjektid = 0;
		String requesturi = request.getRequestURI();
		NVBWLogger.info("requesturi ===" + requesturi + "===");
		if(requesturi.indexOf("/fahrradanlage") != -1) {
			int startpos = requesturi.indexOf("/fahrradanlage");
			System.out.println("startpos #1: " + startpos);
			if(requesturi.indexOf("/",startpos + 1) != -1) {
				paramObjektid = Long.parseLong(requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1));
				System.out.println("Versuch, Objektid zu extrahieren ===" + paramObjektid + "===");
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
			+ "CASE WHEN NOT erf.osm_lon IS NULL THEN 'koord_osm' ELSE CASE WHEN NOT erf.m_lon IS NULL THEN 'koord_merkmal' ELSE 'koord_objekt' END END AS koord_art, "
			+ "CASE WHEN NOT erf.osm_lon IS NULL THEN erf.osm_lon ELSE CASE WHEN NOT erf.m_lon IS NULL THEN erf.m_lon ELSE erf.o_lon END END AS lon, "
			+ "CASE WHEN NOT erf.osm_lat IS NULL THEN erf.osm_lat ELSE CASE WHEN NOT erf.m_lat IS NULL THEN erf.m_lat ELSE erf.o_lat END END AS lat, "
			+ "erf.objekt_id, erf.osmids, erf.o_lon, erf.o_lat, erf.gemeinde, erf.ortsteil, osmimportiert, erf.infraidtemp, hst.dhid AS hst_dhid "
			+ "FROM "
			+ "( "
			+ "SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, o.gemeinde, o.ortsteil, "
			+ "	o.infraidtemp, m.name, m.wert, m.typ, o.dhid, o.objektart, osm.osmid AS osmids, "
			+ "	ST_X(o.koordinate) AS o_lon, ST_Y(o.koordinate) AS o_lat, osmimport.vollstaendig AS osmimportiert, "
			+ "	mlon.wert::numeric AS m_lon, mlat.wert::numeric AS m_lat, ST_X(osm.koordinate) as osm_lon, ST_Y(osm.koordinate) as osm_lat "
			+ "FROM merkmal AS m "
			+ "JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ "LEFT JOIN MERKMAL AS mlon on m.objekt_id = mlon.objekt_id AND mlon.name = 'OBJ_BuR_Lon' "
			+ "LEFT JOIN MERKMAL AS mlat on m.objekt_id = mlat.objekt_id AND mlat.name = 'OBJ_BuR_Lat' "
			+ "LEFT JOIN osmobjektbezug AS osm on osm.objekt_id = o.id "
			+ "LEFT JOIN osmimport on o.id = osmimport.objekt_id "
			+ "WHERE o.id = ?"
			+ ") AS erf "
			+ "FULL OUTER JOIN "
			+ "("
			+ "SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "	m.name, m.wert, m.typ, o.dhid, o.objektart "
			+ "FROM merkmalkorrektur AS m "
			+ "JOIN objekt AS o ON m.objekt_id = o.id "
			+ "WHERE o.id = ?"
			+ ") AS korr "
			+ "ON erf.objekt_id = korr.objekt_id AND korr.name = erf.name "
			+ "JOIN objekt as hst ON erf.parent_id = hst.id WHERE erf.objektart = 'BuR' ORDER BY dhid, erf.objekt_id;";


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
			long objektid = 0;
			double lon = 0.0;
			double lat = 0.0;
			String osmids = null;
			boolean osmimportiert = false;
			StringBuffer osmTags = new StringBuffer();
			String osmaccess = "yes";
			String objektart = "";
			String dhid = "";
			String name = "";
			String wert = "";
			String typ = "";
			String hstdhid = "";
			String gemeinde = "";
			String ortsteil = "";
			String infraid = "";
			while(selectMerkmaleRS.next()) {
				objektid = selectMerkmaleRS.getLong("objekt_id");

				if(anzahldatensaetze == 0) {
					objektart = selectMerkmaleRS.getString("objektart");
					dhid = selectMerkmaleRS.getString("dhid");
					hstdhid = selectMerkmaleRS.getString("hst_dhid");
					gemeinde = selectMerkmaleRS.getString("gemeinde");
					ortsteil = selectMerkmaleRS.getString("ortsteil");
					infraid = selectMerkmaleRS.getString("infraidtemp");
					osmids = selectMerkmaleRS.getString("osmids");
					lon = selectMerkmaleRS.getDouble("lon");
					lat = selectMerkmaleRS.getDouble("lat");
					osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");
				}
				anzahldatensaetze++;

				name = selectMerkmaleRS.getString("name");
				wert = selectMerkmaleRS.getString("wert");
				typ = selectMerkmaleRS.getString("typ");
				System.out.println("akt name: " + name + ", wert: " + wert);

				if(!objektart.equals("BuR")) {
					falscheobjektart = true;
					continue;
				}

				if(name.equals("OBJ_BuR_Anlagentyp")) {
					merkmaleJsonObject.put("anlagentyp", wert);
					String osmwert = "";
					if(wert.equals("Anlehnbuegel"))
						osmwert = "stands";
					else if(wert.equals("doppelstoeckig"))
						osmwert = "two-tier";
					else if(wert.equals("Fahrradboxen"))
						osmwert = "lockers";
					else if(wert.equals("Fahrradparkhaus"))
						osmwert = "building";
					else if(wert.equals("Fahrradsammelanlage"))
						osmwert = "shed";
					else if(wert.equals("Vorderradhalter"))
						osmwert = "wall_loops";
					else
						NVBWLogger.warning("OSM-Tagging kann nicht gesetzt werden "
							+ "für OSM-Key bicycle_parking mit DB-Wert '" + wert + "'");
					if(!osmwert.isEmpty())
						osmTags.append("<tag k='bicycle_parking' v='" + osmwert + "'></tag>\r\n");
				} else if(name.equals("OBJ_BuR_Stellplatzanzahl")) {
					merkmaleJsonObject.put("stellplatzanzahl", (int) Double.parseDouble(wert));
					osmTags.append("<tag k='capacity' v='" + (int) Double.parseDouble(wert) + "'></tag>\r\n");
				} else if(name.equals("OBJ_BuR_Beleuchtet")) {
					merkmaleJsonObject.put("beleuchtet", wert.equals("true"));
					osmTags.append("<tag k='lit' v='" + (wert.equals("true") ? "yes" : "no") + "'></tag>\r\n");
				} else if(name.equals("OBJ_BuR_Ueberdacht")) {
					merkmaleJsonObject.put("ueberdacht", wert.equals("true"));
					osmTags.append("<tag k='covered' v='" + (wert.equals("true") ? "yes" : "no") + "'></tag>\r\n");
				} else if(name.equals("OBJ_BuR_Hinderniszufahrt_Beschreibung"))
					merkmaleJsonObject.put("hinderniszufahrt", wert);
				else if(name.equals("OBJ_BuR_Kostenpflichtig")) {
					merkmaleJsonObject.put("kostenpflichtig", wert.equals("true"));
					osmTags.append("<tag k='fee' v='" + (wert.equals("true") ? "yes" : "no") + "'></tag>\r\n");
					if(wert.equals("true"))
						osmaccess = "customers";
				} else if(name.equals("OBJ_BuR_KostenpflichtigNotiz"))
					merkmaleJsonObject.put("kostenpflichtignotiz", wert);
				else if(name.equals("OBJ_BuR_WegZurAnlageAnfahrbar"))
					merkmaleJsonObject.put("wegzuranlageanfahrbar", wert.equals("true"));
				else if(name.equals("OBJ_BuR_Lon")) {
					if(lon == 0.0) {
						lon = Double.parseDouble(wert);
					}
				} else if(name.equals("OBJ_BuR_Lat")) {
					if(lat == 0.0)
						lat = Double.parseDouble(wert);
				} else if(name.equals("OBJ_BuR_Buegelabstand_cm"))
					merkmaleJsonObject.put("buegelabstand", (int) Double.parseDouble(wert));
				else if(name.equals("OBJ_BuR_Notiz"))
					merkmaleJsonObject.put("notiz", wert);
				else if(name.equals("OBJ_BuR_Foto"))
					merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_BuR_Weg_Foto"))
					merkmaleJsonObject.put("weg_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_BuR_Besonderheiten_Foto"))
					merkmaleJsonObject.put("besonderheiten_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("OBJ_BuR_Hinderniszufahrt_Foto"))
					merkmaleJsonObject.put("hinderniszufahrt_Foto", Bild.getBildUrl(wert, dhid));
				else
					NVBWLogger.warning("in Servlet " + this.getServletName() 
						+ " nicht verarbeitetes Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");
			} // end of Schleife über alle Datensatzmerkmale

			merkmaleJsonObject.put("objekt_dhid", dhid);
			merkmaleJsonObject.put("hst_dhid", hstdhid);
			merkmaleJsonObject.put("objektid", objektid);
			merkmaleJsonObject.put("gemeinde", gemeinde);
			merkmaleJsonObject.put("ortsteil", ortsteil);
			merkmaleJsonObject.put("infraid", infraid);

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
			osmObjekt.append("<tag k='amenity' v='bicycle_parking'></tag>\r\n");
			osmObjekt.append("<tag k='bike_ride' v='yes'></tag>\r\n");
			osmObjekt.append(osmTags);
System.out.println("osmaccess: " + osmaccess);
			if(!osmaccess.isEmpty())
				osmObjekt.append("<tag k='access' v='" + osmaccess + "'></tag>\r\n");
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
				response.getWriter().append("Parameter objekt_id passt nicht zum Objekttyp Fahrradanlage");
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
		System.out.println("Request /fahrradanlage, doPost ...");

		String paramAuthorisierungsId = "";
		String paramOSMUser = "_BFRKWebiste_Sonstiger";
		String paramOSMids = "";
		String paramAnlagentyp = "";
		String paramBemerkungen = "";
		String paramDHID = "";
		long paramObjektid = 0;
		int paramStellplatzanzahl = 0;
		boolean paramInOSMVollstaendig = false;
		boolean paramBeleuchtet = false;
		boolean paramUeberdacht = false;
		String requesturi = request.getRequestURI();
		StringBuffer dbfehlermeldungen = new StringBuffer();
		Date zeitstempelimport = new Date();

		System.out.println("requesturi ===" + requesturi + "===");
		if(requesturi.indexOf("/fahrradanlage") != -1) {
			int startpos = requesturi.indexOf("/fahrradanlage");
			System.out.println("startpos #1: " + startpos);
			if(requesturi.indexOf("/",startpos + 1) != -1) {
				startpos = requesturi.indexOf("/",startpos + 1) + 1;
				int endpos = requesturi.indexOf("/",startpos + 1);
				if(endpos == -1)
					endpos = requesturi.length();
				System.out.println("für Merkmal: Startpos: " + startpos + ",   endpos: " + endpos);
				paramObjektid = Long.parseLong(URLDecoder.decode(requesturi.substring(startpos, endpos),"UTF-8"));
				System.out.println("ObjektId===" + paramObjektid + "===");
			}
		}

		paramAuthorisierungsId = "";
		if(request.getParameter("authorisierungsid") != null) {
			paramAuthorisierungsId = request.getParameter("authorisierungsid");
			System.out.println("url-Parameter authorisierungsid gesetzt ===" + paramAuthorisierungsId + "===");
		} else {
			System.out.println("url-Parameter authorisierungsid fehlt, Pech gehabt");
		}

			// Authorisierung zum PUT-Request prüfen und ggfs. abbrechen
		if(		paramAuthorisierungsId.isEmpty()
			||	(bfrkapiconfiguration.authorisierungsid == null)
			||	!paramAuthorisierungsId.equals(bfrkapiconfiguration.authorisierungsid)) {
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setCharacterEncoding("UTF-8");
			return;
		}

		paramDHID = "";
		if(request.getParameter("dhid") != null) {
			paramDHID = request.getParameter("dhid");
			System.out.println("url-Parameter dhid gesetzt ===" + paramDHID + "===");
		} else {
			System.out.println("url-Parameter dhid fehlt, Pech gehabt");
		}

		String selectObjektSql = "SELECT id FROM objekt WHERE dhid = ? AND id = ? "
			+ "AND objektart = 'BuR';";

		PreparedStatement selectObjektStmt;
		try {
			selectObjektStmt = bfrkConn.prepareStatement(selectObjektSql);
			int stmtindex = 1;
			selectObjektStmt.setString(stmtindex++, paramDHID);
			selectObjektStmt.setLong(stmtindex++, paramObjektid);
			System.out.println("Objekt query: " + selectObjektStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectObjektStmt.executeQuery();

			int anzahldatensaetze = 0;
			boolean falscheobjektart = false;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
			}
			if(anzahldatensaetze != 1) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setCharacterEncoding("UTF-8");
				response.getWriter().append("Parameter DHID und objekt_id passen nicht zur Objektart");
				return;
			}
		} catch (SQLException e) {
			NVBWLogger.severe("DB Transaktion kann nicht gestartet werden");
			NVBWLogger.severe(e.toString());
			dbfehlermeldungen.append("* DB Transaktion kann nicht gestartet werden\r\n");

			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append(dbfehlermeldungen);
			return;
		}

			
			
		if(request.getParameter("osmuser") != null) {
			paramOSMUser = request.getParameter("osmuser");
			System.out.println("url-Parameter osmuser gesetzt ===" + paramOSMUser + "===");
		} else {
			System.out.println("url-Parameter osmuser fehlt, Pech gehabt");
		}

		try {
			bfrkConn.setAutoCommit(false);
		} catch (SQLException e) {
			NVBWLogger.severe("DB Transaktion kann nicht gestartet werden");
			NVBWLogger.severe(e.toString());
			dbfehlermeldungen.append("* DB Transaktion kann nicht gestartet werden\r\n");
		}

		int anzahlerfolgreich = 0;
		int anzahlversuche = 0;
		paramAnlagentyp = "";
		if(request.getParameter("anlagentyp") != null) {
			paramAnlagentyp = request.getParameter("anlagentyp");
			if(		paramAnlagentyp.equals("Anlehnbuegel")
				||	paramAnlagentyp.equals("automatischesParksystem")
				||	paramAnlagentyp.equals("doppelstoeckig")
				||	paramAnlagentyp.equals("Fahrradboxen")
				||	paramAnlagentyp.equals("Fahrradparkhaus")
				||	paramAnlagentyp.equals("Fahrradsammelanlage")
				||	paramAnlagentyp.equals("Sonstiges")
				||	paramAnlagentyp.equals("Vorderradhalter")) {
				System.out.println("url-Parameter anlagentyp gesetzt ===" + paramAnlagentyp + "===");

				String insertKorrekturmerkmalSql = "INSERT INTO merkmalkorrektur "
					+ "(objekt_id, name, wert, typ, zeitstempel) "
					+ "SELECT o.id, 'OBJ_BuR_Anlagentyp', ?,'String', ? "
					+ "FROM objekt AS o JOIN merkmal AS m on m.objekt_id = o.id "
					+ "WHERE objektart = 'BuR' and o.id = ? "
					+ "AND  m.name = 'OBJ_BuR_Anlagentyp' AND NOT wert = ?;";

				PreparedStatement insertKorrekturmerkmalStmt = null;
				try {
					anzahlversuche++;
					insertKorrekturmerkmalStmt = bfrkConn.prepareStatement(insertKorrekturmerkmalSql);
					int stmtindex = 1;
					insertKorrekturmerkmalStmt.setString(stmtindex++, paramAnlagentyp);
					insertKorrekturmerkmalStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
					insertKorrekturmerkmalStmt.setLong(stmtindex++, paramObjektid);
					insertKorrekturmerkmalStmt.setString(stmtindex++, paramAnlagentyp);
					System.out.println("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

					int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
					if(anzahlinserts == 1) {
						System.out.println("insert Korrekturmerkmal Anlagentyp war erfolgreich");
						anzahlerfolgreich++;
					} else
						dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Anlagentyp führte nicht zu einem DB Eintrag\r\n");
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
						+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
						+ "' Details folgen ...");
					NVBWLogger.severe(e1.toString());
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Anlagentyp führte zu einer Exception\r\n");
				}
			} else {
				if(!paramAnlagentyp.isEmpty())
					dbfehlermeldungen.append("* Korrekturwert für Anlagentyp war ungültig, wird ignoriert");
				paramAnlagentyp = "";
			}
		} else {
			System.out.println("url-Parameter anlagentyp fehlt, Pech gehabt");
		}

		paramOSMids = "";
		if(request.getParameter("osmids") != null) {
			paramOSMids = request.getParameter("osmids");
			System.out.println("url-Parameter osmids gesetzt ===" + paramOSMids + "===");

			String insertOSMBezugSql = "INSERT INTO osmobjektbezug (objekt_id, osmid, bezugzeitpunkt) "
				+ "SELECT o.id, ?, ? from objekt as o WHERE objektart = 'BuR' and o.id = ?;";

			PreparedStatement insertOSMBezugStmt = null;
			try {
				anzahlversuche++;
				insertOSMBezugStmt = bfrkConn.prepareStatement(insertOSMBezugSql);
				int stmtindex = 1;
				insertOSMBezugStmt.setString(stmtindex++, paramOSMids);
				insertOSMBezugStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
				insertOSMBezugStmt.setLong(stmtindex++, paramObjektid);
				System.out.println("insertOSMBezug Statement: " + insertOSMBezugStmt.toString() + "===");

				int anzahlinserts = insertOSMBezugStmt.executeUpdate();
				if(anzahlinserts == 1) {
					System.out.println("insert osmobjektbezug war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert OSMObjektbezug führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler in Tabelle osmobjektbezug, " 
					+ "Statement war '" + insertOSMBezugStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				dbfehlermeldungen.append("* DB Insert OSMObjektbezug führte zu einer Exception\r\n");
			}
		} else {
			System.out.println("url-Parameter osmids fehlt, Pech gehabt");
		}

		paramBemerkungen = "";
		if(request.getParameter("bemerkungen") != null) {
			paramOSMids = request.getParameter("bemerkungen");
			System.out.println("url-Parameter bemerkungen gesetzt ===" + paramBemerkungen + "===");
		} else {
			System.out.println("url-Parameter bemerkungen fehlt, Pech gehabt");
		}

		paramInOSMVollstaendig = false;
		if(request.getParameter("inosmvollstaendigerfasst") != null) {
			if(request.getParameter("inosmvollstaendigerfasst").equals("true"))
				paramInOSMVollstaendig = true;
			else {
				paramInOSMVollstaendig = false;
			}
			System.out.println("url-Parameter inosmvollstaendigerfasst: " + (paramInOSMVollstaendig == true));

			String insertOSMimportiertSql = "INSERT INTO osmimport "
				+ "(objekt_id, importiertdurch, vollstaendig, bemerkungen, zeitstempel) "
				+ "SELECT o.id, ?, ?, ?, ? "
				+ "FROM objekt AS o WHERE objektart = 'BuR' and o.id = ?;";

			PreparedStatement insertOSMimportiertStmt = null;
			try {
				anzahlversuche++;
				insertOSMimportiertStmt = bfrkConn.prepareStatement(insertOSMimportiertSql);
				int stmtindex = 1;
				insertOSMimportiertStmt.setString(stmtindex++, paramOSMUser);
				insertOSMimportiertStmt.setBoolean(stmtindex++, paramInOSMVollstaendig);
				insertOSMimportiertStmt.setString(stmtindex++, paramBemerkungen);
				insertOSMimportiertStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
				insertOSMimportiertStmt.setLong(stmtindex++, paramObjektid);
				System.out.println("insertOSMimportiert Statement: " + insertOSMimportiertStmt.toString() + "===");

				int anzahlinserts = insertOSMimportiertStmt.executeUpdate();
				if(anzahlinserts == 1) {
					System.out.println("insert osmimport war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert OSMImport führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler in Tabelle osmimport, " 
					+ "Statement war '" + insertOSMimportiertStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				dbfehlermeldungen.append("* DB Insert OSMImport führte zu einer Exception\r\n");
			}
		} else {
			System.out.println("url-Parameter inosmvollstaendigerfasst fehlt, Pech gehabt");
		}

		paramStellplatzanzahl = 0;
		if(request.getParameter("stellplatzanzahl") != null) {
			paramStellplatzanzahl = Integer.parseInt(request.getParameter("stellplatzanzahl"));
			System.out.println("url-Parameter stellplatzanzahl gesetzt ===" + paramStellplatzanzahl + "===");

			String insertKorrekturmerkmalSql = "INSERT INTO merkmalkorrektur "
				+ "(objekt_id, name, wert, typ, zeitstempel) "
				+ "SELECT o.id, 'OBJ_BuR_Stellplatzanzahl', ?,'String', ? "
				+ "FROM objekt AS o JOIN merkmal AS m on m.objekt_id = o.id "
				+ "WHERE objektart = 'BuR' and o.id = ? "
				+ "AND  m.name = 'OBJ_BuR_Stellplatzanzahl' AND NOT wert = ?;";

			PreparedStatement insertKorrekturmerkmalStmt = null;
			try {
				anzahlversuche++;
				insertKorrekturmerkmalStmt = bfrkConn.prepareStatement(insertKorrekturmerkmalSql);
				int stmtindex = 1;
				insertKorrekturmerkmalStmt.setString(stmtindex++, "" + paramStellplatzanzahl + ".0");
				insertKorrekturmerkmalStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
				insertKorrekturmerkmalStmt.setLong(stmtindex++, paramObjektid);
				insertKorrekturmerkmalStmt.setString(stmtindex++, "" + paramStellplatzanzahl + ".0");
				System.out.println("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

				int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
				if(anzahlinserts == 1) {
					System.out.println("insert Korrekturmerkmal Stellplatzanzahl war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Stellplatzanzahl führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Stellplatzanzahl führte zu einer Exception\r\n");
			}
		} else {
			System.out.println("url-Parameter stellplatzanzahl nicht vorhanden gewesen");
		}

		paramBeleuchtet = false;
		if(request.getParameter("beleuchtet") != null) {
			if(request.getParameter("beleuchtet").equals("true"))
				paramBeleuchtet = true;
			else {
				paramBeleuchtet = false;
			}

			String insertKorrekturmerkmalSql = "INSERT INTO merkmalkorrektur "
				+ "(objekt_id, name, wert, typ, zeitstempel) "
				+ "SELECT o.id, 'OBJ_BuR_Beleuchtet', ?,'String', ? "
				+ "FROM objekt AS o JOIN merkmal AS m on m.objekt_id = o.id "
				+ "WHERE objektart = 'BuR' and o.id = ? "
				+ "AND  m.name = 'OBJ_BuR_Beleuchtet' AND NOT wert = ?;";

			PreparedStatement insertKorrekturmerkmalStmt = null;
			try {
				anzahlversuche++;
				insertKorrekturmerkmalStmt = bfrkConn.prepareStatement(insertKorrekturmerkmalSql);
				int stmtindex = 1;
				insertKorrekturmerkmalStmt.setString(stmtindex++, "" + paramBeleuchtet);
				insertKorrekturmerkmalStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
				insertKorrekturmerkmalStmt.setLong(stmtindex++, paramObjektid);
				insertKorrekturmerkmalStmt.setString(stmtindex++, "" + paramBeleuchtet);
				System.out.println("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

				int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
				if(anzahlinserts == 1) {
					System.out.println("insert Korrekturmerkmal Beleuchtung war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Beleuchtung führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Beleuchtung führte zu einer Exception\r\n");
			}
		} else {
			System.out.println("url-Parameter beleuchtet nicht vorhanden gewesen");
		}

		paramUeberdacht = false;
		if(request.getParameter("ueberdacht") != null) {
			if(request.getParameter("ueberdacht").equals("true"))
				paramUeberdacht = true;
			else {
				paramUeberdacht = false;
			}
			System.out.println("url-Parameter ueberdacht: " + (paramUeberdacht == true));

			String insertKorrekturmerkmalSql = "INSERT INTO merkmalkorrektur "
				+ "(objekt_id, name, wert, typ, zeitstempel) "
				+ "SELECT o.id, 'OBJ_BuR_Ueberdacht', ?,'String', ? "
				+ "FROM objekt AS o JOIN merkmal AS m on m.objekt_id = o.id "
				+ "WHERE objektart = 'BuR' and o.id = ? "
				+ "AND  m.name = 'OBJ_BuR_Ueberdacht' AND NOT wert = ?;";

			PreparedStatement insertKorrekturmerkmalStmt = null;
			try {
				anzahlversuche++;
				insertKorrekturmerkmalStmt = bfrkConn.prepareStatement(insertKorrekturmerkmalSql);
				int stmtindex = 1;
				insertKorrekturmerkmalStmt.setString(stmtindex++, "" + paramUeberdacht);
				insertKorrekturmerkmalStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
				insertKorrekturmerkmalStmt.setLong(stmtindex++, paramObjektid);
				insertKorrekturmerkmalStmt.setString(stmtindex++, "" + paramUeberdacht);
				System.out.println("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

				int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
				if(anzahlinserts == 1) {
					System.out.println("insert Korrekturmerkmal Ueberdacht war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Ueberdacht führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Ueberdacht führte zu einer Exception\r\n");
			}
		} else {
			System.out.println("url-Parameter ueberdacht nicht vorhanden gewesen");
		}

		if(		(anzahlversuche == anzahlerfolgreich)
			&& 	(anzahlerfolgreich >= 1)) {
			try {
				bfrkConn.commit();
				response.setStatus(HttpServletResponse.SC_OK);
				response.setCharacterEncoding("UTF-8");
				return;
			} catch (SQLException e) {
				NVBWLogger.severe("DB Transaktion konnte nicht committed werden");
				NVBWLogger.severe(e.toString());
				dbfehlermeldungen.append("* DB Transaktion konnte nicht committed werden\r\n");
			}
		} else {
			try {
				bfrkConn.rollback();
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				response.setCharacterEncoding("UTF-8");
				response.getWriter().append(dbfehlermeldungen);
				return;
			} catch (SQLException e) {
				NVBWLogger.severe("DB Transaktion konnte nicht rollbacked werden");
				NVBWLogger.severe(e.toString());
				dbfehlermeldungen.append("* DB Transaktion konnte nicht rollbacked werden\r\n");
			}
		}
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append(dbfehlermeldungen);
		return;
	}
}
