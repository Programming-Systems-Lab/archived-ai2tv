package psl.ai2tv.gauge;

import siena.*;
import siena.comm.*;

import java.util.Hashtable;
import java.io.IOException;

import org.apache.log4j.Logger;


/**
	Basic Siena-based event receiver for the gauge.
	Abstract since the implementation of @see notify() is left to subclasses
*/
public abstract class SimpleGaugeSubscriber implements GaugeSubscriber {

    private static final Logger logger = Logger.getLogger(SimpleGaugeSubscriber.class);

    private Filter gaugeFilter;
	private Siena mainSiena;
	
		/** Siena and subscriptions initialization */
	protected void setup() 
		throws SienaException, IOException {
            String server = System.getProperty("ai2tv.server");
            if (server != null) {

                mainSiena = new ThinClient(server);

                //logger.debug("Connnected to server at " + ((ThinClient)mainSiena).getServer());
            }
            else {
                mainSiena = new HierarchicalDispatcher();
                ((HierarchicalDispatcher) mainSiena).setReceiver(new KAPacketReceiver(sienaPort));
	            logger.debug ("Siena Server Up: " + new String(((HierarchicalDispatcher) mainSiena).getReceiver().address()));
            }

	    Filter startFilter = new Filter();
	    startFilter.addConstraint("Start", Op.ANY, (String)null);
	    mainSiena.subscribe(startFilter, this);
	    
	    gaugeFilter = new Filter();
		gaugeFilter.addConstraint("FRAME", Op.EQ, "frame_ready");	
	    mainSiena.subscribe(gaugeFilter, this);

        Filter filter = new Filter();
        filter.addConstraint("AI2TV_FRAME", "");
        mainSiena.subscribe(filter, this);
	}

    public void notify(Notification s[]) {
    	// I never subscribe for patterns anyway. 
    }

}