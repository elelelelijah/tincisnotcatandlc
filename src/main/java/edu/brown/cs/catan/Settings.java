package edu.brown.cs.catan;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/**
 * Constants for Settings that are used across all instances of a game.
 *
 */
public abstract class Settings {

  // Number of pieces each player begins the game with:
  public final static int INITIAL_ROADS = 15;
  public final static int INITIAL_SETTLEMENTS = 5;
  public final static int INITIAL_CITIES = 4;

  // Numbers on the board:
  public final static int[] ROLL_NUMS = { 5, 2, 6, 3, 8, 10, 9, 12, 11, 4, 8,
      10, 9, 4, 5, 6, 3, 11 };
  public final static int[] STANDARD_ROLL_NUMS = { 9, 12, 11, 4, 8, 5, 2, 6, 3,
      8, 10, 5, 6, 3, 10, 9, 4, 11 };

  // Number of each tile type on the board:
  public final static int NUM_WOOD_TILE = 4;
  public final static int NUM_BRICK_TILE = 3;
  public final static int NUM_WHEAT_TILE = 4;
  public final static int NUM_SHEEP_TILE = 4;
  public final static int NUM_ORE_TILE = 3;
  public final static int NUM_DESERT_TILE = 1;

  // How many resources should players start with (Beware of changing this.
  // Wildcards!):
  public final static double INITIAL_RESOURCES = 0.0;

  // Building costs:
  public final static Map<Resource, Double> CITY_COST = ImmutableMap.of(
      Resource.WHEAT, 2.0, Resource.ORE, 3.0);
  public final static Map<Resource, Double> MEDICINE_CITY_COST = ImmutableMap.of(
      Resource.ORE, 1.0, Resource.WHEAT, 1.0);
  public final static Map<Resource, Double> ROAD_COST = ImmutableMap.of(
      Resource.BRICK, 1.0, Resource.WOOD, 1.0);
  public final static Map<Resource, Double> DEV_COST = ImmutableMap.of(
      Resource.WHEAT, 1.0, Resource.SHEEP, 1.0, Resource.ORE, 1.0);
  public final static Map<Resource, Double> SETTLEMENT_COST = ImmutableMap.of(
      Resource.BRICK, 1.0, Resource.SHEEP, 1.0, Resource.WHEAT, 1.0,
      Resource.WOOD, 1.0);

  // Bank rates for trading in basic game:
  public final static double BANK_RATE = 4.0;
  public final static double WILDCARD_RATE = 3.0;
  public final static Map<Resource, Double> PORT_RATES = new ImmutableMap.Builder<Resource, Double>()
      .put(Resource.BRICK, 2.0).put(Resource.ORE, 2.0).put(Resource.SHEEP, 2.0)
      .put(Resource.WHEAT, 2.0).put(Resource.WOOD, 2.0)
      .put(Resource.COAL, 3.0).put(Resource.CONCRETE, 2.0)
      .put(Resource.URANIUM, 2.0).put(Resource.WILDCARD, 3.0).build();

  // Port Resource Order
  public final static Resource[] PORT_ORDER = { Resource.WILDCARD,
      Resource.SHEEP, Resource.WILDCARD, Resource.WHEAT, Resource.BRICK,
      Resource.WILDCARD, Resource.ORE, Resource.WILDCARD, Resource.WOOD };

  // Development Card Deck:
  public final static int NUM_KNIGHTS = 14;
  public final static int NUM_POINTS = 5;
  public final static int NUM_YOP = 2;
  public final static int NUM_MONOPOLY = 2;
  public final static int NUM_ROADBUILDING = 2;

  // Default number of players
  public static final int DEFAULT_NUM_PLAYERS = 4;

  // Largest Army & Longest Road Threshold
  public static final int LARGEST_ARMY_THRESH = 3;
  public static final int LONGEST_ROAD_THRESH = 5;

  // Point values
  public static final int SETTLEMENT_POINT_VAL = 1;
  public static final int CITY_POINT_VAL = 2;
  public static final int LONGEST_ROAD_POINT_VAL = 2;
  public static final int LARGEST_ARMY_POINT_VAL = 2;
  public static final double DROP_CARDS_THRESH = 8;
  public static final int WINNING_POINT_COUNT = 10;

  // --- Cursed Catan Constants ---

  // Victory Point target
  public static final int CURSED_WINNING_POINT_COUNT = 13;

  // Bank rates: coal always trades 3:1, uranium 2:1 (ports do not improve them)
  public final static double COAL_BANK_RATE = 3.0;
  public final static double URANIUM_BANK_RATE = 2.0;

  // The Cursed Catan board is one fixed-size island whatever the player
  // count: 27 land tiles.
  public final static int CURSED_NUM_WOOD_TILE = 4;
  public final static int CURSED_NUM_BRICK_TILE = 3;
  public final static int CURSED_NUM_SHEEP_TILE = 4;
  public final static int CURSED_NUM_WHEAT_TILE = 4;
  public final static int CURSED_NUM_ORE_TILE = 3;
  public final static int CURSED_NUM_DESERT_TILE = 2;
  public final static int CURSED_NUM_GOLD_TILE = 2;
  public final static int CURSED_NUM_CONCRETE_TILE = 2;
  public final static int CURSED_NUM_COAL_TILE = 3;

  // Number chits for the 25 non-desert tiles (2s and 12s are doubled so that
  // Test Sites, which need a 2, a 12 or a desert, have room to go).
  public final static int[] CURSED_ROLL_NUMS = { 2, 2, 3, 3, 4, 4, 4, 5, 5, 5,
      6, 6, 6, 8, 8, 8, 9, 9, 10, 10, 10, 11, 11, 12, 12 };

  // Mirrors the starting stock HumanPlayer currently gives every resource for
  // testing (INITIAL_RESOURCES + 25), applied to coal and concrete in Cursed
  // Catan only. Uranium is locked until the Great War, so it starts empty.
  public final static double CURSED_TEST_STARTING_STOCK = 25.0;

  // --- Cities & Knights Constants ---

  // C&K Victory Point target
  public static final int CK_WINNING_POINT_COUNT = 13;

  // Knight costs
  public final static Map<Resource, Double> KNIGHT_COST = ImmutableMap.of(
      Resource.SHEEP, 1.0, Resource.ORE, 1.0);
  public final static Map<Resource, Double> KNIGHT_ACTIVATE_COST = ImmutableMap
      .of(Resource.WHEAT, 1.0);
  public final static Map<Resource, Double> KNIGHT_PROMOTE_STRONG_COST = ImmutableMap
      .of(Resource.SHEEP, 1.0, Resource.ORE, 1.0);
  public final static Map<Resource, Double> KNIGHT_PROMOTE_MIGHTY_COST = ImmutableMap
      .of(Resource.SHEEP, 2.0, Resource.ORE, 2.0);

  // City Wall
  public final static Map<Resource, Double> CITY_WALL_COST = ImmutableMap.of(
      Resource.BRICK, 2.0);
  public static final int MAX_CITY_WALLS = 3;
  public static final int CITY_WALL_HAND_BONUS = 2;

  // Knight piece counts per player
  public static final int INITIAL_BASIC_KNIGHTS = 2;
  public static final int INITIAL_STRONG_KNIGHTS = 2;
  public static final int INITIAL_MIGHTY_KNIGHTS = 2;

  // Barbarian track
  public static final int BARBARIAN_TRACK_LENGTH = 7;

  // Progress card hand limit
  public static final int PROGRESS_CARD_MAX_HAND = 4;

  // Metropolis point value
  public static final int METROPOLIS_POINT_VAL = 2;

  // Defender of Catan point value
  public static final int DEFENDER_POINT_VAL = 1;

}
