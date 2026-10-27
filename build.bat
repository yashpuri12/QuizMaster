@echo off
setlocal
cd /d "%~dp0"
if not defined JAVA_HOME (
  echo JAVA_HOME is not set. Install or configure a Java 17 JDK.
  exit /b 1
)
if not exist "%JAVA_HOME%\bin\javac.exe" (
  echo Could not find javac.exe under JAVA_HOME: %JAVA_HOME%
  exit /b 1
)
if not exist "build\classes" mkdir "build\classes"
dir /s /b src\main\java\*.java > build\sources.txt
"%JAVA_HOME%\bin\javac.exe" --release 17 -encoding UTF-8 -d build\classes @build\sources.txt
if errorlevel 1 exit /b 1
echo Build successful. Classes are in build\classes.
endlocal
