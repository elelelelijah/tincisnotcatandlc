package edu.brown.cs.board;

import edu.brown.cs.catan.Resource;

/**
 * Enum for the different types of tiles.
 *
 * @author anselvahle
 *
 */
public enum TileType {

  WHEAT(Resource.WHEAT), SHEEP(Resource.SHEEP), ORE(Resource.ORE), WOOD(
      Resource.WOOD), BRICK(Resource.BRICK), DESERT(null), SEA(null),
  // Cursed Catan tiles (appended so the original ordering is unchanged).
  // GOLD pays out any one resource of the player's choice, so it has no fixed
  // resource. WASTELAND is what a tile becomes after enough Devastation.
  COAL(Resource.COAL), CONCRETE(Resource.CONCRETE), URANIUM(Resource.URANIUM),
  GOLD(null), WASTELAND(null);

  private Resource _resType;

  // private constructor for associated resource
  private TileType(Resource res) {
    _resType = res;
  }

  /**
   * Gets the resource type associated with the tileType.
   *
   * @return Resource.
   */
  public Resource getType() {
    return _resType;
  }

}
