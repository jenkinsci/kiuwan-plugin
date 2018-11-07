@echo off
if NOT ""%1""=="""" set _PARAMS=%*
java -jar .\testWindowsEscape.jar %_PARAMS%
set javaErrorLevel=%ERRORLEVEL%
exit /b %javaErrorLevel%
