/**
 * The MIT License

Copyright (c) 2009 Asset Tarabayev

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

 */


/**
 * 
 * StockExchange is the only class that clients are able to connect to directly through RMI.
 * Clients must implement Notifiable interface in order to trade at this exchange.
 * It delegates all calls to necessary classes, returns and wraps results to clients.
 * Individual instruments keep their own ask and bid books, as well as executed and partially executed orders.
 * This class keeps a queue of all currently outstanting orders, as well as a queue of updated orders and instrument notifications. 
 * It also keeps track of all currently traded instruments and keeps record of client's individual orders.
 * Currently only one thread operates on processing orders, but a ThreadPool of such order processors 
 * can be maintained in the future.
 * 
 * @author Asset Tarabayev
 */

package core;

import java.io.IOException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
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
import java.util.logging.*;

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
	private OrderProcessingEngine orderProcessingEngine;
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
	
	protected static Logger logger;
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	
	
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
		orderProcessingEngine = new OrderProcessingEngine(submittedOrders);
		orderNotificationEngine = new ClientOrdersNotificationEngine(registeredClients, updatedOrders);
		taqNotificationEngine = new TAQNotificationEngine(instrumentSubscribers, taqNotifications);
		op = new Thread(orderProcessingEngine);
		cn = new Thread(orderNotificationEngine);
		ne = new Thread(taqNotificationEngine);
		
		//configure logging by specifying fully qualified path and file's name
		configureLogging("/Users/Asset/exchange/exchangeLog.txt");
		
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
			
			logger.info("Exchange Simulator started... logging system is running...");
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
			taqNotifications.clear();
			registeredClients.clear();
			started = false;
			
			logger.info("Exchange Simulator is stopped... terminating...");
			System.exit(0);
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
	//just a security precaution
	protected int generateClientID(){
		return ExchangeSimulator.nextClientID += 7;
	}
	
	private void configureLogging(String logFilePath) {
		
		logger = Logger.getLogger(ExchangeSimulator.class.getName());
		logger.setLevel(Level.INFO);
		try{
			fileTxt = new FileHandler(logFilePath);
			formatterTxt = new SimpleFormatter();
			fileTxt.setFormatter(formatterTxt);
			logger.addHandler(fileTxt);
	
		}catch(IOException e){
			System.out.println("Problem initialising logger...");
			e.printStackTrace();
		}
	}
	
	//The very first method that client must call to obtain correct clientID and be registered for notifications
	public int register(Notifiable client){
		//Generate unique clientID and record for future notifications
		Integer clientID = generateClientID();
		registeredClients.put(clientID, client);
		
		logger.info("Client has registered with the exchange. ClientID: " + clientID);
		
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
		logger.info("New instrument registered: " + instrument.getTicker());
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
		
		if(instrumentSubscribers.containsKey(instrument.getTicker())){
			//don't add the same client twice
			if(!(instrumentSubscribers.get(instrument.getTicker()).contains(client))){
				instrumentSubscribers.get(instrument.getTicker()).add(client);
			}
		}
		//no one has subscribed to this instrument previously, make new entry
		else{
			ArrayList<Notifiable> list = new ArrayList<Notifiable>();
			list.add(client);
			instrumentSubscribers.put(instrument.getTicker(), list);
		}	
		
		try{
			logger.info("clientID: " + client.getClientID() + " subscribed for notifications for " + instrument.getTicker());
		}catch(RemoteException e){
			logger.warning("Communication with a client is corrupted..." + "\n" + e.getStackTrace().toString());
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
		if(instrumentSubscribers.containsKey(instrument.getTicker())){
			if(instrumentSubscribers.get(instrument.getTicker()).contains(client))
				instrumentSubscribers.get(instrument.getTicker()).remove(client);
		}
		try{
			logger.info("clientID: " + client.getClientID() + " unsubscribed from notifications for " +
					instrument.getTicker());
		}catch(RemoteException e){
			logger.warning("Communication with a client is corrupted..." + "\n" + e.getStackTrace().toString());
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
		logger.info("Order " + order.getOrderID() + " is submitted by client " + clientID);
		
		//return of a unique orderID indicates a confirmation that an order has been submitted.
		return order.getOrderID();
	}
	
	private void processOrder(Order order){
		//add the order to the processing queue
		try{
			submittedOrders.put(order);
			logger.info("Submitted order " + order.getOrderID());
		}
		catch(InterruptedException e){
			logger.warning("ORDER QUEUE EXCEPTION " + e.getMessage() + "\n" + e.getStackTrace().toString());
		}
	}
	
	/**
	 * Cancel an existing order <code>orderID</code>, belonging to this client with <code>clientID</code>.
	 * 
	 * @param 	clientID  valid client's own id, assigned by the StockExchange during connection
	 * @param 	orderID   one of orderIDs that this client has for own orders
	 * @return 	an order object that was requested to be cancelled, <code>null</code> if the order does not exist, does not belong
	 *  to this client or has already been filled (specific reason is hard to trace back).
	 */
	public synchronized IOrder cancelOrder(int clientID, long orderID){
		Order order = clientOrdersDB.get(clientID).findOrder(orderID);
		if(order != null){
			logger.info("client " + clientID + " cancelled order " + orderID);
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
		
		logger.info("Last price for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Bid volume for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Ask volume for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Buy volume for " + instrument.getTicker() + " is requsted");
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
		
		logger.info("Sell volume for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Average price for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Average buy price for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Average sell price for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Bid VWAP for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Ask VWAP for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Best bid for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Best ask for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Bid price at depth " + depth + " for " + instrument.getTicker() + " is requested");		
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
		
		logger.info("Ask price at depth " + depth + " for " + instrument.getTicker() + " is requested");	
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
		
		logger.info("Ask volume at price " + price + " for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Bid volume at price " + price + " for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Bid high for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Bid low for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Ask high for " + instrument.getTicker() + " is requested");
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
		
		logger.info("Ask low for " + instrument.getTicker() + " is requested");
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
	
	//This is a sample main method to show a template of how this class should be initialised with RMI
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
			
			//export stub with anonymous port - will be assigned by OS at runtime
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
