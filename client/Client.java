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
 * [ Client ]-----[CommController] <--siena--> WF
 *      \    \       /   |
 *       \    \     /    \/
 *        \    \---/-[CacheController]
 *         \      /     /\
 *          \     |     /
 *           \    |    /
 *            \   \/   \/
 *             \--[Viewer]
 * 
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

class Client {
  private CacheController _cache;
  private CommController _comm;
  private Viewer _viewer;
  private long start;
  private long paused;
  private long pausedStart;
  
  /** */
  Client(){
    String cacheDir = "cache/";
    String framesURL = "http://www1.cs.columbia.edu/~suhit/ai2tv/1/";
    String sienaServer = "ka:localhost:4444";
    String frameFileURL = "http://www.cs.columbia.edu/~suhit/1/frame_index.txt";
    String frameFile = "frame_index.txt";
    double framerate = 30; // frames per sec?

    start = paused = pausedStart = 0;

    _cache = new CacheController(frameFile, framerate, this);
    _viewer = new Viewer();
    _comm = new CommController("AI2TV", sienaServer, null, _viewer, _cache);

    // here's a test run from the client.  
    



    // get some frames...
    // this should really be called from the CommController in response
    // to events, or according to some internal plan that it's following
    FrameDesc[] loaded = new FrameDesc[3];
    loaded[0] = _cache.getNextFrame();
    loaded[1] = _cache.getNextFrame();
    loaded[2] =_cache.getNextFrame();

    // play some frames...
    // this should really be called from the CommController or the CacheController
    // in response to events such as a file downloading succesfully, etc.
    for (int i=0; i<loaded.length; i++){
      _viewer.displayImage(cacheDir + loaded[i].getNum() + ".jpg");
      try {
	Thread.currentThread().sleep(2000);
      } catch (InterruptedException e){
	; // do nothing right now.
      }
    }

    // also, there should be some coordination between the CommController
    // and the CacheController.
    
  }

  private void checkFrameFile(){
  }

  // --------- really bad impersonation of a clock ---------- //
  /** */
  public void startTime(){
    start = Calendar.getInstance().getTimeInMillis();
  }

  public long currentTime(){
    return (Calendar.getInstance().getTimeInMillis() - start - paused);
  }

  public void pauseTime(){
    pausedStart = Calendar.getInstance().getTimeInMillis();    
  }

  public void unpauseTime(){
    paused += (Calendar.getInstance().getTimeInMillis() - pausedStart);
  }
  // --------- done: really bad impersonation of a clock ---------- //


  public static void main(String[] args){
    Client c = new Client();
  }
}
