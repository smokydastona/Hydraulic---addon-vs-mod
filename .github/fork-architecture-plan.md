# Hydraulic Fork Architecture Plan

## Goal
Turn this fork from a narrow override layer into a general compatibility system that can support the maximum practical number of Java mods for Bedrock players with the least possible per-mod hand work.

The target is not:

- block-only JSON overrides
- a pile of per-mod special cases
- a second full resource manager that keeps the whole modpack resident forever
- a fake compatibility score that hides broken behavior
- a Bedrock behavior-pack-first design that ignores what Geyser actually supports

The target is:

- one discovery pass over mod and resource roots
- one authoritative index shared by compatibility, conversion, validation, and caching
- typed compatibility data and compiled runtime plans
- automatic resource generation for the visual layer
- Hydraulic runtime bridges for interaction and behavior where assets are insufficient
- generic capability adapters before mod-specific adapters
- explicit reporting, provenance, confidence, and degradation evidence

The long-term scaling strategy remains:

1. automatic translation
2. metadata correction and override
3. generic capability adapter
4. mod-specific adapter
5. explicit unsupported result when gaps remain

## Ground Truth Snapshot

### Date
- 2026-09-08

### Validated repo baseline
- Live fork: `smokydastona/Hydraulic--Skeleton_Key`
- Current workspace branch tracks Minecraft `26.2`
- Current workspace branch tracks Java `25`
- The live repository is the authority over older notes that still mention `1.20.1` or Java `17`

### Validated code and runtime scope
- `shared/src/main/java/org/geysermc/hydraulic/pack/PackManager.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/ModResourceIndex.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/PackUtil.java`
- `shared/src/main/java/org/geysermc/hydraulic/block/BlockPackModule.java`
- `shared/src/main/java/org/geysermc/hydraulic/item/ItemPackModule.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/*`
- `shared/src/main/java/org/geysermc/hydraulic/compat/*`
- `shared/src/test/java/org/geysermc/hydraulic/metadata/MetadataLoaderTest.java`
- `shared/src/test/java/org/geysermc/hydraulic/compat/MappingResolverTest.java`
- `fabric/run/config/hydraulic/metadata/*`
- `fabric/run/config/hydraulic/reports/*`

### Validated Geyser constraints
- Geyser is the correct foundation for Bedrock pack delivery.
- Geyser can register Bedrock resource packs and deliver them to clients.
- Geyser does not provide a normal server-side Bedrock behavior-pack or add-on execution model that Hydraulic can treat as its primary behavior architecture.
- Hydraulic should therefore treat generated Bedrock resource packs as first-class and generated Bedrock behavior content as optional and secondary to Hydraulic runtime bridges.
- Public Geyser APIs already help with resource packs and custom block, item, and entity registration, but menu and block-entity runtime integration remains constrained and may require carefully targeted seams rather than broad assumptions.

## Executive Verdict

The architecture is directionally correct, but the main performance problem is not one isolated slow method.

The main problem is duplicated discovery and repeated parsing across multiple subsystems.

Today the same mod can be touched repeatedly by:

- `ModResourceIndex`
- `CompatibilityManager` asset scanning
- `PackUtil.getModUUID()` tree hashing
- resource-pack readers
- model indexing
- actual conversion

That must be replaced with:

```text
                 MOD / RESOURCE ROOTS
                         |
                         v
              +-----------------------+
              | UNIVERSAL INDEX       |
              |                       |
              | files                 |
              | resources             |
              | registries            |
              | models                |
              | textures              |
              | blockstates           |
              | data                  |
              | fingerprints          |
              | dependencies          |
              +-----------+-----------+
                          |
             +------------+------------+
             |            |            |
             v            v            v
      COMPATIBILITY   CONVERSION   VALIDATION
         ANALYSIS       ENGINE       ENGINE
             |            |            |
             +------------+------------+
                          |
                          v
                 GENERATED ARTIFACTS
                          |
                    PERSISTENT CACHE
                          |
                          v
                     GEYSER / BEDROCK
```

One discovery pass. One source of truth. Aggressive caching. Lazy parsing. Bounded memory. Parallel work only when data shows it helps.

## Locked Architecture

```text
                         HYDRAULIC
                             |
                      Mod Discovery
                             |
                             v
                   +--------------------+
                   | UNIVERSAL INDEX    |
                   +---------+----------+
                             |
          +------------------+------------------+
          |                  |                  |
          v                  v                  v
    Registry Facts     Resource Facts      Runtime Facts
          |                  |                  |
          +------------------+------------------+
                             |
                             v
                  +-----------------------+
                  | DISCOVERY IR          |
                  +-----------+-----------+
                              |
                              v
                  +-----------------------+
                  | COMPATIBILITY IR      |
                  +-----------+-----------+
                              |
          +-------------------+-------------------+
          |                   |                   |
          v                   v                   v
     Automatic           Knowledge           Adapter
    Translators           Engine              Engine
          |                   |                   |
          +-------------------+-------------------+
                              |
                              v
                  +-----------------------+
                  | COMPILED              |
                  | COMPATIBILITY PLAN    |
                  +-----+-----------+-----+
                        |           |
                        |           +---------------------+
                        |                                 |
                        v                                 v
              +------------------+             +------------------+
              | RESOURCE IR      |             | BRIDGE IR        |
              +--------+---------+             +--------+---------+
                       |                                |
                       v                                v
              Bedrock Resource Pack           Hydraulic Runtime Bridges
                       |                                |
                       +---------------+----------------+
                                       |
                                       v
                               Validation / QA
                                       |
                                       v
                           Content-Addressed Artifact Cache
                                       |
                                       v
                              Geyser Pack Delivery
                                       |
                                       v
                                 Bedrock Player
```

The compatibility layer stays above the current Hydraulic conversion pipeline, but it must stop being a collection of independent rediscovery passes.

## Guiding Principles
- Keep the current Hydraulic conversion pipeline as the base engine.
- Make the live repo and runtime artifacts authoritative over stale notes.
- Fix root-cause duplication before broadening adapter count.
- Keep compatibility semantics correct before optimizing hot paths.
- Prefer one indexed source of truth over repeated filesystem walks.
- Prefer typed IR and compiled runtime plans over flexible runtime interpretation.
- Treat metadata as an override and patch layer, not the primary implementation strategy.
- Prefer shared compatibility logic in `shared/` over platform-specific logic.
- Prefer graceful degradation over aborting a full conversion run.
- Separate presentation compatibility from interaction, behavior, and network compatibility.
- Keep Bedrock resource generation first-class and Bedrock behavior generation optional.
- Make runtime dispatch identifier-driven and approximately `O(1)`.
- Bound memory, not just thread count.
- Prove optimization claims with artifacts and measurements.

## Current State Summary

### What is already implemented
- Pack orchestration still centers correctly in `PackManager`.
- A real `compat` foundation already exists under `shared/`.
- Deterministic metadata loading and precedence already exist.
- The first universal-index seam is now live: `ModResourceIndex` indexes both asset and data inventory categories, and `CompatibilityManager` reuses that indexed data instead of doing its own second mod-root filesystem walk for content inventory generation.
- Pack identity now also uses the shared indexed view: full-tree `PackUtil.getModUUID()` hashing has been replaced by a persisted `ConversionKey` derived from indexed mod resources plus loaded metadata state, so metadata-only changes now invalidate stale cached packs.
- A first-class cache layout now exists under `config/hydraulic/cache` for index, compatibility, conversion, validation, and manifest artifacts. Compatibility inventory and report output can now be reused from cache when indexed mod fingerprints and metadata state are unchanged, while conversion and validation outputs are mirrored into the same artifact tree.
- Model lookup is no longer built from an eager all-model startup flattening pass. `ModResourceIndex` now records generic model file paths, and `IndexedModelProvider` lazily deserializes mod and vanilla models on demand through a bounded cache with negative caching for missing entries.
- The cached index snapshot now rehydrates live `ModResourceIndex` instances on repeat startup when mod roots, indexed files, and indexed directories are unchanged, so the index cache is now a real execution shortcut rather than metrics-only persistence.
- Texture output resolution for item, bow, and block conversion no longer recomputes identical Bedrock texture paths at every call site. A shared bounded `TextureResolutionCache` now sits behind `TexturePackModule` and records hit, miss, eviction, and size evidence in `performance-report.json`.
- Texture conversion now also builds a per-pack dependency graph from converted models and equipment assets before the texture stage runs. The converter records discovered, selected, and omitted texture counts per conversion batch, accumulates the selected texture set across multiple extraction passes for the same mod, and keeps block-texture registration aligned with that aggregated set.
- Block texture registration no longer walks `ResourcePack#textures()` eagerly during post-processing or scans the full indexed texture map for the mod. Hydraulic now resolves only the selected texture keys back through `ModResourceIndex`, and block flipbook registration lazily reads only selected texture `.mcmeta` files when animation metadata is actually needed.
- Pack invalidation is no longer limited to a mod's own indexed fingerprint plus metadata. `ModResourceIndex` now records external namespaces referenced by model and equipment assets, and `ConversionKey` now folds the transitive fingerprints of dependent indexed mods into the persisted cache identity.
- Typed compatibility data already exists:
  - `CompatibilityObject`
  - `CapabilityProfile`
  - `SupportResult`
  - `CompatibilityFinding`
  - `Confidence`
  - `Provenance`
  - `ModFingerprint`
- Analyzer-backed compatibility reporting already exists for blocks, items, recipes, entities, menus, fluids, and block entities.
- `MappingResolver` already carries compact resolved block metadata for current block runtime consumers.
- `MetadataIndex` already precompiles compact menu and block-entity patch templates.
- Hydraulic already emits `content-inventory.json`, `compatibility-report.json`, `performance-report.json`, and `pack-validation-report.json`.
- Eager pack preparation already happens during Hydraulic startup, so conversion evidence survives later Geyser bootstrap or HTTPS failures.
- Post-generation pack validation findings are now merged into `compatibility-report.json` as a `packValidation` section after pack preparation, so manual actions and validation issues are visible next to compatibility findings instead of only in the sibling `pack-validation-report.json` artifact.
- Current runtime consumers already use compatibility decisions for block registration, item registration and exposure, armor and bow attachables, and metadata-backed custom entity registration.
- Current runtime bridges already include explicit metadata-backed menu fallback and block-entity patch translation at real Geyser seams.
- Those live menu and block-entity bridge factories now also require explicit compiled adapter bindings, so runtime instantiation follows the same capability-driven adapter selection surfaced in `compatibility-report.json`.
- Unsupported menu-open and block-entity fallback diagnostics now also consume precompiled runtime-dispatch candidate indexes instead of rescanning the full compatibility report during live warnings.
- Unsupported menu-open handling now also preserves the resolved live Java menu identifier across the mixin, fallback, and warning paths, so the runtime warning can bind directly to the matched compiled menu plan and its explicit menu bridge requirements instead of only reporting a container type plus global candidates.
- State-aware block identifier grouping and per-state runtime metadata now also compile into the runtime dispatch table, so block registration and block-item placement no longer need to re-derive those mappings through `MappingResolver` when compiled entries already exist.
- The remaining block-item texture fallback decision path now also consumes compiled block compatibility plans instead of looking raw objects back up from the compatibility report during conversion.
- Post-generation pack validation now also consumes the texture dependency graph's concrete selected texture set, so `pack-validation-report.json` can structurally fail missing generated texture outputs and warn on leftover unreferenced texture files instead of only validating archive shape.
- Conversion invalidation now also preserves explicit indexed model and equipment dependency edges and hashes only the concrete referenced model and texture file stamps they traverse, so unrelated asset churn in a dependent namespace no longer invalidates another mod's cached pack output.

### What is materially better than earlier assessments
- The fork is no longer only a block metadata experiment.
- Compatibility reporting is already a real regression surface.
- Runtime bridge seams now exist in production code, even if they remain narrow.
- Validation artifacts now exist after pack generation.
- Performance artifacts now include real cache evidence for some hot paths.
- Live Fabric runtime validation now shows the model-provider startup slice is effectively reduced to indexed setup cost rather than eager model deserialization; the current run recorded `modelIndexBuildMillis = 6` while pack conversion and Geyser registration still completed.
- The runtime artifact now also records lazy model-provider hit, miss, eviction, size, and indexed-model counts, so bounded on-demand model behavior is measurable instead of inferred.
- Live Fabric runtime validation now also shows real index rehydration on unchanged startup; the current dev run reported `artifactCache.index.hits = 2` and `misses = 2`, which matches partial reuse for filesystem-backed mod roots while dev-time virtual roots safely fall back to rebuild.
- Live Fabric runtime validation now also shows shared texture-resolution cache reuse during conversion; the current dev run reported `textureResolutionCache.hits = 5`, `misses = 15`, and `evictions = 0`, proving repeated item, bow, and block texture-output resolution is now observable and already benefits from central reuse.
- Live Fabric runtime validation now also shows the texture dependency graph is active in the conversion path. The current bundled test mod reported `discoveredTextures = 17`, `selectedTextures = 17`, and `omittedTextures = 0`, which means the graph is wired correctly even though this small fixture pack currently references every discovered texture.
- Live Fabric runtime validation now also confirms the structural texture-coverage check matches the current fixture pack: after conversion, both `hydraulic` and `hydraulic_test_mod` were `valid = true` in `pack-validation-report.json`, with no missing selected textures and no leftover unreferenced texture files.
- Live Fabric runtime validation now also confirms that the first broader indexed texture consumer is real rather than theoretical: pack conversion, block registration, and report generation still complete with the new indexed texture path plus lazy animation-metadata flow, and the runtime artifact remains stable with `selectedTextures = 17` and `textureResolutionCache.hits = 5` on the current fixture mod.
- Focused `TextureDependencyGraphTest` coverage now also proves the selected-texture set is retained across multiple extraction passes for the same mod, closing a real multi-root correctness gap that would otherwise drop earlier root selections before validation and block post-processing.
- Live Fabric runtime validation now also confirms the dependency-aware invalidation slice is live on the real storage path: Hydraulic rewrote per-mod `conversion-key.json` files with `HYDRAULIC_CONVERSION_KEY_V3`, persisted `dependencyFingerprint` and `dependentModCount`, and safely forced reconversion after the cache identity expanded to explicit resource-edge hashing.

### What is still too narrow
- Discovery is still duplicated across multiple subsystems.
- Fingerprinting and cache invalidation are still not yet resource-kind-aware inside a mod, but cross-mod conversion invalidation is now materially better than the earlier per-mod-only key.
- Resource-pack reading and broader resource resolution are still too eager even though model loading is now lazy and bounded.
- Texture-path reuse is now centralized and measured, texture conversion now consults a real dependency graph, block-texture post-processing now uses indexed texture paths plus lazy animation metadata reads, and conversion invalidation now follows indexed cross-mod dependencies. The remaining gap is that the current fixture packs still reference every discovered texture, and other resource categories still have eager seams.
- Runtime dispatch is now identifier-driven for the shipped bridge seams, unsupported diagnostics, and the first block-state registration paths, but transfer-heavy paths and deeper behavior surfaces still have too much flexible runtime reasoning.
- Compatibility analysis still reconstructs facts too often and still depends on repeated asset discovery.
- Non-block compatibility remains shallower than the block path.
- Fluids, machines, transfer systems, richer menu behavior, entity interaction, custom networking, and custom rendering analysis remain incomplete.
- There is still no universal compiled runtime plan that removes compatibility reasoning from hot paths.

## Primary Architectural Correction

The future architecture should not be described primarily as "more analyzers plus more metadata".

The correct architectural correction is:

1. unify discovery
2. formalize intermediate representations
3. compile compatibility into runtime plans
4. cache intermediate and final artifacts
5. only then widen behavior and adapter breadth

The key rule is:

```text
analyze once
compile once
dispatch directly at runtime
```

## Compatibility Domains

The engine should evaluate each object across six domains, not one flattened compatibility status.

```text
HYDRAULIC COMPATIBILITY ENGINE
|
+-- 1. CONTENT
|   +-- blocks
|   +-- items
|   +-- entities
|   +-- fluids
|   +-- block entities
|
+-- 2. PRESENTATION
|   +-- models
|   +-- textures
|   +-- animations
|   +-- particles
|   +-- sounds
|
+-- 3. STATE / DATA
|   +-- block states
|   +-- item components
|   +-- NBT and custom data
|   +-- tags
|   +-- recipes
|
+-- 4. INTERACTION
|   +-- placement
|   +-- breaking
|   +-- use
|   +-- containers
|   +-- GUIs
|   +-- machines
|   +-- entity interactions
|
+-- 5. BEHAVIOR
|   +-- ticking
|   +-- automation
|   +-- fluid handling
|   +-- energy handling
|   +-- server-side rules
|   +-- generic capability adapters
|   +-- mod-specific adapters
|
+-- 6. NETWORK / PROTOCOL
    +-- custom packets
    +-- custom synchronization
    +-- clientbound state requirements
    +-- serverbound interaction requirements
```

This separation is fundamental. A converted model is not proof of compatibility.

## Universal Index

The current `ModResourceIndex` should evolve into a universal, authoritative index shared across the entire pipeline.

Suggested package direction:

```text
org.geysermc.hydraulic.pack.index
  UniversalResourceIndex
  ModIndex
  ResourceFile
  ResourceKind
  ResourceNamespace
  ResourceFingerprint
  ResourceDependency
  IndexBuilder
  IndexCache
  IndexVersion
```

Suggested `ResourceKind` coverage:

```text
BLOCKSTATE
ITEM_DEFINITION
ITEM_MODEL
MODEL
TEXTURE
ANIMATION
SOUND
LANG
RECIPE
TAG
LOOT_TABLE
ENTITY
PARTICLE
FONT
SHADER
GEOMETRY
DATA
UNKNOWN
```

Each indexed resource should track at least:

```text
namespace
relative path
kind
source root
size
last modified
fingerprint
dependencies
```

Each `ModIndex` should eventually expose:

- registry index
- resource index
- blockstate index
- item-definition index
- item-model index
- model index
- texture index
- recipe index
- entity index
- menu index
- block-entity index
- dependency graph
- fingerprint
- compatibility facts

Every subsystem that currently rescans disk should consume this index instead.

## Discovery IR

Hydraulic needs a formal IR boundary between discovery and compatibility decisions.

```text
Java resources and registries
  -> Discovery IR
  -> Compatibility IR
  -> Compiled Compatibility Plan
  -> Resource IR / Bridge IR
```

The discovery layer should gather facts without making Bedrock decisions.

Example direction:

```text
JavaBlockIR
  -> identifier
  -> owning mod
  -> state definition
  -> default state
  -> model references
  -> texture references
  -> collision facts
  -> selection facts
  -> block-entity facts
  -> tags
  -> interaction facts
  -> capability hints
  -> dependencies
```

The same pattern should exist for items, entities, fluids, menus, recipes, and block entities.

## Compatibility IR

The next major shift remains the move from raw mapping tables to typed compatibility objects, but the IR boundary must become explicit.

```text
CompatibilityObject
  -> content type
  -> Java identifier
  -> owning mod
  -> mod fingerprint
  -> discovery facts
  -> capability profile
  -> analyzer findings
  -> metadata patches
  -> adapter bindings
  -> runtime requirements
  -> support results
  -> confidence
  -> provenance
  -> degradation actions
  -> reasons
```

Every object should answer:

1. What capabilities does it require?
2. Which capabilities are automatically representable on Bedrock?
3. Which gaps can metadata patches close?
4. Which gaps can generic capability adapters close?
5. Which gaps require a mod-specific adapter?
6. Which gaps are impossible or not worth simulating?

## Compiled Compatibility Plan

The compatibility layer should disappear from hot runtime paths.

Compatibility analysis belongs to startup and conversion time.
Runtime should execute compiled plans.

Add:

```text
CompiledCompatibilityPlan
  -> object identifier
  -> presentation plan
  -> placement plan
  -> interaction plan
  -> behavior plan
  -> network plan
  -> block-entity plan
  -> container plan
  -> resource-pack references
  -> adapter references
  -> fallback plan
  -> support level
  -> critical failures
  -> confidence
```

Example:

```text
create:mechanical_press

presentation:
  GENERATED_CUSTOM_BLOCK

placement:
  GEYSER_CUSTOM_BLOCK

interaction:
  MACHINE_CONTAINER_BRIDGE

behavior:
  CREATE_KINETIC_ADAPTER

block_entity:
  CREATE_BLOCK_ENTITY_BRIDGE

resource_pack:
  pack-7a82...

confidence:
  0.97
```

This is the object runtime should consult, not flexible metadata or late analyzer logic.

## Runtime Dispatch Architecture

Current runtime dispatch should evolve from "ask every module for every event" to direct lookup.

Target:

```text
Java event
  -> extract identifier
  -> lookup compiled runtime plan
  -> execute bridge or fallback
```

Add:

```text
RuntimeDispatchTable
  block id       -> BlockRuntimePlan
  item id        -> ItemRuntimePlan
  entity id      -> EntityRuntimePlan
  menu id        -> MenuRuntimePlan
  block entity   -> BlockEntityRuntimePlan
  fluid id       -> FluidRuntimePlan
```

Each runtime plan should already contain:

- translator
- adapter
- fallback
- support level
- requirements

This is one of the highest-value runtime optimizations in the fork.

## Persistent Fingerprints And Conversion Keys

The current full-tree hashing approach in `PackUtil.getModUUID()` is too expensive for routine invalidation.

Replace it with persistent incremental fingerprints.

For directories, fingerprint from:

- relative path
- file count
- size
- last modified
- content hash only when metadata shows change

For jars, fingerprint from:

- size
- last modified
- sha-256

Persist something like:

```json
{
  "mod": "create",
  "fingerprint": "...",
  "algorithm": "HYDRAULIC_INDEX_V2",
  "fileCount": 18342
}
```

The cache key must become more precise than "mod changed or not".

Add:

```text
ConversionKey
  -> mod fingerprint
  -> hydraulic version
  -> converter version
  -> metadata fingerprint
  -> target Minecraft version
  -> target Bedrock version
  -> Geyser version
  -> generator schema version
  -> relevant config fingerprint
```

Same key means cache hit and no reconversion.

## Persistent Artifact Cache

Make the cache a first-class subsystem.

Suggested layout:

```text
config/hydraulic/
  cache/
    index/
    compatibility/
    models/
    textures/
    conversions/
    validation/
    manifests/
```

Suggested service surface:

```text
ArtifactCache
  get(key)
  put(key, artifact)
  contains(key)
  invalidate(key)
  invalidateMod(modId)
```

Do not cache only the final pack zip. Cache expensive intermediate results too.

## Lazy Loading And Bounded Memory

Current resource loading is too eager for large modpacks.

Target lifecycle:

```text
INDEX
  -> LOAD ONLY WHAT IS NEEDED
  -> CONVERT
  -> GENERATE
  -> VALIDATE
  -> WRITE CACHE
  -> RELEASE
```

Do not keep the entire modpack resident after generation just because it was loaded once.

### Model system direction
- Replace all-models-resident behavior with a `ModelIndex` of paths and dependencies.
- Parse model JSON on demand.
- Cache parsed models in a bounded LRU cache.
- Resolve parent-model closure lazily and cache resolved closures.

### Texture system direction
- Build a texture dependency graph.
- Convert only textures required by converted models.
- Deduplicate atlas membership.
- Avoid converting every texture simply because it exists.

### Block-state direction
- Keep metadata authoring human-readable.
- Compile runtime block-state resolution into compact tables.
- Prefer state ordinals or packed state keys over string-heavy hot-path evaluation.

## Data-Oriented Compatibility Engine

The compatibility engine should stop reconstructing facts independently for each analysis phase.

For each object:

```text
object
  -> fact collection
  -> capability inference
  -> domain analysis
  -> decision
  -> compiled plan
```

Do not keep a future design where every object linearly searches all analyzers.

Add:

```text
AnalyzerRegistry
  block        -> BlockAnalyzer
  item         -> ItemAnalyzer
  entity       -> EntityAnalyzer
  fluid        -> FluidAnalyzer
  menu         -> MenuAnalyzer
  blockentity  -> BlockEntityAnalyzer
```

That avoids repeated `supports(...)` scans and makes analyzer routing explicit.

## Capability Model

The capability layer remains central, but it must drive both compatibility decisions and compiled runtime planning.

```text
org.geysermc.hydraulic.compat.capability
  CapabilityAnalyzer
  CapabilityProfile
  Capability
  CapabilityRequirement
  CapabilityResult
  CriticalCapability
  CapabilityWeightProfile
```

Example shape:

```text
Create: crushing_wheel

visual:
  model              YES
  texture            YES
  animation          YES

world:
  placeable          YES
  breakable          YES
  directional        YES
  waterloggable      NO

interaction:
  right_click        YES
  inventory          YES
  gui                YES

behavior:
  ticking            YES
  kinetic_system     YES
  redstone           YES
  entity_interaction NO

data:
  block_entity       YES
  custom_data        YES

network:
  custom_packets     NO
  custom_sync        YES
```

## Multidimensional Support Results

Do not store only one support level.

Store domain-level and capability-level support first, then derive the overall result.

Example:

```text
overall      = ADAPTED
visual       = COMPLETE
placement    = COMPLETE
state        = COMPLETE
interaction  = PARTIAL
inventory    = COMPLETE
behavior     = PARTIAL
network      = PARTIAL
audio        = COMPLETE
```

The final report may still expose a single overall status, but it must be derived from richer support results and critical-capability rules.

## Compatibility Taxonomy

Keep coarse compatibility status values where useful, but add a final support vocabulary for human-facing results:

- `NATIVE`
- `AUTOMATIC`
- `ADAPTED`
- `APPROXIMATED`
- `VISUAL_ONLY`
- `UNSUPPORTED`

Also add explicit degradation actions for implementation planning and reporting:

- `APPROXIMATE`
- `SIMPLIFY`
- `SCRIPT`
- `STUB`
- `OMIT`

These do not replace support results. They explain how Hydraulic degraded behavior.

## Compatibility Score And Critical Capability Rules

Add a machine-readable score, but never let the score overrule a missing critical capability.

Example weighting direction:

```text
Presentation   10%
Placement      10%
State          10%
Interaction    25%
Behavior       25%
Data           10%
Network        10%
```

Object classes may need different weight profiles.

Examples:

- machines should weight behavior, container semantics, and transfer heavily
- decorative blocks should weight presentation and placement heavily
- entities should weight behavior and interaction heavily

Add critical-capability rules such as:

```text
machine:
  processing = CRITICAL
  inventory  = CRITICAL
  visual     = NON_CRITICAL
```

If critical behavior is missing, the result must remain `UNSUPPORTED` or `VISUAL_ONLY` even when presentation looks good.

## Metadata Evolution Plan

### Current state
Keep deterministic recursive discovery and ownership precedence.

### Next structure

```text
config/hydraulic/metadata/
  builtin/
  mods/
  server/
  user/
```

### Design rule
Metadata is a patch layer over automatic analysis, not the primary mapping source.

Example direction:

```json
{
  "target": "create:andesite_casing",
  "patch": {
    "visual.geometry": "...",
    "state.facing": "...",
    "interaction.use": "create:casing_use"
  }
}
```

### Generated versus manual separation

```text
config/hydraulic/
  generated/
  metadata/
    builtin/
    mods/
    server/
    user/
```

Generated compatibility suggestions must never overwrite server or user intent.

### Compilation rule
Flexible authoring metadata must compile into compact runtime metadata.

Target flow:

```text
Metadata JSON
  -> validated metadata
  -> flexible metadata IR
  -> compiled runtime metadata
  -> compiled compatibility plan
```

Runtime representations should prefer:

- enums
- integer ids
- bit flags
- direct references
- compact arrays

Runtime should avoid:

- generic patch maps
- JSON objects
- string-keyed interpretation in hot paths

## Behavior Fact Extraction

The biggest remaining compatibility leap is not arbitrary Java bytecode translation.

It is behavior fact extraction.

Hydraulic should get better at answering "how does this object behave" by extracting facts such as:

- ticks
- has block entity
- opens menu
- has inventory
- accepts items
- produces items
- consumes items
- uses fluids
- uses energy
- reacts to redstone
- changes state
- spawns entities
- plays sounds
- emits particles
- uses custom networking
- uses server-only logic
- uses custom rendering

Those facts should feed the capability model and the compiled runtime plan.

## Generic Machine, Container, And Transfer Model

This is the highest-return compatibility layer after the performance substrate.

### Machine profile direction

```text
MachineProfile
  inventory
    -> input slots
    -> output slots
    -> fuel slots
    -> upgrade slots

  processing
    -> recipes
    -> duration
    -> progress
    -> outputs

  fluids
    -> input tanks
    -> output tanks

  energy
    -> input
    -> output
    -> capacity

  state
    -> active
    -> progress
    -> orientation

  interaction
    -> menu
    -> block interaction

  automation
    -> sided insertion
    -> sided extraction
    -> filtering
    -> redstone
```

### Transfer capabilities
- `ItemTransfer`
- `FluidTransfer`
- `EnergyTransfer`

### Container direction
Infer container archetypes before writing mod-specific menu adapters.

Examples:

- `CHEST`
- `DOUBLE_CHEST`
- `FURNACE`
- `CRAFTING`
- `PROCESSOR`
- `MACHINE`
- `STORAGE`
- `ENERGY_MACHINE`
- `FLUID_MACHINE`
- `CUSTOM_GRID`

Also map slot semantics, not just slot numbers:

- `INPUT`
- `OUTPUT`
- `FUEL`
- `UPGRADE`
- `FLUID_INPUT`
- `FLUID_OUTPUT`
- `CATALYST`

This is how Hydraulic avoids writing hundreds of UI adapters for machines that share the same semantics.

## Fluids, Entities, Networking, And Rendering

### Fluids
Fluids remain a major missing subsystem.

Hydraulic needs:

- `FluidTranslator`
- `FluidBridge`
- fluid block representation
- bucket and item representation
- tank representation
- transfer semantics
- machine interaction semantics

### Entities
Treat entities as three layers:

- presentation
- interaction
- behavior

Hydraulic should not attempt arbitrary AI translation. It should infer and map a generic AI vocabulary when possible.

Examples:

- wander
- follow
- attack
- flee
- guard
- look_at
- trade
- pickup
- work
- breed

### Networking
Networking is its own compatibility domain.

Detect and report:

- custom packets
- custom synchronization
- clientbound state requirements
- serverbound interaction requirements

Unknown or essential custom network behavior should escalate compatibility risk automatically.

### Custom rendering
Detect:

- block entity renderers
- entity renderers
- item renderers
- custom shaders
- custom vertex pipelines

Then classify:

- standard renderer -> automatic
- known renderer pattern -> generic adapter
- unknown custom renderer -> approximation or unsupported

## Geyser Integration Strategy

Hydraulic should explicitly build on Geyser rather than trying to recreate it.

The architecture should be:

```text
Geyser knowledge
  + Hydraulic conversion
  + Hydraulic compatibility engine
  + Hydraulic runtime bridges
  + optional generated Bedrock behavior content
```

not:

```text
Hydraulic recreates Geyser
```

### Important correction
Do not make a generated Bedrock behavior pack the foundation of the project.

The primary behavior mechanism should be:

```text
Java server authoritative state
  -> Hydraulic semantic translation and runtime bridges
  -> Geyser transport and Bedrock-facing registration
  -> Bedrock client
```

Generated Bedrock behavior content can still exist where useful, but it is secondary to the runtime bridge architecture.

### Pack delivery
Automatic Bedrock resource-pack delivery is realistic and should stay in scope.

Target:

```text
modpack installed
  -> Hydraulic converts
  -> pack generated
  -> content-addressed artifact cached
  -> Geyser registers pack or URL
  -> Bedrock player downloads automatically
```

Consider incremental pack structure later:

- `hydraulic-core.mcpack`
- `create.mcpack`
- `mekanism.mcpack`
- optional feature packs

But design this around actual Geyser resource-pack behavior, not assumptions borrowed from Java resource packs.

## Reporting And Validation Model

The report should answer three things simultaneously:

1. What works.
2. How well it works across each domain.
3. Why support is partial or missing.

### Report modes
- production mode
- diagnostic mode
- deep-audit mode

Suggested outputs:

- `compatibility-summary.json`
- `compatibility-report.json`
- deep-audit inventory and dependency artifacts only when explicitly requested

### Validator role
Validation should remain a separate stage after generation.

It should emit structured:

- `errors`
- `warnings`
- `manualActions`

Validation evidence should feed follow-up work for metadata, bridges, generators, and adapters.

## Performance And Observability

The current performance report is useful, but it should evolve into a profiler-lite evidence system.

Target structure:

```text
startup
  discovery
  indexing
  metadata
  compatibility
  cache load

conversion
  per mod
    parsing
    models
    textures
    blocks
    items
    entities
    packaging
    validation

runtime
  bridge calls
  translation calls
  dispatch lookups
  cache hits
  cache misses

memory
  peak heap
  index memory
  model cache
  texture cache
```

Capture when practical:

- count
- total time
- average time
- `p50`
- `p95`
- `p99`
- cache hits
- cache misses

Add stage-level metrics for:

- resource index
- model resolver
- texture resolver
- block-state resolver
- metadata resolver
- compatibility analyzer
- conversion artifact cache
- runtime dispatch

Optimization should be evidence-driven, not intuition-driven.

## Conversion Scheduler

The current thread-pool heuristic is acceptable as a first pass, but the long-term system should schedule conversion work by cost and memory budget.

Target:

```text
ConversionScheduler
  -> max workers
  -> memory budget
  -> queue
  -> priority
  -> estimated cost
```

The scheduler should optimize throughput per memory and I/O budget, not just raw thread count.

Bound memory as well as concurrency.

## Proposed Package Direction

### Keep current high-level classes

```text
org.geysermc.hydraulic.compat
  CompatibilityManager
  CompatibilityRegistry
  CompatibilityReport
  ContentInventory
  MappingResolver
  MappingOwnership
```

### Add or evolve explicit subpackages

```text
org.geysermc.hydraulic.pack.index
  UniversalResourceIndex
  ModIndex
  ResourceFile
  ResourceKind
  ResourceFingerprint
  ResourceDependency
  IndexBuilder
  IndexCache

org.geysermc.hydraulic.compat.analysis
  AnalyzerRegistry
  RegistryAnalyzer
  BlockAnalyzer
  ModelAnalyzer
  StateAnalyzer
  ItemAnalyzer
  ComponentAnalyzer
  EntityAnalyzer
  FluidAnalyzer
  MenuAnalyzer
  BlockEntityAnalyzer
  NetworkAnalyzer
  RenderAnalyzer

org.geysermc.hydraulic.compat.capability
  CapabilityAnalyzer
  CapabilityProfile
  Capability
  CapabilityRequirement
  CapabilityResult
  CriticalCapability
  CapabilityWeightProfile

org.geysermc.hydraulic.compat.model
  CompatibilityObject
  SupportResult
  CompatibilityFinding
  Confidence
  Provenance
  ModFingerprint
  DegradationAction

org.geysermc.hydraulic.compat.ir
  DiscoveryIr
  CompatibilityIr
  ResourceIr
  BridgeIr
  CompiledCompatibilityPlan

org.geysermc.hydraulic.compat.knowledge
  CompatibilityKnowledge
  KnowledgeEntry
  KnowledgeSource
  PatternClassifier

org.geysermc.hydraulic.compat.mapping
  ContentPatch
  PatchCompiler
  CompiledPatch
  StateTranslator
  ModelClassifier
  GeometryResolver
  ItemTranslator
  FluidTranslator
  ContainerTranslator

org.geysermc.hydraulic.compat.runtime
  RuntimeDispatchTable
  BlockRuntimePlan
  ItemRuntimePlan
  EntityRuntimePlan
  MenuRuntimePlan
  BlockEntityRuntimePlan
  FluidRuntimePlan

org.geysermc.hydraulic.compat.bridge
  InteractionBridge
  ContainerBridge
  BlockEntityBridge
  FluidBridge
  EntityBridge
  NetworkBridge

org.geysermc.hydraulic.compat.generator
  ResourcePackGenerator
  OptionalBehaviorGenerator
  ReportGenerator
  ValidationArtifactWriter

org.geysermc.hydraulic.compat.adapter
  ModAdapter
  CapabilityAdapter
  AdapterBinding

org.geysermc.hydraulic.cache
  ArtifactCache
  ConversionKey
  FingerprintService
```

The exact package names may move, but the architectural boundaries should not.

## Updated Phase Plan

## Phase 0: Existing Foundation
Priority: completed baseline

Delivered:
- compatibility scaffolding in `shared/`
- deterministic metadata loading
- typed compatibility data
- early compatibility reporting
- first cache evidence in performance artifacts
- first metadata-backed runtime seams for menu and block-entity handling
- post-generation validation artifact emission

This baseline is real and should be preserved.

## Phase 1: Universal Index And Fingerprints
Priority: highest

Build:
- `UniversalResourceIndex`
- shared file and resource classification
- mod dependency and resource dependency recording
- persistent incremental fingerprints
- replacement for full-tree `getModUUID()` hashing

Exit criteria:
- one authoritative discovery pass
- no independent compatibility filesystem walk
- no independent UUID tree hash walk during normal startup

Current status:
- The compatibility inventory walk has been removed from the normal startup path by reusing `ModResourceIndex` asset and data categories.
- Full-tree pack UUID hashing has been removed from the normal startup path and replaced with persisted conversion keys derived from indexed resource fingerprints, transitive dependent-mod fingerprints, and metadata state.
- A first-class artifact cache layout now persists index, compatibility, conversion, and validation artifacts, and compatibility output can already be reused by cache key across repeat startup when the indexed mod/resource and metadata fingerprints match.
- Model lookup now uses indexed file-path resolution plus lazy deserialization through a bounded `IndexedModelProvider`, so startup no longer eagerly builds a global parsed model map before conversion begins.
- The index itself can now be rehydrated from cache on unchanged startup by validating stored mod roots, indexed files, and indexed directories instead of rewalking the resource tree.
- Shared texture-output resolution now uses a bounded cache in the conversion path, and the performance artifact records its hit/miss/eviction evidence alongside the model-provider cache.
- Texture conversion now records discovered versus selected texture counts from a real dependency graph built from converted models and equipment assets, retains the selected set across multi-root extraction passes, and the post-process block texture map no longer blindly registers textures outside that aggregated set.
- Phase 1 is still incomplete because broader universal-index boundaries have not yet been split out from `ModResourceIndex`, and several runtime consumers still rely on local resource scans or ad hoc resolution paths outside the compiled/indexed surfaces.

## Phase 2: Artifact Cache And Lazy Loading
Priority: highest

Build:
- `ArtifactCache`
- `ConversionKey`
- index cache
- compatibility cache
- model and texture caches
- lazy resource loading
- bounded LRU model resolution

Exit criteria:
- repeat startup can hit index and compatibility caches
- unchanged mods skip expensive recomputation
- entire modpack is not kept parsed in memory by default

## Phase 3: IR And Compiled Runtime Plan
Priority: highest

Current state:
- first `CompiledCompatibilityPlan` projections now compile from the generated compatibility report into an in-memory `RuntimeDispatchTable`
- current block, item, armor, bow, entity, menu, and block-entity runtime consumers now use direct identifier-driven plan lookup instead of re-reading flexible report and metadata structures on hot paths
- item presentation compilation now avoids early component-binding hazards by using safe runtime probes and conservative fallbacks when item components are not yet bound
- deeper resource IR and broader lazy resource loading are still pending

Build:
- `DiscoveryIr`
- `CompatibilityIr`
- `ResourceIr`
- `BridgeIr`
- `CompiledCompatibilityPlan`
- `RuntimeDispatchTable`
- compiled metadata and compiled patches

Exit criteria:
- compatibility decisions are compiled out of hot paths
- runtime becomes identifier-driven map lookup

## Phase 4: Conversion Engine Optimization
Priority: very high

Build:
- model dependency graph
- texture dependency graph
- parent-model resolution cache
- compiled block-state tables
- stable generated identifiers
- content-addressed generated assets
- incremental pack generation
- memory-aware conversion scheduler

Exit criteria:
- conversion scales with required assets, not all assets
- repeated runs produce stable identifiers and deterministic outputs

## Phase 5: Behavior Fact Extraction And Generic Bridges
Priority: very high

Build:
- behavior fact extractor
- generic machine model
- generic container model
- item transfer bridge
- fluid transfer bridge
- energy transfer bridge
- broader block-entity bridge
- fluid bridge
- richer menu bridge

Exit criteria:
- common machine and container families work through generic bridges before mod-specific adapters

## Phase 6: Knowledge And Pattern Classification
Priority: high

Build:
- `CompatibilityKnowledge`
- pattern classifiers
- machine archetype detection
- container archetype detection
- automatic adapter selection helpers
- cost versus value ranking

Exit criteria:
- the engine learns reusable patterns rather than only accumulating mod names

## Phase 7: Mod-Specific Adapters
Priority: high

Only after Phases 1 through 6 are real should the project invest heavily in dedicated adapters for ecosystems such as:

- Create
- Mekanism
- AE2
- Refined Storage
- Botania
- Ars Nouveau
- Immersive Engineering
- Farmer's Delight

## Phase 8: Distribution And Regression Matrix
Priority: high

Build:
- content-addressed pack artifacts
- pack manifest generation
- Geyser pack registration flow
- optional remote hosting support
- compatibility regression matrix
- performance regression matrix
- pack regression matrix

Exit criteria:
- Bedrock resource delivery is reproducible
- compatibility breadth and performance regressions are measurable in CI

## Current-State Execution Roadmap

Use the live Hydraulic repo and its runtime artifacts as the control document for execution order.

### Locked execution order

1. replace duplicated discovery with a universal index
2. replace full-tree pack UUID hashing with incremental persistent fingerprints
3. add a first-class artifact cache and precise conversion keys
4. move model loading to lazy indexed access, then widen that approach to the remaining resource categories; the first shared texture-resolution cache and first selective texture-conversion dependency graph slices are now shipped, but broader indexed texture and resource loading is still eager
5. compile compatibility decisions into runtime plans and direct dispatch tables
6. deepen the resource IR, model dependency graph, and texture dependency graph
7. compile block-state and metadata-heavy paths into compact runtime structures
8. widen generic bridges for menus, block entities, machines, fluids, and transfer systems
9. add knowledge and classifier layers after generalized bridge seams exist
10. add mod-specific adapters after the substrate is stable
11. expand pack delivery and CI-scale compatibility matrices

This order is intentional. Do not start writing dozens of adapters before the universal index, cache, and compiled runtime plan exist.

### Current execution anchors
- `shared/src/main/java/org/geysermc/hydraulic/pack/PackManager.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/ModResourceIndex.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/PackUtil.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/PerformanceReportTracker.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/BlockMapping.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/BlockStateRule.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/MetadataLoader.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/MappingResolver.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/adapter/CapabilityAdapterRegistry.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/runtime/MenuPatchTranslatorFactory.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/runtime/BlockEntityPatchTranslatorFactory.java`

### Current execution rules
- preserve compatibility semantics first, then optimize
- use the live repo and runtime artifacts as truth over older prose
- replace duplicated discovery before widening compatibility breadth
- compile flexible metadata before using it in runtime paths
- treat Geyser runtime and pack-delivery constraints as hard architectural inputs
- keep README and this plan aligned with what was actually validated

## Recommended Immediate Next Slice

The best next implementation slice from the current repo state is:

1. push lazy indexed access past model paths into broader texture and adjacent resource reads so the new dependency graph can prune larger packs instead of only reporting current fixture usage
2. widen the compiled-plan surface from current registration and patch seams into richer block-state, menu, block-entity, and transfer-bridge runtime tables
3. turn current analyzer/runtime requirement output for transfer-heavy and fluid behavior into actual bridge adapters instead of reporting-only findings
4. keep narrowing cache invalidation and runtime lookup surfaces only where fresh runtime evidence shows remaining broad scans or coarse dependencies

The first broader texture-read slice, the first dependency-aware invalidation slice, the first explicit resource-edge invalidation slice, the first diagnostic precompilation slice, the first compiled block-state registration slice, and the first structural texture-coverage validation slice are now all shipped. The next smallest slice is widening the compiled runtime plan into transfer-heavy and deeper behavior paths, because the cache, validation, and indexed-discovery substrate is now strong enough that the remaining hot-path flexibility sits more in those richer runtime decisions than in the already-compiled menu, block-entity, block-state, dependency-invalidation, and pack-validation seams.

## What Not To Do
- Do not keep extending `BlockStateRule` with every future concern.
- Do not leave multiple subsystems independently walking the same mod roots.
- Do not keep full-tree hashing every mod on ordinary startup.
- Do not keep the entire modpack parsed and resident by default.
- Do not keep compatibility reasoning in hot runtime paths.
- Do not equate asset presence with gameplay compatibility.
- Do not let a weighted score hide missing critical behavior.
- Do not make menu, entity, or fluid metadata look complete before runtime bridges exist.
- Do not pivot into hand-authoring hundreds of per-mod JSON files as the main strategy.
- Do not let generated metadata overwrite server-owner or user intent.
- Do not let one failed asset or unsupported mechanic abort the whole modpack conversion.
- Do not recreate Geyser's base knowledge when Hydraulic can consume it.
- Do not describe Hydraulic as a direct Forge jar to Fabric jar converter.
- Do not attempt full Forge or NeoForge API emulation inside Hydraulic's Bedrock compatibility layer.
- Do not make Bedrock behavior-pack delivery the foundational assumption for mod behavior.
- Do not treat decompiled third-party code as a normal implementation input.

## Bottom Line

The fork is pointed in the right direction, but the main missing piece is now execution architecture rather than conceptual vocabulary.

The most important correction is not "add more metadata".
It is:

```text
Universal Index
  -> Compatibility IR
  -> Compiled Compatibility Plan
  -> Runtime Dispatch Table
  -> Resource Pack + Hydraulic Runtime Bridges
  -> Validation + Content-Addressed Cache
```

That architecture matches the live repository, matches Geyser's actual strengths and limits, and keeps the project on the only realistic scaling path:

```text
automatic
  -> metadata patch
  -> generic capability adapter
  -> mod-specific adapter
  -> unsupported
```

The true skeleton-key goal is not to turn every Java mod into an independent Bedrock add-on.
It is to let Bedrock clients participate in a Java mod ecosystem while the Java server remains authoritative and Hydraulic supplies the missing visual, semantic, and interaction bridge layers.
