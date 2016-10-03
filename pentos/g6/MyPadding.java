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
	private int rowMin = Integer.MAX_VALUE;
	private int rowMax = Integer.MIN_VALUE;
	private int colMin = Integer.MAX_VALUE;
	private int colMax = Integer.MIN_VALUE;
	private int width = Integer.MIN_VALUE;
	int colLeft;
	int colRight;
	int rowTop;
	int rowBottom;
	private int[][] hasCell = new int[50][50];

	public MyPadding() {
		for (int[] array : hasCell) {
			Arrays.fill(array, 0);
		}
	}

	@Override
	public Move getPadding(Building request, int rotation, Land land, Row row, int location, boolean buildWater, int offSet) {
		getBuildingDetails(request.rotations()[rotation].iterator(), row);
		Iterator<Cell> iter = request.rotations()[rotation].iterator();
		colLeft = location - 1; // exclusive
		colRight = location + width - 1; // inclusive
		rowTop = row.getStart() + offSet;
		rowBottom = row.getEnd(); // cannot equal to
		int waterCells = 0;
		Set<Cell> water = new HashSet<>();
		Set<Cell> park = new HashSet<>();
		Set<Cell> road = new HashSet<>();
		int previousCol = -1;
		int isStraight = 0;
		while (iter.hasNext()) {
			Cell temp = iter.next();
			hasCell[temp.i + rowTop][location + temp.j] = 2;
			if (previousCol == -1)
				previousCol = temp.j;
			else if (previousCol == temp.j)
				isStraight++;
		}
		
		if (buildWater) {
			for (int i = row.getEnd() - 1, j = location; i >= row.getStart() && waterCells < 4 && isStraight < 4; i--) {
				if (hasCell[i][j] == 0 && land.unoccupied(i, j) && waterCells < 4) {
					water.add(new Cell(i, j));
					hasCell[i][j] = 1;
					waterCells++;
				}
				if (j + 1 < 50 && j + 1 <= colRight && hasCell[i][j + 1] == 0 && land.unoccupied(i, j + 1)
						&& waterCells < 4) {
					water.add(new Cell(i, j + 1));
					hasCell[i][j + 1] = 1;
					waterCells++;
				}
				if (i - 1 >= row.getStart() && hasCell[i - 1][j] == 0 && land.unoccupied(i - 1, j) && waterCells < 4) {
					water.add(new Cell(i - 1, j));
					hasCell[i - 1][j] = 1;
					waterCells++;
				}

				if (i - 1 >= row.getStart() && j + 1 < 50 && j + 1 <= colRight && hasCell[i - 1][j + 1] == 0
						&& land.unoccupied(i - 1, j + 1) && waterCells < 4) {
					water.add(new Cell(i - 1, j + 1));
					hasCell[i - 1][j + 1] = 1;
					waterCells++;
				}

				if (i + 1 < rowBottom && hasCell[i + 1][j] == 0 && land.unoccupied(i + 1, j) && waterCells < 4) {
					water.add(new Cell(i + 1, j));
					hasCell[i + 1][j] = 1;
					waterCells++;
				}

				if (i + 1 < rowBottom && j + 1 < 50 && j + 1 <= colRight && hasCell[i + 1][j + 1] == 0
						&& land.unoccupied(i + 1, j + 1) && waterCells < 4) {
					water.add(new Cell(i + 1, j + 1));
					// 1));
					hasCell[i + 1][j + 1] = 1;
					waterCells++;
				}
			}
			// check valid water cell
			if (water.size() > 2) {
				for (Iterator<Cell> iterator = water.iterator(); iterator.hasNext();) {
					if (!checkValidWaterCell(iterator.next())) {
						waterCells--;
						iterator.remove();
					}
				}
			}

			if (waterCells < 4 && isStraight < 4) {
				if (rowBottom - 1 >= 0 && colLeft >= 0 && checkValidWaterCell(new Cell(rowBottom - 1, colLeft))) {
					for (int i = rowBottom - 1, j = colLeft; i >= row.getStart() && waterCells < 4 && j >= 0&&land.unoccupied(i,j); i--) {
						water.add(new Cell(i, j));
						waterCells++;
					}
				} else {
					for (int i = row.getStart(), j = colLeft; i < rowBottom && waterCells < 4 && j >= 0&&land.unoccupied(i,j); i++) {
						water.add(new Cell(i, j));
						waterCells++;
					}
				}
			}

		}

		// Add road when necessary
		if (row.getRoadLocation() > 0 && row.getRoadLocation() < 50) {
			for (int i = row.getCurrentLocation(); i >= location; i--) {
				if (land.unoccupied(row.getRoadLocation(), i)) {
					road.add(new Cell(row.getRoadLocation(), i));
				}
			}
		}
		if (row.getParkLocation() > 0 && row.getParkLocation() < 50) {
			for (int i = row.getCurrentLocation(); i >= location; i--) {
				if (land.unoccupied(row.getParkLocation(), i)) {
					park.add(new Cell(row.getParkLocation(), i));
					row.setParkSize(row.getParkSize() + 1);
				}
			}
			int col = colLeft;
			while (row.getParkSize() < 4 && col >= 0&&location>46&&land.unoccupied(row.getParkLocation(),46)) {
				if (land.unoccupied(row.getParkLocation(), col)) {
					park.add(new Cell(row.getParkLocation(), col));
					row.setParkSize(row.getParkSize() + 1);
				}
				if (land.unoccupied(row.getRoadLocation(), col)) {
					road.add(new Cell(row.getRoadLocation(), col));
				}
				col--;
			}
		}
		row.setCurrentLocation(location - 1);
		return new Move(true, request, new Cell(rowTop, location), rotation, road, water, park);
	}

	public boolean checkValidWaterCell(Cell cell) {
		int i = cell.i;
		int j = cell.j;
		int count = 0;
		if (j + 1 < 50 && j + 1 <= colRight && hasCell[i][j + 1] == 1)
			count++;
		if (i - 1 >= rowTop && hasCell[i - 1][j] == 1)
			count++;
		if (i + 1 < rowBottom && hasCell[i + 1][j] == 1)
			count++;
		if (j - 1 >= 0 && hasCell[i][j - 1] == 1)
			count++;
		if (count == 0)
			return false;
		return true;
	}

	public void getBuildingDetails(Iterator<Cell> iter, Row row) {
		while (iter.hasNext()) {
			Cell temp = iter.next();
			rowMin = (temp.i < rowMin) ? temp.i : rowMin;
			rowMax = (temp.i > rowMax) ? temp.i : rowMax;
			colMin = (temp.j < colMin) ? temp.j : colMin;
			colMax = (temp.j > colMax) ? temp.j : colMax;
		}
		width = colMax - colMin + 1;
	}

	public void printCells(Set<Cell> cells) {
		Iterator<Cell> iter = cells.iterator();
		while (iter.hasNext()) {
			Cell temp = iter.next();
			System.out.println(temp.i + "," + temp.j);
		}
	}

	public void printCells(Iterator<Cell> iter) {
		while (iter.hasNext()) {
			Cell temp = iter.next();
			System.out.println(temp.i + "," + temp.j);
		}
	}
}
