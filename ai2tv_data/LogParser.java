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
 * @version	$Revision$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */

public class LogParser {
  public static final double SCORE_WEIGHT = 0.2;

  private double goodnessScore;
  private int samples;

  private int DEBUG = 0;

  LogParser(String dirname){
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
    System.out.println("noWF vs WF");
    for (int i=0; i<nowfFiles.length; i++, numSamples++){
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
        System.out.print(wfFiles[i].getName().substring(15, (wfFiles[i].getName().length() - 7)) + ": " + nowfScore + " vs " + wfScore);
        if (nowfScore > wfScore){
          System.out.println(" : -" + Math.abs(diff));
        } else if (nowfScore < wfScore){
          System.out.println(" : +" + Math.abs(diff));
        } else {
          System.out.println(" : 0");
        }
      }
      totalRelativeScore += diff;
    }

    double tscore = calculateTScore(numSamples, sumnoWF, sumSquarednoWF,
                    numSamples, sumWF, sumSquarednoWF);

    System.out.println("t-score: " + tscore);
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

    int theoreticalLevel = 2;
    double score;
    // System.out.println(nowfFiles[i] + "\n  " + wfFiles[i] + "\n");
    theoreticalLevel = extractTheoreticalLevel(file);
    System.out.println("Theoretical level is: " + theoreticalLevel);
    score = parseFile(file, theoreticalLevel);
    System.out.println("score for this trial: " + score);
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
            sampleScore = -1;
          }  else {
            if (currentLevel == -1) System.out.println("shit out of luck, currentLevel = -1");
            if (missedFrame && currentFrame == ((int) input.nval)){
              sampleScore = -1;
            } else if (!missedFrame && currentFrame == ((int) input.nval) ||
                currentFrame != ((int) input.nval)) {
              sampleScore = (theoreticalLevel - currentLevel) * SCORE_WEIGHT;
              missedFrame = false;
              currentFrame = (int) input.nval;
            }
          }
          if (DEBUG == 1)
            System.out.println("found sample score for trial ["+samples+"] of: " + sampleScore);
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
      System.out.println("found total score of: " + goodnessScore + " out of " + samples + " samples = " + (goodnessScore/samples));
    return goodnessScore/samples;
  }

  int extractTheoreticalLevel(File f){
    String[] name = f.getName().split("\\s");
    int level = Integer.parseInt(name[3]);
    if (DEBUG == 1)
      System.out.println("found theoretical level to be: " + level);
    return level;
  }
  double calculateTScore(int n1, double sum1, double sumSquared1,
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

      System.out.println("Average sample1 N(" +n1+") " + avg1 + " +/- " + stdev1);
      System.out.println("Average sample2 N(" +n2+") " + avg2 + " +/- " + stdev2);

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
