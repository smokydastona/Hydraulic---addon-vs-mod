<#
    Confirms Phlodgate's pack-validation-report.json shows every generated pack as valid for the current
    dev run. Reads only PackValidationTracker output; does not run gradle and is therefore safe to run
    while the Fabric dev server is still active.
#>
param(
    [string]$RunDir = (Join-Path $PSScriptRoot "..\fabric\run")
)

$ErrorActionPreference = "Stop"

$reportPath = Join-Path $RunDir "config\hydraulic\reports\pack-validation-report.json"

if (-not (Test-Path $reportPath)) {
    Write-Error "No pack-validation-report.json found at $reportPath. Run 'Hydraulic: Start Dev Server (Fabric)' at least once before validating generated packs."
    exit 1
}

$json = Get-Content $reportPath -Raw | ConvertFrom-Json
$perMod = $json.perMod

if (-not $perMod) {
    Write-Error "pack-validation-report.json at $reportPath has no 'perMod' section."
    exit 1
}

$modIds = ($perMod | Get-Member -MemberType NoteProperty).Name
if ($modIds.Count -eq 0) {
    Write-Error "pack-validation-report.json at $reportPath reports no packs."
    exit 1
}

$failed = @()
foreach ($modId in $modIds) {
    $validation = $perMod.$modId
    $errorCount = 0
    $warningCount = 0
    $manualCount = 0
    if ($validation.errors) { $errorCount = $validation.errors.Count }
    if ($validation.warnings) { $warningCount = $validation.warnings.Count }
    if ($validation.manualActions) { $manualCount = $validation.manualActions.Count }
    $status = if ($validation.valid) { "valid" } else { "INVALID" }
    Write-Host ("  {0,-24} {1,-8} errors={2} warnings={3} manualActions={4}" -f $modId, $status, $errorCount, $warningCount, $manualCount)
    if (-not $validation.valid) {
        $failed += $modId
    }
}

if ($failed.Count -gt 0) {
    Write-Error "Pack validation failed for: $($failed -join ', ')"
    exit 1
}

Write-Host "Pack validation passed for $($modIds.Count) pack(s)."
exit 0
