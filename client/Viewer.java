/*
 * @(#)Viewer.java
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

import java.awt.*;
import java.awt.event.*;

/*
 * Viewer.java
 */
class Viewer extends Frame {
  private Image _image;
  private int _menubarSize = 30; // the size of the menu bar
  private int REFRESH_RATE = 2000; // ms
  private String _filename;
  private MediaTracker mediaTracker;
  private Toolkit toolkit;

  Viewer() {
    toolkit = Toolkit.getDefaultToolkit();
    mediaTracker = new MediaTracker(this);

    _filename = "ai2tv.jpg";
    _image = toolkit.createImage(_filename);

    addWindowListener(new WindowAdapter() {
	public void windowClosing(WindowEvent e) {
	  System.exit(0);
	}
      });
    // setSize(_image.getWidth(null) + 5, _image.getHeight(null) + 40);
    setSize(325, 275);
    show();
  }

  public void paint(Graphics graphics) {
    setTitle(_filename);
    mediaTracker.addImage(_image, 0);
    try {
      mediaTracker.waitForID(0);
    } catch (InterruptedException ie) {
      System.err.println(ie);
      System.exit(1);
    }
    graphics.drawImage(_image, 0, 30, null);
    // start the refreshing thread
    try {
      Thread.currentThread().sleep(REFRESH_RATE);
    } catch (InterruptedException e) { }
    repaint();
  }

  void displayImage(String filename) {
    _image = toolkit.createImage(filename);
    _filename = filename;
    System.out.println("Viewer displaying: " + filename);
  }

  public static void main(String[] args) {

    // test out the Viewer on a couple of images
    Viewer v = new Viewer();
    v.displayImage("images/112.jpg");
    v.displayImage("images/1108.jpg");
  }
}
