/**
 * This class handles Trade and Quote notifications for instruments that a client subscribes to.
 * Clients are notified of any trade that takes place for the instrument they are interested in or if the best quote price changes in the order book.
 * It may be possible that some notifications arrive out of order if the communication channel drops some packets.
 * However, the class implementation guarantees that earlier notifications are sent before the later ones.
 */

package core;

import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import common.Notifiable;

public class TAQNotificationEngine implements Runnable{
	
	private AbstractMap<String, ArrayList<Notifiable>> instrumentSubscribers;
	private BlockingQueue<TAQNotification> notifications;
	
	public TAQNotificationEngine(AbstractMap<String, ArrayList<Notifiable>> instrumentSubscribers, BlockingQueue<TAQNotification> notifications){
		this.instrumentSubscribers = instrumentSubscribers;
		this.notifications = notifications;
	}
	
	public void run(){
		while(true){
			try{
				TAQNotification notification = notifications.take();
				String ticker = notification.getTicker();
								
				ArrayList<Notifiable> clients = instrumentSubscribers.get(ticker);
				
				//need to make sure that this instrument has some client subscribed to it
				if(notification.getType() == TAQNotification.Type.TRADE && clients != null){
					for(Notifiable client : clients){
						client.notifyTrade(ticker, notification.getTime(), notification.getSide(), notification.getPrice(), notification.getQuantity());
						
						ExchangeSimulator.logger.info("Notification sent to all subscribers: " + ticker + "; price: " + notification.getPrice() + 
								"; quantity: " + notification.getQuantity());
					}
				}
				if(notification.getType() != TAQNotification.Type.TRADE)
				{
					System.out.println("***NOTIFICATIONS ARE OF INVALID TYPE***");
				}
			}catch(InterruptedException e){
				ExchangeSimulator.logger.severe("TAQ NOTIFICATION ENGINE HAS BEEN INTERRUPTED..." + "\n" + e.getStackTrace().toString());
				
			}catch(RemoteException e){
				ExchangeSimulator.logger.warning("Connection with a client is lost..." + "\n" + e.getStackTrace().toString());
			}
		}
	}
	

}
