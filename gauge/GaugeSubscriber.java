package psl.ai2tv.gauge;

import siena.*;
import javax.swing.*;
import java.awt.*;

class GaugeSubscriber implements Notifiable {
	private Filter gaugeFilter;
	private GaugeFrame GUI;
	
	
	public GaugeSubscriber(GaugeFrame GUI) {
		this.GUI = GUI;
		gaugeFilter = new Filter();
		gaugeFilter.addConstraint("FRAME", Op.EQ, "frame_ready");	
	}
	
	Filter getGaugeFilter() { return gaugeFilter; }
	
    public void notify(Notification e) {
    	//System.out.println(e);
    	ClientBar currentBar;
    	String id = e.getAttribute("ClientID").stringValue();
    	currentBar = (ClientBar) GUI.clientBars.get(id);
		if (currentBar == null) {
	       	GUI.clientBars.put (id, currentBar = new ClientBar(id, GaugeFrame.barMax));
	       	GUI.barPanel.add(currentBar.getClientPanel());
			GUI.barPanel.add(Box.createRigidArea(new Dimension(0,10)));
			//force refresh (some other way?)
			GUI.barPanel.setVisible(false);
			GUI.barPanel.setVisible(true);
			
			GUI.bucket.add(id);
	    }
    	
    	if (e.getAttribute("Start") != null) {
    		System.out.println("Connecting Client - " + id);
    		long st = e.getAttribute("Start").longValue();
			// if this is the first client issuing a "Start", start the gauge thread
    		if (! GUI.running) {
    			// nominal start time = 1st client start time
    			GUI.nominalBar.setStartTime(st);
		    	currentBar.setStartTime(st);
	    		GUI.nominal.start();
	    	}     		
			else
				// normalize all the client start times
				currentBar.setStartTime(GUI.nominalBar.getStartTime());
				
   			    	
	    }
    	else {
    		/* consider only if the event refers to the time interval currently considered,
    		which is > the moment of the last sample taken by the gauge 
    		and <= the moment when the next sample will be taken */
    		
    		//normalize download time 
    		long t = e.getAttribute("probeTime").longValue() - GUI.nominalBar.getStartTime();
    		 
    		currentBar.setFrame(e.getAttribute("leftbound").intValue(),
    								e.getAttribute("moment").intValue(),
    								e.getAttribute("rightbound").intValue(),
    								t,
    								e.getAttribute("level").intValue());
			JProgressBar jBar= currentBar.getBar();
			int progress = (int)e.getAttribute("rightbound").longValue();
			if (GUI.showProgress) {
				jBar.setValue(progress);
				jBar.setString(Integer.toString(progress));
			}
    		if ( t <= (GUI.progress + GaugeFrame.nomInterval) && t > GUI.progress)
    			GUI.bucket.update(id, currentBar.getFrame());
		}	
            
	}

    public void notify(Notification s[]) {
    	// I never subscribe for patterns anyway. 
    } 
}