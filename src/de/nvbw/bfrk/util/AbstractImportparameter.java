package de.nvbw.bfrk.util;

import java.nio.charset.Charset;

/*
	V1.1, 16.02.2011, Dietmar Seifert
		*	Anpassung der Tabellen für allgemeine Nutzung; Ergänzung Tabellen land und stadt
		*	Datensätze in land und stadt werden ergänzt, wenn noch nicht vorhanden

	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Augsburg" "09761" "Stadtvermessungsamt-Hausnummern.txt"
	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Kaufbeuren" "09762" "Stadt-Kaufbeuren-Hausnummern.txt"
*/

import java.util.HashMap;
import java.util.Map;



public abstract class AbstractImportparameter {
	public enum HEADERFIELD {DIVATeilnetz, Landkreis, Gemeinde , Ortsteil , 
		HalteName, HalteLangname, HalteDHID, HalteLon, HalteLat, 
		BereichName, BereichDHID, BereichLon, BereichLat,
		SteigName, SteigDHID, SteigLon, SteigLat, DbID }

	private String countrycode = "";
	private String municipality = null;
	private String municipalityRef = null;
	
	/**
	 * SRID Id of coordinates System
	 */
	protected String coordinatesSourceSrid = "";


	 /**
	 * filename of official housenumber list to import
	 */
	private String filename = "";

	/**
	 * File format of import filename, 
	 * @see https://docs.oracle.com/javase/7/docs/api/java/nio/charset/StandardCharsets.html
	 */
	private Charset filenameCharsetname = null;

	private String fieldseparator = "";

	private String housenumberadditionseparator = "";
	private String housenumberadditionseparator2 = "-";

	/**
	 * Should the sub areas of a municipality are evaluated?
	 * 
	 */
	private boolean subareaActive = false;

	/**
	 * convert Street name to Upper-Lower case, if completely in upper or lower
	 */
	private boolean convertStreetToUpperLower = false;

	/**
	 * collect all housenumbers, as found in input file. Later, the housenumbers will be stored from this structure to DB. 
	 */
	private Map<HEADERFIELD, String> headerfields = new HashMap<>();

	/**
	 * Structure to identify columns, where column values will be stored as osm-values with osm-key as name of Map value
	 */
	private Map<String, String> customheaderfields = new HashMap<>();
	
	/**
	 * List of municipality ids, when import file contains only references to municipalities
	 * 
	 */
	private Map<String, String> municipalityIDList = new HashMap<>();
	/**
	 * List of subarea municipality ids, when import file contains only references to subareas
	 */
	private Map<String, String> subareaMunicipalityIDList = new HashMap<>();
	/**
	 * List of street ids, when import file contains only references to streets, not the names
	 */
	private Map<String, String> streetIDList = new HashMap<>();

	

		// storage of streetnames and their internal DB id. If a street is missing in DB, it will be inserted,
		// before the insert of the housenumbers at the streets will be inserted
	static HashMap<String, Integer> street_idlist = new HashMap<String, Integer>();
	
	
	
	/**
	 * @return the countrycode
	 */
	public String getCountrycode() {
		return countrycode;
	}


	public String getMunicipality() {
		return this.municipality;
	}
	
	public String getMunicipalityRef() {
		return this.municipalityRef;
	}
	
	public String getFieldSeparator() {
		return fieldseparator;
	}

	public String getHousenumberFieldseparator() {
		return this.housenumberadditionseparator;
	}

	public String getHousenumberFieldseparator2() {
		return this.housenumberadditionseparator2;
	}

	public String getImportfile() {
		return this.filename;
	}

	public Charset getImportfileFormat() {
		return this.filenameCharsetname;
	}
	
	/**
	 * @return the name of municipality, if input parameter key is known. Otherwise, it returns null
	 */
	public String getMunicipalityIDListEntry(String key) {
		return this.municipalityIDList.get(key);
	}

	public String getSourceCoordinateSystem() {
		return this.coordinatesSourceSrid;
	}

	/**
	 * @return the streetIDList
	 */
	public String getStreetIDListEntry(String key) {
		return this.streetIDList.get(key);
	}

	/**
	 * @return the name of subarea of municipality, if input parameter key is known. Otherwise, it returns null
	 */
	public String getSubareaMunicipalityIDListEntry(String key) {
		return this.subareaMunicipalityIDList.get(key);
	}

	/**
	 * @return the subareaActive
	 */
	public boolean isSubareaActive() {
		return subareaActive;
	}

	public boolean convertStreetToUpperLower() {
		return this.convertStreetToUpperLower;
	}

	
	
	/**
	 * @param countrycode the countrycode to set
	 */
	public void setCountrycode( String countrycode ) {
		this.countrycode = countrycode;
	}

	public void setMunicipality( String municipality ) {
		this.municipality = municipality;
	}
	
	public void setMunicipalityRef( String municipalityRef ) {
		this.municipalityRef = municipalityRef;
	}
	
	public void setFieldSeparator(String separator) {
		this.fieldseparator = separator;
	}

	public void setSourceCoordinateSystem(String sourcesrid) {
		this.coordinatesSourceSrid = sourcesrid;
	}

	public void setImportfile(String filename, Charset charset) {
		this.filename = filename;
		this.filenameCharsetname = charset;
	}

	public void setHousenumberFieldseparators(String separator1, String separator2) {
		this.housenumberadditionseparator = separator1;
		this.housenumberadditionseparator2 = separator2;
	}

	/**
	 * store information, what field "column" contains
	 * @param headerfields the headerfields to set
	 */
	public void setHeaderfield(HEADERFIELD field, String column) {
		this.headerfields.put(field, column);
	}

	public void setCustomHeaderfield(String osmKey, String column) {
		this.customheaderfields.put(column, osmKey);
	}


	
	/**
	 * @return the column or name for field as text
	 * "" will be returned, if field name wasn't found
	 */
	public String getHeaderfieldColumn(HEADERFIELD field) {
		if( field == null )
			return "";

		if(headerfields.containsKey(field))
			return headerfields.get(field);

		return "";
	}

	
	public boolean hasField(HEADERFIELD field) {
		if(getHeaderfieldColumn(field).equals(""))
			return false;
		else
			return true;
	}


	public Map<String, String> getCustomHeaderfields() {
		return this.customheaderfields;
	}
	

	public String printHeaderfields() {
		String output = "";
		
		for (Map.Entry<HEADERFIELD, String> fieldentry : headerfields.entrySet()) {
			HEADERFIELD fieldName = fieldentry.getKey();
			String fieldColumn = fieldentry.getValue();
			if (!output.equals(""))
				output += ", ";
			output += fieldColumn + "=" + fieldName;
		}
		return output;
	}
}
