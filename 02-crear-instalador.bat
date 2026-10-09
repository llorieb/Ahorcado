@echo off
setlocal
cd /d "%~dp0"

call 00-verificar-requisitos.bat
if errorlevel 1 exit /b %errorlevel%

if not exist target\portable\Ahorcado\Ahorcado.exe (
  echo [INFO] No existe todavia la imagen portable. Se creara primero.
  call 01-crear-portable.bat
  if errorlevel 1 exit /b %errorlevel%
)

if exist target\installer rmdir /s /q target\installer
mkdir target\installer

echo.
echo ============================================================
echo   Creando instalador Windows Ahorcado 1.0.2
echo ============================================================

jpackage ^
  --type exe ^
  --name Ahorcado ^
  --app-version 1.0.2 ^
  --vendor "Mario Borelli" ^
  --copyright "Copyright (c) 1996-2026 Mario Borelli" ^
  --description "Juego de Ahorcado" ^
  --icon "src\main\resources\images\app-icon.ico" ^
  --app-image "target\portable\Ahorcado" ^
  --dest target\installer ^
  --win-menu ^
  --win-menu-group "Ahorcado" ^
  --win-shortcut ^
  --win-dir-chooser ^
  --win-upgrade-uuid "FEA50566-24F7-55D5-85B8-23BD83E4DB4A"

if errorlevel 1 exit /b %errorlevel%

echo.
echo ============================================================
echo   INSTALADOR CREADO CORRECTAMENTE
echo ============================================================
echo.
echo Buscar el archivo .exe en:
echo   target\installer
exit /b 0
