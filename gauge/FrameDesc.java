package psl.ai2tv.gauge;

public class FrameDesc {

	private int level;
	private int num;
	private int start;
	private int end;
	private boolean downloaded = false;
	
	/** time at which the Frame is downloaded */
	private long downloadedTime;
	
	public FrameDesc() {
		level = -1;
		num = -1;
		start = -1;
		end = -1;
		downloadedTime = -1;	
	}
	
	public FrameDesc(int n, int s, int e, int level) {
		num = n;
		start = s;
		end = e;
		this.level = level;
		//System.out.println ("Frame is " + num + ": < " + start + " : " + end +  ">");
	}
	
	public void setDownloaded (boolean flag) { 
		downloaded = flag; 
		if (flag == true)
			downloadedTime = System.currentTimeMillis();
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
	
	double interval() { return end - start; }
	
	public long getDownloadedTime() {return downloadedTime; }
	public void setDownloadedTime(long t) { downloadedTime = t; }
	
	public String toString() {
		String s = " : level =  " + level + " frame # = " + num + " < " + start + " - " + end + " >";
		return s;
	}
}