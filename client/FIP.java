/*
 * @(#)FIP.java
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

import java.io.*;

/** 
 * reads in the frame index file and outputs new frame index file with 
 * size of the each file as 4th element in each row.
 *
 * @param fip: old frame index file
 * @param newFip: new frame index file
 * @param dirname: directory holding all the pre-downloaded jpgs.
 */
class FIP{
  FIP(String fip, String newFip, String dirname){
    String buff;

    try {
      BufferedWriter out = new BufferedWriter(new FileWriter(newFip));
      BufferedReader in = new BufferedReader(new FileReader(fip));
      // get rid of the first few lines
      int levels = Integer.parseInt(in.readLine().trim());
      buff = "" + levels + "\n";
      System.out.println("writing: " + buff);
      out.write(buff, 0, buff.length());
      int[] frames = new int[levels];

      for (int i=0; i<levels; i++){
	frames[i] = Integer.parseInt(in.readLine().trim());
	buff = "" + frames[i] + "\n";
	System.out.println("writing: " + buff);
	out.write(buff, 0, buff.length());
      }
      
      for (int i=0; i<levels; i++){
	for (int j=0; j<frames[i]; j++){
	  String nextline = in.readLine();
	  String[] tokens = nextline.split("\t");
	  File file = new File(dirname + tokens[0].trim() + ".jpg");
	  long fileSize = file.length();
	  buff = nextline + "\t" + fileSize + "\n";
	  System.out.println("writing: " + buff);
	  out.write(buff, 0, buff.length());
	}
      }
      out.flush();
      out.close();
    } catch (IOException e){
      System.out.println("ERROR converting file : " + e);
      e.printStackTrace();
    }
  }

  public static void main(String[] args){
    String fip = "frame_index.txt";
    String newFip = "new_frame_index.txt";
    String dirname = "cache/";

    if (args.length != 3){
      System.out.println(" usage: java FIP fip newFip dirname");
      System.out.println(" @param fip: old frame index file");
      System.out.println(" @param newFip: new frame index file");
      System.out.println(" @param dirname: directory holding all the pre-downloaded jpgs.");
      System.exit(0);
    } else {
      fip = args[0];
      newFip = args[1];
      dirname = args[2];
    }
    
    new FIP(fip, newFip, dirname);
  }
}
