/*
 * @(#)TimeController.java
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

/**
 * Very simple internal time handling tied directly to system clock.
 *
 * @version	$REvision: $
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

class TimeController{
  // internal clock, directly related to system clock
  private long _startTime;
  private long _pausedTime;
  private long _pausedStartTime;
  private boolean _pausePressed;
  private boolean _isActive;

  /**
   * create a time controller
   */
  TimeController(){
    reset();
  }

  /**
   * set the start time of the internal clock
   */
  public void startTime() {
    _isActive = true;
    _startTime = System.currentTimeMillis();
  }

  /**
   * get the current time as indicated inside the clock
   */
  public long currentTime() {
    if (_isActive) {
      if (_pausePressed) {
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
   * toggle pause
   */
  public void pause() {
    if (!_pausePressed) {
      _pausePressed = true;
      pauseTime();
    } else {
      _pausePressed = false;
      unpauseTime();
    }
  }

  /**
   * pause the time
   */
  public void pauseTime() {
    _pausedStartTime = System.currentTimeMillis();
  }

  /**
   * unpause the time
   */
  public void unpauseTime() {
    _pausedTime += (System.currentTimeMillis() - _pausedStartTime);
  }
  
  /**
   * state that the time is the given time
   *
   * @param newTime: new current time
   */
  public void gotoTime(long newTime) {
    _startTime = System.currentTimeMillis() - newTime * 1000;
  }

  /**
   * @return whether the clock is active (true) or stopped (false)
   */
  public boolean isActive() {
    return _isActive;
  }

  /**
   * reset the time
   */
  public void reset() {
    _startTime = 0;
    _pausedTime = 0;
    _pausedStartTime = 0;
    _pausePressed = false;
    _isActive = false;
  }
}
