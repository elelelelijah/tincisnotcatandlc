package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;

/**
 * Trading the Cursed Catan resources: coal and concrete with players and the
 * bank, coal 3:1 and uranium 2:1 with the bank, and the uranium lock.
 */
public class CursedTradeTest {

  private static Referee newGame(boolean cursed) {
    JsonObject json = new JsonObject();
    json.addProperty("numPlayers", 2);
    json.addProperty("isDecimal", false);
    json.addProperty("isDynamic", false);
    json.addProperty("isStandard", false);
    json.addProperty("isCitiesAndKnights", false);
    json.addProperty("isCursedCatan", cursed);
    Referee ref = new MasterReferee(new GameSettings(json));
    ref.addPlayer("A", "Red");
    ref.addPlayer("B", "Blue");
    ref.setGameStatus(GameStatus.PROGRESS);
    return ref;
  }

  private static Player other(Referee ref, Player p) {
    for (Player q : ref.getPlayers()) {
      if (!q.equals(p)) {
        return q;
      }
    }
    return null;
  }

  private static void set(Player p, Resource r, double amount) {
    double have = p.getResources().get(r);
    if (have > amount) {
      p.removeResource(r, have - amount);
    } else if (have < amount) {
      p.addResource(r, amount - have);
    }
  }

  private static double has(Player p, Resource r) {
    return p.getResources().get(r);
  }

  private static ActionResponse bank(Referee ref, Player p, String give,
      String get, int amount) {
    JsonObject params = new JsonObject();
    params.addProperty("toGive", give);
    params.addProperty("toGet", get);
    params.addProperty("amount", amount);
    return new TradeWithBank(ref, p.getID(), params).execute().get(p.getID());
  }

  private static void startWar(Referee ref) {
    for (int i = 0; i < 20 && !ref.isGreatWarTriggered(); i++) {
      ref.recordCoalSpent();
      ref.advanceIndustryTrack();
    }
    assertTrue(ref.isGreatWarTriggered());
  }

  private static ActionResponse propose(Referee ref, Player p, String a,
      int amountA, String b, int amountB) {
    JsonObject trade = new JsonObject();
    trade.addProperty(a, amountA);
    trade.addProperty(b, amountB);
    JsonObject params = new JsonObject();
    params.add("trade", trade);
    return new ProposeTrade(ref, p.getID(), params).execute().get(p.getID());
  }

  @Test
  public void bankRatesAreThreeToOneForCoalTwoToOneForUranium() {
    Referee ref = newGame(true);
    Player p = ref.getPlayers().iterator().next();
    Map<Resource, Double> rates = ref.getBankRates(p.getID());
    assertEquals(3.0, rates.get(Resource.COAL), 0.001);
    assertEquals(2.0, rates.get(Resource.URANIUM), 0.001);
    assertEquals(4.0, rates.get(Resource.CONCRETE), 0.001);
    assertEquals(4.0, rates.get(Resource.WOOD), 0.001);
  }

  @Test
  public void coalTradesWithTheBankThreeForOne() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    set(p, Resource.COAL, 7);
    set(p, Resource.WOOD, 0);
    assertTrue(bank(ref, p, "coal", "wood", 2).getSuccess());
    assertEquals(1.0, has(p, Resource.COAL), 0.001);
    assertEquals(2.0, has(p, Resource.WOOD), 0.001);
  }

  @Test
  public void concreteTradesWithTheBankFourForOne() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    set(p, Resource.CONCRETE, 9);
    set(p, Resource.ORE, 0);
    assertTrue(bank(ref, p, "concrete", "ore", 2).getSuccess());
    assertEquals(1.0, has(p, Resource.CONCRETE), 0.001);
    assertEquals(2.0, has(p, Resource.ORE), 0.001);
  }

  @Test
  public void youCanBuyCoalAndConcreteFromTheBank() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    set(p, Resource.WOOD, 8);
    set(p, Resource.COAL, 0);
    set(p, Resource.CONCRETE, 0);
    assertTrue(bank(ref, p, "wood", "coal", 1).getSuccess());
    assertTrue(bank(ref, p, "wood", "concrete", 1).getSuccess());
    assertEquals(1.0, has(p, Resource.COAL), 0.001);
    assertEquals(1.0, has(p, Resource.CONCRETE), 0.001);
    assertEquals(0.0, has(p, Resource.WOOD), 0.001);
  }

  @Test
  public void uraniumCanBeGivenToTheBankTwoForOne() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    set(p, Resource.URANIUM, 5);
    set(p, Resource.ORE, 0);
    ActionResponse r = bank(ref, p, "uranium", "ore", 2);
    assertTrue(r.getMessage(), r.getSuccess());
    assertEquals(1.0, has(p, Resource.URANIUM), 0.001);
    assertEquals(2.0, has(p, Resource.ORE), 0.001);
  }

  @Test
  public void uraniumCannotBeBoughtFromTheBankBeforeTheWar() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    set(p, Resource.WOOD, 8);
    set(p, Resource.URANIUM, 0);
    ActionResponse r = bank(ref, p, "wood", "uranium", 1);
    assertFalse(r.getSuccess());
    assertEquals(8.0, has(p, Resource.WOOD), 0.001);
    assertEquals(0.0, has(p, Resource.URANIUM), 0.001);
  }

  @Test
  public void uraniumCanBeBoughtFromTheBankOnceTheWarHasStarted() {
    Referee ref = newGame(true);
    startWar(ref);
    Player p = ref.currentPlayer();
    set(p, Resource.WOOD, 8);
    set(p, Resource.URANIUM, 0);
    assertTrue(bank(ref, p, "wood", "uranium", 1).getSuccess());
    assertEquals(1.0, has(p, Resource.URANIUM), 0.001);
  }

  @Test
  public void bankTradeStillNeedsEnoughOfTheResourceAndYourTurn() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    set(p, Resource.COAL, 2);
    assertFalse(bank(ref, p, "coal", "wood", 1).getSuccess());
    Player q = other(ref, p);
    set(q, Resource.COAL, 6);
    assertFalse(bank(ref, q, "coal", "wood", 1).getSuccess());
    assertEquals(6.0, has(q, Resource.COAL), 0.001);
  }

  @Test
  public void wildcardCannotBeTraded() {
    Referee ref = newGame(true);
    Player p = ref.currentPlayer();
    assertFalse(bank(ref, p, "wood", "wildcard", 1).getSuccess());
    assertFalse(propose(ref, p, "wood", -1, "wildcard", 1).getSuccess());
  }

  @Test
  public void coalAndConcreteTradeBetweenPlayers() {
    Referee ref = newGame(true);
    Player a = ref.currentPlayer();
    Player b = other(ref, a);
    set(a, Resource.COAL, 3);
    set(a, Resource.CONCRETE, 0);
    set(b, Resource.COAL, 0);
    set(b, Resource.CONCRETE, 2);

    ActionResponse proposed = propose(ref, a, "coal", -2, "concrete", 2);
    assertTrue(proposed.getMessage(), proposed.getSuccess());

    JsonObject accept = new JsonObject();
    accept.addProperty("tradeAccepted", true);
    FollowUpAction review = ref.getNextFollowUp(b.getID());
    review.setupAction(ref, b.getID(), accept);
    review.execute();

    JsonObject finalize = new JsonObject();
    finalize.addProperty("tradeAccepted", true);
    finalize.addProperty("trader", a.getID());
    finalize.addProperty("tradee", b.getID());
    FollowUpAction response = ref.getNextFollowUp(a.getID());
    response.setupAction(ref, a.getID(), finalize);
    ActionResponse done = response.execute().get(a.getID());
    assertTrue(done.getMessage(), done.getSuccess());

    assertEquals(1.0, has(a, Resource.COAL), 0.001);
    assertEquals(2.0, has(a, Resource.CONCRETE), 0.001);
    assertEquals(2.0, has(b, Resource.COAL), 0.001);
    assertEquals(0.0, has(b, Resource.CONCRETE), 0.001);
  }

  @Test
  public void uraniumCannotBeTradedBetweenPlayersBeforeTheWar() {
    Referee ref = newGame(true);
    Player a = ref.currentPlayer();
    set(a, Resource.URANIUM, 2);
    assertFalse(propose(ref, a, "uranium", -1, "wood", 1).getSuccess());
    assertFalse(propose(ref, a, "wood", -1, "uranium", 1).getSuccess());
  }

  @Test
  public void uraniumIsNoLongerLockedOnceTheWarHasStarted() {
    Referee ref = newGame(true);
    assertTrue(CatanFormats.tradeLockMessage(ref, Resource.URANIUM) != null);
    startWar(ref);
    assertNull(CatanFormats.tradeLockMessage(ref, Resource.URANIUM));
  }

  @Test
  public void zeroEntriesForLockedResourcesAreIgnored() {
    // The client always sends every resource, including uranium: 0
    Referee ref = newGame(true);
    Player a = ref.currentPlayer();
    set(a, Resource.COAL, 3);
    JsonObject trade = new JsonObject();
    trade.addProperty("brick", 0);
    trade.addProperty("coal", -2);
    trade.addProperty("concrete", 1);
    trade.addProperty("uranium", 0);
    JsonObject params = new JsonObject();
    params.add("trade", trade);
    ActionResponse r = new ProposeTrade(ref, a.getID(), params).execute().get(
        a.getID());
    assertTrue(r.getMessage(), r.getSuccess());
  }

  @Test
  public void cursedResourcesCannotBeTradedInTheBaseGame() {
    Referee ref = newGame(false);
    Player a = ref.currentPlayer();
    set(a, Resource.WOOD, 8);
    assertFalse(bank(ref, a, "wood", "coal", 1).getSuccess());
    assertFalse(bank(ref, a, "coal", "wood", 1).getSuccess());
    assertFalse(propose(ref, a, "wood", -1, "concrete", 1).getSuccess());
  }
}
