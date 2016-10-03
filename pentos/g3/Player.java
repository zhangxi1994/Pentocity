package pentos.g3;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

	// private Random gen = new Random();
	private Set<Cell> road_cells = new HashSet<Cell>();
	private boolean road_built;
	private final int NUMBER_OF_RETRY = 10;


	public void init() { // function is called once at the beginning before play is called
		this.road_built = false;
	}

	public Move play(Building request, Land land) {
		// find all valid building locations and orientations
		ArrayList<Move> moves = findBuildableMoves(request, land);

		if (moves.isEmpty()) return new Move(false);
		
		// find all objective function values of each move, means "how good the move is"
		ArrayList<Integer> objs = findObjectiveOfMoves(moves, land, request);
		ArrayList<Integer> indexes = findSmallestObjs(objs, NUMBER_OF_RETRY);
		
		Move chosen = new Move(false);
		Set<Cell> shiftedCells = new HashSet<Cell>();
		Set<Cell> roadCells = new HashSet<Cell>();
		
		for (Integer index : indexes) {
			chosen = moves.get(index);
			shiftedCells = shiftedCellsFromMove(chosen);
			roadCells = findShortestRoad(shiftedCells, land);
			if (roadCells != null) break;
		}
		/*
		if (!road_built) {
			roadCells = buildRoad(land.side);
			road_built = true;
		}
		*/
		if (roadCells != null) {
			chosen.road = roadCells;
			road_cells.addAll(roadCells);
			/*
			if (request.type == Building.Type.RESIDENCE) { // for residences, build random ponds and fields connected to it
				Set<Cell> markedForConstruction = new HashSet<Cell>();
				markedForConstruction.addAll(roadCells);
				chosen.water = randomWalk(shiftedCells, markedForConstruction, land, 4);
				markedForConstruction.addAll(chosen.water);
				chosen.park = randomWalk(shiftedCells, markedForConstruction, land, 4);
			}
			*/
			return chosen;
		} else {
			return new Move(false);
		}
	}

	private ArrayList<Move> findBuildableMoves(Building request, Land land) {
		ArrayList<Move> moves = new ArrayList<Move> ();
		for (int i = 0 ; i < land.side ; i++) {
			for (int j = 0 ; j < land.side ; j++) {
				Cell p = new Cell(i, j);
				Building[] rotations = request.rotations();
				for (int ri = 0 ; ri < rotations.length ; ri++) {
					Building b = rotations[ri];
					if (land.buildable(b, p)) {
						moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
					}
				}
			}
		}
		return moves;
	}

	private Set<Cell> shiftedCellsFromMove(Move move) {
		Set<Cell> shiftedCells = new HashSet<Cell>();
		for (Cell x : move.request.rotations()[move.rotation])
			shiftedCells.add(new Cell(x.i+move.location.i,x.j+move.location.j));
		return shiftedCells;
	}

	private int edgeIncrease(Set<Cell> shiftedCells, Land land) {
		int edge_increase = 0; // the increase of new edge: the less, the better
		for (Cell shiftedCell : shiftedCells) {
			for (Cell adjCell : shiftedCell.neighbors()) {
				if (!shiftedCells.contains(adjCell) && land.unoccupied(adjCell))
					edge_increase += 1;
				if (!shiftedCells.contains(adjCell) && !land.unoccupied(adjCell))
					edge_increase -= 2;
			}
		}
		return edge_increase;
	}

	private ArrayList<Integer> findObjectiveOfMoves(ArrayList<Move> moves, Land land, Building request) {
		ArrayList<Integer> objs = new ArrayList<Integer> ();
		int i = 0;
		for (Move move : moves) {
			Set<Cell> shiftedCells = shiftedCellsFromMove(move);
			int obj = 0;
			obj += (request.type == Building.Type.FACTORY ? i : -i);
			obj += 100 * edgeIncrease(shiftedCells, land);
			// include other objective functions
			objs.add(obj);
			i += 1;
		}

		return objs;
	}

	private ArrayList<Integer> findSmallestObjs(ArrayList<Integer> objs, int n) {
		int index = 0;
		int j = 0;
		int[] objIndex = new int[n];
		int[] objValue = new int[n];
		ArrayList<Integer> objList = new ArrayList<Integer> ();
		for (j = 0; j < n; j++) {
			objIndex[j] = 0;
			objValue[j] = Integer.MAX_VALUE;
		}
		for (Integer obj : objs) {
			for (j = n-1; j >= 0; j--) {
				if (obj < objValue[j]) {
					if (j != n-1) {
						objValue[j+1] = objValue[j];
						objIndex[j+1] = objIndex[j];
					}
					objValue[j] = obj;
					objIndex[j] = index;
				}
			}
			index += 1;
		}
		for (j = 0; j < n; j ++) {
			if (objValue[j] != Integer.MAX_VALUE) objList.add(objIndex[j]);
		}
		return objList;
	}

	private boolean isReached(Set<Cell> cells, Land land) {
		for (int z=0; z<land.side; z++) {
			if (
				cells.contains(new Cell(0, z)) || 
				cells.contains(new Cell(z, 0)) || 
				cells.contains(new Cell(land.side-1, z)) || 
				cells.contains(new Cell(z, land.side-1))
			) {
				return true;
			}		
		}
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (cells.contains(q)) return true;
			}
		}
		return false;
	}

	// build shortest sequence of road cells to connect to a set of cells b
	private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
		Set<Cell> output = new HashSet<Cell>();
		boolean[][] checked = new boolean[land.side][land.side];
		Queue<Cell> queue = new LinkedList<Cell>();

		if (isReached(b, land)) return output;

		Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); 
		for (int z=0; z<land.side; z++) {
			if (land.unoccupied(0, z))
				queue.add(new Cell(0, z, source));
			if (land.unoccupied(z, 0))
				queue.add(new Cell(z, 0, source));
			if (land.unoccupied(z, land.side-1))
				queue.add(new Cell(z, land.side-1, source));
			if (land.unoccupied(land.side-1, z))
				queue.add(new Cell(land.side-1, z, source));
		}
		// add cells adjacent to current road cells
		for (Cell p : road_cells) {
			for (Cell q : p.neighbors()) {
				if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q)) 
					queue.add(new Cell(q.i, q.j, p)); 
			}
		}
		while (!queue.isEmpty()) {
			Cell p = queue.remove();
			checked[p.i][p.j] = true;
			for (Cell x : p.neighbors()) {		
				if (b.contains(x)) { 
					Cell tail = p;
					while (!b.contains(tail) && !road_cells.contains(tail) && !tail.equals(source)) {
						output.add(new Cell(tail.i,tail.j));
						tail = tail.previous;
					}
					if (!output.isEmpty())
						return output;
				}
				else if (!checked[x.i][x.j] && land.unoccupied(x.i,x.j)) {
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

	/*
	// walk n consecutive cells starting from a building. Used to build a random field or pond. 
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
		if (adjCells.isEmpty()) {
			return new HashSet<Cell>();
		}
		Cell tail = adjCells.get(gen.nextInt(adjCells.size()));
		for (int ii=0; ii<n; ii++) {
			ArrayList<Cell> walk_cells = new ArrayList<Cell>();
			for (Cell p : tail.neighbors()) {
				if (!b.contains(p) && !marked.contains(p) && land.unoccupied(p) && !output.contains(p))
					walk_cells.add(p);		
			}
			if (walk_cells.isEmpty()) {
				//return output; //if you want to build it anyway
				return new HashSet<Cell>();
			}
			output.add(tail);	    
			tail = walk_cells.get(gen.nextInt(walk_cells.size()));
		}
		return output;
	}
	*/
}
