
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;

import de.nvbw.bfrk.util.DBVerbindung;


/**
 * Servlet implementation class projekte
 */
@WebServlet("/importdateien")
public class importdateien extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    private static Connection bfrkConn = null;
    
    public static int SC_NOTFOUND = 404;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public importdateien() {
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
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectImportdateienSql = "SELECT dateiname FROM importdatei;";
		
		Date jetzt = new Date();
		SimpleDateFormat formatterde = new SimpleDateFormat("dd.mm.YYYY HH:mm:ss");
		//response.getWriter().append("Served at: ").append(request.getContextPath())
		//	.append(": ");

		JSONArray dateienJsonArray = new JSONArray();
		
		PreparedStatement selectImportdateienStmt;
		try {
			selectImportdateienStmt = bfrkConn.prepareStatement(selectImportdateienSql);
			System.out.println("Importdateien query: " + selectImportdateienStmt.toString() + "===");

			ResultSet selectImportdateienRS = selectImportdateienStmt.executeQuery();

			String importdatei = "";
			int anzahldatensaetze = 0;
			while(selectImportdateienRS.next()) {
				anzahldatensaetze++;
				importdatei = selectImportdateienRS.getString("dateiname");
				System.out.println("importdatei ===" + importdatei + "===");
				//response.getWriter().append(importdatei + ",");
				dateienJsonArray.put(importdatei);
			}
			selectImportdateienRS.close();
			selectImportdateienStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("SQLException::: " + e.toString());
		}
		//JSONObject outputJson = new JSONObject();
		//outputJson.put("Anzahl", anzahl);
		//outputJson.put("Importdateien", dateienJsonArray);
		//response.getWriter().append(outputJson.toString());
		response.getWriter().append(dateienJsonArray.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
