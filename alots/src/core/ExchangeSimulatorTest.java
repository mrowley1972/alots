package core;
import java.util.List;
import org.testng.annotations.*;
import org.testng.Assert;

import common.IOrder;

public class ExchangeSimulatorTest {

	ExchangeSimulator stockExchange;
	
	@BeforeClass
	public void setUp(){
		stockExchange = new ExchangeSimulator();
	}
	
	@Test
	public void verifyClientIDGeneration(){
		int clientID1 = stockExchange.generateClientID();
		int clientID2 = stockExchange.generateClientID();
		Assert.assertEquals(clientID1, 1);
		Assert.assertEquals(clientID2, 2);	
	}
	
	@Test
	public void verifyNotStarted(){
		Assert.assertFalse(stockExchange.isOpen());
	}
	
	private void addInstrument(String instrument){
		stockExchange.registerInstrument(instrument);
	}
	private void printOrderBook(List<Order> book){
		for(Order order: book){
			System.out.println(order.toString());
		}
	}
	
	private void printIOrderBook(List<IOrder> book){
		for(IOrder order: book){
			System.out.println(order.toString());
		}
	}
	//During testing, it has been uncovered that it is impossible to test parts of program, which are concurrently executing
	//Thread scheduling is non-deterministic
	//Hence, the only way of assessing correct execution is by manually examining books
	
	@Test(dependsOnMethods = {"verifyNotStarted"})
	public void verifyAdditionOfInstruments(){
		stockExchange.start();
		Assert.assertTrue(stockExchange.isOpen());
		addInstrument("MSFT");
		addInstrument("GOOG");
		Assert.assertEquals(stockExchange.getTradedInstrumentsList().size(), 2);
	}
	
	//As soon as orders are created, they are executed in the order they are picked up from the queue by a thread
	@Test(dependsOnMethods = {"verifyAdditionOfInstruments"})
	public void verifyAdditionOfOrders(){
		stockExchange.submitOrder("MSFT", 1, "Buy", "Limit", 24.43, 1000);
		stockExchange.submitOrder("MSFT", 2, "Buy", "Market", 0.0, 500);
		stockExchange.submitOrder("MSFT", 2, "Sell", "Market", 0.0, 500);
		stockExchange.submitOrder("MSFT", 2, "Sell", "Market", 0.0, 500);
		
		try{
			Thread.sleep(2000);
			System.out.println("*** BID BOOK ***");
			printIOrderBook(stockExchange.getInstrumentBidBook("MSFT"));
			Assert.assertEquals(stockExchange.getInstrumentBidBook("MSFT").size(), 1);
			
			System.out.println("*** ASK BOOK ***");
			printIOrderBook(stockExchange.getInstrumentAskBook("MSFT"));
			Assert.assertEquals(stockExchange.getInstrumentAskBook("MSFT").size(), 0);
			
			System.out.println("*** FILLED ORDERS ***");
			printOrderBook(stockExchange.getInstrument("MSFT").getFilledOrders());
			Assert.assertEquals(stockExchange.getInstrument("MSFT").getFilledOrders().size(), 3);
			
			System.out.println("*** PARTIALLY FILLED ORDERS ***");
			printOrderBook(stockExchange.getInstrument("MSFT").getPartiallyFilledOrders());
			Assert.assertEquals(stockExchange.getInstrument("MSFT").getPartiallyFilledOrders().size(), 0);
			System.out.println();
			
			Assert.assertEquals(stockExchange.getInstrumentBidVolume("MSFT"), 500);
			Assert.assertEquals(stockExchange.getInstrumentAskVolume("MSFT"), 0);
			Assert.assertEquals(stockExchange.getInstrumentBuyVolume("MSFT"), 1000);
			Assert.assertEquals(stockExchange.getInstrumentSellVolume("MSFT"), 1000);
			Assert.assertEquals(stockExchange.getInstrumentLastPrice("MSFT"), 24.43);
			
			stockExchange.cancelOrder(2, 10001);
			Thread.sleep(100);
			Assert.assertEquals(stockExchange.getInstrumentBidBook("MSFT").size(), 0);
			
		}
		catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	
}


