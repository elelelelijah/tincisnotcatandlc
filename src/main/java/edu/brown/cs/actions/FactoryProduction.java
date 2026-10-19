package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonPrimitive;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Cursed Catan: follow-up for a player whose factories sit next to a tile that
 * was just rolled. Each factory produces one resource of its tile. For each
 * one the owner may instead spend 1 coal to take any 1 resource. Coal received
 * on the same roll cannot be spent.
 */
public class FactoryProduction implements FollowUpAction {

  public static final String ID = "factoryProduction";
  public static final String KEEP = "keep";

  private Referee _ref;
  private final int _playerID;
  private final List<Resource> _units;
  private final int _maxCoal;
  // For each unit, the resource taken for coal, or null to keep the tile's
  private List<Resource> _choices;
  private boolean _isSetup;

  /**
   * Constructor for the class.
   *
   * @param playerID
   *          The owner of the factories.
   * @param units
   *          The resource each factory produced, one entry per factory.
   * @param maxCoal
   *          How much coal they may spend: what they held before the roll.
   */
  public FactoryProduction(int playerID, List<Resource> units, int maxCoal) {
    _playerID = playerID;
    _units = new ArrayList<>(units);
    _maxCoal = maxCoal;
    _isSetup = false;
  }

  /**
   * Whether coal may buy this resource. Coal itself is not allowed: it would
   * cost nothing and only push the Industry track along.
   */
  public static boolean isChoosable(Resource r) {
    return GoldResource.isChoosable(r) && r != Resource.COAL;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException("Action must be setup.");
    }
    Player player = _ref.getPlayerByID(_playerID);
    int spent = 0;
    for (Resource c : _choices) {
      if (c != null) {
        spent++;
      }
    }
    if (spent > 0) {
      player.removeResource(Resource.COAL, spent);
      _ref.recordCoalSpent();
    }
    StringBuilder got = new StringBuilder();
    for (int i = 0; i < _units.size(); i++) {
      Resource r = _choices.get(i) != null ? _choices.get(i) : _units.get(i);
      player.addResource(r, 1, _ref.getBank());
      if (got.length() > 0) {
        got.append(", ");
      }
      got.append("1 ").append(r);
    }
    _ref.removeFollowUp(this);

    String forYou = String.format("Factory: you received %s%s.", got,
        spent > 0 ? String.format(" (spent %d coal)", spent) : "");
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.getID() == _playerID) {
        toRet.put(p.getID(), new ActionResponse(true, forYou, null));
      } else {
        toRet.put(p.getID(), new ActionResponse(true, String.format(
            "%s collected from their factories%s.", player.getName(),
            spent > 0 ? " using coal" : ""), null));
      }
    }
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject json = new JsonObject();
    json.addProperty("message", "Your factories produced! Take the "
        + "resources, or spend coal to take any resource instead.");
    json.addProperty("maxCoal", _maxCoal);
    JsonArray units = new JsonArray();
    for (Resource r : _units) {
     units.add(new JsonPrimitive(r.toString()));// units.add(r.toString());
    }
    json.add("units", units);
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
    List<Resource> choices = new ArrayList<>();
    try {
      JsonArray given = json.get("choices").getAsJsonArray();
      if (given.size() != _units.size()) {
        throw new IllegalArgumentException(String.format(
            "Choose what to do with each of your %d factories.",
            _units.size()));
      }
      for (JsonElement e : given) {
        String text = e.getAsString();
        if (KEEP.equals(text)) {
          choices.add(null);
        } else {
          Resource r = Resource.stringToResource(text);
          if (!isChoosable(r)) {
            throw new IllegalArgumentException(
                String.format("You cannot take %s from a factory.", r));
          }
          choices.add(r);
        }
      }
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid factory choices");
    }
    int spent = 0;
    for (Resource c : choices) {
      if (c != null) {
        spent++;
      }
    }
    double held = _ref.getPlayerByID(playerID).getResources()
        .get(Resource.COAL);
    if (spent > _maxCoal || spent > held) {
      throw new IllegalArgumentException(String.format(
          "You can only spend %d coal (coal from this roll cannot be spent).",
          (int) Math.min(_maxCoal, held)));
    }
    _choices = choices;
    _isSetup = true;
  }

  @Override
  public String getVerb() {
    return "choose how to use their factories";
  }
}
