/*
 * @(#)CacheController.java
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

import java.util.*;
import java.io.*;
import java.net.*;
import siena.*;
import siena.comm.*;

import psl.ai2tv.gauge.FrameDesc;
import psl.ai2tv.gauge.FrameIndexParser;

/**
 * Controls the downloading and maintence of the frames.
 *
 * Note: the bandwidth window can be adjusted to buffer
 * the accuracy of the current bandwidth calculation.
 *
 * Note: isDownloaded checks physically if the file is there.
 * 
 * TODO:
 * - need to think about permissions ie. protected vs. private
 * - must take care of the case where, if we skip a couple frames
 * and reach the end (thread is finished, run method has completed)
 * and the Viewer rewinds to a missing frame.  then the frame requested
 * must be downloaded at that time.
 */
public class CacheController extends Thread {

  protected FrameIndexParser framesInfo;
  protected int progress[];
  protected String frameFileName;
  protected int currLevel;
  protected FrameDesc currFrame;
  protected int numLevels;
  protected String _cacheDir;
  protected String _baseURL;
  protected Set _cache;

  protected Client _client;
  
  protected int _bandwidthWindowMax;
  protected int _bandwidthWindow;
  protected long _totalBytes;
  protected long _totalTime;

  protected boolean _isActive;
  protected boolean _interrupt;

  public CacheController(Client c, String cacheDir, String baseURL, String frameFile) {
    _client = c;
    _cache = new HashSet();
    if (cacheDir.endsWith("/"))
      _cacheDir = cacheDir;
    else 
      _cacheDir = cacheDir + "/";
    frameFileName = frameFile;
    currLevel = _client.getLevel();
    currFrame = null;
    _interrupt = false;

    _bandwidthWindow = 0;
    _bandwidthWindowMax = 5;  // number of downloads to average against
    _totalBytes = 0;
    _totalTime = 0;
    _baseURL = baseURL;

    File f = new File(_cacheDir);
    if (!f.exists()){
      f.mkdir();
    } else if (!f.isDirectory()) {
      Client.err.println("Error: " + _cacheDir + " had existed, but is not a directory");
      return;
    }

    framesInfo = _client.getFramesInfo();
    numLevels = framesInfo.levels();
    progress = new int[framesInfo.levels()];
    for (int i = 0; i < progress.length; i++)
      progress[i] = 0;
  }
	
  void interruptDownload() {
    _interrupt = true;
    this.interrupt();
  }

  public int getLevel() { return currLevel; }
  public void setLevel(int i) { currLevel = i; }

  /*
  public FrameDesc getNextFrame() {
    return nextFrame();
  }
  */	

  public void setNextFrame(String frame){
    long nextframe = Long.parseLong(frame);
    progress[currLevel] = nextFrameInLevel(currLevel, nextframe);
  }

  public void changeLevelDown(long now) {
    if (currLevel  < numLevels -1 ) { 
      currLevel++;
      Client.out.print ("Down to level " + currLevel + " : ");
      progress[currLevel] = nextFrameInLevel(currLevel, now);
    }
  }

  public void changeLevelUp(long now) {
    if (currLevel > 0)	{
      currLevel --;
      Client.out.print ("Up to level " + currLevel + " : ");
      progress[currLevel] = nextFrameInLevel(currLevel, now);
    }
  }
	
  public FrameDesc getNextFrame() {
    FrameDesc[] curr = framesInfo.frameData()[currLevel];
    int index = progress[currLevel];
    if (index < curr.length) {
      if (! curr[index].isDownloaded()){
	// try {
	// Thread.currentThread().sleep(downloadInterval);
	/*
	Client.out.println("level <" + currLevel + 
			   "> index <" + index + 
			   "> bandwidth <" + getBandwidth() + ">");
	Client.out.println("CacheController downloading file: " + _baseURL + curr[index].getNum() + ".jpg");
	*/

	if (downloadFile(_baseURL + curr[index].getNum() + ".jpg")){
	  curr[index].setDownloaded(true);
	  _client.loadImage(_cacheDir + curr[index].getNum() + ".jpg");
	}
      }
      progress[currLevel] = index + 1;
      currFrame = curr[index];		
      return currFrame;
    }
    else 
      return null;
  }
	
  private int nextFrameInLevel (int level, long now) {
    
    FrameDesc[] curr = framesInfo.frameData()[level];
    //int i = progress[level];
    int i = 0;
    while (curr[i].getEnd() <= now) {
      // Client.out.print (curr[i].getStart() + " - ");
      i++;
			
    }
    //Client.out.print("\n");
    return i;
  }

  /**
   * download a file from the given URL into the cache dir specified
   * in cacheDir.  With smart download feature!  resumes a file from
   * the right place if it had been interrupt earlier.
   *
   * @param fileURL: URL of the file to get.
   */
  private boolean downloadFile(String fileURL) {
    // Client.out.println("CacheController.downloadFile fileURL: " + fileURL);
    String[] tokens = fileURL.split("/");
    String saveFile = _cacheDir + tokens[tokens.length - 1];
    // if "cache" is "initialized" in the ctro, then we can do this: curr[index].setDownloaded(true);
    // otherwise, we'll just check the filesystem, which takes longer!
    
    URL url = null;
    try {
      url = new URL(fileURL);
    } catch (MalformedURLException e){
      Client.err.println("error in downloader: " + e);
      return false;
    }

    if (url == null) {
      Client.out.println("bad URL");
      return false;
    }
      
    long currentTime = 0;    
    try {
      // open the connection
      URLConnection myConnection;
      myConnection=url.openConnection();
      // Client.out.println("downloading : " + fileURL);
		
      // check that the file holds stuff
      if (myConnection.getContentLength()==0) {
	Client.out.println("Error Zero content.");
	return false;
      }

      long i = myConnection.getContentLength();
      // Client.out.println("downloading file length: " + myConnection.getContentLength());
      if (i==-1) {
	Client.out.println("Empty or invalid content.");
	return false;
      }

      File newFile = new File (saveFile);
      boolean append = false;
      long resumeIndex = 0;
      // Client.out.println("current file size: " + newFile.length());
      if (newFile.exists()){
	append = true;
	resumeIndex = i - newFile.length();
	if (resumeIndex == 0){
	  _cache.add(saveFile);
	  return true;
	}
      }

      BufferedInputStream input = new BufferedInputStream(myConnection.getInputStream());
      newFile.createNewFile();
      BufferedOutputStream downloadFile = new BufferedOutputStream(new FileOutputStream(newFile, append));
      int c;
      currentTime = Calendar.getInstance().getTimeInMillis();
      
      while (((c=input.read())!=-1) && (--i > 0)){
	if (!append || i < resumeIndex){
	  if (_interrupt) {
	    _interrupt = false;
	    return false;
	  }
	  downloadFile.write(c);
	}
      }
      currentTime = Calendar.getInstance().getTimeInMillis() - currentTime;

      input.close();
      downloadFile.close();
      
      // BANDWIDTH related stuff
      // Client.out.println("total bytes: " + newFile.length() + " total time: " + currentTime);
      if (_bandwidthWindow++ < _bandwidthWindow){
	_totalBytes += newFile.length();
	_totalTime += currentTime;
      } else {
	_totalBytes = newFile.length();
	_totalTime = currentTime;
	_bandwidthWindow = 0;
      }      

    } catch (IOException e){
      Client.out.println("IOException in CacheController.downloadFile(): " + e);
      return false;
    }

    _cache.add(saveFile);
    return true;
  }

  double getBandwidth() {
    if (_totalTime == 0)
      return 0;
    else 
      return (_totalBytes / _totalTime);
  }

  public void run(){
    _isActive = true;
    while(_isActive){
      getNextFrame();
    }
    _isActive = false;
  }

  public boolean isActive(){
    return _isActive;
  }

  /**
   * shutdown the CacheController thread.
   */
  void shutdown(){
    _isActive = false;
  }

  /**
   * check whether a file has been downloaded
   * @param 
   * @return whether the specified file is already downloaded.
   */
  boolean isDownloaded(String filename){
    return _cache.contains(_cacheDir + filename);
  }

  /**
   * main is used as a point of access for testing
   */
  public static void main(String[] args){
    // dp2041: testing possibility of threading this class
    // conclusion = yes!

    /*
    CacheController cc = new CacheController(null, "frame_index.txt", 1,
					     "http://www1.cs.columbia.edu/~suhit/ai2tv/1/");

    FrameDesc[] fd = new FrameDesc[166];
    FrameDesc newFrame;
    int i=0;
    // for (; i<6; i++){
      do{
	newFrame = cc.getNextFrame();
	Client.out.println("got frame: " + newFrame);
	// fd[i++] = newFrame;
	// cc.hierarchyDown(Calendar.getInstance().getTimeInMillis());
      } while(newFrame != null);
      // }
      // cc.hierarchyDown(Calendar.getInstance().getTimeInMillis());
      */
  }
}
