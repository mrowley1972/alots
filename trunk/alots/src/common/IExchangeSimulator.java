package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IExchangeSimulator extends Remote {
	
	/**
	 * Register with the exchange to start submitting orders
	 * @param client	<code>this</code> client implementing <code>Notifiable</code> interface - needs to be a remote stub
	 * @return clientID	clientID to enable submitting orders
	 * @throws RemoteException
	 */
	int register(Notifiable client) throws RemoteException;
	
	/**
	 * Check whether the exchange is open
	 * @return true if the exchange is operating, false otherwise
	 */
	boolean isOpen() throws RemoteException;
	
	/**
	 * Create an instrument to be traded on the exchange. If the instrument is already being traded, new instrument is not created.
	 * @param tickerSymbol a correct ticker symbol for this instrument
	 * @return void
	 */
	void registerInstrument(String tickerSymbol) throws RemoteException;
	
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
	long submitOrder(String tickerSymbol, int clientID, String side, String type, double price, long quantity)
		throws RemoteException;
	
	/**
	 * Cancel an existing order, belonging to this client.
	 * 
	 * @param 	clientID  valid client's own id, assigned by the StockExchange during connection
	 * @param 	orderID   one of orderIDs that this client has for own orders
	 * @return 	an order object that was requested to be cancelled, <code>null</code> if the order does not exist, does not belong
	 *  to this client or has already been filled (specific reason is hard to trace back).
	 * @exception MarketsClosedException if the market is not currently opened
	 */
	IOrder cancelOrder(int clientID, long orderID) throws RemoteException;
	
	/**
	 * Get an order with <code>orderID</code> and belonging to a client with <code>clientID</code>
	 * @param clientID	valid client's own id, assigned by the StockExchange during connection
	 * @param orderID	one of orderIDs that this client has for own orders
	 * @return	an order with <code>orderID</code>, or <code>null</code> if an order does not belong to a client
	 *  with <code>clientID</code>
	 */
	public IOrder getClientOrder(int clientID, long orderID) throws RemoteException;
	
	/**
	 * Get latest bid order book for an instrument
	 * @param tickerSymbol ticker symbol of a traded instrument
	 * @return current bid order book for this instrument
	 * @exception IllegalArgumentException if instrument's ticker symbol is incorrect
	 */
	
	double getInstrumentLastPrice(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get the total volume of instrument's bid order book
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid order book volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	long getInstrumentBidVolume(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get the total volume of instrument's bid order book
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid order book volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	long getInstrumentAskVolume(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's buy volume
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's buy volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	long getInstrumentBuyVolume(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's buy volume
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's buy volume
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	long getInstrumentSellVolume(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's average buy price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average buy price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentAverageBuyPrice(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's average buy price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average buy price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentAverageSellPrice(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's average buy price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's average buy price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentBidVWAP(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's ask volume-weighted average price (VWAP)
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask VWAP
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentAskVWAP(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's best bid price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's best bid price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentBestBid(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's best ask price
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's best ask price
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentBestAsk(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's bid price at specified <code>depth</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param depth			required depth
	 * @return instrument's bid price at <code>depth</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentBidPriceAtDepth(String tickerSymbol, int depth) throws RemoteException;
	
	/**
	 * Get instrument's ask price at specified <code>depth</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param depth			required depth
	 * @return instrument's ask price at <code>depth</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentAskPriceAtDepth(String tickerSymbol, int depth) throws RemoteException;
	
	/**
	 * Get instrument's ask volume at specified <code>price</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param price			required price
	 * @return instrument's ask volume at <code>price</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	long getInstrumentAskVolumeAtPrice(String tickerSymbol, double price) throws RemoteException;
	
	/**
	 * Get instrument's bid volume at specified <code>price</code>
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @param price			required price
	 * @return instrument's bid volume at <code>price</code>
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	public long getInstrumentBidVolumeAtPrice(String tickerSymbol, double price) throws RemoteException;
	
	/**
	 * Get instrument's daily bid high
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentBidHigh(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's daily bid low
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's bid low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentBidLow(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's daily ask high
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask high for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentAskHigh(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get instrument's daily ask low
	 * @param tickerSymbol	a valid ticker symbol of a currently traded instrument
	 * @return instrument's ask low for the whole time of exchange's operation
	 * @exception IllegalArgumentException if invalid ticker symbol is passed
	 */
	double getInstrumentAskLow(String tickerSymbol) throws RemoteException;
	
	/**
	 * Get a list of currently traded instruments.
	 * @return a list of currently traded instruments, <code>null</code> if there are no instruments being traded.
	 */
	List<String> getTradedInstrumentsList() throws RemoteException;
	
}
