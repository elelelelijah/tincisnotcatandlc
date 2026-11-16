package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Cursed Catan: lets the game pick a player's casualties (cheapest first)
 * instead of asking them each time, so that a player who is away cannot hold a
 * fight up. Works at any time.
 *
 * <p>
 * Request JSON: <code>{enabled:true}</code>
 */
public class SetAutoCasualties implements Action {

  public static final String ID = "setAutoCasualties";

  private final Referee _ref;
  private final Player _player;
  private final boolean _enabled;

  public SetAutoCasualties(Referee ref, int playerID, JsonObject params) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    JsonElement e = params == null ? null : params.get("enabled");
    _enabled = e != null && e.isJsonPrimitive() && e.getAsBoolean();
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    if (!_ref.getGameSettings().isCursedCatan) {
      toRet.put(_player.getID(), new ActionResponse(false,
          "Casualties only exist in Cursed Catan.", null));
      return toRet;
    }
    _ref.setAutoCasualties(_player.getID(), _enabled);
    toRet.put(_player.getID(), new ActionResponse(true, _enabled
        ? "The game will pick your casualties (cheapest first)."
        : "You will be asked to choose your casualties.", null));
    return toRet;
  }
}
