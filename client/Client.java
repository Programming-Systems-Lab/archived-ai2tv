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
 * Main component controlling the others 
 *
 * right now the "Client" is also impersonating the time controller...
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

  private AI2TVJNIJava _JNIIntf;
  private CacheController _cache;
  private CommController _comm;
  private Viewer _viewer;
  private ClientProbe _probe;
  private Thread _probeThread;

  private long _startTime;
  private long _pausedTime;
  private long _pausedStartTime;
  private boolean _pausePressed;

  private boolean _isActive;
  private FrameIndexParser _framesInfo;
  private FrameDesc[][] _framesData;
  private int _level;

  private long _id; // client id number for server identification

  // we need a three frame window in order to be able to detect missed frames
  private FrameDesc _currFrame;
  private FrameDesc _nextFrame;  
  private FrameDesc _neededFrame;

  // quality of service related members
  // we should maybe also add a window to refresh these values every once in a while.
  private int _missedFrames;
  private int _lateFrames;
  private int _earlyFrames;
  private double _lateThreshold;
  private double _earlyThreshold;

  // we should have these in a config file
  String cacheDir = "cache/";
  String baseURL = "http://www1.cs.columbia.edu/~suhit/ai2tv/1/";
  String sienaServer = "ka:localhost:4444";
  String frameFile = "frame_index.txt";
  public static final int FRAME_RATE = 30; // 30 frames / second
  public static final int REFRESH_RATE = 1000; // check for frame changes this often (ms)
  
  /** */
  Client(){
    // what is the prob that two clients start at the exact same time? ...pretty low
    _id = Calendar.getInstance().getTimeInMillis(); 
    _startTime = 0;
    _pausedTime = _pausedStartTime = 0;
    _pausePressed = false;
    _level = 0;
    // _level = 2; // start off at the mid level

    _cache = new CacheController(this, cacheDir);
    // DEBUG: uncomment when the file on the server is correct (has 4 columns)
    // _cache.download(baseURL + frameFile);

    // wait for the frame index file to get here
    //  while(!_cache.isDownloaded(frameFile)){}

    _framesInfo = new FrameIndexParser("c:/pslroot/psl/ai2tv/client/" + cacheDir + frameFile);
    _framesData = _framesInfo.frameData();

    _currFrame = null;
    _nextFrame = null;
    _neededFrame = null;

    _missedFrames = 0;
    _lateFrames = 0;
    _earlyFrames= 0;
    _earlyThreshold = 0.25; // if you can download the file in 1/4 of the time needed, then it's early
    _lateThreshold = 0.75; // if you can download the file in 1/4 of the time needed, then it's early

    _viewer = new Viewer(this);

    _comm = new CommController(this, null, sienaServer);

    startProbe();
  }

  private void startProbe(){
    _probe = new ClientProbe(this);
    _probeThread = new Thread(_probe);
    _probeThread.start();
  }

  private FrameDesc getFrame(int level, long now){
    long currentTime = now / 1000;
    // System.out.println("getting the next frame for : " + level + ", " + currentTime);
    for (int i=0; i<_framesData[level].length; i++){
      // System.out.println("< " + (_framesData[level][i].getStart()/30) + " ? " + currentTime + 
      // " ? " + _framesData[level][i].getEnd()/30 + ">");
      if (_framesData[level][i].getStart()/30 <= currentTime &&
	  currentTime < _framesData[level][i].getEnd()/30)
	return _framesData[level][i];
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
    _probe.shutdown();
    _isActive = false;
  }

  long getID(){
    return _id;
  }

  double getBandwidth(){
    return _cache.getBandwidth();
  }

  FrameDesc getCurrFrame(){
    return _currFrame;
  }

  int getLateFrames(){
    return _lateFrames;
  }

  int getEarlyFrames(){
    return _earlyFrames;
  }

  int getMissedFrames(){
    return _missedFrames;
  }

  // --------- Comm initiated actions ---------- //
  /**
   * pseudoWF that informs the client what to play next.  This 
   * method uses the simple scheme of playing the current frame at the time.
   *
   */
  public void commPlay(){
    System.out.println("commPlay received");
    if (_probe != null){
      _probe.setActive(true);
      _probe.setProbingFrequency (5000);
    }
    
    // have we started, if not, this is it!
    if (_startTime == 0){
      System.out.println("starting time");
      startTime();
    }

    _isActive = true;
    new Thread(){
      public void run(){

	while(_isActive){
	  // System.out.println("paused pressed: " + _pausePressed);
	  if (!_pausePressed){
	    _nextFrame = getFrame(_level, currentTime());

	    if (_nextFrame == null){
	      System.out.println("Are we at the end of the Video?");
	      _isActive = false;
	      break;
	    } 

	    // if time has changed, and we need to show a new frame
	    if (_neededFrame != _nextFrame){
	      if (_currFrame != _neededFrame){
		// System.out.println("missed a frame!: " + _neededFrame);
		_missedFrames++;
		_cache.interruptDownload();
	      }
	      _neededFrame = _nextFrame;
	    }

	    if (_currFrame == null || _currFrame.getNum() != _neededFrame.getNum()){
	      
	      // if (lastFrameDownloaded != null && 

	      if (!_cache.isActive())
		_cache.download(baseURL + _neededFrame.getNum() + ".jpg");
	      
	      if (_cache.isDownloaded(_neededFrame.getNum() + ".jpg")){
		long now = currentTime();
		// then show it.
		_viewer.displayImage(cacheDir + _neededFrame.getNum() + ".jpg");

		// shift the frame window back
		_currFrame = _neededFrame;

		// check if we had actually gotten this frame early!!!
		double percentTimeUsed = (double)(now*30/1000 - _currFrame.getStart()) / 
		                         (double)(_currFrame.getNum() - _currFrame.getStart());
		// System.out.println("current time: " + (now*30/1000) + " frame start: " + _currFrame.getStart()
		// + " frame time: " + _currFrame.getNum()
		// + " percentage = " + percentTimeUsed);
		if (percentTimeUsed < _earlyThreshold){
		  System.out.println("Got the frame early!");
		  _earlyFrames++;
		} 
		else if (percentTimeUsed > _lateThreshold){
		  System.out.println("Got the frame late!");
		  _lateFrames++;
		}
	      } 
	    }
	  }
	  
	  try {
	    sleep(REFRESH_RATE);
	  } catch (InterruptedException e){
	    System.err.println("Client Viewing thread error: " + e);
	    _isActive = false;
	  }

	}
      }
    }.start();
  }

  public void commStop(){
    System.out.println("commStop received");
    _isActive = false;    
    _startTime = 0; // start time over.
    _pausedTime = 0; // start time over.
    _pausePressed = false;
  }

  public void commPause(){
    System.out.println("commPause received");
    if (!_pausePressed){
      _pausePressed = true;
      pauseTime();

    } else {
      _pausePressed = false;
      unpauseTime();
    }
  }

  public void commGoto(int newTime){
    System.out.println("commGoto received");
    // check if the file is downloaded
    // yes: display it
    // no: display filler (waiting for file to download)
    //     and in the meanwhile, download the file
    //     when the file is here, show it.
    _startTime = Calendar.getInstance().getTimeInMillis() - newTime*1000;
  }

  public void setNextFrame(int newFrame){
    System.out.println("Client setting next frame: " + newFrame);    
  }

  public void changeLevel(String change){
    System.out.println("Client setting new level: " + change);
    if (change.indexOf("UP") != -1){
      if (_level > 0)
	_level--;

    } else {
      if (_level < _framesData.length)
	_level++;    
    }
    // reset the _lateFrames, so we can keep count anew
    _missedFrames = 0;
    _lateFrames = 0;
    _earlyFrames= 0;
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
      // System.out.println("current Time is not Active");
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
