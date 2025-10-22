package de.nvbw.bilddb.imports;


import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bilddb.csvreader.Image;
import de.nvbw.bilddb.model.BildDBImage;


/**
 * BildDB Reader für das Format CSV
 * @author SEI
 *
 */
public class BildDBCsvReader {

	
	private Iterator<Entry<String, BildDBImage>> bildIterator = null;

	private String filename = "";

	private Map<String, BildDBImage> imageList = null;

	
	public BildDBCsvReader() {

	}
	
	/**
	 * Einlesen der Mentz Bild-DB-V2 csv-Metadatei in interne Datenstruktur, 
	 * die anschließend über getErsteBildmetainfo und getNaechsteBildmetainfo abgerufen werden kann
	 * @param dateiname
	 */
	public void execute(String dateiname) {

		this.filename = dateiname;

		try {
			Date starttime = new Date();

			imageList = new HashMap<>();
			
			Date endtime = new Date();
			NVBWLogger.info("Nach Speicherallokierung, dauerte " + (endtime.getTime() - starttime.getTime())/1000 + " sek.");
			
		} catch (Exception e) {
			NVBWLogger.severe("in Klasse BildDBCsvReader, Methode execute Exception aufgetreten, Details: " + e.toString());
			return;
		}
		


		if(new File(dateiname).exists())
			imageList = Image.read(dateiname);

		NVBWLogger.info("Anzahl Bilder Metadaten: " + imageList.size());
	}




	public String getFilename() {
		return this.filename;
	}
	

	public BildDBImage getBildMetadaten(String bildid) {
		if((imageList == null) || (imageList.size() == 0))
			return null;
		
		return imageList.get(bildid);
	}

	public BildDBImage getErsteBildmetainfo() {
		BildDBImage actstop = null;

		if(bildIterator == null) {
			bildIterator = imageList.entrySet().iterator();
		}

		if(bildIterator.hasNext()) {
			actstop = bildIterator.next().getValue();
		}
		return actstop;
	}

	public BildDBImage getNaechsteBildmetainfo() {
		BildDBImage actstop = null;

		if(bildIterator == null) {
			NVBWLogger.severe("Methode getNextStop aufgerufen, obwohl zuerst mit getFirstStop() begonnen werden muß");
			return null;
		}

		if(bildIterator.hasNext()) {
			actstop = bildIterator.next().getValue();
		}
		return actstop;
	}

	
	@Override
	public String toString() {
		String output = "";
		
		output += ", Dateiname: " + this.filename;

		if ( this.imageList != null )
			output += ",  Bild Metadaten: " + this.imageList.size() + "\n";
		
		return output;
	}
}