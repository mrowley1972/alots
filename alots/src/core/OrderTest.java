package core;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.Assert;
import org.testng.annotations.*;

import core.Instrument;
import core.Order;
import core.TAQNotification;


public class OrderTest {
	
	Instrument instrument;
	BlockingQueue<Order> queue = new LinkedBlockingQueue<Order>();
	BlockingQueue<TAQNotification> notifications = new LinkedBlockingQueue<TAQNotification>();

	Order order;
	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("GOOG", queue, notifications);
		order = new Order(1, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 200, 15);
	}
	
	/*
	 * A series of tests to test simple getter methods and ensure correct information is returned
	 */
	
	@Test
	public void verifyInstrument(){
		Assert.assertEquals(order.getInstrument().toString(),"GOOG");
	}
	@Test
	public void verifySideAndType(){
		Assert.assertEquals(order.side(), core.Order.Side.BUY);
		Assert.assertEquals(order.type(), core.Order.Type.LIMIT);
	}
	
	@Test
	public void verifyTotalVolumeAndLimitPrice(){
		Assert.assertEquals(order.getQuantity(), 200);
		Assert.assertEquals(order.getPrice(), 15.0);
	}
	
	@Test 
	public void verifyVolumeCalculations(){
		order.execute(20, 14);
		order.execute(30, 11);
		Assert.assertEquals(order.getExecutedQuantity(), 50);
		Assert.assertEquals(order.getOpenQuantity(), 150);
		Assert.assertNotNull(order.getTrades());
		Assert.assertEquals(order.isFilled(), false);
		Assert.assertEquals(order.isClosed(), false);
	}
	
	@Test (dependsOnMethods = {"verifyVolumeCalculations"})
	public void verifyAverageCalculationsAndFills(){
		Assert.assertEquals(order.getAverageExecutedPrice(), 12.2);
		Assert.assertEquals(order.getLastExecutedPrice(), 11.0);
		Assert.assertEquals(order.getNumberOfTrades(), 2);
		Assert.assertEquals(order.getLastExecutedVolume(), 30);
	}
	
}

