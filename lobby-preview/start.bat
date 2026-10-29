@echo off
rem Serves the lobby preview on http://localhost:8080 (needs Python 3 or Node.js)
cd /d "%~dp0"
set PORT=%1
if "%PORT%"=="" set PORT=8080
echo Banner Camp lobby preview -^> http://localhost:%PORT%
start "" http://localhost:%PORT%
where python >nul 2>nul && (python -m http.server %PORT% & goto :eof)
where py >nul 2>nul && (py -3 -m http.server %PORT% & goto :eof)
npx --yes http-server -p %PORT% -c-1 .
