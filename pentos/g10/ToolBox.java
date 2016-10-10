package pentos.g10;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class ToolBox {
	public static double calculateDistance(Cell a,Cell b){
		return Math.abs(a.i-b.i)+Math.abs(a.j-b.j);
	}
	// Use reflection to get the bloody private field
	// throws IllegalArgumentException, IllegalAccessException,
	// NoSuchFieldException, SecurityException
	public static Cell.Type getCellType(Cell c) {
		try {
			Field f = c.getClass().getDeclaredField("type"); // NoSuchFieldException
			f.setAccessible(true);
			Cell.Type iWantThis = (Cell.Type) f.get(c); // IllegalAccessException
			return iWantThis;
		} catch (Exception e) {
			System.out.println("Cannot get the field anyway. Return a default.");
			return Cell.Type.FACTORY;
		}
	}

	public static Set<Cell> getRoads(Land land) {
		try {
			Field f = land.getClass().getDeclaredField("road_network"); // NoSuchFieldException
			f.setAccessible(true);
			@SuppressWarnings("unchecked")
			Set<Cell> iWantThis = (Set<Cell>) f.get(land); // IllegalAccessException
			return iWantThis;
		} catch (Exception e) {
			System.out.println("Cannot get the roads anyway. Return a default.");
			return new HashSet<Cell>();
		}

	}

	public static Set<Cell> copyLandRoads(Land land) {
		try {
			// Copy the set of road cells
			Set<Cell> landRoads = getRoads(land);
			Set<Cell> copyRoads = new HashSet<>();
			copyRoads.addAll(landRoads);
			return copyRoads;
		} catch (Exception e) {
			System.out.println("Cannot copy road network");
			return new HashSet<>();
		}
	}

	public static Set<Cell> setInterception(Set<Cell> large, Set<Cell> small) {
		Set<Cell> keep = new HashSet<Cell>();
		keep.addAll(small);
		keep.retainAll(large);
		return keep;
	}

	/* Return all vacant and occupied neighbors */
	public static Set<Cell> allNeighbors(Set<Cell> cells){
		Set<Cell> neighbors=new HashSet<>();
		for(Cell c:cells){
			Cell[] arr=c.neighbors();
			neighbors.addAll(Arrays.asList(arr));
		}
		neighbors.removeAll(cells);
		return neighbors;
	}
	/*
	 * Shift cells to make it start from the offset.
	 */
	public static Set<Cell> shiftCells(Building building, Cell start) {
		// Validate x and y of start
		if (start == null) {
			System.out.println("Error: The start cell is null!");
			return new HashSet<Cell>();
		}
		if (start.i < 0 || start.j < 0) {
			System.out.println("Error: The start cells has negative coordinates! i: " + start.i + " j: " + start.j);
			return new HashSet<Cell>();
		}
		int size = building.size();
		Set<Cell> cells = new HashSet<>();
		Iterator<Cell> iter = building.iterator();
		Cell.Type t = null;
		while (iter.hasNext()) {
			Cell c = iter.next();
			if (t == null) {
				t = getCellType(c);
			}
			Cell n = new Cell(c.i + start.i, c.j + start.j, t);
			cells.add(n);
		}
		if (size != cells.size()) {
			System.out.println(
					"Error: The numbder of cells in building has changed from " + size + " to " + cells.size() + "!");
		}
		return cells;
	}

	/*
	 * The starting point is the bottom right corner. The reaction is to just
	 * multiple the coord of each cell by -1 to make it a reflection.
	 */
	public static Cell[] offsetBottomRight(Building building, Cell start) {
		Iterator<Cell> iter = building.iterator();
		int len = building.size();
		int i = 0;
		Cell[] reflected = new Cell[len];
		Cell.Type t = null;
		while (iter.hasNext()) {
			Cell c = iter.next();
			if (t == null) {
				t = getCellType(c);
				// System.out.println("Detected the cell is of type "+t);
			}
			int row = start.i - c.i;
			int col = start.j - c.j;
			if (row < 0 || col < 0)
				return null;
			reflected[i] = new Cell(row, col, t);
			i++;
		}
		return reflected;
	}

	// build shortest sequence of road cells to connect to a set of cells b
	public static Set<Cell> findShortestRoad(Set<Cell> b, Land land, Set<Cell> road_cells) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();
		// add border cells that don't have a road currently
		Cell source = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE); // dummy
																		// cell
																		// to
																		// serve
																		// as
																		// road
																		// connector
																		// to
																		// perimeter
																		// cells
		for (int z = 0; z < land.side; z++) {
			if (b.contains(new Cell(0, z)) || b.contains(new Cell(z, 0)) || b.contains(new Cell(land.side - 1, z))
					|| b.contains(new Cell(z, land.side - 1))) // if already on
																// border don't
																// build any
																// roads
				return output;
			if (land.unoccupied(0, z))
				queue.add(new Cell(0, z, source));
			if (land.unoccupied(z, 0))
				queue.add(new Cell(z, 0, source));
			if (land.unoccupied(z, land.side - 1))
				queue.add(new Cell(z, land.side - 1, source));
			if (land.unoccupied(land.side - 1, z))
				queue.add(new Cell(land.side - 1, z, source));
		}
		// add cells adjacent to current road cells
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q))
					queue.add(new Cell(q.i, q.j, p)); // use tail field of cell
														// to keep track of
														// previous road cell
														// during the search
			}
		}
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {
				if (b.contains(x)) { // trace back through search tree to find
										// path
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i, tail.j));
						tail = tail.previous;
					}
					if (!output.isEmpty())
						return output;
				} else if (!checked[x.i][x.j] && land.unoccupied(x.i, x.j)) {
					x.previous = p;
					queue.add(x);
				}

			}
		}
		if (output.isEmpty() && queue.isEmpty())
			return null;
		else
			return output;
	}

	public static Cell findTopLeft(Cell[] cells) {
		if (cells == null || cells.length == 0) {
			System.out.println("Error: Null or empty cells passed to findTopLeft()");
			return null;
		}
		int landLimit=Player.staticLandSize;
		int minRow = 100;
		int minCol = 100;
		for (int i = 0; i < cells.length; i++) {
			Cell c = cells[i];
			if (c.i <= minRow) {
				minRow = c.i;
			}
			if (c.j <= minCol) {
				minCol = c.j;
			}
		}
		if (minRow < 0 || minRow > landLimit-1) {
			System.out.println("Error: Top row in cells is out of range: " + minRow);
			minRow = 0;
		}
		if (minCol < 0 || minCol > landLimit-1) {
			System.out.println("Error: Leftmost column in cells is out of range: " + minCol);
			minCol = 0;
		}
		return new Cell(minRow, minCol);
	}

	public static Cell[] lookToTopLeft(Cell[] cells, Cell topLeft) {
		int landLimit=Player.staticLandSize;
		
		if (cells == null || cells.length == 0) {
			System.out.println("Error: Empty cell array passed to lookToTopLeft()");
			return null;
		}
		if (topLeft == null) {
			System.out.println("Error: Empty startpoint passed to lookToTopLeft()");
			return null;
		}
		if (topLeft.i < 0 || topLeft.i > landLimit-1 || topLeft.j < 0 || topLeft.j > landLimit-1) {
			System.out.println("Error: Invalid start point of coordinates i: " + topLeft.i + " j: " + topLeft.j);
			return null;
		}
		Cell.Type t = getCellType(cells[0]);
		Cell[] arr = new Cell[cells.length];
		for (int k = 0; k < cells.length; k++) {
			Cell c = cells[k];
			int newI = c.i - topLeft.i;
			int newJ = c.j - topLeft.j;
			if (newI < 0 || newJ < 0) {
				System.out.println("Error: Invalid coordinates from shifting i: " + newI + " j: " + newJ);
				return null;
			}
			Cell n = new Cell(newI, newJ, t);
			arr[k] = n;
		}
		return arr;
	}

	public static boolean compareBuildings(Building a, Building b) {
		Set<Cell> aCells = getBuildingCells(a);
		Set<Cell> bCells = getBuildingCells(b);
		return aCells.containsAll(bCells) && bCells.containsAll(aCells);
	}

	public static Set<Cell> getBuildingCells(Building b) {
		Iterator<Cell> iter = b.iterator();
		Set<Cell> cells = new HashSet<>();
		while (iter.hasNext()) {
			cells.add(iter.next());
		}
		return cells;
	}

	/*
	 * toOccupy: the cells this building block occupies overallOccupt: all cells
	 * this building action bundle occupies land: the land before this building
	 * action is carried out
	 */
	public static Set<Cell> vacantNeighbors(Set<Cell> toOccupy, Set<Cell> overallOccupy, Land land) {
		Set<Cell> avail = new HashSet<>();
		for (Cell c : toOccupy) {
			Cell[] neighbors = c.neighbors();
			avail.addAll(Arrays.asList(neighbors));
		}
		Set<Cell> toRemove = new HashSet<>();
		for (Cell c : avail) {
			if (land.unoccupied(c)) {
				if (toOccupy.contains(c)) {
					toRemove.add(c);
				} else if (overallOccupy.contains(c)) {
					toRemove.add(c);
				}
			} else {
				toRemove.add(c);
			}
		}
		avail.removeAll(toRemove);
		return avail;
	}

	@SafeVarargs
	public static Set<Cell> combineSets(Set<Cell>... sets) {
		Set<Cell> total = new HashSet<>();
		for (int i = 0; i < sets.length; i++) {
			Set<Cell> set=sets[i];
			if(set==null)
				continue;
			else
				total.addAll(set);
		}
		return total;
	}

	public static void reportAction(Action action) {
		if (action == null || action.getBuilding() == null) {
			System.out.println("Empty action: Nothing to report.");
			return;
		}

		System.out.println("The action is: ");
		System.out.println("Building " + action.getBuilding().type);
		Building rotated = action.getBuilding().rotations()[action.getRotation()];
		System.out.println("Cells to build: " + ToolBox.shiftCells(rotated, action.getStartPoint()));
		System.out.println("Roads in the pack: " + action.getRoadCells());
	}
	//Find the top left cell if topRight is the top right cell of building
	public static Cell shiftFromTopRight(Building b, Cell topRight) {
		int landLimit=Player.staticLandSize;
		Set<Cell> cells=ToolBox.getBuildingCells(b);
		int minRow = 100;
		int maxCol = 0;
		for (Cell c:cells) {
			if (c.i <= minRow) {
				minRow = c.i;
			}
			if (c.j >= maxCol) {
				maxCol = c.j;
			}
		}
		if (minRow < 0 || minRow > landLimit-1) {
//			System.out.println("Error: Top row in cells is out of range: " + minRow);
			return null;
		}
		if (maxCol < 0 || maxCol > landLimit-1) {
//			System.out.println("Error: Leftmost column in cells is out of range: " + maxCol);
			return null;
		}
//		Cell topRightOfCell=new Cell(minRow,maxCol);
		if(topRight==null||!isValid(topRight)){
//			System.out.println("Error: topRight is null or invalid: "+topRight);
			return null;
		}
		int newI=topRight.i-minRow;
		int newJ=topRight.j-maxCol;
		if(newI<0||newJ<0||newI>landLimit-1||newJ>landLimit-1){
//			System.out.println("Error: Invalid coordinates for shifted top left corner of building i: "+newI+" j: "+newJ);
			return null;
		}
		return new Cell(newI,newJ);
	}

	public static Cell shiftFromBottomLeft(Building b, Cell bottomLeft) {
		int landLimit=Player.staticLandSize;
		Set<Cell> cells=ToolBox.getBuildingCells(b);
		int maxRow = 0;
		int minCol = 100;
		for (Cell c:cells) {
			if (c.i >= maxRow) {
				maxRow = c.i;
			}
			if (c.j <= minCol) {
				minCol = c.j;
			}
		}
		if (maxRow < 0 || maxRow > landLimit-1) {
//			System.out.println("Error: Bottom row in cells is out of range: " + maxRow);
			return null;
		}
		if (minCol < 0 || minCol > landLimit-1) {
//			System.out.println("Error: Leftmost column in cells is out of range: " + minCol);
			return null;
		}
//		Cell topRightOfCell=new Cell(minRow,maxCol);
		if(bottomLeft==null||!isValid(bottomLeft)){
//			System.out.println("Error: bottomLeft is null or invalid: "+bottomLeft);
			return null;
		}
		int newI=bottomLeft.i-maxRow;
		int newJ=bottomLeft.j-minCol;
		if(newI<0||newJ<0||newI>landLimit-1||newJ>landLimit-1){
//			System.out.println("Error: Invalid coordinates for shifted bottom left corner of building i: "+newI+" j: "+newJ);
			return null;
		}
		return new Cell(newI,newJ);
	}

	public static Cell shiftFromBottomRight(Building b, Cell bottomRight) {
		int landLimit=Player.staticLandSize;
		Set<Cell> cells=ToolBox.getBuildingCells(b);
		int maxRow = 0;
		int maxCol = 0;
		for (Cell c:cells) {
			if (c.i >= maxRow) {
				maxRow = c.i;
			}
			if (c.j >= maxCol) {
				maxCol = c.j;
			}
		}
		if (maxRow < 0 || maxRow > landLimit-1) {
//			System.out.println("Error: Bottom row in cells is out of range: " + maxRow);
			return null;
		}
		if (maxCol < 0 || maxCol > landLimit-1) {
//			System.out.println("Error: Rightmost column in cells is out of range: " + maxCol);
			return null;
		}
//		Cell topRightOfCell=new Cell(minRow,maxCol);
		if(bottomRight==null||!isValid(bottomRight)){
//			System.out.println("Error: bottomRight is null or invalid: "+bottomRight);
			return null;
		}
		int newI=bottomRight.i-maxRow;
		int newJ=bottomRight.j-maxCol;
		if(newI<0||newJ<0||newI>landLimit-1||newJ>landLimit-1){
//			System.out.println("Error: Invalid coordinates for shifted bottom right corner of building i: "+newI+" j: "+newJ);
			return null;
		}
		return new Cell(newI,newJ);
	}
	public static boolean isValid(Cell c){
		int landLimit=Player.staticLandSize;
		if(c.i<0||c.i>landLimit-1)
			return false;
		if(c.j<0||c.j>landLimit-1)
			return false;
		return true;
	}
}
