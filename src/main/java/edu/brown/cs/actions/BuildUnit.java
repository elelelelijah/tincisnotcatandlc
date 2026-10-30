package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Building;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Tech;
import edu.brown.cs.catan.UnitType;

/**
 * Cursed Catan, Great War: buy a unit and put it on a tile next to one of
 * your faction's buildings (ruined ones count, but not next to a building an
 * enemy has occupied). Units bought this turn cannot move or fight until the
 * next turn.
 *
 * <p>
 * Request JSON: <code>{unit:"infantry", tile:{x,y,z}, with:"wheat"}</code>.
 * Infantry cost 2 wheat or 2 sheep and artillery cost ore plus sheep or
 * wheat; <code>with</code> says which. Tanks (1 wheat, 2 ore) need Unlock
 * Tanks and aircraft (2 wood, 1 ore) need Unlock Aircraft.
 */
public class BuildUnit implements Action {

  public static final String ID = "buildUnit";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;

  public BuildUnit(Referee ref, int playerID, JsonObject params) {
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

  /**
   * The price of the unit, or null if the request is not clear.
   */
  static Map<Resource, Double> price(UnitType type, String with) {
    Map<Resource, Double> cost = new LinkedHashMap<>();
    switch (type) {
    case INFANTRY:
      if ("wheat".equals(with)) {
        cost.put(Resource.WHEAT, 2.0);
      } else if ("sheep".equals(with)) {
        cost.put(Resource.SHEEP, 2.0);
      } else {
        return null;
      }
      return cost;
    case ARTILLERY:
      if ("wheat".equals(with)) {
        cost.put(Resource.WHEAT, 1.0);
      } else if ("sheep".equals(with)) {
        cost.put(Resource.SHEEP, 1.0);
      } else {
        return null;
      }
      cost.put(Resource.ORE, 1.0);
      return cost;
    case TANK:
      cost.put(Resource.WHEAT, 1.0);
      cost.put(Resource.ORE, 2.0);
      return cost;
    default:
      cost.put(Resource.WOOD, 2.0);
      cost.put(Resource.ORE, 1.0);
      return cost;
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    String problem = WarTurn.problem(_ref, _player, "build units");
    if (problem != null) {
      return fail(problem);
    }
    Faction faction = _ref.getFaction(_player.getID());
    UnitType type;
    Tile tile;
    try {
      type = UnitType.fromString(_params.get("unit").getAsString());
      tile = WarTurn.landTile(_ref, _params, "tile");
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    } catch (RuntimeException e) {
      return fail("Choose a unit and a tile.");
    }
    if (type == UnitType.TANK
        && !_ref.hasTech(_player.getID(), Tech.UNLOCK_TANKS)) {
      return fail("Your faction has not researched Tanks yet.");
    }
    if (type == UnitType.AIRCRAFT
        && !_ref.hasTech(_player.getID(), Tech.UNLOCK_AIRCRAFT)) {
      return fail("Your faction has not researched Aircraft yet.");
    }
    String with = _params.has("with") ? _params.get("with").getAsString() : "";
    Map<Resource, Double> cost = price(type, with);
    if (cost == null) {
      return fail("Say whether you pay with wheat or sheep.");
    }

    // Next to a friendly building, and not next to one an enemy occupies
    boolean friendly = false;
    for (Intersection i : tile.getIntersections()) {
      Building b = i.getBuilding();
      if (b == null) {
        continue;
      }
      if (b.getOccupier() != null && b.getOccupier() != faction) {
        return fail("You cannot place units next to a building an enemy has occupied.");
      }
      if (_ref.getFaction(b.getPlayer().getID()) == faction) {
        friendly = true;
      }
    }
    if (!friendly) {
      return fail("Units go on tiles next to your faction's buildings.");
    }
    for (Map.Entry<Resource, Double> e : cost.entrySet()) {
      if (!_player.hasResource(e.getKey(), e.getValue())) {
        return fail(String.format("You need %s to build that unit.", e.getKey()));
      }
    }
    for (Map.Entry<Resource, Double> e : cost.entrySet()) {
      _player.removeResource(e.getKey(), e.getValue(), _ref.getBank());
    }
    _ref.getWarState().addBuiltUnit(tile.getCoordinate(), faction, type);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true, p.equals(_player) ? String
          .format("You built %s.", describe(type)) : String.format(
          "%s built %s for %s.", _player.getName(), describe(type), faction),
          null));
    }
    return toRet;
  }

  private static String describe(UnitType t) {
    return (t == UnitType.INFANTRY || t == UnitType.ARTILLERY ? "" : "a ")
        + (t == UnitType.INFANTRY ? "infantry" : t == UnitType.ARTILLERY ? "artillery"
            : t.toString());
  }
}
