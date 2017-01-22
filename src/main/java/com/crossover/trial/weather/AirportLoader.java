package com.crossover.trial.weather;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

/**
 * A simple airport loader which reads a file from disk and sends entries to the webservice
 *
 * TODO: Implement the Airport Loader
 * 
 * @author code test administrator
 */
public class AirportLoader {

    /** end point to supply updates */
    private WebTarget collect;

    public AirportLoader() {
        Client client = ClientBuilder.newClient();
        collect = client.target("http://localhost:9090/collect");
    }

    /**
     * Reads airport input stream, and creates an airport for each line present in stream.
     * @param airportDataStream the airport input stream
     * @throws IOException
     */
    public void upload(InputStream airportDataStream) throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(airportDataStream));
        String l = null;

        while ((l = reader.readLine()) != null) {
        	String[] fields = l.split(",");
        	String path = "/airport/" + AirportLoader.removeQuoationMarks(fields[4]) 
        		+ "/" + fields[6] + "/" + fields[7];
        	WebTarget target = collect.path(path);
        	target.request().post(null);
        }
    }
    
    /**
     * Removes surrounding quotation marks of input string.
     * @param s The input string
     * @return
     */
    public static String removeQuoationMarks(String s){
    	s = s.substring(1);
    	return s.substring(0, s.length() - 1);
    }

    /**
     * Reads file, and creates an airport for each line in file.
     * @param args
     * @throws IOException
     */
    public static void main(String args[]) throws IOException{
        File airportDataFile = new File(args[0]);
        if (!airportDataFile.exists() || airportDataFile.length() == 0) {
            System.err.println(airportDataFile + " is not a valid input");
            System.exit(1);
        }

        AirportLoader al = new AirportLoader();
        FileInputStream fileInputStream = new FileInputStream(airportDataFile);
        al.upload(fileInputStream);
        fileInputStream.close();
        System.exit(0);
    }
}
