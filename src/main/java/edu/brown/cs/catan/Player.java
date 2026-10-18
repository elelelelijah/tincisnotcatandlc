package edu.brown.cs.catan;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a Catan Player. Responsible for keeping track of a Player's data
 * including their Hand, Name, Points..etc.
 *
 */
public interface Player {

  /**
   * Returns the number of roads a player has remaining.
   *
   * @return Number of roads remaining.
   */
  int numRoads();

  /**
   * Returns the number of settlements a player has remaining.
   *
   * @return Number of settlements remaining.
   */
  int numSettlements();

  /**
   * Returns the number of cities a player has remaining.
   *
   * @return Number of cities remaining.
   */
  int numCities();

  /**
   * Removes the resource costs of building a road.
   */
  void buildRoad();

  /**
   * Removes the resource costs of building a settlement.
   */
  void buildSettlement();

  /**
   * Removes the resource costs of building a city.
   */
  void buildCity();

  /**
   * Removes the resource costs of buying a development card.
   */
  void buyDevelopmentCard();

  /**
   * Uses a Road piece.
   */
  void useRoad();

  /**
   * Uses a City piece.
   */
  void useCity();

  /**
   * Returns a city piece to the player's supply and consumes a settlement
   * piece (for barbarian pillage: city demoted back to settlement on board).
   */
  default void returnCity() {
    throw new UnsupportedOperationException(
        "Not supported for this player type.");
  }

  void useSettlement();

  void playDevelopmentCard(DevelopmentCard card);

  Map<Resource, Double> getResources();

  Map<DevelopmentCard, Integer> getDevCards();

  void addResource(Resource resource);

  void addResource(Resource resource, double count);

  void addResource(Resource resource, double count, Bank bank);

  void removeResource(Resource resource);

  void removeResource(Resource resource, double count);

  void removeResource(Resource resource, double count, Bank bank);

  double getNumResourceCards();

  int getNumDevelopmentCards();

  void addDevelopmentCard(DevelopmentCard card);

  Player getImmutableCopy();

  String getName();

  int getID();

  int numPlayedKnights();

  int numVictoryPoints();

  String getColor();

  boolean canBuyDevelopmentCard();

  boolean canBuildRoad();

  boolean canBuildCity();

  boolean canBuildSettlement();

  boolean hasResource(Resource res, double count);

  // --- Cursed Catan methods ---

  /**
   * Returns the number of factories this player has on the board.
   */
  default int numFactories() {
    return 0;
  }

  /**
   * Returns the number of capitols this player has on the board.
   */
  default int numCapitols() {
    return 0;
  }

  /**
   * Whether the player can afford a factory and has not built the maximum.
   */
  default boolean canBuildFactory() {
    return false;
  }

  /**
   * Whether the player can afford a capitol and has not built the maximum.
   */
  default boolean canBuildCapitol() {
    return false;
  }

  /**
   * Removes the resource costs of building a factory.
   */
  default void buildFactory() {
    throw new UnsupportedOperationException(
        "Not supported for this player type.");
  }

  /**
   * Removes the resource costs of building a capitol.
   */
  default void buildCapitol() {
    throw new UnsupportedOperationException(
        "Not supported for this player type.");
  }

  /**
   * Records a factory built in place of a settlement: the settlement piece
   * goes back to the player's supply.
   */
  default void useFactory() {
    throw new UnsupportedOperationException(
        "Not supported for this player type.");
  }

  /**
   * Records a capitol built in place of a city: the city piece goes back to
   * the player's supply.
   */
  default void useCapitol() {
    throw new UnsupportedOperationException(
        "Not supported for this player type.");
  }

  // --- Cities & Knights methods ---

  /**
   * Returns the player's commodity hand.
   */
  default Map<Commodity, Double> getCommodities() {
    return Collections.emptyMap();
  }

  /**
   * Adds a commodity to the player's hand.
   */
  default void addCommodity(Commodity commodity) {
    addCommodity(commodity, 1.0);
  }

  /**
   * Adds a specified amount of a commodity.
   */
  default void addCommodity(Commodity commodity, double count) {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Removes a commodity from the player's hand.
   */
  default void removeCommodity(Commodity commodity) {
    removeCommodity(commodity, 1.0);
  }

  /**
   * Removes a specified amount of a commodity.
   */
  default void removeCommodity(Commodity commodity, double count) {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Returns whether the player has at least the given count of a commodity.
   */
  default boolean hasCommodity(Commodity commodity, double count) {
    return getCommodities().getOrDefault(commodity, 0.0) >= count;
  }

  /**
   * Returns the player's city improvement tracker.
   */
  default CityImprovement getCityImprovement() {
    return null;
  }

  /**
   * Returns the player's knight pieces.
   */
  default List<KnightPiece> getKnights() {
    return Collections.emptyList();
  }

  /**
   * Adds a knight to the player's knight list.
   */
  default void addKnight(KnightPiece knight) {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Removes a knight from the player's knight list.
   */
  default void removeKnight(KnightPiece knight) {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Returns the player's progress cards.
   */
  default List<ProgressCard> getProgressCards() {
    return Collections.emptyList();
  }

  /**
   * Adds a progress card to the player's hand.
   */
  default void addProgressCard(ProgressCard card) {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Removes a progress card from the player's hand.
   */
  default void removeProgressCard(ProgressCard card) {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Returns the number of Defender of Catan VP tokens.
   */
  default int getDefenderPoints() {
    return 0;
  }

  /**
   * Adds a Defender of Catan VP token.
   */
  default void addDefenderPoint() {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Returns the number of city walls the player has built.
   */
  default int getCityWallCount() {
    return 0;
  }

  /**
   * Adds a city wall.
   */
  default void addCityWall() {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Removes a city wall (e.g., when a city is pillaged).
   */
  default void removeCityWall() {
    throw new UnsupportedOperationException(
        "Cities & Knights not supported for this player type.");
  }

  /**
   * Returns the total active knight strength for this player.
   */
  default int getActiveKnightStrength() {
    int strength = 0;
    for (KnightPiece k : getKnights()) {
      if (k.isActive()) {
        strength += k.getStrength();
      }
    }
    return strength;
  }

  /**
   * Returns the hand size limit considering city walls.
   * Base limit is 7, each city wall adds 2.
   */
  default double getHandLimit() {
    return Settings.DROP_CARDS_THRESH
        + (getCityWallCount() * Settings.CITY_WALL_HAND_BONUS);
  }

  /**
   * Adds a hidden victory point (e.g. from VP progress cards like Constitution,
   * Printer).
   */
  default void addVictoryPoint() {
    throw new UnsupportedOperationException(
        "Not supported for this player type.");
  }

}

