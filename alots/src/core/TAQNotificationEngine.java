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
					}
				}
				if(notification.getType() != TAQNotification.Type.TRADE)
				{
					System.out.println("***NOTIFICATIONS ARE OF INVALID TYPE***");
				}
			}catch(InterruptedException e){
				e.printStackTrace();
			}catch(RemoteException e){
				e.printStackTrace();
			}
		}
	}
	

}
