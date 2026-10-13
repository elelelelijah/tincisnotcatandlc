package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action for activating an inactive knight. Cost: 1 Wheat.
 */
public class ActivateKnight implements Action {

  private final Player _player;
  private final Referee _ref;
  private final IntersectionCoordinate _knightLocation;
  public static final String ID = "activateKnight";

  public ActivateKnight(Referee ref, int playerID,
      IntersectionCoordinate knightLocation) {
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _knightLocation = knightLocation;
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You cannot activate a knight when it is not your turn.", null));
    }

    // Check cost
    for (Map.Entry<Resource, Double> price : Settings.KNIGHT_ACTIVATE_COST
        .entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false,
                "You cannot afford to activate a knight (requires 1 Wheat).",
                null));
      }
    }

    // Find the knight at the given location
    KnightPiece target = null;
    for (KnightPiece k : _player.getKnights()) {
      if (k.getPosition().equals(_knightLocation)) {
        target = k;
        break;
      }
    }

    if (target == null) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "No knight found at that location.", null));
    }

    if (target.isActive()) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "This knight is already active.", null));
    }

    // Pay
    for (Map.Entry<Resource, Double> price : Settings.KNIGHT_ACTIVATE_COST
        .entrySet()) {
      _player.removeResource(price.getKey(), price.getValue());
    }

    // Activate
    target.activate();
    target.restrictForTurn(_ref.getTurn().getTurnNum());

    // Response
    ActionResponse respToPlayer = new ActionResponse(true,
        "Your knight has been activated!", null);
    String message = String.format("%s activated a knight.",
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
