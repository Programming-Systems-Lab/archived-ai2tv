package psl.ai2tv.workflow;

import psl.ai2tv.gauge.ClientDesc;
import psl.ai2tv.gauge.SimpleGaugeSubscriber;

import siena.Notification;
import siena.SienaException;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.log4j.Logger;

class WFSubscriber extends SimpleGaugeSubscriber {

    private static final Logger logger = Logger.getLogger(WFSubscriber.class);

	private WFGauge myGauge;
	
	public WFSubscriber(WFGauge wfg) 
		throws SienaException, IOException {
		myGauge = wfg;	
		setup();
	}

	public void notify(Notification e) {
		ClientDesc currentClient;

        logger.debug("received " + e);

    	String id = String.valueOf(e.getAttribute("CLIENT_ID").longValue());
    	Hashtable ht = myGauge.getGroupClients();	
    	currentClient = (ClientDesc)ht.get(id);
    	if (currentClient == null) {
    		ht.put(id, currentClient = new ClientDesc(id));
    		myGauge.getBucket().update(id, currentClient);	
    	}
    	
    	/*if (e.getAttribute("Start") != null) {
    		//new client up, update the gauge  with its info
    		System.out.println("Connecting Client - " + id);
    		long st = e.getAttribute("Start").longValue();
    		// if this is the first client issuing a "Start", start the gauge thread*/
    		if (! myGauge.isRunning()) {
    			// nominal start time = 1st client start time
    			long st = System.currentTimeMillis();
                myGauge.setStartTime(st);
    			currentClient.setStartTime(st);
	    		logger.debug("starting nominal");
                myGauge.startNominal();
	    	}     		
		/*	else {
				// normalize all the client start times
				currentClient.setStartTime(myGauge.getStartTime());
			}
    	}
    	else */{
    		//normalize download time
    		long t = e.getAttribute("probeTime").longValue() - myGauge.getStartTime();

    		currentClient.setFrame(e.getAttribute("leftbound").intValue(),
    								e.getAttribute("moment").intValue(),
    								e.getAttribute("rightbound").intValue(),
    								t,
    								e.getAttribute("level").intValue(),
                                    e.getAttribute("size").intValue());
    	}
	}
	
}