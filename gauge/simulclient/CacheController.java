package psl.ai2tv.gauge.simulclient;

import java.util.*;
import java.io.*;
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
	
	
	public CacheController(String name, double rate) {
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
		FrameDesc[] curr = framesInfo.frameData()[currLevel];
		int index = progress[currLevel];
		if (index < curr.length) {
			if (! curr[index].isDownloaded()){
				try {
					Thread.currentThread().sleep(downloadInterval);
				} catch (InterruptedException e) {}	
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
/*	
	private void processFile(String name) {
		int tokenType;
		StreamTokenizer tok = null;
		
		frameFile = new File(name);
		try {
			Reader r = new BufferedReader(new InputStreamReader(new FileInputStream(frameFile)));
			tok = new StreamTokenizer(r);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);	
		}

		tok.parseNumbers();
		try {
			tokenType = tok.nextToken();
			if (tokenType != StreamTokenizer.TT_NUMBER) {
				throw new IOException("Malformed frame file");
			}
			else {
				numLevels = (int) tok.nval;
				System.out.println ("Hierarchy has " + numLevels + " levels");
				levels = new FrameDesc[numLevels][];
				for (int i = 0; i < numLevels; i++) {
					tokenType = tok.nextToken();
					if (tokenType != StreamTokenizer.TT_NUMBER) {
						throw new IOException("Malformed frame file");
					}
					else {
						levels[i] = new FrameDesc[(int)tok.nval];
						System.out.println ("Level " + i + " has " + (int)tok.nval + " frames");
					}
				}
				for (int i = 0; i < numLevels; i++) {
					int framecount = levels[i].length;
					for (int j = 0; j <	framecount; j++) {
						tokenType = tok.nextToken();
						int frameID = (int)tok.nval;
						tokenType = tok.nextToken();
						double start = tok.nval;
						tokenType = tok.nextToken();
						double end = tok.nval;
						levels[i][j] = new FrameDesc(frameID, start, end, i);
					}
					System.out.println ("Instantiated " + framecount + " frames " + " at level " + i);
				}
			}			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
*/
}