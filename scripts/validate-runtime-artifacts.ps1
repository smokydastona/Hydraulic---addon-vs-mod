<#
    Confirms Phlodgate produced at least one real compatibility handoff envelope for the current dev run.
    Reads only existing CompatibilityHandoffQueue/CompatibilityHandoffExporter output; does not run gradle
    and is therefore safe to run while the Fabric dev server is still active.
#>
param(
    [string]$RunDir = (Join-Path $PSScriptRoot "..\fabric\run")
)

$ErrorActionPreference = "Stop"

$exportsDir = Join-Path $RunDir "config\hydraulic\cache\handoff-queue\exports"

if (-not (Test-Path $exportsDir)) {
    Write-Error "No handoff-queue exports directory found at $exportsDir. Run 'Hydraulic: Start Dev Server (Fabric)' at least once before validating runtime artifacts."
    exit 1
}

$envelopes = Get-ChildItem -Path $exportsDir -Filter *.json -File | Sort-Object LastWriteTime -Descending

if ($envelopes.Count -eq 0) {
    Write-Error "Handoff-queue exports directory exists but contains no envelope JSON files at $exportsDir."
    exit 1
}

$latest = $envelopes[0]
Write-Host "Latest handoff envelope: $($latest.Name)"

$json = Get-Content $latest.FullName -Raw | ConvertFrom-Json

Write-Host "  envelopeId:              $($json.envelopeId)"
Write-Host "  compatibilityFingerprint: $($json.compatibilityFingerprint)"
Write-Host "  hydraulicVersion:        $($json.hydraulicVersion)"
Write-Host "  targetMinecraftVersion:  $($json.targetMinecraftVersion)"
Write-Host "  targetBedrockVersion:    $($json.targetBedrockVersion)"
Write-Host "  geyserVersion:           $($json.geyserVersion)"

$findingCount = 0
if ($json.report -and $json.report.metadataFindings) {
    $findingCount = $json.report.metadataFindings.Count
}
Write-Host "  report.metadataFindings count: $findingCount"

if ($json.report -and $json.report.mods) {
    $modKeys = ($json.report.mods | Get-Member -MemberType NoteProperty).Name
    Write-Host "  report.mods:             $($modKeys -join ', ')"
}

Write-Host "Runtime artifact validation passed: $($envelopes.Count) envelope(s) found, latest is $($latest.Name)."
exit 0
