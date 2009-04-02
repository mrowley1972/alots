package core;

import java.util.concurrent.BlockingQueue;

/**
 * 
 * @author Asset
 * Class that is responsible for processing orders from the orders queue in StockExchange
 * Should be ran in an individual thread
 * 
 */

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
			}
			catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
