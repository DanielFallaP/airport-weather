package com.crossover.trial.weather;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
/**
 * This class provides all the queries and updates with the airport data used by the query and collect end points.
 * 
 * @author daniel
 *
 */
public class AirportService {
	
    /** earth radius in KM */
    public static final double R = 6372.8;

    /** shared gson json to object factory */
    public static final Gson gson = new Gson();

    /** all known airports */
    public volatile static List<AirportData> airportData = new ArrayList<>();

    /** atmospheric information for each airport, idx corresponds with airportData */
    public volatile static Map<String,AtmosphericInformation> atmosphericInformation = new HashMap<>();

    /**
     * Internal performance counter to better understand most requested information, this map can be improved but
     * for now provides the basis for future performance optimizations. Due to the stateless deployment architecture
     * we don't want to write this to disk, but will pull it off using a REST request and aggregate with other
     * performance metrics {@link #ping()}
     */
    public volatile static Map<AirportData, Integer> requestFrequency = new HashMap<AirportData, Integer>();

    /**
     * Radio request frequency map.
     */
    public volatile static Map<Double, Integer> radiusFreq = new HashMap<Double, Integer>();

    /**
     * Singleton instance of service class.
     */
    private static AirportService instance = null;
    
    protected AirportService(){
    	
    }
    
    /**
     * Getter method for instance.
     * @return
     */
    public static AirportService getInstance() {
        if(instance == null) {
           instance = new AirportService();
        }
        return instance;
     }
    
	/**
     * Given an iataCode find the airport data
     *
     * @param iataCode as a string
     * @return airport data or null if not found
     */
    public synchronized static AirportData findAirportData(String iataCode) {
        return Collections.synchronizedList(airportData).stream()
            .filter(ap -> ap.getIata().equals(iataCode))
            .findFirst().orElse(null);
    }
    
    /**
     * Update the airports weather data with the collected data.
     *
     * @param iataCode the 3 letter IATA code
     * @param pointType the point type {@link DataPointType}
     * @param dp a datapoint object holding pointType data
     *
     * @throws WeatherException if the update can not be completed
     */
    public static void addDataPoint(String iataCode, String pointType, DataPoint dp) throws WeatherException {
        AirportData airportData = findAirportData(iataCode);
        if (airportData != null)
        	updateAtmosphericInformation(Collections.synchronizedMap(atmosphericInformation).get(airportData.getIata()), pointType, dp);
    }

    /**
     * update atmospheric information with the given data point for the given point type
     *
     * @param ai the atmospheric information object to update
     * @param pointType the data point type as a string
     * @param dp the actual data point
     */
    public static synchronized void updateAtmosphericInformation(AtmosphericInformation ai, String pointType, DataPoint dp) throws WeatherException {

        if (pointType.equalsIgnoreCase(DataPointType.WIND.name())) {
            if (dp.getMean() >= 0) {
                ai.setWind(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.TEMPERATURE.name())) {
            if (dp.getMean() >= -50 && dp.getMean() < 100) {
                ai.setTemperature(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.HUMIDITY.name())) {
            if (dp.getMean() >= 0 && dp.getMean() <= 100) {
                ai.setHumidity(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.PRESSURE.name())) {
            if (dp.getMean() >= 0) {
                ai.setPressure(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.CLOUDCOVER.name())) {
            if (dp.getMean() >= 0 && dp.getMean() <= 100) {
                ai.setCloudCover(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        if (pointType.equalsIgnoreCase(DataPointType.PRECIPITATION.name())) {
            if (dp.getMean() >=0) {
                ai.setPrecipitation(dp);
                ai.setLastUpdateTime(System.currentTimeMillis());
                return;
            }
        }

        throw new IllegalStateException("couldn't update atmospheric data");
    }
    
    /**
     * Deletes the airport identified by iata code.
     * @param iata
     */
    public static synchronized void deleteAirportByCode(String iata){
    	AirportData toRemove = new AirportData();
    	toRemove.setIata(iata);
    	Collections.synchronizedList(airportData).remove(toRemove);
    	Collections.synchronizedMap(atmosphericInformation).remove(iata);
    }
    
    /**
     * Add a new known airport to our list.
     *
     * @param iataCode 3 letter code
     * @param latitude in degrees
     * @param longitude in degrees
     *
     * @return the added airport
     */
    public static synchronized AirportData newAirport(String iataCode, double latitude, double longitude) {
        AirportData ad = new AirportData();

        AtmosphericInformation ai = new AtmosphericInformation();
        ad.setIata(iataCode);
        ad.setLatitude(latitude);
        ad.setLongitude(longitude);
        List<AirportData> synchronizedAirports = Collections.synchronizedList(airportData);
        if (!synchronizedAirports.contains(ad)){
        	synchronizedAirports.add(ad);
        	Collections.synchronizedMap(atmosphericInformation).put(iataCode, ai);
        	return ad;
        }
        throw new IllegalStateException("Airport already exists");
    }
    

    /**
     * Records information about how often requests are made
     *
     * @param iata an iata code
     * @param radius query radius
     */
    public static synchronized void updateRequestFrequency(String iata, Double radius) {
        AirportData airportData = findAirportData(iata);
        Map<AirportData, Integer> synchronizedReqFreq = Collections.synchronizedMap(requestFrequency);
        Map<Double, Integer> synchronizedMap = Collections.synchronizedMap(radiusFreq);
        synchronizedReqFreq.put(airportData, synchronizedReqFreq.getOrDefault(airportData, 0) + 1);
        synchronizedMap.put(radius, synchronizedMap.getOrDefault(radius, 0) + 1);
    }

    /**
     * Haversine distance between two airports.
     *
     * @param ad1 airport 1
     * @param ad2 airport 2
     * @return the distance in KM
     */
    public static double calculateDistance(AirportData ad1, AirportData ad2) {
        double deltaLat = Math.toRadians(ad2.getLatitude() - ad1.getLatitude());
        double deltaLon = Math.toRadians(ad2.getLongitude() - ad1.getLongitude());
        double a =  Math.pow(Math.sin(deltaLat / 2), 2) + Math.pow(Math.sin(deltaLon / 2), 2)
                * Math.cos(ad1.getLatitude()) * Math.cos(ad2.getLatitude());
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
    
    /**
     * A dummy init method that loads hard coded data
     */
    protected void init() {
        airportData.clear();
        atmosphericInformation.clear();
        requestFrequency.clear();

        newAirport("BOS", 42.364347, -71.005181);
        newAirport("EWR", 40.6925, -74.168667);
        newAirport("JFK", 40.639751, -73.778925);
        newAirport("LGA", 40.777245, -73.872608);
        newAirport("MMU", 40.79935, -74.4148747);
    }

}
