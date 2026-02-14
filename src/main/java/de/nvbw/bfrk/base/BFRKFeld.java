package de.nvbw.bfrk.base;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

import de.nvbw.base.NVBWLogger;
import de.nvbw.bfrk.util.OpenDataCSVExportwriter;

public class BFRKFeld {
	private static final Logger LOG = NVBWLogger.getLogger(BFRKFeld.class);

	static private DateFormat date_de_formatter = new SimpleDateFormat("dd.MM.yyyy");

	public enum Datentyp {Boolean, Numeric, String, Date, unset};
	public enum Name {

		// primäre Felder Haltestelle
		HST_DHID (Datentyp.String),
		HST_Soll_Steig (Datentyp.String),
		HST_Name (Datentyp.String),
		HST_Beschreibung (Datentyp.String),
		HST_Gemeinde (Datentyp.String),
		HST_Ortsteil (Datentyp.String),
		HST_Importdateipfad (Datentyp.String),
		HST_Importdateiname (Datentyp.String),
		HST_Datenlieferant (Datentyp.String),
		HST_Soll_Lon (Datentyp.Numeric),
		HST_Soll_Lat (Datentyp.Numeric),
		Objektart (Datentyp.String),
		ObjektId (Datentyp.Numeric),
			// primäre Felder Haltesteig
		STG_DHID (Datentyp.String),
		STG_Soll_Steig (Datentyp.String),
		STG_Name (Datentyp.String),
		STG_Beschreibung (Datentyp.String),
		STG_Soll_Lon (Datentyp.Numeric),
		STG_Soll_Lat (Datentyp.Numeric),

			// 3.1.1 Telefonnummer Ansprechpartner
		HST_Ansprechpartner_Name_D1010 (Datentyp.String),
		HST_Ansprechpartner_Telefonnummer_D1011 (Datentyp.String),

			// 3.1.4 Fahrkartenautomat
		HST_Fahrkartenautomat_D1040 (Datentyp.Boolean),
		STG_Fahrkartenautomat_D1040 (Datentyp.Boolean),
			// 3.1.8 Haltestelle niveaugleich
//TODO
			// 3.1.9 Gepäckaufbewahrung
		HST_Gepaeckaufbewahrung_Vorhanden_D1090 (Datentyp.Boolean),
			// 3.1.10 Gepäcktransport
		HST_Gepaecktransport_Vorhanden_D1100 (Datentyp.Boolean),
			// 3.1.11 Informationssäule / Notrufsäule
//TODO noch zu klären, ob Haltestelle, Steig oder Verbindungselement
		HST_InfoNotrufsaeule (Datentyp.String),
		STG_InfoNotrufsaeule (Datentyp.String),

		
			// 3.1.12 Wartengelegenheit mit Sitzplatz
		HST_WartegelegenheitSitzplatz_Vorhanden_D1120 (Datentyp.Boolean),
		STG_WartegelegenheitSitzplatz_Vorhanden_D1120 (Datentyp.Boolean),
			// 3.1.13 Fahrplananzeigetafeln
		HST_DynFahrplananzeigetafel_Vorhanden_D1130 (Datentyp.Boolean),
		HST_DynFahrplananzeigetafel_Akustisch_D1131 (Datentyp.Boolean),
		STG_DynFahrplananzeigetafel_Vorhanden_D1130 (Datentyp.Boolean),
		STG_DynFahrplananzeigetafel_Akustisch_D1131 (Datentyp.Boolean),
			// 3.1.14 Dynamische Zugziel- / Fahrtzielanzeiger
		HST_DynFahrtzielanzeiger_Vorhanden_D1140 (Datentyp.Boolean),
		HST_DynFahrtzielanzeiger_Akustisch_D1141 (Datentyp.Boolean),
		STG_DynFahrtzielanzeiger_Vorhanden_D1140 (Datentyp.Boolean),
		STG_DynFahrtzielanzeiger_Akustisch_D1141 (Datentyp.Boolean),
			// 3.1.15 Ansagen
		HST_Ansagen_Vorhanden_D1150 (Datentyp.Boolean),
		STG_Ansagen_Vorhanden_D1150 (Datentyp.Boolean),
			// 3.1.17 Bordstein- / Bussteig- / Bahnsteighöhe
		STG_Steighoehe_cm_D1170 (Datentyp.Numeric),
		STG_Bahnsteig_Hoehe_ueberGleis_cm_D1170 (Datentyp.Numeric),
			// 3.1.18 Breite des Bahnsteigs und des Bussteigs
		STG_Steigbreite_cm_D1180 (Datentyp.Numeric),
			// 3.1.19 Abstand zur Gleismitte
		STG_Bahnsteig_Abstand_Kante_zurGleismitte_cm_D1190 (Datentyp.Numeric),
		// 3.1.20 Bordsteinart
		STG_Hochbord_Spurfuehrung_D1200 (Datentyp.Boolean),
		STG_Hochbord_SpurfuehrungDoppelteHohlkehle_D1201 (Datentyp.Boolean),
		STG_Hochbord_ohneSpurfuehrung_D1202 (Datentyp.Boolean),
		STG_Hochbord_KombibordSpurfuehrung_D1203 (Datentyp.Boolean),
		STG_Hochbord_vorhanden (Datentyp.Boolean),
		STG_Hochbord_Art (Datentyp.String),

			// 3.2.5 Unbefestigter Bodenbelag
		VERB_BodenbelagUnbefestigt_Vorhanden_D2050 (Datentyp.Boolean),
			// 3.2.6 Benennung Knotenpunkte und Wege
//TODO
			// 3.2.7 Bodenindikatoren
		STG_Bodenindikator_Vorhanden_D2070 (Datentyp.Boolean),
		STG_Bodenindikator_EinstiegUndAuffind_D2071 (Datentyp.Boolean),
		STG_Bodenindikator_Leitstreifen_D2072 (Datentyp.Boolean),
		STG_Bodenindikator_Einstiegsbereich (Datentyp.Boolean),
		STG_Bodenindikator_Auffindestreifen (Datentyp.Boolean),

		// 3.2.10 Stufe
		@Deprecated
		OBJ_Stufe_Vorhanden_D2100 (Datentyp.Boolean),
		@Deprecated
		OBJ_Stufe_Hoehe_cm_D2101 (Datentyp.Numeric),
			// 3.2.14 Einstieg in der Straßenmitte
		STG_EinstiegStrassenmitte_D2140 (Datentyp.Boolean),
		
			// weitere, Baden-Württemberg-weite Merkmale

		STG_IstPos_Lon (Datentyp.Numeric),
		STG_IstPos_Lat (Datentyp.Numeric),
		STG_Laengsneigung (Datentyp.Numeric),
		STG_Querneigung (Datentyp.Numeric),
		STG_Bodenbelag_Unbefestigt_Vorhanden_D2050 (Datentyp.Boolean),
		STG_Bodenbelag_Art (Datentyp.String),
		STG_SteigbreiteMinimum (Datentyp.Numeric),
		STG_Steiglaenge (Datentyp.Numeric),
		STG_Tuer2_Laenge_cm (Datentyp.Numeric),
		STG_Tuer2_Breite_cm (Datentyp.Numeric),
		STG_Tuer2_Einstiegsflaeche_ausreichend (Datentyp.Boolean),
		STG_SitzeOderUnterstand_Art (Datentyp.String), 
		STG_Beleuchtung (Datentyp.String),
		STG_Fahrkartenautomat_Lon (Datentyp.Numeric),
		STG_Fahrkartenautomat_Lat (Datentyp.Numeric),
		STG_Fahrkartenautomat_ID (Datentyp.String),
		STG_Ticketvalidator_jn (Datentyp.Boolean),
		HST_Fahrkartenautomat_Lon (Datentyp.Numeric),
		HST_Fahrkartenautomat_Lat (Datentyp.Numeric),
		HST_Fahrkartenautomat_ID (Datentyp.String),
		VERB_Tuer_Tuerart_Sonstige (Datentyp.String),
		VERB_Tuer_Oeffnungsart_Sonstige (Datentyp.String),
		HST_Toilette_Sonstige (Datentyp.String),
		HST_Toilette_Lon (Datentyp.Numeric),
		HST_Toilette_Lat (Datentyp.Numeric),
		HST_Toilette_Level (Datentyp.String),
		HST_sonstige_Bildanzahl (Datentyp.Numeric),
		
			// Zusatzmerkmale
		STG_Unterstand_WaendebisBodennaehe_jn (Datentyp.Boolean),
		STG_Unterstand_Kontrastelemente_jn (Datentyp.Boolean),
		STG_Unterstand_offiziell_jn (Datentyp.Boolean),
		STG_Unterstand_RollstuhlfahrerFreieFlaeche (Datentyp.String),
		STG_ZUS_Unterstand_offiziell_jn (Datentyp.Boolean),  // zum speichern wird STG_Unterstand_offiziell_jn verwendet)

			// Mentz-spezifisches, nicht DELFI-relevantes Merkmal
		STG_MentzQuerungstyp (Datentyp.String),
		
			// =================================================================
			//   weitere Objekte 2021
			// =================================================================

			// Zusatzmerkmale zum Haltesteig
		STG_ZUS_Buchtlaenge_m (Datentyp.Numeric),
		STG_ZUS_Uhr_jn (Datentyp.Boolean),
		STG_ZUS_Beleuchtung_Auswahl (Datentyp.String),

			// Zuwege zum Haltesteig
		STG_Zuweg1_Zugangstyp (Datentyp.String),
		STG_Zuweg1_weniger2Prozent_jn (Datentyp.Boolean),
		STG_Zuweg1_eben_Laenge_cm (Datentyp.Numeric),
		STG_Zuweg1_eben_Breite_cm (Datentyp.Numeric),
		STG_Zuweg1_Rampe_Laenge_cm (Datentyp.Numeric),
		STG_Zuweg1_Rampe_Breite_cm (Datentyp.Numeric),
		STG_Zuweg1_Rampe_Neigung_prozent (Datentyp.Numeric),
		STG_Zuweg1_Rampe_Querneigung_prozent (Datentyp.Numeric),
		STG_Zuweg1_Weg_Stufenhoehe_cm (Datentyp.Numeric),
		STG_Zuweg1_Notiz (Datentyp.String),
		STG_Zuweg2_Zugangstyp (Datentyp.String),
		STG_Zuweg2_weniger2Prozent_jn (Datentyp.Boolean),
		STG_Zuweg2_eben_Laenge_cm (Datentyp.Numeric),
		STG_Zuweg2_eben_Breite_cm (Datentyp.Numeric),
		STG_Zuweg2_Rampe_Laenge_cm (Datentyp.Numeric),
		STG_Zuweg2_Rampe_Breite_cm (Datentyp.Numeric),
		STG_Zuweg2_Rampe_Neigung_prozent (Datentyp.Numeric),
		STG_Zuweg2_Rampe_Querneigung_prozent (Datentyp.Numeric),
		STG_Zuweg2_Weg_Stufenhoehe_cm (Datentyp.Numeric),
		STG_Zuweg2_Notiz (Datentyp.String),
		
		
			// Induktive Höranlage   (3.1.16)
		HST_InduktiveHoeranlage_Vorhanden_D1160 (Datentyp.Boolean),
		HST_InduktiveHoeranlage_Standort_D1161 (Datentyp.String),

		
			// Bahnsteig-spezifische Merkmale 
		

			// Rampe und Hublift (Bahnsteiggebundene Einstiegshilfe)   (3.1.21)
		STG_Einstiegrampe_vorhanden_D1210 (Datentyp.Boolean),
		STG_Einstiegrampe_Laenge_cm_D1211 (Datentyp.Numeric),
		STG_Einstiegrampe_Tragfaehigkeit_kg_D1212 (Datentyp.Numeric),
		STG_EinstiegHublift_vorhanden_D1220 (Datentyp.Boolean),
		STG_EinstiegHublift_Laenge_cm_D1221 (Datentyp.Numeric),
		STG_EinstiegHublift_Tragfaehigkeit_kg_D1222 (Datentyp.Numeric),

		STG_Bahnsteig_Sitzplaetz_Summe (Datentyp.Numeric),
		STG_Bahnsteig_Uhr_vorhanden (Datentyp.Boolean),

		STG_Bahnsteig_Haltepunkt_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt2_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt2_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt3_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt3_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt4_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Haltepunkt4_Lat (Datentyp.Numeric),

		STG_Bahnsteig_Abschnitt_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt2_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt2_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt3_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt3_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt4_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt4_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt5_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt5_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt6_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt6_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt7_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt7_Lat (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt8_Lon (Datentyp.Numeric),
		STG_Bahnsteig_Abschnitt8_Lat (Datentyp.Numeric),

		STG_Element_Entfernung_m (Datentyp.Numeric),		// neu ab SPNV 2024
		STG_Element_Art (Datentyp.String),					// neu ab SPNV 2024
		STG_Element_Lon (Datentyp.Numeric),					// neu ab SPNV 2024
		STG_Element_Lat (Datentyp.Numeric),					// neu ab SPNV 2024
		STG_Element_Koordinatenquelle (Datentyp.String),	// neu ab SPNV 2024
		STG_Element_Nummer (Datentyp.Numeric),				// neu ab SPNV 2024
		
				
		
		// =================================================================
		//   SEV-Haltesteig-spezifische Merkmale
		// =================================================================
	
		SEVSTG_nur_SEV_jn (Datentyp.Boolean),
		
			// Bahnsteig-spezifische Merkmale 
		HST_Defi_vorhanden (Datentyp.Boolean),
		HST_Defi_Lagebeschreibung (Datentyp.String), 
		HST_Mission_vorhanden (Datentyp.Boolean),
		HST_SitzeOderUnterstand_Art (Datentyp.String),
		HST_Unterstand_RollstuhlfahrerFreieFlaeche (Datentyp.String),
		HST_Unterstand_WaendebisBodennaehe_jn (Datentyp.Boolean),
		HST_Unterstand_Kontrastelemente_jn (Datentyp.Boolean),
		HST_Unterstand_offiziell_jn (Datentyp.Boolean),
		HST_ZUS_Unterstand_offiziell_jn (Datentyp.Boolean),
		
		HST_weitereBilder_Foto1_Kommentar (Datentyp.String),
		HST_weitereBilder_Foto2_Kommentar (Datentyp.String),
		HST_weitereBilder_Foto3_Kommentar (Datentyp.String),
		
		OBJ_Beschreibung (Datentyp.String),

		// Aufzug  (3.2.9)
		OBJ_Aufzug_Vorhanden_D2090 (Datentyp.Boolean),
		OBJ_Aufzug_Tuerbreite_cm_D2091 (Datentyp.Numeric),
		OBJ_Aufzug_Laufzeit_sek (Datentyp.Numeric),					// neu ab SPNV 2024
		OBJ_Aufzug_Grundflaeche_cm2_D2092 (Datentyp.Numeric),
		OBJ_Aufzug_Grundflaechenlaenge_cm_D2093 (Datentyp.Numeric),
		OBJ_Aufzug_Grundflaechenbreite_cm_D2094 (Datentyp.Numeric),
		OBJ_Aufzug_Verbindungsfunktion_D2095 (Datentyp.String),
		OBJ_Aufzug_OSMID (Datentyp.String),
		OBJ_Aufzug_FastaID (Datentyp.String),						// neu ab 4.8.2025
		
			// Objekt BuR
		OBJ_BuR_Vorhanden (Datentyp.Boolean),		// HST_RT_BuR
		OBJ_BuR_Lon (Datentyp.Numeric),	// HST_RT_BuR_Lon
		OBJ_BuR_Lat (Datentyp.Numeric),	// HST_RT_BuR_Lat
		OBJ_BuR_Notiz (Datentyp.String),	// HST_RT_BuR_Notiz
		OBJ_BuR_Anlagentyp (Datentyp.String), 		// neu 2020-11-16
		OBJ_BuR_Stellplatzanzahl (Datentyp.Numeric),		// neu 2020-11-16
		OBJ_BuR_Buegelabstand_cm (Datentyp.Numeric),		// neu ab SPNV 2024
		OBJ_BuR_Ueberdacht (Datentyp.Boolean),			// neu 2020-11-16
		OBJ_BuR_WegZurAnlageAnfahrbar (Datentyp.Boolean),			// neu 2020-11-16
		OBJ_BuR_Beleuchtet (Datentyp.Boolean),			// neu 2020-11-16
		OBJ_BuR_Kostenpflichtig (Datentyp.Boolean),			// neu 2020-11-16
		OBJ_BuR_KostenpflichtigNotiz (Datentyp.String),			// neu 2020-11-16
		OBJ_BuR_Hinderniszufahrt_Beschreibung (Datentyp.String),
		OBJ_BuR_OSMID (Datentyp.String),
		
			// Objekt Eingang (nur Tür)   (3.2.3)
		OBJ_Tuer_Vorhanden_D2030 (Datentyp.Boolean),
		OBJ_Tuer_Oeffnungszeiten_D2031 (Datentyp.String), 
		OBJ_Tuer_Art_D2032 (Datentyp.String),
		OBJ_Tuer_Oeffnungsart_D2033 (Datentyp.String),
		OBJ_Tuer_Breite_cm_D2034 (Datentyp.Numeric),
		OBJ_Tuer_OSMID (Datentyp.String),


		
		
			// Objekt Engstelle    3.2.8 Umlaufsperre / Sperrelemente / Engstellen
		OBJ_Engstelle_Vorhanden (Datentyp.Boolean),
		OBJ_Engstelle_Durchgangsbreite_cm_D2080 (Datentyp.Numeric),
		OBJ_Engstelle_Bewegflaeche_cm_D2081 (Datentyp.Numeric),

			// Objekt Fahrkartenautomat
		OBJ_Kartenautomat_Vorhanden (Datentyp.Boolean),
		OBJ_Kartenautomat_ID (Datentyp.String),
		OBJ_Kartenautomat_Lon (Datentyp.Numeric),
		OBJ_Kartenautomat_Lat (Datentyp.Numeric),
		OBJ_Kartenautomat_Entwerter (Datentyp.Boolean),

			// Gleisquerung (3.2.4 Höhengleicher Bahnsteigzugang)
		OBJ_Gleisquerung_Vorhanden_D2040 (Datentyp.Boolean),
		OBJ_Gleisquerung_Verbindungsfunktion (Datentyp.String),
		OBJ_Gleisquerung_Breite_cm (Datentyp.Numeric),

			// Informationsstelle   (3.1.3)
		OBJ_Infostelle_Vorhanden_D1030 (Datentyp.Boolean),
		OBJ_Infostelle_Art_D1031 (Datentyp.String),
		OBJ_Infostelle_Stufenfrei_D1032 (Datentyp.Boolean),
		
			// Leihrad    (BW-Zusatzmerkmal)
		OBJ_Leihradanlage_Vorhanden (Datentyp.Boolean),
		OBJ_Leihradanlage_Art (Datentyp.String),
		OBJ_Leihradanlage_Notizen (Datentyp.String),
		OBJ_Leihradanlage_lon (Datentyp.Numeric),
		OBJ_Leihradanlage_lat (Datentyp.Numeric),
		
			// Parpkplatz (3.1.5)
		OBJ_Parkplatz_oeffentlichVorhanden_D1050 (Datentyp.Boolean),
		OBJ_Parkplatz_Eigentuemer (Datentyp.String),
		OBJ_Parkplatz_Art_D1051 (Datentyp.String),
		OBJ_Parkplatz_Bedingungen_D1052 (Datentyp.String),
		OBJ_Parkplatz_Behindertenplaetze_Lon (Datentyp.Numeric),
		OBJ_Parkplatz_Behindertenplaetze_Lat (Datentyp.Numeric),
		OBJ_Parkplatz_BehindertenplaetzeKapazitaet (Datentyp.Numeric),
		OBJ_Parkplatz_Behindertenplaetze_Laenge_cm (Datentyp.Numeric),	// neu ab SPNV 2024
		OBJ_Parkplatz_Behindertenplaetze_Breite_cm (Datentyp.Numeric),  // neu ab SPNV 2024
		OBJ_Parkplatz_Bauart (Datentyp.String),							// neu ab SPNV 2024
		OBJ_Parkplatz_Orientierung (Datentyp.String),					// neu ab SPNV 2024
		OBJ_Parkplatz_MaxParkdauer_min (Datentyp.Numeric),				// neu ab 3.8.2025 nachträgliche Besetzung durch M+I Parkplatzanreicherung
		OBJ_Parkplatz_offen247_jn (Datentyp.Boolean),					// neu ab 3.8.2025 nachträgliche Besetzung durch M+I Parkplatzanreicherung
		OBJ_Parkplatz_Gebuehrenpflichtig_jn (Datentyp.Boolean),			// neu ab 3.8.2025 nachträgliche Besetzung durch M+I Parkplatzanreicherung
		OBJ_Parkplatz_Tarife (Datentyp.String),							// neu ab 3.8.2025 nachträgliche Besetzung durch M+I Parkplatzanreicherung
		OBJ_Parkplatz_OeffnungszeitenOSM (Datentyp.String),				// neu ab 3.8.2025 nachträgliche Besetzung durch M+I Parkplatzanreicherung
		OBJ_Parkplatz_Kapazitaet (Datentyp.Numeric),
		OBJ_Parkplatz_KapazitaetFrauenplaetze (Datentyp.Numeric),		// neu ab SPNV 2024
		OBJ_Parkplatz_KapazitaetFamilienplaetze (Datentyp.Numeric),		// neu ab SPNV 2024
		OBJ_Parkplatz_Lon (Datentyp.Numeric),
		OBJ_Parkplatz_Lat (Datentyp.Numeric),
		OBJ_Parkplatz_Sonstige (Datentyp.String),
		OBJ_Parkplatz_Foto_Kommentar (Datentyp.String),
		OBJ_Parkplatz_Behindertenplaetze_Laenge_cm_Kommentar (Datentyp.String),	// neu ab SPNV 2024
		OBJ_Parkplatz_Behindertenplaetze_Breite_cm_Kommentar (Datentyp.String),	// neu ab SPNV 2024

/* zu Parkplatz-Öffnungszeiten
 * https://github.com/simonpoole/OpeningHoursParser
 * 
 * import ch.poole.openinghoursparser.*;

public class Main {
    public static void main(String[] args) throws ParseException {
        String openingHours = "Mo-Fr 08:00-18:00";
        Rules rules = OpeningHoursParser.parse(openingHours);
        System.out.println("Gültige Öffnungszeiten: " + rules != null);
    }
}


 * https://wiki.openstreetmap.org/wiki/DE:Key:opening_hours/specification
*/
		

			// Rampe   (3.2.12)
		OBJ_Rampe_Vorhanden_D2120 (Datentyp.Boolean),
		OBJ_Rampe_Verbindungsfunktion_D2121 (Datentyp.String),
		OBJ_Rampe_Laenge_cm_D2122 (Datentyp.Numeric),
		OBJ_Rampe_Breite_cm_D2123 (Datentyp.Numeric),
		OBJ_Rampe_Neigung_prozent_D2124 (Datentyp.Numeric),
		OBJ_Rampe_Querneigung_prozent (Datentyp.Numeric),

			// Rolltreppe   (3.2.13)
		OBJ_Rolltreppe_Vorhanden_D2130 (Datentyp.Boolean), 
		OBJ_Rolltreppe_Verbindungsfunktion_D2131 (Datentyp.String),
		OBJ_Rolltreppe_Fahrtrichtung_D2132 (Datentyp.String),
		OBJ_Rolltreppe_WechselndeRichtung_D2133 (Datentyp.String),
		OBJ_Rolltreppe_Laufzeit_sek_D2134 (Datentyp.Numeric),
		
		// Stations- / Haltestellenplan   (3.2.1)
		OBJ_Stationsplan_Vorhanden_D1030 (Datentyp.Boolean),
		OBJ_Stationsplan_Bodenindikatorart (Datentyp.String),
		OBJ_Stationsplan_Plantaktil (Datentyp.Boolean),
		OBJ_Stationsplan_Fahrplanakustisch (Datentyp.Boolean),
		OBJ_Stationsplan_Lon (Datentyp.Numeric),
		OBJ_Stationsplan_Lat (Datentyp.Numeric),
		
			// Taxi-Stand   (3.1.6)
		OBJ_Taxistand_Vorhanden_D1060 (Datentyp.Boolean),
		OBJ_Taxistand_Lon (Datentyp.Numeric),
		OBJ_Taxistand_Lat (Datentyp.Numeric),

			// Toilette   (3.1.7)
		OBJ_Toilette_Vorhanden_D1070 (Datentyp.Boolean), 
		OBJ_Toilette_Rollstuhltauglich (Datentyp.String),
		OBJ_Toilette_Lokalschluessel_Notiz (Datentyp.String),
		OBJ_Toilette_Oeffnungszeiten_Beschreibung_D1075 (Datentyp.String),

			// 3.2.11 Treppe
		OBJ_Treppe_Vorhanden_D2110 (Datentyp.Boolean),
		OBJ_Treppe_Stufenanzahl_D2113 (Datentyp.Numeric),
		OBJ_Treppe_Stufenhoehe_cm_D2112 (Datentyp.Numeric),
		OBJ_Treppe_Verbindungsfunktion_D2111 (Datentyp.String),
		OBJ_Treppe_Handlauf_links (Datentyp.Boolean),
		OBJ_Treppe_Handlauf_rechts (Datentyp.Boolean),
		OBJ_Treppe_Handlauf_mittig (Datentyp.Boolean),
		OBJ_Treppe_ZielBlinde_links (Datentyp.Boolean),		// ab SPNV 2024
		OBJ_Treppe_ZielBlinde_rechts (Datentyp.Boolean),	// ab SPNV 2024
		OBJ_Treppe_ZielBlinde_mittig (Datentyp.Boolean),	// ab SPNV 2024
		OBJ_Treppe_Radschieberille (Datentyp.Boolean),
		
			// Fahrkartennverkaufsstelle   (3.1.2)
		OBJ_Verkaufsstelle_Vorhanden_D1020 (Datentyp.Boolean),
		OBJ_Verkaufsstelle_Art_D1021 (Datentyp.String),
		OBJ_Verkaufsstelle_stufenfrei_D1022 (Datentyp.Boolean),
		
			// Wege niveaugleich   (3.2.2)
		OBJ_Weg_Vorhanden (Datentyp.Boolean),
		OBJ_Weg_Art (Datentyp.String),
		OBJ_Weg_Laenge_cm_D2020 (Datentyp.Numeric),
		OBJ_Weg_Breite_cm_D2021 (Datentyp.Numeric),
		OBJ_Weg_beleuchtet (Datentyp.Boolean),
		OBJ_Weg_ueberdacht (Datentyp.String),
		OBJ_Weg_Hoehe_cm (Datentyp.Numeric),
		OBJ_Weg_Neigung_prozent (Datentyp.Numeric),
		OBJ_Weg_Querneigung_prozent (Datentyp.Numeric),
		OBJ_Weg_Verbindungsfunktion (Datentyp.String),
		
			// Notiz-Objekt (neu ab 11/2021)
		OBJ_Notiz_Objektart (Datentyp.String),
		OBJ_Notiz_Inhalt (Datentyp.String),
		OBJ_Notiz_weitereBilder_Auswahl (Datentyp.String),
		OBJ_Notiz_weitereBilder1_Foto_Kommentar (Datentyp.String),
		OBJ_Notiz_weitereBilder2_Foto_Kommentar (Datentyp.String),
		OBJ_Notiz_weitereBilder3_Foto_Kommentar (Datentyp.String),


			// Landkreis Reutingen spezifische Merkmale
		STG_Lageinnerorts (Datentyp.String), 
		STG_Steigtyp (Datentyp.String), 
		STG_Abfallbehaelter (Datentyp.Boolean),
		STG_Fahrgastinfo (Datentyp.String),
		STG_Fahrgastinfo_inHoehe_100_160cm (Datentyp.Boolean),
		STG_Fahrgastinfo_freierreichbar_jn (Datentyp.Boolean),
		STG_Notiz (Datentyp.String),
		STG_Linienfahrplan_Foto (Datentyp.String),

			// Fahrzeug-Merkmale aus Delfi-Katalog
		FahrzeugtueroeffnungsArt_D3040 (Datentyp.String),
		FahrzeugtuerBreite_cm_D3041 (Datentyp.Numeric),
		FahrzeugEinstiegsspalt_cm_D3080 (Datentyp.Numeric),
		FahrzeugBreiteSPNVTuerbereich_cm_D3090 (Datentyp.Numeric),
		FahrzeugBodenhoehe_cm_3100 (Datentyp.Numeric),
		FahrzeugHoeheUntersteStufe_cm_3101 (Datentyp.Numeric),
		FahrzeugStufevorhanden_D3110 (Datentyp.Boolean),
		FahrzeugStufenhoehe_cm_D3111 (Datentyp.Numeric),
		FahrzeugAnzahlStufenEinstiegsbereich_D3112 (Datentyp.Numeric),
		FahrzeugRampeVorhanden_D3120 (Datentyp.Boolean),
		FahrzeugRampenlaenge_cm_3121 (Datentyp.Numeric),
		FahrzeugRampenbreite_cm_3122 (Datentyp.Numeric),
		FahrzeugRampentragfaehigkeit_kg_D3123 (Datentyp.Numeric),
		FahrzeugRampeImFahrzeugInnenliegend_D3124 (Datentyp.Boolean),
		FahrzeugHubliftvorhanden_3130 (Datentyp.Boolean),
		FahrzeugHublifttragfaehigkeit_kg_D3133 (Datentyp.Numeric),
		FahrzeugMehrzweckflaeche_vorhanden_D3140 (Datentyp.Boolean),
		
			// Weitere Merkmale, die im Rahmen der Datenvearbeitung anfallen, also nicht erfasst wurden
		KorrekturOsmId (Datentyp.String),
		KorrekturImportdatum (Datentyp.String),
		KorrekturImportperson (Datentyp.String),

			// Merkmale, die nur berechnet werden aus anderen Merkmalen, also nicht selbst erfasst wurden
		Steig_Bodenbelag_barrierefrei_berechnet_D2050 (Datentyp.Numeric),
		Steigbreite_ausreichend_berechnet (Datentyp.Boolean),
		FahrzeugdatenVerfuegbar (Datentyp.Boolean),
		FahrzeugEinstiegshoehe_cm_berechnet_3100 (Datentyp.Numeric),
		FahrzeugRampennutzung_Steigbreite_cm_berechnet_D3125 (Datentyp.Numeric),
		FahrzeugRampennutzung_Steiglaenge_cm_berechnet_D3126 (Datentyp.Numeric),
		FahrzeugRampenneigung_Prozent_berechnet_D3127 (Datentyp.Numeric),
		FahrzeugHubliftnutzung_Steigbreite_cm_berechnet_D3131 (Datentyp.Numeric),
		FahrzeugHubliftnutzung_Steiglaenge_cm_berechnet_D3132 (Datentyp.Numeric),
		FahrzeugStufenfrei_berechnet_D3110 (Datentyp.Boolean),
		
			// DIVA Notizen - nur für strukturierten Zugriff, in DB in festen Feldern in DB Notizobjekt
		DIVANOTIZ_dhid (Datentyp.String),
		DIVANOTIZ_Kreisschluessel (Datentyp.String),
		DIVANOTIZ_Objektart (Datentyp.String),
		DIVANOTIZ_Titel (Datentyp.String),
		DIVANOTIZ_Text (Datentyp.String),
		DIVANOTIZ_Lon (Datentyp.Numeric),
		DIVANOTIZ_Lat (Datentyp.Numeric),

		ZUSATZ_OSM_Id (Datentyp.String),
		ZUSATZ_OSM_Lon (Datentyp.Numeric),
		ZUSATZ_OSM_Lat (Datentyp.Numeric),
		ZUSATZ_INFRAIDTEMP (Datentyp.String),
		ZUSATZ_Erfassungsdatum (Datentyp.Date),
		ZUSATZ_Bild_Url (Datentyp.String),
		ZUSATZ_Bild_Lon (Datentyp.Numeric),
		ZUSATZ_Bild_Lat (Datentyp.Numeric),
		
			// Fotos Steig
		STG_Bodenindikator_Leitstreifen_Foto (Datentyp.String),
		STG_Bodenindikator_Einstiegsbereich_Foto (Datentyp.String),
		STG_Bodenindikator_Auffindestreifen_Foto (Datentyp.String), 
		STG_Hochbord_Foto (Datentyp.String),
		STG_SitzeoderUnterstand_Foto (Datentyp.String),
		STG_Breite_Foto (Datentyp.String),
		STG_Foto (Datentyp.String),
		STG_Engstelle_Foto (Datentyp.String),
		STG_Fahrkartenautomat_Foto (Datentyp.String),
		STG_InfoNotrufsaeule_Foto (Datentyp.String),
		STG_Zuwegung_von_Foto (Datentyp.String),
		STG_Zuwegung_nach_Foto (Datentyp.String),
		STG_Gegenueber_Foto (Datentyp.String),
		STG_Fahrplananzeigetafel_Foto (Datentyp.String),
		STG_dynFahrtzielanzeiger_Foto (Datentyp.String),
		STG_TicketValidator_Foto (Datentyp.String),				// neu ab SPNV 2024
		STG_ZUS_Haltestellenmast_Foto (Datentyp.String),
		STG_ZUS_Unterstandnichtofiziell_Foto (Datentyp.String),
		STG_ZUS_sonstigerSteigtyp_Foto (Datentyp.String),
		STG_ZUS_Uhr_Foto (Datentyp.String),
		STG_Zuweg1_Rampe_Foto (Datentyp.String),
		STG_Zuweg2_Rampe_Foto (Datentyp.String),
		STG_Zuweg1_direkt_Foto (Datentyp.String),
		STG_Zuweg2_direkt_Foto (Datentyp.String),
		STG_Zuweg1_eben_Foto (Datentyp.String),
		STG_Zuweg2_eben_Foto (Datentyp.String),
		STG_Zuweg1_Weg_Stufe_Foto (Datentyp.String),
		STG_Zuweg2_Weg_Stufe_Foto (Datentyp.String),
		STG_Zuweg1_sonstiges_Foto (Datentyp.String),
		STG_Zuweg2_sonstiges_Foto (Datentyp.String),
		STG_Notiz_Foto (Datentyp.String),
		
		STG_Bussteig_von_gegenueber (Datentyp.String),
		
			// Fotos Bahnsteig
		STG_2_Foto (Datentyp.String),
		STG_Uhr_Foto (Datentyp.String),
		STG_Haltepunkt_Foto (Datentyp.String),
		STG_Haltepunkt2_Foto (Datentyp.String),
		STG_Haltepunkt3_Foto (Datentyp.String),
		STG_Haltepunkt4_Foto (Datentyp.String),
		STG_Abschnitt_Foto (Datentyp.String),
		STG_Abschnitt2_Foto (Datentyp.String),
		STG_Abschnitt3_Foto (Datentyp.String),
		STG_Abschnitt4_Foto (Datentyp.String),
		STG_Abschnitt5_Foto (Datentyp.String),
		STG_Abschnitt6_Foto (Datentyp.String),
		STG_Abschnitt7_Foto (Datentyp.String),
		STG_Abschnitt8_Foto (Datentyp.String),
		STG_EinstiegHublift_Foto (Datentyp.String),
		STG_Einstiegrampe_Foto (Datentyp.String),
		STG_Element_Foto (Datentyp.String),				// neu ab SPNV 2024

			// Fotos Haltestelle
		HST_Defi_Foto (Datentyp.String),
		HST_Kontaktdaten_Foto (Datentyp.String), 	// erst bei Mentz-App aufgekommen am 24.03.2022
		HST_Fahrkartenautomat_Foto (Datentyp.String),
//		HST_Tuer_Foto (Datentyp.String),
//		HST_Toilette_Foto_Oeffnungszeiten (Datentyp.String),
		HST_Fahrkartenverkaufsstelle_Foto (Datentyp.String),
//		HST_Infostelle_Foto (Datentyp.String),
//		HST_Taxistand_Foto (Datentyp.String),
//		HST_Parkplatz_Behindertenplaetze_Foto (Datentyp.String),
		HST_Totale_Foto (Datentyp.String),
		HST_weitereBilder_Foto1 (Datentyp.String),
		HST_weitereBilder_Foto2 (Datentyp.String),
		HST_weitereBilder_Foto3 (Datentyp.String),
		HST_Fahrkartenverkaufsstelle_Foto_Oeffnungszeiten (Datentyp.String),
//		HST_Infostelle_Foto_Oeffnungszeiten (Datentyp.String),
		HST_Fahrplananzeigetafel_Foto (Datentyp.String),
		HST_Notiz_Foto (Datentyp.String),
//		HST_dynFahrtzielanzeiger_Foto (Datentyp.String),
		HST_SitzeoderUnterstand_Foto (Datentyp.String),
		HST_SitzeoderUnterstand_Umgebung_Foto (Datentyp.String),
		HST_Mission_Foto (Datentyp.String),
		HST_Mission_Weg_Foto (Datentyp.String),
		HST_Mission_Oeffnungszeiten_Foto (Datentyp.String),
		HST_Gepaeckaufbewahrung_Foto (Datentyp.String), 
		HST_Gepaecktransport_Foto (Datentyp.String),
		HST_InfoNotrufsaeule_Foto (Datentyp.String),
		HST_weitereBilder1_Foto (Datentyp.String),
		HST_weitereBilder2_Foto (Datentyp.String),
		HST_weitereBilder3_Foto (Datentyp.String),
		
			// Fotos Landkreis Reutingen spezifisch für Steig
		STG_RT_sonstigerSteigtyp_Foto (Datentyp.String), 
		STG_Haltestellenmast_Foto (Datentyp.String), 
		STG_Fahrgastinfo_nichtbarrierefrei_Foto (Datentyp.String), 
		STG_RT_Gegenueber_Foto (Datentyp.String),
		STG_RT_FotosonstigerSteigtyp (Datentyp.String),

		OBJ_Aufzug_Foto (Datentyp.String),
		OBJ_Aufzug_ID_Foto (Datentyp.String),
		OBJ_Aufzug_StoerungKontakt_Foto (Datentyp.String),
		OBJ_Aufzug_Bedienelemente_Foto (Datentyp.String),
		OBJ_Aufzug_Ebene1_Foto (Datentyp.String),
		OBJ_Aufzug_Ebene1Weg2_Foto (Datentyp.String), 		// neu ab SPNV 2024
		OBJ_Aufzug_Ebene2_Foto (Datentyp.String),
		OBJ_Aufzug_Ebene2Weg2_Foto (Datentyp.String), 		// neu ab SPNV 2024
		OBJ_Aufzug_Ebene3_Foto (Datentyp.String),
		OBJ_Aufzug_Ebene3Weg2_Foto (Datentyp.String), 		// neu ab SPNV 2024
		
			// Fotos Objekt    BuR
		OBJ_BuR_Foto (Datentyp.String),			// früher HST_RT_BuR_Foto
		OBJ_BuR2_Foto (Datentyp.String),
		OBJ_BuR3_Foto (Datentyp.String),
		OBJ_BuR4_Foto (Datentyp.String),
		OBJ_BuR_Weg_Foto (Datentyp.String),
		OBJ_BuR2_Weg_Foto (Datentyp.String),
		OBJ_BuR3_Weg_Foto (Datentyp.String),
		OBJ_BuR4_Weg_Foto (Datentyp.String),
		OBJ_BuR_Hinderniszufahrt_Foto (Datentyp.String),			// neu 2020-11-16
		OBJ_BuR2_Hinderniszufahrt_Foto (Datentyp.String),
		OBJ_BuR3_Hinderniszufahrt_Foto (Datentyp.String),
		OBJ_BuR_Besonderheiten_Foto (Datentyp.String),			// neu 2020-11-16
		OBJ_BuR2_Besonderheiten_Foto (Datentyp.String),
		OBJ_BuR3_Besonderheiten_Foto (Datentyp.String),

		OBJ_Engstelle_Foto (Datentyp.String),
		OBJ_Engstelle_Weg1_Foto (Datentyp.String),
		OBJ_Engstelle_Weg2_Foto (Datentyp.String),
		
		OBJ_Gleisquerung_Foto (Datentyp.String),
		OBJ_Gleisquerung_Weg1_Foto (Datentyp.String),
		OBJ_Gleisquerung_Weg2_Foto (Datentyp.String),

		OBJ_Infostelle_Foto (Datentyp.String),
		OBJ_Infostelle_Oeffnungszeiten_Foto (Datentyp.String),
		OBJ_Infostelle_EingangzuInfostelle_Foto (Datentyp.String),
		OBJ_Infostelle_WegzuInfostelle_Foto (Datentyp.String),

		OBJ_Kartenautomat_Foto (Datentyp.String),
		OBJ_Kartenautomat_TicketValidator_Foto (Datentyp.String),
		OBJ_Kartenautomat2_Foto (Datentyp.String),
		OBJ_Kartenautomat2_TicketValidator_Foto (Datentyp.String),
		OBJ_Kartenautomat3_Foto (Datentyp.String),
		OBJ_Kartenautomat3_TicketValidator_Foto (Datentyp.String),
		OBJ_Kartenautomat4_Foto (Datentyp.String),
		OBJ_Kartenautomat4_TicketValidator_Foto (Datentyp.String),
		
		OBJ_Leihradanlage_Foto (Datentyp.String),
		OBJ_Leihradanlage_Kontaktdaten_Foto (Datentyp.String),

		// Foto weitere Objekte
		OBJ_Parkplatz_Foto (Datentyp.String),
		OBJ_Parkplatz_Oeffnungszeiten_Foto (Datentyp.String),
		OBJ_Parkplatz_Nutzungsbedingungen_Foto (Datentyp.String),
		OBJ_Parkplatz_Behindertenplaetze_Foto (Datentyp.String),
		OBJ_Parkplatz_WegzuHaltestelle_Foto (Datentyp.String),
		OBJ_Parkplatz_Frauenplaetze_Foto (Datentyp.String),			// ab SPNV 2024
		OBJ_Parkplatz_Familienplaetze_Foto (Datentyp.String),		// ab SPNV 2024

			// Rampe
		OBJ_Rampe_Foto (Datentyp.String),
		OBJ_Rampe_Richtung1_Foto (Datentyp.String),
		OBJ_Rampe_Richtung2_Foto (Datentyp.String),

			// Rolltreppe
		OBJ_Rolltreppe_Foto (Datentyp.String),
		OBJ_Rolltreppe_ID_Foto (Datentyp.String),
		OBJ_Rolltreppe_Richtung1_Foto (Datentyp.String),
		OBJ_Rolltreppe_Richtung2_Foto (Datentyp.String),

			// Stationsplan
		OBJ_Stationsplan_Foto (Datentyp.String),
		OBJ_Stationsplan_2_Foto (Datentyp.String),

		OBJ_Taxistand_Foto (Datentyp.String),
		OBJ_Taxistand_WegzurHaltestelle_Foto (Datentyp.String),
		
		OBJ_Toilette_Foto (Datentyp.String),
		OBJ_Toilette_LokalerSchluessel_Foto (Datentyp.String),
		OBJ_Toilette_Oeffnungszeiten_Foto (Datentyp.String),
		OBJ_Toilette_Nutzungsbedingungen_Foto (Datentyp.String),
		OBJ_Toilette_Umgebung1_Foto (Datentyp.String),
		OBJ_Toilette_Umgebung2_Foto (Datentyp.String),
		
		OBJ_Treppe_Foto (Datentyp.String),
		OBJ_Treppe_Richtung1_Foto (Datentyp.String),
		OBJ_Treppe_Richtung2_Foto (Datentyp.String),
		//OBJ_Treppe_Lage_Foto (Datentyp.String),					gibt es seit 2021 nicht
		OBJ_Treppe_unten_Richtung1_Foto (Datentyp.String),			// neu ab SPNV 2024
		OBJ_Treppe_unten_Richtung2_Foto (Datentyp.String),			// neu ab SPNV 2024
		OBJ_Treppe_oben_Richtung1_Foto (Datentyp.String),			// neu ab SPNV 2024
		OBJ_Treppe_oben_Richtung2_Foto (Datentyp.String),			// neu ab SPNV 2024

		OBJ_Tuer_Foto (Datentyp.String),
		OBJ_Tuer_Richtung1_Foto (Datentyp.String),					// neu ab SPNV 2024
		OBJ_Tuer_Richtung2_Foto (Datentyp.String),					// neu ab SPNV 2024

		OBJ_Verkaufsstelle_Foto (Datentyp.String),
		OBJ_Verkaufsstelle_Oeffnungszeiten_Foto (Datentyp.String),
		OBJ_Verkaufsstelle_EingangzuVerkaufsstelle_Foto (Datentyp.String),
		OBJ_Verkaufsstelle_WegzuVerkaufsstelle_Foto (Datentyp.String),
		
		OBJ_Weg_Foto (Datentyp.String),
		OBJ_Weg_Richtung1_Foto (Datentyp.String),
		OBJ_Weg_Richtung2_Foto (Datentyp.String),

		// Notiz-Objekt (neu ab 11/2021)
		OBJ_Notiz_weitereBilder1_Foto (Datentyp.String),
		OBJ_Notiz_weitereBilder2_Foto (Datentyp.String),
		OBJ_Notiz_weitereBilder3_Foto (Datentyp.String),
		
		//============================================================
		// Merkmale über BFRK hinaus
		BF_DS100 (Datentyp.String),
		BF_DBKategorie (Datentyp.Numeric),
		BF_Name (Datentyp.String),
		BF_Nr (Datentyp.Numeric),
		BF_EIU (Datentyp.String),
		BF_Management (Datentyp.String),
		BF_Bahnsteigoberkante (Datentyp.Numeric),
		BF_Bahnsteiglaenge (Datentyp.Numeric),
		BF_Bahnsteigname (Datentyp.String),
		BF_Gleisname (Datentyp.String),
		;

		private Datentyp typ = Datentyp.unset;
	    private String dbname = null;
	    Name(Datentyp typ, String dbname) {
	    	this.typ = typ;
	    	if((dbname != null) && !dbname.equals("")) {
	    		this.dbname = dbname;
	    	}
	    }
	    Name(Datentyp typ) {
	    	this.typ = typ;
    		this.dbname = null;
	    }

	    public String dbname() {
	    	if(this.dbname != null)
	    		return this.dbname;
	    	else
	    		return this.name();
	    }
	    
	    public Datentyp typ() {
	    	return this.typ;
	    }
	}

	private Datentyp typ = Datentyp.unset;
	private String feldname = "";
	private String feldwert = "";
	
	public BFRKFeld(Datentyp feldtyp, String feldname, String feldwert) {
		this.typ = feldtyp;
		this.feldname = feldname;
		this.feldwert = feldwert;
	}
	public BFRKFeld(Datentyp typ, Name feldname, String feldwert) {
		this.typ = typ;
		this.feldname = feldname.name();
		this.feldwert = feldwert;
	}
	public BFRKFeld(Datentyp typ, Name feldname, Date datum) {
		this.typ = typ;
		this.feldname = feldname.name();
		this.feldwert = "" + datum;
	}
	public String getFeldname() {
		return this.feldname;
	}

	public Datentyp getFeldtyp() {
		return this.typ;
	}

	public boolean getBooleanWert() {
		if(this.typ == Datentyp.Boolean) {
			if(this.feldwert.equals("true"))
				return true;
			else
				return false;
		} else {
			LOG.severe("Programmierfehler in getBooleanWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return false;
	}

	public String getBooleanWertalsJN() {
		if(this.typ == Datentyp.Boolean) {
			if(this.feldwert.equals("true"))
				return OpenDataCSVExportwriter.EXPORT_BOOLEAN_TRUE;
			else
				return OpenDataCSVExportwriter.EXPORT_BOOLEAN_FALSE;
		} else {
			LOG.severe("Programmierfehler in getBooleanWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return "nein";
	}

	public String getBooleanWertalsYN() {
		if(this.typ == Datentyp.Boolean) {
			if(this.feldwert.equals("true"))
				return "yes";
			else
				return "no";
		} else {
			LOG.severe("Programmierfehler in getBooleanWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return "no";
	}

	public String getZahlWertalsText() {
		String returnwert = "0";
		
		if(this.typ == Datentyp.Numeric) {
			if(!this.feldwert.equals("")) {
				try {
					returnwert = this.feldwert;
					if(returnwert.indexOf(".") != -1)
						returnwert = returnwert.replace(".", ",");
					return returnwert;
				}
				catch (NumberFormatException numerror) {
					LOG.severe("Fehler bei Konvertierung nach double-Typ, Original ist " + this.feldwert);
					return returnwert;
				}
			}
		} else {
			LOG.severe("Programmierfehler in getZahlWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return returnwert;
	}

	public double getZahlWert() {
		double returnwert = 0;
		
		if(this.typ == Datentyp.Numeric) {
			if(!this.feldwert.equals("")) {
				try {
					returnwert = Double.parseDouble(this.feldwert);
					return returnwert;
				}
				catch (NumberFormatException numerror) {
					LOG.severe("Fehler bei Konvertierung nach double-Typ, Original ist " + this.feldwert);
					return returnwert;
				}
			}
		} else {
			LOG.severe("Programmierfehler in getZahlWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return returnwert;
	}

	public int getIntWert() {
		int returnwert = 0;
		
		if(this.typ == Datentyp.Numeric) {
			if(!this.feldwert.equals("")) {
				try {
					Double tempwert = Double.parseDouble(this.feldwert);
						//TODO eigentlich müsste der Wert um 0,4 erhöht werden und dann intValue() oder Math.round() nehmen
					tempwert += 0.4;
					returnwert = tempwert.intValue();
					return returnwert;
				}
				catch (NumberFormatException numerror) {
					LOG.severe("Fehler bei Konvertierung nach Integer, Original ist " + this.feldwert);
					return returnwert;
				}
			}
		} else {
			LOG.severe("Programmierfehler in getIntWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return returnwert;
	}
	
	public long getLongWert() {
		long returnwert = 0;
		
		if(this.typ == Datentyp.Numeric) {
			if(!this.feldwert.equals("")) {
				try {
					returnwert = Long.parseLong(this.feldwert);
						//TODO eigentlich müsste der Wert um 0,4 erhöht werden und dann intValue() oder Math.round() nehmen
					return returnwert;
				}
				catch (NumberFormatException numerror) {
					LOG.severe("Fehler bei Konvertierung nach Integer, Original ist " + this.feldwert);
					return returnwert;
				}
			}
		} else {
			LOG.severe("Programmierfehler in getIntWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return returnwert;
	}
	
	public String getTextWert() {
		if(this.typ == Datentyp.String) {
			if(this.feldname.toLowerCase().indexOf("_foto") != -1) {
				String url = this.feldwert;
				if(url == null)
					return "";
				int abpos = url.indexOf("imageName=");
				if(abpos != -1) {
					url = url.substring(abpos + "imageName=".length());
					int bispos = url.indexOf("&");
					if(bispos != -1)
						url = url.substring(0,bispos);
				}
				return url;
			} else {
				return this.feldwert;
			}
		} else {
			LOG.severe("Programmierfehler in getTextWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return "";
	}

	public Date getDatumWert() {
		if(this.typ == Datentyp.Date) {
			if((this.feldwert == null) || this.feldwert.isEmpty())
				return null;
			Date datumwert;
			try {
				datumwert = date_de_formatter.parse(this.feldwert);
				return datumwert;
			} catch (ParseException e) {
				LOG.warning("in Klasse BFRKFeld, Methode getDatumWert kann das Datum "
					+ "im Textformat nicht konvertiert werden, Text aus DB ist ===" + this.feldwert + "===");
				return null;
			}
		} else {
			LOG.severe("Programmierfehler in getTextWert, Typ von Feld " + this.feldname + " ist " + this.typ);
		}
		return null;
	}


	@Override
	public String toString() {
		String output = "";
		
		output = "Feldname: " + this.feldname + ", Wert: " + this.feldwert + ",   Typ: " + this.typ.name();
		
		return output;
	}
}
