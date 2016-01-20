# Introduction #

This is the place to discuss any outstanding issues regarding the development of Instrument. Please feel free to add any comments, thoughts or advice.

## Getter methods ##

Instrument class has many getter methods for various properties, including:

  * getLastTradedPrice()
  * getBidVolume()
  * getAskVolume()
  * getBuyVolume()
  * getSellVolume()
  * getAverageBuyPrice()
  * getAverageSellPrice()
  * getBidVWAP
  * getAskVWAP

My recommendation is for these methods to calculate all of these values on the fly, instead of returning instance variables state (Please refer to the latest committed code base). This way, none of these values have to be set on every trade of the instrument, but are only computed as necessary. Also, only latest values are returned in this manner.
Additionally, since these methods are going to be calculating values, and many clients could be querying them, they need to be synchronised.