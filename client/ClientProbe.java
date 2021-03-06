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
  ThinClient _siena;
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
    _siena = null;
    _sienaServer = sienaServer;
    setupSiena();

    _probeIndex = 0;
    _probeTimes = new double[numProbes];
    _probeStatus = new boolean[numProbes];
    for (int i=0; i<_probeTimes.length; i++)
      _probeTimes[i] = 0;

    // send out a registration packet to the WF
    sendRegistrationToWF();
  }

  /**
   * Connect to the Siena server and setup some minor details
   */
  private void setupSiena() {
    try {
      _siena = new ThinClient(_sienaServer);
    } catch (InvalidSenderException ise) {
      Client.out.println ("Cannot connect to Siena bus");
      _siena = null;
      ise.printStackTrace();
    }
  }

  /**
   * publish an update of the state of the client
   */
  private void sendUpdate(Notification event){
    event.putAttribute(SienaConstants.AI2TV_FRAME, "");
    event.putAttribute(SienaConstants.LEVEL, _client.getLevel());
    event.putAttribute(SienaConstants.CACHE_LEVEL, _client.getCacheLevel());
    event.putAttribute(SienaConstants.BANDWIDTH, _client.getBandwidth());
    event.putAttribute(SienaConstants.FRAME_RATE, _client.getFrameRate());
    event.putAttribute(SienaConstants.CLIENT_RESERVE_FRAMES, _client.getReserveFrames());
    event.putAttribute(SienaConstants.PREFETCHED_FRAMES, _client.getNumPrefetchedFrames(_client.getCacheLevel()));

    event.putAttribute(SienaConstants.UID, _client.getUID());
    event.putAttribute(SienaConstants.GID, _client.getGID());
    event.putAttribute(SienaConstants.ABS_TIME_SENT, System.currentTimeMillis());

    try {
      _siena.publish(event);
    } catch (SienaException se) {
      se.printStackTrace();
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
      Notification event = new Notification();
      // 999
      if (natureOfMessage.equals(SienaConstants.AI2TV_FRAME_MISSED)){
	Client.debug.println("CLIENT PROBE SENDING MISSED FRAME: " + _client.currentTime());
      }
      Client.probeOutput.println("sending an update: time diff: " + (time - _probeTimes[ID]));
      int diff = (int)(time - _probeTimes[ID]);
      event.putAttribute(natureOfMessage, diff);
      event.putAttribute(SienaConstants.PROBE_TIME, diff);

      if (natureOfMessage.equals(SienaConstants.TIME_OFFSET))
	addFrameInfo(event, diff);
      sendUpdate(event);

      // Client.probeOutput.println("image: " + _client.getCurrentFrame().getNum() +
      // " shown at: " + _probeTimes[ID] +
      // " late: " + (time - _probeTimes[ID]) + " (ms)");
    }
  }

  /**
   * add the FrameDesc information
   */
  private void addFrameInfo(Notification event, int timeOffset){
    FrameDesc fd = _client.getCurrentFrame();

    if (fd != null) {
      fd.setTimeOffset(timeOffset);
      event.putAttribute(SienaConstants.LEFTBOUND, fd.getStart());
      event.putAttribute(SienaConstants.RIGHTBOUND, fd.getEnd());
      event.putAttribute(SienaConstants.MOMENT, fd.getNum());
      // this was conflicting with the client's level
      // event.putAttribute(SienaConstants.LEVEL, fd.getLevel());
      event.putAttribute(SienaConstants.SIZE, fd.getSize());
      event.putAttribute(SienaConstants.TIME_SHOWN, fd.getTimeShown());
      event.putAttribute(SienaConstants.TIME_OFFSET, timeOffset);
      event.putAttribute(SienaConstants.TIME_DOWNLOADED, fd.getTimeDownloaded());
    }
  }

  /**
   * 
   *
   * @param ID: the ID of the probe
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

  private void sendRegistrationToWF(){
    Notification event = new Notification();
    try{
      Client.out.println("Registering client: " + _client.getUID() + "@" + _client.getGID());
      event.putAttribute(SienaConstants.AI2TV_WF_REG, "");
      event.putAttribute(SienaConstants.UID, _client.getUID());
      event.putAttribute(SienaConstants.GID, _client.getGID());
      _siena.publish(event);
    } catch (siena.SienaException e){
      Client.err.println("CommController publishing sienaException: " + e);
    }      
  }
}

