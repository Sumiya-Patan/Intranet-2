@echo off
setlocal enabledelayedexpansion

REM Load .env variables from file
for /f "usebackq tokens=* delims=" %%a in (".env") do (
    set "line=%%a"
    if defined line if not "!line:~0,1!"=="#" (
        for /f "tokens=1,* delims==" %%b in ("!line!") do set "%%b=%%c"
    )
)

REM Start Spring Boot using system Maven
mvnw spring-boot:run
pause
