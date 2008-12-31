/**
 * @author Asset Tarabayev
 */

/*
 * TODO: CRUCIAL - figure out how to save and update partially filled orders, and executed orders
 * TODO: CRUCIAL - figure out how to update clients about the status of their orders - might have
 * to be postponed until Client/Server architecture is in place
 * 
 */
package core;

import java.util.*;

public class Order {
	/*
	 * Holds a history of all fills that took place
	 */
	List<OrderTrade> trades;

	/*
	 * Two types of enums to ensure type safety 
	 */
	public enum Side{ BUY, SELL}
	public enum Type{ LIMIT, MARKET}
	
	/*
	 * A class to hold an order fill for this order only
	 */
	private class OrderTrade{
		long volume;
		double tradePrice;
		Date tradeTime;
		
		protected OrderTrade(long volume, double price, Date tradeTime){
			this.volume = volume;
			this.tradePrice = price;
			this.tradeTime = tradeTime;
		}
		
		protected long getVolume(){
			return volume;
		}
		protected double getTradePrice(){
			return tradePrice;
		}
		protected Date getTradeTime(){
			return tradeTime;
		}
		
		public String toString(){
			return volume + " @$" + tradePrice + " at " + tradeTime;
		}
		
	}
	
	private final Side side;
	private final Type type;
	
	private int clientID;
	private long orderID;
	private Instrument instrument;
	private Date entryTime;
	private double price;
	private long quantity;
	private long openQuantity;
	private long executedQuantity;
	
	private static int nextID = 10000;
	
	public Order(int clientID, Instrument instrument, Side side, Type type, long quantity, double price){
		this.clientID = clientID;
		this.instrument = instrument;
		this.side = side;
		this.type = type;
		
		if(quantity< 0)
			this.quantity = 0;
		else 
			this.quantity = quantity;
		
		this.price = price;
		
		openQuantity = quantity;
		executedQuantity = 0;
		orderID = Order.nextID++;
		
		/*The entry time is measured to the nearest millisecond, which is important when multiple agents 
		 * place orders nearly simulataneously - this decides the position in the book if the price is 
		 * the same
		 */
		this.entryTime = new Date();
		trades = new Vector<OrderTrade>();
	}
	
	protected Type type(){ 
		return type;
	}
	
	protected Side side(){ 
		return side;
	}
	
	
	protected boolean isFilled(){
		return executedQuantity == quantity;
	}
	protected boolean isClosed(){
		return openQuantity == 0;
	}
	
	protected void cancel(){
		openQuantity = 0;
	}
	
	//updates order's state
	//once an order is submitted, it's total volume stays constant, only openQuantity and executedQuantity can change through this method
	protected void execute(long volume, double price){
		OrderTrade trade = new OrderTrade(volume, price, new Date());
		//everytime an order is executed, a trade takes place, which is recorded
		trades.add(trade);
		openQuantity -= volume;
		executedQuantity += volume;
	}
	
	public Vector<OrderTrade> getTrades(){
		return new Vector<OrderTrade>(trades);
	}
	
	public Instrument getInstrument(){
		return instrument;
	}
	
	public double getPrice(){
		return price;
	}
	protected void setPrice(double price){
		this.price = price;
	}
	
	public long getOrderID(){
		return orderID;
	}
	
	public int getClientID(){
		return clientID;
	}
	
	public Date getEntryTime(){
		return entryTime;
	}
	
	public long getQuantity(){
		return quantity;
	}
	
	public long getOpenQuantity(){
		return openQuantity;
	}
	
	public long getExecutedQuantity(){
		return executedQuantity;
	}
	
	/* TODO: need to decide how average executed price is calculated and whether we should store it or just
	 * calculate on the fly everytime it's requested
	 */
	
	public double getAverageExecutedPrice(){
		double price = 0.0;
		for(OrderTrade fill: trades){
			price += fill.tradePrice;
		}
		return price/trades.size();
	}
	
	/* TODO: decide at what point the last executed price and last executed quantity is updated
	 */
	
	public double getLastExecutedPrice(){
		return trades.get(trades.size()-1).getTradePrice();
	}
	
	public long getLastExecutedVolume(){
		return trades.get(trades.size()-1).getVolume();
	}
	
	public String toString(){
		return "Instrument: " + instrument + " "+ "Type: "+ type().toString() + " " + "Side: " + side().toString() + " " + 
			"Total Volume: " + quantity + " " + "Price: $" + price + " " + "Open Volume: " +
						openQuantity + " " + "Executed volume: " + executedQuantity;
	}
	
	
	public int getNumberOfFills(){
		return trades.size();
	}
	
	public void printFills(){
		for(OrderTrade fill: trades)
			System.out.println("\n" + fill.toString());
	}
}
