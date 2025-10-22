
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import de.nvbw.base.BFRKApiApplicationconfiguration;
import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.bfrk.util.OpenStreetMap;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "benutzerrechte", 
			urlPatterns = {"/benutzerrechte/*"}
		)
public class benutzerrechte extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static BFRKApiApplicationconfiguration bfrkapiconfiguration = null;
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public benutzerrechte() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * initialization on servlett startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
		bfrkapiconfiguration = new BFRKApiApplicationconfiguration();
    	bfrkConn = DBVerbindung.getDBVerbindung();
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject ergebnisJsonObject = new JSONObject();

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				System.out.println("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
					response.setCharacterEncoding("UTF-8");
					response.getWriter().append("Datenbankverbindung verloren, bitte nochmal versuchen");
					return;
				}
			}
		} catch (Exception e1) {
			response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().append("Datenbankverbindung verloren, bitte nochmal versuchen");
			return;
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		response.setHeader("Access-Control-Allow-Headers", "*");

		if(request.getHeader("accesstoken") == null) {
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Pflicht Header accesstoken fehlt");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String accesstoken = "";

		System.out.println("Request Header accesstoken vorhanden ===" + request.getHeader("accesstoken") + "===");
		accesstoken = request.getHeader("accesstoken");
		if(accesstoken.isEmpty()) {
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen, weil Header accesstoken leer ist");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}


		String selectBenutzerrechteSql = "SELECT name, modellspeichern, objekteaendern "
			+ "FROM benutzer WHERE accesstoken = ?";


		PreparedStatement selectBenutzerrechteStmt;
		try {
			selectBenutzerrechteStmt = bfrkConn.prepareStatement(selectBenutzerrechteSql);
			selectBenutzerrechteStmt.setString(1, accesstoken);
			System.out.println("Haltestelle query: " + selectBenutzerrechteStmt.toString() + "===");

			ResultSet selectBenutzerrechteRS = selectBenutzerrechteStmt.executeQuery();

			
			if(selectBenutzerrechteRS.next()) {
				boolean modellspeichern = selectBenutzerrechteRS.getBoolean("modellspeichern");
				boolean objekteaendern = selectBenutzerrechteRS.getBoolean("objekteaendern");
				String username = selectBenutzerrechteRS.getString("name");
				if((username != null) && !username.isEmpty())
					ergebnisJsonObject.put("name", username);
				ergebnisJsonObject.put("modellspeichern", modellspeichern);
				ergebnisJsonObject.put("objekteaendern", objekteaendern);
			} else {
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Access Token ist nicht gültig");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		} catch (SQLException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei SQLException " + e1.toString());
			return;
		} catch (IOException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei IOException " + e1.toString());
			return;
		}

		response.getWriter().append(ergebnisJsonObject.toString());
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		System.out.println("Request /benutzerrechte, doPost ...");

		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return;
	}
}
