package psl.ai2tv.gauge;

import siena.*;
import siena.comm.*;

import java.util.Hashtable;
import java.io.IOException;


/**
	Basic Siena-based event receiver for the gauge.
	Abstract since the implementation of @see notify() is left to subclasses
*/
public abstract class SimpleGaugeSubscriber implements GaugeSubscriber {
	private Filter gaugeFilter;
	private HierarchicalDispatcher mainSiena;
	
		/** Siena and subscriptions initialization */
	protected void setup() 
		throws SienaException, IOException {
	    mainSiena = new HierarchicalDispatcher();
	    mainSiena.setReceiver(new UDPPacketReceiver(sienaPort));
	    System.out.println ("Siena Server Up: " + new String(mainSiena.getReceiver().address()));

	    Filter startFilter = new Filter();
	    startFilter.addConstraint("Start", Op.ANY, (String)null);
	    mainSiena.subscribe(startFilter, this);
	    
	    gaugeFilter = new Filter();
		gaugeFilter.addConstraint("FRAME", Op.EQ, "frame_ready");	
	    mainSiena.subscribe(gaugeFilter, this);
	}

    public void notify(Notification s[]) {
    	// I never subscribe for patterns anyway. 
    } 
		
}