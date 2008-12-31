package core;

@SuppressWarnings("serial")
public class MarketsClosedException extends RuntimeException {

	public MarketsClosedException(){
		super();
	}
	public MarketsClosedException(String args){
		super(args);
	}
}
