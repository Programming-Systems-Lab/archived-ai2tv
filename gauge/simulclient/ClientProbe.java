package psl.ai2tv.gauge.simulclient;

import siena.*;
import siena.comm.*;

import psl.ai2tv.gauge.FrameDesc;

class ClientProbe 
	implements Runnable {
	/**
		interval between probe events in ms.'
	*/
	private long interval;
	private boolean loopAlways = true;
	private boolean active; 
	ThinClient mySiena;
	private Notification frameEvent;
	private String clientID;
	private CacheController cache;

	ClientProbe (String id) {
		clientID = id;
		active = false;
		mySiena = null;
		cache = null;
		setupSiena();
	
	}
	
	public void run() {
		loopAlways = true;
		active = true;
		while (loopAlways) {
			if (active) {
				try {
					Thread.currentThread().sleep(interval);		
				} catch (InterruptedException ie) {
				//do nothing
				}
			}
			//send probe message
			emit(cache.getCurrFrame());
		}

	}

		
	private void setupSiena() {
		try {
			mySiena = new ThinClient("udp:localhost:4444");
		} catch (InvalidSenderException ise) {
			System.out.println ("Cannot connect to Siena bus");
			mySiena = null;
			loopAlways = false;
			active = false;
			ise.printStackTrace();	
		}
		// trying to optimize by calling constructors for events only once
		frameEvent = new Notification();
		frameEvent.putAttribute("FRAME", "frame_ready");
		frameEvent.putAttribute("ClientID", clientID);
		frameEvent.putAttribute("leftbound", 0);	
		frameEvent.putAttribute("rightbound", 0);
		frameEvent.putAttribute("moment", 0);
		frameEvent.putAttribute("level", -1); 
		frameEvent.putAttribute("probeTime", 0);
	}
	
	private void emit (FrameDesc fd) {
		if (fd != null) {
			//System.out.println ("Sending Frame info");
			//update only necessary fields
			frameEvent.putAttribute("leftbound", fd.getStart());	
			frameEvent.putAttribute("rightbound", fd.getEnd());
			frameEvent.putAttribute("moment", fd.getNum());
			frameEvent.putAttribute("level", fd.getLevel());
			frameEvent.putAttribute("probeTime", System.currentTimeMillis());
			try { 
				mySiena.publish(frameEvent);
			} catch (SienaException se) {
				se.printStackTrace();	
			}
		}
	}
	
	void setTarget(CacheController cc) { cache = cc; }
	void setProbingFrequency (long f) {interval = f; }

	void stopProbe() { loopAlways = false; }

	public void setActive(boolean flag) {
		active = flag;
	}
	
	public boolean isActive() { return active; }
}