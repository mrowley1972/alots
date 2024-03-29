/**
 * This class implements BookEngine interface and is used directly by an Instrument class. 
 * Orders are only matched at the top of the order book, which is triggered by insertion of a new order. 
 * This implementation guarantees insertion in O(log(n)) into both ask and bid sides of the book, using binary search index discovery with minor modifications. 
 * Insertion into the book is based on the price-time priority algorithm.
 * 
 * @author Asset Tarabayev
 */

package core;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class EquityBookEngine implements BookEngine {

	private static final long serialVersionUID = -6028495077079186305L;
	
	private String instrumentTicker;
	
	private List<Order> bidLimitOrders;
	private List<Order> askLimitOrders;
	private List<Order> filledOrders;
	private List<Order> partiallyFilledOrders;
	private BlockingQueue<Order> updatedOrders;
	private BlockingQueue<TAQNotification> notifications;
	private static Logger logger;
	private static FileHandler fileTxt;
	private static SimpleFormatter formatterTxt;
	
	/*
	 * When BookEngine object is created, it gets access to all Instrument books 
	 */
	public EquityBookEngine(List<Order> bidLimitOrders, List<Order> askLimitOrders, List<Order> filledOrders, 
			List<Order> partiallyFilledOrders, BlockingQueue<Order> updatedOrders, 
			BlockingQueue<TAQNotification> notifications, String instrumentTicker){
		
		this.bidLimitOrders = bidLimitOrders;
		this.askLimitOrders = askLimitOrders;
		this.filledOrders = filledOrders;
		this.partiallyFilledOrders = partiallyFilledOrders;
		this.updatedOrders = updatedOrders;
		this.notifications = notifications;
		
		this.instrumentTicker = instrumentTicker;
		
		//specify location of the log file
		configureLogging(ExchangeSimulator.logFilePath + getInstrumentTicker() + "BookEngine.log");
		
		logger.info("Equity Book Engine is up and running...");
	}
	
	private String getInstrumentTicker(){
		return instrumentTicker;
	}
	
	private void configureLogging(String logFilePath){
		logger = Logger.getLogger(EquityBookEngine.class.getName());
		logger.setLevel(Level.INFO);
		
		try{
			fileTxt = new FileHandler(logFilePath);
			formatterTxt = new SimpleFormatter();
			fileTxt.setFormatter(formatterTxt);
			logger.addHandler(fileTxt);
			
		}catch(IOException e){
			System.out.println("Problem initialising logger for " + EquityBookEngine.class.getName());
		}
	}
	
	public Order processCancelOrder(Order order){
		Order o;
		
		if(order.side() == core.Order.Side.BUY){
			Iterator<Order> iter = bidLimitOrders.iterator();
			while(iter.hasNext()){
				o = iter.next();
				if(o.equals(order)){
					iter.remove();
					o.cancel();
					o.setStatus(core.Order.Status.CANCELLED);
					updatedOrders.add(o);
					logger.info("Cancelled order " + order.getOrderID());
					return o;
				}
			}
			return null;
		}
		
		if(order.side() == core.Order.Side.SELL){
			Iterator<Order> iter = askLimitOrders.iterator();
			while(iter.hasNext()){
				o = iter.next();
				if(o.equals(order)){
					iter.remove();
					o.cancel();
					o.setStatus(core.Order.Status.CANCELLED);
					updatedOrders.add(o);
					logger.info("Cancelled order " + order.getOrderID());
					return o;
				}
			}
			return null;
		}
		
		return null;
	}
	
	public void processNewOrder(Order order) {
		
		Instrument instrument = order.getInstrument();
		
		if(order.side() == core.Order.Side.BUY){
			//get the market current price for the market order
			if(order.type() == core.Order.Type.MARKET){
				if(askLimitOrders.size() > 0)
					order.setPrice(askLimitOrders.get(0).getPrice());
				
				//if there is no order on the opposite side of the book, then we need to immediately reject the order
				else{
						order.cancel();
						order.setStatus(core.Order.Status.REJECTED);
						updatedOrders.add(order);
						logger.info("Rejected order " + order.getOrderID());
						return;
				}
			}
			
			//Update instrument's immediate statistics
			instrument.updateBidVWAP(order.getQuantity(), order.getPrice());
			instrument.updateBidHigh(order.getPrice());
			instrument.updateBidLow(order.getPrice());
			
			//Try to match immediately
			matchBuyOrder(order);
			
			if(order.isFilled())
				addToFilledOrders(order);
			else
				insertBuyOrder(order);
		}
		else{
			//get the market current price for the market order
			if(order.type() == core.Order.Type.MARKET){
				if(bidLimitOrders.size() > 0)
					order.setPrice(bidLimitOrders.get(0).getPrice());
				
				//if there is no order on the opposite side of the book, then we need to immediately reject the order
				else{
						order.cancel();
						order.setStatus(core.Order.Status.REJECTED);
						updatedOrders.add(order);
						logger.info("Rejected order " + order.getOrderID());
						return;
				}
			}
			
			//Update instrument's immediate statistics
			instrument.updateAskVWAP(order.getQuantity(), order.getPrice());
			instrument.updateAskHigh(order.getPrice());
			instrument.updateAskLow(order.getPrice());
			
			//Try to match immediately
			matchSellOrder(order);
			
			if(order.isFilled())
				addToFilledOrders(order);
			else
				insertSellOrder(order);
		}
			
		//Remove filled orders from partially filled orders, and add them to filled orders
		cleanUpPartiallyFilledOrders();
	}

	//guaranteed to perform insertion in O(log(n))
	public void insertBuyOrder(Order order){
		order.getInstrument().updateBidVolume(order.getOpenQuantity());
		int i = findIndex(order);	
		
		bidLimitOrders.add(i, order);
		logger.info("Order " + order.getOrderID() + " inserted into bid order book");
		
	}
	
	public void insertSellOrder(Order order){
		order.getInstrument().updateAskVolume(order.getOpenQuantity());
		int i = findIndex(order);
		
		askLimitOrders.add(i, order);
		logger.info("Order " + order.getOrderID() + " inserted into ask order book");
		
	}
	
	//finds an index where to insert this order. BinarySearch is used, but the order must never be found, as every
	//new order is unique - hence, we only look for a correct index
	private int findIndex(Order order){
		
		//this returns (-insertion point -1), where insertion point is the needed index
		int i = Collections.binarySearch(bidLimitOrders, order, new PriceTimePriorityComparator());
	
		return -(i+1);	
	}
	
	private synchronized void matchSellOrder(Order order){
		if(bidLimitOrders.size() > 0){
			Instrument instrument = order.getInstrument();
			Iterator<Order> iter = bidLimitOrders.iterator();
			
			while(iter.hasNext()){
				Order curOrder = iter.next();
				double price = curOrder.getPrice();
				
				if(price >= order.getPrice() && order.getOpenQuantity() > 0){
					
					//calculate matched quantity
					long quantity;
					if(curOrder.getOpenQuantity() > order.getOpenQuantity())
						quantity = order.getOpenQuantity();
					else
						quantity = curOrder.getOpenQuantity();
				
					//update order states and set instrument's last price
					curOrder.execute(quantity, price);
					order.execute(quantity, price);
					long time = System.currentTimeMillis();
					
					addToPartiallyFilledOrders(order);
					
					if(curOrder.isFilled()){
						iter.remove();
						addToFilledOrders(curOrder);
						curOrder.setStatus(core.Order.Status.FILLED);
					}
					else{
						addToPartiallyFilledOrders(curOrder);
						curOrder.setStatus(core.Order.Status.PARTIALLY_FILLED);
					}
					
					//update instrument statistics
					instrument.updateLastPrice(price);
					instrument.updateBidVolume(-quantity);
					instrument.updateSellVolume(quantity);
					instrument.updateAveragePrice(quantity, price);
					instrument.updateAverageSellPrice(quantity, price);
					 
					logger.info("Matched order " + order.getOrderID() + "; quantity: " + quantity + "; price " + price + " @ " + new Date(time));
					
					/*
					 * Deal with all the notifications - Order and TAQ
					 */
					if(order.isFilled())
						order.setStatus(core.Order.Status.FILLED);
					else
						order.setStatus(core.Order.Status.PARTIALLY_FILLED);
					//notify clients about updated orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
					
					//create new trade notification
					TAQNotification tradeNotification = new TAQNotification(TAQNotification.NotificationType.TRADE, instrument.getTicker(), 
							time, price, quantity, Order.Side.SELL);
					notifications.add(tradeNotification);
					
					//create new quote notification
					TAQNotification quoteNotification = new TAQNotification(TAQNotification.NotificationType.QUOTE, instrument.getTicker(), 
							time, instrument.getBidPriceAtDepth(0), instrument.getAskPriceAtDepth(0));
					notifications.add(quoteNotification);
					
					logger.info("Trade notification generated @ " + new Date(time));
					logger.info("Quote notification generated @ " + new Date(time));
				}
				//need to break to avoid going through the whole book, as it is ordered
				else{
					break;
				}
			}
		}
	}
	
	private synchronized void matchBuyOrder(Order order){
		
		if(askLimitOrders.size()>0){
			Instrument instrument = order.getInstrument();
			Iterator<Order> iter = askLimitOrders.iterator();
			
			while(iter.hasNext()){
				Order curOrder = iter.next();
				double price = curOrder.getPrice();
				if(order.type() == core.Order.Type.MARKET && order.getOpenQuantity() > 0)
					order.setPrice(price);
				
				//If the current price of sell order price is less
				//than the price of buy order, then it is a best match
				if(price <= order.getPrice() && order.getOpenQuantity() > 0){
					long quantity;
					//figure out the quantity matched
					if(curOrder.getOpenQuantity() > order.getOpenQuantity())
						quantity = order.getOpenQuantity();
					else
						quantity = curOrder.getOpenQuantity();
					
					//update order states and set instrument's last price
					curOrder.execute(quantity, price);
					order.execute(quantity, price);
					long time = System.currentTimeMillis();
					
					if(curOrder.isFilled()){
						iter.remove();
						addToFilledOrders(curOrder);
						curOrder.setStatus(core.Order.Status.FILLED);
					}else{
						addToPartiallyFilledOrders(curOrder);
						curOrder.setStatus(core.Order.Status.PARTIALLY_FILLED);
					}
					addToPartiallyFilledOrders(order);
					
					//update instrument history
					instrument.updateLastPrice(price);
					instrument.updateAskVolume(-quantity);
					instrument.updateBuyVolume(quantity);
					instrument.updateAveragePrice(quantity, price);
					instrument.updateAverageBuyPrice(quantity, price);
					
					logger.info("Matched order " + order.getOrderID() + "; quantity: " + quantity + "; price " + price + " @ " + new Date(time));
					
					/*
					 * Deal with all the notifications - Order and TAQ 
					 */
					if(order.isFilled())
						order.setStatus(core.Order.Status.FILLED);
					else
						order.setStatus(core.Order.Status.PARTIALLY_FILLED);
					//notify clients about updated orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
				
					//create new trade notification
					TAQNotification tradeNotification = new TAQNotification(TAQNotification.NotificationType.TRADE, instrument.getTicker(), 
							time, price, quantity, Order.Side.BUY);
					notifications.add(tradeNotification);
					
					//create new quote notification
					TAQNotification quoteNotification = new TAQNotification(TAQNotification.NotificationType.QUOTE, instrument.getTicker(), 
							time, instrument.getBidPriceAtDepth(0), instrument.getAskPriceAtDepth(0));
					notifications.add(quoteNotification);
					
					logger.info("Trade notification generated @ " + new Date(time));
					logger.info("Quote notification generated @ " + new Date(time));
					
				} 
				//need to break to avoid going through the whole book as it is ordered
				else{
					break;
				}	
			}
		}
	}

	private void addToPartiallyFilledOrders(Order order){
		if(order.isFilled()){
			if(partiallyFilledOrders.contains(order)){
				partiallyFilledOrders.remove(order);
				addToFilledOrders(order);
			}
		}
		else{
			if(!partiallyFilledOrders.contains(order)){
				partiallyFilledOrders.add(order);
			}
		}
		
	}

	private void addToFilledOrders(Order order){
		if(!filledOrders.contains(order))
			filledOrders.add(order);
	}
	
	//clean up partiallyFilledOrders by eliminating already filled orders, and moving
	//them into filledOrders
	private void cleanUpPartiallyFilledOrders(){
		Iterator<Order> iter = partiallyFilledOrders.iterator();
		while(iter.hasNext()){
			Order o = iter.next();
			if(o.isFilled()){
				iter.remove();
				addToFilledOrders(o);
			}
		}
	}
}
