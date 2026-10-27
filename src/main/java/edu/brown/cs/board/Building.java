package edu.brown.cs.board;

import java.util.Map;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

/**
 * Interface for the Buildings on the board.
 *
 * @author anselvahle
 *
 */
public interface Building {

  /**
   * Tells the building to tell the player who is associated with it to collect
   * a resource of the input type.
   *
   * @param resource
   *          Type of resource to collect.
   * @return A map of the player id to a map of resources that they collected
   *         and how many of them.
   */
  Map<Integer, Map<Resource, Integer>> collectResource(Resource resource);

  /**
   * Gets the player asscoiated with this building.
   *
   * @return the Player.
   */
  Player getPlayer();


  /**
   * Whether the building lies in ruins. A ruined building produces nothing
   * and does not keep its owner in the war.
   */
  default boolean isRuined() {
    return false;
  }

  /**
   * Turns the building into a ruin (or rebuilds it).
   */
  default void setRuined(boolean ruined) {
    throw new UnsupportedOperationException(
        "This building cannot be ruined.");
  }
}
