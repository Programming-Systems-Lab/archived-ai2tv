package psl.ai2tv.gauge;

public class CommunicationException 
	extends Exception {

	Exception embeddedExc;

	public CommunicationException() {
		super();
	}	
	
	public CommunicationException (Exception e) {
		this();
		embeddedExc = e;
	}
}