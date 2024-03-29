/**
 * This class handles Trade and Quote notifications for instruments that a client subscribes to.
 * Clients are notified of any trade that takes place for the instrument they are interested in or if the best quote price changes in the order book.
 * It may be possible that some notifications arrive out of order if the communication channel drops some packets.
 * However, the class implementation guarantees that earlier notifications are sent before the later ones.
 * 
 * @author Asset Tarabayev
 */

package core;

import java.rmi.RemoteException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
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
				long time = notification.getTime();
								
				ArrayList<Notifiable> clients = instrumentSubscribers.get(ticker);
				Iterator<Notifiable> iter;
				
				//forced to use iterator because it is the only way to concurrently modify clients container if RemoteException occurs
				if(notification.getType() == TAQNotification.NotificationType.TRADE && clients != null){
					iter = clients.iterator();
					while(iter.hasNext()){
						try{
							iter.next().notifyTrade(ticker, time, notification.getSide().toString(), notification.getPrice(), notification.getQuantity());
							
							ExchangeSimulator.logger.info("Trade notification sent to all subscribers: " + ticker + "; price: " + notification.getPrice() + 
									"; quantity: " + notification.getQuantity() + "; side: " + notification.getSide());
							
						}catch(RemoteException e){
							iter.remove();
							ExchangeSimulator.logger.warning("Connection with a client is lost..." + "\n" + e.getStackTrace().toString());
						}
					}
				}
				
				if(notification.getType() == TAQNotification.NotificationType.QUOTE && clients != null){
					iter = clients.iterator();
					while(iter.hasNext()){
						try{
							iter.next().notifyQuote(ticker, time, notification.getBidPrice(), notification.getAskPrice());
							ExchangeSimulator.logger.info("Quote notification sent to all subscribers: " + ticker + "; bid price: " + notification.getBidPrice() +
									"; ask price: " + notification.getAskPrice());
						}catch(RemoteException e){
							iter.remove();
							ExchangeSimulator.logger.warning("Connection with a client is lost..." + "\n" + e.getStackTrace().toString());
						}
					}
				}
				
			}catch(InterruptedException e){
				ExchangeSimulator.logger.severe("TAQ NOTIFICATION ENGINE HAS BEEN INTERRUPTED..." + "\n" + e.getStackTrace().toString());
				
			}
		}
	}
}
