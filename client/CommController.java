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
import psl.ai2tv.SienaConstants;
import siena.*;

/**
 * The Communications Controller of the AI2TV client.  Main layer
 * of communication handling is done through Siena events.
 *
 * WF related probes:
 * 1)  
 *
 *
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class CommController implements Notifiable{
  public static final int DEBUG = 0;

  private Client _client;
  private ThinClient _siena;
  private String _sienaServer;
  private boolean _isActive = false;

  /**
   * used to ignore late or inverted events (we only respond to the
   * latest events, no early sent messages)
   */
  private long _commandIndex;

  /**
   * create a CommController
   *
   * @param c: higher level client to communicate with
   * @param sienaServer: point of contact to the Siena communications layer.
   */
  CommController(Client c, String server){
    _client = c;
    _siena = null;
    _sienaServer = server;
    _commandIndex = 0;
    setupSienaListener();
  }

  /**
   *
   */
  void incrementCommandIndex(){
    _commandIndex++;
  }

  /**
   * setup the filters describing what this subscriber want to
   * receive.
   */
  void setupSienaFilter()  {
    try {
      Filter filter = new Filter();
      // the string "FOO" doesn't mean anything (the string is ignored)
      filter.addConstraint(SienaConstants.AI2TV_VIDEO_ACTION, Op.ANY, "FOO");
      filter.addConstraint(SienaConstants.GID, Op.EQ, _client.getGID());
      filter.addConstraint(SienaConstants.VSID, Op.EQ, _client.getVSID());
      _siena.subscribe(filter, this);
    } catch (SienaException e){
      Client.err.println("SienaException caught setting up Siena Filter: " + e);
    }
  }

  /**
   * setup the communications with the Video related actions
   */
  void setupWGFilter(){
    try {
      Filter filter = new Filter();
      filter.addConstraint(SienaConstants.GET_ACTIVE_VSIDS_REPLY, Op.ANY, "FOO");
      _siena.subscribe(filter, this);
      
      filter = new Filter();
      filter.addConstraint(SienaConstants.JOIN_NEW_VSID_REPLY, Op.ANY, "FOO");
      _siena.subscribe(filter, this);
    } catch (SienaException e){
      Client.err.println("SienaException caught setting up WG Filter: " + e);
    }
  }

  /**
   * setup the communications with the Video related actions
   */
  private void setupSienaListener(){
    try {
      _siena = new ThinClient(_sienaServer);

      // subsribe to the events (specified in the method)
      //setupFilter();
      // Client.out.println("subscribing for " + filter.toString());
      _isActive = true;

    } catch (SienaException e) {
      // ; // WTF?
      // } catch (siena.comm.InvalidSenderException e) {
      System.err.println ("Cannot connect to Siena bus: "  + e);
      // _siena = null;
      e.printStackTrace(System.err);
    }
  }
  
  /**
   * get the active videos from the WG server
   */
  void getActiveVSIDs(){
    Notification request = new Notification();
    request.putAttribute(SienaConstants.GET_ACTIVE_VSIDS, "FOO");
    publishNotification(request);
  }

  /**
   * create a new video session
   */
  void joinNewVSID(String videoName, String date){
    Notification request = new Notification();
    request.putAttribute(SienaConstants.JOIN_NEW_VSID, "FOO");
    request.putAttribute(SienaConstants.VIDEO_NAME, videoName);
    request.putAttribute(SienaConstants.VIDEO_DATE, date);
    publishNotification(request);
  }

  /**
   * join an existing video session
   */
  void joinActiveVSID(String vsid){
    Notification request = new Notification();
    request.putAttribute(SienaConstants.JOIN_ACTIVE_VSID, "FOO");
    publishNotification(request);
  }

  /**
   * showdown the communications layer
   */
  void shutdown(){
     Client.debug.println("Shutting down CommController");
    Client.debug.println("Unsubscribing to Siena server");
    try {
      Notification shutdownEvent = new Notification();
      shutdownEvent.putAttribute(SienaConstants.REMOVE_USER_FROM_VSID, "FOO");
      publishNotification(shutdownEvent);

      shutdownEvent = new Notification();
      shutdownEvent.putAttribute(SienaConstants.AI2TV_CLIENT_SHUTDOWN, "");
      publishNotification(shutdownEvent);

      Filter filter = new Filter();
      filter.addConstraint(SienaConstants.AI2TV_VIDEO_ACTION, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);

      filter = new Filter();
      filter.addConstraint(SienaConstants.GET_ACTIVE_VSIDS_REPLY, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);

      filter = new Filter();
      filter.addConstraint(SienaConstants.JOIN_NEW_VSID_REPLY, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);
    } catch (siena.SienaException e) {
      Client.err.println("error:" + e);
    }
    Client.debug.println("Shutting down Siena server");
    _siena.shutdown();
    _isActive = false;
  }
  
  /**
   * recieve a notification from the Siena server
   *
   * @param event: Notification sent from the Siena server
   */
  public void notify(Notification event) {
    handleNotification(event);
  };
  
  /**
   * recieve some notifications from the Siena server
   *
   * @param events: Notifications sent from the Siena server
   */
  public void notify(Notification [] events) { 
    for (int i=0; i<events.length; i++)
      handleNotification(events[i]);      
  }

  /**
   * parse, interpret and handle the notification
   *
   * @param event: Notification sent by the Siena server
   */
  private void handleNotification(Notification event){
    
    // Client.out.println("handleNotification(): I just got this event:" + event + ": at : " 
    // + Calendar.getInstance().getTime());
    Client.debug.println("handleNotification(): I just got this event:" + event + ": at : " 
		       + Calendar.getInstance().getTime());

    AttributeValue absAttrib = event.getAttribute(SienaConstants.ABS_TIME_SENT);
    long absTimeSent = -1;
    if (absAttrib != null){
      absTimeSent = absAttrib.longValue();
    }

    if (event.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION) != null){
      String attrib = event.getAttribute(SienaConstants.AI2TV_VIDEO_ACTION).stringValue();
      
      if (attrib.equals(SienaConstants.PLAY)){
	Client.debug.println("CommController: PLAY action event received");
	_client.commPlay(absTimeSent); 
      } else if (attrib.equals(SienaConstants.STOP)){
	Client.debug.println("CommController: PLAY action event received");
	_client.commStop();

      } else if (attrib.equals(SienaConstants.PAUSE)){
	Client.debug.println("CommController: PLAY action event received");
	_client.commPause(absTimeSent);

      } else if (attrib.equals(SienaConstants.GOTO)){
	Client.debug.println("CommController: PLAY action event received");
	_client.commGoto(absTimeSent, event.getAttribute(SienaConstants.NEWTIME).intValue());
      } else {
	Client.err.println("AI2TV_VIDEO_ACTION: Notification Error, received unknown attribute: " + attrib);
      }
      
    } else if (event.getAttribute(SienaConstants.GET_ACTIVE_VSIDS_REPLY) != null){
      if (event.getAttribute(SienaConstants.ACTIVE_VSIDS) != null){
	String activeVSIDs = event.getAttribute(SienaConstants.ACTIVE_VSIDS).stringValue();
	String activeVSIDInfo = event.getAttribute(SienaConstants.ACTIVE_VSIDS_INFO).stringValue();
	_client.setActiveVSID(activeVSIDs, activeVSIDInfo);
      } else {
	_client.setActiveVSID(null, null);
      }

    } else if (event.getAttribute(SienaConstants.JOIN_NEW_VSID_REPLY) != null){
      String vsid = event.getAttribute(SienaConstants.JOIN_NEW_VSID_REPLY).stringValue();
      _client.setVSID(vsid);
      _client.setWaitForWGReply(false);
      
    } else {
      Client.err.println("Notification Error, received unknown event");
    }
  }

  // --------- Viewer initiated actions ---------- //
  /**
   * publish the notification that the PLAY button was pressed
   */
  void playPressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_VIDEO_ACTION, SienaConstants.PLAY);
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  /**
   * publish the notification that the PAUSE button was pressed
   */
  void pausePressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_VIDEO_ACTION, SienaConstants.PAUSE);
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  /**
   * publish the notification that the STOP button was pressed
   */
  void stopPressed(){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_VIDEO_ACTION, SienaConstants.STOP);
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  /**
   * publish the notification that the GOTO slider was used
   *
   * @param gotoTime: time indicated by the new position of the slider
   */
  void gotoPressed(int gotoTime){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_VIDEO_ACTION, SienaConstants.GOTO);
    event.putAttribute(SienaConstants.NEWTIME, gotoTime);
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }
   
  /**
   * handle the actual publishing to the Siena server
   *
   * @param event: Notification to publish
   */
  private void publishNotification(Notification event){
    try{
      Client.out.println("publishing event: " + Calendar.getInstance().getTime());
      event.putAttribute(SienaConstants.ABS_TIME_SENT, System.currentTimeMillis());
      event.putAttribute(SienaConstants.UID, _client.getUID());
      event.putAttribute(SienaConstants.GID, _client.getGID());
      if (_client.getVSID() != null)
	event.putAttribute(SienaConstants.VSID, _client.getVSID());
      _siena.publish(event);
    } catch (siena.SienaException e){
      Client.err.println("CommController publishing sienaException: " + e);
    }  
  }

  // ------- END Viewer initiated actions -------- //
  

  // void setTarget(CacheController cc) { cache = cc; }
  boolean isActive() { return _isActive; }
  void setActive(boolean flag) {
    _isActive = flag;
  }
}
