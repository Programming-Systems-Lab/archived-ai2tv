/*
 * @(#)ClientEffector.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Dan Phung
 * Last modified by:  Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.ai2tv.client;

import siena.*;
import siena.comm.*;
import psl.ai2tv.SienaConstants;
import psl.ai2tv.gauge.FrameDesc;

/**
 * The Client Effector that listens to WF
 * 
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class ClientEffector implements Notifiable {
  /** interval between probe events in ms.' */
  ThinClient _siena;
  private Notification _frameEvent;
  private Client _client;
  String _sienaServer;

  /**
   * create a ClientEffector
   *
   * @param c: associated client
   * @param sienaServer: location of the Siena server
   */
  ClientEffector (Client c, String sienaServer) {
    _client = c;
    _siena = null;
    _sienaServer = sienaServer;
    setupSiena();
  }

  /**
   * setup the filters describing what this subscriber want to
   * receive.
   */
  private void setupFilter() throws siena.SienaException {
    // WF related actions
    Filter filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_CLIENT_ADJUST, "");
    _siena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_WF_UPDATE_REQUEST, Op.ANY, "FOO");
    _siena.subscribe(filter, this);
  }


  /**
   * Connect to the Siena server and setup some minor details
   */
  private void setupSiena() {
    try {
      _siena = new ThinClient(_sienaServer);
      setupFilter();
    } catch (SienaException ise) {
      // } catch (InvalidSenderException ise) {
      Client.out.println ("Cannot connect to Siena bus");
      _siena = null;
      ise.printStackTrace();	
    }
    _frameEvent = new Notification();
  }

  /**
   * receive a notification from the Siena server
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

    Client.out.println("ClientEffector handleNotification(): I just got this event:" + event);
    String name = event.toString().substring(7).split("=")[0];
    AttributeValue attrib = event.getAttribute(name);
    Client.out.println("ClientEffector handle notification: name: " + name);
    Client.out.println("ClientEffector handle notification: attrib: " + attrib);

    if (name.equals(SienaConstants.AI2TV_WF_UPDATE_REQUEST)){
      publishUpdate(ppd);

    } else if (name.equals(SienaConstants.AI2TV_CLIENT_ADJUST) && 
	event.getAttribute(SienaConstants.CLIENT_ID).longValue() == _client.getID()){
      Client.out.println("found a WF commmand to do something, directed to ME!");
      Client.out.println("");
      if (event.getAttribute(SienaConstants.CHANGE_CLIENT_LEVEL_UP) != null){
	// Client.out.println("ClientEffector found command to change levels: " + event.getAttribute(SienaConstants.CHANGE_LEVEL).toString());
	_client.changeLevel(SienaConstants.CHANGE_CLIENT_LEVEL_UP);
      }
      if (event.getAttribute(SienaConstants.CHANGE_CLIENT_LEVEL_DOWN) != null){
	// Client.out.println("ClientEffector found command to change levels: " + event.getAttribute(SienaConstants.CHANGE_LEVEL).toString());
	_client.changeLevel(SienaConstants.CHANGE_CLIENT_LEVEL_DOWN);
      }
      if (event.getAttribute(SienaConstants.CHANGE_CACHE_LEVEL_UP) != null){
	// Client.out.println("ClientEffector found command to change levels: " + event.getAttribute(SienaConstants.CHANGE_LEVEL).toString());
	_client.changeLevel(SienaConstants.CHANGE_CACHE_LEVEL_UP);
      }
      if (event.getAttribute(SienaConstants.CHANGE_CACHE_LEVEL_DOWN) != null){
	// Client.out.println("ClientEffector found command to change levels: " + event.getAttribute(SienaConstants.CHANGE_LEVEL).toString());
	_client.changeLevel(SienaConstants.CHANGE_CACHE_LEVEL_DOWN);
      }
      if (event.getAttribute(SienaConstants.JUMP_TO) != null){
	Client.out.println("ClientEffector found command to jump to a certain frame: " + event.getAttribute(SienaConstants.JUMP_TO).stringValue());
	_client.jumpTo(event.getAttribute(SienaConstants.JUMP_TO).stringValue());
      } 
    } else {
      Client.err.println("Notification Error, received unknown name: " + name);
    }
  }

  /**
   * publish the periodic current client status
   *
   * @param ppd: previous propagation delay
   */
  void publishUpdate(long ppd){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.LEVEL, _client.getLevel());
    event.putAttribute(SienaConstants.CACHE_LEVEL, _client.getCacheLevel());    
    event.putAttribute(SienaConstants.CLIENT_RESERVE_FRAMES, _client.getReserveFrames());
    event.putAttribute(SienaConstants.AI2TV_WF_UPDATE_REPLY, "");
    event.putAttribute(SienaConstants.PREV_PROP_DELAY, ppd);
    event.putAttribute(SienaConstants.PREFETCHED_FRAMES, _client.getNumPrefetchedFrames(_client.getCacheLevel()));
    addFrameInfo(event);
    publishNotification(event);      
  }

  /**
   * handle the actual publishing to the Siena server
   *
   * @param event: Notification to publish
   */
  private void publishNotification(Notification event){
    try{
      event.putAttribute(SienaConstants.CLIENT_ID, _client.getID());
      event.putAttribute(SienaConstants.ABS_TIME_SENT, System.currentTimeMillis());
      
      _siena.publish(event);
    } catch (siena.SienaException e){
      Client.err.println("CommController publishing sienaException: " + e);
    }  
  }

  /**
   * add the information about the current frame into the notification
   */
  private void addFrameInfo(Notification update){
    FrameDesc fd = _client.getCurrentFrame();
    if (fd != null) {
      update.putAttribute(SienaConstants.LEFTBOUND, fd.getStart());
      update.putAttribute(SienaConstants.RIGHTBOUND, fd.getEnd());
      update.putAttribute(SienaConstants.MOMENT, fd.getNum());
      update.putAttribute(SienaConstants.LEVEL, fd.getLevel());
      update.putAttribute(SienaConstants.SIZE, fd.getSize());
      update.putAttribute(SienaConstants.TIME_SHOWN, fd.getTimeShown());
      update.putAttribute(SienaConstants.TIME_OFFSET, fd.getTimeOffset());
      update.putAttribute(SienaConstants.TIME_DOWNLOADED, fd.getTimeDownloaded());
    }
  }

  /**
   * shutdown the effector
   */
  void shutdown(){
    try {
      Filter filter = new Filter();
      filter.addConstraint(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      _siena.unsubscribe(filter, this);

      filter = new Filter();
      filter.addConstraint(SienaConstants.AI2TV_WF_UPDATE_REQUEST, "");
      _siena.unsubscribe(filter, this);
    } catch (siena.SienaException e) {
      Client.err.println("error:" + e);
    }
    Client.out.println("Shutting down Siena server");
    _siena.shutdown();

  }
}
