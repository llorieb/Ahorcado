@echo off
setlocal
cd /d "%~dp0"

echo ============================================================
echo   AHORCADO 1.0.0 - BUILD WINDOWS COMPLETO
echo ============================================================

echo.
call 01-crear-portable.bat
if errorlevel 1 exit /b %errorlevel%

echo.
call 02-crear-instalador.bat
if errorlevel 1 exit /b %errorlevel%

echo.
echo ============================================================
echo   BUILD FINALIZADO
echo ============================================================
echo.
echo Portable:
echo   target\portable\Ahorcado\Ahorcado.exe
echo.
echo Instalador:
echo   target\installer\Ahorcado-1.0.0.exe
echo   ^(si jpackage agrega otro sufijo, usar el unico .exe de esa carpeta^)
echo.
exit /b 0
