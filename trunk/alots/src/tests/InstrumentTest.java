package tests;
import core.*;
import org.testng.annotations.*;
import org.testng.Assert;

public class InstrumentTest {

	Instrument instrument;
	
	@BeforeClass
	public void setUp(){
		instrument = new Instrument("GOOG");
	}
	
	@Test
	public void testInstrumentName(){
		Assert.assertEquals(instrument.getTickerSymbol(), "GOOG");
	}
}
