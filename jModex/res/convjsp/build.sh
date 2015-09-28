# Sample file for running the Jasper JspC compiler
# Please set the following variables to the correct values for your environment

#Tomcat home directory
TOMCAT_HOME=/opt/tomcat

#Path to the Web application (location of input JSP files)
#Please DO NOT include a trailing / in the path name
PATH_TO_WEBAPP=bookstore_jsp

#Destination path where the servlet-based application will be generated
PATH_TO_DESTINATION=bookstore_gen

mkdir -p $PATH_TO_DESTINATION
cp -R $PATH_TO_WEBAPP/ $PATH_TO_DESTINATION

ant -Dtomcat.home=$TOMCAT_HOME -Dwebapp.path=$PATH_TO_DESTINATION

