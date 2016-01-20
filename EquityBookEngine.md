# Introduction #

This is the place to discuss any outstanding issues regarding the development of **EquityBookEngine**, implementing **BookEngine interface**. Please feel free to add any comments, thoughts or advice.

## Processing of orders ##

Orders only get matched when new orders are submitted. When new order is submitted, it is tried to be matched against outstanding orders in the opposing book. If it is partially matched, then it is reinserted into the order book. However, any orders that moved to the top of the book do not trigger any further matching, even though an order's price on the opposing book might've moved in its direction.

One suggestion is to have a separate thread that goes through the book and tries to match orders, while there are no new orders coming in, however, this may lead to race conditions with the thread that services incoming orders. Current implementation stays in place, until further solution is found.