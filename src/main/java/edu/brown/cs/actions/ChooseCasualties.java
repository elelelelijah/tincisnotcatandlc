package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.UnitType;
import edu.brown.cs.catan.WarCombat;

/**
 * Cursed Catan, Great War: a player picks which of their ground units are
 * lost in a fight. The fight waits for the answer.
 *
 * <p>
 * Request JSON: <code>{losses:{infantry:1, tank:1}}</code> to choose, or
 * <code>{auto:true}</code> to let the game pick (the cheapest first). A wrong
 * answer fails and the choice stays pending.
 */
public class ChooseCasualties implements FollowUpAction {

  public static final String ID = "chooseCasualties";

  private final WarCombat.Fight _fight;
  private final WarCombat.Request _request;
  private Referee _ref;
  private Map<UnitType, Integer> _losses;
  private boolean _isSetup = false;

  public ChooseCasualties(WarCombat.Fight fight, WarCombat.Request request) {
    _fight = fight;
    _request = request;
  }

  /**
   * Makes a follow-up for every choice that does not have one yet and puts
   * them in the queue as one group.
   *
   * @return How many were made.
   */
  public static int register(Referee ref, WarCombat.Fight fight,
      List<WarCombat.Request> requests) {
    Collection<FollowUpAction> group = new ArrayList<>();
    for (WarCombat.Request r : requests) {
      if (!r.isRegistered()) {
        r.markRegistered();
        group.add(new ChooseCasualties(fight, r));
      }
    }
    if (!group.isEmpty()) {
      ref.addFollowUp(group);
    }
    return group.size();
  }

  /** What everybody is told while a fight waits. */
  static String waitingMessage(Referee ref, List<WarCombat.Request> pending,
      String log) {
    StringBuilder who = new StringBuilder();
    for (WarCombat.Request r : pending) {
      Player p = ref.getPlayerByID(r.getChooser());
      if (who.length() > 0) {
        who.append(" and ");
      }
      who.append(p.getName());
    }
    return String.format("%s The fight waits for %s to choose casualties.",
        log, who);
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException("Action must be setup.");
    }
    WarCombat.Outcome outcome;
    try {
      outcome = _fight.answer(_request, _losses);
    } catch (IllegalArgumentException e) {
      Map<Integer, ActionResponse> toRet = new HashMap<>();
      toRet.put(_request.getChooser(), new ActionResponse(false,
          e.getMessage(), null));
      return toRet;
    }
    _ref.removeFollowUp(this);
    String message;
    if (outcome.isFinished()) {
      message = outcome.getLog();
    } else {
      ChooseCasualties.register(_ref, _fight, outcome.getPending());
      message = waitingMessage(_ref, outcome.getPending(), outcome.getLog());
    }
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toRet.put(p.getID(), new ActionResponse(true, message, null));
    }
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject json = new JsonObject();
    json.addProperty("message", String.format(
        "Choose %d unit%s of %s to lose (%s).", _request.getRequired(),
        _request.getRequired() == 1 ? "" : "s", _request.getVictim(),
        _request.getReason()));
    json.addProperty("required", _request.getRequired());
    json.addProperty("victim", _request.getVictim().toString());
    json.addProperty("reason", _request.getReason());
    JsonObject options = new JsonObject();
    for (Map.Entry<UnitType, Integer> e : _request.getOptions().entrySet()) {
      options.addProperty(e.getKey().toString(), e.getValue());
    }
    json.add("options", options);
    HexCoordinate at = _fight.getTile().getCoordinate();
    JsonObject tile = new JsonObject();
    tile.addProperty("x", at.getX());
    tile.addProperty("y", at.getY());
    tile.addProperty("z", at.getZ());
    json.add("tile", tile);
    return json;
  }

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public int getPlayerID() {
    return _request.getChooser();
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    if (playerID != _request.getChooser()) {
      throw new IllegalArgumentException("Wrong player ID");
    }
    _ref = ref;
    JsonElement auto = json.get("auto");
    if (auto != null && auto.isJsonPrimitive() && auto.getAsBoolean()) {
      _losses = null;
    } else {
      _losses = new EnumMap<>(UnitType.class);
      try {
        for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("losses")
            .entrySet()) {
          _losses.put(UnitType.fromString(e.getKey()), e.getValue().getAsInt());
        }
      } catch (IllegalArgumentException e) {
        throw e;
      } catch (RuntimeException e) {
        throw new IllegalArgumentException(
            "Choose how many of each unit are lost, or let the game pick.");
      }
    }
    _isSetup = true;
  }

  @Override
  public String getVerb() {
    return "choose your casualties";
  }
}
