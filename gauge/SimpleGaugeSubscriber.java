package psl.ai2tv.gauge;

import siena.*;
import siena.comm.*;

import java.util.Hashtable;
import java.io.IOException;

import org.apache.log4j.Logger;
import psl.ai2tv.SienaConstants;


/**
   Basic Siena-based event receiver for the gauge.
   Abstract since the implementation of @see notify() is left to subclasses
*/
public abstract class SimpleGaugeSubscriber implements GaugeSubscriber {

  private static final Logger logger = Logger.getLogger(SimpleGaugeSubscriber.class);

  protected static Siena mainSiena = null;
	
  public static Siena getSiena() throws SienaException, IOException {
    if (mainSiena == null){
      String server = System.getProperty("ai2tv.server");
      if (server != null) {
	mainSiena = new ThinClient(server);      
	//logger.debug("Connnected to server at " + ((ThinClient)mainSiena).getServer());
      } else {
	mainSiena = new HierarchicalDispatcher();
	((HierarchicalDispatcher) mainSiena).setReceiver(new KAPacketReceiver(sienaPort));
	logger.debug ("Siena Server Up: " + new String(((HierarchicalDispatcher) mainSiena).getReceiver().address()));
      }
    }
    return mainSiena;
  }
  
  /** Siena and subscriptions initialization */
  protected void setup() 
    throws SienaException, IOException {

    // if the siena server hasn't been created at this point, create it.
    getSiena();
    
    // listen for frame updates from clients
    Filter filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_FRAME, Op.ANY, "ANY");
    mainSiena.subscribe(filter, this);

    // listen to a registration notification
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_WF_REG, Op.ANY, "ANY");
    mainSiena.subscribe(filter, this);

    // listen for client reports coming back
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_VIDEO_ACTION, Op.ANY, "ANY");
    mainSiena.subscribe(filter, this);
  }

  public void notify(Notification s[]) {
    // I never subscribe for patterns anyway. 
  }

}
