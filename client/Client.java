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
import java.net.URL;
import java.net.URLConnection;
import java.net.MalformedURLException;
import java.io.*;
import java.util.*;
import psl.ai2tv.gauge.*;
import psl.ai2tv.SienaConstants;

/**
 * Main component controlling the others.  commPlay is where the main
 * fun starts.  after receiving a play message, the client will start
 * the viewing thread.  checkCurrentFrame() is the main function that
 * checks what time it is, what frame we should be showing, etc.  The
 * client only responds after receiving a play command from the Siena
 * server, which calls commPlay to start the viewing.
 *
 *         /---------------------> <--Siena--> --> other clients
 *        /
 * [CommController]
 *     /\
 *     |     /-------> [ ClientProbe ] <--Siena--> WF
 *     |    / /------< [ ClientEffector ] <--Siena--> WF
 *     |   / /
 *     \/ / \/
 *   [ Client ] <------> [CacheController]
 *      /\  
 *       \   
 *        \   
 *        \/
 *      [Viewer]
 *

 *
 * @version	$REvision: $
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

public class Client extends Thread{
  /** the format of the images we're viewing*/
  public static final String IMAGE_FORMAT = ".jpg";
  /** highest possible hierarchy */
  public static final int HIGHEST_LEVEL = 0;
  /** lowest possible hierarchy */
  public static final int LOWEST_LEVEL = 4;
  public static final long CHECK_RATE = 250; // check if frame is downloaded this often (ms)
  public static final int WAIT_CHECK = 500; // check for WG reply this often
  public static final int WAIT_TIMEOUT = 10; // timeout after these many attempts

  public static final String FRAME_FILE = "frame_index.txt";

  // output streams for debugging info
  String lastTime = "";
  public static PrintStream goodness; // added for goodness testing
  public static PrintStream out;
  public static PrintStream err;
  public static PrintStream debug;
  public static PrintStream probeOutput;

  private AI2TVJNIJava _jni;
  private CacheController _cache;
  private CommController _comm;
  private AudioController _audio;
  private Viewer _viewer;
  private TimeController _clock;
  private LoginHandler _login;

  /** current download/viewing hierarchy level */
  private int _level = -1;

  /** frames per second, also determines (inversely) the video playing speed */
  private int _frameRate;

  /** reserved number of frames if the cache and client levels are not the same */
  private int _reserveFrames;

  /** the time it takes for the WF to contact us */
  private int _wfDistance;


  /** indicates whether the client is actively playing a video */
  private boolean _isActive;
  /** indicates whether we are in the middle of adjusting the client */
  private boolean _adjustingClient;
  /** indicates whether we are waiting for a reply from the WG server */
  private boolean _waitingForWGReply;

  /** indicates whether AI2TV is attached to CHIME */
  private boolean _attachedToCHIME;

  private FrameIndexParser _framesInfo;
  private FrameDesc[][] _framesData;

  // we need a three frame window in order to be able to detect missed frames
  private FrameDesc _currentFrame;
  private FrameDesc _nextFrame;
  private FrameDesc _neededFrame;
  private long _timeCurrFrameShown;  // time that the current image was first shown

  /** user id */
  private String _uid;
  /** workgroup id */
  private String _gid;
  /** video session id */
  private String _vsid;
  /** password */
  private String _passwd;
  /** date to view the video */
  private String _dateToView;

  private Hashtable _activeVSIDs;

  // we should have these in a config file
  private String _cacheDir = null;
  private String _baseURL;     // the base url of the available videos
  private String _videoURL;    // the video url holding the frame_index.txt file and the frames
  private String _sienaServer;

  /**
   * this the amount of buffer time I give to allow processing
   * delays (lookahead ~ time to process what image to show +
   * time to load and paint image) in seconds
   */
  private long _lookahead = 0;
  /** WF Probe */
  static ClientProbe probe;

  /** WF Effector */
  private ClientEffector _effector;

  /**
   * Create an AI2TV Client
   */

  Client(AI2TVJNIJava jni) {
    this(true);
    _jni = jni;
  }

  Client(boolean attachedToCHIME) {
    // System.out.println("<Java side> Client ctor");

    _attachedToCHIME = attachedToCHIME;    
    // debugging/logging stuff
    try {
      goodness = new PrintStream(new FileOutputStream(new File("ai2tv_goodness.log")), true);
      goodness.println("SystemTime" + "\t" + "Time" + "\t" + "ClientLevel" + "\t" +
          "Bandwidth" + "\t" + "FrameRate" + "\t" + "CurentFrame");

      out = new PrintStream(new FileOutputStream(new File("ai2tv_out.log")), true);

    } catch (IOException e) {
      e.printStackTrace();
    }

    err = System.err;
    // debug = System.out;
    debug = out;
    probeOutput = out;
    _uid = null;
    _gid = null;
    _vsid = null;
    _activeVSIDs = null;
    _adjustingClient = false;
    _waitingForWGReply = false;

    // set some basic system vars
    getSystemVars();

    // create the audio controller
    _audio = new AudioController();

    // create the comm controller
    _comm = new CommController(this, _sienaServer);
    if (!_comm.isActive()) {
      Client.err.println("Error creating CommController");
      System.exit(1);
    }

    // user login
    if (!_attachedToCHIME)
      _login = new LoginHandler(this);

    // the LoginHandler will then call
    // 1) setLoginInfo 
    // 2) loadVideo 
    // 3) initialize which starts up all other processes (initialize
    // cache, etc)
  }

  /**
   * initialize the Client's components
   */
  private void initialize(){
    _frameRate = 30;

    _timeCurrFrameShown = 0;
    _cache = new CacheController(this, _videoURL);

    if (_cacheDir == null){
      _cacheDir = new String("cache/");
      _cache.setCacheDir(_cacheDir);
    }

    checkFrameFile(FRAME_FILE);

    _framesInfo = new FrameIndexParser(_cacheDir + FRAME_FILE);
    _framesData = _framesInfo.frameData();

    // initialize the internal clock
    _clock = new TimeController();

    // initialize the internal communications controller
    _comm.setupSienaFilter();

    // initialize the audio controller
    String filename = "os_3.wav";
    int bufferSize = 512;

    if (_audio != null){
      boolean audioSuccess = _audio.initializeAudioFile(filename, bufferSize) &&_audio.initializeAudioLine();
      if (!audioSuccess)
	System.err.println("Error, Audio Controller could not initialize.");
    }

    _audio.printAudioFileInfo();

    // initialize the viewer
    if (!_attachedToCHIME)
      _viewer = new Viewer(this);

    _currentFrame = null;
    _nextFrame = null;
    _neededFrame = null;

    _cache.initialize(); // initialize the cache controller

    // select which video quality to load (this set the client level)
    // only if this wasn't set in the environment

    _level = 2;
    if (_level == -1)
      _level = computeVideoQuality(_dateToView);
    debug.println("Client level: " + _level);

    if (_cache.getLevel() == -1)
      _cache.setLevel(_level);

    // start the thread to download frames for this "video"
    _cache.start();

    // attach the probes and effectors
    probe = new ClientProbe(this, _sienaServer, 10); // we will have up to 10 probes set
    _effector = new ClientEffector(this, _sienaServer, probe);
  }

  /**
   * gets needed parameters from the system environment
   */
  private void getSystemVars(){
    _baseURL = System.getProperty("ai2tv.baseURL");
    if (_baseURL == null){
      Client.err.println("Error, you must set the JVM variable ai2tv.baseURL");
      System.exit(0);
    }
    if (!_baseURL.endsWith("/"))
      _baseURL += "/";

    _sienaServer = System.getProperty("ai2tv.server");
    if (_sienaServer == null){
      Client.err.println("Error, you must set the JVM variable ai2tv.server");
      System.exit(0);
    }

    String l = System.getProperty("ai2tv.level");
    if (l != null)
      _level = Integer.parseInt(l);
  }


  /**
   * check that the frame file is downloaded.  if not, go get it.
   *
   * @param frameFile: exact path/filename of the frame index file
   */
  private void checkFrameFile(String frameFile) {
    File fh = new File(_cacheDir + frameFile);

    // if the file doesn't exists, go get it
    if (fh == null || !fh.exists()) {
      System.out.println("videoURL: " + _videoURL);
      System.out.println("frame file not found, going to go download it");
      _cache.downloadFile(_videoURL + frameFile);

    }
  }

  /**
   * @return initialized FrameIndexParser data structure
   */
  FrameIndexParser getFramesInfo() {
    return _framesInfo;
  }


  /**
   * @return frame that should be playing right now
   */
  private FrameDesc getFrame(int level, long now) {
    double currentTime = (double) now / 1000;
    // Client.debug.print("frame to show right now: " + "(" + currentTime + ") : < ");
    for (int i = 0; i < _framesData[level].length; i++) {
      // Client.out.println("< " + (_framesData[level][i].getStart()/_frameRate) + " ? " + currentTime +
      // " ? " + _framesData[level][i].getEnd()/_frameRate + ">");
      if ((double)_framesData[level][i].getStart() / _frameRate <= currentTime &&
          currentTime < (double)_framesData[level][i].getEnd()/_frameRate) {

        // Client.debug.println(_framesData[level][i].getStart()/(double)_frameRate + ", " +
        // _framesData[level][i].getEnd()/(double)_frameRate + "> : " + _framesData[level][i].getNum());
        return _framesData[level][i];
      }
    }
    return null;
  }

  /**
   * @return the clock's current time
   */
  public long currentTime() {
    return _clock.currentTime();
  }

  /**
   * @return length of the video in seconds
   */
  int videoLength() {
    return (int) ((_framesData[0][_framesData[0].length - 1].getEnd() / _frameRate) + 1);
  }

  /**
   * @return whether the client is active
   */
  boolean isActive() {
    return _isActive;
  }

  /**
   * shutdown the client
   */
  void shutdown() {
    Client.debug.println("shutting down the client");
    this.interrupt();
    if (_cache != null){
      Client.debug.println("shutting down the client's cache controller");
      _cache.shutdown();
    }
    if (_comm != null){
      Client.debug.println("shutting down the client's comm controller");
      _comm.shutdown();
    }
    _isActive = false;

    // give the threads 2 seconds to close down
    try {
      sleep(2000);
    } catch (InterruptedException e){
      Client.err.println("Client interrupted during shutdown: " + e);
    }
    System.exit(0);
  }

  /**
   * @return client's UID, 
   */
  String getUID() {
    return _uid;
  }
  /**
   * @param uid: client's UID
   */
  void setUID(String uid) {
    _uid = uid;
  }

  /**
   * @return client's GID, 
   */
  String getGID() {
    return _gid;
  }

  /**
   * @param gid: client's GID,
   */
  void getGID(String gid) {
    _gid = gid;
  }

  /**
   * @return client's VSID, 
   */
  String getVSID() {
    return _vsid;
  }

  /**
   * @param vsid: client's VSID,
   */
  void setVSID(String vsid) {
    _vsid = vsid;
  }

  /**
   * tell the client whether to wait or not to wait
   * for a WG reply
   */
  void setWaitForWGReply(boolean request){
    _waitingForWGReply = request;
  }


  /**
   * @return the current rate which the client computes the frame rate to be
   */
  int getFrameRate() {
    return _frameRate;
  }

  /**
   * @param fr: the current rate which the client computes the frame rate to be
   */
  void setFrameRate(int fr) {
    if (fr > 0)
      _frameRate = fr;
  }

  /**
   * @param dir: the client's directory for holding the frame cache
   */
  void setCacheDir(String dir) {
    if (!dir.endsWith("/"))
      _cacheDir = dir + "/";
    _cacheDir = dir;
    if (_cache != null)
      _cache.setCacheDir(_cacheDir);
  }

  /**
   * @return client's directory for holding the frame cache
   */
  String getCacheDir() {
    return _cacheDir;
  }

  /**
   * @return Client's base URL
   */
  String getBaseURL() {
    return _baseURL;
  }

  /**
   * @param url: Client's base URL
   */
  void setBaseURL(String url) {
    _baseURL = url;
  }
  /**
   * @return client's current bandwidth
   */
  double getBandwidth() {
    if (_cache == null)
      return 0;
    return _cache.getBandwidth();
  }

  /**
   * @return whether the AI2TV client is attached to CHIME
   */
  boolean getAttachedToCHIME() {
    return _attachedToCHIME;
  }

  /**
   * @param uid: the client's username
   * @param gid: the client's group id
   * @param passwd: the client's password
   */
  void setLoginInfo(String uid, String gid, String passwd){
    // Client.debug.println("<Client> setting login info");
    _uid = uid;
    _gid = gid;
    _passwd = passwd;
  }

  /**
   * VERY primitive pull from the server to get the available videos.
   * I get the contents of the baseURL and tokenize the stream.  I
   * then parse this streams searching for a string token ending in a
   * "/", and assumes that the first hit is the parent directory
   * listing, which is ignored.  This puts some important restrictions
   * on the server's html directory layout.
   * 
   * @return array of available videos or a String[] of length 0 if
   * no videos available
   */
  Vector getAvailableVideos(){
    //999: dp2041, this isn't a good place for this.
    if (_comm == null)
      return null;

    _comm.setupWGFilter();

    // first we get the contents of the base URL
    URL url = null;
    try {
      url = new URL(_baseURL);
    } catch (MalformedURLException e) {
      Client.err.println("Bad URL Exception" + e);
    }

    if (url == null)
      return null;

    // here we parse the contents and fill up the available videos aray
    URLConnection myConnection;
    Vector videos = new Vector();
    try {
      Reader reader = new BufferedReader(new InputStreamReader((InputStream)url.getContent()));
      StreamTokenizer input = new StreamTokenizer(reader);

      String token;
      while (input.nextToken() != StreamTokenizer.TT_EOF){
        if (input.sval != null && input.sval.endsWith("/")){
          videos.add(input.sval);
        }
      }
      if (videos.size() > 0)
        videos.remove(0); // the first entry is the parent dir

    } catch (IOException e){
      System.err.println("caught exception: " + e);
      e.printStackTrace();
      return null;
    }

    // get the gid's videos here
    Collection activeVideos = getActiveVSIDs();
    if (activeVideos != null){
      videos.addAll(0, activeVideos);
    }

    return videos;
  }


  /**
   * get active videos in the user's GID
   */
  Collection getActiveVSIDs(){
    if (_comm == null)
      return null;
    
    _comm.getActiveVSIDs();

    waitForWGReply();

    if(_activeVSIDs != null)
      return _activeVSIDs.keySet();
    else 
      return null;
  }

  /**
   * called when a reply to the getActiveVSIDs is answered by the WG
   * server.  This method initialized the Hashtable _activeVSIDs and
   * inserts the relevant values.  The Hashtable is indexed by the
   * activeVSIDInfo because that is the information we start off with
   * later when the user selects the video to view.
   */
  void setActiveVSID(String activeVSIDs, String activeVSIDInfo){
    // example reply: CS4118-10,2003-07-28;08:00,danp,peppo/CS4118-11,2003-07-28;08:00,matias
    Client.debug.println("setActiveVSID: " + activeVSIDs + ", info: " + activeVSIDInfo);
    if (activeVSIDs != null && activeVSIDs != null){
      String[] sessionIDs = activeVSIDs.split("/");
      String[] sessions = activeVSIDInfo.split("/");

      _activeVSIDs = new Hashtable();

      if (sessionIDs.length > 0){
        for (int i=0; i<sessionIDs.length; i++){
          _activeVSIDs.put(sessions[i], sessionIDs[i]);
        }

        // this is in the case if there's only one video session up (no "/" separator)
      } else if (activeVSIDs.length() > 0){
        _activeVSIDs.put(activeVSIDInfo, activeVSIDs);
      }
    }
    _waitingForWGReply = false;
  }

  /**
   * assumes video name and date are valid
   * 
   * @param videoMosh: the name and date of the video, moshed together...
   */
  void loadVideo(String videoMosh){
    // first we get and set the video name and date
    // Client loading video: CS4118-10,2003-08-10;08:00:00,goofy
    Client.debug.println("Client loading video: " + videoMosh);
    String[] info = videoMosh.split(",");

    String videoName = null;
    String date = null;
    if (info.length > 1){
      videoName = info[0];
      date = info[1];
    }

    if (videoName == null || date == null){
      Client.err.println("Error, must input valid format of video name and date");
      return;
    }

    _dateToView = date;

    if (!videoName.endsWith("/"))
      videoName += "/";
    _videoURL = _baseURL + videoName;

    String vsid = null;
    if (_activeVSIDs != null)
      vsid = (String) _activeVSIDs.get(videoMosh);
    // first we check to see if this is an existing video session
    if (vsid != null){
      _vsid = vsid;
      System.out.println("Client joining activeVSID");
      initialize();
      // have to parse out that video info -> videoName and date
      _comm.joinActiveVSID(vsid);
      _login.shutdown();

      // if not, then we have to notify the WG server to get a VSID
    } else {
      _comm.joinNewVSID(videoName, date);

      // wait for the VSID to be received before continuing
      waitForWGReply();
      if (_vsid != null){
        initialize();
	if (!_attachedToCHIME)
	  _login.shutdown();
      }
    }
  }

  /**
   * simply a sleep that waits for the WG reply
   */
  void waitForWGReply(){
    Client.debug.println("waiting for WG reply");
    _waitingForWGReply = true;
    int attempts = 0;
    while(_waitingForWGReply && attempts++ < WAIT_TIMEOUT){
      try {
        sleep(WAIT_CHECK);
      } catch (InterruptedException e){
        Client.err.println("InterruptedException caught while waiting for getActiveVSIDs reply: " + e);
      }
    }
  }

  /**
   * computes the video quality level that is possible to achieve
   * given the current time, bandwidth, and time at which video will
   * start. This method also sets the client level.
   * 
   * @param date: date to get the video by
   */
  private int computeVideoQuality(String date){
    Client.debug.println("computing VideoQuality: " + date);
    // get a preliminary estimate of the bandwidth by downloading the first
    // frame of the highest level.
    if (_cache == null)
      return LOWEST_LEVEL;

    _cache.downloadFile(_videoURL + _framesData[0][0].getNum() + IMAGE_FORMAT);
    double bandwidth = _cache.getBandwidth();
    // double bandwidth = 1.8; // 1.5 kbytes/sec

    // first we get the total frames size of each level
    double[] totalSize = getSizeOfEachLevel();

    // we then parse the date input param
    String[] dayAndTime = date.split(";");
    if (dayAndTime.length != 2) {
      System.err.println("Error, date parameter incorrect format.");
      System.err.println("Correct format: year-month-day;hour:minute:second");
      return LOWEST_LEVEL;
    }
    String[] dateValues = dayAndTime[0].split("-");
    String[] timeValues = dayAndTime[1].split(":");

    if (dateValues.length != 3) {
      System.err.println("Error, date parameter incorrect format.");
      System.err.println("Correct format: year-month-day;hour:minute:second");
      return LOWEST_LEVEL;
    }

    int year = Integer.parseInt(dateValues[0]);
    int month = Integer.parseInt(dateValues[1]) - 1;
    int day = Integer.parseInt(dateValues[2]);

    int hour=0, minute=0, second=0;
    if (timeValues.length > 0)
      hour = Integer.parseInt(timeValues[0]);
    if (timeValues.length > 1)
      minute = Integer.parseInt(timeValues[1]);
    if (timeValues.length > 2)
      second = Integer.parseInt(timeValues[2]);


    // then we create the Calendar representation of the future time
    Calendar later = Calendar.getInstance();
    // the 2nd param is -1 because the month param starts counting at 0
    later.set(year, month, day, hour, minute, second);
    long laterTime = later.getTimeInMillis();

    // we then get our current time and the time available
    long now = System.currentTimeMillis();
    long timeAvailable = (laterTime - now) / 1000;

    // we then compute what at which level we could download
    // then entire "video"
    double timeNeeded;
    int clientLevel = -1;
    for (int i=0; i<totalSize.length; i++){
      timeNeeded = (totalSize[i]/bandwidth);
      if (timeNeeded < timeAvailable){
        clientLevel = i;
        break;
      }
    }

    // if we still can't download the whole thing on time, we'll
    // just set the level according to the pure bandwidth measured.
    double[] avgBandwidthNeeded = _framesInfo.getAvgBandwidthNeeded();
    for (int i=0; i<avgBandwidthNeeded.length; i++){
      if (bandwidth > avgBandwidthNeeded[i]){
        clientLevel = i;
        break;
      }
    }

    // if we still can't hack the downloaded bandwidth needed,
    // we'll set ourselves at the lowest level.
    if (clientLevel == -1)
      clientLevel = LOWEST_LEVEL;

    return clientLevel;
  }

  /**
   * @return int[] of the total sizes (in kbytes) of the entire video
   * for each hierarchy level
   */
  public double[] getSizeOfEachLevel(){
    double[] totalSize = new double[_framesData.length];
    for (int i=0; i<_framesData.length; i++){
      totalSize[i] = 0;
      for (int j=0; j<_framesData[i].length; j++){
        totalSize[i] += _framesData[i][j].getSize();
      }
      totalSize[i] /= 1000; // convert from bytes to kbytes
    }
    return totalSize;
  }

  /**
   * @return time that the current frame was originally shown
   */
  long getTimeCurrFrameShown() {
    return _timeCurrFrameShown;
  }

  /**
   * @return current level of the client
   */
  public int getLevel() {
    return _level;
  }

  /**
   * @return current level of the client's cache controller
   */
  public int getCacheLevel() {
    if (_cache == null)
      return LOWEST_LEVEL;
    return _cache.getLevel();
  }

  /**
   * @return number of prefetched frames in the cache at the client's current level
   */
  public int getReserveFrames() {
    return _reserveFrames;
  }

  /**
   * @param level to check
   * @return number of prefetched frames
   */
  int getNumPrefetchedFrames(int level) {
    if (_cache == null)
      return 0;
    return _cache.getNumPrefetchedFrames(level);
  }

  /**
   * @return current frame showing
   */
  FrameDesc getCurrentFrame() {
    return _currentFrame;
  }

  /**
   * set the time that the current frame was shown
   * @param timeShown: current frame showing
   */
  void setCurrentFrameTimeShown(long timeShown) {
    if (_currentFrame != null)
      _currentFrame.setTimeShown(timeShown);
  }

  private boolean displayImage(){
    boolean displayFrameSuccessful = false;
    if (_attachedToCHIME){
      String textureName = "" + _neededFrame.getNum();
      displayFrameSuccessful = _jni.displayImage(textureName);
    } else {
      displayFrameSuccessful = _viewer.displayImage(_cacheDir + _neededFrame.getNum() + IMAGE_FORMAT);
    }
    return displayFrameSuccessful;
  }

  /**
   * load the image in memory in preparation to be shown
   * @param image: image to load
   */
  void loadImage(String image) {
    // System.out.println("<Java> loading Client.loadImage: " + image);
    if (_attachedToCHIME){
      String[] tokens = image.split("/");
      String imageName = tokens[tokens.length - 1];
      String name = imageName.substring(0,imageName.indexOf(IMAGE_FORMAT));
      String source = "cache/" + imageName;
	
      // System.out.println("<Java> loading Client.loadImage name: " + name + " source: " + source); 
      _jni.loadImage(name, source);

    } else {
      _viewer.loadImage(image);
    }
  }

  /**
   * starts the viewer playing thread that checks what the current frame should be
   */
  private void startViewerThread() {
    Client.debug.println("Client: Starting viewer thread: " + !_isActive);
    if (!_isActive){
      _isActive = true;
      new Thread() {
        public void run() {
          while (_isActive) {
            if (!_adjustingClient)
              checkCurrentFrame();
            try {
              sleep(CHECK_RATE);
            } catch (InterruptedException e) {
              Client.err.println("Client play thread error: " + e);
              shutdown();
            }
          }
        }
      }.start();
    }
  }

  /**
   * checks what time it is + some lookahead factor,
   * and checks to see if:
   * 1) we missed a frame, in which case we interrupt the current download.
   * The cache controller should then start downloading the next frame. 
   * 2) we haven't yet downloaded the frame that was supposed to be
   * showing at this time, so we wait some more
   * 3) we've downloaded the frame, so show it.
   */
  private void checkCurrentFrame() {
    long now = currentTime();
    String thisTime = ((int)(now/60000)) +":"+ ((now/1000)%60);
    long systemNow = System.currentTimeMillis();
    _nextFrame = getFrame(_level, now + _lookahead);
    // Client.debug.println(Calendar.getInstance().getTime() + ": next frame is: " + _nextFrame.getNum());
    if (_nextFrame == null) {

      Client.out.println("Are we at the end of the Video?");
      _isActive = false;
      return;
    }

    // if time has changed, and we need to show a new frame
    // if (_neededFrame != _nextFrame) {
    // if (_nextFrame.getStart() < _neededFrame.getStart() && !_adjustingClient) {
    if (_neededFrame != _nextFrame && !_adjustingClient) {
      // if 1) we're not currently showing the frame we need
      // 2) the frame we need is not downloaded
      // 3) the frame we need is currently trying to be downloaded
      if (_currentFrame != null && _currentFrame != _neededFrame &&
          !_cache.isDownloaded(_nextFrame.getNum() + IMAGE_FORMAT))  {
        Client.debug.println("missed frame: " + _neededFrame.getNum() + "! interrupted by: " + _nextFrame.getNum());
        goodness.println(systemNow + "\t" + thisTime + "\t" + _level + "\t" +
            + getBandwidth() + "\t" + _frameRate + "\t" +
            "missed: " + _neededFrame.getNum());
        probe.endTimeProbe(0, _clock.currentTime(), SienaConstants.AI2TV_FRAME_MISSED);
        _cache.interruptDownload(false);
      }
      _neededFrame = _nextFrame;
    }

    if (_currentFrame == null || _currentFrame.getNum() != _neededFrame.getNum()) {
      if (!probe.getProbeStatus(0)){
        probe.startTimeProbe(0, _clock.currentTime());
      }
      Client.out.println("Time is: " + ((int)(currentTime()/60000))+":"+((int)(currentTime()/1000)%60) +
          " trying to show frame: " + _neededFrame.getNum());
      if (_cache.isDownloaded(_neededFrame.getNum() + IMAGE_FORMAT)) {
        // then show it.
        Client.debug.println(Calendar.getInstance().getTime() + ": showing new frame: " +_neededFrame.getNum() + IMAGE_FORMAT);
        // probe.startTimeProbe(0, (double)_neededFrame.getStart() * 1000 / _frameRate);

        // _viewer.setNewFrame(true);
        boolean displayFrameSuccessful = displayImage();


        if (displayFrameSuccessful){
          _currentFrame = _neededFrame;

          // for WF timing
          setNewFrame();

          // if the cache is downloading frames for a different level, then
          // we need to decrement from that number
          if (_cache.getLevel() != _level)
            _reserveFrames--;
          // from here, the Viewer tries to load in the image, and calls this object's
          // imageShown method after the image is actually painted on the screen.
        }
      } else {
        Client.out.println(_neededFrame.getNum() + ".jpg was not downloaded in time for showing!");
      }
    }

    // for goodness measurements

    if ((((int)(now/1000))%5) == 0 && !thisTime.equals(lastTime)){
      int frameNum = 0;
      if (_currentFrame != null)
        frameNum = _currentFrame.getNum();
      goodness.println(systemNow + "\t" + thisTime + "\t" + _level + "\t" +
          getBandwidth()+ "\t" + _frameRate + "\t" + frameNum );
      lastTime = thisTime;
    }
    if (thisTime.equals("5:0"))
      System.exit(0);
  }

  // --------- ClientEffector initiated actions ---------- //
  /**
   * change viewing/downloading hierarchy level for the client
   *
   * @param newLevel: new level to set the client to
   */
  public void changeLevel(int newLevel) {
    Client.debug.println("Client setting new level: " + newLevel);
    if (HIGHEST_LEVEL <= newLevel && newLevel <= LOWEST_LEVEL)
      _level = newLevel;

    checkReserveFrames();
    // if the cache controller's not doing what it's supposed to be doing, interrupt it.
    FrameDesc fd = _cache.getNextFrame(_level, currentTime());
    if (fd != null && !fd.equals(_cache.getCurrFrame()))
      _cache.interruptDownload(false);
  }

  /**
   * change viewing/downloading hierarchy level for the cache controller
   *
   * @param newLevel: new level for the cache controller
   */
  public void changeCacheLevel(int newLevel) {
    Client.debug.println("CacheController setting new level: " + newLevel);
    if (0 <= newLevel && newLevel <= _framesData.length)
      _cache.setLevel(newLevel);

    checkReserveFrames();
    // if the cache controller's not doing what it's supposed to be doing, interrupt it.
    FrameDesc fd = _cache.getNextFrame(_level, currentTime());
    if (fd != null && !fd.equals(_cache.getCurrFrame()))
      _cache.interruptDownload(false);
  }

  /**
   * check whether we should switch to or from reserve frames
   */
  private void checkReserveFrames(){
    if (_level != _cache.getLevel() && _reserveFrames == 0)
      _reserveFrames = _cache.getNumPrefetchedFrames(_level);
    else 
      _reserveFrames = 0; // reset the reserve frames if the levels are the same
  }

  /**
   * indicate that the frame shown is new
   */
  private void setNewFrame(){
    setCurrentFrameTimeShown(currentTime());
    if (Client.probe.getTimeProbe(0) >= 0)
      Client.probe.endTimeProbe(0, currentTime(), SienaConstants.TIME_OFFSET);
  }

  /**
   * tell the cache controller to jump to this frame.
   */
  public void jumpTo(String newFrame) {
    if (!_adjustingClient){
      _adjustingClient = true;
      _neededFrame = _framesInfo.getFrame(_level, Integer.parseInt(newFrame));
      _cache.jumpTo(newFrame);
      if (_attachedToCHIME){
	// _cacheDir + newFrame + IMAGE_FORMAT
        _jni.displayImage(_cacheDir + newFrame + IMAGE_FORMAT);
      } else {
        _viewer.displayImage(_cacheDir + newFrame + IMAGE_FORMAT);
      }
      setNewFrame();
      _adjustingClient = false;    
    }
  }

  /**
   * @return whether the client is being adjusted (frame wise)
   */
  public boolean adjustingClient() {
    return _adjustingClient;
  }


  // --------- END: ClientEffector initiated actions ---------- //



  // --------- Comm initiated actions ---------- //
  /**
   * CommController's interface to tell the client that a play was pressed
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  public void commPlay(long absTimeSent) {
    Client.debug.println("Client: commPlay method called");
    if (!_isActive){
      _clock.startTime(absTimeSent);
      startViewerThread();
    } else if (_clock != null && _clock.isPaused()){
      // if paused, toggle the paused state
      _clock.pauseTime(absTimeSent);      
    }
  }

  /**
   * CommController's interface to tell the client that a stop was pressed
   */
  public void commStop() {
    Client.debug.println("Client: commStop method called");
    _isActive = false;
    _clock.reset();
  }

  /**
   * CommController's interface to tell the client that a pause was pressed
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  public void commPause(long absTimeSent) {
    Client.debug.println("Client: commPause method called");
    _clock.pauseTime(absTimeSent);
  }

  /**
   * CommController's interface to tell the client that a goto slider action occured
   *
   * @param absTimeSent: absolute system time at which the command was originally sent
   */
  public void commGoto(long absTimeSent, int newTime) {
    Client.debug.println("Client: commGoto method called");
    _clock.gotoTime(absTimeSent, newTime);
  }

  // ------- END: Comm initiated actions ------- //


  // --------- Viewer/CHIME initiated actions ---------- //
  /**
   * send the communications controller the news that everybody
   * needs to start playing
   */
  void playPressed() {
    Client.debug.println("<Client> playPressed method called");
    if (_comm != null) _comm.playPressed();
    if (_audio != null) _audio.play();
  }

  /**
   * send the communications controller the news that everybody
   * needs to stop
   */
  void stopPressed() {
    Client.debug.println("<Client> stopPressed method called");
    if (_isActive){
      if (_comm != null) _comm.stopPressed();
      if (_audio != null) _audio.stop();
    }
  }

  /**
   * send the communications controller the news that everybody
   * needs to pause
   */
  void pausePressed() {
    Client.debug.println("Client: pausePressed method called");
    if (_isActive){
      if (_comm != null) _comm.pausePressed();
      if (_audio != null) _audio.pause();
    }
  }

  /**
   * send the communications controller the news that everybody
   * needs to goto the given time.
   *
   * @param time: time to goto
   */
  void gotoPressed(int time) {
    Client.debug.println("Client: gotoPressed method called");
    if (_isActive){
      if (_comm != null) _comm.gotoPressed(time);
      if (_audio != null) _audio.gotoTimeSeconds((long)time);
    }
  }
  // --------- END: Viewer initiated actions ---------- //

  public static void main(String[] args) {
    boolean attachedToCHIME = false;
    Client c = new Client(attachedToCHIME);
  }

}
