package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import edu.brown.cs.actions.ActionResponse;
import edu.brown.cs.actions.BuildCapitol;
import edu.brown.cs.actions.BuildCity;
import edu.brown.cs.actions.BuildFactory;
import edu.brown.cs.actions.DeployUnits;
import edu.brown.cs.actions.EndTurn;
import edu.brown.cs.actions.FollowUpAction;
import edu.brown.cs.actions.GoldResource;
import edu.brown.cs.actions.PlayKnight;
import edu.brown.cs.actions.PlayRoadBuilding;
import edu.brown.cs.actions.ProposeTrade;
import edu.brown.cs.actions.TradeWithBank;
import edu.brown.cs.api.CatanConverter;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * The start of the Great War in Cursed Catan: factions, setup, deployment,
 * the construction freeze, elimination, Atomic Bombs and the Nuclear Winter
 * track.
 */
public class CursedWarTest {

  // ---------------------------------------------------------------- helpers

  private static GameSettings settings(int players, boolean cursed) {
    JsonObject json = new JsonObject();
    json.addProperty("numPlayers", players);
    json.addProperty("isDecimal", false);
    json.addProperty("isDynamic", false);
    json.addProperty("isStandard", false);
    json.addProperty("isCitiesAndKnights", false);
    json.addProperty("isCursedCatan", cursed);
    return new GameSettings(json);
  }

  private static Referee game(int players, long seed) {
    Referee ref = new MasterReferee(settings(players, true), new AtomicLong(
        1000)::get, new Random(seed));
    String[] colors = { "#cc3333", "#3366cc", "#33aa55", "#ccaa33", "#aa33cc",
        "#33cccc" };
    for (int i = 0; i < players; i++) {
      ref.addPlayer("P" + i, colors[i]);
    }
    ref.setGameStatus(GameStatus.PROGRESS);
    return ref;
  }

  private static Referee game() {
    return game(2, 1);
  }

  private static void startWar(Referee ref) {
    for (int i = 0; i < 20 && !ref.isWarActive(); i++) {
      ref.recordCoalSpent();
      ref.advanceIndustryTrack();
    }
    assertTrue(ref.isWarActive());
  }

  private static Player other(Referee ref, Player p) {
    for (Player q : ref.getPlayers()) {
      if (!q.equals(p)) {
        return q;
      }
    }
    return null;
  }

  private static void give(Player p, Resource r, double amount) {
    double have = p.getResources().get(r);
    if (have > amount) {
      p.removeResource(r, have - amount);
    } else if (have < amount) {
      p.addResource(r, amount - have);
    }
  }

  private static void giveVp(Player p, int n) {
    for (int i = 0; i < n; i++) {
      p.addDevelopmentCard(DevelopmentCard.POINT);
    }
  }

  private static Tile tileOf(Referee ref, TileType type) {
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType() == type) {
        return t;
      }
    }
    return null;
  }

  private static List<Tile> tilesOf(Referee ref, TileType type) {
    List<Tile> list = new ArrayList<>();
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType() == type) {
        list.add(t);
      }
    }
    return list;
  }

  /** A settlement of the player on a free corner of a tile. */
  private static Intersection settle(Tile tile, Player p) {
    for (Intersection i : tile.getIntersections()) {
      if (i.getBuilding() == null) {
        p.useSettlement();
        i.placeSettlement(p);
        return i;
      }
    }
    throw new IllegalStateException("no free corner");
  }

  private static JsonObject hex(HexCoordinate h) {
    JsonObject o = new JsonObject();
    o.addProperty("x", h.getX());
    o.addProperty("y", h.getY());
    o.addProperty("z", h.getZ());
    return o;
  }

  private static JsonObject placements(Object... triples) {
    JsonArray list = new JsonArray();
    for (int i = 0; i < triples.length; i += 3) {
      JsonObject o = new JsonObject();
      o.add("tile", hex(((Tile) triples[i]).getCoordinate()));
      o.addProperty("unit", (String) triples[i + 1]);
      o.addProperty("count", (Integer) triples[i + 2]);
      list.add(o);
    }
    JsonObject json = new JsonObject();
    json.add("placements", list);
    return json;
  }

  /** Hands over the war-start follow-ups and registers them. */
  private static List<FollowUpAction> warFollowUps(Referee ref) {
    List<FollowUpAction> all = new ArrayList<>();
    for (Collection<FollowUpAction> group : ref.takeWarStartFollowUps()) {
      ref.addFollowUp(group);
      all.addAll(group);
    }
    return all;
  }

  private static DeployUnits deployFor(List<FollowUpAction> ups, Player p) {
    for (FollowUpAction f : ups) {
      if (f instanceof DeployUnits && f.getPlayerID() == p.getID()) {
        return (DeployUnits) f;
      }
    }
    return null;
  }

  private static int[] seq(final int... values) {
    return values;
  }

  private static java.util.function.IntSupplier dice(final int... values) {
    final int[] next = { 0 };
    return () -> values[Math.min(next[0]++, values.length - 1)];
  }

  // -------------------------------------------------------------- factions

  @Test
  public void factionsAreHandedOutByRank() {
    String[][] expected = {
        { "USA", "Russia" },
        { "USA", "Russia", "China" },
        { "USA", "Russia", "Russia", "USA" },
        { "USA", "Russia", "Russia", "USA", "USA" },
        { "USA", "Russia", "Russia", "USA", "USA", "Russia" } };
    for (int n = 2; n <= 6; n++) {
      for (int place = 0; place < n; place++) {
        assertEquals("players " + n + ", place " + place, expected[n - 2][place],
            FactionAssigner.factionForPlace(n, place).toString());
      }
    }
  }

  @Test
  public void rankingFollowsPointsAndOnlyTiesAreRandom() {
    Set<String> tieOrders = new HashSet<>();
    for (long seed = 0; seed < 30; seed++) {
      Referee ref = game(3, seed);
      List<Player> players = new ArrayList<>(ref.getPlayers());
      giveVp(players.get(0), 1);
      giveVp(players.get(1), 3);
      giveVp(players.get(2), 3);
      List<Player> ranked = FactionAssigner.rank(players, p -> ref
          .getNumTotalPoints(p.getID()), new Random(seed));
      assertEquals(3, ref.getNumTotalPoints(ranked.get(0).getID()));
      assertEquals(3, ref.getNumTotalPoints(ranked.get(1).getID()));
      assertEquals(1, ref.getNumTotalPoints(ranked.get(2).getID()));
      tieOrders.add(ranked.get(0).getName());
    }
    assertEquals(2, tieOrders.size());
  }

  // ------------------------------------------------------------- war start

  @Test
  public void theWarBeginsWhenTheIndustryTrackReachesItsEnd() {
    Referee ref = game();
    assertFalse(ref.isWarActive());
    assertNull(ref.getWarState());
    for (int i = 0; i < Settings.INDUSTRY_TRACK_LENGTH - 2; i++) {
      ref.recordCoalSpent();
      ref.advanceIndustryTrack();
    }
    assertFalse(ref.isWarActive());
    ref.recordCoalSpent();
    ref.advanceIndustryTrack();
    assertTrue(ref.isWarActive());
    assertTrue(ref.isGreatWarTriggered());
  }

  @Test
  public void theTopScorerFightsForTheUSA() {
    for (int who = 0; who < 2; who++) {
      Referee ref = game(2, 5);
      List<Player> ps = new ArrayList<>(ref.getPlayers());
      giveVp(ps.get(who), 3);
      startWar(ref);
      assertEquals(Faction.USA, ref.getFaction(ps.get(who).getID()));
      assertEquals(Faction.RUSSIA, ref.getFaction(ps.get(1 - who).getID()));
    }
  }

  @Test
  public void threePlayersFightForUsaRussiaAndChina() {
    Referee ref = game(3, 2);
    startWar(ref);
    Set<Faction> factions = new HashSet<>();
    for (Player p : ref.getPlayers()) {
      factions.add(ref.getFaction(p.getID()));
    }
    assertEquals(new HashSet<>(Arrays.asList(Faction.USA, Faction.RUSSIA,
        Faction.CHINA)), factions);
    assertEquals(3, ref.getWarState().getFactionList().size());
  }

  @Test
  public void factionNamesAreUsaRussiaAndChina() {
    assertEquals("USA", Faction.USA.toString());
    assertEquals("Russia", Faction.RUSSIA.toString());
    assertEquals("China", Faction.CHINA.toString());
  }

  @Test
  public void everyFactionStartsWithMoraleTwelveAndTheTrackAtOne() {
    Referee ref = game(3, 3);
    startWar(ref);
    for (Faction f : ref.getWarState().getFactionList()) {
      assertEquals(12, ref.getWarState().getMorale(f));
    }
    assertEquals(1, ref.getWarState().getNuclearWinter());
    assertNull(ref.getWarState().getSanctioned());
  }

  @Test
  public void knightsInHandArePlayedAndEachGivesAnInfantry() {
    Referee ref = game();
    Player p = ref.currentPlayer();
    p.addDevelopmentCard(DevelopmentCard.KNIGHT);
    p.addDevelopmentCard(DevelopmentCard.KNIGHT);
    startWar(ref);
    assertEquals(0, p.getDevCards().get(DevelopmentCard.KNIGHT).intValue());
    assertEquals(2, p.numPlayedKnights());
    // no city: 1 infantry, plus 2 for the knights
    assertEquals(3, ref.getUnitsToDeploy(p.getID()).get(UnitType.INFANTRY)
        .intValue());
  }

  @Test
  public void coalTilesBecomeUraniumAndKeepTheirChits() {
    Referee ref = game();
    Map<HexCoordinate, Integer> chits = new HashMap<>();
    for (Tile t : tilesOf(ref, TileType.COAL)) {
      chits.put(t.getCoordinate(), t.getRollNumber());
    }
    assertEquals(3, chits.size());
    startWar(ref);
    assertTrue(tilesOf(ref, TileType.COAL).isEmpty());
    assertEquals(3, tilesOf(ref, TileType.URANIUM).size());
    for (Tile t : tilesOf(ref, TileType.URANIUM)) {
      assertEquals(chits.get(t.getCoordinate()).intValue(), t.getRollNumber());
    }
  }

  @Test
  public void coalIsDiscardedForResourcesOfChoice() {
    Referee ref = game();
    Player p = ref.currentPlayer();
    give(p, Resource.COAL, 5);
    give(p, Resource.WOOD, 0);
    give(p, Resource.BRICK, 0);
    startWar(ref);
    assertEquals(0.0, p.getResources().get(Resource.COAL), 0.001);
    List<FollowUpAction> ups = warFollowUps(ref);
    GoldResource exchange = null;
    for (FollowUpAction f : ups) {
      if (f instanceof GoldResource && f.getPlayerID() == p.getID()) {
        exchange = (GoldResource) f;
      }
    }
    assertNotNull(exchange);
    assertTrue(exchange.getData().get("message").getAsString().contains(
        "Coal exchange"));
    JsonObject picks = new JsonObject();
    JsonObject res = new JsonObject();
    res.addProperty("wood", 3);
    res.addProperty("brick", 2);
    picks.add("resources", res);
    exchange.setupAction(ref, p.getID(), picks);
    assertTrue(exchange.execute().get(p.getID()).getSuccess());
    assertEquals(3.0, p.getResources().get(Resource.WOOD), 0.001);
    assertEquals(2.0, p.getResources().get(Resource.BRICK), 0.001);
  }

  @Test
  public void coalCannotBeTakenBackAndTheCountMustMatch() {
    Referee ref = game();
    Player p = ref.currentPlayer();
    give(p, Resource.COAL, 2);
    startWar(ref);
    List<FollowUpAction> ups = warFollowUps(ref);
    FollowUpAction exchange = null;
    for (FollowUpAction f : ups) {
      if (f instanceof GoldResource && f.getPlayerID() == p.getID()) {
        exchange = f;
      }
    }
    for (String bad : new String[] { "coal", "uranium", "wildcard" }) {
      JsonObject res = new JsonObject();
      res.addProperty(bad, 2);
      JsonObject json = new JsonObject();
      json.add("resources", res);
      try {
        exchange.setupAction(ref, p.getID(), json);
        throw new AssertionError("accepted " + bad);
      } catch (IllegalArgumentException expected) {
        // good
      }
    }
    JsonObject res = new JsonObject();
    res.addProperty("wood", 1);
    JsonObject json = new JsonObject();
    json.add("resources", res);
    try {
      exchange.setupAction(ref, p.getID(), json);
      throw new AssertionError("accepted the wrong count");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  @Test
  public void withNoCoalThereIsNoExchangeAndTheFollowUpsComeOnce() {
    Referee ref = game();
    for (Player p : ref.getPlayers()) {
      give(p, Resource.COAL, 0);
    }
    startWar(ref);
    List<Collection<FollowUpAction>> groups = ref.takeWarStartFollowUps();
    assertEquals(1, groups.size());
    for (FollowUpAction f : groups.get(0)) {
      assertTrue(f instanceof DeployUnits);
    }
    assertEquals(2, groups.get(0).size());
    assertTrue(ref.takeWarStartFollowUps().isEmpty());
  }

  @Test
  public void exchangeComesBeforeDeployment() {
    Referee ref = game();
    give(ref.currentPlayer(), Resource.COAL, 1);
    startWar(ref);
    List<Collection<FollowUpAction>> groups = ref.takeWarStartFollowUps();
    assertEquals(2, groups.size());
    for (FollowUpAction f : groups.get(0)) {
      assertTrue(f instanceof GoldResource);
    }
    for (FollowUpAction f : groups.get(1)) {
      assertTrue(f instanceof DeployUnits);
    }
  }

  @Test
  public void theRobberIsRemoved() {
    Referee ref = game();
    boolean before = false;
    for (Tile t : ref.getBoard().getTiles()) {
      before |= t.hasRobber();
    }
    assertTrue(before);
    startWar(ref);
    for (Tile t : ref.getBoard().getTiles()) {
      assertFalse(t.hasRobber());
    }
  }

  @Test
  public void theIndustryTrackStopsOnceTheWarHasBegun() {
    Referee ref = game();
    startWar(ref);
    ref.recordCoalSpent();
    assertFalse(ref.advanceIndustryTrack());
    assertEquals(Settings.INDUSTRY_TRACK_LENGTH, ref.getIndustryTrack());
  }

  // ------------------------------------------------------------ deployment

  @Test
  public void startingForcesFollowTheBuildings() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    Tile t = tileOf(ref, TileType.WOOD);
    Intersection town = settle(t, a); // stays a settlement
    Intersection city = settle(tileOf(ref, TileType.BRICK), a);
    city.placeCity(a);
    Intersection factory = settle(tileOf(ref, TileType.ORE), a);
    factory.placeFactory(a);
    Intersection capitol = settle(tileOf(ref, TileType.WHEAT), a);
    capitol.placeCity(a);
    capitol.placeCapitol(a);
    assertTrue(town.getBuilding() != null);
    startWar(ref);
    // city 1 + capitol 1 infantry; factory 1 + capitol 1 artillery
    assertEquals(2, ref.getUnitsToDeploy(a.getID()).get(UnitType.INFANTRY)
        .intValue());
    assertEquals(2, ref.getUnitsToDeploy(a.getID()).get(UnitType.ARTILLERY)
        .intValue());
    // no buildings at all: a single infantry
    assertEquals(1, ref.getUnitsToDeploy(b.getID()).get(UnitType.INFANTRY)
        .intValue());
    assertNull(ref.getUnitsToDeploy(b.getID()).get(UnitType.ARTILLERY));
  }

  @Test
  public void aFactoryAloneStillGetsAnInfantry() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Intersection i = settle(tileOf(ref, TileType.ORE), a);
    i.placeFactory(a);
    startWar(ref);
    assertEquals(1, ref.getUnitsToDeploy(a.getID()).get(UnitType.INFANTRY)
        .intValue());
    assertEquals(1, ref.getUnitsToDeploy(a.getID()).get(UnitType.ARTILLERY)
        .intValue());
  }

  @Test
  public void unitsAreDeployedNextToOwnBuildings() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Tile home = tileOf(ref, TileType.WOOD);
    settle(home, a);
    startWar(ref);
    List<FollowUpAction> ups = warFollowUps(ref);
    DeployUnits deploy = deployFor(ups, a);
    deploy.setupAction(ref, a.getID(), placements(home, "infantry", 1));
    ActionResponse r = deploy.execute().get(a.getID());
    assertTrue(r.getMessage(), r.getSuccess());
    Faction f = ref.getFaction(a.getID());
    assertEquals(1, ref.getWarState().getUnits(home.getCoordinate(), f,
        UnitType.INFANTRY));
    assertTrue(ref.getUnitsToDeploy(a.getID()).isEmpty());
  }

  @Test
  public void deploymentFollowUpTellsTheClientWhatAndWhere() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Tile home = tileOf(ref, TileType.WOOD);
    settle(home, a);
    startWar(ref);
    DeployUnits deploy = deployFor(warFollowUps(ref), a);
    JsonObject data = deploy.getData();
    assertEquals(1, data.getAsJsonObject("units").get("infantry").getAsInt());
    boolean listed = false;
    for (com.google.gson.JsonElement el : data.getAsJsonArray("tiles")) {
      JsonObject t = el.getAsJsonObject();
      listed |= t.get("x").getAsInt() == home.getCoordinate().getX()
          && t.get("y").getAsInt() == home.getCoordinate().getY()
          && t.get("z").getAsInt() == home.getCoordinate().getZ();
    }
    assertTrue(listed);
  }

  @Test
  public void theLastPlayerStandingIsReportedAsSurvivor() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    settle(tileOf(ref, TileType.WOOD), a);
    Intersection bi = settle(tileOf(ref, TileType.BRICK), b);
    startWar(ref);
    bi.getBuilding().setRuined(true);
    JsonObject state = new CatanConverter().getGameState(ref, a.getID());
    assertEquals(a.getID(), state.get("winner").getAsInt());
    assertEquals("survivor", state.get("winReason").getAsString());
  }

  @Test
  public void deploymentMustPlaceExactlyTheUnitsOwed() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Tile home = tileOf(ref, TileType.WOOD);
    settle(home, a);
    startWar(ref);
    DeployUnits deploy = deployFor(warFollowUps(ref), a);
    JsonObject[] bad = { placements(), placements(home, "infantry", 2),
        placements(home, "infantry", 1, home, "artillery", 1),
        placements(home, "tank", 1) };
    for (JsonObject json : bad) {
      try {
        deploy.setupAction(ref, a.getID(), json);
        throw new AssertionError("accepted " + json);
      } catch (IllegalArgumentException expected) {
        // good
      }
    }
    // nothing was placed by the failed attempts
    assertEquals(0, ref.getWarState().getUnitCount(ref.getFaction(a.getID())));
  }

  @Test
  public void deploymentRejectsTilesAwayFromOwnBuildingsAndTheSea() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    settle(tileOf(ref, TileType.WOOD), a);
    Tile theirs = tileOf(ref, TileType.BRICK);
    settle(theirs, b);
    Tile far = tileOf(ref, TileType.ORE);
    startWar(ref);
    DeployUnits deploy = deployFor(warFollowUps(ref), a);
    for (Tile t : new Tile[] { theirs, far }) {
      if (DeployUnits.touchesBuildingOf(t, a)) {
        continue; // shares a corner by chance
      }
      try {
        deploy.setupAction(ref, a.getID(), placements(t, "infantry", 1));
        throw new AssertionError("accepted a tile away from buildings");
      } catch (IllegalArgumentException expected) {
        // good
      }
    }
    Tile sea = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType() == TileType.SEA) {
        sea = t;
        break;
      }
    }
    try {
      deploy.setupAction(ref, a.getID(), placements(sea, "infantry", 1));
      throw new AssertionError("accepted a sea tile");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  @Test
  public void endTurnAnnouncesTheWarAndQueuesDeploymentBeforeTheRoll() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    settle(tileOf(ref, TileType.WOOD), a);
    // The track starts at 1; stop one short of its end
    for (int i = 0; i < Settings.INDUSTRY_TRACK_LENGTH - 2; i++) {
      ref.recordCoalSpent();
      ref.advanceIndustryTrack();
    }
    assertFalse(ref.isWarActive());
    for (Player p : ref.getPlayers()) {
      give(p, Resource.COAL, 0);
    }
    ref.recordCoalSpent();
    Map<Integer, ActionResponse> out = new EndTurn(ref, a.getID()).execute();
    assertTrue(ref.isWarActive());
    assertTrue(out.get(a.getID()).getMessage().contains("GREAT WAR"));
    Player next = ref.currentPlayer();
    assertTrue(ref.getNextFollowUp(next.getID()) instanceof DeployUnits);
    assertTrue(ref.getNextFollowUp(a.getID()) instanceof DeployUnits);
  }

  // ---------------------------------------------------- construction freeze

  @Test
  public void normalConstructionStopsInTheWar() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Intersection i = settle(tileOf(ref, TileType.WOOD), a);
    assertNull(edu.brown.cs.actions.CatanFormats.warBlocksConstruction(ref));
    startWar(ref);
    assertNotNull(edu.brown.cs.actions.CatanFormats.warBlocksConstruction(ref));
    Map<Integer, ActionResponse>[] results = new Map[] {
        new BuildCity(ref, a.getID(), i.getPosition()).execute(),
        new BuildFactory(ref, a.getID(), i.getPosition()).execute(),
        new BuildCapitol(ref, a.getID(), i.getPosition()).execute(),
        new PlayRoadBuilding(ref, a.getID()).execute() };
    for (Map<Integer, ActionResponse> r : results) {
      assertFalse(r.get(a.getID()).getSuccess());
      assertTrue(r.get(a.getID()).getMessage(), r.get(a.getID()).getMessage()
          .contains("Great War"));
    }
  }

  @Test
  public void thereIsNoRobberForKnightsInTheWar() {
    Referee ref = game();
    startWar(ref);
    Player a = ref.currentPlayer();
    a.addDevelopmentCard(DevelopmentCard.KNIGHT);
    ActionResponse r = new PlayKnight(ref, a.getID()).execute().get(a.getID());
    assertFalse(r.getSuccess());
    assertTrue(r.getMessage().contains("no robber"));
  }

  // ---------------------------------------------------------------- trading

  private static ActionResponse propose(Referee ref, Player p) {
    JsonObject trade = new JsonObject();
    trade.addProperty("wood", -1);
    trade.addProperty("brick", 1);
    JsonObject params = new JsonObject();
    params.add("trade", trade);
    return new ProposeTrade(ref, p.getID(), params).execute().get(p.getID());
  }

  @Test
  public void aLoneFactionCannotTradeWithPlayers() {
    Referee ref = game();
    startWar(ref);
    ActionResponse r = propose(ref, ref.currentPlayer());
    assertFalse(r.getSuccess());
    assertTrue(r.getMessage().contains("faction"));
  }

  @Test
  public void tradesGoOnlyToTeammates() {
    Referee ref = game(4, 4);
    startWar(ref);
    Player a = ref.currentPlayer();
    ActionResponse r = propose(ref, a);
    assertTrue(r.getMessage(), r.getSuccess());
    for (Player p : ref.getPlayers()) {
      if (p.equals(a)) {
        continue;
      }
      boolean mate = ref.getFaction(p.getID()) == ref.getFaction(a.getID());
      assertEquals(p.getName(), mate, ref.getNextFollowUp(p.getID()) != null);
    }
  }

  @Test
  public void sanctionsStopMaritimeTradingForTheLaunchersFaction() {
    Referee ref = game();
    startWar(ref);
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    give(a, Resource.WOOD, 8);
    JsonObject params = new JsonObject();
    params.addProperty("toGive", "wood");
    params.addProperty("toGet", "ore");
    params.addProperty("amount", 1);
    assertTrue(new TradeWithBank(ref, a.getID(), params).execute().get(
        a.getID()).getSuccess());
    ref.launchBomb(a.getID(), tileOf(ref, TileType.WOOD).getCoordinate(), 1,
        dice(1));
    assertTrue(ref.isSanctioned(a.getID()));
    assertFalse(ref.isSanctioned(b.getID()));
    ActionResponse r = new TradeWithBank(ref, a.getID(), params).execute()
        .get(a.getID());
    assertFalse(r.getSuccess());
    assertTrue(r.getMessage().contains("Sanctions"));
  }

  // ----------------------------------------------------------- elimination

  @Test
  public void nobodyIsEliminatedOutsideTheWar() {
    Referee ref = game();
    for (Player p : ref.getPlayers()) {
      assertFalse(ref.isEliminated(p.getID()));
    }
  }

  @Test
  public void aPlayerWhoseBuildingsAreAllRuinedIsEliminated() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    Intersection ai = settle(tileOf(ref, TileType.WOOD), a);
    Intersection bi = settle(tileOf(ref, TileType.BRICK), b);
    startWar(ref);
    assertFalse(ref.isEliminated(a.getID()));
    ai.getBuilding().setRuined(true);
    assertTrue(ref.isEliminated(a.getID()));
    assertFalse(ref.isEliminated(b.getID()));
    assertNotNull(bi.getBuilding());
  }

  @Test
  public void aBuildingOnlyNextToDesertAndWastelandDoesNotCount() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Tile t = tileOf(ref, TileType.WOOD);
    Intersection i = settle(t, a);
    startWar(ref);
    assertFalse(ref.isEliminated(a.getID()));
    boolean flip = false;
    for (Tile x : ref.getBoard().getTiles()) {
      if (x.getIntersections().contains(i)) {
        x.setType(flip ? TileType.WASTELAND : TileType.DESERT);
        flip = !flip;
      }
    }
    assertTrue(ref.isEliminated(a.getID()));
  }

  @Test
  public void theLastPlayerStandingWins() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    settle(tileOf(ref, TileType.WOOD), a);
    Intersection bi = settle(tileOf(ref, TileType.BRICK), b);
    startWar(ref);
    assertNull(ref.getWinner());
    bi.getBuilding().setRuined(true);
    assertEquals(a, ref.getWinner());
    assertFalse(ref.isDraw());
  }

  @Test
  public void everyoneEliminatedIsADrawSettledByTheTieBreak() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    Intersection ai = settle(tileOf(ref, TileType.WOOD), a);
    Intersection bi = settle(tileOf(ref, TileType.BRICK), b);
    giveVp(b, 2);
    startWar(ref);
    ai.getBuilding().setRuined(true);
    bi.getBuilding().setRuined(true);
    assertTrue(ref.isDraw());
    assertEquals("All players are eliminated", ref.getDrawReason());
    assertEquals(b, ref.getWinner());
    // tied on points and credit: nobody wins
    giveVp(a, 2);
    assertNull(ref.getWinner());
    assertTrue(ref.isDraw());
  }

  @Test
  public void forfeitingEliminatesAPlayer() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    settle(tileOf(ref, TileType.WOOD), a);
    settle(tileOf(ref, TileType.BRICK), other(ref, a));
    startWar(ref);
    ref.eliminate(a.getID());
    assertTrue(ref.isEliminated(a.getID()));
    assertEquals(other(ref, a), ref.getWinner());
  }

  @Test
  public void eliminatedPlayersTakeNoMoreTurns() {
    Referee ref = game(3, 6);
    List<Player> ps = new ArrayList<>(ref.getPlayers());
    for (Player p : ps) {
      settle(tilesOf(ref, TileType.WOOD).get(ps.indexOf(p)), p);
    }
    startWar(ref);
    Player gone = ps.get(1);
    ref.eliminate(gone.getID());
    for (int i = 0; i < 8; i++) {
      ref.startNextTurn();
      assertFalse(ref.currentPlayer().equals(gone));
    }
  }

  // ----------------------------------------------------------- atomic bombs

  @Test
  public void aBombTurnsTheTileToDesertAndRuinsNeighbouringBuildings() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    Tile target = tileOf(ref, TileType.WOOD);
    int chit = target.getRollNumber();
    assertTrue(chit > 0);
    Intersection ai = settle(target, a);
    Intersection bi = settle(target, b);
    // far away, so it is untouched (the board is random, so look for a tile
    // that shares no corner with the target)
    Tile far = null;
    for (Tile c : ref.getBoard().getTiles()) {
      if (c.getType() != TileType.SEA && c.getType() != TileType.DESERT
          && Collections.disjoint(c.getIntersections(),
              target.getIntersections())) {
        far = c;
        break;
      }
    }
    Intersection farHouse = settle(far, a);
    startWar(ref);
    ref.launchBomb(a.getID(), target.getCoordinate(), 1, dice(1));
    assertEquals(TileType.DESERT, target.getType());
    assertEquals(0, target.getRollNumber());
    assertTrue(ai.getBuilding().isRuined());
    assertTrue(bi.getBuilding().isRuined());
    assertFalse(farHouse.getBuilding().isRuined());
    assertEquals(11, ref.getWarState().getMorale(ref.getFaction(a.getID())));
    assertEquals(11, ref.getWarState().getMorale(ref.getFaction(b.getID())));
  }

  @Test
  public void ruinsProduceNothing() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Intersection i = settle(tileOf(ref, TileType.WOOD), a);
    assertFalse(i.getBuilding().collectResource(Resource.WOOD).isEmpty());
    i.getBuilding().setRuined(true);
    assertTrue(i.getBuilding().collectResource(Resource.WOOD).isEmpty());
  }

  @Test
  public void unitsOnTheStruckTileDieOnFourToSix() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Tile mine = tileOf(ref, TileType.WOOD);
    Tile theirs = tileOf(ref, TileType.BRICK);
    startWar(ref);
    Faction f = ref.getFaction(a.getID());
    Faction g = ref.getFaction(other(ref, a).getID());
    ref.getWarState().addUnits(mine.getCoordinate(), f, UnitType.INFANTRY, 3);
    ref.getWarState().addUnits(theirs.getCoordinate(), g, UnitType.ARTILLERY,
        1);
    // 3 survives, 4 and 6 die
    ref.launchBomb(a.getID(), mine.getCoordinate(), 1, dice(3, 4, 6));
    assertEquals(1, ref.getWarState().getUnits(mine.getCoordinate(), f,
        UnitType.INFANTRY));
    // enemy units are hit too
    ref.launchBomb(a.getID(), theirs.getCoordinate(), 1, dice(5));
    assertEquals(0, ref.getWarState().getUnits(theirs.getCoordinate(), g,
        UnitType.ARTILLERY));
  }

  @Test
  public void theWinterDieNeedsTheRightNumberToAdvance() {
    Referee ref = game();
    startWar(ref);
    WarState war = ref.getWarState();
    int[] needed = { 6, 5, 4, 3, 2, 0 };
    Player a = ref.currentPlayer();
    Tile t = tileOf(ref, TileType.WOOD);
    for (int pos = 1; pos <= 6; pos++) {
      assertEquals(pos, war.getNuclearWinter());
      assertEquals(needed[pos - 1], war.rollNeededToAdvance());
      if (pos == 6) {
        break;
      }
      // one short of what is needed: no movement
      if (needed[pos - 1] > 1) {
        ref.launchBomb(a.getID(), t.getCoordinate(), needed[pos - 1] - 1,
            dice(1));
        assertEquals(pos, war.getNuclearWinter());
      }
      ref.launchBomb(a.getID(), t.getCoordinate(), needed[pos - 1], dice(1));
      assertEquals(pos + 1, war.getNuclearWinter());
    }
  }

  @Test
  public void fallOutTurnsSheepThenWheatToDesertAndTheLastLevelIsADraw() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    Tile t = tileOf(ref, TileType.WOOD);
    // both players have a building far from the strikes, so nobody is out
    Tile far = null;
    for (Tile c : tilesOf(ref, TileType.ORE)) {
      if (Collections.disjoint(c.getIntersections(), t.getIntersections())) {
        far = c;
      }
    }
    assertNotNull(far);
    settle(far, a);
    settle(far, other(ref, a));
    giveVp(a, 1);
    startWar(ref);
    List<Tile> sheep = tilesOf(ref, TileType.SHEEP);
    List<Tile> wheat = tilesOf(ref, TileType.WHEAT);
    assertFalse(sheep.isEmpty());
    for (int launch = 1; launch <= 2; launch++) {
      ref.launchBomb(a.getID(), t.getCoordinate(), 6, dice(1));
    }
    assertEquals(3, ref.getWarState().getNuclearWinter());
    assertEquals(sheep.size(), tilesOf(ref, TileType.SHEEP).size());
    ref.launchBomb(a.getID(), t.getCoordinate(), 6, dice(1)); // to 4
    assertEquals(4, ref.getWarState().getNuclearWinter());
    assertTrue(tilesOf(ref, TileType.SHEEP).isEmpty());
    for (Tile s : sheep) {
      assertEquals(TileType.DESERT, s.getType());
      assertEquals(0, s.getRollNumber());
    }
    assertEquals(wheat.size(), tilesOf(ref, TileType.WHEAT).size());
    ref.launchBomb(a.getID(), t.getCoordinate(), 6, dice(1)); // to 5
    assertTrue(tilesOf(ref, TileType.WHEAT).isEmpty());
    assertFalse(ref.isDraw());
    String what = ref.launchBomb(a.getID(), t.getCoordinate(), 6, dice(1));
    assertEquals(6, ref.getWarState().getNuclearWinter());
    assertTrue(what.contains("draw"));
    assertTrue(ref.isDraw());
    assertEquals("The Nuclear Winter track reached its last level", ref
        .getDrawReason());
    assertEquals(a, ref.getWinner()); // most victory points
  }

  @Test
  public void aStrikeOnUraniumMovesTheTrackAnExtraStep() {
    Referee ref = game();
    startWar(ref);
    Player a = ref.currentPlayer();
    Tile uranium = tilesOf(ref, TileType.URANIUM).get(0);
    ref.launchBomb(a.getID(), uranium.getCoordinate(), 1, dice(1));
    assertEquals(2, ref.getWarState().getNuclearWinter());
    Tile other = tilesOf(ref, TileType.URANIUM).get(0);
    ref.launchBomb(a.getID(), other.getCoordinate(), 6, dice(1));
    assertEquals(4, ref.getWarState().getNuclearWinter());
  }

  @Test
  public void onlyTheFirstLauncherIsSanctioned() {
    Referee ref = game();
    startWar(ref);
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    Tile t = tileOf(ref, TileType.WOOD);
    String first = ref.launchBomb(a.getID(), t.getCoordinate(), 1, dice(1));
    String second = ref.launchBomb(b.getID(), t.getCoordinate(), 1, dice(1));
    assertTrue(first.contains("Sanctions"));
    assertFalse(second.contains("Sanctions"));
    assertTrue(ref.isSanctioned(a.getID()));
    assertFalse(ref.isSanctioned(b.getID()));
  }

  @Test
  public void bombsNeedALandTileAndAWar() {
    Referee ref = game();
    Player a = ref.currentPlayer();
    try {
      ref.launchBomb(a.getID(), tileOf(ref, TileType.WOOD).getCoordinate(), 1,
          dice(1));
      throw new AssertionError("launched in peacetime");
    } catch (IllegalStateException expected) {
      // good
    }
    startWar(ref);
    Tile sea = null;
    for (Tile t : ref.getBoard().getTiles()) {
      if (t.getType() == TileType.SEA) {
        sea = t;
        break;
      }
    }
    try {
      ref.launchBomb(a.getID(), sea.getCoordinate(), 1, dice(1));
      throw new AssertionError("struck the sea");
    } catch (IllegalArgumentException expected) {
      // good
    }
  }

  // ------------------------------------------------------------------ json

  @Test
  public void theGameStateCarriesTheWar() {
    Referee ref = game(3, 7);
    Player a = ref.currentPlayer();
    settle(tileOf(ref, TileType.WOOD), a);
    JsonObject peace = new CatanConverter().getGameState(ref, a.getID());
    assertNull(peace.get("war"));
    startWar(ref);
    ref.getWarState().addUnits(tileOf(ref, TileType.WOOD).getCoordinate(),
        ref.getFaction(a.getID()), UnitType.INFANTRY, 2);
    JsonObject state = new CatanConverter().getGameState(ref, a.getID());
    JsonObject war = state.getAsJsonObject("war");
    assertEquals(ref.getFaction(a.getID()).toString(), war.get("myFaction")
        .getAsString());
    assertEquals(3, war.getAsJsonArray("factions").size());
    for (com.google.gson.JsonElement f : war.getAsJsonArray("factions")) {
      assertEquals(12, f.getAsJsonObject().get("morale").getAsInt());
    }
    assertEquals(1, war.get("nuclearWinter").getAsInt());
    assertEquals(6, war.get("nuclearWinterRollNeeded").getAsInt());
    assertEquals(1, war.getAsJsonArray("units").size());
    assertEquals(2, war.getAsJsonArray("units").get(0).getAsJsonObject()
        .getAsJsonObject("units").get("infantry").getAsInt());
    assertNotNull(war.getAsJsonObject("toDeploy").get("infantry"));
    for (com.google.gson.JsonElement p : state.getAsJsonArray("players")) {
      assertNotNull(p.getAsJsonObject().get("faction"));
      assertNotNull(p.getAsJsonObject().get("eliminated"));
    }
  }
}
