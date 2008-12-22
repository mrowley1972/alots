package core;

import java.util.*;

public abstract class AbstractBookEngine {

	protected abstract Order findOrder(long orderID, List<Order> book);
	
	protected abstract boolean processNewOrder(Order order);
	
	protected abstract Order processCancelledOrder(Order order);
	
	protected abstract void insertOrder(Order order);
	
	protected abstract void matchOrder(Order order, List<Order> book);
}
