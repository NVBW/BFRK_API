
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.base.Applicationconfiguration;
import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.DBVerbindung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "kreise", 
			urlPatterns = {"/kreise/*"}
		)
public class kreise extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    private static Connection bfrkConn = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public kreise() {
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

		String paramOevart = "S";
		String objektart = "Bahnhof";

		if(request.getParameter("oevart") != null) {
			System.out.println("url-Parameter oevart vorhanden ===" + request.getParameter("oevart"));
		}
		if(request.getParameter("oevart") != null) {
			System.out.println("url-Parameter oevart vorhanden ===" + request.getParameter("oevart") + "===");
			paramOevart = request.getParameter("oevart").toUpperCase();
			if(paramOevart.equals("S")) {
				objektart = "Bahnhof";
			} else if(paramOevart.equals("O")) {
				objektart = "Haltestelle";
			}
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectKreiseSql = "SELECT k.kreisschluessel, k.name, count(*) AS anzahl FROM "
			+ "kreis AS k JOIN objekt AS o ON o.kreisschluessel = k.kreisschluessel WHERE "
			+ "oevart = ? AND objektart = ? GROUP BY k.kreisschluessel, k.name order by k.name;";

		JSONArray kreiseJsonArray = new JSONArray();
		
		PreparedStatement selectKreiseStmt;
		try {
			selectKreiseStmt = bfrkConn.prepareStatement(selectKreiseSql);
			int stmtindex = 1;
			selectKreiseStmt.setString(stmtindex++, paramOevart);
			selectKreiseStmt.setString(stmtindex++, objektart);
			System.out.println("Kreise query: " + selectKreiseStmt.toString() + "===");

			ResultSet selectKreiseRS = selectKreiseStmt.executeQuery();

			int anzahl = 0;
			while(selectKreiseRS.next()) {
				anzahl++;

				String kreisschluessel = selectKreiseRS.getString("kreisschluessel");
				String name = selectKreiseRS.getString("name");
				int anzahlobjekte = selectKreiseRS.getInt("anzahl");

				JSONObject kreiseJsonObject = new JSONObject();

				kreiseJsonObject.put("kreisschluessel", kreisschluessel);
				kreiseJsonObject.put("name", name);
				kreiseJsonObject.put("anzahl", anzahlobjekte);
				
				kreiseJsonArray.put(kreiseJsonObject);
			}
			NVBWLogger.info("Anzahl Kreise: " + anzahl);
			selectKreiseRS.close();
			selectKreiseStmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException::: " + e.toString());
		}
		response.getWriter().append(kreiseJsonArray.toString());

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
