package psl.ai2tv.workflow;

import psl.ai2tv.gauge.*;

import java.util.Iterator;

class WFGauge extends GroupGauge {

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
	    	
	    	public void run()	{
	    		running = true;
	    		while (cont) {
		    		try {
		    			// gauge sampling interval
		    			sleep((int)nomInterval);
		    		} catch (InterruptedException ie) {
		    	}
		    		//nomProgress is the time elapsed since client start time	
		    		progress = (int)(System.currentTimeMillis() - startTime);
		    		// in the time elapsed (in secs.) 30 frames per second have been nominally shown
		    		nomProgress = (int)(30 * progress / GroupGauge.SAMPLE_INTERVAL);
		    		evaluateStatus(progress);
		    	}
		    	running = false;
	    	}
	 	};	
	 }
	 
	 protected void fillBucket(long elapsed) {
		Iterator allClients = groupClients.keySet().iterator();
		while(allClients.hasNext()) {
			String id =(String) allClients.next();
			ClientDesc cd = (ClientDesc) groupClients.get(id);
			FrameDesc fd = cd.getFrame();
			long t = fd.getDownloadedTime();
			
			// update the bucket is the time of the last info about a client
			// is within the time of the last sample and this moment
    		if ( t <= elapsed && t > bucket.getTime())
    			bucket.update(id, fd);			
		}
		
		bucket.setTime(elapsed);
	}
  
  protected void publishStatus() {
  	//pass it to LDM plugin for publication

  }
  
  protected FrameIndexParser setFrameInfo() {
  	FrameIndexParser ret = super.setFrameInfo();
  	
  	//now compute equivalent frames for each frame
  	EquivClasses ec = new EquivClasses(ret);
  	ec.computeAllEquivalents(0);
  	return ret;
  }
  	
}