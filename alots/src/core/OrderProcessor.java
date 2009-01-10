package core;

import java.util.concurrent.BlockingQueue;

/**
 * 
 * @author Asset
 * Class that is responsible for processing orders from the orders queue in StockExchange
 * Should be ran in an individual thread
 * 
 */

public class OrderProcessor implements Runnable {

	private BlockingQueue<Order> orders;
	public OrderProcessor(BlockingQueue<Order> orders){
		this.orders = orders;
	}
	
	public void run(){
		while(true){
			try{
				Order order = orders.take();
				order.getInstrument().processNewOrder(order);
			}
			catch(InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
