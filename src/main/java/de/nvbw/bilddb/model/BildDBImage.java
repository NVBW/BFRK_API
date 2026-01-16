package de.nvbw.bilddb.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BildDBImage {
	public static final DateFormat Image_datetime_formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	private String idString = "";
	private String name = "";
	private String dhid = "";
	private double lon = 0.0;
	private double lat = 0.0;
	private Date zeitstempel = null;
	private String erfasser = "";
	private int imstid = 0;


	public BildDBImage ( String id, String titel, String dhid,
		double lon, double lat) {
		this.idString = id;
		this.name = titel;
		this.dhid = dhid;
		this.lon = lon;
		this.lat = lat;
	}

	public void setZeitstempel(Date zeitstempel) {
		this.zeitstempel = zeitstempel;
	}

	public void setErfasser(String erfasser) {
		this.erfasser = erfasser;
	}

	public void setIMSTID(int id) {
		this.imstid = id;
	}

	public String getIDString() {
		return this.idString;
	}

	public String getBildname() {
		return this.idString;
	}

	public String getTitel() {
		return this.name;
	}

	public String getDHID() {
		return this.dhid;
	}
	
	public double getLon() {
		return this.lon;
	}

	public double getLat() {
		return this.lat;
	}

	public String getErfasser() {
		return this.erfasser;
	}

	public int getIMSTID() {
		return this.imstid;
	}

	public Date getZeitstempel() {
		return this.zeitstempel;
	}


	@Override
	public String toString() {
		String output = "";

		output = "ID: " + this.idString + ", Titel: " + this.name
			+ ", DHID: " + this.dhid
			+ ", lon: " + this.lon + ", lat: " + this.lat
			+ ", Zeitstempel: " + Image_datetime_formatter.format(this.zeitstempel)
			+ ", Erfasser: " + this.erfasser
			+ ", IM_ST_ID: " + this.imstid;
		return output;
	}
}
