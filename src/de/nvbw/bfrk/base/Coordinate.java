package de.nvbw.bfrk.base;

public class Coordinate {
	public static double UNSET = 0.0;

	private double lon = UNSET;
	private double lat = UNSET;
	
	
	public Coordinate(double lon, double lat) {
		this.lon = lon;
		this.lat = lat;
	}
	
	public Coordinate() {
	}

	public double getLon() {
		return this.lon;
	}

	public double getLat() {
		return this.lat;
	}
	
	public boolean isUnset()  {
		return ( ( this.lon == UNSET ) || ( this.lat == UNSET ) );
	}

	@Override
	public String toString() {
		return this.lon + "/" + this.lat;
	}
}
