package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.Board;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileStructure;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up to a Border Raid: the player picks an opponent's road or fortress
 * and it is destroyed. The piece goes back to its owner. An invalid choice
 * fails and leaves the follow-up pending.
 *
 * <p>
 * Request JSON: <code>{type:"road", start:{coord1..3}, end:{coord1..3}}</code>
 * or <code>{type:"fortress", tile:{x,y,z}, owner:id}</code>.
 */
public class BorderRaidTarget implements FollowUpAction {

  public static final String ID = "borderRaidTarget";
  private final int _playerID;
  private Referee _ref;
  private JsonObject _json;
  private boolean _isSetup = false;

  public BorderRaidTarget(int playerID) {
    _playerID = playerID;
  }

  /**
   * Whether the player has anything to raid: any road or fortress that belongs
   * to somebody else.
   */
  public static boolean hasTarget(Referee ref, int playerID) {
    Board board = ref.getBoard();
    for (Path path : board.getPaths().values()) {
      if (path.getRoad() != null
          && path.getRoad().getPlayer().getID() != playerID) {
        return true;
      }
    }
    for (Tile tile : board.getTiles()) {
      for (TileStructure s : tile.getStructures()) {
        if (s.getKind() == TileStructure.Kind.FORTRESS
            && s.getOwnerID() != playerID) {
          return true;
        }
      }
    }
    return false;
  }

  private Map<Integer, ActionResponse> failure(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_playerID, new ActionResponse(false, message, null));
    return toRet;
  }

  private Map<Integer, ActionResponse> success(String toPlayer, String toAll) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true,
          p.getID() == _playerID ? toPlayer : toAll, null));
    }
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before it is executed.");
    }
    String type = _json.has("type") ? _json.get("type").getAsString() : "";
    String raider = _ref.getPlayerByID(_playerID).getName();
    try {
      if ("road".equals(type)) {
        IntersectionCoordinate start = toIntersection(_json.get("start")
            .getAsJsonObject());
        IntersectionCoordinate end = toIntersection(_json.get("end")
            .getAsJsonObject());
        Path path = _ref.getBoard().getPath(start, end);
        if (path == null || path.getRoad() == null) {
          return failure("There is no road there.");
        }
        Player owner = path.getRoad().getPlayer();
        if (owner.getID() == _playerID) {
          return failure("You can't raid your own road.");
        }
        _ref.getBoard().removeRoad(start, end);
        owner.returnRoad();
        _ref.removeFollowUp(this);
        return success(String.format("You destroyed one of %s's roads.",
            owner.getName()), String.format(
            "%s destroyed one of %s's roads with a Border Raid.", raider,
            owner.getName()));
      } else if ("fortress".equals(type)) {
        HexCoordinate coord = toHex(_json.get("tile").getAsJsonObject());
        int ownerID = _json.get("owner").getAsInt();
        Tile tile = _ref.getBoard().getTile(coord);
        Player owner = _ref.getPlayerByID(ownerID);
        if (tile == null || owner == null) {
          return failure("There is no fortress there.");
        }
        if (ownerID == _playerID) {
          return failure("You can't raid your own fortress.");
        }
        if (!tile.removeStructure(TileStructure.Kind.FORTRESS, ownerID)) {
          return failure("There is no fortress there.");
        }
        owner.returnFortress();
        _ref.removeFollowUp(this);
        return success(String.format("You destroyed one of %s's fortresses.",
            owner.getName()), String.format(
            "%s destroyed one of %s's fortresses with a Border Raid.", raider,
            owner.getName()));
      }
    } catch (RuntimeException e) {
      return failure("Choose a road or fortress that belongs to an opponent.");
    }
    return failure("Choose a road or fortress that belongs to an opponent.");
  }

  @Override
  public JsonObject getData() {
    JsonObject json = new JsonObject();
    json.addProperty("message",
        "Border Raid: choose an opponent's road or fortress to destroy.");
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
    if (_playerID != playerID) {
      throw new IllegalArgumentException("Wrong player ID");
    }
    _ref = ref;
    _json = json;
    _isSetup = true;
  }

  @Override
  public String getVerb() {
    return "raid an opponent's border";
  }

  private HexCoordinate toHex(JsonObject o) {
    return new HexCoordinate(o.get("x").getAsInt(), o.get("y").getAsInt(), o
        .get("z").getAsInt());
  }

  private IntersectionCoordinate toIntersection(JsonObject o) {
    JsonElement c1 = o.get("coord1");
    JsonElement c2 = o.get("coord2");
    JsonElement c3 = o.get("coord3");
    return new IntersectionCoordinate(toHex(c1.getAsJsonObject()),
        toHex(c2.getAsJsonObject()), toHex(c3.getAsJsonObject()));
  }
}
