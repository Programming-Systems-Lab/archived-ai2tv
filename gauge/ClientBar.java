package psl.ai2tv.gauge;

import javax.swing.*;
import java.awt.*;
import java.util.*;


class ClientBar {

	private JLabel label;
	private JProgressBar bar;
	private JPanel clientPanel;
	
	/** time of client start - normalized at the first client's start  */
	private long startTime;
	private FrameDesc fd;
	
	private long nominalAvgDistance;
	private int samples;
	
	ClientBar(String name, int max) {
		startTime = 0;
		fd = new FrameDesc();
		nominalAvgDistance = 0;
		samples = 0;
				
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
	
	void updateAvgDistance(long d) {
		/*
		if (samples == 0)
			nominalAvgDistance = d;
		else
			nominalAvgDistance = (nominalAvgDistance * samples + d) / ++samples;
		
		//trouble related to excessive distance
		if (nominalAvgDistance > 200)
			insertReport (new GaugeReport(label.getText(), 0, nominalAvgDistance, 0, 0, 0));
		*/	
	}
	
	JLabel getLabel() { return label; }
	JProgressBar getBar() { return bar; }
	JPanel getClientPanel() { return clientPanel; }

	long getStartTime() { return startTime; }
	void setStartTime(long st) { startTime = st; } 
	

}