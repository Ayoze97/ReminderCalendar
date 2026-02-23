@echo off
setlocal enabledelayedexpansion

if "%~1"=="" (
  echo Debes proporcionar un mensaje de commit
  exit /b
)

if "%~2"=="" (
  echo Debes proporcionar una version para la tag
  exit /b
)

set "COMMIT_MSG=%~1"
set "VERSION_TAG=%~2"

git add .
git commit -m "%COMMIT_MSG%"
git push
git tag "%VERSION_TAG%"
git push origin "%VERSION_TAG%"

echo Proceso completado

REM release.bat "feat: Added: Reminder by configurable message and minor fixes" v0.7.0