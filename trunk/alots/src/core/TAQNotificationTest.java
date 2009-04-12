package core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.Assert;
import org.testng.annotations.*;

import core.Instrument;
import core.Order;
import core.TAQNotification;
import core.Order.Side;
import core.TAQNotification.Type;

public class TAQNotificationTest {

	Instrument instrument;
	BlockingQueue<Order> queue = new LinkedBlockingQueue<Order>();
	BlockingQueue<TAQNotification> notifications = new LinkedBlockingQueue<TAQNotification>();
	TAQNotification notification;
	long time;

	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("GOOG", queue, notifications);
		time = System.currentTimeMillis();
		notification = new TAQNotification(TAQNotification.Type.TRADE, instrument.getTickerSymbol(), time, 41.24, 2000, Order.Side.SELL);
		
	}
	
	@Test
	public void testNotificationCreation(){
		Assert.assertNotNull(notification);
	}
	
	@Test
	public void testNotificationFormat(){
		Assert.assertEquals(notification.getType(), TAQNotification.Type.TRADE);
		Assert.assertEquals(notification.getTicker(), "GOOG");
		Assert.assertEquals(notification.getPrice(), 41.24);
		Assert.assertEquals(notification.getQuantity(), 2000.0);
		Assert.assertEquals(notification.getTime(), time);
		
		System.out.println(notification);
	}
}
