/*
* @(#)LogParser.java
*
* Copyright (c) 2003: The Trustees of Columbia University in the City of New York.  All Rights Reserved
*
* Copyright (c) 2003: @author Dan Phung
* Last modified by: Dan Phung (dp2041@cs.columbia.edu)
*
* CVS version control block - do not edit manually
*  $RCSfile$
*  $Revision$
*  $Date$
*  $Source$
*/
// package psl.ai2tv.ai2tv_data;

import java.io.*;

/**
 * @version     $Revision$
 * @author      Dan Phung (dp2041@cs.columbia.edu)
 */

/**
 * LogParser = Stats calculator for AI2TV project.  Ya,
 * so its somewhat of a misnomer...
 *
 * Warning: this script has many magic numbers, constants, etc.  It
 * also assumes filenames of a certain form, and a certain directory
 * structure.  It also automatically overwrites a file:
 * goodness_scores.txt without prompting you!!!
 *
 *
 * Overview: This script parses the log files taken from ai2tv
 * evaluation experiments and calculates, on the fly, goodness scores
 * that measure the relative video quality of the video session.  It
 * also calculates an average and stdev over the trials and calculates
 * a t-score of wf vs. non wf data.
 *
 * The script runs as follows:
 * - open file
 * - for each line, calculate goodness score for that data point
 *   and add to total score.  The score per line depends on whether
 *   the data point is a missed frame or normal score.  The scoring
 *   system is described in our acm-mm ai2tv paper, so go read that
 *   if you're more interested (or just ask me).
 * - at the end of a file, average it all out, and gimme the results.
 *
 * For the paper and other reasons, we also split up the scoring so
 * that missed frames and the normal scoring scheme are kept separate.
 * To calculate that here, you have to edit three parts in the code,
 * labeled 111, 222, and 333.  Within these code parts, only one
 * region should be uncommented, eg. A), B), or C).
 * A) combined scoring + missed frames score
 * B) goodness score (without the missed frames)
 * C) missed frames count (all the other numbers that are
 * spit out don't mean squat).
 *
 *
 * oh, and ignore the weighted stuff, that's old stuff I want to
 * leave in here in case we want to switch scoring schemes.
 */
public class LogParser {
  // get these numbers from the frame_index.txt file.
  public final int NUM_FRAMES_LEVEL_0 = 165;
  public final int NUM_FRAMES_LEVEL_1 = 71;
  public final int NUM_FRAMES_LEVEL_2 = 39;
  public final int NUM_FRAMES_LEVEL_3 = 21;
  public final int NUM_FRAMES_LEVEL_4 = 13;
  public final int[] numFramesPerLevel = {NUM_FRAMES_LEVEL_0,
                                          NUM_FRAMES_LEVEL_1,
                                          NUM_FRAMES_LEVEL_2,
                                          NUM_FRAMES_LEVEL_3,
                                          NUM_FRAMES_LEVEL_4};

  // arbitrary score weight, no longer used (dp2041)
  public static final double SCORE_WEIGHT = 0.2;

  private double goodnessScore;
  private int samples;

  private int DEBUG = 2;

  private PrintStream output;

  LogParser(String dirname){
    String outputFilename = "goodness_scores.txt";
    try {
      File outputFile = new File(outputFilename);
      /*
      if (outputFile.exists()){
        System.out.println("Please move or rename the existing output file: " + outputFilename);
        System.exit(0);
      }
      */
      boolean append = true;
      FileOutputStream outputStream = new FileOutputStream(outputFile, append);
      output = new PrintStream(outputStream, true);
      output.println("");
    } catch (FileNotFoundException e) {
      System.err.println("Caught Exception " + e);
      e.printStackTrace();
    }

    if (!dirname.endsWith("/")) dirname += "/";

    File nowfDir = new File(dirname + "noWF");
    File wfDir = new File(dirname + "WF");
    if (!nowfDir.isDirectory() || !wfDir.isDirectory()){
      System.err.println("dirname must be valid dir: " + dirname);
      System.err.println("and there must be a WF and noWF subdir");
      System.exit(0);
    }

    File[] nowfFiles = nowfDir.listFiles();
    File[] wfFiles = wfDir.listFiles();
    if (nowfFiles!=null && wfFiles!=null &&
        nowfFiles.length != wfFiles.length){
      System.err.println("the number of files are not the same");
      System.exit(0);
    }

    double sumWF = 0;
    double sumnoWF = 0;
    double sumSquaredWF = 0;
    double sumSquarednoWF = 0;
    int numSamples = 0;
    int theoreticalLevel = 2;
    double wfScore, nowfScore;
    double totalRelativeScore = 0;
    double diff;
    output.println("noWF vs WF");
    for (int i=0; i<nowfFiles.length; i++, numSamples++){
      // to test out which files it's running
      // System.out.println(nowfFiles[i] + "\n  " + wfFiles[i] + "\n");

      theoreticalLevel = extractTheoreticalLevel(nowfFiles[i]);
      nowfScore = parseFile(nowfFiles[i], theoreticalLevel);
      wfScore = parseFile(wfFiles[i], theoreticalLevel);
      sumWF += wfScore;
      sumnoWF += nowfScore;
      sumSquaredWF += wfScore*wfScore;
      sumSquarednoWF += nowfScore*nowfScore;
      diff = wfScore - nowfScore;
      if (DEBUG == 2){
        String trialName = wfFiles[i].getName().substring(15, (wfFiles[i].getName().length() - 7));
        int bandwidthRating = 1;
        if (trialName.indexOf("High") != -1)
          bandwidthRating = 4;
        else if (trialName.indexOf("HMid") != -1)
          bandwidthRating = 3;
        else if (trialName.indexOf("Mid") != -1)
          bandwidthRating = 2;
        else if (trialName.indexOf("VLow") != -1)
          bandwidthRating = 0;

        // output.print(wfFiles[i].getName().substring(15, (wfFiles[i].getName().length() - 7)) + ": " + nowfScore + " vs " + wfScore);
        output.println(trialName + "\t" + nowfScore + "\t" + wfScore);
        /*
        if (nowfScore > wfScore){
          output.println(" : -" + Math.abs(diff));
        } else if (nowfScore < wfScore){
          output.println(" : +" + Math.abs(diff));
        } else {
          output.println(" : 0");
        }
        */
      }
      totalRelativeScore += diff;
    }

    double tscore = calculateTScore(numSamples, sumnoWF, sumSquarednoWF,
        numSamples, sumWF, sumSquarednoWF);

    output.println("t-score: " + tscore);
    // System.out.println("total relative score for all clients: " + totalRelativeScore);
    //System.out.println("total average relative score for all clients: " + totalRelativeScore/numSamples);
    // System.out.println("total average relative score for all clients: " + (avgWF - avgnoWF));
  }

  LogParser (String dirname, String filename){
    DEBUG = 1;
    if (!dirname.endsWith("/")) dirname += "/";

    File dir = new File(dirname);
    if (!dir.isDirectory()){
      System.err.println("dirname must be valid dir: " + dirname);
      System.exit(0);
    }

    File file = null;

    try {
      file = new File(dirname + filename);
    } catch (Exception e) {
      System.err.println(dirname + filename + " doesn't exist.");
      System.exit(0);
    }

    double score;
    // System.out.println(nowfFiles[i] + "\n  " + wfFiles[i] + "\n");
    int theoreticalLevel = extractTheoreticalLevel(file);
    output.println("Theoretical level is: " + theoreticalLevel);
    score = parseFile(file, theoreticalLevel);
    output.println("score for this trial: " + score);
  }

  double parseFile(File filename, int theoreticalLevel){
    goodnessScore= 0;
    samples = 0;

    Reader r = null;

    try {
      r = new FileReader(filename);
    } catch (FileNotFoundException e){
      e.printStackTrace();
    }
    StreamTokenizer input = new StreamTokenizer(r);
    input.eolIsSignificant(true);
    input.ordinaryChar(':');
    int currentFrame= -1, currentLevel = -1;
    double sampleScore = 0;
    boolean missedFrame = false;
    int i=0;
    try {
      // skip the first line
      while (input.nextToken() != StreamTokenizer.TT_EOF && input.ttype !=  StreamTokenizer.TT_EOL){
        ;
      }

      while (input.nextToken() != StreamTokenizer.TT_EOF){
        // System.out.println("token: [" + i + "] number: " + input.nval + " string: " + input.sval  + " = " + input);
        if (i == 4) {
          currentLevel = (int) input.nval;

        } else if (i == 7) {
          if (input.sval != null && input.sval.equals("missed")){
            missedFrame = true;
            sampleScore = scoreMissedFrame(theoreticalLevel, currentLevel);
          }  else {
            if (currentLevel == -1) System.out.println("shit out of luck, currentLevel = -1");
            if (missedFrame && currentFrame == ((int) input.nval)){
              sampleScore = scoreMissedFrame(theoreticalLevel, currentLevel);
            } else if (!missedFrame && currentFrame == ((int) input.nval) ||
                currentFrame != ((int) input.nval)) {

              sampleScore = scoreTrial(theoreticalLevel, currentLevel);
              missedFrame = false;
              currentFrame = (int) input.nval;
            }
          }
          if (DEBUG == 1)
            output.println("found sample score for trial ["+samples+"] of: " + sampleScore);
          goodnessScore += sampleScore;
        }

        i++;
        if (input.ttype ==  StreamTokenizer.TT_EOL){
          i = 0;
          samples++;
        }
      }
    } catch (IOException e){
      e.printStackTrace();
    }
    if (DEBUG == 1)
      output.println("found total score of: " + goodnessScore + " out of " + samples + " samples = " + (goodnessScore/samples));

    // dp2041
    // ya, this is bad, but it's the quickest hack.  to get only the
    // WF-vs-noWF score (without missed frames) uncomment A) and
    // comment B).  Also, do the same down below in the
    // scoreMissedFrame method.

    // A)
    return goodnessScore/samples;

    // B)
    // return goodnessScore/samples;

    // C)
    // return goodnessScore; // no normalization (for missed frame count)
  }

  private double scoreTrial(int theoreticalLevel, int currentLevel){
    double sampleScore;
    // previous setting:
    // sampleScore = (theoreticalLevel - currentLevel) * SCORE_WEIGHT;

    if (numFramesPerLevel[currentLevel] >= numFramesPerLevel[theoreticalLevel]){
      sampleScore = (double) numFramesPerLevel[currentLevel] / numFramesPerLevel[theoreticalLevel];
    } else {
      sampleScore = (double) -1 * numFramesPerLevel[theoreticalLevel] / numFramesPerLevel[currentLevel];
    }

    // A)
    return sampleScore;

    // B)
    // return sampleScore;

    // C)
    // return 0; // if we only want to count the missed frames
  }

  private double scoreMissedFrame(int theoreticalLevel, int currentLevel){
    double missedFrameScore = 0;
    // old old old, yesteryear scoring scheme (arbitrary)
    // missedFrameScore = -1;

    // old old, yesteryear scoring scheme (arbitrary)
    // we are going to arbitarily pick the missed frame as twice the proportion
    // of the most frames/least frames.
    // missedFrameScore = (double) -2 * numFramesPerLevel[0] / numFramesPerLevel[4];

    // latest scoring scheme:
    // The missed frame score is computed by extrapolating the end point of the 0 quality level
    // worst score.  This equation was computed using a polynomial regression: -0.4795*(X*X)-0.2035*X+0.401
    // The score below uses X=6. (initial point starts at 1)
    // A)
    // missedFrameScore = -18;

    // B)
    missedFrameScore = 0; // goodness score does not count in the missed frames

    // C)
    // missedFrameScore = 1; // goodness score  simply counts the number of missed frames
    // System.out.println("someone missed a frame");

    return missedFrameScore;
  }

  private int extractTheoreticalLevel(File f){
    String[] name = f.getName().split("\\s");
    int level = Integer.parseInt(name[3]);
    if (DEBUG == 1)
      output.println("found theoretical level to be: " + level);
    return level;
  }

  private double calculateTScore(int n1, double sum1, double sumSquared1,
                                 int n2, double sum2, double sumSquared2){

    double avg1 = sum1/n1;
    double avg2 = sum2/n2;
    double avgSquared1 = sumSquared1/n1;
    double avgSquared2 = sumSquared2/n2;

    double variance1 = (avgSquared1 - (avg1 *avg1)/n1) / (n1- 1);
    double variance2 = (avgSquared2 - (avg2 *avg2)/n2) / (n2 - 1);

    double stdev1 = Math.sqrt(variance1);
    double stdev2 = Math.sqrt(variance2);

    double expectedDifference = 0;  // expected difference greater than this number

    double temp1 = ((n1 - 1)*variance1 + (n2-1)*variance2) / (n1+n2 - 2);
    double temp2 = ((double)1/n1 + (double)1/n2);
    double tscore = (avg1 - avg2 - expectedDifference) /
        (Math.sqrt(temp1) * Math.sqrt(temp2));

    output.println("Average sample1 N(" +n1+") " + avg1 + " +/- " + stdev1);
    output.println("Average sample2 N(" +n2+") " + avg2 + " +/- " + stdev2);

    return tscore;
  }

  public static void main(String[] args){
    if (args.length < 1){
      System.err.println("usage: java LogParser <log file dir>");
      System.exit(0);
    }
    String dirname = args[0];

    if (args.length == 2){
      String filename = args[1];
      LogParser blah = new LogParser(dirname, filename);
    } else {
      LogParser blah = new LogParser(dirname);
    }
  }
}
