package edu.umich.srg.marketsim.privatevalue;

import edu.umich.srg.fourheap.Order.OrderType;

public interface PrivateValue {

  double valueForExchange(int position, OrderType type);

  double valueAtPosition(int position);

}
