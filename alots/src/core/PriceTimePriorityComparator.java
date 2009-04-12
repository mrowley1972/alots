/**
 * This class specifies order in the book for comparing buy and sell orders.
 * The class is used during insertion stage and returns different comparison order for buy orders and sell orders, 
 * for descending and ascending insertion respectively.
 * 
 * @author Asset Tarabayev
 */

package core;
import java.util.*;

public class PriceTimePriorityComparator implements Comparator<Order> {

	public int compare(Order order1, Order order2){
		
		//For a buy order, the highest price at the top position
		if(order1.side() == core.Order.Side.BUY)
			return compareOrder(order1, order2, -1);
		//For a sell order, the lowest price at the top position
		return compareOrder(order1, order2, 1);
		
	}
	
	private int compareOrder(Order o1, Order o2, int sortingOrder){
		//compares current order price with another order price
		int priceComp = ((Double)o1.getPrice()).compareTo((Double)o2.getPrice());
		
		//If both prices are equal, we need to sort according to their entry time
		if(priceComp == 0){
			
			int timeComp =((Long)o2.getEntryTime()).compareTo((Long)o1.getEntryTime());
			
			priceComp = timeComp;
		}
		/*
		 * since the sorting order for the buy and sell order books is different
		 * we need to ensure that orders are sorted correctly.
		 * buy order book - highest buy price at the top position
		 * sell order book - lowest sell price at the top position
		 * sortingOrder will helps to do this ranking
		 * a value of -1 sorts orders in descending order of price and ascending order of time
		 * a value of 1 sorts orders in ascending order of price and ascending order of time
		 */
		
		return priceComp*sortingOrder;
		
	}
}
