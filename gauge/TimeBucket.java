package psl.ai2tv.gauge;

import java.util.*;

public class TimeBucket {

	// want to have nulls
	private Map groupState;
	private long time;
	
	public TimeBucket () {
		groupState = Collections.synchronizedMap(new HashMap());
	}
	
	public void add (String clientKey) {
		groupState.put(clientKey, null);
	}
	
	public void update (Object name, FrameDesc fd) {
		groupState.put(name, fd);
	}
	
	public FrameDesc retrieve (Object clientKey) {
		FrameDesc fd = (FrameDesc) groupState.get(clientKey);
		return fd;
	}
		
	public void setTime(long t) { time = t;}
	public long getTime() { return time; }
	
	public void clearValues() {
		Set s = groupState.keySet();
		synchronized (groupState) {
			Iterator iter = s.iterator();
			while (iter.hasNext())	{
				this.update(iter.next(), (FrameDesc) null);	
			}	
		}
	}
	
	public String toString() {
		String s = new String();
		String id;
		s = "Bucket at time " + time + " : \n";
		Set theSet = groupState.keySet();
		synchronized (groupState) {
			Iterator iter = theSet.iterator();
			while (iter.hasNext())	{
				FrameDesc fd = (FrameDesc) groupState.get(id = (String)iter.next());
				s += "Client " + id + " : ";
				if (fd != null)
					s += fd.toString();	
				else 
					s += "null";
					
				s += "\n";
			}
		}
		return s;
	}

/*	
	class ClientState {
		String id;
		long t;
		int level;
		long bandwidth;
		
		public String toString() {
			String s = "Client " + id + " : level =  " + level + " time = " + t + " bandwidth = " + bandwidth;
			return s;
		}
	}
*/
}


