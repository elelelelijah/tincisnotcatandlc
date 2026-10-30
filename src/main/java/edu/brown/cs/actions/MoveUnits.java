package edu.brown.cs.actions;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.UnitType;
import edu.brown.cs.catan.WarState;

/**
 * Cursed Catan, Great War: move units. Ground troops move 1 tile; aircraft
 * move anywhere. Troops that start on a tile with enemies can only move to a
 * tile with no enemies. Units that moved, entrenched or were built this turn
 * cannot move.
 *
 * <p>
 * Request JSON:
 * <code>{from:{x,y,z}, to:{x,y,z}, units:{infantry:2, tank:1}}</code>
 */
public class MoveUnits implements Action {

  public static final String ID = "moveUnits";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;

  public MoveUnits(Referee ref, int playerID, JsonObject params) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    _params = params;
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    String problem = WarTurn.problem(_ref, _player, "move units");
    if (problem != null) {
      return fail(problem);
    }
    WarState war = _ref.getWarState();
    Faction faction = _ref.getFaction(_player.getID());
    Tile from;
    Tile to;
    Map<UnitType, Integer> units = new EnumMap<>(UnitType.class);
    try {
      from = WarTurn.landTile(_ref, _params, "from");
      to = WarTurn.landTile(_ref, _params, "to");
      for (Map.Entry<String, JsonElement> e : _params.get("units")
          .getAsJsonObject().entrySet()) {
        int n = e.getValue().getAsInt();
        if (n < 0) {
          return fail("Counts cannot be negative.");
        }
        if (n > 0) {
          units.put(UnitType.fromString(e.getKey()), n);
        }
      }
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    } catch (RuntimeException e) {
      return fail("Say where from, where to and which units.");
    }
    if (units.isEmpty()) {
      return fail("Choose which units to move.");
    }
    if (from.equals(to)) {
      return fail("Those units are already there.");
    }
    boolean trapped = war.hasEnemyUnits(from.getCoordinate(), faction, false);
    boolean enemiesAtDestination = war.hasEnemyUnits(to.getCoordinate(),
        faction, false);
    for (Map.Entry<UnitType, Integer> e : units.entrySet()) {
      UnitType t = e.getKey();
      if (war.getUnits(from.getCoordinate(), faction, t) < e.getValue()) {
        return fail(String.format("You don't have %d %s there.", e.getValue(), t));
      }
      if (war.getReadyToMove(from.getCoordinate(), faction, t) < e.getValue()) {
        return fail(String.format(
            "Those %s already moved or were built this turn.", t));
      }
      if (WarState.isGround(t)) {
        if (!from.getCoordinate().isAdjacentTo(to.getCoordinate())) {
          return fail("Ground troops move 1 tile.");
        }
        if (trapped && enemiesAtDestination) {
          return fail("Troops that start with enemies can only move to a tile with no enemies.");
        }
      }
    }
    for (Map.Entry<UnitType, Integer> e : units.entrySet()) {
      war.moveUnits(from.getCoordinate(), to.getCoordinate(), faction, e
          .getKey(), e.getValue());
    }
    int total = 0;
    for (int n : units.values()) {
      total += n;
    }
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true, p.equals(_player) ? String
          .format("You moved %d unit%s.", total, total == 1 ? "" : "s") : String
          .format("%s moved %d unit%s.", faction, total, total == 1 ? "" : "s"),
          null));
    }
    return toRet;
  }
}
