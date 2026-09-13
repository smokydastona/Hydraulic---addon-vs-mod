<#
    Deploys generated Hydraulic .mcpack files to the production Bedrock resource pack directory
    (e.g. Geyser packs directory or custom distribution folder) and generates a verified
    pack distribution manifest.
#>
param(
    [string]$TargetDirectory = ""
)

Write-Host "================================================================"
Write-Host " HYDRAULIC PRODUCTION PACK DEPLOYMENT & PACKAGING"
Write-Host "================================================================"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Push-Location $repoRoot

try {
    $storageDir = Join-Path $repoRoot "fabric\run\config\hydraulic\storage"
    if (-not (Test-Path $storageDir)) {
        Write-Error "Storage directory not found: $storageDir. Run the server or pack generator first."
        exit 1
    }

    if ([string]::IsNullOrWhiteSpace($TargetDirectory)) {
        $TargetDirectory = Join-Path $repoRoot "fabric\run\config\Geyser-Fabric\packs"
    }

    if (-not (Test-Path $TargetDirectory)) {
        New-Item -ItemType Directory -Force -Path $TargetDirectory | Out-Null
    }

    Write-Host "Scanning storage directory: $storageDir"
    Write-Host "Deploying to destination:    $TargetDirectory"

    $mcpacks = Get-ChildItem -Path $storageDir -Filter "*.mcpack" -Recurse
    if ($mcpacks.Count -eq 0) {
        Write-Warning "No .mcpack files found in $storageDir."
        exit 0
    }

    $deployedList = @()
    $totalBytes = 0

    foreach ($pack in $mcpacks) {
        $destPath = Join-Path $TargetDirectory $pack.Name
        Copy-Item -Path $pack.FullName -Destination $destPath -Force

        $hash = (Get-FileHash -Path $destPath -Algorithm SHA256).Hash
        $size = (Get-Item $destPath).Length
        $totalBytes += $size

        $modName = $pack.Directory.Name
        Write-Host "  [+] Deployed $($pack.Name) ($size bytes) [SHA256: $($hash.Substring(0,16))...]" -ForegroundColor Green

        $deployedList += [PSCustomObject]@{
            ModId        = $modName
            FileName     = $pack.Name
            SizeBytes    = $size
            Sha256       = $hash
            DeployedPath = $destPath
        }
    }

    # Write distribution manifest JSON
    $manifest = [PSCustomObject]@{
        schemaVersion       = "1.0.0"
        timestamp           = (Get-Date).ToString("o")
        totalPacks          = $deployedList.Count
        totalSizeBytes      = $totalBytes
        targetBedrockFormat = "1.21"
        packs               = $deployedList
    }

    $manifestPath = Join-Path $TargetDirectory "pack-distribution-manifest.json"
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -Path $manifestPath -Encoding UTF8
    Write-Host "Generated distribution manifest: $manifestPath" -ForegroundColor Cyan

    Write-Host "================================================================"
    Write-Host " PRODUCTION DEPLOYMENT COMPLETE: $($deployedList.Count) pack(s) ($totalBytes bytes) deployed"
    Write-Host "================================================================"
}
finally {
    Pop-Location
}
