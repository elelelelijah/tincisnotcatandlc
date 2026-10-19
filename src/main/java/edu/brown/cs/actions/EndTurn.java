package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Action responsible for ending a turn.
 *
 * @author anselvahle
 *
 */
public class EndTurn implements Action {
  public static final String ID = "endTurn";
  private Player _player;
  private Referee _ref;
  private boolean _hasKnight = false;

  public EndTurn(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }

  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "It is not your turn.", null));
    }
     // Knights recruited, promoted or activated this turn may act from the
    // owner's next turn onwards.
    for (KnightPiece k : _player.getKnights()) {
      k.clearRestriction();
    }
    // Cursed Catan: if any coal was spent this turn, the Industry track moves
    String industryNote = "";
    if (_ref.advanceIndustryTrack()) {
      industryNote = String.format(" The Industry track advanced to %d.",
          _ref.getIndustryTrack());
      if (_ref.isGreatWarTriggered()) {
        industryNote += " The Industry track has reached its end: "
            + "the Great War is coming!";
      }
    }
    _ref.startNextTurn();
    Player nextPlayer = _ref.currentPlayer();
    if (nextPlayer.getDevCards().get(DevelopmentCard.KNIGHT) != 0) {
      _hasKnight = true;
    }
    Map<Integer, ActionResponse> toRet = new HashMap<Integer, ActionResponse>();

    for (Player p : _ref.getPlayers()) {
      if (!p.equals(nextPlayer) && !p.equals(_player)) {
        String message = String.format(
            "%s just ended their turn. It is now %s's turn.",
            _player.getName(), nextPlayer.getName());
        ActionResponse toAdd = new ActionResponse(true, message, null);
        toRet.put(p.getID(), toAdd);
      } else if (!p.equals(nextPlayer) && p.equals(_player)) {
        String message = String.format(
            "You just ended your turn. It is now %s's turn.",
            nextPlayer.getName());
        ActionResponse toAdd = new ActionResponse(true, message, null);
        toRet.put(p.getID(), toAdd);
      } else {
        FollowUpAction toDoNext = null;
        if (!_hasKnight) {
          toDoNext = new RollDice(nextPlayer.getID());
        } else {
          toDoNext = new KnightOrDice(nextPlayer.getID());
        }
        Collection<FollowUpAction> followUp = new ArrayList<FollowUpAction>();
        followUp.add(toDoNext);
        _ref.addFollowUp(followUp);
        String message = String
            .format("%s just ended their turn. It is now your turn.",
                _player.getName());
        ActionResponse toAdd = new ActionResponse(true, message, null);
        toRet.put(p.getID(), toAdd);
      }
    }
    if (!industryNote.isEmpty()) {
      for (Player p : _ref.getPlayers()) {
        ActionResponse old = toRet.get(p.getID());
        toRet.put(p.getID(), new ActionResponse(true, old.getMessage()
            + industryNote, old.getData()));
      }
    }
    return toRet;
  }
}
