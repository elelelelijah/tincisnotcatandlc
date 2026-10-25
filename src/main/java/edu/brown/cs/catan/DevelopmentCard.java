package edu.brown.cs.catan;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Catan DevelopmentCard. This Enum currently contains the Standard
 * Development Cards from the original game of Settlers of Catan. New
 * development cards could be added here in the future.
 *
 */
public enum DevelopmentCard {

  KNIGHT("Knight", "a Knight"), YEAR_OF_PLENTY("Year of Plenty",
      "Year of Plenty"), MONOPOLY("Monopoly", "Monopoly"), POINT(
      "Victory Point", "a Victory Point."), ROAD_BUILDING("Road Building",
      "Road Building"),

  // ---- Cursed Catan only ----
  // Playable cards
  SPY_NETWORK("Spy Network", "a Spy Network", 0), BORDER_RAID("Border Raid",
      "a Border Raid", 0),
  // Held cards: they do nothing when played, but count for social credit
  MAO_ZEDONG("Mao Zedong", "Mao Zedong", 100), XI_JINPING("Xi Jinping",
      "Xi Jinping", 100), BING_CHILLING("Bing Chilling", "Bing Chilling", 420), HALF_DOLLAR(
      "$0.50", "$0.50", 50), NOTHING_HAPPENED("Place Where Nothing Happened",
      "the Place Where Nothing Happened", 1989), TAIWAN("Taiwan", "Taiwan",
      -999999), TROLLING("A Small Amount of Trolling",
      "A Small Amount of Trolling", 0),
  // The five pieces of Exodia: hold all of them to win instantly
  EXODIA_RIGHT_ARM("Right Arm of the Forbidden One",
      "the Right Arm of the Forbidden One", 0), EXODIA_LEFT_ARM(
      "Left Arm of the Forbidden One", "the Left Arm of the Forbidden One", 0), EXODIA_RIGHT_LEG(
      "Right Leg of the Forbidden One", "the Right Leg of the Forbidden One",
      0), EXODIA_LEFT_LEG("Left Leg of the Forbidden One",
      "the Left Leg of the Forbidden One", 0), EXODIA_HEAD(
      "Exodia the Forbidden One", "Exodia the Forbidden One", 0);

  /** The five cards that together win the game. */
  public static final List<DevelopmentCard> EXODIA_PIECES = Collections
      .unmodifiableList(Arrays.asList(EXODIA_RIGHT_ARM, EXODIA_LEFT_ARM,
          EXODIA_RIGHT_LEG, EXODIA_LEFT_LEG, EXODIA_HEAD));

  private final String name;
  private final String description;
  private final boolean cursedOnly;
  private final int socialCredit;

  private DevelopmentCard(String name, String description) {
    this.name = name;
    this.description = description;
    this.cursedOnly = false;
    this.socialCredit = 0;
  }

  private DevelopmentCard(String name, String description, int socialCredit) {
    this.name = name;
    this.description = description;
    this.cursedOnly = true;
    this.socialCredit = socialCredit;
  }

  /**
   * Social credit this card is worth. Only used to break ties.
   */
  public int getSocialCredit() {
    return socialCredit;
  }

  /**
   * Whether this card only exists in Cursed Catan.
   */
  public boolean isCursedOnly() {
    return cursedOnly;
  }

  /**
   * Whether this card is one of the five pieces of Exodia.
   */
  public boolean isExodiaPiece() {
    return EXODIA_PIECES.contains(this);
  }

  /**
   * Used for sending messages to Players.
   *
   * @return A String in a helpful format for Player messages.
   */
  public String toFancyString() {
    return this.description;
  }

  @Override
  public String toString() {
    return this.name;
  }

}
