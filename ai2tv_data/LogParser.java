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

  private int missedFrames;
  private double goodnessScore;
  private int samples;

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

    double totalWF = 0;
    double totalnoWF = 0;
    int numSamples = 0;
    int theoreticalLevel = 2;
    double wfScore, nowfScore;
    double totalRelativeScore = 0;
    double diff;
    for (int i=0; i<nowfFiles.length; i++, numSamples++){
      // System.out.println(nowfFiles[i] + "\n  " + wfFiles[i] + "\n");
      theoreticalLevel = extractTheoreticalLevel(nowfFiles[i]);
      nowfScore = parseFile(nowfFiles[i], theoreticalLevel);
      wfScore = parseFile(wfFiles[i], theoreticalLevel);
      System.out.print("noWF vs wf: " + nowfScore + " vs " + wfScore);
      diff = wfScore - nowfScore;
      if (nowfScore > wfScore){
	System.out.println(" : -" + Math.abs(diff));
      } else if (nowfScore < wfScore){
	System.out.println(" : +" + Math.abs(diff));
      } else {
	System.out.println(" : 0");
      }

      totalRelativeScore += diff;
    }

    System.out.println("total relative score for all clients: " + totalRelativeScore);
    System.out.println("total average relative score for all clients: " + totalRelativeScore/numSamples);
  }

  double parseFile(File filename, int theoreticalLevel){
    missedFrames = 0;
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
            missedFrames++;
            missedFrame = true;
            sampleScore = -1;
          }  else {
            if (currentLevel == -1) System.out.println("shit out of luck, currentLevel = -1");
            if (missedFrame && currentFrame == ((int) input.nval)){
              sampleScore = -1;
              missedFrames++;
            } else if (!missedFrame && currentFrame == ((int) input.nval) ||
                currentFrame != ((int) input.nval)) {
              sampleScore = (theoreticalLevel - currentLevel) * SCORE_WEIGHT;
              missedFrame = false;
              currentFrame = (int) input.nval;
            }
            goodnessScore += sampleScore;

          }

         //  System.out.println("found sample score of: " + sampleScore);
        }

        i++;
        if (input.ttype ==  StreamTokenizer.TT_EOL){
          // System.out.println("- - - END OF LINE - - - ");
          i = 0;
          samples++;
        }
      }
    } catch (IOException e){
      e.printStackTrace();
    }

    // System.out.println("found total score of: " + goodnessScore + " out of " + samples + " samples = " + (goodnessScore/samples));
    return goodnessScore/samples;
  }

  int extractTheoreticalLevel(File f){
    String[] name = f.getName().split("\\s");
    int level = Integer.parseInt(name[3]);
    // System.out.println("found level to be: " + level);
    return level;
  }

  public static void main(String[] args){
    if (args.length < 1){
      System.err.println("usage: java LogParser <log file dir>");
      System.exit(0);
    }
    String dirname = args[0];
    LogParser blah = new LogParser(dirname);
  }
}
