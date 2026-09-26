@echo off
setlocal EnableExtensions EnableDelayedExpansion
title Subir reto-developer a GitHub - mxjava

set "OWNER=mxjava"
set "REPO=reto-developer"
set "REMOTE=https://github.com/%OWNER%/%REPO%.git"

if "%~1"=="" (
    set "PROJECT_ROOT=F:\workExam\reto-developer"
) else (
    set "PROJECT_ROOT=%~1"
)

echo ============================================================
echo  SUBIR RETO-DEVELOPER A GITHUB
echo  Cuenta: %OWNER%
echo  Repo  : %REPO%
echo ============================================================
echo.

if not exist "%PROJECT_ROOT%\pom.xml" (
    echo ERROR: No se encontro el proyecto en:
    echo   %PROJECT_ROOT%
    echo.
    echo Uso opcional:
    echo   %~nx0 "RUTA_DEL_PROYECTO"
    exit /b 1
)

cd /d "%PROJECT_ROOT%"

where git >nul 2>&1
if errorlevel 1 (
    echo ERROR: Git no esta instalado o no esta en PATH.
    exit /b 1
)

echo [1/8] Validando calidad Java...
if exist "%PROJECT_ROOT%\scripts\Verify-Quality.ps1" (
    powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\scripts\Verify-Quality.ps1"
    if errorlevel 1 (
        echo.
        echo ERROR: La validacion de calidad fallo. No se subira nada a GitHub.
        exit /b 1
    )
) else (
    echo ADVERTENCIA: No existe scripts\Verify-Quality.ps1. Se continua sin quality gate.
)

echo.
echo [2/8] Inicializando Git...
if not exist ".git" (
    git init
    if errorlevel 1 exit /b 1
)

git config user.name "%OWNER%"

for /f "delims=" %%E in ('git config user.email 2^>nul') do set "GIT_EMAIL=%%E"
if not defined GIT_EMAIL (
    echo.
    set /p "GIT_EMAIL=Escribe el correo asociado a GitHub: "
    if not defined GIT_EMAIL (
        echo ERROR: Debes indicar un correo para crear el commit.
        exit /b 1
    )
    git config user.email "!GIT_EMAIL!"
)

echo.
echo [3/8] Limpiando archivos que NO deben versionarse...
git rm -r --cached --ignore-unmatch docs >nul 2>&1
git rm -r --cached --ignore-unmatch .github >nul 2>&1
git rm --cached --ignore-unmatch SECURITY.md >nul 2>&1
git rm --cached --ignore-unmatch Reto_Developer.pdf >nul 2>&1
git rm --cached --ignore-unmatch .env >nul 2>&1

echo.
echo [4/8] Preparando archivos...
git add .
if errorlevel 1 exit /b 1

echo.
echo Verificando que no se haya agregado documentacion excluida, PDF o secretos...
set "FORBIDDEN="
for /f "delims=" %%F in ('git diff --cached --name-only') do (
    set "FILE=%%F"
    if /I "!FILE!"=="Reto_Developer.pdf" set "FORBIDDEN=1"
    if /I "!FILE!"=="SECURITY.md" set "FORBIDDEN=1"
    if /I "!FILE!"==".env" set "FORBIDDEN=1"
    echo !FILE! | findstr /I /B /C:"docs/" >nul && set "FORBIDDEN=1"
    echo !FILE! | findstr /I /B /C:".github/" >nul && set "FORBIDDEN=1"
    echo !FILE! | findstr /I /C:"/target/" >nul && set "FORBIDDEN=1"
    echo !FILE! | findstr /I /C:"node_modules/" >nul && set "FORBIDDEN=1"
)

if defined FORBIDDEN (
    echo.
    echo ERROR: Se detectaron archivos prohibidos en staging.
    echo Revisa:
    git status --short
    exit /b 1
)

echo.
echo [5/8] Estado que se subira:
git status --short
echo.

git diff --cached --quiet
if not errorlevel 1 (
    echo No hay cambios nuevos para commit.
) else (
    echo Creando commit...
    git commit -m "feat: complete transaction processing challenge"
    if errorlevel 1 exit /b 1
)

echo.
echo [6/8] Configurando rama main y remote...
git branch -M main

git remote get-url origin >nul 2>&1
if errorlevel 1 (
    git remote add origin "%REMOTE%"
) else (
    git remote set-url origin "%REMOTE%"
)

echo.
echo [7/8] Verificando repositorio remoto...
git ls-remote "%REMOTE%" >nul 2>&1
if errorlevel 1 (
    where gh >nul 2>&1
    if not errorlevel 1 (
        echo No existe el repositorio remoto o no es accesible.
        echo Intentando crearlo con GitHub CLI...
        gh auth status >nul 2>&1
        if not errorlevel 1 (
            choice /C PR /N /M "Crear repo Publico [P] o Privado [R]? "
            if errorlevel 2 (
                gh repo create "%OWNER%/%REPO%" --private --source "%PROJECT_ROOT%" --remote origin
            ) else (
                gh repo create "%OWNER%/%REPO%" --public --source "%PROJECT_ROOT%" --remote origin
            )
            if errorlevel 1 (
                echo ERROR: No fue posible crear el repositorio con GitHub CLI.
                exit /b 1
            )
        ) else (
            echo.
            echo GitHub CLI esta instalado pero no autenticado.
            echo Ejecuta: gh auth login
            echo y vuelve a correr este BAT.
            exit /b 1
        )
    ) else (
        echo.
        echo El repositorio %OWNER%/%REPO% aun no existe o no es accesible.
        echo Se abrira GitHub para que lo crees VACIO, sin README, .gitignore ni licencia.
        start "" "https://github.com/new?name=%REPO%"
        echo.
        pause
    )
)

echo.
echo [8/8] Subiendo a GitHub...
git push -u origin main
if errorlevel 1 (
    echo.
    echo ERROR: El push fallo.
    echo Si GitHub pide autenticacion, completa el login y vuelve a ejecutar este BAT.
    exit /b 1
)

echo.
echo ============================================================
echo  LISTO
echo  Repositorio:
echo  https://github.com/%OWNER%/%REPO%
echo ============================================================
echo.
pause
endlocal
