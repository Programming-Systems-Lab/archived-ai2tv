/*
 * @(#)AI2TVCommController.java
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
import java.net.*;

/**
 * The Communications Controller of the AI2TV client.  Controls TCP
 * and UDP server/client ports.
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

class AI2TVCommController{
  public static final int DEBUG = 0;

  final int MAXPACKETSIZE = 512; // 512 is the max size of a UDP pakcet
  public static PrintStream out = System.out;
  public static PrintStream err = System.err;

  private DatagramSocket _udpSocket;
  private boolean _isActive = false;

  AI2TVCommController(){
    _isActive = true;
    new Thread() {
      public void run() {
	  while(_isActive) {
	    try {
	      _udpSocket = new DatagramSocket(1234);
	      byte[] buffer = new byte[MAXPACKETSIZE];
	      DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
	      _udpSocket.receive(packet);
	      String msg = new String(packet.getData(), 0, packet.getLength());
	      System.out.println(new java.util.Date() + ": " + msg);
	      packet.setLength(MAXPACKETSIZE); // !!! Necessary !!!
	      _isActive = false;
	    } catch (SocketException e) {
	      err.println("Caught exception: " + e);
	    } catch (IOException e) {
	      err.println("Caught exception: " + e);
	    }
	  }
      }
    }.start();
  }

  void shutdown(){
    _isActive = false;
    _udpSocket.close();
  }

  public static void main(String args[]){
    AI2TVCommController foo = new  AI2TVCommController();
  }
}
