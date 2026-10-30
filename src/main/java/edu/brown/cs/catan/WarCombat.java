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
    // Fortresses first, then the weakest
    Collections.sort(list, new Comparator<Target>() {
      @Override
      public int compare(Target a, Target b) {
        if (a.isFortress() != b.isFortress()) {
          return a.isFortress() ? -1 : 1;
        }
        return Integer.compare(a.hp(), b.hp());
      }
    });
    return list;
  }

  /**
   * Fights.
   *
   * @param ref
   *          The referee.
   * @param attacker
   *          The faction of the acting player.
   * @param defender
   *          The enemy faction fought here.
   * @param tile
   *          Where the fight is.
   * @param toBuildings
   *          How many units of each type attack buildings instead of troops.
   * @param die
   *          Supplies each D6 roll.
   * @return A description of the fight.
   * @throws IllegalArgumentException
   *           If the fight is not allowed.
   */
  public static String fight(Referee ref, Faction attacker, Faction defender,
      Tile tile, Map<UnitType, Integer> toBuildings, IntSupplier die) {
    WarState war = ref.getWarState();
    HexCoordinate at = tile.getCoordinate();
    if (defender == null || defender == attacker
        || !war.getFactionList().contains(defender)) {
      throw new IllegalArgumentException("Choose an enemy faction to fight.");
    }
    if (tile.getType() == TileType.SEA) {
      throw new IllegalArgumentException("Fights happen on land tiles.");
    }
    if (war.hasFoughtThisTurn(at)) {
      throw new IllegalArgumentException(
          "There has already been a fight on that tile this turn.");
    }
    int fighters = 0;
    for (UnitType t : UnitType.values()) {
      fighters += war.getReadyToFight(at, attacker, t);
    }
    if (fighters == 0) {
      throw new IllegalArgumentException(
          "You have no units there that can fight (units built this turn cannot).");
    }
    List<Target> targets = targets(ref, tile, defender);
    if (!war.getEnemyFactionsOn(at, attacker).contains(defender)
        && targets.isEmpty()) {
      throw new IllegalArgumentException(
          "There is nothing of that faction to fight on or next to that tile.");
    }
    Map<UnitType, Integer> assign = new EnumMap<>(UnitType.class);
    int assigned = 0;
    for (UnitType t : UnitType.values()) {
      Integer n = toBuildings.get(t);
      if (n != null && n > 0) {
        if (n > war.getReadyToFight(at, attacker, t)) {
          throw new IllegalArgumentException(String.format(
              "You don't have %d %s able to fight there.", n, t));
        }
        assign.put(t, n);
        assigned += n;
      }
    }
    if (assigned > 0 && targets.isEmpty()) {
      throw new IllegalArgumentException("There are no buildings to attack.");
    }

    war.markFought(at);
    StringBuilder log = new StringBuilder(String.format(
        "%s fights %s.", attacker, defender));
    List<Integer> buildingDamage = new ArrayList<>();

    // ---------------------------------------------------- step one: aircraft
    int airA = war.getReadyToFight(at, attacker, UnitType.AIRCRAFT);
    int airD = war.getReadyToFight(at, defender, UnitType.AIRCRAFT);
    int airToBuildings = Math.min(assign.containsKey(UnitType.AIRCRAFT) ? assign
        .get(UnitType.AIRCRAFT) : 0, airA);
    int airToTroops = airA - airToBuildings;
    int diceA = dice(war, attacker, UnitType.AIRCRAFT);
    int diceD = dice(war, defender, UnitType.AIRCRAFT);
    for (int n = 0; n < airToBuildings * diceA; n++) {
      if (hitsBuilding(die.getAsInt())) {
        buildingDamage.add(1);
      }
    }
    int airHitsA = 0;
    for (int n = 0; n < airToTroops * diceA; n++) {
      if (die.getAsInt() >= NATURAL_HIT) {
        airHitsA++;
      }
    }
    int airHitsD = 0;
    for (int n = 0; n < airD * diceD; n++) {
      if (die.getAsInt() >= NATURAL_HIT) {
        airHitsD++;
      }
    }
    if (airA + airD > 0) {
      log.append(String.format(" Aircraft: %s scored %d on troops%s, %s scored %d.",
          attacker, airHitsA, airToBuildings > 0 ? " and "
              + countBuildingHits(buildingDamage) + " on buildings" : "",
          defender, airHitsD));
    }
    // Both sides' casualties are worked out before anything is removed
    int airKilledD = Math.min(airHitsA, airD);
    int groundHitsFromAirA = war.hasTech(attacker, Tech.AIRCRAFT_GROUND) ? airHitsA
        - airKilledD : 0;
    int airKilledA = Math.min(airHitsD, airA);
    int groundHitsFromAirD = war.hasTech(defender, Tech.AIRCRAFT_GROUND) ? airHitsD
        - airKilledA : 0;
    Map<UnitType, Integer> killD = new EnumMap<>(UnitType.class);
    Map<UnitType, Integer> killA = new EnumMap<>(UnitType.class);
    if (airKilledD > 0) {
      killD.put(UnitType.AIRCRAFT, airKilledD);
    }
    if (airKilledA > 0) {
      killA.put(UnitType.AIRCRAFT, airKilledA);
    }
    // Hits on ground troops: the attacker of aircraft picks the strongest;
    // the victim of a ground hit loses the cheapest
    allocateGround(war, at, defender, groundHitsFromAirA, STRONGEST_FIRST, killD);
    allocateGround(war, at, attacker, groundHitsFromAirD, GROUND, killA);
    applyKills(war, at, defender, killD);
    applyKills(war, at, attacker, killA);
    if (!killD.isEmpty() || !killA.isEmpty()) {
      log.append(String.format(" Lost in the air: %s %s, %s %s.", attacker,
          describe(killA), defender, describe(killD)));
    }

    // ------------------------------------------------- step two: ground troops
    int penaltyOnD = war.isEntrenched(at, defender) ? 1 : 0;
    int penaltyOnA = war.isEntrenched(at, attacker) ? 1 : 0;
    int groundHitsA = 0;
    for (UnitType t : GROUND) {
      int ready = war.getReadyToFight(at, attacker, t);
      int toTroops = ready - Math.min(ready, assign.containsKey(t) ? assign
          .get(t) : 0);
      for (int n = 0; n < toTroops * dice(war, attacker, t); n++) {
        if (hitsTroops(die.getAsInt(), t, toHitBonus(war, attacker, t),
            penaltyOnD)) {
          groundHitsA++;
        }
      }
    }
    int groundHitsD = 0;
    for (UnitType t : GROUND) {
      int ready = war.getReadyToFight(at, defender, t);
      for (int n = 0; n < ready * dice(war, defender, t); n++) {
        if (hitsTroops(die.getAsInt(), t, toHitBonus(war, defender, t),
            penaltyOnA)) {
          groundHitsD++;
        }
      }
    }
    for (UnitType t : GROUND) {
      int ready = war.getReadyToFight(at, attacker, t);
      int toB = Math.min(ready, assign.containsKey(t) ? assign.get(t) : 0);
      int bonus = buildingDamageBonus(war, attacker, t);
      for (int n = 0; n < toB * dice(war, attacker, t); n++) {
        if (hitsBuilding(die.getAsInt())) {
          buildingDamage.add(1 + bonus);
        }
      }
    }
    boolean groundFight = groundHitsA + groundHitsD > 0
        || war.getEnemyFactionsOn(at, attacker).contains(defender);
    Map<UnitType, Integer> killD2 = new EnumMap<>(UnitType.class);
    Map<UnitType, Integer> killA2 = new EnumMap<>(UnitType.class);
    allocateGround(war, at, defender, groundHitsA, GROUND, killD2);
    allocateGround(war, at, attacker, groundHitsD, GROUND, killA2);
    applyKills(war, at, defender, killD2);
    applyKills(war, at, attacker, killA2);
    if (groundFight) {
      log.append(String.format(
          " Ground: %s scored %d hits and lost %s; %s scored %d hits and lost %s.",
          attacker, groundHitsA, describe(killA2), defender, groundHitsD,
          describe(killD2)));
    }

    // -------------------------------------------------------------- buildings
    if (!buildingDamage.isEmpty()) {
      log.append(applyBuildingDamage(ref, tile, attacker, defender,
          buildingDamage, die, targets));
    } else if (assigned > 0) {
      log.append(" The attack on the buildings did no damage.");
    }
    return log.toString();
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
