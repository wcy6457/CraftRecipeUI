param(
    [Parameter(Mandatory = $true)]
    [string] $ProjectJar,

    [Parameter(Mandatory = $true)]
    [string] $ServerDir,

    [string] $PluginFileName = "",

    [int] $StopTimeoutSeconds = 25,

    [switch] $VisibleConsole
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-ExistingPath {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Name
    )

    if (-not (Test-Path -LiteralPath $Path)) {
        throw "$Name does not exist: $Path"
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

$serverPath = Resolve-ExistingPath -Path $ServerDir -Name "Server directory"
$jarPath = Resolve-ExistingPath -Path $ProjectJar -Name "Project jar"

if ([string]::IsNullOrWhiteSpace($PluginFileName)) {
    $PluginFileName = Split-Path -Path $jarPath -Leaf
}

$pluginsPath = Join-Path -Path $serverPath -ChildPath "plugins"
New-Item -ItemType Directory -Path $pluginsPath -Force | Out-Null

$serverJar = Get-ChildItem -LiteralPath $serverPath -Filter "paper-*.jar" -File |
    Sort-Object -Property LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $serverJar) {
    throw "No paper-*.jar file found in $serverPath"
}

function Get-TestServerProcess {
    $serverJarName = $serverJar.Name
    $serverJarPath = [regex]::Escape($serverJar.FullName)

    Get-CimInstance Win32_Process |
        Where-Object {
            ($_.Name -eq "java.exe" -or $_.Name -eq "javaw.exe") -and
            $_.CommandLine -and
            ($_.CommandLine -match $serverJarPath -or $_.CommandLine -like "*$serverJarName*")
        }
}

$runningServers = @(Get-TestServerProcess)
if ($runningServers.Count -gt 0) {
    foreach ($process in $runningServers) {
        Write-Host "Stopping test server process $($process.ProcessId)..."
        & taskkill.exe /PID $process.ProcessId | Out-Null
    }

    $deadline = (Get-Date).AddSeconds($StopTimeoutSeconds)
    do {
        Start-Sleep -Milliseconds 500
        $runningServers = @(Get-TestServerProcess)
    } while ($runningServers.Count -gt 0 -and (Get-Date) -lt $deadline)

    foreach ($process in $runningServers) {
        Write-Host "Force stopping test server process $($process.ProcessId)..."
        & taskkill.exe /F /PID $process.ProcessId | Out-Null
    }
} else {
    Write-Host "No running test server process found."
}

Get-ChildItem -LiteralPath $pluginsPath -Filter "RecipeSmith*.jar" -File |
    Remove-Item -Force

$destinationJar = Join-Path -Path $pluginsPath -ChildPath $PluginFileName
Copy-Item -LiteralPath $jarPath -Destination $destinationJar -Force
Write-Host "Installed plugin to $destinationJar"

$startScript = Join-Path -Path $serverPath -ChildPath "start.bat"
$javaExe = "java.exe"
$javaArgs = "-Xmx4G -Xms4G -jar `"$($serverJar.FullName)`""

if (Test-Path -LiteralPath $startScript) {
    $startLine = Get-Content -LiteralPath $startScript |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        Select-Object -First 1

    if ($startLine -match '^\s*"(?<java>[^"]*java\.exe)"\s+(?<args>.*)$') {
        $javaExe = $Matches["java"]
        $javaArgs = $Matches["args"]
    } elseif ($startLine -match '^\s*(?<java>\S*java\.exe)\s+(?<args>.*)$') {
        $javaExe = $Matches["java"]
        $javaArgs = $Matches["args"]
    }
}

if (-not (Test-Path -LiteralPath $javaExe) -and $javaExe -ne "java.exe") {
    throw "Java executable from start.bat does not exist: $javaExe"
}

if ($javaArgs -match '(?i)-jar\s+') {
    $javaArgs = $javaArgs -replace '(?i)-jar\s+(?:"[^"]+"|\S+)', "-jar `"$($serverJar.FullName)`""
}

$windowStyle = "Hidden"
if ($VisibleConsole) {
    $windowStyle = "Normal"
}

Start-Process -FilePath $javaExe -ArgumentList $javaArgs -WorkingDirectory $serverPath -WindowStyle $windowStyle
Write-Host "Started test server with $($serverJar.Name)"
