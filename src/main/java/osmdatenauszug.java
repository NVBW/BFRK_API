
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;




/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "osmdatenauszug", 
			urlPatterns = {"/osmdatenauszug/*"}
		)
public class osmdatenauszug extends HttpServlet {


	private static final long serialVersionUID = 1L;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public osmdatenauszug() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * initialization on servlett startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		DateFormat datetime_dateistempel_formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");

		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		response.setHeader("Access-Control-Allow-Headers", "*");

		
			// ==================== Parameter auslesen =========================
		double links = 0;
		double rechts = 0;
		double oben = 0;
		double unten = 0;
		String osmextension = "osm";
		String contenttype = "text/xml";
		String characterencoding = "UTF-8";
		if(request.getHeader("accept") != null) {
			System.out.println("Request Header accept vorhanden ===" + request.getHeader("accept") + "===");
			if(request.getHeader("accept").contains("application/x-protobuf")) {
				osmextension = "pbf";
				contenttype = "application/x-protobuf";
				characterencoding = "";
			}
		}
		NVBWLogger.info("gesetzte OSM-Extension: " + osmextension);
		response.setContentType(contenttype);
		if(!characterencoding.isEmpty())
			response.setCharacterEncoding(characterencoding);

		try {
				// Abruf nur mit accesstoken erlaubt, sonst Fehler-Abbruch
/*			String accesstoken = "";
			if(request.getHeader("accesstoken") == null) {
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Pflicht Header accesstoken fehlt");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			} else {
			System.out.println("Request Header accesstoken vorhanden ===" + request.getHeader("accesstoken") + "===");
			accesstoken = request.getHeader("accesstoken");
			if(accesstoken.isEmpty()) {
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen, weil Header accesstoken leer ist");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
*/
			if(request.getParameter("links") != null) {
				System.out.println("url-Parameter links vorhanden ===" + request.getParameter("links"));
				links = Double.parseDouble(request.getParameter("links"));
			}
			if(request.getParameter("rechts") != null) {
				System.out.println("url-Parameter rechts vorhanden ===" + request.getParameter("rechts"));
				rechts = Double.parseDouble(request.getParameter("rechts"));
			}
			if(request.getParameter("oben") != null) {
				System.out.println("url-Parameter oben vorhanden ===" + request.getParameter("oben"));
				oben = Double.parseDouble(request.getParameter("oben"));
			}
			if(request.getParameter("unten") != null) {
				System.out.println("url-Parameter unten vorhanden ===" + request.getParameter("unten"));
				unten = Double.parseDouble(request.getParameter("unten"));
			}
		} catch(NumberFormatException e) {
			NVBWLogger.severe("Fehler beim Versuch, die bbox-Grenzen numerisch zu parsen, ABBRUCH");
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "bbox-Angaben nicht numerisch");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Date jetzt = new Date();
		String filename = datetime_dateistempel_formatter.format(jetzt) + "." + osmextension;
		String workingdir = "/home/NVBWAdmin/tomcat-deployment/bfrk_api_home/openstreetmap";
		File workingdirHandle = new File(workingdir);
		String programm = "osmiumextract.sh";
		String parameter = "" + links + " " + unten + " " + rechts + " " + oben + " " + filename;
		try {
			NVBWLogger.info("Prozessaufruf: " + programm + " " + parameter + "...");
			ProcessBuilder processbuilded = new ProcessBuilder(workingdir + File.separator + programm, 
				"" + links, "" + unten, "" + rechts, "" + oben, filename);
			processbuilded.directory(workingdirHandle);
			processbuilded.redirectOutput(new File(workingdir + File.separator + "process_output.log"));
			processbuilded.redirectError(new File(workingdir + File.separator + "process_error.log"));
			NVBWLogger.info("Prozess wird gestartet ...");
			Process processrunning = processbuilded.start();
			int returncode = processrunning.waitFor();
			NVBWLogger.info("Prozess hat geendet");
			NVBWLogger.info("Aufruf osmium ergab returncode: " + returncode);
		} catch (IOException e) {
			NVBWLogger.severe("OsmFileReader/createPlanetAuszug: IOException aufgetreten, Details: " + e.toString());
			e.printStackTrace();
		} catch(Exception e) {
			NVBWLogger.severe("osmdatenauszug, Exception aufgetreten. Details: " + e.toString());
			return;
		}

		File outputdateiHandle = new File("/home/NVBWAdmin/tomcat-deployment/bfrk_api_home/openstreetmap/" + filename);
		

			// wenn von Osmium KEINE extract-Datei herauskam, Fehlermeldung und Ende
		if(!outputdateiHandle.exists()) {
			NVBWLogger.warning("Es wurde keine OSM-Dateiauszug "
				+ "erstellt, daher ABBRUCH");
			String fehlertext = "Der OSM-Dateiauszug ist fehlgeschlagen, Grund unbekannt";
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
			
		response.setContentLengthLong(outputdateiHandle.length());
		response.setHeader("Content-Disposition", "attachment; filename=\"auszug.osm.pbf\"");

	    // Datei auslesen und in Response schreiben
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(outputdateiHandle));
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[2 * 8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

		
		response.setStatus(HttpServletResponse.SC_OK);
		return;
	}
}
