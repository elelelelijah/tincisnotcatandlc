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

/**
 * Cursed Catan: builds a fortress on a tile next to one of the player's
 * buildings. Cost: 2 brick, 1 concrete. Worth no victory points. Each player
 * may build 4, at most one per tile; several players may share a tile.
 */
public class BuildFortress implements Action {

  private final Player _player;
  private final Tile _tile;
  private final Referee _ref;
  public static final String ID = "buildFortress";

  public BuildFortress(Referee ref, int playerID, HexCoordinate tile) {
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

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCursedCatan) {
      return fail("Fortresses only exist in Cursed Catan.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only build a fortress on your own turn.");
    }
    if (_tile.getType() == TileType.SEA) {
      return fail("A fortress must be built on land.");
    }
    if (_player.numFortresses() >= Settings.MAX_FORTRESSES) {
      return fail(String.format(
          "You have already built the maximum of %d fortresses.",
          Settings.MAX_FORTRESSES));
    }
    if (_tile.hasFortress(_player.getID())) {
      return fail("You already have a fortress on that tile.");
    }
    for (Map.Entry<Resource, Double> price : Settings.FORTRESS_COST
        .entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return fail("You cannot afford a fortress (2 brick and 1 concrete).");
      }
    }
    if (!_tile.isTouchedBy(_player.getID())) {
      return fail("A fortress must be next to one of your buildings.");
    }

    _player.buildFortress();
    _player.useFortress();
    _tile.addStructure(TileStructure.Kind.FORTRESS, _player.getID(),
        Settings.FORTRESS_HIT_POINTS);

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), new ActionResponse(true,
            "You built a Fortress!", null));
      } else {
        toReturn.put(player.getID(), new ActionResponse(true,
            String.format("%s built a Fortress.", _player.getName()), null));
      }
    }
    return toReturn;
  }

}
