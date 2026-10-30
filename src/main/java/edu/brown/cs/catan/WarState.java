package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.brown.cs.board.HexCoordinate;

/**
 * Everything that only exists while the Great War is on: who fights for which
 * faction, each faction's Morale, the units on the board, the Nuclear Winter
 * track and Sanctions.
 */
public class WarState {

  public static final int STARTING_MORALE = 12;
  public static final int MAX_MORALE = 20;
  public static final int NUCLEAR_WINTER_LAST = 6;

  /**
   * The roll a faction needs on the Nuclear Winter die to move onto each
   * position of the track (index = position). Position 1 is the start.
   */
  private static final int[] WINTER_ROLL_NEEDED = { 0, 0, 6, 5, 4, 3, 2 };

  private final Map<Integer, Faction> _factions;
  private final Map<Faction, Integer> _morale = new EnumMap<>(Faction.class);
  private final Map<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> _units = new LinkedHashMap<>();
  private int _nuclearWinter = 1;
  private Faction _sanctioned = null;
  // Units that built or moved this turn, and stacks that are entrenched
  private final Map<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> _moved = new LinkedHashMap<>();
  private final Map<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> _built = new LinkedHashMap<>();
  private final Map<HexCoordinate, Set<Faction>> _entrenched = new LinkedHashMap<>();
  private final Set<HexCoordinate> _fought = new HashSet<>();
  private final Map<Faction, Set<Tech>> _tech = new EnumMap<>(Faction.class);
  private final Map<Faction, Integer> _banked = new EnumMap<>(Faction.class);

  public WarState(Map<Integer, Faction> factions) {
    _factions = Collections.unmodifiableMap(new LinkedHashMap<>(factions));
    for (Faction f : _factions.values()) {
      _morale.put(f, STARTING_MORALE);
      _tech.put(f, EnumSet.noneOf(Tech.class));
      _banked.put(f, 0);
    }
  }

  /**
   * Player id to faction.
   */
  public Map<Integer, Faction> getFactions() {
    return _factions;
  }

  public Faction getFaction(int playerID) {
    return _factions.get(playerID);
  }

  /**
   * The factions in the war, in the order they were handed out.
   */
  public List<Faction> getFactionList() {
    List<Faction> list = new ArrayList<>();
    for (Faction f : _factions.values()) {
      if (!list.contains(f)) {
        list.add(f);
      }
    }
    return list;
  }

  public List<Integer> getPlayersOf(Faction faction) {
    List<Integer> ids = new ArrayList<>();
    for (Map.Entry<Integer, Faction> e : _factions.entrySet()) {
      if (e.getValue() == faction) {
        ids.add(e.getKey());
      }
    }
    return ids;
  }

  // ----------------------------------------------------------------- morale

  public int getMorale(Faction faction) {
    Integer m = _morale.get(faction);
    return m == null ? 0 : m;
  }

  /**
   * Changes a faction's Morale, keeping it between 0 and the maximum.
   *
   * @return The new Morale.
   */
  public int changeMorale(Faction faction, int delta) {
    int now = Math.max(0, Math.min(MAX_MORALE, getMorale(faction) + delta));
    _morale.put(faction, now);
    return now;
  }

  // ------------------------------------------------------------------ units

  public void addUnits(HexCoordinate tile, Faction faction, UnitType type,
      int count) {
    if (count <= 0) {
      return;
    }
    Map<Faction, Map<UnitType, Integer>> onTile = _units.get(tile);
    if (onTile == null) {
      onTile = new EnumMap<>(Faction.class);
      _units.put(tile, onTile);
    }
    Map<UnitType, Integer> mine = onTile.get(faction);
    if (mine == null) {
      mine = new EnumMap<>(UnitType.class);
      onTile.put(faction, mine);
    }
    Integer old = mine.get(type);
    mine.put(type, old == null ? count : old + count);
  }

  public int getUnits(HexCoordinate tile, Faction faction, UnitType type) {
    Map<Faction, Map<UnitType, Integer>> onTile = _units.get(tile);
    if (onTile == null || onTile.get(faction) == null) {
      return 0;
    }
    Integer n = onTile.get(faction).get(type);
    return n == null ? 0 : n;
  }

  public int getUnitCount(Faction faction) {
    int total = 0;
    for (Map<Faction, Map<UnitType, Integer>> onTile : _units.values()) {
      if (onTile.get(faction) != null) {
        for (int n : onTile.get(faction).values()) {
          total += n;
        }
      }
    }
    return total;
  }

  /**
   * Units on the board: tile, then faction, then unit type and count.
   */
  public Map<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> getUnits() {
    return Collections.unmodifiableMap(_units);
  }

  /**
   * Removes every unit on a tile (an Atomic Bomb destroys them), or only some.
   *
   * @return How many units were removed.
   */
  public int removeUnit(HexCoordinate tile, Faction faction, UnitType type,
      int count) {
    Map<Faction, Map<UnitType, Integer>> onTile = _units.get(tile);
    if (onTile == null || onTile.get(faction) == null) {
      return 0;
    }
    Integer have = onTile.get(faction).get(type);
    if (have == null) {
      return 0;
    }
    int removed = Math.min(have, count);
    if (removed == have) {
      onTile.get(faction).remove(type);
    } else {
      onTile.get(faction).put(type, have - removed);
    }
    if (onTile.get(faction).isEmpty()) {
      onTile.remove(faction);
    }
    if (onTile.isEmpty()) {
      _units.remove(tile);
    }
    return removed;
  }

  // ---------------------------------------------------------- nuclear winter

  public int getNuclearWinter() {
    return _nuclearWinter;
  }

  /**
   * What the Nuclear Winter die has to show to move onto the next position, or
   * 0 if the track is already at its end.
   */
  public int rollNeededToAdvance() {
    return _nuclearWinter >= NUCLEAR_WINTER_LAST ? 0
        : WINTER_ROLL_NEEDED[_nuclearWinter + 1];
  }

  /**
   * Moves the track one position forward.
   *
   * @return The position it arrived on.
   */
  public int advanceNuclearWinter() {
    if (_nuclearWinter < NUCLEAR_WINTER_LAST) {
      _nuclearWinter++;
    }
    return _nuclearWinter;
  }

  // -------------------------------------------------------------- sanctions

  /**
   * The faction that launched first and is under Sanctions, or null.
   */
  public Faction getSanctioned() {
    return _sanctioned;
  }

  public void sanction(Faction faction) {
    if (_sanctioned == null) {
      _sanctioned = faction;
    }
  }

  // --------------------------------------------------------------- research

  /** Everything the faction has researched. */
  public Set<Tech> getTech(Faction faction) {
    Set<Tech> set = _tech.get(faction);
    return set == null ? EnumSet.noneOf(Tech.class) : EnumSet.copyOf(set);
  }

  public boolean hasTech(Faction faction, Tech tech) {
    Set<Tech> set = _tech.get(faction);
    return set != null && set.contains(tech);
  }

  /**
   * Whether the faction can research the node now: it is not done yet, and
   * the node above it is.
   */
  public boolean canResearch(Faction faction, Tech tech) {
    if (!_tech.containsKey(faction) || hasTech(faction, tech)) {
      return false;
    }
    return tech.getPrerequisite() == null
        || hasTech(faction, tech.getPrerequisite());
  }

  /**
   * Marks a node as researched; it works for the whole faction at once.
   */
  public void unlock(Faction faction, Tech tech) {
    if (!canResearch(faction, tech)) {
      throw new IllegalArgumentException(String.format(
          "%s cannot research %s now.", faction, tech));
    }
    _tech.get(faction).add(tech);
  }

  /** Research Points the faction has gained but not spent yet. */
  public int getBankedPoints(Faction faction) {
    Integer n = _banked.get(faction);
    return n == null ? 0 : n;
  }

  public void addResearchPoints(Faction faction, int points) {
    if (_banked.containsKey(faction) && points > 0) {
      _banked.put(faction, _banked.get(faction) + points);
    }
  }

  /**
   * Spends one banked Research Point.
   *
   * @return False if there was none.
   */
  public boolean spendResearchPoint(Faction faction) {
    if (getBankedPoints(faction) < 1) {
      return false;
    }
    _banked.put(faction, _banked.get(faction) - 1);
    return true;
  }

  /**
   * All the Research Points the faction has gained so far: nodes done plus
   * points still banked. This is what the Morale rules compare.
   */
  public int getResearchTotal(Faction faction) {
    return getTech(faction).size() + getBankedPoints(faction);
  }

  // ----------------------------------------------------- movement and combat

  /** Whether the unit type is a ground troop (everything but aircraft). */
  public static boolean isGround(UnitType type) {
    return type != UnitType.AIRCRAFT;
  }

  private static int get(
      Map<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> map,
      HexCoordinate tile, Faction faction, UnitType type) {
    Map<Faction, Map<UnitType, Integer>> onTile = map.get(tile);
    if (onTile == null || onTile.get(faction) == null) {
      return 0;
    }
    Integer n = onTile.get(faction).get(type);
    return n == null ? 0 : n;
  }

  private static void put(
      Map<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> map,
      HexCoordinate tile, Faction faction, UnitType type, int count) {
    Map<Faction, Map<UnitType, Integer>> onTile = map.get(tile);
    if (onTile == null) {
      onTile = new EnumMap<>(Faction.class);
      map.put(tile, onTile);
    }
    Map<UnitType, Integer> mine = onTile.get(faction);
    if (mine == null) {
      mine = new EnumMap<>(UnitType.class);
      onTile.put(faction, mine);
    }
    if (count <= 0) {
      mine.remove(type);
    } else {
      mine.put(type, count);
    }
  }

  /**
   * Adds a unit that was built this turn: it cannot move or fight until the
   * next turn (ground troops can still entrench).
   */
  public void addBuiltUnit(HexCoordinate tile, Faction faction, UnitType type) {
    addUnits(tile, faction, type, 1);
    put(_built, tile, faction, type, get(_built, tile, faction, type) + 1);
  }

  /** How many of the units built this turn are on the tile. */
  public int getBuiltThisTurn(HexCoordinate tile, Faction faction, UnitType type) {
    return get(_built, tile, faction, type);
  }

  /**
   * How many units of the type could move or entrench from the tile now:
   * the ones that did not arrive, entrench or get built this turn.
   */
  public int getReadyToMove(HexCoordinate tile, Faction faction, UnitType type) {
    return Math.max(0, getUnits(tile, faction, type)
        - get(_moved, tile, faction, type) - get(_built, tile, faction, type));
  }

  /**
   * How many ground troops on the tile could entrench: everything that has
   * not moved this turn (units built this turn can entrench).
   */
  public int getReadyToEntrench(HexCoordinate tile, Faction faction) {
    int n = 0;
    for (UnitType t : UnitType.values()) {
      if (isGround(t)) {
        n += Math.max(0, getUnits(tile, faction, t)
            - get(_moved, tile, faction, t));
      }
    }
    return n;
  }

  /**
   * How many units of the type can fight on the tile: everything except what
   * was built this turn.
   */
  public int getReadyToFight(HexCoordinate tile, Faction faction, UnitType type) {
    return Math.max(0, getUnits(tile, faction, type)
        - get(_built, tile, faction, type));
  }

  /**
   * Moves units from one tile to the next; they cannot move again this turn.
   * Troops that arrive on a tile where their faction is entrenched are
   * entrenched too.
   */
  public void moveUnits(HexCoordinate from, HexCoordinate to, Faction faction,
      UnitType type, int count) {
    if (count > getReadyToMove(from, faction, type)) {
      throw new IllegalArgumentException("Those units cannot move.");
    }
    removeUnit(from, faction, type, count);
    clampFlags(from, faction, type);
    addUnits(to, faction, type, count);
    put(_moved, to, faction, type, get(_moved, to, faction, type) + count);
    cleanEntrenchment(from, faction);
  }

  /**
   * Entrenches the ground troops on the tile. They use up their move.
   */
  public void entrench(HexCoordinate tile, Faction faction) {
    Set<Faction> set = _entrenched.get(tile);
    if (set == null) {
      set = EnumSet.noneOf(Faction.class);
      _entrenched.put(tile, set);
    }
    set.add(faction);
    for (UnitType t : UnitType.values()) {
      if (isGround(t)) {
        put(_moved, tile, faction, t, getUnits(tile, faction, t));
      }
    }
  }

  public boolean isEntrenched(HexCoordinate tile, Faction faction) {
    Set<Faction> set = _entrenched.get(tile);
    return set != null && set.contains(faction);
  }

  /**
   * Removes casualties (and keeps the bookkeeping straight).
   *
   * @return How many were removed.
   */
  public int killUnits(HexCoordinate tile, Faction faction, UnitType type,
      int count) {
    int removed = removeUnit(tile, faction, type, count);
    clampFlags(tile, faction, type);
    cleanEntrenchment(tile, faction);
    return removed;
  }

  private void clampFlags(HexCoordinate tile, Faction faction, UnitType type) {
    int total = getUnits(tile, faction, type);
    put(_moved, tile, faction, type, Math.min(total, get(_moved, tile, faction,
        type)));
    put(_built, tile, faction, type, Math.min(total, get(_built, tile, faction,
        type)));
  }

  private void cleanEntrenchment(HexCoordinate tile, Faction faction) {
    boolean anyGround = false;
    for (UnitType t : UnitType.values()) {
      if (isGround(t) && getUnits(tile, faction, t) > 0) {
        anyGround = true;
      }
    }
    if (!anyGround && _entrenched.get(tile) != null) {
      _entrenched.get(tile).remove(faction);
    }
  }

  /**
   * Whether troops of a faction other than the given one are on the tile.
   *
   * @param groundOnly
   *          Only count ground troops (aircraft do not stop production).
   */
  public boolean hasEnemyUnits(HexCoordinate tile, Faction faction,
      boolean groundOnly) {
    Map<Faction, Map<UnitType, Integer>> onTile = _units.get(tile);
    if (onTile == null) {
      return false;
    }
    for (Map.Entry<Faction, Map<UnitType, Integer>> e : onTile.entrySet()) {
      if (e.getKey() == faction) {
        continue;
      }
      for (Map.Entry<UnitType, Integer> u : e.getValue().entrySet()) {
        if (u.getValue() > 0 && (!groundOnly || isGround(u.getKey()))) {
          return true;
        }
      }
    }
    return false;
  }

  /** The factions (other than the given one) that have units on the tile. */
  public Set<Faction> getEnemyFactionsOn(HexCoordinate tile, Faction faction) {
    Set<Faction> set = new HashSet<>();
    Map<Faction, Map<UnitType, Integer>> onTile = _units.get(tile);
    if (onTile != null) {
      for (Map.Entry<Faction, Map<UnitType, Integer>> e : onTile.entrySet()) {
        int n = 0;
        for (int v : e.getValue().values()) {
          n += v;
        }
        if (e.getKey() != faction && n > 0) {
          set.add(e.getKey());
        }
      }
    }
    return set;
  }

  /**
   * A new turn begins: everything that moved or was built can act again.
   */
  public void clearTurnFlags() {
    _moved.clear();
    _built.clear();
    _fought.clear();
  }

  /** Whether a fight has already happened on the tile this turn. */
  public boolean hasFoughtThisTurn(HexCoordinate tile) {
    return _fought.contains(tile);
  }

  public void markFought(HexCoordinate tile) {
    _fought.add(tile);
  }
}
