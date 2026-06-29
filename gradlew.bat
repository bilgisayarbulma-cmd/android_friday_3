@echo off
rem Bu dosya Android Studio'nun otomatik olusturdugu standart gradlew.bat'tir.
set DIRNAME=%~dp0
set APP_HOME=%DIRNAME%
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

if "%JAVA_HOME%"=="" (
    set JAVA_EXE=java.exe
) else (
    set JAVA_EXE=%JAVA_HOME%\bin\java.exe
)

"%JAVA_EXE%" -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
