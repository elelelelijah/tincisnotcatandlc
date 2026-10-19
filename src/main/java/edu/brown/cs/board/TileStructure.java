package edu.brown.cs.board;

/**
 * Cursed Catan: something a player has built on a tile rather than on an
 * intersection: a Fortress or a Test Site. Neither is worth victory points.
 */
public class TileStructure {

  /**
   * The kinds of structure that can stand on a tile.
   */
  public enum Kind {
    FORTRESS, TEST_SITE
  }

  private final Kind _kind;
  private final int _ownerID;
  private int _hitPoints;

  /**
   * Constructor for the class.
   *
   * @param kind
   *          What this structure is.
   * @param ownerID
   *          The ID of the player who owns it.
   * @param hitPoints
   *          How much damage it can take (used in the Great War).
   */
  public TileStructure(Kind kind, int ownerID, int hitPoints) {
    _kind = kind;
    _ownerID = ownerID;
    _hitPoints = hitPoints;
  }

  public Kind getKind() {
    return _kind;
  }

  public int getOwnerID() {
    return _ownerID;
  }

  public int getHitPoints() {
    return _hitPoints;
  }

  @Override
  public String toString() {
    return String.format("%s (owner %d, %d HP)", _kind, _ownerID, _hitPoints);
  }
}
