/*
 * @(#)FauxWF.java
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
import java.util.Vector;

import siena.*;
import psl.ai2tv.SienaConstants;

/**
 * Stub WF to test out WF functions.
 *
 * TODO: need to add in the measure of the distance of the WF to Probe
 * and vice versa.
 */
class FauxWF extends Thread implements Notifiable {
  public static PrintStream _log;
  private boolean _isActive;
  private ThinClient _mySiena;
  private String _sienaServer;
  private Filter filter;
  private int _earlyThreshold;
  private int _lateThreshold;

  // these are needed to get the avg and stddev for stuff
  private Vector _allTimes;
  private Vector _distanceProbe2WF;
  private Vector _distanceWF2Probe;

  FauxWF(String sienaServer){

    try {
      _log = new PrintStream(new  FileOutputStream(new File("WF.log")), true);
    } catch (FileNotFoundException e){
      e.printStackTrace();
    }

    _isActive = false;
    _mySiena = null;
    _sienaServer = sienaServer;

    _earlyThreshold = -1000;  // threshold for max ms of being early
    _lateThreshold = 1000;  // threshold for min ms of being late

    _distanceProbe2WF = new Vector();
    _distanceWF2Probe = new Vector();

    setupSienaListener();
  }

  public void notify(Notification e) {
    handleNotification(e);
  }

  public void notify(Notification [] s) { }

  private void handleNotification(Notification event){
    long receivedTime = System.currentTimeMillis();
    String name = event.toString().substring(7).split("=")[0];
    AttributeValue attrib = event.getAttribute(name);
    long clientID;

    // System.out.println("name: " + name + " attrib:" + attrib);
    if (name.equals(SienaConstants.AI2TV_FRAME)){
      clientID = event.getAttribute(SienaConstants.CLIENT_ID).longValue();

      int currFrame = event.getAttribute(SienaConstants.MOMENT).intValue(); // DEBUG
      int leftbound = event.getAttribute(SienaConstants.LEFTBOUND).intValue(); // DEBUG
      double bandwidth = event.getAttribute(SienaConstants.BANDWIDTH).doubleValue(); // DEBUG
      int level = event.getAttribute(SienaConstants.LEVEL).intValue();
      long timeShown = event.getAttribute(SienaConstants.TIME_SHOWN).longValue();

      // first we see if the level needs to be changed.
      // Client.debug.println("currFrame: (leftbound) attribute: " + leftbound);
      // Client.debug.println("currFrame: (moment) attribute: " + currFrame);
      _allTimes.add(new Double(timeShown));

      // note that instead of doing three passes, we could just do 
      // one pass for all three stats, but that would remove the 
      // generality from the calcStats method

      _log.println("- - - stats - - - ");
      _log.print(" earlyTimes: ");
      calcStats(_allTimes, "negative");
      _log.print(" lateTimes: ");
      calcStats(_allTimes, "positive");
      _log.print(" all times: ");
      calcStats(_allTimes, "none");
      _log.println("- - - done: stats - - - ");

      if (timeShown < _earlyThreshold)
	changeLevel("UP", clientID);
      else if (timeShown < _earlyThreshold)      
	changeLevel("DOWN", clientID);

      sendEcho(clientID);

    } else if (name.equals(SienaConstants.AI2TV_WF_ECHO_REPLY)){
      clientID = event.getAttribute(SienaConstants.CLIENT_ID).longValue();
      /*
      long wf2probe = event.getAttribute("WF2Probe").longValue();
      long probe2wf = event.getAttribute("Probe2WF").longValue();
      _distanceProbe2WF.add(new Double(timeReceived - probe2wf));
      _distanceWF2Probe.add(new Double(wf2probe));
      long timeSent = event.getAttribute("probeTime").longValue();
      System.out.println(" distance Probe to WF: " + (timeReceived - timeSent) + "(ms)");
      */


    } else {
      System.out.println("Error: NOT equal to AI2TV_FRAME name");
    }
  }

  private void sendEcho(long clientID){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_WF_ECHO, "");
    event.putAttribute(SienaConstants.CLIENT_ID, clientID);
    event.putAttribute(SienaConstants.SENT_TIME, System.currentTimeMillis());
    publishNotification(event);    
  }

  private void gotoFrame(int newFrame, long clientID){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_FRAME_UPDATE, "");
    event.putAttribute(SienaConstants.CLIENT_ID, clientID);
    event.putAttribute(SienaConstants.GOTO, newFrame);
    publishNotification(event);
  }

  private void changeLevel(String dir, long clientID){
    Notification event = new Notification();
    event.putAttribute(SienaConstants.AI2TV_FRAME_UPDATE, "");
    event.putAttribute(SienaConstants.CLIENT_ID, clientID);
    event.putAttribute(SienaConstants.CHANGE_LEVEL, dir);
    publishNotification(event);
  }

  private void setupFilter() throws siena.SienaException {
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_FRAME, "");
    _mySiena.subscribe(filter, this);
  }

  private void publishNotification(Notification event){
    try{
      _mySiena.publish(event);
    } catch (siena.SienaException e){
      System.err.println("CommController publishing sienaException: " + e);
    }  
  }

  private void setupSienaListener(){
    try {
      _mySiena = new ThinClient(_sienaServer);
      setupFilter();

    } catch (siena.comm.PacketSenderException e) {
      // what is this?
      System.out.println ("Caught exception in setting up the Siena server: "  + e);
    } catch (SienaException e) {
      System.out.println ("Cannot connect to Siena bus: "  + e);
      _isActive = false;
      // e.printStackTrace();
    }
  }

  /**
   * calculates avg and stddev of given vector of doubles.  
   * 
   * note: since our data set is complete, we can use the one pass
   * trick, instead of having the sample standard deviation being
   * smaller than the data set standard deviation by a factor of the
   * square root of (n-1)/n.
   *
   */
  private void calcStats(Vector data, String type){
    int dataSize = data.size();
    double avg;
    double squaredAvg;

    double sum = 0;
    double squaredSum = 0;
    double temp;

    _log.println("");
    for (int i=0; i<dataSize; i++){
      temp = ((Double) data.get(i)).doubleValue();
      if ( (type.equals("positive") && temp > 0) || 
	   (type.equals("negative") && temp < 0) || 
	   type.equals("none")){
	sum += temp;
	squaredSum += temp * temp;
	_log.println(" " + temp);
      }
    }
    avg = sum / dataSize;
    squaredAvg = squaredSum / dataSize;
    double variance = squaredAvg - (avg*avg);
    _log.println(" avg: " + avg + " (ms) +/- " + 
		 java.lang.Math.sqrt(variance) + "(ms)");
  }

  public void run(){
    _isActive = true;
    while(_isActive){
      try {
	sleep(10000);
      } catch (InterruptedException e){
	System.out.println("FauxWF error: " + e);
      }
    }
  }

  public static void main(String[] args) {
    if(args.length != 1) {
      System.err.println("Usage: FauxWF <server-address>");
      System.exit(1);
    }
    FauxWF wf = new FauxWF(args[0]);
    wf.start();
  }
}


