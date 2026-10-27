package edu.brown.cs.catan;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.board.Board;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.gamestats.GameStats;

public interface Referee {

  void startNextTurn();

  Turn getTurn();

  Player currentPlayer();

  void playDevCard();

  int addPlayer(String name);

  int addPlayer(String name, String color);

  DevelopmentCard getDevCard();

  boolean devCardDeckIsEmpty();

  GameSettings getGameSettings();

  Referee getReadOnlyReferee();

  Map<Resource, Double> getBankRates(int playerID);

  Board getBoard();

  Player getPlayerByID(int id);

  Collection<Player> getPlayers();

  boolean hasLongestRoad(int playerID);

  boolean hasLargestArmy(int playerID);

  int getNumPublicPoints(int playerID);

  int getNumTotalPoints(int playerID);

  Bank getBank();

  GameStatus getGameStatus();

  void setGameStatus(GameStatus state);

  FollowUpAction getNextFollowUp(int playerID);

  void addFollowUp(Collection<FollowUpAction> actions);

  public void removeFollowUp(FollowUpAction action);

  List<Integer> getTurnOrder();

  Setup getSetup();

  Player getWinner();

  GameStats getGameStats();

  boolean removePlayer(int id);

  // --- Cities & Knights: Merchant ---
  void setMerchant(int playerID, HexCoordinate hex);

  int getMerchantOwner();

  HexCoordinate getMerchantHex();

  // --- Cities & Knights: Alchemist dice override ---
  void setOverriddenDice(int red, int white, int event);

  int[] consumeOverriddenDice();

  public enum GameStatus {
    WAITING, // Waiting for players (pre-game)
    SETUP, // Placement of settlements
    PROGRESS; // Regular game in progress
  }


  // --- Cursed Catan ---

  /**
   * Returns the position (1 to 12) on the Industry track, or 0 when the game
   * is not Cursed Catan.
   */
  default int getIndustryTrack() {
    return 0;
  }

  /**
   * Records that coal was spent this turn, so the Industry track will advance
   * when the turn ends. Trading coal does not count.
   */
  default void recordCoalSpent() {
  }

  /**
   * Called when a turn ends: advances the Industry track once if any coal was
   * spent this turn, and starts the next turn with a clean slate.
   *
   * @return True if the track moved.
   */
  default boolean advanceIndustryTrack() {
    return false;
  }

  /**
   * Whether the Industry track has reached the space where the Great War
   * begins.
   */
  default boolean isGreatWarTriggered() {
    return false;
  }

  /**
   * Whether fortresses are ignored for the robber this turn (the current
   * player used a Spy Network).
   */
  default boolean ignoresFortresses() {
    return false;
  }

  /**
   * Records that the current player used a Spy Network this turn.
   */
  default void activateSpyNetwork() {
  }

  /**
   * Whether the player holds all five pieces of Exodia (Cursed Catan only).
   */
  default boolean holdsExodia(Player player) {
    return false;
  }

  /**
   * The player who has taken the longest turn so far (Cursed Catan), or -1 if
   * nobody has finished a turn yet.
   */
  default int getLongestTurnPlayer() {
    return -1;
  }

  /**
   * How long the longest turn took, in milliseconds.
   */
  default long getLongestTurnMillis() {
    return 0;
  }

  /**
   * Declares the game a draw. When it is a draw the player with the most
   * victory points wins, then the one with the highest social credit; if
   * there is still a tie nobody wins. Used by the war and by the board
   * checks.
   *
   * @param reason
   *          Why the game ended in a draw.
   */
  default void declareDraw(String reason) {
  }

  /**
   * Whether the game has ended in a draw: it was declared, or (Cursed Catan)
   * two or more players are at the winning score at the same time and nobody
   * holds Exodia.
   */
  default boolean isDraw() {
    return false;
  }

  /**
   * Why the game ended in a draw, or null.
   */
  default String getDrawReason() {
    return null;
  }

  /**
   * The winner of a drawn game by the tie-break rules, or null if nobody
   * wins.
   */
  default Player getDrawWinner() {
    return null;
  }
}
