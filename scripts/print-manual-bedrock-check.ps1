<#
    Prints the manual Bedrock-client verification step. This step is never automated: VS Code cannot
    control the real Minecraft Bedrock Windows client, and CLIENT_OBSERVED must remain a human-attested
    outcome per the fork architecture plan's runtime-traceability rule.
#>
param()

Write-Host ""
Write-Host "================================================================"
Write-Host " MANUAL BEDROCK CLIENT CHECK (required - not automatable)"
Write-Host "================================================================"
Write-Host " 1. Open Minecraft Bedrock (Windows client)."
Write-Host " 2. Add/select a server pointing at 127.0.0.1:19132."
Write-Host " 3. Connect and confirm login + world spawn."
Write-Host " 4. Perform the target action (e.g. open the barrel menu fixture,"
Write-Host "    transfer an item) that exercises the runtime path you are"
Write-Host "    validating."
Write-Host " 5. Open config/hydraulic/reports/integration-test-report.json,"
Write-Host "    confirm the resulting trace in the latest handoff export, then"
Write-Host "    manually change bedrockClientCheck from"
Write-Host "    PENDING_MANUAL_CLIENT_CHECK to your observed result."
Write-Host "================================================================"
Write-Host ""
