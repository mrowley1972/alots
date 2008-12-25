package core;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BookEngine extends AbstractBookEngine {

	private List<Order> bidLimitOrders;
	private List<Order> askLimitOrders;
	private List<Order> filledOrders;
	private List<Order> partiallyFilledOrders;
	private Comparator<Order> comparator;
	
	/*
	 * When BookEngine object is created, it gets access to all Instrument books
	 */
	protected BookEngine(List<Order> bidLimitOrders, List<Order> askLimitOrders, List<Order> filledOrders, 
			List<Order> partiallyFilledOrders){
		
		this.bidLimitOrders = bidLimitOrders;
		this.askLimitOrders = askLimitOrders;
		this.filledOrders = filledOrders;
		this.partiallyFilledOrders = partiallyFilledOrders;
		comparator = new PriceTimePriorityComparator();
	}
	
	
	@Override
	/*
	 * returns an Order object if finds one in the supplied book, otherwise returns null
	 * Need to account for the NullPointerException in the calling method
	 */
	protected Order findOrder(long orderID, List<Order> book) {
		for(Order order: book){
			if(order.getOrderID() == orderID)
				return order;
		}
		return null;
	}
	
	
	/*
	 * Add the order, then sort books according to specified priority rules comparator.
	 * Uses mergesort with nlog(n) performance and stability.
	 */
	
	protected void insertOrder(Order order){
		if(order.type() == core.Order.Type.LIMIT){
			if(order.side() == core.Order.Side.BUY){
				bidLimitOrders.add(order);
				Collections.sort(bidLimitOrders, comparator);
			}
			if(order.side() == core.Order.Side.SELL){
				askLimitOrders.add(order);
				Collections.sort(askLimitOrders, comparator);
			}
		}
		//if the order is Market, then it is converted during the matching process into Limit
		//hence, the first If-statement needs to go in the future
	}

	@Override
	protected void matchOrder(Order order, List<Order> book) {
		// TODO Auto-generated method stub

	}

	@Override
	protected Order processCancelledOrder(Order order) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean processNewOrder(Order order) {
		// TODO Auto-generated method stub
		return false;
	}

}
