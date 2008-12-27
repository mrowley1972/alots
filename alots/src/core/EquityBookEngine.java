package core;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractQueue;

public class EquityBookEngine extends AbstractBookEngine {

	private List<Order> bidLimitOrders;
	private List<Order> askLimitOrders;
	private List<Order> filledOrders;
	private List<Order> partiallyFilledOrders;
	private Comparator<Order> comparator;
	private AbstractQueue<Order> updatedOrders;
	
	/*
	 * When BookEngine object is created, it gets access to all Instrument books - probably could do this initialization in the Abstract class
	 * This eliminates passing those books around from method to method
	 */
	protected EquityBookEngine(List<Order> bidLimitOrders, List<Order> askLimitOrders, List<Order> filledOrders, 
			List<Order> partiallyFilledOrders, AbstractQueue<Order> updatedOrders){
		
		this.bidLimitOrders = bidLimitOrders;
		this.askLimitOrders = askLimitOrders;
		this.filledOrders = filledOrders;
		this.partiallyFilledOrders = partiallyFilledOrders;
		comparator = new PriceTimePriorityComparator();
		this.updatedOrders = updatedOrders;
	}
	
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
	
	@Override
	protected void processNewOrder(Order order) {
		//try to match new order straight away
		matchOrder(order);
		
		if(order.isFilled()){
			if(order.side() == core.Order.Side.BUY){
				bidLimitOrders.remove(order);
				filledOrders.add(order);
			}
			else{
				askLimitOrders.remove(order);
				filledOrders.add(order);
			}
		}
		//put the order into the book if it has not been closed
		if(!order.isClosed())
			insertOrder(order);	
		//Partially filled orders need to be traversed, and filled orders from previous matching of opposing orders
		//should be removed and put into fully filled orders
		cleanUpPartiallyFilledOrders();
	}
	
	
	/*
	 * Add the order, then sort books according to specified priority rules comparator.
	 * Uses mergesort with nlog(n) performance and stability.
	 */
	
	protected void insertOrder(Order order){
			
		if(order.side() == core.Order.Side.BUY){
				bidLimitOrders.add(order);
				//probably need to lock the book to sort, however current implementation is Vector
				//hence synchronized - not general to Lists
				Collections.sort(bidLimitOrders, comparator);
			}
		
		if(order.side() == core.Order.Side.SELL){
				askLimitOrders.add(order);
				//probably need to lock the book to sorts, however current implementation is Vector
				//hence synchronized - not general to Lists
				Collections.sort(askLimitOrders, comparator);
		}
	}
	
	@Override
	protected void matchOrder(Order order) {
		//deal with market order price discovery here, so that the actual matching already deals with correct price
		
		if(order.side() == core.Order.Side.BUY){	
			matchBuyOrder(order);
		}
		else{
			matchSellOrder(order);
		}
	}
	
	private synchronized void matchSellOrder(Order order){
		//the order to be matched is a sell order,
		//start iterating orders in bid order book if there are any orders outstanding
		//only permit matching if there are orders outstanding in the bid limit book
		if(bidLimitOrders.size() > 0){
			for(Order curOrder: bidLimitOrders){
			
				double price = curOrder.getPrice();
				//assign price to market order after each match
				if(order.type() == core.Order.Type.MARKET && order.getOpenQuantity() > 0)
					order.setPrice(price);
			
				//If the current price of buy order is greater
				//than the price of sell order, then it is a best match
				if(price >= order.getPrice() && order.getOpenQuantity() > 0){
				
					//calculate matched quantity
					long quantity;
					if(curOrder.getOpenQuantity() > order.getOpenQuantity())
						quantity = order.getOpenQuantity();
					else
						quantity = curOrder.getOpenQuantity();
				
					//update order states and set instrument's last price
					curOrder.execute(quantity, price);
					order.execute(quantity, price);
					//instrument's last price, is the last price of match
					order.getInstrument().setLastPrice(price);
								
					//put orders into partially executed orders
					addToPartiallyFilledOrders(order); 
					//addToPartiallyFilledOrders(curOrder);
					//put into pushing queue for client notifications of both orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
				
				}
			}
		}
		cleanUpBook(bidLimitOrders);
	}
	
	private synchronized void matchBuyOrder(Order order){
		//the order to be matched is a buy order
		//start iterating orders in ask order book if there any orders outstanding
		if(askLimitOrders.size()>0){
			for(Order curOrder: askLimitOrders){
				double price = curOrder.getPrice();
				//assign price to a market order after each match
				if(order.type() == core.Order.Type.MARKET && order.getOpenQuantity() > 0)
					order.setPrice(price);
				
				//If the current price of sell order price is less
				//than the price of buy order, then it is a best match
				if(price <= order.getPrice() && order.getOpenQuantity() > 0){
					long quantity;
					//figure out the quantity matched
					if(curOrder.getOpenQuantity() > order.getOpenQuantity())
						quantity = order.getOpenQuantity();
					else
						quantity = curOrder.getOpenQuantity();
					
					//update order states and set instrument's last price
					curOrder.execute(quantity, price);
					order.execute(quantity, price);
					order.getInstrument().setLastPrice(price);
					
					//put the buy order into partially executed orders
					addToPartiallyFilledOrders(order);
					//put into pushing queue for client notifications of both orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
				}
			}
		}
		cleanUpBook(askLimitOrders);
	}
	
	//deletes filled orders from a book
	private void cleanUpBook(List<Order> book){
		Iterator<Order> iter = book.iterator();
		while(iter.hasNext()){
			if(iter.next().isFilled())
				iter.remove();
		}
	}
	//clean up partiallyFilledOrders by removing an object with the same reference
	//and then adding the same element, but with updated internal state
	private void addToPartiallyFilledOrders(Order order){
		
		if(partiallyFilledOrders.contains(order))
		{
			//if the order is filled, it is put into filled orders in processNewOrders()
			if(order.isFilled())
				partiallyFilledOrders.remove(order);
			else{
				partiallyFilledOrders.remove(order);
				partiallyFilledOrders.add(order);
			}
		}
		else{ 
			if(!order.isFilled())
				partiallyFilledOrders.add(order);
		}
	}
	private void cleanUpPartiallyFilledOrders(){
		Iterator<Order> iter = partiallyFilledOrders.iterator();
		while(iter.hasNext()){
			Order o = iter.next();
			if(o.isFilled()){
				iter.remove();
				filledOrders.add(o);
			}
		}
	}

	@Override
	protected Order processCancelOrder(Order order){
		Order o;
		
		if(order.side() == core.Order.Side.BUY){
			Iterator<Order> iter = bidLimitOrders.iterator();
			while(iter.hasNext()){
				o = iter.next();
				if(o.equals(order)){
					iter.remove();
					o.cancel();
					return o;
				}
			}
			return null;
		}
		if(order.side() == core.Order.Side.SELL){
			Iterator<Order> iter = askLimitOrders.iterator();
			while(iter.hasNext()){
				o = iter.next();
				if(o.equals(order)){
					iter.remove();
					o.cancel();
					return o;
				}
			}
			return null;
		}
		
		return null;
	}


}
