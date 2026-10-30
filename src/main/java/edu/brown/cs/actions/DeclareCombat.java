package edu.brown.cs.actions;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.IntSupplier;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.UnitType;
import edu.brown.cs.catan.WarCombat;

/**
 * Cursed Catan, Great War: fight on a tile where the player's faction has
 * units, if an enemy has units there or a building the player can attack on
 * or next to it. Units listed under <code>buildings</code> attack buildings;
 * the rest attack troops.
 *
 * <p>
 * Request JSON:
 * <code>{tile:{x,y,z}, enemy:"Russia", buildings:{artillery:1}}</code>. The
 * enemy can be left out when only one faction can be fought there.
 */
public class DeclareCombat implements Action {

  public static final String ID = "declareCombat";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;
  private final IntSupplier _die;

  public DeclareCombat(Referee ref, int playerID, JsonObject params) {
    this(ref, playerID, params, new IntSupplier() {
      private final Random random = new Random();

      @Override
      public int getAsInt() {
        return 1 + random.nextInt(6);
      }
    });
  }

  /**
   * Constructor with a given die, so that tests can script the rolls.
   */
  public DeclareCombat(Referee ref, int playerID, JsonObject params,
      IntSupplier die) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    _params = params;
    _die = die;
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    String problem = WarTurn.problem(_ref, _player, "fight");
    if (problem != null) {
      return fail(problem);
    }
    Faction attacker = _ref.getFaction(_player.getID());
    Tile tile;
    Map<UnitType, Integer> toBuildings = new EnumMap<>(UnitType.class);
    try {
      tile = WarTurn.landTile(_ref, _params, "tile");
      if (_params.has("buildings") && _params.get("buildings").isJsonObject()) {
        for (Map.Entry<String, JsonElement> e : _params.getAsJsonObject(
            "buildings").entrySet()) {
          int n = e.getValue().getAsInt();
          if (n < 0) {
            return fail("Counts cannot be negative.");
          }
          if (n > 0) {
            toBuildings.put(UnitType.fromString(e.getKey()), n);
          }
        }
      }
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    } catch (RuntimeException e) {
      return fail("Choose a tile to fight on.");
    }
    Faction defender = WarTurn.faction(_params, "enemy");
    if (defender == null) {
      Faction only = null;
      int candidates = 0;
      for (Faction f : _ref.getWarState().getFactionList()) {
        if (f != attacker && WarCombat.canFightAt(_ref, tile, attacker, f)) {
          only = f;
          candidates++;
        }
      }
      if (candidates != 1) {
        return fail(candidates == 0 ? "There is nothing to fight there."
            : "Choose which enemy faction to fight.");
      }
      defender = only;
    }
    String result;
    try {
      result = WarCombat.fight(_ref, attacker, defender, tile, toBuildings, _die);
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    }
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true, result, null));
    }
    return toRet;
  }
}
