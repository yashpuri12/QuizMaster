@echo off
setlocal
cd /d "%~dp0"
call build.bat
if errorlevel 1 exit /b 1
if not exist "build\test-classes" mkdir "build\test-classes"
dir /s /b src\test\java\*.java > build\test-sources.txt
"%JAVA_HOME%\bin\javac.exe" --release 17 -encoding UTF-8 -cp build\classes -d build\test-classes @build\test-sources.txt
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -cp build\classes;build\test-classes com.quizmaster.Phase2Tests
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -cp build\classes;build\test-classes com.quizmaster.Phase3Tests
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -cp build\classes;build\test-classes com.quizmaster.Phase4Tests
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -cp build\classes;build\test-classes com.quizmaster.Phase5Tests
if errorlevel 1 exit /b 1
"%JAVA_HOME%\bin\java.exe" -cp build\classes;build\test-classes com.quizmaster.Phase7Tests
if errorlevel 1 exit /b 1
endlocal
