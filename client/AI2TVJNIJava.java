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
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class AI2TVJNIJava extends Thread{
  // private AI2TVCommController _comm;

  AI2TVJNIJava(){
    System.out.println("Java side <AI2TVJNIJava constructor>");    
    // _comm = new AI2TVCommController(this);
  }


  // ----- JNI related functions called by the C++ side ----- //
  private void playPressed(){
    System.out.println("Java side <play>");
  }

  private void pausePressed(){
    System.out.println("Java side <pause>");
  }

  private void ffPressed(){
    System.out.println("Java side <ff>");
  }

  private void rwPressed(){
    System.out.println("Java side <rw>");
  }
  // --- END: JNI related functions called by the C++ side -- //


  // ----- JNI related functions implemented on the C++ side ----- //
  // the following are function stubs implemented on the C++ side, 
  // accessed through the library loaded in the static block
  native void displayFrame(String frameNum);
  native void shutdown();
  static {
    System.loadLibrary("AI2TVJNICPP");
  }
  // --- END: JNI related functions implemented on the C++ side -- //

  public static void main(String[] args) {
    AI2TVJNIJava intf = new AI2TVJNIJava();
    return;
  }
}
