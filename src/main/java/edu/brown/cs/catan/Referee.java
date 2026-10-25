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
}
