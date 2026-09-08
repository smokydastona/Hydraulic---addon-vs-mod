# Copilot Instructions — Hydraulic Workspace

## Truth-first rules
- Be direct when something cannot be verified, built, or run in this environment.
- Do not claim a runtime result, generated pack artifact, or successful build unless it was actually checked.
- Do not invent certainty from earlier chats, missing logs, or unavailable files.
- When a behavior depends on Geyser, Fabric runtime state, generated assets, or a live server run, say exactly what was verified and what was not.

## Workspace shape
- The workspace folder is `D:/Downloads/Hydraulic-mod`, but the actual Hydraulic project root is `D:/Downloads/Hydraulic-mod/Hydraulic-mod`.
- Treat `Hydraulic-mod/` inside the workspace as the real repo root for code, Gradle, workflows, and docs.
- Active modules:
  - `shared/` — core Hydraulic logic, pack pipeline, metadata loading, converters, common runtime
  - `fabric/` — active platform bootstrap and main dev runtime
  - `test/` — bundled Fabric test mod and test assets used to validate pack conversion behavior
- Present but not currently active:
  - `neoforge/` exists in the tree, but it is commented out in `settings.gradle.kts`; do not assume it is part of the current build unless the user asks to re-enable it.

## Project mission
- Hydraulic is a companion to Geyser that makes modded Java server content available to Bedrock players through generated packs and custom block/item registration.
- Prefer solutions that preserve Hydraulic's existing conversion flow instead of replacing it with one-off logic.
- Keep new behavior additive and reversible. If an override is optional, keep the default upstream path intact when no override applies.

## Architecture rules that matter here
- The center of gravity is `shared/`, not `fabric/`. If behavior is platform-agnostic, implement it in `shared/` first.
- `PackManager`, pack contexts, pack modules, resource-pack conversion, and custom block/item registration are the core behavior surfaces.
- Prefer rule-driven or metadata-driven extensions over hardcoded special cases when the feature is about mapping Java content to Bedrock content.
- When changing block behavior, follow the real control flow:
  - metadata/config load
  - pack manager/context access
  - pack module conversion
  - Geyser custom block registration
- `test/` is the safest place to anchor examples and validation targets for new conversion logic.

## Metadata override conventions
- Structural metadata lives under Hydraulic's data folder at `config/hydraulic/metadata`.
- In the Fabric dev environment for this workspace, that resolves to `Hydraulic-mod/fabric/run/config/hydraulic/metadata` under the project root.
- Metadata should extend the existing pipeline, not bypass it.
- Current metadata use cases include:
  - matching Java block IDs
  - optional Java state filters
  - Bedrock identifier overrides
  - geometry overrides
  - material overrides
  - narrow Bedrock state overrides for generated custom block properties/permutations
- Keep the schema simple. Add fields only when the code path and validation need them.

## Implementation standards
- Build complete changes. Do not leave placeholders, fake integrations, or speculative TODO behavior in shipped code.
- Fix the root cause when practical instead of stacking narrow patches.
- Keep public behavior stable unless the user asked for a behavior change.
- Preserve the existing style of the touched module. Avoid broad cleanup or unrelated refactors.
- Do not turn a metadata feature into filename heuristics or fuzzy matching if the existing system is registry/model/state based.

## Workflow after every code change
1. Run diagnostics for the touched files, or workspace-wide diagnostics if the change is broad.
2. Check the likely impact radius before committing.
3. Update any affected user-facing documentation in the same change set. In this workspace that usually means `README.md`, and any config/example files that act as documentation.
4. Re-run diagnostics after the edit.
5. If a narrow executable validation exists, run it before doing more edits.
6. Explain what changed, what was verified, and what still depends on runtime or CI.
7. Default to commit and push after a completed change set unless the user says not to.

## Validation guidance for this repo
- Prefer the cheapest validation that can falsify the change:
  - `get_errors` on touched files
  - a narrow Gradle task such as `:shared:compileJava`
  - a focused Fabric runtime check when generated pack output matters
- Use local build validation when it is the only practical way to verify the touched behavior.
- Be careful with environment constraints already seen in this workspace:
  - Java 25 is required
  - the local machine may need `GRADLE_USER_HOME`, `TEMP`, and `TMP` redirected off `C:`
- Do not claim pack-content verification unless the generated output was actually inspected.

## Impact-radius checklists

### Pack pipeline changes
- Re-scan:
  - `shared/src/main/java/org/geysermc/hydraulic/pack/**`
  - `shared/src/main/java/org/geysermc/hydraulic/pack/context/**`
  - any affected `PackModule` implementations
- Verify the change fits the existing initialization and conversion flow.
- Check whether dev/runtime data paths, storage paths, or generated pack paths changed.

### Block conversion changes
- Re-scan:
  - `shared/src/main/java/org/geysermc/hydraulic/block/BlockPackModule.java`
  - related model/state/material classes in `shared/src/main/java/org/geysermc/hydraulic/block/`
  - `test/src/main/java/org/geysermc/hydraulic/fabric/test/ModBlocks.java` when a test block is involved
- Confirm custom block properties, permutations, geometry, materials, and state registration remain consistent.
- If a new override changes Bedrock state declarations, verify the declaration path, condition path, and final block-state registration path all agree.

### Metadata schema or loader changes
- Re-scan:
  - `shared/src/main/java/org/geysermc/hydraulic/metadata/**`
  - `shared/src/main/java/org/geysermc/hydraulic/pack/PackManager.java`
  - consumers such as `BlockPackModule`
- Keep parsing strict enough to reject invalid data, but tolerant enough to skip bad entries without crashing the whole load.
- Update example metadata files if the schema or semantics changed.

### Platform/bootstrap changes
- Re-scan:
  - `fabric/src/main/java/org/geysermc/hydraulic/fabric/**`
  - `shared/src/main/java/org/geysermc/hydraulic/HydraulicImpl.java`
  - `shared/src/main/java/org/geysermc/hydraulic/platform/**`
- Keep cross-platform logic out of platform bootstrap code unless the platform actually owns it.

### Test mod and asset changes
- Re-scan:
  - `test/src/main/java/**`
  - `test/src/main/resources/**`
  - any matching conversion code in `shared/`
- Confirm IDs, textures, generated model assumptions, and sample metadata all point at real test content.

### Gradle/build logic changes
- Re-scan:
  - `settings.gradle.kts`
  - `build.gradle.kts`
  - `build-logic/**`
  - module `build.gradle.kts` files
- Keep the current module picture accurate: `fabric`, `shared`, and `test` are active; `neoforge` is present but not included.
- If build behavior changes, update `README.md` accordingly.

## Documentation rules for this workspace
- Keep README edits practical and specific. Prefer plain language over generic release-note wording.
- Do not mention files or workflows that do not exist in this workspace.
- If you add a configuration format, include one real example using a real ID from the repo when possible.
- If validation could not be completed, say that in the docs or final explanation when it affects setup expectations.

## Version control and shipping
- This local workspace may not itself be a git checkout even when the user wants changes pushed to GitHub.
- If the user asks to push and the current folder is not a git repo, use a safe workflow that preserves the remote history instead of force-pushing invented history.
- Do not commit generated outputs like `build/`, `.gradle/`, `.tmp/`, or `fabric/run/` except when the user explicitly wants a checked-in sample under an otherwise ignored path.
- After every commit, push unless the user explicitly says not to.
- Do not create tags or releases unless asked.

## Commands that matter here
- Narrow compile: `./gradlew :shared:compileJava`
- Full build: `./gradlew build`
- Fabric server runtime: `./gradlew :fabric:runServer`
- Fabric client runtime: `./gradlew :fabric:runClient`

## Repo-specific guidance for Copilot
- Start from the concrete behavior surface: the owning pack module, loader, context, bootstrap, or test block.
- Use Java LSP tools when locating Java symbols by name; use generic search for config, resources, JSON, Gradle, or broad exploration.
- Prefer examples anchored in the `test` module before adding new mod-specific assumptions.
- When the user asks what changed compared to upstream Hydraulic, describe the additive behavior in this workspace rather than inventing a fork roadmap.