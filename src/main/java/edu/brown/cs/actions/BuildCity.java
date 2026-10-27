package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Settings;

/**
 * Action that is responsible for building a city.
 *
 * @author anselvahle
 *
 */
public class BuildCity implements Action {

  private Player _player;
  private Intersection _intersection;
  private Referee _ref;
  private static final String MESSAGE = "Congratulations! You built a City!";
  public static final String ID = "buildCity";

  public BuildCity(Referee ref, int playerID, IntersectionCoordinate i) {
    assert ref != null && i != null;
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _intersection = _ref.getBoard().getIntersections().get(i);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (_intersection == null) {
      throw new IllegalArgumentException("The intersection could not be found.");
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    String warMessage = CatanFormats.warBlocksConstruction(_ref);
    if (warMessage != null) {
      return java.util.Collections.singletonMap(_player.getID(),
          new ActionResponse(false, warMessage, null));
    }
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      ActionResponse resp = new ActionResponse(false,
          "You cannot build when it is not your turn.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    boolean medicineDiscount = _ref.getTurn().hasMedicineDiscount();
    if (!medicineDiscount && !_player.canBuildCity()) {
      ActionResponse resp = new ActionResponse(false,
          "You cannot afford to buy a City.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (medicineDiscount) {
      for (Map.Entry<Resource, Double> price : Settings.MEDICINE_CITY_COST.entrySet()) {
        if (!_player.hasResource(price.getKey(), price.getValue())) {
          ActionResponse resp = new ActionResponse(false,
              "You need 1 ore + 1 wheat for Medicine city.", null);
          return ImmutableMap.of(_player.getID(), resp);
        }
      }
    }
    if (_player.numCities() <= 0) {
      ActionResponse resp = new ActionResponse(false,
          "You have no cities left.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }
    if (!_intersection.canPlaceCity(_player)) {
      ActionResponse resp = new ActionResponse(false,
          "You can only place a city on your own settlement.", null);
      return ImmutableMap.of(_player.getID(), resp);
    }

    // The Action:
    if (medicineDiscount) {
      for (Map.Entry<Resource, Double> price : Settings.MEDICINE_CITY_COST.entrySet()) {
        _player.removeResource(price.getKey(), price.getValue());
      }
      _ref.getTurn().setMedicineDiscount(false);
    } else {
      _player.buildCity();
    }
    _player.useCity();
    _intersection.placeCity(_player);

    // Formatting the response:
    ActionResponse respToPlayer = new ActionResponse(true, MESSAGE, null);
    String message = String.format("%s built a City.", _player.getName());
    ActionResponse respToAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), respToPlayer);
      } else {
        toReturn.put(player.getID(), respToAll);
      }
    }
    return toReturn;
  }
}
