package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Cursed Catan, Great War: instead of moving, the ground troops on a tile
 * entrench. Attackers roll at -1 against entrenched defenders, and troops that
 * later move into the tile are entrenched too. Units built this turn can
 * entrench.
 *
 * <p>
 * Request JSON: <code>{tile:{x,y,z}}</code>
 */
public class Entrench implements Action {

  public static final String ID = "entrench";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;

  public Entrench(Referee ref, int playerID, JsonObject params) {
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
    String problem = WarTurn.problem(_ref, _player, "entrench");
    if (problem != null) {
      return fail(problem);
    }
    Tile tile;
    try {
      tile = WarTurn.landTile(_ref, _params, "tile");
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    }
    Faction faction = _ref.getFaction(_player.getID());
    if (_ref.getWarState().getReadyToEntrench(tile.getCoordinate(), faction) < 1) {
      return fail("You have no ground troops there that can entrench.");
    }
    if (_ref.getWarState().isEntrenched(tile.getCoordinate(), faction)
        && _ref.getWarState().getReadyToMove(tile.getCoordinate(), faction,
            edu.brown.cs.catan.UnitType.INFANTRY) == 0
        && _ref.getWarState().getReadyToEntrench(tile.getCoordinate(), faction) < 1) {
      return fail("Already entrenched.");
    }
    _ref.getWarState().entrench(tile.getCoordinate(), faction);
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true, p.equals(_player) ? "Your troops entrenched."
          : String.format("%s entrenched.", faction), null));
    }
    return toRet;
  }
}
