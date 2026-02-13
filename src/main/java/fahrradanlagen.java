
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
@WebServlet(name = "fahrradanlagen", 
			urlPatterns = {"/fahrradanlagen"}
		)
public class fahrradanlagen extends HttpServlet {
	private static DateFormat datetime_de_formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	private static final long serialVersionUID = 1L;

    private static Connection bfrkConn = null;
	private static Applicationconfiguration configuration = new Applicationconfiguration();


    /**
     * @see HttpServlet#HttpServlet()
     */
    public fahrradanlagen() {
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

		Date requestStart = new Date();
		Date requestEnde = null;

		NVBWLogger.info("fahrradanlagen Request-Beginn: " + datetime_de_formatter.format(requestStart));

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				NVBWLogger.warning("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
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
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unerwarteter Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		

		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		
		
		
		String selectHaltestelleSql = "SELECT "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN 'korrektur' ELSE 'erfassung' END AS quelle, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.merkmal_id ELSE erf.merkmal_id END as merkmal_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.objekt_id ELSE erf.objekt_id END as objekt_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.parent_id ELSE erf.parent_id END AS parent_id, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.name ELSE erf.name END AS name, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.beschreibung ELSE erf.beschreibung END AS beschreibung, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.wert ELSE erf.wert END AS wert, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.typ ELSE erf.typ END AS typ, "
			+ "CASE WHEN NOT korr.merkmal_id IS NULL THEN korr.dhid ELSE erf.dhid END AS aktdhid, "
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
			+ ") AS erf "
			+ "FULL OUTER JOIN "
			+ "("
			+ "SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, o.oevart, o.beschreibung, "
			+ "	m.name, m.wert, m.typ, o.dhid, o.objektart "
			+ "FROM merkmalkorrektur AS m "
			+ "JOIN objekt AS o ON m.objekt_id = o.id "
			+ ") AS korr "
			+ "ON erf.objekt_id = korr.objekt_id AND korr.name = erf.name "
			+ "JOIN objekt as hst ON erf.parent_id = hst.id WHERE erf.objektart = 'BuR' ORDER BY aktdhid, erf.objekt_id;";
		
		JSONArray objektarray = new JSONArray();

		JSONObject merkmaleJsonObject = new JSONObject();
		//merkmaleJsonObject.put("objektid", paramObjektid);		// evtl. in API in Response weglassen

		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			NVBWLogger.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			int anzahldatensaetze = 0;
			int anzahlobjekte = 0;
			boolean falscheobjektart = false;
			long objektid = 0;
			long merkmalid = 0;
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
	
					NVBWLogger.info("Länge merkmaleJsonObject: " + merkmaleJsonObject.toString().length());
					objektarray.put(merkmaleJsonObject);
					NVBWLogger.info("Objektarray-Länge nach Erweiterung: " + objektarray.toString().length());
					NVBWLogger.info("Bisherige Anzahl Objekte in objektarray: " + anzahlobjekte);
					merkmaleJsonObject = new JSONObject();
					lon = 0.0;
					lat = 0.0;
					osmimportiert = false;
					osmids = "";
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
//					NVBWLogger.info("Neues Objekt #" + anzahlobjekte + " gefunden, gefunden HST-DHID: " + hstdhid
//						+ ", Objekt-DHID: " + dhid + ", Objektart: " + objektart
//						+ ", Objekt-ID: " + objektid + ", OSM-Importiert? " + osmimportiert);
				}
				anzahldatensaetze++;

				merkmalid = selectMerkmaleRS.getLong("merkmal_id");
				name = selectMerkmaleRS.getString("name");
				wert = selectMerkmaleRS.getString("wert");
				typ = selectMerkmaleRS.getString("typ");
				NVBWLogger.info("objektid: " + objektid + ", Merkmal-ID: " + merkmalid + ", Name: " + name + ", Wert: " + wert + ", Typ: " + typ);

				if(!objektart.equals("BuR")) {
					falscheobjektart = true;
					vorherigeobjektid = objektid;
					continue;
				}

                switch (name) {
                    case "OBJ_BuR_Anlagentyp" -> merkmaleJsonObject.put("anlagentyp", wert);
                    case "OBJ_BuR_Stellplatzanzahl" ->
                            merkmaleJsonObject.put("stellplatzanzahl", (int) Double.parseDouble(wert));
                    case "OBJ_BuR_Beleuchtet" -> merkmaleJsonObject.put("beleuchtet", wert.equals("true"));
                    case "OBJ_BuR_Ueberdacht" -> merkmaleJsonObject.put("ueberdacht", wert.equals("true"));
                    case "OBJ_BuR_Hinderniszufahrt_Beschreibung" -> merkmaleJsonObject.put("hinderniszufahrt", wert);
                    case "OBJ_BuR_Kostenpflichtig" -> merkmaleJsonObject.put("kostenpflichtig", wert.equals("true"));
                    case "OBJ_BuR_KostenpflichtigNotiz" -> merkmaleJsonObject.put("kostenpflichtignotiz", wert);
                    case "OBJ_BuR_WegZurAnlageAnfahrbar" ->
                            merkmaleJsonObject.put("wegzuranlageanfahrbar", wert.equals("true"));
                    case "OBJ_BuR_Lon" -> {
                        if (lon == 0.0)
                            lon = Double.parseDouble(wert);
                    }
                    case "OBJ_BuR_Lat" -> {
                        if (lat == 0.0)
                            lat = Double.parseDouble(wert);
                    }
                    case "OBJ_BuR_Buegelabstand_cm" ->
                            merkmaleJsonObject.put("buegelabstand", (int) Double.parseDouble(wert));
                    case "OBJ_BuR_Notiz" -> merkmaleJsonObject.put("notiz", wert);
                    case "OBJ_BuR_Foto" -> {
                        String urls = Bild.getBildUrl(wert, hstdhid);
                        if ((urls != null) && !urls.isEmpty())
                            merkmaleJsonObject.put("objekt_Foto", urls);
                    }
                    case "OBJ_BuR_Weg_Foto" -> {
                        String urls = Bild.getBildUrl(wert, hstdhid);
                        if ((urls != null) && !urls.isEmpty())
                            merkmaleJsonObject.put("weg_Foto", urls);
                    }
                    case "OBJ_BuR_Besonderheiten_Foto" -> {
                        String urls = Bild.getBildUrl(wert, hstdhid);
                        if ((urls != null) && !urls.isEmpty())
                            merkmaleJsonObject.put("besonderheiten_Foto", urls);
                    }
                    case "OBJ_BuR_Hinderniszufahrt_Foto" -> {
                        String urls = Bild.getBildUrl(wert, hstdhid);
                        if ((urls != null) && !urls.isEmpty())
                            merkmaleJsonObject.put("hinderniszufahrt_Foto", urls);
                    }
                    case "OBJ_BuR_Vorhanden" -> {
                        // nichts zu tun
                    }
                    default -> NVBWLogger.warning("in Servlet " + this.getServletName()
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

				NVBWLogger.info("Länge merkmaleJsonObject: " + merkmaleJsonObject.toString().length() + " Bytes");
				objektarray.put(merkmaleJsonObject);
				NVBWLogger.info("Objektarray-Länge nach Erweiterung: " + objektarray.toString().length() + " Bytes");
				NVBWLogger.info("Am Ende Anzahl Objekte in objektarray: " + anzahlobjekte);
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
				NVBWLogger.severe("falsche Objektart, ENDE Request: " 
					+ datetime_de_formatter.format(requestEnde));
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				response.setCharacterEncoding("UTF-8");
				response.getWriter().append("Parameter objekt_id passt nicht zum Objekttyp Fahrradanlage");
				return;
			}
			NVBWLogger.info("Anzahl Datensätze: " + anzahldatensaetze);
		} catch (SQLException e) {
			NVBWLogger.severe("SQLException::: " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-Abfragefehler aufgetreten, bitte Administrator informieren");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		response.getWriter().append(objektarray.toString());
		NVBWLogger.info("objektarray am Ende: " + objektarray.toString().length()
			+ " Bytes, " + datetime_de_formatter.format(requestEnde));
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		NVBWLogger.info("Request /fahrradanlagen, doPost ...");

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /fahrradanlagen ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
}
