package de.nvbw.bilddb.csvreader;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bilddb.model.BildDBImage;
import de.nvbw.imports.CsvReader;


public class Image {
	private static final Logger LOG = NVBWLogger.getLogger(Image.class);

	public static Map<String, BildDBImage> read(String filename) {
		Map<String, BildDBImage> imageBildDBList = new HashMap<>();

		CsvReader csvreader = new CsvReader();
		List<Map<String, String>> dateizeilen = csvreader.readData(filename);

		String idString = "";
		String name = "";
		String dhid = "";
		double lon = 0.0;
		double lat = 0.0;
		int imstid = 0;
		String erfasser = "";
		Date zeitstempel = null;
		Map<String, String> zeile = null;

        for (Map<String, String> stringStringMap : dateizeilen) {
            zeile = stringStringMap;


            idString = "";
            name = "";
            dhid = "";
            lon = 0.0;
            lat = 0.0;
            imstid = 0;
            zeitstempel = null;
            erfasser = "";

			/*
				IM_ID	
				IM_NAME	
							IM_TEXT	
				IM_DATETIME_UTC	
				IM_CREATOR	
							IM_OWNER	
							IM_WEIGHT	
				IM_OBJECT_ID	
							IM_OBJECT_NAME	
							IM_OBJECT_POINT	
				IM_LATITUDE	
				IM_LONGITUDE	
							IM_HEADING	
							IM_LOCATION	
							IM_STOP	
							IM_STOP_AREA	
							IM_STOP_POINT	
							IM_WIDTH	
							IM_HEIGHT	
							IM_COLORS	
							IM_FILE_SIZE	
				IM_ST_ID	
							IM_OI_ID	
							created_at	
							updated_on
			*/
            String spaltenname = "";

            try {
                spaltenname = "IM_ID";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty())
                    idString = zeile.get(spaltenname);

                spaltenname = "IM_ST_ID";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty()) {
                    imstid = Integer.parseInt(zeile.get(spaltenname));
                }

                spaltenname = "IM_DATETIME_UTC";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty()) {
                    try {
                        zeitstempel = BildDBImage.Image_datetime_formatter.parse(zeile.get(spaltenname));
                    } catch (ParseException e) {
                        LOG.warning("Klasse Image: beim einlesen des Zeitstempels ist ein Dateformatter.parse-Fehler aufgetreten, "
                                + ", der String ist ===" + zeile.get(spaltenname) + "===, Details folgen: " + e.toString());
                    }
                }

                spaltenname = "IM_CREATOR";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty())
                    erfasser = zeile.get(spaltenname);

                spaltenname = "IM_NAME";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty())
                    name = zeile.get(spaltenname);

                spaltenname = "IM_OBJECT_ID";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty())
                    dhid = zeile.get(spaltenname);

                spaltenname = "IM_LATITUDE";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty())
                    lat = Double.parseDouble(zeile.get(spaltenname));

                spaltenname = "IM_LONGITUDE";
                if (zeile.containsKey(spaltenname) && !zeile.get(spaltenname).isEmpty())
                    lon = Double.parseDouble(zeile.get(spaltenname));
            } catch (NumberFormatException nferror) {
                LOG.severe("Fehler beim Versuch, in der Spalte '" + spaltenname
                        + "' eine Zahl zu erkennen, Inhalt war ===" + zeile.get(spaltenname) + "===");
            } catch (IllegalArgumentException illegalerror) {
                LOG.severe("Fehler beim Versuch, in der Spalte '" + spaltenname
                        + "' einen Enum-Wert zu erkennen, Inhalt war ===" + zeile.get(spaltenname) + "===");
            }

            BildDBImage image = new BildDBImage(idString, name, dhid, lon, lat);
            if (imstid != 0)
                image.setIMSTID(imstid);
            if (zeitstempel != null)
                image.setZeitstempel(zeitstempel);
            if (!erfasser.isEmpty())
                image.setErfasser(erfasser);

            imageBildDBList.put(idString, image);
            LOG.finer("ImageBildDB-Eintrag: " + image.toString());

        }
		return imageBildDBList;
	}
}
