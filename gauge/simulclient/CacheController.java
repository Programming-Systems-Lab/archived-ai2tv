package psl.ai2tv.gauge.simulclient;

import java.util.*;
import java.io.*;
import siena.*;
import siena.comm.*;

import psl.ai2tv.gauge.FrameDesc;
import psl.ai2tv.gauge.FrameIndexParser;
import psl.ai2tv.client.*;

public class CacheController 
	extends psl.ai2tv.client.CacheController {

/*
	private FrameIndexParser framesInfo;
	private int progress[];
	private String frameFileName;
	private File frameFile = null;
	private int currLevel;
	FrameDesc currFrame;
	private int numLevels;
*/
	private long downloadInterval;

	
	public CacheController(String name, double rate) {
		super(name, rate);
		System.out.println ("in constructor of SimulClient.CacheController");
/*		frameFileName = name;
		currLevel = 0;
		currFrame = null;
		downloadInterval = (long)(1000/rate);
		framesInfo = new FrameIndexParser(frameFileName);
		progress = new int[framesInfo.levels()];
		for (int i = 0; i < progress.length; i++)
			progress[i] = 0;
*/
	}
	
	public int getLevel() { return currLevel; }
	public void setLevel(int i) { currLevel = i; }
	public FrameDesc getCurrFrame() { return currFrame; }

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
	
	protected FrameDesc nextFrame() {
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
}