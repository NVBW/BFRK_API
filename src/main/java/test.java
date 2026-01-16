
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
@WebServlet(name = "test", 
			urlPatterns = {"/test/*"}
		)
public class test extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public test() {
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
		
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		StringBuilder output = new StringBuilder();
		output.append("<html>\r\n");
		output.append("  <head>\r\n");
		output.append("    <meta charset=\"UTF-8\">\r\n");
		output.append("  </head>\r\n");
		output.append("  <body>\r\n");
		output.append("    <h1>BFRK-API</h1>\r\n");
		output.append("    <p>GET test Request angekommen.\r\n");
		output.append("  </body>\r\n");
		output.append("</html>\r\n");
		
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append(output.toString());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Headers", "*");

		StringBuilder output = new StringBuilder();
		output.append("<html>\r\n");
		output.append("  <head>\r\n");
		output.append("    <meta charset=\"UTF-8\">\r\n");
		output.append("  </head>\r\n");
		output.append("  <body>\r\n");
		output.append("    <h1>BFRK-API</h1>\r\n");
		output.append("    <p>POST test Request angekommen.\r\n");
		output.append("  </body>\r\n");
		output.append("</html>\r\n");
		
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().append(output.toString());
	}

}
