@echo off
rem Abre uma janela de cliente (GUI). Pode ser executado várias vezes.
cd /d "%~dp0.."
java -jar target\projeto-final.jar client
