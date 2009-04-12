/**
 * This class represents an TAQ Notification, which is based on tagged-value principle.
 * Some fields are only present when the type of the notification is a Trade.
 * It is the responsibility of the class using this to check for the type of notification and make appropriate calls
 *  with expectation that some may contain null values.
 *  
 *  @author Asset Tarabayev
 */

package core;

import java.util.Date;

public class TAQNotification {

	public enum Type{TRADE, QUOTE};
	
	private Type type;
	private String ticker;
	private long time;
	private double price;
	private long quantity;
	private Order.Side side;
	
	public TAQNotification(Type type, String ticker, long time, double price, long quantity, Order.Side side){
		this.type = type;
		this.ticker = ticker;
		this.time = time;
		this.price = price;
		this.quantity = quantity;
		this.side = side;
	}

	public Type getType() {
		return type;
	}

	public String getTicker() {
		return ticker;
	}

	public long getTime() {
		return time;
	}

	public double getPrice() {
		return price;
	}

	public double getQuantity() {
		return quantity;
	}

	public Order.Side getSide() {
		return side;
	}
	
	public String toString(){
		return ticker + " Type: " + type + " Price: " + price + " Quantity: " + quantity + " Side: " + 
		side + " Time: " + new Date(time) ;
	}

}
