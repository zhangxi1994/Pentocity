package pentos.g6;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;
import pentos.util.Row;

public class Player implements pentos.sim.Player {
	
	private static int[] numFactoryRowsPerSize = {5, 4, 3, 2};
	private static int factoryRowSizeShift = 2;
	private static int[] numResidenceRowsPerSize = {12, 6};
	private static int residenceRowSizeShift = 2;
	
	private HashMap<Integer, Set<Row>> factoryRows;
	private HashMap<Integer, Set<Row>> residenceRows;
	
	private enum ResidenceType {LINE, L_FACE, R_FACE, INV_L, L, R_BLK, L_BLK, INV_LIGHTNING,
								LIGHTNING, T, U, RANGLE, STEPS, PLUS, L_TOTEM, R_TOTEM, INV_Z, Z};
	
	@Override
	public void init() {
		int currentRow = 0;
		for (int i = 0; i < numFactoryRowsPerSize.length; ++i) {
			for (int j = 0; j < numFactoryRowsPerSize[i]; ++j) {
				Row row = new Row(currentRow, currentRow + i + factoryRowSizeShift);
				if (! factoryRows.containsKey(i + factoryRowSizeShift)) {
					factoryRows.put(i + factoryRowSizeShift, new HashSet<Row>());
				}
				factoryRows.get(i + factoryRowSizeShift).add(row);
			}
		}
		
		for (int i = 0; i < numResidenceRowsPerSize.length; ++i) {
			for (int j = 0; j < numResidenceRowsPerSize[i]; ++j) {
				Row row = new Row(currentRow, currentRow + i + residenceRowSizeShift);
				if (! residenceRows.containsKey(i + residenceRowSizeShift)) {
					residenceRows.put(i + residenceRowSizeShift, new HashSet<Row>());
				}
				residenceRows.get(i + residenceRowSizeShift).add(row);
			}
		}
	}

	@Override
	public Move play(Building request, Land land) {
		
		if(request.getType() == Type.FACTORY){
			int[] factoryDimensions = getBuildingDimensions(request);
			
			Row bestRow = null;
			
			for(Row row : factoryRows.get(factoryDimensions[0])){
				
			}
			
			
		}
		else{
			// Received request for residence
			
			// TODO: (future) Figure out building shape
			
			/* Use rotation that has least number of cells in the leftmost row
			 * If one of the dimensions is size 2, prefer that as the height
			 */
			int rotation = 0;
			int minCellsOnLeft = Integer.MAX_VALUE;
			boolean is2 = false;
			Building[] rotations = request.rotations();
			for (int i = 0; i < rotations.length; ++i) {
				int[] residenceDimensions = getBuildingDimensions(rotations[i]);
				if(residenceDimensions[1] == 2) {
					// Size 2
					int leftCells = countCellsOnLeft(rotations[i]);
					if (is2) {
						if (leftCells < minCellsOnLeft) {
							minCellsOnLeft = leftCells;
							rotation = i;
						}
					} else {
						is2 = true;
						rotation = i;
						minCellsOnLeft = leftCells;
					}
				} else {
					// Size 3 rotation
					if (is2) {
						// Forget this rotation
					} else {
						int leftCells = countCellsOnLeft(rotations[i]);
						if (leftCells < minCellsOnLeft) {
							minCellsOnLeft = leftCells;
							rotation = i;
						}
					}
				}
			}
			Set<Row> possibleRows;
			if (is2) {
				possibleRows = residenceRows.get(2);
			} else {
				possibleRows = residenceRows.get(3);
			}
			
			/*
			 *  Now we have the rotation we want, and the set of rows we can put it in
			 */
			
			Building rotatedRequest = request.rotations()[rotation];
			
			Row bestRow = null;
			int bestLocation = Integer.MIN_VALUE;
			for (Row row : possibleRows) {
				if (residenceRowExtendable(land, row, rotatedRequest) && row.getCurrentLocation() > bestLocation) {
					bestLocation = row.getCurrentLocation();
					bestRow = row;
				}
			}
			
			// If it is still null, it means we didn't find the row to place it
			if (bestRow == null) {
				return new Move(false);
			}
			
			// TODO: (Future) align building so that padding is on opposite side of road
			
			// All decided, now generate the complete
			Move move = padding(request, rotation, land, bestRow);
			return move;
		}
		
		return null;
	}
	
	public int[] getBuildingDimensions(Building factory){
		if(factory.getType() != Type.FACTORY){
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		int rowMin = Integer.MAX_VALUE, rowMax = Integer.MIN_VALUE, colMin = Integer.MAX_VALUE, colMax = Integer.MIN_VALUE;
		Iterator<Cell> iter = factory.iterator();
		
		while(iter.hasNext()){
			Cell temp = iter.next();
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}
		
		int height = rowMax - rowMin;
		int width = colMax - colMin;
		
		return new int[]{height, width};
	}
	
	public boolean factoryRowExtendable(Row row, int extendBy, Land land, Building factory){
		if(factory.getType() != Type.FACTORY){
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		for(int i=0; i<row.size(); i++){
			for(int j=row.getCurrentLocation(); j < row.getCurrentLocation()+extendBy; j++){
				if(!land.buildable(factory, new Cell(i,j))){
					return false;
				}
			}
		}
		return true;
	}

	private static boolean residenceRowExtendable(Land land, Row row, Building residence) {
		if (residence.getType() != Type.RESIDENCE) {
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		int maxWidth = Integer.MIN_VALUE;
		for (Cell cell : residence) {
			if (cell.i > maxWidth) {
				maxWidth = cell.i;
			}
		}
		
		// Check if you can build to the left starting from currentLocation
		if (! land.buildable(residence, new Cell(row.getStart(), row.getCurrentLocation() - maxWidth))) {
			return true;
		} else {
			return false;
		}
	}
	
	private static int countCellsOnLeft(Building building) {
		int minRow = Integer.MAX_VALUE;
		int numCellsAtMin = 0;
		Iterator<Cell> iterator = building.iterator();
		while(iterator.hasNext()) {
			Cell cell = iterator.next();
			if (cell.i < minRow) {
				minRow = cell.i;
				numCellsAtMin = 1;
			} else if (cell.i == minRow) {
				numCellsAtMin++;
			}
		}
		return numCellsAtMin;
	}
}
