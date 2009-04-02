/**
 * This class handles client notifications of executed orders.
 * Clients are notified of their own orders that were matched and are updated about their average executed price
 *  and quantity. A client will be notified of the same order if the order has been partially filled multiple times.
 *  It may be possible that some notifications arrive out of order if the communication channel drops some calls.
 *  However, the queue implementation guarantees that earlier notifications are sent before the later ones.
 */


package core;

import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.concurrent.BlockingQueue;


import common.Notifiable;


public class ClientOrdersNotificationEngine implements Runnable {

	//clients are registered with the exchange
	private AbstractMap<Integer, Notifiable> registeredClients;
	//container holding all updated orders
	private BlockingQueue<Order> updatedOrders;
	
	public ClientOrdersNotificationEngine(AbstractMap<Integer, Notifiable> registeredClients, BlockingQueue<Order> updatedOrders){
		this.registeredClients = registeredClients;
		this.updatedOrders = updatedOrders;
	}
	
	public void run() {
		while(true){
			try{
				Order order = updatedOrders.take();
				Notifiable client = registeredClients.get(order.getClientID());
				
				if(client != null){
					client.notifyOrder(order.getOrderID(), order.getAverageExecutedPrice(), order.getExecutedQuantity());
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
