package core;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class EquityBookEngine implements BookEngine {

	private static final long serialVersionUID = -6028495077079186305L;
	
	private List<Order> bidLimitOrders;
	private List<Order> askLimitOrders;
	private List<Order> filledOrders;
	private List<Order> partiallyFilledOrders;
	private BlockingQueue<Order> updatedOrders;
	private BlockingQueue<TAQNotification> notifications;
	
	/*
	 * When BookEngine object is created, it gets access to all Instrument books 
	 */
	public EquityBookEngine(List<Order> bidLimitOrders, List<Order> askLimitOrders, List<Order> filledOrders, 
			List<Order> partiallyFilledOrders, BlockingQueue<Order> updatedOrders, 
			BlockingQueue<TAQNotification> notifications){
		
		this.bidLimitOrders = bidLimitOrders;
		this.askLimitOrders = askLimitOrders;
		this.filledOrders = filledOrders;
		this.partiallyFilledOrders = partiallyFilledOrders;
		this.updatedOrders = updatedOrders;
		this.notifications = notifications;
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
					updatedOrders.add(o);
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
					updatedOrders.add(o);
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
			if(order.type() == core.Order.Type.MARKET && askLimitOrders.size()>0)
				order.setPrice(askLimitOrders.get(0).getPrice());
			
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
			if(order.type() == core.Order.Type.MARKET && bidLimitOrders.size()>0)
				order.setPrice(bidLimitOrders.get(0).getPrice());
			
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

	//currently takes O(n) for the insertion in the worst case - need to do in log(n)
	
	public void insertBuyOrder(Order order){
		order.getInstrument().updateBidVolume(order.getOpenQuantity());
		
		int i = findIndex(order);	
		bidLimitOrders.add(i, order);
		
			/*
			int i;
			double price = order.getPrice();
			Long entryTime = order.getEntryTime();
			
			for(i=0; i<bidLimitOrders.size(); i++){
				Order curOrder = bidLimitOrders.get(i);
			
				if((curOrder.getPrice() < price) || (curOrder.getPrice()==price && 
						((Long)curOrder.getEntryTime()).compareTo(entryTime)>0)){
					bidLimitOrders.add(i, order);
					break;
				}
			}
			if(i == (bidLimitOrders.size()))
				bidLimitOrders.add(order);
	*/
	}
	
	
	
	public void insertSellOrder(Order order){
		order.getInstrument().updateAskVolume(order.getOpenQuantity());
		int i = findIndex(order);
		askLimitOrders.add(i, order);
		
		/*
		int i;
		double price = order.getPrice();
		Long entryTime = order.getEntryTime();
		
		for(i=0; i<askLimitOrders.size(); i++){
			Order curOrder = askLimitOrders.get(i);
		
			if((curOrder.getPrice() > price) || (curOrder.getPrice()==price && 
					((Long)curOrder.getEntryTime()).compareTo(entryTime)>0)){
				askLimitOrders.add(i, order);
				break;
			}
		}
		if(i == (askLimitOrders.size()))
			askLimitOrders.add(order);
			
		*/
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
					
					addToPartiallyFilledOrders(order);
					
					if(curOrder.isFilled()){
						iter.remove();
						addToFilledOrders(curOrder);
					}
					else{
						addToPartiallyFilledOrders(curOrder);
					}
					
					//update instrument statistics
					instrument.updateLastPrice(price);
					instrument.updateBidVolume(-quantity);
					instrument.updateSellVolume(quantity);
					instrument.updateAveragePrice(quantity, price);
					instrument.updateAverageSellPrice(quantity, price);
					 
					//put into pushing queue for client notifications of both orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
					
					//create new TAQNotification object about this trade
					TAQNotification notification = new TAQNotification(TAQNotification.Type.TRADE, instrument.getTickerSymbol(), 
							System.currentTimeMillis(), price, quantity, Order.Side.SELL);
					notifications.add(notification);
					System.out.println("Notification added: " + notification);
				
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
					
					if(curOrder.isFilled()){
						iter.remove();
						addToFilledOrders(curOrder);
					}else{
						addToPartiallyFilledOrders(curOrder);
					}
					addToPartiallyFilledOrders(order);
					
					//update instrument history
					instrument.updateLastPrice(price);
					instrument.updateAskVolume(-quantity);
					instrument.updateBuyVolume(quantity);
					instrument.updateAveragePrice(quantity, price);
					instrument.updateAverageBuyPrice(quantity, price);
					
					//put into pushing queue for client notifications of both orders
					updatedOrders.add(order); updatedOrders.add(curOrder);
					
					//create new TAQNotification about this trade
					TAQNotification notification = new TAQNotification(TAQNotification.Type.TRADE, instrument.getTickerSymbol(), 
							System.currentTimeMillis(), price, quantity, Order.Side.BUY);
					notifications.add(notification);
					
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
	
	//add to filled orders by removing an object with the same reference
	//and adding the same object, but with updated internal state
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
