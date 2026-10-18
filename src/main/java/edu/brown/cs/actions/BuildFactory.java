package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Cursed Catan: upgrades one of the player's settlements to a factory.
 * Cost: 1 coal, 1 ore, 2 concrete. Worth 1 VP. Each player may build 2.
 */
public class BuildFactory implements Action {

  private final Player _player;
  private final Intersection _intersection;
  private final Referee _ref;
  public static final String ID = "buildFactory";

  public BuildFactory(Referee ref, int playerID, IntersectionCoordinate loc) {
    assert ref != null && loc != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _intersection = _ref.getBoard().getIntersections().get(loc);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    if (_intersection == null) {
      throw new IllegalArgumentException("The intersection could not be found.");
    }
  }

  private Map<Integer, ActionResponse> fail(String message) {
    return ImmutableMap.of(_player.getID(),
        new ActionResponse(false, message, null));
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCursedCatan) {
      return fail("Factories only exist in Cursed Catan.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only build a factory on your own turn.");
    }
    if (_player.numFactories() >= Settings.MAX_FACTORIES) {
      return fail(String.format(
          "You have already built the maximum of %d factories.",
          Settings.MAX_FACTORIES));
    }
    for (Map.Entry<Resource, Double> price : Settings.FACTORY_COST.entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return fail(
            "You cannot afford a factory (1 coal, 1 ore and 2 concrete).");
      }
    }
    if (!_intersection.canPlaceFactory(_player)) {
      return fail("A factory can only replace one of your own settlements.");
    }

    _player.buildFactory();
    _player.useFactory();
    _intersection.placeFactory(_player);

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), new ActionResponse(true,
            "Congratulations! You built a Factory!", null));
      } else {
        toReturn.put(player.getID(), new ActionResponse(true,
            String.format("%s built a Factory.", _player.getName()), null));
      }
    }
    return toReturn;
  }

}
