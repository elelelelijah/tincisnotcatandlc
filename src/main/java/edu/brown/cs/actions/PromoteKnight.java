package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.KnightPiece.KnightLevel;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action for promoting a knight to the next level. Promotion requirements:
 * - Basic → Strong: 1 Sheep + 1 Ore. No Politics track requirement.
 * - Strong → Mighty: 2 Sheep + 2 Ore. Requires Politics track level ≥ 3.
 */
public class PromoteKnight implements Action {

  private final Player _player;
  private final Referee _ref;
  private final IntersectionCoordinate _knightLocation;
  public static final String ID = "promoteKnight";

  public PromoteKnight(Referee ref, int playerID,
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
              "You cannot promote a knight when it is not your turn.", null));
    }

    // Find the knight
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

    if (target.getLevel() == KnightLevel.MIGHTY) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "This knight is already at the maximum level.", null));
    }

    // Determine cost and requirements
    Map<Resource, Double> baseCost;
    CityImprovement improvement = _player.getCityImprovement();
    boolean politicsLevel3 = improvement != null
        && improvement.getLevel(CityImprovement.Track.POLITICS) >= 3;

    if (target.getLevel() == KnightLevel.BASIC) {
      baseCost = Settings.KNIGHT_PROMOTE_STRONG_COST;
    } else {
      // Strong → Mighty requires Politics track level ≥ 3
      baseCost = Settings.KNIGHT_PROMOTE_MIGHTY_COST;
      if (!politicsLevel3) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false,
                "Promoting to Mighty requires Politics track level 3 or higher.",
                null));
      }
    }

    // Politics level 3: waive sheep/wool cost for promotion
    Map<Resource, Double> cost = new HashMap<>(baseCost);
    if (politicsLevel3) {
      cost.remove(Resource.SHEEP);
    }

    // Check cost
    for (Map.Entry<Resource, Double> price : cost.entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false,
                "You cannot afford to promote this knight.", null));
      }
    }

    // Pay
    for (Map.Entry<Resource, Double> price : cost.entrySet()) {
      _player.removeResource(price.getKey(), price.getValue());
    }

    // Promote
    KnightLevel oldLevel = target.getLevel();
    target.promote();
     target.restrictForTurn(_ref.getTurn().getTurnNum());

    // Response
    ActionResponse respToPlayer = new ActionResponse(true,
        String.format("Your %s knight has been promoted to %s!",
            oldLevel, target.getLevel()),
        null);
    String message = String.format("%s promoted a knight to %s.",
        _player.getName(), target.getLevel());
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
