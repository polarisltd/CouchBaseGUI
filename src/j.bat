set JAVA_HOME=C:\Apps\java\jdk1.7.0_21
set GROOVY_HOME=C:\Apps\groovy-2.3.2
set PATH=%GROOVY_HOME%\bin;%JAVA_HOME%\bin;%PATH%
set L=G:\projects\digitalroute\2014_YPTO\couchbase\couchbase.robertsp\groovy\lib
rem groovy -cp %L%\spymemcached-2.11.4.jar;%L%\couchbase-client-1.4.4.jar %1
set CLASSPATH=%L%\spymemcached-2.11.4.jar;%L%\couchbase-client-1.4.4.jar;%L%\gson-2.2.4.jar;.
javac  Rant.java
javac  StoreExample.java
