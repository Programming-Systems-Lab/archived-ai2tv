package psl.ai2tv.workflow;

import psl.ai2tv.gauge.ClientDesc;
import psl.ai2tv.gauge.SimpleGaugeSubscriber;
import psl.ai2tv.SienaConstants;

import siena.*;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

/**
 *
 * NOTE, to see what we're subscribing to, see psl/ai2tv/gauge/GroupGauge.java
 */
class WFSubscriber extends SimpleGaugeSubscriber implements Runnable{


  private static final Logger logger = Logger.getLogger(WFSubscriber.class);

  private static final long REFRESH_DURATION = 5000; // check clients every 5 secs

  static WFGauge myGauge;
  private long _id;
  private boolean _isActive;

  public WFSubscriber(WFGauge wfg)
    throws SienaException, IOException {

    _id = System.currentTimeMillis();
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
   * loop through all clients and send out a probe to check each client's status
   */
  Collection clientIDs;	// set of clients that we will check
  private void checkClients(){
    clientIDs = ((Hashtable)myGauge.getGroupClients()).values();
    if (clientIDs.size() == 0) {
      return;
    }

    ClientDesc currentClient;
    for (Iterator i=clientIDs.iterator(); i.hasNext(); ){
      currentClient = (ClientDesc) i.next();
      getReport(currentClient);
    }
  }

  private void getReport(ClientDesc currentClient){
    Notification probe = new Notification();
    probe.putAttribute(SienaConstants.AI2TV_WF_UPDATE_REQUEST, "");
    String[] id = currentClient.getClientID().split("@");
    probe.putAttribute(SienaConstants.UID, id[0]);
    probe.putAttribute(SienaConstants.GID, id[1]);
    probe.putAttribute(SienaConstants.ABS_TIME_SENT, System.currentTimeMillis());
    try {
      siena.publish(probe);
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
    long now = System.currentTimeMillis();
    logger.debug("received " + e);


    // get the propagation delay
    AttributeValue absAttrib = e.getAttribute(SienaConstants.ABS_TIME_SENT);
    long absTimeSent = -1;
    long ppd = -1; // ppd: previous propagation delay
    if (absAttrib != null){
      // here we calculate the difference between when the request was
      // sent and when it was received/handled.
      absTimeSent = absAttrib.longValue();
      ppd = now - absTimeSent;
    }

    String uid = e.getAttribute(SienaConstants.UID).stringValue();
    String gid = e.getAttribute(SienaConstants.GID).stringValue();
    String id = uid + "@" + gid;
    Hashtable ht = myGauge.getGroupClients();
    ClientDesc currentClient = (ClientDesc)ht.get(id);
    // this basically registers the client into the hash table, so we don't have to 
    // check for it later

    if (currentClient == null) {
      currentClient = new ClientDesc(id);
      ht.put(id, currentClient);
      myGauge.getBucket().update(id, currentClient);
    }

    currentClient.setPenalties(0); // reset the penalties on each report

    if (e.getAttribute(SienaConstants.AI2TV_WF_REG) != null){
      // don't do anything as we register above.  I catch
      // this here so that I don't get an error later.

    } else if (e.getAttribute(SienaConstants.AI2TV_FRAME) != null){
      if (e.getAttribute(SienaConstants.AI2TV_FRAME_MISSED) != null)
	currentClient.setPenalties(1);
      else if (e.getAttribute(SienaConstants.MOMENT) != null)
	currentClient.setFrame(e.getAttribute(SienaConstants.LEFTBOUND).intValue(),
			       e.getAttribute(SienaConstants.MOMENT).intValue(),
			       e.getAttribute(SienaConstants.RIGHTBOUND).intValue(),
			       e.getAttribute(SienaConstants.SIZE).intValue(),
			       e.getAttribute(SienaConstants.LEVEL).intValue(),
			       e.getAttribute(SienaConstants.TIME_SHOWN).longValue(),
			       e.getAttribute(SienaConstants.TIME_OFFSET).intValue(),
			       e.getAttribute(SienaConstants.TIME_DOWNLOADED).longValue());
      getClientInfo(e, currentClient);
			       
      if (e.getAttribute(SienaConstants.PREFETCHED_FRAMES) != null)
	currentClient.setPrefetchedFrames(e.getAttribute(SienaConstants.PREFETCHED_FRAMES).intValue());

    } else if (e.getAttribute(SienaConstants.AI2TV_WF_UPDATE_REPLY) != null){
      currentClient.addDistClient2WF(ppd);
      // logger.debug("client -> WF avg is: " + 
      // currentClient.getAvgDistClient2WF() + 
      // " +/- " + currentClient.getStddevDistClient2WF());

      currentClient.addDistWF2Client(e.getAttribute(SienaConstants.PREV_PROP_DELAY).longValue());
      if (e.getAttribute(SienaConstants.MOMENT) != null)
	currentClient.setFrame(e.getAttribute(SienaConstants.LEFTBOUND).intValue(),
			       e.getAttribute(SienaConstants.MOMENT).intValue(),
			       e.getAttribute(SienaConstants.RIGHTBOUND).intValue(),
			       e.getAttribute(SienaConstants.SIZE).intValue(),
			       e.getAttribute(SienaConstants.LEVEL).intValue(),
			       e.getAttribute(SienaConstants.TIME_SHOWN).longValue(),
			       e.getAttribute(SienaConstants.TIME_OFFSET).intValue(),
			       e.getAttribute(SienaConstants.TIME_DOWNLOADED).longValue());
      getClientInfo(e, currentClient);
      // logger.debug("WF -> client avg is: " + 
      // currentClient.getAvgDistWF2Client() + 
      // " +/- " + currentClient.getStddevDistWF2Client());
      
    } else if (e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION) != null){
      String action = e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION).stringValue();
      if (action.equals(SienaConstants.PLAY)) {
	//check if clients are already running
	if (! myGauge.isRunning()) {
	  // nominal start time = 1st client start time	
	 long st = myGauge.clock.currentTime(); // System.currentTimeMillis();
	 myGauge.setStartTime(st);
	 currentClient.setStartTime(st);
	 logger.debug("starting nominal");
	  myGauge.startNominal();
	}
	WFGauge.clock.startTime(absTimeSent);

      } else if (action.equals(SienaConstants.PAUSE)) {
	WFGauge.clock.pauseTime(absTimeSent);

      } else if (action.equals(SienaConstants.STOP)) {
	WFGauge.clock.stopTime();

      } else if (action.equals(SienaConstants.GOTO)) {
	String clientid = e.getAttribute(SienaConstants.UID).stringValue() + "@" + 
	  e.getAttribute(SienaConstants.UID).stringValue();
	// id of -1 is reserved for the WF
	if (!clientid.equals("" + _id)){
	  WFGauge.clock.gotoTime(absTimeSent, e.getAttribute(SienaConstants.NEWTIME).intValue());
	}
      } else {
	// event that we don't know what action this is
	// should throw an unknown error, or something
      }
    } else if (e.getAttribute(SienaConstants.AI2TV_CLIENT_SHUTDOWN) != null){
      myGauge.removeClient(id);
      logger.debug("Client: " + id + " removed from WF");

    } else {
      // event that we don't know what EVENT this is
      // should throw an unknown error, or something
      logger.debug("Received unknown event: " + e);
    }
  }

  private void getClientInfo(Notification e, ClientDesc currentClient){
    if (e.getAttribute(SienaConstants.ABS_TIME_SENT) != null)
      myGauge.setLastSampleTime(e.getAttribute(SienaConstants.ABS_TIME_SENT).longValue());
    if (e.getAttribute(SienaConstants.PREFETCHED_FRAMES) != null)
      currentClient.setPrefetchedFrames(e.getAttribute(SienaConstants.PREFETCHED_FRAMES).intValue());
    if (e.getAttribute(SienaConstants.LEVEL) != null)
      currentClient.setLevel(e.getAttribute(SienaConstants.LEVEL).intValue());
    if (e.getAttribute(SienaConstants.CACHE_LEVEL) != null)
      currentClient.setCacheLevel(e.getAttribute(SienaConstants.CACHE_LEVEL).intValue());
    if (e.getAttribute(SienaConstants.BANDWIDTH) != null)
      currentClient.setBandwidth(e.getAttribute(SienaConstants.BANDWIDTH).doubleValue());
    if (e.getAttribute(SienaConstants.FRAME_RATE) != null)
      currentClient.setFrameRate(e.getAttribute(SienaConstants.FRAME_RATE).intValue());
    if (e.getAttribute(SienaConstants.CLIENT_RESERVE_FRAMES) != null)
      currentClient.setReserveFrames(e.getAttribute(SienaConstants.CLIENT_RESERVE_FRAMES).intValue());
  }
}
