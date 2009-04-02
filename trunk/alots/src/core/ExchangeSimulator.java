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

	//holds all orders that have been submitted by a client
	private AbstractMap<Integer, ClientOrders> clientOrdersDB;
	//holds all currently traded instruments
	private AbstractMap<String, Instrument> registeredInstruments;
	//holds all submitted, but not yet processed client orders
	private BlockingQueue<Order> submittedOrders;
	
	//holds all clients that have been registered to be notified of their orders
	private AbstractMap<Integer, Notifiable> registeredClients;
	
	//holds all updated orders that have been either fully or partially processed
	//these are pushed to subscribed clients
	private BlockingQueue<Order> updatedOrders;
	//holds subscriptions to specific instruments
	private AbstractMap<String, ArrayList<Notifiable>> instrumentSubscribers;
	//holds notifications to be communicated to clients
	private BlockingQueue<TAQNotification> taqNotifications;
	
	
	//responsible for processing new orders from order's queue
	private OrderProcessor orderProcessingEngine;
	//responsible for notifying clients about any updated orders that belong to them
	private ClientOrdersNotificationEngine orderNotificationEngine;
	//responsible for notifying clients of changes to the instruments they subscribed to
	private TAQNotificationEngine taqNotificationEngine;
	
	private Thread op;
	private Thread cn;
	private Thread ne;
	private static int nextClientID = 0;
	
	//state of the stock exchange
	private boolean started = false;
	
	/**
	 * Creates a <code>StockExchange</code> with default implementation
	 */
	public ExchangeSimulator(){
		//Initialise all containers
		clientOrdersDB = new ConcurrentHashMap<Integer, ClientOrders>();
		registeredInstruments = new ConcurrentHashMap<String, Instrument>();
		submittedOrders = new LinkedBlockingQueue<Order>();
		updatedOrders = new LinkedBlockingQueue<Order>();
		registeredClients = new ConcurrentHashMap<Integer, Notifiable>();
		instrumentSubscribers = new ConcurrentHashMap<String, ArrayList<Notifiable>>();
		taqNotifications = new LinkedBlockingQueue<TAQNotification>();
		
		//Initialise all engines and make new threads
		orderProcessingEngine = new OrderProcessor(submittedOrders);
		orderNotificationEngine = new ClientOrdersNotificationEngine(registeredClients, updatedOrders);
		taqNotificationEngine = new TAQNotificationEngine(instrumentSubscribers, taqNotifications);
		op = new Thread(orderProcessingEngine);
		cn = new Thread(orderNotificationEngine);
		ne = new Thread(taqNotificationEngine);
	}
	
	/**
	 * Start the exchange
	 */
	public void start(){
		if(!started){
			started = true;
			op.start();
			cn.start();
			ne.start();
		}
	}
	
	/**
	 * Stop the exchange.
	 */
	protected void stop(){
		try{
			op.join(1000);
			submittedOrders.clear();
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
	
	//client ids aren't sequential to avoid trivial guessing by other traders
	protected int generateClientID(){
		return ExchangeSimulator.nextClientID +=7;
	}
	
	//The very first method that client must call to obtain correct clientID and be registered for notifications
	public int register(Notifiable client){
		//Generate unique clientID and record for future notifications
		Integer clientID = generateClientID();
		registeredClients.put(clientID, client);
		return clientID.intValue();
	}
	

	
	/**
	 * Create an instrument to be traded on the exchange. If the instrument is already being traded, new instrument is not created.
	 * @param ticker a correct ticker symbol for this instrument
	 * @return void
	 */
	public synchronized void registerInstrument(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null){
			instrument = new Instrument(ticker, updatedOrders, taqNotifications);
			registeredInstruments.put(ticker.toUpperCase(), instrument);
		}
	}
	

	/**
	 * Subscribe to get TAQ notifications from a particular instrument
	 * @param client	a remote Notifiable stub (yourself)
	 * @param ticker	a valid traded instrument's ticker symbol to be subscribed to
	 * @exception IllegalArgumentException if parameters do not comply
	 */
	public void subscribeToInstrument(Notifiable client, String ticker){
		
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Cannot subscribe to this instrument, invalid ticker symbol: " + ticker);
		
		if(instrumentSubscribers.containsKey(instrument.getTickerSymbol())){
			//don't add the same client twice
			if(!(instrumentSubscribers.get(instrument.getTickerSymbol()).contains(client))){
				instrumentSubscribers.get(instrument.getTickerSymbol()).add(client);
			}
		}
		//no one has subscribed to this instrument previously, make new entry
		else{
			ArrayList<Notifiable> list = new ArrayList<Notifiable>();
			list.add(client);
			instrumentSubscribers.put(instrument.getTickerSymbol(), list);
		}		
	}
	
	/**
	 * Unsubscribe from getting notifications about an instrument
	 * @param client	a remote Notifiable stub (yourself)
	 * @param ticker	a valid traded instrument's ticker symbol to be unsubscribed from
	 * @exception IllegalArgumentException if parameters do not comply
	 */
	public void unsubscribeFromInstrument(Notifiable client, String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: " + ticker);
		
		//will ignore if the client has never been subscribed to this instrument
		if(instrumentSubscribers.containsKey(instrument.getTickerSymbol())){
			if(instrumentSubscribers.get(instrument.getTickerSymbol()).contains(client))
				instrumentSubscribers.get(instrument.getTickerSymbol()).remove(client);
		}
	}
	
	/**
	 * Submit an order to the exchange to be traded
	 * @param ticker  a valid traded instrument's ticker symbol
	 * @param side 			either BUY or SELL
	 * @param type 			either LIMIT or MARKET
	 * @param price 		positive order price 
	 * @param quantity 		non-negative order quantity
	 * @return orderID		an id of this submitted order
	 * @exception IllegalArgumentException if parameters do not comply
	 */
	public synchronized long submitOrder(String ticker, int clientID, 
			String side, String type, double price, long quantity){
	
		// Validate all of the order's parameters
		Instrument instrument = findInstrument(ticker);
		core.Order.Side orderSide;
		core.Order.Type orderType;
		//can only create orders for existing instruments
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: " + ticker);
		
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
			submittedOrders.put(order);
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
	 * @param ticker ticker symbol of a traded instrument
	 * @return current bid order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<IOrder> getInstrumentBidBook(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol " + ticker);
		
		return instrument.getBidLimitOrders();
	}
	
	/**
	 * Get latest ask order book for an instrument
	 * @param ticker ticker symbol of a traded instrument
	 * @return current ask order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	public List<IOrder> getInstrumentAskBook(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol "+ ticker);
		
		return instrument.getAskLimitOrders();
	}

	/*
	 * The following are delegating methods to Instrument class. Client must not be able to obtain Instrument object directly
	 */
	
	/**
	 * Get the last price of an instrument
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's last price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentLastPrice(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getLastPrice();
	}
	
	/**
	 * Get the total volume of instrument's bid order book
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid order book volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentBidVolume(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBidVolume();
	}
	
	/**
	 * Get the total volume of instrument's ask order book
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask order book volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentAskVolume(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAskVolume();
	}
	
	/**
	 * Get instrument's buy volume
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's buy volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentBuyVolume(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBuyVolume();
	}
	
	/**
	 * Get instrument's sell volume
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's sell volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentSellVolume(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getSellVolume();
	}
	
	/**
	 * Get instrument's average price per share
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average buy price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	
	public double getInstrumentAveragePrice(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAveragePrice();
	}
	
	/**
	 * Get instrument's average buy price per share
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average sell price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	
	public double getInstrumentAverageBuyPrice(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAverageBuyPrice();
	}
	
	/**
	 * Get instrument's average sell price per share
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average sell price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	
	public double getInstrumentAverageSellPrice(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAverageSellPrice();
	}
	
	/**
	 * Get instrument's bid volume-weighted average price (VWAP)
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid VWAP
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidVWAP(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBidVWAP();
	}
	
	/**
	 * Get instrument's ask volume-weighted average price (VWAP)
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask VWAP
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskVWAP(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAskVWAP();
	}
	
	/**
	 * Get instrument's best bid price
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's best bid price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBestBid(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBestBid();
	}
	
	/**
	 * Get instrument's best ask price
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's best ask price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBestAsk(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBestAsk();
	}
	
	/**
	 * Get instrument's bid price at specified <code>depth</code>
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @param depth			required depth
	 * @return instrument's bid price at <code>depth</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidPriceAtDepth(String ticker, int depth){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBidPriceAtDepth(depth);
	}
	
	/**
	 * Get instrument's ask price at specified <code>depth</code>
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @param depth			required depth
	 * @return instrument's ask price at <code>depth</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskPriceAtDepth(String ticker, int depth){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAskPriceAtDepth(depth);
	}
	
	/**
	 * Get instrument's ask volume at specified <code>price</code>
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @param price			required price
	 * @return instrument's ask volume at <code>price</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentAskVolumeAtPrice(String ticker, double price){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAskVolumeAtPrice(price);
	}
	
	/**
	 * Get instrument's bid volume at specified <code>price</code>
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @param price			required price
	 * @return instrument's bid volume at <code>price</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentBidVolumeAtPrice(String ticker, double price){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBidVolumeAtPrice(price);
	}
	
	/**
	 * Get instrument's daily bid high
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid high for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidHigh(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBidHigh();
	}
	
	/**
	 * Get instrument's daily bid low
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentBidLow(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getBidLow();
	}
	
	/**
	 * Get instrument's daily ask high
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask high for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskHigh(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAskHigh();
	}
	
	/**
	 * Get instrument's daily ask low
	 * @param ticker	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public double getInstrumentAskLow(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
		return instrument.getAskLow();
	}
	
	/**
	 * Get a list of currently traded instruments.
	 * @return a list of currently traded instruments, <code>null</code> if there are no instruments being traded.
	 */
	public List<String> getTradedInstrumentsList(){
		Iterator<String> iter = registeredInstruments.keySet().iterator();
		List<String> list = new ArrayList<String>();
		while(iter.hasNext()){
			list.add(iter.next());
		}
		return list;
	}
	
	private Instrument findInstrument(String ticker){
		String symbol = ticker.toUpperCase();
		if(registeredInstruments.containsKey(symbol))
			return registeredInstruments.get(symbol);
		return null;
	}
	
	private ClientOrders findClientOrders(int clientID){
		
		if(clientOrdersDB.containsKey(clientID))
			return clientOrdersDB.get(clientID);
		
		return new ClientOrders(clientID);
	}
	
	// Helper method to ease testing, must only be used during testing
	protected Instrument getInstrument(String ticker){
		Instrument instrument = findInstrument(ticker);
		if(instrument == null)
			throw new IllegalArgumentException("Invalid ticker symbol: "+ ticker);
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
			System.out.println("*** Exchange Simulator is up and running on the server ***");
			
		}
		catch(Exception e){
			System.out.println("ExchangeSimulator exception: ");
			e.printStackTrace();
		}
	}
	
}
