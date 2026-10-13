package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up for the owner of a knight that has just been displaced. They
 * choose an empty, road-connected intersection to move it to (its active
 * status is unchanged). Once they have, the displacing knight completes its
 * move onto the vacated intersection and becomes inactive.
 */
public class RelocateKnight implements FollowUpAction {

  public static final String ID = "relocateKnight";

  private Referee _ref;
  private final int _playerID; // owner of the displaced knight
  private final int _moverID; // owner of the displacing knight
  private final IntersectionCoordinate _moverFrom;
  private final IntersectionCoordinate _displacedAt;
  private final List<IntersectionCoordinate> _options;
  private IntersectionCoordinate _chosen;
  private boolean _isSetup;

  public RelocateKnight(int playerID, int moverID,
      IntersectionCoordinate moverFrom, IntersectionCoordinate displacedAt,
      List<IntersectionCoordinate> options) {
    _playerID = playerID;
    _moverID = moverID;
    _moverFrom = moverFrom;
    _displacedAt = displacedAt;
    _options = options;
    _isSetup = false;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException("Action must be setup.");
    }
    if (!_options.contains(_chosen)) {
      return respond(false,
          "Choose one of the highlighted intersections for your knight.");
    }

    Player defender = _ref.getPlayerByID(_playerID);
    Player mover = _ref.getPlayerByID(_moverID);
    KnightPiece displaced = KnightPaths.knightOf(defender, _displacedAt);
    KnightPiece moving = KnightPaths.knightOf(mover, _moverFrom);
    if (displaced == null) {
      // The knight is already gone; nothing left to resolve.
      _ref.removeFollowUp(this);
      return respond(true, "The displaced knight is no longer on the board.");
    }

    // The chosen square must still be free (the mover's own square is about to
    // be vacated, so it is allowed).
    Intersection dest = _ref.getBoard().getIntersections().get(_chosen);
    boolean free = dest != null && dest.getBuilding() == null
        && (KnightPaths.knightAt(_ref, _chosen) == null
            || _chosen.equals(_moverFrom));
    if (!free) {
      return respond(false, "That intersection is no longer available.");
    }

    // Relocate the displaced knight (status unchanged), then complete the
    // displacement: the mover takes the vacated square and becomes inactive.
    displaced.relocateTo(_chosen);
    if (moving != null) {
      moving.moveTo(_displacedAt);
    }

    _ref.removeFollowUp(this);
    return respond(true, String.format(
        "%s relocated their displaced knight.", defender.getName()));
  }

  private Map<Integer, ActionResponse> respond(boolean success,
      String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (success || p.getID() == _playerID) {
        toRet.put(p.getID(), new ActionResponse(success, message, null));
      }
    }
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject json = new JsonObject();
    json.addProperty("message",
        "Your knight was displaced! Choose an empty intersection connected "
            + "to it by your roads.");
    Gson gson = new Gson();
    JsonArray options = new JsonArray();
    for (IntersectionCoordinate c : _options) {
      options.add(gson.toJsonTree(c));
    }
    json.add("options", options);
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
    try {
      _chosen = toIntersectionCoordinate(
          json.get("coordinate").getAsJsonObject());
      _isSetup = true;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid coordinate");
    }
  }

  @Override
  public String getVerb() {
    return "relocate a displaced knight";
  }

  private IntersectionCoordinate toIntersectionCoordinate(JsonObject object) {
    JsonObject coord1 = object.get("coord1").getAsJsonObject();
    JsonObject coord2 = object.get("coord2").getAsJsonObject();
    JsonObject coord3 = object.get("coord3").getAsJsonObject();
    HexCoordinate h1 = new HexCoordinate(coord1.get("x").getAsInt(),
        coord1.get("y").getAsInt(), coord1.get("z").getAsInt());
    HexCoordinate h2 = new HexCoordinate(coord2.get("x").getAsInt(),
        coord2.get("y").getAsInt(), coord2.get("z").getAsInt());
    HexCoordinate h3 = new HexCoordinate(coord3.get("x").getAsInt(),
        coord3.get("y").getAsInt(), coord3.get("z").getAsInt());
    return new IntersectionCoordinate(h1, h2, h3);
  }
}
