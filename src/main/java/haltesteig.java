
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

import de.nvbw.base.NVBWLogger;
import org.json.JSONObject;

import de.nvbw.bfrk.util.Bild;
import de.nvbw.bfrk.util.DBVerbindung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "haltesteig", 
			urlPatterns = {"/haltesteig/*"}
		)
public class haltesteig extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final Logger LOG = NVBWLogger.getLogger(stopmodell.class);

    private static Connection bfrkConn = null;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public haltesteig() {
        super();
    }

    /**
     * initialization on servlet startup
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
				LOG.severe("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					LOG.severe("FEHLER: keine DB-Verbindung offen");
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren");
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		} catch (SQLException e1) {
			LOG.severe("SQLException aufgetreten in aufzug doGet, " + e1.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "keine DB-Verbindung verfügbar, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		} catch (IOException e1) {
			LOG.severe("IOException aufgetreten in aufzug doGet, " + e1.toString());
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "unbekannter Fehler aufgetreten, bitte Administrator informieren: "
					+ e1.toString());
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		long paramObjektid = 0;
		String paramDhid = "";
		if(request.getParameter("dhid") != null) {
			LOG.info("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			paramDhid = request.getParameter("dhid");
		} else {
			LOG.info("url-Parameter dhid fehlt ...");
			String requesturi = request.getRequestURI();
			LOG.info("requesturi ===" + requesturi + "===");
			if(requesturi.contains("/haltesteig")) {
				int startpos = requesturi.indexOf("/haltesteig");
				LOG.info("startpos #1: " + startpos);
				if(requesturi.indexOf("/",startpos + 1) != -1) {
					paramObjektid = Long.parseLong(requesturi.substring(requesturi.indexOf("/",startpos + 1) + 1));
					LOG.info("Versuch, Objektid zu extrahieren ===" + paramObjektid + "===");
				}
			}
		}

		if((paramObjektid == 0) && paramDhid.isEmpty()) {
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Angabe objektid oder Parameter dhid fehlen");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		String selectHaltestelleSql = "";
		if(paramObjektid != 0)
			selectHaltestelleSql = "SELECT o.dhid, merkmal.name, merkmal.wert, typ FROM merkmal "
				+ "JOIN objekt AS o ON objekt.id = merkmal.objekt_id "
				+ "WHERE objekt_id = ?;";
		else
			selectHaltestelleSql = "SELECT o.dhid, merkmal.name, merkmal.wert, typ FROM merkmal "
				+ "JOIN objekt AS o ON o.id = merkmal.objekt_id "
				+ "WHERE objektart IN ('Haltesteig', 'SEVHaltesteig') AND dhid = ?;";

		JSONObject merkmaleJsonObject = new JSONObject();

		PreparedStatement selectHaltestelleStmt;
		try {
			selectHaltestelleStmt = bfrkConn.prepareStatement(selectHaltestelleSql);
			if(paramObjektid != 0)
				selectHaltestelleStmt.setLong(1, paramObjektid);
			else
				selectHaltestelleStmt.setString(1, paramDhid);
			LOG.info("Haltestelle query: " + selectHaltestelleStmt.toString() + "===");

			ResultSet selectMerkmaleRS = selectHaltestelleStmt.executeQuery();

			int anzahldatensaetze = 0;
			while(selectMerkmaleRS.next()) {
				anzahldatensaetze++;
				String dhid = selectMerkmaleRS.getString("dhid");
				String name = selectMerkmaleRS.getString("name");
				String wert = selectMerkmaleRS.getString("wert");
				String typ = selectMerkmaleRS.getString("typ");

				//       Beginn spezifische Haltesteig-Merkmale				
                switch (name) {
                    case "STG_EinstiegStrassenmitte_D2140" ->
                            merkmaleJsonObject.put("einstiegstrassenmitte", wert.equals("true"));
                    case "STG_Hochbord_vorhanden" -> merkmaleJsonObject.put("hochbordvorhanden", wert.equals("true"));
                    case "STG_Steighoehe_cm_D1170" ->
                            merkmaleJsonObject.put("hochbordhoehe_cm", (int) Double.parseDouble(wert));
                    case "STG_Hochbord_Art" -> merkmaleJsonObject.put("hochboardart", wert);
                    case "STG_Steigtyp" -> merkmaleJsonObject.put("steigtyp", wert);
                    case "STG_Tuer2_Laenge_cm" ->
                            merkmaleJsonObject.put("tuer2laenge_cm", (int) Double.parseDouble(wert));
                    case "STG_Tuer2_Breite_cm" ->
                            merkmaleJsonObject.put("tuer2breite_cm", (int) Double.parseDouble(wert));
                    case "STG_Tuer2_Einstiegsflaeche_ausreichend" ->
                            merkmaleJsonObject.put("tuer2eintiegsflaechevorhanden", wert.equals("true"));
                    case "STG_Unterstand_offiziell_jn" ->
                            merkmaleJsonObject.put("unterstand_offiziell", wert.equals("true"));
                    case "STG_ZUS_Buchtlaenge_m" ->
                            merkmaleJsonObject.put("buchtlaenge_cm", (int) (Double.parseDouble(wert) * 100.0));
                    case "STG_Zuweg1_Zugangstyp" -> merkmaleJsonObject.put("zuweg1typ", wert);
                    case "STG_Zuweg1_weniger2Prozent_jn" ->
                            merkmaleJsonObject.put("zuweg1weniger2prozent", wert.equals("true"));
                    case "STG_Zuweg1_eben_Laenge_cm" ->
                            merkmaleJsonObject.put("zuweg1ebenlaenge_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg1_eben_Breite_cm" ->
                            merkmaleJsonObject.put("zuweg1ebenbreite_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg1_Rampe_Laenge_cm" ->
                            merkmaleJsonObject.put("zuweg1rampelaenge_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg1_Rampe_Breite_cm" ->
                            merkmaleJsonObject.put("zuweg1rampebreite_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg1_Rampe_Neigung_prozent" ->
                            merkmaleJsonObject.put("zuweg1rampelaengsneigung_prozent", Double.parseDouble(wert));
                    case "STG_Zuweg1_Rampe_Querneigung_prozent" ->
                            merkmaleJsonObject.put("zuweg1rampequerneigung_prozent", Double.parseDouble(wert));
                    case "STG_Zuweg1_Stufe_Hoehe_cm", "STG_Zuweg1_ZuwegmitStufe_Hoehe_cm" ->
                            merkmaleJsonObject.put("zuweg1stufehoehe_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg1_Notiz" -> merkmaleJsonObject.put("zuweg1notiz", wert);
                    case "STG_Zuweg2_Zugangstyp" -> merkmaleJsonObject.put("zuweg2typ", wert);
                    case "STG_Zuweg2_weniger2Prozent_jn" ->
                            merkmaleJsonObject.put("zuweg2weniger2prozent", wert.equals("true"));
                    case "STG_Zuweg2_eben_Laenge_cm" ->
                            merkmaleJsonObject.put("zuweg2ebenlaenge_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg2_eben_Breite_cm" ->
                            merkmaleJsonObject.put("zuweg2ebenbreite_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg2_Rampe_Laenge_cm" ->
                            merkmaleJsonObject.put("zuweg2rampelaenge_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg2_Rampe_Breite_cm" ->
                            merkmaleJsonObject.put("zuweg2rampebreite_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg2_Rampe_Neigung_prozent" ->
                            merkmaleJsonObject.put("zuweg2rampelaengsneigung_prozent", Double.parseDouble(wert));
                    case "STG_Zuweg2_Rampe_Querneigung_prozent" ->
                            merkmaleJsonObject.put("zuweg2rampequerneigung_prozent", Double.parseDouble(wert));
                    case "STG_Zuweg2_Stufe_Hoehe_cm", "STG_Zuweg2_ZuwegmitStufe_Hoehe_cm" ->
                            merkmaleJsonObject.put("zuweg2stufehoehe_cm", (int) Double.parseDouble(wert));
                    case "STG_Zuweg2_Notiz" -> merkmaleJsonObject.put("zuweg2notiz", wert);

                    //       Ende spezifische Haltesteig-Merkmale
                    case "STG_Abfallbehaelter" -> merkmaleJsonObject.put("abfallbehaelter", wert.equals("true"));
                    case "STG_Ansagen_Vorhanden_D1150" -> merkmaleJsonObject.put("ansage", wert.equals("true"));
                    case "STG_ZUS_Uhr_jn" -> merkmaleJsonObject.put("uhr", wert.equals("true"));
                    case "STG_Beleuchtung" -> merkmaleJsonObject.put("beleuchtung", wert.equals("true"));
                    case "STG_Bodenbelag_Art" -> merkmaleJsonObject.put("bodenbelag", wert);
                    case "STG_Bodenbelag_Unbefestigt_Vorhanden_D2050" ->
                            merkmaleJsonObject.put("bodenbelag_unbefestigt", wert.equals("true"));
                    case "STG_Bodenindikator_Vorhanden_D2070" ->
                            merkmaleJsonObject.put("bodenindikator", wert.equals("true"));
                    case "STG_Bodenindikator_Einstiegsbereich" ->
                            merkmaleJsonObject.put("bodenindikator_einstiegsbereich", wert.equals("true"));
                    case "STG_Bodenindikator_EinstiegUndAuffind_D2071" ->
                            merkmaleJsonObject.put("bodenindikator_einstiegauffind", wert.equals("true"));
                    case "STG_Bodenindikator_Leitstreifen_D2072" ->
                            merkmaleJsonObject.put("bodenindikator_leitstreifen", wert.equals("true"));
                    case "STG_DynFahrtzielanzeiger_Vorhanden_D1140" ->
                            merkmaleJsonObject.put("dynfahrtzielanzeiger", wert.equals("true"));
                    case "STG_DynFahrtzielanzeiger_Akustisch_D1141" ->
                            merkmaleJsonObject.put("dynfahrtzielanzeiger_akustisch", wert.equals("true"));
                    case "STG_EinstiegHublift_vorhanden_D1220" ->
                            merkmaleJsonObject.put("hublift", wert.equals("true"));
                    case "STG_EinstiegHublift_Laenge_cm_D1221" ->
                            merkmaleJsonObject.put("hublift_laenge_cm", (int) Double.parseDouble(wert));
                    case "STG_EinstiegHublift_Tragfaehigkeit_kg_D1222" ->
                            merkmaleJsonObject.put("hublift_tragfaehigkeit_kg", (int) Double.parseDouble(wert));
                    case "STG_Einstiegrampe_vorhanden_D1210" -> merkmaleJsonObject.put("rampe", wert.equals("true"));
                    case "STG_Einstiegrampe_Laenge_cm_D1211" ->
                            merkmaleJsonObject.put("rampe_laenge_cm", (int) Double.parseDouble(wert));
                    case "STG_Einstiegrampe_Tragfaehigkeit_kg_D1212" ->
                            merkmaleJsonObject.put("rampe_tragfaehigkeit_kg", (int) Double.parseDouble(wert));
                    case "STG_Fahrgastinfo" -> merkmaleJsonObject.put("fahrgastinfo", wert);
                    case "STG_Fahrgastinfo_inHoehe_100_160cm" ->
                            merkmaleJsonObject.put("fahrgastinfo_inhoehe100_160cm", wert.equals("true"));
                    case "STG_Fahrgastinfo_freierreichbar_jn" ->
                            merkmaleJsonObject.put("fahrgastinfo_freierreichbar", wert.equals("true"));
                    case "STG_Fahrkartenautomat_D1040" ->
                            merkmaleJsonObject.put("fahrkartenautomat", wert.equals("true"));
                    case "STG_Fahrkartenautomat_ID" -> merkmaleJsonObject.put("fahrkartenautomat_ID", wert);
                    case "STG_Fahrkartenautomat_Lon" ->
                            merkmaleJsonObject.put("fahrkartenautomat_lon", Double.parseDouble(wert));
                    case "STG_Fahrkartenautomat_Lat" ->
                            merkmaleJsonObject.put("fahrkartenautomat_lat", Double.parseDouble(wert));
                    case "STG_InfoNotrufsaeule" -> merkmaleJsonObject.put("infonotruf", wert);
                    case "STG_IstPos_Lon" -> merkmaleJsonObject.put("steig_lon", Double.parseDouble(wert));
                    case "STG_IstPos_Lat" -> merkmaleJsonObject.put("steig_lat", Double.parseDouble(wert));
                    case "STG_Laengsneigung" ->
                            merkmaleJsonObject.put("laengsneigung_prozent", Double.parseDouble(wert));
                    case "STG_Querneigung" -> merkmaleJsonObject.put("querneigung_prozent", Double.parseDouble(wert));
                    case "STG_Notiz" -> merkmaleJsonObject.put("notiz", wert);
                    case "STG_WartegelegenheitSitzplatz_Vorhanden_D1120" ->
                            merkmaleJsonObject.put("wartegelegenheit", wert.equals("true"));
                    case "STG_SitzeOderUnterstand_Art" -> merkmaleJsonObject.put("wartegelegenheit_art", wert);
                    case "STG_Steigbreite_cm_D1180" ->
                            merkmaleJsonObject.put("breite_cm", (int) Double.parseDouble(wert));
                    case "STG_SteigbreiteMinimum" ->
                            merkmaleJsonObject.put("engstellebreite_cm", (int) Double.parseDouble(wert));
                    case "STG_Steiglaenge" ->
// in m, umwandeln
                            merkmaleJsonObject.put("laenge_cm", (int) (Double.parseDouble(wert) * 100.0));
                    case "STG_Unterstand_Kontrastelemente_jn" ->
                            merkmaleJsonObject.put("unterstand_kontrastelemente", wert.equals("true"));
                    case "STG_Unterstand_RollstuhlfahrerFreieFlaeche" ->
                            merkmaleJsonObject.put("unterstand_freieflaeche", wert.equals("true"));
                    case "STG_Unterstand_WaendebisBodennaehe_jn" ->
                            merkmaleJsonObject.put("unterstand_waendebodennah", wert.equals("true"));


                    // ===============================================================================
                    // Fotos

                    //       Beginn spezifische Haltesteig-Fotos
                    case "STG_Zuweg1_direkt_Foto" ->
                            merkmaleJsonObject.put("zuweg1direkt_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg1_eben_Foto" ->
                            merkmaleJsonObject.put("zuweg1eben_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg1_nurStufe_Foto" ->
                            merkmaleJsonObject.put("zuweg1nurstufe_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg1_Rampe_Foto" ->
                            merkmaleJsonObject.put("zuweg1rampe_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg1_sonstiges_Foto" ->
                            merkmaleJsonObject.put("zuweg1sonstiges_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg1_Weg_Stufe_Foto" ->
                            merkmaleJsonObject.put("zuweg1wegstufe_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg2_direkt_Foto" ->
                            merkmaleJsonObject.put("zuweg2direkt_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg2_eben_Foto" ->
                            merkmaleJsonObject.put("zuweg2eben_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg2_nurStufe_Foto" ->
                            merkmaleJsonObject.put("zuweg2nurstufe_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg2_Rampe_Foto" ->
                            merkmaleJsonObject.put("zuweg2rampe_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg2_sonstiges_Foto" ->
                            merkmaleJsonObject.put("zuweg2sonstiges_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Zuweg2_Weg_Stufe_Foto" ->
                            merkmaleJsonObject.put("zuweg2wegstufe_Foto", Bild.getBildUrl(wert, dhid));

                    //       Ende spezifische Haltesteig-Fotos
                    case "STG_Foto" -> merkmaleJsonObject.put("steig_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_2_Foto" -> merkmaleJsonObject.put("steig2_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt_Foto" -> merkmaleJsonObject.put("abschnitt1_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt2_Foto" ->
                            merkmaleJsonObject.put("abschnitt2_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt3_Foto" ->
                            merkmaleJsonObject.put("abschnitt3_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt4_Foto" ->
                            merkmaleJsonObject.put("abschnitt4_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt5_Foto" ->
                            merkmaleJsonObject.put("abschnitt5_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt6_Foto" ->
                            merkmaleJsonObject.put("abschnitt6_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt7_Foto" ->
                            merkmaleJsonObject.put("abschnitt7_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Abschnitt8_Foto" ->
                            merkmaleJsonObject.put("abschnitt8_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Bodenindikator_Einstiegsbereich_Foto" ->
                            merkmaleJsonObject.put("bodenindikator_einstiegsbereich_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Bodenindikator_Leitstreifen_Foto" ->
                            merkmaleJsonObject.put("bodenindikator_leitstreifen_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Breite_Foto" -> merkmaleJsonObject.put("breite_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_dynFahrtzielanzeiger_Foto" ->
                            merkmaleJsonObject.put("dynfahrtzielanzeiger_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_EinstiegHublift_Foto" ->
                            merkmaleJsonObject.put("hublift_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Einstiegrampe_Foto" -> merkmaleJsonObject.put("rampe_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Engstelle_Foto" ->
                            merkmaleJsonObject.put("engstellebreite_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Fahrgastinfo_nichtbarrierefrei_Foto" ->
                            merkmaleJsonObject.put("fahrgastinfo_nichtfreierreichbar_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Fahrkartenautomat_Foto" ->
                            merkmaleJsonObject.put("fahrkartenautomat_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Gegenueber_Foto" ->
                            merkmaleJsonObject.put("steiggegenueber_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Haltepunkt_Foto" ->
                            merkmaleJsonObject.put("haltepunkt1_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Haltepunkt2_Foto" ->
                            merkmaleJsonObject.put("haltepunkt2_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Haltepunkt3_Foto" ->
                            merkmaleJsonObject.put("haltepunkt3_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Haltepunkt4_Foto" ->
                            merkmaleJsonObject.put("haltepunkt4_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_InfoNotrufsaeule_Foto" ->
                            merkmaleJsonObject.put("infonotruf_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_SitzeoderUnterstand_Foto" ->
                            merkmaleJsonObject.put("wartegelegenheit_Foto", Bild.getBildUrl(wert, dhid));
                    case "STG_Uhr_Foto" -> merkmaleJsonObject.put("uhr_Foto", Bild.getBildUrl(wert, dhid));
                    default -> LOG.warning("in Servlet " + this.getServletName()
                            + " nicht verarbeitetes Merkmal Name '" + name + "'"
                            + ", Wert '" + wert + "'");
                }
			}
			selectMerkmaleRS.close();
			selectHaltestelleStmt.close();

			if(anzahldatensaetze == 0) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		} catch (SQLException e) {
			LOG.severe("SQLException::: " + e.toString());
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
		LOG.info("Request angekommen in /haltesteig doPost ...");

		response.setCharacterEncoding("UTF-8");
		JSONObject ergebnisJsonObject = new JSONObject();
		ergebnisJsonObject.put("status", "fehler");
		ergebnisJsonObject.put("fehlertext", "POST Request /aufzug ist nicht vorhanden");
		response.getWriter().append(ergebnisJsonObject.toString());
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}

}
