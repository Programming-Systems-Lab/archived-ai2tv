package psl.ai2tv.workflow;

import psl.ai2tv.gauge.ClientDesc;
import psl.ai2tv.gauge.SimpleGaugeSubscriber;
import psl.ai2tv.SienaConstants;

import siena.Notification;
import siena.SienaException;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

class WFSubscriber extends SimpleGaugeSubscriber implements Runnable{

  private static final Logger logger = Logger.getLogger(WFSubscriber.class);

  private static final long REFRESH_DURATION = 5000; // check clients every 5 secs

  private WFGauge myGauge;
  private long _id;
  private boolean _isActive;

  public WFSubscriber(WFGauge wfg)
    throws SienaException, IOException {

    _id = -1 * System.currentTimeMillis();
    myGauge = wfg;
    _isActive = false;
    setup();
  }

  /**
   * collect periodic stats to inform the WFSubscriber
   */
  public void run(){
    /*
    while (_isActive){
      try {
	//sleep(REFRESH_DURATION);
	sleep(5000);
	checkClients();
      } catch (InterruptedException e){
	System.err.println("Exception in WFSubscriber periodic thread: " + e);
      }
    }
    // mainSiena.publish
    */
  }

  private void checkClients(){
    Hashtable ht = myGauge.getGroupClients();
    // 999
    // Set clientIDs = ht.getKeys();

    // for each client, get a new set of reports
    // we should
    // 1) check that the current times reported by
    // the clients are all syncrhonized
    // 2)
    ClientDesc currentClient;
    // 999
    /*
    for (Iterator i=clientIDs.iterator(); i.hasNext(); ){
      currentClient = i.next();
      getReport(currentClient);
    }
    */
  }

  private void getReport(ClientDesc currentClient){
    Notification probe = new Notification();
    probe.putAttribute(SienaConstants.AI2TV_WF_UPDATE_REQUEST, "");
    probe.putAttribute(SienaConstants.CLIENT_ID, currentClient.getClientID());
    try {
      // dan needs to learn how to use log4j.
      System.out.println("WF publishing report request: " + probe);
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

    String id = String.valueOf(e.getAttribute(SienaConstants.CLIENT_ID).longValue());
    Hashtable ht = myGauge.getGroupClients();
    currentClient = (ClientDesc)ht.get(id);
    if (currentClient == null) {
        logger.debug("adding new client " + id);

      ht.put(id, currentClient = new ClientDesc(id));
      myGauge.getBucket().update(id, currentClient);
    }

    if (e.getAttribute(SienaConstants.AI2TV_FRAME) != null){

      long currentTime = myGauge.clock.currentTime();
      currentClient.setFrame(e.getAttribute(SienaConstants.LEFTBOUND).intValue(),
			     e.getAttribute(SienaConstants.MOMENT).intValue(),
			     e.getAttribute(SienaConstants.RIGHTBOUND).intValue(),
			     currentTime,
			     e.getAttribute(SienaConstants.LEVEL).intValue(),
			     e.getAttribute(SienaConstants.SIZE).intValue());

      // 999
      // } else if (e.getAttribute(SienaConstants.AI2TV_WF_REPORT_REPLY) != null){




    } else if (e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION) != null){
      String action = e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION).stringValue();
      long absTimeSent = e.getAttribute(SienaConstants.ABS_TIME_SENT).longValue();
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
	logger.debug("Received unknown event: " + e);
      }
    } else {
      // event that we don't know what EVENT this is
      // should throw an unknown error, or something
      logger.debug("Received unknown event: " + e);
    }
  }
}
