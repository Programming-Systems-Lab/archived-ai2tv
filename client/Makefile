NAME=AI2TVJNI
JAVADIR=c:/java1.4
SIENA_HOST=localhost

all:
	javac *.java
	javah -jni -classpath ../../../ psl.ai2tv.client.${NAME}Java
	cl -I${JAVADIR}/include -I${JAVADIR}/include/win32 -MD ${NAME}CPP.cpp ${JAVADIR}/lib/jvm.lib -Fe${NAME}CPP.dll
	cl -I${JAVADIR}/include -I${JAVADIR}/include/win32 -MD ${NAME}CPP.cpp ${JAVADIR}/lib/jvm.lib


jar:
	jar cvf AI2TVClient.jar Client.class 

# psl.ai2tv.client.CommController.class psl.ai2tv.client.CacheController.class psl.ai2tv.client.TimeController.class psl.ai2tv.client.Viewer.class psl.ai2tv.client.ClientProbe.class psl.ai2tv.client.ClientEffector.class

run:
	rm -rf cache/*.jpg
	java -Dai2tv.server=ka:${SIENA_HOST}:4444 psl.ai2tv.client.Client 2> errors
#	java -classpath . ${NAME}Java

mid:
	javap -classpath . -s -p ${NAME}Java

client:
	javac Client.java CommController.java CacheController.java TimeController.java Viewer.java ClientProbe.java ClientEffector.java

clean:
	rm -rf *.class ${NAME}Java.h ${NAME}Java.class ${NAME}CPP.obj ${NAME}CPP.exp ${NAME}CPP.dll ${NAME}CPP.lib ${NAME}CPP.exe psl_ai2tv_client_AI2TVJNIJava.h

