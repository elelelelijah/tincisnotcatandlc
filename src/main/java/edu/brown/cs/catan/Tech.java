package edu.brown.cs.catan;

/**
 * The Military Technology tree of the Great War. Each faction has its own copy
 * and works its way down it like a video-game tech tree: a node can only be
 * researched once the node above it is done. The Infantry/Artillery trunk is
 * one start; the Nuclear Weapons line is a second, independent start. The
 * Tanks, Infantry/Artillery and Aircraft lines all hang below the trunk's
 * second node.
 */
public enum Tech {

  // ---- Trunk (start here)
  INFANTRY_HIT_1("infantryHit1", "Infantry: +1 to hit", "trunk", null),
  ARTILLERY_BUILDINGS_1("artilleryBuildings1",
      "Artillery: +1 damage to buildings", "trunk", INFANTRY_HIT_1),

  // ---- Tanks
  TANKS_PREREQUISITE("tanksPrerequisite", "Tanks prerequisite", "tanks",
      ARTILLERY_BUILDINGS_1), UNLOCK_TANKS("unlockTanks", "Unlock Tanks",
      "tanks", TANKS_PREREQUISITE), TANK_HIT_1("tankHit1", "Tanks: +1 to hit",
      "tanks", UNLOCK_TANKS), TANK_HIT_2("tankHit2", "Tanks: +1 to hit",
      "tanks", TANK_HIT_1),

  // ---- Infantry / Artillery
  INFANTRY_BUILDINGS_1("infantryBuildings1",
      "Infantry: +1 damage to buildings", "infantry-artillery",
      ARTILLERY_BUILDINGS_1), ARTILLERY_HIT_1("artilleryHit1",
      "Artillery: +1 to hit", "infantry-artillery", INFANTRY_BUILDINGS_1), INFANTRY_DIE_1(
      "infantryDie1", "Infantry: +1 attack die", "infantry-artillery",
      ARTILLERY_HIT_1), ARTILLERY_BUILDINGS_2("artilleryBuildings2",
      "Artillery: +1 damage to buildings", "infantry-artillery",
      INFANTRY_DIE_1),

  // ---- Aircraft
  AIRCRAFT_PREREQUISITE("aircraftPrerequisite", "Aircraft prerequisite",
      "aircraft", ARTILLERY_BUILDINGS_1), UNLOCK_AIRCRAFT("unlockAircraft",
      "Unlock Aircraft", "aircraft", AIRCRAFT_PREREQUISITE), AIRCRAFT_DIE_1(
      "aircraftDie1", "Aircraft: +1 attack die", "aircraft", UNLOCK_AIRCRAFT), AIRCRAFT_GROUND(
      "aircraftGround", "Aircraft may attack ground troops", "aircraft",
      AIRCRAFT_DIE_1),

  // ---- Nuclear Weapons (a second start)
  NUCLEAR_1("nuclear1", "Nuclear prerequisite", "nuclear", null), NUCLEAR_2(
      "nuclear2", "Nuclear prerequisite", "nuclear", NUCLEAR_1), NUCLEAR_3(
      "nuclear3", "Nuclear prerequisite", "nuclear", NUCLEAR_2), UNLOCK_TEST_SITE(
      "unlockTestSite", "Unlock Test Site", "nuclear", NUCLEAR_3), UNLOCK_ATOMIC_BOMBS(
      "unlockAtomicBombs", "Unlock Atomic Bombs", "nuclear", UNLOCK_TEST_SITE);

  private final String _id;
  private final String _label;
  private final String _branch;
  private final Tech _prerequisite;

  private Tech(String id, String label, String branch, Tech prerequisite) {
    _id = id;
    _label = label;
    _branch = branch;
    _prerequisite = prerequisite;
  }

  /** The name the client uses for this node. */
  public String getId() {
    return _id;
  }

  public String getLabel() {
    return _label;
  }

  /** "trunk", "tanks", "infantry-artillery", "aircraft" or "nuclear". */
  public String getBranch() {
    return _branch;
  }

  /** The node that has to be researched first, or null for a start. */
  public Tech getPrerequisite() {
    return _prerequisite;
  }

  /**
   * Turns a name sent by the client into a node.
   *
   * @throws IllegalArgumentException
   *           If there is no such node.
   */
  public static Tech fromId(String id) {
    for (Tech t : values()) {
      if (t._id.equals(id)) {
        return t;
      }
    }
    throw new IllegalArgumentException("Unknown technology: " + id);
  }

  @Override
  public String toString() {
    return _label;
  }
}
