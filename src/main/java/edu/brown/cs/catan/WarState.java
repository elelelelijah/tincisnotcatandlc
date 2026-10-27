package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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

  public WarState(Map<Integer, Faction> factions) {
    _factions = Collections.unmodifiableMap(new LinkedHashMap<>(factions));
    for (Faction f : _factions.values()) {
      _morale.put(f, STARTING_MORALE);
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
}
