/*
 * @(#)FrameIndexParser.java
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

import java.io.*;
import java.util.*;

/**
 * Parses the frame index file and puts it in an accessible data
 * structure.
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class FrameIndexParser {
  /** debugging utility */
  public static final int DEBUG = 0;

  /** output stream */
  public static PrintStream out = System.out;
  /** error stream */
  public static PrintStream err = System.err;

  /** data fields on each line <br>
   * [0] frame number, <br>
   * [1] start of frame context, <br>
   * [2] end of frame context.
   */
  private final int _numTimeData = 3;
  /** the number of architectural levels */
  private int _levels;
  /** the number of frames in each level */
  private int[] _frameNum;

  /**
   * the main data structure holding all the data in the frame index
   * file <br>
   *
   * Note that this structure wastes space in that it allocates an NxN
   * array but doesn't use it all.  Space here is traded for the
   * quickness and ease in using this data structure
   *
   * indexed by: <br>
   * [i] hierarchy level <br>
   * [h] frame index <br>
   * [j] data number in frame (see _numTimeData) <br>
   */
  private int[][][] _frameData;

  /**
   * the secondary data structure holding all the data in the frame index
   * file <br>
   *
   * indexed by: <br>
   * [i] hierarchy level <br>
   * [h] frame number <br>
   */
  private ArrayList _frameTimes;

  /**
   * Creates a FrameIndexParser parsing the given filename.
   *
   * @param filename
   */
  public FrameIndexParser(String filename){
    _parseFile(filename);
    _createReverseLookup();
  }

  /**
   * parse the file in the data structure
   *
   * @param filename: filename of frame index file
   */
  private void _parseFile(String filename){
    if (DEBUG > 0)
      out.println("filename: " + filename);

    try {
      BufferedReader in = new BufferedReader(new FileReader(filename));

      // get the number of levels in this architecture
      _levels = Integer.parseInt(in.readLine().trim());
      if (DEBUG > 0)
	out.println(" levels: " + _levels);

      // get the frame numbers for each level
      _frameNum = new int[_levels];
      int i, j, k;
      for (i=0; i<_levels; i++){
	_frameNum[i] = Integer.parseInt(in.readLine().trim());
	if (DEBUG > 0)
	  out.println(" frameNum: [" + i + "]: " + _frameNum[i]);
      }


      // get the frame times for each level
      // assuming that the first level is the highest
      _frameData = new int[_levels][_frameNum[0]][3];
      String[] tempBuffer;
      for (i=0; i<_levels; i++){
	for (j=0; j<_frameNum[i]; j++){
	  tempBuffer = in.readLine().trim().split("\\s");
	  if (DEBUG > 0)
	    out.print(" level: [" + i + "]: " +
		      " frame: [" + j + "]: ");
	  for (k=0; k<_numTimeData; k++){
	    _frameData[i][j][k] = Integer.parseInt(tempBuffer[k].trim());
	    if (DEBUG > 0)
	      out.print(_frameData[i][j][k] + " ");
	  }
	  if (DEBUG > 0)
	    out.println("");
	}
      }


    } catch (IOException e){
      err.println("Caught IOException: " + e);
    }
    if (DEBUG > 0)
      out.println("Finished extracting data");
  }

  /**
   * print out the data that we've extracted
   */
  public void print(){
    if (_frameData == null) {
      out.println("No data");
      return;
    }

    int i, j, k;
    out.println(" levels: " + _levels);
    for (i=0; i<_levels; i++)
      out.println(" frameNum: [" + i + "]: " + _frameNum[i]);
    for (i=0; i<_levels; i++){
      for (j=0; j<_frameNum[i]; j++){
	out.print(" level: [" + i + "]: " +
		  " frame: [" + j + "]: ");
	for (k=0; k<_numTimeData; k++)
	  out.print(_frameData[i][j][k] + " ");
	out.println("");
      }
    }
  }

  /**
   * create the reverse lookup indexed by hierarchy level and frame number.
   * this method uses data from the main structure, so that struct must be
   * initialized first.
   */
  private void _createReverseLookup(){
    _frameTimes = new ArrayList();
    int i, j;
    for (i=0; i<_frameNum.length; i++){
      Hashtable tempHash = new Hashtable(_frameNum[i]);
      for (j=0; j<_frameNum[i]; j++){
	ArrayList tempArray = new ArrayList(2);
	tempArray.add(0, new Integer(_frameData[i][j][1]));
	tempArray.add(1, new Integer(_frameData[i][j][2]));
	tempHash.put(new Integer(_frameData[i][j][0]), tempArray);
      }
      _frameTimes.add(i, tempHash);
    }
  }

  /**
   * @return the number of time related data elements
   */
  public int numTimeData(){
    return _numTimeData;
  }

  /**
   * @return the number of levels
   */
  public int levels(){
    return _levels;
  }

  /**
   * @return the number frameNum array
   */
  public int[] frameNum(){
    return _frameNum;
  }

  /**
   * @return the data of the frame index file indexed by: <br>
   * [i] hierarchy level <br>
   * [j] frame index <br>
   * [k] data number in frame (see _numTimeData) <br>
   */
  public int[][][] frameData(){
    return _frameData;
  }

  /**
   * @return the data of the frame index file indexed by: <br>
   * [i] hierarchy level, which returns an ArrayList <br>
   * [j] frame number, which returns a Hashtable <br>
   * [k] frame number, which returns an Integer <br>
   *
   * DO NOT USE THIS TO ACCESS THE DATA! Use getFrameTime() instead.
   */
  private ArrayList frameTimes(){
    return _frameTimes;
  }

  /**
   * get the specified frame time
   *
   * @param level: heirarchy level
   * @param frameNumber: the actual frame number
   * @param index: beginning [0] or end of the window [1]
   * @return the time specified by the parameters
   */
  public int getFrameTime(int level, int frameNumber, int index){
    return ((Integer)((ArrayList)
		      ((Hashtable) _frameTimes.get(level)).
		      get(new Integer(frameNumber)))
	    .get(index)).intValue();
  }

  public static void main(String[] args){
    if (args.length != 1){
      out.println("usage: java FrameIndexParser frame_index.txt");
      System.exit(0);
    }

    long a = java.util.Calendar.getInstance().getTimeInMillis();
    FrameIndexParser fip = new FrameIndexParser(args[0]);
    long b = java.util.Calendar.getInstance().getTimeInMillis();
    System.out.println("time to create fip: " + (b-a));
    // fip.print();
  }

}
