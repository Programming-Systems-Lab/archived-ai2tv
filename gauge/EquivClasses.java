/*
 * @(#)EquivClasses.java
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
 * Compute and generate a data structue to hold the equivalent
 * classes across AI2TV frame heirarchies.
 *
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class EquivClasses {
  public static final int DEBUG = 0;

  public static PrintStream out = System.out;
  public static PrintStream err = System.err;

  /* stuff from FrameIndexParser */
  private FrameIndexParser _fip;

  /**
   * create the data structure of equivalence classes
   */
  EquivClasses(String filename){
    _fip = new FrameIndexParser(filename);
  }

  /**
   * compute the overlap percentage.  this is measured as: <br>
   * window of overlap / total window <br>
   *
   * @param firstBeg: the beginning of the first window
   * @param firstEnd: the end of the first window
   * @param secondBeg: the beginning of the second window
   * @param secondEnd: the end of the second window
   * @return the percentage of overlap of the two windows.  a negative
   * number means the second window started before the first.
   */
  private double _computeOverlap(int firstBeg, int firstEnd,
				 int secondBeg, int secondEnd){
    if (DEBUG > 0)
      System.out.println("comparing: " + 
			 firstBeg + "," +
			 firstEnd + " vs " +
			 secondBeg + "," +
			 secondEnd);

    /* Scenarios
     * 1) no overlap = 0
     * 0 10
     * 10 20
     * 2) complete overlap = 1
     * 0 10
     * 10 20
     *
     * 3) incomplete overlap = 5 / 15 = 33%
     * 0 10
     * 5 15
     *
     * 4) incomplete overlap = 2 / 10 = 20%
     * 0 10
     * 5 7
     *
     * 5) and 6) same as 3 and 4 but with the second window earlier than the
     * first, so the result is negative
     */

    // |-----|         or           |-----|
    //         |-----|     |-----|
    if ((firstBeg < secondBeg && firstEnd < secondBeg) ||
	(secondBeg < firstBeg && secondEnd < firstBeg))
      return 0;



    if (firstBeg <= secondBeg) {
      if (secondEnd > firstEnd){
	// |-----|
	//    |-----|
	return (double)(firstEnd - secondBeg) / (double)(secondEnd - firstBeg);

      } else {
	// |-------|
	//   |--|
	return (double)(secondEnd - secondBeg) / (double)(firstEnd - firstBeg);
      }
    } else {
      if (firstEnd > secondEnd){
	//    |-----|
	// |-----|
	return -(double)(secondEnd - firstBeg) / (double)(firstEnd - secondBeg);

      } else {
	//   |--|
	// |-------|
	return -(double)(firstEnd - firstBeg) / (double)(secondEnd - secondBeg);
      }
    }
  }

  /**
   * Compute the equivalences between the given frames.  Expects the
   * the input ArrayList to be consecutive pairs of (hierarchyLevel,
   * frameNumber).  Compares all others against the first pair.
   *
   * @param frames: the frames to compare.
   * @return an array of the equivalences
   */
  public double[] computeEquivalence(int[] frames){
    // check if the ArrayList has at least 2 pairs
    if (frames.length<2 || (frames.length%2 > 0 )) {
      err.println("Error: EquivClasses.computeEquivalence input incorrect");
      return null;
    }
    double[] results = new double[frames.length/2];
    int index = 0;
    int firstBeg, firstEnd, secondBeg, secondEnd;
    firstBeg = _fip.getFrameTime(frames[0], frames[1], 0);
    firstEnd = _fip.getFrameTime(frames[0], frames[1], 1);
    
    // the first iteration is redundant, but i left it for clarity
    for (int i=0; i<frames.length; i+=2){
      secondBeg = _fip.getFrameTime(frames[i], frames[i+1], 0);
      secondEnd = _fip.getFrameTime(frames[i], frames[i+1], 1);

      results[index++] = _computeOverlap(firstBeg, firstEnd, secondBeg, secondEnd);
    }
    return results;
  }

  public static void main(String[] args){
    if (args.length != 1){
      out.println("usage: java EquivClasses frame_index.txt");
      System.exit(0);
    }

    String filename = args[0];
    EquivClasses ec = new EquivClasses(filename);

    /* example use of the equivalence computation
     * 
     * Here we enter in some data into an array
     * the data is input as pairs, 
     * 1st of pair = hierarchy level
     * 2nd of pair = frame number
     *
     * The very first pair is the one the others will be compared
     * against.
     */

    int[] al = new int[10];
    al[0] = 0;
    al[1] = 428;

    al[2] = 1;
    al[3] = 428;

    al[4] = 2;
    al[5] = 432;

    al[6] = 3;
    al[7] = 358;

    al[8] = 4;
    al[9] = 358;

    long a = java.util.Calendar.getInstance().getTimeInMillis();

    /* The result is an array of the overlap compared to the first
     * pair.  Negative values mean that the second window starts
     * earlier than the first.
     */
    double[] results = ec.computeEquivalence(al);
    for (int i=0; i<results.length; i++)
      System.out.println("[" +i+ "]: " + results[i]);
    long b = java.util.Calendar.getInstance().getTimeInMillis();
    System.out.println("time test: " + (b - a));
  }
}
