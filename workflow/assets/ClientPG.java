/*
 * <copyright>
 *  Copyright 1997-2002 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

/* @generated Fri Apr 18 12:54:39 EDT 2003 from properties.def - DO NOT HAND EDIT */
/** Primary client interface for ClientPG.
 * representation of an A2TV client as sampled
 *  @see NewClientPG
 *  @see ClientPGImpl
 **/

package psl.ai2tv.workflow.assets;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;

import java.util.Vector;
import  psl.ai2tv.gauge.*;


public interface ClientPG extends PropertyGroup, org.cougaar.planning.ldm.dq.HasDataQuality {
  /** unique client ID **/
  String getId();
  /** host name **/
  String getHost();
  /** last bandwidth measure **/
  double getBandwidth();
  /**  moment of the sample - same as the equivalent field in BucketPG **/
  long getSampleTime();

  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newClientPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "psl.ai2tv.workflow.assets.NewClientPG";
  /** the factory class **/
  Class factoryClass = psl.ai2tv.workflow.assets.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = psl.ai2tv.workflow.assets.ClientPG.class;
  String assetSetter = "setClientPG";
  String assetGetter = "getClientPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  ClientPG nullPG = new Null_ClientPG();

/** Null_PG implementation for ClientPG **/
final class Null_ClientPG
  implements ClientPG, Null_PG
{
  public String getId() { throw new UndefinedValueException(); }
  public String getHost() { throw new UndefinedValueException(); }
  public double getBandwidth() { throw new UndefinedValueException(); }
  public long getSampleTime() { throw new UndefinedValueException(); }
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
  public NewPropertyGroup unlock(Object key) { return null; }
  public PropertyGroup lock(Object key) { return null; }
  public PropertyGroup lock() { return null; }
  public PropertyGroup copy() { return null; }
  public Class getPrimaryClass(){return primaryClass;}
  public String getAssetGetMethod() {return assetGetter;}
  public String getAssetSetMethod() {return assetSetter;}
  public Class getIntrospectionClass() {
    return ClientPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
  public org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return null; }
}

/** Future PG implementation for ClientPG **/
final class Future
  implements ClientPG, Future_PG
{
  public String getId() {
    waitForFinalize();
    return _real.getId();
  }
  public String getHost() {
    waitForFinalize();
    return _real.getHost();
  }
  public double getBandwidth() {
    waitForFinalize();
    return _real.getBandwidth();
  }
  public long getSampleTime() {
    waitForFinalize();
    return _real.getSampleTime();
  }
  public Object clone() throws CloneNotSupportedException {
    throw new CloneNotSupportedException();
  }
  public NewPropertyGroup unlock(Object key) { return null; }
  public PropertyGroup lock(Object key) { return null; }
  public PropertyGroup lock() { return null; }
  public PropertyGroup copy() { return null; }
  public Class getPrimaryClass(){return primaryClass;}
  public String getAssetGetMethod() {return assetGetter;}
  public String getAssetSetMethod() {return assetSetter;}
  public Class getIntrospectionClass() {
    return ClientPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }
  public synchronized org.cougaar.planning.ldm.dq.DataQuality getDataQuality() {
    return (_real==null)?null:(_real.getDataQuality());
  }

  // Finalization support
  private ClientPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof ClientPG) {
      _real=(ClientPG) real;
      notifyAll();
    } else {
      throw new IllegalArgumentException("Finalization with wrong class: "+real);
    }
  }
  private synchronized void waitForFinalize() {
    while (_real == null) {
      try {
        wait();
      } catch (InterruptedException _ie) {
        // We should really let waitForFinalize throw InterruptedException
        Thread.interrupted();
      }
    }
  }
}
}
