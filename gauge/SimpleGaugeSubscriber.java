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

  protected static Siena siena = null;
	
  public static Siena getSiena() throws SienaException, IOException {
    if (siena == null){
      String server = System.getProperty("ai2tv.server");
      if (server != null) {
	siena = new ThinClient(server);
	//logger.debug("Connnected to server at " + ((ThinClient)siena).getServer());
      } else {
	// siena = new HierarchicalDispatcher();
	// ((HierarchicalDispatcher) siena).setReceiver(new KAPacketReceiver(sienaPort));
	logger.debug ("Siena Server Up: " + new String(((HierarchicalDispatcher) siena).getReceiver().address()));
      }
    }
    return siena;
  }
  
  /** Siena and subscriptions initialization */
  protected void setup() 
    throws SienaException, IOException {

    // if the siena server hasn't been created at this point, create it.
    getSiena();
    
    // listen for frame updates from clients
    Filter filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_FRAME, Op.ANY, "ANY");
    siena.subscribe(filter, this);

    // listen to a registration notification
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_WF_REG, Op.ANY, "ANY");
    siena.subscribe(filter, this);


    // listen for client's actions
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_VIDEO_ACTION, Op.ANY, "ANY");
    siena.subscribe(filter, this);

    // listen for client status updates coming back
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_WF_UPDATE_REPLY, Op.ANY, "ANY");
    siena.subscribe(filter, this);

    // listen for client status updates coming back
    filter = new Filter();
    filter.addConstraint(SienaConstants.AI2TV_CLIENT_SHUTDOWN, Op.ANY, "ANY");
    siena.subscribe(filter, this);
  }

  public void notify(Notification s[]) {
    // I never subscribe for patterns anyway. 
  }

}
