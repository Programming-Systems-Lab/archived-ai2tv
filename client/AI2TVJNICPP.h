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
#include "psl_ai2tv_client_AI2TVJNIJava.h"
#include "c:\\java1.4\\include\\win32\\jni_md.h"
#include "c:\\java1.4\\include\\jni.h"

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
  void playPressed();

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
