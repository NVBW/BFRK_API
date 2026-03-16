package de.nvbw.imports;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.nvbw.base.NVBWLogger;


public class CsvReader {
	private static final Logger LOG = NVBWLogger.getLogger(CsvReader.class);

	private Map<Integer, String> headercolumns = new HashMap<>();

	private static String[] removeAt(int k, String[] arr) {
	    final int L = arr.length;
	    String[] ret = new String[L - 1];
	    System.arraycopy(arr, 0, ret, 0, k);
	    System.arraycopy(arr, k + 1, ret, k, L - k - 1);
	    return ret;
	}

	public List<Map<String, String>> readData(String filename) {
		return this.readData(filename, StandardCharsets.UTF_8);
	}

	public List<Map<String, String>> readData(String filename, Charset zeichensatz) {
		List<Map<String, String>> zeilen = new ArrayList<>();
		try {
			BufferedReader  dateireader = new BufferedReader(new InputStreamReader(
				new FileInputStream(new File(filename)), 
				zeichensatz));

			String splitsequence = ";";

			int filelineno = 0;
			String line = "";
			while ((line = dateireader.readLine()) != null) {
				filelineno++;
				if ( ( filelineno % 1000 ) == 0 )
					LOG.fine("reading File line #" + filelineno);
				if(line.equals(""))
					continue;
				if(line.indexOf("\"") != -1) {
					boolean anfuerzeichenOk = false;
					while(!anfuerzeichenOk) {
						int anzanfuehrzeichen = 0;
						int startpos = 0;
						while(line.indexOf("\"", startpos) != -1) {
							anzanfuehrzeichen++;
							startpos = line.indexOf("\"", startpos) + 1;
						}
						if((anzanfuehrzeichen % 2) == 1) {
							line += "\n" + dateireader.readLine();
							filelineno++;
						} else
							anfuerzeichenOk = true;
					}
				}
				if(filelineno == 1) {
					// ignore UTF-8 BOM character, if present
					if (line.codePointAt(0) == 65279)
						line = line.substring(1);
					String[] columns = line.split(splitsequence, -1);
					for(int index = 0; index < columns.length; index++) {
						int lfdnr = 0;
						if ( columns[index].startsWith("\"") && columns[index].endsWith("\"") ) {
							columns[index] = columns[index].substring(1);
							columns[index] = columns[index].substring(0, columns[index].length() - 1 );
						} else if ( columns[index].startsWith("\"") ) {
							columns[index] = columns[index].substring(1);
							lfdnr = 1;
							while ( !columns[index+lfdnr].endsWith("\"")) {
								columns[index] += splitsequence + columns[index+lfdnr];
								lfdnr++;
							}
							columns[index] += splitsequence + columns[index+lfdnr];
							columns[index] = columns[index].substring(0, columns[index].length() - 1 );
							for( int i2 = 0; i2 < lfdnr; i2++ ) {
								//System.out.println("entferne Spalte " + (index + 1) + " ..");
								//System.out.println("  inhaltlich ===" + columns[index + 1] + "===");
								columns = removeAt((index + 1), columns);
							}
							lfdnr = 0;
						}
						headercolumns.put(index, columns[index]);
					}
					continue;
				}
				
				String[] columns = null;
				if ( line.indexOf(splitsequence) == -1 ) {
					LOG.severe("ERROR: column separator not '" + splitsequence + "'");
					dateireader.close();
					return zeilen;
				}
				columns = line.split(splitsequence, -1);
				if ( ( line.indexOf("\"" + splitsequence) != -1 ) || ( line.indexOf(splitsequence + "\"") != -1 ) ) {
					int lfdnr = 0;
					for ( int index = 0; index < columns.length; index++ ) {
						if ( columns[index].startsWith("\"") && columns[index].endsWith("\"") ) {
							columns[index] = columns[index].substring(1);
							columns[index] = columns[index].substring(0, columns[index].length() - 1 );
						} else if ( columns[index].startsWith("\"") ) {
							columns[index] = columns[index].substring(1);
							lfdnr = 1;
							while ( !columns[index+lfdnr].endsWith("\"")) {
								columns[index] += splitsequence + columns[index+lfdnr];
								lfdnr++;
							}
							columns[index] += splitsequence + columns[index+lfdnr];
							columns[index] = columns[index].substring(0, columns[index].length() - 1 );
							for( int i2 = 0; i2 < lfdnr; i2++ ) {
								//System.out.println("entferne Spalte " + (index + 1) + " ..");
								//System.out.println("  inhaltlich ===" + columns[index + 1] + "===");
								columns = removeAt((index + 1), columns);
							}
							lfdnr = 0;
						}
						
					}
				}

				Map<String, String> zeile = new HashMap<>();
				for ( int index = 0; index < columns.length; index++ ) {
					String spaltenname = headercolumns.get(index);
					zeile.put(spaltenname, columns[index]);
				}
				zeilen.add(zeile);
			} // end of loop over all lines of trip-file
			LOG.info("Read File Number of lines: " + filelineno + ", File " + filename);
			dateireader.close();
		} catch (IOException ioe) {
			LOG.severe("Fehler bei Datei lesen");
			LOG.severe(ioe.toString());
		}
		return zeilen;
	}
}
