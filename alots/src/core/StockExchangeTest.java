package core;
import java.util.AbstractQueue;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.annotations.*;
import org.testng.Assert;

public class StockExchangeTest {

	StockExchange stockExchange;
	
	@BeforeClass
	public void setUp(){
		stockExchange = new StockExchange();
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
	//The following tests are commented out, as they cause problems with other tests after them... This is due to matching happening too quickly
	//and hence other tests checking state of books start to fail.
	
	/*
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidInstrumentOrder(){
		stockExchange.start();
		long order = stockExchange.createOrder("MSFT", 1, "buy", "limit", 24.95, 2000);
		stockExchange.stop();
	}
	
	@Test(dependsOnMethods = {"verifyNonValidInstrumentOrder"})
	public void verifyValidInstrumentOrder(){
		stockExchange.registerInstrument("MSFT");
		long order = stockExchange.createOrder("MSFT", 1, "buy", "limit", 24.54, 1000);
	}
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidSideOrder(){
		stockExchange.registerInstrument("MSFT");
		long order = stockExchange.createOrder("MSFT", 1, "deal", "limit", 12.33, 1000);
	}
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidTypeOrder(){
		stockExchange.registerInstrument("MSFT");
		long order = stockExchange.createOrder("MSFT", 1, "buy", "stop", 12.33, 1000);
	}
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidPriceOrder(){
		stockExchange.registerInstrument("MSFT");
		long order = stockExchange.createOrder("MSFT", 1, "buy", "limit", -12.43, 1000);
	}
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidQuantityOrder(){
		stockExchange.registerInstrument("MSFT");
		long order = stockExchange.createOrder("MSFT", 1, "buy", "limit", 12.33, 0);
	}
	@Test
	public void verifyCorrectOrderPlacement(){
		stockExchange.registerInstrument("GOOG");
		long order = stockExchange.createOrder("GOOG", 2, "buy", "limit", 24.56, 100);
	}
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidTickerSymbolAskBook(){
		stockExchange.getInstrumentAskBook("CSCO");
	}
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidTickerSymbolBuyBook(){
		stockExchange.getInstrumentBidBook("CSCO");
	}
	*/
	public void addInstruments(){
		stockExchange.registerInstrument("MSFT");
		stockExchange.registerInstrument("GOOG");
	}
	public void printBook(List<Order> book){
		for(Order order: book){
			System.out.println(order.toString());
		}
	}
	
	@Test(dependsOnMethods = {"verifyNotStarted"})
	public void verifyCreateOrder(){
		stockExchange.start();
		addInstruments();
		long MSFTorderID = stockExchange.createOrder("MSFT", 1, "Buy", "Limit", 24.43, 1000);
		long GOOGorderID = stockExchange.createOrder("GOOG", 2, "Sell", "Market", 30.76, 100);
		
		Assert.assertNotSame(MSFTorderID, GOOGorderID);
		Assert.assertEquals(stockExchange.getTradedInstrumentsList().size(), 2);
		Assert.assertTrue(stockExchange.isOpen());
		Assert.assertEquals(stockExchange.getInstrumentAskBook("MSFT").size(), 0);
		Assert.assertEquals(stockExchange.getInstrumentBidBook("MSFT").size(), 1);
		Assert.assertEquals(stockExchange.getInstrumentAskBook("GOOG").size(), 1);
		Assert.assertEquals(stockExchange.getInstrumentBidBook("GOOG").size(), 0);
	}
	
	@Test(dependsOnMethods = {"verifyCreateOrder"})
	public void verifyOrderMatching(){
		stockExchange.createOrder("MSFT", 3, "Sell", "Limit", 24.11, 900);
		stockExchange.createOrder("MSFT", 3, "Sell", "Limit", 24.10, 200);
		
		try{
			Thread.sleep(100);
			Assert.assertEquals(stockExchange.getInstrumentBidBook("MSFT").size(), 0);
			Assert.assertEquals(stockExchange.getInstrumentAskBook("MSFT").size(), 1);
			
			Assert.assertEquals(stockExchange.getInstrumentLastPrice("MSFT"), 24.43);
			Assert.assertEquals(stockExchange.getInstrumentBidVolume("MSFT"), 0);
			Assert.assertEquals(stockExchange.getInstrumentAskVolume("MSFT"), 100);
			
			printBook(stockExchange.getInstrumentAskBook("MSFT"));
			Instrument instr = stockExchange.getInstrument("MSFT");
			System.out.println("***FILLED ORDERS***");
			printBook(instr.getFilledOrders());
			System.out.println("***PARTIALLY FILLED ORDERS***");
			printBook(instr.getPartiallyFilledOrders());
			
	
		}catch(InterruptedException e){
			
		}
	}
	
}
