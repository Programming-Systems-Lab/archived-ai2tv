set PSL_HOME=\pslcvs
set ALP_INSTALL_PATH=%PSL_HOME%\tools\cougaar-8.8
set COUGAAR_LIB=%ALP_INSTALL_PATH%\lib\core.jar;%ALP_INSTALL_PATH%\lib\build.jar;%ALP_INSTALL_PATH%\lib\glm.jar;%ALP_INSTALL_PATH%\lib\planserver.jar
set SIENA_LIB=\pslcvs\tools\siena-1.4.2\siena-1.4.2.jar
set LIBPATHS=%ALP_INSTALL_PATH%\sys\xerces.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\sys\log4j.jar
set LIBPATHS=%LIBPATHS%;%ALP_INSTALL_PATH%\sys\jsse.jar

set CP=\pslcvs;%SIENA_LIB%;%COUGAAR_LIB%;%LIBPATHS%

set CODEDIR=\pslcvs\psl\ai2tv

set MYPROPERTIES=-Dorg.cougaar.useBootstrapper=false -DWVM_RMI_PORT=3800 -DDEBUG=1 -Dpsl.ai2tv.frameindex=%2
set MYMEMORY=
set MYCLASSES=org.cougaar.core.society.Node
set MYARGUMENTS= -c -n "%1"

@ECHO ON

java.exe %MYPROPERTIES% %MYMEMORY% -classpath %CP% %MYCLASSES% %MYARGUMENTS%
