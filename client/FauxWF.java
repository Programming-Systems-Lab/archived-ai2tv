package psl.ai2tv.client;

import java.util.Calendar;
import siena.*;

/**
 *
 */
class FauxWF extends Thread implements Notifiable {
  private boolean _isActive;
  private ThinClient _mySiena;
  private Filter filter;
  private int _earlyThreshold;
  private int _lateThreshold;

  FauxWF(){
    _isActive = false;
    _mySiena = null;

    _earlyThreshold = 2;  // threshold for max number of early frames
    _lateThreshold = 2;  // threshold for min number of late frames

    setupSienaListener();
  }

  public void notify(Notification e) {
    // System.out.println("I just got this event:");
    System.out.println(e.toString());
    handleNotification(e);
  }

  public void notify(Notification [] s) { }

  private void handleNotification(Notification event){
    String name = event.toString().substring(7).split("=")[0];
    AttributeValue attrib = event.getAttribute(name);
    long clientID;

    // System.out.println("name: " + name + " attrib:" + attrib);
    if (name.equals("AI2TV_FRAME")){
      clientID = event.getAttribute("CLIENT_ID").longValue();

      // first we see if the level needs to be changed.
      int early = event.getAttribute("earlyframes").intValue();
      int late = event.getAttribute("lateframes").intValue();
      int missed = event.getAttribute("missedframes").intValue();
      int level = event.getAttribute("level").intValue();
      
      if (early > _earlyThreshold && level > 0){
	System.out.println("Sending some uppers");
	changeLevel("UP", clientID);

      } else if (late > _lateThreshold && level < 4) {
	System.out.println("Sending some downers");
	changeLevel("DOWN", clientID);
      }      
    } else {
      System.out.println("Error: NOT equal to AI2TV_FRAME name");
    }


  }

  private void gotoFrame(int newFrame, long clientID){
    Notification event = new Notification();
    event.putAttribute("AI2TV_FRAME_UPDATE", "");
    event.putAttribute("CLIENT_ID", clientID);
    event.putAttribute("GOTO_FRAME", newFrame);
    publishNotification(event);
  }

  private void changeLevel(String dir, long clientID){
    Notification event = new Notification();
    event.putAttribute("AI2TV_FRAME_UPDATE", "");
    event.putAttribute("CLIENT_ID", clientID);
    event.putAttribute("CHANGE_LEVEL", dir);
    publishNotification(event);
  }

  private void setupFilter() throws siena.SienaException {
    filter = new Filter();
    filter.addConstraint("AI2TV_FRAME", "");
    _mySiena.subscribe(filter, this);
  }

  private void publishNotification(Notification event){
    try{
      System.out.println("publishing event: " + Calendar.getInstance().getTime() + ": " + event);
      _mySiena.publish(event);
    } catch (siena.SienaException e){
      System.err.println("CommController publishing sienaException: " + e);
    }  
  }

  private void setupSienaListener(){
    try {
      _mySiena = new ThinClient("ka:localhost:4444");

      setupFilter();

    } catch (siena.comm.PacketSenderException e) {
      // what is this?
      System.out.println ("Caught exception in setting up the Siena server: "  + e);
    } catch (SienaException e) {
      // ; // WTF?
      // } catch (siena.comm.InvalidSenderException e) {
      System.out.println ("Cannot connect to Siena bus: "  + e);
      _isActive = false;
      // e.printStackTrace();
    }
  }

  public void run(){
    _isActive = true;
    while(_isActive){
      try {
	sleep(10000);
      } catch (InterruptedException e){
	System.out.println("FauxWF error: " + e);
      }
    }
  }

  public static void main(String[] args) {
    if(args.length != 1) {
      System.err.println("Usage: FauxWF <server-address>");
      System.exit(1);
    }
    FauxWF wf = new FauxWF();
    wf.start();
  }
}

