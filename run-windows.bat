@echo off
setlocal
cd /d "%~dp0"

where java >nul 2>&1 || (
  echo ERROR: Java was not found. Install a 64-bit JDK 17 and make sure JAVA_HOME/PATH are configured.
  pause
  exit /b 1
)

call mvnw.cmd clean javafx:run
if errorlevel 1 (
  echo.
  echo The game could not be started. Review the error messages above.
  pause
  exit /b %errorlevel%
)

endlocal
