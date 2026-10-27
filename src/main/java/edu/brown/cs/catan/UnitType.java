package edu.brown.cs.catan;

/**
 * The kinds of units that fight in the Great War.
 */
public enum UnitType {

  INFANTRY("infantry"), ARTILLERY("artillery"), TANK("tank"), AIRCRAFT(
      "aircraft");

  private final String _name;

  private UnitType(String name) {
    _name = name;
  }

  @Override
  public String toString() {
    return _name;
  }

  /**
   * Turns a name sent by the client into a unit type.
   *
   * @param name
   *          The name, e.g. "infantry".
   * @return The unit type.
   * @throws IllegalArgumentException
   *           If there is no such unit.
   */
  public static UnitType fromString(String name) {
    for (UnitType t : values()) {
      if (t._name.equals(name)) {
        return t;
      }
    }
    throw new IllegalArgumentException("Unknown unit: " + name);
  }
}
