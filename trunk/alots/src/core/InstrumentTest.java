package core;
import java.util.AbstractQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.testng.annotations.*;
import org.testng.Assert;

import common.IOrder;

public class InstrumentTest {

	Instrument instrument;
	AbstractQueue<Order> queue = new LinkedBlockingQueue<Order>();
	Order bOrder, bOrder2, bOrder3, bOrder4, bOrder5;
	Order sOrder, sOrder2, sOrder3, sOrder4, sOrder5;
	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("MSFT", queue);
	}
	
	@Test
	public void testInstrumentName(){
		Assert.assertEquals(instrument.getTickerSymbol(), "MSFT");
	}
	
	@Test
	public void insertBuyOrders(){
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
	
	@Test (dependsOnMethods = {"insertBuyOrders"})
	public void verifyInsertionOfNewBuyOrders(){
		
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 5);
		System.out.println();
		for(int i=0; i<instrument.getBidLimitOrders().size(); i++){
			System.out.println(instrument.getBidLimitOrders().get(i));
		}
		
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
	
	@Test (dependsOnMethods = {"insertSellOrders", "insertBuyOrders"})
	public void verifyInsertionOfNewSellOrders(){
		Assert.assertEquals(instrument.getAskLimitOrders().size(), 5);
		System.out.println();
		for(int i=0; i<instrument.getAskLimitOrders().size(); i++){
			System.out.println(instrument.getAskLimitOrders().get(i));
		}
	}
	
	@Test(dependsOnMethods = {"insertBuyOrders","insertSellOrders", "verifyInsertionOfNewSellOrders", 
			"verifyInsertionOfNewBuyOrders"})
	public void submitBuyMarketOrder(){
		Order marketOrder = new Order(3, instrument, core.Order.Side.BUY, core.Order.Type.MARKET, 2000, 0.0);
		instrument.processNewOrder(marketOrder);
	}
	
	@Test(dependsOnMethods = {"submitBuyMarketOrder"})
	public void verifyStateOfBooksAfterMarketOrder(){
	
		Assert.assertEquals(instrument.getAskLimitOrders().size(), 1);
		Assert.assertEquals(instrument.getFilledOrders().size(), 5);
		Assert.assertEquals(instrument.getPartiallyFilledOrders().size(), 1);
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 5);
	}
	
	@Test(dependsOnMethods = {"verifyStateOfBooksAfterMarketOrder"})
	public void submitBuyLimitOrder(){
		Order limitOrder = new Order(1, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 1700, 24.10);
		Order limitOrder2 = new Order(2, instrument, core.Order.Side.BUY, core.Order.Type.LIMIT, 1000, 24.063);
		instrument.processNewOrder(limitOrder);
		instrument.processNewOrder(limitOrder2);
	}
	
	@Test(dependsOnMethods = {"submitBuyLimitOrder"})
	public void verifyStateOfBooksAfterLimitOrder(){
		Assert.assertEquals(instrument.getPartiallyFilledOrders().size(), 1);
		Assert.assertEquals(instrument.getFilledOrders().size(), 6);
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 7);
		Assert.assertEquals(instrument.getAskLimitOrders().size(), 0);
	}
	
	@Test(dependsOnMethods = {"verifyStateOfBooksAfterLimitOrder"})
	public void submitSellLimitOrder(){
		Order limitOrder = new Order(3, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 300, 24.04);
		instrument.processNewOrder(limitOrder);
		limitOrder = new Order(2, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 100, 25.02);
		instrument.processNewOrder(limitOrder);
		limitOrder = new Order(3, instrument, core.Order.Side.SELL, core.Order.Type.LIMIT, 300, 24.05);
		instrument.processNewOrder(limitOrder);		
	}
	
	@Test(dependsOnMethods = {"submitSellLimitOrder"})
	public void verifyStateOfBooksAfterSellLimitOrders(){
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 6);
		Assert.assertEquals(instrument.getAskLimitOrders().size(), 1);
		Assert.assertEquals(instrument.getFilledOrders().size(), 9);
		Assert.assertEquals(instrument.getPartiallyFilledOrders().size(), 1);
		printBooks();
	}
	
	@Test(dependsOnMethods = {"verifyStateOfBooksAfterSellLimitOrders"})
	public void verifyBookVolumes(){
		Assert.assertEquals(instrument.getBidVolume(), 13119);
		Assert.assertEquals(instrument.getAskVolume(), 100);
		
		Assert.assertEquals(instrument.getBuyVolume(), 3681);
		Assert.assertEquals(instrument.getSellVolume(), 600);
	}
	
	@Test(dependsOnMethods = {"verifyStateOfBooksAfterSellLimitOrders"})
	public void verifyCancelOrder(){
		instrument.processCancelOrder(bOrder4);
		Assert.assertEquals(instrument.getBidLimitOrders().size(), 5);
	}
	
	@Test(dependsOnMethods = {"verifyCancelOrder"})
	public void verifyCancelAlreadyTradedOrder(){
		Assert.assertNull(instrument.processCancelOrder(sOrder));
	}

	//Helper method to see up-to-date view of the books
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
		System.out.println("Partially executed orders: ");
		for(Order order: instrument.getPartiallyFilledOrders()){
			System.out.println(order.toString());
			order.printTrades();
		}
		System.out.println();
		System.out.println("Fully executed orders: ");
		for(Order order: instrument.getFilledOrders()){
			System.out.println(order.toString());
			order.printTrades();
		}
		System.out.println("Instrument's last traded price: " + instrument.getLastPrice());
		Assert.assertEquals(instrument.getLastPrice(), 24.063);
		System.out.println("Instrument's average price per share: " + instrument.getAveragePrice());
		System.out.println("Instrument's sell volume: " + instrument.getSellVolume());
		System.out.println("Instrument's buy volume: " + instrument.getBuyVolume());
		System.out.println("Instrument's average buy price: " + instrument.getAverageBuyPrice());
		System.out.println("Instrument's average sell price " + instrument.getAverageSellPrice());
		System.out.println("Instrument's bid VWAP " + instrument.getBidVWAP());
		Assert.assertEquals(instrument.getBidVWAP(), 24.0789);
		System.out.println("Instrument's ask VWAP " + instrument.getAskVWAP());
		Assert.assertEquals(instrument.getAskVWAP(), 24.1843);
		System.out.println("Instrument's best bid " + instrument.getBestBid());
		Assert.assertEquals(instrument.getBestBid(), 24.10);
		System.out.println("Instrument's best ask " + instrument.getBestAsk());
		Assert.assertEquals(instrument.getBestAsk(), 24.04);
		
		
		System.out.println();
	}
}
