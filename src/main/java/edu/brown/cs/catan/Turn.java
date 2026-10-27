package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import edu.brown.cs.actions.FollowUpAction;

/**
 * Represents a Standard Turn in Catan and keeps track of all necessary data
 * associated with a Turn. For example, it keeps track of whether the player has
 * used a development card during the turn.
 *
 */
public class Turn {

  private long _timeStarted;
  private final LongSupplier _clock;
  // Longest Turn: the turn's player, and the time spent waiting on others
  private int _turnPlayerID = -1;
  private long _waitStarted = -1;
  private long _waitedMillis = 0;
  private boolean _devHasBeenPlayed;
  private final int _turnNum;
  private List<Collection<FollowUpAction>> _followUps;
  private Map<DevelopmentCard, Integer> _initialDevCardHand;

  // Cities & Knights per-turn flags
  private boolean _medicineDiscountActive = false;
  private boolean _craneDiscountActive = false;
  private boolean _merchantFleetActive = false;

  /**
   * Creates a Turn.
   *
   * @param turnNum
   *          The turn number
   * @param initialDevCardHand
   *          The player's initial development card hand.
   */
  public Turn(int turnNum, Map<DevelopmentCard, Integer> initialDevCardHand) {
    this(turnNum, initialDevCardHand, System::currentTimeMillis);
  }

  /**
   * Creates a Turn that reads the time from the given clock (so that tests can
   * control how long a turn takes).
   *
   * @param turnNum
   *          The turn number
   * @param initialDevCardHand
   *          The player's initial development card hand.
   * @param clock
   *          Supplies the current time in milliseconds.
   */
  public Turn(int turnNum, Map<DevelopmentCard, Integer> initialDevCardHand,
      LongSupplier clock) {
    _clock = clock;
    _timeStarted = clock.getAsLong();
    _devHasBeenPlayed = false;
    _turnNum = turnNum;
    _followUps = new ArrayList<>();
    _initialDevCardHand = new HashMap<>(initialDevCardHand);
  }

  /**
   * Creates a copy of a Turn.
   *
   * @param turn
   *          the Turn to copy.
   */
  private Turn(Turn turn) {
    _clock = turn._clock;
    _turnPlayerID = turn._turnPlayerID;
    _waitStarted = turn._waitStarted;
    _waitedMillis = turn._waitedMillis;
    _timeStarted = turn.getTimeStarted();
    _devHasBeenPlayed = turn.devHasBeenPlayed();
    _turnNum = turn.getTurnNum();
    _followUps = turn.getAllFollowUps();
    _initialDevCardHand = new HashMap<>(turn.getInitialDevCards());
    _medicineDiscountActive = turn.hasMedicineDiscount();
    _craneDiscountActive = turn.hasCraneDiscount();
    _merchantFleetActive = turn.hasMerchantFleet();
  }

  /* Returns all FollowUps. */
  private List<Collection<FollowUpAction>> getAllFollowUps() {
    List<Collection<FollowUpAction>> list = new ArrayList<>();
    for (Collection<FollowUpAction> el : _followUps) {
      list.add(el);
    }
    return list;
  }

  /**
   * Returns whether the Turn is waiting for a FollowUpAction.
   *
   * @return Boolean whether Turn has a FollowUp
   */
  public boolean waitingForFollowUp() {
    return !_followUps.isEmpty();
  }

  private Map<DevelopmentCard, Integer> getInitialDevCards() {
    return Collections.unmodifiableMap(_initialDevCardHand);
  }

  void addFollowUp(Collection<FollowUpAction> actions) {
    List<FollowUpAction> actionsCopy = new ArrayList<>();
    for (FollowUpAction action : actions) {
      actionsCopy.add(action);
    }
    _followUps.add(actionsCopy);
    refreshWaiting();
  }

  /**
   * Returns the next FollowUpAction for a given player.
   *
   * @param playerID
   *          The players id
   * @return The Action that must be performed.
   */
  public FollowUpAction getNextFollowUp(int playerID) {
    if (!_followUps.isEmpty()) {
      Collection<FollowUpAction> curr = _followUps.get(0);
      for (FollowUpAction action : curr) {
        if (action.getPlayerID() == playerID) {
          return action;
        }
      }
    }
    return null;
  }

  /**
   * Removes a FollowUp.
   *
   * @param action
   *          The FollowUpAction to remove.
   */
  public void removeFollowUp(FollowUpAction action) {
    Collection<FollowUpAction> curr = _followUps.get(0);
    curr.remove(action);
    if (curr.isEmpty()) {
      _followUps.remove(curr);
    }
    refreshWaiting();
  }

  /**
   * Should be called when a development card is played in a turn.
   */
  public void setDevCardHasBeenPlayed() {
    _devHasBeenPlayed = true;
  }

  /**
   * Returns a copy of the Turn.
   *
   * @return A Turn.
   */
  public Turn getCopy() {
    return new Turn(this);
  }

  /**
   * Starts the turn's clock again from now. Used when the game moves from
   * setup to regular play, so that the time spent in the lobby and in setup
   * does not count as the first turn.
   */
  public void restartClock() {
    _timeStarted = _clock.getAsLong();
    _waitedMillis = 0;
    _waitStarted = -1;
    refreshWaiting();
  }

  public long getTimeElapsed() {
    return _clock.getAsLong() - _timeStarted;
  }

  /**
   * Tells the turn whose turn it is, so that it can tell when that player is
   * only waiting for somebody else (see {@link #getActiveMillis()}).
   *
   * @param playerID
   *          The player taking this turn.
   */
  public void setTurnPlayerID(int playerID) {
    _turnPlayerID = playerID;
    refreshWaiting();
  }

  /**
   * Whether the player taking the turn is held up by a follow-up that only
   * other players can answer (for example, others discarding or answering a
   * trade).
   */
  private boolean isWaitingOnOthers() {
    if (_turnPlayerID < 0 || _followUps.isEmpty()) {
      return false;
    }
    for (FollowUpAction action : _followUps.get(0)) {
      if (action.getPlayerID() == _turnPlayerID) {
        return false;
      }
    }
    return true;
  }

  private void refreshWaiting() {
    boolean waiting = isWaitingOnOthers();
    long now = _clock.getAsLong();
    if (waiting && _waitStarted < 0) {
      _waitStarted = now;
    } else if (!waiting && _waitStarted >= 0) {
      _waitedMillis += now - _waitStarted;
      _waitStarted = -1;
    }
  }

  /**
   * How long the turn has taken, not counting the time the player spent
   * waiting for other players' follow-ups. This is what Longest Turn measures.
   *
   * @return The active time of the turn in milliseconds.
   */
  public long getActiveMillis() {
    long now = _clock.getAsLong();
    long waited = _waitedMillis + (_waitStarted >= 0 ? now - _waitStarted : 0);
    return Math.max(0, now - _timeStarted - waited);
  }

  public long getTimeStarted() {
    return _timeStarted;
  }

  public boolean devHasBeenPlayed() {
    return _devHasBeenPlayed;
  }

  public int getTurnNum() {
    return _turnNum;
  }

  /**
   * Returns whether the current player had the given development card before
   * the turn started.
   *
   * @param dev
   *          The DevelopmentCard
   * @return Whether the player had the development card.
   */
  public boolean hadInitialDevCard(DevelopmentCard dev) {
    Integer held = _initialDevCardHand.get(dev);
    return held != null && held > 0;
  }

  // --- Cities & Knights per-turn flag accessors ---

  public boolean hasMedicineDiscount() {
    return _medicineDiscountActive;
  }

  public void setMedicineDiscount(boolean active) {
    _medicineDiscountActive = active;
  }

  public boolean hasCraneDiscount() {
    return _craneDiscountActive;
  }

  public void setCraneDiscount(boolean active) {
    _craneDiscountActive = active;
  }

  public boolean hasMerchantFleet() {
    return _merchantFleetActive;
  }

  public void setMerchantFleet(boolean active) {
    _merchantFleetActive = active;
  }

}
