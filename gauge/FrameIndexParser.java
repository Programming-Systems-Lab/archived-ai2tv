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
public class FrameIndexParser {
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
  /** the number of hierarchy levels */
  private int _levels;
  /** the number of frames in each level */
  private int[] _frameNum;

  /**
   * the main data structure holding all the data in the frame index
   * file <br>
   *
   * indexed by: <br>
   * [i] hierarchy level <br>
   * [h] frame index within each hierarchy level <br>
   */
  private FrameDesc[][] _frameData;

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
      _frameData = new FrameDesc[_levels][];
      String[] tempBuffer;
      int num;
      int start;
      int end;
      int size;
      for (i=0; i<_levels; i++){
      	_frameData[i] = new FrameDesc[_frameNum[i]];
		for (j=0; j<_frameNum[i]; j++){
		    tempBuffer = in.readLine().trim().split("\\s");
		    if (DEBUG > 0)
	    		out.print(" level: [" + i + "]: " +
		      			" frame: [" + j + "]: ");

		    num = Integer.parseInt(tempBuffer[0]);
		    start = Integer.parseInt(tempBuffer[1]);
		    end = Integer.parseInt(tempBuffer[2]);
		    size = Integer.parseInt(tempBuffer[3]);
		    _frameData[i][j] = new FrameDesc(num, start, end, i, size);
	    	if (DEBUG > 0)
	      		out.print(_frameData[i][j] + "\n");
	  	}
	  	if (DEBUG > 0)
	    	out.println("");
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
	  out.print(_frameData[i][j] + " ");
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
		FrameDesc fd = _frameData[i][j];      	
		tempHash.put(new Integer(fd.getNum()), fd);
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
   * @return the data of the frame index file in a fashion suitable for 
   * sequential access.<br>
   * [i] hierarchy level <br>
   * [j] frame sequence <br>
   */
  public FrameDesc[][] frameData(){
    return _frameData;
  }

  /**
   * @return the data of the frame index file in a fashion suitable for 
   * random access.<br>
   * [i] hierarchy level, which returns a Hashtable <br>
   * [k] frame number, which returns an FrameDesc <br>
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

  /**
   * get the specified FrameDesc
   *
   * @param level: hierarchy level
   * @param frameNumber: the actual frame number
   * @return frame descriptor specified by the level and frame number
   */
  public FrameDesc getFrame(int level, int frameNumber){
    Hashtable ht = (Hashtable)_frameTimes.get(level);
  	return (FrameDesc)ht.get(new Integer(frameNumber));
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
