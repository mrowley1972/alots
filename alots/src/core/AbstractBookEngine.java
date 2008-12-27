package core;

import java.util.*;

public abstract class AbstractBookEngine {

	protected abstract Order findOrder(long orderID, List<Order> book);
	
	protected abstract void processNewOrder(Order order);
	
	protected abstract Order processCancelOrder(Order order);
	
	protected abstract void insertOrder(Order order);
	
	protected abstract void matchOrder(Order order);
}
