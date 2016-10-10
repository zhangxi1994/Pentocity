package pentos.g10;

import java.util.Set;
import java.util.*;

import pentos.sim.Cell;
import pentos.sim.Land;

public class ParkAndWaterFinder {
	public static Set<Cell> findPark(Player player, Action action, Land land) {
		Set<Cell> cells = action.getAbsoluteBuildingCells();
		return new HashSet<Cell>();
	}

	public static Set<Cell> findWater(Player player, Action action, Land land) {
		Set<Cell> cells = action.getAbsoluteBuildingCells();
		return new HashSet<Cell>();
	}
}
