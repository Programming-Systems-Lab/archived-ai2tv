/* * @(#)CacheController.java * * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved * * Copyright (c) 2001: @author Dan Phung * Last modified by: Dan Phung (dp2041@cs.columbia.edu) * * CVS version control block - do not edit manually *  $RCSfile$ *  $Revision$ *  $Date$ *  $Source$ */package psl.ai2tv.client;import java.util.*;import java.io.*;import java.net.*;import siena.*;import siena.comm.*;import psl.ai2tv.gauge.FrameDesc;import psl.ai2tv.gauge.FrameIndexParser;/** * Controls the downloading and maintence of the frames. * * Note: the bandwidth window can be adjusted to buffer * the accuracy of the current bandwidth calculation. *  * TODO: * - need to think about permissions ie. protected vs. private * - must take care of the case where, if we skip a couple frames * and reach the end (thread is finished, run method has completed) * and the Viewer rewinds to a missing frame.  then the frame requested * must be downloaded at that time. * * WF related probes * 1) * * @version	$Revision$ * @author	Dan Phung (dp2041@cs.columbia.edu) */public class CacheController extends Thread {  // the higher level associated client  protected Client _client;  // members related to tracking what to download and what is downloaded.  protected FrameIndexParser framesInfo = null;  protected FrameDesc[][] framesData;  protected FrameDesc _currFrame;  protected int numLevels;  protected int _level;  protected String _cacheDir;  protected String _baseURL;  protected Set _cache;  protected boolean _isActive;  protected boolean _interrupt;  protected boolean _interruptSuccessful;  protected boolean _adjustingClient;  // bandwidth measurement related members  protected int _bandwidthWindowMax;  protected int _bandwidthWindow;  protected long _totalBytes;  protected double _totalTime;  /** the format of the images we're viewing*/   protected String _imageFormat = ".jpg";  /** highest possible hierarchy */  protected final int HIGHEST_LEVEL = 4;  /** lowest possible hierarchy */  protected final int LOWEST_LEVEL = 0;  /**   * Create a CacheController.  Immediately begins to download at our   * level.  Note that the downloading method holds intelligence to   * resume interrupted/incomplete downloads.   *    * The initial level is the same as the client's level, but during   * the lifetime of the video client, the two levels may differ   * according to hierarchy level shifts.   *    * @param c: parent client to download files for   * @param cacheDir: directory name of target downloaded files   * @param baseURL: base URL of the files to download from   */  public CacheController(Client c, String cacheDir, String baseURL) {    _client = c;    _cache = new HashSet();    if (cacheDir.endsWith("/"))      _cacheDir = cacheDir;    else       _cacheDir = cacheDir + "/";    _currFrame = null;    _interrupt = false;    _interruptSuccessful = false;    _adjustingClient = false;    _bandwidthWindow = 0;    _bandwidthWindowMax = 5;  // number of downloads to average against    _totalBytes = 0;    _totalTime = 0;    _baseURL = baseURL;    File f = new File(_cacheDir);    if (!f.exists()){      f.mkdir();    } else if (!f.isDirectory()) {      Client.err.println("Error: " + _cacheDir + " had existed, but is not a directory");      return;    }  }  /**    * initalize the CacheController's vars that weren't known before   */  void initialize(){    framesInfo = _client.getFramesInfo();    _level = _client.getLevel();        framesData = framesInfo.frameData();    numLevels = framesInfo.levels();    preloadCache();  }  /**   * preload any frames already downloaded   */  private void preloadCache(){    File dir = new File(_cacheDir);    File file;    String fileName;    String[] files = null;    if (dir.isDirectory() && dir.canRead()){      files = dir.list();      long fileSize, frameSize;      int frameNum;      FrameDesc fd = null;      for (int i=0; i<files.length; i++){	fileName = files[i];	if (fileName.toLowerCase().endsWith(".jpg")){	  file = new File(_cacheDir + fileName);	  fileSize = file.length();	  frameNum = Integer.parseInt(fileName.substring(0,fileName.length() - 4)); // 4 = ".jpg"	  for (int level=0; level<numLevels; level++){	    fd = framesInfo.getFrame(level, frameNum);	    if (fd != null)	      break;	  }	  frameSize = fd.getSize() - 1;	  if (frameSize == fileSize){	    _cache.add(_cacheDir + fileName);	    _client.loadImage(_cacheDir + fileName);	  }	}      }    }  }  /**   * interrupt the current download   *    */  void interruptDownload(boolean force) {    if (!_interrupt && (!_adjustingClient || force)){      _interrupt = true;      this.interrupt();      _interruptSuccessful = false;      while (!_interruptSuccessful){	try {	  Thread.sleep(333);	} catch (InterruptedException e){	  Client.err.println("Error, interrupt download interrupted while waiting for completion");	}      }      _interrupt = false;    }  }  /**   * Plan for the frame to be shown at some future time.  This   * function is usually invoked by the WF to adjust the client that   * is having trouble keeping up with the base client.  Here we   * interrupt the current download and start downloading the   * specified frame.   *   * @param frame: filename of frame to jump to   */  public void jumpTo(String frame){    _adjustingClient = true;    interruptDownload(true);    // download the given frame    if (downloadFile(_baseURL + frame + _imageFormat)){      _cache.add(_cacheDir + frame + _imageFormat);      _client.loadImage(_cacheDir + frame + _imageFormat);    } else {      Client.err.println("error in downloading file: " + _currFrame.getNum() + _imageFormat);    }    _adjustingClient = false;  }  /**   * gets the next frame in the level thats not downloaded   *    * @param level: the level of the frame   * @param now: the time at which to find corresponding next frame   * @return the next frame in the given level according to now.   */  FrameDesc getNextFrame(int level, long now) {    Client.out.println("getting the next frame for : " + level + ", " + now);    double time = (double)now /1000;    double end, num;    String file2check;    FrameDesc fd;    for (int i = 0; i < framesData[level].length; i++) {      fd = framesData[level][i];      end = (double)fd.getEnd()/_client.getFrameRate();      // double end = (double)framesData[level][i].getEnd()/_client.getFrameRate();      file2check = fd.getNum() + _imageFormat;      // Client.out.println("checking file: " + file2check + " if " + start + " >= " + time +      // "for level: " + level);      if (!isDownloaded(file2check) && end >= time) {	// Client.out.println("going to download: < " + start + " ? " + time + " ? " + end + "> " + framesData[level][i].getNum());		return fd;      }    }    return null;  }  /**   * check the number of prefetched frames for the given level   *    * @param level to check   * @return number of prefetched frames for the given level   */  int getNumPrefetchedFrames(int level) {        double now = (double)_client.currentTime() /1000;    double start, num;    String file2check;    FrameDesc fd;    int viewedIndex=-1, downloadedIndex=-1, i=0;    for (; i < framesData[level].length; i++) {      fd = framesData[level][i];      start = (double)fd.getStart()/_client.getFrameRate();      file2check = fd.getNum() + _imageFormat;      if (start >= now && viewedIndex == -1) {	// if the start of the frame is greater than now, then we've	// passed the frame that we should have been viewing.	if (i == 0)	  viewedIndex = 0;	else 	  viewedIndex = i-1;      }      // System.out.println("CacheController checking: " + file2check + ": " + isDownloaded(file2check));      if (!isDownloaded(file2check) && downloadedIndex == -1) {	// if the we've not downloaded this frame, then the last 	// index is what we've already downloaded.	if (i == 0)	  downloadedIndex = 0;	else 	  downloadedIndex = i-1;      }      if (viewedIndex != -1 && downloadedIndex != -1)	break;    }    if (viewedIndex == -1) viewedIndex = framesData[level].length;    if (downloadedIndex == -1) downloadedIndex = framesData[level].length;    // Client.debug.println("CacheController computing prefetched frames for level: "     // + level + " found downloaded " + downloadedIndex +    // " - viewed: " + viewedIndex + " = " + (downloadedIndex - viewedIndex));    return (downloadedIndex - viewedIndex);  }  /**   * @return the current operating hierarchy level   */  int getLevel() {    return _level;  }  /**   * @param level: new hierarchy level to operate at   */  void setLevel(int l) {    if (LOWEST_LEVEL <= l && l <= HIGHEST_LEVEL)      _level = l;  }  /**   * @return the current bandwidth value in kbyte/s   */  double getBandwidth() {    if (_totalTime == 0)      return 0;    else       return (_totalBytes / _totalTime / 1000);  }  /**   * @return frame currently downloading   */  FrameDesc getCurrFrame() {    return _currFrame;  }  /**   * continually download frames at this levell   */  public void run(){    if (framesInfo == null)      initialize();    _isActive = true;    FrameDesc fd = null;    while(_isActive){      _currFrame = getNextFrame(_level, _client.currentTime());      if (_currFrame == null) {	// wait for a while, maybe we'll change levels and have to go back, etc...	try {	  Client.out.println(Calendar.getInstance().getTime()+": CacheController nothing to get");	  sleep(500);	} catch (InterruptedException e) {	  Client.err.println("CacheController downloading thread error: " + e);	}      } else {	Client.out.println(Calendar.getInstance().getTime()+": CacheController getting: "+ _currFrame);	if (!_adjustingClient && downloadFile(_baseURL + _currFrame.getNum() + _imageFormat)){	  System.out.println("CacheController downloaded file: " + _currFrame.getNum() + _imageFormat);	  _currFrame.setDownloaded(true);	  _currFrame.setTimeDownloaded(_client.currentTime());	  _client.loadImage(_cacheDir + _currFrame.getNum() + _imageFormat);	} else {	  // Client.err.println("error in downloading file: " + _currFrame.getNum() + _imageFormat);	}      }      // slow down the thread from hogging cpu      // try {      // 	sleep(100);      // } catch (InterruptedException e) {      // Client.err.println("CacheController downloading thread error: " + e);      // }    }    _isActive = false;  }  /**   * @return whether the CacheController is currently active.   */  public boolean isActive(){    return _isActive;  }  /**   * shutdown the CacheController thread.   */  void shutdown(){    _isActive = false;  }  /**   * check whether a file has been downloaded   * @param    * @return whether the specified file is already downloaded.   */  boolean isDownloaded(String filename){    return _cache.contains(_cacheDir + filename);  }  /**   * download a file from the given URL into the cache dir specified   * in cacheDir.  With smart download feature!  resumes a file from   * the right place if it had been interrupt earlier.   *   * @param fileURL: URL of the file to get.   */  boolean downloadFile(String fileURL) {    Client.out.println("CacheController.downloadFile fileURL: " + fileURL);    String[] tokens = fileURL.split("/");    String saveFile = _cacheDir + tokens[tokens.length - 1];    // if "cache" is "initialized" in the ctor, then we can do this: curr[index].setDownloaded(true);    // otherwise, we'll just check the filesystem, which takes longer!    URL url = null;    try {      url = new URL(fileURL);    } catch (MalformedURLException e){      Client.err.println("error in downloader: " + e);      return false;    }    if (url == null) {      Client.out.println("bad URL");      return false;    }          double currentTime = 0;    try {      // open the connection      URLConnection myConnection;      myConnection=url.openConnection();      // Client.out.println("downloading : " + fileURL);      // check that the file holds stuff      if (myConnection.getContentLength()==0) {	Client.out.println("Error Zero content.");	return false;      }      long i = myConnection.getContentLength();      // Client.out.println("downloading file length: " + myConnection.getContentLength());      if (i==-1) {	Client.out.println("Empty or invalid content.");	return false;      }      File newFile = new File (saveFile);      boolean append = false;      long resumeIndex = 0;      // Client.out.println("current file size: " + newFile.length());      if (newFile.exists()){	append = true;	resumeIndex = i - newFile.length();	if (resumeIndex == 0){	  _cache.add(saveFile);	  return true;	}      }      BufferedInputStream input = new BufferedInputStream(myConnection.getInputStream());      newFile.createNewFile();      BufferedOutputStream downloadFile = new BufferedOutputStream(new FileOutputStream(newFile, append));      int c;      currentTime = System.currentTimeMillis();      while (((c=input.read())!=-1) && (--i > 0)){	if (_interrupt) {	  Client.debug.println("!!! " + newFile + " interrupted in the midst of downloading!");	  _interrupt = false;	  _interruptSuccessful = true;	  return false;	}	if (!append || i < resumeIndex){	  downloadFile.write(c);	  downloadFile.flush();	}      }      currentTime = System.currentTimeMillis() - currentTime;      input.close();      downloadFile.close();      // BANDWIDTH related stuff      Client.out.println("total bytes: " + newFile.length() + " total time: " + currentTime);      if (_bandwidthWindow++ < _bandwidthWindow){	_totalBytes += newFile.length();	_totalTime += (currentTime / 1000);      } else {	_totalBytes = newFile.length();	_totalTime = (currentTime / 1000);	_bandwidthWindow = 0;      }          } catch (IOException e){      Client.out.println("IOException in CacheController.downloadFile(): " + e);      return false;    }    _cache.add(saveFile);    return true;  }  /**   * main is used as a point of access for testing   */  public static void main(String[] args){    /*      CacheController cc = new CacheController(null, "frame_index.txt", 1,      "http://www1.cs.columbia.edu/~suhit/ai2tv/1/");      FrameDesc[] fd = new FrameDesc[166];      FrameDesc newFrame;      int i=0;      // for (; i<6; i++){      do{      newFrame = cc.getNextFrame();      Client.out.println("got frame: " + newFrame);      // fd[i++] = newFrame;      // cc.hierarchyDown(System.currentTimeMillis());      } while(newFrame != null);      // }      // cc.hierarchyDown(System.currentTimeMillis());      */  }}