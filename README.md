# Hydraulic

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

Hydraulic is a companion to Geyser which allows for Bedrock players to join modded Minecraft: Java Edition servers.

Hydraulic is an open collaboration project by [CubeCraft Games](https://cubecraft.net).

## About This Fork
This fork keeps the normal Hydraulic pack pipeline, but adds a metadata layer for block, item, and recipe compatibility overrides.

The goal is simple: when Hydraulic's normal model and material lookup is not enough, you can describe a block override in JSON instead of hardcoding everything in Java.

Right now this fork adds:
- metadata-based block matching by Java block ID and optional Java state filters
- item and recipe identifier mapping metadata on the same compatibility/report foundation
- override support for Bedrock block identifier, geometry, and material
- a first narrow Bedrock state override path for custom block properties and permutations
- local Fabric dev metadata examples for the test block, item, and recipe surfaces

## What is Hydraulic?
Hydraulic is a server-side mod, which allows for Bedrock players to join modded Minecraft: Java Edition servers. This project works alongside [Geyser](https://github.com/GeyserMC/Geyser) to make this possible.

### This project is still in very early development and should not be used on production setups! You can get [Hydraulic](https://geysermc.org/download?project=other-projects&hydraulic=expanded) from the GeyserMC website.

## What Changed Compared To Upstream Hydraulic?
Upstream Hydraulic mainly relies on its existing registry, model, and resource-pack conversion flow.

This fork adds a declarative metadata index that is loaded during pack manager startup. That metadata is then used by the block conversion path to selectively override how a block is exposed to Bedrock.

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
- `behavior_required`: reserved for future behavior-pack work
- `behavior_tag`: reserved for future behavior-pack work

The same metadata directory now also supports simple item and recipe identifier mappings:

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
	]
}
```

## Compatibility Inventory And Report
On startup, this fork now writes two early compatibility artifacts under Hydraulic's data folder:

- `config/hydraulic/reports/content-inventory.json`
- `config/hydraulic/reports/compatibility-report.json`

These files are meant to give you a machine-readable view of what Hydraulic discovered before deeper compatibility analyzers exist. The current report is inventory-backed, so it is useful for regression tracking and metadata coverage, but it is not yet a full gameplay compatibility verdict.

## Contributing
Any contributions are appreciated. Please feel free to reach out to us on [Discord](https://discord.gg/geysermc) if
you're interested in helping out with Hydraulic.

### Project Setup
1. Clone the repo to your computer.
2. Navigate to the Hydraulic root directory and run `git submodule update --init --recursive`. This command downloads all the needed submodules for Hydraulic and is a crucial step in this process.
3. If your default JVM/JDK is not Java 25, please set your IDE to use a valid Java 25 JVM. Otherwise, you will run into an error while building Hydraulic. 
4. The project should import into your IDE after the loom setup is complete. For more detailed information, see the [Fabric setup](https://docs.fabricmc.net/develop/getting-started/setting-up).
5. Use `./gradlew build` to compile a jar file, or use `./gradlew :fabric:runServer` to run a server with Hydraulic installed. Make sure you have Geyser in your `mods` folder along with Hydraulic!
6. If you want to test metadata overrides in the Fabric dev environment, place your JSON files in `fabric/run/config/hydraulic/metadata` before starting the server.

## Links:
- Website: https://geysermc.org
- Docs: https://geysermc.org/wiki/other/hydraulic
- Download: https://geysermc.org/download?project=other-projects&hydraulic=expanded
- Discord: https://discord.gg/geysermc
- Donate: https://opencollective.com/geysermc
