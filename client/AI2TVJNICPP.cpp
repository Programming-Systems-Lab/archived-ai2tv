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

#include "cssysdef.h"
#include "ChimeSystemDriver.h"
#include "AI2TVJNICPP.h"

// The global pointer to ChimeSystemDriver
extern ChimeSystemDriver *driver;

/**
 * The CPP side JNI interface for the AI2TV client.
 *
 * NOTE: need to have the environment vars seem to be ignored, and
 * they must be set internally in here.  Also, Dan needs to figure out
 * how to set and pass these vars from the command line or
 * something... (instead of hard-wiring these in here)
 *
 * @version	$$
 * @author	Dan Phung (dp2041@cs.columbia.edu)
 */
AI2TVJNICPP::AI2TVJNICPP(){
  _isActive = 1;
  doDEBUG=1;
  // make sure the base psl dir is in your classpath
  JAVACLASS = "psl/ai2tv/client/AI2TVJNIJava"; 
 
  classpath = "-Djava.class.path=c:/pslroot/psl/ai2tv/client/build;c:/pslroot/jars/siena-1.4.3.jar;.";

  libpath = "-Djava.library.path=c:/pslroot/psl/ai2tv/client/";
  // this is the default base video URL 
  baseURL = "-Dai2tv.baseURL=http://wall.psl.cs.columbia.edu/ai2tv/";

  // this is the default siena server
  // sienaServer = "-Dai2tv.server=ka:franken.psl.cs.columbia.edu:4444";
  sienaServer = "-Dai2tv.server=ka:localhost:4444";

  _jvm = NULL;
  _env = NULL;
  _class = NULL;
  _obj = NULL;

  if (doDEBUG > 0)
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
 * Destructor: cleanup class variables, destroy the JVM.  We should
 * delete the objects that we used here. 
 * 
 * Note: because of Sun JDK issues, the DestroyJavaVM() method will
 * always return/cause an error, so don't worry if that happens
 * 
 * ref (page relevant as of: 2003 August 4)
 * http://java.sun.com/j2se/1.3/docs/guide/jni/jni-12.html#DestroyJavaVM
 */
AI2TVJNICPP::~AI2TVJNICPP(){
  if (doDEBUG > 0)
    printf("Shutting down the Java VM");  
  delete _class;
  delete _obj;
  delete _env;
  int error = _jvm->DestroyJavaVM();
  if (error != 0)
    printf("Error in shutting down the Java VM");      
}

/**
 * indicates whether the client is usable
 */
int AI2TVJNICPP::isActive(){
  return _isActive;
}

/**
 * Create the Java Virutal Machine
 */
JNIEnv* AI2TVJNICPP::create_vm(JavaVM* jvm) {
  JNIEnv* env;
  JavaVMInitArgs args;
  const int numOptions = 5;
  JavaVMOption options[numOptions];

  /* There is a new JNI_VERSION_1_4, but it doesn't matter since
     we're not using any of the new stuff for attaching to threads. */
  args.version = JNI_VERSION_1_4;
  options[0].optionString = "-Djava.compiler=NONE"; /* disable JIT */
  options[1].optionString = classpath;              /* user classes */
  options[2].optionString = libpath;              /* user classes */
  options[3].optionString = baseURL;  /* the base video URL */
  options[4].optionString = sienaServer; /* the siena comm server */
  args.options = options;
  args.nOptions = numOptions;
  args.ignoreUnrecognized = JNI_TRUE;

  jint result = -1; 
  result = JNI_CreateJavaVM(&jvm, (void **)&env, &args);
  if( result < 0 )
    {
      printf("Could not create JVM, error code %d\n", result);
      // exit(1);
      _isActive = 0;
	  return NULL;
    }
  return env;
}

/**
 * Initiatiate the java class to be used (AI2TVJNIJava)
 */
void AI2TVJNICPP::instantiateClasses(){
  if (_class == NULL) { 
	  if (doDEBUG > 0)
      printf("Finding the class\n");
    _class = _env->FindClass(JAVACLASS);
  }

  printf("the env <%d>\n", _env);
  printf("the class <%s> <%d>\n", JAVACLASS, _class);
  if (_class == NULL) {
	_isActive = 0;
	return;
  }

  if (_obj == NULL) {
  if (doDEBUG > 0)
    printf("Instantiating the JObject\n");

    jmethodID mid = _env->GetMethodID(_class, "<init>", "()V");
    if (mid != 0)
      _obj = _env->NewObject(_class, mid);
    else 
      printf("Error, method ID for constructor not found!\n");
  }
}

// ----- JNI related functions implemented on the Java side ----- //

/**
 * Tell the Java client that the play button was pressed
 */
void AI2TVJNICPP::playPressed(){
  /* instantiate object, call play */
  jmethodID mid;
  mid = _env->GetMethodID(_class, "playPressed","()V");
  if (mid == 0) 
    printf("Error, playPressed method ID not found\n");
  else {
    printf("playPressed method ID: %d\n", mid);
    _env->CallVoidMethod(_obj, mid);
  }
}

/**
 * Tell the Java client that the stop button was pressed
 */
void AI2TVJNICPP::stopPressed(){
  /* instantiate object, call stop */
  jmethodID mid;

  mid = _env->GetMethodID(_class, "stopPressed","()V");
  if (mid == 0) 
    printf("Error, stopPressed method ID not found\n");
  else {
    printf("stopPressed method ID: %d\n", mid);
    _env->CallVoidMethod(_obj, mid);
  }
}

/**
 * Tell the Java client that the pause button was pressed
 */
void AI2TVJNICPP::pausePressed(){
  /* instantiate object, call pause */
  jmethodID mid;

  mid = _env->GetMethodID(_class, "pausePressed","()V");
  if (mid == 0) 
    printf("Error, pausePressed method ID not found\n");
  else {
    printf("pausePressed method ID: %d\n", mid);
    _env->CallVoidMethod(_obj, mid);
  }
}

/**
 * Tell the Java client that the goto button was pressed
 *
 * @param time: time to jump to
 */
void AI2TVJNICPP::gotoPressed(int time){
  /* instantiate object, call goto */
  jmethodID mid;

  mid = _env->GetMethodID(_class, "gotoPressed","(I)V");
  if (mid == 0)
    printf("Error, gotoPressed method ID not found\n");
  else {
    printf("gotoPressed method ID: %d\n", mid);
    _env->CallVoidMethod(_obj, mid, time);
  }
}

/**
 * This functon returns the AI2TV Client's current video time in
 * seconds.
 */
long AI2TVJNICPP::currentTime(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "currentTime","()J");
  if (mid != 0){
    jlong time = _env->CallLongMethod(_obj, mid);
    return (long) time;
  }

  return 0;
}

/**
 * Returns the length of the video in seconds
 */
int AI2TVJNICPP::videoLength(){
  jint videoLength = 0;

  jmethodID mid;
  mid = _env->GetMethodID(_class, "videoLength","()I");
  if (mid != 0){
    videoLength = _env->CallIntMethod(_obj, mid);
  }

  return videoLength;  
}

/**
 * sets the directory location for the frame cache to be stored
 * 
 * @param dir: directory location
 */
void AI2TVJNICPP::setCacheDir(const char* dir){
  jmethodID mid;

  mid = _env->GetMethodID(_class, "setCacheDir","(Ljava/lang/String;)V");
  if (mid !=0)
    _env->CallVoidMethod(_obj, mid, _env->NewStringUTF(dir));
}

/**
 * gets the current directory location of the frame cache storage
 * 
 * @return dir: directory location
 */
char* AI2TVJNICPP::getCacheDir(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "getCacheDir","()Ljava/lang/String;");
  if (mid != 0){
    jstring dir = (jstring) _env->CallObjectMethod(_obj, mid);
    return (char*) _env->GetStringUTFChars(dir,0);
  }
  return NULL;
}

/**
 * set the client's base URL
 * 
 * @param url: URL with the location of the available videos
 */
void AI2TVJNICPP::setBaseURL(char* url){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "setBaseURL","(Ljava/lang/String;)V");
  if (mid != 0)
    _env->CallVoidMethod(_obj, mid, _env->NewStringUTF(url));
}

/**
 * get the client's base URL
 * 
 * @return baseURL: URL with the location of the available videos
 */
char* AI2TVJNICPP::getBaseURL(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "getBaseURL","()Ljava/lang/String;");
  if (mid != 0){
    jstring dir = (jstring) _env->CallObjectMethod(_obj, mid);
    return (char*) _env->GetStringUTFChars(dir,0);
  }
  return NULL;
}

/**
 * Set the user login information in the AI2TV module.
 * 
 * NOTE!!! Need the rest of the login info to add to the param list
 * 
 * @param info: login information
 */
void AI2TVJNICPP::setLoginInfo(const char* login,
			       const char* password, 
			       const char* server, 
			       const char* uid, 
			       const char* gid){

  printf("AI2TVJNICPP: Login info: %s %s %s %s %s\n", login, password, server, uid, gid);

  jmethodID mid;
  mid = _env->GetMethodID(_class, "setLoginInfo","(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");

  if (mid != 0){
    _env->CallVoidMethod(_obj, mid, 
			 _env->NewStringUTF(uid),
			 _env->NewStringUTF(gid),
			 _env->NewStringUTF(password));
			 
  } else {
    printf("Error, mid for setLoginInfo not found\n");
  }

}

/**
 * tell the AI2TV module what video to load and when to load it by 
 * 
 * @param name: name of the video
 * @param date: date/time to load the video by
 */

void AI2TVJNICPP::loadVideo(char* name, char* date){
  printf("loadVideo: %s for %s\n", name, date);
  jmethodID mid;
  mid = _env->GetMethodID(_class, "loadVideo","(Ljava/lang/String;Ljava/lang/String;)V");
  if (mid != 0)
    _env->CallVoidMethod(_obj, mid, _env->NewStringUTF(name), _env->NewStringUTF(date));
}

/**
 * initialize the AI2TV component 
 */

void AI2TVJNICPP::initialize(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "initialize","()V");
  if (mid != 0)
    _env->CallVoidMethod(_obj, mid);
}

/**
 * gets the available videos from the server
 * @param videoList: pre-initialized double-array of video names
 * @return number of available videos
 */
int AI2TVJNICPP::getAvailableVideos(char availableVideos[10][50]){
  jmethodID mid;

  mid = _env->GetMethodID(_class, "getAvailableVideos","()[Ljava/lang/String;");
  printf("getAvailableVideos MID: %d\n", mid);
  if (mid == 0)
    return 0;

  jobjectArray videoObjectArray = (jobjectArray) _env->CallObjectMethod(_obj, mid);
  if (videoObjectArray == NULL)
    return 0;

  jboolean* isCopy = new jboolean(false);
  int i=0;
  int arrayLength =  _env->GetArrayLength(videoObjectArray) - 1;

  for (; i<arrayLength; i++){
    jstring video = (jstring)_env->GetObjectArrayElement(videoObjectArray, i);
    const char* str = _env->GetStringUTFChars(video,isCopy);
    strcpy(availableVideos[i], str);
    availableVideos[i][49] = '\0'; // put a cap in dat ass, cuz strpy doesn't!
    _env->DeleteLocalRef(video);
    _env->ReleaseStringUTFChars(video, str);
  }

  // clean up the used objects
  delete isCopy;
  _env->DeleteLocalRef(videoObjectArray);

  return i;
}

/**
 * Returns the length of the video in seconds
 */
void AI2TVJNICPP::shutdown(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "shutdown","()V");
  if (mid != 0)
    _env->CallVoidMethod(_obj, mid);
}


// ----- END: JNI related functions implemented on the Java side ----- //

// ----- JNI related functions called by the Java side ----- //

/**
 * Tell the CHIME "Video" viewer to load this frame into memory
 */
JNIEXPORT void JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_loadImage(JNIEnv *env, jobject obj, jstring frame) {
  jboolean* isCopy = new jboolean(false);
  const char *frameString = env->GetStringUTFChars(frame,isCopy);
  printf("c++ : loading frame %s\n", frameString);

  driver->LoadFrame (frameString, frameString);

  delete isCopy;
  env->ReleaseStringUTFChars(frame, frameString);
  return;
}

/**
 * Tell the CHIME "Video" viewer to display this frame
 */
JNIEXPORT jboolean JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_displayImage(JNIEnv *env, jobject obj, jstring frame) {
  // dan needs to figure out when to release these in memory
  jboolean* isCopy = new jboolean(false);
  jboolean* displaySuccessful = new jboolean(false);

  const char *frameString = env->GetStringUTFChars(frame,isCopy);

  printf("c++ : Displayed frame %s\n", frameString);

  driver->DisplayFrame (frameString);

  delete isCopy;
  delete displaySuccessful;
  env->ReleaseStringUTFChars(frame, frameString);
  return *displaySuccessful;
}

// ----- END: JNI related functions called by the Java side ----- //

/**
 * point of entry, uncomment if you're going to use this class from
 * the command line.  This main function is only for testing purposes.
 *
 * to run this, you need this values in these environment variables:  
 * PATH = c:\j2sdk1.4.2_01\jre\bin\client (needs the jvm.dll)
 *
 */
int main(int argc, char **argv) {
  /*
  AI2TVJNICPP* foo = new AI2TVJNICPP();
  printf("success, now trying to invoke a class method\n");
  foo->playPressed();
  
  char videos[10][50];
  int length = foo->getAvailableVideos(videos);
  printf("length of video: %d\n", length);
  // i'm not printing it correctly here, so don't count on this.
  for (int i=0; i<length; i++){
    if (videos[i] != NULL)
      printf("video %s\n", videos[i]);
    else 
      break;
  }
  

  printf("Entering wait thread\n");  
  while(foo->isActive() != 0){
    printf("sleeping...\n");
    system("sleep 5");
    printf("awake!\n");
    // _isActive = 0;
  }
  printf("Out of wait thread\n");  

  if (foo != NULL)
    delete foo;
  */

  return 0;
}

