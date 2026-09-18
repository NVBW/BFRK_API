
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
@WebServlet(name = "haltestelle", 
			urlPatterns = {"/haltestelle/*"}
		)
public class haltestelle extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(stopmodell.class);
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public haltestelle() {
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
				LOG.severe("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
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
		String paramDHID = "%";

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

		if(request.getParameter("dhid") != null) {
			LOG.info("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramDHID = URLDecoder.decode(request.getParameter("dhid"),"UTF-8");
		} else {
			LOG.info("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			LOG.info("requesturi ===" + requesturi + "===");
			if(requesturi.indexOf("/haltestelle") != -1) {
				int startpos = requesturi.indexOf("/haltestelle");
				if(requesturi.indexOf("/",startpos + 1) != -1) {
					paramDHID = requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1);
					LOG.info("Versuch, dhid zu extrahieren ===" + paramDHID + "===");
				}
			}
		}
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectHaltestelleSql = "SELECT *,ST_X(koordinate) AS lon, ST_Y(koordinate) AS lat "
			+ "FROM objekt WHERE dhid like ? AND "
			+ "(objektart = ? OR objektart = ?) AND "
			+ "kreisschluessel like ?;";

		JSONArray haltestellenJsonArray = new JSONArray();
		
		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			int stmtindex = 1;
			selectHaltestelleStmt.setString(stmtindex++, paramDHID);
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

				if(objektart.equals("BuR"))
					objektart = "Fahrradanlage";

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
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOG.severe("SQLException::: " + e.toString());
		}
		response.getWriter().append(haltestellenJsonArray.toString());

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
