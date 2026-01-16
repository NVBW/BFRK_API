package de.nvbw.bfrk.util;



/*
	V1.1, 16.02.2011, Dietmar Seifert
		*	Anpassung der Tabellen für allgemeine Nutzung; Ergänzung Tabellen land und stadt
		*	Datensätze in land und stadt werden ergänzt, wenn noch nicht vorhanden

	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Augsburg" "09761" "Stadtvermessungsamt-Hausnummern.txt"
	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Kaufbeuren" "09762" "Stadt-Kaufbeuren-Hausnummern.txt"
*/


public class CsvImportparameter extends AbstractImportparameter {


	/**
	 * convert Street name to Upper-Lower case, if completely in upper or lower
	 */
	private boolean convertStreetToUpperLower = false;
	
	
	public boolean convertStreetToUpperLower() {
		return this.convertStreetToUpperLower;
	}

	public void convertStreetToUpperLower(boolean convert) {
		this.convertStreetToUpperLower = convert;
	}

}
