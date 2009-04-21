package core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.annotations.*;

import common.IOrder;
import core.Instrument;
import core.Order;
import core.TAQNotification;

public class BinaryInsertionTest {
	
	Instrument instrument;
	BlockingQueue<Order> queue = new LinkedBlockingQueue<Order>();
	BlockingQueue<TAQNotification> notifications = new LinkedBlockingQueue<TAQNotification>();

	Order bOrder, bOrder2, bOrder3, bOrder4, bOrder5;
	Order sOrder, sOrder2, sOrder3, sOrder4, sOrder5;
	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("GOOG", queue, notifications);
	}
	
	@Test
	public void insertBuyOrders() throws InterruptedException{
		bOrder = new Order(1, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 500, 24.0620);
		bOrder2 = new Order(2, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 5000, 24.0600);
		bOrder3 = new Order(3, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 6000, 24.0610);
		bOrder4 = new Order(1, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 1100, 24.0550);
		bOrder5 = new Order(3, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 100, 24.0600);
		
		instrument.insertBuyOrder(bOrder);
		instrument.insertBuyOrder(bOrder2);
		instrument.insertBuyOrder(bOrder3);
		instrument.insertBuyOrder(bOrder4);
		instrument.insertBuyOrder(bOrder5);
	}
	
	@Test
	public void insertSellOrders(){
		sOrder = new Order(1, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 1981, 24.0900);
		sOrder2 = new Order(3, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 200, 24.0800);
		sOrder3 = new Order(2, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 500, 24.0700);
		sOrder4 = new Order(1, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 500, 24.0690);
		sOrder5 = new Order(3, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 500, 24.0690);
		instrument.insertSellOrder(sOrder);
		instrument.insertSellOrder(sOrder2);
		instrument.insertSellOrder(sOrder3);
		instrument.insertSellOrder(sOrder4);
		instrument.insertSellOrder(sOrder5);
		
	}

	
	@Test(dependsOnMethods={"insertBuyOrders", "insertSellOrders"})
	public void checkIndex() throws InterruptedException{
		
		printBooks();
	}
	
	
	public void printBooks(){
			System.out.println();
			System.out.println("**********BOOKS STATE***********");
			System.out.println("Bid book state: ");
			for(IOrder order: instrument.getBidLimitOrders()){
				System.out.println(order.toString());
			}
			System.out.println();
			
			System.out.println("Ask book state: ");
			for(IOrder order: instrument.getAskLimitOrders()){
				System.out.println(order.toString());
			}
			System.out.println();
			
	}
	

}
