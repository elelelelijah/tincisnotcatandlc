package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Moves one of the player's active knights along their connected roads. If the
 * destination holds an opponent's strictly weaker knight, that knight is
 * displaced: its owner must relocate it (see {@link RelocateKnight}), or it is
 * removed if it has nowhere to go.
 *
 * Rules enforced:
 * - the knight must be active, and not recruited/promoted/activated this turn
 * - destination must be connected to the knight by the player's own roads
 * - destination must have no settlement/city and no knight of the player's own
 * - only a strictly stronger knight may displace (Basic can't displace;
 *   Strong displaces Basic; Mighty displaces Basic or Strong; nothing can
 *   displace a Mighty)
 * - the moving knight becomes inactive afterwards
 * - the displaced knight keeps its active/inactive status
 */
public class MoveKnight implements Action {

  public static final String ID = "moveKnight";

  private final Player _player;
  private final Referee _ref;
  private final IntersectionCoordinate _from;
  private final IntersectionCoordinate _to;

  public MoveKnight(Referee ref, int playerID, IntersectionCoordinate from,
      IntersectionCoordinate to) {
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _from = from;
    _to = to;
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    if (from == null || to == null) {
      throw new IllegalArgumentException("Missing knight location.");
    }
  }

  private Map<Integer, ActionResponse> fail(String message) {
    return ImmutableMap.of(_player.getID(),
        new ActionResponse(false, message, null));
  }

  private Map<Integer, ActionResponse> successToAll(String message) {
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      toReturn.put(p.getID(), new ActionResponse(true, message, null));
    }
    return toReturn;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (_ref.getGameStatus() != GameStatus.PROGRESS) {
      return fail("You can only move knights during the game.");
    }
    if (!_ref.currentPlayer().equals(_player)) {
      return fail("You cannot move a knight when it is not your turn.");
    }

    // The moving knight
    KnightPiece mover = KnightPaths.knightOf(_player, _from);
    if (mover == null) {
      return fail("No knight of yours found at that location.");
    }
    if (!mover.isActive()) {
      return fail("Only an active knight can move.");
    }
    if (mover.isRestrictedOn(_ref.getTurn().getTurnNum())) {
      return fail("A knight cannot act on the turn it was recruited, "
          + "promoted or activated.");
    }
    if (_from.equals(_to)) {
      return fail("Choose a different intersection to move to.");
    }

    // The destination
    Intersection dest = _ref.getBoard().getIntersections().get(_to);
    if (dest == null) {
      return fail("The destination intersection could not be found.");
    }
    if (dest.getBuilding() != null) {
      return fail("A knight cannot move onto a settlement or city.");
    }
    Set<IntersectionCoordinate> reach = KnightPaths.reachable(_ref,
        _player.getID(), _from);
    if (!reach.contains(_to)) {
      return fail("The destination must be connected to your knight "
          + "along your roads.");
    }

    KnightPiece occupant = KnightPaths.knightAt(_ref, _to);

    // Plain move onto an empty intersection
    if (occupant == null) {
      mover.moveTo(_to); // also deactivates
      return successToAll(String.format("%s moved a knight.",
          _player.getName()));
    }

    // Displacement
    if (occupant.getOwnerID() == _player.getID()) {
      return fail("You already have a knight there.");
    }
    if (!mover.canDisplace(occupant)) {
      return fail("Your knight must be stronger than the knight you "
          + "displace.");
    }
    Player defender = _ref.getPlayerByID(occupant.getOwnerID());

    // Where could the displaced knight go? Empty intersections connected to
    // it by its owner's roads. The mover's current square counts as empty
    // because the mover is about to leave it.
    List<IntersectionCoordinate> options = new ArrayList<>();
    for (IntersectionCoordinate c : KnightPaths.reachable(_ref,
        defender.getID(), _to)) {
      if (c.equals(_to)) {
        continue;
      }
      Intersection ci = _ref.getBoard().getIntersections().get(c);
      if (ci == null || ci.getBuilding() != null) {
        continue;
      }
      if (KnightPaths.knightAt(_ref, c) == null || c.equals(_from)) {
        options.add(c);
      }
    }

    if (options.isEmpty()) {
      // Nowhere to go: the displaced knight is removed from the board
      defender.removeKnight(occupant);
      mover.moveTo(_to);
      return successToAll(String.format(
          "%s displaced %s's knight, which had nowhere to go and was removed.",
          _player.getName(), defender.getName()));
    }

    // The owner chooses where the displaced knight goes. The mover moves once
    // they have chosen.
    _ref.addFollowUp(ImmutableList.<FollowUpAction> of(new RelocateKnight(
        defender.getID(), _player.getID(), _from, _to, options)));
    return successToAll(String.format(
        "%s is displacing %s's knight. %s must relocate it.",
        _player.getName(), defender.getName(), defender.getName()));
  }
}
