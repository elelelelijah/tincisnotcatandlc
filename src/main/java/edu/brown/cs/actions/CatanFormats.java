package edu.brown.cs.actions;

import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Class responsible for formatting for decimal trade, plus the shared trade
 * restrictions for the Cursed Catan resources.
 *
 * @author anselvahle
 *
 */
public abstract class CatanFormats {

  private static final double ROUND_TO = 100.0;

  public synchronized static double round(double raw) {
    return Math.round(raw * ROUND_TO) / ROUND_TO;
  }

  /**
   * Checks whether a resource may take part in a trade between players (or in
   * the receiving half of a bank trade). Coal and concrete trade freely in
   * Cursed Catan. Uranium cannot be bought until the Great War has started.
   * Cursed-only resources never exist in the other game modes.
   *
   * @param ref
   *          The referee.
   * @param res
   *          The resource being traded.
   * @return An error message, or null when the resource may be traded.
   */
  public static String tradeLockMessage(Referee ref, Resource res) {
    if (res == Resource.WILDCARD) {
      return "A wildcard cannot be traded.";
    }
    if (res.isCursedOnly() && !ref.getGameSettings().isCursedCatan) {
      return String.format("%s only exists in Cursed Catan.", res);
    }
    if (res == Resource.URANIUM && !ref.isGreatWarTriggered()) {
      return "Uranium cannot be traded until the Great War begins.";
    }
    return null;
  }

  /**
   * Checks a bank trade. Any resource can be given to the bank at its rate
   * (uranium included, at 2:1), but the resource received has to be tradeable.
   *
   * @param ref
   *          The referee.
   * @param toGive
   *          The resource the player gives the bank.
   * @param toGet
   *          The resource the player takes from the bank.
   * @return An error message, or null when the trade is allowed.
   */
  public static String tradeLockMessage(Referee ref, Resource toGive,
      Resource toGet) {
    if (toGive == Resource.WILDCARD || toGet == Resource.WILDCARD) {
      return "A wildcard cannot be traded.";
    }
    if ((toGive.isCursedOnly() || toGet.isCursedOnly())
        && !ref.getGameSettings().isCursedCatan) {
      return "Coal, concrete and uranium only exist in Cursed Catan.";
    }
    if (toGet == Resource.URANIUM && !ref.isGreatWarTriggered()) {
      return "Uranium cannot be bought from the bank until the Great War begins.";
    }
    return null;
  }

}
