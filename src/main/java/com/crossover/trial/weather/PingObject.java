package com.crossover.trial.weather;

import java.util.Map;

/**
 * This class is an object representation
 * of the query ping response
 * @author daniel
 *
 */
public class PingObject {

	private int[] radius_freq;

	private int datasize;
	
	private Map<String, Double> iata_freq;
	
	public int getDatasize() {
		return datasize;
	}

	public void setDatasize(int datasize) {
		this.datasize = datasize;
	}
	
	public int[] getRadius_freq() {
		return radius_freq;
	}
	
	public void setRadius_freq(int[] radius_freq) {
		this.radius_freq = radius_freq;
	}

	public Map<String, Double> getIata_freq() {
		return iata_freq;
	}

	public void setIata_freq(Map<String, Double> iata_freq) {
		this.iata_freq = iata_freq;
	}
}
