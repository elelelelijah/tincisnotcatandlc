package edu.brown.cs.board;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Implementation of Building for the City.
 *
 * @author anselvahle
 *
 */
public class City implements Building {
  private boolean _ruined = false;

  @Override
  public boolean isRuined() {
    return _ruined;
  }

  @Override
  public void setRuined(boolean ruined) {
    _ruined = ruined;
  }

  private int _hp = -1;
  private edu.brown.cs.catan.Faction _occupier = null;

  @Override
  public int getMaxHp() {
    return 8;
  }

  @Override
  public int getHp() {
    return _hp < 0 ? getMaxHp() : _hp;
  }

  @Override
  public void setHp(int hp) {
    _hp = Math.max(0, hp);
  }

  @Override
  public edu.brown.cs.catan.Faction getOccupier() {
    return _occupier;
  }

  @Override
  public void setOccupier(edu.brown.cs.catan.Faction faction) {
    _occupier = faction;
  }

  private Player _player;

  /**
   * Constructor for the class.
   *
   * @param player
   *          Player to be associated with the the building.
   */
  public City(Player player) {
    _player = player;
  }

  @Override
  public Map<Integer, Map<Resource, Integer>> collectResource(Resource resource) {
    if (_ruined) {
      // A ruin produces nothing until it is rebuilt
      return new HashMap<Integer, Map<Resource, Integer>>();
    }
    Map<Resource, Integer> resourceCount = new HashMap<Resource, Integer>();
    resourceCount.put(resource, 2);
    Map<Integer, Map<Resource, Integer>> playerResource = new HashMap<Integer, Map<Resource, Integer>>();
    playerResource.put(_player.getID(), resourceCount);
    return playerResource;
  }

  @Override
  public Player getPlayer() {
    return _player;
  }

}
