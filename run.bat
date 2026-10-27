@echo off
setlocal
cd /d "%~dp0"
if not exist "build\classes\com\quizmaster\App.class" (
  call build.bat
  if errorlevel 1 exit /b 1
)
if defined JAVA_HOME (
  "%JAVA_HOME%\bin\java.exe" -cp build\classes com.quizmaster.App
) else (
  java -cp build\classes com.quizmaster.App
)
endlocal
