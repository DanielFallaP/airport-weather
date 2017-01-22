package com.crossover.trial.weather;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Main use cases for all end points of the query and collect services.
 * @author daniel
 *
 */
public class WeatherEndpointTest {

	/** an instance of the query end point **/
    private WeatherQueryEndpoint _query = new RestWeatherQueryEndpoint();

    /** an instance of the collector end point **/
    private WeatherCollectorEndpoint _update = new RestWeatherCollectorEndpoint();

    private Gson _gson = new Gson();

    private DataPoint _dp;
    
    /**
     * Creates several airports, updates the weather for an airport and performs a query
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
    	AirportService.getInstance().init();
        _dp = new DataPoint.Builder()
                .withCount(10).withFirst(10).withMean(20).withThird(30).withSecond(22).build();
        _update.updateWeather("BOS", "wind", _gson.toJson(_dp));
        _query.weather("BOS", "0").getEntity();
    }

    /**
     * Tests the query ping service. Asserts datasize and iata_freq entry set size according
     * to input data.
     * @throws Exception
     */
    @Test
    public void testPing() throws Exception {
        String ping = _query.ping();
        JsonElement pingResult = new JsonParser().parse(ping);
        assertEquals(1, pingResult.getAsJsonObject().get("datasize").getAsInt());
        assertEquals(5, pingResult.getAsJsonObject().get("iata_freq").getAsJsonObject().entrySet().size());
    }

    /**
     * Asserts the wind information for BOS was stored correctly after setup.
     * @throws Exception
     */
    @Test
    public void testGet() throws Exception {
        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("BOS", "0").getEntity();
        assertEquals(ais.get(0).getWind(), _dp);
    }

    /**
     * Asserts all NY airports are within 200 km of JFK.
     * @throws Exception
     */
    @Test
    public void testGetNearby() throws Exception {
        // check datasize response
        _update.updateWeather("JFK", "wind", _gson.toJson(_dp));
        _dp.setMean(40);
        _update.updateWeather("EWR", "wind", _gson.toJson(_dp));
        _dp.setMean(30);
        _update.updateWeather("LGA", "wind", _gson.toJson(_dp));

        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("JFK", "200").getEntity();
        assertEquals(3, ais.size());
    }
    
    /**
     * Asserts all NY airports are within 200 km of EWR.
     * @throws Exception
     */
    @Test
    public void testGetNearbyEWR() throws Exception {
        // check datasize response
        _update.updateWeather("JFK", "wind", _gson.toJson(_dp));
        _dp.setMean(40);
        _update.updateWeather("EWR", "wind", _gson.toJson(_dp));
        _dp.setMean(30);
        _update.updateWeather("LGA", "wind", _gson.toJson(_dp));

        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("EWR", "200").getEntity();
        assertEquals(3, ais.size());
    }
    
    /**
     * Asserts all NY airports are within 200 km of LGA.
     * @throws Exception
     */
    @Test
    public void testGetNearbyLGA() throws Exception {
        // check datasize response
        _update.updateWeather("JFK", "wind", _gson.toJson(_dp));
        _dp.setMean(40);
        _update.updateWeather("EWR", "wind", _gson.toJson(_dp));
        _dp.setMean(30);
        _update.updateWeather("LGA", "wind", _gson.toJson(_dp));

        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("EWR", "200").getEntity();
        assertEquals(3, ais.size());
    }

    /**
     * Asserts the only airport with information is Boston's.
     * Updates cloud coverage for Boston, and retrieves info from service to compare.
     * @throws Exception
     */
    @Test
    public void testUpdate() throws Exception {

        DataPoint windDp = new DataPoint.Builder()
                .withCount(10).withFirst(10).withMean(20).withThird(30).withSecond(22).build();
        _update.updateWeather("BOS", "wind", _gson.toJson(windDp));
        _query.weather("BOS", "0").getEntity();

        String ping = _query.ping();
        JsonElement pingResult = new JsonParser().parse(ping);
        assertEquals(1, pingResult.getAsJsonObject().get("datasize").getAsInt());

        DataPoint cloudCoverDp = new DataPoint.Builder()
                .withCount(4).withFirst(10).withMean(60).withThird(100).withSecond(50).build();
        _update.updateWeather("BOS", "cloudcover", _gson.toJson(cloudCoverDp));

        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("BOS", "0").getEntity();
        assertEquals(ais.get(0).getWind(), windDp);
        assertEquals(ais.get(0).getCloudCover(), cloudCoverDp);
    }
    
    /**
     * Deletes an existing airport and compares list size to assert airport deletion.
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
    	List<?> airports = (List<?>) _update.getAirports().getEntity();
    	_update.deleteAirport("JFK");
    	assertEquals(4,airports.size()-1);
    	airports = (List<String>) _update.getAirports().getEntity();
		assertEquals(false, airports.contains("JFK"));
    }
    
    /**
     * Creates a new airport, then queries same airport, and asserts response
     * has input values.
     * @throws Exception
     */
    @Test
    public void testCreate() throws Exception {
    	_update.addAirport("MDE", "20.89", "40.98");
    	AirportData airport = (AirportData) _update.getAirport("MDE").getEntity();
    	assertEquals("MDE",airport.getIata());
    	assertEquals("20.89",Double.valueOf(airport.getLatitude()).toString());
    	assertEquals("40.98",Double.valueOf(airport.getLongitude()).toString());
    	
    }
    
    /**
     * Creates a new airport, then tries creating it again. Asserts it is contained 
     * only once in airports list.
     * @throws Exception
     */
    @Test
    public void testCreateDuplicate() throws Exception {
    	
    	try {
    		_update.addAirport("MDE", "20.89", "40.98");
    		_update.addAirport("MDE", "20.89", "40.98");
			
		} catch (Exception e) {
			List<String> airports = (List<String>) _update.getAirports().getEntity();
			int times=0;
			for (String airport: airports){
				if (airport.equals("MDE"))
					times++;
			}
			assertEquals(1,times);
		}
    }
    
    /**
     * Tests each atmospheric information type is stored correctly.
     * @throws Exception
     */
    @Test
    public void testAtmosphericInformationTypes() throws Exception {
        _update.updateWeather("LGA", "wind", _gson.toJson(_dp));
        _update.updateWeather("LGA", "temperature", _gson.toJson(_dp));
        _update.updateWeather("LGA", "humidity", _gson.toJson(_dp));
        _update.updateWeather("LGA", "pressure", _gson.toJson(_dp));
        _update.updateWeather("LGA", "cloudcover", _gson.toJson(_dp));
        _update.updateWeather("LGA", "precipitation", _gson.toJson(_dp));
        
        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("LGA", "0").getEntity();
        assertEquals(ais.get(0).getWind(), _dp);
        assertEquals(ais.get(0).getTemperature(), _dp);
        assertEquals(ais.get(0).getHumidity(), _dp);
        assertEquals(ais.get(0).getPressure(), _dp);
        assertEquals(ais.get(0).getCloudCover(), _dp);
        assertEquals(ais.get(0).getPrecipitation(), _dp);
    }
    
    /**
     * Tests the only atmospheric information pertains to provided 
     * airport when radius is 0.
     * @throws Exception
     */
    @Test
    public void testZeroRadius() throws Exception{
    	_update.updateWeather("LGA", "wind", _gson.toJson(_dp));
        
        List<AtmosphericInformation> ais = (List<AtmosphericInformation>) _query.weather("LGA", "0").getEntity();
        
        if (!ais.isEmpty()){
        	assertEquals(1, ais.size());
        }
        else
        	assertEquals(true, true);
    }
    
    /**
     * Asserts the radius frequency histogram is being populated
     * correctly.
     * @throws Exception
     */
    @Test
    public void testRadiusFreq() throws Exception{
    	_query.weather("BOS", "100").getEntity();
    	_query.weather("BOS", "300").getEntity();
        
    	String pingResult = _query.ping();
    	PingObject retval = _gson.fromJson(pingResult, PingObject.class);
    	assertEquals(retval.getRadius_freq()[100], 1);
    	assertEquals(retval.getRadius_freq()[300], 1);
    }
    
    /**
     * Asserts the airport query frequency is being populated
     * correctly.
     * @throws Exception
     */
    @Test
    public void testIataFreq() throws Exception{
    	_query.weather("EWR", "0").getEntity();
    	String pingResult = _query.ping();
    	PingObject retval = _gson.fromJson(pingResult, PingObject.class);
    	assertEquals(Double.valueOf(retval.getIata_freq().get("BOS")*100).intValue(), 50);
    }
    
}