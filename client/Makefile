NAME=AI2TVJNI
JAVADIR=c:/java1.4
SIENA_HOST=localhost
VIDEO_URL=http://franken/CS4118-10/

all:
	javac *.java
	javah -jni -classpath ../../../ psl.ai2tv.client.${NAME}Java
	cl -I${JAVADIR}/include -I${JAVADIR}/include/win32 -MD ${NAME}CPP.cpp ${JAVADIR}/lib/jvm.lib -Fe${NAME}CPP.dll
	cl -I${JAVADIR}/include -I${JAVADIR}/include/win32 -MD ${NAME}CPP.cpp ${JAVADIR}/lib/jvm.lib


jar: client
	cd ../../../; jar -cvf ai2tvClient.jar psl/ai2tv/SienaConstants.class psl/ai2tv/gauge/FrameIndexParser.class psl/ai2tv/gauge/FrameDesc.class psl/ai2tv/client/*.class psl/ai2tv/client/ai2tv_ready.jpg 

run:
	rm -rf cache/*.jpg
	java -Dai2tv.videoURL ${VIDEO_URL}="http://franken/CS4118-10/" -Dai2tv.server=ka:${SIENA_HOST}:4444 psl.ai2tv.client.Client 2> errors

# this is to run the jni executable (if it's not already hooked onto another main)
#	java -classpath . ${NAME}Java

mid:
	javap -classpath . -s -p ${NAME}Java

client:
	javac Client.java CommController.java CacheController.java TimeController.java Viewer.java ClientProbe.java ClientEffector.java
clean:
	rm -rf *.class ${NAME}Java.h ${NAME}Java.class ${NAME}CPP.obj ${NAME}CPP.exp ${NAME}CPP.dll ${NAME}CPP.lib ${NAME}CPP.exe psl_ai2tv_client_AI2TVJNIJava.h
