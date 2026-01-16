
import java.io.IOException;
import java.net.URLDecoder;
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

import de.nvbw.bfrk.util.DBVerbindung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "gemeinde", 
			urlPatterns = {"/gemeinde/*"}
		)
public class gemeinde extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    private static Connection bfrkConn = null;

    public static int SC_NOTFOUND = 404;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public gemeinde() {
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

		String paramKreisid= "%";
		String paramSuche = "%";
		if(request.getParameter("kreisid") != null) {
			System.out.println("url-Parameter kreisid vorhanden ===" + request.getParameter("kreisid"));
			paramKreisid = URLDecoder.decode(request.getParameter("kreisid"),"UTF-8");
		}
		if(request.getParameter("suche") != null) {
			System.out.println("url-Parameter suche vorhanden ===" + request.getParameter("suche"));
			paramSuche = URLDecoder.decode(request.getParameter("suche"),"UTF-8");
			paramSuche = "%" + paramSuche + "%";
		}
	
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectGemeindeSql = "SELECT DISTINCT ON (gemeindeschluessel, gemeinde) "
			+ "kreisschluessel, gemeindeschluessel, gemeinde "
			+ "FROM haltestellenliste WHERE "
			+ "kreisschluessel like ? AND "
			+ "gemeinde ilike ? "
			+ "ORDER BY gemeinde;";

		JSONArray gemeindenJsonArray = new JSONArray();
		
		PreparedStatement selectGemeindeStmt;
		try {
			selectGemeindeStmt = bfrkConn.prepareStatement(selectGemeindeSql);
			selectGemeindeStmt.setString(1, paramKreisid);
			selectGemeindeStmt.setString(2, paramSuche);
			System.out.println("Gemeinde query: " + selectGemeindeStmt.toString() + "===");

			ResultSet selectGemeindeRS = selectGemeindeStmt.executeQuery();

			int anzahldatensaetze = 0;
			while(selectGemeindeRS.next()) {
				anzahldatensaetze++;
				JSONObject gemeindeJsonObject = new JSONObject();

				String kreisschluessel = selectGemeindeRS.getString("kreisschluessel");
				gemeindeJsonObject.put("kreisid", kreisschluessel);
				String gemeindeschluessel = selectGemeindeRS.getString("gemeindeschluessel");
				gemeindeJsonObject.put("gemeindeschluessel", gemeindeschluessel);
				String gemeinde = selectGemeindeRS.getString("gemeinde");
				gemeindeJsonObject.put("gemeinde", gemeinde);
				
				gemeindenJsonArray.put(gemeindeJsonObject);
			}
			selectGemeindeRS.close();
			selectGemeindeStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException::: " + e.toString());
		}
		response.getWriter().append(gemeindenJsonArray.toString());

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
