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

/* @generated Fri Apr 18 12:54:40 EDT 2003 from properties.def - DO NOT HAND EDIT */
/** Primary client interface for BucketPG.
 * representation of a gauge sample
 *  @see NewBucketPG
 *  @see BucketPGImpl
 **/

package psl.ai2tv.workflow.assets;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;

import java.util.Vector;
import  psl.ai2tv.gauge.*;


public interface BucketPG extends PropertyGroup, org.cougaar.planning.ldm.dq.HasDataQuality {
  /** moment of the sample **/
  long getSampleTime();
  /** set of clients in the same group and sampled; includes ClientAsset instances **/
  Vector getGroup();

  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newBucketPG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "psl.ai2tv.workflow.assets.NewBucketPG";
  /** the factory class **/
  Class factoryClass = psl.ai2tv.workflow.assets.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = psl.ai2tv.workflow.assets.BucketPG.class;
  String assetSetter = "setBucketPG";
  String assetGetter = "getBucketPG";
  /** The Null instance for indicating that the PG definitely has no value **/
  BucketPG nullPG = new Null_BucketPG();

/** Null_PG implementation for BucketPG **/
final class Null_BucketPG
  implements BucketPG, Null_PG
{
  public long getSampleTime() { throw new UndefinedValueException(); }
  public Vector getGroup() { throw new UndefinedValueException(); }
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
    return BucketPGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
  public org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return null; }
}

/** Future PG implementation for BucketPG **/
final class Future
  implements BucketPG, Future_PG
{
  public long getSampleTime() {
    waitForFinalize();
    return _real.getSampleTime();
  }
  public Vector getGroup() {
    waitForFinalize();
    return _real.getGroup();
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
    return BucketPGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }
  public synchronized org.cougaar.planning.ldm.dq.DataQuality getDataQuality() {
    return (_real==null)?null:(_real.getDataQuality());
  }

  // Finalization support
  private BucketPG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof BucketPG) {
      _real=(BucketPG) real;
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
