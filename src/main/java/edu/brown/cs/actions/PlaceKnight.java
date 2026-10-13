package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action for placing a new basic knight on the board. The knight must be placed
 * at an unoccupied intersection connected to one of the player's roads.
 * Cost: 1 Sheep, 1 Ore.
 */
public class PlaceKnight implements Action {

  private final Player _player;
  private final Intersection _intersection;
  private final Referee _ref;
  public static final String ID = "placeKnight";

  public PlaceKnight(Referee ref, int playerID, IntersectionCoordinate loc) {
    assert ref != null && loc != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _intersection = _ref.getBoard().getIntersections().get(loc);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    if (_intersection == null) {
      throw new IllegalArgumentException(
          "The intersection could not be found.");
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    // Validation
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You cannot place a knight when it is not your turn.", null));
    }

    // Check cost
    for (Map.Entry<Resource, Double> price : Settings.KNIGHT_COST.entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false,
                "You cannot afford to recruit a knight.", null));
      }
    }

    // Must be empty intersection
    if (_intersection.getBuilding() != null) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You can only place a knight on an empty intersection.", null));
    }

    // Check no existing knight at this intersection
    for (KnightPiece k : _player.getKnights()) {
      if (k.getPosition().equals(_intersection.getPosition())) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false,
                "There is already a knight at this intersection.", null));
      }
    }

    // Must be connected to player's road
    boolean connected = false;
    for (Path p : _intersection.getPaths()) {
      if (p.getRoad() != null
          && p.getRoad().getPlayer().getID() == _player.getID()) {
        connected = true;
        break;
      }
    }
    if (!connected) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You must place a knight on an intersection connected to your road.",
              null));
    }

    // Pay
    for (Map.Entry<Resource, Double> price : Settings.KNIGHT_COST.entrySet()) {
      _player.removeResource(price.getKey(), price.getValue());
    }

    // Place knight
    KnightPiece knight = new KnightPiece(_player.getID(),
        _intersection.getPosition());
     knight.restrictForTurn(_ref.getTurn().getTurnNum());
    _player.addKnight(knight);

    // Response
    ActionResponse respToPlayer = new ActionResponse(true,
        "You recruited a Basic Knight!", null);
    String message = String.format("%s recruited a knight.",
        _player.getName());
    ActionResponse respToAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), respToPlayer);
      } else {
        toReturn.put(player.getID(), respToAll);
      }
    }
    return toReturn;
  }

}
