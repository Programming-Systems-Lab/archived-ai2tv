/*
 * @(#)Client.java
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

import java.io.*;
import java.util.*;
import psl.ai2tv.gauge.*;

/**
 * Main component controlling the others.  commPlay is where the main
 * fun starts.  after receiving a play message, the client will start
 * the viewing thread.  checkCurrentFrame() is the main function that
 * checks what time it is, what frame we should be showing, etc.  The
 * client only responds after receiving a play command from the Siena
 * server, which calls commPlay to start the viewing.
 * 
 *         /---------------------> <--Siena--> --> other clients 
 *        /
 * [CommController]        
 *     /\
 *     |     /-------> [ ClientProbe ] <--Siena--> WF
       |    / /------< [ ClientEffector ] <--Siena--> WF
 *     |   / /
 *     \/ / \/
 *   [ Client ] <------> [CacheController]
 *      /\  /\ 
 *       \   \ 
 *        \   \-------< [
 *        \/
 *      [Viewer]
 * 
 *
 * WF related probes:
 * 0) 
 *
 * 
 *
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class Client {
  // this private class is declared here temporarily to separate the
  // dependency on the C++ side of things, for testing and development
  // purposes
  private class AI2TVJNIJava{}

  // output streams for debugging info
  public static PrintStream out;
  public static PrintStream err;
  public static PrintStream debug; 
  public static PrintStream probeOutput; 

  private AI2TVJNIJava _JNIIntf;
  private CacheController _cache;
  private CommController _comm;
  private Viewer _viewer;

  /** current download/viewing hierarchy level */
  private int _level;

  /** the time it takes for the WF to contact us */
  private int _wfDistance;

  // internal clock, directly related to system clock
  private long _startTime;
  private long _pausedTime;
  private long _pausedStartTime;
  private boolean _pausePressed;

  private boolean _isActive;
  private FrameIndexParser _framesInfo;
  private FrameDesc[][] _framesData;

  private long _id; // client id number for server identification

  // we need a three frame window in order to be able to detect missed frames
  private FrameDesc _currFrame;
  private FrameDesc _nextFrame;  
  private FrameDesc _neededFrame;

  private long _timeCurrFrameShown;  // time that the current image was first shown

  // we should have these in a config file
  public static final String _cacheDir = "cache/";
  // public static final String _baseURL = "http://franken.psl.cs.columbia.edu/ai2tv/1/";
  // public static final String _sienaServer = "ka:franken:4444";
  public static final String _baseURL = "http://128.59.14.163/ai2tv/1/";
  public static final String _sienaServer = "ka:128.59.14.163:4444";

  public static final String _frameFile = "frame_index.txt";
  public static final long FRAME_RATE = 30; // frames / second
  public static final long CHECK_RATE = 250; // check if frame is downloaded this often (ms)

  /** 
   * this the amount of buffer time I give to allow processing 
   * delays (lookahead ~ time to process what image to show + 
   * time to load and paint image) in seconds
   */
  private long _lookahead = 0;

  /**
   * WF Probe
   */
  static ClientProbe probe;

  /**
   * WF Effector
   */
  private ClientEffector _effector;

  /** 
   * Create an AI2TV Client
   */
  Client(){
    try {
      out = new PrintStream(new  FileOutputStream(new File("ai2tv_out.log")), true);
      err = new PrintStream(new  FileOutputStream(new File("ai2tv_err.log")), true);
    } catch (FileNotFoundException e){
      e.printStackTrace();
    }
    debug = System.out;
    probeOutput = out;

    // what is the prob that two clients start at the exact same time? ...pretty low
    _id = System.currentTimeMillis(); 
    _startTime = 0;
    _pausedTime = _pausedStartTime = 0;
    _pausePressed = false;
    _level = 0;
    _timeCurrFrameShown = 0;
    _cache = new CacheController(this, _cacheDir, _baseURL);

    checkFrameFile(_frameFile);

    _framesInfo = new FrameIndexParser(_cacheDir + _frameFile);
    _framesData = _framesInfo.frameData();

    _viewer = new Viewer(this);
    _comm = new CommController(this, _id, _sienaServer);
    if (!_comm.isActive()){
      System.out.println("Error creating CommController");
      System.exit(1);
    }
      

    _cache.initialize(); // start the thread to download frames
    _cache.start(); // start the thread to download frames
    _currFrame = null;
    _nextFrame = null;
    _neededFrame = null;

    probe = new ClientProbe(this, _sienaServer, 10); // we will have 10 probes set
    _effector = new ClientEffector(this, _sienaServer);
  }

  /**
   * check that the frame file is downloaded.  if not, go get it.
   *
   * @param frameFile: exact path/filename of the frame index file
   */
  private void checkFrameFile(String frameFile){
    File fh = new File(_cacheDir + frameFile);
    if (fh == null || !fh.exists()){
      _cache.downloadFile(_baseURL + frameFile);
    }
  }  

  /**
   * @return initialized FrameIndexParser data structure
   */
  FrameIndexParser getFramesInfo(){
    return _framesInfo;
  }

  /**
   * get the parsed FramesIndexParser
   *
   * @return initialized FrameIndexParser data structure
   */
  private FrameDesc getFrame(int level, long now){
    long currentTime = now / 1000;
    // Client.out.println("getting the next frame for : " + level + ", " + currentTime);
    for (int i=0; i<_framesData[level].length; i++){
      // Client.out.println("< " + (_framesData[level][i].getStart()/30) + " ? " + currentTime + 
      // " ? " + _framesData[level][i].getEnd()/30 + ">");
      if (_framesData[level][i].getStart()/30 <= currentTime &&
	  currentTime < _framesData[level][i].getEnd()/30){
	return _framesData[level][i];
      }
    }
    return null;
  }

  /**
   * get the next frame
   *
   *
   *
   */
  private FrameDesc getNextFrame(int level, long now){
    long currentTime = now / 1000;
    // Client.out.println("getting the next frame for : " + level + ", " + currentTime);
    for (int i=0; i<_framesData[level].length; i++){
      // Client.out.println("< " + (_framesData[level][i].getStart()/30) + " ? " + currentTime + 
      // " ? " + _framesData[level][i].getEnd()/30 + ">");
      if (_framesData[level][i].getStart()/30 <= currentTime &&
	  currentTime < _framesData[level][i].getEnd()/30)
	if ((i+1) < _framesData[level].length)
	  return _framesData[level][i+1];
    }
    return null;
  }


  /**
   * @return time the video ends at
   */
  int videoEndTime(){
    return (_framesData[0][_framesData[0].length - 1].getEnd()/ 30 + 1);
  }

  /**
   * @return whether the client is active
   */
  boolean isActive(){
    return _isActive;
  }
  
  /**
   * shutdown the client
   */
  void shutdown(){
    _isActive = false;
  }

  /**
   * @return client's ID, which is the currently time of creation
   */
  long getID(){
    return _id;
  }

  /**
   * @return client's current bandwidth
   */
  double getBandwidth(){
    return _cache.getBandwidth();
  }

  /**
   * @return time that the current frame was originally shown
   */
  long getTimeCurrFrameShown(){
    return _timeCurrFrameShown;
  }

  /**
   * @return current frame showing
   */
  FrameDesc getCurrFrame(){
    return _currFrame;
  }

  /**
   * load the image in memory in preparation to be shown
   * @param image: image to load
   */
  void loadImage(String image){
    _viewer.loadImage(image);
  }

  /**
   * starts the viewer playing thread that checks what the current frame should be
   */
  private void startViewerThread(){
    _isActive = true;
    new Thread(){
      public void run(){

	while(_isActive){
	  checkCurrentFrame();

	  try {
	    sleep(CHECK_RATE);
	  } catch (InterruptedException e){
	    Client.err.println("Client play thread error: " + e);
	    shutdown();
	  }
	}
      }
    }.start();
}
  
  /**
   * checks what time it is + some lookahead factor, 
   * and checks to see if:
   * 1) we missed a frame, in which case we interrupt the current download. 
   * The cache controller should then start downloading the next frame that it.
   * can possible get. 
   * 2) we haven't yet downloaded the frame that was supposed to be
   * showing at this time, so we wait some more
   * 3) we've downloaded the frame, so show it.
   */
  private void checkCurrentFrame(){
    _nextFrame = getFrame(_level, currentTime() + _lookahead);
	    
    if (_nextFrame == null){
      Client.out.println("Are we at the end of the Video?");
      _isActive = false;
      return;
    } 

    // if time has changed, and we need to show a new frame
    if (_neededFrame != _nextFrame){
      if (_currFrame != null && _currFrame != _neededFrame){
	// Client.out.println("missed a frame!: " + _neededFrame);
	_cache.interruptDownload();
	// in addition to interrupting the download, it should also
	// tell it the next frame to download and inform the WF
      }
      _neededFrame = _nextFrame;
    }

    if (_currFrame == null || _currFrame.getNum() != _neededFrame.getNum()){
	      
      // Client.out.println("Time is: " + currentTime() + " trying to show frame: " + _neededFrame.getNum());
      if (_cache.isDownloaded(_neededFrame.getNum() + ".jpg")){
	// then show it.
	probe.startTimeProbe(0, _neededFrame.getStart()*1000/30);
	_viewer.setNewFrame(true);		
	_viewer.displayImage(_cacheDir + _neededFrame.getNum() + ".jpg");

	// shift the frame window back
	_currFrame = _neededFrame;
		
	// from here, the Viewer tries to load in the image, and calls this object's 
	// imageShown method after the image is actually painted on the screen.
      } else {
	Client.debug.println(_neededFrame.getNum() + ".jpg was not downloaded in time for showing!");
      }
    }
    // }
  }

  // --------- Comm initiated actions ---------- //
  /**
   * pseudoWF that informs the client what to play next.  This 
   * method uses the simple scheme of playing the current frame at the time.
   *
   * if the image wasn't downloaded on time, then we want to loop incesstantly, is it here, 
   * how bout now, now?
   *
   * otherwise, we can wait until right up to the second before to check.
   */
  public void commPlay(){
    Client.out.println("commPlay received");
    
    // have we started, if not, this is it!
    if (_startTime == 0){
      Client.out.println("starting time");
      startTime();
    }
    startViewerThread();
  }

  public void commStop(){
    Client.out.println("commStop received");
    _isActive = false;    
    _startTime = 0; // start time over.
    _pausedTime = 0; // start time over.
    _pausePressed = false;
  }

  public void commPause(){
    Client.out.println("commPause received");
    if (!_pausePressed){
      _pausePressed = true;
      pauseTime();

    } else {
      _pausePressed = false;
      unpauseTime();
    }
  }

  public void commGoto(int newTime){
    Client.out.println("commGoto received");
    // check if the file is downloaded
    // yes: display it
    // no: display filler (waiting for file to download)
    //     and in the meanwhile, download the file
    //     when the file is here, show it.
    _startTime = System.currentTimeMillis() - newTime*1000;
  }

  public void setNextFrame(int newFrame){
    Client.out.println("Client setting next frame: " + newFrame);    
    _cache.setNextFrame("" + newFrame);
  }

  public int getLevel(){
    return _level;
  }

  /**
   * change hierarchy downloading /viewing levels
   *
   * @param change: 
   */
  public void changeLevel(String change){

    Client.debug.println("Client setting new level: " + change);
    if (change.indexOf("UP") != -1){
      if (_level > 0){
 	_level--;
      }
      
    } else {
      if (_level < _framesData.length){
 	_level++;
      }
     }
  }
  // ------- END: Comm initiated actions ------- //


  // --------- Viewer initiated actions ---------- //
  /**
   * send the communications controller the news that everybody 
   * needs to start playing
   */
  void playPressed(){
      // start the pseudoWF thread. 
      // this thread is doing a simple version of what 
      // the WF will be doing.
    if (!_isActive)
      _comm.playPressed();
  }

  /**
  * send the communications controller the news that everybody 
   * needs to stop
   */
  void stopPressed(){
    if (_isActive)
      _comm.stopPressed();
  }

  /**
   * send the communications controller the news that everybody 
   * needs to pause
   */
  void pausePressed(){
    if (_isActive)
      _comm.pausePressed();
  }

  /**
   * send the communications controller the news that everybody 
   * needs to goto the given time.
   *
   * @param time: time to goto
   */
  void gotoPressed(int time){
      _comm.gotoPressed(time);
  }

  // --------- END: Viewer initiated actions ---------- //

  // --------- internal clock handling: directly tied to the system clock  ---------- //
  /** 
   * set the start time of the internal clock
   */
  public void startTime(){
    _startTime = System.currentTimeMillis();
  }

  /** 
   * get the current time as indicated inside the clock
   */
  public long currentTime(){
    if (_isActive){
      if (_pausePressed){
	return (_pausedStartTime - _startTime - _pausedTime);
      } else {
	return (System.currentTimeMillis() - _startTime - _pausedTime);
      }
    } else {
      // Client.out.println("current Time is not Active");
      return 0; // dp2041: this actually needs to be something else, 
    }
    // in case the user has already started and is paused.
  }

  /** 
   * pause the time
   */
  public void pauseTime(){
    _pausedStartTime = System.currentTimeMillis();    
  }

  /** 
   * unpause the time
   */
  public void unpauseTime(){
    _pausedTime += (System.currentTimeMillis() - _pausedStartTime);
  }
  // --------- END: internal clock handling ---------- //

  public static void main(String[] args){
    Client c = new Client();
  }
}

