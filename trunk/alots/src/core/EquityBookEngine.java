package core;

import java.util.Collections;
import java.util.Comparator;
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
	
	@Override
	protected boolean processNewOrder(Order order) {
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
		if(!order.isClosed())
			insertOrder(order);	
		//something wrong with this boolean return statement
		return updatedOrders.size() != 0;
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
		//deal with market order price discovery here, so that the actual matching already deals with correct prices
		
		if(order.side() == core.Order.Side.BUY){
			
			if(order.type() == core.Order.Type.MARKET){
				if(askLimitOrders.size() > 0)
					order.setPrice(askLimitOrders.get(0).getPrice());
			}		
			matchBuyOrder(order);
		}
		else{
			
			if(order.type() == core.Order.Type.MARKET){
				if(bidLimitOrders.size() > 0)
					order.setPrice(bidLimitOrders.get(0).getPrice());
			}
			matchSellOrder(order);
		}
	}
	
	private void matchSellOrder(Order order){
		//the order to be matched is a sell order,
		//start iterating orders in bid order book if there are any orders outstanding
		
		while(bidLimitOrders.size() > 0){
			for(Order curOrder: bidLimitOrders){
				//If the current price of buy order is greater
				//than the price of sell order, then it is a best match
				if(curOrder.getPrice() >= order.getPrice() && order.getOpenQuantity() > 0){
				
					long quantity;
					double price = curOrder.getPrice();
				
					//figure out the quantity matched
					if(curOrder.getOpenQuantity() > order.getOpenQuantity())
						quantity = order.getOpenQuantity();
					else
						quantity = curOrder.getOpenQuantity();
				
					//update order states and set instrument's last price
					curOrder.execute(quantity, price);
					order.execute(quantity, price);
					order.getInstrument().setLastPrice(price);
				
					//check whether the bid order is filled, then remove from the bidLimitOrders
					if(curOrder.isFilled())
						bidLimitOrders.remove(curOrder);
					//put the sell order into partially executed orders
					addToPartiallyFilledOrders(order);
					//put into pushing queue for client notifications of both orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
				
				}
			}
		}
		
	}
	
	private void matchBuyOrder(Order order){
		//the order to be matched is a buy order
		//start iterating orders in ask order book if there any orders outstanding
		while(askLimitOrders.size()>0){
			for(Order curOrder: askLimitOrders){
				//If the current price of sell order price is less
				//than the price of buy order, then it is a best match
				if(curOrder.getPrice() <= order.getPrice() && order.getOpenQuantity() > 0){
					long quantity;
					double price = curOrder.getPrice();
					//figure out the quantity matched
					if(curOrder.getOpenQuantity() > order.getOpenQuantity())
						quantity = order.getOpenQuantity();
					else
						quantity = curOrder.getOpenQuantity();
					
					//update order states and set instrument's last price
					curOrder.execute(quantity, price);
					order.execute(quantity, price);
					order.getInstrument().setLastPrice(price);
					
					//check whether the ask order is filled, then remove from the askLimitOrders
					if(curOrder.isFilled())
						askLimitOrders.remove(curOrder);
					//put the buy order into partially executed orders
					addToPartiallyFilledOrders(order);
					//put into pushing queue for client notifications of both orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
				}
			}
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

	@Override
	protected Order processCancelledOrder(Order order) {
		// TODO Auto-generated method stub
		return null;
	}


}
