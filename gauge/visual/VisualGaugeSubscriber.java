package psl.ai2tv.gauge.visual;

import siena.*;
import siena.comm.*;
import javax.swing.*;
import java.awt.*;

import java.util.Hashtable;
import java.io.IOException;

import psl.ai2tv.gauge.*;

/**
	Siena-based event receiver for the gauge.
	Updates also visual bars with progress
*/
class VisualGaugeSubscriber 
	 extends SimpleGaugeSubscriber {
	
	private VisualGauge theGauge;	
	
	public VisualGaugeSubscriber(VisualGauge gg) 
		throws SienaException, IOException {
		theGauge = gg;
		setup();
	}
	
	/**
		Callback when an event arrives. Used to manipulate the incoming event
	*/
    public void notify(Notification e) {
    	//System.out.println(e);
    	ClientBar currentBar;
    	String id = e.getAttribute("ClientID").stringValue();
    	Hashtable ht = theGauge.getGroupClients();
    	currentBar = (ClientBar) ht.get(id);
		if (currentBar == null) {
	       	ht.put (id, currentBar = new ClientBar(id, GaugeFrame.barMax));
	       	theGauge.getGaugeGUI().barPanel.add(currentBar.getClientPanel());
			theGauge.getGaugeGUI().barPanel.add(Box.createRigidArea(new Dimension(0,10)));
			//force refresh (some other way?)
			theGauge.getGaugeGUI().barPanel.setVisible(false);
			theGauge.getGaugeGUI().barPanel.setVisible(true);
			
			theGauge.getBucket().add(id);
	    }
    	
    	if (e.getAttribute("Start") != null) {
    		//new client up,, update the gauge GUI with its progress bar
    		System.out.println("Connecting Client - " + id);
    		long st = e.getAttribute("Start").longValue();
			// if this is the first client issuing a "Start", start the gauge thread
    		if (! theGauge.isRunning()) {
    			// nominal start time = 1st client start time
    			theGauge.setStartTime(st);
		    	currentBar.setStartTime(st);
	    		theGauge.startNominal();
	    	}     		
			else
				// normalize all the client start times
				currentBar.setStartTime(theGauge.getStartTime());
	    }
    	else {
    		
    		//normalize download time 
    		long t = e.getAttribute("probeTime").longValue() - theGauge.getStartTime();
    		 
    		currentBar.setFrame(e.getAttribute("leftbound").intValue(),
    								e.getAttribute("moment").intValue(),
    								e.getAttribute("rightbound").intValue(),
    								t,
    								e.getAttribute("level").intValue());
			JProgressBar jBar= currentBar.getBar();
			int progress = (int)e.getAttribute("rightbound").longValue();
			if (theGauge.getGaugeGUI().showProgress) {
				jBar.setValue(progress);
				jBar.setString(Integer.toString(progress));
			}
		}	
            
	}

}