package psl.ai2tv.gauge.visual;

import javax.swing.*;
import java.awt.*;
import java.util.*;

import psl.ai2tv.gauge.*;

/**
	Envelopes info about a client: its ID, its state and its GUI elements.
*/
class ClientBar {

	/** contains client ID */
	private JLabel label;
	
	/** Progress bar for visualization */
	private JProgressBar bar;
	
	/** panel for showing the progress bar and the label */
	private JPanel clientPanel;
	
	/** time of client start - normalized at the first client's start  */
	private long startTime;
	
	/** last frame being displayed by the client */
	private FrameDesc fd;
	
	ClientBar(String name, int max) {
		startTime = 0;
		fd = new FrameDesc();
				
		label = new JLabel (name);
		bar = new JProgressBar(0, max);
		bar.setValue(0);
		bar.setStringPainted(true);
		
		clientPanel = new JPanel();
	    clientPanel.setLayout(new BorderLayout());
		clientPanel.setBorder(BorderFactory.createLineBorder(Color.black));
	   	clientPanel.add(label, BorderLayout.WEST);
	    clientPanel.add(bar, BorderLayout.CENTER);
	    System.out.println ("Added Client Panel - " + label.getText());
	}
	
	void setFrame(int l, int m, int r, long t, int level) {
		fd.setStart (l);
		fd.setNum (m);
		fd.setEnd (r);
		fd.setDownloadedTime(t);
		fd.setLevel (level);
	}
	
	FrameDesc getFrame() { return fd; }
	
	
	JLabel getLabel() { return label; }
	JProgressBar getBar() { return bar; }
	JPanel getClientPanel() { return clientPanel; }

	long getStartTime() { return startTime; }
	void setStartTime(long st) { startTime = st; } 
}