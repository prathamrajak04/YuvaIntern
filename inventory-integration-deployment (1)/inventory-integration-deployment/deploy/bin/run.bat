@echo off
rem Launcher for Windows.  Usage: bin\run.bat [--data=PATH] [--config=PATH] [--version] [--help]
rem Same behavior as run.sh: checks Java, switches to the install folder, starts the JAR.
rem Optional: set JAVA_OPTS=-Xmx256m before calling to pass JVM options.
setlocal

set "APP_HOME=%~dp0.."

where java >nul 2>nul
if errorlevel 1 (
  echo ERROR: Java was not found on PATH. Install a JRE or JDK 17 or newer. 1>&2
  exit /b 1
)

set "JAR="
for %%f in ("%APP_HOME%\lib\inventory-app-*.jar") do set "JAR=%%~ff"
if not defined JAR (
  echo ERROR: application JAR not found in %APP_HOME%\lib 1>&2
  exit /b 1
)

cd /d "%APP_HOME%"
java %JAVA_OPTS% -jar "%JAR%" --config="%APP_HOME%\conf\application.properties" %*
exit /b %ERRORLEVEL%
