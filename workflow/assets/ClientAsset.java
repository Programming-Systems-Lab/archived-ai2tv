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

/* @generated Fri Apr 18 12:54:35 EDT 2003 from Client_assets.def - DO NOT HAND EDIT */
package psl.ai2tv.workflow.assets;
import org.cougaar.planning.ldm.asset.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.Vector;
import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
/** Representation of a GW Service **/

public class ClientAsset extends psl.ai2tv.workflow.ClientAssetAdapter {

  public ClientAsset() {
    myClientPG = null;
    myFramePG = null;
  }

  public ClientAsset(ClientAsset prototype) {
    super(prototype);
    myClientPG=null;
    myFramePG=null;
  }

  /** For infrastructure only - use org.cougaar.core.domain.Factory.copyInstance instead. **/
  public Object clone() throws CloneNotSupportedException {
    ClientAsset _thing = (ClientAsset) super.clone();
    if (myClientPG!=null) _thing.setClientPG(myClientPG.lock());
    if (myFramePG!=null) _thing.setFramePG(myFramePG.lock());
    return _thing;
  }

  /** create an instance of the right class for copy operations **/
  public Asset instanceForCopy() {
    return new ClientAsset();
  }

  /** create an instance of this prototype **/
  public Asset createInstance() {
    return new ClientAsset(this);
  }

  protected void fillAllPropertyGroups(Vector v) {
    super.fillAllPropertyGroups(v);
    { Object _tmp = getClientPG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
    { Object _tmp = getFramePG();
    if (_tmp != null && !(_tmp instanceof Null_PG)) {
      v.addElement(_tmp);
    } }
  }

  private transient ClientPG myClientPG;

  public ClientPG getClientPG() {
    ClientPG _tmp = (myClientPG != null) ?
      myClientPG : (ClientPG)resolvePG(ClientPG.class);
    return (_tmp == ClientPG.nullPG)?null:_tmp;
  }
  public void setClientPG(PropertyGroup arg_ClientPG) {
    if (!(arg_ClientPG instanceof ClientPG))
      throw new IllegalArgumentException("setClientPG requires a ClientPG argument.");
    myClientPG = (ClientPG) arg_ClientPG;
  }

  private transient FramePG myFramePG;

  public FramePG getFramePG() {
    FramePG _tmp = (myFramePG != null) ?
      myFramePG : (FramePG)resolvePG(FramePG.class);
    return (_tmp == FramePG.nullPG)?null:_tmp;
  }
  public void setFramePG(PropertyGroup arg_FramePG) {
    if (!(arg_FramePG instanceof FramePG))
      throw new IllegalArgumentException("setFramePG requires a FramePG argument.");
    myFramePG = (FramePG) arg_FramePG;
  }

  // generic search methods
  public PropertyGroup getLocalPG(Class c, long t) {
    if (ClientPG.class.equals(c)) {
      return (myClientPG==ClientPG.nullPG)?null:myClientPG;
    }
    if (FramePG.class.equals(c)) {
      return (myFramePG==FramePG.nullPG)?null:myFramePG;
    }
    return super.getLocalPG(c,t);
  }

  public PropertyGroupSchedule getLocalPGSchedule(Class c) {
    return super.getLocalPGSchedule(c);
  }

  public void setLocalPG(Class c, PropertyGroup pg) {
    if (ClientPG.class.equals(c)) {
      myClientPG=(ClientPG)pg;
    } else
    if (FramePG.class.equals(c)) {
      myFramePG=(FramePG)pg;
    } else
      super.setLocalPG(c,pg);
  }

  public void setLocalPGSchedule(PropertyGroupSchedule pgSchedule) {
      super.setLocalPGSchedule(pgSchedule);
  }

  public PropertyGroup removeLocalPG(Class c) {
    PropertyGroup removed = null;
    if (ClientPG.class.equals(c)) {
      removed=myClientPG;
      myClientPG=null;
    } else
    if (FramePG.class.equals(c)) {
      removed=myFramePG;
      myFramePG=null;
    } else
      removed=super.removeLocalPG(c);
    return removed;
  }

  public PropertyGroup removeLocalPG(PropertyGroup pg) {
    PropertyGroup removed = null;
    Class pgc = pg.getPrimaryClass();
    if (ClientPG.class.equals(pgc)) {
      removed=myClientPG;
      myClientPG=null;
    } else
    if (FramePG.class.equals(pgc)) {
      removed=myFramePG;
      myFramePG=null;
    } else
      removed= super.removeLocalPG(pg);
    return removed;
  }

  public PropertyGroupSchedule removeLocalPGSchedule(Class c) {
    PropertyGroupSchedule removed = null;
    return removed;
  }

  public PropertyGroup generateDefaultPG(Class c) {
    if (ClientPG.class.equals(c)) {
      return (myClientPG= new ClientPGImpl());
    } else
    if (FramePG.class.equals(c)) {
      return (myFramePG= new FramePGImpl());
    } else
      return super.generateDefaultPG(c);
  }

  // dumb serialization methods

  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
      if (myClientPG instanceof Null_PG || myClientPG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myClientPG);
      }
      if (myFramePG instanceof Null_PG || myFramePG instanceof Future_PG) {
        out.writeObject(null);
      } else {
        out.writeObject(myFramePG);
      }
  }

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    in.defaultReadObject();
      myClientPG=(ClientPG)in.readObject();
      myFramePG=(FramePG)in.readObject();
  }
  // beaninfo support
  private static PropertyDescriptor properties[];
  static {
    try {
      properties = new PropertyDescriptor[2];
      properties[0] = new PropertyDescriptor("ClientPG", ClientAsset.class, "getClientPG", null);
      properties[1] = new PropertyDescriptor("FramePG", ClientAsset.class, "getFramePG", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    PropertyDescriptor[] pds = super.getPropertyDescriptors();
    PropertyDescriptor[] ps = new PropertyDescriptor[pds.length+2];
    System.arraycopy(pds, 0, ps, 0, pds.length);
    System.arraycopy(properties, 0, ps, pds.length, 2);
    return ps;
  }
}
