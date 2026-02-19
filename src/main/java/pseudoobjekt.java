
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.nvbw.base.NVBWLogger;
import org.json.JSONObject;

import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.util.DBVerbindung;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "pseudoobjekt", 
			urlPatterns = {"/pseudoobjekt/*"}
		)
public class pseudoobjekt extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(notiz.class);

	private static final DateFormat date_de_formatter = new SimpleDateFormat("dd.MM.yyyy");

    private static Connection bfrkConn = null;

    
    private static boolean ObjektmerkmaleInDBSpeichern(long objektid, String objektart,
    	String verbindungsinformation) {
    	boolean returncode = false;
 
		Map<BFRKFeld.Name, String> merkmaleMap = new HashMap<>();

        switch (objektart) {
            case "Aufzug" -> {
                merkmaleMap.put(BFRKFeld.Name.OBJ_Aufzug_Vorhanden_D2090, "true");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Aufzug_Verbindungsfunktion_D2095, verbindungsinformation);
                merkmaleMap.put(BFRKFeld.Name.OBJ_Aufzug_Tuerbreite_cm_D2091, "98.765");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenlaenge_cm_D2093, "198.765");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Aufzug_Grundflaechenbreite_cm_D2094, "98.765");
            }
            case "Rampe" -> {
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rampe_Vorhanden_D2120, "true");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rampe_Verbindungsfunktion_D2121, verbindungsinformation);
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rampe_Laenge_cm_D2122, "198.765");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rampe_Breite_cm_D2123, "98.765");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rampe_Neigung_prozent_D2124, "5.987");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rampe_Querneigung_prozent, "1.987");
            }
            case "Rolltreppe" -> {
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rolltreppe_Vorhanden_D2130, "true");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Rolltreppe_Verbindungsfunktion_D2131, verbindungsinformation);
            }
            case "Treppe" -> {
                merkmaleMap.put(BFRKFeld.Name.OBJ_Treppe_Vorhanden_D2110, "true");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Treppe_Verbindungsfunktion_D2111, verbindungsinformation);
                merkmaleMap.put(BFRKFeld.Name.OBJ_Treppe_Stufenanzahl_D2113, "2");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Treppe_Stufenhoehe_cm_D2112, "15.987");
            }
            case "Weg" -> {
                merkmaleMap.put(BFRKFeld.Name.OBJ_Weg_Art, "weg");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Weg_Verbindungsfunktion, verbindungsinformation);
                merkmaleMap.put(BFRKFeld.Name.OBJ_Weg_Laenge_cm_D2020, "198.765");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Weg_Breite_cm_D2021, "98.765");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Weg_Neigung_prozent, "1.987");
                merkmaleMap.put(BFRKFeld.Name.OBJ_Weg_Querneigung_prozent, "1.987");
            }
            case "BuR" -> {
                merkmaleMap.put(BFRKFeld.Name.OBJ_BuR_Vorhanden, "true");
                merkmaleMap.put(BFRKFeld.Name.OBJ_BuR_Notiz, verbindungsinformation);
            }
            case "Parkplatz" -> merkmaleMap.put(BFRKFeld.Name.OBJ_Parkplatz_Art_D1051, "unbekannt");
            default -> {
                LOG.warning("ungültige Objektart für Pseudoobjekt mit Objekt-Id: " + objektid
                        + ", falsche Objektart: " + objektart);
                return false;
            }
        }

		String insertMerkmalSql = "INSERT INTO merkmal (objekt_id, name, wert, typ) VALUES (?, ?, ?, ?) RETURNING id;";

		PreparedStatement insertMerkmalStmt = null;
		try {
			insertMerkmalStmt = bfrkConn.prepareStatement(insertMerkmalSql);

			for(Map.Entry<BFRKFeld.Name, String> merkmaleentry: merkmaleMap.entrySet()) {
				BFRKFeld.Name merkmal = merkmaleentry.getKey();
				String wert = merkmaleentry.getValue();
				
				int stmtindex = 1;
				insertMerkmalStmt.setLong(stmtindex++, objektid);
				insertMerkmalStmt.setString(stmtindex++, merkmal.dbname());
				insertMerkmalStmt.setString(stmtindex++, wert);
				insertMerkmalStmt.setString(stmtindex++, merkmal.typ().name());
				LOG.fine("SQL-insert Statement zum speichern Merkmal '"
					+  insertMerkmalStmt.toString() + "'");
		
				long dbid = 0;
				ResultSet insertMerkmalRs = insertMerkmalStmt.executeQuery();
				if (insertMerkmalRs.next()) {
					dbid = insertMerkmalRs.getLong("id");
				}
	
				LOG.info("Speicherung erfolgt: Merkmal-ID: " + dbid + ""
					+ ",  Objekt-ID: " + objektid + ",  [" + merkmal.dbname() + "] ===" + wert + "===");
				returncode = true;
			}
		} catch (SQLException e1) {
				// am 13.09.2024 Reihenfolge geändert: vorher zuerst ob updateszulaessig
			if(e1.getSQLState().equals("23505")) {
				LOG.warning("Datensatz existiert schon, Details: " + e1.toString());
			} else {
				LOG.severe("SQL-Insert Fehler, als ein Merkmal in die Tabelle eingetragen werden sollte." 
					+ "Statement war '" + insertMerkmalStmt.toString() 
					+ "' Details folgen ...");
				LOG.severe(e1.toString() + ", Code: " + e1.getSQLState());
			}
			returncode = false;
		}
		return returncode;
    }
 
    /**
     * @see HttpServlet#HttpServlet()
     */
    public pseudoobjekt() {
        super();
    }

    /**
     * initialization on servlet startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
		LOG.info("Beginn init pseudoobjekt " + new Date());
		bfrkConn = DBVerbindung.getDBVerbindung();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		LOG.info("Beginn GET pseudoobjekt " + new Date());
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "GET Request nicht verfügbar");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

		LOG.info("Ende GET pseudoobjekt " + new Date());
		return;
	}

	/**
	 * @see HttpServlet#doPut(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		LOG.info("Beginn PUT pseudoobjekt " + new Date());

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "POST");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "PUT Request nicht verfügbar");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);

		LOG.info("Ende PUT pseudoobjekt " + new Date());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject ergebnisJsonObject = new JSONObject();

		LOG.info("Beginn POST pseudoobjekt " + new Date());

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				LOG.severe("es konnte keine DB-Verbindung herstellt werden");
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		} catch (SQLException e1) {
			LOG.severe("SQLException aufgetreten, " + e1.toString());
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e1) {
			LOG.severe("IOException aufgetreten, " + e1.toString());
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unbekannter Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "*");

		if(request.getHeader("accesstoken") == null) {
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Pflicht Header accesstoken fehlt");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String accesstoken = "";

		LOG.info("Request Header accesstoken vorhanden ===" + request.getHeader("accesstoken") + "===");
		accesstoken = request.getHeader("accesstoken");
		if(accesstoken.isEmpty()) {
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen, weil Header accesstoken leer ist");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

			// ============= prüfen, ob accesstoken gültig ist ==============
		String authentifizierungSql = "SELECT modellspeichern, name FROM benutzer "
			+ "WHERE accesstoken = ?;";

		String bearbeiter = null;
		PreparedStatement authentifizierungStmt;
		try {
			authentifizierungStmt = bfrkConn.prepareStatement(authentifizierungSql);
			int stmtindex = 1;
			authentifizierungStmt.setString(stmtindex++, accesstoken);
			LOG.info("Authentifizierung query: " + authentifizierungStmt.toString() + "===");

			ResultSet authentifizierungRS = authentifizierungStmt.executeQuery();

			String dbaccesstoken = null;
			boolean modellspeichern = false;
			
			if(authentifizierungRS.next()) {
				bearbeiter = authentifizierungRS.getString("name");
				modellspeichern = authentifizierungRS.getBoolean("modellspeichern");
			}
			authentifizierungRS.close();
			authentifizierungStmt.close();

			if(!modellspeichern) {
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			
		} catch (SQLException e) {
			LOG.severe("SQLException::: " + e.toString());
			String fehlertext = "DB-Fehler aufgetreten, bitte den Administrtaor benachrichtigen";
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		String paramdhid = "";
		String paramoevart = "";
		String paramobjektart = "";
		double paramlon = 0;
		double paramlat = 0;
		String paramverbindungsinformation = "";
		String parentobjektart = "";
		String paramosmid = "";

		try {
			if(request.getParameter("dhid") != null) {
				LOG.info("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
				paramdhid = request.getParameter("dhid");
			} else {
				String fehlertext = "Parameter dhid ist nicht angegeben";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			if(request.getParameter("oevart") != null) {
				LOG.info("url-Parameter oevart vorhanden ===" + request.getParameter("oevart") + "===");
				paramoevart = request.getParameter("oevart");
				if(!paramoevart.equals("S") && !paramoevart.equals("O")) {
					String fehlertext = "Parameter oevart hat ungültigen Wert, nämlich '" + paramoevart + "'";
					ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
				if(paramoevart.equals("S")) {
					parentobjektart = "Bahnhof";
				} else if(paramoevart.equals("O")) {
					parentobjektart = "Haltestelle";
				} else {
					String fehlertext = "Parameter oevart hat ungültigen Wert, nämlich '" + paramoevart + "'";
					ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
			} else {
				String fehlertext = "Parameter oevart ist nicht angegeben";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}

			if(request.getParameter("verbindungsinformation") != null) {
				LOG.info("url-Parameter verbindungsinformation vorhanden ===" + request.getParameter("verbindungsinformation"));
				paramverbindungsinformation = request.getParameter("verbindungsinformation");
			} else {
				String fehlertext = "Parameter verbindungsinformation ist nicht angegeben";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			if(request.getParameter("lon") != null) {
				LOG.info("url-Parameter lon vorhanden ===" + request.getParameter("lon"));
				paramlon = Double.parseDouble(request.getParameter("lon"));
			} else {
				String fehlertext = "Parameter lon ist nicht angegeben";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			if(request.getParameter("lat") != null) {
				LOG.info("url-Parameter lat vorhanden ===" + request.getParameter("lat"));
				paramlat = Double.parseDouble(request.getParameter("lat"));
			} else {
				String fehlertext = "Parameter lat ist nicht angegeben";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			if(request.getParameter("osmid") != null) {
				LOG.info("url-Parameter osmid vorhanden ===" + request.getParameter("osmid"));
				paramosmid = request.getParameter("osmid");
			}
			if(request.getParameter("objektart") != null) {
				LOG.info("url-Parameter dhid vorhanden ===" + request.getParameter("objektart"));
				paramobjektart = request.getParameter("objektart");
				if((paramobjektart == null) || paramobjektart.isEmpty()) {
					String fehlertext = "Parameter objektart ist nicht angegeben";
					ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					return;
				} else {
					if(		!paramobjektart.equals("Aufzug")
						&&	!paramobjektart.equals("Rampe")
						&&	!paramobjektart.equals("Rolltreppe")
						&&	!paramobjektart.equals("Treppe")
						&&	!paramobjektart.equals("Weg")
						&&	!paramobjektart.equals("BuR")
						&&	!paramobjektart.equals("Parkplatz")) {
						String fehlertext = "Parameter objektart hat keinen gültigen Wert, sondern '" + paramobjektart + "'";
						ergebnisJsonObject = new JSONObject();
						ergebnisJsonObject.put("status", "fehler");
						ergebnisJsonObject.put("fehlertext", fehlertext);
						response.getWriter().append(ergebnisJsonObject.toString());
						response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						return;
					}
				}
			} else {
				String fehlertext = "Parameter objektart ist nicht angegeben";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} catch(Exception e) {
			LOG.severe("Exception aufgetreten bei Parameter auslesen, Details folgen..");
			LOG.severe(e.toString());
			String fehlertext = "Parameter auslesen fehlgeschlagen";
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}


		
		LOG.info("dhid: " + paramdhid);
		LOG.info("oevart: " + paramoevart);
		LOG.info("objektart: " + paramobjektart);
		LOG.info("lon: " + paramlon);
		LOG.info("lat: " + paramlat);
		LOG.info("verbindunginformation: " + paramverbindungsinformation);

		String selectParentObjektSql = "SELECT id, kreisschluessel, gemeinde, ortsteil FROM objekt "
			+ "WHERE oevart = ? AND dhid = ? AND objektart = ?;";

		String insertPseudoObjektSql = "INSERT INTO objekt (kreisschluessel, objektart, dhid, oevart, parent_id, "
			+ "gemeinde, ortsteil, erfassungsdatum, pseudoflag, koordinate) "
			+ "VALUES(?, ?, ?, ?, ?, "
			+ " ?, ?, now(), true, ST_SetSrid(ST_MakePoint(?, ?), 4326)) RETURNING id;";

		String kreisschluessel = "";
		String gemeinde = "";
		String ortsteil = "";
		PreparedStatement selectParentObjektStmt;
		PreparedStatement insertPseudoObjektStmt;
		long pseudoObjektID = 0;
		try {
			selectParentObjektStmt = bfrkConn.prepareStatement(selectParentObjektSql);
			int stmtindex = 1;
			selectParentObjektStmt.setString(stmtindex++, paramoevart);
			selectParentObjektStmt.setString(stmtindex++, paramdhid);
			selectParentObjektStmt.setString(stmtindex++, parentobjektart);
			LOG.info("select Parent-Objekt query: " + selectParentObjektStmt.toString() + "===");

			ResultSet selectParentObjektRS = selectParentObjektStmt.executeQuery();

			int anzahlParentObjekte = 0;
			long parentObjektId = 0;
			List<Long> parentObjektListe = new ArrayList<>();
			while(selectParentObjektRS.next()) {
				anzahlParentObjekte++;
				parentObjektId = selectParentObjektRS.getLong("id");
				kreisschluessel = selectParentObjektRS.getString("kreisschluessel");
				gemeinde = selectParentObjektRS.getString("gemeinde");
				ortsteil = selectParentObjektRS.getString("ortsteil");
				parentObjektListe.add(parentObjektId);
			}
			selectParentObjektRS.close();
			selectParentObjektStmt.close();

			if(anzahlParentObjekte == 0) {
				String fehlertext = "Es wurde kein Objekt vom Typ " + parentobjektart + " gefunden, Abbruch";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			} else if(anzahlParentObjekte > 1) {
				String fehlertext = "Es wurde mehr als 1 Objekt vom Typ " + parentobjektart + " gefunden, Liste Treffer: "
					+ parentObjektListe.toString() + ", Abbruch";
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			insertPseudoObjektStmt = bfrkConn.prepareStatement(insertPseudoObjektSql);
			stmtindex = 1;
			insertPseudoObjektStmt.setString(stmtindex++, kreisschluessel);
			insertPseudoObjektStmt.setString(stmtindex++, paramobjektart);
			insertPseudoObjektStmt.setString(stmtindex++, paramdhid);
			insertPseudoObjektStmt.setString(stmtindex++, paramoevart);
			insertPseudoObjektStmt.setLong(stmtindex++, parentObjektId);
			insertPseudoObjektStmt.setString(stmtindex++, gemeinde);
			insertPseudoObjektStmt.setString(stmtindex++, ortsteil);
			insertPseudoObjektStmt.setDouble(stmtindex++, paramlon);
			insertPseudoObjektStmt.setDouble(stmtindex++, paramlat);
			LOG.info("Pseudo-Objekt store: " + insertPseudoObjektStmt.toString() + "===");

			ResultSet insertPseudoObjektRs = insertPseudoObjektStmt.executeQuery();
			if (insertPseudoObjektRs.next()) {
				pseudoObjektID = insertPseudoObjektRs.getLong("id");
				LOG.info("Erstelltes Pseudo-Objekt hat die Objekt-Id: " + pseudoObjektID);

				insertPseudoObjektRs.close();
				insertPseudoObjektStmt.close();

			    if(ObjektmerkmaleInDBSpeichern(pseudoObjektID, paramobjektart, paramverbindungsinformation)) {
					ergebnisJsonObject.put("objektid", pseudoObjektID);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_OK);
					return;
			    } else {
			    	String fehlertext = "DB-Speicherungsfehler aufgetreten, bitte den Administrator benachrichtigen";
					ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
			    }
			}
			insertPseudoObjektRs.close();
			insertPseudoObjektStmt.close();

			String fehlertext = "SQL-Speicherungsfehler: es wurde nach dem DB-INSERT kein Ergebnisdatensatz gefunden, "
				+ "vermutlich also nicht gespeichert. Bitte den Programmierer informieren.";
			LOG.severe(fehlertext);
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (SQLException e) {
			LOG.severe("SQLException::: " + e.toString());
			String fehlertext = "DB-Fehler aufgetreten, bitte den Administrator benachrichtigen";
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

		if((paramosmid == null) || paramosmid.isEmpty()) {
			return;
		}

		String insertOsmObjektbezugSql = "INSERT INTO osmobjektbezug (objekt_id, osmid, osmstatus, subobjektart) "
				+ "VALUES(?, ?, 'offen', 'Hauptobjekt') RETURNING id;";

		PreparedStatement insertOsmObjektbezugStmt = null;
		long osmdbID = 0;
		try {
			insertOsmObjektbezugStmt = bfrkConn.prepareStatement(insertOsmObjektbezugSql);
			int stmtindex = 1;
			insertOsmObjektbezugStmt.setLong(stmtindex++, pseudoObjektID);
			insertOsmObjektbezugStmt.setString(stmtindex++, paramosmid);
			LOG.info("OSM-Objektbezug speichern zum Pseudo-Objekt: " + insertOsmObjektbezugStmt.toString() + "===");

			ResultSet insertOSmObjektbezugRs = insertOsmObjektbezugStmt.executeQuery();
			if (insertOSmObjektbezugRs.next()) {
				osmdbID = insertOSmObjektbezugRs.getLong("id");
				LOG.info("Erstelltes OsmObjektbezug-Objekt hat die Objekt-Id: " + osmdbID);

				insertOSmObjektbezugRs.close();
				insertOsmObjektbezugStmt.close();

				ergebnisJsonObject.put("objektid", pseudoObjektID);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}
			insertOSmObjektbezugRs.close();
			insertOsmObjektbezugStmt.close();

		} catch (SQLException e) {
			LOG.severe("SQLException bei insert OsmObjektbezug: " + e.toString());
			LOG.severe("SQL-Statement war: " + insertOsmObjektbezugStmt.toString());
			String fehlertext = "DB-Fehler aufgetreten, bitte den Administrator benachrichtigen";
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}

	}
}
