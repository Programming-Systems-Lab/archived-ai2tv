/*
 * @(#)Viewer.java
 *
 * Copyright (c) 2003: The Trustees of Columbia University in the City of New York.  All Rights Reserved
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
import java.util.Hashtable;
import javax.swing.*;
import javax.swing.event.*;
import psl.ai2tv.SienaConstants;

/** 
 * Very simple viewer.  The steps to showing an image are:
 * 1) load an image into the Viewer's memory using loadImage(String filename)
 * 2) use displayImage(String filename) to display the image.
 *
 * Note: that this viewer can only take up to 65536 images, then it
 * wraps around and starts using the same indices.
 *
 * WF related probes:
 * 0) 
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class Viewer extends JFrame {
  private Image _image;
  private int _menubarSize = 30; // the size of the menu bar
  private int REFRESH_RATE = 500; // refresh the image (ms)
  private String _filename;
  private MediaTracker _mediaTracker;
  private Toolkit toolkit;
  private Client _client;

  // members related to the display components
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

  // members involved in video frame 
  private String _lastImage;
  protected boolean _newFrame;
  protected int _imageIndex;
  protected int _viewIndex;
  private Hashtable _images;


  /**
   * Create the Viewer and associate it with the Client.
   *
   * @param c: parent client to communicate actions to.
   */
  Viewer(Client c) {
    // initialize internal members
    _client = c;
    _newFrame = false;
    _images = new Hashtable();
    _imageIndex = 0;
    _viewIndex = 0;
    toolkit = Toolkit.getDefaultToolkit();
    _mediaTracker = new MediaTracker(this);

    // declare some AWT and Swing related members
    _mainCanvasFG = Color.white;
    _mainCanvasBG = Color.white;
    Container cp = getContentPane();
    setSize(325, 325);
    setResizable(false);
    _lastImage = "";

    // create the panels
    _mainPanel = new JPanel();
    _mainPanel.setLayout(new BorderLayout());
    _bottomPanel = new JPanel();
    _bottomPanel.setLayout(new BorderLayout());
    _buttonPanel = new JPanel();
    _buttonPanel.setLayout(new FlowLayout());

    // add elements to the panels,main canvas, and content pane
    addButtons();
    createSlider();
    _bottomPanel.add(_buttonPanel, BorderLayout.NORTH); 
    _bottomPanel.add(_slider, BorderLayout.SOUTH); 
    _mainCanvas = paintImage();
    _mainPanel.add(_mainCanvas, BorderLayout.CENTER);
    cp.add(_mainPanel, BorderLayout.CENTER);
    cp.add(_bottomPanel, BorderLayout.SOUTH);

    addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  shutdown();
	}
      });

    // load the ready image and start the viewer
    _filename = "ai2tv_ready.jpg";
    ImageIcon imageIcon = null;
    try {
    ClassLoader cl = getClass().getClassLoader();
    imageIcon = new ImageIcon((java.net.URL) cl.getSystemResources("psl/ai2tv/client/ai2tv_ready.jpg").nextElement());
    } catch (java.io.IOException e) { e.printStackTrace(); }

    if (imageIcon != null){
      loadImage(imageIcon.getImage(), _filename);
      displayImage(imageIcon.getImage());
    } else {
      java.io.File startingImage = new java.io.File(_filename);
      if (startingImage.exists()){
	loadImage(_filename);
	displayImage(_filename);
      }
    }

    show();
  }

  /**
   * main function that repaints the frame image
   *
   * @param g: graphics object to paint to
   */
  void paintFrame(Graphics g){
    // foo and bar measure the time that it takes to check if the
    // image is loaded.
    // long foo = _client.currentTime();
    if (_mediaTracker.statusID(_viewIndex, false) == MediaTracker.COMPLETE){
      // long bar = _client.currentTime();
      // if (bar - foo != 0) 
      // Client.out.println("then: " + foo + " now: " + bar + ": " + (bar - foo));
      
      setTitle(_filename);
      _lastImage = _filename;
      g.drawImage(_image, 0, 0, null);
    }
      
    if (_newFrame){
      _client.setCurrentFrameTimeShown(_client.currentTime());
      if (Client.probe.getTimeProbe(0) >= 0)
	Client.probe.endTimeProbe(0, _client.currentTime(), SienaConstants.TIME_OFFSET);
      _newFrame = false;
      // don't know when I should do the following
      // _mediaTracker.removeImage(_image);  
    }
  }
  
  /**
   * paint thread that refreshes the Viewer display.
   *
   * @return the created canvas
   */
  private Canvas paintImage() {
    Canvas newCanvas = new Canvas() {
	private Image doubleBuffer = null;
	public void update(Graphics g) {
	  // to reduce flickering
	  int dbw = _mainPanel.getSize().width;
	  int dbh = _mainPanel.getSize().height;
	  if (doubleBuffer==null 
	      || doubleBuffer.getWidth(_mainPanel)!=dbw
	      || doubleBuffer.getHeight(_mainPanel)!=dbh) {
	    doubleBuffer = _mainPanel.createImage(dbw, dbh);
	  }
	  paint(doubleBuffer.getGraphics());
	  g.drawImage(doubleBuffer, 0, 0, _mainPanel);
	}
	public void paint(Graphics g) {
	  // Client.out.println("repainting: filename " + _filename + " image: " + _image);
	  g.setColor(_mainCanvasBG);
	  g.fillRect(0, 0, 325, 235);
	  g.setColor(_mainCanvasFG);
	  
	  paintFrame(g);
	
	  if (_client != null && _client.currentTime() != 0){
	    int time = (int) (_client.currentTime() / (long) 1000);
	    String minutes = (time/60 > 9) ? ""+time/60 : "0" + time/60;
	    String seconds = (time%60 > 9) ? ""+time%60 : "0" + time%60;
	    _time.setText(minutes + ":" + seconds);

	    // only update the slider every 3 seconds.
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
   * add the button panel to the Viewer display
   */
  private void addButtons() {
    _time = new JTextField(4);
    _time.replaceSelection("00:00");
    _buttonPanel.add(_time);

    JButton playButton = new JButton("Play");
    playButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  Client.out.println("play button pressed");
	  _client.playPressed();
	}
      });
    _buttonPanel.add(playButton);

    JButton pauseButton = new JButton("Pause");
    pauseButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  Client.out.println("pause button pressed");
	  _client.pausePressed();
	}
      });
    _buttonPanel.add(pauseButton);

    JButton stopButton = new JButton("Stop");
    stopButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  Client.out.println("pause button pressed");
	  _client.stopPressed();
	}
      });
    _buttonPanel.add(stopButton);
  }

  /**
   * create the slider of the Viewer display
   */
  private void createSlider(){
    int startTime = 0; 
    int endTime = _client.videoLength(); // this needs to be set to the time of the last frame.
    _slider = new JSlider(JSlider.HORIZONTAL, 0, endTime, startTime);
    _slider.setMajorTickSpacing(60); // set the tick spacing to 1 min each
    _slider.setPaintTicks(true);
    _slider.addChangeListener(new ChangeListener() {
	public void stateChanged(ChangeEvent e) {
	  JSlider source = (JSlider)e.getSource();
	  if (source.getValueIsAdjusting()) {
	    Client.out.println("abnormal change");
	    int newTime = (int)source.getValue();
	    Client.out.println("slider changing time to: " + newTime);
	    _client.gotoPressed(newTime);
	    source.setValueIsAdjusting(true);
	  }
	}
      });
  }

  void shutdown(){
    dispose();
    _client.shutdown();
    // this is the right way to close, not that System.exit stuff.
  }

  // - - - - - - ENTRY FUNCTIONS USED BY CLIENT - - - - - - - //

  /**
   * tells the viewer whether the frame to be shown is a new frame, usually
   * the usage is to set the value to true.
   * 
   * @param value: true/false depending on if the next frame to be
   * shown is new.
   */
  void setNewFrame(boolean value){
    _newFrame = value;
  }

  void displayImage(Image image) {
    _image = image;
  }

  /**
   * displays given image filename.  filename must refer to 
   * a previously loaded file using loadImage(String).
   * 
   * @param filename: image file to display
   */
  boolean displayImage(String filename) {
    // Client.out.println("displayImage called: " + _client.currentTime());
    ImageIndexPair pair = (ImageIndexPair) _images.get(filename);
    Client.debug.println("Viewer trying to display: " + filename + " pair: " + pair);
    if (pair != null){
      _image = pair.image;
      _viewIndex = pair.id;
      _filename = filename;
      // Client.debug.println("Viewer displaying: " + filename);
      return true;
    } else {
      Client.err.println("_image: " + _images);
      Client.err.println("Error in Viewer.displayImage: " + filename + " not valid.");
      return false;
    }
  }

  boolean loadImage(String filename) {
    Image image = toolkit.createImage(filename);
    return loadImage(image, filename);
  }

  /**
   * load the image into memory.  the specified file must be local 
   * to this system.
   * 
   * @param filename: image file to display.  
   */  
  boolean loadImage(Image image, String filename) {
    try {
      _mediaTracker.addImage(image, _imageIndex);
      _mediaTracker.waitForID(_imageIndex);
      _images.put(filename, new ImageIndexPair(image, _imageIndex));
      // Client.debug.println("Viewer loaded image: " + filename + " with id: " + _imageIndex);
      _imageIndex = (_imageIndex + 1) % 65535; // wraps around on the 65556'th image
      return true;
    } catch (InterruptedException e){
      Client.err.println("Viewer error in loading image " +filename +": "+ e);
      e.printStackTrace(Client.err);
    }
    return false;
  }

  // - - - - - - DONE: ENTRY FUNCTIONS USED BY CLIENT - - - - - - - //

  /**
   * main is used mainly for testing.
   */  
  public static void main(String[] args) {

    // test out the Viewer on a couple of images
    Viewer v = new Viewer(null);
    // sleep(1000);
    v.displayImage("cache/358.jpg");
  }
  
  // this private class encapsulates an image and it's loaded index
  private class ImageIndexPair{
    public Image image;
    public int id;
    public ImageIndexPair(Image im, int in){
      this.image = im;
      this.id = in;
    }
  }
}

