package core;
import java.util.List;
import org.testng.annotations.*;
import org.testng.Assert;

import common.IOrder;

public class ExchangeSimulatorTest {

	ExchangeSimulator exchange;
	
	@BeforeClass
	public void setUp(){
		exchange = new ExchangeSimulator();
	}
	
	@Test
	public void verifyClientIDGeneration(){
		int clientID1 = exchange.generateClientID();
		int clientID2 = exchange.generateClientID();
		Assert.assertEquals(clientID1, 1);
		Assert.assertEquals(clientID2, 2);	
	}
	
	@Test
	public void verifyNotStarted(){
		Assert.assertFalse(exchange.isOpen());
	}
	
	private void addInstrument(String instrument){
		exchange.registerInstrument(instrument);
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
		exchange.start();
		Assert.assertTrue(exchange.isOpen());
		addInstrument("MSFT");
		addInstrument("GOOG");
		Assert.assertEquals(exchange.getTradedInstrumentsList().size(), 2);
	}
	
	//As soon as orders are created, they are executed in the order they are picked up from the queue by a thread
	@Test(dependsOnMethods = {"verifyAdditionOfInstruments"})
	public void verifyAdditionOfOrders(){
		exchange.submitOrder("MSFT", 1, "Buy", "Limit", 24.43, 1000);
		exchange.submitOrder("MSFT", 2, "Buy", "Market", 0.0, 500);
		exchange.submitOrder("MSFT", 2, "Sell", "Market", 0.0, 500);
		exchange.submitOrder("MSFT", 2, "Sell", "Market", 0.0, 500);
		
		try{
			
			Thread.sleep(2000);
			System.out.println("*** BID BOOK ***");
			printIOrderBook(exchange.getInstrumentBidBook("MSFT"));
			//Assert.assertEquals(exchange.getInstrumentBidBook("MSFT").size(), 1);
			
			System.out.println("*** ASK BOOK ***");
			printIOrderBook(exchange.getInstrumentAskBook("MSFT"));
			Assert.assertEquals(exchange.getInstrumentAskBook("MSFT").size(), 0);
			
			System.out.println("*** FILLED ORDERS ***");
			printOrderBook(exchange.getInstrument("MSFT").getFilledOrders());
			Assert.assertEquals(exchange.getInstrument("MSFT").getFilledOrders().size(), 3);
			
			System.out.println("*** PARTIALLY FILLED ORDERS ***");
			printOrderBook(exchange.getInstrument("MSFT").getPartiallyFilledOrders());
			Assert.assertEquals(exchange.getInstrument("MSFT").getPartiallyFilledOrders().size(), 0);
			System.out.println();
			
			Assert.assertEquals(exchange.getInstrumentBidVolume("MSFT"), 500);
			Assert.assertEquals(exchange.getInstrumentAskVolume("MSFT"), 0);
			Assert.assertEquals(exchange.getInstrumentBuyVolume("MSFT"), 0);
			Assert.assertEquals(exchange.getInstrumentSellVolume("MSFT"), 1000);
			Assert.assertEquals(exchange.getInstrumentAskVWAP("MSFT"), 24.43);
			Assert.assertEquals(exchange.getInstrumentBidVWAP("MSFT"), 24.43);
			Assert.assertEquals(exchange.getInstrumentLastPrice("MSFT"), 24.43);
			Assert.assertEquals(exchange.getInstrumentBestBid("MSFT"), 0.0);
			Assert.assertEquals(exchange.getInstrumentBestAsk("MSFT"), 0.0);
			
			exchange.cancelOrder(2, 10001);
			Thread.sleep(100);
			Assert.assertEquals(exchange.getInstrumentBidBook("MSFT").size(), 0);
			
		}
		catch(InterruptedException e){
			e.printStackTrace();
		}
	}
	
}


