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
  // libpath, the settings set below are not present.
  libpath = "-Djava.class.path=c:/pslroot/psl/ai2tv/client;c:/pslroot;.";

  // this is the default base video URL 
  baseURL = "-Dai2tv.baseURL=http://franken.psl.cs.columbia.edu/ai2tv/";

  // this is the default siena server
  sienaServer = "-Dai2tv.server=ka:franken.psl.cs.columbia.edu:4444";

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
  options[3].optionString = baseURL;  /* the base video URL */
  options[4].optionString = sienaServer; /* the siena comm server */
  args.options = options;
  args.nOptions = 5;
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
  _env->CallVoidMethod(_obj, mid, time);
  printf("DDD>\n");
}

/**
 * This functon returns the AI2TV Client's current video time in
 * seconds.
 */
long AI2TVJNICPP::currentTime(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "currentTime","()J");
  jlong time = _env->CallLongMethod(_obj, mid);
  return time;
}

/**
 * Returns the length of the video in seconds
 */
int AI2TVJNICPP::videoLength(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "videoLength","()I");
  jint videoLength = _env->CallIntMethod(_obj, mid);

  return videoLength;
}

/**
 * sets the directory location for the frame cache to be stored
 * 
 * @param dir: directory location
 */
void AI2TVJNICPP::setCacheDir(char* dir){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "setCacheDir","(Ljava/lang/String;)V");
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
  jstring dir = (jstring) _env->CallObjectMethod(_obj, mid);
  
  return (char*) _env->GetStringUTFChars(dir,0);
}

/**
 * set the client's base URL
 * 
 * @param url: URL with the location of the available videos
 */
void AI2TVJNICPP::setBaseURL(char* url){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "setBaseURL","(Ljava/lang/String;)V");
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
  jstring dir = (jstring) _env->CallObjectMethod(_obj, mid);
  
  return (char*) _env->GetStringUTFChars(dir,0);
}

/**
 * Set the user login information in the AI2TV module.
 * 
 * NOTE!!! Need the rest of the login info to add to the param list
 * 
 * @param info: login information
 */
void AI2TVJNICPP::setLoginInfo(char* info){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "setLoginInfo","(Ljava/lang/String;)V");
  _env->CallVoidMethod(_obj, mid, _env->NewStringUTF(info));  
}

/**
 * tell the AI2TV module what video to load and when to load it by 
 * 
 * @param name: name of the video
 * @param date: date/time to load the video by
 */

void AI2TVJNICPP::loadVideo(char* name, char* date){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "setLoginInfo","(Ljava/lang/String;)V");
  _env->CallVoidMethod(_obj, mid, _env->NewStringUTF(name), _env->NewStringUTF(date));
}

/**
 * initialize the AI2TV component 
 */

void AI2TVJNICPP::initialize(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "initialize","()V");
  _env->CallVoidMethod(_obj, mid);
}

/**
 * gets the available videos from the server
 * @param videoList: pre-initialized double-array of video names
 */
// char** videoList
void AI2TVJNICPP::getAvailableVideos(char videoList[3][10]){

  strcpy (videoList[0], "CS4118-10");
  strcpy (videoList[1], "CS4118-11");
  strcpy (videoList[2], "CS4118-12");

  /*
  jboolean* isCopy = new jboolean(false);

  jmethodID mid;
  mid = _env->GetMethodID(_class, "getAvailableVideos","()[Ljava/lang/String;");
  jstring* videos = (jstring*) _env->CallObjectMethod(_obj, mid);
  // jstring videos[] = (jstring[]) _env->CallObjectMethod(_obj, mid);

  const char *str;
  for (int i=0; i<NUM_VIDEOS; i++){
    // if (videos[i] == NULL || videoList[i] == NULL)
    if (videos[i] == NULL || videoList[i] == NULL)
      break;
    // str = env->GetStringUTFChars(videos[i],isCopy);
    str = _env->GetStringUTFChars(videos[i],isCopy);
    strcpy(videoList[i], str);
    // env->ReleaseStringUTFChars(videos[i], str);
    _env->ReleaseStringUTFChars(videos[i], str);
  }
  */
}

/**
 * Returns the length of the video in seconds
 */
void AI2TVJNICPP::shutdown(){
  jmethodID mid;
  mid = _env->GetMethodID(_class, "shutdown","()V");
  _env->CallVoidMethod(_obj, mid);
}


// ----- END: JNI related functions implemented on the Java side ----- //

// ----- JNI related functions called by the Java side ----- //

/**
 * Tell the CHIME "Video" viewer to load this frame into memory
 */
JNIEXPORT void JNICALL
Java_psl_ai2tv_client_AI2TVJNIJava_loadFrame(JNIEnv *env, jobject obj, jstring frame) {
  jboolean* isCopy = new jboolean(false);
  const char *str = env->GetStringUTFChars(frame,isCopy);

  printf("c++ : loading frame %s\n", str);

  /* 
   * Mark needs to add in functionality here.
   */

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

  /* 
   * Mark needs to add in functionality here.
   */

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
  // foo->playPressed();
  char videos[3][10];
  // videos = new char[3][10];
  foo->getAvailableVideos(videos);
  for (int i=0; i<16; i++){
    printf("%s\n", videos[i]);
  }

  

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


