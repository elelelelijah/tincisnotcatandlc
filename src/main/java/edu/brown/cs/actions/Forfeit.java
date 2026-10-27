package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.FollowUpActionIds;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Cursed Catan, Great War: a player may forfeit at the start of their turn
 * (before rolling). They are treated as eliminated and their turn ends.
 */
public class Forfeit implements Action {

  public static final String ID = "forfeit";

  private final Referee _ref;
  private final Player _player;

  public Forfeit(Referee ref, int playerID) {
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
      return fail("You can only forfeit during the Great War.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only forfeit on your own turn.");
    }
    FollowUpAction pending = _ref.getNextFollowUp(_player.getID());
    if (pending == null || !FollowUpActionIds.ROLL_DICE.equals(pending.getID())) {
      return fail("You can only forfeit at the start of your turn, before you roll.");
    }
    _ref.removeFollowUp(pending);
    _ref.eliminate(_player.getID());
    Map<Integer, ActionResponse> ended = new EndTurn(_ref, _player.getID())
        .execute();
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      ActionResponse base = ended.get(p.getID());
      String rest = base == null ? "" : " " + base.getMessage();
      String lead = p.equals(_player) ? "You forfeited and are out of the war."
          : String.format("%s forfeited and is out of the war.", _player
              .getName());
      toRet.put(p.getID(), new ActionResponse(true, lead + rest, null));
    }
    return toRet;
  }
}
