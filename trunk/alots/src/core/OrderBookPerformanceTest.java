package core;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import core.Instrument;
import core.Order;
import core.TAQNotification;



public class OrderBookPerformanceTest {

	
	public static void main(String[] args) {

		BlockingQueue<Order> queue = new LinkedBlockingQueue<Order>();

		BlockingQueue<TAQNotification> notifications = new LinkedBlockingQueue<TAQNotification>();
		Instrument instrument = new Instrument("MSFT", queue, notifications);
		
		// simulation step

		int round = 100000;
		int printround = 1000;

		Random rand = new Random();

		double minPrice = 100;
		double maxPrice = 200;
		double ticksize = 0.01;
		double phi = 5;
		int id = 1;
		int skip = 0;

		long start = System.currentTimeMillis();
		long time = start;

		// generate stub price around the middle

		int ts = 10;

		double mprice = (maxPrice + minPrice)/2;

		double bprice = ((int)(100*mprice))/100.0;

		double sprice = bprice;

		double tsize  = ((int)(100*ticksize*ts))/100.0;

		//Insert BUY orders
		for (int t=10;t<1000;t+=ts) {

			sprice = sprice + tsize; sprice = ((int)sprice*100)/100.0;

			bprice = bprice - tsize; bprice = ((int)bprice*100)/100.0;

			Order border = new Order(id++,instrument,Order.Side.BUY,core.Order.Type.LIMIT, 1, sprice);
			
			
			instrument.processNewOrder(border);

			Order sorder = new Order(id++,instrument,Order.Side.SELL,core.Order.Type.LIMIT, 1, bprice);

			instrument.processNewOrder(sorder);			

		}

			

		// generate order

		for (int r=0;r<round;++r) {
			if ((r%printround) ==0) {
				
				long ntime = System.currentTimeMillis();
				
				System.out.println(r + ":" + instrument.getLastPrice()  

									 + "," + instrument.getBuyVolume()  

									 + "," + instrument.getSellVolume()

									 + "," + instrument.getBidVolume()  

									 + "," + instrument.getAskVolume()  

									 + "," + skip

									 + " use " + (ntime-time) + " milisecond,"

									 + " " + ((ntime-time)*1.0)/printround + " per order");

				time = System.currentTimeMillis();

			}

			int tick = -1 - (int) Math.floor(phi*Math.log(rand.nextDouble()));

			double price;

			Order.Side side;

			if (rand.nextDouble() < 0.5) {

				side  = Order.Side.BUY;

				price = instrument.getBestAsk() - tick*ticksize;

				if (price >= maxPrice) { skip++; continue; }

			} else {

				side = Order.Side.SELL;

				price = instrument.getBestBid() + tick*ticksize;

				if (price <= minPrice) { skip++; continue; }

			}

			price = ((int)(price * 100))/100.0;

			if (price < minPrice) price = minPrice;

			if (price > maxPrice) price = maxPrice;

			Order order = new Order(id++,instrument,side,core.Order.Type.LIMIT, 1, price);
			instrument.processNewOrder(order);

		}

		time = System.currentTimeMillis();

		System.out.println("Use " + (time - start) + " miliseconds.");

	}	

	

}

