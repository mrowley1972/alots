/**
 * A simple client, acting as a dealer to test out exchange performance
 */

package client;


import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.List;
import common.Notifiable;
import common.IExchangeSimulator;
import common.IOrder;


public class Client implements Notifiable{
	
	private int clientID;
	
	public int getClientID(){
		return clientID;
	}
	
	public void setClientID(int clientID){
		this.clientID = clientID;
	}
	
	public Client(){
		super();
	}

	//This method needs to have functionality for accepting updates about orders
	
	public void notifyOrder(long orderID, double price, double quantity, String status){
		System.out.println("Order notification: " + orderID + "; average executed price: " + price +
				"; executed quantity: " + quantity + "; status: " + status);
	}
	
	public void notifyTrade(String ticker, long time, String side, double price, double quantity){
		System.out.println("Instrument trade notification: " + ticker + "; time: " + new Date(time) + "; side: " + side + 
				"; price: " + price + "; quantity: " + quantity);
	}
	
	public void notifyQuote(String ticker, long time, double bidPrice, double askPrice){
		System.out.println("Instrument quote notification: " + ticker + "; time: " + new Date(time) + "; Bid Price: " +
				bidPrice + "; Ask Price: " + askPrice);
	}
	
	public void printBidBook(List<IOrder> bidLimitOrders){
		System.out.println("***BID BOOK***");
		for(IOrder order: bidLimitOrders){
			System.out.println(order.toString());
		}
	}
	
	public void printAskBook(List<IOrder> askLimitOrders){
		System.out.println("***ASK BOOK***");
		for(IOrder order: askLimitOrders){
			order.toString();
		}
	}
	

	public static void main(String args[]){
		
		if(args.length < 2){
			System.out.println("Usage: Client <hostname> <rmi_port>");
			System.exit(1);
		}
		
		String host = args[0];
		int rmiPort = Integer.parseInt(args[1]);
		Client client = new Client();
	
		if(System.getSecurityManager() == null){
			System.setSecurityManager(new SecurityManager());
		}
		
		try{
			//This part is for the exchange to be able to issue order notifications to clients
			Notifiable stub = (Notifiable)UnicastRemoteObject.exportObject(client, 0);
			//Locate registry on specific port
			Registry registry = LocateRegistry.getRegistry(host, rmiPort);
			String name = "ExchangeSimulator";
			IExchangeSimulator exchange = (IExchangeSimulator)registry.lookup(name);
			
			//Initial stage is to register at the exchange
			client.setClientID(exchange.register(stub));
			System.out.println("Assigned clientID is " + client.getClientID());
			
			//Register an instrument to be traded
			String msft = "MSFT";
			exchange.registerInstrument(msft);
			exchange.subscribeToInstrument(stub, msft);
			System.out.println("Subscribed to " + msft);
			
			System.out.println("Traded instruments " + exchange.getTradedInstrumentsList().toString());
			
			//Issue some buy orders
			System.out.println("Issuing buy orders...");
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "buy", "market", 20.8, 500));
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "buy", "limit", 20.8, 500));
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "buy", "limit", 21.8, 1000));
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "buy", "limit", 20.5, 500));
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "buy", "limit", 20.8, 500));
			
			Thread.sleep(1000);
			
			System.out.println("Best bid: " + exchange.getInstrumentBestBid(msft));	
			System.out.println("Bid volume: " + exchange.getInstrumentBidVolume(msft));
			System.out.println("Bid price at depth 0: " + exchange.getInstrumentBidPriceAtDepth(msft, 0));
			System.out.println("Bid price at depth 1: " + exchange.getInstrumentBidPriceAtDepth(msft, 1));
			
			//Issue some sell orders
			System.out.println("Issuing sell orders...");
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "sell", "limit", 20.0, 1000));
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "sell", "limit", 22.5, 2000));
			System.out.println("Order: " + exchange.submitOrder(msft, client.getClientID(), "sell", "market", 0.0, 200));
			
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
