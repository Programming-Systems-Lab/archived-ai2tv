package psl.ai2tv.workflow;

import psl.ai2tv.gauge.*;

import java.util.Iterator;

import org.apache.log4j.Logger;

class WFGauge extends GroupGauge {

    private static final Logger logger = Logger.getLogger(WFGauge.class);

    private GaugeLDMPlugIn LDMHandle = null;

    public void setLDMHandle(GaugeLDMPlugIn pi) {
        LDMHandle = pi;
    }

    // overrides of GroupGauge

    protected GaugeComm setupCommunications()
            throws CommunicationException {
        GaugeSubscriber subscriber;
        try {
            subscriber = new WFSubscriber(this);
        } catch (Exception e) {
            subscriber = null;
            throw new CommunicationException(e);
        }
        return subscriber;
    }

    protected Thread defineNominalClient() {
        return new Thread() {
            boolean cont = true;

            public void run() {
                running = true;
                while (cont) {
                    try {
                        // gauge sampling interval
                        sleep((int) nomInterval);
                    } catch (InterruptedException ie) {
                    }
                    //nomProgress is the time elapsed since client start time
                    progress = (int) (System.currentTimeMillis() - startTime);
                    // in the time elapsed (in secs.) 30 frames per second have been nominally shown
                    nomProgress = (int) (30 * progress / GroupGauge.SAMPLE_INTERVAL);
                    //logger.debug("nominalClientThread calling evaluate progress");
                    try {
                        evaluateStatus(progress);
                    }
                    catch (Exception e) {
                        logger.error("Exception in evaluateStatus: " + e);
                    }
                }
                running = false;
            }
        };
    }

    protected void fillBucket(long elapsed) {
        Iterator allClients = groupClients.keySet().iterator();
        while (allClients.hasNext()) {
            String id = (String) allClients.next();
            ClientDesc cd = (ClientDesc) groupClients.get(id);
            FrameDesc fd = cd.getFrame();
            long t = fd.getDownloadedTime();

            // update the bucket is the time of the last info about a client
            // is within the time of the last sample and this moment
            if (t <= elapsed && t > bucket.getTime()) {
                logger.debug("updating bucket for client " + id);
                bucket.update(id, cd);
            }
            else {
                logger.debug("NOT updating bucket for client " + id);
                logger.debug("t=" + t + ", elapsed=" + elapsed + ", bucket.getTime()=" + bucket.getTime());
            }
        }

        bucket.setTime(elapsed);
    }


    protected void evaluateStatus(long elapsed) {
        fillBucket(elapsed);
        publishStatus();
        //clearStatus();
    }


    protected void publishStatus() {
        //pass it to LDM plugin for publication
        //System.out.println (bucket);
        LDMHandle.setReport(bucket);
    }

    protected FrameIndexParser setFrameInfo() {
        FrameIndexParser ret = super.setFrameInfo();

        //now compute equivalent frames for each frame
        EquivClasses ec = new EquivClasses(ret);
        ec.computeAllEquivalents(0);
        return ret;
    }

}