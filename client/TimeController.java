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

public class TimeController{
  // internal clock, directly related to system clock
  private long _startTime;
  private long _pausedTime;
  private long _pausedStartTime;
  private boolean _pauseActive;
  private boolean _isActive;

  /**
   * create a time controller
   */
  public TimeController(){
    reset();
  }

  /**
   * start the time of the internal clock
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  public void startTime(long absSentTime) {
    if (!_isActive){
      _isActive = true;
      // _startTime = System.currentTimeMillis() ;
      _startTime = absSentTime;
    } else if (_pauseActive){
      pauseTime(absSentTime);
    }
  }

  //999
  public long getStartTime() {
    return _startTime;
  }

  /**
   * get the current time as indicated inside the clock
   */
  public long currentTime() {
    if (_isActive) {
      if (_pauseActive) {
	return (_pausedStartTime - _startTime - _pausedTime);
      } else {
	return (System.currentTimeMillis() - _startTime - _pausedTime);
      }

    } else {
      return 0; // if we're not active, time is 0
    }
    // in case the user has already started and is paused.
  }

  /**
   * toggle pause
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  public void pauseTime(long absTimeSent) {
    if (!_pauseActive) {
      _pauseActive = true;
      pause(absTimeSent);
    } else {
      _pauseActive = false;
      unpause(absTimeSent);
    }
  }

  /**
   * pause the time
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  private void pause(long absTimeSent) {
    // _pausedStartTime = System.currentTimeMillis();
    _pausedStartTime = absTimeSent;
  }

  /**
   * unpause the time
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  private void unpause(long absTimeSent) {
    // _pausedTime += (System.currentTimeMillis() - _pausedStartTime);
    _pausedTime += (absTimeSent - _pausedStartTime);
  }

  /**
   * @return whether the time is paused.
   */
  public boolean isPaused() {
    return _pauseActive;
  }
  


  /**
   * state that the time is the given time
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   * @param newTime: new current time
   */
  public void gotoTime(long absTimeSent, long newTime) {
    // _startTime = System.currentTimeMillis() - newTime * 1000;
    _startTime = absTimeSent - newTime * 1000;
  }

  /**
   * reset the time, basically the STOP function
   */
  public void stopTime() {
    reset();
  }

  /**
   * @return whether the clock is active (true) or stopped (false)
   */
  public boolean isActive() {
    return _isActive;
  }

  /**
   * reset the time, basically the STOP function
   */
  public void reset() {
    _startTime = 0;
    _pausedTime = 0;
    _pausedStartTime = 0;
    _pauseActive = false;
    _isActive = false;
  }
}
