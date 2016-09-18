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
	
	@Override
	public void init() {
		int currentRow = 0;
		for (int i = 0; i < numFactoryRowsPerSize.length; ++i) {
			for (int j = 0; j < numFactoryRowsPerSize[i]; j++) {
				Row row = new Row(currentRow, currentRow + i + factoryRowSizeShift);
				if (! factoryRows.containsKey(i + factoryRowSizeShift)) {
					factoryRows.put(i + factoryRowSizeShift, new HashSet<Row>());
				}
				factoryRows.get(i + factoryRowSizeShift).add(row);
			}
		}
		
		for (int i = 0; i < numResidenceRowsPerSize.length; ++i) {
			for (int j = 0; j < numResidenceRowsPerSize[i]; j++) {
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
			int[] factoryDimensions = getFactoryDimensions(request);
			
			Row bestRow = null;
			
			for(Row row : factoryRows.get(factoryDimensions[0])){
				
			}
			
			
		}
		else{
			throw new RuntimeException("Unhandled Building request.");
		}
		return null;
	}
	
	public int[] getFactoryDimensions(Building factory){
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

}
