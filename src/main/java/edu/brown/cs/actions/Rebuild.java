package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Building;
import edu.brown.cs.board.Capitol;
import edu.brown.cs.board.City;
import edu.brown.cs.board.Factory;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;

/**
 * Cursed Catan: rebuild a ruined building of yours, or repair a damaged one,
 * back to full hit points. The price is the rebuild cost of the building (a
 * repair costs the same). A building an enemy occupies has to be liberated
 * first.
 *
 * <pre>
 * Settlement  brick + wood
 * City        brick + ore, or brick + concrete    (with: "ore" | "concrete")
 * Factory     2 brick, or 2 concrete              (with: "brick" | "concrete")
 * Capitol     2 brick + ore + concrete
 * </pre>
 *
 * <p>
 * Request JSON: <code>{coordinate:{coord1..3}, with:"ore"}</code>
 */
public class Rebuild implements Action {

  public static final String ID = "rebuild";

  private final Referee _ref;
  private final Player _player;
  private final Intersection _intersection;
  private final JsonObject _params;

  public Rebuild(Referee ref, int playerID, IntersectionCoordinate coord,
      JsonObject params) {
    assert ref != null && coord != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    _intersection = ref.getBoard().getIntersections().get(coord);
    if (_intersection == null) {
      throw new IllegalArgumentException("The intersection could not be found.");
    }
    _params = params;
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  /**
   * The price of rebuilding or repairing the building, or null if the
   * player's choice ("with") does not name a way to pay.
   */
  private Map<Resource, Double> price(Building b) {
    Map<Resource, Double> cost = new LinkedHashMap<>();
    String with = null;
    JsonElement w = _params == null ? null : _params.get("with");
    if (w != null && w.isJsonPrimitive()) {
      with = w.getAsString();
    }
    if (b instanceof Capitol) {
      cost.put(Resource.BRICK, 2.0);
      cost.put(Resource.ORE, 1.0);
      cost.put(Resource.CONCRETE, 1.0);
    } else if (b instanceof City) {
      cost.put(Resource.BRICK, 1.0);
      if ("ore".equals(with)) {
        cost.put(Resource.ORE, 1.0);
      } else if ("concrete".equals(with)) {
        cost.put(Resource.CONCRETE, 1.0);
      } else {
        return null;
      }
    } else if (b instanceof Factory) {
      if ("brick".equals(with)) {
        cost.put(Resource.BRICK, 2.0);
      } else if ("concrete".equals(with)) {
        cost.put(Resource.CONCRETE, 2.0);
      } else {
        return null;
      }
    } else {
      cost.put(Resource.BRICK, 1.0);
      cost.put(Resource.WOOD, 1.0);
    }
    return cost;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCursedCatan) {
      return fail("Rebuilding only exists in Cursed Catan.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only rebuild on your own turn.");
    }
    Building b = _intersection.getBuilding();
    if (b == null || !b.getPlayer().equals(_player)) {
      return fail("Choose one of your own buildings.");
    }
    if (b.getOccupier() != null) {
      return fail("An enemy occupies that building. Liberate it first.");
    }
    if (!b.isRuined() && b.getHp() >= b.getMaxHp()) {
      return fail("That building is not damaged.");
    }
    Map<Resource, Double> cost = price(b);
    if (cost == null) {
      return fail("Choose how to pay: this building can be paid for in more than one way.");
    }
    for (Map.Entry<Resource, Double> e : cost.entrySet()) {
      if (!_player.hasResource(e.getKey(), e.getValue())) {
        return fail("You cannot afford that.");
      }
    }
    for (Map.Entry<Resource, Double> e : cost.entrySet()) {
      _player.removeResource(e.getKey(), e.getValue(), _ref.getBank());
    }
    boolean ruined = b.isRuined();
    b.setRuined(false);
    b.setHp(b.getMaxHp());

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      String what = ruined ? "rebuilt a ruined building" : "repaired a building";
      toRet.put(p.getID(), new ActionResponse(true, p.equals(_player) ? "You "
          + what + "." : String.format("%s %s.", _player.getName(), what),
          null));
    }
    return toRet;
  }
}
