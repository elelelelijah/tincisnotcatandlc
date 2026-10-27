package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileStructure;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;
import edu.brown.cs.catan.Tech;

/**
 * Cursed Catan: builds a test site on a 2, a 12 or a desert next to one of the
 * player's buildings. Cost: 1 uranium, 2 concrete. Worth no victory points.
 * Each player may build 1. It may share a tile with a fortress.
 */
public class BuildTestSite implements Action {

  private final Player _player;
  private final Tile _tile;
  private final Referee _ref;
  public static final String ID = "buildTestSite";

  public BuildTestSite(Referee ref, int playerID, HexCoordinate tile) {
    assert ref != null && tile != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _tile = _ref.getBoard().getTile(tile);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    if (_tile == null) {
      throw new IllegalArgumentException("The tile could not be found.");
    }
  }

  private Map<Integer, ActionResponse> fail(String message) {
    return ImmutableMap.of(_player.getID(),
        new ActionResponse(false, message, null));
  }

  /**
   * Whether a test site may stand on this tile: a 2, a 12 or a desert.
   */
  static boolean isTestSiteTile(Tile tile) {
    return tile.getType() == TileType.DESERT || tile.getRollNumber() == 2
        || tile.getRollNumber() == 12;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCursedCatan) {
      return fail("Test sites only exist in Cursed Catan.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only build a test site on your own turn.");
    }
    if (_ref.isWarActive()
        && !_ref.hasTech(_player.getID(), Tech.UNLOCK_TEST_SITE)) {
      return fail("Your faction has not researched the Test Site yet.");
    }
    if (_tile.getType() == TileType.SEA || !isTestSiteTile(_tile)) {
      return fail("A test site can only go on a 2, a 12 or a desert.");
    }
    if (_player.numTestSites() >= Settings.MAX_TEST_SITES) {
      return fail(String.format(
          "You have already built the maximum of %d test site.",
          Settings.MAX_TEST_SITES));
    }
    for (Map.Entry<Resource, Double> price : Settings.TEST_SITE_COST
        .entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return fail("You cannot afford a test site (1 uranium and 2 concrete).");
      }
    }
    if (!_tile.isTouchedBy(_player.getID())) {
      return fail("A test site must be next to one of your buildings.");
    }

    _player.buildTestSite();
    _player.useTestSite();
    _tile.addStructure(TileStructure.Kind.TEST_SITE, _player.getID(),
        Settings.TEST_SITE_HIT_POINTS);

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), new ActionResponse(true,
            "You built a Test Site!", null));
      } else {
        toReturn.put(player.getID(), new ActionResponse(true,
            String.format("%s built a Test Site.", _player.getName()), null));
      }
    }
    return toReturn;
  }

}
