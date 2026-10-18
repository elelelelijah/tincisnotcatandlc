package edu.brown.cs.catan;

/**
 * Represents a Catan resource. The Wildcard resource is added for convenience
 * in ports and trades.
 *
 */
public enum Resource {

  WHEAT("wheat", "a wheat"), SHEEP("sheep", "a sheep"), ORE("ore", "an ore"), WOOD(
      "wood", "a wood"), BRICK("brick", "a brick"),
  // Cursed Catan resources. These stay at zero (and unused) in the base game
  // and in Cities & Knights.
  COAL("coal", "a coal"), CONCRETE("concrete", "a concrete"), URANIUM(
      "uranium", "a uranium"),
  // Stays last: stands for "any card" in ports and trades.
  WILDCARD("wildcard", "any card");

  private final String _description;
  private final String _withArticle;

  private Resource(String description, String withArticle) {
    _description = description;
    _withArticle = withArticle;
  }

  @Override
  public String toString() {
    return _description;
  }

  public String stringWithArticle() {
    return _withArticle;
  }

  /**
   * Returns true for the resources that only exist in Cursed Catan (coal,
   * concrete and uranium).
   *
   * @return Whether this resource belongs to the Cursed Catan expansion.
   */
  public boolean isCursedOnly() {
    return this == COAL || this == CONCRETE || this == URANIUM;
  }

  public static Resource stringToResource(String str) {
    switch (str) {
    case "wheat":
      return WHEAT;
    case "sheep":
      return SHEEP;
    case "ore":
      return ORE;
    case "wood":
      return WOOD;
    case "brick":
      return BRICK;
    case "coal":
      return COAL;
    case "concrete":
      return CONCRETE;
    case "uranium":
      return URANIUM;
    case "wildcard":
      return WILDCARD;
    default:
      throw new IllegalArgumentException(String.format(
          "The resource %s does not exist.", str));
    }
  }
}
