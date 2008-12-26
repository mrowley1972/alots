package tests;
import java.util.AbstractQueue;
import java.util.concurrent.LinkedBlockingQueue;

import core.*;

import org.testng.annotations.*;
import org.testng.Assert;

public class InstrumentTest {

	Instrument instrument;
	AbstractQueue<Order> queue = new LinkedBlockingQueue<Order>();
	Order bOrder, bOrder2, bOrder3, bOrder4, bOrder5;
	Order sOrder, sOrder2, sOrder3, sOrder4, sOrder5;
	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("GOOG", queue);
	}
	
	@Test
	public void testInstrumentName(){
		Assert.assertEquals(instrument.getTickerSymbol(), "GOOG");
	}
	
	@Test
	public void verifyConstructorInitialisations(){
		Assert.assertNotNull(instrument.askLimitOrders);
		Assert.assertNotNull(instrument.bidLimitOrders);
		Assert.assertNotNull(instrument.filledOrders);
		Assert.assertNotNull(instrument.partiallyFilledOrders);
		Assert.assertNotNull(instrument.bookEngine);
	}
	@Test
	public void insertBuyOrders(){
		bOrder = new Order(1, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 300, 15);
		bOrder2 = new Order(2, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 200, 13);
		bOrder3 = new Order(3, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 400, 16);
		bOrder4 = new Order(4, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT,100, 11);
		bOrder5 = new Order(5, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 101, 11);
		
		instrument.insertOrder(bOrder);
		instrument.insertOrder(bOrder2);
		instrument.insertOrder(bOrder3);
		instrument.insertOrder(bOrder4);
		instrument.insertOrder(bOrder5);
	}
	
	@Test (dependsOnMethods = {"insertBuyOrders"})
	public void verifyInsertionOfNewBuyOrders(){
		
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 5);
		Assert.assertEquals(instrument.getAskLimitOrders().size(), 0);
		
		//This is an easier method to verify ordering is correct
		System.out.println();
		System.out.println(instrument.getBidLimitOrders().get(0));
		System.out.println(instrument.getBidLimitOrders().get(1));
		System.out.println(instrument.getBidLimitOrders().get(2));
		System.out.println(instrument.getBidLimitOrders().get(3));
		System.out.println(instrument.getBidLimitOrders().get(4));
	}
	
	@Test
	public void insertSellOrders(){
		sOrder = new Order(1, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 300, 15);
		sOrder2 = new Order(2, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 200, 13);
		sOrder3 = new Order(3, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 400, 16);
		sOrder4 = new Order(4, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT,100, 11);
		sOrder5 = new Order(5, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 101, 11);
		
		instrument.insertOrder(sOrder);
		instrument.insertOrder(sOrder2);
		instrument.insertOrder(sOrder3);
		instrument.insertOrder(sOrder4);
		instrument.insertOrder(sOrder5);
	}
	
	@Test (dependsOnMethods = {"insertSellOrders", "insertBuyOrders"})
	public void verifyInsertionOfNewSellOrders(){
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 5);
		Assert.assertEquals(instrument.getAskLimitOrders().size(), 5);
		//This is an easier method to verify ordering is correct
		System.out.println();
		System.out.println(instrument.getAskLimitOrders().get(0));
		System.out.println(instrument.getAskLimitOrders().get(1));
		System.out.println(instrument.getAskLimitOrders().get(2));
		System.out.println(instrument.getAskLimitOrders().get(3));
		System.out.println(instrument.getAskLimitOrders().get(4));
	}
	
	
	
	
	
	
	
	
	
}
