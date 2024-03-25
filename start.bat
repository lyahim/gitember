@echo off
setlocal

IF NOT DEFINED JAVA_HOME17 (
	echo Please initialize JAVA_HOME17 variable first!
	exit /b 1
)

"%JAVA_HOME17%\bin\java" -jar gitember-2.5-spring-boot.jar