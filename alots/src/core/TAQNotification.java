/**
 * This class represents an TAQ Notification, which is based on tagged-value principle.
 * Some fields are only present when the type of the notification is a Trade.
 * It is the responsibility of the class using this to check for the type of notification and make appropriate calls
 *  with expectation that some may contain null values.
 *  
 *  @author Asset Tarabayev
 */

package core;

import java.io.Serializable;
import java.util.Date;

public class TAQNotification implements Serializable{

	private static final long serialVersionUID = 4442908068382700370L;

	public enum NotificationType{TRADE, QUOTE};
	
	private NotificationType type;
	private String ticker;
	private long time;
	
	//variants for Trade notification
	private double price;
	private long quantity;
	private Order.Side side;
	
	//variants for Quote notification
	private double bidPrice;
	private double askPrice;
	
	//constructor for Trade Notification
	public TAQNotification(NotificationType type, String ticker, long time, double price, long quantity, Order.Side side){
		this.type = type;
		this.ticker = ticker;
		this.time = time;
		this.price = price;
		this.quantity = quantity;
		this.side = side;
	}
	
	//constructor for Quote notification
	public TAQNotification(NotificationType type, String ticker, long time, double bidPrice, double askPrice){
		
		this.type = type;
		this.ticker = ticker;
		this.time = time;
		this.bidPrice = bidPrice;
		this.askPrice = askPrice;
	}

	public NotificationType getType() {
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

	public long getQuantity() {
		return quantity;
	}

	public Order.Side getSide() {
		return side;
	}
	
	public double getBidPrice(){
		return bidPrice;
	}
	
	public double getAskPrice(){
		return askPrice;
	}
	
	public String toString(){
		
		if(type == NotificationType.TRADE){
			return ticker + " Type: " + type + " Price: " + price + " Quantity: " + quantity + " Side: " + 
			side + " Time: " + new Date(time) ;
		}
		
		return ticker + " Type: " + type + " Bid Price: " + bidPrice +  " Ask Price: " + askPrice;
		
	}
}
