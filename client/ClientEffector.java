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

/**
 * The Client Effector that listens to WF
 * 
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class ClientEffector implements Notifiable {
  /** interval between probe events in ms.' */
  ThinClient _mySiena;
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
    _mySiena = null;
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
    _mySiena.subscribe(filter, this);
  }


  /**
   * Connect to the Siena server and setup some minor details
   */
  private void setupSiena() {
    try {
      _mySiena = new ThinClient(_sienaServer);
      setupFilter();
    } catch (SienaException ise) {
      // } catch (InvalidSenderException ise) {
      Client.out.println ("Cannot connect to Siena bus");
      _mySiena = null;
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
    Client.out.println("ClientEffector handleNotification(): I just got this event:" + event);
    String name = event.toString().substring(7).split("=")[0];
    AttributeValue attrib = event.getAttribute(name);
    Client.out.println("ClientEffector handle notification: name: " + name);
    Client.out.println("ClientEffector handle notification: attrib: " + attrib);
    if (name.equals(SienaConstants.AI2TV_CLIENT_ADJUST) && 
	event.getAttribute(SienaConstants.CLIENT_ID).longValue() == _client.getID()){
      Client.out.println("found a WF commmand to do something, directed to ME!");
      Client.out.println("");
      if (event.getAttribute(SienaConstants.CHANGE_LEVEL) != null){
	Client.out.println("ClientEffector found command to change levels: " + event.getAttribute(SienaConstants.CHANGE_LEVEL).toString());
	_client.changeLevel(event.getAttribute(SienaConstants.CHANGE_LEVEL).toString());
      } else if (event.getAttribute(SienaConstants.PLAN_FOR) != null){
	Client.out.println("ClientEffector found command to plan for frame: " + event.getAttribute(SienaConstants.PLAN_FOR).intValue());
	_client.planFor(event.getAttribute(SienaConstants.PLAN_FOR).stringValue());
      } else {
	Client.err.println("AI2TV_FRAME_UDPATE: Notification Error, received unknown attribute: " + attrib);
      }

    } else {
      Client.err.println("Notification Error, received unknown name: " + name);
    }
  }

  void shutdown(){
    try {
      Filter filter = new Filter();
      filter.addConstraint(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      _mySiena.unsubscribe(filter, this);
    } catch (siena.SienaException e) {
      Client.err.println("error:" + e);
    }
    Client.out.println("Shutting down Siena server");
    _mySiena.shutdown();

  }
}
