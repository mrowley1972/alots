**Automated Limit Order Trading Simulator**

This project was a part of my final year at university and I wanted to share this software with everyone who might be interested. The idea behind this software is that it simulates an equities stock exchange/electronic crossing network - multiple clients can connect to the exchange and execute various trading strategies based on the behaviour of the limit order book, thus trading against each other. Client can submit buy and sell, limit and market orders, which will be matched by the engine according to the price-time priority. Apart from matching/crossing orders, the platform also offers extensive statistical data about any traded instrument - clients can trade many instruments simultaneously without any interference. Statistics include state of the order book, such as best ask and bid, price at any book depth, volumes at different price levels, average prices, VWAPs, etc. All of these methods can be found in _IExchangeSimulator_ remote interface.
Additionally ALOTS constantly updates it's clients with TAQ notifications and own-order notifications, thus giving virtual clients ability to change their strategy dynamically.

Similar project was undertaken by a group of students at the Pennsylvania University a few years ago in collaboration with Lehman Brothers proprietary desk traders, but I felt that it was not advanced enough for today's needs, hence I've implemented my own platform. This software is now used at the Financial Computation Group within Computer Science department of University College London.

The simulator is implemented in Java 6.0; virtual clients connect to the exchange via Java RMI, hence some work needs to be done to make this connection. Any client needs to implement _Notifiable_ remote interface, which the exchange uses to make notification callbacks. All the necessary interfaces can be found in the _common_ package.

Unfortunately, I do not have time to make lengthy descriptions about functionality of my software, but the code is commented fairly well and you can generate Java Doc to see API descriptions, which are fairly self-explanatory.

The code base is quite large, so please spend some time trying to understand APIs. I've licensed this program under MIT license, hence you can use this code in any manner possible, as long as you reference me in your projects. And please feel free to drop me a line with any suggestions - I'll try my best to respond as quickly as possible.

Enjoy!!!