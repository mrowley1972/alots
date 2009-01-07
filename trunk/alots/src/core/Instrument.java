/**
 * An class fully encapsulating properties of a traded Instrument on an exchange. 
 * The class holds four books - bid order book, ask order book, and two books representing filled and partially 
 * filled orders. These books are not manipulated by this class directly, instead it requires a class implementing 
 * <code>BookEngine</code> interface, that makes all of the necessary modifications to these containers. 
 * Clients must not be able to access this object directly, instead delegating calls are made from <code>StockExchange</code> 
 * class for any particular instrument. 
 * Each instrument does its own matching and order manipulations, without interfering with any other traded instruments.
 * 
 * @author Asset Tarabayev
 */

package core;

import java.util.AbstractQueue;
import java.util.List;
import java.util.Vector;
import java.math.*;

import common.IOrder;


public class Instrument {
	/*
	 * Books implemented as a Vector, thread-safe and increment size can be set
	 */
	protected List<Order> bidLimitOrders;
	protected List<Order> askLimitOrders;
	
	//A book of fully filled orders, i.e. order.isFilled() is TRUE
	protected List<Order> filledOrders;
	
	//A book of partially filled orders, when fully filled moved into filledOrders
	protected List<Order> partiallyFilledOrders;
	
	private BookEngine bookEngine;
	private String tickerSymbol;
	private double lastPrice;
	
	private long bidVolume;
	private long askVolume;
	private long buyVolume;
	private long sellVolume;
	private double averageBuyPrice;
	private double averageSellPrice;
	private double averagePrice;
	private double bidVWAP;
	private double askVWAP;
	private double bestBid;
	private double bestAsk;
	
	//Helper variables to calculate various average statistics
	private double total_QuantityTimesPrice;
	private long total_Quantity;
	private double total_BoughtQuantityTimesPrice;
	private double total_BoughtQuantity;
	private double total_SoldQuantityTimesPrice;
	private double total_SoldQuantity;
	private double total_AskQuantityTimesPrice;
	private long total_AskQuantity;
	private double total_BidQuantityTimesPrice;
	private long total_BidQuantity;
	
	
	
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
		
		bidVolume = askVolume = buyVolume = sellVolume = 0;
		averagePrice = averageSellPrice = averageBuyPrice = 0.0;
		bidVWAP = askVWAP = 0.0;
		bestBid = bestAsk = 0.0;
		
		//Initialise helper variables
		total_QuantityTimesPrice = 0.0;
		total_Quantity = 0;
		total_BoughtQuantityTimesPrice = 0.0;
		total_BoughtQuantity = 0;
		total_SoldQuantityTimesPrice = 0.0;
		total_SoldQuantity = 0;
		total_AskQuantityTimesPrice = 0.0;
		total_AskQuantity = 0;
		total_BidQuantityTimesPrice = 0.0;
		total_BidQuantity = 0;
	}
	
	/**
	 * Get the bid order book
	 * @return a Vector of bid limit orders
	 */
	protected List<IOrder> getBidLimitOrders(){
		return new Vector<IOrder>(bidLimitOrders);
	}
	/**
	 * Get the ask order book
	 * @return a Vector of ask limit orders
	 */
	protected List<IOrder> getAskLimitOrders(){
		return new Vector<IOrder>(askLimitOrders);
	}
	/**
	 * Get the book of fully filled orders
	 * @return a Vector of fully filled orders
	 */
	protected List<Order> getFilledOrders(){
		return new Vector<Order>(filledOrders);
	}
	/**
	 * Get the book of partially filled orders
	 * @return a Vector of partially filled orders
	 */
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
	
	/**
	 * Insert a valid buy order into either of order books.
	 * @param order an <code>Order</code> object previously checked for its validity.
	 */
	protected void insertBuyOrder(Order order){
		bookEngine.insertBuyOrder(order);
	}
	
	/**
	 * Insert a valid sell buy order into either of order books.
	 * @param order an <code>Order</code> object previously checked for its validity.
	 */
	protected void insertSellOrder(Order order){
		bookEngine.insertSellOrder(order);
	}
	
	/**
	 * Get this Instrument's ticker symbol
	 * @return this instrument's ticker symbol
	 */
	protected String getTickerSymbol(){
		return tickerSymbol;
	}
	
	/**
	 * Get this Instrument's last traded price
	 * @return last price of this instrument
	 */
	protected double getLastPrice(){
		return lastPrice;
	}
	
	/**
	 * Set this Instrument's last traded price
	 * @param price a non-negative price 
	 * @exception IllegalArgumentException if the passed argument is negative
	 */
	protected void updateLastPrice(double price){
		if(price <0) 
			throw new IllegalArgumentException("Invalid price: " + price);
		lastPrice = price;
	}
	
	/**
	 * Get this Instrument's bid volume. 
	 * @return bid volume of <code>this</code> instrument
	 */
	protected long getBidVolume(){
		return bidVolume;
	}
	
	protected void updateBidVolume(long volume){
		bidVolume += volume;
	}

	/**
	 * Get this Instrument's ask volume. 
	 * @return ask volume of <code>this</code> instrument
	 */
	protected long getAskVolume(){
		return askVolume;
	}
	
	protected void updateAskVolume(long volume){
		askVolume += volume;
	}
	
	/**
	 * Get this Instrument's buy volume. 
	 * @return <code>this</code> Instrument's buy volume
	 */
	protected long getBuyVolume(){
		
		return buyVolume;
	}
	
	protected void updateBuyVolume(long volume){
		buyVolume += volume;
	}
	
	/**
	 * Get this Instrument's sell volume. 
	 * @return <code>this</code> Instrument's sell volume
	 */
	protected long getSellVolume(){
		return sellVolume;	
	}
	
	protected void updateSellVolume(long volume){
		sellVolume += volume;
	}
	
	/**
	 * Get this Instrument's average traded price
	 * @return <code>this</code> Instrument's bid volume weighted average price
	 */
	protected double getAveragePrice(){
		return averagePrice;
	}
	
	//Uses BigDecimal to do correct rounding of doubles. Currently rounds to 4 decimal places using HALF_UP mode
	protected void updateAveragePrice(long quantity, double price){
		total_QuantityTimesPrice += quantity*price;
		total_Quantity += quantity;
		
		averagePrice = (new BigDecimal(total_QuantityTimesPrice/total_Quantity)).setScale(4, 
			RoundingMode.HALF_UP).doubleValue();
	}
	
	/**
	 * Get this Instrument's average price, initiated by a buy order
	 * @return <code>this</code> Instrument's bid volume weighted average price
	 */
	protected double getAverageBuyPrice(){
		return averageBuyPrice;
	}
	
	protected void updateAverageBuyPrice(long quantity, double price){
		total_BoughtQuantityTimesPrice += quantity*price;
		total_BoughtQuantity += quantity;
		
		averageBuyPrice = (new BigDecimal(total_BoughtQuantityTimesPrice/total_BoughtQuantity)).setScale(4, 
			RoundingMode.HALF_UP).doubleValue();
		
	}
	
	/**
	 * Get this Instrument's average price, initiated by a sell order
	 * @return <code>this</code> Instrument's bid volume weighted average price
	 */
	protected double getAverageSellPrice(){
		return averageSellPrice;
	}
	
	protected void updateAverageSellPrice(long quantity, double price){
		total_SoldQuantityTimesPrice += quantity*price;
		total_SoldQuantity += quantity;
		
		averageSellPrice = (new BigDecimal(total_SoldQuantityTimesPrice/total_SoldQuantity)).setScale(4, 
				RoundingMode.HALF_UP).doubleValue();
	}
	
	
	/**
	 * Get this Instrument's bid volume weighted average price (VWAP)
	 * @return <code>this</code> Instrument's bid volume weighted average price
	 */
	protected double getBidVWAP(){
		return bidVWAP;
	}
	
	protected void updateBidVWAP(long quantity, double price){
		
		total_BidQuantityTimesPrice += quantity*price;
		if(price != 0.0)
			total_BidQuantity += quantity;
		bidVWAP = (new BigDecimal(total_BidQuantityTimesPrice/total_BidQuantity)).setScale(4, RoundingMode.HALF_UP).doubleValue();
	}
	
	/**
	 * Get this Instrument's ask volume weighted average price (VWAP)
	 * @return <code>this</code> Instrument's ask volume weighted average price
	 */
	protected double getAskVWAP(){
		return askVWAP;
	}
	
	protected void updateAskVWAP(long quantity, double price){
		total_AskQuantityTimesPrice += quantity*price;
		if(price != 0.0)
			total_AskQuantity += quantity;
		askVWAP = (new BigDecimal(total_AskQuantityTimesPrice/total_AskQuantity)).setScale(4, RoundingMode.HALF_UP).doubleValue();
	}
	
	/*
	 * Get a volume at a specified price
	 */
	protected long getVolumeAtPrice(double price){
		return 0;
	}
	
	/*
	 * Get the best bid price.
	 * 
	 */
	protected double getBestBid(){
		return bestBid;
	}
	
	protected void updateBestBid(double price){
		if(price > bestBid)
			bestBid = price;
	}
	
	/*
	 * Get the best ask price
	 */
	protected double getBestAsk(){
		return bestAsk;
	}
	
	protected void updateBestAsk(double price){
		if(bestAsk == 0)
			bestAsk = price;
		if(price < bestAsk)
			bestAsk = price;
	}
	
	/**
	 * @return ticker symbol of <code>this</code> Instrument
	 */
	public String toString(){
		return tickerSymbol;
	}
	
}
