/*
 * @(#)ClientDesc.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Peppo Valetto
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.ai2tv.gauge;

/**
 * base class for holding client state in the gauge
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
	
  /** 
   * penalties accumulated thus far (penalties are times which the
   * client wasn't able to view the correct frame at the needed time
   */
  protected int penalties;

  // These are needed to get the avg and stddev for the distance
  // measurements.  Measured are the propagation delay (distance) from
  // WF to client (and vice versa) in milliseconds.
  /** The number of samples before the value of each respective stat
   * is reset to its average */
  protected final int statWindow = 10;
  /** statistical N (sample size) */
  protected int nDistClient2WF;
  /** The sum of the distances*/
  protected long sumDistClient2WF;
  /** The sum of the squares of the distances, only needed for stddev*/
  protected long sumOfSquaredDistClient2WF;

  /** statistical N (sample size) */
  protected int nDistWF2Client;
  /** The sum of the distances*/
  protected long sumDistWF2Client;
  /** The sum of the squares of the distances, only needed for stddev*/
  protected long sumOfSquaredDistWF2Client;

  // I declare these here so that during runtime, we won't incur
  // continuous overhead for redeclaring these each time the
  // stats related methods are used.
  /** average */
  private double avg;
  /** average of squared values*/
  private double avgS;
  /** variance */
  private double variance;

  public ClientDesc (String name) {
    clientID = name;
    startTime = 0;
    bandwidth = 0;
    penalties = 0;
    fd = new FrameDesc();

    nDistClient2WF = 0;
    sumDistClient2WF = 0;
    sumOfSquaredDistClient2WF = 0;
    nDistWF2Client = 0;
    sumDistWF2Client = 0;
    sumOfSquaredDistWF2Client = 0;
    avg = 0;
    avgS = 0;
    variance = 0;
  }
		
  public void setFrame(int l, int m, int r, int level, int s, 
		       long timeShown, int timeOffset, long timeDownloaded) {
    fd.setStart (l);
    fd.setNum (m);
    fd.setEnd (r);
    fd.setLevel (level);
    fd.setSize (s);
    fd.setTimeShown(timeShown);
    fd.setTimeOffset(timeOffset);
    fd.setTimeDownloaded(timeDownloaded);
  }
	
  public FrameDesc getFrame() { return fd; }
  public long getStartTime() { return startTime; }
  public void setStartTime(long st) { startTime = st; } 
  public double getBandwidth() { return bandwidth; }
  public void setBandwidth(double d) { bandwidth = d; }
  public String getClientID() { return clientID; }

  /**
   * get the current penalties that this client has incurred
   */
  public int getPenalties(){
    return penalties;
  }

  /**
   * Reset the penalties of this client 
   */
  public void resetPenalties(){
    penalties = 0;
  }

  /**
   * Reset the penalties of this client 
   */
  public void addPenalty(){
    penalties++;
  }

  /**
   * Add another measurement to the stats.  I "reset" the sum after
   * statWindow values so that we will never overflow the long
   * containers.  I also weight the average a bit.
   *
   * @param dist: measured distance from client to WF
   */
  public void addDistClient2WF(long dist) { 
    if (nDistClient2WF < statWindow){
      nDistClient2WF++;
      sumDistClient2WF += dist;
      sumOfSquaredDistClient2WF += dist*dist;
    } else {
      sumDistClient2WF = sumDistClient2WF/nDistClient2WF * 5;
      sumOfSquaredDistClient2WF = sumOfSquaredDistClient2WF/nDistClient2WF * 5;
      nDistClient2WF = 5;
    }
  }

  /**
   * @return average distance from client to WF
   */
  public double getAvgDistClient2WF() { 
    if (nDistClient2WF > 0)
      return (double)sumDistClient2WF/nDistClient2WF;
    else 
      return 0;
  }

  /**
   * @return average standard deviation from client to WF
   */
  public double getStddevDistClient2WF() { 
    if (nDistClient2WF > 1){
      // I don't know why, but this calculation just returns the 
      // double value of sumOfSquaredDistWF2Client.  The calulation is more efficient 
      // than the one used below which is: 
      // the following is: (sumOfSquared*(N-1) - sum^2) / N-1
      // int N = nDistClient2WF - 1;
      // double variance = sumOfSquaredDistClient2WF - (1/N*N)*sumDistClient2WF*sumDistClient2WF;
      avg = sumDistClient2WF / nDistClient2WF;
      avgS = sumOfSquaredDistClient2WF / nDistClient2WF;
      variance = avgS - (avg*avg);
      
      return java.lang.Math.sqrt(variance);
    } else 
      return 0;
  }

  /**
   * Add another measurement to the stats
   *
   * @param dist: measured distance from WF to client
   */
  public void addDistWF2Client(long dist) { 
    if (nDistWF2Client < statWindow){
      nDistWF2Client++;
      sumDistWF2Client += dist;
      sumOfSquaredDistWF2Client += dist*dist;
    } else {
      System.out.println("resetting stats sums");
      sumDistWF2Client = sumDistWF2Client/nDistWF2Client * 5;
      sumOfSquaredDistWF2Client = sumOfSquaredDistWF2Client/nDistWF2Client * 5;
      nDistWF2Client = 5;
    }
  }  

  /**
   * @return average distance from WF to client
   */
  public double getAvgDistWF2Client() { 
    if (nDistWF2Client > 0)
      return (double)sumDistWF2Client/nDistWF2Client;
    else 
      return 0;
   }  

  /**
   * @return average standard deviation from WF to client 
   */
  public double getStddevDistWF2Client() { 
    if (nDistWF2Client > 1){
      // I don't know why, but this calculation just returns the 
      // double value of sumOfSquaredDistWF2Client.  The calulation is more efficient 
      // than the one used below which is: 
      // sumOfSquared - 1/(N-1)^2 * sum^2
      // int N = nDistWF2Client - 1;
      // double variance = sumOfSquaredDistWF2Client - 1/(N*N) * sumDistWF2Client*sumDistWF2Client;

      avg = sumDistWF2Client / nDistWF2Client;
      avgS = sumOfSquaredDistWF2Client / nDistWF2Client;
      variance = avgS - (avg*avg);
      return java.lang.Math.sqrt(variance);
    } else 
      return 0;
  }
}
