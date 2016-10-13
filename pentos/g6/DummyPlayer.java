package pentos.g6;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class DummyPlayer implements pentos.sim.Player {

	private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();

	public void init() { // function is called once at the beginning before play
							// is called

	}
	
	public void initializeRoadCells(Land land){
		for(int i=0; i<land.side; i++){
			for(int j=0; j<land.side; j++){
				Cell p = new Cell(i,j);
				if(land.getCellType(p)==Cell.Type.ROAD){
					road_cells.add(p);
				}				
			}
		}
	}
	
	public Move leastRoadMove(Building request, Land land){
		//find all valid building locations and orientations
		ArrayList<Move> moves = new ArrayList<Move>();
		if(request.type == Building.Type.FACTORY){
			//searching top-down, left-right
			for(int j=0; j<land.side; j++){
				for(int i=0; i<land.side; i++){
					Cell p = new Cell(i,j);
					Building[] rotations = request.rotations();
					for(int ri=0; ri < rotations.length; ri++){
						Building b = rotations[ri];
						if(land.buildable(b, p))
							moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
					}
				}
			}
		}
		else if(request.type == Building.Type.RESIDENCE){
			//Searching top-down, right-left
			for(int i=0; i<land.side; i++){
				for(int j=land.side; j>=0; j--){
					Cell p = new Cell(i,j);
					Building[] rotations = request.rotations();
					for(int ri=0; ri < rotations.length; ri++){
						Building b = rotations[ri];
						if(land.buildable(b, p))
							moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
					}
				}
			}
		}
		//Reject is no valid placements exist
		if(moves.isEmpty())
			return new Move(false);
		
		ArrayList<Move> moves2 = new ArrayList<Move>();
		//Iterate through the moves and find their shortest roads for each
		for(int i=0; i<moves.size(); i++){
			Move mc = moves.get(i);
			// get coordinates of building placement (position plus local
						// building cell coordinates)
						Set<Cell> shiftedCells = new HashSet<Cell>();
						for (Cell x : mc.request.rotations()[mc.rotation])
							shiftedCells.add(new Cell(x.i + mc.location.i, x.j + mc.location.j));
						// builda road to connect this building to perimeter
						Set<Cell> roadCells = findShortestRoad(shiftedCells, land);
						if(roadCells == null){
							if(isNextToRoad(mc.location.i, mc.location.j, mc.request, land)){
								continue;
							}
						}
						else {
							mc.road = roadCells;
							moves2.add(mc);
						}
		}
		moves = moves2;
		
		//Reject is no valid placements exist
		if(moves.isEmpty())
			return new Move(false);
		
		//Find minimum road length move
		int minimumIndex =0;
		for(int i=0; i<moves.size(); i++){
			if(moves.get(i).road.size() < moves.get(minimumIndex).road.size())
				minimumIndex = i;
		}
		
		Move chosenMove = moves.get(minimumIndex);
		road_cells.addAll(chosenMove.road); //Adding this road to the set of roads
		return chosenMove;
	}

	public Move play(Building request, Land land){
		return leastRoadMove(request, land);
	}
	
	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();
		// add border cells that don't have a road currently
		// dummy cell to serve as road connector to perimeter cells
		Cell source = new Cell(Integer.MAX_VALUE, Integer.MAX_VALUE); 
		for (int z = 0; z < land.side; z++) {
			if (b.contains(new Cell(0, z)) || b.contains(new Cell(z, 0)) || b.contains(new Cell(land.side - 1, z))
					|| b.contains(new Cell(z, land.side - 1))) // if already on border don't build any roads
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

	private boolean isNextToRoad(int locI, int locJ, Building request, Land land){
		Iterator<Cell> cIt = request.iterator();
		while(cIt.hasNext()){
			Cell c = cIt.next();
			if(land.getCellType(locI + c.i, locJ + c.j+1)==Cell.Type.ROAD) return true;
			if(land.getCellType(locI + c.i, locJ + c.j-1)==Cell.Type.ROAD) return true;
			if(land.getCellType(locI + c.i+1, locJ + c.j)==Cell.Type.ROAD) return true;
			if(land.getCellType(locI + c.i-1, locJ + c.j)==Cell.Type.ROAD) return true;
		}
		return false;
	}
	
	// walk n consecutive cells starting from a building. Used to build a random
	// field or pond.
	private Set<Cell> randomWalk(Set<Cell> b, Set<Cell> marked, Land land, int n) {
		ArrayList<Cell> adjCells = new ArrayList<Cell>();
		Set<Cell> output = new HashSet<Cell>();
		for (Cell p : b) {
			for (Cell q : p.neighbors()) {
				if (land.isField(q) || land.isPond(q))
					return new HashSet<Cell>();
				if (!b.contains(q) && !marked.contains(q) && land.unoccupied(q))
					adjCells.add(q);
			}
		}
		if (adjCells.isEmpty())
			return new HashSet<Cell>();
		Cell tail = adjCells.get(gen.nextInt(adjCells.size()));
		for (int ii = 0; ii < n; ii++) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (Cell p : tail.neighbors()) {
				if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
					walk_cells.add(p);
			}
			if (walk_cells.isEmpty()) {
				// return output; //if you want to build it anyway
				return new HashSet<Cell>();
			}
			output.add(tail);
			tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		}
		return output;
	}

}
