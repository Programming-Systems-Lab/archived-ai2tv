package psl.ai2tv.gauge;

import java.util.Hashtable;

/**
	Base class for any gauge implementation 
	assessing the state of a group of clients that work together
*/
public class GroupGauge {

	/**
	   Constant defining the time interval (in ms.) for getting
	   group state snaphsots
	*/
	public static final long SAMPLE_INTERVAL = 1000;
	
	/**
		Data structure to hold and manipulate frame-by-frame info
	*/
	protected FrameIndexParser fip;
	
	/**
		file name to be parsed to fill the @see fip data structure
	*/
	protected String frameFileName;
	
	/**
		Time interval for taking group snapshots.
	*/
	protected long nomInterval;
	
	/**
		Whether this gauge is activated
	*/	
	protected boolean running = false;
	
	/**
		start time in probe time
	*/
	protected long startTime;
	
	/**
  		time since start
  	*/
  protected long progress=0;
  
   /**
  	frames nominally seen since start time (assuming nominal frame rate 30 fps)
  	NOTE: expresses a frame number
  */
  protected long nomProgress = 0;
  
  /**
  	Holds clients descriptors for the set of clients in this group
  */
  protected Hashtable groupClients = new Hashtable();

  /** Virtualizes a nominal client, that is, it's designed 
  	to keep track of what a nominal client - enjoying 30 fps - would progress 
  */  
  protected Thread nominal;
  
    /** data structure for taking snapshot of the state of the clients' group*/
  protected TimeBucket bucket;
  
  /** Communication facility of this gague, used primarily to listen for probe events */
  protected GaugeComm comm;
 
	public GroupGauge() {
 		nomInterval = SAMPLE_INTERVAL;
 		running = false;
 		frameFileName = null;	

 		//prepare time bucket for snapshots
 		bucket =  new TimeBucket();
 	}
 	
 	public Hashtable getGroupClients() { return groupClients; }
 	public void removeClient(String id) {groupClients.remove(id);}
	public TimeBucket getBucket() { return bucket; }
	public boolean isRunning() {return running; }
	public void startNominal() { nominal.start(); }
	public void setStartTime (long t) { startTime = t; }
	public long getStartTime() { return startTime; }
	public void setFrameFileName (String s) { frameFileName = s; }
	public String getFrameFileName() { return frameFileName; }
	public FrameIndexParser getFrameIndexParser() { return fip; }
	
	public void setup() {
		// take in all frame info if available
 		fip = setFrameInfo();
		try {
 			comm = setupCommunications();
 		} catch (CommunicationException ce) {
	 		comm = null;
	 		ce.printStackTrace();		
	 	}
 		nominal = defineNominalClient();
	 }

	/** sets up communication facilities: see @see comm
		Empty method - override in subclasess as needed
	*/
	protected GaugeComm setupCommunications() 
		throws CommunicationException {
		// to be overridden
		return (GaugeComm) null;
	}
	
	/** sets up the nominal client as a point of reference for the gauge.
		see @see nominal.
		Empty method - override in subclasess as needed
	*/
	protected Thread defineNominalClient() {
		// to be overridden by defining the application code for the Thread
		// use code below as a template
		return new Thread() {
			boolean cont = true;
			 
			public void run() {
				running = true;	
				while (cont) {
				// here add code to compute the progress of the group
				// as well as of the nominal client
				
				//take group snapshot at this moment
				evaluateStatus(progress);
				}
				running = false;			
			}	
		};
	}
	
	/**
		produce a snapshot of the client group
		@param elapsed the time the snapshot is taken (in probe time)
	*/
	protected void evaluateStatus(long elapsed) {
		fillBucket(elapsed);
		publishStatus();
		clearStatus();	
	}
	
	/** put fresh data into the bucket */
	protected void fillBucket(long elapsed) {
		// override to update the bucket
		
		bucket.setTime(elapsed);		
	}
	
	protected void publishStatus() {
		//override	
	}
	
	/**
  		clears the time bucket before starting data collection for next sample
  	*/
	protected final void clearStatus() {
		bucket.clearValues();
	}
	
	/**
		parses and returns the data structure about frame information
		stub implementation, override as needed
	*/
	protected FrameIndexParser setFrameInfo() {
		//assuming the frameFileName field is correctly set
		if (frameFileName != null)
			return new FrameIndexParser(frameFileName);
		else
			return (FrameIndexParser) null;
	}
}
