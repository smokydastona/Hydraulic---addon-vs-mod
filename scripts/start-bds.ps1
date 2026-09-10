<#
    Starts the optional Bedrock Dedicated Server (Test B) if a local binary has been placed under
    dev/bedrock/bedrock_server.exe. Never fails the build when the binary is absent, since BDS is
    optional/best-effort and unrelated to validating Phlodgate itself (Test A).
#>
param()

$ErrorActionPreference = "Stop"

$devBedrockDir = Join-Path $PSScriptRoot "..\dev\bedrock"
$exePath = Join-Path $devBedrockDir "bedrock_server.exe"

if (-not (Test-Path $exePath)) {
    Write-Host "Bedrock Dedicated Server not found at dev/bedrock/bedrock_server.exe."
    Write-Host "Download it from https://www.minecraft.net/en-us/download/server/bedrock (accept the EULA),"
    Write-Host "extract it into dev/bedrock/, then re-run this task to enable Test B."
    exit 0
}

$propsPath = Join-Path $devBedrockDir "server.properties"

if (-not (Test-Path $propsPath)) {
    @(
        "server-name=Phlodgate Bedrock BDS Dev"
        "server-port=19133"
        "server-portv6=19134"
        "level-name=Bedrock BDS Dev"
        "gamemode=survival"
        "difficulty=easy"
        "allow-cheats=true"
        "max-players=10"
        "online-mode=false"
    ) | Set-Content -Path $propsPath -Encoding ASCII
    Write-Host "Wrote default dev/bedrock/server.properties pinned to port 19133 (Geyser/Phlodgate keeps 19132)."
} else {
    $existing = Get-Content $propsPath -Raw
    if ($existing -notmatch "(?m)^server-port=19133\s*$") {
        Write-Warning "dev/bedrock/server.properties does not pin server-port=19133; it may collide with Geyser's 19132."
    }
}

Write-Host "Starting Bedrock Dedicated Server (Test B - independent of Phlodgate validation) on port 19133..."
Push-Location $devBedrockDir
try {
    & $exePath
} finally {
    Pop-Location
}
