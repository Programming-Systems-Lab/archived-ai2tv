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
 * The Communications Controller of the AI2TV client.  Main layer
 * of communication handling is done through Siena events.
 *
 * WF related probes:
 * 1)  
 *
 *
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class CommController implements Notifiable{
  public static final int DEBUG = 0;

  private Notification frameEvent;
  private long clientID;
  private Client _client;
  private ThinClient _mySiena;
  private String _sienaServer;
  private Filter filter;
  private boolean _isActive = false;
  
  /**
   * create a CommController
   *
   * @param c: higher level client to communicate with
   * @param id: ID of the associated client, to be sent out with each message
   * @param sienaServer: point of contact to the Siena communications layer.
   */
  CommController(Client c, long id, String server){
    _isActive = true;
    _client = c;
    clientID = id;
    _mySiena = null;
    _sienaServer = server;
    setupSienaListener();
  }

  /**
   * setup the filters describing what this subscriber want to
   * receive.
   * 
   * note: dp2041: is there a better way to do this?  
   */
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

  /**
   * setup the communications link
   */
  private void setupSienaListener(){
    try {
      _mySiena = new ThinClient(_sienaServer);

      // subsribe to the events (specified in the method)
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
      _isActive = false;
      // e.printStackTrace();
    }
  }

  /**
   * showdown the communications layer
   */
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
  /**
   * publish the notification that the PLAY button was pressed
   */
  void playPressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "PLAY");
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  /**
   * publish the notification that the PAUSE button was pressed
   */
  void pausePressed(){
    // need to publish the notification that we are need to start playing.
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "PAUSE");
    Client.out.println("CommController publishing event: " + event);
    publishNotification(event);
  }

  /**
   * publish the notification that the STOP button was pressed
   */
  void stopPressed(){
    Notification event = new Notification();
    event.putAttribute("AI2TV_VIDEO_ACTION", "STOP");
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
    event.putAttribute("AI2TV_VIDEO_ACTION", "GOTO");
    event.putAttribute("NEWTIME", gotoTime);
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
      _mySiena.publish(event);
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

