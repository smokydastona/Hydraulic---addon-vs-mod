[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/613163671870242838.svg?color=%237289da&label=discord)](https://discord.gg/geysermc)

# Hydraulic-Phlodgate

<p align="center">
  <img src="https://raw.githubusercontent.com/smokydastona/Hydraulic-Phlodgate/master/.github/Phlodgate_LOGO.png" alt="Phlodgate Banner">
</p>
> **Hydraulic-Phlodgate is a fork of [GeyserMC Hydraulic](https://github.com/GeyserMC/Hydraulic).**
>
> Phlodgate's goal is to push Hydraulic further toward automatic compatibility with modded Java servers, so Bedrock players can interact with as much of a modded server as possible without requiring the server owner to manually create a Bedrock compatibility layer for every mod.

## What is Hydraulic?

[Hydraulic](https://github.com/GeyserMC/Hydraulic) is a companion mod for [Geyser](https://github.com/GeyserMC/Geyser) that allows Bedrock players to connect to modded Minecraft: Java Edition servers.

Hydraulic handles the difficult part of taking Java mod content and preparing a Bedrock-compatible representation of that content.

Phlodgate starts with that system rather than replacing it.

The basic idea is:

**Hydraulic provides the foundation. Phlodgate expands the compatibility layer around it.**

---

# What is Phlodgate?

Phlodgate is an experimental compatibility-focused fork of Hydraulic.

The main difference is that Phlodgate does more than convert resources and generate Bedrock packs. It also tries to determine **what a Java mod actually requires in order to be usable from Bedrock**.

That means looking at things such as:

* blocks
* block states
* items
* recipes
* entities
* fluids
* block entities
* menus
* models
* textures
* metadata
* interactions
* behavior requirements
* dependencies between mods
* Bedrock protocol limitations

Instead of treating every unsupported object as simply "converted" or "not converted", Phlodgate builds compatibility information around the discovered content.

The long-term goal is to make compatibility increasingly automatic.

Ideally, a server owner should be able to install a modpack, start the server, and have Phlodgate handle as much of the Bedrock compatibility work as possible.

---

# Phlodgate vs Upstream Hydraulic

Phlodgate is **not a replacement for Hydraulic**.

It is a fork that builds additional systems on top of the Hydraulic conversion and Geyser integration.

| Area                              | Upstream Hydraulic   | Phlodgate                          |
| --------------------------------- | -------------------- | ---------------------------------- |
| Geyser integration                | Yes                  | Yes                                |
| Mod resource discovery            | Yes                  | Expanded                           |
| Bedrock pack generation           | Yes                  | Yes                                |
| Registry/resource conversion      | Yes                  | Yes                                |
| Compatibility analysis            | Limited              | Expanded                           |
| Compatibility metadata            | Limited              | Extensive                          |
| Block compatibility rules         | Basic conversion     | Metadata + state-aware rules       |
| Item compatibility                | Conversion           | Analysis + behavior classification |
| Entity compatibility              | Conversion           | Analysis + runtime registration    |
| Fluid analysis                    | Limited              | Dedicated fluid analysis           |
| Menu analysis                     | Limited              | Dedicated menu analysis            |
| Block entity analysis             | Limited              | Dedicated block-entity analysis    |
| Compatibility reports             | Basic                | Detailed structured reports        |
| Runtime compatibility plan        | No equivalent system | Yes                                |
| Runtime bridge categories         | No equivalent system | Yes                                |
| Metadata patches                  | No equivalent system | Metadata V2                        |
| Lazy model loading                | Limited              | Indexed/lazy model provider        |
| Texture dependency tracking       | Limited              | Indexed dependency graph           |
| Conversion caching                | Yes                  | Expanded and dependency-aware      |
| Pack validation                   | Limited              | Dedicated validation report        |
| Automatic compatibility decisions | Limited              | Expanded                           |
| Mod-specific adapters             | Foundation           | Expanded adapter system            |
| Machine/automation behavior       | Incomplete           | In development                     |
| Fluid runtime behavior            | Incomplete           | In development                     |

The important distinction is that **Phlodgate is trying to add the compatibility and runtime layers that sit between Hydraulic's conversion pipeline and the actual behavior of a mod.**

---

# What Phlodgate Adds

## Compatibility Analysis

Phlodgate analyzes discovered mod content and records compatibility information instead of only converting the resource files.

Compatibility is currently divided into several areas:

* **Content**
* **Presentation**
* **State / Data**
* **Interaction**
* **Behavior**

Each discovered object can receive structured compatibility information including:

* support level
* confidence
* provenance
* findings
* required capabilities
* adapter bindings
* runtime requirements
* mod fingerprints

Current support levels include:

* `NATIVE`
* `AUTOMATIC`
* `ADAPTED`
* `APPROXIMATED`
* `VISUAL_ONLY`
* `UNSUPPORTED`

This makes it possible to distinguish between something that is fully usable, something that only looks correct, and something that still needs an actual runtime bridge.

---

# Metadata

Phlodgate adds a metadata system on top of Hydraulic's normal conversion process.

Metadata can describe how specific Java content should be represented on Bedrock.

For example:

```json
{
  "blocks": [
    {
      "java_id": "example:machine",
      "rules": [
        {
          "bedrock_identifier": "example:machine",
          "geometry": "minecraft:geometry.full_block",
          "material": "example:block/machine"
        }
      ]
    }
  ]
}
```

Metadata can currently describe things such as:

* Bedrock identifiers
* Java state conditions
* Bedrock state values
* geometry
* materials
* interaction prompts
* bucket textures
* behavior requirements
* behavior categories
* item mappings
* recipe mappings
* entity mappings
* menu mappings

Metadata V2 also supports patch-style changes for supported runtime data.

Machine processing can be compiled from patch facts when the runtime object exposes executable item transfer operations:

```json
{
       "patch": {
              "machine.processing.enabled": "true",
              "machine.inventory.enabled": "true",
              "machine.inventory.input_slot": "0",
              "machine.inventory.output_slot": "1",
              "machine.processing.recipe.0.input": "minecraft:stone",
              "machine.processing.recipe.0.input_count": "1",
              "machine.processing.recipe.0.output": "minecraft:iron_ingot",
              "machine.processing.recipe.0.output_count": "1",
              "machine.processing.recipe.0.duration": "20"
       }
}
```

Malformed recipe facts fail closed and do not create a machine processing bridge.

Metadata is loaded from:

```text
config/hydraulic/metadata
```

with separate areas for:

```text
builtin/
mods/
server/
user/
```

Higher-priority metadata can override lower-priority metadata.

---

# Runtime Compatibility

One of the biggest differences between Phlodgate and the original Hydraulic approach is the addition of a compiled compatibility plan.

Phlodgate takes the results of compatibility analysis and compiles them into a runtime dispatch structure.

This means runtime systems do not need to repeatedly walk the entire compatibility report looking for matching objects.

The current runtime plan covers areas including:

* block registration
* block creative exposure
* block placement
* item registration
* item creative exposure
* armor presentation
* bow presentation
* entity registration
* menu fallback handling
* block entity patch handling
* fluid compatibility lookup
* state-aware block definitions
* runtime compatibility requirements

Runtime requirements are also classified into typed bridge categories such as:

```text
MENU_CONTAINER
BLOCK_ENTITY_DATA
FLUID_RUNTIME
ITEM_BEHAVIOR
ITEM_TRANSFER
FLUID_TRANSFER
ENERGY_TRANSFER
MACHINE_INVENTORY
AUTOMATION_ACCESS
```

This is intended to give future runtime bridges a consistent place to plug into the system.

---

# Current Runtime Bridges

Some of these systems are already live rather than being report-only features.

Current work includes:

### Entity Runtime

Metadata-backed custom entities can be registered through Geyser when the compatibility result allows the entity to be represented safely.

### Menu Fallback

A metadata-defined Bedrock `ContainerType` can be used when a Java menu cannot be translated through the normal Geyser path.

This is intentionally a fallback mechanism.

It does **not** magically make an arbitrary Java machine interface work on Bedrock.

### Block Entity Data

Metadata can describe Bedrock block-entity data and copy selected values from incoming Java NBT.

For example:

```text
$java.CustomName
$java.front_text.page
$java.Items.0.Count
```

Numeric list paths are supported on both the Java source and Bedrock destination side.

### Fluid Presentation

Phlodgate currently has a fluid-adjacent bridge for bucket presentation.

A fluid can provide a configured bucket texture and the corresponding bucket item can inherit that presentation when the required Java bucket item can be resolved.

This is intentionally separate from actual fluid simulation.

### Executable Capability Bridges

The shared runtime layer now exposes fail-closed executable bridges for:

* item, fluid, and energy transfer
* machine inventory slot reads
* sided automation insertion and extraction
* generic machine processing from compiled recipe facts
* normalized container-to-tank transfers with fluid identity checks

These bridges operate against compatible Java runtime objects through the compiled dispatch table. Unsupported object shapes, missing directions, malformed recipes, and mismatched fluids do not become silent no-op behavior.

---

# Machines, Automation and Fluids

This is one of the most important parts of the project.

A machine that looks correct on Bedrock is not necessarily a machine that **works** on Bedrock.

A real modded machine may involve:

* item insertion
* item extraction
* fluid insertion
* fluid extraction
* energy transfer
* inventories
* sided inventories
* pipes
* conveyors
* machines
* processing recipes
* GUI state
* block entity state
* automation
* redstone interaction
* world interaction

Generating a Bedrock block or menu for these systems is only the presentation layer.

Phlodgate's long-term goal is to bridge the underlying behavior as well.

### Current limitation

**Universal machine, automation and deeper fluid runtime behavior are not finished.**

The current fluid system can analyze fluid content and handle some presentation-related compatibility, but it does not yet provide a universal translation layer for:

* world fluid simulation
* world fluid simulation
* universal recipe discovery
* machine-specific fluid and energy semantics
* network synchronization for custom machines
* arbitrary automation systems and filtering semantics

These are active areas of development.

Phlodgate will not claim that a machine is fully supported simply because its block and GUI can be displayed.

---

# Performance

Compatibility scanning can become expensive on large modpacks, so Phlodgate tries to avoid repeatedly doing the same work.

The current implementation includes:

* indexed mod resources
* persistent resource indexes
* compatibility caching
* conversion caching
* validation caching
* dependency-aware invalidation
* lazy model loading
* bounded model caches
* negative model caching
* texture-output caching
* indexed texture discovery
* texture dependency tracking
* demand-driven block material generation
* indexed blockstate loading
* indexed item asset loading
* compiled runtime lookup tables

The cache is stored under:

```text
config/hydraulic/cache
```

The intention is simple:

**Do the expensive work when something actually changed, not every time the server starts.**

---

# Reports

Phlodgate produces several reports under:

```text
config/hydraulic/reports
```

Including:

```text
content-inventory.json
compatibility-report.json
performance-report.json
pack-validation-report.json
```

These are useful both for debugging and for identifying where compatibility work is still missing.

The compatibility report can show:

* discovered content
* support level
* compatibility domains
* adapter bindings
* runtime requirements
* metadata findings
* confidence
* provenance
* validation results
* unsupported behavior

The performance report records measured work performed during indexing, compatibility analysis, conversion and runtime dispatch.

---

# Automatic Compatibility

The larger goal of Phlodgate is not to maintain a giant list of manually written compatibility rules forever.

The project is designed around the idea that a mod should be inspected first.

If the system can determine that something can be represented automatically, it should.

If it needs a known adapter, it should use one.

If metadata can solve the problem, metadata should be able to describe the solution.

If the Java behavior cannot currently be translated, the compatibility system should identify exactly what is missing instead of pretending the object is fully supported.

That gives the project a path toward gradually expanding compatibility without hardcoding every mod directly into the core.

---

# Mod-Specific Adapters

Some mods will always require special handling.

Phlodgate therefore supports adapter-driven compatibility rather than forcing every mod-specific implementation into the main conversion code.

The intended model is:

```text
Java Mod
   ↓
Resource / Content Discovery
   ↓
Compatibility Analysis
   ↓
Known Adapter or Generic Compatibility
   ↓
Compiled Compatibility Plan
   ↓
Runtime Bridge
   ↓
Geyser
   ↓
Bedrock
```

The generic path handles what can be handled generically.

Adapters handle the things that cannot.

---

# Compatibility With Hydraulic

Phlodgate intentionally remains closely tied to upstream Hydraulic.

Changes to Hydraulic are not automatically treated as obsolete simply because Phlodgate has additional systems.

The goal is to keep the fork recognizable and compatible with the direction of the upstream project while experimenting with features that are currently outside Hydraulic's scope.

When upstream Hydraulic improves its conversion, resource-pack, Geyser, or protocol handling, those changes are important to Phlodgate as well.

Likewise, Phlodgate's compatibility work is intended to eventually provide ideas, implementations, or proven solutions that could be useful to the wider Hydraulic ecosystem.

Upstream project:

**[GeyserMC/Hydraulic](https://github.com/GeyserMC/Hydraulic)**

This fork:

**[smokydastona/Hydraulic-Phlodgate](https://github.com/smokydastona/Hydraulic-Phlodgate)**

---

# Project Status

Phlodgate is experimental.

It should **not** currently be considered a universal compatibility solution for arbitrary modpacks.

The project already has real compatibility-analysis, metadata, caching, pack-generation, validation, and runtime-bridge infrastructure, but there are still major areas to solve.

The largest remaining problems are behavior-heavy systems such as:

* machines
* automation
* item transfer
* fluid transfer
* energy systems
* complex menus
* complex block entities
* mod-specific interactions

Those systems require actual runtime bridges rather than additional reporting alone.

---

# The Goal

The end goal is straightforward:

**Make modded Java Minecraft as accessible as possible to Bedrock players.**

Ideally:

```text
Install Java mods
       ↓
Start server
       ↓
Phlodgate discovers the mods
       ↓
Phlodgate analyzes what they contain
       ↓
Known compatibility is applied
       ↓
Generic compatibility is applied where possible
       ↓
Bedrock resources are generated
       ↓
Runtime bridges handle supported behavior
       ↓
Bedrock players join
```

The closer Phlodgate can get to that workflow without requiring a server owner to manually configure every mod, the better.

---

# Development

Phlodgate is being developed as an experimental extension of Hydraulic.

The project is intentionally focused on solving the difficult parts of modded Java → Bedrock compatibility rather than simply making unsupported content look correct.

If something cannot actually work yet, the compatibility system should say so.

If something can be adapted, there should be a defined path for doing it.

And when a new runtime bridge is added, it should become part of the same compatibility pipeline instead of another isolated special case.

---

## Credits

Phlodgate is based on **Hydraulic**, developed by the **GeyserMC** project and contributors.

Hydraulic exists to allow Bedrock players to join modded Java Edition servers through Geyser.

Phlodgate builds on that foundation and focuses specifically on expanding automatic compatibility, compatibility analysis, metadata-driven adaptation, and runtime bridging.

* [GeyserMC](https://github.com/GeyserMC)
* [Hydraulic](https://github.com/GeyserMC/Hydraulic)


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
