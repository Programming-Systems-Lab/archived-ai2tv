package psl.ai2tv.gauge.visual;

import javax.swing.UIManager;
import java.awt.*;
import javax.swing.JProgressBar;
import java.util.Iterator;

import psl.ai2tv.gauge.*;

/**
	Main class for gauge that includes visualization facilities
*/
public class VisualGauge extends GroupGauge {
  private boolean packFrame = false;
  private GaugeFrame frame;

  //Construct the application
  public VisualGauge() {
  	// initialize GUI
    frame = new GaugeFrame();
    //Validate frames that have preset sizes
    //Pack frames that have useful preferred size info, e.g. from their layout
    if (packFrame) {
      frame.pack();
    }
    else {
      frame.validate();
    }
    //Center the window
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension frameSize = frame.getSize();
    if (frameSize.height > screenSize.height) {
      frameSize.height = screenSize.height;
    }
    if (frameSize.width > screenSize.width) {
      frameSize.width = screenSize.width;
    }
    frame.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    frame.setVisible(true);
  }
  
  public GaugeFrame getGaugeGUI() { return frame; }

	// overrides of GroupGauge  

  	protected GaugeComm setupCommunications() 
  		throws CommunicationException {
  		VisualGaugeSubscriber subscriber;
  		try {
	 		subscriber = new VisualGaugeSubscriber(this);
	 	} catch (Exception e) {
	 		subscriber = null;
	 		throw new CommunicationException(e); 
	 	}
	 	return subscriber;		
	 }
	 
	 protected Thread defineNominalClient() {
	 	return new Thread() {
		 	JProgressBar nomJBar = frame.nominalBar.getBar();
	    	boolean cont = true;
	    	
	    	public void run()	{
	    		running = true;
	    		frame.nominalBar.setStartTime(startTime);
	    		while (cont) {
		    		try {
		    			// gauge sampling interval
		    			sleep((int)nomInterval);
		    		} catch (InterruptedException ie) {
		    	}
		    		//nomProgress is the time elapsed since client start time	
		    		progress = (int)(System.currentTimeMillis() - frame.nominalBar.getStartTime());
		    		// in the time elapsed (in secs.) 30 frames per second have been nominally shown
		    		nomProgress = (int)(30 * progress / GroupGauge.SAMPLE_INTERVAL);
		    		evaluateStatus(progress);
		    		
		    		if (frame.showProgress) {
			    		nomJBar.setValue(nomProgress);
			    		nomJBar.setString(Integer.toString(nomProgress));
			    	}
		    	}
		    	running = false;
	    	}
	 	};	
	 }
	 
	 protected void fillBucket(long elapsed) {
		Iterator allClients = groupClients.values().iterator();
		while(allClients.hasNext()) {
			ClientBar bar =(ClientBar) allClients.next();
			String id = bar.getLabel().getText();
			long t = bar.getFrame().getDownloadedTime();
			
			// update the bucket is the time of the last info about a client
			// is within the time of the last sample and this moment
    		if ( t <= elapsed && t > bucket.getTime())
    			bucket.update(id, bar);			
		}
		
		bucket.setTime(elapsed);
	}
  
  protected void publishStatus() {
  	// for now just a printout 
  	System.out.println (bucket.toString());
  }
  
  protected FrameIndexParser setFrameInfo() {
  	FrameIndexParser ret = super.setFrameInfo();
  	
  	//now compute equivalent frames for each frame
  	EquivClasses ec = new EquivClasses(ret);
  	ec.computeAllEquivalents(0);
  	return ret;
  	
  }
  
  //Main method
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    }
    catch(Exception e) {
      e.printStackTrace();
    }
    VisualGauge vg = new VisualGauge();
	if (args.length > 0)
		vg.setFrameFileName(args[0]);
		    
    vg.setup();

    	
  }
}