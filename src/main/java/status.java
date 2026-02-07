
import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.diva.graph.Grapherzeugung;

/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "status", 
			urlPatterns = {"/status"}
		)
public class status extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    private static Connection bfrkConn = null;

    public static int SC_NOTFOUND = 404;

    private static Date startzeitpunkt = null;
    
	private static DateFormat datetime_de_formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public status() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * initialization on servlett startup
     * - connect to bfrk DB
     */
    @Override
    public void init() {
    	startzeitpunkt = new Date();
    	bfrkConn = DBVerbindung.getDBVerbindung();
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		JSONObject merkmaleJsonObject = new JSONObject();

		Date jetzt = new Date();

		merkmaleJsonObject.put("API Abfragezeitpunkt", datetime_de_formatter.format(jetzt));

		String dbname = DBVerbindung.getDbnameoeffentlich();
		merkmaleJsonObject.put("DB-Name", dbname);

		Date startzeitpunkt = DBVerbindung.getDBVerbindungsaufbauzeitpunkt();
		merkmaleJsonObject.put("DB-Verbindungsaufbauzeitpunkt", datetime_de_formatter.format(startzeitpunkt));

		double activeTime = DBVerbindung.getDBActiveTime();
		merkmaleJsonObject.put("DB-aktive-Zeit", activeTime);
				
		long id = DBVerbindung.gethoechsteTabellenID("Objekt");
		merkmaleJsonObject.put("höchste ID Tabelle Objekt", id);
		
		id = DBVerbindung.gethoechsteTabellenID("Osmimport");
		merkmaleJsonObject.put("höchste ID Tabelle Osmimport", id);

		id = DBVerbindung.gethoechsteTabellenID("Osmobjektbezug");
		merkmaleJsonObject.put("höchste ID Tabelle Osmobjektbezug", id);

		String grapherzeugungversion = Grapherzeugung.getVersion();
		merkmaleJsonObject.put("Grapherzeugung Programmversion", grapherzeugungversion);

		merkmaleJsonObject.put("BFRK-API manuell gesetzte Programmversion", "20251027-220700");
		response.getWriter().append(merkmaleJsonObject.toString());

		response.setStatus(HttpServletResponse.SC_OK);
	}

}
