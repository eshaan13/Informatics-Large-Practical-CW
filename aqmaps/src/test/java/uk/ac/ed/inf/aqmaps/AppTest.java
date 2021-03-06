package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;
import org.junit.Test;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.*;
import com.mapbox.turf.TurfJoins;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
	
	// Date to check for.
	String d = "12"; String m = "12"; String yr = "2020";
	
    /**
     * Test to check the text files generated by the App class :-)
     * @throws InterruptedException 
     * @throws IOExcepion 
     */
    @SuppressWarnings("static-access")
	@Test
    public void testTxtOutputFiles() throws IOException, InterruptedException
    {
    		
    			Position pt = new Position(-3.188396, 55.944425);
    		    App app = new App(d, m, yr, pt, 80);
    		    String args[] = {d, m, yr, "55.944425", "-3.188396", "5678", "80"};
    		    app.main(args);
    		    
    		    String filename = "flightpath-" + d + "-" + m + "-" + yr + ".txt";
    		    int count = 0;
    		    int sensorsRead = 0;
    		    try {
    		        File myObj = new File(filename);
    		        Scanner myReader = new Scanner(myObj);
    		        while (myReader.hasNextLine()) {
	    		          String data = myReader.nextLine();
	    		          ++ count;
	    		          String vals[] = data.split("\\,", 7);
	    		          
	    		          double p1_x = Double.parseDouble(vals[1]);
	    		          double p1_y = Double.parseDouble(vals[2]);
	    		          
	    		          double p2_x = Double.parseDouble(vals[4]);
	    		          double p2_y = Double.parseDouble(vals[5]);
	    		          
	    		          // angles are less than 350 
	    		          assertTrue(Double.parseDouble(vals[3]) <= 350);
	    		          
	    		          // no negative angles
	    		          assertTrue(Double.parseDouble(vals[3]) >= 0);
	    		          
	    		          // move of 0.0003 degrees
	    		          String dist = String.format("%.1g%n", getDist(p1_x, p1_y, p2_x, p2_y));
	    		          assertTrue(Double.parseDouble(dist) == 0.0003);
	    		          
	    		          // drone in range of the sensor when read
	    		          if(!vals[6].equals("null")) { 
	    		        	  WhatThreeWord sensor = app.whatThreeWord(vals[6]);
	    		        	  double sensorLng = sensor.getCoordinates().getLongitude();
	    		        	  double sensorLat = sensor.getCoordinates().getLatitude();
	    		        	  ++ sensorsRead;
	    		        	  assertTrue(getDist(p2_x, p2_y, sensorLng, sensorLat) < 0.0002);
	    		          }
	    		          
	    		          // No crossing of the No Fly Zone
	    		          app.noFlyZoneBuildings();
	    		          var nfz = app.noFlyZoneBuildings;
	    		          Point p1 = Point.fromLngLat(p1_x, p1_y);
	    		          Point p2 = Point.fromLngLat(p2_x, p2_y);
	    		          
	    		          for(int i = 0; i < nfz.size(); ++i) {
	    		        	  assertTrue(!TurfJoins.inside(p1, nfz.get(i))); 
	    		        	  assertTrue(!TurfJoins.inside(p2, nfz.get(i)));
	    		          }
    		        }
    		        myReader.close();
    		      } catch (FileNotFoundException e) {
    		        System.out.println("An error occurred: File Not Found");
    		        e.printStackTrace();
    		      }
    		    
    		    // max 150 lines
    		    assertTrue(count <= 150);
    		    
    		    // number of sensors read are 33
    		    assertTrue(sensorsRead == 33);
    		    
    		    // Deleting the generated files 
    		    File myObj = new File(filename); 
    		    if (myObj.delete()) { 
    		      System.out.println("Deleted the file: " + myObj.getName());
    		    } else {
    		      System.out.println("Failed to delete the file.");
    		    }
    }
    
    /**
     * Test to check the GeoJson files generated by the App class :-)
     * @throws InterruptedException 
     * @throws IOExcepion 
     */
    @Test
    public void testGeoJsonOutputFiles() {
    	
    	String fileName = "readings-" + d + "-" + m + "-" + yr + ".geojson";
    	
    	try {
	    	File myObj = new File(fileName);
	        Scanner myReader = new Scanner(myObj);
	        while (myReader.hasNextLine()) {
		          String data = myReader.nextLine();
		          FeatureCollection fc = FeatureCollection.fromJson(data);
		          var features = fc.features();
		          int pointsCount = 0;
		          int lineStringCount = 0;
		          
		          // 34 features
		          assertTrue(features.size() == 34);
		          
		          for(Feature f: features) {
		        	  if(f.geometry() instanceof Point)
		        		  ++ pointsCount;
		        	  else if(f.geometry() instanceof LineString)
		        		  ++ lineStringCount;
		          }
		          // 33 GeoJson points for Sensors
		          assertTrue(pointsCount == 33);
		          
		          // 1 Line string for drone's path
		          assertTrue(lineStringCount == 1);
	        }
	        myReader.close();
    	} catch (FileNotFoundException e) {
	        System.out.println("An error occurred: File Not Found");
	        e.printStackTrace();
	      }
    	
    	// Deleting the file
	    File myObj2 = new File(fileName);
	    if (myObj2.delete()) { 
	      System.out.println("Deleted the file: " + myObj2.getName());
	    } else {
	      System.out.println("Failed to delete the file.");
	    }
    }
    
    public double getDist(double p1_x, double p1_y, double p2_x, double p2_y) {
    	double distance = Math.sqrt(Math.pow(p1_x - p2_x, 2) + Math.pow(p1_y - p2_y, 2));
    	return distance;
    }
}
