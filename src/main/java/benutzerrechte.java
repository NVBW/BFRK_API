
import java.io.IOException;
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

import org.json.JSONObject;

import de.nvbw.base.Applicationconfiguration;
import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.DBVerbindung;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "benutzerrechte", 
			urlPatterns = {"/benutzerrechte/*"}
		)
public class benutzerrechte extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(benutzerrechte.class);
	private static Applicationconfiguration configuration = new Applicationconfiguration();
    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public benutzerrechte() {
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
		JSONObject ergebnisJsonObject = new JSONObject();

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				LOG.warning("keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					LOG.severe("es konnte keine DB-Verbindung herstellt werden, in benutzerrechte doGet");
					ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren");
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		} catch (Exception e1) {
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unerwarteter Fehler aufgetreten, bitte Administrator informieren");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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

		LOG.info("Request Header accesstoken vorhanden ===" + request.getHeader("accesstoken") + "===");
		accesstoken = request.getHeader("accesstoken");
		if(accesstoken.isEmpty()) {
			ergebnisJsonObject = new JSONObject();
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
			LOG.info("Haltestelle query: " + selectBenutzerrechteStmt.toString() + "===");

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
				ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Access Token ist nicht gültig");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
		} catch (SQLException e1) {
			LOG.severe("SQLException::: " + e1.toString());
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e1) {
			LOG.severe("IOException " + e1.toString());
			ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unerwarteter Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		response.getWriter().append(ergebnisJsonObject.toString());
	}


	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		LOG.info("Request /benutzerrechte, doPost ...");

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /benutzerrechte ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		return;
	}
}
