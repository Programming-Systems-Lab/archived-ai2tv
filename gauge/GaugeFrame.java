package psl.ai2tv.gauge;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import siena.*;
import siena.comm.*;

public class GaugeFrame extends JFrame {
  public static final int sienaPort = 4444;
  static long nomInterval;
  public static final int barMax = 32039;
  public static final int frameMax = 165;
  //static int clientNum = 0;
  /**
  	time since start
  */
  int progress=0;
  /**
  	frames nominally seen since start time (assuming nominal frame rate 30 fps)
  */
  int nomProgress = 0;
  boolean showProgress;
  boolean running;

  JPanel contentPane;
  JPanel barPanel;
  JButton jButton1;
  ClientBar nominalBar = new ClientBar("Nominal", barMax);
  Hashtable clientBars = new Hashtable();
  Thread nominal;
  TimeBucket bucket;
  
 
  //Construct the frame
  public GaugeFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    /* gauge sampling interval:
     */
    //nomInterval = barMax / frameMax;
    nomInterval = 1000;
    bucket = new TimeBucket();
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }

  }
  //Component initialization
  private void jbInit() throws Exception  {
    contentPane = (JPanel) this.getContentPane();
    contentPane.setLayout(new BorderLayout());
    this.setSize(new Dimension(800, 400));
    this.setTitle("Frame Title");
    jButton1 = new JButton();
    jButton1.setText("Pause");
    jButton1.addActionListener(new GaugeFrame_jButton1_actionAdapter(this));
        
     barPanel = new JPanel();
    //barPanel.setBorder(BorderFactory.createLineBorder(Color.black));
    barPanel.setLayout(new BoxLayout(barPanel, BoxLayout.Y_AXIS));
    
    barPanel.add(nominalBar.getClientPanel());
	barPanel.add(Box.createRigidArea(new Dimension(0,10)));
    //barPanel.add(clientPanel);
	//barPanel.add(Box.createRigidArea(new Dimension(0,10)));
	
	contentPane.add(barPanel, BorderLayout.NORTH);
    contentPane.add(jButton1, BorderLayout.SOUTH);
    
    //Siena initialization
    HierarchicalDispatcher mainSiena = new HierarchicalDispatcher();
    mainSiena.setReceiver(new UDPPacketReceiver(sienaPort));
    System.out.println ("Siena Server Up: " + new String(mainSiena.getReceiver().address()));
    GaugeSubscriber subscriber = new GaugeSubscriber(this);
    mainSiena.subscribe(subscriber.getGaugeFilter(), subscriber);
    Filter startFilter = new Filter();
    startFilter.addConstraint("Start", Op.ANY, (String)null);
    mainSiena.subscribe(startFilter, subscriber);
    
    showProgress = true;
    running = false;
    
    nominal = new Thread() {
    	JProgressBar nomJBar = nominalBar.getBar();
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
	    		progress = (int)(System.currentTimeMillis() - nominalBar.getStartTime());
	    		// in the time elapsed (in secs.) 30 frames per second have been nominally shown
	    		nomProgress = (30 * progress / 1000);
	    		evaluateStatus(progress);
	    		
	    		if (showProgress) {
		    		nomJBar.setValue(nomProgress);
		    		nomJBar.setString(Integer.toString(nomProgress));
		    	}
	    	}
	    	running = false;
    	}
    };
  }

  //Overridden so we can exit when window is closed
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      System.exit(0);
    }
  }

  void jButton1_actionPerformed(ActionEvent e) {
   showProgress = !showProgress;
  }
  
  void evaluateStatus(long elapsed) {
	Iterator allClients = clientBars.values().iterator();
	while(allClients.hasNext()) {
		ClientBar bar =(ClientBar) allClients.next();
		//long left = bar.getFrameLeft();
		String id = bar.getLabel().getText();
		//long moment = bar.getFrame().getNum();
		//int level = bar.getFrame().getLevel();
		//long right = bar.getFrameRight();
		//bucket.update(id, bar.getFrame());	
		bucket.setTime(elapsed);
		
		/*
		long distance = elapsed - moment;
		if (elapsed < left || elapsed > right) {
			GaugeReport trouble = createReport(bar.getLabel().getText(), elapsed, distance, left, moment, right);
			bar.insertReport(trouble);
		}
		else
			bar.updateAvgDistance(distance);
		*/
	}
	publishStatus();
	clearStatus();
  }
    
  private void publishStatus() {
  	System.out.println (bucket.toString());
  }
  
  /**
  	clears the time bucket before starting next sample
  */
  private void clearStatus() {
  	bucket.clearValues();
  }
}

class GaugeFrame_jButton1_actionAdapter implements java.awt.event.ActionListener {
  GaugeFrame adaptee;

  GaugeFrame_jButton1_actionAdapter(GaugeFrame adaptee) {
    this.adaptee = adaptee;
  }
  public void actionPerformed(ActionEvent e) {
    adaptee.jButton1_actionPerformed(e);
  }
  
}