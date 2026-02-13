
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

import de.nvbw.base.Applicationconfiguration;
import org.json.JSONObject;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "bahnsteig", 
			urlPatterns = {"/bahnsteig/*"}
		)
public class bahnsteig extends HttpServlet {
	private static final long serialVersionUID = 1L;

    private static Connection bfrkConn = null;
	private static Applicationconfiguration configuration = new Applicationconfiguration();


    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public bahnsteig() {
        super();
        // TODO Auto-generated constructor stub
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

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				NVBWLogger.severe("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					response.getWriter().append("FEHLER: keine DB-Verbindung offen");
					return;
				}
			}
		} catch (SQLException e) {
			NVBWLogger.severe("FEHLER: keine DB-Verbindung offen, bei SQLException " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung möglich beim Aufruf /bahnsteig, bitte Administrator informieren: "
					+ e.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e) {
			NVBWLogger.severe("FEHLER: keine DB-Verbindung offen, bei IOException " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unerwarteter Fehler aufgetreten beim Aufruf /bahnsteig, bitte Administrator informieren: "
					+ e.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		long paramObjektid = 0;
		String paramDhid = "";
		if(request.getParameter("dhid") != null) {
			NVBWLogger.info("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramDhid = request.getParameter("dhid");
		} else {
			NVBWLogger.info("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			NVBWLogger.info("requesturi ===" + requesturi + "===");
			if(requesturi.contains("/bahnsteig")) {
				int startpos = requesturi.indexOf("/bahnsteig");
				NVBWLogger.info("startpos #1: " + startpos);
				if(requesturi.indexOf("/",startpos + 1) != -1) {
					paramObjektid = Long.parseLong(requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1));
					NVBWLogger.info("Versuch, Objektid zu extrahieren ===" + paramObjektid + "===");
				}
			}
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		if((paramObjektid == 0) && paramDhid.isEmpty()) {
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Angabe objektid oder Parameter dhid fehlen");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		

//TODO Merkmalkorrekturen einarbeiten
		String selectHaltestelleSql = "";
		if(paramObjektid != 0)
			selectHaltestelleSql = "SELECT o.dhid, merkmal.name, merkmal.wert, typ FROM merkmal "
				+ "JOIN objekt AS o ON objekt.id = merkmal.objekt_id "
				+ "WHERE objekt_id = ?;";
		else
			selectHaltestelleSql = "SELECT o.dhid, merkmal.name, merkmal.wert, typ FROM merkmal "
				+ "JOIN objekt AS o ON o.id = merkmal.objekt_id "
				+ "WHERE objektart = 'Bahnsteig' AND dhid = ?;";

		JSONObject merkmaleJsonObject = new JSONObject();
		//merkmaleJsonObject.put("objektid", paramObjektid);		// evtl. in API in Response weglassen

		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			if(paramObjektid != 0)
				selectHaltestelleStmt.setLong(1, paramObjektid);
			else
				selectHaltestelleStmt.setString(1, paramDhid);
			NVBWLogger.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			int anzahldatensaetze = 0;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				String dhid = selectMerkmaleRS.getString("dhid");
				String name = selectMerkmaleRS.getString("name");
				String wert = selectMerkmaleRS.getString("wert");
				String typ = selectMerkmaleRS.getString("typ");
				if(name.equals("STG_Abfallbehaelter"))
					merkmaleJsonObject.put("abfallbehaelter", wert.equals("true"));
				else if(name.equals("STG_Ansagen_Vorhanden_D1150"))
					merkmaleJsonObject.put("ansage", wert.equals("true"));
				else if(name.equals("STG_Bahnsteig_Abschnitt_Lon"))
					merkmaleJsonObject.put("abschnitt1_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt_Lat"))
					merkmaleJsonObject.put("abschnitt1_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt2_Lon"))
					merkmaleJsonObject.put("abschnitt2_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt2_Lat"))
					merkmaleJsonObject.put("abschnitt2_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt3_Lon"))
					merkmaleJsonObject.put("abschnitt3_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt3_Lat"))
					merkmaleJsonObject.put("abschnitt3_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt4_Lon"))
					merkmaleJsonObject.put("abschnitt4_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt4_Lat"))
					merkmaleJsonObject.put("abschnitt4_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt5_Lon"))
					merkmaleJsonObject.put("abschnitt5_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt5_Lat"))
					merkmaleJsonObject.put("abschnitt5_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt6_Lon"))
					merkmaleJsonObject.put("abschnitt6_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt6_Lat"))
					merkmaleJsonObject.put("abschnitt6_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt7_Lon"))
					merkmaleJsonObject.put("abschnitt7_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt7_Lat"))
					merkmaleJsonObject.put("abschnitt7_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt8_Lon"))
					merkmaleJsonObject.put("abschnitt8_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Abschnitt8_Lat"))
					merkmaleJsonObject.put("abschnitt8_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt_Lon"))
					merkmaleJsonObject.put("haltepunkt1_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt_Lat"))
					merkmaleJsonObject.put("haltepunkt1_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt2_Lon"))
					merkmaleJsonObject.put("haltepunkt2_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt2_Lat"))
					merkmaleJsonObject.put("haltepunkt2_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt3_Lon"))
					merkmaleJsonObject.put("haltepunkt3_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt3_Lat"))
					merkmaleJsonObject.put("haltepunkt3_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt4_Lon"))
					merkmaleJsonObject.put("haltepunkt4_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Haltepunkt4_Lat"))
					merkmaleJsonObject.put("haltepunkt4_lat", Double.parseDouble(wert));
				
				else if(name.equals("STG_Bahnsteig_Sitzplaetz_Summe"))
					merkmaleJsonObject.put("summesitzplaetze", (int) Double.parseDouble(wert));
				else if(name.equals("STG_Bahnsteig_Uhr_vorhanden"))
					merkmaleJsonObject.put("uhr", wert.equals("true"));
				else if(name.equals("STG_Beleuchtung"))
					merkmaleJsonObject.put("beleuchtung", wert.equals("true"));
				else if(name.equals("STG_Bodenbelag_Art"))
					merkmaleJsonObject.put("bodenbelag", wert);
				else if(name.equals("STG_Bodenbelag_Unbefestigt_Vorhanden_D2050"))
					merkmaleJsonObject.put("bodenbelag_unbefestigt", wert.equals("true"));
				else if(name.equals("STG_Bodenindikator_Vorhanden_D2070"))
					merkmaleJsonObject.put("bodenindikator", wert.equals("true"));
				else if(name.equals("STG_Bodenindikator_Einstiegsbereich"))
					merkmaleJsonObject.put("bodenindikator_einstiegsbereich", wert.equals("true"));
				else if(name.equals("STG_Bodenindikator_EinstiegUndAuffind_D2071"))
					merkmaleJsonObject.put("bodenindikator_einstiegauffind", wert.equals("true"));
				else if(name.equals("STG_Bodenindikator_Leitstreifen_D2072"))
					merkmaleJsonObject.put("bodenindikator_leitstreifen", wert.equals("true"));

				else if(name.equals("STG_DynFahrtzielanzeiger_Vorhanden_D1140"))
					merkmaleJsonObject.put("dynfahrtzielanzeiger", wert.equals("true"));
				else if(name.equals("STG_DynFahrtzielanzeiger_Akustisch_D1141"))
					merkmaleJsonObject.put("dynfahrtzielanzeiger_akustisch", wert.equals("true"));
				else if(name.equals("STG_EinstiegHublift_vorhanden_D1220"))
					merkmaleJsonObject.put("hublift", wert.equals("true"));
				else if(name.equals("STG_EinstiegHublift_Laenge_cm_D1221"))
					merkmaleJsonObject.put("hublift_laenge_cm", (int) Double.parseDouble(wert));
				else if(name.equals("STG_EinstiegHublift_Tragfaehigkeit_kg_D1222"))
					merkmaleJsonObject.put("hublift_tragfaehigkeit_kg", (int) Double.parseDouble(wert));
				else if(name.equals("STG_Einstiegrampe_vorhanden_D1210"))
					merkmaleJsonObject.put("rampe", wert.equals("true"));
				else if(name.equals("STG_Einstiegrampe_Laenge_cm_D1211"))
					merkmaleJsonObject.put("rampe_laenge_cm", (int) Double.parseDouble(wert));
				else if(name.equals("STG_Einstiegrampe_Tragfaehigkeit_kg_D1212"))
					merkmaleJsonObject.put("rampe_tragfaehigkeit_kg", (int) Double.parseDouble(wert));
				else if(name.equals("STG_Fahrgastinfo"))
					merkmaleJsonObject.put("fahrgastinfo", wert);
				else if(name.equals("STG_Fahrgastinfo_inHoehe_100_160cm"))
					merkmaleJsonObject.put("fahrgastinfo_inhoehe100_160cm", wert.equals("true"));
				else if(name.equals("STG_Fahrgastinfo_freierreichbar_jn"))
					merkmaleJsonObject.put("fahrgastinfo_freierreichbar", wert.equals("true"));
				else if(name.equals("STG_Fahrkartenautomat_D1040"))
					merkmaleJsonObject.put("fahrkartenautomat", wert.equals("true"));
				else if(name.equals("STG_Fahrkartenautomat_ID"))
					merkmaleJsonObject.put("fahrkartenautomat_ID", wert);
				else if(name.equals("STG_Fahrkartenautomat_Lon"))
					merkmaleJsonObject.put("fahrkartenautomat_lon", Double.parseDouble(wert));
				else if(name.equals("STG_Fahrkartenautomat_Lat"))
					merkmaleJsonObject.put("fahrkartenautomat_lat", Double.parseDouble(wert));
				else if(name.equals("STG_InfoNotrufsaeule"))
					merkmaleJsonObject.put("infonotruf", wert);
				else if(name.equals("STG_IstPos_Lon"))
					merkmaleJsonObject.put("steig_lon", Double.parseDouble(wert));
				else if(name.equals("STG_IstPos_Lat"))
					merkmaleJsonObject.put("steig_lat", Double.parseDouble(wert));
				else if(name.equals("STG_Laengsneigung"))
					merkmaleJsonObject.put("laengsneigung_prozent", Double.parseDouble(wert));
				else if(name.equals("STG_Querneigung"))
					merkmaleJsonObject.put("querneigung_prozent", Double.parseDouble(wert));
				else if(name.equals("STG_Notiz"))
					merkmaleJsonObject.put("notiz", wert);
				else if(name.equals("STG_WartegelegenheitSitzplatz_Vorhanden_D1120"))
					merkmaleJsonObject.put("wartegelegenheit", wert.equals("true"));
				else if(name.equals("STG_SitzeOderUnterstand_Art"))
					merkmaleJsonObject.put("wartegelegenheit_art", wert);
				else if(name.equals("STG_Steigbreite_cm_D1180"))
					merkmaleJsonObject.put("breite_cm", (int) Double.parseDouble(wert));
				else if(name.equals("STG_SteigbreiteMinimum"))
					merkmaleJsonObject.put("engstellebreite_cm", (int) Double.parseDouble(wert));
				else if(name.equals("STG_Steiglaenge"))		// in m, umwandeln
					merkmaleJsonObject.put("laenge_cm", (int) (Double.parseDouble(wert)*100.0));
				else if(name.equals("STG_Unterstand_Kontrastelemente_jn"))
					merkmaleJsonObject.put("unterstand_kontrastelemente", wert.equals("true"));
				else if(name.equals("STG_Unterstand_RollstuhlfahrerFreieFlaeche"))
					merkmaleJsonObject.put("unterstand_freieflaeche", wert.equals("true"));
				else if(name.equals("STG_Unterstand_WaendebisBodennaehe_jn"))
					merkmaleJsonObject.put("unterstand_waendebodennah", wert.equals("true"));

					// ===============================================================================
					// Fotos

				else if(name.equals("STG_Foto"))
					merkmaleJsonObject.put("steig_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_2_Foto"))
					merkmaleJsonObject.put("steig2_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt_Foto"))
					merkmaleJsonObject.put("abschnitt1_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt2_Foto"))
					merkmaleJsonObject.put("abschnitt2_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt3_Foto"))
					merkmaleJsonObject.put("abschnitt3_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt4_Foto"))
					merkmaleJsonObject.put("abschnitt4_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt5_Foto"))
					merkmaleJsonObject.put("abschnitt5_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt6_Foto"))
					merkmaleJsonObject.put("abschnitt6_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt7_Foto"))
					merkmaleJsonObject.put("abschnitt7_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Abschnitt8_Foto"))
					merkmaleJsonObject.put("abschnitt8_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Bodenindikator_Einstiegsbereich_Foto"))
					merkmaleJsonObject.put("bodenindikator_einstiegsbereich_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Bodenindikator_Leitstreifen_Foto"))
					merkmaleJsonObject.put("bodenindikator_leitstreifen_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Breite_Foto"))
					merkmaleJsonObject.put("breite_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_dynFahrtzielanzeiger_Foto"))
					merkmaleJsonObject.put("dynfahrtzielanzeiger_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_EinstiegHublift_Foto"))
					merkmaleJsonObject.put("hublift_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Einstiegrampe_Foto"))
					merkmaleJsonObject.put("rampe_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Engstelle_Foto"))
					merkmaleJsonObject.put("engstellebreite_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Fahrgastinfo_nichtbarrierefrei_Foto"))
					merkmaleJsonObject.put("fahrgastinfo_nichtfreierreichbar_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Fahrkartenautomat_Foto"))
					merkmaleJsonObject.put("fahrkartenautomat_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Gegenueber_Foto"))
					merkmaleJsonObject.put("steiggegenueber_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Haltepunkt_Foto"))
					merkmaleJsonObject.put("haltepunkt1_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Haltepunkt2_Foto"))
					merkmaleJsonObject.put("haltepunkt2_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Haltepunkt3_Foto"))
					merkmaleJsonObject.put("haltepunkt3_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Haltepunkt4_Foto"))
					merkmaleJsonObject.put("haltepunkt4_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_InfoNotrufsaeule_Foto"))
					merkmaleJsonObject.put("infonotruf_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_SitzeoderUnterstand_Foto"))
					merkmaleJsonObject.put("wartegelegenheit_Foto", Bild.getBildUrl(wert, dhid));
				else if(name.equals("STG_Uhr_Foto"))
					merkmaleJsonObject.put("uhr_Foto", Bild.getBildUrl(wert, dhid));
				else
					NVBWLogger.warning("in Servlet " + this.getServletName() 
						+ " nicht verarbeitetes Merkmal Name '" + name + "'" 
						+ ", Wert '" + wert + "'");
			}
			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			NVBWLogger.severe("SQLException::: " + e.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "SQL-Fehler aufgetreten, bitte Administrator informieren: "
					+ e.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		response.getWriter().append(merkmaleJsonObject.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
