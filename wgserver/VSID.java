/*
 * @(#)VSID.java
 *
 * Copyright (c) 2001: The Trustees of Columbia University in the City of New York.  All Rights Reserved
 *
 * Copyright (c) 2001: @author Dan Phung
 * Last modified by: Dan Phung (dp2041@cs.columbia.edu)
 *
 * CVS version control block - do not edit manually
 *  $RCSfile$
 *  $Revision$
 *  $Date$
 *  $Source:
 */

package psl.ai2tv.wgserver;
import java.util.*;

/**
 * Video session ID's that keep track of the workgroup and users of
 * the video session.
 */
class VSID{
  /** id of this video session */
  private String _vsid;
  /** name of the video */
  private String _name;
  /** date;time that the video session is planned to start */
  private String _date;
  /** the group id associated with the clients viewing this video session */
  private String _gid;
  /** the users viewing this video session */
  private Set _uids;

  /** create a new video session */
  VSID(String id, String name, String gid, String date){
    if (name.endsWith("/")){
      name = name.substring(0, (name.length() - 1));
    }
    _vsid = id;
    _name = name;
    _gid = gid;
    _date = date;
    _uids = new HashSet();
  }
  
  /**
   * @return unique id of this video session
   */
  String getVSID(){
    return _vsid;
  }

  /**
   * @return name of the video
   */
  String getName(){
    return _name;
  }

  /**
   * @return the workgroup id associated with this video session
   */
  String getGID(){
    return _gid;
  }

  /**
   * @return the date that the video session plans to start
   */
  String getDate(){
    return _date;
  }

  /**
   * @return the users associated with this video session
   */
  Set getUIDs(){
    return _uids;
  }

  /**
   * @return the number of users associated with this video session
   */
  int getNumUIDs(){
    return _uids.size();
  }

  /**
   * add a user to this video session
   *
   * @param uid: uid of the user to be added
   */
  boolean addUID(String uid){
    if (_uids.contains(uid))
      return false;
    _uids.add(uid);
    return true;
  }

  /**
   * remove a user from this video session
   *
   * @param uid: uid of the user to be removed
   */
  boolean removeUID(String uid){
    if (!_uids.contains(uid))
      return false;
    _uids.remove(uid);
    return true;
  }
  
  /** 
   * String representation of a VSID
   */
  public String toString(){
    String rep = _vsid + ": " + _name + ", " + _date + ", " + _gid + ": users = ";
    Iterator i = _uids.iterator();
    while (i.hasNext()){
      rep += (String) i.next();
      if (i.hasNext())
	rep += ", ";      
    }
    return rep;
  }
}
