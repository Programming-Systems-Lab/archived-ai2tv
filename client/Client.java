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
  private FrameDesc _currFrame;

  private int _missedFrames; // client id number for server identification
  private long _id; // client id number for server identification

  // we should have these in a config file
  String cacheDir = "cache/";
  String baseURL = "http://www1.cs.columbia.edu/~suhit/ai2tv/1/";
  String sienaServer = "ka:localhost:4444";
  String frameFile = "frame_index.txt";
  public static final int FRAMERATE = 30; // 30 frames / second
  public static final int CHECK_FOR_CHANGE = 2000; // check for frame changes this often (ms)
  
  /** */
  Client(){
    // what is the prob that two clients start at the exact same time? ...pretty low
    _id = Calendar.getInstance().getTimeInMillis(); 
    _startTime = 0;
    _pausedTime = _pausedStartTime = 0;
    _pausePressed = false;

    _cache = new CacheController(this, frameFile, FRAMERATE, baseURL);
    _cache.start(); // start the thread to download frames

    _framesInfo = _cache.getFramesInfo();
    _framesData = _framesInfo.frameData();
    _currFrame = null;

    _viewer = new Viewer(this);

    _comm = new CommController(this, null, sienaServer);

    _probe = new ClientProbe(this);
    // maybe I should start the client probe later on in the commPlay method() instead
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
    _probe.setActive(true);
    _probe.setProbingFrequency (10000);
    
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
	    // check to see if the next frame is downloaded
	    FrameDesc nextFrame = getFrame(_cache.getLevel(), currentTime());
	    
	    if (nextFrame == null){
	      System.out.println("Are we at the end of the Video?");
	      _isActive = false;
	      break;
	    } 
	    // check whether it's downloaded
	    // System.out.println("next frame to display: " + nextFrame.getNum());
	    if (_currFrame == null || _currFrame.getNum() != nextFrame.getNum()){
	      if (_cache.isDownloaded(nextFrame.getNum() + ".jpg")){
		// then show it.
		_viewer.displayImage(cacheDir + nextFrame.getNum() + ".jpg");
		_currFrame = nextFrame;
	      } else {
		// we missed a frame
		_missedFrames++;
	      }
	    }
	  }
	  
	  try {
	    sleep(CHECK_FOR_CHANGE);
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
    _cache.setNextFrame("" + newFrame);
  }

  public void changeHierarchy(String change){
    System.out.println("Client setting new hierarchy: " + change);
    if (change.equals("UP"))
      _cache.hierarchyUp(currentTime());
    else 
      _cache.hierarchyDown(currentTime());
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
