package core;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.annotations.*;
import org.testng.Assert;

import core.ClientOrders;
import core.Instrument;
import core.Order;
import core.TAQNotification;
import core.Order.Side;
import core.Order.Type;

public class ClientOrdersTest {

	int clientID;
	ClientOrders orders;
	Order order1, order2;
	
	@BeforeClass
	public void setUp(){
		clientID = 1;
		Instrument instrument = new Instrument("MSFT", new LinkedBlockingQueue<Order>(), 
				new LinkedBlockingQueue<TAQNotification>());
		order1 = new Order(1, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 2000, 24.056);
		order2 = new Order(2, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 2000, 24.056);
		orders = new ClientOrders(clientID);
		
	}
	
	@Test
	public void verifyAddClientOrder(){
		Assert.assertNotNull(orders.addOrder(order1));
	}
	@Test 
	public void verifyAddNonClientOrder(){
		Assert.assertFalse(orders.addOrder(order2));
	}
	@Test
	public void verifyRemoveClientOrder(){
		Assert.assertTrue(orders.removeOrder(order1));
	}
	@Test
	public void verifyRemoveNonClientOrder(){
		Assert.assertFalse(orders.removeOrder(order2));
	}
	@Test
	public void verifyClientFindOrder(){
		Assert.assertNotNull(orders.findOrder(order1.getOrderID()));
		Assert.assertEquals(orders.findOrder(order1.getOrderID()), order1);
	}
	@Test
	public void verifyNonClientFindOrder(){
		Assert.assertNotSame(orders.findOrder(order1.getOrderID()), order2);
		Assert.assertNull(orders.findOrder(order2.getOrderID()));
	}
}
