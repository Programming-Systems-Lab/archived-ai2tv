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

class CommController implements Notifiable{
  /** interval between probe events in ms.' */
  private long probeDelay;
  private boolean loopAlways = true;

  private Notification frameEvent;
  private String clientID;
  private Client _client;

  public static final int DEBUG = 0;

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

    // WF related actions
    filter = new Filter();
    filter.addConstraint("AI2TV_FRAME_UPDATE", "");
    _mySiena.subscribe(filter, this);
  }

  private void setupSienaListener(){
    try {
      _mySiena = new ThinClient("ka:localhost:4444");

      setupFilter();
      // Client.out.println("subscribing for " + filter.toString());

    } catch (siena.comm.PacketSenderException e) {
      // what is this?
      Client.out.println ("Caught exception in setting up the Siena server: "  + e);
    } catch (SienaException e) {
      // ; // WTF?
      // } catch (siena.comm.InvalidSenderException e) {
      Client.out.println ("Cannot connect to Siena bus: "  + e);
      // mySiena = null;
      loopAlways = false;
      _isActive = false;
      // e.printStackTrace();
    }
  }

  void shutdown(){
    Client.out.println("Shutting down CommController");
    Client.out.println("Unsubscribing to Siena server");
    try {
      _mySiena.unsubscribe(filter, this);
    } catch (siena.SienaException e) {
      Client.out.println("error:" + e);
    }
    Client.out.println("Shutting down Siena server");
    _mySiena.shutdown();
  }
  
  public void notify(Notification e) {
    handleNotification(e);
  };

  public void notify(Notification [] s) { }

  private void handleNotification(Notification event){
    Client.out.println("handleNotification(): I just got this event:" + event + ": at : " 
		       + Calendar.getInstance().getTime());
    
    String name = event.toString().substring(7).split("=")[0];
    AttributeValue attrib = event.getAttribute(name);
    Client.out.println("handle notification: name: " + name);
    Client.out.println("handle notification: attrib: " + attrib);
    if (name.equals("AI2TV_VIDEO_ACTION")){
      if (attrib.toString().equals("\"PLAY\"")){
	_client.commPlay(); 
      } else if (attrib.toString().equals("\"STOP\"")){
	_client.commStop(); 
      } else if (attrib.toString().equals("\"PAUSE\"")){
	_client.commPause(); 
      } else if (attrib.toString().startsWith("\"GOTO")){
	_client.commGoto(event.getAttribute("NEWTIME").intValue());
      } else {
	Client.err.println("AI2TV_VIDEO_ACTION: Notification Error, received unknown attribute: " + attrib);
      }

    } else if (name.equals("AI2TV_FRAME_UPDATE") && 
	       event.getAttribute("CLIENT_ID").longValue() == _client.getID()){
      Client.out.println("found a WF commmand to do something, directed to ME!");
      Client.out.println("");
      if (event.getAttribute("CHANGE_LEVEL") != null){
	_client.changeLevel(event.getAttribute("CHANGE_LEVEL").toString());
      } else if (event.getAttribute("GOTO_FRAME") != null){
	_client.setNextFrame(event.getAttribute("GOTO_FRAME").intValue());
      } else {
	Client.err.println("AI2TV_FRAME_UDPATE: Notification Error, received unknown attribute: " + attrib);
      }

    } else {
      Client.err.println("Notification Error, received unknown name: " + name);
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
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  void pausePressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "PAUSE");
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  void stopPressed(){
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "STOP");
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  void gotoPressed(int gotoTime){
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "GOTO");
    event.putAttribute("NEWTIME", gotoTime);
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }
  
  private void publishNotification(Notification event){
    try{
      Client.out.println("publishing event: " + Calendar.getInstance().getTime());
      _mySiena.publish(event);
    } catch (siena.SienaException e){
      Client.err.println("CommController publishing sienaException: " + e);
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
