
/*
 * @(#)Viewer.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Dan Phung
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */
package psl.ai2tv.client;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

/*
 * Viewer.java
 */
class Viewer extends JFrame {
  private Image _image;
  private int _menubarSize = 30; // the size of the menu bar
  private int REFRESH_RATE = 500; // refresh the image (ms)
  private String _filename;
  private MediaTracker _mediaTracker;
  private Toolkit toolkit;
  private Client _client;
  private String _lastImage;

  // items related to the viewer display
  private Canvas _mainCanvas;
  private Color _mainCanvasBG;
  private Color _mainCanvasFG;
  private JPanel _mainPanel;
  private JPanel _bottomPanel; // contains the button and slider panels
  private JPanel _buttonPanel;
  private JPanel _sliderPanel;
  private JTextField _goField;
  private JTextField _time;
  private JSlider _slider;

  /*
   * Viewer.java
   */
  Viewer(Client c) {
    _client = c;
    toolkit = Toolkit.getDefaultToolkit();
    _mediaTracker = new MediaTracker(this);
    _mainCanvasFG = Color.white;
    _mainCanvasBG = Color.white;
    Container cp = getContentPane();
    setSize(325, 325);
    setResizable(true);
    _lastImage = "";

    _filename = "ai2tv_ready.jpg";
    _image = toolkit.createImage(_filename);
    _mediaTracker.addImage(_image, 0);
    
    _mainPanel = new JPanel();
    _mainPanel.setLayout(new BorderLayout());

    _bottomPanel = new JPanel();
    _bottomPanel.setLayout(new BorderLayout());

    _buttonPanel = new JPanel();
    _buttonPanel.setLayout(new FlowLayout());
    addButtons();
    createSlider();
    
    _bottomPanel.add(_buttonPanel, BorderLayout.NORTH); 
    _bottomPanel.add(_slider, BorderLayout.SOUTH); 

    addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.exit(0);
	}
      });

    _mainCanvas = paintImage();
    _mainPanel.add(_mainCanvas, BorderLayout.CENTER);


    cp.add(_mainPanel, BorderLayout.CENTER);
    cp.add(_bottomPanel, BorderLayout.SOUTH);

    show();
  }

  /**
   * main paint thread that handles the repainting of the image
   */
  private Canvas paintImage() {
    Canvas newCanvas = new Canvas() {
	public void paint(Graphics g) {
	  // System.out.println("repainting: filename " + _filename + " image: " + _image);
	  g.setColor(_mainCanvasBG);
	  g.fillRect(0, 0, 325, 235);
	  g.setColor(_mainCanvasFG);
	  
	  if (_filename != _lastImage){
	    setTitle(_filename);
	    _lastImage = _filename;
	    try {
	      _mediaTracker.waitForID(0);
	    } catch (InterruptedException ie) {
	      System.err.println(ie);
	      System.exit(1);
	    }
	  }
	  // System.out.print("< about to drawImage: ");
	  g.drawImage(_image, 0, 0, null);
	  // System.out.println(" :done about to drawImage >");
	  // start the refreshing thread
	
	
	  if (_client != null && _client.currentTime() != 0){
	    int time = (int) (_client.currentTime() / (long) 1000);
	    String minutes = (time/60 > 9) ? ""+time/60 : "0" + time/60;
	    String seconds = (time%60 > 9) ? ""+time%60 : "0" + time%60;
	    _time.setText(minutes + ":" + seconds);

	    // only update the slider every 10 seconds.
	    if (time%3 == 0){
	      _slider.setValue(time);
	    }
	  }
	  
	  try {
	    Thread.currentThread().sleep(REFRESH_RATE);
	  } catch (InterruptedException e) { }
	  repaint();
	}
      };
    return newCanvas;
  }

  /** 
   * add the button panel
   */
  private void addButtons() {
    _time = new JTextField(4);
    _time.replaceSelection("00:00");
    _buttonPanel.add(_time);

    JButton playButton = new JButton("Play");
    playButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  System.out.println("play button pressed");
	  _client.playPressed();
	}
      });
    _buttonPanel.add(playButton);

    JButton pauseButton = new JButton("Pause");
    pauseButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  System.out.println("pause button pressed");
	  _client.pausePressed();
	}
      });
    _buttonPanel.add(pauseButton);

    JButton stopButton = new JButton("Stop");
    stopButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  System.out.println("pause button pressed");
	  _client.stopPressed();
	}
      });
    _buttonPanel.add(stopButton);

    /*
    JButton goButton = new JButton("Go");
    _goField = new JTextField(3);
    goButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  System.out.println("Go field contents <" + _goField.getText() + ">");
	  _client.goPressed(Integer.parseInt(_goField.getText()));
	  _goField.setText("");
	}
      });
    _buttonPanel.add(goButton);
    _buttonPanel.add(_goField);
    */
  }

  /**
   * create the slider
   */
  private void createSlider(){
    int startTime = 0; 
    int endTime = _client.videoEndTime(); // this needs to be set to the time of the last frame.
    _slider = new JSlider(JSlider.HORIZONTAL, 0, endTime, startTime);
    _slider.setMajorTickSpacing(60); // set the tick spacing to 1 min each
    _slider.setPaintTicks(true);
    // _slider.setMinorTickSpacing(60); // set the tick spacing to 1 min each
    // _slider.createStandardLabels(60); // set the tick spacing to 1 min each
    _slider.addChangeListener(new ChangeListener() {
	public void stateChanged(ChangeEvent e) {
	  JSlider source = (JSlider)e.getSource();
	  if (source.getValueIsAdjusting()) {
	    System.out.println("abnormal change");
	    int newTime = (int)source.getValue();
	    System.out.println("slider changing time to: " + newTime);
	    _client.gotoPressed(newTime);
	    source.setValueIsAdjusting(true);
	  }
	}
      });
  }


  /**
   * displays given image filename.  filename must refer to 
   * a previously downloaded file.
   * 
   * @param filename: image file to display
   */
  void displayImage(String filename) {
    _image = toolkit.createImage(filename);
    _mediaTracker.addImage(_image, 0);
    _filename = filename;
    // System.out.println("Viewer displaying: " + filename);
  }

  /**
   *
   */  
  public static void main(String[] args) {

    // test out the Viewer on a couple of images
    Viewer v = new Viewer(null);
    // sleep(1000);
    v.displayImage("cache/358.jpg");
  }
}
