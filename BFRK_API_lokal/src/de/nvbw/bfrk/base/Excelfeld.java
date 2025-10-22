package de.nvbw.bfrk.base;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.poi.ss.usermodel.CellStyle;

public class Excelfeld {
	public enum Datentyp {Standard, Zahl, Zahl_Integer, Zahl_Nachkomma1, Zahl_Nachkomma2, 
		Text, Datum, Hyperlink_Bild, Hyperlink_Karte, Josm_Datenuebertragung, 
		Josm_Umgebungladen, Zahl_Double, Inaktiv};

	private Datentyp typ = Datentyp.Standard;
	private String inhalt = "";
	private CellStyle style = null;

	private DateFormat datum_de_formatter = new SimpleDateFormat("dd.MM.yyyy");

	public Excelfeld(Datentyp typ, String text) {
		this(typ, text, null);
	}
	
	public Excelfeld(Datentyp typ, String text, CellStyle style) {
		this.typ = typ;
		this.inhalt = text;
		if(style != null)
			this.style = style;
	}

	public Excelfeld(Datentyp typ, int wert, CellStyle style) {
		this.typ = typ;
		this.inhalt = "" + wert;
		if(style != null)
			this.style = style;
	}

	public Excelfeld(Datentyp typ, double wert, CellStyle style) {
		this.typ = typ;
		this.inhalt = "" + wert;
		if(style != null)
			this.style = style;
	}

	public Excelfeld(Datentyp typ, int wert) {
		this.typ = typ;
		this.inhalt = "" + wert;
	}

	public Excelfeld(Datentyp typ, double wert) {
		this.typ = typ;
		this.inhalt = "" + wert;
	}

	public Excelfeld(Datentyp typ, Date datum) {
		this.typ = typ;
		this.inhalt = datum_de_formatter.format(datum);
	}

	public void setInhalt(String text) {
		this.inhalt = text;
	}

	public void setTyp(Datentyp typ) {
		this.typ = typ;
	}

	public void setStyle(CellStyle style) {
		this.style = style;
	}

	public Datentyp getFeldtyp() {
		return this.typ;
	}

	public double getZahlWert() {
		return Double.parseDouble(this.inhalt);
	}

	public String getTextWert() {
		return this.inhalt;
	}

	public Date getDatumWert() {
		try {
			return datum_de_formatter.parse(this.inhalt);
		} catch (ParseException e) {
			return null;
		}
	}

	public String getHyperlink() {
		return this.inhalt;
	}

	public CellStyle getStyle() {
		return this.style;
	}

	
	@Override
	public String toString() {
		String output = "";
		
		output = "Wert: " + this.inhalt 
			+ ",   Typ: " + this.typ;
		if(this.style != null)
			output += ",   Style: " + this.style.toString();
		
		return output;
	}
}
