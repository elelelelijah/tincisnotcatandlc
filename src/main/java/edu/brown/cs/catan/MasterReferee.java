package edu.brown.cs.catan;

import static edu.brown.cs.catan.Settings.CITY_POINT_VAL;
import static edu.brown.cs.catan.Settings.INITIAL_CITIES;
import static edu.brown.cs.catan.Settings.INITIAL_SETTLEMENTS;
import static edu.brown.cs.catan.Settings.LARGEST_ARMY_POINT_VAL;
import static edu.brown.cs.catan.Settings.LONGEST_ROAD_POINT_VAL;
import static edu.brown.cs.catan.Settings.SETTLEMENT_POINT_VAL;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;

import edu.brown.cs.actions.DeployUnits;
import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.actions.GoldResource;
import edu.brown.cs.board.Board;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Building;
import edu.brown.cs.board.Capitol;
import edu.brown.cs.board.City;
import edu.brown.cs.board.Factory;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.gamestats.CatanStats;
import edu.brown.cs.gamestats.GameStats;

import java.util.AbstractMap;

/**
 * An implementation of Referee that can read and write data. There should be
 * one MasterReferee per game. Contains all Catan game data.
 *
 */
public class MasterReferee implements Referee {

  private final Board _board;
  private final Map<Integer, Player> _players;
  private final List<Integer> _turnOrder;
  private Turn _turn;
  // Cursed Catan: the March of Industry (1 to 12) and whether any coal has
  // been spent during the current turn
  private int _industryTrack = 1;
  private boolean _coalSpentThisTurn = false;
  // Cursed Catan: the current player used a Spy Network this turn
  private boolean _spyNetworkActive = false;
  // Cursed Catan: Longest Turn and drawn games
  private final LongSupplier _clock;
  private int _longestTurnPlayer = -1;
  private long _longestTurnMillis = 0;
  private String _drawReason = null;
  // Cursed Catan: the Great War
  private final Random _random;
  private WarState _war = null;
  private boolean _warJustStarted = false;
  private final Map<Integer, Integer> _coalOwed = new HashMap<>();
  private final Map<Integer, Map<UnitType, Integer>> _toDeploy = new HashMap<>();
  private final Set<Integer> _eliminated = new HashSet<>();
  private final Bank _bank;
  private final List<DevelopmentCard> _devCardDeck;
  private final GameSettings _gameSettings;
  private Player _largestArmy = null;
  private Player _longestRoad = null;
  private GameStatus _gameStatus;
  private final Setup _setup;
  private GameStats _gameStats;

  // Cities & Knights state
  private BarbarianTrack _barbarianTrack;
  private List<ProgressCard> _tradeDeck;
  private List<ProgressCard> _politicsDeck;
  private List<ProgressCard> _scienceDeck;
  private int _merchantOwner = -1; // playerID who has the merchant, -1 if none
  private HexCoordinate _merchantHex = null; // hex where merchant is placed
  private int[] _overriddenDice = null; // Alchemist: pre-set dice values
  private Map<CityImprovement.Track, Integer> _metropolisOwners;

  /**
   * Creates a MasterReferee. Contains all Catan game data with default game
   * settings.
   */
  public MasterReferee() {
    this(new GameSettings());
  }

  /**
   * Creates a MasterReferee. Contains all Catan game data with inputted
   * GameSettings.
   *
   * @param gameSettings
   *                     A GameSettings object to use as the gameSettings.
   */
  public MasterReferee(GameSettings gameSettings) {
    this(gameSettings, System::currentTimeMillis);
  }

  /**
   * Creates a referee that reads the time from the given clock, so that tests
   * can control how long turns take (Longest Turn).
   *
   * @param gameSettings
   *          The game settings.
   * @param clock
   *          Supplies the current time in milliseconds.
   */
  public MasterReferee(GameSettings gameSettings, LongSupplier clock) {
    this(gameSettings, clock, new Random());
  }

  /**
   * Creates a referee with a given clock and source of randomness, so that
   * tests can make the war's tie-break dice predictable.
   *
   * @param gameSettings
   *          The game settings.
   * @param clock
   *          Supplies the current time in milliseconds.
   * @param random
   *          Used for the dice that break ties when factions are handed out.
   */
  public MasterReferee(GameSettings gameSettings, LongSupplier clock,
      Random random) {
    _random = random;
    _clock = clock;
    _gameSettings = gameSettings;
    _board = new Board(_gameSettings);
    _players = new HashMap<Integer, Player>();
    _turnOrder = initializeTurnOrder(_gameSettings.numPlayers);
    _bank = initializeBank(_gameSettings.isDynamic);
    _devCardDeck = initializeDevDeck();
    _turn = new Turn(1, Collections.emptyMap(), _clock);
    _gameStatus = GameStatus.WAITING;
    _setup = new Setup(getSetupOrder());
    _gameStats = CatanStats.getGameStatsObject();
    initCitiesAndKnights();
  }

  private void initCitiesAndKnights() {
    if (_gameSettings.isCitiesAndKnights) {
      _barbarianTrack = new BarbarianTrack();
      _tradeDeck = ProgressCard.createTradeDeck();
      _politicsDeck = ProgressCard.createPoliticsDeck();
      _scienceDeck = ProgressCard.createScienceDeck();
      _metropolisOwners = new HashMap<>();
    }
  }

  /**
   * Returns the BarbarianTrack, or null if not in C&K mode.
   */
  public BarbarianTrack getBarbarianTrack() {
    return _barbarianTrack;
  }

  /**
   * Draws a progress card from the specified category deck.
   * Returns null if the deck is empty.
   */
  public ProgressCard drawProgressCard(ProgressCard.Category category) {
    List<ProgressCard> deck;
    switch (category) {
      case TRADE:
        deck = _tradeDeck;
        break;
      case POLITICS:
        deck = _politicsDeck;
        break;
      case SCIENCE:
        deck = _scienceDeck;
        break;
      default:
        return null;
    }
    if (deck == null || deck.isEmpty()) {
      return null;
    }
    return deck.remove(0);
  }

  @Override
  public int getMerchantOwner() {
    return _merchantOwner;
  }

  @Override
  public HexCoordinate getMerchantHex() {
    return _merchantHex;
  }

  @Override
  public void setMerchant(int playerID, HexCoordinate hex) {
    _merchantOwner = playerID;
    _merchantHex = hex;
  }

  @Override
  public void setOverriddenDice(int red, int white, int event) {
    _overriddenDice = new int[] { red, white, event };
  }

  @Override
  public int[] consumeOverriddenDice() {
    int[] result = _overriddenDice;
    _overriddenDice = null;
    return result;
  }

  private List<Integer> getSetupOrder() {
    List<Integer> toRet = new ArrayList<>();
    toRet.addAll(_turnOrder);
    List<Integer> copyTurnOrder = new ArrayList<>(_turnOrder);
    Collections.reverse(copyTurnOrder);
    toRet.addAll(copyTurnOrder);
    return toRet;
  }

  @Override
  public Setup getSetup() {
    return _setup;
  }

  private List<Integer> initializeTurnOrder(int numFullPlayers) {
    List<Integer> toReturn = new ArrayList<>();
    for (int i = 0; i < _gameSettings.numPlayers; i++) {
      toReturn.add(i);
    }
    Collections.shuffle(toReturn);
    return toReturn;
  }

  @Override
  public void startNextTurn() {
    recordLongestTurn();
    _spyNetworkActive = false;
    int turnNum = _turn.getTurnNum();
    Player nextPlayer = _players.get(_turnOrder.get(turnNum
        % _gameSettings.numPlayers));
    // Eliminated players take no more turns
    for (int tries = 0; _war != null && tries < _gameSettings.numPlayers
        && isEliminated(nextPlayer.getID()); tries++) {
      turnNum++;
      nextPlayer = _players.get(_turnOrder.get(turnNum
          % _gameSettings.numPlayers));
    }

    if (_gameStatus == GameStatus.PROGRESS) {
      _turn = new Turn(turnNum + 1, nextPlayer.getDevCards(), _clock);
      _turn.setTurnPlayerID(nextPlayer.getID());
    } else {
      _turn = new Turn(turnNum + 1, Collections.emptyMap(), _clock);
    }
    checkBarrenBoard();
  }

  /**
   * Cursed Catan: when a turn ends, the player who took the longest turn so
   * far holds the Longest Turn card (-2 VP). A longer turn takes it; a tie
   * leaves it where it is. Time spent waiting for other players does not
   * count.
   */
  private void recordLongestTurn() {
    if (!_gameSettings.isCursedCatan || _gameStatus != GameStatus.PROGRESS) {
      return;
    }
    Player ending = currentPlayer();
    if (ending == null) {
      return;
    }
    _turn.setTurnPlayerID(ending.getID());
    long active = _turn.getActiveMillis();
    if (_longestTurnPlayer < 0 || active > _longestTurnMillis) {
      _longestTurnPlayer = ending.getID();
      _longestTurnMillis = active;
    }
  }

  /**
   * Cursed Catan: if every land tile is Desert or Wasteland, the game is a
   * draw.
   */
  private void checkBarrenBoard() {
    if (_gameSettings.isCursedCatan && _gameStatus == GameStatus.PROGRESS
        && Board.isAllBarren(_board.getTiles())) {
      declareDraw("Every land tile is Desert or Wasteland");
    }
  }

  @Override
  public int getLongestTurnPlayer() {
    return _gameSettings.isCursedCatan ? _longestTurnPlayer : -1;
  }

  @Override
  public long getLongestTurnMillis() {
    return _gameSettings.isCursedCatan ? _longestTurnMillis : 0;
  }

  @Override
  public void declareDraw(String reason) {
    if (_gameSettings.isCursedCatan && _drawReason == null) {
      _drawReason = reason == null ? "The game ended in a draw" : reason;
    }
  }

  private boolean anyoneHoldsExodia() {
    for (Player p : _players.values()) {
      if (holdsExodia(p)) {
        return true;
      }
    }
    return false;
  }

  private List<Player> playersAtWinningScore() {
    List<Player> atScore = new ArrayList<>();
    for (Player p : _players.values()) {
      if (getNumTotalPoints(p.getID()) >= _gameSettings.winningPointCount) {
        atScore.add(p);
      }
    }
    return atScore;
  }

  @Override
  public boolean isDraw() {
    if (!_gameSettings.isCursedCatan || anyoneHoldsExodia()) {
      return false;
    }
    return _drawReason != null || allEliminated()
        || playersAtWinningScore().size() >= 2;
  }

  @Override
  public String getDrawReason() {
    if (!isDraw()) {
      return null;
    }
    if (_drawReason != null) {
      return _drawReason;
    }
    return allEliminated() ? "All players are eliminated"
        : "Two players reached the winning score at the same time";
  }

  @Override
  public Player getDrawWinner() {
    if (!isDraw()) {
      return null;
    }
    Collection<Player> candidates = (_drawReason != null || allEliminated()) ? _players
        .values() : playersAtWinningScore();
    return breakTie(candidates);
  }

  /**
   * The draw tie-break: the most victory points wins, then the highest social
   * credit. If there is still a tie, nobody wins.
   *
   * @param candidates
   *          The players in the running.
   * @return The winner, or null if nobody wins.
   */
  private Player breakTie(Collection<Player> candidates) {
    List<Player> best = new ArrayList<>();
    int bestPoints = Integer.MIN_VALUE;
    for (Player p : candidates) {
      int points = getNumTotalPoints(p.getID());
      if (points > bestPoints) {
        bestPoints = points;
        best.clear();
        best.add(p);
      } else if (points == bestPoints) {
        best.add(p);
      }
    }
    if (best.size() == 1) {
      return best.get(0);
    }
    List<Player> bestCredit = new ArrayList<>();
    int credit = Integer.MIN_VALUE;
    for (Player p : best) {
      int c = p.getSocialCredit();
      if (c > credit) {
        credit = c;
        bestCredit.clear();
        bestCredit.add(p);
      } else if (c == credit) {
        bestCredit.add(p);
      }
    }
    return bestCredit.size() == 1 ? bestCredit.get(0) : null;
  }

  @Override
  public Player currentPlayer() {
    if (_players.size() != _gameSettings.numPlayers) {
      return null;
    }
    if (_gameStatus == GameStatus.PROGRESS) {
      return _players.get(_turnOrder.get((_turn.getTurnNum() - 1)
          % _gameSettings.numPlayers));
    } else {
      return getPlayerByID(_setup.getCurrentPlayerID());
    }
  }

  @Override
  public DevelopmentCard getDevCard() {
    return _devCardDeck.remove(0);
  }

  @Override
  public void addFollowUp(Collection<FollowUpAction> actions) {
    _turn.addFollowUp(actions);
  }

  @Override
  public FollowUpAction getNextFollowUp(int playerID) {
    return _turn.getNextFollowUp(playerID);
  }

  private Bank initializeBank(boolean isSmart) {
    if (isSmart) {
      return new DynamicBank();
    } else {
      return new SimpleBank();
    }
  }

  private List<DevelopmentCard> initializeDevDeck() {
    if (_gameSettings.isCursedCatan) {
      return initializeCursedDevDeck();
    }
    List<DevelopmentCard> toReturn = new ArrayList<>();
    for (int i = 0; i < Settings.NUM_KNIGHTS; i++) {
      toReturn.add(DevelopmentCard.KNIGHT);
    }
    for (int i = 0; i < Settings.NUM_POINTS; i++) {
      toReturn.add(DevelopmentCard.POINT);
    }
    for (int i = 0; i < Settings.NUM_ROADBUILDING; i++) {
      toReturn.add(DevelopmentCard.ROAD_BUILDING);
    }
    for (int i = 0; i < Settings.NUM_YOP; i++) {
      toReturn.add(DevelopmentCard.YEAR_OF_PLENTY);
    }
    for (int i = 0; i < Settings.NUM_MONOPOLY; i++) {
      toReturn.add(DevelopmentCard.MONOPOLY);
    }
    Collections.shuffle(toReturn);
    return toReturn;
  }

  /**
   * The 44-card Cursed Catan development deck: 16 Knights, 5 Victory Points,
   * 2 each of Road Building, Year of Plenty, Monopoly and Spy Network, 3
   * Border Raids, the seven held cards and the five pieces of Exodia.
   */
  private List<DevelopmentCard> initializeCursedDevDeck() {
    List<DevelopmentCard> toReturn = new ArrayList<>();
    addCards(toReturn, DevelopmentCard.KNIGHT, Settings.CURSED_NUM_KNIGHTS);
    addCards(toReturn, DevelopmentCard.POINT, Settings.NUM_POINTS);
    addCards(toReturn, DevelopmentCard.ROAD_BUILDING, Settings.NUM_ROADBUILDING);
    addCards(toReturn, DevelopmentCard.YEAR_OF_PLENTY, Settings.NUM_YOP);
    addCards(toReturn, DevelopmentCard.MONOPOLY, Settings.NUM_MONOPOLY);
    addCards(toReturn, DevelopmentCard.SPY_NETWORK,
        Settings.CURSED_NUM_SPY_NETWORK);
    addCards(toReturn, DevelopmentCard.BORDER_RAID,
        Settings.CURSED_NUM_BORDER_RAID);
    toReturn.add(DevelopmentCard.MAO_ZEDONG);
    toReturn.add(DevelopmentCard.XI_JINPING);
    toReturn.add(DevelopmentCard.BING_CHILLING);
    toReturn.add(DevelopmentCard.HALF_DOLLAR);
    toReturn.add(DevelopmentCard.NOTHING_HAPPENED);
    toReturn.add(DevelopmentCard.TAIWAN);
    toReturn.add(DevelopmentCard.TROLLING);
    toReturn.addAll(DevelopmentCard.EXODIA_PIECES);
    Collections.shuffle(toReturn);
    return toReturn;
  }

  private static void addCards(List<DevelopmentCard> deck,
      DevelopmentCard card, int count) {
    for (int i = 0; i < count; i++) {
      deck.add(card);
    }
  }

  @Override
  public boolean ignoresFortresses() {
    return _gameSettings.isCursedCatan && _spyNetworkActive;
  }

  @Override
  public void activateSpyNetwork() {
    if (_gameSettings.isCursedCatan) {
      _spyNetworkActive = true;
    }
  }

  @Override
  public boolean holdsExodia(Player player) {
    if (!_gameSettings.isCursedCatan || player == null) {
      return false;
    }
    Map<DevelopmentCard, Integer> hand = player.getDevCards();
    for (DevelopmentCard piece : DevelopmentCard.EXODIA_PIECES) {
      Integer held = hand.get(piece);
      if (held == null || held < 1) {
        return false;
      }
    }
    return true;
  }

  // ------------------------------------------------------------ Great War

  @Override
  public boolean isWarActive() {
    return _war != null;
  }

  @Override
  public WarState getWarState() {
    return _war;
  }

  @Override
  public Faction getFaction(int playerID) {
    return _war == null ? null : _war.getFaction(playerID);
  }

  /**
   * The Great War begins (Industry track at its end). Plays every Knight, hands
   * the players to factions, swaps coal tiles for uranium, takes the coal,
   * removes the robber and works out the forces each player deploys.
   */
  private void beginGreatWar() {
    // 1. Every Knight in hand is played with no effect (Longest Army follows)
    for (Player p : _players.values()) {
      int knights = p.getDevCards().get(DevelopmentCard.KNIGHT);
      for (int i = 0; i < knights; i++) {
        p.playDevelopmentCard(DevelopmentCard.KNIGHT);
      }
    }
    // 2. Factions by victory point rank; ties are broken by dice
    List<Player> ranked = FactionAssigner.rank(_players.values(),
        p -> getNumTotalPoints(p.getID()), _random);
    _war = new WarState(FactionAssigner.assign(ranked));
    // 5. Coal tiles become uranium (chits and buildings stay); coal is
    // discarded, and each coal is worth one resource of choice
    for (Tile t : _board.getTiles()) {
      if (t.getType() == TileType.COAL) {
        t.setType(TileType.URANIUM);
      }
    }
    for (Player p : _players.values()) {
      int coal = (int) Math.floor(p.getResources().get(Resource.COAL));
      if (coal > 0) {
        p.removeResource(Resource.COAL, coal, _bank);
        _coalOwed.put(p.getID(), coal);
      }
    }
    // 6. No robber in the war
    _board.removeRobber();
    // 8. Forces: 1 infantry per city, 1 artillery per factory, both for a
    // capitol; a player with no city or capitol gets 1 infantry; and 1
    // infantry per Knight played
    for (Player p : _players.values()) {
      int infantry = 0;
      int artillery = 0;
      boolean hasCity = false;
      for (Tile t : _board.getTiles()) {
        for (Intersection i : t.getIntersections()) {
          Building b = i.getBuilding();
          if (b == null || !b.getPlayer().equals(p) || !countedOnce(i, t)) {
            continue;
          }
          if (b instanceof Capitol) {
            infantry++;
            artillery++;
            hasCity = true;
          } else if (b instanceof City) {
            infantry++;
            hasCity = true;
          } else if (b instanceof Factory) {
            artillery++;
          }
        }
      }
      if (!hasCity) {
        infantry++;
      }
      infantry += p.numPlayedKnights();
      Map<UnitType, Integer> owed = new java.util.EnumMap<>(UnitType.class);
      owed.put(UnitType.INFANTRY, infantry);
      if (artillery > 0) {
        owed.put(UnitType.ARTILLERY, artillery);
      }
      _toDeploy.put(p.getID(), owed);
    }
    _warJustStarted = true;
  }

  /**
   * An intersection touches up to three tiles; count its building only from
   * the first tile that contains it.
   */
  private boolean countedOnce(Intersection i, Tile current) {
    for (Tile t : _board.getTiles()) {
      if (t.getIntersections().contains(i)) {
        return t == current;
      }
    }
    return true;
  }

  @Override
  public List<Collection<FollowUpAction>> takeWarStartFollowUps() {
    List<Collection<FollowUpAction>> groups = new ArrayList<>();
    if (!_warJustStarted) {
      return groups;
    }
    _warJustStarted = false;
    Collection<FollowUpAction> coal = new ArrayList<>();
    for (Map.Entry<Integer, Integer> e : _coalOwed.entrySet()) {
      coal.add(new GoldResource(e.getKey(), e.getValue(), "Coal exchange"));
    }
    _coalOwed.clear();
    if (!coal.isEmpty()) {
      groups.add(coal);
    }
    Collection<FollowUpAction> deploy = new ArrayList<>();
    for (Player p : _players.values()) {
      if (_toDeploy.containsKey(p.getID())) {
        List<HexCoordinate> valid = new ArrayList<>();
        for (Tile t : _board.getTiles()) {
          if (t.getType() != TileType.SEA && DeployUnits.touchesBuildingOf(t, p)) {
            valid.add(t.getCoordinate());
          }
        }
        deploy.add(new DeployUnits(p.getID(), _toDeploy.get(p.getID()), valid));
      }
    }
    if (!deploy.isEmpty()) {
      groups.add(deploy);
    }
    return groups;
  }

  @Override
  public Map<UnitType, Integer> getUnitsToDeploy(int playerID) {
    Map<UnitType, Integer> owed = _toDeploy.get(playerID);
    return owed == null ? Collections.<UnitType, Integer> emptyMap()
        : Collections.unmodifiableMap(owed);
  }

  @Override
  public void markDeployed(int playerID) {
    _toDeploy.remove(playerID);
  }

  @Override
  public boolean hasTech(int playerID, Tech tech) {
    return _war != null && _war.getFaction(playerID) != null
        && _war.hasTech(_war.getFaction(playerID), tech);
  }

  @Override
  public boolean isSanctioned(int playerID) {
    return _war != null && _war.getSanctioned() != null
        && _war.getFaction(playerID) == _war.getSanctioned();
  }

  // ------------------------------------------------------------ elimination

  /**
   * A player is still in the war while one of their buildings is standing and
   * touches a tile that is neither sea, desert nor wasteland.
   */
  private boolean hasLivingBuilding(Player player) {
    for (Tile t : _board.getTiles()) {
      TileType type = t.getType();
      if (type == TileType.SEA || type == TileType.DESERT
          || type == TileType.WASTELAND) {
        continue;
      }
      for (Intersection i : t.getIntersections()) {
        Building b = i.getBuilding();
        if (b != null && b.getPlayer().equals(player) && !b.isRuined()) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public boolean isEliminated(int playerID) {
    if (_war == null) {
      return false;
    }
    if (_eliminated.contains(playerID)) {
      return true;
    }
    Player p = getPlayerByID(playerID);
    return p != null && !hasLivingBuilding(p);
  }

  @Override
  public void eliminate(int playerID) {
    if (_war != null) {
      _eliminated.add(playerID);
    }
  }

  private boolean allEliminated() {
    if (_war == null || _players.isEmpty()) {
      return false;
    }
    for (Player p : _players.values()) {
      if (!isEliminated(p.getID())) {
        return false;
      }
    }
    return true;
  }

  // ------------------------------------------------------------ atomic bombs

  @Override
  public String launchBomb(int playerID, HexCoordinate target, int winterDie,
      IntSupplier unitDie) {
    if (_war == null) {
      throw new IllegalStateException("There is no war.");
    }
    Tile struck = _board.getTile(target);
    if (struck == null || struck.getType() == TileType.SEA) {
      throw new IllegalArgumentException("Choose a land tile.");
    }
    Faction launcher = _war.getFaction(playerID);
    StringBuilder what = new StringBuilder();
    boolean uraniumStrike = struck.getType() == TileType.URANIUM;

    // The Nuclear Winter die is rolled first
    int needed = _war.rollNeededToAdvance();
    if (needed > 0 && winterDie >= needed) {
      what.append(arriveOnWinterPosition(_war.advanceNuclearWinter()));
    }
    // A strike on a uranium tile moves the track one extra step
    if (uraniumStrike && _war.getNuclearWinter() < WarState.NUCLEAR_WINTER_LAST) {
      what.append(arriveOnWinterPosition(_war.advanceNuclearWinter()));
    }

    // The struck tile becomes desert and loses its chit
    struck.setType(TileType.DESERT);
    struck.setRollNumber(0);
    // Adjacent settlements, cities, factories and capitols are ruined, and
    // each costs its faction 1 Morale
    for (Intersection i : struck.getIntersections()) {
      Building b = i.getBuilding();
      if (b != null && !b.isRuined()) {
        b.setRuined(true);
        _war.changeMorale(_war.getFaction(b.getPlayer().getID()), -1);
      }
    }
    // Every unit on the tile, friendly too, is destroyed on a 4-6
    for (Faction f : _war.getFactionList()) {
      for (UnitType type : UnitType.values()) {
        int units = _war.getUnits(target, f, type);
        int destroyed = 0;
        for (int n = 0; n < units; n++) {
          if (unitDie.getAsInt() >= 4) {
            destroyed++;
          }
        }
        _war.removeUnit(target, f, type, destroyed);
      }
    }
    // The first faction to launch is sanctioned for the rest of the game
    if (_war.getSanctioned() == null && launcher != null) {
      _war.sanction(launcher);
      what.append(String.format(" %s is under Sanctions: no maritime trading.",
          launcher));
    }
    return what.toString().trim();
  }

  /**
   * What happens when the Nuclear Winter token reaches a position.
   */
  private String arriveOnWinterPosition(int position) {
    switch (position) {
    case 4:
      convertToDesert(TileType.SHEEP);
      return String.format(" Nuclear Winter reached %d: all sheep tiles become desert.",
          position);
    case 5:
      convertToDesert(TileType.WHEAT);
      return String.format(" Nuclear Winter reached %d: all wheat tiles become desert.",
          position);
    case WarState.NUCLEAR_WINTER_LAST:
      declareDraw("The Nuclear Winter track reached its last level");
      return String.format(" Nuclear Winter reached %d: game over (draw).",
          position);
    default:
      return String.format(" Nuclear Winter reached %d.", position);
    }
  }

  private void convertToDesert(TileType from) {
    for (Tile t : _board.getTiles()) {
      if (t.getType() == from) {
        t.setType(TileType.DESERT);
        t.setRollNumber(0);
      }
    }
  }

  @Override
  public int getIndustryTrack() {
    return _gameSettings.isCursedCatan ? _industryTrack : 0;
  }

  @Override
  public void recordCoalSpent() {
    if (_gameSettings.isCursedCatan) {
      _coalSpentThisTurn = true;
    }
  }

  @Override
  public boolean advanceIndustryTrack() {
    if (!_gameSettings.isCursedCatan || _war != null) {
      // There is no Industry track once the war has begun
      return false;
    }
    if (!_coalSpentThisTurn) {
      return false;
    }
    _coalSpentThisTurn = false;
    if (_industryTrack < Settings.INDUSTRY_TRACK_LENGTH) {
      _industryTrack++;
      if (_industryTrack >= Settings.INDUSTRY_TRACK_LENGTH) {
        beginGreatWar();
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean isGreatWarTriggered() {
    return _gameSettings.isCursedCatan
        && _industryTrack >= Settings.INDUSTRY_TRACK_LENGTH;
  }

  @Override
  public Referee getReadOnlyReferee() {
    return new ReadOnlyReferee(this);
  }

  @Override
  public Turn getTurn() {
    return _turn.getCopy();
  }

  @Override
  public Board getBoard() {
    return _board;
  }

  @Override
  public Player getPlayerByID(int id) {
    return _players.get(id);
  }

  @Override
  public Collection<Player> getPlayers() {
    return Collections.unmodifiableCollection(_players.values());
  }

  @Override
  public void playDevCard() {
    _turn.setDevCardHasBeenPlayed();
  }

  @Override
  public boolean hasLongestRoad(int id) {
    if (_longestRoad == null) {
      int max = 0;
      for (Player p : _players.values()) {
        int longestPath = _board.longestPath(p);
        if (longestPath > max && longestPath >= Settings.LONGEST_ROAD_THRESH) {
          max = longestPath;
          _longestRoad = p;
        }
      }
    } else {
      int toBeat = _board.longestPath(_longestRoad);
      for (Player p : _players.values()) {
        int longestPath = _board.longestPath(p);
        if (longestPath > toBeat) {
          toBeat = longestPath;
          _longestRoad = p;
        }
      }
    }
    return _longestRoad != null ? _longestRoad.getID() == id : false;
  }

  @Override
  public boolean hasLargestArmy(int id) {
    Player player = getPlayerByID(id);
    int maxArmy = _largestArmy != null ? _largestArmy.numPlayedKnights() : 0;
    Player maxPlayer = null;
    for (Player p : _players.values()) {
      if (p.numPlayedKnights() > maxArmy) {
        maxArmy = p.numPlayedKnights();
        maxPlayer = p;
      }
    }
    if (maxArmy >= Settings.LARGEST_ARMY_THRESH && maxPlayer != null
        && maxPlayer.equals(player)) {
      _largestArmy = maxPlayer;
      return true;
    }
    if (_largestArmy != null && player.equals(_largestArmy)) {
      return true;
    }
    return false;
  }

  @Override
  public int getNumPublicPoints(int id) {
    Player player = getPlayerByID(id);
    int settlementPoints = SETTLEMENT_POINT_VAL
        * (INITIAL_SETTLEMENTS - player.numSettlements());
    int cityPoints = CITY_POINT_VAL * (INITIAL_CITIES - player.numCities());
    int roadArmyPts = 0;

    // Largest Army does not exist in C&K mode
    if (!_gameSettings.isCitiesAndKnights) {
      roadArmyPts += hasLargestArmy(id) ? LARGEST_ARMY_POINT_VAL : 0;
    }
    roadArmyPts += hasLongestRoad(id) ? LONGEST_ROAD_POINT_VAL : 0;

    // C&K points: defender tokens, metropolises, and merchant
    // C&K points: defender tokens, metropolises, and merchant
    int ckPoints = 0;
    if (_gameSettings.isCitiesAndKnights) {
      ckPoints += player.getDefenderPoints() * Settings.DEFENDER_POINT_VAL;
      // Merchant: +1 VP for the merchant owner
      if (_merchantOwner == id) {
        ckPoints += 1;
      }
      // Metropolis: +2 VP for each metropolis owned
      for (CityImprovement.Track t : CityImprovement.Track.values()) {
        if (_metropolisOwners.containsKey(t) && _metropolisOwners.get(t) == id) {
          ckPoints += Settings.METROPOLIS_POINT_VAL;
        }
      }
    }

    // Cursed Catan points: factories and capitols. Their settlement and city
    // pieces went back to the supply, so they are not counted above.
    int cursedPoints = 0;
    if (_gameSettings.isCursedCatan) {
      cursedPoints += Settings.FACTORY_POINT_VAL * player.numFactories();
      cursedPoints += Settings.CAPITOL_POINT_VAL * player.numCapitols();
      if (id == _longestTurnPlayer) {
        cursedPoints -= Settings.CURSED_LONGEST_TURN_PENALTY;
      }
    }

    return settlementPoints + cityPoints + roadArmyPts + ckPoints
        + cursedPoints;
  }

  /**
   * Updates the metropolis owner for the given track.
   * Checks if the candidate player can claim or steal the metropolis.
   */
  public void updateMetropolis(CityImprovement.Track track, int candidateID) {
    Player candidate = getPlayerByID(candidateID);
    int candidateLevel = candidate.getCityImprovement().getLevel(track);

    // Must be at least level 4 to have a metropolis
    if (candidateLevel < CityImprovement.METROPOLIS_THRESHOLD) {
      return;
    }

    Integer currentOwnerID = _metropolisOwners.get(track);
    if (currentOwnerID == null) {
      // No owner, claim it
      _metropolisOwners.put(track, candidateID);
    } else {
      // Check if candidate can steal it (must have strictly higher level)
      Player owner = getPlayerByID(currentOwnerID);
      int ownerLevel = owner.getCityImprovement().getLevel(track);
      if (candidateLevel > ownerLevel) {
        _metropolisOwners.put(track, candidateID);
      }
    }
  }

  public Integer getMetropolisOwner(CityImprovement.Track track) {
    return _metropolisOwners.get(track);
  }

  @Override
  public int getNumTotalPoints(int id) {
    int publicPoints = getNumPublicPoints(id);
    publicPoints += getPlayerByID(id).numVictoryPoints();
    return publicPoints;
  }

  @Override
  public Player getWinner() {
    // Cursed Catan: holding all five pieces of Exodia wins on the spot
    for (Player p : _players.values()) {
      if (holdsExodia(p)) {
        return p;
      }
    }
    // Cursed Catan: a draw (declared, or two players at the winning score at
    // once) goes to the tie-break; it may leave nobody as the winner
    if (isDraw()) {
      return getDrawWinner();
    }
    // Great War: the last player standing wins
    if (_war != null && _players.size() > 1) {
      Player survivor = null;
      int alive = 0;
      for (Player p : _players.values()) {
        if (!isEliminated(p.getID())) {
          alive++;
          survivor = p;
        }
      }
      if (alive == 1) {
        return survivor;
      }
    }
    for (Player p : _players.values()) {
      if (getNumTotalPoints(p.getID()) >= _gameSettings.winningPointCount) {
        return p;
      }
    }
    return null;
  }

  @Override
  public Map<Resource, Double> getBankRates(int id) {
    Player player = getPlayerByID(id);
    Map<Resource, Double> rates = new HashMap<>();
    for (Resource r : Resource.values()) {
      rates.put(r, _bank.getBankRate(r));
    }
    for (Intersection i : _board.getIntersections().values()) {
      if (i.getPort() != null && i.getBuilding() != null
          && i.getBuilding().getPlayer().equals(player)) {
        if (i.getPort().getResource() == Resource.WILDCARD) {
          for (Resource r : Resource.values()) {
            double rate = Math.min(rates.get(r), _bank.getWildCardRate(r));
            rates.put(r, rate);
          }
        } else {
          Resource r = i.getPort().getResource();
          double rate = Math.min(rates.get(r), _bank.getPortRate(r));
          rates.put(r, rate);
        }
      }
    }
    return rates;
  }

  @Override
  public int addPlayer(String name) {
    return addPlayer(name, _gameSettings.COLORS[_players.size()]);
  }

  @Override
  public int addPlayer(String name, String color) {
    if (_turn.getTurnNum() == 1) {
      int id = _players.size();
      _players.put(id, new HumanPlayer(id, name, color,
          _gameSettings.isCursedCatan));
      return id;
    }
    throw new UnsupportedOperationException(
        "Cannot currently add players during a game.");
  }

  @Override
  public Bank getBank() {
    return _bank;
  }

  @Override
  public GameSettings getGameSettings() {
    return _gameSettings;
  }

  @Override
  public void removeFollowUp(FollowUpAction action) {
    _turn.removeFollowUp(action);

  }

  @Override
  public List<Integer> getTurnOrder() {
    return Collections.unmodifiableList(_turnOrder);
  }

  @Override
  public GameStatus getGameStatus() {
    return _gameStatus;
  }

  @Override
  public void setGameStatus(GameStatus state) {
    _gameStatus = state;
    if (state == GameStatus.PROGRESS) {
      // The first real turn starts now, not when the game was created
      Player first = currentPlayer();
      if (first != null) {
        _turn.setTurnPlayerID(first.getID());
      }
      _turn.restartClock();
    }
  }

  @Override
  public boolean devCardDeckIsEmpty() {
    return _devCardDeck.isEmpty();
  }

  @Override
  public GameStats getGameStats() {
    return _gameStats;
  }

  @Override
  public boolean removePlayer(int id) {
    return _players.remove(id) != null;
  }

  private class ReadOnlyReferee implements Referee {

    private final Referee _referee;

    public ReadOnlyReferee(Referee referee) {
      _referee = referee;
    }

    @Override
    public boolean holdsExodia(Player player) {
      return _referee.holdsExodia(player);
    }

    @Override
    public boolean isWarActive() {
      return _referee.isWarActive();
    }

    @Override
    public WarState getWarState() {
      return _referee.getWarState();
    }

    @Override
    public Faction getFaction(int playerID) {
      return _referee.getFaction(playerID);
    }

    @Override
    public boolean isEliminated(int playerID) {
      return _referee.isEliminated(playerID);
    }

    @Override
    public boolean hasTech(int playerID, Tech tech) {
      return _referee.hasTech(playerID, tech);
    }

    @Override
    public boolean isSanctioned(int playerID) {
      return _referee.isSanctioned(playerID);
    }

    @Override
    public boolean ignoresFortresses() {
      return _referee.ignoresFortresses();
    }

    @Override
    public int getLongestTurnPlayer() {
      return _referee.getLongestTurnPlayer();
    }

    @Override
    public long getLongestTurnMillis() {
      return _referee.getLongestTurnMillis();
    }

    @Override
    public boolean isDraw() {
      return _referee.isDraw();
    }

    @Override
    public String getDrawReason() {
      return _referee.getDrawReason();
    }

    @Override
    public Player getDrawWinner() {
      return _referee.getDrawWinner();
    }

    @Override
    public void startNextTurn() {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot change the turn.");
    }

    @Override
    public DevelopmentCard getDevCard() {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot draw from the dev card deck.");
    }

    @Override
    public Referee getReadOnlyReferee() {
      return this;
    }

    @Override
    public Turn getTurn() {
      return _referee.getTurn();
    }

    @Override
    public Board getBoard() {
      return _referee.getBoard(); // Change to unmodifiable?
    }

    @Override
    public Player getPlayerByID(int id) {
      return _referee.getPlayerByID(id).getImmutableCopy();
    }

    @Override
    public List<Player> getPlayers() {
      List<Player> list = new ArrayList<>();
      for (Player p : _referee.getPlayers()) {
        list.add(p.getImmutableCopy());
      }
      return Collections.unmodifiableList(list);
    }

    @Override
    public void playDevCard() {
      throw new UnsupportedOperationException(
          "ReadOnlyReferee cannot play development cards.");

    }

    @Override
    public Player currentPlayer() {
      return _referee.currentPlayer().getImmutableCopy();
    }

    @Override
    public boolean hasLongestRoad(int player) {
      return _referee.hasLongestRoad(player);
    }

    @Override
    public boolean hasLargestArmy(int player) {
      return _referee.hasLargestArmy(player);
    }

    @Override
    public int getNumPublicPoints(int player) {
      return _referee.getNumPublicPoints(player);
    }

    @Override
    public int getNumTotalPoints(int player) {
      return _referee.getNumTotalPoints(player);
    }

    @Override
    public Map<Resource, Double> getBankRates(int player) {
      return _referee.getBankRates(player);
    }

    @Override
    public int addPlayer(String name) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot add players.");
    }

    @Override
    public int addPlayer(String name, String color) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot add players.");
    }

    @Override
    public Bank getBank() {
      return _referee.getBank();
    }

    @Override
    public GameSettings getGameSettings() {
      return _referee.getGameSettings();
    }

    @Override
    public FollowUpAction getNextFollowUp(int playerID) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot getNextFollowup");
    }

    @Override
    public void addFollowUp(Collection<FollowUpAction> actions) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot addFollowUp.");
    }

    @Override
    public void removeFollowUp(FollowUpAction action) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot removeFollowUp.");

    }

    @Override
    public List<Integer> getTurnOrder() {
      return Collections.unmodifiableList(_turnOrder);
    }

    @Override
    public GameStatus getGameStatus() {
      return _referee.getGameStatus();
    }

    @Override
    public void setGameStatus(GameStatus state) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot setGameState.");

    }

    @Override
    public Setup getSetup() {
      return _referee.getSetup();
    }

    @Override
    public Player getWinner() {
      return _referee.getWinner() != null ? _referee.getWinner()
          .getImmutableCopy() : null;
    }

    @Override
    public boolean devCardDeckIsEmpty() {
      return _referee.devCardDeckIsEmpty();
    }

    @Override
    public GameStats getGameStats() {
      return _referee.getGameStats();
    }

    @Override
    public boolean removePlayer(int id) {
      return _referee.removePlayer(id);
    }

    @Override
    public void setMerchant(int playerID, HexCoordinate hex) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot set the merchant.");
    }

    @Override
    public int getMerchantOwner() {
      return _referee.getMerchantOwner();
    }

    @Override
    public HexCoordinate getMerchantHex() {
      return _referee.getMerchantHex();
    }

    @Override
    public void setOverriddenDice(int red, int white, int event) {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot set overridden dice.");
    }

    @Override
    public int[] consumeOverriddenDice() {
      throw new UnsupportedOperationException(
          "A ReadOnlyReferee cannot consume overridden dice.");
    }

  }

}
