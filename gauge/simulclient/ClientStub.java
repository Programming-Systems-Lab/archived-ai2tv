package psl.ai2tv.gauge.simulclient;

import siena.*;

import psl.ai2tv.gauge.FrameDesc;

/**
	Simulation of an AI2TV client
*/ 
public class ClientStub 
	implements Runnable {

	/* data shared by all classes in package */
	/** start of operation  */
	static long startTime;
	/** Frame currently being shown */
	static FrameDesc currFrame;
	/**  number of probe events per sec. */
	final long probeRate = 1000 / 3; 
	/** Number of frames to be displayed per second.
		Regulates the performance of the @see CacheController
		which hands over the frames to the client	
	 */
	private double framerate;
	
	/** time passed from start time */
	private long elapsedTime = 0;
	
	/** continuation flag for the infinite Thread loop */
	private boolean loopAlways = true;
	
	/** flag to activate / deactivate self-adaptation of this client, according to
	 	a hardcoded scheme (for testing)
	  */
	private boolean adaptive = true;
	/** should be a unique ID among the clients in the same group */
	private String clientID;
	
	private CacheController myCache;
	private ClientProbe myProbe;
	private Siena mySiena;
	
	public ClientStub (String frameFile, double rate, String ID) {
		framerate = rate;
		startTime = 0;
		clientID = ID;
		myCache = new CacheController(frameFile, framerate);
		myProbe = new ClientProbe(ID);	
		mySiena = myProbe.mySiena;
	}
	 
	public void run() {
		//set up probe
		myProbe.setTarget(myCache);
		myProbe.setProbingFrequency(probeRate);
		Thread probeThread = new Thread(myProbe);

		Notification startEvent = new Notification();
		startEvent.putAttribute("Start", (startTime = System.currentTimeMillis()));
		startEvent.putAttribute("ClientID", clientID);

		try { 
			mySiena.publish(startEvent);
			//run the probe
			probeThread.start();
		} catch (SienaException se) {
			se.printStackTrace();
			loopAlways = false;	
		}
		
		while (loopAlways) {
			getNextFrame();
			if (currFrame != null) {
				display();
				
				if (adaptive) {
					selfAdapt();	
				}					
			}
			else
				stopAtEnd();
		}
		myProbe.setActive(false);
	}
	
	/** self-adaptation scheme for testing purposes */
	private void selfAdapt() {
		if (elapsedTime > currFrame.getEnd())
			myCache.hierarchyDown(elapsedTime);
		else if (elapsedTime < currFrame.getStart())
			myCache.hierarchyUp(elapsedTime);	
	}
	
	/** show the current frame */
	private void display() {
		// for now in this stub it's just a printout
		//myCache.notify(currFrame);		
		System.out.print("--- ");
		System.out.print("Displaying: [ " + currFrame.getStart() +
			" - " + currFrame.getNum() + " - " + currFrame.getEnd() + " ] " +
			" at time " + (elapsedTime = System.currentTimeMillis() - startTime) +
			" ( " +  elapsedTime / 1000 * 30 + " ) " );
		System.out.println ( " ---");

	}
	
	private void stopAtEnd() {
		System.out.println ("*** At end of video ***");
		loopAlways = false;
	}
	
	private void getNextFrame() {
		currFrame = myCache.getNextFrame();
	}
	
	/**
		Arguments:
		args[0] = frame index file
		args[1] = frame rate
		args[2] = Client ID
		args[3] = self-adaptive (boolean flag)
	*/
	public static void main (String[] args) {
		if (args.length < 3 && args.length > 4) {
			usage();
			System.exit(0);
		}
		ClientStub myself = new ClientStub(args[0], Double.parseDouble(args[1]), args[2]);
		if (args.length == 4)
			myself.adaptive = Boolean.valueOf(args[3]).booleanValue();
		Thread mainThread = new Thread(myself);
		mainThread.start();
	}
	
	private static void usage() {
		System.out.println ("Usage: java ClientStub "  + " <Frame file> <Frame rate> <name> [adaptive flag]");
	}
}