@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo   AHORCADO 1.0.0 - Verificacion de requisitos de Windows
echo ============================================================
echo.

set "ERRORS=0"

where java >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No se encontro java en PATH.
  set "ERRORS=1"
) else (
  echo [OK] Java encontrado:
  java -version 2>&1 | findstr /R /C:"version" /C:"Runtime" /C:"VM"
)

echo.
where javac >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No se encontro javac. Se necesita un JDK completo, no un JRE.
  set "ERRORS=1"
) else (
  for /f "delims=" %%V in ('javac -version 2^>^&1') do echo [OK] %%V
)

echo.
where jpackage >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No se encontro jpackage. JAVA_HOME debe apuntar a un JDK 17 completo.
  set "ERRORS=1"
) else (
  for /f "delims=" %%V in ('jpackage --version 2^>^&1') do echo [OK] jpackage %%V
)

echo.
if "%JAVA_HOME%"=="" (
  echo [AVISO] JAVA_HOME no esta definido. El build puede funcionar si PATH es correcto,
  echo         pero se recomienda configurarlo al directorio del JDK 17.
) else (
  echo [OK] JAVA_HOME=%JAVA_HOME%
)

echo.
where candle >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No se encontro candle.exe de WiX Toolset 3.x en PATH.
  set "ERRORS=1"
) else (
  echo [OK] WiX candle.exe encontrado.
)

where light >nul 2>&1
if errorlevel 1 (
  echo [ERROR] No se encontro light.exe de WiX Toolset 3.x en PATH.
  set "ERRORS=1"
) else (
  echo [OK] WiX light.exe encontrado.
)

echo.
if "%ERRORS%"=="1" (
  echo ============================================================
  echo   FALTAN REQUISITOS. Revisar BUILD_WINDOWS.md
  echo ============================================================
  exit /b 1
)

echo ============================================================
echo   TODO LISTO PARA CREAR LA VERSION WINDOWS
  echo ============================================================
exit /b 0
