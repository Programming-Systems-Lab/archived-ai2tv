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
 *
 * TODO:
 * - must take care of the case where, if we skip a couple frames
 * and reach the end (thread is finished, run method has completed)
 * and the Viewer rewinds to a missing frame.  then the frame requested
 * must be downloaded at that time.
 */
public class CacheController extends Thread {

  protected FrameIndexParser framesInfo;
  protected int progress[];
  private String frameFileName;
  private File frameFile = null;
  protected int currLevel;
  protected FrameDesc currFrame;
  protected int numLevels;
  String cacheDir = "cache";

  // we should get this from the config file.
  String baseURL = "http://www1.cs.columbia.edu/~suhit/ai2tv/1/";

  protected Client _client;
  private int _bandwidthWindowMax;
  private int _bandwidthWindow;
  private long _totalBytes;
  private long _totalTime;

  private boolean _isActive;

  public CacheController(Client c, String name, double rate) {
    _client = c;
    frameFileName = name;
    currLevel = 0;
    currFrame = null;

    _bandwidthWindow = 0;
    _bandwidthWindowMax = 5;  // number of downloads to average against
    _totalBytes = 0;
    _totalTime = 0;

    framesInfo = new FrameIndexParser(frameFileName);
    numLevels = framesInfo.levels();
    progress = new int[framesInfo.levels()];
    for (int i = 0; i < progress.length; i++)
      progress[i] = 0;

    File f = new File(cacheDir);
    if (!f.exists()){
      f.mkdir();
    } else if (!f.isDirectory()) {
      System.err.println("Error: " + cacheDir + " had existed, but is not a directory");
      return;
    }
  }
	
  public int getLevel() { return currLevel; }
  public void setLevel(int i) { currLevel = i; }

  public FrameDesc getNextFrame() {
    return nextFrame();
  }
	
  public void hierarchyDown(long now) {
    if (currLevel  < numLevels -1 ) { 
      currLevel++;
      System.out.print ("Down to level " + currLevel + " : ");
      progress[currLevel] = nextFrameInLevel(currLevel, now);
    }
  }
  
  public void hierarchyUp(long now) {
    if (currLevel > 0)	{
      currLevel --;
      System.out.print ("Up to level " + currLevel + " : ");
      progress[currLevel] = nextFrameInLevel(currLevel, now);
    }
  }
	
  protected FrameDesc nextFrame() {
    FrameDesc[] curr = framesInfo.frameData()[currLevel];
    int index = progress[currLevel];
    if (index < curr.length) {
      if (! curr[index].isDownloaded()){
	// try {
	// Thread.currentThread().sleep(downloadInterval);
	System.out.println("level <" + currLevel + 
			   "> index <" + index + 
			   "> bandwidth <" + calcBandwidth() + ">");
	System.out.println("CacheController downloading file: " + baseURL + curr[index].getNum() + ".jpg");

	if (downloadFile(baseURL + curr[index].getNum() + ".jpg"))
	  curr[index].setDownloaded(true);
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
      // System.out.print (curr[i].getStart() + " - ");
      i++;
			
    }
    //System.out.print("\n");
    return i;
  }

  /**
   * download a file from the given URL into the cache dir specified
   * in cacheDir.
   *
   * @param fileURL: URL of the file to get.
   */
  private boolean downloadFile(String fileURL) {
    String[] tokens = fileURL.split("/");
    String saveFile = cacheDir + "/" + tokens[tokens.length - 1];
    File newFile = new File (saveFile);
    long currentTime = 0;
    if (newFile.exists()){
      System.out.println("file: " + saveFile + " already downloaded");
      return true;
    }

    URL url = null;
    try {
      url = new URL(fileURL);
    } catch (MalformedURLException e){
      System.err.println("error in downloader: " + e);
      return false;
    }

    if (url == null) {
      System.out.println("bad URL");
      return false;
    }
      

    try {
      // open the connection
      URLConnection myConnection;
      myConnection=url.openConnection();
      System.out.println("downloading : " + fileURL);
		
      // check that the file holds stuff
      if (myConnection.getContentLength()==0) {
	System.out.println("Error Zero content.");
	return false;
      }

      int i = myConnection.getContentLength();
      if (i==-1) {
	System.out.println("Empty or invalid content.");
	return false;
      }
      // System.out.println("Length : " + i + " bytes");

      BufferedInputStream input = new BufferedInputStream(myConnection.getInputStream());
      int p=0;
      newFile.createNewFile();
      BufferedOutputStream downloadFile = new BufferedOutputStream(new FileOutputStream(newFile));
      int c;
      currentTime = Calendar.getInstance().getTimeInMillis();
      while (((c=input.read())!=-1) && (--i > 0))
	downloadFile.write(c);
      currentTime = Calendar.getInstance().getTimeInMillis() - currentTime;

      input.close();
      downloadFile.close();
      
    } catch (IOException e){
      System.out.println("IOException in CacheController.downloadFile(): " + e);
      return false;
    }

    System.out.println("total bytes: " + newFile.length() + " total time: " + currentTime);
    if (_bandwidthWindow++ < _bandwidthWindow){
      _totalBytes += newFile.length();
      _totalTime += currentTime;
    } else {
      _totalBytes = newFile.length();
      _totalTime = currentTime;
      _bandwidthWindow = 0;
    }      


    return true;
  }

  private double calcBandwidth() {
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

  void shutdown(){
    _isActive = false;
  }

  public static void main(String[] args){
    // dp2041: testing possibility of threading this class
    // conclusion = yes!

    CacheController cc = new CacheController(null, "frame_index.txt", 1);

    FrameDesc[] fd = new FrameDesc[166];
    FrameDesc newFrame;
    int i=0;
    do{
      newFrame = cc.getNextFrame();
      fd[i++] = newFrame;
      cc.hierarchyDown(Calendar.getInstance().getTimeInMillis());
    } while(newFrame != null);

  }
}
