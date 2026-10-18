package edu.brown.cs.board;

import java.util.HashMap;
import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Implementation of Building for the Cursed Catan Capitol: an upgraded city.
 * It produces three resources and is worth three victory points. It is still a
 * City for every other purpose, but cannot be upgraded again.
 */
public class Capitol extends City {
  private Player _capitolPlayer;

  /**
   * Constructor for the class.
   *
   * @param player
   *          Player to be associated with the building.
   */
  public Capitol(Player player) {
    super(player);
    _capitolPlayer = player;
  }

  @Override
  public Map<Integer, Map<Resource, Integer>> collectResource(Resource resource) {
    Map<Resource, Integer> resourceCount = new HashMap<Resource, Integer>();
    resourceCount.put(resource, 3);
    Map<Integer, Map<Resource, Integer>> playerResource = new HashMap<Integer, Map<Resource, Integer>>();
    playerResource.put(_capitolPlayer.getID(), resourceCount);
    return playerResource;
  }

}
