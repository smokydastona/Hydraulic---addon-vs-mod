<##
    Verifies the repository source-of-truth policy without changing any worktree.

    The canonical source is the repository containing this script. Runtime validation
    output belongs in the canonical repository's ignored fabric/run tree or in the
    linked _runtime_validation worktree. _push_worktree is an export/push checkout,
    not an additional development source tree.
##>
param(
    [string]$CanonicalRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$WorkspaceRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
)

$ErrorActionPreference = "Stop"

function Get-GitValue([string]$Path, [string[]]$Arguments) {
    $value = & git -C $Path @Arguments 2>$null
    if ($LASTEXITCODE -ne 0) {
        return $null
    }
    return (($value | Out-String).Trim())
}

if (-not (Test-Path (Join-Path $CanonicalRoot "settings.gradle.kts"))) {
    throw "Canonical root does not contain settings.gradle.kts: $CanonicalRoot"
}

$canonicalTopLevel = Get-GitValue $CanonicalRoot @("rev-parse", "--show-toplevel")
if ([string]::IsNullOrWhiteSpace($canonicalTopLevel)) {
    throw "Canonical root is not a Git repository: $CanonicalRoot"
}

if ([IO.Path]::GetFullPath($canonicalTopLevel).TrimEnd("\") -ne [IO.Path]::GetFullPath($CanonicalRoot).TrimEnd("\")) {
    throw "Canonical root resolves to a different Git top-level: $canonicalTopLevel"
}

$canonicalBranch = Get-GitValue $CanonicalRoot @("branch", "--show-current")
$canonicalCommit = Get-GitValue $CanonicalRoot @("rev-parse", "--short", "HEAD")
$canonicalStatus = @(Get-GitValue $CanonicalRoot @("status", "--porcelain"))

Write-Host "Canonical source: $CanonicalRoot"
Write-Host "  branch: $canonicalBranch"
Write-Host "  commit: $canonicalCommit"
Write-Host "  status: $(if ($canonicalStatus.Count -eq 0 -or [string]::IsNullOrWhiteSpace($canonicalStatus[0])) { 'clean' } else { 'changes present' })"

$runtimeValidationRoot = Join-Path $WorkspaceRoot "_runtime_validation"
if (Test-Path $runtimeValidationRoot) {
    $runtimeGitValue = Get-GitValue $runtimeValidationRoot @("rev-parse", "--git-dir")
    $runtimeBranch = Get-GitValue $runtimeValidationRoot @("branch", "--show-current")
    $runtimeCommit = Get-GitValue $runtimeValidationRoot @("rev-parse", "--short", "HEAD")
    if ([string]::IsNullOrWhiteSpace($runtimeGitValue)) {
        throw "_runtime_validation exists but is not a Git worktree: $runtimeValidationRoot"
    }
    Write-Host "Runtime validation tree: $runtimeValidationRoot"
    Write-Host "  branch: $(if ([string]::IsNullOrWhiteSpace($runtimeBranch)) { '(detached)' } else { $runtimeBranch })"
    Write-Host "  commit: $runtimeCommit"
    Write-Host "  role: validation-only; do not use as a source tree"
} else {
    Write-Warning "_runtime_validation is absent; runtime validation artifacts are not available in this workspace."
}

$pushRoot = Join-Path $WorkspaceRoot "_push_worktree"
if (Test-Path $pushRoot) {
    $pushTopLevel = Get-GitValue $pushRoot @("rev-parse", "--show-toplevel")
    if ([string]::IsNullOrWhiteSpace($pushTopLevel)) {
        throw "_push_worktree exists but is not a Git repository: $pushRoot"
    }
    $pushBranch = Get-GitValue $pushRoot @("branch", "--show-current")
    $pushCommit = Get-GitValue $pushRoot @("rev-parse", "--short", "HEAD")
    Write-Host "Push/export tree: $pushRoot"
    Write-Host "  branch: $pushBranch"
    Write-Host "  commit: $pushCommit"
    Write-Host "  role: export/push-only; do not use as a source tree"
} else {
    Write-Warning "_push_worktree is absent; no export/push checkout was detected."
}

Write-Host "Repository topology validation passed."
