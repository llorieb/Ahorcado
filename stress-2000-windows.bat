@echo off
setlocal
cd /d "%~dp0"
echo Ejecutando prueba manual de 2000 partidas...
call mvnw.cmd -Dahorcado.stress=true -Dtest=PartidasStressTest test
set EXITCODE=%ERRORLEVEL%
echo.
if %EXITCODE% EQU 0 (
  echo Prueba finalizada correctamente.
) else (
  echo La prueba termino con codigo %EXITCODE%.
)
pause
exit /b %EXITCODE%
