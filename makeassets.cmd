rem Script to generate asset classes
set ALP_INSTALL_PATH=\pslcvs\tools\cougaar-8.8
set COUGAAR_LIB=%ALP_INSTALL_PATH%\lib\core.jar;%ALP_INSTALL_PATH%\lib\build.jar;%ALP_INSTALL_PATH%\lib\glm.jar;%ALP_INSTALL_PATH%\lib\planserver.jar
set SIENA_LIB=\pslcvs\tools\siena-1.4.2\siena-1.4.2.jar
set CP=\pslcvs;%SIENA_LIB%;%COUGAAR_LIB%

set CODEDIR=\pslcvs\psl\ai2tv

echo on

rem Regenerate and recompile all property/asset files
cd workflow\assets
java -classpath %CP% org.cougaar.tools.build.AssetWriter properties.def -Ppsl.ai2tv.workflow.assets Report_assets.def 
java -classpath %CP% org.cougaar.tools.build.AssetWriter properties.def -Ppsl.ai2tv.workflow.assets Client_assets.def 
java -classpath %CP% org.cougaar.tools.build.PGWriter properties.def
cd ..\..
