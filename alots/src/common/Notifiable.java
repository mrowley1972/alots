package common;
/**
 * This interface needs to be implemented by all clients wishing to connect to the ExchangeSimulator.
 * <code>notify(long orderID)</code> method is used by the simulator to notify clients of updates to their orders.
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Notifiable extends Remote{

	public void notify(long orderID) throws RemoteException;
	
	//Notification of last price
}
