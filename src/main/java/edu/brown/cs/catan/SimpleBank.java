package edu.brown.cs.catan;

import java.util.Map;

/**
 * A simple bank implementation used in the regular/classic game of catan. Rates
 * are simply determined by a constant.
 *
 */
public class SimpleBank implements Bank {

  Map<Resource, Double> _supply;

  public SimpleBank() {
  }

  @Override
  public void getResource(Resource resource) {
  }

  @Override
  public void discardResource(Resource resource) {
  }

  @Override
  public void getResource(Resource resource, double count) {
  }

  @Override
  public void discardResource(Resource resource, double count) {
  }

  @Override
  public double getBankRate(Resource res) {
    // Cursed Catan: coal always trades 3:1 and uranium 2:1
    switch (res) {
    case COAL:
      return Settings.COAL_BANK_RATE;
    case URANIUM:
      return Settings.URANIUM_BANK_RATE;
    default:
      return Settings.BANK_RATE;
    }
  }

  @Override
  public double getPortRate(Resource res) {
    Double rate = Settings.PORT_RATES.get(res);
    return rate != null ? rate : Settings.BANK_RATE;
  }

  @Override
  public double getWildCardRate(Resource res) {
    return Settings.WILDCARD_RATE;
  }
}
