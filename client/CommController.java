/*
 * @(#)CommController.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Dan Phung
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.ai2tv.client;
import java.io.*;
import java.util.Calendar;
import java.net.*;
import psl.ai2tv.gauge.*;
import siena.*;

/**
 * The Communications Controller of the AI2TV client.  Controls TCP
 * and UDP server/client ports.
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

class CommController implements Notifiable, Runnable{
  /** interval between probe events in ms.' */
  private long probeDelay;
  private boolean loopAlways = true;

  private Notification frameEvent;
  private String clientID;
  private Client _client;

  public static final int DEBUG = 0;

  public static PrintStream out = System.out;
  public static PrintStream err = System.err;

  private boolean _isActive = false;

  private ThinClient _mySiena;

  private Filter filter;

  String _mySienaServer;
    
  CommController(Client c, String id, String sienaServer){
    probeDelay = 10000;
    _isActive = true;
    _client = c;
    clientID = id;
    _mySiena = null;
    setupSienaListener();
    _mySienaServer = sienaServer;
    Thread mainThread = new Thread(this);
    mainThread.start();
  }

  // dp2041: is there a better way to do this?
  private void setupFilter() throws siena.SienaException {
    filter = new Filter();
    filter.addConstraint("AI2TV_VIDEO_ACTION", "UP_LEVEL");
    _mySiena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint("AI2TV_VIDEO_ACTION", "DOWN_LEVEL");
    _mySiena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint("AI2TV_VIDEO_ACTION", "PLAY");
    _mySiena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint("AI2TV_VIDEO_ACTION", "STOP");
    _mySiena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint("AI2TV_VIDEO_ACTION", "PAUSE");
    _mySiena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint("AI2TV_VIDEO_ACTION", "GOTO");
    _mySiena.subscribe(filter, this);
  }

  private void setupSienaListener(){
    try {
      _mySiena = new ThinClient("ka:localhost:4444");

      setupFilter();
      // System.out.println("subscribing for " + filter.toString());

    } catch (siena.comm.PacketSenderException e) {
      // what is this?
      System.out.println ("Caught exception in setting up the Siena server: "  + e);
    } catch (SienaException e) {
      // ; // WTF?
      // } catch (siena.comm.InvalidSenderException e) {
      System.out.println ("Cannot connect to Siena bus: "  + e);
      // mySiena = null;
      loopAlways = false;
      _isActive = false;
      // e.printStackTrace();
    }

    // trying to optimize by calling constructors for events only once
    frameEvent = new Notification();
    frameEvent.putAttribute("FRAME", "frame_ready");
    frameEvent.putAttribute("ClientID", clientID);
    frameEvent.putAttribute("leftbound", 0);	
    frameEvent.putAttribute("rightbound", 0);
    frameEvent.putAttribute("moment", 0);
    frameEvent.putAttribute("level", -1); 
    frameEvent.putAttribute("probeTime", 0);
  }

  public void run(){
    // loopAlways = true;
    // _isActive = true;
    // try {
    // _mySiena.subscribe(filter, this);
      // while(_isActive){
	try {
	  // Thread.sleep(300000);	// sleeps for five minutes
	  Thread.currentThread().sleep(probeDelay);
	} catch (java.lang.InterruptedException e) {
	  System.out.println("interrupted: " + e); 
	}
	// }
	//     } catch (SienaException ex) {
	// System.err.println("Siena error:" + ex.toString());
	//     }
      //send probe message
      // sendUpdate(cache.currFrame);
  }
	
  /**
   * sends updates
   *
   * @param fd: 
   */
  private void sendUpdate (FrameDesc fd) {
    if (fd != null) {
      //System.out.println ("Sending Frame info");
      //update only necessary fields
      frameEvent.putAttribute("leftbound", fd.getStart());
      frameEvent.putAttribute("rightbound", fd.getEnd());
      frameEvent.putAttribute("moment", fd.getNum());
      frameEvent.putAttribute("level", fd.getLevel());
      frameEvent.putAttribute("probeTime", System.currentTimeMillis());
      try { 
	// mySiena.publish(frameEvent);
	_mySiena.publish(frameEvent);
      } catch (SienaException se) {
	se.printStackTrace();	
      }
    }
  }

  void shutdown(){
    System.out.println("Shutting down CommController");
    System.out.println("Unsubscribing to Siena server");
    try {
      _mySiena.unsubscribe(filter, this);
    } catch (siena.SienaException e) {
      System.out.println("error:" + e);
    }
    System.out.println("Shutting down Siena server");
    _mySiena.shutdown();
  }
  
  public void notify(Notification e) {
    handleNotification(e);
  };

  public void notify(Notification [] s) { }

  private void handleNotification(Notification event){
    System.out.println("handleNotification(): I just got this event:" + event + ": at : " 
		       + Calendar.getInstance().getTime());
    AttributeValue attrib = event.getAttribute("AI2TV_VIDEO_ACTION");
    if (attrib.toString().equals("\"PLAY\"")){
      _client.commPlay(); 
    } else if (attrib.toString().equals("\"STOP\"")){
      _client.commStop(); 
    } else if (attrib.toString().equals("\"PAUSE\"")){
      _client.commPause(); 
    } else if (attrib.toString().startsWith("\"GOTO")){
      _client.commGoto(event.getAttribute("NEWTIME").intValue());
    } else {
      System.err.println("Notification Error, received unknown attribute: " + attrib);
    }
  }

  // --------- Viewer initiated actions ---------- //
  /*
   *
   */
  void playPressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "PLAY");
    System.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  void pausePressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "PAUSE");
    System.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  void stopPressed(){
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "STOP");
    System.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  void gotoPressed(int gotoTime){
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "GOTO");
    event.putAttribute("NEWTIME", gotoTime);
    System.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }
  
  private void publishNotification(Notification event){
    try{
      System.out.println("publishing event: " + Calendar.getInstance().getTime());
      _mySiena.publish(event);
    } catch (siena.SienaException e){
      System.err.println("CommController publishing sienaException: " + e);
    }  
  }

  // ------- END Viewer initiated actions -------- //
  

  // void setTarget(CacheController cc) { cache = cc; }
  void setProbingFrequency (long f) {probeDelay = f; }
  void stopProbe() { loopAlways = false; }
  boolean isActive() { return _isActive; }
  void setActive(boolean flag) {
    _isActive = flag;
  }
}
