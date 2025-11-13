@echo off
REM queuectl - Background Job Queue System
REM Windows batch script wrapper

java -jar "%~dp0target\queuectl.jar" %*
