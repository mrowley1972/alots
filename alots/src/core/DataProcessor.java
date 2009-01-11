
package core;

import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.concurrent.BlockingQueue;


import common.Notifiable;


public class DataProcessor implements Runnable {

	private AbstractMap<Integer, Notifiable> registeredClients;
	private BlockingQueue<Order> updatedOrders;
	
	public DataProcessor(AbstractMap<Integer, Notifiable> registeredClients, BlockingQueue<Order> updatedOrders){
		this.registeredClients = registeredClients;
		this.updatedOrders = updatedOrders;
	}
	
	public void run() {
		while(true){
			try{
				Order order = updatedOrders.take();
				Notifiable client = registeredClients.get(order.getClientID());
				if(client != null){
					client.notify(order.getOrderID());
					System.out.println("Client " + order.getClientID() + " has been notified of order " + order.getOrderID());
				}
				
			}catch(InterruptedException e){
				e.printStackTrace();
			}catch(RemoteException re){
				re.printStackTrace();
			}
		}
		
	}
}
