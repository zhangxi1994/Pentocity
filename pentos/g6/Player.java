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
import pentos.g0.*;

public class Player implements pentos.sim.Player {

	private static int[] numFactoryRowsPerSize = { 5, 4, 3, 2 };
	private static int factoryRowSizeShift = 2;
	private static int[] numResidenceRowsPerSize = { 6, 4, 1 };
	private static int residenceRowSizeShift = 3;
	private static int rejectNum = 0;
	
	private HashMap<Integer, Set<Row>> factoryRows = new HashMap<Integer, Set<Row>>();
	private HashMap<Integer, Set<Row>> residenceRows = new HashMap<Integer, Set<Row>>();
	
	private enum ResidenceType {LINE, L_FACE, R_FACE, INV_L, L, R_BLK, L_BLK, INV_LIGHTNING,
								LIGHTNING, T, U, RANGLE, STEPS, PLUS, L_TOTEM, R_TOTEM, INV_Z, Z};
	
	int numberOfRejections = 0; //Counts the number of rejections.
	
	@Override
	public void init() {
		initializeFactoryRows();
		initializeResidenceRows();
	}

	private void initializeFactoryRows() {
		// Initializing factory rows
		int currentRow = 0;
		int rowNumber= 0;
		for (int i = 0; i < numFactoryRowsPerSize.length; ++i) {
			for (int j = 0; j < numFactoryRowsPerSize[i]; ++j) {
				int roadLocation;
				if(rowNumber % 2 == 0) {
					roadLocation = currentRow - 1;
				} else {
					roadLocation = currentRow + i + factoryRowSizeShift;
				}
				Row row = new Row(currentRow, currentRow + i + factoryRowSizeShift, roadLocation);
				if (!factoryRows.containsKey(i + factoryRowSizeShift)) {
					factoryRows.put(i + factoryRowSizeShift, new HashSet<Row>());
				}
				factoryRows.get(i + factoryRowSizeShift).add(row);

				if (rowNumber % 2 == 0) {
					currentRow = currentRow + i + factoryRowSizeShift;
				} else {
					currentRow = currentRow + i + factoryRowSizeShift + 1;
				}
				rowNumber++;
			}
		}
	}

	private void initializeResidenceRows() {
		// Initializing residence rows, accounting for both parks and roads in the middle
		int currentRow = 0;
		int rowNumber= 0;
		for (int i = 0; i < numResidenceRowsPerSize.length; ++i) {
			for (int j = 0; j < numResidenceRowsPerSize[i]; ++j) {
				int roadLocation, parkLocation;
				if(rowNumber % 2 == 0) {
					roadLocation = currentRow - 1;
					parkLocation = currentRow + i + residenceRowSizeShift;
				} else {
					roadLocation = currentRow + i + residenceRowSizeShift;
					parkLocation = currentRow - 1;
				}
				Row row = new Row(currentRow, 
									currentRow + i + residenceRowSizeShift, 
									roadLocation, 
									parkLocation,
									49);
				if (!residenceRows.containsKey(i + residenceRowSizeShift)) {
					residenceRows.put(i + residenceRowSizeShift, new HashSet<Row>());
				}
				residenceRows.get(i + residenceRowSizeShift).add(row);
				
				currentRow = currentRow + i + residenceRowSizeShift + 1;
				rowNumber++;
			}
		}		
	}
	
	@Override
	public Move play(Building request, Land land) {

		if(rejectNum==2){
			DummyPlayer player = new DummyPlayer();
			return player.play(request, land);
		}
		if(request.getType() == Type.FACTORY){
			Move move = generateFactoryMove(request, land);
			if(move.accept==false) rejectNum++;
			return move;
			
		} else {
			// Received request for residence
			Move move = generateResidenceMove(request, land);
			if(move.accept==false) rejectNum++;
			return move;
		}
	}
	
	private Move generateFactoryMove(Building request, Land land){
		if(request.getType() != Type.FACTORY){
			throw new RuntimeException("Calling factory play on non-factory");
		}
		
		int[] factoryDimensions = getBuildingDimensions(request);
		
		System.out.println("Factory Request: " + factoryDimensions[0] + "\t" + factoryDimensions[1]);
		
		Row bestRow = null;
		int minLength = -1;
		boolean rotate = false;
		
		int promotionBump=-1;
		while(bestRow==null && promotionBump <= 3){
			promotionBump++;
			//This is checking for the best row if you use the first dimension of the factory
			if(factoryDimensions[0] + promotionBump <= 5){ //Gotta make sure it's a valid dimension request
				for(Row row : factoryRows.get(factoryDimensions[0]+promotionBump)){
					if(!factoryRowExtendable(row, land, request.rotations()[0])){
						continue;
					}
					else{
						if(bestRow == null){
							bestRow = row;
							minLength = bestRow.getCurrentLocation() + factoryDimensions[1];
							rotate = false;
						}
						else{
							if(row.getCurrentLocation() + factoryDimensions[1] < minLength){
								bestRow = row;
								minLength = bestRow.getCurrentLocation() + factoryDimensions[1];
								rotate = false;
							}
						}
					}
				}
			}	
			//This is checking for the best row if you use the second dimension of the factory
			if(factoryDimensions[0]!=factoryDimensions[1]){ //This makes sure that Dim2 isn't the same as Dim1
				if(factoryDimensions[1] + promotionBump <= 5){	//This makes sure it's a valid dimension request
					for(Row row : factoryRows.get(factoryDimensions[1]+promotionBump)){
						if(!factoryRowExtendable(row, land, request.rotations()[1])){
							continue;
						}
						else{
							if(bestRow == null){
								bestRow = row;
								minLength = bestRow.getCurrentLocation() + factoryDimensions[0];
								rotate = true;
							}
							else{
								if(row.getCurrentLocation() + factoryDimensions[0] < minLength){
									bestRow = row;
									minLength = bestRow.getCurrentLocation() + factoryDimensions[0];
									rotate = true;
								}
							}
						}
					}
				}
			}
		}
		
		
		//Suppose no best row was found, you should reject the request
		if(bestRow==null){
			System.out.println("Rejecting because no bestRow was found.");
			numberOfRejections++;
			return new Move(false);
		}
		
		
		boolean accept = true;
		int yLoc = (bestRow.getRoadLocation() > bestRow.getStart()) ? bestRow.getStart() + promotionBump : bestRow.getStart();
//		int yLoc = bestRow.getStart();
		System.out.println("Placing at " + bestRow.getCurrentLocation() + ", " + yLoc);
		Cell location = new Cell(yLoc, bestRow.getCurrentLocation());
		int rotation = (rotate) ? 1 : 0;
		
		Set<Cell> road = new HashSet<Cell>();
		if(bestRow.getStart() == 0){
			//This means bestRow is on the top edge and needs no roads
			//Do nothing
		}
		else if(bestRow.getEnd() == 50){
			//This means bestRow is on the bottom edge and needs no roads
			//Do nothing
		}
		else{
			int extension = (rotate) ? factoryDimensions[0] : factoryDimensions[1];
			for(int i=0; i<extension; i++){
				if(!land.unoccupied(bestRow.getRoadLocation(), bestRow.getCurrentLocation() + i)){
					//This means it should already be road, so don't do anything. There might be a logic issue here.
					//return new Move(false);
					continue;
				}
				else{
					road.add(new Cell(bestRow.getRoadLocation(), bestRow.getCurrentLocation() + i));
				}
			}
		}
		
	
		Set<Cell> water = new HashSet<Cell>(); //This stays empty. No water
		Set<Cell> park = new HashSet<Cell>(); //This stays empty. No parks
		
		//Update currentLocation! 
		bestRow.setCurrentLocation((rotate) ? bestRow.getCurrentLocation() + factoryDimensions[0] : bestRow.getCurrentLocation() + factoryDimensions[1]);
		return new Move(accept, request, location, rotation, road, water, park);
		
	}

	private Move generateResidenceMove(Building request, Land land) {
		/* Use rotation that has least number of cells in the leftmost row for now
		 * If one of the dimensions is size 4 or more, prefer that as the height
		 */
		int rotation = 0;
		int minCellsOnLeft = Integer.MAX_VALUE;
		boolean is4 = false;
		boolean is5 = false;
		Building[] rotations = request.rotations();
		for (int i = 0; i < rotations.length; ++i) {
			int[] residenceDimensions = getBuildingDimensions(rotations[i]);
			if (residenceDimensions[0] == 5) {
				// If size 5, use this rotation
				is5 = true;
				rotation = i;
				break;
			} else if(residenceDimensions[0] == 4) {
				// Size 4
				int leftColumnCells = countCellsOnLeft(rotations[i]);
				if (is4) {
					if (leftColumnCells < minCellsOnLeft) {
						minCellsOnLeft = leftColumnCells;
						rotation = i;
					}
				} else {
					is4 = true;
					rotation = i;
					minCellsOnLeft = leftColumnCells;
				}
			} else {
				// Size 3 or less
				if (is4) {
					// Forget this rotation
				} else if (residenceDimensions[0] == 3) {
					// Only consider height 3s
					int leftColumnCells = countCellsOnLeft(rotations[i]);
					if (leftColumnCells < minCellsOnLeft) {
						minCellsOnLeft = leftColumnCells;
						rotation = i;
					}
				}
			}
		}
		Set<Row> possibleRows;
		if (is5) {
			possibleRows = residenceRows.get(5);
		}else if (is4) {
			possibleRows = residenceRows.get(4);
		} else {
			possibleRows = residenceRows.get(3);
		}
		
		/*
		 *  Now we have the rotation we want, with the set of rows we can put it in
		 */
		
		Building rotatedRequest = request.rotations()[rotation];
		
		Row bestRow = null;
		int bestLocation = Integer.MIN_VALUE;
		for (Row row : possibleRows) {
			int positionInRow = residenceRowExtendPosition(land, row, rotatedRequest);
			if (positionInRow >= 0 && positionInRow > bestLocation) {
				bestLocation = positionInRow;
				bestRow = row;
			}
		}
		
		// If it is still null, it means we didn't find the row to place it
		if (bestRow == null) {
			numberOfRejections++;
			return new Move(false);
		}
		
		// All decided, now generate the complete move
		Padding padding = new MyPadding();
		Move move;
		if (bestRow.getRecentlyPadded()) {
			move = padding.getPadding(request, rotation, land, bestRow, bestLocation, false);
			bestRow.setWasNotRecentlyPadded();
		} else {
			move = padding.getPadding(request, rotation, land, bestRow, bestLocation, true);
			bestRow.setWasRecentlyPadded();
		}
		if(!land.buildable(move.request.rotations()[move.rotation], move.location)) {
			System.out.println("***Cannot build***"+move.location.i+","+move.location.j);
		}
		System.out.println(move.location.i+","+move.location.j);
		//System.out.println("***Can build***");
		return move;
	}

	public int[] getBuildingDimensions(Building factory){
		int rowMin = Integer.MAX_VALUE, rowMax = Integer.MIN_VALUE, colMin = Integer.MAX_VALUE,
				colMax = Integer.MIN_VALUE;
		Iterator<Cell> iter = factory.iterator();

		while (iter.hasNext()) {
			Cell temp = iter.next();
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}
		
		int height = rowMax - rowMin + 1;
		int width = colMax - colMin + 1;
		
		return new int[]{height, width};
	}
	
	public boolean factoryRowExtendable(Row row, Land land, Building factory){
		if(factory.getType() != Type.FACTORY){
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		int topCell = row.getStart();
		int leftCell = row.getCurrentLocation();
		
		return land.buildable(factory, new Cell(topCell,leftCell));

	}

/*	public Move padding(Building request, int rotation, Land land, Row currentRow) {
		int rowMin = Integer.MAX_VALUE, rowMax = Integer.MIN_VALUE;
		int colMin = Integer.MAX_VALUE, colMax = Integer.MIN_VALUE;
		Iterator<Cell> iter = request.rotations()[rotation].iterator();
		int paddType = 1;
		// Map<Integer,Map<Integer,Cell>> cellMap = new HashMap<>();
		boolean[][] hasBuildingCell = new boolean[50][50];
		for (boolean[] array : hasBuildingCell) {
			Arrays.fill(array, false);
		}
		while (iter.hasNext()) {
			Cell temp = iter.next();
			//hasBuildingCell[temp.i + currentRow.getStart() - 1][currentRow.getCurrentLocation() - temp.j + 1] = true;
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}
		iter = request.rotations()[rotation].iterator();
		int width = colMax - colMin + 1;
		while (iter.hasNext()) {
			Cell temp = iter.next();
			hasBuildingCell[temp.i + currentRow.getStart()][currentRow.getCurrentLocation() - colMax + temp.j] = true;
		}
		for (int i = currentRow.getStart(); i < currentRow.getEnd()
				&& currentRow.getCurrentLocation() != land.side - 1; i++) {
			if (land.isField(i, currentRow.getCurrentLocation() + 1))
				paddType = 1;// build field
			else if (land.isPond(i, currentRow.getCurrentLocation() + 1))
				paddType = 2;// build pond
		}

		// Add water and park by filling blank spaces
		Set<Cell> water = new HashSet<>();
		Set<Cell> park = new HashSet<>();
		Set<Cell> road = new HashSet<>();
		for (int i = currentRow.getStart(); i < currentRow.getEnd(); i++) {
			for (int j = currentRow.getCurrentLocation(); j > currentRow.getCurrentLocation() - colMax + colMin	-1; j--) {
				if (!hasBuildingCell[i][j]) {
					if (paddType == 1)
						water.add(new Cell(i, j));
					else
						park.add(new Cell(i, j));
				}
			}
		}
		
		// Adding extra water cells
		if(paddType == 1 && water.size() < 4){
			for(int i = currentRow.getStart();i<currentRow.getEnd();i++){
				if(currentRow.getCurrentLocation() - colMax + colMin + 1 < 50 &&
						currentRow.getCurrentLocation() - colMax + colMin + 1 > 0){
					water.add(new Cell(i,currentRow.getCurrentLocation() - colMax + colMin+ 1));
				}		
			}
				
		}
		// Adding extra park cells
		else if(paddType==2&&park.size()<4){
			for(int i = currentRow.getStart();i<currentRow.getEnd();i++)
				if(currentRow.getCurrentLocation() - colMax + colMin + 1 < 50 &&
						currentRow.getCurrentLocation() - colMax + colMin + 1 > 0){
					park.add(new Cell(i,currentRow.getCurrentLocation() - colMax + colMin+ 1));
				}	
		}
		
		// Add road when necessary
		if(currentRow.getRoadLocation()>0 && currentRow.getRoadLocation()<50) {
			for(int i = currentRow.getCurrentLocation();i>currentRow.getCurrentLocation() - colMax + colMin	- 1; 
					i--) {
				if (land.unoccupied(currentRow.getRoadLocation(),i)) {
					road.add(new Cell(currentRow.getRoadLocation(),i)); 
				}
			}

		}
		currentRow.setCurrentLocation(currentRow.getCurrentLocation() - width);
		
		if (width == 5 && currentRow.getStart() < currentRow.getRoadLocation()) {
			Set<Cell> newWater = new HashSet<>();
			for (Cell cell : water) {
				newWater.add(new Cell(cell.i-1, cell.j));
			}
			
			Set<Cell> newPark = new HashSet<>();
			for (Cell cell : park) {
				newPark.add(new Cell(cell.i-1, cell.j));
			}
			
			return new Move(true,request,new Cell(currentRow.getStart() + 1,
					currentRow.getCurrentLocation() + 1),
					rotation, road, newWater, newPark);
		}
		
		return new Move(true,request,new Cell(currentRow.getStart(),currentRow.getCurrentLocation() + 1),rotation,road,water,park);
	}*/

	public Move fillCell(Iterator cells, int fillType) {
		Set<Cell> paddedCells = new HashSet<>();

		return null;
	}

	private static int residenceRowExtendPosition(Land land, Row row, Building residence) {
		if (residence.getType() != Type.RESIDENCE) {
			throw new RuntimeException("Incorrect building type inputted.");
		}
		
		int position = row.getCurrentLocation();
		while (position >= 0) {
			if(land.buildable(residence, new Cell(row.getStart(), position))) {
				return position;
			}
			position--;
		}
		
		return -1;
	}
	
	private static int countCellsOnLeft(Building building) {
		int minCol = Integer.MAX_VALUE;
		int numCellsAtMin = 0;
		Iterator<Cell> iterator = building.iterator();
		while(iterator.hasNext()) {
			Cell cell = iterator.next();
			if (cell.j < minCol) {
				minCol = cell.j;
				numCellsAtMin = 1;
			} else if (cell.j == minCol) {
				numCellsAtMin++;
			}
		}
		return numCellsAtMin;
	}
}
