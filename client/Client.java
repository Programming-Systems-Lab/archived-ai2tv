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
 * Main component controlling the others.  commPlay is the main function where the main
 * work happens.  The client only responds after receiving a play command from the Siena
 * server, which calls commPlay to start the viewing.
 *
 * right now the "Client" is also impersonating the time controller...
 *
 * [CommController] <--siena--> WF
 *       /\
 *       |
 *       |
 *       \/
 *   [ Client ] <------> [CacheController]
 *        /\   
 *         \  
 *          \ 
 *          \/
 *        [Viewer]
 * 
 *
 * 
 * QUESTIONS: 
 * 
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

  public static PrintStream out; 
  public static PrintStream err;
  public static PrintStream debug; 

  private AI2TVJNIJava _JNIIntf;
  private CacheController _cache;
  private CommController _comm;
  private Viewer _viewer;
  // private ClientProbe _probe;
  private Thread _probeThread;
  private int _level;

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
  private String _cacheDir = "cache/";
  private String _baseURL = "http://www1.cs.columbia.edu/~suhit/ai2tv/1/";
  private String _sienaServer = "ka:localhost:4444";
  private String _frameFile = "frame_index.txt";
  public static final long FRAME_RATE = 30; // 30 frames / second
  public static final long CHECK_RATE = 250; // check if frame is downloaded this often (ms)

  /** 
   * this the amount of buffer time I give to allow processing 
   * delays (lookahead ~ time to process what image to show + 
   * time to load and paint image) in seconds
   */
  private long _lookahead = 1000;

  /** */
  Client(){
    // out = System.out;
    // err = System.err;  
    debug = System.out;  

    try {
      out = new PrintStream(new  FileOutputStream(new File("ai2tv_out.log")), true);
      err = new PrintStream(new  FileOutputStream(new File("ai2tv_err.log")), true);
    } catch (FileNotFoundException e){
      e.printStackTrace();
    }
    

    // what is the prob that two clients start at the exact same time? ...pretty low
    _id = Calendar.getInstance().getTimeInMillis(); 
    _startTime = 0;
    _pausedTime = _pausedStartTime = 0;
    _pausePressed = false;
    _level = 2;
    _timeCurrFrameShown = 0;

    _framesInfo = new FrameIndexParser(_frameFile);
    _framesData = _framesInfo.frameData();

    _viewer = new Viewer(this);
    _comm = new CommController(this, _id, _sienaServer);

    _cache = new CacheController(this, _cacheDir, _baseURL, _frameFile);
    _cache.start(); // start the thread to download frames
    _currFrame = null;
    _nextFrame = null;
    _neededFrame = null;

    startProbe();
  }

  FrameIndexParser getFramesInfo(){
    return _framesInfo;
  }

  private void startProbe(){
    // _probe = new ClientProbe(this);
    // _probeThread = new Thread(_probe);
    // _probeThread.start();
  }

  private FrameDesc getFrame(int level, long now){
    long currentTime = now / 1000;
    // out.println("getting the next frame for : " + level + ", " + currentTime);
    for (int i=0; i<_framesData[level].length; i++){
      // out.println("< " + (_framesData[level][i].getStart()/30) + " ? " + currentTime + 
      // " ? " + _framesData[level][i].getEnd()/30 + ">");
      if (_framesData[level][i].getStart()/30 <= currentTime &&
	  currentTime < _framesData[level][i].getEnd()/30){
	return _framesData[level][i];
      }
    }
    return null;
  }

  private FrameDesc getNextFrame(int level, long now){
    long currentTime = now / 1000;
    // out.println("getting the next frame for : " + level + ", " + currentTime);
    for (int i=0; i<_framesData[level].length; i++){
      // out.println("< " + (_framesData[level][i].getStart()/30) + " ? " + currentTime + 
      // " ? " + _framesData[level][i].getEnd()/30 + ">");
      if (_framesData[level][i].getStart()/30 <= currentTime &&
	  currentTime < _framesData[level][i].getEnd()/30)
	if ((i+1) < _framesData[level].length)
	  return _framesData[level][i+1];
    }
    return null;
  }


  int videoEndTime(){
    return (_framesData[0][_framesData[0].length - 1].getEnd()/ 30 + 1);
  }

  boolean isActive(){
    return _isActive;
  }
  
  void shutdown(){
    // _probe.shutdown();
    _isActive = false;
  }

  long getID(){
    return _id;
  }

  double getBandwidth(){
    return _cache.getBandwidth();
  }

  long getTimeCurrFrameShown(){
    return _timeCurrFrameShown;
  }

  FrameDesc getCurrFrame(){
    return _currFrame;
  }

  void loadImage(String image){
    _viewer.loadImage(image);
  }

  void imageShown(){
    _timeCurrFrameShown = currentTime();
    
    if (_currFrame != null){
      long lateness = _timeCurrFrameShown - _currFrame.getStart()*1000/30;
      debug.println("image: " + _currFrame.getNum() + " shown at: " + _timeCurrFrameShown + 
		    " late: " + lateness + " (ms)");

      _comm.sendUpdate();  // send an update to the other clients
    }
    // 999
    // need to send an update to 
    // _currFrame.getStart() - _timeCurrFrameShown;

  }

  private void startViewerThread(){
    _isActive = true;
    new Thread(){
      public void run(){

	while(_isActive){
	  checkNextFrame();

	  try {
	    sleep(CHECK_RATE);
	  } catch (InterruptedException e){
	    err.println("Client play thread error: " + e);
	    shutdown();
	  }
	}
      }
    }.start();
}
  
  private void checkNextFrame(){
    _nextFrame = getFrame(_cache.getLevel(), currentTime() + _lookahead);
	    
    if (_nextFrame == null){
      out.println("Are we at the end of the Video?");
      _isActive = false;
      return;
    } 

    // if time has changed, and we need to show a new frame
    if (_neededFrame != _nextFrame){
      if (_currFrame != null && _currFrame != _neededFrame){
	// out.println("missed a frame!: " + _neededFrame);
	_cache.interruptDownload();
	// in addition to interrupting the download, it should also
	// tell it the next frame to download and inform the WF
      }
      _neededFrame = _nextFrame;
    }

    if (_currFrame == null || _currFrame.getNum() != _neededFrame.getNum()){
	      
      // out.println("Time is: " + currentTime() + " trying to show frame: " + _neededFrame.getNum());
      if (_cache.isDownloaded(_neededFrame.getNum() + ".jpg")){
	// then show it.
	_viewer.setNewFrame(true);		
	_viewer.displayImage(_cacheDir + _neededFrame.getNum() + ".jpg");

	// shift the frame window back
	_currFrame = _neededFrame;
		
	// from here, the Viewer tries to load in the image, and calls this object's 
	// imageShown method after the image is actually painted on the screen.
      } else {
	Client.debug.println(_neededFrame.getNum() + ".jpg was not downloaded");
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
    out.println("commPlay received");
    // _probe.setActive(true);
    // _probe.setProbingFrequency (5000);
    
    // have we started, if not, this is it!
    if (_startTime == 0){
      out.println("starting time");
      startTime();
    }
    startViewerThread();
  }

  public void commStop(){
    out.println("commStop received");
    _isActive = false;    
    _startTime = 0; // start time over.
    _pausedTime = 0; // start time over.
    _pausePressed = false;
  }

  public void commPause(){
    out.println("commPause received");
    if (!_pausePressed){
      _pausePressed = true;
      pauseTime();

    } else {
      _pausePressed = false;
      unpauseTime();
    }
  }

  public void commGoto(int newTime){
    out.println("commGoto received");
    // check if the file is downloaded
    // yes: display it
    // no: display filler (waiting for file to download)
    //     and in the meanwhile, download the file
    //     when the file is here, show it.
    _startTime = Calendar.getInstance().getTimeInMillis() - newTime*1000;
  }

  public void setNextFrame(int newFrame){
    out.println("Client setting next frame: " + newFrame);    
    _cache.setNextFrame("" + newFrame);
  }

  public int getLevel(){
    return _level;
  }

  public void changeLevel(String change){

    debug.println("Client setting new level: " + change);
    if (change.indexOf("UP") != -1){
      if (_level > 0){
 	_level--;
	_cache.changeLevelUp(currentTime());
      }
      
    } else {
      if (_level < _framesData.length){
 	_level++;
 	_cache.changeLevelDown(currentTime());
      }
     }
  }
  // ------- END: Comm initiated actions ------- //


  // --------- Viewer initiated actions ---------- //
  void playPressed(){
      // start the pseudoWF thread. 
      // this thread is doing a simple version of what 
      // the WF will be doing.
    if (!_isActive)
      _comm.playPressed();
  }

  void stopPressed(){
    if (_isActive)
      _comm.stopPressed();
  }

  void pausePressed(){
    if (_isActive)
      _comm.pausePressed();
  }

  void gotoPressed(int gotoTime){
      _comm.gotoPressed(gotoTime);
  }

  // --------- END: Viewer initiated actions ---------- //




  // --------- really bad impersonation of a clock ---------- //
  /** */
  public void startTime(){
    _startTime = Calendar.getInstance().getTimeInMillis();
  }

  public long currentTime(){
    if (_isActive){
      if (_pausePressed){
	return (_pausedStartTime - _startTime - _pausedTime);
      } else {
	return (Calendar.getInstance().getTimeInMillis() - _startTime - _pausedTime);
      }
    } else {
      // out.println("current Time is not Active");
      return 0; // dp2041: this actually needs to be something else, 
    }
    // in case the user has already started and is paused.
  }

  public void pauseTime(){
    _pausedStartTime = Calendar.getInstance().getTimeInMillis();    
  }

  public void unpauseTime(){
    _pausedTime += (Calendar.getInstance().getTimeInMillis() - _pausedStartTime);
  }
  // --------- END: really bad impersonation of a clock ---------- //

  public static void main(String[] args){
    Client c = new Client();
  }
}
