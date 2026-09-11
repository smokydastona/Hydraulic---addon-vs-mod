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
Write-Host " 4. Place hydraulic_test_mod:processing_machine, hold cobblestone,"
Write-Host "    and use the block once. Confirm the held stack decreases by one;"
Write-Host "    repeated use must stop consuming items when input slot 0 is full."
Write-Host " 5. Place hydraulic_test_mod:menu_machine and use it with an empty hand."
Write-Host "    Confirm the Furnace-compatible menu opens, both machine slots are"
Write-Host "    usable with output insertion blocked, and the progress property"
Write-Host "    advances without reopening or desynchronizing the menu."
Write-Host " 6. Disconnect/reconnect, reopen menu_machine, and confirm its visible"
Write-Host "    inventory persisted and progress resumes without stale values."
Write-Host " 7. Open config/hydraulic/reports/integration-test-report.json,"
Write-Host "    confirm the resulting trace in the latest handoff export, then"
Write-Host "    manually change bedrockClientCheck from"
Write-Host "    PENDING_MANUAL_CLIENT_CHECK to your observed result."
Write-Host "================================================================"
Write-Host ""
