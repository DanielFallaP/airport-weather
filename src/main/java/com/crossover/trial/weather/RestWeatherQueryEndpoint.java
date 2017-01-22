package com.crossover.trial.weather;

import static com.crossover.trial.weather.AirportService.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
/**
 * The Weather App REST endpoint allows clients to query, update and check health stats. Currently, all data is
 * held in memory. The end point deploys to a single container
 *
 * @author code test administrator
 */
@Path("/query")
public class RestWeatherQueryEndpoint implements WeatherQueryEndpoint {

    public final static Logger LOGGER = Logger.getLogger("WeatherQuery");

    /**
     * Retrieve service health including total size of valid data points and request frequency information.
     *
     * @return health stats for the service as a string
     */
    @Override
    public String ping() {
        Map<String, Object> retval = new HashMap<>();

        int datasize = 0;
        Map<String, AtmosphericInformation> synchronizedAI = Collections.synchronizedMap(atmosphericInformation);
        synchronized (synchronizedAI){
        	for (String iata : synchronizedAI.keySet()) {
        		AtmosphericInformation ai = synchronizedAI.get(iata);
        		// we only count recent readings
        		if (ai.getCloudCover() != null
        				|| ai.getHumidity() != null
        				|| ai.getPressure() != null
        				|| ai.getPrecipitation() != null
        				|| ai.getTemperature() != null
        				|| ai.getWind() != null) {
        			// updated in the last day
        			if (ai.getLastUpdateTime() > System.currentTimeMillis() - 86400000) {
        				datasize++;
        			}
        		}
        	}
        }
        retval.put("datasize", datasize);

        Map<String, Double> freq = new HashMap<>();
        // fraction of queries
        List<AirportData> synchronizedAirports = Collections.synchronizedList(airportData);
        Map<AirportData, Integer> synchronizedReqFreq = Collections.synchronizedMap(requestFrequency);
        
        synchronized (synchronizedAirports) {
        	int total=0;
        	synchronized (synchronizedReqFreq){
        		for (AirportData ai: synchronizedReqFreq.keySet()){
        			total+=synchronizedReqFreq.get(ai);
        		}
        	}
        	for (AirportData data : synchronizedAirports) {
        		if (synchronizedReqFreq.size() > 0){
        			double frac = (double) synchronizedReqFreq.getOrDefault(data, 0) / total;
        			freq.put(data.getIata(), frac);
        		}
        		else{
        			freq.put(data.getIata(), null);
        		}
        		
        	}
		}
        retval.put("iata_freq", freq);
        
        Map<Double, Integer> synchronizedRadFreq = Collections.synchronizedMap(radiusFreq);

        synchronized (synchronizedRadFreq){
        	int m = synchronizedRadFreq.keySet().stream()
        			.max(Double::compare)
        			.orElse(1000.0).intValue() + 1;
        	
        	int[] hist = new int[m];
        	for (Map.Entry<Double, Integer> e : synchronizedRadFreq.entrySet()) {
        		int i = e.getKey().intValue();
        		hist[i] += e.getValue();
        	}
        	retval.put("radius_freq", hist);
        }

        return gson.toJson(retval);
    }

    /**
     * Given a query in json format {'iata': CODE, 'radius': km} extracts the requested airport information and
     * return a list of matching atmosphere information.
     *
     * @param iata the iataCode
     * @param radiusString the radius in km
     *
     * @return a list of atmospheric information
     */
    @Override
    public Response weather(String iata, String radiusString) {
        double radius = radiusString == null || radiusString.trim().isEmpty() ? 0 : Double.valueOf(radiusString);
        updateRequestFrequency(iata, radius);

        List<AtmosphericInformation> retval = new ArrayList<>();
        if (radius == 0) {
            AtmosphericInformation ai = Collections.synchronizedMap(atmosphericInformation).get(iata);
            if (ai.getCloudCover() != null || ai.getHumidity() != null || ai.getPrecipitation() != null
                    || ai.getPressure() != null || ai.getTemperature() != null || ai.getWind() != null){
                     retval.add(ai);
             }
        } else {
            AirportData ad = findAirportData(iata);
            List<AirportData> synchronizedAirports = Collections.synchronizedList(airportData);
            synchronized (synchronizedAirports) {
            	for (int i=0;i< airportData.size(); i++){
            		if (calculateDistance(ad, airportData.get(i)) <= radius){
            			AtmosphericInformation ai = Collections.synchronizedMap(atmosphericInformation).get(airportData.get(i).getIata());
            			if (ai.getCloudCover() != null || ai.getHumidity() != null || ai.getPrecipitation() != null
            					|| ai.getPressure() != null || ai.getTemperature() != null || ai.getWind() != null){
            				retval.add(ai);
            			}
            		}
            	}
			}
        }
        return Response.status(Response.Status.OK).entity(retval).build();
    }
}
