package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.DevelopmentCard;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.UnitType;
import edu.brown.cs.catan.WarState;

/**
 * Cursed Catan, Great War: plays a card of the Great War deck. Like a
 * development card, one can be played a turn, never on the turn it was bought.
 *
 * <pre>
 * mercenaries         {unit:"tank"|"aircraft", tile}   place it on any land tile
 * marines             {tile}                           2 infantry on a coastal tile
 * copium                                               your faction's Morale +2
 * desertion           {tile, unit:"infantry"|"artillery", enemy?}  remove one
 * industrialSabotage  {target: playerId}               steal 2 cards (enemy faction)
 * redacted            {tile}                           2 Devastation
 * propaganda          {enemy?}                         enemy Morale -1
 * firebombs           {tile}                           1 Devastation
 * tankBuster          {tile, enemy?}                   remove 1 enemy tank
 * ackAckGuns          {tile, enemy?}                   remove 1 enemy aircraft
 * reforestation       {tile}                           remove 1 Devastation
 * science                                              gain 2 Research Points
 * secretNuclearTests  (cannot be played: it counts as a Test Site in hand)
 * </pre>
 *
 * Units that arrive by card count as built this turn (they cannot move or
 * fight until the next turn).
 */
public class PlayWarCard implements Action {

  public static final String ID = "playWarCard";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;
  private final Random _random;

  public PlayWarCard(Referee ref, int playerID, JsonObject params) {
    this(ref, playerID, params, new Random());
  }

  /**
   * Constructor with a given source of randomness (for stealing cards).
   */
  public PlayWarCard(Referee ref, int playerID, JsonObject params, Random random) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    _params = params;
    _random = random;
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    String problem = WarTurn.problem(_ref, _player, "play a war card");
    if (problem != null) {
      return fail(problem);
    }
    DevelopmentCard card;
    try {
      card = DevelopmentCard.fromWarId(_params.get("card").getAsString());
    } catch (RuntimeException e) {
      return fail("Choose a war card to play.");
    }
    if (card == DevelopmentCard.SECRET_NUCLEAR_TESTS) {
      return fail("Secret Nuclear Tests is never played: while it is in your hand it counts as a Test Site.");
    }
    if (_player.getDevCards().get(card) < 1) {
      return fail(String.format("You don't have %s.", card));
    }
    if (_ref.getTurn().devHasBeenPlayed()) {
      return fail("You already played a development card this turn.");
    }
    if (!_ref.getTurn().hadInitialDevCard(card)) {
      return fail("You cannot play a card on the turn you bought it.");
    }

    String[] what; // [to the player, to everybody else]
    Integer victim = null;
    try {
      what = apply(card);
      victim = _victimID;
    } catch (IllegalArgumentException e) {
      return fail(e.getMessage());
    }
    _player.playDevelopmentCard(card);
    _ref.playDevCard();

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      String message;
      if (p.equals(_player)) {
        message = what[0];
      } else if (victim != null && p.getID() == victim) {
        message = what[2];
      } else {
        message = what[1];
      }
      toRet.put(p.getID(), new ActionResponse(true, message, null));
    }
    return toRet;
  }

  private Integer _victimID = null;

  /** The land tile of the request. */
  private Tile tile() {
    return WarTurn.landTile(_ref, _params, "tile");
  }

  private UnitType unit(UnitType... allowed) {
    try {
      UnitType t = UnitType.fromString(_params.get("unit").getAsString());
      for (UnitType a : allowed) {
        if (a == t) {
          return t;
        }
      }
    } catch (RuntimeException e) {
      // falls through
    }
    StringBuilder names = new StringBuilder();
    for (UnitType a : allowed) {
      names.append(names.length() == 0 ? "" : " or ").append(a);
    }
    throw new IllegalArgumentException("Choose " + names + ".");
  }

  /** The enemy faction named in the request, or the only one possible. */
  private Faction enemy(Set<Faction> candidates, String what) {
    Faction named = WarTurn.faction(_params, "enemy");
    Faction mine = _ref.getFaction(_player.getID());
    if (named != null) {
      if (named == mine || !candidates.contains(named)) {
        throw new IllegalArgumentException(String.format(
            "%s has no %s there.", named, what));
      }
      return named;
    }
    if (candidates.size() == 1) {
      return candidates.iterator().next();
    }
    if (candidates.isEmpty()) {
      throw new IllegalArgumentException("There is nothing like that to remove there.");
    }
    throw new IllegalArgumentException("Choose which enemy faction.");
  }

  private Set<Faction> enemiesWith(Tile tile, UnitType... types) {
    WarState war = _ref.getWarState();
    Faction mine = _ref.getFaction(_player.getID());
    Set<Faction> set = java.util.EnumSet.noneOf(Faction.class);
    for (Faction f : war.getFactionList()) {
      for (UnitType t : types) {
        if (f != mine && war.getUnits(tile.getCoordinate(), f, t) > 0) {
          set.add(f);
        }
      }
    }
    return set;
  }

  private Faction onlyOrNamedEnemy() {
    WarState war = _ref.getWarState();
    Faction mine = _ref.getFaction(_player.getID());
    Set<Faction> others = java.util.EnumSet.noneOf(Faction.class);
    for (Faction f : war.getFactionList()) {
      if (f != mine) {
        others.add(f);
      }
    }
    Faction named = WarTurn.faction(_params, "enemy");
    if (named != null) {
      if (!others.contains(named)) {
        throw new IllegalArgumentException("Choose an enemy faction.");
      }
      return named;
    }
    if (others.size() == 1) {
      return others.iterator().next();
    }
    throw new IllegalArgumentException("Choose which enemy faction.");
  }

  /**
   * Checks the request and carries the card out. Nothing is changed unless
   * everything is valid.
   *
   * @return The message for the player, the one for everybody else, and the
   *         one for the victim of Industrial Sabotage.
   */
  private String[] apply(DevelopmentCard card) {
    WarState war = _ref.getWarState();
    Faction mine = _ref.getFaction(_player.getID());
    String me = _player.getName();
    switch (card) {
    case MERCENARIES: {
      UnitType type = unit(UnitType.TANK, UnitType.AIRCRAFT);
      Tile t = tile();
      war.addBuiltUnit(t.getCoordinate(), mine, type);
      return msg(String.format("You placed a %s with Mercenaries.", type),
          String.format("%s placed a %s with Mercenaries.", me, type));
    }
    case MARINES: {
      Tile t = tile();
      if (!isCoastal(t)) {
        throw new IllegalArgumentException(
            "Marines land on a coastal tile: one next to the sea.");
      }
      war.addBuiltUnit(t.getCoordinate(), mine, UnitType.INFANTRY);
      war.addBuiltUnit(t.getCoordinate(), mine, UnitType.INFANTRY);
      return msg("You landed 2 infantry with Marines.", String.format(
          "%s landed 2 infantry with Marines.", me));
    }
    case COPIUM: {
      int now = war.changeMorale(mine, 2);
      return msg(String.format("Copium: %s Morale is now %d.", mine, now),
          String.format("%s played Copium: %s Morale +2.", me, mine));
    }
    case DESERTION: {
      UnitType type = unit(UnitType.INFANTRY, UnitType.ARTILLERY);
      Tile t = tile();
      Faction victim = enemy(enemiesWith(t, type), type.toString());
      war.killUnits(t.getCoordinate(), victim, type, 1);
      return msg(String.format("Desertion: one %s %s deserted.", victim, type),
          String.format("%s played Desertion: one %s %s deserted.", me, victim,
              type));
    }
    case INDUSTRIAL_SABOTAGE:
      return sabotage();
    case REDACTED: {
      Tile t = tile();
      boolean waste = _ref.devastate(t.getCoordinate(), 2);
      return msg("[REDACTED]: 2 Devastation." + (waste ? " The tile became wasteland." : ""),
          String.format("%s played [REDACTED]: 2 Devastation on a tile.%s", me,
              waste ? " It became wasteland." : ""));
    }
    case PROPAGANDA: {
      Faction target = onlyOrNamedEnemy();
      int now = war.changeMorale(target, -1);
      return msg(String.format("Propaganda: %s Morale is now %d.", target, now),
          String.format("%s played Propaganda: %s Morale -1.", me, target));
    }
    case FIREBOMBS: {
      Tile t = tile();
      boolean waste = _ref.devastate(t.getCoordinate(), 1);
      return msg("Firebombs: 1 Devastation." + (waste ? " The tile became wasteland." : ""),
          String.format("%s played Firebombs: 1 Devastation on a tile.%s", me,
              waste ? " It became wasteland." : ""));
    }
    case TANK_BUSTER: {
      Tile t = tile();
      Faction victim = enemy(enemiesWith(t, UnitType.TANK), "tank");
      war.killUnits(t.getCoordinate(), victim, UnitType.TANK, 1);
      return msg(String.format("Tank Buster destroyed a %s tank.", victim),
          String.format("%s played Tank Buster: a %s tank was destroyed.", me,
              victim));
    }
    case ACK_ACK_GUNS: {
      Tile t = tile();
      Faction victim = enemy(enemiesWith(t, UnitType.AIRCRAFT), "aircraft");
      war.killUnits(t.getCoordinate(), victim, UnitType.AIRCRAFT, 1);
      return msg(String.format("Ack-Ack Guns shot down a %s aircraft.", victim),
          String.format("%s played Ack-Ack Guns: a %s aircraft was shot down.",
              me, victim));
    }
    case REFORESTATION: {
      Tile t = tile();
      if (war.getDevastation(t.getCoordinate()) < 1) {
        throw new IllegalArgumentException("That tile has no Devastation.");
      }
      war.removeDevastation(t.getCoordinate());
      return msg("Reforestation: 1 Devastation removed.", String.format(
          "%s played Reforestation: 1 Devastation removed.", me));
    }
    case SCIENCE: {
      war.addResearchPoints(mine, 2);
      return msg(String.format("Science! %s gains 2 Research Points (now %d banked).",
          mine, war.getBankedPoints(mine)), String.format(
          "%s played Science!: %s gains 2 Research Points.", me, mine));
    }
    default:
      throw new IllegalArgumentException("That is not a card you can play.");
    }
  }

  private String[] msg(String toMe, String toOthers) {
    return new String[] { toMe, toOthers, toOthers };
  }

  private boolean isCoastal(Tile t) {
    for (Tile other : _ref.getBoard().getTiles()) {
      if (other.getType() == TileType.SEA
          && other.getCoordinate().isAdjacentTo(t.getCoordinate())) {
        return true;
      }
    }
    return false;
  }

  /** Industrial Sabotage: steal 2 random resource cards from an enemy. */
  private String[] sabotage() {
    Player target;
    try {
      target = _ref.getPlayerByID(_params.get("target").getAsInt());
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Choose a player to steal from.");
    }
    Faction mine = _ref.getFaction(_player.getID());
    if (target == null || target.equals(_player)
        || _ref.getFaction(target.getID()) == mine) {
      throw new IllegalArgumentException(
          "Choose a player of an enemy faction.");
    }
    List<Resource> pool = new ArrayList<>();
    for (Resource r : Resource.values()) {
      if (r == Resource.WILDCARD) {
        continue;
      }
      for (int i = 0; i < (int) Math.floor(target.getResources().get(r)); i++) {
        pool.add(r);
      }
    }
    if (pool.isEmpty()) {
      throw new IllegalArgumentException(String.format(
          "%s has no cards to steal.", target.getName()));
    }
    List<Resource> taken = new ArrayList<>();
    for (int i = 0; i < 2 && !pool.isEmpty(); i++) {
      taken.add(pool.remove(_random.nextInt(pool.size())));
    }
    for (Resource r : taken) {
      target.removeResource(r, 1);
      _player.addResource(r, 1);
    }
    _victimID = target.getID();
    String list = taken.toString();
    return new String[] {
        String.format("Industrial Sabotage: you stole %s from %s.", list,
            target.getName()),
        String.format("%s stole %d cards from %s with Industrial Sabotage.",
            _player.getName(), taken.size(), target.getName()),
        String.format("%s stole %s from you with Industrial Sabotage.",
            _player.getName(), list) };
  }
}
