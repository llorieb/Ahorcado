@echo off
setlocal
cd /d "%~dp0"

call 00-verificar-requisitos.bat
if errorlevel 1 exit /b %errorlevel%

echo.
echo ============================================================
echo   1/2 - Compilando Ahorcado 1.0.0 con Maven
echo ============================================================
call mvnw.cmd clean package -DskipTests
if errorlevel 1 exit /b %errorlevel%

if exist target\portable rmdir /s /q target\portable
mkdir target\portable

echo.
echo ============================================================
echo   2/2 - Creando aplicacion portable autocontenida
echo ============================================================

jpackage ^
  --type app-image ^
  --name Ahorcado ^
  --app-version 1.0.0 ^
  --vendor "Mario Borelli" ^
  --copyright "Copyright (c) 1996-2026 Mario Borelli" ^
  --description "Juego de Ahorcado" ^
  --icon "src\main\resources\images\app-icon.ico" ^
  --module-path "target\Ahorcado-1.0.0.jar;target\dependency" ^
  --module "com.llorieb.ahorcado/com.llorieb.ahorcado.Main" ^
  --dest target\portable

if errorlevel 1 exit /b %errorlevel%

echo.
echo ============================================================
echo   PORTABLE CREADO CORRECTAMENTE
echo ============================================================
echo.
echo Ejecutable:
echo   target\portable\Ahorcado\Ahorcado.exe
echo.
echo Probar ese EXE antes de crear el instalador.
exit /b 0
