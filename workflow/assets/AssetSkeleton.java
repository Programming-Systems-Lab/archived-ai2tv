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
/** Abstract Asset Skeleton implementation
 * Implements default property getters, and additional property
 * lists.
 * Intended to be extended by org.cougaar.planning.ldm.asset.Asset
 **/

package psl.ai2tv.workflow.assets;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;

import java.util.Vector;
import  psl.ai2tv.gauge.*;

import java.io.Serializable;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;

public abstract class AssetSkeleton extends org.cougaar.planning.ldm.asset.Asset {

  protected AssetSkeleton() {}

  protected AssetSkeleton(AssetSkeleton prototype) {
    super(prototype);
  }

  /**                 Default PG accessors               **/

  /** Search additional properties for a ClientPG instance.
   * @return instance of ClientPG or null.
   **/
  public ClientPG getClientPG()
  {
    ClientPG _tmp = (ClientPG) resolvePG(ClientPG.class);
    return (_tmp==ClientPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a ClientPG
   **/
  public boolean hasClientPG() {
    return (getClientPG() != null);
  }

  /** Set the ClientPG property.
   * The default implementation will create a new ClientPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setClientPG(PropertyGroup aClientPG) {
    if (aClientPG == null) {
      removeOtherPropertyGroup(ClientPG.class);
    } else {
      addOtherPropertyGroup(aClientPG);
    }
  }

  /** Search additional properties for a FramePG instance.
   * @return instance of FramePG or null.
   **/
  public FramePG getFramePG()
  {
    FramePG _tmp = (FramePG) resolvePG(FramePG.class);
    return (_tmp==FramePG.nullPG)?null:_tmp;
  }

  /** Test for existence of a FramePG
   **/
  public boolean hasFramePG() {
    return (getFramePG() != null);
  }

  /** Set the FramePG property.
   * The default implementation will create a new FramePG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setFramePG(PropertyGroup aFramePG) {
    if (aFramePG == null) {
      removeOtherPropertyGroup(FramePG.class);
    } else {
      addOtherPropertyGroup(aFramePG);
    }
  }

  /** Search additional properties for a BucketPG instance.
   * @return instance of BucketPG or null.
   **/
  public BucketPG getBucketPG()
  {
    BucketPG _tmp = (BucketPG) resolvePG(BucketPG.class);
    return (_tmp==BucketPG.nullPG)?null:_tmp;
  }

  /** Test for existence of a BucketPG
   **/
  public boolean hasBucketPG() {
    return (getBucketPG() != null);
  }

  /** Set the BucketPG property.
   * The default implementation will create a new BucketPG
   * property and add it to the otherPropertyGroup list.
   * Many subclasses override with local slots.
   **/
  public void setBucketPG(PropertyGroup aBucketPG) {
    if (aBucketPG == null) {
      removeOtherPropertyGroup(BucketPG.class);
    } else {
      addOtherPropertyGroup(aBucketPG);
    }
  }

}
