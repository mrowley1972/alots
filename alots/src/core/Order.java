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
	List<OrderFill> fills;

	/*
	 * Two types of enums to ensure type safety 
	 */
	public enum Side{ BUY, SELL}
	public enum Type{ LIMIT, MARKET}
	
	/*
	 * A class to hold an order fill for this order only
	 */
	private class OrderFill{
		long volume;
		double price;
		Date fillTime;
		
		protected OrderFill(long volume, double price, Date fillTime){
			this.volume = volume;
			this.price = price;
			this.fillTime = fillTime;
		}
		
		protected long getVolume(){
			return volume;
		}
		protected double getPrice(){
			return price;
		}
		protected Date getFillTime(){
			return fillTime;
		}
		
		public String toString(){
			return volume + " @$" + price + " at " + fillTime;
		}
		
	}
	
	private final Side side;
	private final Type type;
	
	private int clientID;
	private long orderID;
	private Instrument instrument;
	private Date entryTime;
	private double price;
	private long totalVolume;
	private long openVolume;
	private long executedVolume;
	
	private static int nextID = 10000;
	
	public Order(int clientID, Instrument instrument, Side side, Type type, long totalVolume, double price){
		this.clientID = clientID;
		this.instrument = instrument;
		this.side = side;
		this.type = type;
		
		if(totalVolume< 0)
			this.totalVolume = 0;
		else 
			this.totalVolume = totalVolume;
		
		this.price = price;
		
		openVolume = totalVolume;
		executedVolume = 0;
		orderID = Order.nextID++;
		
		/*The entry time is measured to the nearest millisecond, which is important when multiple agents 
		 * place orders nearly simulataneously - this decides the position in the book if the price is 
		 * the same
		 */
		this.entryTime = new Date();
		fills = new Vector<OrderFill>();
	}
	
	public Type type(){ 
		return type;
	}
	
	public Side side(){ 
		return side;
	}
	
	
	public boolean isFilled(){
		return executedVolume == totalVolume;
	}
	public boolean isClosed(){
		return openVolume == 0;
	}
	
	public void cancel(){
		openVolume = 0;
	}
	
	public void execute(long volume, double price){
		OrderFill fill = new OrderFill(volume, price, new Date());
		fills.add(fill);
		openVolume -= volume;
		executedVolume += volume;
	}
	
	public Vector<OrderFill> getFills(){
		return new Vector<OrderFill>(fills);
	}
	
	public Instrument getInstrument(){
		return instrument;
	}
	
	public double getPrice(){
		return price;
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
	
	public long getTotalVolume(){
		return totalVolume;
	}
	
	public long getOpenVolume(){
		return openVolume;
	}
	
	public long getExecutedVolume(){
		return executedVolume;
	}
	
	/* TODO: need to decide how average executed price is calculated and whether we should store it or just
	 * calculate on the fly everytime it's requested
	 */
	
	public double getAverageExecutedPrice(){
		double price = 0.0;
		for(OrderFill fill: fills){
			price += fill.price;
		}
		return price/fills.size();
	}
	
	/* TODO: decide at what point the last executed price and last executed quantity is updated
	 */
	
	public double getLastExecutedPrice(){
		return fills.get(fills.size()-1).getPrice();
	}
	
	public long getLastExecutedVolume(){
		return fills.get(fills.size()-1).getVolume();
	}
	
	public String toString(){
		return "Instrument: " + instrument + " " + "Type: " + type().toString() + " " + "Side: " + side().toString() + " " + 
			"Total Volume: " + totalVolume + " " + "Price: $" + price + " " + "Open Volume: " +
						openVolume + " " + "Executed volume: " + executedVolume;
	}
	
	
	public int getNumberOfFills(){
		return fills.size();
	}
	
	public void printFills(){
		for(OrderFill fill: fills)
			System.out.println("\n" + fill.toString());
	}
}
