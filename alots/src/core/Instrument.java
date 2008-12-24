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
	
	List<Order> bidLimitOrders = new Vector<Order>();
	List<Order> askLimitOrders = new Vector<Order>();
	
	/*
	 * Very important that neither filledOrders nor partiallyFilledOrders contain duplicate orders.
	 * All methods rely on these lists containing at most one object with the same orderID.
	 */
	
	//A book of fully filled orders, i.e. order.isFilled() is TRUE
	List<Order> filledOrders = new Vector<Order>();
	
	//A book of partially filled orders, when fully filled moved into filledOrders
	List<Order> partiallyFilledOrders = new Vector<Order>();
	
	private String tickerSymbol;
	private double lastTradedPrice;	
	
	
	public Instrument(String tickerSymbol){
		this.tickerSymbol = tickerSymbol.toUpperCase();
	}
	
	//These are just delegating methods, the actual implementation is in BooksEngine
	protected boolean processNewOrder(Order order){
		//dummy return
		return true;
	}
	
	protected Order processCancelledOrder(Order order){
		//dummy return
		return order;
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
	 * Since orders are executed sequentially, last intraday price == order's last executed price
	 * This is set everytime an order is executed on this instrument during matching
	 */
	
	public double getLastTradedPrice(){
		return lastTradedPrice;
	}
	
	public long getBidVolume(){
		long volume = 0;
		for(Order order: bidLimitOrders){
			volume += order.getTotalVolume();
		}
		return volume;
	}

	public long getAskVolume(){
		long volume = 0;
		for(Order order: askLimitOrders){
			volume += order.getTotalVolume();
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
				volume += order.getExecutedVolume();
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
				volume += order.getExecutedVolume();
		}
		return volume;	
	}
	
	
	public double getAverageBuyPrice(){
		long orderVolume = 0;
		double averagePrice = 0.0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.BUY){
				averagePrice += order.getAverageExecutedPrice();
				orderVolume += order.getTotalVolume();
			}
		}
		
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.BUY){
				averagePrice += order.getAverageExecutedPrice();
				orderVolume += order.getExecutedVolume();
			}
		}
		
		return averagePrice/(double)orderVolume;
	}
	
	public double getAverageSellPrice(){
		long orderVolume = 0;
		double averagePrice = 0.0;
		
		for(Order order: filledOrders){
			if(order.side() == core.Order.Side.SELL){
				averagePrice += order.getAverageExecutedPrice();
				orderVolume += order.getTotalVolume();
			}
		}
		for(Order order: partiallyFilledOrders){
			if(order.side() == core.Order.Side.SELL){
				averagePrice += order.getAverageExecutedPrice();
				orderVolume += order.getExecutedVolume();
			}
		}
		
		return averagePrice/(double)orderVolume;
	}
	
	/*
	 * VWAP of all outstanding orders, both partially filled and not filled
	 */
	public double getBidVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: bidLimitOrders){
			volume += order.getTotalVolume();
			price += order.getLimitPrice();
		}
		return price/(double)volume;
	}
	
	public double getAskVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: askLimitOrders){
			volume += order.getTotalVolume();
			price += order.getLimitPrice();
		}
		return price/(double)volume;
	}
	public String toString(){
		return tickerSymbol;
	}
	
}
