set JAVA_HOME=C:\Apps\java\jdk1.7.0_21
set PATH=%JAVA_HOME%\bin;%PATH%
echo %CLASSPATH%
java -jar cbgui.jar "http://10.251.12.48:8091/pools" "LFP_Dossiers"
