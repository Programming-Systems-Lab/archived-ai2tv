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
    _client = new Client();
  }


  // ----- JNI related functions called by the C++ side ----- //
  /**
   * Indicate to the Client that play was pressed
   */
  private void playPressed(){
    System.out.println("Java side <play>");
    _client.playPressed();
  }

  /**
   * Indicate to the Client that pause was pressed
   */
  private void pausePressed(){
    System.out.println("Java side <pause>");
    _client.pausePressed();
  }


  /**
   * Indicate to the Client that stop was pressed
   */
  private void stopPressed(){
    System.out.println("Java side <stop>");
    _client.stopPressed();
  }

  /**
   * Slider function to goto a certain time
   * 
   * @param time: time to jump to
   */
  private void gotoPressed(int time){
    System.out.println("Java side <goto>: " + time);
    _client.gotoPressed(time);
  }
  // --- END: JNI related functions called by the C++ side -- //


  // ----- JNI related functions implemented on the C++ side ----- //
  // the following are function stubs implemented on the C++ side, 
  // accessed through the library loaded in the static block
  /**
   * Set the cache dir for CrystalSpace
   * 
   * @param dir: name of the cache dir
   */
  native void setCacheDir(String dir);

  /**
   * Tell CPP side to load a certain frame into memory
   * 
   * @param frame: name of the image file to display
   */
  native void loadFrame(String frame);

  /**
   * Tell CPP side to display a certain frame 
   * 
   * @param frame: name of the image file to display
   */
  native void displayFrame(String frame);

  /**
   * shutdown the process
   */
  native void shutdown();

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

