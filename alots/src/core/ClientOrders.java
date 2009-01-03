package core;

import java.util.AbstractMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds orders for a client. This class is necessary to ensure client-order integrity. 
 * Client has to know orderIDs of his/her own orders, otherwise there is no way to track an order. 
 * The orderID is returned back to the client everytime a new order is created.
 * @author Asset Tarabayev
 */

public class ClientOrders {
	
	private int clientID;
	AbstractMap<Long, Order> orders;
	
	public ClientOrders(int clientID){
		this.clientID = clientID;
		orders = new ConcurrentHashMap<Long, Order>();
	}
	
	/*
	 * If the order does belong to this client, then add it and return true.
	 * Return false otherwise
	 */
	protected boolean addOrder(Order order){
		if(order.getClientID() == clientID){
			orders.put(order.getOrderID(), order);
			return true;
		}
		return false;
	}
	
	/*
	 * returns true is the client had specified order, and subsequently removes it.
	 * returns false otherwise.
	 */
	protected boolean removeOrder(Order order){
		if(order.getClientID() == clientID){
			if(orders.containsKey(order.getOrderID())){
				orders.remove(order.getOrderID());
				return true;
			}
		}
		return false;
	}
	
	/*
	 * Find order by its orderID.
	 * Returns null if order was not found
	 */
	public Order findOrder(long orderID){
		if(orders.containsKey(orderID)){
			return orders.get(orderID);
		}
		return null;
	}
}
