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
 * each function has access to everything in assets/properties.def
 *
 * inParams = 
 * (in findBase) "clients" = vector of ClientAsset(s) 
 * (in evalClient,adaptClient) "clients" = ClientAsset(s) 
 * "baseClient" = ClientAsset (of base)
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
  public static final double FRAME_RATE_INCREASE = 1.3;

  // these two vars are used to allow for a long term 
  // adjustment, to let the effects of an adjustment 
  // act on the clients before we readjust
  public static final double REPORT_THRESHOLD = 10;
  private int _numReports;

  private WFGauge _myGauge = WFSubscriber.myGauge;
  private Siena _siena;
  private FrameIndexParser _fip;
  private FrameDesc[][] _allFrames;
  private double[] _avgBandwidthNeeded;

  /**
   * 
   */
  public WFHelperFunctions(){
    _fip = _myGauge.getFrameIndexParser();
    _allFrames = _fip.frameData();
    _avgBandwidthNeeded = _fip.getAvgBandwidthNeeded();
    _numReports = 0;
    try {
      _siena = SimpleGaugeSubscriber.getSiena();
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
      ; // evaluateClients(inParams, outParams);
    else if (method.equals("AdaptClient"))
      ; // adaptClient(inParams, outParams);
    else
      throw new Exception("WFHelperFunctions Exception: unknown method name " + method);
  }

  /**
   * This method computes the base clients that other client will be
   * measured against.  
   *
   * Currently, the base is a client who has been missing frames or
   * the lowest bandwidth client.
   *
   * Also, notice that I am changing the clientPG's here.
   *
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  private void findBase(Hashtable inParams, Hashtable outParams)
    throws Exception {

    /*
    logger.debug("- - - - - - - - - - - - - - - - - - - - ");
    logger.debug("- - - - - WF Helper Functions: findBase - - - - - \n\n");
    */

    ClientAsset baseCA = (ClientAsset) inParams.get("baseClient");
    NewClientPG basePG = (NewClientPG) baseCA.getClientPG();
    Vector clients = (Vector) inParams.get("clients");

    ++_numReports;
    double lowestBandwidth = 99999;
    int frameRate = 0;
    int penalties = 0;
    double bandwidth = 0;
    double ratio = 0;
    int level = 0;
    int prefetchedFrames = 0;

    // first we check to see if anybody's missed any frames and what the lowest 
    // level client is
    ClientAsset clientCA = null;
    ClientPG clientPG = null;
    
    for (int i = 0; i < clients.size(); i++) {
      clientCA = (ClientAsset) clients.get(i);
      clientPG = (ClientPG) clientCA.getClientPG();
      bandwidth = clientPG.getBandwidth();
      level = clientPG.getLevel();
      penalties = clientPG.getPenalties();

      /*
      logger.debug("frame rate: " + clientPG.getFrameRate());
      logger.debug("bandwidth: " + bandwidth);
      logger.debug("level: " + level);
      */

      if (level == LOWEST_LEVEL && bandwidth < lowestBandwidth){
	lowestBandwidth = bandwidth;

	if (penalties > 0) {
	  ratio = bandwidth / _avgBandwidthNeeded[level];
	  if (ratio <= 0)
	    ratio = DEFAULT_FRAME_RATE_ADJUSTMENT;
	  
	  /*
	  logger.debug("ratio: " + ratio);	  
	  logger.debug("frame rate: " + clientPG.getFrameRate());
	  logger.debug("NEW FRAME RATE: " + ((int)(ratio * clientPG.getFrameRate())));
	  */
	  basePG.setFrameRate((int)(ratio * clientPG.getFrameRate()));
	  basePG.setAdapt(true);
	  
	} else if (clientPG.getPrefetchedFrames() > PREFETCH_CHANGE_THRESHOLD &&
		   (_numReports%REPORT_THRESHOLD) == 0){
	  basePG.setFrameRate((int)(frameRate * FRAME_RATE_INCREASE));
	  basePG.setAdapt(true);
	}
      }
    }
    /*
    if (basePG.getAdapt())
      logger.debug("findBase found adapt==true, newFrameRate: "+basePG.getFrameRate());
    logger.debug("\n\n- - - - - - - - - - - - - - - - - - - - ");
    */
  }


  /**
   * Evaluate the clients (tasks spawned in parallel)
   *
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  private void evaluateClients(Hashtable inParams, Hashtable outParams) {
    /*
    logger.debug("- - - - - - - - - - - - - - - - - - - - ");
    logger.debug("- - - - - WF Helper Functions: evalClient - - - - - \n\n");
    */

    ClientAsset baseCA = (ClientAsset) inParams.get("baseClient");
    ClientPG basePG = (ClientPG) baseCA.getClientPG();
    ClientAsset clientCA = (ClientAsset) inParams.get("clients");
    NewClientPG clientPG = (NewClientPG) clientCA.getClientPG();
    NewFramePG clientFramePG = (NewFramePG)clientCA.getFramePG();
    String adaptMethod = null;

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - -  Adjustments made to clients all clients! - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    if (basePG.getAdapt()){
      clientPG.setAdapt(true);
      clientPG.setFrameRate(basePG.getFrameRate());
      adaptMethod = SienaConstants.CHANGE_FRAME_RATE + ",";
    }
    
    int clientLevel = clientPG.getLevel();
    int cacheLevel = clientPG.getCacheLevel();
    int penalties = clientPG.getPenalties();
    double timeOffset = (double)clientFramePG.getTimeOffset()/1000;
    int prefetchedFrames = clientPG.getPrefetchedFrames();
    int reserveFrames = clientPG.getReserveFrames();

    /*
    logger.debug("clientLevel: " + clientLevel);
    logger.debug("cacheLevel: " + cacheLevel);
    logger.debug("penalties: " + penalties);
    logger.debug("prefetched: " + prefetchedFrames);
    logger.debug("reserveFrames: " + reserveFrames);
    */

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - Adjustments made to clients who are BAD! - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    if (penalties > 0 || timeOffset > OFFSET_THRESHOLD || reserveFrames == RESERVE_THRESHOLD){
      clientPG.setAdapt(true);      
      if (clientLevel == LOWEST_LEVEL){
	// logger.debug("!!! client is too slow, must skip frames !!!");
	FrameDesc fd = computeNextDownload(clientLevel, clientFramePG.getNum(),
				 clientPG.getBandwidth(), 
				 clientPG.getAvgDistWF2Client());
	adaptMethod += SienaConstants.JUMP_TO + ",";
	clientFramePG.setNum(fd.getNum());
	
      } else {
	// logger.debug("!!! client is too slow!  setting client/cache DOWN a level !!!");
	clientPG.setLevel(clientLevel + 1);
	clientPG.setCacheLevel(clientLevel + 1);
	adaptMethod += SienaConstants.CHANGE_CLIENT_LEVEL + "," + 
	  SienaConstants.CHANGE_CACHE_LEVEL + ",";
      }
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //
    // - - - - - - - - Adjustments made to clients who are GOOD! - - - - - - - - //
    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - //

    } else if (clientLevel == cacheLevel && prefetchedFrames >= PREFETCH_THRESHOLD){
      clientPG.setAdapt(true);      
      // logger.debug("!!! client is WAY FAST!  setting client CACHE UP a level !!!");
      adaptMethod += SienaConstants.CHANGE_CACHE_LEVEL + ",";
      clientPG.setCacheLevel(cacheLevel - 1);
    } else if (clientLevel > cacheLevel && prefetchedFrames >= PREFETCH_CHANGE_THRESHOLD) {
      clientPG.setAdapt(true);      
      // logger.debug("!!! client's cache is ready.  Setting client UP a level !!!");
      adaptMethod += SienaConstants.CHANGE_CLIENT_LEVEL + ",";
      clientPG.setLevel(cacheLevel);
    }

    // logger.debug("\n\n- - - - - - - - - - - - - - - - - - - - ");
  }


  /**
   * Adapt the clients according to the input paramters (tasks spawned
   * in parallel)
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
  private void adaptClient(Hashtable inParams, Hashtable outParams) {  
    /*
    logger.debug("- - - - - - - - - - - - - - - - - - - - ");
    logger.debug("- - - - - WF Helper Functions: adaptClient - - - - - \n\n");
    */
    ClientAsset clientCA = (ClientAsset) inParams.get("client");
    ClientPG clientPG = (ClientPG) clientCA.getClientPG();
    FramePG clientFramePG = (FramePG) clientCA.getFramePG();

    Notification event = null;
    if (clientPG.getAdapt()){
      event = new Notification();

      String adaptMethod = clientPG.getAdaptMethod();
      // logger.debug("adaptClient methods: " + adaptMethod);
      if (adaptMethod.indexOf(SienaConstants.CHANGE_FRAME_RATE) != -1){
	event.putAttribute(SienaConstants.CHANGE_FRAME_RATE, clientPG.getFrameRate());
      }
      if (adaptMethod.indexOf(SienaConstants.JUMP_TO) != -1){
	event.putAttribute(SienaConstants.JUMP_TO, clientFramePG.getNum());
      }
      if (adaptMethod.indexOf(SienaConstants.CHANGE_CLIENT_LEVEL) != -1){
	event.putAttribute(SienaConstants.CHANGE_CLIENT_LEVEL, clientPG.getLevel());

      }
      if (adaptMethod.indexOf(SienaConstants.CHANGE_CACHE_LEVEL) != -1){
	event.putAttribute(SienaConstants.CHANGE_CACHE_LEVEL, clientPG.getCacheLevel());
      }

      event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
      logger.info("sending event: " + event);

      try {
	_siena.publish(event);
      } catch (siena.SienaException e) {
	System.err.println("Error in WF, AdaptClient Seina Publishing: " + e);
      }
    }
    
    // logger.debug("\n\n- - - - - - - - - - - - - - - - - - - - ");
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
    
    long now = _myGauge.clock.currentTime();
    // first we fast-forward to the index of the current frame that 
    // should be showing next
    int j=0;
    FrameDesc fd = null;

    for (; j<_allFrames[level].length; j++){
      fd = _allFrames[level][j];
      if (now > ((double) fd.getStart() * 1000 / 30))
	break;
    }

    if (fd == null)
      return null;
    // disregard this for now
    // we just ff-ed to the current frame that should be showing, so 
    // we want to see if we can get the next frame in line.
    // FrameDesc fd = _allFrames[level][++j];      

    // the timeNeeded calculation is :
    // (bytes / kbytes/sec) == (kbytes / kbytes/millisec) = ms needed
    double timeNeeded = fd.getSize() / bandwidth;
    while ((now + timeNeeded + (overhead * 1000)) > fd.getStart() 
	   && j<_allFrames[level].length){
      fd = _allFrames[level][++j];  
      timeNeeded = fd.getSize() / bandwidth;
    }
    return fd;
  }

}
