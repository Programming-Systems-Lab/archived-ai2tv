package psl.ai2tv.gauge;

import java.util.*;

/** Class descrbing a video frame. 
	Holds the information presented in the format of the frame index file
	plus runtime state information 
*/
public class FrameDesc {

	/** Hierarchy level of the frame */
	private int level;
	/** frame number */
	private int num;
	
	/** beginning of the frame interval represented by this frame at its hierarchy level 
		(expresssed as a frame number)	
	*/ 
	private int start;

	/** end of the frame interval represented by this frame at its hierarchy level  
		(expresssed as a frame number)	
	*/
	private int end;
	
	/** has it been downloaded in the cache already? */
	private boolean downloaded = false;
	
	/** time at which the Frame was shown */
	private long frameShownTime;
	
	/** Vector containing the FrameDesc of all the equivalent frames to this frame */
	private Vector equivalents;
	
	/** size in bytes of the frame */
	private int size;

	public FrameDesc() {
		level = -1;
		num = -1;
		start = -1;
		end = -1;
		size = -1;
		frameShownTime = -1;
		equivalents = null;
	}
	
	public FrameDesc(int n, int s, int e, int level, int sz) {
		num = n;
		start = s;
		end = e;
		this.level = level;
		size = sz;
		//System.out.println ("Frame is " + num + ": < " + start + " : " + end +  ">");
	}
	
	public void setDownloaded (boolean flag) { 
		downloaded = flag; 
		if (flag == true)
		  ; // downloadedTime = System.currentTimeMillis();
	}
	
	public boolean isDownloaded() { return downloaded; }
	public int getNum() { return num; }
	public void setNum(int n) { num = n; }
	public int getStart() { return start; }
	public void setStart(int s) { start = s; }
	public int getEnd() { return end; }
	public void setEnd (int e) { end = e; }
	public int getLevel() { return level; }
	public void setLevel(int l) { level = l; }
	public int getSize() { return level; }
	public void setSize(int s) { size = s; }
	
	public long getFrameShownTime() {return frameShownTime; }
	public void setFrameShownTime(long t) { frameShownTime = t; }
	
	public void setEquivalents(Vector v) { equivalents = v; }
	public Iterator getEquivalents() { return equivalents.iterator(); }
	
	public String toString() {
		String s = " : level =  " + level + " frame # = " + num + " < " + start + " - " + end + " >";
		return s;
	}
}
