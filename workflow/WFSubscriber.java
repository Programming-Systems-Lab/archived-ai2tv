package psl.ai2tv.workflow;

import psl.ai2tv.gauge.ClientDesc;
import psl.ai2tv.gauge.SimpleGaugeSubscriber;
import psl.ai2tv.SienaConstants;

import siena.Notification;
import siena.SienaException;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

class WFSubscriber extends SimpleGaugeSubscriber {

  private static final Logger logger = Logger.getLogger(WFSubscriber.class);

  private WFGauge myGauge;
	
  public WFSubscriber(WFGauge wfg) 
    throws SienaException, IOException {

    myGauge = wfg;	
    setup();
  }

  public void notify(Notification e) {
    ClientDesc currentClient;

    logger.debug("received " + e);

    String id = String.valueOf(e.getAttribute(SienaConstants.CLIENT_ID).longValue());
    Hashtable ht = myGauge.getGroupClients();	
    currentClient = (ClientDesc)ht.get(id);
    if (currentClient == null) {
      ht.put(id, currentClient = new ClientDesc(id));
      myGauge.getBucket().update(id, currentClient);
    }
    
    
    if (e.getAttribute(SienaConstants.AI2TV_FRAME) != null){
      
      //normalize download time
      long t = myGauge.currentTime(); // e.getAttribute(SienaConstants.PROBE_TIME).longValue();// - myGauge.getStartTime();
      currentClient.setFrame(e.getAttribute(SienaConstants.LEFTBOUND).intValue(),
			     e.getAttribute(SienaConstants.MOMENT).intValue(),
			     e.getAttribute(SienaConstants.RIGHTBOUND).intValue(),
			     t,
			     e.getAttribute(SienaConstants.LEVEL).intValue(),
			     e.getAttribute(SienaConstants.SIZE).intValue());
      
    } else {
      String action = e.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION).stringValue();
      if (action.equals(SienaConstants.PLAY)) {
	//check if clients are already running
	if (! myGauge.isRunning()) {
	  // nominal start time = 1st client start time
	  long st = myGauge.currentTime(); // System.currentTimeMillis();
	  myGauge.setStartTime(st);
	  currentClient.setStartTime(st);
	  logger.debug("starting nominal");
	  myGauge.startNominal();
	} 
	myGauge.startTime();
	
      } else if (action.equals(SienaConstants.PAUSE)) {
	  myGauge.pauseTime();

      } else if (action.equals(SienaConstants.STOP)) {
	myGauge.stopTime();
	
      } else if (action.equals(SienaConstants.GOTO)) {
	myGauge.gotoTime(e.getAttribute(SienaConstants.NEWTIME).intValue());
      
      } else {
	// should throw an unknown error, or something
	logger.debug("Received unknown event: " + e);
      }
    }
  }
}
