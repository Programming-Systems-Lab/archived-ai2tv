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
  protected String _baseURL;

  protected Client _client;
  private int _bandwidthWindowMax;
  private int _bandwidthWindow;
  private long _totalBytes;
  private long _totalTime;

  private boolean _isActive;
  
  private String _nextFrame; // set next frame to download

  public CacheController(Client c, String name, double rate, String baseURL) {
    _client = c;
    frameFileName = name;
    currLevel = 0;
    currFrame = null;

    _bandwidthWindow = 0;
    _bandwidthWindowMax = 5;  // number of downloads to average against
    _totalBytes = 0;
    _totalTime = 0;
    _baseURL = baseURL;
    
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

    /* right here we should initialize the "cache" by 
     * reading all the files currently already downloaded (prefetched) and 
     * setting each curr[index].setDownloaded(true);
     */
  }
	
  public int getLevel() { return currLevel; }
  public void setLevel(int i) { currLevel = i; }

  public FrameDesc getNextFrame() {
    return nextFrame();
  }
	
  public void setNextFrame(String frame){
    long nextframe = Long.parseLong(frame);
    progress[currLevel] = nextFrameInLevel(currLevel, nextframe);
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
	/*
	System.out.println("level <" + currLevel + 
			   "> index <" + index + 
			   "> bandwidth <" + getBandwidth() + ">");
	System.out.println("CacheController downloading file: " + _baseURL + curr[index].getNum() + ".jpg");
	*/

	if (downloadFile(_baseURL + curr[index].getNum() + ".jpg"))
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
    // System.out.println("CacheController.downloadFile fileURL: " + fileURL);
    String[] tokens = fileURL.split("/");
    String saveFile = cacheDir + "/" + tokens[tokens.length - 1];
    // if "cache" is "initialized" in the ctro, then we can do this: curr[index].setDownloaded(true);
    // otherwise, we'll just check the filesystem, which takes longer!
    
    File newFile = new File (saveFile);
    if (newFile.exists()){
      // System.out.println("file: " + saveFile + " already downloaded");
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
      

    // File newFile = new File (saveFile);
    long currentTime = 0;    
    try {
      // open the connection
      URLConnection myConnection;
      myConnection=url.openConnection();
      // System.out.println("downloading : " + fileURL);
		
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

    // System.out.println("total bytes: " + newFile.length() + " total time: " + currentTime);
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
    // this is a shitty way of doing it, has to go to the filesystem.  need to 
    // instead have a hash of the downloaded files to check.
    File dirFile = new File(cacheDir + "/" + filename);

    if (dirFile.exists())
      return true;
    else 
      return false;
  }

  FrameIndexParser getFramesInfo(){
    return framesInfo;
  }

  /**
   * main is used as a point of access for testing
   */
  public static void main(String[] args){
    // dp2041: testing possibility of threading this class
    // conclusion = yes!

    CacheController cc = new CacheController(null, "frame_index.txt", 1,
					     "http://www1.cs.columbia.edu/~suhit/ai2tv/1/");

    FrameDesc[] fd = new FrameDesc[166];
    FrameDesc newFrame;
    int i=0;
    // for (; i<6; i++){
      do{
	newFrame = cc.getNextFrame();
	System.out.println("got frame: " + newFrame);
	// fd[i++] = newFrame;
	// cc.hierarchyDown(Calendar.getInstance().getTimeInMillis());
      } while(newFrame != null);
      // }
      // cc.hierarchyDown(Calendar.getInstance().getTimeInMillis());
  }
}
