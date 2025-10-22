
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
@WebServlet(name = "suche", 
			urlPatterns = {"/suche/*"}
		)
public class suche extends HttpServlet {
	private static final long serialVersionUID = 1L;

    private static Connection bfrkConn = null;

    public static int METHOD_NOT_ALLOWED = 405;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public suche() {
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
		
		NVBWLogger.info("requesturi ===" + request.getRequestURI() + "===");
		NVBWLogger.info("querystring ===" + request.getQueryString() + "===");

		String paramSuche = "%";
		String paramGemeinde = "%";
		String paramObjektart1 = "Bahnhof";
		String paramObjektart2 = "Haltestelle";
		if(request.getParameter("suche") != null) {
			System.out.println("url-Parameter suche vorhanden ===" + request.getParameter("suche") + "===");
			paramSuche = "%" + request.getParameter("suche") + "%";
		}
		if(request.getParameter("gemeinde") != null) {
			System.out.println("url-Parameter gemeinde vorhanden ===" + request.getParameter("gemeinde") + "===");
			paramGemeinde = request.getParameter("gemeinde");
		}
		if(request.getParameter("oevart") != null) {
			System.out.println("url-Parameter oevart vorhanden ===" + request.getParameter("oevart") + "===");
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
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectSucheSql = "SELECT o.dhid, o.kreisschluessel, o.objektart, "
			+ "haltename AS name, o.oevart, h.gemeinde, h.ortsteil, "
			+ "ST_X(koordinate) AS lon, ST_Y(koordinate) AS lat FROM "
			+ "objekt AS o JOIN haltestellenliste AS h ON h.dhid = o.dhid "
			+ "WHERE (o.objektart = ? OR o.objektart = ?) AND "
			+ "haltename ilike ? AND "
			+ "h.gemeinde like ?;";


		JSONArray haltestellenJsonArray = new JSONArray();

		PreparedStatement selectSucheStmt;
		try {
			selectSucheStmt = bfrkConn.prepareStatement(selectSucheSql);
			int stmtindex = 1;
			selectSucheStmt.setString(stmtindex++, paramObjektart1);
			selectSucheStmt.setString(stmtindex++, paramObjektart2);
			selectSucheStmt.setString(stmtindex++, paramSuche);
			selectSucheStmt.setString(stmtindex++, paramGemeinde);
			System.out.println("Suche query: " + selectSucheStmt.toString() + "===");

			ResultSet selectSucheRS = selectSucheStmt.executeQuery();

			int anzahl = 0;
			while(selectSucheRS.next()) {
				anzahl++;

				JSONObject haltestelleJsonObject = new JSONObject();

				String dhid = selectSucheRS.getString("dhid");
				haltestelleJsonObject.put("dhid", dhid);
				String kreisschluessel = selectSucheRS.getString("kreisschluessel");
				haltestelleJsonObject.put("kreisschluessel", kreisschluessel);
				String objektart = selectSucheRS.getString("objektart");
				haltestelleJsonObject.put("objektart", objektart);
				String name = selectSucheRS.getString("name");
				haltestelleJsonObject.put("name", name);
				String oevart = selectSucheRS.getString("oevart");
				haltestelleJsonObject.put("oevart", oevart);
				String gemeinde = selectSucheRS.getString("gemeinde");
				haltestelleJsonObject.put("gemeinde", gemeinde);
				String ortsteil = selectSucheRS.getString("ortsteil");
				haltestelleJsonObject.put("ortsteil", ortsteil);
				double lon = selectSucheRS.getDouble("lon");
				haltestelleJsonObject.put("lon", lon);
				double lat = selectSucheRS.getDouble("lat");
				haltestelleJsonObject.put("lat", lat);
				
				haltestellenJsonArray.put(haltestelleJsonObject);
			}
			NVBWLogger.info("Anzahl Suchtreffer: " + anzahl);
			selectSucheRS.close();
			selectSucheStmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException::: " + e.toString());
		}
		response.getWriter().append(haltestellenJsonArray.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setStatus(METHOD_NOT_ALLOWED);
		response.getWriter().append("post Request will not be supported.");
	}

}
