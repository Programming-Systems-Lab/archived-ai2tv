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
import java.util.Hashtable;
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
  protected boolean _newFrame;
  protected int _imageIndex;
  protected int _viewIndex;

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

  private Hashtable _images;

  /**
   * note that this viewer can only take up to 65536 images, then it wraps around and 
   * stars using the same indices
   *
   *
   */
  Viewer(Client c) {
    _client = c;
    _newFrame = true;
    _images = new Hashtable();
    _imageIndex = 0;
    _viewIndex = 0;

    toolkit = Toolkit.getDefaultToolkit();
    _mediaTracker = new MediaTracker(this);
    _mainCanvasFG = Color.white;
    _mainCanvasBG = Color.white;
    Container cp = getContentPane();
    setSize(325, 325);
    setResizable(false);
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
   * main function that repaints of the frame
   */
  void paintFrame(Graphics g){
    long foo = _client.currentTime();
    if (_mediaTracker.statusID(_viewIndex, false) == MediaTracker.COMPLETE){
      long bar = _client.currentTime();
      if (bar - foo != 0) 
	Client.out.println("then: " + foo + " now: " + bar + ": " + (bar - foo));
      
      setTitle(_filename);
      _lastImage = _filename;
      g.drawImage(_image, 0, 0, null);
    }
    
    if (_newFrame){
      Client.out.println("imageShown called: " + _client.currentTime());
      _client.imageShown();
      _newFrame = false;
      // don't know when I should do the following
      // _mediaTracker.removeImage(_image);  
    }
  }
  

  /**
   * main paint thread that handles the repainting of the image
   */
  private Canvas paintImage() {
    Canvas newCanvas = new Canvas() {
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

    /*
    JButton goButton = new JButton("Go");
    _goField = new JTextField(3);
    goButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  Client.out.println("Go field contents <" + _goField.getText() + ">");
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
	    Client.out.println("abnormal change");
	    int newTime = (int)source.getValue();
	    Client.out.println("slider changing time to: " + newTime);
	    _client.gotoPressed(newTime);
	    source.setValueIsAdjusting(true);
	  }
	}
      });
  }

  // - - - - - - ENTRY FUNCTIONS USED BY CLIENT - - - - - - - //

  /**
   * tells the viewer whether the frame just showed is a new frame
   */
  void setNewFrame(boolean value){
    _newFrame = value;
  }

  /**
   * displays given image filename.  filename must refer to 
   * a previously downloaded file.
   * 
   * @param filename: image file to display
   */
  void displayImage(String filename) {
    Client.out.println("displayImage called: " + _client.currentTime());
    // _image = toolkit.createImage(filename);
    // _mediaTracker.addImage(_image, 0);
    ImageIndexPair pair = (ImageIndexPair) _images.get(filename);
    if (pair != null){
      _image = pair.image;
      _viewIndex = pair.id;
      _filename = filename;
      // Client.out.println("Viewer displaying: " + filename);
    } else {
      Client.err.println("Error in Viewer.displayImage: " + filename + " not valid.");
    }
  }

  /**
   *
   */  
  void loadImage(String filename) {
    Image image = toolkit.createImage(filename);
    try {
      _mediaTracker.addImage(image, _imageIndex);
      _mediaTracker.waitForID(_imageIndex);
      _images.put(filename, new ImageIndexPair(image, _imageIndex));
      //Client.debug.println("Viewer loaded image: " + filename + " with id: " + _imageIndex);
      _imageIndex = (_imageIndex + 1) % 65535; // wraps around on the 65556'th image

    } catch (InterruptedException e){
      Client.err.println("Viewer error in loading image: " + e);
    }

  }
  // - - - - - - DONE: ENTRY FUNCTIONS USED BY CLIENT - - - - - - - //

  /**
   *
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
