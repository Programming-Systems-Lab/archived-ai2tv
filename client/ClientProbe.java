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

import psl.ai2tv.SienaConstants;
import psl.ai2tv.gauge.FrameDesc;
import psl.ai2tv.SienaConstants;

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
 * @version	$Revision$
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
  private double[] _probeTimes;
  private boolean[] _probeStatus;

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
    _probeTimes = new double[numProbes];
    _probeStatus = new boolean[numProbes];
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
  }

  /**
   * publish an update of the state of the client
   */
  private void sendUpdate(){
    _frameEvent.putAttribute(SienaConstants.AI2TV_FRAME, "");
    _frameEvent.putAttribute(SienaConstants.CLIENT_ID, _client.getID());
    _frameEvent.putAttribute(SienaConstants.BANDWIDTH, _client.getBandwidth());


    // this element must be the last one added, as we are doing a
    // timing measurement to find the distance to the WF.
    // _frameEvent.putAttribute(SienaConstants.PROBE_TIME, System.currentTimeMillis());
    // _frameEvent.putAttribute(SienaConstants.PROBE_TIME, _client.currentTime());
    // _frameEvent.putAttribute(SienaConstants.CLIENT_CURRENT_TIME, _client.currentTime());
    try {
      _mySiena.publish(_frameEvent);
    } catch (SienaException se) {
      se.printStackTrace();
    }
  }

  private void addFrameInfo(){
    FrameDesc fd = _client.getCurrFrame();
    if (fd != null) {
      _frameEvent.putAttribute(SienaConstants.LEFTBOUND, fd.getStart());
      _frameEvent.putAttribute(SienaConstants.RIGHTBOUND, fd.getEnd());
      _frameEvent.putAttribute(SienaConstants.MOMENT, fd.getNum());
      _frameEvent.putAttribute(SienaConstants.LEVEL, fd.getLevel());
      _frameEvent.putAttribute(SienaConstants.SIZE, fd.getSize());
      // 000
      // _frameEvent.putAttribute(SienaConstants.PROBE_TIME, fd.currentTime());
    }
  }

  /**
   * get the current time set by the probe
   *
   * @param ID: the ID of the probe
   * @return the time associated with given ID
   */
  double getTimeProbe(int ID){
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
  void startTimeProbe(int ID, double time){
    if (ID >= 0 && ID < _probeTimes.length){
      _probeTimes[ID] = time;
      setProbe(ID);
    }
  }


  /**
   * unsetting the probe causes a message to be sent.
   *
   * @param ID: the ID of the probe
   * @param time: ending time of probe
   * @param natureOfMessage: header to message to be sent that expounds upon
   * nature of this probe.
   */
  void endTimeProbe(int ID, double time, String natureOfMessage){
    // long diff = _time - _probeTimes[ID];
    if (ID >= 0 && ID < _probeTimes.length){
      unsetProbe(ID);
      Client.probeOutput.println("sending an update: time diff: " + (time - _probeTimes[ID]));
      // this is where TIME_SHOWN is put in
      _frameEvent.putAttribute(natureOfMessage, (time - _probeTimes[ID]));
      _frameEvent.putAttribute(SienaConstants.PROBE_TIME, (time - _probeTimes[ID]));

      if (natureOfMessage.equals(SienaConstants.TIME_SHOWN))
	addFrameInfo();
      sendUpdate();

      Client.probeOutput.println("image: " + _client.getCurrFrame().getNum() +
				 " shown at: " + _probeTimes[ID] +
				 " late: " + (time - _probeTimes[ID]) + " (ms)");
    }
  }


  /**
   * 
   *
   * @param ID: the ID of the probe
   * @param time: start of time associated with this probe
   */
  void setProbe(int ID){
    _probeStatus[ID] = true;
  }

  void unsetProbe(int ID){
    _probeStatus[ID] = false;
  }

  boolean getProbeStatus(int ID){
    return _probeStatus[ID];
  }


}

