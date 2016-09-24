package pentos.g6;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;
import pentos.sim.Move;

public class MyPadding implements Padding {

	@Override
	public Move getPadding(Building request, int rotation, Land land, Row row, int location) {
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
		while (iter.hasNext()) {
			Cell temp = iter.next();
			hasBuildingCell[temp.i + row.getStart()][row.getCurrentLocation() - colMax + temp.j] = true;
		}
		for (int i = row.getStart(); i < row.getEnd()
				&& row.getCurrentLocation() != land.side - 1; i++) {
			if (land.isField(i, row.getCurrentLocation() + 1))
				paddType = 1;// build field
			else if (land.isPond(i, row.getCurrentLocation() + 1))
				paddType = 2;// build pond
		}

		// Add water and park by filling blank spaces
		Set<Cell> water = new HashSet<>();
		Set<Cell> park = new HashSet<>();
		Set<Cell> road = new HashSet<>();
		for (int i = row.getStart(); i < row.getEnd(); i++) {
			for (int j = row.getCurrentLocation(); j > row.getCurrentLocation() - colMax + colMin	-1; j--) {
				if (!hasBuildingCell[i][j]) {
					if (paddType == 1)
						water.add(new Cell(i, j));
					else
						park.add(new Cell(i, j));
				}
			}
		}
		
		/*// Adding extra water cells
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
		}*/
		
		// Add road when necessary
		if(row.getRoadLocation()>0 && row.getRoadLocation()<50) {
			for(int i = row.getCurrentLocation();i>row.getCurrentLocation() - colMax + colMin	- 1; 
					i--) {
				if (land.unoccupied(row.getRoadLocation(),i)) {
					road.add(new Cell(row.getRoadLocation(),i));
				}
			}

		}
		
		int width = colMax - colMin + 1;
		row.setCurrentLocation(row.getCurrentLocation() - width);
		
		if (width == 5 && row.getStart() < row.getRoadLocation()) {
			Set<Cell> newWater = new HashSet<>();
			for (Cell cell : water) {
				newWater.add(new Cell(cell.i-1, cell.j));
			}
			
			Set<Cell> newPark = new HashSet<>();
			for (Cell cell : park) {
				newPark.add(new Cell(cell.i-1, cell.j));
			}
			
			return new Move(true,request,new Cell(row.getStart() + 1,
					row.getCurrentLocation() + 1),
					rotation, road, newWater, newPark);
		}
		
		return new Move(true,request,new Cell(row.getStart(),row.getCurrentLocation() + 1),rotation,road,water,park);
	}

}
