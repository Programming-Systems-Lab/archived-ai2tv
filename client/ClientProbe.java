/*
 * @(#)ClientProbe.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Giuseppe Valetto
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

import psl.ai2tv.gauge.FrameDesc;

/**
 * Probes the client for status updates
 */
class ClientProbe implements Runnable {
  /** interval between probe events in ms.' */
  private long interval;
  private boolean loopAlways = true;
  private boolean active; 
  ThinClient mySiena;
  private Notification frameEvent;
  private Client _client;
  private CacheController cache;

  ClientProbe (Client c) {
    _client = c;
    active = false;
    mySiena = null;
    cache = null;
    setupSiena();
  }

  public void run() {
    System.out.println("ClientProbe running !!! ");
    loopAlways = true;
    active = true;
    while (loopAlways) {
      if (active) {
	try {
	  Thread.currentThread().sleep(interval);		
	} catch (InterruptedException ie) {
	  //do nothing
	}
      }
      //send probe message
      emit(_client.getCurrFrame());
    }

  }


  private void setupSiena() {
    try {
      // mySiena = new ThinClient("udp:localhost:4444");
      mySiena = new ThinClient("ka:localhost:4444");
    } catch (InvalidSenderException ise) {
      System.out.println ("Cannot connect to Siena bus");
      mySiena = null;
      loopAlways = false;
      active = false;
      ise.printStackTrace();	
    }
    // trying to optimize by calling constructors for events only once
    frameEvent = new Notification();
    frameEvent.putAttribute("AI2TV_FRAME", "frame_ready");
    frameEvent.putAttribute("ClientID", _client.getID());
    frameEvent.putAttribute("leftbound", 0);	
    frameEvent.putAttribute("rightbound", 0);
    frameEvent.putAttribute("moment", 0);
    frameEvent.putAttribute("level", -1); 
    frameEvent.putAttribute("probeTime", 0);
  }

  private void emit (FrameDesc fd) {
    if (fd != null) {
      System.out.println("ClientProbe sending update: " + fd);
      //System.out.println ("Sending Frame info");
      //update only necessary fields
      frameEvent.putAttribute("AI2TV_FRAME", "");
      frameEvent.putAttribute("leftbound", fd.getStart());
      frameEvent.putAttribute("rightbound", fd.getEnd());
      frameEvent.putAttribute("moment", fd.getNum());
      frameEvent.putAttribute("level", fd.getLevel());
      frameEvent.putAttribute("bandwidth", _client.getBandwidth());
      frameEvent.putAttribute("probeTime", System.currentTimeMillis());
      try { 
	mySiena.publish(frameEvent);
      } catch (SienaException se) {
	se.printStackTrace();	
      }
    }
  }

  void setTarget(CacheController cc) { cache = cc; }
  void setProbingFrequency (long f) {interval = f; }

  void stopProbe() { loopAlways = false; }

  public void setActive(boolean flag) {
    active = flag;
  }

  public boolean isActive() { return active; }

  /**
   * shutdown the thread
   */
  public void shutdown() { 
    loopAlways = false; 
    active = false; 
  }
}
