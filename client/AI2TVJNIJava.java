/*
 * @(#)AI2TVJNIJava.java
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

/**
 * The Java side JNI interface for the AI2TV client.
 *
 * @version    $Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class AI2TVJNIJava{
  private Client _client;

  /**
   * Creates an interface between CHIME and the Client
   */
  AI2TVJNIJava(){
    System.out.println("Java side <AI2TVJNIJava constructor>");    
    System.out.println("creating the client");    
    _client = new Client(this);
    System.out.println("done creating the client");    
  }


  // ----- JNI related functions called by the C++ side ----- //
  /**
   * Indicate to the Client that play was pressed
   */
  void playPressed(){
    System.out.println("Java side <play>");
    // _client.playPressed();
  }

  /**
   * Indicate to the Client that pause was pressed
   */
  void pausePressed(){
    System.out.println("Java side <pause>");
    _client.pausePressed();
  }


  /**
   * Indicate to the Client that stop was pressed
   */
  void stopPressed(){
    System.out.println("Java side <stop>");
    _client.stopPressed();
  }

  /**
   * Slider function to goto a certain time
   * 
   * @param time: time to jump to
   */
  void gotoPressed(int time){
    System.out.println("Java side <goto>: " + time);
    _client.gotoPressed(time);
  }

  /**
   * @return the client's current video time in seconds
   */
  long currentTime(){
    return _client.currentTime();
  }

  /**
   * @return the length of the video (in seconds)
   */
  int videoLength(){
    return _client.videoLength();
  }

  /**
   * set the client's cache directory
   * 
   * @param dir: directory to store frame cache
   */
  void setCacheDir(String dir){
    _client.setCacheDir(dir);
  }

  /**
   * get the client's cache directory
   * 
   * @return dir: directory used to store frame cache
   */
  String getCacheDir(){
    return _client.getCacheDir();
  }

  /**
   * set the client's base URL
   * 
   * @param url: URL with the location of the available videos
   */
  void setBaseURL(String url){
    _client.setBaseURL(url);
  }

  /**
   * get the client's base URL
   * 
   * @return baseURL: URL with the location of the available videos
   */
  String getBaseURL(){
    return _client.getBaseURL();
  }

  /**
   * Set the user login information in the AI2TV module.
   * 
   * NOTE!!! Need the rest of the login info to add to the param list
   * 
   * @param login: login information
   * @param passwd
   */
  void setLoginInfo(String login, String passwd, String server,
		    String uid, String gid){
    ; // _client.login(info);
  }

  /**
   * tell the AI2TV module what video to load and when to load it by 
   * 
   * @param name: name of the video
   * @param date: date/time to load the video by
   */
  void loadVideo(String name, String date){
    _client.loadVideo(name +";"+ date);
  }


  /**
   * initialize the AI2TV component 
   */
  void initialize(){
    _client.initialize();
  }

  /**
   * @return list of available videos from the server
   */
  String[] getAvailableVideos(){
    java.util.Vector v = _client.getAvailableVideos();

    String[] availableVideos;
   if (v != null && v.size() > 1)
      availableVideos = new String[v.size() + 1];
    else {
      availableVideos = new String[3];
      availableVideos[0] = "CS4118-10";
      availableVideos[1] = "CS4118-11";
      availableVideos[2] = "CS4118-12";
      return availableVideos;
      // return new String[0];
    }

    for (int i=0; i<v.size(); i++)
      availableVideos[i] = v.get(i).toString();

    return availableVideos;
  }

  /**
   * shutdown the client
   */
  void shutdown(){
    _client.shutdown();
  }


  // --- END: JNI related functions called by the C++ side -- //


  // ----- JNI related functions implemented on the C++ side ----- //
  // the following are function stubs implemented on the C++ side, 
  // accessed through the library loaded in the static block
  /**
   * Tell CPP side to load a certain frame into memory
   * 
   * @param frame: name of the image file to display
   */
  native void loadImage(String frame);

  /**
   * Tell CPP side to display a certain frame 
   * 
   * @param frame: name of the image file to display
   */
  native boolean displayImage(String frame);

  /**
   * the static block used to load the appropriate CPP library
   */
  static {
    System.loadLibrary("AI2TVJNICPP");
  }
  // --- END: JNI related functions implemented on the C++ side -- //

  public static void main(String[] args) {
    AI2TVJNIJava intf = new AI2TVJNIJava();
    return;
  }
}

