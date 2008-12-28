package core;
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
	
	@Test(expectedExceptions = IllegalArgumentException.class)
	public void verifyNonValidInstrumentOrder(){
		Order order = stockExchange.createOrder("MSFT", 1, "buy", "limit", 24.95, 2000);
	}
	
	
}
