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
	 * Two types of enums to ensure type safety 
	 */
	public enum Side{ BUY, SELL}
	public enum Type{ LIMIT, MARKET}
	
	private final Side side;
	private final Type type;
	
	private int clientID;
	private long orderID;
	private Instrument instrument;
	private Date entryTime;
	private double limitPrice;
	private long totalQuantity;
	private long openQuantity;
	private long executedQuantity;
	
	private double averageExecutedPrice;
	private double lastExecutedPrice;
	private long lastExecutedQuantity;
	
	private static int nextID = 10000;
	
	public Order(int clientID, Instrument instrument, Side side, Type type, long totalQuantity, double limitPrice){
		this.clientID = clientID;
		this.instrument = instrument;
		this.side = side;
		this.type = type;
		this.totalQuantity = totalQuantity;
		this.limitPrice = limitPrice;
		
		openQuantity = totalQuantity;
		executedQuantity = 0;
		orderID = Order.nextID++;
		
		/*The entry time is measured to the nearest millisecond, which is important when multiple agents 
		 * place orders nearly simulataneously - this decides the position in the book if the price is 
		 * the same
		 */
		this.entryTime = new Date();
	}
	
	public Type orderType(){ return type;}
	public Side orderSide(){ return side;}
	
	
	public boolean isFilled(){
		return executedQuantity == totalQuantity;
	}
	public boolean isClosed(){
		return openQuantity == 0;
	}
	
	/*
	 * This algorithm needs to be very careful about which orders it cancels, how it is removed 
	 * and how the clients are notified
	 */
	
	public void cancel(){
		
	}
	
	public void execute(double price, long quantity){
		
	}
	
	public Instrument getInstrument(){
		return instrument;
	}
	
	public double getLimitPrice(){
		return limitPrice;
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
	
	public long getTotalQuantity(){
		return totalQuantity;
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
	protected void setAverageExecutedPrice(){
		
	}
	public double getAverageExecutedPrice(){
		return averageExecutedPrice;
	}
	
	/* TODO: decide at what point the last executed price and last executed quantity is updated
	 * 
	 */
	protected void setLastExecutedPrice(){
		
	}
	public double getLastExecutedPrice(){
		return lastExecutedPrice;
	}
	
	protected void setLastExecutedQuantity(){
		
	}
	public long getLastExecutedQuantity(){
		return lastExecutedQuantity;
	}
}
