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

  private Notification frameEvent;
  private long clientID;
  private Client _client;
  private ThinClient _siena;
  private String _sienaServer;
  private boolean _isActive = false;

  /**
   * create a CommController
   *
   * @param c: higher level client to communicate with
   * @param id: ID of the associated client, to be sent out with each message
   * @param sienaServer: point of contact to the Siena communications layer.
   */
  CommController(Client c, long id, String server){
    _client = c;
    clientID = id;
    _siena = null;
    _sienaServer = server;
    setupSienaListener();

    // send out a registration packet to the WF
    sendRegistrationToWF();
  }

  /**
   * setup the filters describing what this subscriber want to
   * receive.
   */
  private void setupFilter() throws siena.SienaException {
    Filter filter = new Filter();
    // the string "FOO" doesn't mean anything (the string is ignored)
    filter.addConstraint(SienaConstants.AI2TV_VIDEO_ACTION, Op.ANY, "FOO");
    _siena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_WF_UPDATE_REQUEST, Op.ANY, "FOO");
    _siena.subscribe(filter, this);
  }

  /**
   * setup the communications link
   */
  private void setupSienaListener(){
    try {
      _siena = new ThinClient(_sienaServer);

      // subsribe to the events (specified in the method)
      setupFilter();
      // Client.out.println("subscribing for " + filter.toString());
      _isActive = true;

    } catch (siena.comm.PacketSenderException e) {
      // what is this?
      Client.out.println ("Caught exception in setting up the Siena server: "  + e);
    } catch (SienaException e) {
      // ; // WTF?
      // } catch (siena.comm.InvalidSenderException e) {
      Client.out.println ("Cannot connect to Siena bus: "  + e);
      // mySiena = null;
      // e.printStackTrace();
    }
  }
  
  private void sendRegistrationToWF(){
    Notification event = new Notification();
    try{
      Client.out.println("Registering client: " + _client.getID());
      event.putAttribute(SienaConstants.AI2TV_WF_REG, "");

      Client.out.println("publishing event: " + Calendar.getInstance().getTime());
      event.putAttribute(SienaConstants.CLIENT_ID, _client.getID());
      _siena.publish(event);
    } catch (siena.SienaException e){
      Client.err.println("CommController publishing sienaException: " + e);
    }      
  }


  /**
   * showdown the communications layer
   */
  void shutdown(){
    Client.out.println("Shutting down CommController");
    Client.out.println("Unsubscribing to Siena server");
    try {
      Notification shutdownEvent = new Notification();
      shutdownEvent.putAttribute(SienaConstants.AI2TV_CLIENT_SHUTDWON, "");
      publishNotification(shutdownEvent);

      Filter filter = new Filter();
      filter.addConstraint(SienaConstants.AI2TV_VIDEO_ACTION, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);
    } catch (siena.SienaException e) {
      Client.err.println("error:" + e);
    }
    Client.out.println("Shutting down Siena server");
    _siena.shutdown();
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
    long now = System.currentTimeMillis();
    Client.out.println("handleNotification(): I just got this event:" + event + ": at : " 
		       + Calendar.getInstance().getTime());
    
    // get the propagation delay
    AttributeValue absAttrib = event.getAttribute(SienaConstants.ABS_TIME_SENT);
    long absTimeSent = -1;
    long ppd = -1; // ppd: previous propagation delay
    if (absAttrib != null){
      // here we calculate the difference between when the request was
      // sent and when it was received/handled .  Note that this
      // difference includes some overhead of some attrib checking so
      // it is not entirely accurate
      absTimeSent = absAttrib.longValue();
      ppd = now - absTimeSent;
    }

    String name = event.toString().substring(7).split("=")[0];
    String attrib = event.getAttribute(name).stringValue();

    Client.out.println("handle notification: name: " + name);
    Client.out.println("handle notification: attrib: " + attrib);
    if (name.equals(SienaConstants.AI2TV_WF_UPDATE_REQUEST)){
      publishUpdate(ppd);
      
    } else if (name.equals(SienaConstants.AI2TV_VIDEO_ACTION)){
      
      if (attrib.equals(SienaConstants.PLAY)){
	_client.commPlay(absTimeSent); 
      } else if (attrib.equals(SienaConstants.STOP)){
	_client.commStop();

      } else if (attrib.equals(SienaConstants.PAUSE)){
	_client.commPause(absTimeSent);

      } else if (attrib.equals(SienaConstants.GOTO)){
	_client.commGoto(absTimeSent, event.getAttribute(SienaConstants.NEWTIME).intValue());
      } else {
	Client.err.println("AI2TV_VIDEO_ACTION: Notification Error, received unknown attribute: " + attrib);
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
   * publish the current client status
   *
   * @param ppd: previous propagation delay
   */
  void publishUpdate(long ppd){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_WF_UPDATE_REPLY, "");
    event.putAttribute(SienaConstants.PREV_PROP_DELAY, ppd);
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
      event.putAttribute(SienaConstants.CLIENT_ID, _client.getID());
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
