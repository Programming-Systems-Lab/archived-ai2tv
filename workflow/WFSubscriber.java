package psl.ai2tv.workflow;

import psl.ai2tv.gauge.ClientDesc;
import psl.ai2tv.gauge.SimpleGaugeSubscriber;
import psl.ai2tv.SienaConstants;

import siena.*;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

class WFSubscriber extends SimpleGaugeSubscriber implements Runnable{

  private static final Logger logger = Logger.getLogger(WFSubscriber.class);

  private static final long REFRESH_DURATION = 1000; // check clients every 5 secs

  private WFGauge myGauge;
  private long _id;
  private boolean _isActive;

  public WFSubscriber(WFGauge wfg)
    throws SienaException, IOException {

    _id = System.currentTimeMillis(); // don't know why it was this: -1 * System.currentTimeMillis();
    myGauge = wfg;
    _isActive = false;
    setup();
  }

  /**
   * collect periodic stats to inform the WFSubscriber
   */
  public void run(){
    _isActive = true;
    while (_isActive){
      try {
	Thread.sleep(REFRESH_DURATION);
	checkClients();
      } catch (InterruptedException e){
	System.err.println("Exception in WFSubscriber periodic thread: " + e);
      }
    }
  }

  /**
   * loop through all clients and send out a probe to check each client status
   */
  // Set clientIDs;	// set of clients that we will check
  Collection clientIDs;	// set of clients that we will check
  private void checkClients(){
    // clientIDs = ((Hashtable)myGauge.getGroupClients()).keySet();
    clientIDs = ((Hashtable)myGauge.getGroupClients()).values();

    // for each client, get a new set of reports
    // we should
    // 1) check that the current times reported by
    // the clients are all syncrhonized
    // 2)
    System.out.println("checking clients, there is/are: " + clientIDs.size() + " client(s)");
    
    ClientDesc currentClient;
    for (Iterator i=clientIDs.iterator(); i.hasNext(); ){
      currentClient = (ClientDesc) i.next();
      getReport(currentClient);
    }
  }

  private void getReport(ClientDesc currentClient){
    Notification probe = new Notification();
    probe.putAttribute(SienaConstants.AI2TV_WF_UPDATE_REQUEST, "");
    probe.putAttribute(SienaConstants.CLIENT_ID, currentClient.getClientID());
    probe.putAttribute(SienaConstants.ABS_TIME_SENT, System.currentTimeMillis());
    System.out.println("dp2041: publishing report request: " + probe);
    try {
      mainSiena.publish(probe);
    } catch (siena.SienaException e){
      System.err.println("WFSubscriber publishing sienaException during report request: " + e);
    }
  }

  public boolean isActive(){
    return _isActive;
  }

  public void notify(Notification e) {
    handleNotification(e);
  }

  private void handleNotification(Notification e){
    ClientDesc currentClient;
    logger.debug("received " + e);

    // get the propagation delay
    AttributeValue absAttrib = e.getAttribute(SienaConstants.ABS_TIME_SENT);
    long absTimeSent = -1;
    long ppd = -1; // ppd: previous propagation delay
    if (absAttrib != null){
      // here we calculate the difference between when the request was
      // sent and when it was received/handled .  Note that this
      // difference includes some overhead of some attrib checking so
      // it is not entirely accurate
      absTimeSent = absAttrib.longValue();
      ppd = System.currentTimeMillis() - absTimeSent;
    }

    String id = String.valueOf(e.getAttribute(SienaConstants.CLIENT_ID).longValue());
    Hashtable ht = myGauge.getGroupClients();
    currentClient = (ClientDesc)ht.get(id);
    // this basically registers the client into the hash table, so we don't have to 
    // check for it later
    if (currentClient == null) {
      logger.debug("adding new client " + id);
      ht.put(id, currentClient = new ClientDesc(id));
      myGauge.getBucket().update(id, currentClient);
    }
    if (e.getAttribute(SienaConstants.AI2TV_WF_REG) != null){
      // don't do anything as we register above.  I catch
      // this here so that I don't get an error later.

    } else if (e.getAttribute(SienaConstants.AI2TV_FRAME) != null){
      long currentTime = myGauge.clock.currentTime();
      currentClient.setFrame(e.getAttribute(SienaConstants.LEFTBOUND).intValue(),
			     e.getAttribute(SienaConstants.MOMENT).intValue(),
			     e.getAttribute(SienaConstants.RIGHTBOUND).intValue(),
			     currentTime,
			     e.getAttribute(SienaConstants.LEVEL).intValue(),
			     e.getAttribute(SienaConstants.SIZE).intValue());

      // dp2041: need to implement
    } else if (e.getAttribute(SienaConstants.AI2TV_WF_UPDATE_REPLY) != null){
      System.out.println(" ---------------------------------------- ");
      System.out.println("WF just got an update reply from a client");
      System.out.println("WF -> client was: " + ppd);
      
      ppd = System.currentTimeMillis() 
	- e.getAttribute(SienaConstants.ABS_TIME_SENT).longValue();
      e.getAttribute(SienaConstants.ABS_TIME_SENT).longValue();
      System.out.println("client -> WF was: " + ppd);
      System.out.println(" ---------------------------------------- ");
      

    } else if (e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION) != null){
      String action = e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION).stringValue();
      if (action.equals(SienaConstants.PLAY)) {
	//check if clients are already running
	if (! myGauge.isRunning()) {
	  /* // nominal start time = 1st client start time	
	  long st = myGauge.clock.currentTime(); // System.currentTimeMillis();
	  myGauge.setStartTime(st);
	  currentClient.setStartTime(st);
	  */
	  logger.debug("starting nominal");
	  myGauge.startNominal();
	}
	WFGauge.clock.startTime(absTimeSent);

      } else if (action.equals(SienaConstants.PAUSE)) {
	WFGauge.clock.pauseTime(absTimeSent);

      } else if (action.equals(SienaConstants.STOP)) {
	WFGauge.clock.stopTime();

      } else if (action.equals(SienaConstants.GOTO)) {
	long clientid = e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION).longValue();
	// id of -1 is reserved for the WF
	if (clientid != _id){
	  WFGauge.clock.gotoTime(absTimeSent, e.getAttribute(SienaConstants.NEWTIME).intValue());
	}
      } else {
	// event that we don't know what action this is
	// should throw an unknown error, or something
	logger.debug("Received AI2TV VIDEO ACTION event: " + e);
      }
    } else {
      // event that we don't know what EVENT this is
      // should throw an unknown error, or something
      logger.debug("Received unknown event: " + e);
    }
  }
}
