
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.nvbw.base.Applicationconfiguration;
import org.json.JSONArray;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.base.BFRKFeld;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;
import de.nvbw.bfrk.util.ReaderBase;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "stopmodelle", 
			urlPatterns = {"/stopmodelle/*"}
		)
public class stopmodelle extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static DateFormat datetime_rfc3339_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	private static Applicationconfiguration configuration = new Applicationconfiguration();
    private static Connection bfrkConn = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public stopmodelle() {
        super();
    }

    /**
     * initialization on servlett startup
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

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONArray objektarray = new JSONArray();
		
		String dhid = "";
		if(request.getParameter("dhid") != null) {
			System.out.println("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			dhid = request.getParameter("dhid");
		}
		System.out.println("Parameter dhid: " + dhid);

		if(!dhid.isEmpty()) {

			String selectModellSql = "SELECT dhid, release, mayorversion, minorversion, "
				+ "benutzer, kommentar, content, zeitstempel "
				+ "FROM objektmodell "
				+ "WHERE dhid = ? ORDER BY release DESC, mayorversion DESC, minorversion DESC;";

			PreparedStatement selectModellStmt;
			try {
				selectModellStmt = bfrkConn.prepareStatement(selectModellSql);
				selectModellStmt.setString(1, dhid);
				NVBWLogger.info("Objektmodell query: " + selectModellStmt.toString() + "===");

				ResultSet selectModellRS = selectModellStmt.executeQuery();


				while(selectModellRS.next()) {
					JSONObject modellJsonObject = new JSONObject();

					String dbdhid = selectModellRS.getString("dhid");
					int release = selectModellRS.getInt("release");
					int mayorversion = selectModellRS.getInt("mayorversion");
					int minorversion = selectModellRS.getInt("minorversion");
					String benutzer = selectModellRS.getString("benutzer");
					String kommentar = selectModellRS.getString("kommentar");
					String content = selectModellRS.getString("content");
					Date zeitstempel = selectModellRS.getTimestamp("zeitstempel");

					if(dbdhid != null)
						modellJsonObject.put("dhid",  dbdhid);
					modellJsonObject.put("release",  release);
					modellJsonObject.put("mayorversion",  mayorversion);
					modellJsonObject.put("minorversion",  minorversion);
					if(benutzer != null)
						modellJsonObject.put("benutzer",  benutzer);
					if(kommentar != null)
						modellJsonObject.put("kommentar",  kommentar);
					if(content != null)
						modellJsonObject.put("modelldateilaenge",  content.length());
					if(zeitstempel != null)
						modellJsonObject.put("zeitstempel",  datetime_rfc3339_formatter.format(zeitstempel));

					objektarray.put(modellJsonObject);
				}
				selectModellRS.close();
				selectModellStmt.close();

			} catch (SQLException e) {
				NVBWLogger.severe("SQLException::: " + e.toString());
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten, bitte Administrator informieren.");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}

		response.getWriter().append(objektarray.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /stopmodelle ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return;
	}
}
