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
	
	public List<Order> bidLimitOrders;
	public List<Order> askLimitOrders;
	
	//A book of fully filled orders, i.e. order.isFilled() is TRUE
	public List<Order> filledOrders;
	
	//A book of partially filled orders, when fully filled moved into filledOrders
	public List<Order> partiallyFilledOrders;
	
	public AbstractBookEngine bookEngine;
	
	private String tickerSymbol;
	private double lastPrice;	
	
	public Instrument(String tickerSymbol, AbstractQueue<Order> updatedOrders){
		this.tickerSymbol = tickerSymbol.toUpperCase();
		bidLimitOrders = new Vector<Order>();
		askLimitOrders = new Vector<Order>();
		filledOrders = new Vector<Order>();
		partiallyFilledOrders = new Vector<Order>();
		bookEngine = new EquityBookEngine(bidLimitOrders, askLimitOrders, filledOrders, partiallyFilledOrders, updatedOrders);
	}
	
	/*
	 * Methods for clients to get latest view of the book
	 */
	public List<Order> getBidLimitOrders(){
		return new Vector<Order>(bidLimitOrders);
	}
	public List<Order> getAskLimitOrders(){
		return new Vector<Order>(askLimitOrders);
	}
	
	
	
	//These are just delegating methods, the actual implementation is in BooksEngine
	public boolean processNewOrder(Order order){
		//dummy return
		return true;
	}
	
	public Order processCancelledOrder(Order order){
		//dummy return
		return order;
	}
	
	public void insertOrder(Order order){
		bookEngine.insertOrder(order);
	}
	
	/*
	 * TODO: It needs to be decided about the access modifiers of these getter methods
	 * They should preferably be protected, with StockExchange providing delegating methods...
	 * This is important for security purposes, so that no client ever gets access to an Instrument object
	 * 
	 * The Client should be requesting according to API: getInstrumentLastTradedPrice(String tickerSymbol)
	 */
	public String getTickerSymbol(){
		return tickerSymbol;
	}
	
	/*
	 * lastPrice is set by the matching engine everytime an order gets matched
	 */
	
	public double getLastPrice(){
		return lastPrice;
	}
	
	protected void setLastPrice(double price){
		lastPrice = price;
	}
	/*
	 * Outstanding bid volume
	 */
	public long getBidVolume(){
		long volume = 0;
		for(Order order: bidLimitOrders){
			volume += order.getOpenQuantity();
		}
		return volume;
	}

	/*
	 * Outstanding ask volume
	 */
	public long getAskVolume(){
		long volume = 0;
		for(Order order: askLimitOrders){
			volume += order.getOpenQuantity();
		}
		return volume;
	}
	
	
	/*
	 * buy volume = total bought quantities from filled orders + executed bought quantities from partially filled orders
	 */
	public long getBuyVolume(){
		long volume = 0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.BUY)
				volume += order.getTotalVolume();
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
	public long getSellVolume(){
		long volume = 0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.SELL)
				volume += order.getTotalVolume();
		}
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.SELL)
				volume += order.getExecutedQuantity();
		}
		return volume;	
	}
	
	
	public double getAverageBuyPrice(){
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
	
	public double getAverageSellPrice(){
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
	public double getBidVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: bidLimitOrders){
			volume += order.getTotalVolume();
			price += order.getPrice()*order.getTotalVolume();
		}
		return price/(double)volume;
	}
	
	public double getAskVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: askLimitOrders){
			volume += order.getTotalVolume();
			price += order.getPrice()*order.getTotalVolume();
		}
		return price/(double)volume;
	}
	public String toString(){
		return tickerSymbol;
	}
	
}
