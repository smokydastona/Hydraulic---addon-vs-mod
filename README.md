# Hydraulic

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

Hydraulic is a companion to Geyser which allows for Bedrock players to join modded Minecraft: Java Edition servers.

Hydraulic is an open collaboration project by [CubeCraft Games](https://cubecraft.net).

## About This Fork
This fork keeps the normal Hydraulic pack pipeline, but now adds a broader compatibility-analysis and metadata-patch layer on top of it.

The goal is no longer just "JSON override a block." The fork now records typed compatibility evidence for discovered mod content, separates compatibility into content, presentation, state/data, interaction, and behavior domains, and lets metadata patches progressively refine the result.

Right now this fork adds:
- metadata-based block matching by Java block ID and optional Java state filters
- item, recipe, entity, and menu identifier mapping metadata on the same compatibility/report foundation
- Metadata V2 patch loading for blocks, items, recipes, entities, and menus
- lazy on-demand model loading from indexed model paths with a bounded cache instead of eager global model deserialization at startup
- shared cached texture-output resolution for model- and block-texture paths, with live hit and miss metrics in the performance report
- a first metadata-backed menu fallback bridge that can route unsupported Java menu opens into an explicitly declared Bedrock `ContainerType`
- capability-driven adapter dispatch for the live menu and block-entity bridge seams, so runtime translator creation now follows analyzer-produced adapter bindings
- override support for Bedrock block identifier, geometry, and material
- typed compatibility objects with support levels, confidence, provenance, findings, and mod fingerprints
- a first analyzer API with block, item, entity, fluid, block-entity, menu, and recipe analyzers
- a first entity runtime consumer that registers metadata-backed custom entities through Geyser
- local Fabric dev metadata examples for the test block, item, and recipe surfaces

## What is Hydraulic?
Hydraulic is a server-side mod, which allows for Bedrock players to join modded Minecraft: Java Edition servers. This project works alongside [Geyser](https://github.com/GeyserMC/Geyser) to make this possible.

### This project is still in very early development and should not be used on production setups! You can get [Hydraulic](https://geysermc.org/download?project=other-projects&hydraulic=expanded) from the GeyserMC website.

## What Changed Compared To Upstream Hydraulic?
Upstream Hydraulic mainly relies on its existing registry, model, and resource-pack conversion flow.

This fork adds a declarative metadata index that is loaded during pack manager startup. That metadata is then used by the block conversion path to selectively override how a block is exposed to Bedrock, while the compatibility report records how far each discovered content object gets across the five compatibility domains.

In practice, that means you can now attach extra mapping rules in JSON for cases where a mod content entry needs:
- a different Bedrock identifier
- a specific Bedrock geometry name
- a specific material key
- a small Bedrock state override for the generated custom block state

If no metadata rule matches, Hydraulic falls back to its normal behavior.

## Metadata Overrides
Metadata files are loaded from Hydraulic's metadata directory:

`config/hydraulic/metadata`

The loader now walks this directory recursively. The intended layout is:

- `config/hydraulic/metadata/builtin`
- `config/hydraulic/metadata/mods`
- `config/hydraulic/metadata/server`
- `config/hydraulic/metadata/user`

Higher-priority folders win when rules overlap:

- `builtin` = 0
- `mods` = 100
- `server` = 500
- `user` = 1000

In the Fabric dev environment used by this repo, that resolves to:

`fabric/run/config/hydraulic/metadata`

The included example file is:

`fabric/run/config/hydraulic/metadata/hydraulic_test_mod.golden_barrel.json`

Example:

```json
{
	"blocks": [
		{
			"java_id": "hydraulic_test_mod:golden_barrel",
			"rules": [
				{
					"bedrock_identifier": "hydraulic_test_mod:golden_barrel_override",
					"bedrock_state": {
						"variant": "gold"
					},
					"geometry": "minecraft:geometry.full_block",
					"material": "hydraulic_test_mod:block/golden_barrel"
				}
			]
		}
	]
}
```

Supported fields today:
- `java_id`: the Java block identifier
- `java_when`: optional Java block state match values
- `bedrock_identifier`: optional Bedrock block identifier override
- `bedrock_state`: optional Bedrock state values to inject into the generated custom block state
- `geometry`: optional Bedrock geometry override
- `material`: optional Bedrock material override
- `behavior_required`: marks content that still needs a runtime bridge or behavior-layer support
- `behavior_tag`: declares the bridge category Hydraulic should report and route through capability adapters

The same metadata directory also supports simple item, recipe, entity, and menu identifier mappings:

```json
{
	"items": [
		{
			"java_id": "hydraulic_test_mod:barrel_pack",
			"bedrock_identifier": "hydraulic_test_mod:barrel_pack_override"
		}
	],
	"recipes": [
		{
			"java_id": "hydraulic_test_mod:barrel_stick",
			"bedrock_identifier": "hydraulic_test_mod:barrel_stick_recipe_override"
		}
	],
	"entities": [
		{
			"java_id": "hydraulic_test_mod:barrel_cube",
			"bedrock_identifier": "hydraulic_test_mod:barrel_cube_override"
		}
	],
	"menus": [
		{
			"java_id": "hydraulic_test_mod:barrel_menu",
			"bedrock_identifier": "hydraulic_test_mod:barrel_menu_override"
		}
	]
}
```

Metadata V2 additionally supports patch entries. Patches are loaded recursively, respect the same ownership precedence, are recorded in the compatibility report, and can synthesize current block and identifier mappings when they expose fields Hydraulic already understands.

Example patch file:

```json
{
	"patches": [
		{
			"target": "hydraulic_test_mod:golden_barrel",
			"content_type": "block",
			"patch": {
				"visual": {
					"geometry": "minecraft:geometry.full_block",
					"material": "hydraulic_test_mod:block/golden_barrel"
				},
				"bedrock": {
					"identifier": "hydraulic_test_mod:golden_barrel_override",
					"state": {
						"variant": "gold"
					}
				},
				"behavior": {
					"required": true,
					"tag": "machine"
				}
			}
		}
	]
}
```

Supported patch paths today:
- `bedrock.identifier`
- `bedrock.state.<key>`
- `bedrock.menu.container_type`
- `bedrock.block_entity.id`
- `bedrock.block_entity.data.<key>`
- `visual.geometry`
- `visual.material`
- `java.when.<key>`
- `behavior.required`
- `behavior.tag`

Unsupported patch data is still preserved in the metadata index and compatibility report, but only the fields above are synthesized into the current runtime mapping layer.

## Compatibility Inventory And Report
On startup, this fork now writes three compatibility artifacts under Hydraulic's data folder:

- `config/hydraulic/reports/content-inventory.json`
- `config/hydraulic/reports/compatibility-report.json`
- `config/hydraulic/reports/performance-report.json`
- `config/hydraulic/reports/pack-validation-report.json`

`content-inventory.json` records the per-mod discovery inventory, including registry entries, discovered assets, metadata targets, patch targets, and a mod fingerprint.

That inventory now reuses the same indexed asset and data pass created during startup, so compatibility reporting no longer performs its own second filesystem walk just to rebuild those discovered resource categories.

Pack invalidation is now also driven by a persisted conversion key under Hydraulic's per-mod storage instead of manifest UUIDs derived from a full-tree hash walk. That key is built from the indexed mod resource fingerprint, the transitive fingerprints of any indexed mods referenced through model or equipment namespaces, Hydraulic version, Minecraft version, and loaded metadata state, so metadata-only changes and cross-mod asset dependency changes now correctly force reconversion.

Hydraulic now also persists a first-class cache layout under `config/hydraulic/cache` for index, compatibility, conversion, validation, and manifest artifacts. On repeat startup, Hydraulic can now rehydrate the live per-mod `ModResourceIndex` state directly from the cached index snapshot when mod roots, indexed files, and indexed directories are unchanged, and the compatibility layer can reuse cached `content-inventory.json` and `compatibility-report.json` when the indexed mod fingerprints and metadata fingerprint are unchanged. Conversion and validation artifacts are mirrored into the same cache tree for inspection and future reuse work.

Model lookup during pack conversion no longer requires eagerly flattening every discovered mod model into one startup-time map. `ModResourceIndex` now records generic model file paths, and Hydraulic builds a lightweight `IndexedModelProvider` that lazily deserializes mod or vanilla model JSON on demand through a bounded cache. Missing models are negatively cached as well, so repeated parent lookups for absent entries do not repeatedly hit disk.

Texture-output resolution now also reuses a shared bounded cache instead of recomputing the same Bedrock output paths every time item, bow, or block conversion asks for them. The current slice centralizes those lookups under `TexturePackModule` and records live hit, miss, eviction, and size evidence in the same performance artifact used for the model-provider and runtime-dispatch slices.

Texture conversion itself now also builds a per-pack dependency graph from converted models plus armor equipment assets before the texture stage runs. Hydraulic uses that graph to record how many textures were discovered, selected, and omitted for each conversion batch, accumulates the selected set across multi-root extraction passes for the same mod, and keeps block-texture registration aligned with that aggregated selected set. In the current bundled test mod, the validated runtime still selected all 17 discovered textures because every texture asset is referenced, but the dependency graph and report path are now real and ready to expose reductions in larger packs.

Block texture post-processing no longer needs an eager full indexed-texture scan just to register Bedrock texture names and flipbooks. Hydraulic now resolves only the selected texture keys for the current mod back through `ModResourceIndex`, and it lazily reads only the selected texture `.mcmeta` files that actually need animation metadata during block registration.

Block material caching is now also demand-driven. Instead of stitching every parsed model during preprocessing just to populate `materials.json`, Hydraulic now builds and persists material entries only when a real block-state model or explicit metadata `material` override is actually consumed during custom-block registration.

Block preprocessing no longer parses the full `ResourcePack` blockstate asset set up front. `ModResourceIndex` now resolves blockstate file paths directly for the registered blocks owned by the current mod, and Hydraulic deserializes only those indexed blockstates that the block registration path can actually consume.

Item preprocessing no longer depends on a full parsed `ResourcePack` item-definition walk either. Hydraulic now resolves indexed item asset paths per registered item, deserializes modern `assets/.../items/*.json` definitions only when present, and falls back to the existing lazy model-provider path for legacy `models/item/*.json` assets.

The current compatibility report is now also compiled into a first in-memory runtime dispatch surface during startup. The first `CompiledCompatibilityPlan` slice covers block creative and placement decisions, item registration and creative exposure, armor and bow attachable presentation, metadata-backed custom entity registration, menu fallback translators, block-entity patch translators, the candidate indexes used by unsupported menu and block-entity runtime diagnostics, and precompiled state-aware block definition groupings plus per-state runtime metadata for block registration and block-item placement. The remaining block-item texture fallback decision path now also consumes compiled block plans instead of reading raw compatibility objects back out of the report during conversion. Current runtime bridges and warning paths now hit direct identifier-driven lookups or precompiled candidate lists instead of re-scanning compatibility profiles or metadata templates on each use.

`compatibility-report.json` records per-object analyzer output, including:
- support levels: `NATIVE`, `AUTOMATIC`, `ADAPTED`, `APPROXIMATED`, `VISUAL_ONLY`, `UNSUPPORTED`
- the five compatibility domains: content, presentation, state/data, interaction, behavior
- capability requirements and analyzer results
- derived adapter bindings for the currently implemented generic runtime bridges
- derived runtime requirements for the bridges or generators that are still missing
- patch-derived facts such as `behavior_required` and `behavior_tag` when present
- confidence, provenance, and structured findings
- metadata validation issues emitted during Metadata V2 loading
- a `packValidation` section merged in after pack preparation completes, summarizing each mod's `valid` flag, `errorCount`, `warningCount`, and `manualActions` from `pack-validation-report.json`, so manual actions are visible next to compatibility findings instead of only in the sibling artifact

`performance-report.json` records the measured startup and conversion costs for the current run, including resource indexing time, indexed blockstate and item-asset totals, metadata load time, compatibility initialization time, lazy model-provider setup time, cumulative `StateDefinition` model-resolution cache hits and misses, lazy model-provider cache hits/misses/evictions/size/indexed-model totals, shared texture-resolution cache hits/misses/evictions/size, aggregate and per-mod texture dependency selection counts (`discoveredTextures`, `selectedTextures`, `omittedTextures`, `textureDependencySources`), artifact-cache hit/miss evidence for index, compatibility, conversion, and validation stages, runtime-dispatch hit/miss evidence for block, item, entity, menu, and block-entity plan lookups, and the last pack-conversion batch with per-mod outcomes. Compatibility inventory generation, index rehydration, pack invalidation, model lookup, repeated texture-output resolution, texture-stage selection across multiple resource roots, and block texture post-processing now all ride on indexed discovery data or bounded caches rather than separate eager filesystem or model-parse passes, and the compiled plan slice continues to expose measurable identifier-driven lookup activity in the same artifact.

The persisted `conversion-key.json` in Hydraulic's per-mod storage now also records `dependencyFingerprint` and `dependentModCount`, so cache identity is inspectable when a mod's generated output depends on assets owned by another indexed mod. That dependency fingerprint is now driven by explicit indexed model and equipment resource edges plus the concrete referenced model and texture file stamps they traverse, which means unrelated files elsewhere in a dependent namespace no longer force reconversion.

`pack-validation-report.json` records post-generation Bedrock pack validation per converted mod. It now checks whether a generated pack archive exists, whether `manifest.json` is present and structurally valid, whether all generated JSON entries are parseable, whether the archive contains content beyond manifest scaffolding, whether `pack_icon.png` is present, and whether generated texture files match the texture dependency graph's concrete selected texture set. That last slice can now fail missing required generated textures and warn on leftover unreferenced texture files before runtime registration. Each mod result includes structured `errors`, `warnings`, and `manualActions` so invalid generated output is visible before runtime registration. Hydraulic now prepares converted packs during its own startup and only registers those prepared packs later at the Geyser resource-pack seam, so these structural artifacts still land even when a later Geyser Bedrock bootstrap step fails.

When Geyser receives a Java menu open for a container type it cannot translate, Hydraulic now emits a compatibility-backed runtime warning that explains the protocol boundary and lists any discovered menu objects still requiring menu runtime bridges. When Hydraulic can resolve the live Java menu identifier, that warning now binds to the matched compiled menu plan and reports its explicit requirements such as `container_bridge` or `menu_behavior_bridge` instead of only reporting the protocol container type.

Metadata V2 can now also drive a first real menu fallback bridge. A `menu` patch with `bedrock.menu.container_type` lets Hydraulic resolve the live Java menu identifier from the active server container state and substitute an existing Geyser inventory translator when the original open-screen path had no translator. That runtime path now also requires the compiled `menu.fallback_translator` adapter binding, so the bridge activates through the same capability-driven adapter selection recorded in compatibility reporting instead of from raw patch presence alone. This is intentionally limited to explicit fallback layouts; it does not create a generic menu behavior bridge.

When Geyser receives Java block entity data that falls through to its default `EmptyBlockEntityTranslator`, Hydraulic now attempts to resolve the live Java block entity at that position and emits a compatibility-backed runtime warning only when the compatibility report already marks that object as still requiring block-entity runtime bridges such as `block_entity_data_bridge`. That unsupported-runtime path now also reads from precompiled dispatch plans and bridge-candidate indexes instead of rescanning compatibility objects live.

Metadata V2 can now also drive a first real block-entity data bridge. A `block_entity` patch with `bedrock.block_entity.id` and `bedrock.block_entity.data.*` synthesizes a constant Bedrock block-entity tag at the same Geyser seam before Hydraulic falls back to the unsupported-runtime warning path. That translator now also requires the compiled `block_entity.patch_translator` adapter binding, so runtime activation follows analyzer-backed capability selection rather than raw patch presence alone. This is intentionally limited to data translation; interaction and behavior bridges are still missing.

The current report is still conservative. It is intended to answer "what do we know right now from registries, assets, metadata, and patches?" not "is this mod fully playable end-to-end on Bedrock?" Behavior-heavy entities, fluids, menus, and block entities will still show low support until dedicated runtime bridges are implemented. For items, behavior-tagged patches already affect runtime exposure decisions, so unsupported behavior can now suppress Bedrock creative exposure even when the item is still registered. For entities, metadata-backed identifier mappings now drive custom entity registration, but that is still not full interaction or behavior translation.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](https://discord.gg/geysermc) if
you're interested in helping out with Hydraulic.

### Project Setup
1. Clone the repo to your computer.
2. Navigate to the Hydraulic root directory and run `git submodule update --init --recursive`. This command downloads all the needed submodules for Hydraulic and is a crucial step in this process.
3. If your default JVM/JDK is not Java 25, please set your IDE to use a valid Java 25 JVM. Otherwise, you will run into an error while building Hydraulic. 
4. The project should import into your IDE after the loom setup is complete. For more detailed information, see the [Fabric setup](https://docs.fabricmc.net/develop/getting-started/setting-up).
5. Use `./gradlew build` to compile jars and refresh the generated test assets used by this fork's Fabric test module.
6. Use `./gradlew :fabric:runServer` to run a server with Hydraulic installed. This runtime path reuses the last generated test assets instead of rerunning datagen on every launch. Make sure you have Geyser in your `mods` folder along with Hydraulic.
7. If you want to refresh only the generated test assets without a full build, run `./gradlew :test:prepareGeneratedResources`.
8. If you want to test metadata overrides in the Fabric dev environment, place your JSON files in `fabric/run/config/hydraulic/metadata` before starting the server.

## Links:
- Website: https://geysermc.org
- Docs: https://geysermc.org/wiki/other/hydraulic
- Download: https://geysermc.org/download?project=other-projects&hydraulic=expanded
- Discord: https://discord.gg/geysermc
- Donate: https://opencollective.com/geysermc
