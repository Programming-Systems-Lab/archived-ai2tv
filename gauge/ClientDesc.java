package psl.ai2tv.gauge;

/**
	base class for holding client state in the gauge
*/
public class ClientDesc {

	/** client unique id */
	protected String clientID;

	/** time of client start - normalized at the first client's start  */
	protected long startTime;
	
	/** last frame being displayed by the client */
	protected FrameDesc fd;
	
	/** bandwidth as sampled last */
	protected double bandwidth;
	
	public ClientDesc (String name) {
		clientID = name;
		startTime = 0;
		bandwidth = 0;
		fd = new FrameDesc();	
	}
	

		
	public void setFrame(int l, int m, int r, long t, int level, int s) {
		fd.setStart (l);
		fd.setNum (m);
		fd.setEnd (r);
		fd.setDownloadedTime(t);
		fd.setLevel (level);
		fd.setSize (s);
	}
	
	public FrameDesc getFrame() { return fd; }
	public long getStartTime() { return startTime; }
	public void setStartTime(long st) { startTime = st; } 
	public double getBandwidth() { return bandwidth; }
	public void setBandwidth(double d) { bandwidth = d; }
	public String getClientID() { return clientID; }
	
}
