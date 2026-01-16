
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import de.nvbw.base.BFRKApiApplicationconfiguration;
import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.DBVerbindung;
import de.nvbw.diva.graph.Grapherzeugung;




/**
 * Servlet implementation class haltestelle
 */
@WebServlet(name = "stopmodell", 
			urlPatterns = {"/stopmodell/*"}
		)
public class stopmodell extends HttpServlet {
	private static DateFormat date_de_formatter = new SimpleDateFormat("dd.MM.yyyy");

	private static BFRKApiApplicationconfiguration bfrkapiconfiguration = null;

	private static final long serialVersionUID = 1L;

    private static Connection bfrkConn = null;

    /**
     * @see HttpServlet#HttpServlet()
     */
    public stopmodell() {
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

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
		response.setHeader("Access-Control-Allow-Headers", "*");

			// ==================== Parameter auslesen =========================
		String dhid = "";
		int release = 0;
		int mayorversion = 0;
		int minorversion = 0;
		if(request.getParameter("dhid") != null) {
			System.out.println("url-Parameter dhid vorhanden ===" + request.getParameter("dhid"));
			dhid = request.getParameter("dhid");
		}
		if(request.getParameter("release") != null) {
			System.out.println("url-Parameter release vorhanden ===" + request.getParameter("release"));
			try {
				release = Integer.parseInt(request.getParameter("release"));
				System.out.println("Parameter release: " + release);
			} catch(NumberFormatException ne) {
				System.out.println("Parameter release kann nicht numerisch geparst werden");
			}
		}
		if(request.getParameter("mayorversion") != null) {
			System.out.println("url-Parameter mayorversion vorhanden ===" + request.getParameter("mayorversion"));
			try {
				mayorversion = Integer.parseInt(request.getParameter("mayorversion"));
				System.out.println("Parameter mayorversion: " + mayorversion);
			} catch(NumberFormatException ne) {
				System.out.println("Parameter mayorversion kann nicht numerisch geparst werden");
			}
		}
		if(request.getParameter("minorversion") != null) {
			System.out.println("url-Parameter minorversion vorhanden ===" + request.getParameter("minorversion"));
			try {
				minorversion = Integer.parseInt(request.getParameter("minorversion"));
				System.out.println("Parameter minorversion: " + minorversion);
			} catch(NumberFormatException ne) {
				System.out.println("Parameter minorversion kann nicht numerisch geparst werden");
			}
		}


		if(request.getParameter("dhid") == null) {
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Pflicht-Parameter dhid fehlt");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				NVBWLogger.severe("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				System.out.println("FEHLER: keine DB-Verbindung offen, es wird versucht, DB-init aufzurufen");
				init();
				if((bfrkConn == null) || !bfrkConn.isValid(5)) {
					response.getWriter().append("FEHLER: keine DB-Verbindung offen");
					return;
				}
			}
		} catch (SQLException e1) {
			NVBWLogger.severe("FEHLER: keine DB-Verbindung offen, bei SQLException " + e1.toString());
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei SQLException " + e1.toString());
			return;
		} catch (IOException e1) {
			NVBWLogger.severe("FEHLER: keine DB-Verbindung offen, bei IOException " + e1.toString());
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei IOException " + e1.toString());
			return;
		}

		int dbrelease = 0;
		int dbmayorversion = 0;
		int dbminorversion = 0;
		long dbid = 0;
		PreparedStatement selectneuestesModellStmt = null;
		try {
				// ==================== ermitteln der neuesten Version (ggfs. gibt es auch gar keine Version) =========================
			String selectneuestesModellSql = "SELECT id, release, mayorversion, minorversion FROM objektmodell "
				+ "WHERE dhid = ? ORDER BY release ASC, mayorversion ASC, minorversion ASC;";

			selectneuestesModellStmt = bfrkConn.prepareStatement(selectneuestesModellSql);
			selectneuestesModellStmt.setString(1, dhid);
			NVBWLogger.info("Objektmodell neustes Modell query: " + selectneuestesModellStmt.toString() + "===");

			ResultSet selectneuestesModellRS = selectneuestesModellStmt.executeQuery();

			while(selectneuestesModellRS.next()) {
				dbid = selectneuestesModellRS.getLong("id");
				dbrelease = selectneuestesModellRS.getInt("release");
				dbmayorversion = selectneuestesModellRS.getInt("mayorversion");
				dbminorversion = selectneuestesModellRS.getInt("minorversion");
				if((release != 0) || (mayorversion != 0) || (minorversion != 0)) {
					System.out.println("ok, es wurde ein spezielle Version abgefragt: " + release + "/" + mayorversion + "/" + minorversion);
					if((dbrelease == release) && (dbmayorversion == mayorversion) && (dbminorversion == minorversion)) {
						System.out.println("ok, die speziell abgefragte Version wurde gefunden, sie hat DB-Id: " + dbid);
						break;
					}
				}
			}
			selectneuestesModellRS.close();
			selectneuestesModellStmt.close();

		} catch (SQLException e) {
			NVBWLogger.warning("SQLException. Select-Query neueste Version führte zu einem Fehler, "
				+ "Query war: " + selectneuestesModellStmt.toString());
			NVBWLogger.warning("Details: " + e.toString());
			String fehlertext = "Die DB-Abfrage nach ggfs. vorhandener Graph-Versionen ist fehlgeschlagen";
			JSONObject ergebnisJsonObject = new JSONObject();
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		System.out.println("nach Select auf objektmodell gefundende Version: " + dbrelease + "/" + dbmayorversion + "/" + dbminorversion);

		release = dbrelease;
		mayorversion = dbmayorversion;
		minorversion = dbminorversion;

			// ==================== wenn es noch gar keine Version gibt, Grapherzeugung aufrufen =========================

			// rausschreiben von tomcat9 aus ist normal nicht in ein beliebiges Verzeichnis möglich
			// anpassen der /etc/systemd/system/tomcat9.service.d/override.conf
			// dort 
			//[Service]
			//ReadWritePaths=/home/NVBWAdmin/tomcat-deployment/bfrk_api_home/tomcatoutput
			// siehe https://stackoverflow.com/questions/56827735/how-to-allow-tomcat-war-app-to-write-in-folder
		if((release == 0) && (mayorversion == 0) && (minorversion == 0)) {
				// ok, initiale Version auf 1.0.0 setzen
			release = 1;

			String outputdatei = bfrkapiconfiguration.application_datadir 
				+ File.separator + "tomcatoutput" + File.separator + "bahnhofsmodellierung.json";
			File outputdateiHandle = new File(outputdatei);
			if(outputdateiHandle.exists())
				outputdateiHandle.delete();
			
			String args[] = new String[6];
			int argsindex = 0;
			args[argsindex++] = "-dhid";
			args[argsindex++] = dhid;
			args[argsindex++] = "-kanten";
			args[argsindex++] = "leer";
			args[argsindex++] = "-ausgabedatei";
			args[argsindex++] = outputdatei;
			JSONObject graphresultJson = Grapherzeugung.execute(args);

			try {
				if(graphresultJson != null) {
					String status = graphresultJson.get("status").toString();
					NVBWLogger.info("Status ===" + status + "===");
					if(status.equals("fehler")) {
						String fehlertext = graphresultJson.getString("fehlertext").toString();
						if(fehlertext != null) {
							if(fehlertext.startsWith("In DIVA wurde die angegebene DHID nicht gefunden")) {
								response.getWriter().append(graphresultJson.toString());
								response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							} else {
								response.getWriter().append(graphresultJson.toString());
								response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
							}
						} else {
							response.getWriter().append(graphresultJson.toString());
							response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						}
						return;
					}
				}
			} catch (JSONException e) {
				System.out.println("");
			}

				// wenn von Grapherzeugung eine Datei herauskam, in der DB speichern
			if(outputdateiHandle.exists()) {
				StringBuilder jsoncontent = new StringBuilder();

				try {
					BufferedReader  dateireader = new BufferedReader(new InputStreamReader(
						new FileInputStream(new File(outputdatei)), 
						StandardCharsets.UTF_8));

					String line = "";
					while ((line = dateireader.readLine()) != null) {
						jsoncontent.append(line + "\r\n");
					}
					dateireader.close();
					NVBWLogger.info("Json-Outputdatei Länge: " + jsoncontent.length());
				} catch (IOException ioe) {
					NVBWLogger.warning("Fehler bei Datei lesen " + outputdatei);
					NVBWLogger.warning(ioe.toString());
					String fehlertext = "Die Grapherzeugung im Json-Format ist nicht lesbar";
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}

				try {
			        // typecasting obj to JSONObject
			        JSONObject modellJson = new JSONObject(jsoncontent.toString());
			        JSONObject metadaten = (JSONObject) modellJson.get("metadaten");
			        JSONObject metagraph = (JSONObject) metadaten.get("graph");
			        metagraph.put("release", release);
			        metagraph.put("mayorversion", mayorversion);
			        metagraph.put("minorversion", minorversion);
			        NVBWLogger.info("json Metadaten in outputdatei nach Anpassung Versionierung auf 0/0/1 ===" + metagraph.toString() + "===");
			        NVBWLogger.info("json outputdatei insgesamt nach Anpassung Versionierung ===" + metagraph.toString() + "===");

					String insertModellSql = "INSERT INTO objektmodell (dhid, release, mayorversion, minorversion, "
						+ "benutzer, kommentar, content) VALUES(?, ?, ?, ?, ?, ?, ?::jsonb);";

					PreparedStatement selectModellStmt;
					PreparedStatement insertModellStmt;
					try {
						insertModellStmt = bfrkConn.prepareStatement(insertModellSql);
						int stmtindex = 1;
						insertModellStmt.setString(stmtindex++, dhid);
						insertModellStmt.setInt(stmtindex++, release);
						insertModellStmt.setInt(stmtindex++, mayorversion);
						insertModellStmt.setInt(stmtindex++, minorversion);
						insertModellStmt.setString(stmtindex++, "AUTOERZEUGER");
						insertModellStmt.setString(stmtindex++, "erzeugt am " + date_de_formatter.format(new Date()));
						insertModellStmt.setString(stmtindex++, modellJson.toString());
						System.out.println("Objektmodell store: " + insertModellStmt.toString() + "===");

						insertModellStmt.execute();
						insertModellStmt.close();

						response.getWriter().append(modellJson.toString());
						response.setStatus(HttpServletResponse.SC_OK);
						return;
					} catch (SQLException e) {
						System.out.println("SQLException::: " + e.toString());
						String fehlertext = "DB-Fehler aufgetreten, bitte den Administrtaor benachrichtigen";
						JSONObject ergebnisJsonObject = new JSONObject();
						ergebnisJsonObject.put("status", "fehler");
						ergebnisJsonObject.put("fehlertext", fehlertext);
						response.getWriter().append(ergebnisJsonObject.toString());
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						return;
					}
			        
		        } catch (FileNotFoundException e) {
					NVBWLogger.warning("FileNotFoundException. Es wurde keine Ausgabedatei "
						+ "von Grapherzeugung erstellt, daher ABBRUCH");
					NVBWLogger.warning("Details: " + e.toString());
					String fehlertext = "Die Grapherzeugung ist fehlgeschlagen, Grund unbekannt";
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				} catch (IOException e) {
					NVBWLogger.warning("IOException. Es wurde keine Ausgabedatei "
						+ "von Grapherzeugung erstellt, daher ABBRUCH");
					NVBWLogger.warning("Details: " + e.toString());
					String fehlertext = "Die Grapherzeugung ist fehlgeschlagen, Grund unbekannt";
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				} catch (Exception e) {
					NVBWLogger.warning("Exception. Die Ausgabedatei von "
						+ "Grapherzeugung hat einen Parserfehler, daher ABBRUCH");
					NVBWLogger.warning("Details: " + e.toString());
					String fehlertext = "Die Grapherzeugung erzeugte eine invalide json-Datei";
					JSONObject ergebnisJsonObject = new JSONObject();
					ergebnisJsonObject.put("status", "fehler");
					ergebnisJsonObject.put("fehlertext", fehlertext);
					response.getWriter().append(ergebnisJsonObject.toString());
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			} else {
				NVBWLogger.warning("Es wurde keine Ausgabedatei "
					+ "von Grapherzeugung erstellt, daher ABBRUCH");
				String fehlertext = "Die Grapherzeugung ist fehlgeschlagen, Grund unbekannt";
				JSONObject ergebnisJsonObject = new JSONObject();
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}
		}
		
		
			// ==================== die neueste Version holen =========================
		try {
			String selectModellSql = "SELECT dhid, release, mayorversion, minorversion, "
				+ "benutzer, kommentar, content FROM objektmodell "
				+ "WHERE dhid = ? AND release = ? AND mayorversion = ? AND minorversion = ?;";

			PreparedStatement selectModellStmt;

			selectModellStmt = bfrkConn.prepareStatement(selectModellSql);
			int stmtindex = 1;
			selectModellStmt.setString(stmtindex++, dhid);
			selectModellStmt.setInt(stmtindex++, release);
			selectModellStmt.setInt(stmtindex++, mayorversion);
			selectModellStmt.setInt(stmtindex++, minorversion);
			System.out.println("Objektmodell query: " + selectModellStmt.toString() + "===");

			ResultSet selectModellRS = selectModellStmt.executeQuery();

			JSONObject ergebnisJsonObject = new JSONObject();

			if(selectModellRS.next()) {
				String content = selectModellRS.getString("content");
				ergebnisJsonObject = new JSONObject(content);
			} else {
				String fehlertext = "unerwarteter Fehler aufgetreten beim Versuch, den Graphen zu holen";
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", fehlertext);
			}
			selectModellRS.close();
			selectModellStmt.close();

			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_OK);
			return;

		} catch (SQLException e) {
			NVBWLogger.warning("SQLException. Select-Query konkrete Version "
				+ "führte zu einem Fehler");
			NVBWLogger.warning("Details: " + e.toString());
			String fehlertext = "Die DB-Abfrage nach konkreter Graph-Version ist fehlgeschlagen";
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
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONObject ergebnisJsonObject = new JSONObject();

		try {
			if((bfrkConn == null) || !bfrkConn.isValid(5)) {
				response.getWriter().append("FEHLER: keine DB-Verbindung offen");
				return;
			}
		} catch (SQLException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei SQLException " + e1.toString());
			return;
		} catch (IOException e1) {
			response.getWriter().append("FEHLER: keine DB-Verbindung offen, bei IOException " + e1.toString());
			return;
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET, PUT, OPTIONS");
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

	    StringBuilder stringBuilder = new StringBuilder();
	    BufferedReader bufferedReader = null;

	    try {
	        ServletInputStream inputStream = request.getInputStream();
	        if (inputStream != null) {
	            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	            char[] charBuffer = new char[128];
	            int bytesRead = -1;
	            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
	                stringBuilder.append(charBuffer, 0, bytesRead);
	            }
	        } else {
	            stringBuilder.append("");
	        }
	    } catch (IOException ex) {
	        throw ex;
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException ex) {
	                throw ex;
	            }
	        }
	    }

	    System.out.println("erhaltener Content ===" + stringBuilder.toString() + "====");

		if(stringBuilder.length() == 0) {
			String fehlertext = "Content ist leer, das ist unzulässig";
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

			// ============= prüfen, ob accesstoken gültig ist ==============
		String authentifizierungSql = "SELECT modellspeichern, name FROM benutzer "
			+ "WHERE accesstoken = ?;";

		String bearbeiter = null;
		PreparedStatement authentifizierungStmt;
		try {
			authentifizierungStmt = bfrkConn.prepareStatement(authentifizierungSql);
			int stmtindex = 1;
			authentifizierungStmt.setString(stmtindex++, accesstoken);
			System.out.println("Authentifizierung query: " + authentifizierungStmt.toString() + "===");

			ResultSet authentifizierungRS = authentifizierungStmt.executeQuery();

			String dbaccesstoken = null;
			boolean modellspeichern = false;
			
			if(authentifizierungRS.next()) {
				bearbeiter = authentifizierungRS.getString("name");
				modellspeichern = authentifizierungRS.getBoolean("modellspeichern");
			}
			authentifizierungRS.close();
			authentifizierungStmt.close();

			if(modellspeichern == false) {
				ergebnisJsonObject.put("status", "fehler");
				ergebnisJsonObject.put("fehlertext", "Authentifizierung fehlgeschlagen");
				response.getWriter().append(ergebnisJsonObject.toString());
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				return;
			}
			
		} catch (SQLException e) {
			System.out.println("SQLException::: " + e.toString());
			String fehlertext = "DB-Fehler aufgetreten, bitte den Administrtaor benachrichtigen";
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		String dhid = "";
		String kommentar = "";
		int release = 0;
		int mayorversion = 0;
		int minorversion = 0;
		

		JSONObject contentJson = null;
    	JSONObject metadatenJson = null;
    	JSONObject metagraphJson = null;
	    try {
	    	contentJson = new JSONObject(stringBuilder.toString());

		    try {
		    	metadatenJson = (JSONObject) contentJson.get("metadaten");
		    	
			    try {
			    	dhid = metadatenJson.getString("dhid");
			    } catch(JSONException e) {
			    	NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
			    }
			    try {
			    	metagraphJson = (JSONObject) metadatenJson.get("graph");

				    try {
				    	release = metagraphJson.getInt("release");
				    } catch(JSONException e) {
				    	NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
				    }
				    try {
				    	mayorversion = metagraphJson.getInt("mayorversion");
				    } catch(JSONException e) {
				    	NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
				    }
				    try {
				    	minorversion = metagraphJson.getInt("minorversion");
				    	minorversion++;
				    	metagraphJson.put("minorversion", minorversion);
				    } catch(JSONException e) {
				    	NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
				    }
					try {
						kommentar = metagraphJson.getString("kommentar");
					} catch(JSONException e) {
						NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
					}
						// Bearbeiter aus Token-Analyse ermittelt und in Graphen integrieren
					metagraphJson.put("bearbeiter", bearbeiter);
				} catch(JSONException e) {
					NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
				}
		    } catch(JSONException e) {
		    	NVBWLogger.warning("Json-Objekt nicht vorhanden, Details " + e.toString());
		    }
	    } catch(JSONException e) {
	    	NVBWLogger.warning("Json-Content konnte nicht in JSONObject geparsed werden");
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", "Der Graph im JSON-Format konnte nicht geparsed werden");
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
	    }

		NVBWLogger.info("dhid: " + dhid);
		NVBWLogger.info("bearbeiter: " + bearbeiter);
		NVBWLogger.info("kommentar: " + kommentar);
		NVBWLogger.info("release: " + release);
		NVBWLogger.info("mayorversion: " + mayorversion);
		NVBWLogger.info("minorversion: " + minorversion);

		String selectModellSql = "SELECT release, mayorversion, minorversion FROM objektmodell "
			+ "WHERE dhid = ? "
			+ "ORDER BY release DESC, mayorversion DESC, minorversion DESC LIMIT 1;";

		String insertModellSql = "INSERT INTO objektmodell (dhid, release, mayorversion, minorversion, "
			+ "benutzer, kommentar, content) VALUES(?, ?, ?, ?, ?, ?, ?::jsonb);";

		
		PreparedStatement selectModellStmt;
		PreparedStatement insertModellStmt;
		try {
/*			selectModellStmt = bfrkConn.prepareStatement(selectModellSql);
			int stmtindex = 1;
			selectModellStmt.setString(stmtindex++, dhid);
			System.out.println("Objektmodell query: " + selectModellStmt.toString() + "===");

			ResultSet selectModellRS = selectModellStmt.executeQuery();

			while(selectModellRS.next()) {
				release = selectModellRS.getInt("release");
				mayorversion = selectModellRS.getInt("mayorversion");
				minorversion = selectModellRS.getInt("minorversion");
				minorversion++;
			}
			selectModellRS.close();
			selectModellStmt.close();
*/
			insertModellStmt = bfrkConn.prepareStatement(insertModellSql);
			int stmtindex = 1;
			insertModellStmt.setString(stmtindex++, dhid);
			insertModellStmt.setInt(stmtindex++, release);
			insertModellStmt.setInt(stmtindex++, mayorversion);
			insertModellStmt.setInt(stmtindex++, minorversion);
			insertModellStmt.setString(stmtindex++, bearbeiter);
			insertModellStmt.setString(stmtindex++, kommentar);
			insertModellStmt.setString(stmtindex++, contentJson.toString());
			System.out.println("Objektmodell store: " + insertModellStmt.toString() + "===");

			insertModellStmt.execute();

			insertModellStmt.close();
		} catch (SQLException e) {
			System.out.println("SQLException::: " + e.toString());
			String fehlertext = "DB-Fehler aufgetreten, bitte den Administrtaor benachrichtigen";
			ergebnisJsonObject.put("status", "fehler");
			ergebnisJsonObject.put("fehlertext", fehlertext);
			response.getWriter().append(ergebnisJsonObject.toString());
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}
}
