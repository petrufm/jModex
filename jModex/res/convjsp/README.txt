Compiling the JSP pages into servlet classes


This set of scripts use JSPC (javadoc: http://tomcat.apache.org/tomcat-6.0-doc/api/org/apache/jasper/JspC.html) to read a JSP-based application and generate the corresponding Java servlets that represent the actual executable form of the application.

This package includes the JSPC ant file (build.xml), along with a shell script (build.sh) to start it. The script assumes the Apache Ant is properly configured, and the ant executable (shell script) is available in the $PATH.
Tomcat 6 must also be installed in the system.

In order to run the build.sh script, you must first edit it to modify the following environment variables so that they point to the proper paths in your system:

The Tomcat home directory
TOMCAT_HOME=/opt/tomcat

The path to the Web application (the location of input JSP files). Please DO NOT include a trailing / in the path name
PATH_TO_WEBAPP=bookstore_jsp

The destination path where the servlet-based application will be generated
PATH_TO_DESTINATION=bookstore_gen

The above values are examples you should modify them accordingly.

Once the variables are correctly set, simply running build.sh will generate the servlets.

Please note that, when importing the generated project in Eclipse, an additional setting must be done to properly run the jModex plugin. The Eclipse project containing the case study depends on several jar files, included in the res/dependencies directory.
 