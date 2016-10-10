package pentos.g2;

import pentos.sim.Cell;
import pentos.sim.Building;
import pentos.sim.Land;
import pentos.sim.Move;

import java.util.*;

public class Player implements pentos.sim.Player {

    private Random gen = new Random();
    private Set<Cell> road_cells = new HashSet<Cell>();

    // per-turn set of cells to build
    private HashSet<Cell> parks_to_build = new HashSet<Cell>();
    private HashSet<Cell> water_to_build = new HashSet<Cell>();

    //Data structures for blob detection:
    private boolean[][] occupied_cells;
    private boolean[][] road_map;

    private int global_num_bad_blobs = 0;

    private int max_i = 0;
    private int max_j = 0;

    private int staging_max_i = 0;
    private int staging_max_j = 0;

    public void init() { // function is called once at the beginning before play is called
        // For blob detection:
        occupied_cells = new boolean[50][50];
        road_map = new boolean[50][50];
    }
    
    public Move play(Building request, Land land) {
        // find all valid building locations and orientations
        ArrayList <Move> moves = new ArrayList <Move> ();
        for (int i = 0 ; i < land.side ; i++)
            for (int j = 0 ; j < land.side ; j++) {
            Cell p = new Cell(i, j);
            Building[] rotations = request.rotations();
            for (int ri = 0 ; ri < rotations.length ; ri++) {
                Building b = rotations[ri];
                if (land.buildable(b, p)) 
                    moves.add(new Move(true, request, p, ri, new HashSet<Cell>(), new HashSet<Cell>(), new HashSet<Cell>()));
            }
        }
        // choose a building placement at random
        if (moves.isEmpty()) // reject if no valid placements
            return new Move(false);
        else {
            int inc;
            int placement_idx;
            if(request.type == Building.Type.FACTORY) {
                placement_idx = moves.size() - 1;
                inc = -1;
            } else {
                placement_idx = 0;
                inc = 1;
            }

            // Keep track of the move with the least amount of created blobs
            Move bestMove = null;
            int min_blobs = Integer.MAX_VALUE;
            int moveCount = 0;

            while (true) {
                // Look at the next possible place to look for 
                Move chosen = moves.get(placement_idx); 

                // Get coordinates of building placement (position plus local building cell coordinates).
                Set<Cell> shiftedCells = new HashSet<Cell>();
                staging_max_i = max_i;
                staging_max_j = max_j;
                for (Cell x : chosen.request.rotations()[chosen.rotation]) {

                    // Update data structures for blob detection
                    if(request.type == Building.Type.RESIDENCE) {
                        if(x.i+chosen.location.i > staging_max_i) {
                            staging_max_i = x.i+chosen.location.i;
                        }
                        if(x.j+chosen.location.j > staging_max_j) {
                            staging_max_j = x.j+chosen.location.j;
                        }
                    }
        
                    shiftedCells.add(new Cell(x.i+chosen.location.i, x.j+chosen.location.j));
                }

                Set<Cell> roadCells;

                roadCells = findShortestRoad(shiftedCells, land);
                if (roadCells != null) {
                    for (Cell x : roadCells) {
                        if(x.i > staging_max_i) {
                            staging_max_i = x.i;
                        }
                        if(x.j > staging_max_j) {
                            staging_max_j = x.j;
                        }
                    }

                    // For the existing road algorithm
                    chosen.road = roadCells;

                    if(request.type == Building.Type.RESIDENCE) {

                        // for blob detection
                        System.out.println("Checking for blobs with i:" + staging_max_i + " j:" + staging_max_j);
                        int num_bad_blobs = num_unusable_blobs(roadCells, shiftedCells, land);
                        //System.out.println("Detected " + num_bad_blobs + " blobs within i: 0-" + staging_max_i + " j: 0-" + staging_max_j); 

                        chosen.water = (HashSet) water_to_build.clone();
                        chosen.park = (HashSet) parks_to_build.clone();

                        if (num_bad_blobs <= global_num_bad_blobs/* || moveCount > 24*/) {
                            // Update data structures for blob detection
                            for(Cell x : roadCells) {
                                road_map[x.i][x.j] = true;
                            }

                            max_i = staging_max_i;
                            max_j = staging_max_j;
                            
                            //System.out.format("max_i: %d\nmax_j: %d\n\n", max_i, max_j);
                            System.out.println("Placed with " + num_bad_blobs + " blobs.");

                            road_cells.addAll(roadCells);
                            return chosen;
                        } else if (moveCount > 250) {
                            if(bestMove != null) {

                                for(Cell x : bestMove.road) {
                                    road_map[x.i][x.j] = true;
                                }

                                road_cells.addAll(bestMove.road);

                                global_num_bad_blobs = min_blobs;

                                max_i = staging_max_i;
                                max_j = staging_max_j;

                                System.out.println("Placed with " + min_blobs + " blobs.");

                                return bestMove;
                            } else {
                                return new Move(false);    
                            }
                        } else {
                            System.out.println("Next move #" + (++moveCount) + "/" + moves.size());

                            if(num_bad_blobs < min_blobs) {
                                bestMove = chosen;
                                min_blobs = num_bad_blobs;

                                //System.out.println("Best new blob count: " + min_blobs);
                            }

                            placement_idx += inc;
                            if(placement_idx < 0 || placement_idx >= moves.size()) {
                                if(bestMove != null) {

                                    for(Cell x : bestMove.road) {
                                        occupied_cells[x.i][x.j] = true;
                                        road_map[x.i][x.j] = true;
                                    }

                                    road_cells.addAll(bestMove.road);

                                    global_num_bad_blobs = min_blobs;

                                    max_i = staging_max_i;
                                    max_j = staging_max_j;

                                    System.out.println("Placed with " + min_blobs + " blobs.");

                                    return bestMove;
                                } else {
                                    return new Move(false);    
                                }
                            }
                        }

                    } else { // Building.Type.FACTORY
                        // Update data structures for blob detection:
                        for(Cell x : roadCells) {
                            occupied_cells[x.i][x.j] = true;
                            road_map[x.i][x.j] = true;
                        }
                        for(Cell x : shiftedCells) {
                            occupied_cells[x.i][x.j] = true;
                        }

                        road_cells.addAll(roadCells);
                        return chosen;
                    } 
                }
                else { // Reject placement if building cannot be connected by road
                    System.out.println("Trying new roads.");
                    placement_idx += inc;
                    if(placement_idx < 0 || placement_idx >= moves.size()) {
                        if(bestMove != null) {

                            // Update data structures for blob detection.
                            road_cells.addAll(bestMove.road);

                            if (roadCells != null) {
                                for(Cell x : roadCells) {
                                    occupied_cells[x.i][x.j] = true;
                                    road_map[x.i][x.j] = true;
                                }
                            }

                            if (shiftedCells != null) {
                                for(Cell x : shiftedCells) {
                                    occupied_cells[x.i][x.j] = true;
                                }
                            }

                            road_cells.addAll(bestMove.road);

                            max_i = staging_max_i;
                            max_j = staging_max_j;

                            global_num_bad_blobs = min_blobs;

                            return bestMove;

                        } else {
                            return new Move(false);    
                        }
                    }
                }
            }
        }
    }


    private boolean build_parks_next = true;
    // Blob detection algorithm
    // A blob is unusable if any of the following is true:
    //      - is less than 5 blocks big
    //      - there is no road access
    //
    // Connected component labeling
    // 
    //     followed the guide on this page:
    //          http://aishack.in/tutorials/connected-component-labelling/
    //
    //  The algorithm also finds blobs of size 4 and builds a park or pond in them.
    private int num_unusable_blobs(Set<Cell> roads, Set<Cell> buildings, Land land) {
        // Create buffer to store land with the buildings of this new move.
        boolean[][] new_occupied_cells = new boolean[50][50];
        boolean[][] new_road_map = new boolean[50][50];

        // Clone the existing occupied cells.
        for (int i = 0; i < 50; ++i) {
            new_road_map[i] = road_map[i].clone();
        }

        for (int i = 0; i < 50; ++i) {
            for (int j = 0; j < 50; ++j) {
                new_occupied_cells[i][j] = !land.unoccupied(i, j);
            }
        }

        // Add in the cells created by this new move.
        for(Cell x : roads) {
            new_occupied_cells[x.i][x.j] = true;
            new_road_map[x.i][x.j] = true;
        }
        for(Cell x : buildings) {
            new_occupied_cells[x.i][x.j] = true;
        }

        // Create a buffer of blob labels.
        int[][] blob_labels = new int[50][50];
        int next_label = 1;

        // Create a tree of the labels to use in the blob detection algorithm.
        ArrayList<UF> label_tree = new ArrayList<UF>();

        // First pass (mark blobs and acknolwedge connections in the tree of labels)
        for(int i = 0; i <= staging_max_i; ++i) {
            for(int j = 0; j <= staging_max_j; ++j) {

                if (new_occupied_cells[i][j] == false) {

                    // Label a is the label above the current cell.
                    int label_a = 0;

                    // Label b is the label to the left of the current cell.
                    int label_b = 0;

                    if(i > 0) {
                        label_a = blob_labels[i-1][j];
                    }
                    if(j > 0) {
                        label_b = blob_labels[i][j-1];
                    }

                    // If both neighbors are occupied, create a new blob label
                    if(label_a == 0 && label_b == 0) {
                        blob_labels[i][j] = next_label;
                        label_tree.add(new UF(next_label));
                        ++next_label;
                    } else if (label_b == 0) {
                        blob_labels[i][j] = label_a;
                    } else if (label_a == 0) {
                        blob_labels[i][j] = label_b;
                    } else {
                        blob_labels[i][j] = Math.min(label_a, label_b);

                        if(label_a == label_b) {
                            // Combine the labels.
                            UF.union(label_tree.get(label_a - 1), label_tree.get(label_b - 1));
                        }
                    }
                }

            }
        }

        // Second pass (combine blobs using the tree of connections)
        for(int i = 0; i <= staging_max_i; ++i) {
            for(int j = 0; j <= staging_max_j; ++j) {
                if(blob_labels[i][j] == 0 || label_tree.get(blob_labels[i][j] - 1).isRoot()) {
                    // go to next pixel
                } else {
                    blob_labels[i][j] = label_tree.get(blob_labels[i][j] - 1).findRoot().getLabel();
                }
            }
        }

        // Make an array of blobsizes, and check whether the blob is accessible by road.
        int potential_blob_count = next_label - 1;
        int[] blobsizes = new int[potential_blob_count + 1]; // the size is the potential number of blobs
        boolean[] road_accessible = new boolean[potential_blob_count + 1];

        // this refers to the border of the staging area, not the map border.
        boolean[] touches_border = new boolean[potential_blob_count + 1];

        // Third pass (calculate blob size and blob accessibility, 
        //             TODO: don't count blobs outside of the staging range)
        for(int i = 0; i <= staging_max_i; ++i) {
            for(int j = 0; j <= staging_max_j; ++j) {
                int cur_blob = blob_labels[i][j];

                if(cur_blob != 0) {
                    // Increment size of this blob
                    ++blobsizes[blob_labels[i][j]];

                    if (!road_accessible[cur_blob]) {
                        // Detect is this blob has road access.
                        if(i == 0 || i == 49 || j == 0 || j == 49) {
                            road_accessible[cur_blob] = true;
                        } else {
                            if(new_road_map[i-1][j]) {
                                road_accessible[cur_blob] = true;
                            } else if (new_road_map[i][j-1]) {
                                road_accessible[cur_blob] = true;
                            } else if (new_road_map[i+1][j]) {
                                road_accessible[cur_blob] = true;
                            } else if (new_road_map[i][j+1]) {
                                road_accessible[cur_blob] = true;
                            }
                        }
                    }

                    if (staging_max_j != 49 || staging_max_i != 49) {
                        if(i == staging_max_i || j == staging_max_j) {
                            touches_border[cur_blob] = true;
                        }
                    }
                    
                }
            }
        }

        parks_to_build.clear();
        water_to_build.clear();

        // Count the number of unusable blobs
        int num_bad_blobs = 0;
        for (int cur_blob = 1; cur_blob <= potential_blob_count; ++cur_blob) {
            if (blobsizes[cur_blob] == 0) {
                continue;
            } if ((blobsizes[cur_blob] == 4) && !touches_border[cur_blob]) { // if the blobs created are the size of a field or pond, add it
                for(int i = 0; i <= staging_max_i; ++i) {
                    for(int j = 0; j <= staging_max_j; ++j) {
                        if (blob_labels[i][j] == cur_blob) {
                            if(build_parks_next) {
                                parks_to_build.add(new Cell(i, j));
                            } else {
                                water_to_build.add(new Cell(i, j));
                            }

                        }
                    }
                }
                build_parks_next = !build_parks_next;
            }
            else if((blobsizes[cur_blob] < 5 && !touches_border[cur_blob]) || (!road_accessible[cur_blob] && !touches_border[cur_blob])) {
                ++num_bad_blobs;
            }
        }

        return num_bad_blobs;
    }
    
    // build shortest sequence of road cells to connect to a set of cells b
    // build shortest sequence of road cells to connect to a set of cells b
    private Set<Cell> findShortestRoad(Set<Cell> b, Land land) {
        Set<Cell> output = new HashSet<Cell>();
        boolean[][] checked = new boolean[land.side][land.side];
        Queue<Cell> queue = new LinkedList<Cell>();
        // add border cells that don't have a road currently
        Cell source = new Cell(Integer.MAX_VALUE,Integer.MAX_VALUE); // dummy cell to serve as road connector to perimeter cells
        for(Cell x : b) {
            if(x.i==0 || x.i==land.side-1 || x.j==0 || x.j==land.side-1) return new HashSet<Cell>();
            for(Cell y : x.neighbors()) {
                if(road_cells.contains(y)) {
                    return new HashSet<Cell>();
                }
            }
        }
        for (int z=0; z<land.side; z++) {
            if (b.contains(new Cell(0,z)) || b.contains(new Cell(z,0)) || b.contains(new Cell(land.side-1,z)) || b.contains(new Cell(z,land.side-1))) //if already on border don't build any roads
                return output;
            if (land.unoccupied(0,z))
                queue.add(new Cell(0,z,source));
            if (land.unoccupied(z,0))
                queue.add(new Cell(z,0,source));
            if (land.unoccupied(z,land.side-1))
                queue.add(new Cell(z,land.side-1,source));
            if (land.unoccupied(land.side-1,z))
                queue.add(new Cell(land.side-1,z,source));
        }
        // add cells adjacent to current road cells

        for (Cell p : road_cells) {
            for (Cell q : p.neighbors()) {
                if (!road_cells.contains(q) && land.unoccupied(q) && !b.contains(q))
                    queue.add(new Cell(q.i,q.j,p)); // use tail field of cell to keep track of previous road cell during the search
            }
        }
        while (!queue.isEmpty()) {
            Cell p = queue.remove();
            checked[p.i][p.j] = true;
            for (Cell x : p.neighbors()) {
                if (b.contains(x)) { // trace back through search tree to find path
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
                    checked[x.i][x.j] = true;
                    queue.add(x);
                }

            }
        }
        if (output.isEmpty() && queue.isEmpty())
            return null;
        else
            return output;
    }

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
    if (adjCells.isEmpty())
        return new HashSet<Cell>();
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

}
