/**
 * A sample buyer client to test out exchange performance
 */

package client;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import common.IExchangeSimulator;
import common.Notifiable;
import core.Order.Side;

public class ClientBuyer implements Notifiable{
	
	private int clientID;

	public int getClientID() throws RemoteException {
		return clientID;
	}
	
	public void setClientID(int clientID){
		this.clientID = clientID;
	}

	public void notifyOrder(long orderID, double price, double quantity)
			throws RemoteException {
		System.out.println("Order notification: " + orderID + "; average executed price: " + price +
				"; executed quantity: " + quantity);
	}

	public void notifyTrade(String ticker, long time, Side side, double price,
			double quantity) throws RemoteException {
		System.out.println("Instrument notification: " + ticker + "; time: " + new Date(time) + "; side: " + side + 
				"; price: " + price + "; quantity: " + quantity);
	}
	
public static void main(String args[]){
		
		if(args.length < 2){
			System.out.println("Usage: Client <hostname> <rmi_port>");
			System.exit(1);
		}
		
		String host = args[0];
		int rmiPort = Integer.parseInt(args[1]);
		ClientBuyer clientBuyer = new ClientBuyer();
	
		if(System.getSecurityManager() == null){
			System.setSecurityManager(new SecurityManager());
		}
		
		try{
			//This part is for the exchange to be able to issue order notifications to clients
			Notifiable stub = (Notifiable)UnicastRemoteObject.exportObject(clientBuyer, 0);
			
			//Locate registry on specific port
			Registry registry = LocateRegistry.getRegistry(host, rmiPort);
			
			String name = "ExchangeSimulator";
			IExchangeSimulator exchange = (IExchangeSimulator)registry.lookup(name);
			
			//Initial stage is to register at the exchange
			clientBuyer.setClientID(exchange.register(stub));
			System.out.println("Assigned clientID is " + clientBuyer.getClientID());
			
			//Register an instrument to be traded
			String msft = "MSFT";
			
			exchange.subscribeToInstrument(stub, msft);
			System.out.println("Subscribed to " + msft);
			
			//Issue some buy orders
			System.out.println("Issuing buy orders...");
			System.out.println("Order: " + exchange.submitOrder(msft, clientBuyer.getClientID(), "buy", "market", 0.0, 600));
			System.out.println("Order: " + exchange.submitOrder(msft, clientBuyer.getClientID(), "buy", "limit", 22.8, 1000));
			System.out.println("Order: " + exchange.submitOrder(msft, clientBuyer.getClientID(), "buy", "limit", 21.5, 700));
			System.out.println("Order: " + exchange.submitOrder(msft, clientBuyer.getClientID(), "buy", "limit", 21.8, 900));
			
			Thread.sleep(1000);
			
			System.out.println("Best bid: " + exchange.getInstrumentBestBid(msft));	
			System.out.println("Bid volume: " + exchange.getInstrumentBidVolume(msft));
			System.out.println("Bid price at depth 0: " + exchange.getInstrumentBidPriceAtDepth(msft, 0));
			System.out.println("Bid price at depth 1: " + exchange.getInstrumentBidPriceAtDepth(msft, 1));
			
			Thread.sleep(1000);
			
			System.out.println("Best ask: " + exchange.getInstrumentBestAsk(msft));
			System.out.println("Last price: " + exchange.getInstrumentLastPrice(msft));
			System.out.println("Ask volume: " + exchange.getInstrumentAskVolume(msft));
			System.out.println("Buy volume: " + exchange.getInstrumentBuyVolume(msft));
			System.out.println("Sell volume: " + exchange.getInstrumentSellVolume(msft));
			System.out.println("Average buy price: " + exchange.getInstrumentAverageBuyPrice(msft));
			System.out.println("Average sell price: " + exchange.getInstrumentAverageSellPrice(msft));
			
			System.out.println("Ask price at depth 0: " + exchange.getInstrumentAskPriceAtDepth(msft, 0));
		}
		catch(Exception e){
			System.out.println("Client Exception:");
			e.printStackTrace();
		}
	}

}
