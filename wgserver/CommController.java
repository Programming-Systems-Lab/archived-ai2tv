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
 *  $Source:
 */

package psl.ai2tv.wgserver;

import java.io.*;
import java.util.*;
import psl.ai2tv.gauge.*;
import psl.ai2tv.SienaConstants;
import siena.*;

/**
 * The Communications Controller of the WG server.  Main layer
 * of communication handling is done through Siena events.
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class CommController implements Notifiable{
  public static final int DEBUG = 0;

  private WGServer _server;
  private ThinClient _siena;
  private String _sienaServer;
  private boolean _isActive = false;

  /**
   * create a CommController
   *
   * @param s: main process thread to communicate with
   * @param sienaServer: point of contact to the Siena communications layer.
   */
  CommController(WGServer s, String server){
    _server = s;
    _siena = null;
    _sienaServer = server;
    setupSienaListener();
  }

  /**
   * setup the filters describing what this subscriber want to
   * receive.
   */
  private void setupFilter() throws siena.SienaException {
    // these first 4 filters are for WG registration features
    Filter filter = new Filter();
    filter.addConstraint(SienaConstants.GET_ACTIVE_VSIDS, Op.ANY, "FOO");
    _siena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint(SienaConstants.JOIN_NEW_VSID, Op.ANY, "FOO");
    _siena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint(SienaConstants.JOIN_ACTIVE_VSID, Op.ANY, "FOO");
    _siena.subscribe(filter, this);

    filter = new Filter();
    filter.addConstraint(SienaConstants.REMOVE_USER_FROM_VSID, Op.ANY, "FOO");
    _siena.subscribe(filter, this);

    // we subscribe to play events in order to synch up late comers
    filter = new Filter();
    filter.addConstraint(SienaConstants.PLAY, Op.ANY, "FOO");
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
      // System.out.println("subscribing for " + filter.toString());
      _isActive = true;

    } catch (siena.comm.PacketSenderException e) {
      // what is this?
      System.err.println ("Caught exception in setting up the Siena server: "  + e);
      e.printStackTrace(System.err);
      _siena = null;
    } catch (siena.comm.InvalidSenderException e) {
      System.err.println ("InvalidSenderException caught in setupSienaListener: "  + e);
      e.printStackTrace(System.err);
      _siena = null;
    } catch (SienaException e) {
      System.err.println ("Cannot connect to Siena bus: "  + e);
      e.printStackTrace(System.err);
      _siena = null;
    }
    if (_siena == null)
      System.exit(1);
  }

  /**
   * showdown the communications layer
   */
  void shutdown(){
    try {
      Filter filter = new Filter();
      filter.addConstraint(SienaConstants.GET_ACTIVE_VSIDS, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);

      filter = new Filter();
      filter.addConstraint(SienaConstants.JOIN_NEW_VSID, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);

      filter = new Filter();
      filter.addConstraint(SienaConstants.JOIN_ACTIVE_VSID, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);

      filter = new Filter();
      filter.addConstraint(SienaConstants.REMOVE_USER_FROM_VSID, Op.ANY, "FOO");
      _siena.unsubscribe(filter, this);

    } catch (siena.SienaException e) {
      System.err.println("error:" + e);
    }
    System.out.println("Shutting down WGServer's Siena server");
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
    System.out.println("handleNotification(): I just got this event:" + event + ": at : " 
		       + Calendar.getInstance().getTime());

    if (event.getAttribute(SienaConstants.GET_ACTIVE_VSIDS) != null){
      System.out.println("got event to get active vsids");
      String gid = event.getAttribute(SienaConstants.GID).stringValue();
      Hashtable sessions = _server.getActiveVSIDs(gid);
      Notification reply = new Notification();
      reply.putAttribute(SienaConstants.GET_ACTIVE_VSIDS_REPLY, "FOO");
      if (sessions != null){
	// example reply: CS4118-10,2003-07-28;08:00,danp,peppo/CS4118-11,2003-07-28;08:00,matias
	String activeVIDs = "";   // the actual VIDs
	String activeVIDsInfo = ""; // the VID info (video name, date, etc.)
	Set vsids = sessions.keySet();
	Iterator itr = vsids.iterator();
	VSID vsid;
	// insert each video session into the reply string
	while(itr.hasNext()){
	  vsid = (VSID) sessions.get(itr.next());
	  activeVIDs += vsid.getVSID();
	  activeVIDsInfo += vsid.getName() + "," + vsid.getDate() + ",";

	  // insert the users
	  Set users = vsid.getUIDs();
	  Iterator userItr = users.iterator();
	  while(userItr.hasNext()){
	    activeVIDsInfo += userItr.next().toString();
	    if (userItr.hasNext())
	      activeVIDsInfo += ",";
	  }
	  if (itr.hasNext())
	    activeVIDs += "/";
	    activeVIDsInfo += "/";
	}
	
	reply.putAttribute(SienaConstants.ACTIVE_VSIDS, activeVIDs);
	reply.putAttribute(SienaConstants.ACTIVE_VSIDS_INFO, activeVIDsInfo);
      }
      publishNotification(reply);      

    } else if (event.getAttribute(SienaConstants.JOIN_NEW_VSID) != null){
      System.out.println("got event to join a new vsid");
      String videoName = event.getAttribute(SienaConstants.VIDEO_NAME).stringValue();
      String uid = event.getAttribute(SienaConstants.UID).stringValue();
      String gid = event.getAttribute(SienaConstants.GID).stringValue();
      String date = event.getAttribute(SienaConstants.VIDEO_DATE).stringValue();

      String newID = _server.joinNewVSID(videoName, uid, gid, date);
      Notification reply = new Notification();
      reply.putAttribute(SienaConstants.JOIN_NEW_VSID_REPLY, newID);
      publishNotification(reply);

    } else if (event.getAttribute(SienaConstants.JOIN_ACTIVE_VSID) != null){
      System.out.println("got event to join an active vsid");
      String vsid = event.getAttribute(SienaConstants.VSID).stringValue();
      String uid = event.getAttribute(SienaConstants.UID).stringValue();
      String gid = event.getAttribute(SienaConstants.GID).stringValue();

      _server.joinActiveVSID(vsid, uid, gid);

    } else if (event.getAttribute(SienaConstants.REMOVE_USER_FROM_VSID) != null){
      System.out.println("got event to remove a user from a vsid");
      String vsid = event.getAttribute(SienaConstants.VSID).stringValue();
      String uid = event.getAttribute(SienaConstants.UID).stringValue();
      String gid = event.getAttribute(SienaConstants.GID).stringValue();
      _server.removeUserFromVSID(vsid, uid, gid);
      
    } else if (event.getAttribute(SienaConstants.PLAY) != null){
      String vsid = event.getAttribute(SienaConstants.VSID).stringValue();
      String uid = event.getAttribute(SienaConstants.UID).stringValue();
      String gid = event.getAttribute(SienaConstants.GID).stringValue();
      long startTime = event.getAttribute(SienaConstants.ABS_TIME_SENT).longValue();
      _server.playPressed(vsid, uid, gid, startTime);
      
    } else {
      System.err.println("Notification Error, received unknown event");
    }
  }

  /**
   * send out a message to the workgroup to start playing, and to set
   * the start time back to this time (fast forward the client).
   *
   * @param vsid: id of the video session
   * @param uid: user id
   * @param gid: group id
   * @param startTime: absolute system time that the video session had
   * actualy started
   */
  void sendPlay(String vsid, String uid, String gid, long startTime){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_VIDEO_ACTION, SienaConstants.PLAY);
    event.putAttribute(SienaConstants.ABS_TIME_SENT, startTime);
    event.putAttribute(SienaConstants.UID, uid);
    event.putAttribute(SienaConstants.GID, gid);
    event.putAttribute(SienaConstants.VSID, vsid);
    publishNotification(event);
  }

  /**
   * handle the actual publishing to the Siena server
   *
   * @param event: Notification to publish
   */
  private void publishNotification(Notification event){
    try{
      System.out.println("publishing event: " + Calendar.getInstance().getTime() + ": " + event);
      _siena.publish(event);
    } catch (siena.SienaException e){
      System.err.println("CommController publishing sienaException: " + e);
    }  
  }

  /**
   * @return CommController's active value (thread is active if true)
   */
  boolean isActive() { return _isActive; }

  /**
   * Set the CommController active value
   *
   * @param flag: CommController active value (thread is active if true)
   */
  void setActive(boolean flag) {
    _isActive = flag;
  }
}
