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
  private CacheController _cache;
  private Viewer _viewer;

  public static final int DEBUG = 0;

  public static PrintStream out = System.out;
  public static PrintStream err = System.err;

  private boolean _isActive = false;
  private AI2TVJNIJava _JNIIntf;
  private ThinClient _mySiena;
  // private HierarchicalDispatcher _mySiena;

  private Filter filter;

  String _mySienaServer;

  CommController(String id, String sienaServer){
    this(id, sienaServer, null, null, null);
  }
    
  CommController(String id, String sienaServer, AI2TVJNIJava intf, Viewer v, CacheController c){
    probeDelay = 10000;
    _isActive = true;
    _JNIIntf = intf;
    clientID = id;
    _mySiena = null;
    _cache = c;
    setupSienaListener();
    _mySienaServer = sienaServer;
    Thread mainThread = new Thread(this);
    mainThread.start();
  }

  private void setupFilter(){
    filter = new Filter();
    /*
    filter.addConstraint("AI2TV_VIDEO_ACTION", "UP_LEVEL");   // form the sync WF: go to the higher hierarchy
    filter.addConstraint("AI2TV_VIDEO_ACTION", "DOWN_LEVEL"); // form the sync WF: go to the lower hierarchy
    */
    // don't quite understand filters, tried the other constraints
    // thinking they were parallel, but it looks like they are additive.
    filter.addConstraint("AI2TV_VIDEO_ACTION", "PLAY");	   // from other clients: start playing
    // filter.addConstraint("AI2TV_VIDEO_ACTION", "PAUSE");	   // from other clients: pause playing
    // filter.addConstraint("AI2TV_VIDEO_ACTION", "STOP");	   // from other clients: stop playing
  }

  private void setupSienaListener(){
    try {
      // _mySiena = new HierarchicalDispatcher();
      // _mySiena.setMaster(_mySienaServer);
      _mySiena = new ThinClient("ka:localhost:4444");

      setupFilter();
      System.out.println("subscribing for " + filter.toString());
      _mySiena.subscribe(filter, this);

      // } catch (IOException e) {
      // System.out.println ("Caught exception in setting up the Siena server: "  + e);
    } catch (siena.comm.PacketSenderException e) {
      ; // what is this?
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
    System.out.println("I just got this event:");
    System.out.println(e.toString());
  };

  public void notify(Notification [] s) { }

  // void setTarget(CacheController cc) { cache = cc; }
  void setProbingFrequency (long f) {probeDelay = f; }
  void stopProbe() { loopAlways = false; }
  boolean isActive() { return _isActive; }
  void setActive(boolean flag) {
    _isActive = flag;
  }
	

  public static void main(String args[]){
    if(args.length != 1) {
      System.err.println("Usage: CommController <server-address>");
      System.exit(1);
    }
    CommController foo = new  CommController("foobar", args[0]);
  }
}
