package core;

public abstract class AbstractBookEngine {

	protected abstract void processNewOrder(Order order);
	
	protected abstract Order processCancelOrder(Order order);
	
	protected abstract void insertOrder(Order order);
	
	protected abstract void matchOrder(Order order);
}
