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

  protected Client _client;
  protected String _cacheDir;
  protected Set _cache;
  protected boolean _isActive;
  protected boolean _interrupt;
  protected Thread _downloadThread;
  protected String _currFile; // the current file downloading

  // bandwidth measurement related members
  protected int _bandwidthWindowMax;
  protected int _bandwidthWindow;
  protected long _totalBytes;
  protected long _totalTime;
  



  public CacheController(Client c, String cacheDir) {
    _client = c;
    if (cacheDir.endsWith("/"))
      _cacheDir = cacheDir;
    else 
      _cacheDir = cacheDir + "/";
    _cache = new HashSet();
    _currFile = "NONE";

    _bandwidthWindow = 0;
    _bandwidthWindowMax = 5;  // number of downloads to average against
    _totalBytes = 0;
    _totalTime = 0;

    
    File f = new File(_cacheDir);
    if (!f.exists()){
      f.mkdir();
    } else if (!f.isDirectory()) {
      System.err.println("Error: " + cacheDir + " had existed, but is not a directory");
      return;
    } 
  }

  void interruptDownload() {
    if (_downloadThread != null)
      _downloadThread.interrupt();
    // _interrupt = true;
  }

  boolean download(String fileURL) {
    // if we're not already downloading a file.
    if (!_isActive){
      _currFile = fileURL;
      _downloadThread = new Thread(this);
      _downloadThread.start();
      return true;
    } else {
      return false;    
    }
  }

  public void run(){
    // System.out.println("entering thread to download: " + _currFile);
    _isActive = true;
    downloadFile(_currFile);
    _downloadThread = null;
    _isActive = false;
    // System.out.println("done with thread, downloaded: " + _currFile);
    _currFile = "NONE";
  }

  /**
   * get the current file being downloaded
   */
  public String getCurrentFile(){
    return _currFile;
  }

  public boolean isActive(){
    return _isActive;
  }

  /**
   * download a file from the given URL into the cache dir specified
   * in cacheDir.  Smart download feature: resumes a file from the right 
   * place if it had been interrupt earlier.
   * 
   * @param fileURL: URL of the file to get.
   */
  private boolean downloadFile(String fileURL) {
    // System.out.println("CacheController.downloadFile fileURL: " + fileURL);
    String[] tokens = fileURL.split("/");
    String saveFile = _cacheDir + tokens[tokens.length - 1];
    // if "cache" is "initialized" in the ctro, then we can do this: curr[index].setDownloaded(true);
    // otherwise, we'll just check the filesystem, which takes longer!
    
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

      long i = myConnection.getContentLength();
      // System.out.println("downloading file length: " + myConnection.getContentLength());
      if (i==-1) {
	System.out.println("Empty or invalid content.");
	return false;
      }

      File newFile = new File (saveFile);
      boolean append = false;
      long resumeIndex = 0;
      // System.out.println("current file size: " + newFile.length());
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
	  downloadFile.write(c);
	  /*
	    if (_interrupt) {
	    _interrupt = false;
	    return false;
	    }
	  */
	}
      }
      currentTime = Calendar.getInstance().getTimeInMillis() - currentTime;

      input.close();
      downloadFile.close();
      
      // BANDWIDTH related stuff
      // System.out.println("total bytes: " + newFile.length() + " total time: " + currentTime);
      if (_bandwidthWindow++ < _bandwidthWindow){
	_totalBytes += newFile.length();
	_totalTime += currentTime;
      } else {
	_totalBytes = newFile.length();
	_totalTime = currentTime;
	_bandwidthWindow = 0;
      }      

    } catch (IOException e){
      System.out.println("IOException in CacheController.downloadFile(): " + e);
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
  }

}
