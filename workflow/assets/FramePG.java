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
/** Primary client interface for FramePG.
 * representation of a downloaded video frame
 *  @see NewFramePG
 *  @see FramePGImpl
 **/

package psl.ai2tv.workflow.assets;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;

import java.util.Vector;
import  psl.ai2tv.gauge.*;


public interface FramePG extends PropertyGroup, org.cougaar.planning.ldm.dq.HasDataQuality {
  /** hierarchy level **/
  int getLevel();
  /** frame number **/
  int getNum();
  /** start of the represented frame interval **/
  int getStart();
  /** end of the represented frame interval **/
  int getEnd();

  // introspection and construction
  /** the method of factoryClass that creates this type **/
  String factoryMethod = "newFramePG";
  /** the (mutable) class type returned by factoryMethod **/
  String mutableClass = "psl.ai2tv.workflow.assets.NewFramePG";
  /** the factory class **/
  Class factoryClass = psl.ai2tv.workflow.assets.PropertyGroupFactory.class;
  /** the (immutable) class type returned by domain factory **/
   Class primaryClass = psl.ai2tv.workflow.assets.FramePG.class;
  String assetSetter = "setFramePG";
  String assetGetter = "getFramePG";
  /** The Null instance for indicating that the PG definitely has no value **/
  FramePG nullPG = new Null_FramePG();

/** Null_PG implementation for FramePG **/
final class Null_FramePG
  implements FramePG, Null_PG
{
  public int getLevel() { throw new UndefinedValueException(); }
  public int getNum() { throw new UndefinedValueException(); }
  public int getStart() { throw new UndefinedValueException(); }
  public int getEnd() { throw new UndefinedValueException(); }
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
    return FramePGImpl.class;
  }

  public boolean hasDataQuality() { return false; }
  public org.cougaar.planning.ldm.dq.DataQuality getDataQuality() { return null; }
}

/** Future PG implementation for FramePG **/
final class Future
  implements FramePG, Future_PG
{
  public int getLevel() {
    waitForFinalize();
    return _real.getLevel();
  }
  public int getNum() {
    waitForFinalize();
    return _real.getNum();
  }
  public int getStart() {
    waitForFinalize();
    return _real.getStart();
  }
  public int getEnd() {
    waitForFinalize();
    return _real.getEnd();
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
    return FramePGImpl.class;
  }
  public synchronized boolean hasDataQuality() {
    return (_real!=null) && _real.hasDataQuality();
  }
  public synchronized org.cougaar.planning.ldm.dq.DataQuality getDataQuality() {
    return (_real==null)?null:(_real.getDataQuality());
  }

  // Finalization support
  private FramePG _real = null;
  public synchronized void finalize(PropertyGroup real) {
    if (real instanceof FramePG) {
      _real=(FramePG) real;
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
