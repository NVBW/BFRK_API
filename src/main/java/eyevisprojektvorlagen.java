
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
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
@WebServlet(name = "eyevisprojektvorlagen", 
			urlPatterns = {"/eyevisprojektvorlagen/*"}
		)
public class eyevisprojektvorlagen extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(eyevisprojektvorlagen.class);
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public eyevisprojektvorlagen() {
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

		Enumeration<String> enums = request.getParameterNames();
		while(enums.hasMoreElements()) {
			String element = enums.nextElement();
			LOG.info("Parametername ===" + element + "===");
		}

		String paramOevart = "";
		if(request.getParameter("oevart") != null) {
			System.out.println("url-Parameter oevart vorhanden ===" + request.getParameter("oevart") + "===");
			String tempOevart = request.getParameter("oevart");
			if((tempOevart == null) || tempOevart.isEmpty()) {
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Parameter oevart ist leer, der Parameter muss mit O|S gesetzt werden");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			if(	!tempOevart.equals("S") &&
				!tempOevart.equals("O")) {
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Parameter oevart ist falsch gesetzt, zulässig sind nur O|S");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			paramOevart = tempOevart;
		} else {
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Parameter oevart ist leer, der Parameter muss mit O|S gesetzt werden");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		String selectEyevisvorlagenSql = "SELECT vorlage FROM eyevisprojektvorlagen WHERE oevart = ? "
			+ "ORDER BY vorlage";


		PreparedStatement selectEyevisvorlagenStmt;
		try {
			selectEyevisvorlagenStmt = bfrkConn.prepareStatement(selectEyevisvorlagenSql);
			selectEyevisvorlagenStmt.setString(1, paramOevart);
			System.out.println("Haltestelle query: " + selectEyevisvorlagenStmt.toString() + "===");

			ResultSet selectEyevisvorlagenRS = selectEyevisvorlagenStmt.executeQuery();

			JSONArray vorlagenArray = new JSONArray();

			String vorlage = "";
			while(selectEyevisvorlagenRS.next()) {
				vorlage = selectEyevisvorlagenRS.getString("vorlage");

				if((vorlage != null) && !vorlage.isEmpty())
					vorlagenArray.put(vorlage);
				LOG.info("gefundene Vorlage ===" + vorlage + "===");
			}
			ergebnisJsonObject.put("vorlagen", vorlagenArray);

		} catch (SQLException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei SQLException " + e1.toString());
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
