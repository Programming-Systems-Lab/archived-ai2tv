package psl.ai2tv.gauge.visual;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import psl.ai2tv.gauge.*;

/** Main real estate for the Gauge GUI */
 class GaugeFrame extends JFrame {

  public static final int barMax = 32039; //should be extracted by the frame index file

  boolean showProgress;

  JPanel contentPane;
  JPanel barPanel;
  JButton jButton1;
  ClientBar nominalBar = new ClientBar("Nominal", barMax);

  //Construct the frame
  public GaugeFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
  
    try {
      jbInit();
    }
    catch(Exception e) {
      e.printStackTrace();
    }

  }
  /** GUI Frame initialization: 
  	simply sets up the real estate	
  */
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
    
    showProgress = true;
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