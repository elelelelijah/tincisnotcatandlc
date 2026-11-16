package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;

/**
 * Cursed Catan, Great War: players of the same faction can give each other
 * cards freely: resource cards and development cards (war cards too). On your
 * own turn, to a teammate who is still in the war.
 *
 * <p>
 * Request JSON:
 * <code>{to: playerId, resources:{wood:2}, devCards:{"Knight":1}}</code>
 * (development cards by name, as they appear in the hand).
 */
public class GiveCards implements Action {

  public static final String ID = "giveCards";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;

  public GiveCards(Referee ref, int playerID, JsonObject params) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    _params = params;
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    String problem = WarTurn.problem(_ref, _player, "give cards");
    if (problem != null) {
      return fail(problem);
    }
    Player to;
    try {
      to = _ref.getPlayerByID(_params.get("to").getAsInt());
    } catch (RuntimeException e) {
      return fail("Choose a teammate to give the cards to.");
    }
    if (to == null || to.equals(_player)
        || _ref.getFaction(to.getID()) != _ref.getFaction(_player.getID())) {
      return fail("You can only give cards to another player of your faction.");
    }
    if (_ref.isEliminated(to.getID())) {
      return fail("That player is out of the war.");
    }

    Map<Resource, Integer> resources = new LinkedHashMap<>();
    Map<DevelopmentCard, Integer> devCards = new LinkedHashMap<>();
    try {
      readResources(resources);
      readDevCards(devCards);
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    }
    int count = 0;
    for (int n : resources.values()) {
      count += n;
    }
    for (int n : devCards.values()) {
      count += n;
    }
    if (count == 0) {
      return fail("Choose at least one card to give.");
    }

    for (Map.Entry<Resource, Integer> e : resources.entrySet()) {
      _player.removeResource(e.getKey(), e.getValue());
      to.addResource(e.getKey(), e.getValue());
    }
    for (Map.Entry<DevelopmentCard, Integer> e : devCards.entrySet()) {
      for (int i = 0; i < e.getValue(); i++) {
        _player.giveAwayDevelopmentCard(e.getKey());
        to.addDevelopmentCard(e.getKey());
      }
    }

    StringBuilder what = new StringBuilder();
    for (Map.Entry<Resource, Integer> e : resources.entrySet()) {
      what.append(what.length() > 0 ? ", " : "").append(e.getValue()).append(' ')
          .append(e.getKey());
    }
    for (Map.Entry<DevelopmentCard, Integer> e : devCards.entrySet()) {
      what.append(what.length() > 0 ? ", " : "").append(e.getValue()).append(' ')
          .append(e.getKey());
    }
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      String message;
      if (p.equals(_player)) {
        message = String.format("You gave %s to %s.", what, to.getName());
      } else if (p.equals(to)) {
        message = String.format("%s gave you %s.", _player.getName(), what);
      } else {
        message = String.format("%s gave %d card%s to %s.", _player.getName(),
            count, count == 1 ? "" : "s", to.getName());
      }
      toRet.put(p.getID(), new ActionResponse(true, message, null));
    }
    return toRet;
  }

  private void readResources(Map<Resource, Integer> out) {
    JsonElement el = _params.get("resources");
    if (el == null || !el.isJsonObject()) {
      return;
    }
    for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
      int n = whole(e.getValue());
      if (n == 0) {
        continue;
      }
      Resource r;
      try {
        r = Resource.stringToResource(e.getKey());
      } catch (RuntimeException ex) {
        throw new IllegalArgumentException("Give real resources.");
      }
      if (r == Resource.WILDCARD) {
        throw new IllegalArgumentException("Give real resources.");
      }
      if (_player.getResources().get(r) < n) {
        throw new IllegalArgumentException(String.format(
            "You don't have %d %s.", n, r));
      }
      out.put(r, n);
    }
  }

  private void readDevCards(Map<DevelopmentCard, Integer> out) {
    JsonElement el = _params.get("devCards");
    if (el == null || !el.isJsonObject()) {
      return;
    }
    for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
      int n = whole(e.getValue());
      if (n == 0) {
        continue;
      }
      DevelopmentCard card = null;
      for (DevelopmentCard c : DevelopmentCard.values()) {
        if (c.toString().equals(e.getKey())) {
          card = c;
        }
      }
      if (card == null) {
        throw new IllegalArgumentException("Unknown card: " + e.getKey());
      }
      if (_player.getDevCards().get(card) < n) {
        throw new IllegalArgumentException(String.format(
            "You don't have %d %s.", n, card));
      }
      out.put(card, n);
    }
  }

  private static int whole(JsonElement v) {
    double d;
    try {
      d = v.getAsDouble();
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Give whole numbers of cards.");
    }
    if (d < 0 || d != Math.floor(d)) {
      throw new IllegalArgumentException("Give whole numbers of cards.");
    }
    return (int) d;
  }
}
