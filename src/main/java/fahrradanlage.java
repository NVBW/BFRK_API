
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

import de.nvbw.base.Applicationconfiguration;
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

    private static Connection bfrkConn = null;
	private static Applicationconfiguration configuration = new Applicationconfiguration();


    /**
     * @see HttpServlet#HttpServlet()
     */
    public fahrradanlage() {
        super();
    }

    /**
     * initialization on servlet startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
		NVBWLogger.init(configuration.logging_console_level,
				configuration.logging_file_level);
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
				NVBWLogger.info("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
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
		if(requesturi.contains("/fahrradanlage")) {
			int startpos = requesturi.indexOf("/fahrradanlage");
			NVBWLogger.info("startpos #1: " + startpos);
			if(requesturi.indexOf("/",startpos + 1) != -1) {
				paramObjektid = Long.parseLong(requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1));
				NVBWLogger.info("Versuch, Objektid zu extrahieren ===" + paramObjektid + "===");
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
			NVBWLogger.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

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
				NVBWLogger.info("akt name: " + name + ", wert: " + wert);

				if(!objektart.equals("BuR")) {
					falscheobjektart = true;
					continue;
				}

                switch (name) {
                    case "OBJ_BuR_Anlagentyp" -> {
                        merkmaleJsonObject.put("anlagentyp", wert);
                        String osmwert = "";
                        switch (wert) {
                            case "Anlehnbuegel" -> osmwert = "stands";
                            case "doppelstoeckig" -> osmwert = "two-tier";
                            case "Fahrradboxen" -> osmwert = "lockers";
                            case "Fahrradparkhaus" -> osmwert = "building";
                            case "Fahrradsammelanlage" -> osmwert = "shed";
                            case "Vorderradhalter" -> osmwert = "wall_loops";
                            default -> NVBWLogger.warning("OSM-Tagging kann nicht gesetzt werden "
                                    + "für OSM-Key bicycle_parking mit DB-Wert '" + wert + "'");
                        }
                        if (!osmwert.isEmpty())
                            osmTags.append("<tag k='bicycle_parking' v='" + osmwert + "'></tag>\r\n");
                    }
                    case "OBJ_BuR_Stellplatzanzahl" -> {
                        merkmaleJsonObject.put("stellplatzanzahl", (int) Double.parseDouble(wert));
                        osmTags.append("<tag k='capacity' v='" + (int) Double.parseDouble(wert) + "'></tag>\r\n");
                    }
                    case "OBJ_BuR_Beleuchtet" -> {
                        merkmaleJsonObject.put("beleuchtet", wert.equals("true"));
                        osmTags.append("<tag k='lit' v='" + (wert.equals("true") ? "yes" : "no") + "'></tag>\r\n");
                    }
                    case "OBJ_BuR_Ueberdacht" -> {
                        merkmaleJsonObject.put("ueberdacht", wert.equals("true"));
                        osmTags.append("<tag k='covered' v='" + (wert.equals("true") ? "yes" : "no") + "'></tag>\r\n");
                    }
                    case "OBJ_BuR_Hinderniszufahrt_Beschreibung" -> merkmaleJsonObject.put("hinderniszufahrt", wert);
                    case "OBJ_BuR_Kostenpflichtig" -> {
                        merkmaleJsonObject.put("kostenpflichtig", wert.equals("true"));
                        osmTags.append("<tag k='fee' v='" + (wert.equals("true") ? "yes" : "no") + "'></tag>\r\n");
                        if (wert.equals("true"))
                            osmaccess = "customers";
                    }
                    case "OBJ_BuR_KostenpflichtigNotiz" -> merkmaleJsonObject.put("kostenpflichtignotiz", wert);
                    case "OBJ_BuR_WegZurAnlageAnfahrbar" ->
                            merkmaleJsonObject.put("wegzuranlageanfahrbar", wert.equals("true"));
                    case "OBJ_BuR_Lon" -> {
                        if (lon == 0.0) {
                            lon = Double.parseDouble(wert);
                        }
                    }
                    case "OBJ_BuR_Lat" -> {
                        if (lat == 0.0)
                            lat = Double.parseDouble(wert);
                    }
                    case "OBJ_BuR_Buegelabstand_cm" ->
                            merkmaleJsonObject.put("buegelabstand", (int) Double.parseDouble(wert));
                    case "OBJ_BuR_Notiz" -> merkmaleJsonObject.put("notiz", wert);
                    case "OBJ_BuR_Foto" -> merkmaleJsonObject.put("objekt_Foto", Bild.getBildUrl(wert, dhid));
                    case "OBJ_BuR_Weg_Foto" -> merkmaleJsonObject.put("weg_Foto", Bild.getBildUrl(wert, dhid));
                    case "OBJ_BuR_Besonderheiten_Foto" ->
                            merkmaleJsonObject.put("besonderheiten_Foto", Bild.getBildUrl(wert, dhid));
                    case "OBJ_BuR_Hinderniszufahrt_Foto" ->
                            merkmaleJsonObject.put("hinderniszufahrt_Foto", Bild.getBildUrl(wert, dhid));
                    default -> NVBWLogger.warning("in Servlet " + this.getServletName()
                            + " nicht verarbeitetes Merkmal Name '" + name + "'"
                            + ", Wert '" + wert + "'");
                }
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
                for (String s : osmlinksArray) osmlinksJA.put(s);
				merkmaleJsonObject.put("osmlinks", osmlinksJA);
			}

			StringBuilder osmObjekt = new StringBuilder();
			osmObjekt.append("<?xml version='1.0' encoding='UTF-8'?>\r\n");
			osmObjekt.append("<osm version='0.6' generator='NVBW Haltestellenimport' upload='never' download='never'>\r\n");
			osmObjekt.append("<node id='-1' "
					+ "lat='" + lat + "' lon='" + lon + "'>\r\n");
			osmObjekt.append("<tag k='amenity' v='bicycle_parking'></tag>\r\n");
			osmObjekt.append("<tag k='bike_ride' v='yes'></tag>\r\n");
			osmObjekt.append(osmTags);
			osmObjekt.append("<tag k='access' v='" + osmaccess + "'></tag>\r\n");
			osmObjekt.append("</node>\r\n");
			osmObjekt.append("</osm>\r\n");
			merkmaleJsonObject.put("osmtagging", osmObjekt.toString());

			String josmlink = "http://localhost:8111/load_data?&new_layer=false&"
				+ "download_policy=never&upload_policy=never&data=" 
				+ URLEncoder.encode(osmObjekt.toString(), StandardCharsets.UTF_8);
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
			NVBWLogger.severe("SQLException::: " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-DB Fehler aufgetreten, bitte Administrator informieren.");
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
		NVBWLogger.info("Request /fahrradanlage, doPost ...");

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


		String accesstoken = "";

		NVBWLogger.info("Request Header accesstoken vorhanden ===" + request.getHeader("accesstoken") + "===");
		accesstoken = request.getHeader("accesstoken");
		if(accesstoken.isEmpty()) {
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen, weil Header accesstoken leer ist");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		// ============= prüfen, ob accesstoken gültig ist ==============
		String authentifizierungSql = "SELECT objekteaendern, name FROM benutzer "
				+ "WHERE accesstoken = ?;";

		String bearbeiter = null;
		PreparedStatement authentifizierungStmt;
		try {
			authentifizierungStmt = bfrkConn.prepareStatement(authentifizierungSql);
			int stmtindex = 1;
			authentifizierungStmt.setString(stmtindex++, accesstoken);
			NVBWLogger.info("Authentifizierung query: " + authentifizierungStmt.toString() + "===");

			ResultSet authentifizierungRS = authentifizierungStmt.executeQuery();

			String dbaccesstoken = null;
			boolean objekteaendern = false;

			if(authentifizierungRS.next()) {
				bearbeiter = authentifizierungRS.getString("name");
				objekteaendern = authentifizierungRS.getBoolean("objekteaendern");
			}
			authentifizierungRS.close();
			authentifizierungStmt.close();

			if(!objekteaendern) {
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}

		} catch (SQLException e) {
			NVBWLogger.info("SQLException::: " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "DB-Fehler aufgetreten, bitte den Administrtaor benachrichtigen");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		NVBWLogger.info("requesturi ===" + requesturi + "===");
		if(requesturi.contains("/fahrradanlage")) {
			int startpos = requesturi.indexOf("/fahrradanlage");
			NVBWLogger.info("startpos #1: " + startpos);
			if(requesturi.indexOf("/",startpos + 1) != -1) {
				startpos = requesturi.indexOf("/",startpos + 1) + 1;
				int endpos = requesturi.indexOf("/",startpos + 1);
				if(endpos == -1)
					endpos = requesturi.length();
				NVBWLogger.info("für Merkmal: Startpos: " + startpos + ",   endpos: " + endpos);
				paramObjektid = Long.parseLong(URLDecoder.decode(requesturi.substring(startpos, endpos),"UTF-8"));
				NVBWLogger.info("ObjektId===" + paramObjektid + "===");
			}
		}

		paramDHID = "";
		if(request.getParameter("dhid") != null) {
			paramDHID = request.getParameter("dhid");
			NVBWLogger.info("url-Parameter dhid gesetzt ===" + paramDHID + "===");
		} else {
			NVBWLogger.info("url-Parameter dhid fehlt, Pech gehabt");
		}

		String selectObjektSql = "SELECT id FROM objekt WHERE dhid = ? AND id = ? "
			+ "AND objektart = 'BuR';";

		PreparedStatement selectObjektStmt;
		try {
			selectObjektStmt = bfrkConn.prepareStatement(selectObjektSql);
			int stmtindex = 1;
			selectObjektStmt.setString(stmtindex++, paramDHID);
			selectObjektStmt.setLong(stmtindex++, paramObjektid);
			NVBWLogger.info("Objekt query: " + selectObjektStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectObjektStmt.executeQuery();

			int anzahldatensaetze = 0;
			boolean falscheobjektart = false;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
			}
			if(anzahldatensaetze != 1) {
				response.setCharacterEncoding("UTF-8");
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Parameter DHID und objekt_id passen nicht zur Objektart");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			NVBWLogger.severe("DB Transaktion kann nicht gestartet werden");
			NVBWLogger.severe(e.toString());
			response.setCharacterEncoding("UTF-8");
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}


		if(request.getParameter("osmuser") != null) {
			paramOSMUser = request.getParameter("osmuser");
			NVBWLogger.info("url-Parameter osmuser gesetzt ===" + paramOSMUser + "===");
		} else {
			NVBWLogger.info("url-Parameter osmuser fehlt, Pech gehabt");
		}

		try {
			bfrkConn.setAutoCommit(false);
		} catch (SQLException e) {
			NVBWLogger.severe("DB Transaktion kann nicht gestartet werden");
			NVBWLogger.severe(e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
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
				NVBWLogger.info("url-Parameter anlagentyp gesetzt ===" + paramAnlagentyp + "===");

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
					NVBWLogger.info("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

					int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
					if(anzahlinserts == 1) {
						NVBWLogger.info("insert Korrekturmerkmal Anlagentyp war erfolgreich");
						anzahlerfolgreich++;
					} else
						dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Anlagentyp führte nicht zu einem DB Eintrag\r\n");
				} catch (SQLException e1) {
					NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
						+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
						+ "' Details folgen ...");
					NVBWLogger.severe(e1.toString());
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			} else {
				if(!paramAnlagentyp.isEmpty())
					dbfehlermeldungen.append("* Korrekturwert für Anlagentyp war ungültig, wird ignoriert");
				paramAnlagentyp = "";
			}
		} else {
			NVBWLogger.info("url-Parameter anlagentyp fehlt, Pech gehabt");
		}

		paramOSMids = "";
		if(request.getParameter("osmids") != null) {
			paramOSMids = request.getParameter("osmids");
			NVBWLogger.info("url-Parameter osmids gesetzt ===" + paramOSMids + "===");

			String insertOSMBezugSql = "INSERT INTO osmobjektbezug (objekt_id, osmid, bezugzeitpunkt, subobjektart) "
				+ "SELECT o.id, ?, ?, 'Hauptobjekt' from objekt as o WHERE objektart = 'BuR' and o.id = ?;";

			PreparedStatement insertOSMBezugStmt = null;
			try {
				anzahlversuche++;
				insertOSMBezugStmt = bfrkConn.prepareStatement(insertOSMBezugSql);
				int stmtindex = 1;
				insertOSMBezugStmt.setString(stmtindex++, paramOSMids);
				insertOSMBezugStmt.setTimestamp(stmtindex++, new java.sql.Timestamp(zeitstempelimport.getTime()));
				insertOSMBezugStmt.setLong(stmtindex++, paramObjektid);
				NVBWLogger.info("insertOSMBezug Statement: " + insertOSMBezugStmt.toString() + "===");

				int anzahlinserts = insertOSMBezugStmt.executeUpdate();
				if(anzahlinserts == 1) {
					NVBWLogger.info("insert osmobjektbezug war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert OSMObjektbezug führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler in Tabelle osmobjektbezug, " 
					+ "Statement war '" + insertOSMBezugStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			NVBWLogger.info("url-Parameter osmids fehlt, Pech gehabt");
		}

		paramBemerkungen = "";
		if(request.getParameter("bemerkungen") != null) {
			paramOSMids = request.getParameter("bemerkungen");
			NVBWLogger.info("url-Parameter bemerkungen gesetzt ===" + paramBemerkungen + "===");
		} else {
			NVBWLogger.info("url-Parameter bemerkungen fehlt, Pech gehabt");
		}

		paramInOSMVollstaendig = false;
		if(request.getParameter("inosmvollstaendigerfasst") != null) {
			if(request.getParameter("inosmvollstaendigerfasst").equals("true"))
				paramInOSMVollstaendig = true;
			else {
				paramInOSMVollstaendig = false;
			}
			NVBWLogger.info("url-Parameter inosmvollstaendigerfasst: " + (paramInOSMVollstaendig == true));

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
				NVBWLogger.info("insertOSMimportiert Statement: " + insertOSMimportiertStmt.toString() + "===");

				int anzahlinserts = insertOSMimportiertStmt.executeUpdate();
				if(anzahlinserts == 1) {
					NVBWLogger.info("insert osmimport war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert OSMImport führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler in Tabelle osmimport, " 
					+ "Statement war '" + insertOSMimportiertStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			NVBWLogger.info("url-Parameter inosmvollstaendigerfasst fehlt, Pech gehabt");
		}

		paramStellplatzanzahl = 0;
		if(request.getParameter("stellplatzanzahl") != null) {
			paramStellplatzanzahl = Integer.parseInt(request.getParameter("stellplatzanzahl"));
			NVBWLogger.info("url-Parameter stellplatzanzahl gesetzt ===" + paramStellplatzanzahl + "===");

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
				NVBWLogger.info("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

				int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
				if(anzahlinserts == 1) {
					NVBWLogger.info("insert Korrekturmerkmal Stellplatzanzahl war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Stellplatzanzahl führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			NVBWLogger.info("url-Parameter stellplatzanzahl nicht vorhanden gewesen");
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
				NVBWLogger.info("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

				int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
				if(anzahlinserts == 1) {
					NVBWLogger.info("insert Korrekturmerkmal Beleuchtung war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Beleuchtung führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			NVBWLogger.info("url-Parameter beleuchtet nicht vorhanden gewesen");
		}

		paramUeberdacht = false;
		if(request.getParameter("ueberdacht") != null) {
			if(request.getParameter("ueberdacht").equals("true"))
				paramUeberdacht = true;
			else {
				paramUeberdacht = false;
			}
			NVBWLogger.info("url-Parameter ueberdacht: " + (paramUeberdacht == true));

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
				NVBWLogger.info("insertKorrekturmerkmal Statement: " + insertKorrekturmerkmalStmt.toString() + "===");

				int anzahlinserts = insertKorrekturmerkmalStmt.executeUpdate();
				if(anzahlinserts == 1) {
					NVBWLogger.info("insert Korrekturmerkmal Ueberdacht war erfolgreich");
					anzahlerfolgreich++;
				} else
					dbfehlermeldungen.append("* DB Insert Merkmalkorrektur Ueberdacht führte nicht zu einem DB Eintrag\r\n");
			} catch (SQLException e1) {
				NVBWLogger.severe("SQL-Insert Fehler, als eine Merkmalkorrektur in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertKorrekturmerkmalStmt.toString() 
					+ "' Details folgen ...");
				NVBWLogger.severe(e1.toString());
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			NVBWLogger.info("url-Parameter ueberdacht nicht vorhanden gewesen");
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
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
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
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten Versuch, Daten zu ändern in /fahrradanlage, bitte Administrator informieren: ");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		}
	}
}
