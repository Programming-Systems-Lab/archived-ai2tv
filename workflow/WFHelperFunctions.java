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

  ClientAsset baseCA, clientAsset;
  NewClientPG basePG;
  ClientPG clientPG, medianPG;
  FramePG clientFramePG;
  Vector clients;
  Notification event;
  Siena siena;

  /**
   * 
   */
  public WFHelperFunctions(){
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
      findBase(inParams, outParams);
    else if (method.equals("EvaluateClient"))
      evaluateClientWrtBase(inParams, outParams);
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

    baseCA = (ClientAsset) inParams.get("baseClient");
    basePG = (NewClientPG) baseCA.getClientPG();
    clients = (Vector) inParams.get("clients");

    logger.debug("findBase: baseClient=" + baseCA + ", client=" + clients);

    SortedMap m = new TreeMap();
    for (int i = 0; i < clients.size(); i++) {
      clientAsset = (ClientAsset) clients.get(i);
      clientPG = clientAsset.getClientPG();
      m.put(new Double(clientPG.getBandwidth()), clientPG);
    }

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
   * @param inParams: hash of input paramters
   * @param outParams: hash of output paramters
   */
  private void adaptClient(Hashtable inParams, Hashtable outParams) {
    clientAsset = (ClientAsset) inParams.get("clients");
    clientPG = clientAsset.getClientPG();
    clientFramePG = clientAsset.getFramePG();

    baseCA = (ClientAsset) inParams.get("baseClient");
    basePG = (NewClientPG) baseCA.getClientPG();

    // logger.debug("findBase: baseClient=" + baseCA + ", client=" + clientPG);

    // dp2041: how do we get to the equivalent classes
    // function?
    event = null;
    double end = (double)clientFramePG.getEnd()/30;
    double start = (double)clientFramePG.getStart()/30;
    double timeShown = (double)clientFramePG.getTimeShown()/1000;
    double timeOffset = (double)clientFramePG.getTimeOffset()/1000;
    double timeDownloaded = (double)clientFramePG.getTimeDownloaded()/1000;

    logger.debug("- - -");
    logger.debug("client's avg WF->client: " + clientPG.getAvgDistWF2Client());
    logger.debug("frame's info: " + timeShown + ", " + timeOffset + ", " + timeDownloaded);
    logger.debug("- - -");

    logger.debug("frame start=" + start + ",end=" + end + ",timeShown=" + timeShown
		 + ", clientid=" + clientPG.getId());
	
    

    int threshold = 2000;
    if (timeShown > 2000 && timeShown < 15000) {
      event = new Notification();
      event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
      event.putAttribute(SienaConstants.CHANGE_LEVEL, SienaConstants.CHANGE_LEVEL_DOWN);
    } else if ( timeShown < 200) {
      event = new Notification();
      event.putAttribute(SienaConstants.AI2TV_CLIENT_ADJUST, "");
      event.putAttribute(SienaConstants.CLIENT_ID, clientPG.getId());
      event.putAttribute(SienaConstants.CHANGE_LEVEL, SienaConstants.CHANGE_LEVEL_UP);
    }

    if (event != null) {
      logger.info("sending event: " + event);
      try {
	siena.publish(event);
      } catch (siena.SienaException e) {
	System.err.println("Error in WF, AdaptClient Seina Publishing: " + e);
      }
    }
  }
}
