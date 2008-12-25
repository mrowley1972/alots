package tests;
import core.*;

import org.testng.Assert;
import org.testng.annotations.*;

public class OrderTest {
	
	Instrument instrument;
	Order order;
	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("GOOG");
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
		Assert.assertEquals(order.getTotalVolume(), 200);
		Assert.assertEquals(order.getPrice(), 15.0);
	}
	
	@Test 
	public void verifyVolumeCalculations(){
		order.execute(20, 14);
		order.execute(30, 11);
		Assert.assertEquals(order.getExecutedVolume(), 50);
		Assert.assertEquals(order.getOpenVolume(), 150);
		Assert.assertNotNull(order.getFills());
		Assert.assertEquals(order.isFilled(), false);
		Assert.assertEquals(order.isClosed(), false);
	}
	
	@Test (dependsOnMethods = {"verifyVolumeCalculations"})
	public void verifyAverageCalculationsAndFills(){
		Assert.assertEquals(order.getAverageExecutedPrice(), 12.5);
		Assert.assertEquals(order.getLastExecutedPrice(), 11.0);
		Assert.assertEquals(order.getNumberOfFills(), 2);
		Assert.assertEquals(order.getLastExecutedVolume(), 30);
	}
	
}

