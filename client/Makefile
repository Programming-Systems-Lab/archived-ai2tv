name=AI2TVJNI

all:
	javac *.java
	javah -jni -classpath . ${name}Java
	cl -Ic:/java1.4/include -Ic:/java1.4/include/win32 -MD ${name}CPP.cpp c:/java1.4/lib/jvm.lib -Fe${name}CPP.dll
	cl -Ic:/java1.4/include -Ic:/java1.4/include/win32 -MD ${name}CPP.cpp c:/java1.4/lib/jvm.lib


run:
	java -classpath . ${name}Java

mid:
	javap -classpath . -s -p ${name}Java

client:
	javac Client.java CommController.java CacheController.java Viewer.java ClientProbe.java FauxWF.java

clean:
	rm -rf *.class ${name}Java.h ${name}Java.class ${name}CPP.obj ${name}CPP.exp ${name}CPP.dll ${name}CPP.lib ${name}CPP.exe

