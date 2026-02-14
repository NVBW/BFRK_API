
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.DBVerbindung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "haltestellenobjektart", 
			urlPatterns = {"/haltestellenobjektart/*"}
		)
public class haltestellenobjektart extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(stopmodell.class);
    private static Connection bfrkConn = null;

    public static int SC_NOTFOUND = 404;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public haltestellenobjektart() {
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

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject resultObjectJson = new JSONObject();

		LOG.info("===== Request /haltestellenobjektart GET ...");

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				LOG.info("keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					LOG.severe("keine DB-Verbindung hergestellt");
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
		} catch (SQLException e1) {
			LOG.severe("bei DB-init SQLException aufgetreten, Details folgen ...");
			LOG.severe(e1.toString());
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "SQLException bei Aufbau Datenbankverbindung aufgetreten, bitte später nochmal versuchen");
			errorObjektJson.put("message", "DB-Verbindung SQLException");
			errorObjektJson.put("messageId", 124);
			resultObjectJson.put("error", errorObjektJson);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			return;
		} catch (IOException e1) {
			LOG.severe("bei DB-init IOException aufgetreten, Details folgen ...");
			LOG.severe(e1.toString());
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "IOException bei Aufbau Datenbankverbindung aufgetreten, bitte später nochmal versuchen");
			errorObjektJson.put("message", "DB-Verbindung IOException");
			errorObjektJson.put("messageId", 125);
			resultObjectJson.put("error", errorObjektJson);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			return;
		}

		String paramDHID = null;
		if(request.getParameter("dhid") != null) {
			LOG.info("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramDHID = URLDecoder.decode(request.getParameter("dhid"),"UTF-8");
		} else {
			LOG.info("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			LOG.info("requesturi ===" + requesturi + "===");
			if(requesturi.contains("/haltestellenobjektart")) {
				int startpos = requesturi.indexOf("/haltestellenobjektart");
				if(requesturi.indexOf("/",startpos + 1) != -1) {
					paramDHID = requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1);
					LOG.info("Versuch, dhid zu extrahieren ===" + paramDHID + "===");
				}
			}
		}
		if((paramDHID == null) || paramDHID.isEmpty()) {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request Pflicht-Parameter dhid fehlt");
			errorObjektJson.put("message", "Paramater dhid muss gesetzt werden.");
			errorObjektJson.put("messageId", 9994711);
			resultObjectJson.put("error", errorObjektJson);
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		String paramOevart = "";
		String paramObjektart = "";
		if(request.getParameter("oevart") != null) {
			LOG.info("url-Parameter oevart vorhanden ===" + request.getParameter("oevart"));
			paramOevart = URLDecoder.decode(request.getParameter("oevart"),"UTF-8");
			if(paramOevart.equalsIgnoreCase("O"))
				paramObjektart = "Haltestelle";
			else if(paramOevart.equalsIgnoreCase("S"))
				paramObjektart = "Bahnhof";
			else {
				JSONObject errorObjektJson = new JSONObject();
				errorObjektJson.put("subject", "Request parameter oevart hat falschen Wert");
				errorObjektJson.put("message", "Paramaterwert unzulässig, mögliche Werte sind S|O, Wert war "
					+ request.getParameter("oevart").toUpperCase());
				errorObjektJson.put("messageId", 9994712);
				resultObjectJson.put("error", errorObjektJson);
				response.getWriter().append(resultObjectJson.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request Pflicht-Parameter oevart fehlt");
			errorObjektJson.put("message", "Paramater oevart muss gesetzt werden, mögliche Werte sind S|O");
			errorObjektJson.put("messageId", 9994712);
			resultObjectJson.put("error", errorObjektJson);
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		String selectHaltestelleSql = "SELECT subobjekt.id, objektart, beschreibung FROM objekt AS subobjekt JOIN "
			+ "(SELECT id FROM objekt WHERE dhid = ? AND objektart = ?) AS hauptobjekt ON "
			+ "hauptobjekt.id = subobjekt.id OR hauptobjekt.id = subobjekt.parent_id "
			+ "ORDER BY dhid, objektart;";

		JSONArray objektartenJsonArray = new JSONArray();
		
		PreparedStatement selectHaltestelleStmt = null;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			selectHaltestelleStmt.setString(1, paramDHID);
			selectHaltestelleStmt.setString(2, paramObjektart);
			LOG.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectHaltestelleRS = selectHaltestelleStmt.executeQuery();

			int anzahldatensaetze = 0;
			while(selectHaltestelleRS.next()) {
				anzahldatensaetze++;
				Long id = selectHaltestelleRS.getLong("id");
				String objektart = selectHaltestelleRS.getString("objektart");
				String beschreibung = selectHaltestelleRS.getString("beschreibung");

				if(objektart.equals("BuR"))
					objektart = "Fahrradanlage";

				JSONObject haltestelleJsonObject = new JSONObject();
				haltestelleJsonObject.put("id",  id);
				haltestelleJsonObject.put("objektart", objektart);
				haltestelleJsonObject.put("beschreibung", beschreibung);

				objektartenJsonArray.put(haltestelleJsonObject);
			}
			selectHaltestelleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			LOG.severe("bei DB-Query SQLException aufgetreten, Details folgen ...");
			LOG.severe(e.toString());
			if(selectHaltestelleStmt != null)
				LOG.severe("DB-Query war: " + selectHaltestelleStmt.toString());
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "SQLException bei Datenbankabfrage aufgetreten, bitte nochmal versuchen");
			errorObjektJson.put("message", "DB-Abfrage SQLException");
			errorObjektJson.put("messageId", 2001);
			resultObjectJson.put("error", errorObjektJson);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			return;
		}
		response.getWriter().append(objektartenJsonArray.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOG.info("Request angekommen in /notiz doPost ...");

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /notiz ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

}
