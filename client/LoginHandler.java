/*
 * @(#)LoginHandler.java
 *
 * Copyright (c) 2003: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2003: @author Dan Phung (dp2041@cs.columbia.edu)
 * Last Modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.ai2tv.client;

import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * The Login handler for the AI2TV client
 *
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 * @version	1.0
 */
public class LoginHandler extends JFrame {
  public static final int DEBUG = 3;

  static private final int REFRESH_RATE = 50;

  private int _units;
  private int _scale;

  private Canvas mainCanvas;
  private Color mainCanvasBG;
  private Color mainCanvasFG;
  private final int MAXCL = 255;
  private Container mainContentPane;
  JPanel mainPanel;
  JPanel menuPanel;

  private String _uid = null;
  private int _gid = -1;
  private String _passwd = null;
  private String _videoName = null;
  private String _date = null;
  private String _time = null;

  private JTextField _uidField;
  private JTextField _gidField;
  private JPasswordField _passwdField;
  private JTextField _baseURLField;
  private JTextField _dateField;
  private JTextField _timeField;
  private JList _availableVideos;
  
  /** link to the AI2TV client */ 
  private Client _client;

  LoginHandler(Client c){
    super("LoginHandler");

    _client = c;
    _uidField = null;
    _gidField = null;
    _passwdField = null;
    _dateField = null;
    _timeField = null;
    _availableVideos = null;

    mainCanvasBG = Color.white;
    mainCanvasFG = Color.black;
    mainContentPane = getContentPane();

    setTitle("AI2TV Login");
    setResizable(true);
    mainPanel = new JPanel();
    mainCanvas = addMainCanvas();

    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(mainCanvas, BorderLayout.CENTER);

    menuPanel = new JPanel();
    menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

    addLoginFields();

    setSize(130, 200);
    mainContentPane.add(menuPanel, BorderLayout.WEST);
    mainContentPane.add(mainPanel);
    show();
  }

  private Canvas addMainCanvas() {
    Canvas newCanvas = new Canvas() {
	private Image doubleBuffer = null;
	public void update(Graphics g) {
	  int dbw = mainPanel.getSize().width;
	  int dbh = mainPanel.getSize().height;
	  if (doubleBuffer==null 
	      || doubleBuffer.getWidth(mainPanel)!=dbw
	      || doubleBuffer.getHeight(mainPanel)!=dbh) {
	    doubleBuffer = mainPanel.createImage(dbw, dbh);
	  }
	  paint(doubleBuffer.getGraphics());
	  g.drawImage(doubleBuffer, 0, 0, mainPanel);
	}
	public void paint(Graphics g) {
	  // basic setup
	  g.setColor(mainCanvasBG);

	  // start the refreshing thread
	  try {
	    Thread.currentThread().sleep(REFRESH_RATE);
	  } catch (InterruptedException e) { }
	  repaint();
	}
      };
    return newCanvas;
  }

  private void changeView(){
    mainContentPane.removeAll();
    mainContentPane = getContentPane();
    mainPanel = new JPanel();
    mainCanvas = addMainCanvas();

    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(mainCanvas, BorderLayout.CENTER);

    menuPanel = new JPanel();
    menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));

    addVideoFields();

    mainContentPane.add(menuPanel, BorderLayout.WEST);
    mainContentPane.add(mainPanel);
    show();
  }

  private void addLoginFields() {

    // ----------- ADD BUTTONS and MENU PANEL --------- //
    // ------ UID ------ //
    JLabel uidLabel = new JLabel("UID");
    menuPanel.add(uidLabel);

    _uidField = new JTextField(8);
    _uidField.setText("dp2041");
    menuPanel.add(_uidField);

    // ------ GID ------ //
    JLabel gidLabel = new JLabel("GID");
    menuPanel.add(gidLabel);

    _gidField = new JTextField(8);
    _gidField.setText("101");
    menuPanel.add(_gidField);

    // ------ passwd ------ //
    JLabel passwdLabel = new JLabel("Password");
    menuPanel.add(passwdLabel);

    _passwdField = new JPasswordField(8);
    menuPanel.add(_passwdField);

    // ------ login ------ //
    JButton createButton = new JButton("Login");
    createButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  if (_uidField.getText().trim().length() > 0 && 
	      _gidField.getText().trim().length() > 0 &&
	      _passwdField.getPassword().length > 0) {
	    
	    _uid = _uidField.getText();
	    _gid = Integer.parseInt(_gidField.getText());
	    _passwd = new String(_passwdField.getPassword());
	    
	    _client.setLoginInfo(_uid, _gid, _passwd);
	    changeView();
	  } else {
	    Client.err.println("You must fill in all the fields");
	  }
	}
      });
    menuPanel.add(createButton);


    // -------- DONE ADD BUTTONS and MENU PANEL ------ //
  }

  void addVideoFields(){

    // ------ show/change base URL ------ //
    JLabel baseURLLabel = new JLabel("Base URL");
    menuPanel.add(baseURLLabel);
    
    _baseURLField = new JTextField(8);
    _baseURLField.setText(_client.getBaseURL());
    menuPanel.add(_baseURLField);

    JButton baseURLButton = new JButton("change base URL");
    baseURLButton.addActionListener(new ActionListener() {
	public void actionPerformed(ActionEvent evt) {
	  if (_baseURLField.getText().trim().length() > 0) {
	    _client.setBaseURL(_baseURLField.getText());
	    changeView();
	  }
	}
      });
    menuPanel.add(baseURLButton);

    // ------ Select videos ------ //
    String[] listOfVideos = _client.getAvailableVideos();
    JLabel videoLabel;
    if (listOfVideos.length == 0){
      Client.err.println("no videos available");
      videoLabel = new JLabel("Available videos: NONE");
    } else
      videoLabel = new JLabel("Available videos");

    menuPanel.add(videoLabel);

    final JList _availableVideos = new JList(listOfVideos);
    menuPanel.add(_availableVideos);
    if (listOfVideos.length == 0){ 
      setSize(200, 120);

    // if there are available videos to get
    } else {
      setSize(200, (200 + (listOfVideos.length * 20)));

      // ------ Date ------ //
      JLabel dateLabel = new JLabel("Date to view video");
      menuPanel.add(dateLabel);
      
      _dateField = new JTextField(8);
      _dateField.setText("2003-07-24");
      menuPanel.add(_dateField);
      
      // ------ Time ------ //
      JLabel timeLabel = new JLabel("Time to view video");
      menuPanel.add(timeLabel);
      
      _timeField = new JTextField(8);
      _timeField.setText("14:30:00");
      menuPanel.add(_timeField);
      
      // ------ Get Video ------ //
      JButton startButton = new JButton("Retrieve video");
      startButton.addActionListener(new ActionListener() {
	  public void actionPerformed(ActionEvent evt) {
	    _videoName = (String) _availableVideos.getSelectedValue();
	    if (_videoName != null && 
		_dateField.getText().trim().length() > 0 &&
		_timeField.getText().trim().length() > 0){
	      
	      _date = _dateField.getText();
	      _time = _timeField.getText();
	      
	      Client.out.println("selected video: " + _videoName);
	      _client.loadVideo(_videoName, (_date+";"+_time));
	      _client.initialize();
	      shutdown();
	    } else {
	      Client.err.println("You must select a video and set the date and time");
	    }
	  }
	});
      menuPanel.add(startButton);
    }
  }

  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      _client.shutdown();
      shutdown();
    }
  }
    
  // Returns just the class name -- no package info.
  protected String getClassName(Object o) {
    String classString = o.getClass().getName();
    int dotIndex = classString.lastIndexOf(".");
    return classString.substring(dotIndex+1);
  }

  void shutdown(){
    dispose();
  }

  /** main method, for testing only */
  public static void main(String args[]) {
    System.setProperty("ai2tv.siena", "ka:localhost:4444");
    System.setProperty("ai2tv.videoURL", "http://franken/ai2tv/CS4118-10");
    System.setProperty("ai2tv.level", "2");
    Client c = new Client();
    LoginHandler lh = new LoginHandler(c);
  }
}
