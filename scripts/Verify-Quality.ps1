param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$Maven = "F:\Programs\maven\apache-maven-3.9.16\bin\mvn.cmd",
    [string]$JavaHome = "F:\Programs\Java\jdk-21.0.12.1+1"
)

$ErrorActionPreference = "Stop"

function Remove-StaleSyncDuplicates {
    param([string]$Root)

    $scanRoots = @(
        (Join-Path $Root "security-common\src\main\java"),
        (Join-Path $Root "transaction-service-api\src\main\java"),
        (Join-Path $Root "transaction-gateway-api\src\main\java"),
        (Join-Path $Root "security-common\target\classes"),
        (Join-Path $Root "transaction-service-api\target\classes"),
        (Join-Path $Root "transaction-gateway-api\target\classes")
    )

    foreach ($scanRoot in $scanRoots) {
        if (-not (Test-Path $scanRoot)) {
            continue
        }

        $duplicateDirectories = Get-ChildItem $scanRoot -Recurse -Directory -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^(?<base>.+?)\s*\(\d+\)$' } |
            Sort-Object { $_.FullName.Length } -Descending

        foreach ($duplicate in $duplicateDirectories) {
            if ($duplicate.Name -notmatch '^(?<base>.+?)\s*\(\d+\)$') {
                continue
            }

            $canonicalName = $Matches['base'].TrimEnd()
            $canonicalPath = Join-Path $duplicate.Parent.FullName $canonicalName

            if (Test-Path $canonicalPath -PathType Container) {
                Write-Host "Eliminando duplicado de sincronizacion: $($duplicate.FullName)" -ForegroundColor Yellow
                Remove-Item $duplicate.FullName -Recurse -Force -ErrorAction Stop
            }
        }

        $duplicateFiles = Get-ChildItem $scanRoot -Recurse -File -Force -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^(?<base>.+?)\s*\(\d+\)(?<extension>\.[^.]+)$' }

        foreach ($duplicate in $duplicateFiles) {
            if ($duplicate.Name -notmatch '^(?<base>.+?)\s*\(\d+\)(?<extension>\.[^.]+)$') {
                continue
            }

            $canonicalName = $Matches['base'].TrimEnd() + $Matches['extension']
            $canonicalPath = Join-Path $duplicate.Directory.FullName $canonicalName

            if (Test-Path $canonicalPath -PathType Leaf) {
                Write-Host "Eliminando archivo duplicado de sincronizacion: $($duplicate.FullName)" -ForegroundColor Yellow
                Remove-Item $duplicate.FullName -Force -ErrorAction Stop
            }
        }
    }
}

if (-not (Test-Path $Maven)) {
    throw "No se encontro Maven 3.9+: $Maven"
}

if (-not (Test-Path $JavaHome)) {
    throw "No se encontro Java 21: $JavaHome"
}

$env:JAVA_HOME = $JavaHome
$env:MAVEN_HOME = Split-Path (Split-Path $Maven -Parent) -Parent
$env:Path = "$env:JAVA_HOME\bin;$(Split-Path $Maven -Parent);$env:Path"

$pom = Join-Path $ProjectRoot "pom.xml"

Write-Host "[0/3] Limpiando duplicados de sincronizacion..." -ForegroundColor Cyan
Remove-StaleSyncDuplicates -Root $ProjectRoot

Write-Host "[1/3] Aplicando formato Java estandar..." -ForegroundColor Cyan
& $Maven -f $pom -DskipTests spotless:apply
if ($LASTEXITCODE -ne 0) {
    throw "No fue posible aplicar el formato Java configurado."
}

Write-Host "[2/3] Verificando formato Java..." -ForegroundColor Cyan
& $Maven -f $pom -DskipTests spotless:check
if ($LASTEXITCODE -ne 0) {
    throw "El codigo Java sigue presentando violaciones de formato despues de spotless:apply."
}

Write-Host "[3/3] Ejecutando analisis estatico PMD..." -ForegroundColor Cyan
& $Maven -f $pom -DskipTests -Pquality verify
if ($LASTEXITCODE -ne 0) {
    throw "La validacion de calidad o el build fallo. Revisa el error Maven inmediatamente anterior."
}

Write-Host ""
Write-Host "Calidad Java validada correctamente." -ForegroundColor Green
Write-Host "  - Formato e indentacion: Spotless + google-java-format (AOSP, 4 espacios)"
Write-Host "  - Imports no usados: eliminados/verificados por Spotless"
Write-Host "  - Analisis estatico: PMD error-prone + best-practices"
Write-Host "  - Java: 21"
