/*
 * @(#)Histogram.java
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

package psl.ai2tv.gauge;

import java.util.*;

/**
 * Generate a Histogram data structure.
 *
 * warning, non-growing!
 * To switch to a dynamic Histogram, we would use a Vector instead of an array.
 * There are performace issues, however, with growing the vector from the beginning (adding
 * elements before the beginning).
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
public class Histogram {
  final static int DEBUG = 0;

  /** main data structure, bins */
  private int _bin[];

  /** beginning point at which to look for data points */
  private int _windowBeg;

  /** end point at which to stop looking for data points */
  private int _windowEnd;

  /** width of the bins */
  private int _binWidth;

  /** number of bins */
  private int _numBins;


  /**
   * create a Histogram data object.
   *
   * @param data: data to be entered.
   * @param windowBeg: beginning at which to look for data points
   * @param windowEnd: end point at which to stop looking for data points
   * @param binWidth: width of the bins
   */
  public Histogram(int[] data, int windowBeg, int windowEnd, int binWidth){
    _windowBeg = windowBeg;
    _windowEnd = windowEnd;
    _binWidth = binWidth;
    _numBins = (_windowEnd - _windowBeg)/_binWidth + 1;
    if (DEBUG > 0)
      System.out.println("window size: "  + (_windowEnd - _windowBeg) + " # bins: " + _numBins);
    _bin = new int[_numBins];

    // initialize histogram to 0
    for (int i=0; i<_numBins; i++)
      _bin[i] = 0;

    insertData(data);
  }

  /**
   * insert one data point
   *
   * @param data: data point to enter
   */
  public void insertData(int data){

    int windowIndex = _windowBeg;
    int binIndex = 0;
    while(data > (windowIndex+_binWidth)){
      windowIndex += _binWidth;
      binIndex++;
    }

    if (data < _windowEnd)
      _bin[binIndex]++;
  }

  /**
   * Insert an array of data.
   *
   * @param data[]: array of points to enter
   */
  public void insertData(int data[]){
    Arrays.sort(data);  // can't rely on user to do this, so we do it here.

    int windowIndex = _windowBeg;
    int binIndex = 0;
    for (int i=0; i<data.length; i++){
      while(data[i] >= (windowIndex+_binWidth)){
	windowIndex += _binWidth;
	binIndex++;
      }

      _bin[binIndex]++;
    }
  }

  /**
   * @return the bin data
   */
  public int[] getData(){
    return _bin;
  }

  /**
   * @return the beginning of the data window
   */
  public int getWindowBeg(){
    return _windowBeg;
  }

  /**
   * @return the end of the data window
   */
  public int getWindowEnd(){
    return _windowEnd;
  }

  /**
   * @return the width of the bins
   */
  public int getBinWidth(){
    return _binWidth;
  }

  /**
   * print out the data in visual format
   */
  public void print(){
    for (int i=0; i<_bin.length; i++){
      for (int j=0; j<_bin[i]; j++){
	System.out.print("*");
      }
      System.out.println("");
    }
  }

  /**
   * prints out all the data in the histogram
   */
  public String toString(){
    String s = "";
    for (int i=0; i<_bin.length; i++)
      s += " " + _bin[i];

    return s;
  }

  public static void main(String args[]){

    if (args.length < 2){
      System.err.println(" usage: java Histogram index_file binwidth");
      System.err.println(" example: java Histogram frame_index.txt 3000");
      System.exit(0);
    }
    String filename = args[0];

    // retrieve the data from the frame index file.
    FrameIndexParser fip = new FrameIndexParser(filename);
    int levels = fip.levels();
    int[] frameNum = fip.frameNum();
    int[][][] frameData = fip.frameData();

    int windowBeg = 0;
    int windowEnd = 0;
    int binWidth = Integer.parseInt(args[1]);


    int i, j, k;
    for (i=0, j=0; i<levels; i++)
      // the total number of frames
      j += frameNum[i];

    int[] data = new int[j];
    int time;
    for (i=0, k=0 ; i<levels; i++){
      for (j=0; j<frameNum[i]; j++)
	{
	  time = frameData[i][j][0];
	  if (time < windowBeg)
	    windowBeg = time;
	  if (time > windowEnd)
	    windowEnd = time;
	  data[k++] = time;
	}
    }

    System.out.println("window start, end: " + windowBeg + "," + windowEnd);
    Histogram h = new Histogram(data, windowBeg, windowEnd, binWidth);

    // print the histogram in visual format
    h.print();

    // print the histogram data
    System.out.println("h: " + h);
  }
}
