package edu.brown.cs.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.BarbarianTrack;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.ProgressCard;

import edu.brown.cs.actions.ActionResponse;
import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.board.Board;
import edu.brown.cs.board.BoardTile;
import edu.brown.cs.board.Building;
import edu.brown.cs.board.City;
import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.board.Port;
import edu.brown.cs.board.Road;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;

public class CatanConverter {

  private Gson _gson;

  public CatanSettings getSettings(String settings) {
    try {
      return _gson.fromJson(settings, CatanSettings.class);
    } catch (Exception e) { // TODO: change to something better?
      throw new IllegalArgumentException("Could not parse settings JSON.");
    }

  }

  public CatanConverter() {
    _gson = new Gson();
  }

  public JsonObject getGameState(Referee ref, int playerID) {
    return _gson.toJsonTree(new GameState(ref, playerID)).getAsJsonObject();
  }

  public Map<Integer, JsonObject> responseToJSON(
      Map<Integer, ActionResponse> response) {
    Map<Integer, JsonObject> toReturn = new HashMap<>();
    for (Map.Entry<Integer, ActionResponse> entry : response.entrySet()) {
      toReturn.put(entry.getKey(), _gson.toJsonTree(entry.getValue())
          .getAsJsonObject());
    }
    return toReturn;
  }

  private static class GameState {
    private int playerID;
    private List<Integer> turnOrder;
    private Integer winner;
    private Hand hand;
    private BoardRaw board;
    private int currentTurn;
    private FollowUpActionRaw followUp;
    private Collection<PublicPlayerRaw> players;
    private GameSettings settings;
    private GameStatsRaw stats;
    // C&K fields (null when not in C&K mode)
    private BarbarianTrackRaw barbarianTrack;
    private Integer merchantOwner;
    private HexCoordinate merchantHex;
    // Active card effect flags for the current turn's player
    private Boolean medicineActive;
    private Boolean craneActive;
    private Boolean merchantFleetActive;

    public GameState(Referee ref, int playerID) {
      this.playerID = playerID;
      this.currentTurn = ref.currentPlayer() != null ? ref.currentPlayer()
          .getID() : -1;
      this.hand = new Hand(ref.getPlayerByID(playerID),
          ref.getGameSettings().isCitiesAndKnights);
      this.board = new BoardRaw(ref, ref.getBoard(),
          playerID);
      this.turnOrder = (ref.getGameStatus() != GameStatus.WAITING) ? ref
          .getTurnOrder() : null;
      this.winner = ref.getWinner() != null ? ref.getWinner().getID() : null;
      this.followUp = ref.getNextFollowUp(playerID) != null ? new FollowUpActionRaw(
          ref.getNextFollowUp(playerID)) : null;
      this.players = new ArrayList<>();
      this.settings = ref.getGameSettings();
      this.stats = new GameStatsRaw(ref);
      for (Player p : ref.getPlayers()) {
        players.add(new PublicPlayerRaw(p, ref.getReadOnlyReferee()));
      }
      // C&K state
      if (ref.getGameSettings().isCitiesAndKnights && ref instanceof MasterReferee) {
        MasterReferee mr = (MasterReferee) ref;
        this.barbarianTrack = new BarbarianTrackRaw(mr.getBarbarianTrack());
        if (mr.getMerchantOwner() >= 0) {
          this.merchantOwner = mr.getMerchantOwner();
          this.merchantHex = mr.getMerchantHex();
        }
      }
      // Active card effect flags (only for the requesting player on their turn)
      if (ref.currentPlayer() != null && ref.currentPlayer().getID() == playerID) {
        this.medicineActive = ref.getTurn().hasMedicineDiscount();
        this.craneActive = ref.getTurn().hasCraneDiscount();
        this.merchantFleetActive = ref.getTurn().hasMerchantFleet();
      }
    }
  }

  public static class CatanSettings {
    private final int numPlayers;
    private final boolean isDecimal;

    public CatanSettings(int numPlayers, boolean decimal) {
      this.numPlayers = numPlayers;
      this.isDecimal = decimal;
    }

    public int getNumPlayers() {
      return numPlayers;
    }

    public boolean isDecimal() {
      return isDecimal;
    }

  }

  private static class Hand {
    private final Map<Resource, Double> resources;
    private final Map<DevelopmentCard, Integer> devCards;
    private boolean canBuildRoad;
    private boolean canBuildSettlement;
    private boolean canBuildCity;
    private boolean canBuyDevCard;
    // C&K fields (null when not in C&K mode)
    private Map<Commodity, Double> commodities;
    private List<String> progressCards;

    public Hand(Player player, boolean isCK) {
      resources = player.getResources();
      devCards = player.getDevCards();
      canBuildRoad = player.canBuildRoad();
      canBuildSettlement = player.canBuildSettlement();
      canBuildCity = player.canBuildCity();
      canBuyDevCard = player.canBuyDevelopmentCard();
      if (isCK) {
        commodities = player.getCommodities();
        progressCards = new ArrayList<>();
        for (ProgressCard pc : player.getProgressCards()) {
          progressCards.add(pc.toString());
        }
      }
    }
  }

  private static class BoardRaw {
    private final Collection<TileRaw> tiles;
    private final Collection<IntersectionRaw> intersections;
    private final Collection<PathRaw> paths;

    public BoardRaw(Referee ref, Board board, int playerID) {
      intersections = new ArrayList<>();
      Map<IntersectionCoordinate, String> metropolisMap = new HashMap<>();

      // Calculate metropolises if C&K
      if (ref.getGameSettings().isCitiesAndKnights && ref instanceof MasterReferee) {
        MasterReferee mr = (MasterReferee) ref;
        // Group cities by player
        Map<Integer, List<Intersection>> playerCities = new HashMap<>();
        for (Intersection i : board.getIntersections().values()) {
          if (i.getBuilding() != null && i.getBuilding() instanceof City) {
            int owner = i.getBuilding().getPlayer().getID();
            if (!playerCities.containsKey(owner)) {
              playerCities.put(owner, new ArrayList<>());
            }
            playerCities.get(owner).add(i);
          }
        }

        // Assign metropolises
        for (Map.Entry<Integer, List<Intersection>> entry : playerCities.entrySet()) {
          int pid = entry.getKey();
          List<Intersection> cities = entry.getValue();
          // Sort consistently
          cities.sort((a, b) -> a.getPosition().toString().compareTo(b.getPosition().toString()));

          // Check owned metropolises
          List<String> ownedMetros = new ArrayList<>();
          if (Integer.valueOf(pid).equals(mr.getMetropolisOwner(CityImprovement.Track.TRADE)))
            ownedMetros.add("trade");
          if (Integer.valueOf(pid).equals(mr.getMetropolisOwner(CityImprovement.Track.POLITICS)))
            ownedMetros.add("politics");
          if (Integer.valueOf(pid).equals(mr.getMetropolisOwner(CityImprovement.Track.SCIENCE)))
            ownedMetros.add("science");

          for (int j = 0; j < Math.min(cities.size(), ownedMetros.size()); j++) {
            metropolisMap.put(cities.get(j).getPosition(), ownedMetros.get(j));
          }
        }
      }
         // C&K: knights are stored on players, not on intersections, so build a
      // lookup of every knight by position for the client to draw.
      Map<IntersectionCoordinate, KnightPiece> knightMap = new HashMap<>();
      if (ref.getGameSettings().isCitiesAndKnights) {
        for (Player p : ref.getPlayers()) {
          for (KnightPiece k : p.getKnights()) {
            knightMap.put(k.getPosition(), k);
          }
        }
      }
 
      for (Intersection intersection : board.getIntersections().values()) {
        String metro = metropolisMap.get(intersection.getPosition());
        KnightPiece knight = knightMap.get(intersection.getPosition());
        intersections.add(new IntersectionRaw(intersection, ref, playerID, metro,
            knight));
      }
     /* for (Intersection intersection : board.getIntersections().values()) {
        String metro = metropolisMap.get(intersection.getPosition());
        intersections.add(new IntersectionRaw(intersection, ref, playerID, metro));
      }*/
      paths = new ArrayList<>();
      for (Path path : board.getPaths().values()) {
        paths.add(new PathRaw(ref.getReadOnlyReferee(), path, playerID));
      }

      tiles = new ArrayList<>();
      for (Tile tile : board.getTiles()) {
        tiles.add(new TileRaw(tile));
      }
    }
  }

  public static class PathRaw {
    private IntersectionCoordinate start;
    private IntersectionCoordinate end;
    private RoadRaw road;
    private boolean canBuildRoad;

    public PathRaw(Referee ref, Path path, int playerID) {
      start = path.getStart().getPosition();
      end = path.getEnd().getPosition();
      road = path.getRoad() != null ? new RoadRaw(path.getRoad()) : null;
      canBuildRoad = ref.getGameStatus() == GameStatus.SETUP ? path
          .canPlaceSetupRoad(ref.getSetup())
          : path.canPlaceRoad(ref
              .getPlayerByID(playerID));
    }

  }

  private static class RoadRaw {
    private int player;

    public RoadRaw(Road road) {
      player = road.getPlayer().getID();
    }
  }

  private static class BuildingRaw implements Building {

    private int player;
    private final String type;
    private final String metropolis;

    BuildingRaw(Building building, String metropolis) {
      if (building.getPlayer() != null) {
        player = building.getPlayer().getID();
      }
      type = building.getClass().getSimpleName().toLowerCase();
      this.metropolis = metropolis;
    }

    @Override
    public Map<Integer, Map<Resource, Integer>> collectResource(
        Resource resource) {
      assert false; // Should never be called!
      return null;
    }

    @Override
    public Player getPlayer() {
      assert false; // Should never be called!
      return null;
    }

  }
  // C&K: a knight standing on an intersection (level is 1=Basic .. 3=Mighty)
  private static class KnightRaw {
    private final int player;
    private final int level;
    private final boolean active;
 
    KnightRaw(KnightPiece k) {
      player = k.getOwnerID();
      level = k.getStrength();
      active = k.isActive();
    }
  }

  private static class IntersectionRaw {

    private final BuildingRaw building;
    private final Port port;
    private final IntersectionCoordinate coordinate;
    private final boolean canBuildSettlement;

    IntersectionRaw(Intersection i, Referee ref, int playerID, String metropolis) {
      building = i.getBuilding() != null ? new BuildingRaw(i.getBuilding(), metropolis)
          : null;
      port = i.getPort();
      coordinate = i.getPosition();
      canBuildSettlement = i.canPlaceSettlement(ref, playerID);
    }

  }

  private static class TileRaw {
    private final HexCoordinate hexCoordinate;
    private final TileType type;
    private final boolean hasRobber;
    private final int number;
    private final List<IntersectionCoordinate> portLocations;
    private final Resource portType;

    public TileRaw(BoardTile tile) {
      hexCoordinate = tile.getCoordinate();
      type = tile.getType();
      hasRobber = tile.hasRobber();
      number = tile.getRollNumber();
      portLocations = tile.getPortLocations();
      portType = tile.getPortType();
    }
  }

  private static class PublicPlayerRaw {
    private String name;
    private int id;
    private String color;
    private int numSettlements;
    private int numCities;
    private int numPlayedKnights;
    private int numRoads;
    private boolean longestRoad;
    private boolean largestArmy;
    private int victoryPoints;
    private double numResourceCards;
    private int numDevelopmentCards;
    private Map<Resource, Double> rates;
    // C&K fields (null/0 when not in C&K mode)
    private Integer numKnights;
    private Integer activeKnightStrength;
    private Integer defenderPoints;
    private Integer cityWalls;
    private Map<String, Integer> cityImprovements;
    private Integer numProgressCards;

    public PublicPlayerRaw(Player p, Referee r) {
      name = p.getName();
      id = p.getID();
      color = p.getColor();
      numSettlements = p.numSettlements();
      numCities = p.numCities();
      numPlayedKnights = p.numPlayedKnights();
      numRoads = p.numRoads();
      longestRoad = r.hasLongestRoad(p.getID());
      largestArmy = r.hasLargestArmy(p.getID());
      victoryPoints = r.getNumPublicPoints(p.getID());
      rates = r.getBankRates(p.getID());
      numResourceCards = p.getNumResourceCards();
      numDevelopmentCards = p.getNumDevelopmentCards();
      if (r.getGameSettings().isCitiesAndKnights) {
        numKnights = p.getKnights().size();
        activeKnightStrength = p.getActiveKnightStrength();
        defenderPoints = p.getDefenderPoints();
        cityWalls = p.getCityWallCount();
        cityImprovements = new HashMap<>();
        cityImprovements.put("trade", p.getCityImprovement().getLevel(
            edu.brown.cs.catan.CityImprovement.Track.TRADE));
        cityImprovements.put("politics", p.getCityImprovement().getLevel(
            edu.brown.cs.catan.CityImprovement.Track.POLITICS));
        cityImprovements.put("science", p.getCityImprovement().getLevel(
            edu.brown.cs.catan.CityImprovement.Track.SCIENCE));
        numProgressCards = p.getProgressCards().size();
      }
    }

  }

  private static class GameStatsRaw {
    private int[] rolls;
    private int turn;

    GameStatsRaw(Referee ref) {
      this.rolls = ref.getGameStats().getRollsArray();
      this.turn = ref.getTurn().getTurnNum();
    }

  }

  private static class FollowUpActionRaw {

    private String actionName;
    private Object actionData;

    public FollowUpActionRaw(FollowUpAction followUp) {
      actionName = followUp.getID();
      actionData = followUp.getData();
    }
  }

  // C&K: Barbarian track state for JSON serialization
  private static class BarbarianTrackRaw {
    private int position;
    private int trackLength;
    private int attackCount;

    public BarbarianTrackRaw(BarbarianTrack track) {
      this.position = track.getPosition();
      this.trackLength = edu.brown.cs.catan.Settings.BARBARIAN_TRACK_LENGTH;
      this.attackCount = track.getAttackCount();
    }
  }
}
