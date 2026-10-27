package edu.brown.cs.board;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Settlement implentation of the Building interface.
 *
 * @author anselvahle
 *
 */
public class Settlement implements Building {
  private boolean _ruined = false;

  @Override
  public boolean isRuined() {
    return _ruined;
  }

  @Override
  public void setRuined(boolean ruined) {
    _ruined = ruined;
  }

  private Player _player;

  /**
   * Constructor for the class.
   * 
   * @param player
   *          Player who built this settlement.
   */
  public Settlement(Player player) {
    _player = player;
  }

  @Override
  public Map<Integer, Map<Resource, Integer>> collectResource(Resource resource) {
    if (_ruined) {
      // A ruin produces nothing until it is rebuilt
      return new HashMap<Integer, Map<Resource, Integer>>();
    }
    Map<Resource, Integer> resourceCount = new HashMap<Resource, Integer>();
    resourceCount.put(resource, 1);
    Map<Integer, Map<Resource, Integer>> playerResource = new HashMap<Integer, Map<Resource, Integer>>();
    playerResource.put(_player.getID(), resourceCount);
    return playerResource;
  }

  @Override
  public Player getPlayer() {
    return _player;
  }

}
