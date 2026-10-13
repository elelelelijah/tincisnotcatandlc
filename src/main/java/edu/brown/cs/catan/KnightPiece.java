package edu.brown.cs.catan;

import edu.brown.cs.board.IntersectionCoordinate;

/**
 * Represents a Knight piece in the Cities & Knights expansion. Knights can be
 * placed at intersections, activated, promoted (Basic → Strong → Mighty), and
 * moved along roads. Active knights contribute to barbarian defense.
 */
public class KnightPiece {

  /**
   * Possible knight levels with their corresponding strength values.
   */
  public enum KnightLevel {
    BASIC(1), STRONG(2), MIGHTY(3);

    private final int _strength;

    private KnightLevel(int strength) {
      _strength = strength;
    }

    public int getStrength() {
      return _strength;
    }

    /**
     * Returns the next promotion level, or null if already at max.
     */
    public KnightLevel nextLevel() {
      switch (this) {
      case BASIC:
        return STRONG;
      case STRONG:
        return MIGHTY;
      default:
        return null;
      }
    }

    @Override
    public String toString() {
      return name().substring(0, 1) + name().substring(1).toLowerCase();
    }
  }

  private KnightLevel _level;
  private boolean _active;
  private final int _ownerID;
  private IntersectionCoordinate _position;
  private boolean _usedThisTurn;
  private boolean _newThisTurn = false;
private int _restrictedTurn = -1;
  /**
   * Creates a new Basic, inactive knight.
   *
   * @param ownerID
   *          The ID of the player who owns this knight.
   * @param position
   *          The intersection where this knight is placed.
   */
  public KnightPiece(int ownerID, IntersectionCoordinate position) {
    _level = KnightLevel.BASIC;
    _active = false;
    _ownerID = ownerID;
    _position = position;
    _usedThisTurn = false;
  }

  /**
   * Activates this knight. Costs 1 wheat.
   */
  public void activate() {
    _active = true;
  }

  /**
   * Deactivates this knight. Called after movement or barbarian attack.
   */
  public void deactivate() {
    _active = false;
  }

  /**
   * Promotes this knight to the next level.
   *
   * @throws IllegalStateException
   *           if already at MIGHTY level.
   */
  public void promote() {
    KnightLevel next = _level.nextLevel();
    if (next == null) {
      throw new IllegalStateException("Cannot promote a Mighty knight.");
    }
    _level = next;
  }

  /**
   * Moves this knight to a new position and deactivates it.
   *
   * @param newPosition
   *          The new intersection coordinate.
   */
  public void moveTo(IntersectionCoordinate newPosition) {
    _position = newPosition;
    _active = false;
    _usedThisTurn = true;
  }
/**
   * Relocates this knight WITHOUT changing its active status. Used when an
   * opponent displaces this knight and its owner moves it to safety.
   *
   * @param newPosition
   *          The new intersection coordinate.
   */
  public void relocateTo(IntersectionCoordinate newPosition) {
    _position = newPosition;
  }
   /**
   * Records that this knight was recruited, promoted or activated on the given
   * turn, so it cannot perform an action (move / displace) until a later turn.
   */
 public void restrictForTurn(int turnNum) {
    _newThisTurn = true;
  }
 
  /**
   * Returns true if this knight was recruited, promoted or activated during
   * the current turn and therefore cannot act yet. The turn number argument is
   * kept for API compatibility and is not used.
   */
  public boolean isRestrictedOn(int turnNum) {
    return _newThisTurn;
  }
 
  /**
   * Lifts the same-turn restriction. Called when the owner ends their turn.
   */
  public void clearRestriction() {
    _newThisTurn = false;
  }
  /**
   * Returns whether this knight can displace the given opponent knight. A
   * knight can only displace a strictly weaker knight.
   *
   * @param other
   *          The opponent knight.
   * @return true if this knight is stronger.
   */
  public boolean canDisplace(KnightPiece other) {
    return _active && _level.getStrength() > other._level.getStrength();
  }

  /**
   * Marks this knight as having been used this turn.
   */
  public void markUsed() {
    _usedThisTurn = true;
  }

  /**
   * Resets the per-turn usage flag. Called at the start of each turn.
   */
  public void resetTurnUsage() {
    _usedThisTurn = false;
  }

  // --- Getters ---

  public KnightLevel getLevel() {
    return _level;
  }

  public int getStrength() {
    return _level.getStrength();
  }

  public boolean isActive() {
    return _active;
  }

  public int getOwnerID() {
    return _ownerID;
  }

  public IntersectionCoordinate getPosition() {
    return _position;
  }

  public boolean hasBeenUsedThisTurn() {
    return _usedThisTurn;
  }

  @Override
  public String toString() {
    return String.format("%s Knight (Owner: %d, Active: %b, Position: %s)",
        _level, _ownerID, _active, _position);
  }

}
