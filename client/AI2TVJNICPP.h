/*
 * @(#)AI2TVJNICPP.h
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
 *  $Source$
 */

#if !defined(_AI2TVJNICPP_H_)
#define _AI2TVJNICPP_H_

#include <stdio.h>
#include <stdlib.h>
#include "jni_md.h"
#include "jni.h"
#include "psl_ai2tv_client_AI2TVJNIJava.h"

/**
 * The CPP side JNI interface for the AI2TV client.
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
class AI2TVJNICPP
{
  int DEBUG;

 public:
  AI2TVJNICPP();
  ~AI2TVJNICPP();
  
  // the following functions are called by the CPP side to execute
  // Java side methods
  void playPressed();
  void stopPressed();
  void pausePressed();
  void gotoPressed(int time);
  long currentTime();
  int videoLength();

  // These functions are the CPP functions available to the Java side.  
  // I display these here for information purposes only.
  // Java_psl_ai2tv_client_AI2TVJNIJava_shutdown(JNIEnv *env, jobject obj)
  // Java_psl_ai2tv_client_AI2TVJNIJava_displayFrame(JNIEnv *env, jobject obj)

 private:
  const char* JAVACLASS;
  char* classpath;
  char* libpath;
  JavaVM* _jvm;
  JNIEnv *_env;
  jobject _obj;   // this should really be a pointer 
  jclass _class;  // this should really be a pointer 
  JNIEnv* create_vm(JavaVM* jvm);
  void instantiateClasses();
};

#endif // !defined(_AI2TVJNICPP_H_)

