
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "filter", 
			urlPatterns = {"/filter/*"}
		)
public class filter extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(notiz.class);
    private static Connection bfrkConn = null;

	private static final DateFormat date_rfc3339_formatter = new SimpleDateFormat("yyyy-MM-dd");

	/**
     * @see HttpServlet#HttpServlet()
     */
    public filter() {
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
		final String EXTENSION = "_nvbw_";

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				LOG.severe("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					LOG.severe("es konnte keine DB-Verbindung herstellt werden");
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren");
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		} catch (SQLException e1) {
			LOG.severe("SQLException aufgetreten, " + e1.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e1) {
			LOG.severe("IOException aufgetreten, " + e1.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unbekannter Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
			LOG.info("url-Parameter filter vorhanden ===" + request.getParameter("filter"));
			filtertext = request.getParameter("filter");
		} else {
			LOG.info("kein zulässiger Parameter vorhanden ...");
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request parameter filter fehlt");
			errorObjektJson.put("message", "Der Parameter filter ist nicht vorhanden oder leer");
			errorObjektJson.put("messageId", 9994711);
			resultObjectJson.put("error", errorObjektJson);
			response.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		//TODO die 3 Parameter analysieren
		String paramstopareaDHID = "";
		String paramstopDHID = "de:08116:7800";
		String paramstoppointDHID = "%";

		//  {"StopArea":"" , "ID" :"de:08111:2235" , "StopPointID":""}
		if(filtertext.startsWith("{")) {
			LOG.info("ok, ich parse den filtertext ...");
			try {
				JSONObject jsonObject = new JSONObject(filtertext);
				paramstopareaDHID = jsonObject.getString("StopArea");
				paramstopDHID = jsonObject.getString("ID");
				paramstoppointDHID = jsonObject.getString("StopPointID");
				if(paramstoppointDHID.isEmpty())
					paramstoppointDHID = "%";
				LOG.info("paramstopareaDHID ===" + paramstopareaDHID + "===");
				LOG.info("paramstopDHID ===" + paramstopDHID + "===");
				LOG.info("paramstoppointDHID ===" + paramstoppointDHID + "===");
			} catch(JSONException e) {
				LOG.severe("beim parsen von fltertext kam eine exception: " + e.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}

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
			+ "erf.hst_id, erf.parent_id, erf.oevart, erf.hst_dhid, erf.infraidtemp, erf.osmids, erf.osmlon, erf.osmlat, osmimportiert, erf.erfassungsdatum "
			+ "FROM ( SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, "
			+ "hst.id AS hst_id, hst.dhid AS hst_dhid, "
			+ "o.oevart, o.beschreibung, o.infraidtemp, "
			+ "m.name AS merkmalname, m.wert AS merkmalwert, m.typ AS merkmaltyp, o.dhid AS objekt_dhid, o.objektart, o.erfassungsdatum, "
			+ "osm.osmid AS osmids, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_X(osm.koordinate) END AS osmlon, "
			+ "CASE WHEN osm.osmid IS NULL THEN 0.0 ELSE ST_Y(osm.koordinate) END AS osmlat, "
			+ "osmimport.vollstaendig AS osmimportiert "
			+ "FROM merkmal AS m "
			+ "JOIN objekt AS o ON m.objekt_id = o.id "
			+ "JOIN objekt AS hst ON o.parent_id = hst.id "
			+ "LEFT JOIN osmobjektbezug AS osm on o.id = osm.objekt_id AND subobjektart = 'Hauptobjekt' "
			+ "LEFT JOIN osmimport on o.id = osmimport.objekt_id "
			+ "WHERE "
			+ "hst.dhid like ?"
			+ "AND o.dhid like ? "
			+ ") AS erf "
			+ "LEFT JOIN (SELECT m.id AS merkmal_id, o.id AS objekt_id, o.parent_id, "
			+ "o.oevart, o.beschreibung, m.name, m.wert, m.typ, o.dhid, o.objektart "
			+ "FROM merkmalkorrektur AS m  JOIN objekt AS o "
			+ "ON m.objekt_id = o.id "
			+ ") AS korr "
			+ "ON erf.objekt_id = korr.objekt_id AND korr.name = erf.merkmalname "
			+ "ORDER BY erf.hst_dhid, erf.parent_id, erf.objektart, erf.objekt_id;";

		JSONArray resultJSA = new JSONArray();

		JSONObject aktJsonObjekt = new JSONObject();

		JSONArray steigeJsonArray = new JSONArray();
		JSONArray aufzuegeJsonArray = new JSONArray();
		JSONArray fahrradanlagenJsonArray = new JSONArray();
		JSONArray parkplaetzeJsonArray = new JSONArray();
		JSONArray rampenJsonArray = new JSONArray();
		JSONArray rolltreppenJsonArray = new JSONArray();
		JSONArray taxisJsonArray = new JSONArray();
		JSONArray toilettenJsonArray = new JSONArray();
		JSONArray treppenJsonArray = new JSONArray();
		JSONArray tuerenJsonArray = new JSONArray();

		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			selectHaltestelleStmt.setString(1, paramstopDHID);
			selectHaltestelleStmt.setString(2, paramstoppointDHID);
			LOG.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			int anzahlhauptobjekte = 0;
			int anzahldatensaetze = 0;
			long aktuelleHstObjektID = 0;
			long aktuelleObjektID = 0;
			long vorherigeHstObjektID = 0;
			String aktuelleHstDHID = "";
			String vorherigeHstDHID = "";
			long vorherigeObjektID = 0;
			String objektart = "";
			String vorherigeObjektart = "";
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				aktuelleHstObjektID = selectMerkmaleRS.getLong("hst_id");
				aktuelleObjektID = selectMerkmaleRS.getLong("objekt_id");
				objektart = selectMerkmaleRS.getString("objektart");
				aktuelleHstDHID = selectMerkmaleRS.getString("hst_dhid");
				String steigDHID = selectMerkmaleRS.getString("objekt_dhid");
				String infraidtemp = selectMerkmaleRS.getString("infraidtemp");
				String merkmalname = selectMerkmaleRS.getString("merkmalname");
				String merkmalwert = selectMerkmaleRS.getString("merkmalwert");
				String merkmaltyp = selectMerkmaleRS.getString("merkmaltyp");
				Date erfassungsdatum = selectMerkmaleRS.getDate("erfassungsdatum");
				String osmids = selectMerkmaleRS.getString("osmids");
				double osmlon = selectMerkmaleRS.getDouble("osmlon");
				double osmlat = selectMerkmaleRS.getDouble("osmlat");
				boolean osmimportiert = selectMerkmaleRS.getBoolean("osmimportiert");
					// wenn nächstes Hauptobjekt (Bahnhof oder Haltestelle) gefunden wurde, vorherige Daten speichern
				if(!aktuelleHstDHID.equals(vorherigeHstDHID) && !vorherigeHstDHID.isEmpty()) {
					LOG.info("vorherige Haltestellen-DHID: " + vorherigeHstDHID +
							", aktuelle Haltestellen-DHID: " + aktuelleHstDHID);

					JSONObject aktresultJSO = new JSONObject();
					aktresultJSO.put("ExternalKey", aktuelleHstDHID);
					if(!steigeJsonArray.isEmpty()) {
						aktresultJSO.put("StopPoints", steigeJsonArray);
						LOG.info("- Ergänzung Steige: " + steigeJsonArray.length());
					}
					if(!aufzuegeJsonArray.isEmpty()) {
						aktresultJSO.put("Elevators", aufzuegeJsonArray);
						LOG.info("- Ergänzung Aufzüge: " + aufzuegeJsonArray.length());
					}
					if(!fahrradanlagenJsonArray.isEmpty()) {
						aktresultJSO.put("BikeAndRides", fahrradanlagenJsonArray);
						LOG.info("- Ergänzung Fahrradanlagen: " + fahrradanlagenJsonArray.length());
					}
					if(!parkplaetzeJsonArray.isEmpty()) {
						aktresultJSO.put("ParkAndRides", parkplaetzeJsonArray);
						LOG.info("- Ergänzung Parkplätze: " + parkplaetzeJsonArray.length());
					}
					if(!rampenJsonArray.isEmpty()) {
						aktresultJSO.put("Ramps", rampenJsonArray);
						LOG.info("- Ergänzung Rampen: " + rampenJsonArray.length());
					}
					if(!rolltreppenJsonArray.isEmpty()) {
						aktresultJSO.put("Escalators", rolltreppenJsonArray);
						LOG.info("- Ergänzung Rolltreppen: " + rolltreppenJsonArray.length());
					}
					if(!taxisJsonArray.isEmpty()) {
						aktresultJSO.put("Taxis", taxisJsonArray);
						LOG.info("- Ergänzung Taxistände: " + taxisJsonArray.length());
					}
					if(!toilettenJsonArray.isEmpty()) {
						aktresultJSO.put("Toilets", toilettenJsonArray);
						LOG.info("- Ergänzung Toiletten: " + toilettenJsonArray.length());
					}
					if(!treppenJsonArray.isEmpty()) {
						aktresultJSO.put("Stairs", treppenJsonArray);
						LOG.info("- Ergänzung Treppen: " + treppenJsonArray.length());
					}
					if(!tuerenJsonArray.isEmpty()) {
						aktresultJSO.put("DoorEntrances", tuerenJsonArray);
						LOG.info("- Ergänzung Türen: " + tuerenJsonArray.length());
					}
					if(!aktresultJSO.isEmpty()) {
						resultJSA.put(aktresultJSO);
						anzahlhauptobjekte++;
					}

					steigeJsonArray = new JSONArray();
					aufzuegeJsonArray = new JSONArray();
					fahrradanlagenJsonArray = new JSONArray();
					parkplaetzeJsonArray = new JSONArray();
					rampenJsonArray = new JSONArray();
					rolltreppenJsonArray = new JSONArray();
					taxisJsonArray = new JSONArray();
					toilettenJsonArray = new JSONArray();
					treppenJsonArray = new JSONArray();
					tuerenJsonArray = new JSONArray();
				}
					// wenn nächstes Objekt gefunden wurde, vorheriges speichern
				if((aktuelleObjektID != vorherigeObjektID) && (vorherigeObjektID != -1)) {
					LOG.info("vorherige Objektart: " + vorherigeObjektart + " (" + vorherigeObjektID + ")" +
							", aktuell: " + objektart + " (" + aktuelleObjektID + ")");
                    switch (vorherigeObjektart) {
                        case "Bahnsteig", "Haltesteig" -> steigeJsonArray.put(aktJsonObjekt);
                        case "Aufzug" -> aufzuegeJsonArray.put(aktJsonObjekt);
                        case "BuR" -> fahrradanlagenJsonArray.put(aktJsonObjekt);
                        case "Parkplatz" -> parkplaetzeJsonArray.put(aktJsonObjekt);
                        case "Rampe" -> rampenJsonArray.put(aktJsonObjekt);
                        case "Rolltreppe" -> rolltreppenJsonArray.put(aktJsonObjekt);
                        case "Taxistand" -> taxisJsonArray.put(aktJsonObjekt);
						case "Toilette" -> toilettenJsonArray.put(aktJsonObjekt);
                        case "Treppe" -> treppenJsonArray.put(aktJsonObjekt);
						case "Tür" -> tuerenJsonArray.put(aktJsonObjekt);
                    }
					aktJsonObjekt = new JSONObject();
				}

				if(objektart.equals("Bahnsteig") || objektart.equals("Haltesteig")) {
					if((steigDHID != null) && !steigDHID.isEmpty())
						aktJsonObjekt.put("Id", steigDHID);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}

					switch (merkmalname) {
						case "OBJ_Aufzug_Tuerbreite_cm_D2091" ->
								aktJsonObjekt.put("HasDestinationDisplay", merkmalwert.equals("true"));
						case "STG_DynFahrtzielanzeiger_Akustisch_D1141" ->
								aktJsonObjekt.put("HasAccousticDestinationDisplay", merkmalwert.equals("true"));
						case "STG_Ansagen_Vorhanden_D1150" ->
								aktJsonObjekt.put("HasAnncouncments", merkmalwert.equals("true"));

						case "STG_Bodenindikator_Vorhanden_D2070" ->
								aktJsonObjekt.put("HasGroundIndication", merkmalwert.equals("true"));
						case "STG_Bodenindikator_EinstiegUndAuffind_D2071" ->
								aktJsonObjekt.put("HasEntranceGroundIndication", merkmalwert.equals("true"));
						case "STG_Bodenindikator_Leitstreifen_D2072" ->
								aktJsonObjekt.put("HasTactileGroundIndication", merkmalwert.equals("true"));
						case "STG_Bahnsteig_Hoehe_ueberGleis_cm_D1170" ->
								aktJsonObjekt.put("PltHeight1", (int) Double.parseDouble(merkmalwert));
						case "STG_Einstiegrampe_vorhanden_D1210" ->
								aktJsonObjekt.put("HasMobileRamp", merkmalwert.equals("true"));
						case "STG_Einstiegrampe_Laenge_cm_D1211" ->
								aktJsonObjekt.put("MobileRampLength", (int) Double.parseDouble(merkmalwert));
						case "STG_Einstiegrampe_Tragfaehigkeit_kg_D1212" ->
								aktJsonObjekt.put("MobileRampMaxLoad", (int) Double.parseDouble(merkmalwert));
						case "STG_Laengsneigung" ->
								aktJsonObjekt.put("Inclination", (int) Math.round(Double.parseDouble(merkmalwert)));
						case "STG_Querneigung" ->
								aktJsonObjekt.put("Crossfall", (int) Math.round(Double.parseDouble(merkmalwert)));
						case "STG_Beleuchtung" ->
								aktJsonObjekt.put("Illumination", merkmalwert.equals("true"));
						/*
                    "HasSeating": true,
                    "StreetAccess": 1,
                    "BospSpaceProvided": 389,
                    "BospLength": 28600,
                    "PassageWidth": 362,
                    "Pavement": 1, */
					}
				}

				if(objektart.equals("Aufzug")) {
					aktJsonObjekt.put("Id", infraidtemp);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
                    switch (merkmalname) {
                        case "OBJ_Aufzug_Tuerbreite_cm_D2091" ->
                                aktJsonObjekt.put("Width", (int) Double.parseDouble(merkmalwert));
                        case "OBJ_Aufzug_Grundflaechenlaenge_cm_D2093" ->
                                aktJsonObjekt.put("Length", (int) Double.parseDouble(merkmalwert));
                        case "OBJ_Aufzug_Grundflaechenbreite_cm_D2094" ->
                                aktJsonObjekt.put("Depth", (int) Double.parseDouble(merkmalwert));
                        case "OBJ_Aufzug_Verbindungsfunktion_D2095" ->
								aktJsonObjekt.put("Description", merkmalwert);
                    }
				}

				if(objektart.equals("BuR")) {
					if((steigDHID != null) && !steigDHID.isEmpty())
						aktJsonObjekt.put("Id", steigDHID);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_BuR_Anlagentyp" ->
								aktJsonObjekt.put(EXTENSION + "Type", merkmalwert);
					}
				}

				if(objektart.equals("Parkplatz")) {
					aktJsonObjekt.put("OsmId", osmids);
					if((steigDHID != null) && !steigDHID.isEmpty())
						aktJsonObjekt.put("Id", steigDHID);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_Parkplatz_Kapazitaet" ->
								aktJsonObjekt.put("NoParkingSpaces", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Parkplatz_BehindertenplaetzeKapazitaet" ->
								aktJsonObjekt.put("NoParkingSpacesWheelChairAcc", (int) Double.parseDouble(merkmalwert));
					}
				}

				if(objektart.equals("Rampe")) {
					aktJsonObjekt.put("Id", infraidtemp);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_Rampe_Laenge_cm_D2122" ->
								aktJsonObjekt.put("Length", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Rampe_Breite_cm_D2123" ->
								aktJsonObjekt.put("Width", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Rampe_Neigung_prozent_D2124" ->
								aktJsonObjekt.put("Inclination", (int) Math.round(Double.parseDouble(merkmalwert)));
						case "OBJ_Rampe_Verbindungsfunktion_D2121" ->
								aktJsonObjekt.put("Description", merkmalwert);
					}
				}

				if(objektart.equals("Rolltreppe")) {
					aktJsonObjekt.put("Id", infraidtemp);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_Rolltreppe_Laufzeit_sek_D2134" ->
								aktJsonObjekt.put("TransportTime", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Rolltreppe_Fahrtrichtung_D2132" -> {
							String ausgabetext = "";
							if(merkmalwert.equals("beide_richtungen") ||
									merkmalwert.equals("hauptrichtung_abwaerts") ||
									merkmalwert.equals("hauptrichtung_aufwaerts"))
								ausgabetext = "0 both";
							else if(merkmalwert.equals("nur_abwaerts"))
								ausgabetext = "2 down";
							else if(merkmalwert.equals("nur_aufwaerts"))
								ausgabetext = "1 up";
							else
								LOG.warning("Merkmalname OBJ_Rolltreppe_Fahrtrichtung_D2132 hat unerwarteten Wert " + merkmalwert);
							if(!ausgabetext.isEmpty())
								aktJsonObjekt.put("TransportDirection", ausgabetext);
						}
						case "OBJ_Rolltreppe_Verbindungsfunktion_D2131" ->
								aktJsonObjekt.put("Description", merkmalwert);
					}
				}

				if(objektart.equals("Taxistand")) {
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					if((steigDHID != null) && !steigDHID.isEmpty())
						aktJsonObjekt.put("Id", steigDHID);
				}

				if(objektart.equals("Toilette")) {
					aktJsonObjekt.put("Id", infraidtemp);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_Toilette_Rollstuhltauglich" -> {
							String ausgabetext = "";
							if (merkmalwert.startsWith("ja"))
								aktJsonObjekt.put("WheelChairAcc", true);
							if (merkmalwert.equals("ja_allereisenden"))
								aktJsonObjekt.put("WheeChairAndTravellerAcc", true);
							if (merkmalwert.equals("ja_euroschluessel"))
								aktJsonObjekt.put("EuroKeyAcc", true);
							if (merkmalwert.equals("ja_lokalschluessel"))
								aktJsonObjekt.put("SpecialKeyAcc", true);
						/*
				          "description": {
        				  "type": "string" */
						}

					}
				}

				if(objektart.equals("Treppe")) {
					aktJsonObjekt.put("Id", infraidtemp);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_Treppe_Stufenhoehe_cm_D2112" ->
								aktJsonObjekt.put("StepHeight", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Treppe_Stufenanzahl_D2113" ->
								aktJsonObjekt.put("NoSteps", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Treppe_Handlauf_links" ->
								aktJsonObjekt.put("HandrailLeft", merkmalwert.equals("true"));
						case "OBJ_Treppe_Handlauf_mittig" ->
								aktJsonObjekt.put("HandrailCentered", merkmalwert.equals("true"));
						case "OBJ_Treppe_Handlauf_rechts" ->
								aktJsonObjekt.put("HandrailRight", merkmalwert.equals("true"));
						case "OBJ_Treppe_Verbindungsfunktion_D2111" ->
								aktJsonObjekt.put("Description", merkmalwert);
					}
				}

				if(objektart.equals("Tür")) {
					aktJsonObjekt.put("Id", infraidtemp);
					aktJsonObjekt.put("OsmId", osmids);
					if(osmids != null) {
						aktJsonObjekt.put("CoordX", osmlon);
						aktJsonObjekt.put("CoordY", osmlat);
					}
					switch (merkmalname) {
						case "OBJ_Tuer_Oeffnungszeiten_D2031" ->
								aktJsonObjekt.put("OpeningHours", merkmalwert);
						case "OBJ_Tuer_Breite_cm_D2034" ->
								aktJsonObjekt.put("Space", (int) Double.parseDouble(merkmalwert));
						case "OBJ_Tuer_Art_D2032" -> {
								aktJsonObjekt.put("DoorEntranceType", merkmalwert);
								LOG.warning("unklar, ob OBJ_Tuer_Art_D2032 => DoorEntranceType richtig ist");
						}
						case "OBJ_Tuer_Oeffnungsart_D2033" -> {
								aktJsonObjekt.put("DoorType", merkmalwert);
								LOG.warning("unklar, ob OBJ_Tuer_Oeffnungsart_D2033 => DoorType richtig ist");
						}
					}
				}

				vorherigeHstObjektID = aktuelleHstObjektID;
				vorherigeHstDHID = aktuelleHstDHID;
				vorherigeObjektID = aktuelleObjektID;
				vorherigeObjektart = objektart;
			} // Ende Schleife über alle Merkmale

			// das letzte Objekt noch speichern
            switch (objektart) {
                case "Bahnsteig", "Haltesteig" -> steigeJsonArray.put(aktJsonObjekt);
                case "Aufzug" -> aufzuegeJsonArray.put(aktJsonObjekt);
                case "BuR" -> fahrradanlagenJsonArray.put(aktJsonObjekt);
                case "Parkplatz" -> parkplaetzeJsonArray.put(aktJsonObjekt);
                case "Rampe" -> rampenJsonArray.put(aktJsonObjekt);
                case "Treppe" -> treppenJsonArray.put(aktJsonObjekt);
                case "Taxistand" -> taxisJsonArray.put(aktJsonObjekt);
            }

			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			} else {
				LOG.info("Am Ende ist mindestens ein Datensatz vorhanden, also Aufbau Ergebnis-JSON-Struktur ...");
				JSONObject aktresultJSO = new JSONObject();
				aktresultJSO.put("ExternalKey", aktuelleHstDHID);
				if(!steigeJsonArray.isEmpty()) {
					aktresultJSO.put("StopPoints", steigeJsonArray);
					LOG.info("- Ergänzung Steige: " + steigeJsonArray.length());
				}
				if(!aufzuegeJsonArray.isEmpty()) {
					aktresultJSO.put("Elevators", aufzuegeJsonArray);
					LOG.info("- Ergänzung Aufzüge: " + aufzuegeJsonArray.length());
				}
				if(!fahrradanlagenJsonArray.isEmpty()) {
					aktresultJSO.put("BikeAndRides", fahrradanlagenJsonArray);
					LOG.info("- Ergänzung Fahrradanlagen: " + fahrradanlagenJsonArray.length());
				}
				if(!parkplaetzeJsonArray.isEmpty()) {
					aktresultJSO.put("ParkAndRides", parkplaetzeJsonArray);
					LOG.info("- Ergänzung Parkplätze: " + parkplaetzeJsonArray.length());
				}
				if(!rampenJsonArray.isEmpty()) {
					aktresultJSO.put("Ramps", rampenJsonArray);
					LOG.info("- Ergänzung Rampen: " + rampenJsonArray.length());
				}
				if(!taxisJsonArray.isEmpty()) {
					aktresultJSO.put("Taxis", taxisJsonArray);
					LOG.info("- Ergänzung Taxistände: " + taxisJsonArray.length());
				}
				if(!treppenJsonArray.isEmpty()) {
					aktresultJSO.put("Stairs", treppenJsonArray);
					LOG.info("- Ergänzung Treppen: " + treppenJsonArray.length());
				}
				if(!aktresultJSO.isEmpty()) {
					resultJSA.put(aktresultJSO);
					anzahlhauptobjekte++;
				}

				JSONObject gesamtResultJsonObjekt = new JSONObject();
				gesamtResultJsonObjekt.put("count", anzahlhauptobjekte);
				gesamtResultJsonObjekt.put("result", resultJSA);

				LOG.info("erstelltes Gesamt-JSON: " + gesamtResultJsonObjekt.toString());
				response.getWriter().append(gesamtResultJsonObjekt.toString());
				response.setStatus(HttpServletResponse.SC_OK);
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
	}
}
