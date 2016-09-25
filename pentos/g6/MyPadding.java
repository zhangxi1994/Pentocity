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
	public Move getPadding(Building request, int rotation, Land land, Row row, int location) {
		getBuildingDetails(request.rotations()[rotation].iterator(), row);
		Iterator<Cell> iter = request.rotations()[rotation].iterator();
		colLeft = row.getCurrentLocation() - width;
		colRight = row.getCurrentLocation();
		rowTop = row.getStart();
		rowBottom = row.getEnd(); // cannot equal to
		int waterCells = 0;
		Set<Cell> water = new HashSet<>();
		Set<Cell> park = new HashSet<>();
		Set<Cell> road = new HashSet<>();
		int previousCol = -1;
		int isStraight = 0;
		boolean extend = false;
		while (iter.hasNext()) {
			Cell temp = iter.next();
			hasCell[temp.i + row.getStart()][location + temp.j] = 2;
			if (previousCol == -1)
				previousCol = temp.j;
			else if (previousCol == temp.j)
				isStraight++;
			System.out.println(
					"Budling Cell---" + (temp.i + row.getStart()) + "," + (location+ temp.j));
		}
		for (int i = row.getEnd()-1, j = location; i >= rowTop && waterCells < 4 && isStraight < 4; i--) {
			// System.out.println(i+","+j+","+colLeft);
			if (hasCell[i][j] == 0 && land.unoccupied(i, j) && waterCells < 4) {
				water.add(new Cell(i, j));
				// System.out.println("Water Cell:" + i + "," + j);
				hasCell[i][j] = 1;
				waterCells++;
			}
			if (j + 1 < 50 && j + 1 <= colRight && hasCell[i][j + 1] == 0 && land.unoccupied(i, j + 1)
					&& waterCells < 4) {
				water.add(new Cell(i, j + 1));
				// System.out.println("Water Cell:" + i + "," + (j + 1));
				hasCell[i][j + 1] = 1;
				waterCells++;
			}
			if (i - 1 >= rowTop && hasCell[i - 1][j] == 0 && land.unoccupied(i - 1, j) && waterCells < 4) {
				water.add(new Cell(i - 1, j));
				// System.out.println("Water Cell:" + (i - 1) + "," + j);
				hasCell[i - 1][j] = 1;
				waterCells++;
			}

			if (i - 1 >= rowTop && j + 1 < 50 && j + 1 <= colRight && hasCell[i - 1][j + 1] == 0
					&& land.unoccupied(i - 1, j + 1) && waterCells < 4) {
				water.add(new Cell(i - 1, j + 1));
				// System.out.println("Water Cell:" + (i - 1) + "," + (j + 1));
				hasCell[i - 1][j + 1] = 1;
				waterCells++;
			}

			if (i + 1 < rowBottom && hasCell[i + 1][j] == 0 && land.unoccupied(i + 1, j) && waterCells < 4) {
				water.add(new Cell(i + 1, j));
				// System.out.println("Water Cell:" + (i + 1) + "," + j);
				hasCell[i + 1][j] = 1;
				waterCells++;
			}

			if (i + 1 < rowBottom && j + 1 < 50 && j + 1 <= colRight && hasCell[i + 1][j + 1] == 0
					&& land.unoccupied(i + 1, j + 1) && waterCells < 4) {
				water.add(new Cell(i + 1, j + 1));
				// System.out.println("Water Cell:" + (i + 1) + "," + (j + 1));
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
					System.out.println("water cell remove");
				}
			}
			/*
			 * for (Cell cell : water) { if (!checkValidWaterCell(cell)) {
			 * waterCells--; water.remove(cell); } }
			 */
		}

		if (waterCells < 4 && isStraight < 4) {
			if (checkValidWaterCell(new Cell(rowBottom - 1, colLeft))) {
				for (int i = rowBottom - 1, j = colLeft; i >= rowTop && waterCells < 4; i--) {
					water.add(new Cell(i, j));
					//System.out.println("Water Cell:" + i + "," + j);
					waterCells++;
				}
			} /*
				 * else if (checkValidWaterCell(new Cell(rowTop, colLeft))) {
				 * for (int i = rowTop, j = colLeft; i < rowBottom && waterCells
				 * < 4; i++) { water.add(new Cell(i, j));
				 * System.out.println("Water Cell:" + i + "," + j);
				 * waterCells++; } }
				 */else {
				for (int i = rowTop, j = colLeft; i < rowBottom && waterCells < 4; i++) {
					water.add(new Cell(i, j));
					//System.out.println("Water Cell:" + i + "," + j);
					waterCells++;
				}
			}
			extend = true;
		}
		/*
		 * for (int i = row.getStart(); i < row.getEnd(); i++) { for (int j =
		 * row.getCurrentLocation(); j > row.getCurrentLocation() - colMax +
		 * colMin -1; j--) { if (!hasBuildingCell[i][j]) { if (paddType == 1)
		 * water.add(new Cell(i, j)); else park.add(new Cell(i, j)); } } }
		 */

		// Add road when necessary
		if (row.getRoadLocation() > 0 && row.getRoadLocation() < 50) {
			for (int i = row.getCurrentLocation(); i > location; i--) {
				if (land.unoccupied(row.getRoadLocation(), i)) {
					road.add(new Cell(row.getRoadLocation(), i));
				}
			}
			/*
			 * if (extend == true) { if (land.unoccupied(row.getRoadLocation(),
			 * colLeft)) { road.add(new Cell(row.getRoadLocation(), colLeft)); }
			 * }
			 */
		}
		int parksize = 0;
		if (row.getParkLocation() > 0 && row.getParkLocation() < 50 && isStraight < 4) {
			for (int i = row.getCurrentLocation(); i > row.getCurrentLocation() - width; i--) {
				if (land.unoccupied(row.getParkLocation(), i)) {
					park.add(new Cell(row.getParkLocation(), i));
				}
				if (land.unoccupied(row.getRoadLocation(), i)) {
					road.add(new Cell(row.getRoadLocation(), i));
				}
				parksize++;
			}
			int col = colLeft;
			while (parksize < 4) {
				if (land.unoccupied(row.getParkLocation(), col)) {
					park.add(new Cell(row.getParkLocation(), col));
				}

				if (land.unoccupied(row.getRoadLocation(), col)) {
					road.add(new Cell(row.getRoadLocation(), col));
				}

				col--;
				parksize++;
			}

		}


		System.out.println("Building location:"+row.getStart() + "," + location);getClass();
		System.out.println("Building:");
		printCells(request.rotations()[rotation].iterator());
		System.out.println("water:");
		printCells(water);
		
		if (isStraight < 4) {
			row.setCurrentLocation(row.getCurrentLocation() - width);
			return new Move(true, request, new Cell(row.getStart(), location), rotation, road, water, park);
		} else
			return new Move(true, request, new Cell(row.getStart(), location), rotation, road, water, park);
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
