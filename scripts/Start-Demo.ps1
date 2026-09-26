param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$Maven = "F:\Programs\maven\apache-maven-3.9.16\bin\mvn.cmd",
    [string]$JavaHome = "F:\Programs\Java\jdk-21.0.12.1+1",
    [string]$Npm = "F:\Programs\nodejs\npm.cmd"
)

$ErrorActionPreference = "Stop"

function New-RandomBase64([int]$Size) {
    $bytes = New-Object byte[] $Size
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    [Convert]::ToBase64String($bytes)
}

function Test-Port([int]$Port) {
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Copy-VerifiedJar {
    param(
        [string]$Source,
        [string]$Destination,
        [string]$Component
    )

    if (-not (Test-Path $Source -PathType Leaf)) {
        throw "No existe el JAR de ${Component}: $Source"
    }

    $sourceInfo = Get-Item $Source
    if ($sourceInfo.Length -lt 1MB) {
        throw "El JAR de $Component es demasiado pequeno ($($sourceInfo.Length) bytes). " +
              "Puede estar incompleto o haber sido alterado por sincronizacion."
    }

    Copy-Item $Source $Destination -Force

    $sourceHash = (Get-FileHash $Source -Algorithm SHA256).Hash
    $destinationHash = (Get-FileHash $Destination -Algorithm SHA256).Hash

    if ($sourceHash -ne $destinationHash) {
        throw "La copia de runtime de $Component no coincide con el JAR generado."
    }

    Write-Host "[OK] $Component copiado a runtime local seguro." -ForegroundColor Green
}

function Wait-ForPort {
    param(
        [int]$Port,
        [int]$TimeoutSeconds = 60,
        [string]$Component = "Aplicacion"
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $Port) {
            Write-Host "[OK] $Component escuchando en puerto $Port." -ForegroundColor Green
            return
        }
        Start-Sleep -Seconds 1
    }

    throw "$Component no abrio el puerto $Port en $TimeoutSeconds segundos. Revisa su ventana de consola."
}

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
if (-not (Test-Path $Maven)) { throw "No se encontro Maven 3.9+: $Maven" }
if (-not (Test-Path $JavaHome)) { throw "No se encontro Java 21: $JavaHome" }
if (-not (Test-Path $Npm)) { throw "No se encontro npm.cmd: $Npm" }

$env:JAVA_HOME = $JavaHome
$env:MAVEN_HOME = Split-Path (Split-Path $Maven -Parent) -Parent
$env:Path = "$env:JAVA_HOME\bin;$(Split-Path $Maven -Parent);$(Split-Path $Npm -Parent);$env:Path"

foreach ($port in 8082,8081,5173,9092) {
    if (Test-Port $port) {
        throw "El puerto $port ya esta en uso. Cierra la instancia anterior de la demo antes de continuar."
    }
}

$env:SECURITY_JWT_SECRET_BASE64 = New-RandomBase64 32
$env:APP_AES_KEY_BASE64 = New-RandomBase64 32
$env:H2_DB_USERNAME = "sa"
$env:H2_DB_PASSWORD = New-RandomBase64 24
$env:APP_H2_CONSOLE_ENABLED = "true"
$env:APP_H2_TCP_ENABLED = "true"
$env:APP_H2_TCP_PORT = "9092"
$env:APP_BOOTSTRAP_USERNAME = "admin"

# Siempre pedir la contraseña del usuario demo en cada arranque.
# Así no se reutiliza por accidente un APP_BOOTSTRAP_PASSWORD antiguo de la sesión PowerShell.
Remove-Item Env:APP_BOOTSTRAP_PASSWORD -ErrorAction SilentlyContinue
$secure = Read-Host "Password temporal para admin (minimo 12 caracteres)" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $env:APP_BOOTSTRAP_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
}
finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}

if ([string]::IsNullOrWhiteSpace($env:APP_BOOTSTRAP_PASSWORD) -or $env:APP_BOOTSTRAP_PASSWORD.Length -lt 12) {
    throw "El password temporal de admin debe contener al menos 12 caracteres."
}

$env:APP_CORS_ALLOWED_ORIGINS = "http://localhost:5173"
$env:TRANSACTION_SERVICE_URL = "http://localhost:8082"
$env:SECURITY_JWT_ISSUER = "reto-developer-local"
$env:SECURITY_JWT_AUDIENCE = "transaction-api"
$env:SECURITY_JWT_TTL_MINUTES = "15"
$env:VITE_API_URL = "http://localhost:8081"
$env:VITE_AES_KEY_BASE64 = $env:APP_AES_KEY_BASE64

Write-Host "[0/7] Limpiando duplicados de sincronizacion..."
Remove-StaleSyncDuplicates -Root $ProjectRoot

Write-Host "[1/7] Aplicando formato Java estandar..."
& $Maven -f (Join-Path $ProjectRoot "pom.xml") -DskipTests spotless:apply
if ($LASTEXITCODE -ne 0) { throw "El formateo Java fallo. No se iniciara la demo." }

Write-Host "[2/7] Validando build backend..."
& $Maven -f (Join-Path $ProjectRoot "pom.xml") package
if ($LASTEXITCODE -ne 0) { throw "El build backend fallo. No se iniciara la demo." }

Write-Host "[3/7] Validando build frontend..."
$frontRoot = Join-Path $ProjectRoot "transaction-front"
$viteCmd = Join-Path $frontRoot "node_modules\.bin\vite.cmd"

Push-Location $frontRoot
try {
    if (-not (Test-Path $viteCmd)) {
        Write-Host "Dependencias frontend incompletas. Ejecutando npm install..." -ForegroundColor Yellow
        & $Npm install --no-fund
        if ($LASTEXITCODE -ne 0) { throw "npm install fallo." }
    }

    if (-not (Test-Path $viteCmd)) {
        throw "Vite no fue instalado en node_modules\.bin. Revisa npm install y package.json."
    }

    & $Npm run build
    if ($LASTEXITCODE -ne 0) { throw "npm run build fallo." }
}
finally { Pop-Location }

$serviceJarSource = Join-Path $ProjectRoot "transaction-service-api\target\transaction-service-api-1.1.0-SNAPSHOT.jar"
$gatewayJarSource = Join-Path $ProjectRoot "transaction-gateway-api\target\transaction-gateway-api-1.1.0-SNAPSHOT.jar"

# No ejecutar los fat JAR directamente desde una carpeta sincronizada.
# Spring Boot carga dependencias anidadas de forma diferida; si Drive reemplaza el
# archivo mientras el proceso esta vivo pueden aparecer NoClassDefFoundError.
$runtimeBase = Join-Path $env:LOCALAPPDATA "RetoDeveloper\runtime"
$runtimeRoot = Join-Path $runtimeBase ([Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null

$serviceJar = Join-Path $runtimeRoot "transaction-service-api.jar"
$gatewayJar = Join-Path $runtimeRoot "transaction-gateway-api.jar"

Write-Host "[4/7] Preparando runtime local fuera de sincronizacion..."
Copy-VerifiedJar -Source $serviceJarSource -Destination $serviceJar -Component "Transaction Service"
Copy-VerifiedJar -Source $gatewayJarSource -Destination $gatewayJar -Component "Transaction Gateway"

$javaExe = Join-Path $JavaHome "bin\java.exe"
$serviceCmd = "Set-Location '$runtimeRoot'; & '$javaExe' -jar '$serviceJar'"
$gatewayCmd = "Set-Location '$runtimeRoot'; & '$javaExe' -jar '$gatewayJar'"
$frontCmd = "Set-Location '$frontRoot'; & '$Npm' run dev"

Write-Host "[5/7] Iniciando Transaction Service :8082..."
Start-Process powershell.exe -ArgumentList "-NoExit","-Command",$serviceCmd
Wait-ForPort -Port 8082 -TimeoutSeconds 60 -Component "Transaction Service"
Wait-ForPort -Port 9092 -TimeoutSeconds 30 -Component "H2 TCP Server"

# El Service ya heredó el password; evitar que quede persistido en esta sesión del launcher.
Remove-Item Env:APP_BOOTSTRAP_PASSWORD -ErrorAction SilentlyContinue

Write-Host "[6/7] Iniciando Transaction Gateway :8081..."
Start-Process powershell.exe -ArgumentList "-NoExit","-Command",$gatewayCmd
Wait-ForPort -Port 8081 -TimeoutSeconds 60 -Component "Transaction Gateway"

Write-Host "[7/7] Iniciando Frontend :5173..."
Start-Process powershell.exe -ArgumentList "-NoExit","-Command",$frontCmd
Wait-ForPort -Port 5173 -TimeoutSeconds 30 -Component "Frontend"

Write-Host ""
Write-Host "Demo iniciada correctamente:" -ForegroundColor Green
Write-Host "  Frontend   : http://localhost:5173"
Write-Host "  Gateway    : http://localhost:8081"
Write-Host "  Service    : http://localhost:8082"
Write-Host "  H2 Console : http://localhost:8082/h2-console"
Write-Host "  H2 JDBC    : jdbc:h2:tcp://localhost:9092/mem:transactionsdb"
Write-Host "  H2 Usuario : $env:H2_DB_USERNAME"
Write-Host "  H2 Password: $env:H2_DB_PASSWORD"
Write-Host "  Login      : admin + password capturado al inicio de este arranque"
Write-Host "  Runtime JAR: $runtimeRoot"
Write-Host ""
Write-Host "La contraseña de admin no se imprime ni se persiste. El password H2 es efimero y cambia en cada arranque."
