/**
 * This class is responsible for processing orders from the orders queue in StockExchange.
 * The class is implements Runnable interface and should be used as a separate thread in the main ExchangeSimulator class
 * @author Asset Tarabayev
 */

package core;

import java.util.concurrent.BlockingQueue;

public class OrderProcessingEngine implements Runnable {

	private BlockingQueue<Order> submittedOrders;
	public OrderProcessingEngine(BlockingQueue<Order> orders){
		this.submittedOrders = orders;
	}
	
	public void run(){
		while(true){
			try{
				Order order = submittedOrders.take();
				order.getInstrument().processNewOrder(order);
				
				ExchangeSimulator.logger.info("Order " + order.getOrderID() + " for " + order.getInstrumentName() + 
						" has been processed");
			}
			catch(InterruptedException e){
				
				ExchangeSimulator.logger.severe("ORDER PROCESSING ENGINE HAS BEEN INTERRUPTED..." + "\n" + 
						e.getStackTrace().toString());
			}
		}
	}
}
