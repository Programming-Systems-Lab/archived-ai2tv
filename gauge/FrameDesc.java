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
	
	/** 
	 * beginning of the frame interval represented by this frame at its hierarchy level 
	 * (expresssed as a frame number)	
	 */ 
	private int start;

	/** 
	 * end of the frame interval represented by this frame at its hierarchy level  
	 * (expresssed as a frame number)	
	 */
	private int end;
	
	/** size in bytes of the frame */
	private int size;

	/** has it been downloaded in the cache already? */
	private boolean downloaded = false;
	/** video clock time when the frame was downloaded */
	private long timeDownloaded;

	/** 
	 * offset between when the frame should have been shown and
	 * when when it was actually shown (so 0 would be right on
	 * time)
	 */
	private int timeOffset;
  
        /** video clock time when the frame was shown  */
	private long timeShown;

	/** Vector containing the FrameDesc of all the equivalent frames to this frame */
	private Vector equivalents;

	public FrameDesc() {
		level = -1;
		num = -1;
		start = -1;
		end = -1;
		size = -1;
		timeDownloaded = -1;
		timeShown = -1;
		timeOffset = 0;
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
		  timeDownloaded = System.currentTimeMillis(); // this clock time should be synchronized through NTP
		// Warning, on the Client side, I manually set the
		// downloaded time to the video clock time
	}
	
	public boolean isDownloaded() { return downloaded; }
	public long getTimeDownloaded() { return timeDownloaded; }
	public void setTimeDownloaded(long t) { timeDownloaded = t; }
	public int getNum() { return num; }
	public void setNum(int n) { num = n; }
	public int getStart() { return start; }
	public void setStart(int s) { start = s; }
	public int getEnd() { return end; }
	public void setEnd (int e) { end = e; }
	public int getLevel() { return level; }
	public void setLevel(int l) { level = l; }
	public int getSize() { return size; }
	public void setSize(int s) { size = s; }
	public long getTimeShown() {return timeShown; }
	public void setTimeShown(long t) { timeShown = t; }
	public int getTimeOffset() {return timeOffset; }
	public void setTimeOffset(int t) { timeOffset = t; }
	public void setEquivalents(Vector v) { equivalents = v; }
	public Iterator getEquivalents() { return equivalents.iterator(); }
	
	public String toString() {
	  String s = " :level =  " + level + " frame # = " + num + " < " + start + " - " + end + " >\n";
	  s += " :timeShown: " + timeShown + " timeOffset: " + timeOffset + " timeDownloaded: " + timeDownloaded;
	  return s;
	}
}
