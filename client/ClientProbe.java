/*
 * @(#)ClientProbe.java
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

import psl.ai2tv.gauge.FrameDesc;

/**
 * Very simple probing architecture.  Set a probe with a certain time.
 * Later in the future, unset the probe with another time and a message. 
 * The difference in times (second - first) along with the message gets 
 * sent to the server.
 *
 * Manages probing by watching the "set" value of each probe and
 * sending a message when a probe is unset.  The objective of this
 * class is to measure the timing of certain events.
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class ClientProbe {
  /** interval between probe events in ms.' */
  ThinClient _mySiena;
  private Notification _frameEvent;
  private Client _client;
  String _sienaServer;

  private int _probeIndex;

  // for speed, i'm going to try using an array first
  // private Vector _probes; //
  private long[] _probeTimes;

  /**
   * create a ClientProbe
   *
   * @param c: associated client
   * @param sienaServer: location of the Siena server
   * @param numProbes: max number of probes that are going to be set
   */
  ClientProbe (Client c, String sienaServer, int numProbes) {
    _client = c;
    _mySiena = null;
    _sienaServer = sienaServer;
    setupSiena();
    
    _probeIndex = 0;
    _probeTimes = new long[numProbes];
    for (int i=0; i<_probeTimes.length; i++)
      _probeTimes[i] = 0;
  }

  /**
   * Connect to the Siena server and setup some minor details
   */
  private void setupSiena() {
    try {
      _mySiena = new ThinClient(_sienaServer);
    } catch (InvalidSenderException ise) {
      Client.out.println ("Cannot connect to Siena bus");
      _mySiena = null;
      ise.printStackTrace();	
    }
    // trying to optimize by calling constructors for events only once
    _frameEvent = new Notification();

    // ask peppo if it really makes a difference if we add these now...
    /*
    _frameEvent.putAttribute("AI2TV_FRAME", "frame_ready");
    _frameEvent.putAttribute("CLIENT_ID", _client.getID());
    _frameEvent.putAttribute("leftbound", 0);	
    _frameEvent.putAttribute("rightbound", 0);
    _frameEvent.putAttribute("moment", 0);
    _frameEvent.putAttribute("level", -1); 
    _frameEvent.putAttribute("probeTime", 0);
    */
  }

  /**
   * publish an update of the state of the client 
   */
  private void sendUpdate(){
    FrameDesc fd = _client.getCurrFrame();
    Notification event = new Notification();
    if (fd != null) {
      // Client.out.println("ClientProbe sending update: " + fd);
      //Client.out.println ("Sending Frame info");
      _frameEvent.putAttribute("AI2TV_FRAME", "");
      _frameEvent.putAttribute("CLIENT_ID", _client.getID());
      _frameEvent.putAttribute("leftbound", fd.getStart());
      _frameEvent.putAttribute("rightbound", fd.getEnd());
      _frameEvent.putAttribute("moment", fd.getNum());
      // _frameEvent.putAttribute("timeShown", _client.getTimeCurrFrameShown());
      _frameEvent.putAttribute("level", fd.getLevel());
      _frameEvent.putAttribute("bandwidth", _client.getBandwidth());
      _frameEvent.putAttribute("probeTime", System.currentTimeMillis());

      try { 
	_mySiena.publish(_frameEvent);
      } catch (SienaException se) {
	se.printStackTrace();	
      }

    }
  }

  /**
   * get the current time set by the probe
   * 
   * @param ID: the ID of the probe
   * @return the time associated with given ID
   */
  long getTimeProbe(int ID){
    if (ID >= 0 && ID < _probeTimes.length)
      return _probeTimes[ID];
    return -1;
  }

  /**
   * set the probe's time start
   *
   * @param ID: the ID of the probe
   * @param time: start of time associated with this probe
   */
  void startTimeProbe(int ID, long time){
    if (ID >= 0 && ID < _probeTimes.length)
      _probeTimes[ID] = time;    
  }

  /**
   * unsetting the probe causes a message to be sent.
   *
   * @param ID: the ID of the probe
   * @param time: ending time of probe
   * @param natureOfMessage: header to message to be sent that expounds upon 
   * nature of this probe.
   */
  void endTimeProbe(int ID, long time, String natureOfMessage){
    // long diff = _time - _probeTimes[ID];
    if (ID >= 0 && ID < _probeTimes.length){
      Client.debug.println("sending an update: time diff: " + (time - _probeTimes[ID]));
      _frameEvent.putAttribute(natureOfMessage, (time - _probeTimes[ID]));
      sendUpdate();
    }
  }

}

