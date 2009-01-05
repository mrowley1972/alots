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
	 * Insert a valid order into either of order books.
	 * @param order an <code>Order</code> object previously checked for its validity.
	 */
	protected void insertOrder(Order order){
		bookEngine.insertOrder(order);
	}
	
	/**
	 * Get this Instrument's ticker symbol
	 * @return this instrument's ticker symbol
	 */
	protected String getTickerSymbol(){
		return tickerSymbol;
	}
	
	/*
	 * lastPrice is set by the matching engine everytime an order gets matched
	 */
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
	protected void setLastPrice(double price){
		if(price <0) 
			throw new IllegalArgumentException("Invalid price: " + price);
		lastPrice = price;
	}
	
	/**
	 * Get this Instrument's bid volume. Bid volume is calculated by summing open quantities from orders 
	 * that are queued in the bid order book.
	 * @return bid volume of <code>this</code> instrument
	 */
	protected long getBidVolume(){
		long volume = 0;
		for(Order order: bidLimitOrders){
			volume += order.getOpenQuantity();
		}
		return volume;
	}

	/**
	 * Get this Instrument's ask volume. Ask volume is calculated by summing open quantities from orders 
	 * tjat are queued in the ask order book.
	 * @return ask volume of <code>this</code> instrument
	 */
	protected long getAskVolume(){
		long volume = 0;
		for(Order order: askLimitOrders){
			volume += order.getOpenQuantity();
		}
		return volume;
	}
	
	/**
	 * Get this Instrument's buy volume. 
	 * Buy volume = filled orders' buy side total quantities + partially filled orders' buy side executed quantities
	 * 
	 * @return <code>this</code> Instrument's buy volume
	 */
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
	
	/**
	 * Get this Instrument's sell volume. 
	 * Sell volume = filled orders' sell side total quantities + partially filled orders' sell side executed quantities
	 * @return <code>this</code> Instrument's sell volume
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
	
	/**
	 * Get this Instrument's average buy price
	 * @return <code>this</code> Instrument's average buy price
	 */
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
	
	/**
	 * Get this Instrument's average sell price
	 * @return <code>this</code> Instrument's average sell price
	 */
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
	
	/**
	 * Get this Instrument's bid volume weighted average price (VWAP)
	 * @return <code>this</code> Instrument's bid volume weighted average price
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
	
	/**
	 * Get this Instrument's ask volume weighted average price (VWAP)
	 * @return <code>this</code> Instrument's ask volume weighted average price
	 */
	protected double getAskVWAP(){
		long volume = 0;
		double price = 0.0;
		for(Order order: askLimitOrders){
			volume += order.getQuantity();
			price += order.getPrice()*order.getQuantity();
		}
		return price/(double)volume;
	}
	
	/**
	 * @return ticker symbol of <code>this</code> Instrument
	 */
	public String toString(){
		return tickerSymbol;
	}
	
}
