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
import edu.brown.cs.board.TileStructure;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Tech;
import edu.brown.cs.catan.UnitType;
import edu.brown.cs.catan.WarState;
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
    private String winReason;
    private Boolean draw;
    private String drawReason;
    private Integer longestTurnPlayer;
    private Integer longestTurnSeconds;
    private Hand hand;
    private BoardRaw board;
    private int currentTurn;
    private FollowUpActionRaw followUp;
    private Collection<PublicPlayerRaw> players;
    private GameSettings settings;
    private GameStatsRaw stats;
    // Cursed Catan fields (null when not in Cursed mode)
    private Integer industryTrack;
    private Boolean greatWarTriggered;
    private WarRaw war;
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
          ref.getGameSettings().isCitiesAndKnights,
          ref.getGameSettings().isCursedCatan, ref.isWarActive());
      this.board = new BoardRaw(ref, ref.getBoard(),
          playerID);
      this.turnOrder = (ref.getGameStatus() != GameStatus.WAITING) ? ref
          .getTurnOrder() : null;
      this.winner = ref.getWinner() != null ? ref.getWinner().getID() : null;
      if (ref.getWinner() != null) {
        if (ref.holdsExodia(ref.getWinner())) {
          this.winReason = "exodia";
        } else if (ref.isDraw()) {
          this.winReason = "draw";
        } else if (ref.isWarActive()
            && ref.getNumTotalPoints(ref.getWinner().getID()) < ref
                .getGameSettings().winningPointCount) {
          // everyone else was eliminated
          this.winReason = "survivor";
        } else {
          this.winReason = "points";
        }
      }
      if (ref.isDraw()) {
        this.draw = true;
        this.drawReason = ref.getDrawReason();
      }
      if (ref.getGameSettings().isCursedCatan
          && ref.getLongestTurnPlayer() >= 0) {
        this.longestTurnPlayer = ref.getLongestTurnPlayer();
        this.longestTurnSeconds = (int) (ref.getLongestTurnMillis() / 1000);
      }
      this.followUp = ref.getNextFollowUp(playerID) != null ? new FollowUpActionRaw(
          ref.getNextFollowUp(playerID)) : null;
      this.players = new ArrayList<>();
      this.settings = ref.getGameSettings();
      this.stats = new GameStatsRaw(ref);
      for (Player p : ref.getPlayers()) {
        players.add(new PublicPlayerRaw(p, ref.getReadOnlyReferee()));
      }
      // Cursed Catan state
      if (ref.getGameSettings().isCursedCatan) {
        this.industryTrack = ref.getIndustryTrack();
        this.greatWarTriggered = ref.isGreatWarTriggered();
        if (ref.isWarActive()) {
          this.war = new WarRaw(ref, playerID);
        }
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
    // Cursed Catan fields (null when not in Cursed mode)
    private Boolean canBuildFactory;
    private Boolean canBuildCapitol;
    private Boolean canBuildFortress;
    private Boolean canBuildTestSite;
    private Integer socialCredit;
    private Integer numBombs;

    public Hand(Player player, boolean isCK, boolean isCursed, boolean war) {
      resources = player.getResources();
      devCards = player.getDevCards();
      canBuildRoad = player.canBuildRoad();
      canBuildSettlement = player.canBuildSettlement();
      canBuildCity = player.canBuildCity();
      canBuyDevCard = player.canBuyDevelopmentCard();
      if (isCursed) {
        canBuildFactory = player.canBuildFactory();
        canBuildCapitol = player.canBuildCapitol();
        canBuildFortress = player.canBuildFortress();
        canBuildTestSite = player.canBuildTestSite();
        socialCredit = player.getSocialCredit();
        if (war) {
          numBombs = player.numBombs();
        }
      }
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
    // Cursed Catan: ruins, occupation and damage (omitted when not relevant)
    private Boolean ruined;
    private String occupiedBy;
    private Integer hp;
    private Integer maxHp;

    BuildingRaw(Building building, String metropolis) {
      if (building.getPlayer() != null) {
        player = building.getPlayer().getID();
      }
      type = building.getClass().getSimpleName().toLowerCase();
      this.metropolis = metropolis;
      if (building.isRuined()) {
        ruined = true;
      }
      if (building.getOccupier() != null) {
        occupiedBy = building.getOccupier().toString();
      }
      if (building.getMaxHp() > 0
          && (building.isRuined() || building.getHp() < building.getMaxHp())) {
        hp = building.getHp();
        maxHp = building.getMaxHp();
      }
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
  private final boolean canAct;

  KnightRaw(KnightPiece k, int turn) {
    player = k.getOwnerID();
    level = k.getStrength();
    active = k.isActive();
    canAct = k.isActive() && !k.isRestrictedOn(turn);
  }
}

  private static class IntersectionRaw {

    private final BuildingRaw building;
     private final KnightRaw knight; // null (omitted from JSON) if no knight
    private final Port port;
    private final IntersectionCoordinate coordinate;
    private final boolean canBuildSettlement;

    IntersectionRaw(Intersection i, Referee ref, int playerID, String metropolis, KnightPiece knightPiece) {
      // in IntersectionRaw's constructor:
knight = knightPiece != null
    ? new KnightRaw(knightPiece, ref.getTurn().getTurnNum()) : null;
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
    // Cursed Catan: fortresses and test sites (null when there are none)
    private final List<StructureRaw> structures;

    public TileRaw(BoardTile tile) {
      hexCoordinate = tile.getCoordinate();
      type = tile.getType();
      hasRobber = tile.hasRobber();
      number = tile.getRollNumber();
      portLocations = tile.getPortLocations();
      portType = tile.getPortType();
      List<StructureRaw> built = new ArrayList<>();
      if (tile instanceof Tile) {
        for (TileStructure s : ((Tile) tile).getStructures()) {
          built.add(new StructureRaw(s));
        }
      }
      structures = built.isEmpty() ? null : built;
    }
  }

  private static class StructureRaw {
    private final String type;
    private final int player;
    private final int hitPoints;

    StructureRaw(TileStructure s) {
      type = s.getKind().name().toLowerCase();
      player = s.getOwnerID();
      hitPoints = s.getHitPoints();
    }
  }

  /**
   * The Great War as the client sees it. Absent outside the war.
   */
  private static class WarRaw {
    private String myFaction;
    private List<FactionRaw> factions = new ArrayList<>();
    private List<UnitsRaw> units = new ArrayList<>();
    private Map<String, Integer> toDeploy;
    private Integer nuclearWinter;
    private Integer nuclearWinterRollNeeded;
    private String sanctioned;
    private List<TechRaw> techTree = new ArrayList<>();

    private static class TechRaw {
      private String id;
      private String label;
      private String branch;
      private String requires;
    }

    private static class FactionRaw {
      private String name;
      private int morale;
      private int numUnits;
      private List<Integer> players;
      private List<String> tech = new ArrayList<>();
      private int researchPoints;
      private int researchTotal;
    }

    private static class UnitsRaw {
      private int x;
      private int y;
      private int z;
      private String faction;
      private Map<String, Integer> units = new java.util.LinkedHashMap<>();
      private boolean entrenched;
      private Map<String, Integer> canMove = new java.util.LinkedHashMap<>();
    }

    public WarRaw(Referee ref, int playerID) {
      WarState war = ref.getWarState();
      myFaction = String.valueOf(war.getFaction(playerID));
      for (Tech t : Tech.values()) {
        TechRaw tr = new TechRaw();
        tr.id = t.getId();
        tr.label = t.getLabel();
        tr.branch = t.getBranch();
        tr.requires = t.getPrerequisite() == null ? null : t.getPrerequisite()
            .getId();
        techTree.add(tr);
      }
      for (Faction f : war.getFactionList()) {
        FactionRaw fr = new FactionRaw();
        fr.name = f.toString();
        fr.morale = war.getMorale(f);
        fr.numUnits = war.getUnitCount(f);
        fr.players = war.getPlayersOf(f);
        for (Tech t : Tech.values()) {
          if (war.hasTech(f, t)) {
            fr.tech.add(t.getId());
          }
        }
        fr.researchPoints = war.getBankedPoints(f);
        fr.researchTotal = war.getResearchTotal(f);
        factions.add(fr);
      }
      for (Map.Entry<HexCoordinate, Map<Faction, Map<UnitType, Integer>>> t : war
          .getUnits().entrySet()) {
        for (Map.Entry<Faction, Map<UnitType, Integer>> f : t.getValue()
            .entrySet()) {
          UnitsRaw u = new UnitsRaw();
          u.x = t.getKey().getX();
          u.y = t.getKey().getY();
          u.z = t.getKey().getZ();
          u.faction = f.getKey().toString();
          u.entrenched = war.isEntrenched(t.getKey(), f.getKey());
          for (UnitType ut : UnitType.values()) {
            int ready = war.getReadyToMove(t.getKey(), f.getKey(), ut);
            if (ready > 0) {
              u.canMove.put(ut.toString(), ready);
            }
          }
          for (Map.Entry<UnitType, Integer> c : f.getValue().entrySet()) {
            u.units.put(c.getKey().toString(), c.getValue());
          }
          units.add(u);
        }
      }
      Map<UnitType, Integer> owed = ref.getUnitsToDeploy(playerID);
      if (!owed.isEmpty()) {
        toDeploy = new java.util.LinkedHashMap<>();
        for (Map.Entry<UnitType, Integer> e : owed.entrySet()) {
          toDeploy.put(e.getKey().toString(), e.getValue());
        }
      }
      nuclearWinter = war.getNuclearWinter();
      nuclearWinterRollNeeded = war.rollNeededToAdvance();
      if (war.getSanctioned() != null) {
        sanctioned = war.getSanctioned().toString();
      }
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
    // Cursed Catan fields (null when not in Cursed mode)
    private Integer numFactories;
    private Integer numCapitols;
    private Integer numFortresses;
    private Integer numTestSites;
    private Boolean longestTurn;
    private String faction;
    private Boolean eliminated;

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
      if (r.getGameSettings().isCursedCatan) {
        numFactories = p.numFactories();
        numCapitols = p.numCapitols();
        numFortresses = p.numFortresses();
        numTestSites = p.numTestSites();
        longestTurn = r.getLongestTurnPlayer() == p.getID();
        if (r.isWarActive()) {
          faction = String.valueOf(r.getFaction(p.getID()));
          eliminated = r.isEliminated(p.getID());
        }
      }
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
