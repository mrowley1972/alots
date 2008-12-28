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
	
	public StockExchange(){
		clientOrdersDB = new ConcurrentHashMap<Integer, ClientOrders>();
		instruments = new ConcurrentHashMap<String, Instrument>();
		orders = new LinkedBlockingQueue<Order>();
		updatedOrders = new LinkedBlockingQueue<Order>();
		orderProcessor = new OrderProcessor(orders);
		op = new Thread(orderProcessor);
	}
	
	public int generateClientID(){
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
			orders.clear();
			clientOrdersDB.clear();
			op.join();
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
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol "+ tickerSymbol);
		return instrument.getAskLimitOrders();
	}

	/*
	 * 1. Validate order's parameters, if something is wrong, an order is not created
	 * 2. Create a new order
	 * 3. Put newly created order into client's orders list
	 * 4. Update client orders db
	 * 5. Process this order
	 */
	/**
	 * Submit an order to the exchange to be traded
	 * @param tickerSymbol  a valid traded instrument on the exchange
	 * @param side 			either BUY or SELL
	 * @param type 			either LIMIT or MARKET
	 * @param price 		price greater than zero
	 * @param quantity 		quantity greater than zero
	 * @exception IllegalArgumentException if parameters do not comply
	 */
	public Order createOrder(String tickerSymbol, int clientID, String side, String type, double price, long quantity){
	
		// Validate all of the order's parameters
		Instrument instrument = findInstrument(tickerSymbol);
		core.Order.Side orderSide;
		core.Order.Type orderType;
		
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: " + tickerSymbol);
		
		if(side.equalsIgnoreCase("BUY"))
			orderSide = core.Order.Side.BUY;
		else{
			if(side.equalsIgnoreCase("SELL"))
				orderSide = core.Order.Side.SELL;
			else
				throw new IllegalArgumentException("Invalid side" + side);
		}
		
		if(type.equalsIgnoreCase("LIMIT"))
			orderType = core.Order.Type.LIMIT;
		else{
			if(type.equalsIgnoreCase("MARKET"))
				orderType = core.Order.Type.MARKET;
			else
				throw new IllegalArgumentException("Invalid type" + type);
		}
		if(price < 0)
			throw new IllegalArgumentException("Invalid price" + price);
		
		if(quantity <=0)
			throw new IllegalArgumentException("Invalid quantity" + quantity);
		
		//if all parameters are valid, create a new order
		Order order = new Order(clientID, instrument, orderSide, orderType, quantity, price);
		//add this order to a client's orders list
		ClientOrders clientOrders = findClientOrders(clientID);
		clientOrders.addOrder(order);
		clientOrdersDB.put(clientID, clientOrders);
		
		processOrder(order);
		return order;
		
		
	}
	/*
	 * An order can only be cancelled if clientID matches the order's clientID
	 * Client needs to supply an ID of the order that needs to be cancelled
	 * Returns null if cancellation is not possible, otherwise returns an order that was cancelled
	 */
	
	/**
	 * Method to cancel an existing order, which can only belong to this client.
	 * 
	 * @param 	clientID  needs to be client's id, assigned by the StockExchange during connection
	 * @param 	orderID   can be one of orderIDs that this client has
	 * @return 	an order object that was requested to be cancelled, null if the order does not exist or does not belong to this client
	 */
	public Order cancelOrder(int clientID, long orderID){
		Order order = clientOrdersDB.get(clientID).findOrder(orderID);
		Order result = order.getInstrument().processCancelOrder(order);
		return result;
		
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
	
	
	/*
	 * An instrument is created if it does not already exist
	 * Upon creation, instrument gets access to a queue of updated orders that are pushed to clients
	 * That queue is centralized for the whole exchange
	 */
	/**
	 * Create an instrument to be traded on the exchange. Does not create new instrument, if it is already traded
	 */
	public void registerInstrument(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null){
			instrument = new Instrument(tickerSymbol, updatedOrders);
		}
	}
	
	/**
	 * Print currently traded instruments on the exchange
	 */
	public void printAvailableInstruments(){
		Iterator<String> iter = instruments.keySet().iterator();
		System.out.println("**********AVAILABLE INSTRUMENTS***********");
		while(iter.hasNext()){
			System.out.println(instruments.get(iter.next()));
		}
	}
	
	private Instrument findInstrument(String tickerSymbol){
		if(instruments.containsKey(tickerSymbol))
			return instruments.get(tickerSymbol);
		return null;
	}
	private ClientOrders findClientOrders(int clientID){
		if(clientOrdersDB.containsKey(clientID))
			return clientOrdersDB.get(clientID);
		
		return new ClientOrders(clientID);
	}
	
	
	
}
