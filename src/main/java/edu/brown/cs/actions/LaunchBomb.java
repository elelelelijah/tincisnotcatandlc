package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.IntSupplier;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

/**
 * Cursed Catan, Great War: launch an Atomic Bomb at a land tile. The Nuclear
 * Winter die is rolled first, then the bomb resolves (see
 * {@link Referee#launchBomb}).
 *
 * <p>
 * Request JSON: <code>{tile:{x,y,z}}</code>
 */
public class LaunchBomb implements Action {

  public static final String ID = "launchBomb";

  private final Referee _ref;
  private final Player _player;
  private final JsonObject _params;
  private final IntSupplier _die;

  public LaunchBomb(Referee ref, int playerID, JsonObject params) {
    this(ref, playerID, params, new IntSupplier() {
      private final Random random = new Random();

      @Override
      public int getAsInt() {
        return 1 + random.nextInt(6);
      }
    });
  }

  /**
   * Constructor with a given die, so that tests can decide what is rolled.
   *
   * @param die
   *          Supplies a roll of a D6, first for the Nuclear Winter die and then
   *          for each unit on the struck tile.
   */
  public LaunchBomb(Referee ref, int playerID, JsonObject params,
      IntSupplier die) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(String.format(
          "No player exists with ID: %d", playerID));
    }
    _params = params;
    _die = die;
  }

  private Map<Integer, ActionResponse> fail(String message) {
    Map<Integer, ActionResponse> toRet = new HashMap<>();
    toRet.put(_player.getID(), new ActionResponse(false, message, null));
    return toRet;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_ref.getGameSettings().isCursedCatan || !_ref.isWarActive()) {
      return fail("Atomic Bombs can only be launched during the Great War.");
    }
    if (_ref.getGameStatus() != GameStatus.PROGRESS
        || !_ref.currentPlayer().equals(_player)) {
      return fail("You can only launch a bomb on your own turn.");
    }
    if (_player.numBombs() < 1) {
      return fail("You don't have an Atomic Bomb.");
    }
    Tile tile;
    try {
      JsonObject t = _params.get("tile").getAsJsonObject();
      tile = _ref.getBoard().getTile(new HexCoordinate(t.get("x").getAsInt(), t
          .get("y").getAsInt(), t.get("z").getAsInt()));
    } catch (RuntimeException e) {
      return fail("Choose a tile to strike.");
    }
    if (tile == null || tile.getType() == TileType.SEA) {
      return fail("Choose a land tile to strike.");
    }

    // Read the chit before the strike takes it away
    String where = tile.getRollNumber() > 0 ? "the " + tile.getRollNumber()
        + " tile" : "a tile";
    int winterDie = _die.getAsInt();
    _player.useBomb();
    String what = _ref.launchBomb(_player.getID(), tile.getCoordinate(),
        winterDie, _die);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      String who = p.equals(_player) ? "You launched" : _player.getName()
          + " launched";
      toRet.put(p.getID(), new ActionResponse(true, String.format(
          "%s an Atomic Bomb at %s! (Nuclear Winter die: %d.) %s", who, where,
          winterDie, what).trim(), null));
    }
    return toRet;
  }
}
