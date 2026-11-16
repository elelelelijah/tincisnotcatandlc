package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;

import edu.brown.cs.board.Building;
import edu.brown.cs.board.Capitol;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileStructure;
import edu.brown.cs.board.TileType;

/**
 * Resolves one fight of the Great War on a tile: aircraft first, then ground
 * troops, both sides rolling at the same time in each step; building damage
 * (fortresses first); sieges; and occupation.
 *
 * <p>
 * Dice are read in a fixed order so that tests can script them: step one,
 * the attacker's aircraft assigned to buildings, then the attacker's other
 * aircraft, then the defender's aircraft; step two, the attacker's ground
 * troops fighting troops (infantry, artillery, tanks), the defender's ground
 * troops (same order), the attacker's ground troops fighting buildings
 * (same order); finally 2 dice for each siege.
 *
 * <p>
 * Casualties are picked automatically: a side that loses units loses its
 * cheapest first (infantry, then artillery, aircraft, tanks); aircraft that
 * hit troops take the strongest first.
 */
public final class WarCombat {

  private static final int BUILDING_HIT = 5;
  private static final int NATURAL_HIT = 6;
  private static final UnitType[] GROUND = { UnitType.INFANTRY,
      UnitType.ARTILLERY, UnitType.TANK };
  /** What an attacker removes first when it hits troops. */
  private static final UnitType[] STRONGEST_FIRST = { UnitType.TANK,
      UnitType.ARTILLERY, UnitType.INFANTRY };

  private WarCombat() {
  }

  // ------------------------------------------------------------- unit stats

  public static int dice(WarState war, Faction f, UnitType t) {
    switch (t) {
    case INFANTRY:
      return 1 + (war.hasTech(f, Tech.INFANTRY_DIE_1) ? 1 : 0);
    case ARTILLERY:
      return 2;
    case TANK:
      return 3;
    default:
      return 1 + (war.hasTech(f, Tech.AIRCRAFT_DIE_1) ? 1 : 0);
    }
  }

  /** The roll a unit needs against troops before bonuses. */
  public static int baseToHit(UnitType t) {
    switch (t) {
    case INFANTRY:
      return 6;
    case AIRCRAFT:
      return 6;
    default:
      return 5;
    }
  }

  public static int toHitBonus(WarState war, Faction f, UnitType t) {
    switch (t) {
    case INFANTRY:
      return war.hasTech(f, Tech.INFANTRY_HIT_1) ? 1 : 0;
    case ARTILLERY:
      return war.hasTech(f, Tech.ARTILLERY_HIT_1) ? 1 : 0;
    case TANK:
      return (war.hasTech(f, Tech.TANK_HIT_1) ? 1 : 0)
          + (war.hasTech(f, Tech.TANK_HIT_2) ? 1 : 0);
    default:
      return 0;
    }
  }

  /** Extra damage each hit on a building deals. */
  public static int buildingDamageBonus(WarState war, Faction f, UnitType t) {
    switch (t) {
    case INFANTRY:
      return war.hasTech(f, Tech.INFANTRY_BUILDINGS_1) ? 1 : 0;
    case ARTILLERY:
      return (war.hasTech(f, Tech.ARTILLERY_BUILDINGS_1) ? 1 : 0)
          + (war.hasTech(f, Tech.ARTILLERY_BUILDINGS_2) ? 1 : 0);
    default:
      return 0;
    }
  }

  /**
   * A roll of 6 always hits; otherwise the roll plus bonuses minus the
   * entrenched penalty has to reach the unit's number.
   */
  static boolean hitsTroops(int roll, UnitType t, int bonus, int penalty) {
    return roll >= NATURAL_HIT || roll + bonus - penalty >= baseToHit(t);
  }

  static boolean hitsBuilding(int roll) {
    return roll >= BUILDING_HIT;
  }

  // ------------------------------------------------------------- the fight

  /** Something an attacker can damage: a building or a tile structure. */
  private static class Target {
    private Building building;
    private TileStructure structure;
    private Tile tile;
    private int lostHp = 0;

    int hp() {
      return building != null ? building.getHp() : structure.getHitPoints();
    }

    boolean isFortress() {
      return structure != null
          && structure.getKind() == TileStructure.Kind.FORTRESS;
    }
  }

  private static Faction controller(Referee ref, Building b) {
    return b.getOccupier() != null ? b.getOccupier() : ref.getFaction(b.getPlayer()
        .getID());
  }

  /**
   * Whether the acting faction may fight on the tile against the defender:
   * the defender has units there, or something of theirs can be attacked.
   */
  public static boolean canFightAt(Referee ref, Tile tile, Faction attacker,
      Faction defender) {
    WarState war = ref.getWarState();
    return war.getEnemyFactionsOn(tile.getCoordinate(), attacker).contains(
        defender)
        || !targets(ref, tile, defender).isEmpty();
  }

  private static List<Target> targets(Referee ref, Tile tile, Faction defender) {
    List<Target> list = new ArrayList<>();
    for (TileStructure s : tile.getStructures()) {
      if (ref.getFaction(s.getOwnerID()) == defender && s.getHitPoints() > 0) {
        Target t = new Target();
        t.structure = s;
        t.tile = tile;
        list.add(t);
      }
    }
    Set<Building> seen = new LinkedHashSet<>();
    for (Intersection i : tile.getIntersections()) {
      Building b = i.getBuilding();
      if (b != null && seen.add(b) && controller(ref, b) == defender) {
        Target t = new Target();
        t.building = b;
        t.tile = tile;
        list.add(t);
      }
    }
    sortTargets(list);
    return list;
  }

  /** Fortresses first, then the weakest. */
  private static void sortTargets(List<Target> list) {
    Collections.sort(list, new Comparator<Target>() {
      @Override
      public int compare(Target a, Target b) {
        if (a.isFortress() != b.isFortress()) {
          return a.isFortress() ? -1 : 1;
        }
        return Integer.compare(a.hp(), b.hp());
      }
    });
  }

  /**
   * Who picks the casualties of a faction: the id of a player, or null when
   * they are picked automatically.
   */
  public interface Chooser {
    Integer chooserFor(Faction faction);
  }

  /** A faction's casualties have to be chosen by somebody. */
  public static final class Request {
    private final int _chooser;
    private final Faction _victim;
    private final int _required;
    private final Map<UnitType, Integer> _options;
    private final UnitType[] _autoOrder;
    private final String _reason;
    private Map<UnitType, Integer> _chosen = null;
    private boolean _registered = false;

    private Request(int chooser, Faction victim, int required,
        Map<UnitType, Integer> options, UnitType[] autoOrder, String reason) {
      _chooser = chooser;
      _victim = victim;
      _required = required;
      _options = options;
      _autoOrder = autoOrder;
      _reason = reason;
    }

    /** The player who has to choose. */
    public int getChooser() {
      return _chooser;
    }

    /** The faction that loses the units. */
    public Faction getVictim() {
      return _victim;
    }

    /** How many units have to be chosen. */
    public int getRequired() {
      return _required;
    }

    /** The ground units the losses can be taken from. */
    public Map<UnitType, Integer> getOptions() {
      return Collections.unmodifiableMap(_options);
    }

    public String getReason() {
      return _reason;
    }

    /** Whether a follow-up has already been made for this choice. */
    public boolean isRegistered() {
      return _registered;
    }

    public void markRegistered() {
      _registered = true;
    }
  }

  /** What a fight has come to: finished, or waiting for choices. */
  public static final class Outcome {
    private final boolean _finished;
    private final String _log;
    private final List<Request> _pending;

    private Outcome(boolean finished, String log, List<Request> pending) {
      _finished = finished;
      _log = log;
      _pending = pending;
    }

    public boolean isFinished() {
      return _finished;
    }

    /** Everything that has happened so far. */
    public String getLog() {
      return _log;
    }

    /** The choices that are still needed. */
    public List<Request> getPending() {
      return _pending;
    }
  }

  /**
   * Starts a fight that stops whenever somebody has a real choice of
   * casualties (see {@link Fight#begin()}).
   *
   * @param chooser
   *          Who picks each faction's casualties, or null for automatic.
   * @throws IllegalArgumentException
   *           If the fight is not allowed.
   */
  public static Fight start(Referee ref, Faction attacker, Faction defender,
      Tile tile, Map<UnitType, Integer> toBuildings, IntSupplier die,
      Chooser chooser) {
    return new Fight(ref, attacker, defender, tile, toBuildings, die, chooser);
  }

  /**
   * Fights with every casualty picked automatically.
   *
   * @return A description of the fight.
   * @throws IllegalArgumentException
   *           If the fight is not allowed.
   */
  public static String fight(Referee ref, Faction attacker, Faction defender,
      Tile tile, Map<UnitType, Integer> toBuildings, IntSupplier die) {
    Fight f = start(ref, attacker, defender, tile, toBuildings, die, null);
    Outcome o = f.begin();
    while (!o.isFinished()) {
      List<Request> pending = new ArrayList<>(o.getPending());
      for (Request r : pending) {
        o = f.answer(r, null);
      }
    }
    return o.getLog();
  }

  private enum Phase {
    NEW, AIR, GROUND, DONE
  }

  /**
   * One fight. Aircraft roll first, then ground troops. When a side has to
   * pick which of its ground units are lost, the fight waits for the answer.
   */
  public static final class Fight {
    private final Referee _ref;
    private final WarState _war;
    private final Faction _attacker;
    private final Faction _defender;
    private final Tile _tile;
    private final HexCoordinate _at;
    private final IntSupplier _die;
    private final Chooser _chooser;
    private final Map<UnitType, Integer> _toBuildings;
    private final StringBuilder _log = new StringBuilder();
    private final List<Integer> _buildingDamage = new ArrayList<>();
    private List<Target> _targets;
    private int _assigned;
    private Phase _phase = Phase.NEW;
    private final List<Request> _pending = new ArrayList<>();
    // losses already worked out for this step; applied when all is chosen
    private final Map<UnitType, Integer> _killA = new EnumMap<>(UnitType.class);
    private final Map<UnitType, Integer> _killD = new EnumMap<>(UnitType.class);
    private final Map<Request, Boolean> _forAttackerStack = new java.util.IdentityHashMap<>();

    private Fight(Referee ref, Faction attacker, Faction defender, Tile tile,
        Map<UnitType, Integer> toBuildings, IntSupplier die, Chooser chooser) {
      _ref = ref;
      _war = ref.getWarState();
      _attacker = attacker;
      _defender = defender;
      _tile = tile;
      _at = tile.getCoordinate();
      _die = die;
      _chooser = chooser;
      _toBuildings = toBuildings;
    }

    public Faction getAttacker() {
      return _attacker;
    }

    public Faction getDefender() {
      return _defender;
    }

    public Tile getTile() {
      return _tile;
    }

    /**
     * Checks the fight is allowed and plays it up to the first choice.
     *
     * @throws IllegalArgumentException
     *           If the fight is not allowed.
     */
    public Outcome begin() {
      if (_phase != Phase.NEW) {
        throw new IllegalStateException("This fight has already begun.");
      }
      validate();
      _war.markFought(_at);
      _log.append(String.format("%s fights %s.", _attacker, _defender));
      return stepAir();
    }

    private void validate() {
      if (_defender == null || _defender == _attacker
          || !_war.getFactionList().contains(_defender)) {
        throw new IllegalArgumentException("Choose an enemy faction to fight.");
      }
      if (_tile.getType() == TileType.SEA) {
        throw new IllegalArgumentException("Fights happen on land tiles.");
      }
      if (_war.hasFoughtThisTurn(_at)) {
        throw new IllegalArgumentException(
            "There has already been a fight on that tile this turn.");
      }
      int fighters = 0;
      for (UnitType t : UnitType.values()) {
        fighters += _war.getReadyToFight(_at, _attacker, t);
      }
      if (fighters == 0) {
        throw new IllegalArgumentException(
            "You have no units there that can fight (units built this turn cannot).");
      }
      _targets = targets(_ref, _tile, _defender);
      if (!_war.getEnemyFactionsOn(_at, _attacker).contains(_defender)
          && _targets.isEmpty()) {
        throw new IllegalArgumentException(
            "There is nothing of that faction to fight on or next to that tile.");
      }
      _assign.clear();
      _assigned = 0;
      for (UnitType t : UnitType.values()) {
        Integer n = _toBuildings.get(t);
        if (n != null && n > 0) {
          if (n > _war.getReadyToFight(_at, _attacker, t)) {
            throw new IllegalArgumentException(String.format(
                "You don't have %d %s able to fight there.", n, t));
          }
          _assign.put(t, n);
          _assigned += n;
        }
      }
      if (_assigned > 0 && _targets.isEmpty()) {
        throw new IllegalArgumentException("There are no buildings to attack.");
      }
    }

    private final Map<UnitType, Integer> _assign = new EnumMap<>(UnitType.class);

    // ------------------------------------------------ step one: aircraft

    private Outcome stepAir() {
      WarState war = _war;
      int airA = war.getReadyToFight(_at, _attacker, UnitType.AIRCRAFT);
      int airD = war.getReadyToFight(_at, _defender, UnitType.AIRCRAFT);
      int airToBuildings = Math.min(_assign.containsKey(UnitType.AIRCRAFT) ? _assign
          .get(UnitType.AIRCRAFT) : 0, airA);
      int airToTroops = airA - airToBuildings;
      int diceA = dice(war, _attacker, UnitType.AIRCRAFT);
      int diceD = dice(war, _defender, UnitType.AIRCRAFT);
      for (int n = 0; n < airToBuildings * diceA; n++) {
        if (hitsBuilding(_die.getAsInt())) {
          _buildingDamage.add(1);
        }
      }
      int airHitsA = 0;
      for (int n = 0; n < airToTroops * diceA; n++) {
        if (_die.getAsInt() >= NATURAL_HIT) {
          airHitsA++;
        }
      }
      int airHitsD = 0;
      for (int n = 0; n < airD * diceD; n++) {
        if (_die.getAsInt() >= NATURAL_HIT) {
          airHitsD++;
        }
      }
      if (airA + airD > 0) {
        _log.append(String.format(
            " Aircraft: %s scored %d on troops%s, %s scored %d.", _attacker,
            airHitsA, airToBuildings > 0 ? " and " + _buildingDamage.size()
                + " on buildings" : "", _defender, airHitsD));
      }
      // Both sides' casualties are worked out before anything is removed
      int airKilledD = Math.min(airHitsA, airD);
      int groundHitsFromAirA = war.hasTech(_attacker, Tech.AIRCRAFT_GROUND) ? airHitsA
          - airKilledD : 0;
      int airKilledA = Math.min(airHitsD, airA);
      int groundHitsFromAirD = war.hasTech(_defender, Tech.AIRCRAFT_GROUND) ? airHitsD
          - airKilledA : 0;
      _killD.clear();
      _killA.clear();
      if (airKilledD > 0) {
        _killD.put(UnitType.AIRCRAFT, airKilledD);
      }
      if (airKilledA > 0) {
        _killA.put(UnitType.AIRCRAFT, airKilledA);
      }
      _pending.clear();
      // Aircraft that hit troops pick them: their own side chooses
      askOrAllocate(_defender, groundHitsFromAirA, _attacker, STRONGEST_FIRST,
          _killD, "your aircraft hit enemy troops", false);
      askOrAllocate(_attacker, groundHitsFromAirD, _defender, GROUND, _killA,
          "your aircraft hit enemy troops", true);
      _phase = Phase.AIR;
      if (!_pending.isEmpty()) {
        return waiting();
      }
      return afterAir();
    }

    private Outcome afterAir() {
      applyKills(_war, _at, _defender, _killD);
      applyKills(_war, _at, _attacker, _killA);
      if (!_killD.isEmpty() || !_killA.isEmpty()) {
        _log.append(String.format(" Lost in the air: %s %s, %s %s.", _attacker,
            describe(_killA), _defender, describe(_killD)));
      }
      return stepGround();
    }

    // ------------------------------------------- step two: ground troops

    private Outcome stepGround() {
      WarState war = _war;
      int penaltyOnD = war.isEntrenched(_at, _defender) ? 1 : 0;
      int penaltyOnA = war.isEntrenched(_at, _attacker) ? 1 : 0;
      int groundHitsA = 0;
      for (UnitType t : GROUND) {
        int ready = war.getReadyToFight(_at, _attacker, t);
        int toTroops = ready - Math.min(ready, _assign.containsKey(t) ? _assign
            .get(t) : 0);
        for (int n = 0; n < toTroops * dice(war, _attacker, t); n++) {
          if (hitsTroops(_die.getAsInt(), t, toHitBonus(war, _attacker, t),
              penaltyOnD)) {
            groundHitsA++;
          }
        }
      }
      int groundHitsD = 0;
      for (UnitType t : GROUND) {
        int ready = war.getReadyToFight(_at, _defender, t);
        for (int n = 0; n < ready * dice(war, _defender, t); n++) {
          if (hitsTroops(_die.getAsInt(), t, toHitBonus(war, _defender, t),
              penaltyOnA)) {
            groundHitsD++;
          }
        }
      }
      for (UnitType t : GROUND) {
        int ready = war.getReadyToFight(_at, _attacker, t);
        int toB = Math.min(ready, _assign.containsKey(t) ? _assign.get(t) : 0);
        int bonus = buildingDamageBonus(war, _attacker, t);
        for (int n = 0; n < toB * dice(war, _attacker, t); n++) {
          if (hitsBuilding(_die.getAsInt())) {
            _buildingDamage.add(1 + bonus);
          }
        }
      }
      _groundFight = groundHitsA + groundHitsD > 0
          || war.getEnemyFactionsOn(_at, _attacker).contains(_defender);
      _groundHitsA = groundHitsA;
      _groundHitsD = groundHitsD;
      _killA.clear();
      _killD.clear();
      _pending.clear();
      // Each side picks which of its own ground troops are lost
      askOrAllocate(_defender, groundHitsA, _defender, GROUND, _killD,
          "ground hits", false);
      askOrAllocate(_attacker, groundHitsD, _attacker, GROUND, _killA,
          "ground hits", true);
      _phase = Phase.GROUND;
      if (!_pending.isEmpty()) {
        return waiting();
      }
      return afterGround();
    }

    private boolean _groundFight;
    private int _groundHitsA;
    private int _groundHitsD;

    private Outcome afterGround() {
      applyKills(_war, _at, _defender, _killD);
      applyKills(_war, _at, _attacker, _killA);
      if (_groundFight) {
        _log.append(String.format(
            " Ground: %s scored %d hits and lost %s; %s scored %d hits and lost %s.",
            _attacker, _groundHitsA, describe(_killA), _defender, _groundHitsD,
            describe(_killD)));
      }
      if (!_buildingDamage.isEmpty()) {
        _log.append(applyBuildingDamage(_ref, _tile, _attacker, _defender,
            _buildingDamage, _die, _targets));
      } else if (_assigned > 0) {
        _log.append(" The attack on the buildings did no damage.");
      }
      _phase = Phase.DONE;
      return new Outcome(true, _log.toString(), Collections.<Request> emptyList());
    }

    // ------------------------------------------------------------ choices

    /**
     * Works out the casualties of the victim's ground troops: they are picked
     * automatically unless somebody has a real choice (the victim has several
     * kinds of ground unit and the hits do not take them all), in which case
     * a request is added.
     */
    private void askOrAllocate(Faction victim, int hits, Faction hitter,
        UnitType[] autoOrder, Map<UnitType, Integer> losses, String reason,
        boolean attackerStack) {
      int total = 0;
      int kinds = 0;
      Map<UnitType, Integer> options = new EnumMap<>(UnitType.class);
      Faction stackOwner = attackerStack ? _attacker : _defender;
      Faction owner = reason.startsWith("ground") ? victim : victim;
      for (UnitType t : GROUND) {
        int have = _war.getUnits(_at, stackOwner, t)
            - (losses.containsKey(t) ? losses.get(t) : 0);
        if (have > 0) {
          options.put(t, have);
          total += have;
          kinds++;
        }
      }
      int required = Math.min(hits, total);
      if (required <= 0) {
        return;
      }
      // Ground hits are chosen by the victim; aircraft hits by the hitter
      Faction chooserFaction = reason.startsWith("ground") ? stackOwner : hitter;
      Integer chooser = _chooser == null ? null : _chooser.chooserFor(
          chooserFaction);
      if (chooser == null || required >= total || kinds <= 1) {
        allocateGround(_war, _at, stackOwner, required, autoOrder, losses);
        return;
      }
      Request r = new Request(chooser, stackOwner, required, options, autoOrder,
          reason);
      _forAttackerStack.put(r, attackerStack);
      _pending.add(r);
    }

    private Outcome waiting() {
      return new Outcome(false, _log.toString(), new ArrayList<>(_pending));
    }

    /**
     * Gives the answer to a request. With no losses given (null) the casualties
     * are picked automatically.
     *
     * @param losses
     *          How many of each ground unit type are lost.
     * @throws IllegalArgumentException
     *           If the answer does not fit the request.
     */
    public Outcome answer(Request request, Map<UnitType, Integer> losses) {
      if (!_pending.contains(request)) {
        throw new IllegalStateException("That choice is not being waited for.");
      }
      Map<UnitType, Integer> chosen = new EnumMap<>(UnitType.class);
      if (losses == null) {
        allocateFrom(request, request._autoOrder, chosen);
      } else {
        int sum = 0;
        for (Map.Entry<UnitType, Integer> e : losses.entrySet()) {
          int n = e.getValue() == null ? 0 : e.getValue();
          if (n < 0 || !WarState.isGround(e.getKey())) {
            throw new IllegalArgumentException("Choose ground units to lose.");
          }
          if (n == 0) {
            continue;
          }
          Integer have = request._options.get(e.getKey());
          if (have == null || n > have) {
            throw new IllegalArgumentException(String.format(
                "There are not %d %s to lose.", n, e.getKey()));
          }
          chosen.put(e.getKey(), n);
          sum += n;
        }
        if (sum != request._required) {
          throw new IllegalArgumentException(String.format(
              "Choose exactly %d unit%s to lose (you chose %d).",
              request._required, request._required == 1 ? "" : "s", sum));
        }
      }
      request._chosen = chosen;
      _pending.remove(request);
      Map<UnitType, Integer> target = _forAttackerStack.get(request) ? _killA
          : _killD;
      for (Map.Entry<UnitType, Integer> e : chosen.entrySet()) {
        Integer old = target.get(e.getKey());
        target.put(e.getKey(), (old == null ? 0 : old) + e.getValue());
      }
      if (!_pending.isEmpty()) {
        return waiting();
      }
      return _phase == Phase.AIR ? afterAir() : afterGround();
    }

    private void allocateFrom(Request r, UnitType[] order,
        Map<UnitType, Integer> out) {
      int left = r._required;
      for (UnitType t : order) {
        if (left <= 0) {
          break;
        }
        Integer have = r._options.get(t);
        int take = Math.min(left, have == null ? 0 : have);
        if (take > 0) {
          out.put(t, take);
          left -= take;
        }
      }
    }
  }

  private static int countBuildingHits(List<Integer> damage) {
    return damage.size();
  }

  private static String describe(Map<UnitType, Integer> losses) {
    if (losses.isEmpty()) {
      return "nothing";
    }
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<UnitType, Integer> e : losses.entrySet()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(e.getValue()).append(' ').append(e.getKey());
    }
    return sb.toString();
  }

  /**
   * Takes ground casualties out of the faction's stack in the given order,
   * adding them to the loss list (nothing is removed yet).
   */
  private static void allocateGround(WarState war, HexCoordinate at,
      Faction victim, int hits, UnitType[] order, Map<UnitType, Integer> losses) {
    int left = hits;
    for (UnitType t : order) {
      if (left <= 0) {
        break;
      }
      int already = losses.containsKey(t) ? losses.get(t) : 0;
      int available = war.getUnits(at, victim, t) - already;
      int take = Math.min(left, Math.max(0, available));
      if (take > 0) {
        losses.put(t, already + take);
        left -= take;
      }
    }
  }

  private static void applyKills(WarState war, HexCoordinate at, Faction f,
      Map<UnitType, Integer> losses) {
    for (Map.Entry<UnitType, Integer> e : losses.entrySet()) {
      war.killUnits(at, f, e.getKey(), e.getValue());
    }
  }

  // ---------------------------------------------------- damage and sieges

  private static String applyBuildingDamage(Referee ref, Tile tile,
      Faction attacker, Faction defender, List<Integer> damage, IntSupplier die,
      List<Target> targets) {
    WarState war = ref.getWarState();
    StringBuilder log = new StringBuilder();
    int total = 0;
    for (int hit : damage) {
      total += hit;
      Target target = null;
      boolean fortressAlive = false;
      for (Target t : targets) {
        if (t.isFortress() && t.hp() > 0) {
          fortressAlive = true;
        }
      }
      // A fortress has to fall before anything else on the tile can be hit
      for (Target t : targets) {
        if (t.hp() > 0 && (!fortressAlive || t.isFortress())) {
          target = t;
          break;
        }
      }
      if (target == null) {
        break; // everything here is already destroyed
      }
      int dealt = Math.min(hit, target.hp());
      target.lostHp += dealt;
      if (target.building != null) {
        target.building.setHp(target.building.getHp() - dealt);
      } else {
        target.structure.damage(dealt);
      }
      // Re-sort: the weakest of the rest next
      Collections.sort(targets, new Comparator<Target>() {
        @Override
        public int compare(Target a, Target b) {
          if (a.isFortress() != b.isFortress()) {
            return a.isFortress() ? -1 : 1;
          }
          return Integer.compare(a.hp(), b.hp());
        }
      });
    }
    log.append(String.format(" %d building damage.", total));

    // Destroyed buildings and structures
    for (Target t : targets) {
      if (t.lostHp == 0 || t.hp() > 0) {
        continue;
      }
      if (t.structure != null) {
        t.tile.removeStructure(t.structure.getKind(), t.structure.getOwnerID());
        log.append(String.format(" A %s was destroyed.", t.structure.getKind()
            .toString().toLowerCase()));
      } else {
        log.append(occupy(ref, t.building, attacker, defender));
      }
    }
    // Sieges: every settlement, city and factory that lost HP but stands
    for (Target t : targets) {
      Building b = t.building;
      if (b == null || t.lostHp == 0 || b.getHp() <= 0 || b.isRuined()
          || b instanceof Capitol) {
        continue;
      }
      Faction owner = ref.getFaction(b.getPlayer().getID());
      int roll = die.getAsInt() + die.getAsInt();
      if (roll > war.getMorale(owner)) {
        log.append(String.format(" A damaged building surrendered (rolled %d against Morale %d).",
            roll, war.getMorale(owner)));
        log.append(occupy(ref, b, attacker, defender));
      }
    }
    return log.toString();
  }

  /**
   * A building is taken (0 HP or surrender) or liberated. It lies in ruins
   * with 4 HP and produces nothing. Occupying costs the defender 1 Morale;
   * liberating gives the liberator 1.
   */
  static String occupy(Referee ref, Building b, Faction attacker,
      Faction defender) {
    WarState war = ref.getWarState();
    Faction owner = ref.getFaction(b.getPlayer().getID());
    b.setRuined(true);
    b.setHp(4);
    if (b.getOccupier() != null && owner == attacker) {
      b.setOccupier(null);
      war.changeMorale(attacker, 1);
      return String.format(" %s liberated a building (+1 Morale).", attacker);
    }
    b.setOccupier(attacker);
    war.changeMorale(defender, -1);
    return String.format(" %s occupied a building (%s -1 Morale).", attacker,
        defender);
  }
}
