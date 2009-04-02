package common;

import java.io.Serializable;

public interface IOrder extends Serializable{
	
	/**
	 * Get this order's instrument name
	 * @return <code>this</code> order's instrument name
	 */
	String getInstrumentName();

	/**
	 * Get this order's price
	 * @return <code>this</code> order's price
	 */
	double getPrice();
	
	/**
	 * Get this order's orderID
	 * @return <code>this</code> order's orderID
	 */
	long getOrderID();
	
	/**
	 * Get this order's clientID
	 * @return <code>this</code> order's clientID
	 */
	int getClientID();
	
	/**
	 * Get this order's time of entry
	 * @return <code>this</code> order's submition time
	 */
	Long getEntryTime();
	
	/**
	 * Get this order's total quantity
	 * @return <code>this</code> order's total quantity
	 */
	long getQuantity();
	
	/**
	 * Get this order's open quantity
	 * @return <code>this</code> order's open quantity
	 */
	long getOpenQuantity();
	
	/**
	 * Get this order's executed (filled) quantity
	 * @return <code>this</code> order's executed (filled) quantity
	 */
	long getExecutedQuantity();
	
	/**
	 * Get this order's average executed price, i.e. an average price of all fills
	 * @return <code>this</code> order's average executed price
	 */
	double getAverageExecutedPrice();
	
	/**
	 * Get this order's last executed price, i.e. price at which it was last filled or partially filled
	 * @return <code>this</code> order's last executed price
	 */
	double getLastExecutedPrice();
	
	/**
	 * Get this order's last executed volume, i.e. volume that was last filled or partially filled
	 * @return <code>this</code> order's last executed volume
	 */
	long getLastExecutedVolume();
	
	/**
	 * Get number of trades (fills) that took place for this order
	 * @return a number of trades (fills) that were executed for this order
	 */
	int getNumberOfTrades();
	
	/**
	 * Print all trades (fills) that took place for this order
	 */
	void printTrades();
	
	/**
	 * Get textual description for this order
	 * @return textual description for this order
	 */
	String toString();
	
}
