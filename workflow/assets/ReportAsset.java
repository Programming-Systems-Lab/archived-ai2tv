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

/* @generated Fri Apr 18 12:54:37 EDT 2003 from Report_assets.def - DO NOT HAND EDIT */
package psl.ai2tv.workflow.assets;
import org.cougaar.planning.ldm.asset.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Vector;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
/** Representation of a gauge sample **/

public class ReportAsset extends psl.ai2tv.workflow.ReportAssetAdapter {

  public ReportAsset() {
    myBucketPG = null;
  }

  public ReportAsset(ReportAsset prototype) {
    super(prototype);
    myBucketPG=null;
  }

  /** For infrastructure only - use org.cougaar.core.domain.Factory.copyInstance instead. **/
  public Object clone() throws CloneNotSupportedException {
    ReportAsset _thing = (ReportAsset) super.clone();
    if (myBucketPG!=null) _thing.setBucketPG(myBucketPG.lock());
    return _thing;
  }

  /** create an instance of the right class for copy operations **/
  public Asset instanceForCopy() {
    return new ReportAsset();
  }

  /** create an instance of this prototype **/
  public Asset createInstance() {
    return new ReportAsset(this);
  }

  protected void fillAllPropertyGroups(Vector v) {
    super.fillAllPropertyGroups(v);
    { Object _tmp = getBucketPG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
  }

  private transient BucketPG myBucketPG;

  public BucketPG getBucketPG() {
    BucketPG _tmp = (myBucketPG != null) ?
      myBucketPG : (BucketPG)resolvePG(BucketPG.class);
    return (_tmp == BucketPG.nullPG)?null:_tmp;
  }
  public void setBucketPG(PropertyGroup arg_BucketPG) {
    if (!(arg_BucketPG instanceof BucketPG))
      throw new IllegalArgumentException("setBucketPG requires a BucketPG argument.");
    myBucketPG = (BucketPG) arg_BucketPG;
  }

  // generic search methods
  public PropertyGroup getLocalPG(Class c, long t) {
    if (BucketPG.class.equals(c)) {
      return (myBucketPG==BucketPG.nullPG)?null:myBucketPG;
    }
    return super.getLocalPG(c,t);
  }

  public PropertyGroupSchedule getLocalPGSchedule(Class c) {
    return super.getLocalPGSchedule(c);
  }

  public void setLocalPG(Class c, PropertyGroup pg) {
    if (BucketPG.class.equals(c)) {
      myBucketPG=(BucketPG)pg;
    } else
      super.setLocalPG(c,pg);
  }

  public void setLocalPGSchedule(PropertyGroupSchedule pgSchedule) {
      super.setLocalPGSchedule(pgSchedule);
  }

  public PropertyGroup removeLocalPG(Class c) {
    PropertyGroup removed = null;
    if (BucketPG.class.equals(c)) {
      removed=myBucketPG;
      myBucketPG=null;
    } else
      removed=super.removeLocalPG(c);
    return removed;
  }

  public PropertyGroup removeLocalPG(PropertyGroup pg) {
    PropertyGroup removed = null;
    Class pgc = pg.getPrimaryClass();
    if (BucketPG.class.equals(pgc)) {
      removed=myBucketPG;
      myBucketPG=null;
    } else
      removed= super.removeLocalPG(pg);
    return removed;
  }

  public PropertyGroupSchedule removeLocalPGSchedule(Class c) {
    PropertyGroupSchedule removed = null;
    return removed;
  }

  public PropertyGroup generateDefaultPG(Class c) {
    if (BucketPG.class.equals(c)) {
      return (myBucketPG= new BucketPGImpl());
    } else
      return super.generateDefaultPG(c);
  }

  // dumb serialization methods

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
      if (myBucketPG instanceof Null_PG || myBucketPG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myBucketPG);
      }
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
      myBucketPG=(BucketPG)in.readObject();
  }
  // beaninfo support
  private static PropertyDescriptor properties[];
  static {
    try {
      properties = new PropertyDescriptor[1];
      properties[0] = new PropertyDescriptor("BucketPG", ReportAsset.class, "getBucketPG", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pds = super.getPropertyDescriptors();
    PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+1];
    System.arraycopy(pds, 0, ps, 0, pds.length);
    System.arraycopy(properties, 0, ps, pds.length, 1);
    return ps;
  }
}
