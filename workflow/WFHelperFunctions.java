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

import psl.workflakes.littlejil.ExecutableTask;
import psl.workflakes.littlejil.assestExecutableTask;

/**
 * Implemented interface that can be used by Little-JIL tasks via the
 * TaskExecutorInternalPlugin.  The "method" paramter in the execute
 * method holds the method that this class will execute
 *
 *
 *
 *
 * ? matias, what kinds of objects, etc can be passed through the hash?
 * ? are these functions the actual 
 *
 *
 *
 *
 *
 *
 * each function has: 
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
 *
 *
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class WFHelperFunctions implements ExecutableTask{

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
    throws Exception
  {
    if (method.equals("FindBase"))
      FindBase(inParams, outParams); 
    else if (method.equals("EvaluateClientWrtBase"))
      EvaluateClientWrtBase(inParams, outParams);
    else if (method.equals("AdaptClient"))
      AdaptClient(inParams, outParams);
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
  private void FindBase(Hashtable inParams, Hashtable outParams) 
    throws Exception
  {
    ClientAsset bc = inParams.get("baseClient");
    ClientPG baseClient = bc.getClientPG();

    Vector clients = inParams.get("clients");

    ClientPG clientAsset;
    ClientPG clientPG;
    SortedMap m = new TreeMap();
    for (int i=0; i<clients.size(); i++){
      clientAsset = (ClientAsset) clients.get(i);
      clientPG = clientAsset.getClientPG();
      m.put(new Double(clientPG.getBandwith()), clientPG);
    }

    Collection vals =  m.values();
    Iterator i = vals.iterator();
    for (int j=0; j<vals.size()/2; j++){
      i.next();
    }    
    baseClient = i.next();
    System.out.println("Base (midway) client is: " + baseClient);
  }


  /**
   * Evaluate the clients with respect to the base.
   *
   * @param inParams: hash of input paramters 
   * @param outParams: hash of output paramters 
   */
  private void EvaluateClientWrtBase(Hashtable inParams, Hashtable outParams) 
  {
    /*
     * need to ask peppo about the logic here.  so why are we
     * evaluating and adapting in two separate clients?  if 
     * we evaluate a client to be defecient, do we then adjust him
     * here, or how do we flag that he needs to change, and how do 
     * we specify the type of change needed?
     */
  }

  /**
   * Adapt the clients according to the input paramters
   *
   * @param inParams: hash of input paramters 
   * @param outParams: hash of output paramters 
   */
  private void AdaptClient(Hashtable inParams, Hashtable outParams)
  {
    ClientAsset c = inParams.get("clients");
    ClientPG currentClient = c.getClientPG();
    FramePG currentBaseFrame = c.getFramePG();

    ClientAsset bc = inParams.get("baseClient");
    ClientPG baseClient = bc.getClientPG();
    FramePG currentClientFrame = bc.getFramePG();
    
    // hopefully, though I need to verify with peppo, 
    // the FramePG is the current frame.

    // how do we get to the equivalent classes 
    // function? 

    // right now we'll just see if the frame the other clients 
    // are viewing is at the right time, if not, we'll 
    // try to drop it down a hierarchy, see if that doesn't 
    // help (though I don't think it will.

    // !!! need to add the cilentID to the wf info
    Notification event = null;
    if (currentClientFrame.getEnd() <= baseClient.getSampleTime()){
      event = new Notification();
      event.putAttribute("AI2TV_FRAME_UPDATE", "");
      event.putAttribute("CLIENT_ID", clientAsset.getClientID());
      event.putAttribute("CHANGE_LEVEL", "DOWN");
    } else if (currentClientFrame.getStart() >= baseClient.getSampleTime()){
      event = new Notification();
      event.putAttribute("AI2TV_FRAME_UPDATE", "");
      event.putAttribute("CLIENT_ID", clientAsset.getClientID());
      event.putAttribute("CHANGE_LEVEL", "UP");
    }

    /*
     * ?? is there a way to just pass in the Siena client?  in this
     * way, we'd just have the siena client persistent and use the
     * same proxy.  otherwise, I think we're incurring some overhead
     * here of recreating the client each time.
     */

    if (event != null) {
      try{
	// need to set this in the environment
	ThinClient _mySiena = new ThinClient(System.getProperty("ai2tv.server"));
	_mySiena.publish(event);
      } catch (siena.SienaException e){
	System.err.println("Error in WF, AdaptClient Seina Publishing: " + e);
      }  
    }
  }
}
