/*
 * @(#)AI2TVJNICPP.cpp
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

#include "AI2TVJNICPP.h"

int isActive = 1;

/**
 * The CPP side JNI interface for the AI2TV client.
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
AI2TVJNICPP::AI2TVJNICPP(){
  DEBUG=1;
  // make sure the base psl dir is in your classpath
  JAVACLASS = "psl/ai2tv/client/AI2TVJNIJava"; 
  classpath = "-Djava.class.path=c:/pslroot/psl/ai2tv/client;c:/pslroot;.";

  // note: don't know why, but setting this libpath here doesn't have any 
  // effect later, meaning that on the Java side, should you print out the 
  // libpath, the settings set below are now present.
  libpath = "-Djava.class.path=c:/pslroot/psl/ai2tv/client;c:/pslroot;.";

  _jvm = NULL;
  _env = NULL;
  _class = NULL;
  _obj = NULL;

  if (DEBUG > 0)
    printf("Creating the Java VM\n");
  _env = create_vm(_jvm);
  if (_env == NULL) return;

  // printf("getting environment \n");
  // void** env = NULL;
  // jint args = NULL;
  // _jvm->GetEnv(env, args);
  // printf("jint %d\n", args);

  instantiateClasses();
}

/**
 * Desctructor: cleanup class variables, destroy the JVM.  We should
 * delete the objects that we used here.
 */
AI2TVJNICPP::~AI2TVJNICPP(){
  if (DEBUG > 0)
    printf("Shutting down the Java VM");  
  int error = _jvm->DestroyJavaVM();
  if (error != 0)
    printf("Error in shutting down the Java VM");      
}

/**
 * Create the Java Virutal Machine
 */
JNIEnv* AI2TVJNICPP::create_vm(JavaVM* jvm) {
  JNIEnv* env;
  JavaVMInitArgs args;
  JavaVMOption options[3];

  /* There is a new JNI_VERSION_1_4, but it doesn't matter since
     we're not using any of the new stuff for attaching to threads. */
  args.version = JNI_VERSION_1_4;
  options[0].optionString = "-Djava.compiler=NONE"; /* disable JIT */
  options[1].optionString = classpath;              /* user classes */
  options[2].optionString = libpath;  /* set native library path */
  args.options = options;
  args.nOptions = 3;
  args.ignoreUnrecognized = JNI_TRUE;

  if( (JNI_CreateJavaVM(&jvm, (void **)&env, &args)) < 0 )
    {
      printf("Could not create JVM\n");
      // exit(1);
      return NULL;
    }
  return env;
}

/**
 * Initiatiate the java class to be used (AI2TVJNIJava)
 */
void AI2TVJNICPP::instantiateClasses(){
  if (_class == NULL) {
    if (DEBUG > 0)
      printf("Finding the class\n");
    _class = _env->FindClass(JAVACLASS);
  }

  printf("the env <%d>\n", _env);
  printf("the class <%s> <%d>\n", JAVACLASS, _class);
  if (_class == NULL) return;

  if (_obj == NULL) {
  if (DEBUG > 0)
    printf("Instantiating the JObject\n");

    jmethodID mid = _env->GetMethodID(_class, "<init>", "()V");
    _obj = _env->NewObject(_class, mid);
  }
}

// ----- JNI related functions implemented on the Java side ----- //

/**
 * Tell the Java client that the play button was pressed
 */
void AI2TVJNICPP::playPressed(){
  /* instantiate object, call play */
  jmethodID mid;

  printf("AI2TVJNICPP::playPressed trying to find methodID <CCC ... ");
  mid = _env->GetMethodID(_class, "playPressed","()V");
  if (mid == 0) 
    printf("CCC> no method found with id: playPressed ()V\n");
  else
    printf("CCC> found methodID\n");

  printf("AI2TVJNICPP::playPressed calling the method <DDD ... ");
  _env->CallVoidMethod(_obj, mid);
  printf("DDD>\n");

}

/**
 * Tell the Java client that the stop button was pressed
 */
void AI2TVJNICPP::stopPressed(){
  /* instantiate object, call stop */
  jmethodID mid;

  printf("AI2TVJNICPP::stopPressed trying to find methodID <CCC ... ");
  mid = _env->GetMethodID(_class, "stopPressed","()V");
  if (mid == 0) 
    printf("CCC> no method found with id: stopPressed ()V\n");
  else 
    printf("CCC> found methodID\n");

  printf("AI2TVJNICPP::stopPressed calling the method <DDD ... ");
  _env->CallVoidMethod(_obj, mid);
  printf("DDD>\n");
}

/**
 * Tell the Java client that the pause button was pressed
 */
void AI2TVJNICPP::pausePressed(){
  /* instantiate object, call pause */
  jmethodID mid;

  printf("AI2TVJNICPP::pausePressed trying to find methodID <CCC ... ");
  mid = _env->GetMethodID(_class, "pausePressed","()V");
  if (mid == 0) 
    printf("CCC> no method found with id: pausePressed ()V\n");
  else 
    printf("CCC> found methodID\n");

  printf("AI2TVJNICPP::pausePressed calling the method <DDD ... ");
  _env->CallVoidMethod(_obj, mid);
  printf("DDD>\n");
}

/**
 * Tell the Java client that the goto button was pressed
 *
 * @param time: time to jump to
 */
void AI2TVJNICPP::gotoPressed(int time){
  /* instantiate object, call goto */
  jmethodID mid;

  printf("AI2TVJNICPP::gotoPressed trying to find methodID <CCC ... ");
  mid = _env->GetMethodID(_class, "gotoPressed","(I)V");
  if (mid == 0)
    printf("CCC> no method found with the id: gotoPressed (I)V\n");
  else
    printf("CCC> found methodID\n");

  printf("AI2TVJNICPP::pausePressed calling the method <DDD ... ");
  _env->CallVoidMethod(_obj, mid);
  printf("DDD>\n");
}

/**
 * This functon returns the AI2TV Client's current video time in
 * seconds.
 */
long AI2TVJNICPP::currentTime(){
  /* instantiate object, call goto */
  jmethodID mid;

  printf("AI2TVJNICPP::currentTime trying to find methodID <CCC ... ");
  mid = _env->GetMethodID(_class, "currentTime","()J");
  if (mid == 0) 
    printf("CCC> no method found with id: currentTime ()J\n");
  else 
    printf("CCC> found methodID\n");

  printf("AI2TVJNICPP::currentTime calling the method <DDD ... ");
  jlong time = _env->CallLongMethod(_obj, mid);
  printf("DDD>\n");

  return time;
}

/**
 * Returns the length of the video in seconds
 */
int AI2TVJNICPP::videoLength(){
  /* instantiate object, call goto */
  jmethodID mid;

  printf("AI2TVJNICPP::videoLength trying to find methodID <CCC ... ");
  mid = _env->GetMethodID(_class, "videoLength","()I");
  if (mid == 0)
    printf("CCC> no method found with the signature: videoLength ()I\n");
  else 
    printf("CCC> found methodID\n");

  printf("AI2TVJNICPP::videoLength calling the method <DDD ... ");
  jint videoLength = _env->CallIntMethod(_obj, mid);
  printf("DDD>\n");

  return videoLength;
}

// ----- END: JNI related functions implemented on the Java side ----- //

// ----- JNI related functions called by the Java side ----- //

/**
 * Tell the CHIME AI2TV processes to shutdown 
 */
JNIEXPORT void JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_shutdown(JNIEnv *env, jobject obj) {
  printf("C++ side: shutdown");
  isActive = 0;
}

/**
 * Set the cache dir for CyrstalSpace
 */
JNIEXPORT void JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_setCacheDir(JNIEnv *env, jobject obj, jstring dir) {
  jboolean* isCopy = new jboolean(false);
  const char *str = env->GetStringUTFChars(dir,isCopy);

  printf("c++ : Displayed frame %s\n", str);
  env->ReleaseStringUTFChars(dir, str);

  return;
}

/**
 * Tell the CHIME "Video" viewer to load this frame into memory
 */
JNIEXPORT void JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_loadFrame(JNIEnv *env, jobject obj, jstring frame) {
  jboolean* isCopy = new jboolean(false);
  const char *str = env->GetStringUTFChars(frame,isCopy);

  printf("c++ : Displayed frame %s\n", str);
  env->ReleaseStringUTFChars(frame, str);

  return;
}

/**
 * Tell the CHIME "Video" viewer to display this frame
 */
JNIEXPORT void JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_displayFrame(JNIEnv *env, jobject obj, jstring frame) {
  jboolean* isCopy = new jboolean(false);
  const char *str = env->GetStringUTFChars(frame,isCopy);

  printf("c++ : Displayed frame %s\n", str);
  env->ReleaseStringUTFChars(frame, str);

  return;
}

// ----- END: JNI related functions called by the Java side ----- //

/**
 * point of entry, uncomment if you're going to use this class from
 * the command line.  This main function is only for testing purposes.
 */
int main(int argc, char **argv) {
  // JNIEnv* env = create_vm();
  AI2TVJNICPP* foo = new AI2TVJNICPP();
  printf("success, now trying to invoke a class\n");
  foo->playPressed();
  printf("\n");

  printf("Entering wait thread\n");  
  while(isActive != 0){
    printf("sleeping...\n");
    system("sleep 5");
    printf("awake!\n");
  }
  printf("Out of wait thread\n");  

  printf("<ZZZ - ");
  if (foo != NULL)
    delete foo;
  printf(" - ZZZ>\n");
  return 0;
}


