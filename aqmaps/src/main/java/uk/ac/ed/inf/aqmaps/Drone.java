package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.*;
import com.mapbox.turf.TurfJoins;

public class Drone {
	
	/** Starting position of the drone */
	private Position startingPosition;
	
	/** List of sensors to read */
	private List<Sensor> sensors = new ArrayList<Sensor>();
	
	/** List of no fly zone buildings */
	private List<Polygon> noFlyZoneBuildings = new ArrayList<Polygon>();
	
	/** List of edges of drone confinement zone */
	private List<Point> droneConfinementZone = new ArrayList<Point>();
	
	/** List of read (explored) sensors */
	private List<Integer> exploredSensors = new ArrayList<Integer>();
	
	/** List of moves made by the drone */
	private List<Position> moves = new ArrayList<Position>();
	
	/** Count of moves made by the drone */
	private int movesCount = 0;
	
	
	/**
	 * Constructor to initilaise the global variables of the Drone class
	 * @param startingPosition starting posiiton of the drone
	 * @param sensors sensors to read
	 * @param noFlyZoneBuildings list of no fly zone buildings
	 * @param droneConfinementZone list of drone confinement zone edges
	 */
	public Drone(Position startingPosition, List<Sensor> sensors, List<Polygon> noFlyZoneBuildings,
			List<Point> droneConfinementZone) {
		super();
		this.startingPosition = startingPosition;
		this.sensors = sensors;
		this.noFlyZoneBuildings = noFlyZoneBuildings;
		this.droneConfinementZone = droneConfinementZone;
	}
	
	public Position getStartingPosition() {
		return startingPosition;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public List<Polygon> getNoFlyZoneBuildings() {
		return noFlyZoneBuildings;
	}

	public List<Point> getDroneConfinementZone() {
		return droneConfinementZone;
	}

	public List<Integer> getExploredSensors() {
		return exploredSensors;
	}

	public List<Position> getMoves() {
		return moves;
	}

	public int getMovesCount() {
		return movesCount;
	}

	/**
	 * Method to traverse over all the sensors avoiding the No Fly Zone
	 * @return list of moves when the drone reads a sensor (used for generation of output files)
	 */
	List<Integer> traverseSensors() {
		var moveWhileReading = new ArrayList<Integer>();
		Position currentPos = startingPosition; // starting position of the drone
		moves.add(startingPosition);
		
		// reading the sensors till all the 33 sensors are read and in max 150 moves
		while(exploredSensors.size() < 33 && movesCount < 150) {
			int closestSensorNum = 0;
			Sensor closestSensor = null;
			Position nextPos = null;
			do {
				closestSensorNum =  currentPos.findClosestSensor(sensors, exploredSensors); // finding the number of the 
																								//closest sensor to the current position of the drone
				closestSensor = sensors.get(closestSensorNum);
				nextPos = makeMove(currentPos, closestSensorNum, null); // Next position for the drone
				currentPos = nextPos;
				moves.add(currentPos); // adding the new position to the list of moves
				++ movesCount;
			} while(!closestSensor.inRange(currentPos) && movesCount < 150);
			if(closestSensor.inRange(currentPos)) {
				readSensor(closestSensorNum); // reading the sensor when the drone is in its range
				moveWhileReading.add(movesCount); /* Storing the count of the move 
													when a sensor was read (used to generate output files) */
			}
		}
		return moveWhileReading;
	}
	
	/**
	 * Method to make the drone come back to the starting position
	 */
	void homeComing() {
		Position currentPos = moves.get(moves.size()-1);
		Position target = startingPosition; // returning back to the start location
		
		// Keep on moving till it reaches the start location 
		while(!(currentPos.distance(target) < 0.0003) && movesCount < 150) {
			Position nextPos = makeMove(currentPos, 0, target);
			currentPos = nextPos;
			moves.add(currentPos);
			++ movesCount;
		}
	}
	
	/**
	 * Method to compute the next position of the drone outside the No Fly Zone 
	 * @param currentPos current position of the drone
	 * @param closestSensorNum number of the closest sensor to the drone
	 * @param target starting position of the drone used when the drone is coming back 
	 * to the starting position, null otherwise
	 * @return next position of the drone
	 */
	private Position makeMove(Position currentPos, int closestSensorNum, Position target) {
		Position destination;
		if(target == null) // when the drone is heading towards a sensor
			destination = sensors.get(closestSensorNum).getPos(); // location of the sensor
		else destination = target; // start location (when the drone is heading back to the start location)
		
		double direction = currentPos.findDirection(destination); // direction towards the desination position
		
		if(checkDroneCrossNFZ(currentPos, direction))// checking if the next position of the drone in given direction crosses any no fly zone
			direction = posInsideNFZ(currentPos, direction, closestSensorNum); // finding the new direction (outside the No Fly Zone)
		
		double radianAngle = Math.toRadians(direction);
		Position nextPos = new Position(currentPos.getLng() + 0.0003 * Math.cos(radianAngle), 
				currentPos.getLat() + 0.0003 * Math.sin(radianAngle)); // final next position of the drone in the new direction
		return nextPos;
	}
	
	/**
	 * Method to check if the drone crosses any no fly zone in the direction given
	 * @param currentPos current position of the drone
	 * @param direction direction drone is heading
	 * @return true if it crosses a no fly zone, false otherwise
	 */
	private boolean checkDroneCrossNFZ(Position currentPos, double direction) {
		double radianAngle = Math.toRadians(direction);
		Position nextPos = null;
		// Breaking one move into 1000 points of 0.0000003 degrees to check if any lies inside the No fly zone
		for(int i = 1; i <= 1000; ++i) {
			nextPos = new Position(currentPos.getLng() + (0.0000003 * i) * Math.cos(radianAngle), 
					currentPos.getLat() + (0.0000003 * i) * Math.sin(radianAngle));
			if(insideNoFlyZone(nextPos) || !nextPos.insideDroneConfinementZone(droneConfinementZone))
				return true; // return true if any of the points lie inside the No fly zone
		}
		return false; // None of the points lie in the no fly zone
	}
	
	/**
	 * Method to find the next position of the drone after checking which side (left or right) 
	 * of the no fly zone building the drone will take less number of moves to get to the sensor
	 * @param currentPos current position pf the drone
	 * @param direction direction of the next closest sensor
	 * @param closestSensorNum number of the closesst sensor to read
	 * @return new direction for the drone outside no fly zone
	 */
	private double posInsideNFZ(Position currentPos, double direction, int closestSensorNum) {
		int countRight = goRightOrLeft(currentPos, closestSensorNum, movesCount, "+"); // number of moves the drone would take to go from right side of the No Fly Zone building
		int countLeft = goRightOrLeft(currentPos, closestSensorNum, movesCount, "-"); // number of moves the drone would take to go from left side of the No Fly Zone building
		if(countRight < countLeft) // checking which side has less number of moves
			return goRight(currentPos, direction); // go from the right side of the building
		else return goLeft(currentPos, direction); // go from the left side of the building
	}
	
	/**
	 * Recursive method to find the number of moves the drone would take 
	 * to get to the sensor from either side f the no fly zne building 
	 * @param currentPos current position of the drone
	 * @param closestSensorNum number of closest sensor
	 * @param movesCountTemp count of moves
	 * @param sign (+) incase to fly from the right side the building and 
	 * 			   (-) incase to fly from the left side the building 
	 * @return number of moves taken to get to the drone
	 */
	private int goRightOrLeft(Position currentPos, int closestSensorNum, int movesCountTemp, String sign) {
		
		// original direction towards the closest sensor
		double directionNew = currentPos.findDirection((sensors.get(closestSensorNum).getPos()));
		
		// Base case
		if(sensors.get(closestSensorNum).inRange(currentPos)) // checking if the new position is range of the sensor
			return movesCountTemp; // returning the number of moves the drone took to go from the right or left side the building
		else if(movesCountTemp > 150)
			return Integer.MAX_VALUE; // return a very big value if the drone doesn't reach the sensor within 150 moves
		
		// Recursive case
		// Finding a new direction till it comes out of the no fly zone 
		while(checkDroneCrossNFZ(currentPos, directionNew)) {
			if(sign.equals("+")) // the drone is going from the right side of the building
				directionNew = directionNew + 10; // increasing the direction by 10 to get a new direction outside the no fly zone
			else if(sign.equals("-")) // the drone is going from the left side of the building 
				directionNew = directionNew - 10; // decreaing the direction by 10 to get a new direction outside the no fly zone
			directionNew = directionNew % 360; // incase the new direction is greater than 360 degrees
		}
		
		double radianAngle = Math.toRadians(directionNew);
		++ movesCountTemp; // increasing the moves count
		Position nextPos = new Position(currentPos.getLng() + 0.0003 * Math.cos(radianAngle), 
				currentPos.getLat() + 0.0003 * Math.sin(radianAngle)); // computing the next position of the drone in the new direction
		return goRightOrLeft(nextPos, closestSensorNum, movesCountTemp, sign); // recursive call to find the next position
	}
	
	/**
	 * Method to find the new direction in the right side of the no fly zone building 
	 * @param currentPos current position of the drone
	 * @param direction original direction towards the sensor (crossing No Fly Zone)
	 * @return new direction
	 */
	private double goRight(Position currentPos, double direction) {
		double directionNew = direction;
		// finding the new direction outside no fly zone
		do {
			directionNew = directionNew + 10;
			directionNew = directionNew % 360;
		} while(checkDroneCrossNFZ(currentPos, directionNew));
		return directionNew;
	}
	
	/**
	 * Method to find the new direction in the left side of the no fly zone building 
	 * @param currentPos current position of the drone
	 * @param direction original direction towards the sensor (crossing No Fly Zone)
	 * @return new direction
	 */
	private double goLeft(Position currentPos, double direction) {
		double directionNew = direction;
		// finding the new direction outside no fly zone
		do {
			directionNew = directionNew - 10;
			directionNew = directionNew % 360;
		} while(checkDroneCrossNFZ(currentPos, directionNew));
		return directionNew;
	}
	
	/**
	 * Method to check if the next position of the drone is inside any of the No Fly Zone Buildings
	 * @param position of the drone
	 * @return true if it is inside no fly zone, false otherwise
	 */
	private Boolean insideNoFlyZone(Position pos) {
		Point p = Point.fromLngLat(pos.getLng(), pos.getLat());
		
		// checking if the position given is inside any of the no fly zone buildings
		boolean result_AT =  TurfJoins.inside(p, noFlyZoneBuildings.get(0));
		boolean result_DHT =  TurfJoins.inside(p, noFlyZoneBuildings.get(1));
		boolean result_LIB =  TurfJoins.inside(p, noFlyZoneBuildings.get(2));	
		boolean result_INF =  TurfJoins.inside(p, noFlyZoneBuildings.get(3));
		
		return result_AT || result_INF || result_DHT || result_LIB;
	}

	/**
	 * Method to read the sensor when the drone is in it's range.
	 * @param closestSensorNum Number of the closest sensor to read
	 */
	private void readSensor(int closestSensorNum) {
		sensors.get(closestSensorNum).setVisited(true); // setting the visited field of the Sensor class to true 
		exploredSensors.add(closestSensorNum); // adding the sensor number to the explored list to make sure it's not visited again 
	}
}