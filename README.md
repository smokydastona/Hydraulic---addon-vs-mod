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

`content-inventory.json` records the per-mod discovery inventory, including registry entries, discovered assets, metadata targets, patch targets, and a mod fingerprint.

`compatibility-report.json` records per-object analyzer output, including:
- support levels: `NATIVE`, `AUTOMATIC`, `ADAPTED`, `APPROXIMATED`, `VISUAL_ONLY`, `UNSUPPORTED`
- the five compatibility domains: content, presentation, state/data, interaction, behavior
- capability requirements and analyzer results
- derived adapter bindings for the currently implemented generic runtime bridges
- derived runtime requirements for the bridges or generators that are still missing
- patch-derived facts such as `behavior_required` and `behavior_tag` when present
- confidence, provenance, and structured findings
- metadata validation issues emitted during Metadata V2 loading

`performance-report.json` records the measured startup and conversion costs for the current run, including resource indexing time, metadata load time, compatibility initialization time, resource-pack/model indexing time, and the last pack-conversion batch with per-mod outcomes. This is the report to inspect when checking whether cache/indexing changes actually moved the cost profile.

When Geyser receives a Java menu open for a container type it cannot translate, Hydraulic now emits a compatibility-backed runtime warning that explains the protocol boundary and lists any discovered menu objects still requiring the `container_bridge` runtime requirement.

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
