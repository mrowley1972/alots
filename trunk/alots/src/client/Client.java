package client;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import common.Notifiable;
import common.IExchangeSimulator;
import common.IOrder;

public class Client implements Notifiable{
	
	private int clientID;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public Client(){
		try{
			UnicastRemoteObject.exportObject(this);
		}
		catch(RemoteException re){
			re.printStackTrace();
		}
	}

	//This method needs to have functionality for accepting updates about orders
	public void notify(long orderID){
		System.out.println("Received notification for order: " + orderID);
	}
}
