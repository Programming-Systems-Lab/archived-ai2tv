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

  FauxWF(){
    _isActive = false;
    _mySiena = null;
    setupSienaListener();
  }

  public void notify(Notification e) {
    System.out.println("I just got this event:");
    System.out.println(e.toString());
    handleNotification(e);
  }

  public void notify(Notification [] s) { }

  private void handleNotification(Notification event){
    String name = event.toString().substring(7).split("=")[0];
    AttributeValue attrib = event.getAttribute(name);
    if (name.equals("WF_FRAME_UPDATE")){
      System.out.println("equal to WF_FRAME_UPDATE attrib: " + attrib);
      /*
      if (attrib.toString().equals("\"PLAY\"")){
	_client.commPlay(); 
      }
      */
    } else {
      System.out.println("NOT equal to WF_FRAME_UPDATE attrib: " + attrib);
    }
      hierarchyDown();
  }

  private void hierarchyDown(){
    Notification event = new Notification();
    event.putAttribute("WF_FRAME_CHANGELEVEL", "DOWN");
    System.out.println("FauxWF publishing event: " + event);
    publishNotification(event);
  }
 
  private void hierarchyUp(){
    Notification event = new Notification();
    event.putAttribute("WF_FRAME_CHANGELEVEL", "UP");
    System.out.println("FauxWF publishing event: " + event);
    publishNotification(event);
  }

  private void setupFilter() throws siena.SienaException {
    filter = new Filter();
    filter.addConstraint("WF_FRAME_UPDATE", "");
    _mySiena.subscribe(filter, this);
  }

  private void publishNotification(Notification event){
    try{
      System.out.println("publishing event: " + Calendar.getInstance().getTime());
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

