
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

import de.nvbw.base.NVBWLogger;
import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.bfrk.util.DBVerbindung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "haltestellebyame", 
			urlPatterns = {"/haltestellebyname/*"}
		)
public class haltestellebyname extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(stopmodell.class);
    private static Connection bfrkConn = null;

    public static int SC_NOTFOUND = 404;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public haltestellebyname() {
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
		

		/*Map<String, String[]> alleparameter = request.getParameterMap();
		for(Map.Entry<String, String[]> paramentry: alleparameter.entrySet()) {
			String paramkey = paramentry.getKey();
			String[] paramvalues = paramentry.getValue();
			LOG.info("param  [" + paramkey + "] ===" + paramvalues.toString() + "===");
		}
		*/
		//LOG.info("Request-Uri ===" + request.getRequestURI());
		//LOG.info("Request-Url ===" + request.getRequestURL().toString());
		//LOG.info("Query-String ===" + request.getQueryString() + "===");
		
		String paramKreisid= "%";
		String paramObjektart1 = "Bahnhof";
		String paramObjektart2 = "Haltestelle";
		String paramName = "%";

		if(request.getParameter("kreisid") != null) {
			LOG.info("url-Parameter kreisid vorhanden ===" + request.getParameter("kreisid"));
			paramKreisid = URLDecoder.decode(request.getParameter("kreisid"),"UTF-8");
		}

		if(request.getParameter("oevart") != null) {
			LOG.info("url-Parameter oevart vorhanden ===" + request.getParameter("oevart") + "===");
			String tempOevart = request.getParameter("oevart");
			if(tempOevart.equals("S")) {
				paramObjektart1 = "Bahnhof";
				paramObjektart2 = "Bahnhof";
			} else if(tempOevart.equals("O")) {
				paramObjektart1 = "Haltestelle";
				paramObjektart2 = "Haltestelle";
			} else {
				paramObjektart1 = "Bahnhof";
				paramObjektart2 = "Haltestelle";
			}
		}

		if(request.getParameter("name") != null) {
			LOG.info("url-Parameter name vorhanden ===" + request.getParameter("name"));
			paramName = URLDecoder.decode(request.getParameter("name"),"UTF-8");
			paramName = paramName.replace("*", "%");
		}
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectHaltestelleSql = "SELECT *,ST_X(koordinate) AS lon, ST_Y(koordinate) AS lat "
			+ "FROM objekt WHERE name ILIKE ? AND "
			+ "(objektart = ? OR objektart = ?) AND "
			+ "kreisschluessel like ?;";

		JSONArray haltestellenJsonArray = new JSONArray();
		
		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			int stmtindex = 1;
			selectHaltestelleStmt.setString(stmtindex++, paramName);
			selectHaltestelleStmt.setString(stmtindex++, paramObjektart1);
			selectHaltestelleStmt.setString(stmtindex++, paramObjektart2);
			selectHaltestelleStmt.setString(stmtindex++, paramKreisid);
			LOG.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectHaltestelleRS = selectHaltestelleStmt.executeQuery();

			String importdatei = "";
			int anzahldatensaetze = 0;
			while(selectHaltestelleRS.next()) {
				anzahldatensaetze++;
				String dhid = selectHaltestelleRS.getString("dhid");
				String kreisschluessel = selectHaltestelleRS.getString("kreisschluessel");
				String objektart = selectHaltestelleRS.getString("objektart");
				String name = selectHaltestelleRS.getString("name");
				String oevart = selectHaltestelleRS.getString("oevart");
				String gemeinde = selectHaltestelleRS.getString("gemeinde");
				String ortsteil = selectHaltestelleRS.getString("ortsteil");
				double lon = selectHaltestelleRS.getDouble("lon");
				double lat = selectHaltestelleRS.getDouble("lat");

				JSONObject haltestelleJsonObject = new JSONObject();

				haltestelleJsonObject.put("dhid", dhid);
				haltestelleJsonObject.put("kreisschluessel", kreisschluessel);
				haltestelleJsonObject.put("objektart", objektart);
				haltestelleJsonObject.put("name", name);
				haltestelleJsonObject.put("oevart", oevart);
				haltestelleJsonObject.put("gemeinde", gemeinde);
				haltestelleJsonObject.put("ortsteil", ortsteil);
				haltestelleJsonObject.put("lon", lon);
				haltestelleJsonObject.put("lat", lat);
				
				haltestellenJsonArray.put(haltestelleJsonObject);
			}
			LOG.info("Anzahl Haltestellen: " + anzahldatensaetze);
			selectHaltestelleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
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
		response.getWriter().append(haltestellenJsonArray.toString());

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOG.info("Request angekommen in /haltestellebyname doPost ...");

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /haltestellebyname ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

}
