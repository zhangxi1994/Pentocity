package pentos.g10;

import java.util.HashSet;
import java.util.Set;

import pentos.sim.Building;
import pentos.sim.Cell;
import pentos.sim.Land;

public class PlanEvaluator {
	public static double evaluatePlan(Player player, Action action, Land land) {
		double score = 0;
		if (action == null)
			return 0;
		if (action.getBuilding() == null)
			return 0;

		/* Find all the cells this building plan will occupy */
		Set<Cell> toOccupy = ToolBox.combineSets(action.getAbsoluteBuildingCells(), action.getRoadCells(),
				action.getParkCells(), action.getWaterCells());

		/* Road related score calculation */
		if (action.getRoadCells() != null) {
			if (action.getRoadCells().size() > 0) {
				score -= 3.0;
				int siz=action.getRoadCells().size();
				if(siz<5)
					score -= 0.5 * action.getRoadCells().size();
				else
					score-=3;
			}
		}
		int occupiedRoadNeighbors = ToolBox.setInterception(player.roadNeighbors, toOccupy).size();

		/*
		 * The fewer the vacant road neighbor cells are, the more expensive to
		 * occupy each.
		 */
		int potentialRoad = player.roadNeighbors.size();
		System.out.println(potentialRoad + " road neighbors now");
		if (potentialRoad <= 10)
			score -= 2.0 * occupiedRoadNeighbors;
		else
			score -= 0.5 * occupiedRoadNeighbors;
		int vacantBorderSize = player.vacantBorders.size();
		int coverBorder = ToolBox.setInterception(toOccupy, player.vacantBorders).size();
		if (vacantBorderSize <= 10) {

			score -= 5.0 * coverBorder;
		} else {
			score -= 1 * coverBorder;
		}
		/* Field and park related score calculation */
		double parkScore = parkScore(toOccupy, player, action, land);
		double waterScore = waterScore(toOccupy, player, action, land);
		score += parkScore;
		score += waterScore;

		/* How packed is the building to the existing cluster */
		double packedToCluster = compactnessScore(toOccupy, player, action, land);
		score += packedToCluster;

		/* The tidiness of the land map */
		double tidiness = tidinessScore(toOccupy, land);
		score += tidiness;

		/* Check if the building will block a road */
		boolean blockRoad = checkBlockRoads(action, player, land);
		if (blockRoad) {
			System.out.println("==================");
			System.out.println("Blocking the road!");
			score -= 100;
			System.out.println("==================");
		}

		return score;
	}

	public static double evaluateLastMinutePlan(Player player, Action action, Land land) {
		double score = 0.0;
		/* Find all the cells this building plan will occupy */
		Set<Cell> toOccupy = ToolBox.combineSets(action.getAbsoluteBuildingCells(), action.getRoadCells(),
				action.getParkCells(), action.getWaterCells());
		double packedToCluster = compactnessScore(toOccupy, player, action, land);
		score += packedToCluster;
		/* Check if the building will block a road */
		boolean blockRoad = checkBlockRoads(action, player, land);
		if (blockRoad) {
			System.out.println("==================");
			System.out.println("Blocking the road!");
			score -= 50;
			System.out.println("==================");
		}
		return score;
	}

	public static double tidinessScore(Set<Cell> toOccupy, Land land) {
		int count = 0;
		for (int i = 0; i < 50; i++) {
			for (int j = 0; j < 50; j++) {
				Cell c = new Cell(i, j);
				if (vacantSquare(c, toOccupy, land, 3))
					count++;
			}
		}
		return 0.2 * count;
	}

	public static boolean vacantSquare(Cell c, Set<Cell> toOccupy, Land land, int side) {
		for (int i = 0; i < side; i++) {
			for (int j = 0; j < side; j++) {
				Cell toCheck = new Cell(c.i + i, c.j + j);
				if (!ToolBox.isValid(toCheck))// cell itself has invalid
												// coordinates
					return false;
				if (toOccupy.contains(toCheck))// cell will be occupied
					return false;
				if (!land.unoccupied(toCheck))// cell is already occupied
					return false;
			}
		}
		return true;
	}

	public static double parkScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;
		if (action.getBuilding().type == Building.Type.FACTORY)
			return score;

		int adjacentToParks = ToolBox.setInterception(player.parkNeighbors, toOccupy).size();
		if (adjacentToParks > 0)
			score += 2;

		// int occupiedParkNeighbors =
		// ToolBox.setInterception(player.parkNeighbors, toOccupy).size();
		score -= 0.5 * adjacentToParks;

		return score;
	}

	public static double waterScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		double score = 0.0;
		if (action.getBuilding().type == Building.Type.FACTORY)
			return score;

		int adjacentToWater = ToolBox.setInterception(player.waterNeighbors, toOccupy).size();
		if (adjacentToWater > 0)
			score += 2;

		// int occupiedWaterNeighbors =
		// ToolBox.setInterception(player.waterNeighbors, toOccupy).size();
		score -= 0.5 * adjacentToWater;

		return score;
	}

	public static double compactnessScore(Set<Cell> toOccupy, Player player, Action action, Land land) {
		Set<Cell> itself = action.getAbsoluteBuildingCells();

		double score = 0.0;
		int packedToCluster = 0;
		// Calculate how much it's packed to its cluster
		if (action.getBuilding().type == Building.Type.RESIDENCE)
			packedToCluster = ToolBox.setInterception(player.residenceStart, itself).size();
		else if (action.getBuilding().type == Building.Type.FACTORY)
			packedToCluster = ToolBox.setInterception(player.factoryStart, itself).size();
		else {
			System.out.println("The type of building is not recognized!");
			return -100.0;
		}
		score = packedToCluster;

		// Packed to border
		// int packedToBorder = ToolBox.setInterception(player.borders,
		// itself).size();
		// score += 3 * packedToBorder;

		// calculate how far it's from the corner
		Cell start = action.getStartPoint();
		double distance = 0.0;
		if (action.getBuilding().type == Building.Type.RESIDENCE) {
			distance = ToolBox.calculateDistance(new Cell(0, 0), start);
			if (start.i > land.side / 2)
				distance += 10;
			if (start.j > land.side / 2)
				distance += 10;
		} else if (action.getBuilding().type == Building.Type.FACTORY) {
			distance = ToolBox.calculateDistance(new Cell(land.side - 1, land.side - 1), start);
			if (start.i < land.side / 2)
				distance += 10;
			if (start.j < land.side / 2)
				distance += 10;
		}
		score -= 0.5 * distance;

		return score;
	}

	public static boolean validateMove(Action action, Player player, Land land) {
		if (action == null || action.getBuilding() == null)
			return false;
		/* Validate move is buildable */
		Building[] rotations = action.getBuilding().rotations();
		Building building = rotations[action.getRotation()];
		Cell start = action.getStartPoint();
		boolean canDo = land.buildable(building, start);
		if (!canDo) {
			System.out.println("The action cannot be performed?!");
			return false;
		}

		/* Validate if there is any overlap of cells */
		boolean noOverlap = validateActionOverlap(action, land);
		if (!noOverlap)
			return false;

		/* Validate roads */
		boolean roadValid = validateRoads(action, land);
		if (!roadValid)
			return false;

		/* Validate the building is adjacent to roads */
		boolean nextToRoads = validateNextToRoads(action, player);
		if (!nextToRoads)
			return false;

		/* The action is valid if all validations have passed */
		return true;
	}

	public static boolean validateNextToRoads(Action action, Player player) {
		Set<Cell> roadsToBuild = action.getRoadCells();
		Set<Cell> roadsBuiltNeighbors = player.roadNeighbors;
		// System.out.println(roadsBuiltNeighbors.size()+" existing road
		// neighbors.");

		Set<Cell> buildingCells = action.getAbsoluteBuildingCells();
		Set<Cell> buildingNeighbors = ToolBox.allNeighbors(buildingCells);

		// The building has cells that are neighbor of existing roads
		Set<Cell> adjacentToRoad = ToolBox.setInterception(action.getAbsoluteBuildingCells(), roadsBuiltNeighbors);
		// The building has neighbors that are in the road plan
		Set<Cell> adjacentToRoadPlan = ToolBox.setInterception(buildingNeighbors, roadsToBuild);
		if (adjacentToRoad.size() == 0 && adjacentToRoadPlan.size() == 0) {
			System.out.println("Error: The action is not adjacent to road!");
			return false;
		}
		return true;
	}

	/*
	 * Validate the building, roads, parks and water do not interfere with each
	 * other
	 */
	public static boolean validateActionOverlap(Action action, Land land) {
		Set<Cell> shifted = action.getAbsoluteBuildingCells();
		Set<Cell> total = ToolBox.combineSets(shifted, action.getRoadCells(), action.getWaterCells(),
				action.getParkCells());
		int totalSize = shifted.size() + action.getRoadCells().size() + action.getWaterCells().size()
				+ action.getParkCells().size();
		if (totalSize != total.size()) {
			System.out.println("There are overlap in the building, roads, water and parks! They in total should occupy "
					+ totalSize + " but now only occupying " + total.size());
			return false;
		}
		return true;
	}

	/* Validate that roads are connected and connected with borders */
	public static boolean validateRoads(Action action, Land land) {
		Set<Cell> existingRoads = ToolBox.copyLandRoads(land);
		if (action.getRoadCells() != null) {
			for (Cell r : action.getRoadCells()) {
				existingRoads.add(new Cell(r.i + 1, r.j + 1));
			}
		}
		Cell[] erc = existingRoads.toArray(new Cell[existingRoads.size()]);
		boolean roadValid = Cell.isConnected(erc, 50 + 2);
		if (!roadValid) {
			System.out.println("The road plan is rejected!");
			return false;
		}
		return true;
	}

	public static boolean checkBlockRoads(Action action, Player player, Land land) {
		Set<Cell> centerCells = new HashSet<>();
		// check if the center is occupied
		Cell center = new Cell(land.side / 2 - 1, land.side / 2 - 1);
		if (land.unoccupied(center))
			centerCells.add(center);
		else {
			boolean found = false;
			Cell[] n = center.neighbors();
			for (int j = 0; j < n.length; j++) {
				Cell toCheck = n[j];
				if (land.unoccupied(toCheck)) {
					found = true;
					centerCells.add(toCheck);
				}
			}
			// 4 other cells
			Cell[] localCenter=new Cell[]{
					new Cell(land.side/4,land.side/4),
					new Cell(3*land.side/4,land.side/4),
					new Cell(land.side/4,3*land.side/4),
					new Cell(3*land.side/4,3*land.side/4),
			};
			for(int j=0;j<localCenter.length;j++){
				Cell toCheck=localCenter[j];
				if(land.unoccupied(toCheck)){
					found = true;
					centerCells.add(toCheck);
				}
			}
			if (found == false) {
				System.out.println("No vacant cells in the center. Forget it.");
				return false;
			}
		}

		// Find the closest road to the building
		Set<Cell> cells = action.getAbsoluteBuildingCells();
		Set<Cell> roadPlan = action.getRoadCells();
		Set<Cell> roadClues = new HashSet<>();
		if (roadPlan.size() == 0) {
			System.out.println("No roads in plan. Search the neighbors of this building.");
			Set<Cell> buildingNeighbors = ToolBox.allNeighbors(cells);
			for (Cell c : buildingNeighbors) {
				if (player.roadcells.contains(c)) {
					roadClues.add(c);
				}
			}
			System.out.println("Found " + roadClues.size() + " road cells around the building.");
		} else {
			Set<Cell> roadPlanNeighbors = ToolBox.allNeighbors(roadPlan);
			for (Cell c : roadPlanNeighbors) {
				if (player.roadcells.contains(c)) {
					roadClues.add(c);
				}
			}
			System.out.println("Found " + roadClues.size() + " road cells around the road plan.");
		}
		if (roadClues.size() == 0) {
			System.out.println("If the action is connected to the border it's fine.");
			return false;
		}

		// //Find the road based on the road cells that we have found
		Set<Cell> roadCells = new HashSet<>();
		for (Cell c : roadClues) {
			Set<Cell> connection = findConnectionToBorder(c, player, land, new HashSet<Cell>());
			if (connection != null)
				roadCells.addAll(connection);
		}
		System.out.println("The road found has " + roadCells.size() + " cells");

		// //Occupy the cells according to the action
		// Set<Cell>
		// willBeOccupied=ToolBox.combineSets(action.getAbsoluteBuildingCells(),action.getRoadCells(),action.getParkCells(),action.getWaterCells());
		//

		// Check connection to the center cell
		roadClues.addAll(action.getRoadCells());
		roadClues.addAll(roadCells);

		boolean allFailed = true;
		for (Cell c : centerCells) {
			boolean connectToCenter = RoadFinder.findClearPath(c, roadClues, player, action, land);
			if (connectToCenter) {
				System.out.println("Center cell i:" + c.i + " j:" + c.j + " can connect to road.");
				allFailed = false;
			}
		}
		if (allFailed) {
			System.out.println("All center cells cannot connect to the road! We cannot let the action happen! ");
			return true;
		} else {
			System.out.println("Still hope for vacant roads. ");
			return false;
		}

	}

	public static Set<Cell> findConnectionToBorder(Cell c, Player player, Land land, Set<Cell> checked) {
		Set<Cell> path = new HashSet<>();
		if (c.i == 0 || c.i == land.side - 1) {
			// System.out.println("Found the horizontal border at " + c);
			path.add(c);
			// System.out.println("Road found so far " + path);
			return path;
		} else if (c.j == 0 || c.j == land.side - 1) {
			// System.out.println("Found the vertical border at " + c);
			path.add(c);
			// System.out.println("Road found so far " + path);
			return path;
		} else {
			// System.out.println(c + " is not a border cell.");
			Cell[] cells = c.neighbors();
			boolean roadAround = false;
			for (int i = 0; i < cells.length; i++) {
				Cell n = cells[i];
				if (checked.contains(n)) {
					// System.out.println("Cell " + n + " has been checked.");
					// there is another possibility that n is checked and n is
					// road
					if (player.roadcells.contains(n)) {
						// System.out.println(n + " is a checked road cell.
						// Adding it whatsoever.");
						path.add(n);
						roadAround = true;
					}
					continue;
				}
				// System.out.println("Cell " + n + " is not checked. Checking
				// now.");
				checked.add(n);
				if (player.roadcells.contains(n)) {
					// System.out.println(n + " is a road cell");
					roadAround = true;
					path = findConnectionToBorder(n, player, land, checked);
					if (path == null) {
						System.out.println("No path found!");
					}
					path.add(n);
					path.add(c);
				}
			}
			if (roadAround == false) {
				System.out.println("Broken road here i:" + c.i + " j:" + c.j);
				return null;
			} else {
				// System.out.println("Road found so far " + path);
				return path;
			}
		}
	}

}
