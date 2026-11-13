package edu.brown.cs.catan;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import edu.brown.cs.board.Building;
import edu.brown.cs.board.Capitol;
import edu.brown.cs.board.City;
import edu.brown.cs.board.Factory;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;

/**
 * Works out how each faction's Morale changes when a 7 is rolled during the
 * Great War:
 *
 * <pre>
 * More troops than every enemy                 +1
 * More Research Points than every enemy         +1
 * Fewer troops than the strongest enemy         -1
 * Each city or factory occupied by an enemy     -1
 * Each capitol occupied by an enemy             -2
 * Fewer than 5 troops on the board              -1
 * No troops on the board                        -3
 * </pre>
 *
 * The penalties add up (no troops is -1 and -3 and -1 for having fewer than
 * the enemy).
 */
public final class WarMorale {

  private WarMorale() {
  }

  /**
   * @param ref
   *          The referee (the war must be on).
   * @return The change for every faction in the war.
   */
  public static Map<Faction, Integer> changes(Referee ref) {
    WarState war = ref.getWarState();
    Map<Faction, Integer> changes = new EnumMap<>(Faction.class);
    for (Faction f : war.getFactionList()) {
      int troops = war.getUnitCount(f);
      int research = war.getResearchTotal(f);
      boolean moreTroops = true;
      boolean moreResearch = true;
      int strongest = 0;
      for (Faction other : war.getFactionList()) {
        if (other == f) {
          continue;
        }
        int theirs = war.getUnitCount(other);
        strongest = Math.max(strongest, theirs);
        if (theirs >= troops) {
          moreTroops = false;
        }
        if (war.getResearchTotal(other) >= research) {
          moreResearch = false;
        }
      }
      int delta = 0;
      if (war.getFactionList().size() > 1) {
        delta += moreTroops ? 1 : 0;
        delta += moreResearch ? 1 : 0;
        delta -= troops < strongest ? 1 : 0;
      }
      delta -= occupiedBy(ref, f);
      if (troops < 5) {
        delta -= 1;
      }
      if (troops == 0) {
        delta -= 3;
      }
      changes.put(f, delta);
    }
    return changes;
  }

  /** Morale lost to enemy occupation: 1 per city or factory, 2 per capitol. */
  static int occupiedBy(Referee ref, Faction f) {
    int lost = 0;
    Set<Building> seen = new LinkedHashSet<>();
    for (Tile t : ref.getBoard().getTiles()) {
      for (Intersection i : t.getIntersections()) {
        Building b = i.getBuilding();
        if (b == null || !seen.add(b) || b.getOccupier() == null
            || b.getOccupier() == f || ref.getFaction(b.getPlayer().getID()) != f) {
          continue;
        }
        if (b instanceof Capitol) {
          lost += 2;
        } else if (b instanceof City || b instanceof Factory) {
          lost += 1;
        }
      }
    }
    return lost;
  }
}
