package psl.ai2tv.workflow;

import siena.*;
import siena.comm.*;

import java.util.Hashtable;
import java.io.IOException;
import siena.SienaException;

import psl.ai2tv.gauge.*;

class WFSubscriber extends SimpleGaugeSubscriber {
	
	private WFGauge myGauge;
	
	public WFSubscriber(WFGauge wfg) 
		throws SienaException, IOException {
		myGauge = wfg;	
		setup();
	}

	public void notify(Notification e) {
		ClientDesc currentClient;
		
    	String id = e.getAttribute("ClientID").stringValue();
    	Hashtable ht = myGauge.getGroupClients();	
    	currentClient = (ClientDesc)ht.get(id);
    	if (currentClient == null) {
    		ht.put(id, currentClient = new ClientDesc(id));
    		myGauge.getBucket().update(id, currentClient);	
    	}
    	
    	if (e.getAttribute("Start") != null) {
    		//new client up, update the gauge  with its info
    		System.out.println("Connecting Client - " + id);
    		long st = e.getAttribute("Start").longValue();
    		// if this is the first client issuing a "Start", start the gauge thread
    		if (! myGauge.isRunning()) {
    			// nominal start time = 1st client start time
    			myGauge.setStartTime(st);
    			currentClient.setStartTime(st);
	    		myGauge.startNominal();
	    	}     		
			else {
				// normalize all the client start times
				currentClient.setStartTime(myGauge.getStartTime());
			}
    	}
    	else {
    		//normalize download time 
    		long t = e.getAttribute("probeTime").longValue() - myGauge.getStartTime();
    		 
    		currentClient.setFrame(e.getAttribute("leftbound").intValue(),
    								e.getAttribute("moment").intValue(),
    								e.getAttribute("rightbound").intValue(),
    								t,
    								e.getAttribute("level").intValue());
    	}
	}
	
}