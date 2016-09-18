package pentos.util;

import java.util.HashSet;
import java.util.Set;

import pentos.sim.Building;

public class Row {
	
	private int start, end;
	private int currentLocation;

	private Set<Building> buildings;
	
	public Row(int start, int end) {
		this.start = start;
		this.end = end;
		this.currentLocation = 0;
		this.buildings = new HashSet<Building>();
	}
	
	public int size() {
		return this.end - this.start;
	}
	
	public int getCurrentLocation() {
		return currentLocation;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}

}
