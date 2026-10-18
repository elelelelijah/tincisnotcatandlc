package edu.brown.cs.catan;

import com.google.gson.JsonObject;

/**
 * Represents one game of Catan's settings.
 *
 */
public class GameSettings {

  public final int numPlayers;
  public final int winningPointCount;
  // Add more colors if more players are supported.
  public final String[] COLORS = { "#BF2720", "#115EC9", "#DFA629", "#EDEAD9" };
  public final boolean isDecimal;
  public final boolean isDynamic;
  public final boolean isStandard;
  public final boolean isCitiesAndKnights;
  public final boolean isCursedCatan;

  public GameSettings(JsonObject settings) {
    int numPlayers = Settings.DEFAULT_NUM_PLAYERS;
    try {
      numPlayers = settings.get("numPlayers").getAsInt();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing numPlayers parameter");
    }
    int winningPointCount = Settings.WINNING_POINT_COUNT;
    try {
      winningPointCount = settings.get("victoryPoints").getAsInt();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing victoryPoints parameter");
    }
    boolean isDecimal = false;
    try {
      isDecimal = settings.get("isDecimal").getAsBoolean();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isDecimal parameter");
    }
    boolean isDynamic = false;
    if (isDecimal) {
      try {
        isDynamic = settings.get("isDynamic").getAsBoolean();
      } catch (NullPointerException e) {
        System.out.println("SETTINGS missing isDynamic parameter");
      }
    }
    boolean isStandard = false;
    try {
      isStandard = settings.get("isStandard").getAsBoolean();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isStandard parameter");
    }
    boolean isCitiesAndKnights = false;
    try {
      isCitiesAndKnights = settings.get("isCitiesAndKnights").getAsBoolean();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isCitiesAndKnights parameter");
    }
    boolean isCursedCatan = false;
    try {
      isCursedCatan = settings.get("isCursedCatan").getAsBoolean();
    } catch (NullPointerException e) {
      System.out.println("SETTINGS missing isCursedCatan parameter");
    }
    // Expansions are mutually exclusive
    if (isCitiesAndKnights && isCursedCatan) {
      throw new IllegalArgumentException(
          "Cities & Knights and Cursed Catan cannot be combined.");
    }
    // Override winning point count for C&K if not explicitly set
    if (isCitiesAndKnights && winningPointCount == Settings.WINNING_POINT_COUNT) {
      winningPointCount = Settings.CK_WINNING_POINT_COUNT;
    }
    // ... and for Cursed Catan
    if (isCursedCatan && winningPointCount == Settings.WINNING_POINT_COUNT) {
      winningPointCount = Settings.CURSED_WINNING_POINT_COUNT;
    }
    this.winningPointCount = winningPointCount;
    this.numPlayers = numPlayers;
    this.isDecimal = isDecimal;
    this.isDynamic = isDynamic;
    this.isStandard = isStandard;
    this.isCitiesAndKnights = isCitiesAndKnights;
    this.isCursedCatan = isCursedCatan;
  }

  // Default Settings
  public GameSettings() {
    this.numPlayers = Settings.DEFAULT_NUM_PLAYERS;
    this.winningPointCount = Settings.WINNING_POINT_COUNT;
    this.isDecimal = false;
    this.isDynamic = false;
    this.isStandard = false;
    this.isCitiesAndKnights = false;
    this.isCursedCatan = false;
  }

}
