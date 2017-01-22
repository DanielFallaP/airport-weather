package com.crossover.trial.weather;

import static com.crossover.trial.weather.AirportService.addDataPoint;
import static com.crossover.trial.weather.AirportService.airportData;
import static com.crossover.trial.weather.AirportService.deleteAirportByCode;
import static com.crossover.trial.weather.AirportService.findAirportData;
import static com.crossover.trial.weather.AirportService.gson;
import static com.crossover.trial.weather.AirportService.newAirport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * A REST implementation of the WeatherCollector API. Accessible only to airport weather collection
 * sites via secure VPN.
 *
 * @author code test administrator
 */

@Path("/collect")
public class RestWeatherCollectorEndpoint implements WeatherCollectorEndpoint {
    public final static Logger LOGGER = Logger.getLogger(RestWeatherCollectorEndpoint.class.getName());

    @Override
    public Response ping() {
        return Response.status(Response.Status.OK).entity("ready").build();
    }

    @Override
	public Response updateWeather(String iata, String pointType, 
								  String dataPointJson) {
		try {
			addDataPoint(iata, pointType, gson.fromJson(dataPointJson, DataPoint.class));
		}catch (WeatherException e) {
			e.printStackTrace();
		}
		return Response.status(Response.Status.OK).build();
	}


    @Override
    public Response getAirports() {
        List<String> retval = new ArrayList<>();
        List<AirportData> synchronizedAirports = Collections.synchronizedList(airportData);
        synchronized (synchronizedAirports){
        	for (AirportData ad : synchronizedAirports) {
        		retval.add(ad.getIata());
        	}
        }
        return Response.status(Response.Status.OK).entity(retval).build();
    }

    @Override
	public Response getAirport(String iata) {
	    AirportData ad = findAirportData(iata);
        return Response.status(Response.Status.OK).entity(ad).build();
	}

    @Override
	public Response addAirport(String iata, String latString, 
							   String longString) {
    	newAirport(iata, Double.valueOf(latString), Double.valueOf(longString));
        return Response.status(Response.Status.OK).build();
	}
    
    @Override
	public Response deleteAirport(String iata) {
		deleteAirportByCode(iata);
    	return Response.status(Response.Status.OK).build();
	}

    @Override
    public Response exit() {
        System.exit(0);
        return Response.noContent().build();
    }
}
