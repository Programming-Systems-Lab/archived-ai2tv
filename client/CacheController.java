package psl.ai2tv.client;

import java.util.*;
import java.io.*;
import java.net.*;
import siena.*;
import siena.comm.*;


import psl.ai2tv.gauge.FrameDesc;
import psl.ai2tv.gauge.FrameIndexParser;

public class CacheController {

  private FrameIndexParser framesInfo;
  private int progress[];
  private String frameFileName;
  private File frameFile = null;
  private int currLevel;
  FrameDesc currFrame;
  private int numLevels;
  private long downloadInterval;
  String cacheDir = "cache";
  String baseURL = "http://www1.cs.columbia.edu/~suhit/ai2tv/1/";
	
  Client _client;

  public CacheController(String name, double rate, Client c) {
    _client = c;
    frameFileName = name;
    currLevel = 0;
    currFrame = null;
    downloadInterval = (long)(1000/rate);
    framesInfo = new FrameIndexParser(frameFileName);
    progress = new int[framesInfo.levels()];
    for (int i = 0; i < progress.length; i++)
      progress[i] = 0;
  }
	
  public int getLevel() { return currLevel; }
  public void setLevel(int i) { currLevel = i; }

  public FrameDesc getNextFrame() {
    return nextFrame();
  }
	
  public void hierarchyDown(long now) {
    if (currLevel  < numLevels -1 ) { 
      currLevel++;
      System.out.print ("Down to level " + currLevel + " : ");
      progress[currLevel] = nextFrameInLevel(currLevel, now);
    }
  }
	
  public void hierarchyUp(long now) {
    if (currLevel > 0)	{
      currLevel --;
      System.out.print ("Up to level " + currLevel + " : ");
      progress[currLevel] = nextFrameInLevel(currLevel, now);
    }
  }
	
  private FrameDesc nextFrame() {
    System.out.println("getting the next frame");    
    FrameDesc[] curr = framesInfo.frameData()[currLevel];
    int index = progress[currLevel];
    if (index < curr.length) {
      if (! curr[index].isDownloaded()){
	// try {
	// Thread.currentThread().sleep(downloadInterval);
	System.out.println("CacheController downloading file: " + baseURL + curr[index].getNum() + ".txt");
	downloadFile(baseURL + curr[index].getNum() + ".jpg");
	// } catch (InterruptedException e) {}	
	curr[index].setDownloaded(true);
      }
      progress[currLevel] = index + 1;
      currFrame = curr[index];		
      return currFrame;
    }
    else 
      return null;
  }
	
  private int nextFrameInLevel (int level, long now) {
    FrameDesc[] curr = framesInfo.frameData()[level];
    //int i = progress[level];
    int i = 0;
    while (curr[i].getEnd() <= now) {
      // System.out.print (curr[i].getStart() + " - ");
      i++;
			
    }
    //System.out.print("\n");
    return i;
  }

  /**
   * download a file from the given URL into the cache dir specified
   * in cacheDir.
   *
   * @param fileURL: URL of the file to get.
   */
  void downloadFile(String fileURL) {
    File f = new File(cacheDir);
    if (!f.exists()){
      f.mkdir();
    } else if (!f.isDirectory()) {
      System.err.println("Error: " + cacheDir + " had existed, but is not a directory");
      return;
    }

    String[] tokens = fileURL.split("/");
    String saveFile = cacheDir + "/" + tokens[tokens.length - 1];

    URL url = null;
    try {
      url = new URL(fileURL);
    } catch (MalformedURLException e){
      System.err.println("error in downloader: " + e);
    }

    if (url == null) {
      System.out.println("bad URL");
      return;
    }
      

    try {
    // open the connection
    URLConnection myConnection;
    myConnection=url.openConnection();
    System.out.println("donwloading : " + fileURL);
		
    // check that the file holds stuff
    if (myConnection.getContentLength()==0) {
      System.out.println("Error Zero content.");
      return;
    }

    int i = myConnection.getContentLength();
    if (i==-1) {
      System.out.println("Empty or invalid content.");
      return;
    }
    // System.out.println("Length : " + i + " bytes");

    BufferedInputStream input = new BufferedInputStream(myConnection.getInputStream());
    int p=0;
    File newFile = new File (saveFile);
    newFile.createNewFile();
    BufferedOutputStream downloadFile = new BufferedOutputStream(new FileOutputStream(newFile));
    int c;
    while (((c=input.read())!=-1) && (--i > 0))
      downloadFile.write(c);

    input.close();
    downloadFile.close();

    } catch (IOException e){
      System.out.println("IOException in CacheController.downloadFile(): " + e);
    }
    return;
  }
}
