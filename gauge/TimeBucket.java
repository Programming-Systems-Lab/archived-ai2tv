package psl.ai2tv.gauge;

import java.util.*;

/** represents a periodic report constructed by the gauge,
	a snapshot of the states of all the clients in an AI2TV group at a given time
*/
public class TimeBucket {

	/** info on the group state. 
		The <code>Map</code> contains the latest FrameDescriptor for each client before this snapshot
		was taken.
		It is synchronized and allows for null elements and values.
	*/
	private Map groupState;
	/** time of the snapshot */
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
}


