package edu.brown.cs.board;

import static edu.brown.cs.board.TileType.BRICK;
import static edu.brown.cs.board.TileType.COAL;
import static edu.brown.cs.board.TileType.CONCRETE;
import static edu.brown.cs.board.TileType.DESERT;
import static edu.brown.cs.board.TileType.GOLD;
import static edu.brown.cs.board.TileType.ORE;
import static edu.brown.cs.board.TileType.SEA;
import static edu.brown.cs.board.TileType.SHEEP;
import static edu.brown.cs.board.TileType.WHEAT;
import static edu.brown.cs.board.TileType.WOOD;
import static edu.brown.cs.catan.Settings.NUM_BRICK_TILE;
import static edu.brown.cs.catan.Settings.NUM_DESERT_TILE;
import static edu.brown.cs.catan.Settings.NUM_ORE_TILE;
import static edu.brown.cs.catan.Settings.NUM_SHEEP_TILE;
import static edu.brown.cs.catan.Settings.NUM_WHEAT_TILE;
import static edu.brown.cs.catan.Settings.NUM_WOOD_TILE;
import static edu.brown.cs.catan.Settings.ROLL_NUMS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Settings;

/**
 * Board Class. Functions as a container class for all of the board data.
 *
 * @author anselvahle
 *
 */
public class Board {
  private Collection<Tile> _tiles;
  private Map<IntersectionCoordinate, Intersection> _intersections;
  private Map<PathCoordinate, Path> _paths;
  private final List<HexCoordinate> PORT_LOCATION;
  private final static int DEPTH = 2;

  /**
   * Adds a certain number of tiles of a specific type to a list to shuffle when
   * constructing the board.
   *
   * @param availTiles
   *                   List of tiles.
   * @param type
   *                   Type of tiles to add.
   * @param numTiles
   *                   How many tiles to add.
   */
  private void addTiles(List<TileType> availTiles, TileType type, int numTiles) {
    for (int i = 0; i < numTiles; i++) {
      availTiles.add(type);
    }
  }

  /**
   * Gets permutations of sea coordinates and puts them into a list.
   *
   * @return List of HexCoordinates of the location of Sea Tiles.
   */
  private List<HexCoordinate> getSeaPermutations() {
    List<HexCoordinate> coords = new ArrayList<HexCoordinate>();
    int[] oneThree = { 0, 0, 3 };
    int[] oneThreeOneTwo = { 0, 2, 3 };
    int[] oneThreeOneOne = { 0, 1, 3 };
    int[] twoThree = { 0, 3, 3 };

    coords.add(new HexCoordinate(oneThree[0], oneThree[1], oneThree[2]));
    while (permute(oneThree)) {
      coords.add(new HexCoordinate(oneThree[0], oneThree[1], oneThree[2]));
    }

    coords.add(new HexCoordinate(oneThreeOneTwo[0], oneThreeOneTwo[1],
        oneThreeOneTwo[2]));
    while (permute(oneThreeOneTwo)) {
      coords.add(new HexCoordinate(oneThreeOneTwo[0], oneThreeOneTwo[1],
          oneThreeOneTwo[2]));
    }

    coords.add(new HexCoordinate(oneThreeOneOne[0], oneThreeOneOne[1],
        oneThreeOneOne[2]));
    while (permute(oneThreeOneOne)) {
      coords.add(new HexCoordinate(oneThreeOneOne[0], oneThreeOneOne[1],
          oneThreeOneOne[2]));
    }

    coords.add(new HexCoordinate(twoThree[0], twoThree[1], twoThree[2]));
    while (permute(twoThree)) {
      coords.add(new HexCoordinate(twoThree[0], twoThree[1], twoThree[2]));
    }
    return coords;
  }

  /**
   * Returns true if there are possible permutations of the array.
   *
   * @param data
   *             Array to permute
   * @return Boolean stating if there are more permutations
   */
  private boolean permute(int[] data) {
    int k = data.length - 2;
    while (data[k] >= data[k + 1]) {
      k--;
      if (k < 0) {
        return false;
      }
    }
    int l = data.length - 1;
    while (data[k] >= data[l]) {
      l--;
    }
    swap(data, k, l);
    int length = data.length - (k + 1);
    for (int i = 0; i < length / 2; i++) {
      swap(data, k + 1 + i, data.length - i - 1);
    }
    return true;
  }

  private void swap(int[] data, int idx1, int idx2) {
    int tmp = data[idx1];
    data[idx1] = data[idx2];
    data[idx2] = tmp;
  }

  /**
   * Tells the tiles what was rolled.
   *
   * @param roll
   *             Num that was rolled.
   */
  public void notifyTiles(int roll) {
    for (Tile t : _tiles) {
      if (t.getRollNumber() == roll) {
        t.notifyIntersections();
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder toRet = new StringBuilder();
    toRet.append(_tiles.toString());
    toRet.append("\n");
    toRet.append(_intersections.keySet().size());
    toRet.append("\n");
    toRet.append(_paths.keySet().size());
    return toRet.toString();
  }

  /**
   * Getter for the tiles.
   * 
   * @return Collection of the tiles in the board.
   */
  public Collection<Tile> getTiles() {
    return Collections.unmodifiableCollection(_tiles);
  }

  /**
   * Getter for the intersections.
   *
   * @return Map of the intersections on the board.
   */
  public Map<IntersectionCoordinate, Intersection> getIntersections() {
    return Collections.unmodifiableMap(_intersections);
  }

  /**
   * Getter for the paths.
   *
   * @return Map of the paths in the board.
   */
  public Map<PathCoordinate, Path> getPaths() {
    return Collections.unmodifiableMap(_paths);
  }

  /**
   * Sets the port locations for a standard board size;
   * 
   * @return List of the coordinates of the port locations for the standard
   *         board.
   */
  private List<HexCoordinate> setPortLocations() {
    List<HexCoordinate> toRet = new ArrayList<HexCoordinate>();
    toRet.add(new HexCoordinate(0, 0, 3));
    toRet.add(new HexCoordinate(0, 2, 3));
    toRet.add(new HexCoordinate(0, 3, 2));
    toRet.add(new HexCoordinate(0, 3, 0));
    toRet.add(new HexCoordinate(2, 3, 0));
    toRet.add(new HexCoordinate(3, 2, 0));
    toRet.add(new HexCoordinate(3, 0, 0));
    toRet.add(new HexCoordinate(3, 0, 2));
    toRet.add(new HexCoordinate(2, 0, 3));
    return toRet;
  }

  /**
   * Constructor for the Board.
   *
   * @param settings
   *                 Settings for how the board should be made.
   */
  public Board(GameSettings settings) {
    if (settings.isCursedCatan) {
      // Cursed Catan uses its own fixed-size island; ports are placed by
      // buildCursedBoard() rather than at the standard locations.
      PORT_LOCATION = new ArrayList<HexCoordinate>();
      buildCursedBoard();
      return;
    }
    List<TileType> availTiles = new ArrayList<TileType>();
    int[] rollNums = ROLL_NUMS;
    // Determines whether the board should be random or not;
    if (settings.isStandard) {
      availTiles = standardBoard();
      rollNums = Settings.STANDARD_ROLL_NUMS;
    } else {
      addTiles(availTiles, WOOD, NUM_WOOD_TILE);
      addTiles(availTiles, BRICK, NUM_BRICK_TILE);
      addTiles(availTiles, SHEEP, NUM_SHEEP_TILE);
      addTiles(availTiles, WHEAT, NUM_WHEAT_TILE);
      addTiles(availTiles, ORE, NUM_ORE_TILE);
      addTiles(availTiles, DESERT, NUM_DESERT_TILE);
      do {
        Collections.shuffle(availTiles);
      } while ((availTiles.get(0) == DESERT));
    }
    // Sets the port locations
    PORT_LOCATION = setPortLocations();

    Map<IntersectionCoordinate, Intersection> intersections = new HashMap<IntersectionCoordinate, Intersection>();
    Map<PathCoordinate, Path> paths = new HashMap<PathCoordinate, Path>();
    _tiles = new ArrayList<>();
    int currRoll = 0;
    int currTile = 0;
    int currDepth = DEPTH;
    int x = DEPTH;
    int y = 0;
    int z = 0;
    int i = 0;
    // How it constructs the board
    while (currDepth >= 0) {
      currRoll = addTile(availTiles.get(currTile), new HexCoordinate(x, y, z),
          intersections, paths, currRoll, currTile, rollNums);
      currTile++;

      for (i = 0; i < currDepth; i++) {
        y++;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }

      for (i = 0; i < currDepth; i++) {
        x--;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }

      for (i = 0; i < currDepth; i++) {
        z++;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }

      for (i = 0; i < currDepth; i++) {
        y--;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }

      for (i = 0; i < currDepth; i++) {
        x++;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }

      for (i = 1; i < currDepth; i++) {
        z--;
        currRoll = addTile(availTiles.get(currTile),
            new HexCoordinate(x, y, z), intersections, paths, currRoll,
            currTile, rollNums);
        currTile++;
      }
      z--;
      x--;
      currDepth--;
    }

    _intersections = intersections;
    _paths = paths;
    i = 0;
    List<HexCoordinate> seaCoords = getSeaPermutations();
    // Adds sea tiles and ports
    for (HexCoordinate hc : seaCoords) {
      Tile seaTile = new Tile(hc, SEA, _intersections);
      if (PORT_LOCATION.contains(hc)) {
        seaTile.setPorts(new Port(
            Settings.PORT_ORDER[PORT_LOCATION.indexOf(hc)]));
        i++;
      }
      _tiles.add(seaTile);
    }
  }

  /**
   * 
   * @param tileType
   * @param coord
   * @param intersections
   * @param paths
   * @param currRoll
   * @param currTile
   * @param rollNums
   * @return
   */
  private int addTile(TileType tileType, HexCoordinate coord,
      Map<IntersectionCoordinate, Intersection> intersections,
      Map<PathCoordinate, Path> paths, Integer currRoll, Integer currTile,
      int[] rollNums) {
    if (tileType != DESERT) {
      _tiles.add(new Tile(rollNums[currRoll], coord, intersections, paths,
          tileType));
      return currRoll + 1;
    } else {
      _tiles.add(new Tile(0, coord, intersections, paths, tileType, true));
      return currRoll;
    }
  }

  // ---------------------------------------------------------------------
  // Cursed Catan board
  // ---------------------------------------------------------------------

  // Rows of the island in order of increasing z (the client draws them as
  // columns, so on screen they read 4, 5, 6, 5, 4, 3 from left to right): a
  // fat, vanilla-like hexagon that swells to six tiles and tapers to three.
  private static final int[] CURSED_ROW_WIDTHS = { 3, 4, 5, 6, 5, 4 };
  // Number of ports on the coast (matches the nine resources in PORT_ORDER).
  private static final int CURSED_NUM_PORTS = 9;
  // How many times to retry number placement before accepting a layout.
  private static final int CURSED_ROLL_ATTEMPTS = 1000;

  /**
   * Builds the Cursed Catan board: one island of 27 land tiles, the same size
   * for every player count, surrounded by a ring of sea tiles that carries the
   * ports.
   */
  private void buildCursedBoard() {
    List<HexCoordinate> land = cursedLandCoordinates();
    List<TileType> types = cursedTileTypes();
    Collections.shuffle(types);
    int[] rolls = assignCursedRolls(land, types);

    Map<IntersectionCoordinate, Intersection> intersections = new HashMap<IntersectionCoordinate, Intersection>();
    Map<PathCoordinate, Path> paths = new HashMap<PathCoordinate, Path>();
    _tiles = new ArrayList<>();

    // Land. Only the first desert starts with the robber.
    boolean robberPlaced = false;
    for (int i = 0; i < land.size(); i++) {
      if (types.get(i) == DESERT) {
        _tiles.add(new Tile(0, land.get(i), intersections, paths, DESERT,
            !robberPlaced));
        robberPlaced = true;
      } else {
        _tiles.add(new Tile(rolls[i], land.get(i), intersections, paths,
            types.get(i)));
      }
    }
    _intersections = intersections;
    _paths = paths;

    // Sea ring and ports
    List<HexCoordinate> ring = seaRing(land);
    for (int i = 0; i < ring.size(); i++) {
      List<HexCoordinate> landNeighbors = new ArrayList<>();
      for (HexCoordinate n : neighborsOf(ring.get(i))) {
        if (land.contains(n)) {
          landNeighbors.add(n);
        }
      }
      Tile seaTile = new Tile(ring.get(i), SEA, _intersections, landNeighbors);
      for (int p = 0; p < CURSED_NUM_PORTS; p++) {
        if (i == portRingIndex(p, ring.size())) {
          seaTile.setPorts(new Port(Settings.PORT_ORDER[p]));
        }
      }
      _tiles.add(seaTile);
    }
  }

  /**
   * The land coordinates of the Cursed Catan island, row by row.
   *
   * @return The 27 land coordinates.
   */
  static List<HexCoordinate> cursedLandCoordinates() {
    List<HexCoordinate> land = new ArrayList<>();
    int rows = CURSED_ROW_WIDTHS.length;
    for (int r = 0; r < rows; r++) {
      int width = CURSED_ROW_WIDTHS[r];
      // Centres the island near the origin. The offset has to keep
      // (width - 1 + z) even, which is what makes y below a whole number: with
      // widths 3, 4, 5, 6, 5, 4 an offset of 2 does that for every row.
      int z = r - 2;
      for (int i = 0; i < width; i++) {
        // Horizontal position, centred on the row. Twice the value, so that it
        // stays an integer for rows with an even number of tiles.
        int twiceX = 2 * i - (width - 1);
        if ((twiceX + z) % 2 != 0) {
          throw new IllegalStateException(
              "Cursed Catan row layout does not fit the hex grid.");
        }
        int y = (twiceX + z) / 2;
        int x = 0;
        // Shift so the smallest of the three coordinates is zero, matching the
        // standard board's style (this does not move the tile).
        int shift = -Math.min(x, Math.min(y, z));
        land.add(new HexCoordinate(x + shift, y + shift, z + shift));
      }
    }
    return land;
  }

  // One tile type per land coordinate, in a fixed order (shuffled by caller).
  private List<TileType> cursedTileTypes() {
    List<TileType> types = new ArrayList<>();
    addTiles(types, WOOD, Settings.CURSED_NUM_WOOD_TILE);
    addTiles(types, BRICK, Settings.CURSED_NUM_BRICK_TILE);
    addTiles(types, SHEEP, Settings.CURSED_NUM_SHEEP_TILE);
    addTiles(types, WHEAT, Settings.CURSED_NUM_WHEAT_TILE);
    addTiles(types, ORE, Settings.CURSED_NUM_ORE_TILE);
    addTiles(types, DESERT, Settings.CURSED_NUM_DESERT_TILE);
    addTiles(types, GOLD, Settings.CURSED_NUM_GOLD_TILE);
    addTiles(types, CONCRETE, Settings.CURSED_NUM_CONCRETE_TILE);
    addTiles(types, COAL, Settings.CURSED_NUM_COAL_TILE);
    return types;
  }

  /**
   * Deals the number chits to the non-desert tiles at random, so that no two
   * touching tiles both carry a 6 or an 8, or the same number. Tiles are
   * numbered one at a time, each taking a random value that does not clash
   * with its already-numbered neighbours; if that gets stuck it starts over.
   *
   * @return The roll number of each land tile (0 for deserts).
   */
  private int[] assignCursedRolls(List<HexCoordinate> land,
      List<TileType> types) {
    List<Integer> producing = new ArrayList<>();
    for (int i = 0; i < land.size(); i++) {
      if (types.get(i) != DESERT) {
        producing.add(i);
      }
    }
    List<Integer> nums = new ArrayList<>();
    for (int n : Settings.CURSED_ROLL_NUMS) {
      nums.add(n);
    }
    if (nums.size() != producing.size()) {
      throw new IllegalStateException(String.format(
          "Cursed Catan needs %d number chits but has %d.", producing.size(),
          nums.size()));
    }
    // Which tiles touch which
    List<List<Integer>> touches = new ArrayList<>();
    for (int i = 0; i < land.size(); i++) {
      List<Integer> mine = new ArrayList<>();
      for (int j = 0; j < land.size(); j++) {
        if (i != j && touching(land.get(i), land.get(j))) {
          mine.add(j);
        }
      }
      touches.add(mine);
    }

    for (int attempt = 0; attempt < CURSED_ROLL_ATTEMPTS; attempt++) {
      int[] rolls = new int[land.size()];
      List<Integer> remaining = new ArrayList<>(nums);
      List<Integer> order = new ArrayList<>(producing);
      Collections.shuffle(order);
      boolean stuck = false;
      for (int tile : order) {
        List<Integer> options = new ArrayList<>();
        for (int value : remaining) {
          if (!options.contains(value) && !clashes(value, tile, rolls,
              touches)) {
            options.add(value);
          }
        }
        if (options.isEmpty()) {
          stuck = true;
          break;
        }
        int chosen = options.get((int) (Math.random() * options.size()));
        rolls[tile] = chosen;
        remaining.remove(Integer.valueOf(chosen));
      }
      if (!stuck) {
        return rolls;
      }
    }
    // Practically unreachable; fall back to a plain shuffle rather than fail.
    Collections.shuffle(nums);
    int[] rolls = new int[land.size()];
    for (int k = 0; k < producing.size(); k++) {
      rolls[producing.get(k)] = nums.get(k);
    }
    return rolls;
  }

  // True if putting value on tile would give it a touching neighbour that is
  // also a 6/8, or has the same number.
  private static boolean clashes(int value, int tile, int[] rolls,
      List<List<Integer>> touches) {
    boolean hot = value == 6 || value == 8;
    for (int neighbor : touches.get(tile)) {
      int other = rolls[neighbor];
      if (other == 0) {
        continue;
      }
      if (other == value || (hot && (other == 6 || other == 8))) {
        return true;
      }
    }
    return false;
  }

  // True if two hexes share an edge.
  private static boolean touching(HexCoordinate a, HexCoordinate b) {
    double dx = a.cartesianX() - b.cartesianX();
    double dy = a.cartesianY() - b.cartesianY();
    return Math.sqrt(dx * dx + dy * dy) < 1.01;
  }

  // The six hexes around a hex, in the same order Tile.fillEdges() uses.
  private static List<HexCoordinate> neighborsOf(HexCoordinate h) {
    List<HexCoordinate> n = new ArrayList<>();
    n.add(new HexCoordinate(h.getX(), h.getY(), h.getZ() + 1));
    n.add(new HexCoordinate(h.getX(), h.getY() + 1, h.getZ() + 1));
    n.add(new HexCoordinate(h.getX(), h.getY() + 1, h.getZ()));
    n.add(new HexCoordinate(h.getX() + 1, h.getY() + 1, h.getZ()));
    n.add(new HexCoordinate(h.getX() + 1, h.getY(), h.getZ()));
    n.add(new HexCoordinate(h.getX() + 1, h.getY(), h.getZ() + 1));
    return n;
  }

  /**
   * Every non-land hex that touches the island, ordered by angle around the
   * island's centre so evenly spaced picks are evenly spaced on the coast.
   */
  private static List<HexCoordinate> seaRing(List<HexCoordinate> land) {
    final List<HexCoordinate> ring = new ArrayList<>();
    for (HexCoordinate l : land) {
      for (HexCoordinate n : neighborsOf(l)) {
        if (!land.contains(n) && !ring.contains(n)) {
          ring.add(n);
        }
      }
    }
    double cx = 0;
    double cy = 0;
    for (HexCoordinate l : land) {
      cx += l.cartesianX();
      cy += l.cartesianY();
    }
    final double centerX = cx / land.size();
    final double centerY = cy / land.size();
    Collections.sort(ring, new java.util.Comparator<HexCoordinate>() {
      @Override
      public int compare(HexCoordinate a, HexCoordinate b) {
        return Double.compare(
            Math.atan2(a.cartesianY() - centerY, a.cartesianX() - centerX),
            Math.atan2(b.cartesianY() - centerY, b.cartesianX() - centerX));
      }
    });
    return ring;
  }

  // Index in the ring of the sea tile that holds port number p.
  private static int portRingIndex(int p, int ringSize) {
    return (int) Math.round(p * ringSize / (double) CURSED_NUM_PORTS)
        % ringSize;
  }

  public HexCoordinate findRobber() {
    for (Tile t : _tiles) {
      if (t.hasRobber()) {
        return t.getCoordinate();
      }
    }
    return null;
  }

  /**
   * Takes the robber off the board (the Great War has no robber).
   */
  public void removeRobber() {
    for (Tile t : _tiles) {
      t.hasRobber(false);
    }
  }

  /**
   * Puts the robber back on a tile, wherever it was before (used when the war
   * is over).
   *
   * @param coord
   *          The tile to put it on.
   */
  public void placeRobber(HexCoordinate coord) {
    for (Tile t : _tiles) {
      t.hasRobber(t.getCoordinate().equals(coord));
    }
  }

  /**
   * Moves the robber to a given HexCoord.
   * 
   * @param coord
   *              Coordinate of the tile to moved it to.
   * @return Set of Integers that are the playerIDs of the people on the tile
   *         the robber was moved to.
   */
  public Set<Integer> moveRobber(HexCoordinate coord) {
    Set<Integer> playersOnTile = Collections.emptySet();
    for (Tile t : _tiles) {
      if (t.hasRobber()) {
        if (t.getCoordinate().equals(coord)) {
          throw new IllegalArgumentException(
              "The robber must be moved to a new location.");
        }
        t.hasRobber(false);
      } else if (t.getCoordinate().equals(coord)) {
        t.hasRobber(true);
        playersOnTile = t.getPlayersOnTile();
      }
    }
    return playersOnTile;
  }

  /**
   * Finds the longest path on the board.
   * 
   * @param player
   *               Finds the longest path belonging to this player.
   * @return The length of the players longest road.
   */
  public int longestPath(Player player) {
    int max = 0;
    for (Path path : _paths.values()) {
      int longestPath = path.getLongestPath(player);
      if (longestPath > max) {
        max = longestPath;
      }
    }
    return max;
  }

  // Standard Board
  private List<TileType> standardBoard() {
    List<TileType> tiles = new ArrayList<>();
    tiles.add(WHEAT);
    tiles.add(SHEEP);
    tiles.add(WOOD);
    tiles.add(BRICK);
    tiles.add(DESERT);
    tiles.add(BRICK);
    tiles.add(ORE);
    tiles.add(WHEAT);
    tiles.add(WOOD);
    tiles.add(ORE);
    tiles.add(WHEAT);
    tiles.add(SHEEP);
    tiles.add(BRICK);
    tiles.add(ORE);
    tiles.add(WOOD);
    tiles.add(SHEEP);
    tiles.add(SHEEP);
    tiles.add(WOOD);
    tiles.add(WHEAT);
    return tiles;
  }

  /**
   * Gets the Path between two intersection coordinates.
   *
   * @param start Start intersection coordinate.
   * @param end   End intersection coordinate.
   * @return The Path, or null if not found.
   */
  public Path getPath(IntersectionCoordinate start,
      IntersectionCoordinate end) {
    PathCoordinate pc = new PathCoordinate(start, end);
    Path p = _paths.get(pc);
    if (p != null) {
      return p;
    }
    // Try reverse
    PathCoordinate pcReverse = new PathCoordinate(end, start);
    return _paths.get(pcReverse);
  }

  /**
   * Removes a road from the path between two intersections.
   *
   * @param start Start coordinate.
   * @param end   End coordinate.
   * @throws IllegalArgumentException if no road exists there.
   */
  public void removeRoad(IntersectionCoordinate start,
      IntersectionCoordinate end) {
    Path path = getPath(start, end);
    if (path == null || path.getRoad() == null) {
      throw new IllegalArgumentException("No road exists at that location.");
    }
    path.removeRoad();
  }

  /**
   * Whether nothing can grow any more: every land tile in the collection is
   * Desert or Wasteland. In Cursed Catan that is a draw.
   *
   * @param tiles
   *          The tiles to look at (sea tiles are ignored).
   * @return True if every land tile is Desert or Wasteland.
   */
  public static boolean isAllBarren(Collection<Tile> tiles) {
    for (Tile t : tiles) {
      TileType type = t.getType();
      if (type != TileType.SEA && type != TileType.DESERT
          && type != TileType.WASTELAND) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets a tile by its hex coordinate.
   *
   * @param coord The coordinate to look up.
   * @return The Tile, or null if not found.
   */
  public Tile getTile(HexCoordinate coord) {
    for (Tile t : _tiles) {
      if (t.getCoordinate().equals(coord)) {
        return t;
      }
    }
    return null;
  }

  /**
   * Swaps the roll numbers between two tiles. Used by the Inventor card.
   * Cannot swap tiles with roll numbers 2, 6, 8, or 12.
   *
   * @param h1 First hex coordinate.
   * @param h2 Second hex coordinate.
   * @throws IllegalArgumentException if tiles not found or restricted numbers.
   */
  public void swapRollNumbers(HexCoordinate h1, HexCoordinate h2) {
    Tile t1 = getTile(h1);
    Tile t2 = getTile(h2);
    if (t1 == null || t2 == null) {
      throw new IllegalArgumentException("One or both tiles not found.");
    }
    int num1 = t1.getRollNumber();
    int num2 = t2.getRollNumber();
    if (num1 == 2 || num1 == 6 || num1 == 8 || num1 == 12
        || num2 == 2 || num2 == 6 || num2 == 8 || num2 == 12) {
      throw new IllegalArgumentException(
          "Cannot swap tiles with roll numbers 2, 6, 8, or 12.");
    }
    t1.setRollNumber(num2);
    t2.setRollNumber(num1);
  }

}
