package edu.brown.cs.actions;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Action Responsible for moving the robber.
 * 
 * @author anselvahle
 *
 */
public class MoveRobber implements FollowUpAction {

  private boolean _isSetup;
  private Referee _ref;
  private int _playerID;
  private HexCoordinate _newLocation;
  public final static String ID = "moveRobber";
  private final static String VERB = "move the Robber";
  private boolean _isTurnStart = false;
  private boolean _sevenWasRolled = false;
  // Cursed Catan: option to spend a Spy Network and ignore fortresses
  private boolean _useSpyNetwork = false;

  public MoveRobber(int playerID) {
    _playerID = playerID;
    _isSetup = false;
  }

  public MoveRobber(int playerID, boolean isTurnStart) {
    _playerID = playerID;
    _isSetup = false;
    _isTurnStart = isTurnStart;
  }

  public MoveRobber(int playerID, boolean isTurnStart, boolean sevenWasRolled) {
    _playerID = playerID;
    _isSetup = false;
    _sevenWasRolled = sevenWasRolled;
  }


  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetup) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before it is executed.");
    }
    Map<Integer, ActionResponse> toRet = new HashMap<Integer, ActionResponse>();
    Set<Integer> playersOnTile = Collections.emptySet();
    if (_useSpyNetwork) {
      String problem = spyNetworkProblem();
      if (problem != null) {
        toRet.put(_playerID, new ActionResponse(false, problem, null));
        return toRet;
      }
    }
    try {
      playersOnTile = _ref.getBoard().moveRobber(_newLocation);
    } catch (IllegalArgumentException e) {
      ActionResponse toAdd = new ActionResponse(false,
          "Please choose a new location", null);
      toRet.put(_playerID, toAdd);
      return toRet;
    }
    // Cursed Catan: a fortress protects its owner from being stolen from
    Tile robbed = _ref.getBoard().getTile(_newLocation);
    if (robbed != null) {
      Set<Integer> unprotected = new HashSet<>();
      for (int player : playersOnTile) {
        if (_useSpyNetwork || !robbed.hasFortress(player)) {
          unprotected.add(player);
        }
      }
      playersOnTile = unprotected;
    }
    int originalPlayersOnTile = playersOnTile.size();
    // Remove the actual player & all players with no resources:
    Set<Integer> temp = new HashSet<>();
    for (int player : playersOnTile) {
      if (player != _playerID) {
        for (Map.Entry<Resource, Double> res : _ref.getPlayerByID(player)
            .getResources().entrySet()) {
          if (res.getValue() >= 1.0) {
            temp.add(player);
            break;
          }
        }
      } else {
        originalPlayersOnTile -= 1;
      }
    }
    playersOnTile = temp;
    _ref.removeFollowUp(this);
    if (!playersOnTile.isEmpty()) {
      FollowUpAction followUp = new TakeCardAction(_playerID, playersOnTile); // TODO!
      _ref.addFollowUp(ImmutableList.of(followUp));
    }
    if(_isTurnStart){
      _ref.addFollowUp(ImmutableList.of(new RollDice(_playerID)));
    }
    if (_useSpyNetwork) {
      // The move succeeded, so the Spy Network is spent
      _ref.getPlayerByID(_playerID).playDevelopmentCard(
          DevelopmentCard.SPY_NETWORK);
      _ref.playDevCard();
      _ref.activateSpyNetwork();
    }
    String message = "You moved the Robber. There was no one to steal from where you placed the Robber.";
    if (originalPlayersOnTile > 0) {
      message = "You moved the Robber. No player has enough cards for you to steal.";
    }
    if (_useSpyNetwork) {
      message += " Your Spy Network ignored all fortresses.";
    }
    ActionResponse toPlayer = new ActionResponse(true, message, playersOnTile);
    String messageToAll = String.format("%s moved the Robber", _ref
        .getPlayerByID(_playerID).getName());
    if (_useSpyNetwork) {
      messageToAll += ", using a Spy Network to ignore fortresses";
    }
    ActionResponse toAll = new ActionResponse(true, messageToAll, null);
    for (Player p : _ref.getPlayers()) {
      if (p.getID() == _playerID) {
        toRet.put(p.getID(), toPlayer);
      } else {
        toRet.put(p.getID(), toAll);
      }
    }
    return toRet;
  }

  /**
   * Checks that the player may spend a Spy Network on this robber move.
   *
   * @return A message for the player, or null if it is allowed.
   */
  private String spyNetworkProblem() {
    Player player = _ref.getPlayerByID(_playerID);
    if (!_ref.getGameSettings().isCursedCatan) {
      return "Spy Networks only exist in Cursed Catan.";
    }
    Integer held = player.getDevCards().get(DevelopmentCard.SPY_NETWORK);
    if (held == null || held < 1) {
      return "You don't have a Spy Network";
    }
    if (_ref.getTurn().devHasBeenPlayed()) {
      return "You already played a development card this turn";
    }
    if (!_ref.getTurn().hadInitialDevCard(DevelopmentCard.SPY_NETWORK)) {
      return "You cannot play a development card on the turn you bought it";
    }
    return null;
  }

  @Override
  public JsonObject getData() {
    String message = "Please move the robber.";
    if(_sevenWasRolled){
      message = "A 7 was rolled. Please move the robber.";
    }

    JsonObject toReturn = new JsonObject();
    toReturn.addProperty("message", message);
    return toReturn;
  }

  @Override
  public String getID() {
    return MoveRobber.ID;
  }

  @Override
  public void setupAction(Referee ref, int playerID, JsonObject json) {
    JsonObject params = json.get("newLocation").getAsJsonObject();
    HexCoordinate newLocation = convertToHexCoordrinate(params);
    assert ref != null;
    assert _playerID == playerID;
    _ref = ref;
    if (_ref.getPlayerByID(playerID) == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    _newLocation = newLocation;
    JsonElement spy = json.get("useSpyNetwork");
    _useSpyNetwork = spy != null && spy.isJsonPrimitive()
        && spy.getAsBoolean();
    _isSetup = true;
  }

  @Override
  public int getPlayerID() {
    return _playerID;
  }

  private HexCoordinate convertToHexCoordrinate(JsonObject json) {
    try {
      int x = json.get("x").getAsInt();
      int y = json.get("y").getAsInt();
      int z = json.get("z").getAsInt();
      return new HexCoordinate(x, y, z);
    } catch (NullPointerException | JsonSyntaxException e) {
      throw new IllegalArgumentException("Missing coordinate x,y, or z");
    }
  }

  @Override
  public String getVerb() {
    return MoveRobber.VERB;
  }

}
