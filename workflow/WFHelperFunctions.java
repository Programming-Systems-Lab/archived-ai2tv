package psl.ai2tv.workflow;

/*
 * @(#)WFHelperFunctions.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City
 * of New York.  All Rights Reserved
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

import java.util.*;

import psl.workflakes.littlejil.ExecutableTask;
import psl.ai2tv.workflow.assets.*;
import siena.*;
import org.apache.log4j.Logger;
import psl.ai2tv.SienaConstants;
import psl.ai2tv.gauge.SimpleGaugeSubscriber;
import psl.ai2tv.workflow.WFGauge;
import psl.ai2tv.workflow.WFSubscriber;
import psl.ai2tv.gauge.FrameIndexParser;
import psl.ai2tv.gauge.FrameDesc;


/**
 * Implemented interface that can be used by Little-JIL tasks via the
 * TaskExecutorInternalPlugin.  The "method" paramter in the execute
 * method holds the method that this class will execute.
 *
 * each function has access to:
 *
 * client
 * ClientPG clientPG = clientAsset.getClientPG()
 * clientPG.getSampleTime(), .getBandwith()
 * clientAsset.getFramePG()
 *
 * framePG.getEnd(), getLevel(), getNum(), getStart()
 *
 * for Evaluate and AdaptClient, the parameter is also "clients" for the
 * one client to work on... that's because that resource is used to
 * iterate and create one EvaluateClient task, for each element in the
 * clients vector...
 *
 * for Eval and Adapt it's just a ClientAsset object, even though the param is called "clients"
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
public class WFHelperFunctions implements ExecutableTask {

  private static final Logger logger = Logger.getLogger(WFHelperFunctions.class);

  private WFGauge myGauge = WFSubscriber.myGauge;
  private ClientAsset baseCA, clientAsset;
  private NewClientPG basePG;
  private ClientPG clientPG, medianPG;
  private FramePG clientFramePG;
  private Vector clients;
  private Notification event;
  private Siena siena;
  private FrameIndexParser fip;
  private FrameDesc[][] allFrames;
  private int numLevels;

  public final int HIGHEST_LEVEL = 0;
  public final int LOWEST_LEVEL = 4; 
  public final int PREFETCH_THRESHOLD = 4;
  public final int PREFETCH_CHANGE_THRESHOLD = 2;
  public final int RESERVE_THRESHOLD = 2;
  public final int OFFSET_THRESHOLD = 2000;

  /**
   * 
   */
  public WFHelperFunctions(){
    fip = myGauge.getFrameIndexParser();
    allFrames = fip.frameData();
    numLevels = fip.levels();
    baseCA = null;
    clientAsset = null;
    basePG = null;
    clientPG = null;
    clients = null;
    medianPG = null;
    clientFramePG = null;
    event = null;
    try {
      siena = SimpleGaugeSubscriber.getSiena();
    } catch (siena.SienaException e) {
      System.err.println("Error in WFHelperFunctions, could not get Siena server: " + e);
    } catch (java.io.IOException e) {
      System.err.println("Error in WFHelperFunctions, could not get Siena server: " + e);
    }
  }

  /**
   * method to be executed by the WF/Little-Jil tasks
   *
   * @param method the method name to execute. Note that this is up
   * to the class to interpret, and does not necessarily map to any
   * real methods.
   * @param inParams input parameters to the task, as defined in the Little-JIL diagram.
   * @param outParams optional output parameters that should be
   * copied-out of this task, as defined in the Little-JIL diagram.
   * @throws Exception if an error occurred.
   */
  public void execute(String method, Hashtable inParams, Hashtable outParams)
    throws Exception {
    if (method.equals("FindBase"))
      ; // findBase(inParams, outParams);
    else if (method.equals("EvaluateClient"))
      ; // evaluateClientWrtBase(inParams, outParams);
    else if (method.equals("AdaptClient"))
      adaptClient(inParams, outParams);
    else
      throw new Exception("WFHelperFunctions Exception: unknown method name " + method);
  }


  /**
   * This method computes the base clients that other client will be
   * measured against.
   *
   * For the first attempt we will try the client which is the
   * median amongst all the clients.  The only criteria available
   * at this time is the bandwidth.
   *
   * Note that this sorting, if executed and used often, should
   * Be done upon insert into the Container in the BB.
   *
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  private void findBase(Hashtable inParams, Hashtable outParams)
    throws Exception {

    // Currently, I don't use the base client in the adaptClient
    // function, so I've commented the following out to bump up the
    // efficiency measures
    /*
    baseCA = (ClientAsset) inParams.get("baseClient");
    basePG = (NewClientPG) baseCA.getClientPG();
    clients = (Vector) inParams.get("clients");

    logger.debug("findBase: baseClient=" + baseCA + ", client=" + clients);

    // here is where the clients get sorted by bandwidths
    SortedMap m = new TreeMap();
    for (int i = 0; i < clients.size(); i++) {
      clientAsset = (ClientAsset) clients.get(i);
      clientPG = clientAsset.getClientPG();
      m.put(new Double(clientPG.getBandwidth()), clientPG);
    }

    // here I simply get the index of the middle (median) client
    Collection vals = m.values();
    Iterator i = vals.iterator();
    for (int j = 0; j < vals.size() / 2; j++) {
      i.next();
    }

    medianPG = (ClientPG) i.next();
    basePG.setBandwidth(medianPG.getBandwidth());
    basePG.setHost(medianPG.getHost());
    basePG.setId(medianPG.getId());
    basePG.setSampleTime(medianPG.getSampleTime());

    logger.debug("Base (midway) client is: " + basePG);
    */
  }


  /**
   * Evaluate the clients with respect to the base.
   *
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  private void evaluateClientWrtBase(Hashtable inParams, Hashtable outParams) {
    // not currently used
  }

  /**
   * Adapt the clients according to the input paramters
   * 
   * Adaptation policies: 
   * - if the client is bad
   *   as measured by 1) showing the frame too late
   *		      2) missed frames (penalties)
   * 
   * 1) bump down a level
   * 2) check if the client has any penalties (right now that only means    
   *    that it missed a frame)
   *    : drop down a level, if at level 5 (lowest), then skip to
   *    the next frame that the client can possibly download in
   *    time.
   * 
   * - if the client is good
   *   as measured by 1) number of pre-fetched frames, bump up his quality level
   * 
   * - otherwise, client is normal, leave him alone.
   *
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  double timeOffset = -1;
  int penalties = -1;
  int prefetchedFrames = -1;
  int clientLevel = -1;
  int cacheLevel = -1;
  int reserveFrames = -1;
  FrameDesc fd = null;

  private void adaptClient(Hashtable inParams, Hashtable outParams) {  
    clientAsset = (ClientAsset) inParams.get("clients");
    clientPG = clientAsset.getClientPG();
    clientFramePG = clientAsset.getFramePG();

    // we don't use the base currently
    // baseCA = (ClientAsset) inParams.get("baseClient");
    // basePG = (NewClientPG) baseCA.getClientPG();
    
    // logger.debug("findBase: baseClient=" + baseCA + ", client=" + clientPG);
    // double end = (double)clientFramePG.getEnd()/30;
    // double start = (double)clientFramePG.getStart()/30;
    // double timeShown = (double)clientFramePG.getTimeShown()/1000;
    // double timeDownloaded = (double)clientFramePG.getTimeDownloaded()/1000;

    event = null;
    timeOffset = (double)clientFramePG.getTimeOffset()/1000;
    penalties = clientPG.getPenalties();
    prefetchedFrames = clientPG.getPrefetchedFrames();
    clientLevel = clientFramePG.getLevel();
    cacheLevel = clientPG.getCacheLevel();
    reserveFrames = clientPG.getReserveFrames();

    // logger.debug("- - - - - - - - - - - - - - - - - - - - ");
    // logger.debug("- - - - - WF Helper Functions - - - - - ");
    // logger.debug("");
    // logger.debug("");
    // logger.debug("client's avg WF->client: " + clientPG.getAvgDistWF2Client());
    // logger.debug("frame's info: " + timeShown + ", " + timeOffset + ", " + timeDownloaded);
    // logger.debug("- - -");

    // logger.debug("frame start=" + start + ",end=" + end + ",timeShown=" + timeShown
    // + ", clientid=" + clientPG.getId());

    // logger.debug("clientLevel: " + clientLevel);
    // logger.debug("cacheLevel: " + cacheLevel);
    // logger.debug("penalties: " + penalties);
    // logger.debug("prefetched: " + prefetchedFrames);
    // logger.debug("reserveFrames: " + reserveFrames);

    if (penalties > 0 || timeOffset > OFFSET_THRESHOLD || reserveFrames == RESERVE_THRESHOLD){
      if (clientLevel > (LOWEST_LEVEL - 1)){
	// logger.debug("!!! client is too slow, must skip frames !!!");
	event = new Notification();
	event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
	event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
	fd = computeNextDownload(clientLevel, clientFramePG.getNum(), 
					   clientPG.getBandwidth(), 
					   clientPG.getAvgDistWF2Client());
	event.putAttribute(SienaConstants.JUMP_TO, fd.getNum());
      } else {
	// logger.debug("!!! client is too slow, setting client down a level !!!");
	event = new Notification();
	// addHeader(event, clientPG);
	event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
	event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
	event.putAttribute(SienaConstants.CHANGE_CLIENT_LEVEL_DOWN, "");
      }

    } else if (clientLevel == cacheLevel && prefetchedFrames >= PREFETCH_THRESHOLD){
      // logger.debug("!!! client is WAY FAST!  setting client CACHE UP a level !!!");
      event = new Notification();
      // addHeader(event, clientPG);
      event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
      event.putAttribute(SienaConstants.CHANGE_CACHE_LEVEL_UP, "" );
    } else if (clientLevel != cacheLevel && prefetchedFrames >= PREFETCH_CHANGE_THRESHOLD) {
      // logger.debug("!!! client's cache is ready.  Setting client UP a level !!!");
      event = new Notification();
      // addHeader(event, clientPG);
      event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
      event.putAttribute(SienaConstants.CHANGE_CLIENT_LEVEL_UP, "" );
    }

    if (event != null) {
      // logger.info("sending event: " + event);
      try {
	siena.publish(event);
      } catch (siena.SienaException e) {
	System.err.println("Error in WF, AdaptClient Seina Publishing: " + e);
      }
    } else {
      // logger.debug("!!! client is normal, doing nothing !!!");      
    }
    // logger.debug("");
    // logger.debug("");
    // logger.debug("- - - - - - - - - - - - - - - - - - - - ");
  }

  /**
   * add the header information to the adjustment event
   */
  private void addHeader(Notification event, ClientPG clientPG){
    event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
    event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
  }

  /**
   * Compute the next frame to download given the bandwidth resources
   * and WF2Client (plus any other) overhead.
   * 
   * note: to increase efficiency of this function, I take the
   * currentTime() sparingly, thus some of the comparisons in this
   * function are not as accurate as they could be.
   * 
   * note:
   * ? should we try to get it by the frame start or frame end?
   * one issue is whether we want to sacrifice one frame in hopes
   * that after we get the frame after that, we should be able to
   * get all other frames on time.  The possible error is that after
   * we skip this one frame, we might get the next one after that
   * but then have to skip the subsequent one.  Though, this error
   * is present in the next suggestion as well.
   * OR
   * we could see if we can get this frame before the end of its
   * semantic limits.  that way it can at least show it part of the
   * time in which it is relevant.
   * 
   * right now we are going with plan A (skip this one and get the
   * first one where we can download by the start of its semantic
   * window.
   * 
   * 
   * @param level: hierarchy level of the client
   * @param frameNum: current frameNum that the client is at
   * @param bandwidth: bandwidth of the client
   * @param overhead: propagation overhead of getting the message to the client (in seconds)
   * @return FrameDesc to jump to
   */
  private FrameDesc computeNextDownload(int level, int frameNum, double bandwidth, double overhead){
    
    long now = myGauge.clock.currentTime();
    // first we fast-forward to the index of the current from that 
    // should be showing now.
    int j=0, i=0;
    FrameDesc fd;
    for (; i<numLevels; i++, j++){
      fd = allFrames[i][j];
      if (now < ((double) fd.getStart() * 1000) / 30)
	break;
    }

    // we just ff-ed to the current frame that should be showing, so 
    // we want to see if we can get the next frame in line.
    fd = allFrames[i][++j];      

    // the timeNeeded calculation is :
    // (bytes / kbytes/sec) == (kbytes / kbytes/millisec) = ms needed
    double timeNeeded = fd.getSize() / bandwidth;
    while ((now + timeNeeded + (overhead * 1000)) > fd.getStart()){
      fd = allFrames[i][++j];  
      timeNeeded = fd.getSize() / bandwidth;
    }
    return fd;
  }

}
