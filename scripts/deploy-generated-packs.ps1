<#
.SYNOPSIS
    Deploys validated Hydraulic-generated .mcpack files into a production Bedrock pack directory.

.DESCRIPTION
    The source directory is expected to contain .mcpack files produced by PackManager, normally
    fabric/run/config/hydraulic/storage. Every archive is validated as a ZIP and must contain a
    readable manifest.json before it is staged and promoted. Existing destination files are preserved
    unless -Force is explicitly supplied. Promotion uses same-volume Move-Item operations so a staged
    file is never exposed as a partially copied pack.

    This script never downloads content, deletes unrelated files, or modifies Bedrock worlds.

.EXAMPLE
    .\deploy-generated-packs.ps1 -DestinationRoot 'C:\Geyser\bedrock\resource_packs' -DryRun

.EXAMPLE
    .\deploy-generated-packs.ps1 -DestinationRoot 'C:\Geyser\bedrock\resource_packs' -Force
#>
[CmdletBinding(SupportsShouldProcess)]
param(
    [Parameter()]
    [string]$SourceRoot = (Join-Path $PSScriptRoot "..\fabric\run\config\hydraulic\storage"),

    [Parameter(Mandatory = $true)]
    [string]$DestinationRoot,

    [Parameter()]
    [switch]$DryRun,

    [Parameter()]
    [switch]$Force
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-ExistingDirectory([string]$Path, [string]$Name) {
    if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
        throw "$Name does not exist or is not a directory: $Path"
    }
    return (Resolve-Path -LiteralPath $Path).Path
}

function Read-PackManifest([string]$PackPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = $null
    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($PackPath)
        $manifestEntry = $archive.GetEntry("manifest.json")
        if ($null -eq $manifestEntry) {
            throw "manifest.json is missing"
        }
        $reader = New-Object System.IO.StreamReader($manifestEntry.Open())
        try {
            $manifest = $reader.ReadToEnd() | ConvertFrom-Json
        }
        finally {
            $reader.Dispose()
        }
        if ($null -eq $manifest.header -or [string]::IsNullOrWhiteSpace([string]$manifest.header.name)) {
            throw "manifest.header.name is missing"
        }
        return $manifest
    }
    catch {
        throw "Invalid .mcpack '$PackPath': $($_.Exception.Message)"
    }
    finally {
        if ($null -ne $archive) {
            $archive.Dispose()
        }
    }
}

$source = Resolve-ExistingDirectory $SourceRoot "SourceRoot"
$destination = if (Test-Path -LiteralPath $DestinationRoot -PathType Container) {
    (Resolve-Path -LiteralPath $DestinationRoot).Path
} else {
    if ($DryRun) {
        [System.IO.Path]::GetFullPath($DestinationRoot)
    } else {
        New-Item -ItemType Directory -Path $DestinationRoot -Force | Out-Null
        (Resolve-Path -LiteralPath $DestinationRoot).Path
    }
}

$packs = @(Get-ChildItem -LiteralPath $source -Filter "*.mcpack" -File -Recurse | Sort-Object FullName)
if ($packs.Count -eq 0) {
    throw "No generated .mcpack files were found under $source"
}

$deployment = [System.Collections.Generic.List[object]]::new()
$staging = Join-Path $destination (".hydraulic-staging-" + [Guid]::NewGuid().ToString("N"))

try {
    foreach ($pack in $packs) {
        $manifest = Read-PackManifest $pack.FullName
        $hash = (Get-FileHash -LiteralPath $pack.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        $target = Join-Path $destination $pack.Name
        $record = [ordered]@{
            file = $pack.Name
            source = $pack.FullName
            destination = $target
            sha256 = $hash
            manifestName = [string]$manifest.header.name
            manifestUuid = [string]$manifest.header.uuid
            bytes = [int64]$pack.Length
            deployed = $false
        }
        $deployment.Add([pscustomobject]$record)

        if ((Test-Path -LiteralPath $target -PathType Leaf) -and -not $Force) {
            $existingHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash.ToLowerInvariant()
            if ($existingHash -eq $hash) {
                Write-Host "UNCHANGED $($pack.Name) ($hash)"
                continue
            }
            throw "Destination already contains a different pack '$target'. Use -Force only after reviewing the hash change."
        }
        Write-Host "VALID   $($pack.Name) manifest='$($manifest.header.name)' sha256=$hash"
    }

    if ($DryRun) {
        Write-Host "Dry run complete. $($packs.Count) pack(s) validated; no files were changed."
        return
    }

    New-Item -ItemType Directory -Path $staging -Force | Out-Null
    foreach ($record in $deployment) {
        $sourceFile = $record.source
        $stagedFile = Join-Path $staging $record.file
        Copy-Item -LiteralPath $sourceFile -Destination $stagedFile
        $stagedHash = (Get-FileHash -LiteralPath $stagedFile -Algorithm SHA256).Hash.ToLowerInvariant()
        if ($stagedHash -ne $record.sha256) {
            throw "Staged hash mismatch for $($record.file)"
        }
    }

    foreach ($record in $deployment) {
        $stagedFile = Join-Path $staging $record.file
        $target = $record.destination
        if (Test-Path -LiteralPath $target -PathType Leaf) {
            if (-not $Force) {
                continue
            }
            Remove-Item -LiteralPath $target -Force
        }
        Move-Item -LiteralPath $stagedFile -Destination $target
        $record.deployed = $true
        Write-Host "DEPLOYED $($record.file) -> $target" -ForegroundColor Green
    }

    $deploymentPath = Join-Path $destination ".hydraulic-deployment.json"
    [ordered]@{
        generatedAt = [DateTimeOffset]::UtcNow.ToString("o")
        sourceRoot = $source
        destinationRoot = $destination
        packs = @($deployment)
    } | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $deploymentPath -Encoding UTF8
    Write-Host "Wrote deployment manifest: $deploymentPath"
}
finally {
    if (Test-Path -LiteralPath $staging) {
        Remove-Item -LiteralPath $staging -Recurse -Force
    }
}
