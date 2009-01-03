/**
 * 
 * @author Asset
 * This is the only class that clients are permitted to connect to.
 * It delegates all calls to necessary classes, returns and wraps results to clients.
 * Individual instruments keep their own ask and bid books, as well as executed and partially executed orders.
 * 
 */

package core;

import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class StockExchange {

	private AbstractMap<Integer, ClientOrders> clientOrdersDB;
	private AbstractMap<String, Instrument> instruments;
	//holds all submitted, but not yet processed client orders
	private BlockingQueue<Order> orders;
	//holds all updated orders that have been either fully or partially processed
	//these are pushed to subscribed clients
	private AbstractQueue<Order> updatedOrders;
	//state of the stock exchange
	private boolean started = false;
	
	private OrderProcessor orderProcessor;
	private Thread op;
	private static int nextClientID = 0;
	
	/**
	 * Creates a <code>StockExchange</code> with default implementation
	 */
	public StockExchange(){
		clientOrdersDB = new ConcurrentHashMap<Integer, ClientOrders>();
		instruments = new ConcurrentHashMap<String, Instrument>();
		orders = new LinkedBlockingQueue<Order>();
		updatedOrders = new LinkedBlockingQueue<Order>();
		orderProcessor = new OrderProcessor(orders);
		op = new Thread(orderProcessor);
	}
	
	protected int generateClientID(){
		return ++StockExchange.nextClientID;
	}
	
	public void start(){
		if(!started){
			started = true;
			op.start();
		}
	}
	public void stop(){
		try{
			op.join(1000);
			orders.clear();
			clientOrdersDB.clear();
			started = false;
		}
		catch(InterruptedException e){
			System.out.println("STOP EXCEPTION: " + e.getMessage());
			e.printStackTrace();
		}
	}
	/**
	 * Check whether the exchange is operating
	 * @return true if the exchange is operating, false otherwise
	 */
	public boolean isOpen(){
		return started;
	}
	/**
	 * Get latest bid order book for a particular instrument
	 * @param tickerSymbol ticker symbol of a traded instrument
	 * @return current bid order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<Order> getInstrumentBidBook(String tickerSymbol){
		if(!started)
			throw new MarketsClosedException("The market is currently closed");
		
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol " + tickerSymbol);
		return instrument.getBidLimitOrders();
	}
	
	/**
	 * Get latest ask order book for a particular instrument
	 * @param tickerSymbol ticker symbol of a traded instrument
	 * @return current ask order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<Order> getInstrumentAskBook(String tickerSymbol){
		if(!started)
			throw new MarketsClosedException("The market is currently closed");
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol "+ tickerSymbol);
		return instrument.getAskLimitOrders();
	}

	/**
	 * Submit an order to the exchange to be traded
	 * @param tickerSymbol  a valid traded instrument's ticker symbol
	 * @param side 			either BUY or SELL
	 * @param type 			either LIMIT or MARKET
	 * @param price 		positive order price 
	 * @param quantity 		non-negative order quantity
	 * @return orderID		an id of this submitted order
	 * @exception IllegalArgumentException if parameters do not comply
	 */
	public synchronized long createOrder(String tickerSymbol, int clientID, 
			String side, String type, double price, long quantity){
	
		// Validate all of the order's parameters
		Instrument instrument = findInstrument(tickerSymbol);
		core.Order.Side orderSide;
		core.Order.Type orderType;
		//can only create orders for existing instruments
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: " + tickerSymbol);
		
		if(side.equalsIgnoreCase("BUY"))
			orderSide = core.Order.Side.BUY;
		else{
			if(side.equalsIgnoreCase("SELL"))
				orderSide = core.Order.Side.SELL;
			else
				throw new IllegalArgumentException("Invalid side: " + side);
		}
		
		if(type.equalsIgnoreCase("LIMIT"))
			orderType = core.Order.Type.LIMIT;
		else{
			if(type.equalsIgnoreCase("MARKET"))
				orderType = core.Order.Type.MARKET;
			else
				throw new IllegalArgumentException("Invalid type: " + type);
		}
		if(price < 0)
			throw new IllegalArgumentException("Negative value price: " + price);
		
		if(quantity <=0)
			throw new IllegalArgumentException("Invalid quantity: " + quantity);
		
		//if all parameters are valid, create a new order
		Order order = new Order(clientID, instrument, orderSide, orderType, quantity, price);
		
		//add this order to a client's orders list
		ClientOrders clientOrders = findClientOrders(clientID);
		clientOrders.addOrder(order);
		clientOrdersDB.put(clientID, clientOrders);
		
		//finally process this order and return the object
		processOrder(order);
		return order.getOrderID();
	}
	/*
	 * An order can only be cancelled if clientID matches the order's clientID
	 * Client needs to supply an ID of the order that needs to be cancelled
	 * Returns null if cancellation is not possible, otherwise returns an order that was cancelled
	 */
	
	/**
	 * Method to cancel an existing order, belonging to this client.
	 * 
	 * @param 	clientID  valid client's own id, assigned by the StockExchange during connection
	 * @param 	orderID   one of orderIDs that this client has for own orders
	 * @return 	an order object that was requested to be cancelled, <code>null</code> if the order does not exist, does not belong to this client or has already 
	 * been filled
	 * @exception MarketsClosedException if the market is not currently opened
	 */
	public synchronized Order cancelOrder(int clientID, long orderID){
		if(!started)
			throw new MarketsClosedException("The market is currently closed");
		
		Order order = clientOrdersDB.get(clientID).findOrder(orderID);
		if(order != null){
			return order.getInstrument().processCancelOrder(order);
		}
		return null;
		
	}
	
	/**
	 * Create an instrument to be traded on the exchange. If the instrument is already being traded, new instrument is not created.
	 * @param tickerSymbol a correct ticker symbol for this instrument
	 * @return void
	 */
	public synchronized void registerInstrument(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null){
			instrument = new Instrument(tickerSymbol, updatedOrders);
			instruments.put(tickerSymbol.toUpperCase(), instrument);
		}
	}
	
	/**
	 * Get a list of currently traded instruments.
	 * @return a list of currently traded instruments, <code>null</code> if there are no instruments being traded.
	 */
	public List<String> getTradedInstrumentsList(){
		Iterator<String> iter = instruments.keySet().iterator();
		List<String> list = new ArrayList<String>();
		while(iter.hasNext()){
			list.add(iter.next());
		}
		return list;
	}
	
	/*
	 * The following, are all delegating methods. 
	 */
	
	/**
	 * Get the last price of an instrument
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return this instrument's last price
	 * @exception IllegalArgumentException if wrong ticker symbol has been passed
	 */
	public double getInstrumentLastPrice(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getLastPrice();
	}
	
	/**
	 * Get the total volume of instrument's bid order book
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return this instrument's bid order book volume
	 * @exception IllegalArgumentException if wrong ticker symbol has been passed
	 */
	public long getInstrumentBidVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBidVolume();
	}
	
	/**
	 * Get the total volume of instrument's ask order book
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return this instrument's ask order book volume
	 * @exception IllegalArgumentException if wrong ticker symbol has been passed
	 */
	public long getInstrumentAskVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskVolume();
	}
	
	
	public long getInstrumentBuyVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBuyVolume();
	}
	
	public long getInstrumentSellVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getSellVolume();
	}
	
	// Helper method to ease testing
	protected Instrument getInstrument(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument;
	}
	
	
	private void processOrder(Order order){
		//add the order to the processing queue
		try{
			orders.put(order);
		}
		catch(InterruptedException e){
			System.out.println("ORDER QUEUE EXCEPTION: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	private Instrument findInstrument(String tickerSymbol){
		String symbol = tickerSymbol.toUpperCase();
		if(instruments.containsKey(symbol))
			return instruments.get(symbol);
		return null;
	}
	private ClientOrders findClientOrders(int clientID){
		
		if(clientOrdersDB.containsKey(clientID))
			return clientOrdersDB.get(clientID);
		
		return new ClientOrders(clientID);
	}
	
	
	
}
