package psl.ai2tv.gauge;

import siena.*;

/**
	Empty interface serving as a placeholder for 
	defining a Siena-based communication facility to be used by a gauge
*/
public interface GaugeSubscriber
	extends GaugeComm, Notifiable {
		
	static final int sienaPort = 4444;
	
}