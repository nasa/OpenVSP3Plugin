@if "%DEBUG%" == "" @echo off

:: JDK_HOME is required and must point to an installed JDK 
set "JDK_HOME=C:/Program Files/Java/jdk1.8.0_151"

:: MC_INSTALL is optional but required to use the plugin with ModelCenter
:: Comment this line if you don't have ModelCenter
::set "MC_INSTALL=C:/Program Files/Phoenix Integration/ModelCenter 12.0"

:: tool paths
set JAVAC_EXE=%JDK_HOME%/bin/javac.exe
set JAR_EXE=%JDK_HOME%/bin/jar.exe

:: directories match Netbeans file structure but can be changed
set CLASSDIR=build\classes
set JARDIR=dist

:: recreate temp directory for class files
rmdir %CLASSDIR% /s /q
md %CLASSDIR%

:: ModelCenterPlugin.java and ModelCenterPluginInfo are only used if ModelCenter is installed
if "%MC_INSTALL%"=="" goto buildOpenMDAOPlugin

echo Compiling for ModelCenter/OpenMDAO jar
"%JAVAC_EXE%" -Xlint -cp "%MC_INSTALL%/ModelCenter.jar;%MC_INSTALL%/log4j-1.2.15.jar" -d %CLASSDIR% src/main/java/openvsp3plugin/*.java
goto buildJar

:buildOpenMDAOPlugin

echo Compiling for OpenMDAO only jar
"%JAVAC_EXE%" -d %CLASSDIR% src/main/java/openvsp3plugin/DesignVariable.java src/main/java/openvsp3plugin/DesignVariableGroup.java src/main/java/openvsp3plugin/DesignVariableTableCell.java src/main/java/openvsp3plugin/JavaFXUI.java src/main/java/openvsp3plugin/Logger.java src/main/java/openvsp3plugin/OpenMDAO.java src/main/java/openvsp3plugin/OpenVSP3File.java src/main/java/openvsp3plugin/OpenVSP3Plugin.java src/main/java/openvsp3plugin/PluginState.java src/main/java/openvsp3plugin/SwingDialog.java src/main/java/openvsp3plugin/XPathUtil.java

:buildJar

echo Building jar
:: Create manifest
echo Manifest-Version: 1.0 > manifest2.mf
echo Main-Class: openvsp3plugin.OpenMDAO >> manifest2.mf
:: create directory for jar file
md %JARDIR%
:: create jar file
"%JAR_EXE%" -cfm %JARDIR%/OpenVSP3Plugin.jar manifest2.mf -C %CLASSDIR% . -C src/main/java openvsp3plugin/JavaFXUI.fxml

echo Done building OpenVSP3Plugin.jar
%COMSPEC% /C exit /B %ERRORLEVEL%
pause
