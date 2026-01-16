
import java.io.IOException;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "landingpage", 
			urlPatterns = {"/"}
		)
public class _landingpage extends HttpServlet {
	private static final long serialVersionUID = 1L;


    /**
     * @see HttpServlet#HttpServlet()
     */
    public _landingpage() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * initialization on servlett startup
     */
    @Override
    public void init() {
    }


	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");

		StringBuilder output = new StringBuilder();
		
		output.append("<html>\r\n");
		output.append("  <head>\r\n");
		output.append("    <meta charset=\"UTF-8\">\r\n");
		output.append("  </head>\r\n");
		output.append("  <body>\r\n");
		output.append("    <h1>BFRK-API</h1>\r\n");
		output.append("    <p>Version 0.4.3\r\n");
		output.append("    <h2>Dokumentation zur BFRK-API</h2>\r\n");
		output.append("    <p>Die Dokumentation ist auf <a href=\"https://app.swaggerhub.com/apis/NVBWSeifert/BFRK_API\">Swaggerhub</a> einsehbar.\r\n");
		output.append("  </body>\r\n");
		output.append("</html>\r\n");
		
		response.getWriter().append(output.toString());
		
	}
}
