package pentos.g6;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Building.Type;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;
import pentos.util.Row;

public class Player implements pentos.sim.Player {

	private static int[] numFactoryRowsPerSize = { 5, 4, 3, 2 };
	private static int factoryRowSizeShift = 2;
	private static int[] numResidenceRowsPerSize = { 12, 6 };
	private static int residenceRowSizeShift = 2;
	
	private HashMap<Integer, Set<Row>> factoryRows = new HashMap<Integer, Set<Row>>();
	private HashMap<Integer, Set<Row>> residenceRows = new HashMap<Integer, Set<Row>>();
	
	@Override
	public void init() {
		int currentRow = 0;
		int rowNumber= 0;
		for (int i = 0; i < numFactoryRowsPerSize.length; ++i) {
			for (int j = 0; j < numFactoryRowsPerSize[i]; j++) {
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
			}
		}
		currentRow = 0 ;
		rowNumber = 0;
		for (int i = 0; i < numResidenceRowsPerSize.length; ++i) {
			for (int j = 0; j < numResidenceRowsPerSize[i]; j++) {
				int roadLocation;
				if(rowNumber % 2 == 0) {
					roadLocation = currentRow - 1;
				} else {
					roadLocation = currentRow + i + residenceRowSizeShift;
				}
				Row row = new Row(currentRow, currentRow + i + residenceRowSizeShift,roadLocation);
				if (!residenceRows.containsKey(i + residenceRowSizeShift)) {
					residenceRows.put(i + residenceRowSizeShift, new HashSet<Row>());
				}
				residenceRows.get(i + residenceRowSizeShift).add(row);
				
				if (rowNumber % 2 == 0) {
					currentRow = currentRow + i + residenceRowSizeShift;
				} else {
					currentRow = currentRow + i + residenceRowSizeShift + 1;
				}
			}
		}
	}

	@Override
	public Move play(Building request, Land land) {

		if(request.getType() == Type.FACTORY){
			int[] factoryDimensions = getBuildingDimensions(request);
			
			Row bestRow = null;
			int minLength = -1;
			boolean rotate = false;
			
			//This is checking for the best row if you use the first dimension of the factory
			System.out.println("Dimensions: " + factoryDimensions[0] +'\t'+factoryDimensions[1]);
			for(Row row : factoryRows.get(factoryDimensions[0])){
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
			//This is checking for the best row if you use the second dimension of the factory
			if(factoryDimensions[0]!=factoryDimensions[1]){ //This makes sure that Dim2 isn't the same as Dim1
				for(Row row : factoryRows.get(factoryDimensions[1])){
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

			//Suppose no best row was found, you should reject the request
			if(bestRow==null){
				System.out.println("Rejecting because no bestRow was found.");
				return new Move(false);
			}
			
			
			boolean accept = true;
			Cell location = new Cell(bestRow.getStart(), bestRow.getCurrentLocation());
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
			//ToDo: Figure out how to make the roads
			
		
			Set<Cell> water = new HashSet<Cell>(); //This stays empty. No water
			Set<Cell> park = new HashSet<Cell>(); //This stays empty. No parks
			
			//Update currentLocation! 
			bestRow.setCurrentLocation((rotate) ? factoryDimensions[0] : factoryDimensions[1]);
			return new Move(accept, request, location, rotation, road, water, park);
			
		}
		else{
			throw new RuntimeException("Unhandled Building request.");
		}
	}

	public int[] getBuildingDimensions(Building factory){
		if(factory.getType() != Type.FACTORY){
			throw new RuntimeException("Incorrect building type inputted.");
		}

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

	public Move padding(Building request, int rotation, Land land, Row currentRow) {
		int rowMin = Integer.MAX_VALUE, rowMax = Integer.MIN_VALUE;
		int colMin = Integer.MAX_VALUE, colMax = Integer.MIN_VALUE;
		Iterator<Cell> iter = request.rotations()[rotation].iterator();
		int paddType = 1;
		// Map<Integer,Map<Integer,Cell>> cellMap = new HashMap<>();
		boolean[][] hasBuildingCell = new boolean[50][50];
		Arrays.fill(hasBuildingCell, false);
		while (iter.hasNext()) {
			Cell temp = iter.next();
			hasBuildingCell[temp.i + currentRow.getStart() - 1][currentRow.getCurrentLocation() - temp.j + 1] = true;
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}
		for (int i = currentRow.getStart(); i < currentRow.getEnd()
				&& currentRow.getCurrentLocation() != land.side - 1; i++) {
			if (land.isField(i, currentRow.getCurrentLocation() + 1))
				paddType = 1;// build field
			else if (land.isPond(i, currentRow.getCurrentLocation() + 1))
				paddType = 2;// build pond
		}

		//padd water and park
		Set<Cell> water = new HashSet<>();
		Set<Cell> park = new HashSet<>();
		Set<Cell> road = new HashSet<>();
		for (int i = currentRow.getStart(); i < currentRow.getEnd(); i++) {
			for (int j = currentRow.getCurrentLocation(); j > currentRow.getCurrentLocation() - colMax + colMin
					+ 1; j--) {
				if (!hasBuildingCell[i][j]) {
					if (paddType == 1)
						water.add(new Cell(i, j));
					else
						park.add(new Cell(i, j));
				}
			}
		}
		//add cells to water or park make it at least 4
		if(paddType==1&&water.size()<4){
			for(int i = currentRow.getStart();i<currentRow.getEnd();i++)
				water.add(new Cell(1,currentRow.getCurrentLocation() - colMax + colMin
						+ 1));
		}else if(paddType==2&&park.size()<4){
			for(int i = currentRow.getStart();i<currentRow.getEnd();i++)
				park.add(new Cell(1,currentRow.getCurrentLocation() - colMax + colMin
						+ 1));
		}
		//padd road
		for(int i = currentRow.getCurrentLocation();i>currentRow.getCurrentLocation() - colMax + colMin
				+ 1;i--)
			water.add(new Cell(currentRow.getRoadLocation(),i));
		
		return new Move(true,request,new Cell(currentRow.getEnd(),currentRow.getCurrentLocation())
				,rotation,road,water,park);
	}

	public Move fillCell(Iterator cells, int fillType) {
		Set<Cell> paddedCells = new HashSet<>();

		return null;
	}

}
