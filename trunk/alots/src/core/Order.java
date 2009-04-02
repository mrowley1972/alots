package core;

/**
 * @author Asset Tarabayev
 */

import common.IOrder;

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

public class Order implements IOrder{
	
	private static final long serialVersionUID = 1L;
	/*
	 * Holds a history of all trades that took place
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
	private class OrderTrade implements Serializable{
		
		private static final long serialVersionUID = 1L;
		long volume;
		double tradePrice;
		long tradeTime;
		
		protected OrderTrade(long volume, double price, long tradeTime){
			this.volume = volume;
			this.tradePrice = price;
			this.tradeTime = tradeTime;
		}
		
		public long getVolume(){
			return volume;
		}
		public double getTradePrice(){
			return tradePrice;
		}
		public long getTradeTime(){
			return tradeTime;
		}
		
		public String toString(){
			return volume + " @$" + tradePrice;
		}
		
	}
	
	private final Side side;
	private final Type type;
	
	private int clientID;
	private long orderID;
	private Instrument instrument;
	private long entryTime;
	private double price;
	private long quantity;
	private long openQuantity;
	private long executedQuantity;
	
	private static long nextID = 10000;
	
	public Order(int clientID, Instrument instrument, Side side, Type type, long quantity, double price){
		this.clientID = clientID;
		this.instrument = instrument;
		this.side = side;
		this.type = type;
		this.quantity = quantity;
		this.price = price;

		openQuantity = quantity;
		executedQuantity = 0;
		orderID = Order.generateOrderID();
		
		this.entryTime = System.nanoTime();
		trades = new Vector<OrderTrade>();
	}
	
	private static long generateOrderID(){
		return nextID++;
	}
	
	public Type type(){ 
		return type;
	}
	
	public Side side(){ 
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
		OrderTrade trade = new OrderTrade(volume, price, System.nanoTime());
		//everytime an order is executed, a trade takes place, which is recorded
		trades.add(trade);
		openQuantity -= volume;
		executedQuantity += volume;
	}
	
	protected Vector<OrderTrade> getTrades(){
		return new Vector<OrderTrade>(trades);
	}
	
	protected Instrument getInstrument(){
		return instrument;
	}
	
	public String getInstrumentName(){
		return instrument.toString();
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
	
	public Long getEntryTime(){
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
	
	//This is an average executed price per share
	public double getAverageExecutedPrice(){
		double avgPrice = 0.0;
		long volume = 0;
		
		for(OrderTrade trade: trades){
			avgPrice += (trade.getTradePrice()*trade.getVolume());
			volume += trade.getVolume();
		}
		
		return avgPrice/volume;
		
	}
	
	public double getLastExecutedPrice(){
		return trades.get(trades.size()-1).getTradePrice();
	}
	
	public long getLastExecutedVolume(){
		return trades.get(trades.size()-1).getVolume();
	}
	
	public String toString(){
		return "Symbol: " + instrument.toString() + " " + "Quantity: " + quantity + " " +
		" " + "Open: " + openQuantity + " " + "Executed: " + executedQuantity + " " +
		"Side: " + side + " " + "Type: " + type + " " + "Price: " + price;
		
	}
	
	public int getNumberOfTrades(){
		return trades.size();
	}
	
	public void printTrades(){
		for(OrderTrade trade: trades)
			System.out.println(trade.toString());
	}
}
