package edu.brown.cs.actions;

import java.util.EnumMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Building;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.UnitType;

/**
 * Cursed Catan, Great War: every player places their starting forces on tiles
 * next to their own buildings (ruined ones count). They must place exactly
 * the units they were given.
 *
 * <p>
 * Request JSON:
 * <code>{placements:[{tile:{x,y,z}, unit:"infantry", count:2}, ...]}</code>
 */
public class DeployUnits implements FollowUpAction {

  public static final String ID = "deployUnits";

  private Referee _ref;
  private final int _playerID;
  private final Map<UnitType, Integer> _owed;
  private final List<HexCoordinate> _validTiles;
  private Map<HexCoordinate, Map<UnitType, Integer>> _placements;
  private boolean _isSetup = false;

  /**
   * Constructor for the class.
   *
   * @param playerID
   *          The player who deploys.
   * @param owed
   *          How many units of each kind they place (for the client).
   * @param validTiles
   *          The tiles they may place units on (for the client).
   */
  public DeployUnits(int playerID, Map<UnitType, Integer> owed,
      List<HexCoordinate> validTiles) {
    _playerID = playerID;
    _owed = new EnumMap<>(UnitType.class);
    _owed.putAll(owed);
    _validTiles = new ArrayList<>(validTiles);
  }

  /**
   * Whether the player has a building (ruined or not) on the tile's corners.
   */
  public static boolean touchesBuildingOf(Tile tile, Player player) {
    for (Intersection i : tile.getIntersections()) {
      Building b = i.getBuilding();
      if (b != null && b.getPlayer().equals(player)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException("Action must be setup.");
    }
    Player player = _ref.getPlayerByID(_playerID);
    Faction faction = _ref.getFaction(_playerID);
    for (Map.Entry<HexCoordinate, Map<UnitType, Integer>> e : _placements
        .entrySet()) {
      for (Map.Entry<UnitType, Integer> u : e.getValue().entrySet()) {
        _ref.getWarState().addUnits(e.getKey(), faction, u.getKey(),
            u.getValue());
      }
    }
    _ref.markDeployed(_playerID);
    _ref.removeFollowUp(this);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.getID() == _playerID) {
        toRet.put(p.getID(), new ActionResponse(true,
            "You deployed your forces.", null));
      } else {
        toRet.put(p.getID(), new ActionResponse(true, String.format(
            "%s deployed their forces for %s.", player.getName(), faction),
            null));
      }
    }
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject json = new JsonObject();
    json.addProperty("message", "The Great War has begun! Deploy your forces.");
    JsonObject owed = new JsonObject();
    for (Map.Entry<UnitType, Integer> e : _owed.entrySet()) {
      owed.addProperty(e.getKey().toString(), e.getValue());
    }
    json.add("units", owed);
    JsonArray tiles = new JsonArray();
    for (HexCoordinate c : _validTiles) {
      JsonObject t = new JsonObject();
      t.addProperty("x", c.getX());
      t.addProperty("y", c.getY());
      t.addProperty("z", c.getZ());
      tiles.add(t);
    }
    json.add("tiles", tiles);
    return json;
  }

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public int getPlayerID() {
    return _playerID;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    _ref = ref;
    if (_playerID != playerID) {
      throw new IllegalArgumentException("Wrong player ID");
    }
    Player player = ref.getPlayerByID(playerID);
    Map<UnitType, Integer> owed = ref.getUnitsToDeploy(playerID);
    Map<UnitType, Integer> placed = new EnumMap<>(UnitType.class);
    Map<HexCoordinate, Map<UnitType, Integer>> placements = new LinkedHashMap<>();
    try {
      JsonArray list = json.get("placements").getAsJsonArray();
      for (JsonElement el : list) {
        JsonObject o = el.getAsJsonObject();
        JsonObject t = o.get("tile").getAsJsonObject();
        HexCoordinate coord = new HexCoordinate(t.get("x").getAsInt(), t.get(
            "y").getAsInt(), t.get("z").getAsInt());
        UnitType type = UnitType.fromString(o.get("unit").getAsString());
        int count = o.get("count").getAsInt();
        if (count < 0) {
          throw new IllegalArgumentException("Counts cannot be negative.");
        }
        if (count == 0) {
          continue;
        }
        Tile tile = ref.getBoard().getTile(coord);
        if (tile == null || tile.getType() == TileType.SEA) {
          throw new IllegalArgumentException("Units go on land tiles.");
        }
        if (!touchesBuildingOf(tile, player)) {
          throw new IllegalArgumentException(
              "Units must be placed on tiles next to your own buildings.");
        }
        Map<UnitType, Integer> onTile = placements.get(coord);
        if (onTile == null) {
          onTile = new EnumMap<>(UnitType.class);
          placements.put(coord, onTile);
        }
        Integer already = onTile.get(type);
        onTile.put(type, already == null ? count : already + count);
        Integer total = placed.get(type);
        placed.put(type, total == null ? count : total + count);
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid deployment");
    }
    for (UnitType type : UnitType.values()) {
      int want = owed.containsKey(type) ? owed.get(type) : 0;
      int have = placed.containsKey(type) ? placed.get(type) : 0;
      if (want != have) {
        throw new IllegalArgumentException(String.format(
            "You must place exactly %d %s (you placed %d).", want, type, have));
      }
    }
    _placements = placements;
    _isSetup = true;
  }

  @Override
  public String getVerb() {
    return "deploy your forces";
  }
}
