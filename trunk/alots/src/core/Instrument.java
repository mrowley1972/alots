/**
 * @author Asset Tarabayev
 */

/*
 * TODO: CRUCIAL - figure out how to present a client with the update-to-date book
 * Possible solution is for a client to have a Book object, that is notified through the Observer pattern
 */

package core;

import java.util.*;

public class Instrument {
	/*
	 * Books implemented as a Vector, because it is thread-safe and increment size can be set
	 */
	protected List<Order> bidLimitOrders;
	protected List<Order> askLimitOrders;
	
	//A book of fully filled orders, i.e. order.isFilled() is TRUE
	//Need to consider changing to Hashtable to improve deletion and insertions
	//Consider using ConcurrentHashMap<K,V>
	protected List<Order> filledOrders;
	
	//A book of partially filled orders, when fully filled moved into filledOrders
	//Need to consider changing to HashTable to improve deletion and insertions
	protected List<Order> partiallyFilledOrders;
	
	protected AbstractBookEngine bookEngine;
	
	private String tickerSymbol;
	private double lastPrice;	
	
	/**
	 * Creates an <code>Instrument</code> object with its own bid and ask order books, and book processing engine
	 * @param tickerSymbol	a valid ticker symbol of an already traded instrument
	 * @param updatedOrders	an <code>queue</code> where all orders that have been updated are placed
	 * @return a fully encapsulated <code>Instrument</code> object
	 */
	public Instrument(String tickerSymbol, AbstractQueue<Order> updatedOrders){
		this.tickerSymbol = tickerSymbol.toUpperCase();
		bidLimitOrders = new Vector<Order>();
		askLimitOrders = new Vector<Order>();
		filledOrders = new Vector<Order>();
		partiallyFilledOrders = new Vector<Order>();
		bookEngine = new EquityBookEngine(bidLimitOrders, askLimitOrders, filledOrders, partiallyFilledOrders, updatedOrders);
	}
	
	/*
	 * Methods to get latest view of books
	 */
	protected List<Order> getBidLimitOrders(){
		return new Vector<Order>(bidLimitOrders);
	}
	protected List<Order> getAskLimitOrders(){
		return new Vector<Order>(askLimitOrders);
	}
	protected List<Order> getFilledOrders(){
		return new Vector<Order>(filledOrders);
	}
	protected List<Order> getPartiallyFilledOrders(){
		return new Vector<Order>(partiallyFilledOrders);
	}
	
	/**
	 * Processes an order with specified price and quantity
	 * @param order	a valid order object
	 * @return void
	 */
	protected void processNewOrder(Order order){
		bookEngine.processNewOrder(order);
	}
	
	/**
	 * Processes a cancellation of an already submitted order
	 * @param order	client's own order
	 * @return	an <code>Order</code> object that has been cancelled, <code>null</code> if the order has already been filled
	 */
	protected Order processCancelOrder(Order order){
		return bookEngine.processCancelOrder(order);
	}
	
	protected void insertOrder(Order order){
		bookEngine.insertOrder(order);
	}
	
	/*
	 * TODO: It needs to be decided about the access modifiers of these getter methods
	 * They should preferably be protected, with StockExchange providing delegating methods...
	 * This is important for security purposes, so that no client ever gets access to an Instrument object
	 * 
	 * The Client should be requesting according to API: getInstrumentLastTradedPrice(String tickerSymbol)
	 */
	protected String getTickerSymbol(){
		return tickerSymbol;
	}
	
	/*
	 * lastPrice is set by the matching engine everytime an order gets matched
	 */
	
	protected double getLastPrice(){
		return lastPrice;
	}
	
	protected void setLastPrice(double price){
		lastPrice = price;
	}
	/*
	 * Outstanding bid volume
	 */
	protected long getBidVolume(){
		long volume = 0;
		for(Order order: bidLimitOrders){
			volume += order.getOpenQuantity();
		}
		return volume;
	}

	/*
	 * Outstanding ask volume
	 */
	protected long getAskVolume(){
		long volume = 0;
		for(Order order: askLimitOrders){
			volume += order.getOpenQuantity();
		}
		return volume;
	}
	
	
	/*
	 * buy volume = total bought quantities from filled orders + executed bought quantities from partially filled orders
	 */
	
	//TODO: This method causes the state of books to change, need to find out why
	protected long getBuyVolume(){
		long volume = 0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.BUY)
				volume += order.getQuantity();
		}
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.BUY)
				volume += order.getExecutedQuantity();
		}
		return volume;
	}
	
	/*
	 * sell volume = total sold quantities from filled orders + executed sold quantities from partially filled orders	 
	 */
	protected long getSellVolume(){
		long volume = 0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.SELL)
				volume += order.getQuantity();
		}
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.SELL)
				volume += order.getExecutedQuantity();
		}
		return volume;	
	}
	
	
	protected double getAverageBuyPrice(){
		long orders = 0;
		double averageOrderPrice = 0.0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.BUY){
				averageOrderPrice += order.getAverageExecutedPrice();
				orders++;
			}
		}
		
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.BUY){
				averageOrderPrice += order.getAverageExecutedPrice();
				orders++;
			}
		}
		
		return averageOrderPrice/(double)orders;
	}
	
	protected double getAverageSellPrice(){
		long orders = 0;
		double averageOrderPrice = 0.0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.SELL){
				averageOrderPrice += order.getAverageExecutedPrice();
				orders++;
			}
		}
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.SELL){
				averageOrderPrice += order.getAverageExecutedPrice();
				orders++;
			}
		}
		
		return averageOrderPrice/(double)orders;
	}
	
	/*
	 * VWAP of all outstanding orders, both partially filled and not filled
	 */
	protected double getBidVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: bidLimitOrders){
			volume += order.getQuantity();
			price += order.getPrice()*order.getQuantity();
		}
		return price/(double)volume;
	}
	
	protected double getAskVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: askLimitOrders){
			volume += order.getQuantity();
			price += order.getPrice()*order.getQuantity();
		}
		return price/(double)volume;
	}
	
	public String toString(){
		return tickerSymbol;
	}
	
}
