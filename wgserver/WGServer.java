/*
 * @(#)WGServer.java
 *
 * Copyright (c) 2003: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 * 
 * Copyright (c) 2003: @author Dan Phung (dp2041@cs.columbia.edu)
 * Last Modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source$
 */

package psl.ai2tv.wgserver;

import java.util.*;

/**
 * WGServer that handles keeping track of active video sessions
 * (pids), workgroups (gids), and their uids...?
 *
 */
class WGServer extends Thread  {
  private boolean _isActive;
  private CommController _comm;
  private Random _idGenerator;

  /**
   * Main structure holding the gid to vsid mappings.  The Hashtable
   * will use the gid string as the key to an inner Hashtable whose
   * key/value pairs are the vsid values and the VSID.
   */
  private Hashtable _VSIDs;

  /**
   * create a WG server
   * 
   * @param sienaServer: string specifying the Siena server
   */
  WGServer(String sienaServer){
    _comm = new CommController(this, sienaServer);
    _VSIDs = new Hashtable();
    _idGenerator = new Random();
    System.out.println("WG Server ready");
  }

  /**
   * get the active video sessions
   * 
   * @param gid: the gid to query
   * @return hashtable of video sessions associated with the gid
   */
  Hashtable getActiveVSIDs(String gid){
    // first we get all the vsid's in the gid
    Hashtable vsidTable = (Hashtable) _VSIDs.get(gid);
    return vsidTable;
  }

  /**
   * join/create a new video session
   *
   * @param name: name of the video
   * @param uid: user id
   * @param gid: group id
   * @param date: date that the video session plans to start
   * @return VSID of the new video session
   */
  String joinNewVSID(String name, String uid, String gid, String date){
    // get all the vsid's in the gid
    Hashtable vsidTable = (Hashtable) _VSIDs.get(gid);

    String id;
    // if there were no other gids, then this is the first
    if (vsidTable == null){
      vsidTable = new Hashtable();
      id = "" +  _idGenerator.nextLong();

      // create the vsid and add the user to it
      VSID vsid = new VSID(id, name, gid, date);
      vsid.addUID(uid);

      // add the vsid to the inner Hashtable
      vsidTable.put(id, vsid);

      // add the inner Hashtable to the main Hashtable
      _VSIDs.put(gid, vsidTable);

    } else {
      // here we create a new unique vsid and make sure no other vsid is
      // using the id
      id = "" + _idGenerator.nextLong();
      while (vsidTable != null && vsidTable.contains(id))
	id = "" + _idGenerator.nextLong();
      
      // create the vsid and add the user to it
      VSID vsid = new VSID(id, name, gid, date);
      vsid.addUID(uid);

      // add the vsid to the inner Hashtable
      vsidTable.put(id, vsid);
      // since the Set was already in the main Hashtable, we're done
      // here.
    }

    return id;
  }

  /**
   * join an active video session
   *
   * @param vsid: id of the video session
   * @param uid: user id
   * @param gid: group id
   * @return whether the vsid/gid video session is present
   */
  boolean joinActiveVSID(String vsid, String uid, String gid){
    // first we get all the vsid's in the gid
    Hashtable vsidTable = (Hashtable) _VSIDs.get(gid);
    if (vsidTable == null)
      return false;

    VSID session = (VSID) vsidTable.get(vsid);
    if (session != null){
      session.addUID(uid);
      long time = session.getStartTime();
      if (time != -1){
	System.out.println("joinActiveVSID: video has already started: " + time);
	System.out.println("sending message to: " + uid + "@" + gid + " for: " + vsid);
	_comm.sendPlay(vsid, uid, gid, time);
      }
      return true;
    } else 
      return false;      
  }

  /**
   * remove a user from a video session
   *
   * @param vsid: id of the video session
   * @param uid: user id
   * @param gid: group id
   * @return whether the vsid/gid video session is present
   */
  boolean removeUserFromVSID(String vsid, String uid, String gid){
    // first we get all the vsid's in the gid
    Hashtable vsidTable = (Hashtable) _VSIDs.get(gid);
    if (vsidTable != null){
      VSID session = (VSID) vsidTable.get(vsid);
      if (session != null && session.removeUID(uid)){
	if (session.getNumUIDs() == 0){
	  vsidTable.remove(session.getVSID());
	}
	return true;
      } 
    }
    return false;
  }
  
  /**
   * indicate that the video session has started (play has been pressed) 
   *
   * @param vsid: id of the video session
   * @param uid: user id
   * @param gid: group id
   */
  void playPressed(String vsid, String uid, String gid, long startTime){
    Hashtable vsidTable = (Hashtable) _VSIDs.get(gid);
    if (vsidTable != null){
      VSID session = (VSID) vsidTable.get(vsid);
      if (session != null && session.containsUID(uid) &&
	  session.getStartTime() == -1){
	session.setStartTime(startTime);
      }
    }
  }


  /**
   * print out the current WG's contents
   */
public void print(){
    // System.out.println("main Hashtable: " + _VSIDs);
    Set s = _VSIDs.keySet();
    Iterator itr = s.iterator();
    while (itr.hasNext()){
      String foogid = (String) itr.next();
      System.out.println("gid: " + foogid);
      Hashtable h = (Hashtable) _VSIDs.get(foogid);
      if (h != null){
	Set s2 = h.keySet();
	Iterator itr2 = s2.iterator();
	while (itr2.hasNext()){
	  // System.out.println("next element: " + itr2.next());
	  String foovsid = (String) itr2.next();
	  VSID foo = (VSID) h.get(foovsid);
	  System.out.println("vsid: " + foo);
	}
      }	  
    }
  }

  /**
   * thread that keeps the wgserver alive
   */
  public void run(){
    _isActive = true;
    while(_isActive){
      try {
	sleep(1000);
      } catch (InterruptedException e){
	System.out.println("WGServer error: " + e);
      }
    }
  }

  /**
   * start up the WG server process
   */
  public static void main(String[] args) {
    if(args.length != 1) {
      System.err.println("Usage: java WGServer <siena server address>");
      System.exit(1);
    }

    /** create and start the WG server */
    WGServer server = new WGServer(args[0]);

    // System.out.println("- - - joinNewVSID - - -");
    String videoName = "CS4118-10/";
    String uid = "goofy";
    String gid = "psl";
    String date = "2003-08-10;08:00:00";
    String vsid = server.joinNewVSID(videoName, uid, gid, date);
    //long startTime = System.currentTimeMillis() - 20000; // 1059408886; // 226
    // System.out.println("setting start time: " + startTime);
    // server.print();

    server.start();

    /** some unit testing
    WGServer server = new WGServer(args[0]);    

    String vsid = "11223101";
    String uid = "dp2041";
    String gid = "psl";
    String videoName = "CS4118-10/";
    String date = "2003-08-10;08:00:00";


    // get the active vsids, should be null
    System.out.println("- - - getActiveVSIDs - - -");
    Hashtable t = server.getActiveVSIDs(gid);
    if (t == null)
      System.out.println("correct, t is null");
    else 
      System.out.println("incorrect, t is not null");

    // try joining a active vsid, should return false
    System.out.println("- - - joinActiveVSID - - -");
    boolean tryJoin = server.joinActiveVSID(vsid, uid, gid);
    if (!tryJoin)
      System.out.println("correct, tryJoin failed!");
    else 
      System.out.println("incorrect, tryJoin succeeded?");

    // try joining a new vsid
    System.out.println("- - - joinNewVSID - - -");
    vsid = server.joinNewVSID(videoName, uid, gid, date);
    System.out.println("done joining new vsid, id = " + vsid);
    System.out.println("checking containers");
    server.print();

    // try joining a new vsid
    System.out.println("- - - joinNewVSID - - -");
    String newuid = "goofy";
    server.joinNewVSID(videoName, newuid, gid, date);
    server.print();

    // join an active vsid
    System.out.println("- - - joinActiveVSID - - -");
    uid = "peppo";
    tryJoin = server.joinActiveVSID(vsid, uid, gid);
    if (tryJoin)
      System.out.println("correct, tryJoin succeeded!");
    else 
      System.out.println("incorrect, tryJoin failed?");
    server.print();

    // remove a user from an active vsid
    System.out.println("- - - remove user from vsid - - -");
    boolean tryRemove = server.removeUserFromVSID(vsid, uid, gid);
    if (tryRemove)
      System.out.println("correct, tryRemove succeeded!");
    else 
      System.out.println("incorrect, tryRemove failed?");
    server.print();

    // remove a user from an active vsid
    System.out.println("- - - remove another user from vsid - - -");
    uid = "dp2041";
    tryRemove = server.removeUserFromVSID(vsid, uid, gid);
    if (tryRemove)
      System.out.println("correct, tryRemove succeeded!");
    else 
      System.out.println("incorrect, tryRemove failed?");
    server.print();
    */
  }
}
