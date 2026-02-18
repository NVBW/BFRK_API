

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.nvbw.base.NVBWLogger;
import org.json.JSONObject;

import de.nvbw.base.Applicationconfiguration;
import de.nvbw.bfrk.ExportNachEYEvis;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "eyevisprojektdaten", 
			urlPatterns = {"/eyevisprojektdaten/*"}
		)
public class eyevisprojektdaten extends HttpServlet {
	private static final long serialVersionUID = 1L;

	static DateFormat datetime_filesystem_formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");

	private static final Logger LOG = NVBWLogger.getLogger(eyevisprojektdaten.class);
	private static Applicationconfiguration configuration = new Applicationconfiguration();

    /**
     * @see HttpServlet#HttpServlet()
     */
    public eyevisprojektdaten() {
        super();
    }

    /**
     * initialization on servlet startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String umgebung = configuration.servername;
		//TODO die folgende Url sollte ggfs. Staging-relevant gesetzt werden
		String webserverStatischeAusgabebasisurl = "https://bfrk-kat-api.efa-bw.de/eyevisprojektdaten";


		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject resultObjectJson = new JSONObject();

		String paramOevart = "";
		String paramDhidListe = "";
		String paramObjektarten = "%";
		String paramDatenlieferant = "";
		String paramVorlage = "";

		if(request.getParameter("oevart") != null) {
			LOG.info("url-Parameter oevart vorhanden ===" + request.getParameter("oevart") + "===");
			paramOevart = URLDecoder.decode(request.getParameter("oevart").toUpperCase(),"UTF-8");
			if(		!paramOevart.equals("S")
				&&	!paramOevart.equals("O")) {
				JSONObject errorObjektJson = new JSONObject();
				errorObjektJson.put("subject", "Request parameter oevart hat falschen Wert");
				errorObjektJson.put("message", "Paramaterwert unzulässig, mögliche Werte sind S|O, Wert war "
					+ request.getParameter("oevart").toUpperCase());
				errorObjektJson.put("messageId", 9994712);
				resultObjectJson.put("error", errorObjektJson);
				response.getWriter().append(resultObjectJson.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			LOG.info("in variable paramOevart ===" + paramOevart + "===");
		} else {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request Pflicht-Parameter oevart fehlt");
			errorObjektJson.put("message", "Paramater oevart muss gesetzt werden, mögliche Werte sind S|O");
			errorObjektJson.put("messageId", 9994712);
			resultObjectJson.put("error", errorObjektJson);
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if(request.getParameter("dhids") != null) {
			LOG.info("url-Parameter dhids vorhanden ===" + request.getParameter("dhids") + "===");
			paramDhidListe = URLDecoder.decode(request.getParameter("dhids"), StandardCharsets.UTF_8);
			LOG.info("in variable paramDHIDListe ===" + paramDhidListe + "===");
		} else {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request Pflicht-Parameter dhid fehlt");
			errorObjektJson.put("message", "Paramater dhid muss gesetzt werden.");
			errorObjektJson.put("messageId", 9994711);
			resultObjectJson.put("error", errorObjektJson);
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if(request.getParameter("datenlieferant") != null) {
			LOG.info("url-Parameter datenlieferant vorhanden ===" + request.getParameter("datenlieferant") + "===");
			paramDatenlieferant = URLDecoder.decode(request.getParameter("datenlieferant"),"UTF-8");
			LOG.info("in variable paramDatenlieferant ===" + paramDatenlieferant + "===");

			if(	paramDatenlieferant.equals("bodo")
				|| paramDatenlieferant.equals("CalwLK")
				|| paramDatenlieferant.equals("ding")
				|| paramDatenlieferant.equals("Enzkreis")
				|| paramDatenlieferant.equals("FreudenstadtLK")
				|| paramDatenlieferant.equals("HeidenheimLK")
				|| paramDatenlieferant.equals("HeilbronnLK")
				|| paramDatenlieferant.equals("Hohenlohekreis")
				|| paramDatenlieferant.equals("KonstanzLK")
				|| paramDatenlieferant.equals("Ortenaukreis")
				|| paramDatenlieferant.equals("Ostalbkreis")
				|| paramDatenlieferant.equals("PforzheimSK")
				|| paramDatenlieferant.equals("ReutlingenLK")
				|| paramDatenlieferant.equals("RottweilLK")
				|| paramDatenlieferant.equals("SigmaringenLK")
				|| paramDatenlieferant.equals("TübingenLK")
				|| paramDatenlieferant.equals("ZRF")
				|| paramDatenlieferant.equals("SPNV")) {
			} else {
				JSONObject errorObjektJson = new JSONObject();
				errorObjektJson.put("subject", "Request Pflicht-Parameter datenlieferant hat ungültigen Wert");
				errorObjektJson.put("message", "Parameter datenlieferant fehlerhaft");
				errorObjektJson.put("messageId", 9994731);
				resultObjectJson.put("error", errorObjektJson);
				response.getWriter().append(resultObjectJson.toString());
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		} else {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request Pflicht-Parameter datenlieferant fehlt");
			errorObjektJson.put("message", "Paramater datenlieferant muss gesetzt werden.");
			errorObjektJson.put("messageId", 9994711);
			resultObjectJson.put("error", errorObjektJson);
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		if(request.getParameter("objektart") != null) {
			LOG.info("url-Parameter objektart vorhanden ===" + request.getParameter("objektart") + "===");
			paramObjektarten = URLDecoder.decode(request.getParameter("objektart"), StandardCharsets.UTF_8);
			LOG.info("in variable paramOjektarten ===" + paramObjektarten + "===");
		}

		if(request.getParameter("eyevisvorlage") != null) {
			LOG.info("url-Parameter eyevivorlage vorhanden ===" + request.getParameter("eyevisvorlage") + "===");
			paramVorlage = URLDecoder.decode(request.getParameter("eyevisvorlage"), StandardCharsets.UTF_8);
			LOG.info("in variable paramVorlage ===" + paramVorlage+ "===");
		} else {
			JSONObject errorObjektJson = new JSONObject();
			errorObjektJson.put("subject", "Request Pflicht-Parameter eyevisvorlage fehlt");
			errorObjektJson.put("message", "Parameter eyevisvorlage fehlt");
			errorObjektJson.put("messageId", 9994732);
			resultObjectJson.put("error", errorObjektJson);
			response.getWriter().append(resultObjectJson.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String paramAuftragsverzeichnis  = "";
		String paramAusgabepfad = "";
		String paramEingangspfad  = "";
		if(umgebung.equals("nvbw_nb_sei")) {
			paramAuftragsverzeichnis = "C:\\Users\\sei\\Projekte\\PMDatenbank\\Auftrag";
			paramEingangspfad = paramAuftragsverzeichnis + File.separator + "Eingang";
			paramAusgabepfad = paramAuftragsverzeichnis + File.separator + "Ausgang";
		} else {
			paramAuftragsverzeichnis = configuration.application_homedir + File.separator + "eyevisprojektdaten";
			paramEingangspfad = paramAuftragsverzeichnis + File.separator + "Eingang";
			paramAusgabepfad = paramAuftragsverzeichnis + File.separator + "Ausgang";
		}
		long zufallszahl = (long) (Math.random() * 1000);
		Date jetzt = new Date();
		String datetime = datetime_filesystem_formatter.format(jetzt);
		String ausgabedateiprefix = datetime + "-" + zufallszahl;
		String auftragspfadundname = paramEingangspfad + File.separator 
			+ datetime + "-" + zufallszahl + ".txt";
		String auftragstempname = auftragspfadundname + "_temp";

		
		String args[] = new String[16];
		int argsindex = 0;
		args[argsindex++] = "-dhids";
		args[argsindex++] = paramDhidListe;
		args[argsindex++] = "-oevart";
		args[argsindex++] = paramOevart;
		args[argsindex++] = "-objektarten";
		args[argsindex++] = paramObjektarten;
		args[argsindex++] = "-ausgabedateiprefix";
		args[argsindex++] = ausgabedateiprefix;
		args[argsindex++] = "-bilderkopieren";
		args[argsindex++] = "ja";
		args[argsindex++] = "-bilderzielverzeichnis";
		args[argsindex++] = paramAusgabepfad;
		args[argsindex++] = "-rootdir";
		args[argsindex++] = paramAuftragsverzeichnis;
		args[argsindex++] = "-eyevisvorlage";
		args[argsindex++] = paramVorlage;
		LOG.info("Die Programmaufrufparameter sind ...");
		for(int argindex = 0; argindex < args.length; argindex++) {
			LOG.info("args[" + argindex + "] ===" + args[argindex] + "===");
		}
		LOG.info("vor Aufruf ExportNachEYEvis.main ...");
//TODO normalen Aufruf mit x Methodenparametern umsetzen
		int returncode = ExportNachEYEvis.execute(args);
		LOG.info("nach Aufruf ExportNachEYEvis.main, returncode: " + returncode);

		if(returncode != 0) {
			LOG.warning("Der Returncode von ExportNachEyevis.main war nicht 0, also fehlerhaft, deshalb jetzt Abbruch");
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Die EYEvis-Projekterstellung hat kein Ergebnis gebracht.");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		LOG.info("jetzt suchen im Ausgabeverzeichnis " + paramAusgabepfad + " ...");

		String gefdatei = paramAusgabepfad + File.separator + ausgabedateiprefix + "_auftragsausgabe.txt";
		
		File outputdateiHandle = new File(gefdatei);
		
			// wenn vom Programm eine Ausgabe-Metadatei herauskam, in der DB speichern
		if(outputdateiHandle.exists()) {

			try {
				String csvdateiname = "";
				String zipdateiname = "";
				int csvdateigroesse = 0;
				int zipdateigroesse = 0;
				BufferedReader  dateireader = new BufferedReader(new InputStreamReader(
					new FileInputStream(outputdateiHandle), 
					StandardCharsets.UTF_8));

				String line = "";
				int abpos = 0;
				while ((line = dateireader.readLine()) != null) {
					if(line.contains("="))
						abpos = line.indexOf("=") + 1;
					if(line.startsWith("csvdatei="))
						csvdateiname = line.substring(abpos);
					if(line.startsWith("csvdateigroesse="))
						csvdateigroesse = Integer.parseInt(line.substring(abpos));
					if(line.startsWith("zipdatei="))
						zipdateiname = line.substring(abpos);
					if(line.startsWith("zipdateigroesse="))
						zipdateigroesse = Integer.parseInt(line.substring(abpos));
				}
				dateireader.close();
				LOG.info("csvdateiname    ===" + csvdateiname + "===");
				LOG.info("csvdateigroesse ===" + csvdateigroesse + "===");
				LOG.info("zipdateiname    ===" + zipdateiname + "===");
				LOG.info("zipdateigroesse ===" + zipdateigroesse + "===");

				resultObjectJson.put("zipdateilink", webserverStatischeAusgabebasisurl + "/" + zipdateiname);
				resultObjectJson.put("zipdateigroesse", zipdateigroesse);
				resultObjectJson.put("csvdateilink", webserverStatischeAusgabebasisurl + "/" + csvdateiname);
				resultObjectJson.put("csvdateigroesse", csvdateigroesse);
				response.getWriter().append(resultObjectJson.toString());
				response.setStatus(HttpServletResponse.SC_OK);
				return;

			} catch (IOException ioe) {
				LOG.warning("Fehler bei Datei lesen " + gefdatei);
				LOG.warning(ioe.toString());
				String fehlertext = "Die EYEvis-Projekterstellsdatei ist nicht lesbar";
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
			
		} else {
			LOG.warning("Es wurde keine Ausgabedatei "
				+ "von EYEvis-Projektdateierzeugung erstellt, daher ABBRUCH");
			String fehlertext = "Die Grapherzeugung ist fehlgeschlagen, Grund unbekannt";
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /eyevisprojektdaten ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

}
