package edu.brown.cs.actions;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Helpers shared by the knight movement actions: finding knights on the board
 * and walking a player's connected road network.
 */
final class KnightPaths {

  private KnightPaths() {
  }

  /** Returns the knight owned by the given player at loc, or null. */
  static KnightPiece knightOf(Player player, IntersectionCoordinate loc) {
    for (KnightPiece k : player.getKnights()) {
      if (k.getPosition() != null && k.getPosition().equals(loc)) {
        return k;
      }
    }
    return null;
  }

  /** Returns any player's knight standing at loc, or null. */
  static KnightPiece knightAt(Referee ref, IntersectionCoordinate loc) {
    for (Player p : ref.getPlayers()) {
      KnightPiece k = knightOf(p, loc);
      if (k != null) {
        return k;
      }
    }
    return null;
  }

  /**
   * Returns every intersection reachable from start (including start) by
   * walking only roads owned by the given player.
   */
  static Set<IntersectionCoordinate> reachable(Referee ref, int playerID,
      IntersectionCoordinate start) {
    Set<IntersectionCoordinate> seen = new HashSet<>();
    Deque<IntersectionCoordinate> queue = new ArrayDeque<>();
    seen.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      IntersectionCoordinate cur = queue.poll();
      Intersection inter = ref.getBoard().getIntersections().get(cur);
      if (inter == null) {
        continue;
      }
      for (Path p : inter.getPaths()) {
        if (p.getRoad() == null
            || p.getRoad().getPlayer().getID() != playerID) {
          continue;
        }
        IntersectionCoordinate a = p.getStart().getPosition();
        IntersectionCoordinate b = p.getEnd().getPosition();
        IntersectionCoordinate next = a.equals(cur) ? b : a;
        if (seen.add(next)) {
          queue.add(next);
        }
      }
    }
    return seen;
  }
}
