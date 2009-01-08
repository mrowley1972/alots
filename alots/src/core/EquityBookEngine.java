package core;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.AbstractQueue;

public class EquityBookEngine implements BookEngine {

	private List<Order> bidLimitOrders;
	private List<Order> askLimitOrders;
	private List<Order> filledOrders;
	private List<Order> partiallyFilledOrders;
	private Comparator<Order> comparator;
	private AbstractQueue<Order> updatedOrders;
	
	/*
	 * When BookEngine object is created, it gets access to all Instrument books 
	 */
	public EquityBookEngine(List<Order> bidLimitOrders, List<Order> askLimitOrders, List<Order> filledOrders, 
			List<Order> partiallyFilledOrders, AbstractQueue<Order> updatedOrders){
		
		this.bidLimitOrders = bidLimitOrders;
		this.askLimitOrders = askLimitOrders;
		this.filledOrders = filledOrders;
		this.partiallyFilledOrders = partiallyFilledOrders;
		comparator = new PriceTimePriorityComparator();
		this.updatedOrders = updatedOrders;
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
			if(order.type() == core.Order.Type.MARKET && askLimitOrders.size()>0)
				order.setPrice(askLimitOrders.get(0).getPrice());
			//Update instrument statistics
			instrument.updateBidVWAP(order.getQuantity(), order.getPrice());
			
			
			//Try to match immediately
			matchBuyOrder(order);
			
			if(order.isFilled())
				addToFilledOrders(order);
			else
				insertBuyOrder(order);
		}
		else{
			if(order.type() == core.Order.Type.MARKET && bidLimitOrders.size()>0)
				order.setPrice(bidLimitOrders.get(0).getPrice());
			//Update instrument statistics
			instrument.updateAskVWAP(order.getQuantity(), order.getPrice());
			
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

	public void insertBuyOrder(Order order){
		bidLimitOrders.add(order);
		order.getInstrument().updateBidVolume(order.getOpenQuantity());
		Collections.sort(bidLimitOrders, comparator);
	}
	
	public void insertSellOrder(Order order){
		askLimitOrders.add(order);
		order.getInstrument().updateAskVolume(order.getOpenQuantity());
		Collections.sort(askLimitOrders, comparator);
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
				
				}
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
				} else {
					break;
				}	
			}
		}
	}

	private void addToPartiallyFilledOrders(Order order){
		
		if(partiallyFilledOrders.contains(order)){
			if(order.isFilled()){
				partiallyFilledOrders.remove(order);
				addToFilledOrders(order);
			}
		}
		else{
				if(!order.isFilled())
					partiallyFilledOrders.add(order);
				else
					addToFilledOrders(order);
			}
	}
	
	//clean up partiallyFilledOrders by eliminating already filled orders, and moving
	//them into filledOrders
	private void cleanUpPartiallyFilledOrders(){
		Iterator<Order> iter = partiallyFilledOrders.iterator();
		while(iter.hasNext()){
			Order o = iter.next();
			if(o.isFilled()){
				iter.remove();
				//filledOrders.add(o);
				addToFilledOrders(o);
			}
		}
	}
	
	//add to filled orders by removing an object with the same reference
	//and adding the same object, but with updated internal state
	private void addToFilledOrders(Order order){
		if(!filledOrders.contains(order))
			filledOrders.add(order);
	}
}
