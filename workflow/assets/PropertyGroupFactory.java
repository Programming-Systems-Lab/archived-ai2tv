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
/** AbstractFactory implementation for Properties.
 * Prevents clients from needing to know the implementation
 * class(es) of any of the properties.
 **/

package psl.ai2tv.workflow.assets;

import org.cougaar.planning.ldm.measure.*;
import org.cougaar.planning.ldm.asset.*;
import org.cougaar.planning.ldm.plan.*;
import java.util.*;

import java.util.Vector;
import  psl.ai2tv.gauge.*;


public class PropertyGroupFactory {
  // brand-new instance factory
  public static NewClientPG newClientPG() {
    return new ClientPGImpl();
  }
  // instance from prototype factory
  public static NewClientPG newClientPG(ClientPG prototype) {
    return new ClientPGImpl(prototype);
  }

  // brand-new instance factory
  public static NewFramePG newFramePG() {
    return new FramePGImpl();
  }
  // instance from prototype factory
  public static NewFramePG newFramePG(FramePG prototype) {
    return new FramePGImpl(prototype);
  }

  // brand-new instance factory
  public static NewBucketPG newBucketPG() {
    return new BucketPGImpl();
  }
  // instance from prototype factory
  public static NewBucketPG newBucketPG(BucketPG prototype) {
    return new BucketPGImpl(prototype);
  }

  /** Abstract introspection information.
   * Tuples are {<classname>, <factorymethodname>}
   * return value of <factorymethodname> is <classname>.
   * <factorymethodname> takes zero or one (prototype) argument.
   **/
  public static String properties[][]={
    {"psl.ai2tv.workflow.assets.ClientPG", "newClientPG"},
    {"psl.ai2tv.workflow.assets.FramePG", "newFramePG"},
    {"psl.ai2tv.workflow.assets.BucketPG", "newBucketPG"}
  };
}
