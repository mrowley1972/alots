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
	/*Two books implemented as Vectors, as these are thread-safe
	 * and since we have multiple engines updating them, this is necessary
	 */
	
	List<Order> bidLimitOrders = new Vector<Order>();
	List<Order> askLimitOrders = new Vector<Order>();
	List<Order> executedOrders = new Vector<Order>();
	
	private String tickerSymbol;
	private double lastTradedPrice;
	// Bid and ask volumes are totals from the book
	private long bidVolume;
	private long askVolume;
	
	//Buy and sell volumes are the actual executed volumes
	private long buyVolume;
	private long sellVolume;
	
	private double averageBuyPrice;
	private double averageSellPrice;
	private double bidVWAP;
	private double askVWAP;
	
	public Instrument(String tickerSymbol){
		this.tickerSymbol = tickerSymbol.toUpperCase();
	}
	
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
	
	public double getLastTradedPrice(){
		return lastTradedPrice;
	}
	
	public long getBidVolume(){
		return bidVolume;
	}
	public long getAskVolume(){
		return askVolume;
	}
	public long getBuyVolume(){
		return buyVolume;
	}
	public long getSellVolume(){
		return sellVolume;
	}
	
	public double getAverageBuyPrice(){
		return averageBuyPrice;
	}
	public double getAverageSellPrice(){
		return averageSellPrice;
	}
	public double getBidVWAP(){
		return bidVWAP;
	}
	public double getAskVWAP(){
		return askVWAP;
	}
}
