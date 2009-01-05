package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

public interface IOrder extends Remote{
	
	String getInstrumentName() throws RemoteException;

	double getPrice() throws RemoteException;
	
	long getOrderID() throws RemoteException;
	
	int getClientID() throws RemoteException;
	
	Date getEntryTime() throws RemoteException;
	
	long getQuantity() throws RemoteException ;
	
	long getOpenQuantity() throws RemoteException;
	
	long getExecutedQuantity() throws RemoteException;
	
	double getAverageExecutedPrice() throws RemoteException;
	
	double getLastExecutedPrice() throws RemoteException;
	
	long getLastExecutedVolume() throws RemoteException;
	
	int getNumberOfTrades() throws RemoteException;
	
	void printTrades() throws RemoteException;
	
}
