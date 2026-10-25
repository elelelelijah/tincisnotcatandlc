package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Plays the Cursed Catan Border Raid card: the player then destroys one road
 * or fortress belonging to an opponent (see {@link BorderRaidTarget}).
 */
public class PlayBorderRaid implements Action {

  public static final String ID = "playBorderRaid";
  private final Player _player;
  private final Referee _ref;

  public PlayBorderRaid(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
  }

  private ActionResponse failure(String message) {
    return new ActionResponse(false, message, null);
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    int id = _player.getID();
    if (!_ref.getGameSettings().isCursedCatan) {
      return ImmutableMap.of(id,
          failure("Border Raids only exist in Cursed Catan"));
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || _ref.currentPlayer() == null
        || _ref.currentPlayer().getID() != id) {
      return ImmutableMap.of(id, failure("You can only play it on your turn"));
    }
    Integer held = _player.getDevCards().get(DevelopmentCard.BORDER_RAID);
    if (held == null || held < 1) {
      return ImmutableMap.of(id, failure("You don't have a Border Raid"));
    }
    if (_ref.getTurn().devHasBeenPlayed()) {
      return ImmutableMap.of(id,
          failure("You already played a development card this turn"));
    }
    if (!_ref.getTurn().hadInitialDevCard(DevelopmentCard.BORDER_RAID)) {
      return ImmutableMap.of(id, failure(
          "You cannot play a development card on the turn you bought it"));
    }
    if (!BorderRaidTarget.hasTarget(_ref, id)) {
      return ImmutableMap.of(id,
          failure("No opponent has a road or fortress to raid"));
    }
    _player.playDevelopmentCard(DevelopmentCard.BORDER_RAID);
    _ref.playDevCard();

    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    ActionResponse toPlayer = new ActionResponse(true,
        "You played a Border Raid. Choose an opponent's road or fortress to destroy.",
        null);
    ActionResponse toAll = new ActionResponse(true, String.format(
        "%s played a Border Raid.", _player.getName()), null);
    for (Player p : _ref.getPlayers()) {
      toReturn.put(p.getID(), p.equals(_player) ? toPlayer : toAll);
    }
    _ref.addFollowUp(ImmutableList.of(new BorderRaidTarget(id)));
    return toReturn;
  }

}
