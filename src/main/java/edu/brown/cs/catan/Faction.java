package edu.brown.cs.catan;

/**
 * The factions of the Great War in Cursed Catan. Two players fight as USA and
 * Russia; with three players China joins as a third faction.
 */
public enum Faction {

  USA("USA"), RUSSIA("Russia"), CHINA("China");

  private final String _name;

  private Faction(String name) {
    _name = name;
  }

  @Override
  public String toString() {
    return _name;
  }
}
