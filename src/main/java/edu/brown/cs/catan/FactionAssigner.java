package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.ToIntFunction;

/**
 * Hands players to factions by victory point rank when the Great War starts.
 *
 * <pre>
 * Players   USA          Russia
 * 2         1st          2nd
 * 3         1st          2nd (China: 3rd, one faction per player)
 * 4         1st, 4th     2nd, 3rd
 * 5         1st, 4th, 5th  2nd, 3rd
 * 6         1st, 4th, 5th  2nd, 3rd, 6th
 * </pre>
 */
public final class FactionAssigner {

  private FactionAssigner() {
  }

  /**
   * Ranks the players: most points first. Ties are broken randomly, as if by
   * dice.
   *
   * @param players
   *          The players.
   * @param points
   *          How many points a player has.
   * @param random
   *          Where the tie-break dice come from.
   * @return The players, first place first.
   */
  public static List<Player> rank(Collection<Player> players,
      ToIntFunction<Player> points, Random random) {
    List<Player> ranked = new ArrayList<>(players);
    Collections.shuffle(ranked, random);
    // The sort is stable, so the shuffle decides ties
    Collections.sort(ranked, new Comparator<Player>() {
      @Override
      public int compare(Player a, Player b) {
        return Integer.compare(points.applyAsInt(b), points.applyAsInt(a));
      }
    });
    return ranked;
  }

  /**
   * The faction of the player in the given place.
   *
   * @param numPlayers
   *          How many players there are.
   * @param place
   *          Zero for first place, one for second and so on.
   * @return The faction.
   */
  public static Faction factionForPlace(int numPlayers, int place) {
    if (numPlayers == 3) {
      return Faction.values()[place];
    }
    switch (place) {
    case 0:
    case 3:
    case 4:
      return Faction.USA;
    default:
      return Faction.RUSSIA;
    }
  }

  /**
   * Assigns every player a faction.
   *
   * @param ranked
   *          The players, first place first.
   * @return Player id to faction, in rank order.
   */
  public static Map<Integer, Faction> assign(List<Player> ranked) {
    Map<Integer, Faction> toReturn = new LinkedHashMap<>();
    for (int place = 0; place < ranked.size(); place++) {
      toReturn.put(ranked.get(place).getID(),
          factionForPlace(ranked.size(), place));
    }
    return toReturn;
  }
}
