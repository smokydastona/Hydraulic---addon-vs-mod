package org.geysermc.hydraulic.companion;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ensures the companions directory tree exists and seeds a README explaining the
 * companion package layout. This never fabricates a fake companion package; server
 * owners install real companion packages, for example via
 * {@code scripts/deploy-companion-pack.ps1} in the repository root.
 */
public final class CompanionBuiltinBootstrap {
    private static final String README = """
            # Hydraulic Companion Packages

            Each subdirectory here is a Phlodgate "Companion Package": a Bedrock resource pack
            (delivered automatically to every Bedrock/Geyser session) paired with an optional
            Bedrock behavior pack that is documented for capability reporting only. Geyser does
            not install or execute Bedrock behavior-pack scripts on the client, so a behavior
            pack listed here never runs through Geyser; it only informs
            `config/hydraulic/reports/companion-report.json`.

            Expected layout:

            ```
            config/hydraulic/companions/<companion-id>/
              companion.json
              resource_pack/
                manifest.json
                ...
              behavior_pack/        (optional, documentation/capability-reporting only)
                manifest.json
                ...
            ```

            `companion.json` fields:

            - `id` (required): stable companion identifier.
            - `name`, `version`: display metadata.
            - `resourcePack`: directory name of the Bedrock resource pack (default `resource_pack`).
            - `behaviorPack`: directory name of the Bedrock behavior pack, if any.
            - `executionMode`: `client_global_behavior_pack` (default) or `geyser_resource_pack_only`.
            - `capabilities`: array of `{ "id", "description", "requiresServerBridge" }` entries used
              to produce `config/hydraulic/reports/companion-report.json`.

            See `scripts/deploy-companion-pack.ps1` in the repository root for an example of building
            and installing a real companion package (the bundled Phlodgate Add-On).
            """;

    private CompanionBuiltinBootstrap() {
    }

    public static void ensureLayout(@NotNull Logger logger, @NotNull Path companionsRoot) {
        try {
            Files.createDirectories(companionsRoot);
            Path readme = companionsRoot.resolve("README.md");
            if (!Files.exists(readme)) {
                Files.writeString(readme, README, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            logger.warn("Failed to seed companions directory {}", companionsRoot, e);
        }
    }
}
