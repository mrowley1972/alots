/**
 * 
 * StockExchange is the only class that clients are able to connect to directly through RMI.
 * It delegates all calls to necessary classes, returns and wraps results to clients.
 * Individual instruments keep their own ask and bid books, as well as executed and partially executed orders.
 * This class keeps a queue of all currently outstanting orders, as well as a queue of updated orders. It
 * also keeps track of all currently traded instruments and keeps record of client's individual orders.
 * Currently only one thread operates on processing orders, but a ThreadPool of such order processors 
 * can be maintained in the future.
 * 
 * @author Asset Tarabayev
 */

package core;

import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.IExchangeSimulator;
import common.IOrder;

public class ExchangeSimulator implements IExchangeSimulator{

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
	public ExchangeSimulator(){
		clientOrdersDB = new ConcurrentHashMap<Integer, ClientOrders>();
		instruments = new ConcurrentHashMap<String, Instrument>();
		orders = new LinkedBlockingQueue<Order>();
		updatedOrders = new LinkedBlockingQueue<Order>();
		orderProcessor = new OrderProcessor(orders);
		op = new Thread(orderProcessor);
	}
	
	protected int generateClientID(){
		return ++ExchangeSimulator.nextClientID;
	}
	
	/**
	 * Start the exchange
	 */
	public void start(){
		if(!started){
			started = true;
			op.start();
		}
	}
	/**
	 * Stop the exchange.
	 */
	protected void stop(){
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
	 * Check whether the exchange is open
	 * @return true if the exchange is operating, false otherwise
	 */
	public boolean isOpen(){
		return started;
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
	public synchronized long submitOrder(String tickerSymbol, int clientID, 
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
		//return of a unique orderID indicates a confirmation that an order has been submitted.
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
	 * @return 	an order object that was requested to be cancelled, <code>null</code> if the order does not exist, does not belong
	 *  to this client or has already been filled (specific reason is hard to trace back).
	 * @exception MarketsClosedException if the market is not currently opened
	 */
	public synchronized Order cancelOrder(int clientID, long orderID){
		//if(!started)
			//throw new MarketsClosedException("The market is currently closed");
		
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
	 * Get latest bid order book for an instrument
	 * @param tickerSymbol ticker symbol of a traded instrument
	 * @return current bid order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<IOrder> getInstrumentBidBook(String tickerSymbol){
		//if(!started)
			//throw new MarketsClosedException("The market is currently closed");
		
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol " + tickerSymbol);
		return instrument.getBidLimitOrders();
	}
	
	/**
	 * Get latest ask order book for an instrument
	 * @param tickerSymbol ticker symbol of a traded instrument
	 * @return current ask order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<IOrder> getInstrumentAskBook(String tickerSymbol){
		//if(!started)
			//throw new MarketsClosedException("The market is currently closed");
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol "+ tickerSymbol);
		return instrument.getAskLimitOrders();
	}

	/*
	 * The following are delegating methods to Instrument class. Client must not be able to obtain Instrument object directly
	 */
	
	/**
	 * Get the last price of an instrument
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's last price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
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
	 * @return instrument's bid order book volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
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
	 * @return instrument's ask order book volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentAskVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskVolume();
	}
	
	/**
	 * Get instrument's buy volume
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's buy volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentBuyVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBuyVolume();
	}
	
	/**
	 * Get instrument's sell volume
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's sell volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentSellVolume(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getSellVolume();
	}
	
	/**
	 * Get instrument's average price per share
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average buy price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	
	public double getInstrumentAveragePrice(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAveragePrice();
	}
	
	/**
	 * Get instrument's average buy price per share
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average sell price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	
	public double getInstrumentAverageBuyPrice(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAverageBuyPrice();
	}
	
	/**
	 * Get instrument's average sell price per share
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average sell price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	
	public double getInstrumentAverageSellPrice(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAverageSellPrice();
	}
	
	/**
	 * Get instrument's bid volume-weighted average price (VWAP)
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid VWAP
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidVWAP(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBidVWAP();
	}
	
	/**
	 * Get instrument's ask volume-weighted average price (VWAP)
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask VWAP
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskVWAP(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskVWAP();
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
	
	// Helper method to ease testing, must only be used during testing
	protected Instrument getInstrument(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument;
	}
	
	/*
	public static void main(String args[]){
		if(System.getSecurityManager() == null){
			System.setSecurityManager(new RMISecurityManager());
		}
		try{
			String name = "ExchangeSimulator";
			IExchangeSimulator exchangeSimulator = new ExchangeSimulator();
			//exchangeSimulator.start();
			
			IExchangeSimulator stub = (IExchangeSimulator) UnicastRemoteObject.exportObject(exchangeSimulator, 0);
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind(name, stub);
			System.out.println("Exchange Simulator is bound");
			
		}
		catch(Exception e){
			System.out.println("ExchangeSimulator exception");
			e.printStackTrace();
		}
	}
	*/
}
