package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;
import edu.brown.cs.catan.Tech;

/**
 * Cursed Catan, Great War: buy an Atomic Bomb for 3 uranium and 3 ore. The
 * faction must have researched Atomic Bombs and the buyer needs a Test Site.
 * Bombs are kept off-hand until they are launched.
 */
public class BuyBomb implements Action {

  public static final String ID = "buyBomb";

  private final Referee _ref;
  private final Player _player;

  public BuyBomb(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCursedCatan || !_ref.isWarActive()) {
      return fail("Atomic Bombs can only be bought during the Great War.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only buy a bomb on your own turn.");
    }
    if (!_ref.hasTech(_player.getID(), Tech.UNLOCK_ATOMIC_BOMBS)) {
      return fail("Your faction has not researched Atomic Bombs yet.");
    }
    if (_player.numTestSites() < 1) {
      return fail("You need a Test Site to buy an Atomic Bomb.");
    }
    for (Map.Entry<Resource, Double> price : Settings.ATOMIC_BOMB_COST
        .entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return fail("An Atomic Bomb costs 3 uranium and 3 ore.");
      }
    }
    for (Map.Entry<Resource, Double> price : Settings.ATOMIC_BOMB_COST
        .entrySet()) {
      _player.removeResource(price.getKey(), price.getValue(), _ref.getBank());
    }
    _player.addBomb();

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true, p.equals(_player) ? String
          .format("You bought an Atomic Bomb. You now hold %d.", _player
              .numBombs()) : String.format("%s bought something.", _player
          .getName()), null));
    }
    return toRet;
  }
}
