set ALP_INSTALL_PATH=%PSL_HOME%\tools\cougaar-8.8
set COUGAAR_LIB=%ALP_INSTALL_PATH%\lib\core.jar;%ALP_INSTALL_PATH%\lib\build.jar;%ALP_INSTALL_PATH%\lib\glm.jar;%ALP_INSTALL_PATH%\lib\planserver.jar
set SIENA_LIB=\pslcvs\tools\siena-1.4.2\siena-1.4.2.jar
set CP=\pslcvs;%SIENA_LIB%;%COUGAAR_LIB%

set CODEDIR=\pslcvs\psl\ai2tv

call makeassets


javac -classpath %CP% %CODEDIR%\gauge\*.java %CODEDIR%\gauge\simulclient\*.java %CODEDIR%\gauge\visual\*.java %CODEDIR%\workflow\*.java %CODEDIR%\workflow\assets\*.java 