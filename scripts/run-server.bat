@echo off
rem Sobe o servidor (broker MOM + RMI). Execute a partir da pasta do projeto.
cd /d "%~dp0.."
java -jar target\projeto-final.jar server
