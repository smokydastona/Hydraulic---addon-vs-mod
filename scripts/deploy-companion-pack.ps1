<#
.SYNOPSIS
    Builds the Phlodgate Add-On (Bedrock companion) and installs it as a Hydraulic
    companion package so it is delivered automatically to Bedrock/Geyser sessions.

.DESCRIPTION
    This script:
      1. Runs `npm run package` inside the Plodgate_Add-on project (builds scripts/
         then packages BP/ and RP/ into .mcpack/.mcaddon archives under dist/).
      2. Mirrors the add-on's RP/ directory into
         <CompanionsRoot>/<CompanionId>/resource_pack, and its BP/ directory into
         <CompanionsRoot>/<CompanionId>/behavior_pack (documentation/capability
         reporting only - it is never sent to the Bedrock client by Geyser).
      3. Writes a companion.json descriptor next to them, declaring this companion's
         real capabilities. Capabilities that need authoritative Java-side state
         are marked "requiresServerBridge": true; only "companion_detection_signal"
         has an implemented Hydraulic bridge (a Bedrock-visible scoreboard objective).

    Geyser cannot install or execute Bedrock behavior-pack scripts on a connecting
    client; it only supports pushing resource packs to a session. The companion's
    behavior pack must be enabled manually by the player as a Bedrock "Global
    Resource" to run at all. This script never modifies the player's client.

.EXAMPLE
    .\deploy-companion-pack.ps1

.EXAMPLE
    .\deploy-companion-pack.ps1 -CompanionsRoot 'C:\bds\config\hydraulic\companions' -DryRun
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter()]
    [string]$AddonRoot = (Join-Path $PSScriptRoot "..\..\Plodgate_Add-on"),

    [Parameter()]
    [string]$CompanionsRoot = (Join-Path $PSScriptRoot "..\fabric\run\config\hydraulic\companions"),

    [Parameter()]
    [string]$CompanionId = "phlodgate",

    [Parameter()]
    [switch]$SkipBuild,

    [Parameter()]
    [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-ExistingDirectory([string]$Path, [string]$Name) {
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw "$Name does not exist or is not a directory: $Path"
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

$addonRoot = Resolve-ExistingDirectory $AddonRoot "AddonRoot"
$rpSource = Join-Path $addonRoot "RP"
$bpSource = Join-Path $addonRoot "BP"
Resolve-ExistingDirectory $rpSource "Resource pack source (RP)" | Out-Null
Resolve-ExistingDirectory $bpSource "Behavior pack source (BP)" | Out-Null

$packageJsonPath = Join-Path $addonRoot "package.json"
$packageJson = Get-Content -LiteralPath $packageJsonPath -Raw | ConvertFrom-Json
$version = [string]$packageJson.version

if (-not $SkipBuild) {
    Write-Host "Building Phlodgate Add-On v$version ..."
    Push-Location $addonRoot
    try {
        if (-not (Test-Path -LiteralPath (Join-Path $addonRoot "node_modules"))) {
            & npm install
            if ($LASTEXITCODE -ne 0) { throw "npm install failed with exit code $LASTEXITCODE" }
        }

        & npm run package
        if ($LASTEXITCODE -ne 0) { throw "npm run package failed with exit code $LASTEXITCODE" }
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "Skipping build (-SkipBuild); reusing existing BP/ and RP/ contents on disk."
}

$destinationRoot = if (Test-Path -LiteralPath $CompanionsRoot -PathType Container) {
    (Resolve-Path -LiteralPath $CompanionsRoot).Path
} elseif ($DryRun) {
    [System.IO.Path]::GetFullPath($CompanionsRoot)
} else {
    New-Item -ItemType Directory -Path $CompanionsRoot -Force | Out-Null
    (Resolve-Path -LiteralPath $CompanionsRoot).Path
}

$companionDestination = Join-Path $destinationRoot $CompanionId
$rpDestination = Join-Path $companionDestination "resource_pack"
$bpDestination = Join-Path $companionDestination "behavior_pack"
$manifestDestination = Join-Path $companionDestination "companion.json"

# These capabilities reflect what the add-on's src/features actually implement.
# Everything here reads standard Bedrock world/entity/inventory/item state that
# Geyser already translates from the Java server; only the companion-detection
# signal genuinely needs Java-side involvement, and Hydraulic implements that one
# real bridge as a scoreboard objective (see CompanionSignalBridge.java).
$capabilities = @(
    [ordered]@{ id = "companion_detection_signal"; description = "Detects a live Hydraulic-Phlodgate server via a scoreboard objective, world tags, and modded item/entity namespaces already visible through Geyser."; requiresServerBridge = $true }
    [ordered]@{ id = "jei_inventory_search"; description = "JEI-style inventory/recipe search UI."; requiresServerBridge = $false }
    [ordered]@{ id = "waypoints"; description = "Player-managed waypoints stored in client dynamic properties."; requiresServerBridge = $false }
    [ordered]@{ id = "food_hud"; description = "AppleSkin-style food/saturation HUD read from standard player state."; requiresServerBridge = $false }
    [ordered]@{ id = "durability_hud"; description = "Held/worn item durability HUD read from standard item component state."; requiresServerBridge = $false }
    [ordered]@{ id = "machine_inspector"; description = "Inspects modded block/item type IDs and namespaces already present in standard Bedrock protocol state."; requiresServerBridge = $false }
    [ordered]@{ id = "optimization_engine"; description = "Client-side entity/particle/fog optimization heuristics."; requiresServerBridge = $false }
    [ordered]@{ id = "hydraulic_control_room"; description = "Control-room UI surfacing companion bridge status and quick actions."; requiresServerBridge = $false }
)

$manifest = [ordered]@{
    id = $CompanionId
    name = "Phlodgate Add-On"
    version = $version
    resourcePack = "resource_pack"
    behaviorPack = "behavior_pack"
    executionMode = "client_global_behavior_pack"
    capabilities = $capabilities
}

if ($DryRun) {
    Write-Host "Dry run: would install companion '$CompanionId' v$version to $companionDestination"
    Write-Host ($manifest | ConvertTo-Json -Depth 6)
    return
}

New-Item -ItemType Directory -Path $companionDestination -Force | Out-Null

if (Test-Path -LiteralPath $rpDestination) { Remove-Item -LiteralPath $rpDestination -Recurse -Force }
if (Test-Path -LiteralPath $bpDestination) { Remove-Item -LiteralPath $bpDestination -Recurse -Force }

Copy-Item -LiteralPath $rpSource -Destination $rpDestination -Recurse -Force
Copy-Item -LiteralPath $bpSource -Destination $bpDestination -Recurse -Force

$manifest | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $manifestDestination -Encoding UTF8

Write-Host "Installed companion '$CompanionId' v$version to $companionDestination" -ForegroundColor Green
Write-Host "Resource pack will be delivered automatically to Bedrock/Geyser sessions on next server start."
Write-Host "Behavior pack scripts will NOT be executed by Geyser; enable BP/ manually as a Bedrock Global Resource to use them."
