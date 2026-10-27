package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;
import edu.brown.cs.catan.Tech;
import edu.brown.cs.catan.WarState;

/**
 * Cursed Catan, Great War: research a node of the faction's technology tree.
 * A Research Point costs 3 resources of the player's choice; a faction that
 * already has a banked Research Point spends that one instead. The node's
 * benefit applies to the whole faction at once.
 *
 * <p>
 * Request JSON: <code>{node:"unlockTanks", pay:{wood:1, ore:2}}</code>. The
 * <code>pay</code> field is not needed when the faction has a banked point.
 */
public class Research implements Action {

  public static final String ID = "research";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;

  public Research(Referee ref, int playerID, JsonObject params) {
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
    if (!_ref.getGameSettings().isCursedCatan || !_ref.isWarActive()) {
      return fail("You can only research during the Great War.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only research on your own turn.");
    }
    Faction faction = _ref.getFaction(_player.getID());
    WarState war = _ref.getWarState();
    Tech tech;
    try {
      tech = Tech.fromId(_params.get("node").getAsString());
    } catch (RuntimeException e) {
      return fail("Choose a technology to research.");
    }
    if (war.hasTech(faction, tech)) {
      return fail(String.format("%s already has %s.", faction, tech));
    }
    if (!war.canResearch(faction, tech)) {
      return fail(String.format("%s must be researched first.", tech
          .getPrerequisite()));
    }

    boolean useBanked = war.getBankedPoints(faction) > 0;
    Map<Resource, Double> payment = new LinkedHashMap<>();
    if (!useBanked) {
      String problem = readPayment(payment);
      if (problem != null) {
        return fail(problem);
      }
    }

    if (useBanked) {
      war.spendResearchPoint(faction);
    } else {
      for (Map.Entry<Resource, Double> e : payment.entrySet()) {
        _player.removeResource(e.getKey(), e.getValue(), _ref.getBank());
      }
    }
    war.unlock(faction, tech);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      boolean mine = _ref.getFaction(p.getID()) == faction;
      String message;
      if (p.equals(_player)) {
        message = String.format("You researched %s for %s.", tech, faction);
      } else if (mine) {
        message = String.format("%s researched %s for your faction.",
            _player.getName(), tech);
      } else {
        message = String.format("%s researched %s.", faction, tech);
      }
      toRet.put(p.getID(), new ActionResponse(true, message, null));
    }
    return toRet;
  }

  /**
   * Reads and checks the 3 resources the player pays with.
   *
   * @return A message if the payment is not valid, or null.
   */
  private String readPayment(Map<Resource, Double> payment) {
    JsonElement pay = _params.get("pay");
    if (pay == null || !pay.isJsonObject()) {
      return String.format("Pay %d resources of your choice.",
          Settings.RESEARCH_POINT_COST);
    }
    double total = 0;
    try {
      for (Map.Entry<String, JsonElement> e : pay.getAsJsonObject().entrySet()) {
        double n = e.getValue().getAsDouble();
        if (n < 0 || n != Math.floor(n)) {
          return "Pay whole numbers of resources.";
        }
        if (n == 0) {
          continue;
        }
        Resource r = Resource.stringToResource(e.getKey());
        if (r == Resource.WILDCARD) {
          return "Pay with real resources.";
        }
        payment.put(r, n);
        total += n;
      }
    } catch (RuntimeException e) {
      return "Pay with real resources.";
    }
    if (total != Settings.RESEARCH_POINT_COST) {
      return String.format("A Research Point costs exactly %d resources.",
          Settings.RESEARCH_POINT_COST);
    }
    for (Map.Entry<Resource, Double> e : payment.entrySet()) {
      if (!_player.hasResource(e.getKey(), e.getValue())) {
        return String.format("You don't have enough %s.", e.getKey());
      }
    }
    return null;
  }
}
