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
import siena.Notification;
import siena.ThinClient;
import org.apache.log4j.Logger;
import psl.ai2tv.SienaConstants;


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
public class WFHelperFunctions implements ExecutableTask {

    private static final Logger logger = Logger.getLogger(WFHelperFunctions.class);


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
        ClientAsset bc = (ClientAsset) inParams.get("baseClient");
        NewClientPG basePG = (NewClientPG) bc.getClientPG();

        Vector clients = (Vector) inParams.get("clients");

        logger.debug("findBase: baseClient=" + bc + ", client=" + clients);

        ClientAsset clientAsset;
        ClientPG clientPG;
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

        ClientPG median = (ClientPG) i.next();
        basePG.setBandwidth(median.getBandwidth());
        basePG.setHost(median.getHost());
        basePG.setId(median.getId());
        basePG.setSampleTime(median.getSampleTime());

        logger.debug("Base (midway) client is: " + basePG);
    }


    /**
     * Evaluate the clients with respect to the base.
     *
     * @param inParams: hash of input paramters
     * @param outParams: hash of output paramters
     */
    private void evaluateClientWrtBase(Hashtable inParams, Hashtable outParams) {
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
    private void adaptClient(Hashtable inParams, Hashtable outParams) {
        ClientAsset clientAsset = (ClientAsset) inParams.get("clients");
        ClientPG currentClient = clientAsset.getClientPG();
        FramePG currentClientFrame = clientAsset.getFramePG();

        ClientAsset bc = (ClientAsset) inParams.get("baseClient");
        ClientPG baseClient = bc.getClientPG();


        logger.debug("findBase: baseClient=" + baseClient + ", client=" + currentClient);

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
        int end = currentClientFrame.getEnd()/30;
        int start = currentClientFrame.getStart()/30;

        logger.debug("frame start=" + start + ",end=" + end + ",sampleTime=" + baseClient.getSampleTime()
                        + ", clientid=" + currentClient.getId());
        if (end <= baseClient.getSampleTime()) {
            event = new Notification();
            event.putAttribute(SienaConstants.AI2TV_FRAME_UPDATE, "");
            event.putAttribute(SienaConstants.CLIENT_ID, currentClient.getId());
            event.putAttribute(SienaConstants.CHANGE_LEVEL, SienaConstants.CHANGE_LEVEL_DOWN);
        } else if (start >= baseClient.getSampleTime()) {
            event = new Notification();
            event.putAttribute(SienaConstants.AI2TV_FRAME_UPDATE, "");
            event.putAttribute(SienaConstants.CLIENT_ID, currentClient.getId());
            event.putAttribute(SienaConstants.CHANGE_LEVEL, SienaConstants.CHANGE_LEVEL_UP);
        }

        /*
         * ?? is there a way to just pass in the Siena client?  in this
         * way, we'd just have the siena client persistent and use the
         * same proxy.  otherwise, I think we're incurring some overhead
         * here of recreating the client each time.
         */
        if (event != null) {
            logger.info("sending event: " + event);
            try {
                // need to set this in the environment
                ThinClient _mySiena = new ThinClient(System.getProperty("ai2tv.server"));
                _mySiena.publish(event);
            } catch (siena.SienaException e) {
                System.err.println("Error in WF, AdaptClient Seina Publishing: " + e);
            }
        }
    }
}
