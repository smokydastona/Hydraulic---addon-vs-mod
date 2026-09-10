<#
    Best-effort helper that opens a minecraft:// deep link to register the Phlodgate dev server with the
    installed Bedrock client. This is not guaranteed automatable behavior; add the server manually in-game
    if the deep link does not register it.
#>
param(
    [string]$ServerName = "Phlodgate DEV",
    [string]$Address = "127.0.0.1:19132"
)

$encodedName = [System.Uri]::EscapeDataString($ServerName)
$uri = "minecraft://?addExternalServer=$encodedName|$Address"

Write-Host "Attempting best-effort Bedrock server registration via deep link:"
Write-Host "  $uri"
Write-Host "If this does not register the server automatically, add it manually in-game:"
Write-Host "  Name: $ServerName"
Write-Host "  Address: $Address"

Start-Process $uri
