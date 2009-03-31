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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import common.IExchangeSimulator;
import common.IOrder;
import common.Notifiable;

public class ExchangeSimulator implements IExchangeSimulator{

	private AbstractMap<Integer, ClientOrders> clientOrdersDB;
	private AbstractMap<String, Instrument> instruments;
	//holds all submitted, but not yet processed client orders
	private BlockingQueue<Order> orders;
	//holds all updated orders that have been either fully or partially processed
	//these are pushed to subscribed clients
	private BlockingQueue<Order> updatedOrders;
	//holds all clients that have been registered to be notified of their orders
	private AbstractMap<Integer, Notifiable> registeredClients;
	//state of the stock exchange
	private boolean started = false;
	
	//responsible for processing new orders from order's queue
	private OrderProcessor orderProcessor;
	//responsible for notifying clients about any updated orders that belong to them
	private DataProcessor dataProcessor;
	private Thread op;
	private Thread dp;
	private static int nextClientID = 0;
	
	/**
	 * Creates a <code>StockExchange</code> with default implementation
	 */
	public ExchangeSimulator(){
		//Initialise all containers
		clientOrdersDB = new ConcurrentHashMap<Integer, ClientOrders>();
		instruments = new ConcurrentHashMap<String, Instrument>();
		orders = new LinkedBlockingQueue<Order>();
		updatedOrders = new LinkedBlockingQueue<Order>();
		registeredClients = new ConcurrentHashMap<Integer, Notifiable>();
		
		//Initialise all processors and make new threads
		orderProcessor = new OrderProcessor(orders);
		dataProcessor = new DataProcessor(registeredClients, updatedOrders);
		op = new Thread(orderProcessor);
		dp = new Thread(dataProcessor);
	}
	
	protected int generateClientID(){
		return ++ExchangeSimulator.nextClientID;
	}
	
	//The first method that client must call to obtain correct clientID and be registered for notifications
	public int register(Notifiable client){
		//Generate unique clientID and record for future notifications
		Integer clientID = generateClientID();
		registeredClients.put(clientID, client);
		return clientID.intValue();
	}
	
	/**
	 * Start the exchange
	 */
	public void start(){
		if(!started){
			started = true;
			op.start();
			dp.start();
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
		System.out.println("Order " + order.getOrderID() + " has been submitted");
		
		return order.getOrderID();
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
	 * Cancel an existing order <code>orderID</code>, belonging to this client with <code>clientID</code>.
	 * 
	 * @param 	clientID  valid client's own id, assigned by the StockExchange during connection
	 * @param 	orderID   one of orderIDs that this client has for own orders
	 * @return 	an order object that was requested to be cancelled, <code>null</code> if the order does not exist, does not belong
	 *  to this client or has already been filled (specific reason is hard to trace back).
	 * @exception MarketsClosedException if the market is not currently opened
	 */
	public synchronized IOrder cancelOrder(int clientID, long orderID){
		Order order = clientOrdersDB.get(clientID).findOrder(orderID);
		if(order != null){
			return order.getInstrument().processCancelOrder(order);
		}
		return null;
		
	}
	
	/**
	 * Get an order with <code>orderID</code> and belonging to a client with <code>clientID</code>
	 * @param clientID	valid client's own id, assigned by the StockExchange during connection
	 * @param orderID	one of orderIDs that this client has for own orders
	 * @return	an order with <code>orderID</code>, or <code>null</code> if an order does not belong to a client
	 *  with <code>clientID</code>
	 */
	public IOrder getClientOrder(int clientID, long orderID){
		IOrder order = clientOrdersDB.get(clientID).findOrder(orderID);
		if(order != null)
			return order;
		//return null if order is not found
		return null;
	}
	
	
	
	/**
	 * Get latest bid order book for an instrument
	 * @param tickerSymbol ticker symbol of a traded instrument
	 * @return current bid order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<IOrder> getInstrumentBidBook(String tickerSymbol){
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
	
	/**
	 * Get instrument's best bid price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's best bid price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBestBid(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBestBid();
	}
	
	/**
	 * Get instrument's best ask price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's best ask price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBestAsk(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBestAsk();
	}
	
	/**
	 * Get instrument's bid price at specified <code>depth</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param depth			required depth
	 * @return instrument's bid price at <code>depth</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidPriceAtDepth(String tickerSymbol, int depth){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBidPriceAtDepth(depth);
	}
	
	/**
	 * Get instrument's ask price at specified <code>depth</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param depth			required depth
	 * @return instrument's ask price at <code>depth</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskPriceAtDepth(String tickerSymbol, int depth){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskPriceAtDepth(depth);
	}
	
	/**
	 * Get instrument's ask volume at specified <code>price</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param price			required price
	 * @return instrument's ask volume at <code>price</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentAskVolumeAtPrice(String tickerSymbol, double price){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskVolumeAtPrice(price);
	}
	
	/**
	 * Get instrument's bid volume at specified <code>price</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param price			required price
	 * @return instrument's bid volume at <code>price</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentBidVolumeAtPrice(String tickerSymbol, double price){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBidVolumeAtPrice(price);
	}
	
	/**
	 * Get instrument's daily bid high
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid high for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidHigh(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBidHigh();
	}
	
	/**
	 * Get instrument's daily bid low
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidLow(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getBidLow();
	}
	
	/**
	 * Get instrument's daily ask high
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask high for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskHigh(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskHigh();
	}
	
	/**
	 * Get instrument's daily ask low
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskLow(String tickerSymbol){
		Instrument instrument = findInstrument(tickerSymbol);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ tickerSymbol);
		return instrument.getAskLow();
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
	
	
	public static void main(String args[]){
		if(args.length < 1){
			System.out.println("Usage: ExchangeSimulator <rmi_port>");
			System.exit(1);
		}
		int rmiPort = Integer.parseInt(args[0]);
		if(System.getSecurityManager() == null){
			System.setSecurityManager(new RMISecurityManager());
		}
		
		try{
			String name = "ExchangeSimulator";
			ExchangeSimulator exchange = new ExchangeSimulator();
			exchange.start();
			
			IExchangeSimulator stub = (IExchangeSimulator) UnicastRemoteObject.exportObject(exchange, 0);
			Registry registry = LocateRegistry.getRegistry(rmiPort);
			registry.rebind(name, stub);
			System.out.println("Exchange Simulator is up and running on the server");
			
		}
		catch(Exception e){
			System.out.println("ExchangeSimulator exception: ");
			e.printStackTrace();
		}
	}
	
}