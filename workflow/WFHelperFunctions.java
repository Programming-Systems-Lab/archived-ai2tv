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
 * method holds the method that this class will execute.  I have
 * declared all variables used in any of the functions
 *
 * each function has access to:
 *
 * client
 * ClientPG clientPG = clientAsset.getClientPG()
 * clientPG.getSampleTime(), .getBandwith()
 * clientAsset.getFramePG()
 * bandwi
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

  public static final int HIGHEST_LEVEL = 0;
  public static final int LOWEST_LEVEL = 4;
  public static final int PREFETCH_THRESHOLD = 4;
  public static final int PREFETCH_CHANGE_THRESHOLD = 2;
  public static final int RESERVE_THRESHOLD = 2;
  public static final int OFFSET_THRESHOLD = 2000;
  public static final int DEFAULT_FRAME_RATE = 30;
  public static final double DEFAULT_FRAME_RATE_ADJUSTMENT = .5;


  private WFGauge myGauge = WFSubscriber.myGauge;
  private ClientAsset baseCA, clientAsset;
  private NewClientPG basePG;
  private NewClientPG clientPG, medianPG;
  private FramePG clientFramePG;
  private Vector clients;
  private Notification event;
  private Siena siena;
  private FrameIndexParser fip;
  private FrameDesc[][] allFrames;
  private double[] avgBandwidthNeeded;
  private int numLevels;
  
  private double lowestBandwidth; // used in FindBase
  private double bandwidth;       // used in FindBase
  private int level;              // used in FindBase  
  private double ratio;
  private int frameRate;
  private int newFrameRate;

  /**
   * 
   */
  public WFHelperFunctions(){
    fip = myGauge.getFrameIndexParser();
    allFrames = fip.frameData();
    numLevels = fip.levels();
    avgBandwidthNeeded = fip.getAvgBandwidthNeeded();
    baseCA = null;
    clientAsset = null;
    basePG = null;
    clientPG = null;
    clients = null;
    medianPG = null;
    clientFramePG = null;
    event = null;
    //changeFrameRate = false;
    newFrameRate = -1;
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
      findBase(inParams, outParams);
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

    logger.debug("- - - - - - - - - - - - - - - - - - - - ");
    logger.debug("- - - - - WF Helper Functions: findBase - - - - - \n\n");

    // changeFrameRate = false;
    baseCA = (ClientAsset) inParams.get("baseClient");
    basePG = (NewClientPG) baseCA.getClientPG();
    clients = (Vector) inParams.get("clients");

    // logger.debug("findBase: baseClient=" + baseCA + ", client=" + clients);

    lowestBandwidth = 99999;
    int baseIndex = -1;
    int lowestBWIndex = -1;

    // first we check to see if anybody's missed any frames and what the lowest 
    // level client is
    for (int i = 0; i < clients.size(); i++) {
      clientAsset = (ClientAsset) clients.get(i);
      clientPG = (NewClientPG) clientAsset.getClientPG();
      penalties = clientPG.getPenalties();
      bandwidth = clientPG.getBandwidth(); // switch to bytes/sec
      level = clientPG.getLevel(); // switch to bytes/sec

      logger.debug("frame rate: " + clientPG.getFrameRate());
      logger.debug("bandwidth: " + bandwidth);
      logger.debug("level: " + level);
      logger.debug("penalties: " + penalties);

      if (bandwidth < lowestBandwidth && level == LOWEST_LEVEL){
	lowestBandwidth = bandwidth;
	lowestBWIndex = i;	
	if (penalties > 0)
	  baseIndex = i;
      }
    }

    logger.debug("!!!!!! baseIndex: " + baseIndex + " lowestBWIndex: " + lowestBWIndex);
    // if there were no penalites
    if (baseIndex < 0 && lowestBWIndex >= 0 ){
      ; /*
      clientAsset = (ClientAsset) clients.get(lowestBWIndex);
      clientPG = (NewClientPG) clientAsset.getClientPG();
      level = clientPG.getLevel();
      if (clientPG.getPrefetchedFrames() > PREFETCH_CHANGE_THRESHOLD){
	if (lowestBandwidth < avgBandwidthNeeded[level]) {
	  // this frame rate will hopefully keep the client steady
	  newFrameRate = (int)(lowestBandwidth / avgBandwidthNeeded[level]) * DEFAULT_FRAME_RATE;
	} else {
	  // this frame rate is if the lowest bandwidth is greater than the client's avg need, so the
	  // ratio is greater than one, so the client should be fine at this frame rate
	  newFrameRate = DEFAULT_FRAME_RATE;
	}
	newFrameRate = (int)(clientPG.getFrameRate() + clientPG.getFrameRate() * 0.5);
	for (int i = 0; i < clients.size(); i++) {
	  clientAsset = (ClientAsset) clients.get(i);
	  clientPG = (NewClientPG) clientAsset.getClientPG();
	  clientPG.setAdapt(true);
	  clientPG.setNewFrameRate(newFrameRate);
	  }
	}
	*/ 
    } else if (baseIndex != -1){
      clientAsset = (ClientAsset) clients.get(baseIndex);
      clientPG = (NewClientPG) clientAsset.getClientPG();
      bandwidth = clientPG.getBandwidth();
      frameRate = clientPG.getFrameRate();
      level = clientPG.getLevel();

      if (bandwidth < avgBandwidthNeeded[level])
	ratio = bandwidth / avgBandwidthNeeded[level];
      else 
	ratio = DEFAULT_FRAME_RATE_ADJUSTMENT;

      logger.debug("the client's bandwith is: " + bandwidth);
      logger.debug("the calculated needed bandwith is: " + avgBandwidthNeeded[level]);
      logger.debug("the client's level is: " + level);
      logger.debug("the client's frame rate is: " + frameRate);
      logger.debug("the ratio is: " + ratio);
      // if (ratio > 0)
      // basePG.setFrameRate((int)(ratio * frameRate));
      // need to clone the clientpg here
      // basePG.setBandwidth(bandwidth);
      // basePG.setHost(clientPG.getHost());
      // basePG.setId(clientPG.getId());
      // basePG.setSampleTime(clientPG.getSampleTime());
      logger.debug("found a client that needs adjusting");
      logger.debug("new frame rate is: " + (int)(ratio * frameRate));

      // changeFrameRate = true;
      newFrameRate = (int)(ratio * frameRate); // basePG.getFrameRate();
      for (int i = 0; i < clients.size(); i++) {
	clientAsset = (ClientAsset) clients.get(i);
	clientPG = (NewClientPG) clientAsset.getClientPG();
	clientPG.setAdapt(true);
	clientPG.setNewFrameRate(newFrameRate);
      }
    }

    // logger.debug("Base (midway) client after: " + basePG);
    // logger.debug("frame rate: " + basePG.getFrameRate());
    // logger.debug("bandwidth: " + basePG.getBandwidth());
    logger.debug("\n\n- - - - - - - - - - - - - - - - - - - - ");
  }


  /**
   * Evaluate the clients with respect to the base.
   *
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  private void evaluateClientWrtBase(Hashtable inParams, Hashtable outParams) {

    /*
    baseCA = (ClientAsset) inParams.get("baseClient");
    basePG = (NewClientPG) baseCA.getClientPG();
    clientAsset = (ClientAsset) inParams.get("clients");
    clientPG = clientAsset.getClientPG();
    if (clientPG.getFrameRate() != basePG.getFrameRate()){
      // should I do this, and have clientPG = clientPGImpl or should I just pass the info within this class?
      // clientPG.setFrameRate((int)basePG.getFrameRate());
      
      // now we have to set all the clients to this new frame rate
      // ...but, how do we know if we should do this in the other 
      // function?
      changeFrameRate = true;
      newFrameRate = basePG.getFrameRate();
      // but we could be doing this above, and the per client comparision isn't needed.  we just 
      // need to know that the base client has a certain change, and we should adjust everybody.
      // there are no per client changes wrt to the base.
    }
    */
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
    clientPG = (NewClientPG) clientAsset.getClientPG();
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
    clientLevel = clientPG.getLevel();
    cacheLevel = clientPG.getCacheLevel();
    reserveFrames = clientPG.getReserveFrames();

    logger.debug("- - - - - - - - - - - - - - - - - - - - ");
    logger.debug("- - - - - WF Helper Functions: adaptClient - - - - - \n\n");
    // logger.debug("client's avg WF->client: " + clientPG.getAvgDistWF2Client());
    // logger.debug("frame's info: " + timeShown + ", " + timeOffset + ", " + timeDownloaded);
    // logger.debug("- - -");

    // logger.debug("frame start=" + start + ",end=" + end + ",timeShown=" + timeShown
    // + ", clientid=" + clientPG.getId());

    logger.debug("clientLevel: " + clientLevel);
    logger.debug("cacheLevel: " + cacheLevel);
    logger.debug("penalties: " + penalties);
    logger.debug("prefetched: " + prefetchedFrames);
    logger.debug("reserveFrames: " + reserveFrames);

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - -  Adjustments made to clients all clients! - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    logger.debug("CHANGE THE FRAME RATE? " + clientPG.getAdapt());
    if (clientPG.getAdapt()){
      logger.debug("Changing the frame rates to " + clientPG.getNewFrameRate());
      event = new Notification();
      event.putAttribute(SienaConstants.CHANGE_FRAME_RATE, clientPG.getNewFrameRate());
      clientPG.setAdapt(false);
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - Adjustments made to clients who are BAD! - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    if (penalties > 0 || timeOffset > OFFSET_THRESHOLD || reserveFrames == RESERVE_THRESHOLD){
      if (clientLevel > (LOWEST_LEVEL - 1)){
	logger.debug("!!! client is too slow, must skip frames !!!");
	if (event == null)
	  event = new Notification();
	// fd = computeNextDownload(clientLevel, clientFramePG.getNum(), 
	// clientPG.getBandwidth(), 
	// clientPG.getAvgDistWF2Client());
	// event.putAttribute(SienaConstants.JUMP_TO, fd.getNum());
      } else {
	logger.debug("!!! client is too slow!  setting client/cache DOWN a level !!!");
	if (event == null)
	  event = new Notification();
	// we want the cache and the client levels to be the same here.
	event.putAttribute(SienaConstants.CHANGE_CLIENT_LEVEL, (clientLevel + 1));
	event.putAttribute(SienaConstants.CHANGE_CACHE_LEVEL, (clientLevel + 1)); 
      }


    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - Adjustments made to clients who are GOOD! - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    } else if (clientLevel == cacheLevel && prefetchedFrames >= PREFETCH_THRESHOLD){
      logger.debug("!!! client is WAY FAST!  setting client CACHE UP a level !!!");
      if (event == null)
	event = new Notification();
      event.putAttribute(SienaConstants.CHANGE_CACHE_LEVEL, (cacheLevel - 1));
    } else if (clientLevel > cacheLevel && prefetchedFrames >= PREFETCH_CHANGE_THRESHOLD) {
      logger.debug("!!! client's cache is ready.  Setting client UP a level !!!");
      if (event == null)
	event = new Notification();
      event.putAttribute(SienaConstants.CHANGE_CLIENT_LEVEL, cacheLevel);
    }
      

    if (event != null) {
      event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
      // logger.info("sending event: " + event);
      try {
	logger.debug("publishing: " + event);
	siena.publish(event);
      } catch (siena.SienaException e) {
	System.err.println("Error in WF, AdaptClient Seina Publishing: " + e);
      }
    } else {
      // logger.debug("!!! client is normal, doing nothing !!!");      
    }
    logger.debug("\n\n- - - - - - - - - - - - - - - - - - - - ");
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
    // first we fast-forward to the index of the current frame that 
    // should be showing next
    int j=0;
    FrameDesc fd = null;

    for (; j<allFrames[level].length; j++){
      fd = allFrames[level][j];
      if (now > ((double) fd.getStart() * 1000 / 30))
	break;
    }

    if (fd == null)
      return null;
    // disregard this for now
    // we just ff-ed to the current frame that should be showing, so 
    // we want to see if we can get the next frame in line.
    // FrameDesc fd = allFrames[level][++j];      

    // the timeNeeded calculation is :
    // (bytes / kbytes/sec) == (kbytes / kbytes/millisec) = ms needed
    double timeNeeded = fd.getSize() / bandwidth;
    while ((now + timeNeeded + (overhead * 1000)) > fd.getStart() 
	   && j<allFrames[level].length){
      fd = allFrames[level][++j];  
      timeNeeded = fd.getSize() / bandwidth;
    }
    return fd;
  }

}
