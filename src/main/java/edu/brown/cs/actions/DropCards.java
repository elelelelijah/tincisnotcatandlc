package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Action responsible fro discarding cards.
 * 
 * @author anselvahle
 *
 */
public class DropCards implements FollowUpAction {

  private boolean _isSetup;
  private double _numToDrop;
  private int _requiredPlayer;

  private Referee _ref;
  private Player _player;
  private Map<Resource, Double> _toDrop;
  private Map<Commodity, Double> _commodityToDrop;
  private final static double TOLERANCE = 0.001;
  public final static String ID = "dropCards";
  private final static String VERB = "discard";

  public DropCards(int playerID, double numToDrop) {
    _isSetup = false;
    _numToDrop = CatanFormats.round(numToDrop);
    _requiredPlayer = playerID;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before it is executed.");
    }
    // Validation:
    double droppedCards = 0.0;
    for (Map.Entry<Resource, Double> res : _toDrop.entrySet()) {
      if (!_player.hasResource(res.getKey(), res.getValue())) {
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You do not have the cards you are attempting to drop.", null));
      }
      droppedCards += res.getValue();
    }
    if (_commodityToDrop != null) {
      for (Map.Entry<Commodity, Double> entry : _commodityToDrop.entrySet()) {
        if (!_player.hasCommodity(entry.getKey(), entry.getValue())) {
          return ImmutableMap.of(_player.getID(), new ActionResponse(false,
              "You do not have the commodities you are attempting to drop.", null));
        }
        droppedCards += entry.getValue();
      }
    }
    if (Math.abs(droppedCards - _numToDrop) > TOLERANCE) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You did not drop enough cards. Please try again.", null));
    }

    // Action:
    for (Map.Entry<Resource, Double> res : _toDrop.entrySet()) {
      _player.removeResource(res.getKey(), res.getValue(), _ref.getBank());
    }
    if (_commodityToDrop != null) {
      for (Map.Entry<Commodity, Double> entry : _commodityToDrop.entrySet()) {
        _player.removeCommodity(entry.getKey(), entry.getValue());
      }
    }
    _ref.removeFollowUp(this);

    // Formulate Responses:
    ActionResponse toPlayer = new ActionResponse(true, "Thanks for discarding",
        null);
    String message = String
        .format("%s finished discarding.", _player.getName());
    ActionResponse toAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.equals(_player)) {
        toReturn.put(p.getID(), toPlayer);
      } else {
        toReturn.put(p.getID(), toAll);
      }
    }
    return toReturn;
  }

  @Override
  public JsonObject getData() {
    JsonObject toReturn = new JsonObject();
    toReturn.addProperty("numToDrop", _numToDrop);
    toReturn.addProperty("message", "A 7 was rolled. Please drop half of your cards.");
    return toReturn;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    assert playerID == _requiredPlayer;
    JsonObject params = json.get("toDrop").getAsJsonObject();
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    _isSetup = true;
    if (_player == null) {
      throw new IllegalArgumentException("No player exists with the given ID.");
    }
    try {
      _toDrop = new HashMap<Resource, Double>();
      for (Resource res : Resource.values()) {
        if (res == Resource.WILDCARD) {
          continue;
        }
        if (res.isCursedOnly() && !params.has(res.toString())) {
          // Coal, concrete and uranium only appear in Cursed Catan; every
          // other client simply does not send them.
          _toDrop.put(res, 0.0);
        } else {
          _toDrop.put(res, params.get(res.toString()).getAsDouble());
        }
      }
      // C&K: parse commodity drops if present
      if (ref.getGameSettings().isCitiesAndKnights) {
        _commodityToDrop = new HashMap<>();
        for (Commodity c : Commodity.values()) {
          if (params.has(c.toString())) {
            _commodityToDrop.put(c, params.get(c.toString()).getAsDouble());
          } else {
            _commodityToDrop.put(c, 0.0);
          }
        }
      }
    } catch (JsonSyntaxException | NullPointerException e) {
      throw new IllegalArgumentException("Missing a resource or bad JSON input");
    }
  }

  @Override
  public String getID() {
    return DropCards.ID;
  }

  @Override
  public int getPlayerID() {
    return _requiredPlayer;
  }

  @Override
  public String getVerb() {
    return VERB;
  }

}
