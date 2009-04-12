package common;
/**
 * This interface needs to be implemented by all clients wishing to connect to the ExchangeSimulator.
 * <code>notify(long orderID)</code> method is used by the simulator to notify clients of updates to their orders.
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

import core.Order;

public interface Notifiable extends Remote{

	public void notifyOrder(long orderID, double price, double quantity) throws RemoteException;
	
	public void notifyTrade(String ticker, long time, Order.Side side, double price, double quantity) 
				throws RemoteException;
	
	public void notifyQuote(String ticker, long time, double bidPrice, double askPrice) throws RemoteException;
	
	public int getClientID() throws RemoteException;
}
