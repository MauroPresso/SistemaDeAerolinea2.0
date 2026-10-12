@echo off
cd /d "%~dp0"
if exist "dist\SistemaDeAerolinea2.0.jar" (
  java -jar "dist\SistemaDeAerolinea2.0.jar"
) else (
  java -jar "target\SistemaDeAerolinea2.0.jar"
)
