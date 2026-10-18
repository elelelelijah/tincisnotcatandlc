package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Cursed Catan: follow-up for a player whose buildings touch a gold tile that
 * was just rolled. They take that many resources of their choice from the
 * bank (any mix, except uranium, which is not available until the war).
 */
public class GoldResource implements FollowUpAction {

  public static final String ID = "goldResource";

  private Referee _ref;
  private final int _playerID;
  private final int _count;
  private Map<Resource, Integer> _choice;
  private boolean _isSetup;

  public GoldResource(int playerID, int count) {
    _playerID = playerID;
    _count = count;
    _isSetup = false;
  }

  /**
   * Whether gold may be taken as this resource.
   */
  public static boolean isChoosable(Resource r) {
    return r != Resource.WILDCARD && r != Resource.URANIUM;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException("Action must be setup.");
    }
    Player player = _ref.getPlayerByID(_playerID);
    StringBuilder taken = new StringBuilder();
    for (Map.Entry<Resource, Integer> e : _choice.entrySet()) {
      if (e.getValue() > 0) {
        player.addResource(e.getKey(), e.getValue(), _ref.getBank());
        if (taken.length() > 0) {
          taken.append(", ");
        }
        taken.append(e.getValue()).append(" ").append(e.getKey());
      }
    }
    _ref.removeFollowUp(this);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.getID() == _playerID) {
        toRet.put(p.getID(), new ActionResponse(true,
            String.format("Gold: you received %s.", taken), null));
      } else {
        toRet.put(p.getID(), new ActionResponse(true,
            String.format("%s took resources from a gold tile.",
                player.getName()),
            null));
      }
    }
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject json = new JsonObject();
    json.addProperty("message", String.format(
        "Gold! Choose %d resource%s.", _count, _count == 1 ? "" : "s"));
    json.addProperty("count", _count);
    return json;
  }

  @Override
  public String getID() {
    return ID;
  }

  @Override
  public int getPlayerID() {
    return _playerID;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    _ref = ref;
    if (_playerID != playerID) {
      throw new IllegalArgumentException("Wrong player ID");
    }
    Map<Resource, Integer> choice = new LinkedHashMap<>();
    int total = 0;
    try {
      JsonObject chosen = json.get("resources").getAsJsonObject();
      for (Map.Entry<String, JsonElement> e : chosen.entrySet()) {
        Resource r = Resource.stringToResource(e.getKey());
        int amount = e.getValue().getAsInt();
        if (!isChoosable(r)) {
          throw new IllegalArgumentException(
              String.format("You cannot take %s from gold.", r));
        }
        if (amount < 0) {
          throw new IllegalArgumentException("Amounts cannot be negative.");
        }
        choice.put(r, amount);
        total += amount;
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid resource selection");
    }
    if (total != _count) {
      throw new IllegalArgumentException(
          String.format("You must choose exactly %d resource%s.", _count,
              _count == 1 ? "" : "s"));
    }
    _choice = choice;
    _isSetup = true;
  }

  @Override
  public String getVerb() {
    return "choose gold resources";
  }
}
