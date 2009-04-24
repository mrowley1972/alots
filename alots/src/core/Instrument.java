/**
 * This class fully encapsulates properties of a traded exchange instrument. 
 * The class holds four books - bid order, ask order, and two books representing filled and partially 
 * filled orders. It additionally holds references to updated orders and notifications containers. 
 * These containers are not manipulated by this class directly, instead it requires a class implementing 
 * <code>BookEngine</code> interface, that makes all of the necessary modifications to these containers. 
 * Clients must not be able to access this object directly, instead delegating calls are made from 
 * <code>ExchangeSimulator</code> class for any particular instrument. 
 * Each instrument does its own matching and order manipulations, without interfering with any other traded instruments.
 * 
 * @author Asset Tarabayev
 */

package core;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.io.Serializable;
import java.math.*;
import common.IOrder;

public class Instrument implements Serializable{
	
	private static final long serialVersionUID = -5116539492730231241L;
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
	private String ticker;
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
	private double bidHigh;
	private double bidLow;
	private double askHigh;
	private double askLow;
	
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
	 * Creates an <code>Instrument</code> object with its own bid and ask order books, and books processing engine
	 * @param tickerSymbol	a valid ticker symbol of an already traded instrument
	 * @param updatedOrders	an <code>queue</code> where all orders that have been updated are placed
	 * @param notifications an <code>queue</code> where all instrument TAQ notifications are placed
	 * @return a fully encapsulated <code>Instrument</code> object
	 */
	public Instrument(String tickerSymbol, BlockingQueue<Order> updatedOrders, 
			BlockingQueue<TAQNotification> notifications){
		this.ticker = tickerSymbol.toUpperCase();
		bidLimitOrders = new Vector<Order>();
		askLimitOrders = new Vector<Order>();
		filledOrders = new Vector<Order>();
		partiallyFilledOrders = new Vector<Order>();
		
		//start a new book processing engine for this instrument
		bookEngine = new EquityBookEngine(bidLimitOrders, askLimitOrders, filledOrders, partiallyFilledOrders, 
				updatedOrders, notifications, ticker);
		
		//Make sure all variables are initialized to zero from the beginning
		bidVolume = askVolume = buyVolume = sellVolume = 0;
		averagePrice = averageSellPrice = averageBuyPrice = 0.0;
		bidVWAP = askVWAP = 0.0;
		bidHigh = bidLow = askHigh = askLow = 0.0;
		
		//Initialise helper variables to zeroes
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
	protected String getTicker(){
		return ticker;
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
	 * Get this Instrument's bid volume - volume of the bid side of the book
	 * @return bid volume of <code>this</code> instrument
	 */
	protected long getBidVolume(){
		return bidVolume;
	}
	
	//bid volume is only updated when a new buy order is inserted, or when matched by a sell order
	//i.e. it is a volume of the book on the bid side
	protected void updateBidVolume(long volume){
		bidVolume += volume;
	}

	/**
	 * Get this Instrument's ask volume - volume of the ask side of the book
	 * @return ask volume of <code>this</code> instrument
	 */
	protected long getAskVolume(){
		return askVolume;
	}
	
	//ask volume is only updated when a new sell order is inserted, or when matched by a buy order
	//i.e. it is a volume of the book on the sell side
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
	
	//buy volume is only updated when buy order has been matched
	//i.e. it is a volume of matched buy orders
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
	
	//sell volume is only updated when sell order has been matched
	//i.e. it is a volume of matched sell orders
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
	
	/**
	 * Get this Instrument's bid volume at specific price
	 * @param price 	price to get volume at
	 * @return	bid volume at the specified price
	 */
	protected long getBidVolumeAtPrice(double price){
		
		long volume = 0;
		//Will return zero immediately if there is no order at this price or the book is empty
		for(Order order : bidLimitOrders){
			if(order.getPrice() == price)
				volume += order.getOpenQuantity();
			if(order.getPrice() < price)
				break;
		}
		return volume;
	}
	
	/**
	 * Get this Instrument's ask volume at specific price
	 * @param price 	price to get volume at
	 * @return	ask volume at the specified price
	 */
	protected long getAskVolumeAtPrice(double price){
		long volume = 0;
		//Returns zero immediately if there is no order at this price or the book is empty
		for(Order order : askLimitOrders){
			if(order.getPrice() == price)
				volume += order.getOpenQuantity();
			if(order.getPrice() > price)
				break;
		}
		return volume;
	}
	
	/**
	 * Get this Instrument's best bid price
	 * @return this instrument's best bid price
	 */
	protected double getBestBid(){
		return getBestPrice(bidLimitOrders);
	}
	
	/**
	 * Get this Instrument's best ask price
	 * @return this instrument's best ask price
	 */
	protected double getBestAsk(){
		return getBestPrice(askLimitOrders);
	}
	
	private double getBestPrice(List<Order> book){
		if(book.size() > 0)
			return book.get(0).getPrice();
		//if the book is empty, returns zero
		return 0.0;
	}
	
	/*
	 * Get a price at specific depth
	 */
	protected double getBidPriceAtDepth(int depth){
		return getPriceAtDepth(depth, bidLimitOrders);
	}
	protected double getAskPriceAtDepth(int depth){
		return getPriceAtDepth(depth, askLimitOrders);
	}
	
	private double getPriceAtDepth(int depth, List<Order> book){
		//a variable to hold a number of unique prices seen so far
		int tracker = 0;
		if(book.size()>0){
			//depth zero refers to the best price in the book
			if(depth == 0)
				return book.get(0).getPrice();
			//extract the very first price, first unique price seen so far
			double price = book.get(0).getPrice();
			tracker++;
			
			//price at depth d, is the unique price number d+1
			for(Order order: book){
				if(order.getPrice() != price){
					tracker++;
					price = order.getPrice();
				}
				if(tracker == (depth+1))
					return order.getPrice();
			}
			
			//return 0, if there is no price at this depth
			if(tracker < depth+1)
				return 0.0;
		}
		//return 0, is the book currently empty
		return 0.0;
	}
	
	protected double getBidHigh(){
		return bidHigh;
	}
	protected void updateBidHigh(double price){
		if(price > bidHigh)
			bidHigh = price;
	}
	protected double getBidLow(){
		return bidLow;
	}
	protected void updateBidLow(double price){
		if(bidLow == 0)
			bidLow = price;
		else{
			if(price < bidLow)
				bidLow = price;
		}
	}
	
	protected double getAskHigh(){
		return askHigh;
	}
	protected void updateAskHigh(double price){
		if(price > askHigh)
			askHigh = price;
	}
	protected double getAskLow(){
		return askLow;
	}
	protected void updateAskLow(double price){
		if(askLow == 0)
			askLow = price;
		else{
			if(price < askLow)
				askLow = price;
		}
	}
	
	public String toString(){
		return ticker;
	}
	
}
