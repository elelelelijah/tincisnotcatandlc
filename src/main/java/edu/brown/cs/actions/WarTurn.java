package edu.brown.cs.actions;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Tile;
import edu.brown.cs.board.TileType;
import edu.brown.cs.catan.Faction;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Checks and parsing shared by the Great War's turn actions (building units,
 * moving, entrenching and fighting).
 */
final class WarTurn {

  private WarTurn() {
  }

  /**
   * Whether the player may take a war action now.
   *
   * @return A message if not, or null.
   */
  static String problem(Referee ref, Player player, String what) {
    if (!ref.getGameSettings().isCursedCatan || !ref.isWarActive()) {
      return String.format("You can only %s during the Great War.", what);
    }
    if (ref.getGameStatus() != GameStatus.PROGRESS
        || !ref.currentPlayer().equals(player)) {
      return String.format("You can only %s on your own turn.", what);
    }
    if (ref.isEliminated(player.getID())) {
      return "You have been eliminated.";
    }
    return null;
  }

  /**
   * Reads {x,y,z} from a JSON field into a land tile.
   *
   * @throws IllegalArgumentException
   *           If the field is missing or is not a land tile.
   */
  static Tile landTile(Referee ref, JsonObject params, String field) {
    try {
      JsonObject t = params.get(field).getAsJsonObject();
      Tile tile = ref.getBoard().getTile(new HexCoordinate(t.get("x")
          .getAsInt(), t.get("y").getAsInt(), t.get("z").getAsInt()));
      if (tile != null && tile.getType() != TileType.SEA) {
        return tile;
      }
    } catch (RuntimeException e) {
      // falls through
    }
    throw new IllegalArgumentException("Choose a land tile.");
  }

  /**
   * Parses a faction name such as "USA" or "russia".
   *
   * @return The faction, or null if the field is missing or unknown.
   */
  static Faction faction(JsonObject params, String field) {
    JsonElement el = params.get(field);
    if (el == null || !el.isJsonPrimitive()) {
      return null;
    }
    for (Faction f : Faction.values()) {
      if (f.toString().equalsIgnoreCase(el.getAsString())) {
        return f;
      }
    }
    return null;
  }
}
