# Hydraulic Fork Architecture Plan

## Goal
Turn this fork from a narrow override layer into a general compatibility system that can support the maximum practical number of mods with the least possible per-mod hand work.

The target is not:

- block-only JSON overrides

The target is:

- Java registry discovery
- per-mod content inventory
- automatic compatibility analysis
- declarative metadata corrections and overrides
- generated Bedrock resource content
- generated Bedrock behavior content where needed
- runtime bridges for interactions that cannot be solved by assets alone

The long-term architecture remains:

1. automatic translation
2. metadata correction and override
3. dedicated mod adapters only when the first two layers are insufficient

## Guiding Principles
- Keep the current Hydraulic conversion pipeline as the base engine.
- Keep new behavior additive and reversible.
- Treat metadata as an override layer, not the primary implementation strategy.
- Prefer shared compatibility logic in `shared/` over platform-specific logic.
- Prefer graceful degradation over aborting a full conversion run.
- Build around typed compatibility data instead of endlessly extending block-specific classes.
- Separate visual compatibility from gameplay compatibility in both design and reporting.

## Audit Snapshot

### Date
- 2026-09-08

### Audit scope
- `shared/src/main/java/org/geysermc/hydraulic/pack/PackManager.java`
- `shared/src/main/java/org/geysermc/hydraulic/block/BlockPackModule.java`
- `shared/src/main/java/org/geysermc/hydraulic/item/ItemPackModule.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/*`
- `shared/src/main/java/org/geysermc/hydraulic/compat/*`
- `shared/src/test/java/org/geysermc/hydraulic/metadata/MetadataLoaderTest.java`
- `shared/src/test/java/org/geysermc/hydraulic/compat/MappingResolverTest.java`
- `fabric/run/config/hydraulic/metadata/*`
- `fabric/run/config/hydraulic/reports/*`

## Locked Architecture

```text
             HYDRAULIC
               |
            Compatibility Core
               |
        +--------------+--------------+
        |              |              |
      Discovery      Knowledge      Metadata
        |              |              |
        +--------------+--------------+
               |
             Analyzers
               |
            Capability Model
               |
          Automatic Translators
               |
          +----------+----------+
          |                     |
      Resource Layer         Behavior Layer
          |                     |
          +----------+----------+
               |
            Runtime Bridges
               |
             Mod Adapters
               |
          Compatibility Report
               |
             Bedrock Client
```

The compatibility layer stays above the current Hydraulic conversion pipeline, but it should no longer be treated as a single undifferentiated mapping system.

## Compatibility Domains

The engine should be split into five domains that are analyzed and reported separately.

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
  +-- behavior-pack generation
  +-- runtime translation
  +-- Java <-> Bedrock events
  +-- generic capability adapters
  +-- mod-specific adapters
```

This separation is fundamental, not just a reporting refinement. A converted model is not proof of compatibility.

## Current State Summary

### What is already implemented
- Pack orchestration still centers correctly in `PackManager`.
- A `compat` foundation already exists under `shared/`:
  - `CompatibilityManager`
  - `CompatibilityRegistry`
  - `CompatibilityReport`
  - `CompatibilityProfile`
  - `ContentInventory`
  - `MappingResolver`
  - `MappingOwnership`
- `MetadataLoader` already performs recursive deterministic JSON discovery using `Files.walk(...)` plus sorted normalized paths.
- Metadata ownership and precedence already exist:
  - `builtin = 0`
  - `mods = 100`
  - `server = 500`
  - `user = 1000`
  - `legacy = 500`
- `MetadataLoader` already supports more than blocks:
  - block rules
  - item identifier mappings
  - recipe identifier mappings
  - entity identifier mappings
  - menu identifier mappings
  - typed content patches
- `CompatibilityManager` already emits early artifacts under `config/hydraulic/reports/`:
  - `content-inventory.json`
  - `compatibility-report.json`
- Typed compatibility data is now implemented and emitted:
  - `CompatibilityObject`
  - `CapabilityProfile`
  - `SupportResult`
  - `CompatibilityFinding`
  - `Confidence`
  - `Provenance`
  - `ModFingerprint`
- Analyzer-backed compatibility reporting now exists for:
  - blocks
  - items
  - recipes
  - entities
  - menus
  - fluids
  - block entities
- `MappingResolver` already has state-aware block resolution and groups block states by resolved Bedrock identifier.
- `MappingResolver` now also carries compact resolved block metadata for block-state overrides, geometry overrides, and material overrides so block runtime consumers no longer need to reopen flexible rule objects after resolution.
- `MetadataIndex` now precompiles compact menu and block-entity patch templates so analyzers and runtime bridges stop reparsing raw patch operations on hot paths.
- `BlockPackModule` already consumes resolved state-aware block definitions during custom block registration.
- `ItemPackModule` already uses compatibility-aware block placement for block items, consults item compatibility objects for non-block custom item registration, suppresses creative exposure when item behavior is only approximated, and continues to translate modern item components through `ComponentConverter`.
- `ArmorPackModule` now generates humanoid armor attachables from direct equipment-asset loading and gates them through compatibility decisions.
- `BowPackModule` now consumes compatibility-driven item presentation decisions before generating bow attachables.
- `EntityPackModule` now consumes metadata-backed entity compatibility objects and registers custom entities through `GeyserDefineEntitiesEvent` when presentation support exists.
- A first capability-adapter layer now exists for current block, item, armor, bow, and entity runtime consumers, driven by explicit adapter features and `behavior_tag` metadata when present.
- `CompatibilityObject` now carries derived adapter bindings so the report can describe which current generic runtime bridges apply to a given block, item, or entity.
- `CompatibilityObject` now carries derived runtime requirements so the report can describe which bridge or generator category is still missing for a given object.
- Patch-declared behavior requirements and tags now flow into compatibility objects, structured findings, and runtime suppression reasons.
- Item discovery now falls back from modern `assets/<ns>/items/*.json` definitions to legacy `models/item/*.json` assets for compatibility inventory and conversion indexing.
- Focused tests already exist for loader precedence, resolver behavior, compatibility decisions, equipment asset loading, item asset lookup, and report generation.
- The local Fabric runtime has already produced report artifacts and logged metadata/report initialization successfully.
- Geyser unsupported runtime paths now emit compatibility-backed diagnostics for unsupported Java menu opens and for block entity data packets that fall through to Geyser's default empty block-entity translator when Hydraulic already knows the affected object still needs block-entity bridges.
- Hydraulic now has a first real metadata-backed menu fallback bridge at the Geyser open-screen seam: when Hydraulic can resolve the live Java menu identifier and metadata declares an explicit fallback `bedrock.menu.container_type`, it can substitute an existing Geyser inventory translator instead of only warning.
- Hydraulic now has a first real metadata-backed block-entity data bridge at the Geyser empty-translator seam: explicit `bedrock.block_entity.id` and `bedrock.block_entity.data.*` patches can synthesize Bedrock block-entity tags before the unsupported-runtime warning path.

### What is materially better than the earlier fork assessment
- The fork is no longer only a block metadata experiment.
- Phase 0 scaffolding is already present in code, not just planned.
- Deterministic metadata loading is already implemented.
- Compatibility reporting already exists as an early regression surface.
- Item, recipe, entity, and menu metadata hooks already exist, even though they are still shallow.

### What is still too narrow
- Non-block metadata is still shallow compared to the block path. It can now express typed patches and behavior requirements, but it still does not model full rendering, slots, recipes, fluids, or rich interactions.
- The block path remains the richest end-to-end compatibility path.
- There is no behavior-pack generator.
- There are still no generic runtime interaction bridges for fluids, machines, or entity behavior logic, and menu or block-entity support remains limited to explicit metadata-backed seams rather than broad protocol translation.
- Runtime consumption of compatibility decisions now exists for block custom registration, block item texture fallback, item custom registration, item creative exposure, armor attachables, bow attachables, and metadata-backed custom entity registration, but broad interaction and behavior bridges still do not.
- Entity runtime consumption now reaches metadata-backed custom entity registration, but not interaction or behavior translation.
- There is no compatibility knowledge layer yet, and the current capability-adapter layer covers only the runtime bridges that already exist.
- Runtime consumers now resolve those current bridges through explicit adapter bindings, but fluids, machines, richer entity behavior, and broad menu/block-entity interaction still have no adapter-backed runtime implementation.

## Actual Current Control Flow

Today the relevant control path is:

```text
PackManager.initialize
  -> initializeModLookups
  -> MetadataLoader.load(config/hydraulic/metadata)
  -> CompatibilityManager.initialize(...)
    -> build ContentInventory
    -> run analyzers
    -> build CompatibilityReport
    -> write reports/content-inventory.json
    -> write reports/compatibility-report.json
    -> create CompatibilityRegistry + MappingResolver
    -> CompatibilityDecisions resolves current runtime bridges through CapabilityAdapterRegistry
  -> normal pack conversion pipeline
  -> BlockPackModule consumes resolved block mappings during custom block registration
    -> ItemPackModule consumes compatibility-aware block placement mapping plus adapter-aware registration/exposure decisions
    -> ArmorPackModule consumes adapter-aware wearable decisions for attachable generation
    -> BowPackModule consumes adapter-aware attachable decisions for bow generation
    -> EntityPackModule consumes adapter-aware entity registration decisions
```

This remains the correct insertion point. The architecture change is not to move the compatibility layer. The change is to deepen what the layer knows and how it decides.

## Core Architectural Correction

The current inventory list is useful, but the future system should not treat blocks, items, entities, fluids, recipes, menus, particles, sounds, tags, dimensions, effects, and enchantments as if they all flow through one identical compatibility mechanism.

The engine should instead evaluate each object across the five domains above:

- content
- presentation
- state and data
- interaction
- behavior

That distinction should drive:

- analyzers
- support results
- metadata patches
- runtime bridges
- final reporting

## Capability Model

The next major subsystem should be a dedicated capability layer.

```text
org.geysermc.hydraulic.compat.capability
  CapabilityAnalyzer
  CapabilityProfile
  Capability
  CapabilityRequirement
  CapabilityResult
```

Every registered object should receive a capability profile before the engine decides whether the object is automatic, metadata-assisted, adapter-driven, visual-only, or unsupported.

Example capability shape:

```text
Create: crushing_wheel

CAPABILITIES

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
```

The central question becomes: can Bedrock represent the required capabilities, and if not, what is the smallest layer needed to close the gap?

## Target Compatibility Model

The next major shift should be from raw mapping tables to typed compatibility data.

```text
CompatibilityObject
  -> content type
  -> Java identifier
  -> owning mod
  -> mod fingerprint
  -> inventory facts
  -> capability profile
  -> analyzer findings
  -> automatic translation result
  -> metadata patches
  -> adapter bindings
  -> runtime requirements
  -> support results
  -> confidence
  -> provenance
  -> reasons
```

Every object should eventually answer:

1. What capabilities does it require?
2. Which of those capabilities are automatically representable on Bedrock?
3. Which gaps can be closed by metadata patches?
4. Which gaps require a generic capability adapter?
5. Which gaps require a mod-specific adapter?
6. If gaps remain, is the result approximated, visual-only, or unsupported?

## Multidimensional Support Results

Do not store only one support level.

The engine should store domain-level and capability-level support first, then derive the overall result.

Example:

```text
overall = ADAPTED

visual = COMPLETE
placement = COMPLETE
state = COMPLETE
interaction = PARTIAL
inventory = COMPLETE
behavior = PARTIAL
animation = COMPLETE
audio = COMPLETE
```

The final report can still expose a single overall status, but that overall value must be derived from richer support results.

## Compatibility Taxonomy

Keep the current `CompatibilityStatus` values for coarse coverage math when useful, but add a separate final support vocabulary:

- `NATIVE`
- `AUTOMATIC`
- `ADAPTED`
- `APPROXIMATED`
- `VISUAL_ONLY`
- `UNSUPPORTED`

These values should describe the end result, not replace the underlying per-domain support data.

## Compatibility Score

Add a machine-readable score, but never use the score as a substitute for support level.

Example:

```text
Visual:       100%
Placement:    100%
Interaction:   90%
Behavior:      75%
Data:         100%

Overall:       91%
Status:        ADAPTED
```

Scores are useful for ranking, regression tracking, and modpack summaries. Support levels remain the human-facing verdict.

## Reason Engine

Every degraded result should explain why, not just what the status is.

Example unsupported result:

```text
UNSUPPORTED

Reason:
Java block entity requires server-side kinetic capability
with no generic Bedrock representation.

Suggested resolution:
Create capability adapter or mod adapter for kinetic behavior.
```

Example visual-only result:

```text
VISUAL_ONLY

Reason:
Model and texture converted successfully.

Missing:
- custom inventory
- block entity interaction
- machine processing
```

This becomes critical once the project is evaluating hundreds of objects across many mods.

## Analyzer Inputs

The analyzer engine should inspect more than registries.

It should pull facts from four sources:

### Registry
- blocks
- items
- entities
- fluids
- menus
- recipes
- sounds
- particles

### Resources
- models
- blockstates
- textures
- animations
- lang
- loot tables
- tags
- recipes

### Runtime objects
- `Block`
- `Item`
- `BlockEntity`
- `Entity`
- `Menu`
- `Fluid`

### Capabilities
- inventory
- energy
- fluid handling
- redstone
- ticking
- interaction
- animation
- custom rendering

Without this wider fact set, the analyzer cannot make credible automatic decisions.

## Mod Fingerprint

Every mod should receive a machine-readable fingerprint that adapters and analyzers can target safely.

```text
ModFingerprint
  -> mod_id
  -> version
  -> loader
  -> minecraft_version
  -> registered_blocks
  -> registered_items
  -> registered_entities
  -> registered_fluids
  -> registered_menus
  -> registered_recipes
  -> uses_block_entities
  -> uses_custom_renderers
  -> uses_custom_networking
  -> uses_capabilities
  -> uses_custom_item_components
  -> uses_custom_models
  -> uses_custom_particles
  -> uses_custom_sounds
```

Adapters should be able to declare supported mod ranges and required capabilities instead of blindly activating on namespace alone.

## CompatibilityKnowledge

Add a reusable knowledge layer that captures learned patterns without turning each discovery into hardcoded special-case logic.

```text
compat/
  knowledge/
    blocks/
    items/
    models/
    capabilities/
    interactions/
    mods/
```

Examples of reusable knowledge:

- blocks with this model structure behave like stairs
- menus with these slot families can use generic container X
- this renderer requires Java-side runtime support
- this capability appears across these mod families

This is the mechanism that should let the engine improve over time without exploding in adapter count.

## Proposed Package Direction

### Keep current classes

```text
org.geysermc.hydraulic.compat
  CompatibilityManager
  CompatibilityRegistry
  CompatibilityReport
  ContentInventory
  MappingResolver
  MappingOwnership
```

### Add explicit subpackages

```text
org.geysermc.hydraulic.compat.capability
  CapabilityAnalyzer
  CapabilityProfile
  Capability
  CapabilityRequirement
  CapabilityResult

org.geysermc.hydraulic.compat.analysis
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

org.geysermc.hydraulic.compat.knowledge
  CompatibilityKnowledge
  KnowledgeEntry
  KnowledgeSource

org.geysermc.hydraulic.compat.model
  CompatibilityObject
  SupportResult
  CompatibilityFinding
  Confidence
  Provenance
  ModFingerprint

org.geysermc.hydraulic.compat.mapping
  ContentPatch
  StateTranslator
  ModelClassifier
  GeometryResolver
  ItemTranslator
  FluidTranslator
  ContainerTranslator

org.geysermc.hydraulic.compat.bridge
  InteractionBridge
  ContainerBridge
  BlockEntityBridge
  FluidBridge
  EntityBridge

org.geysermc.hydraulic.compat.generator
  BehaviorPackGenerator
  BehaviorDefinition
  BehaviorAdapter
  ReportGenerator

org.geysermc.hydraulic.compat.adapter
  ModAdapter
  CapabilityAdapter
```

The important point is not package purity. The important point is to separate knowledge, capability analysis, translation, runtime bridging, and final adapters into explicit architectural layers.

## Metadata Evolution Plan

### Current state
The loader already supports recursive discovery and ownership precedence. That part should be preserved.

### Next structure

```text
config/hydraulic/metadata/
  builtin/
  mods/
  server/
  user/
```

### Metadata as patch system
Do not think of metadata as primary mappings. Treat metadata as compatibility patches over automatic analysis.

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

This lets automatic translation provide the baseline while metadata modifies only the parts that need correction.

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

Generated compatibility suggestions should never overwrite manual server or user overrides.

### Provenance for generated data
Every generated result should carry provenance:

```text
source:
  AUTOMATIC_ANALYZER

analyzer:
  BlockModelAnalyzer

confidence:
  0.97

generated_at:
  timestamp

hydraulic_version:
  ...

overridden:
  false
```

### Metadata design rules
- Keep legacy simple block files working.
- Add typed schemas instead of stuffing unrelated concerns into `BlockStateRule`.
- Make validation strict enough to reject bad entries and tolerant enough to skip them without killing the load.
- Report conflicts and invalid entries as compatibility findings, not just log noise.

## Confidence Model

Automatic translation should record confidence so the engine can avoid confidently doing the wrong thing.

Example thresholds:

```text
>= 0.95     AUTOMATIC
0.75-0.94   AUTOMATIC + WARNING
0.50-0.74   APPROXIMATED
< 0.50      NEEDS PATCH OR ADAPTER
```

Confidence should influence decisions and report severity, but it should not replace support results or reasons.

## Geyser Knowledge Integration

Hydraulic should explicitly build on Geyser's existing mapping ecosystem rather than recreate it.

The architecture should be:

```text
Geyser Knowledge
     +
Hydraulic Conversion
     +
Hydraulic Compatibility Engine
     +
Mod-specific knowledge
```

not:

```text
Hydraulic recreates Geyser
```

That reduces divergence and makes the compatibility layer focus on modded gaps rather than re-implementing the base Java to Bedrock knowledge set.

## Real-World Constraint

There is no architecture that can make every Java mod fully compatible with Bedrock automatically.

The practical goal is:

- automatically achieve the highest possible compatibility
- isolate irreducible Java-specific behavior behind runtime bridges and adapters
- make limitations explicit instead of pretending unsupported mechanics work

That is the technically defensible goal for the project.

## Compatibility Pipeline

The intended pipeline is:

1. Enumerate Java registries.
2. Group content by owning mod.
3. Build typed content inventory and mod fingerprint data.
4. Run analyzers across content, presentation, state/data, interaction, and behavior domains.
5. Build capability profiles and support results.
6. Emit an early compatibility report with reasons, confidence, and provenance.
7. Attempt automatic translation.
8. Apply metadata patches.
9. Apply generic capability adapters where possible.
10. Apply mod-specific adapters only when required.
11. Generate Bedrock resource and behavior content.
12. Register runtime bridges.
13. Emit final compatibility results.

Today the code only fully reaches the early inventory/report stage and partially reaches metadata-assisted resolution for the current block path.

## Stability Work Before Breadth

### 1. Never-crash conversion policy
Any individual asset or mapping failure should:

- log the failure
- record a structured finding
- emit fallback output when possible
- continue processing the rest of the mod and modpack

### 2. Per-mod isolation
One degraded mod should not stop the rest of the pack from converting.

### 3. Per-asset isolation
Texture, model, metadata, and registration failures need explicit boundaries at the smallest practical scope.

### 4. Deterministic conflict reporting
Now that rule ordering is deterministic, conflicts should be surfaced as structured compatibility findings.

### 5. Correctness over optimistic coverage
Do not classify something as complete because an asset exists if the runtime behavior is still missing.

## Adapter Model

Do not make adapters only flat per-mod buckets.

The adapter structure should be capability-driven:

```text
ModAdapter
   |
   +-- identifies mod
   +-- declares supported versions
   +-- declares capabilities
   +-- registers analyzers
   +-- registers mappings
   +-- registers runtime bridges
   +-- registers generators
```

Example direction:

```text
CreateAdapter
  +-- KineticCapability
  +-- ContraptionCapability
  +-- StressCapability
  +-- BasinCapability
```

This makes reusable logic possible across mod families.

## Generic Machine Compatibility

Machines deserve an explicit intermediate layer before mod-specific adapters.

```text
MachineCompatibility

INPUT
  +-- item slots
  +-- fluid slots
  +-- energy
  +-- processing
  +-- upgrades
  +-- output

OUTPUT
  +-- generic container
  +-- generic interaction
  +-- optional mod-specific behavior
```

Without this layer, every tech mod risks reinventing the same inventory and container logic under different adapter names.

## Reporting Model

The report should eventually answer three things simultaneously:

1. What works.
2. How well it works across each domain.
3. Why it does not work when support is partial or missing.

Example future output:

```text
Create
-----------------------------
Blocks             98.2%
Items              97.4%
Machines           83.1%
Entities           91.7%
Fluids            100.0%
Recipes            96.5%

Overall            93.8%

NATIVE             102
AUTOMATIC          421
ADAPTED             87
APPROXIMATED        31
VISUAL_ONLY         12
UNSUPPORTED          6
```

This should be backed by per-object reasons, support results, confidence, and suggested resolutions.

## Updated Phase Plan

## Phase 0: Existing Foundation
Priority: completed baseline

Delivered:
- `compat/` scaffolding in `shared/`
- `ContentInventory`
- `CompatibilityReport`
- `CompatibilityRegistry`
- `MappingResolver`
- `MappingOwnership`
- recursive deterministic metadata loading
- metadata precedence handling
- early compatibility reporting before pack conversion
- tests for metadata precedence and resolver behavior

This phase is complete enough to build on and should not be re-done.

## Phase 1: Compatibility Data Model
Priority: completed baseline

Build:
- `CompatibilityObject`
- `CapabilityProfile`
- `SupportResult`
- `CompatibilityFinding`
- `Confidence`
- `Provenance`
- `ModFingerprint`

This phase is implemented and in active use by the analyzer and reporting pipeline.

## Phase 2: Analyzer Engine
Priority: implemented initial slice

Build:
- `RegistryAnalyzer`
- `BlockAnalyzer`
- `ModelAnalyzer`
- `StateAnalyzer`
- `ItemAnalyzer`
- `ComponentAnalyzer`
- `EntityAnalyzer`
- `FluidAnalyzer`
- `MenuAnalyzer`
- `BlockEntityAnalyzer`

This phase is partially complete. The analyzer set exists and produces per-domain support results, confidence, provenance, and findings, but deeper capability inference remains limited for several content types.

## Phase 3: Automatic Translators
Priority: critical

Build:
- `StateTranslator`
- `ModelClassifier`
- `GeometryResolver`
- `ItemTranslator`
- `FluidTranslator`
- `ContainerTranslator`

## Phase 4: Reporting
Priority: implemented initial slice

Produce detailed compatibility output before trying to make every behavior functional.

Deliverables:
- multidimensional support results
- reasons and suggested resolutions
- confidence and provenance
- per-mod and per-object scores

This phase is partially complete. The compatibility report already carries multidimensional support results, findings, confidence, provenance, and scores, but downstream consumers and higher-level summaries are still incomplete.

## Phase 5: Metadata V2
Priority: implemented initial slice

Metadata becomes a patch and override system rather than the main source of truth.

Deliverables:
- typed patch schemas
- generated/manual separation
- structured diagnostics
- conflict reporting

This phase is partially complete. Typed patch schemas, synthesized mappings, and validation are present, but ownership-specific directory structure and generated suggestion workflows are still incomplete.

## Phase 6: Runtime Compatibility
Priority: very high

Build:
- `InteractionBridge`
- `ContainerBridge`
- `BlockEntityBridge`
- `FluidBridge`
- `EntityBridge`

Current verified runtime consumers:
- block creative exposure and placement gating
- block item texture fallback
- non-block item registration and creative exposure gating
- armor attachable generation gating
- bow attachable generation gating
- metadata-backed custom entity registration
- compatibility-backed unsupported menu diagnostics at the real Geyser open-screen path
- metadata-backed menu fallback translation for explicit `bedrock.menu.container_type` patch templates at the real Geyser open-screen path
- compatibility-backed unsupported block-entity data diagnostics at the real Geyser empty-translator path
- metadata-backed block-entity data translation for explicit `bedrock.block_entity.*` patch templates at the real Geyser empty-translator path

This phase is still early. Hydraulic now has first real metadata-backed menu and block-entity runtime bridges for explicit templates, but generic interaction, container behavior, fluid, entity behavior, and broader block-entity bridges are still not implemented.

## Phase 7: Behavior Generation
Priority: very high

Build:
- `BehaviorPackGenerator`
- `BehaviorDefinition`
- `BehaviorAdapter`

## Phase 8: Generic Machine Compatibility
Priority: very high

Build a reusable machine layer before writing large numbers of tech-mod-specific adapters.

## Phase 9: Mod Adapters
Priority: high

Only now should the project lean into dedicated adapters for ecosystems such as:

- Create
- Mekanism
- AE2
- Refined Storage
- Botania
- Ars Nouveau
- Immersive Engineering
- Farmer's Delight

## Phase 10: Automated Mod Matrix
Priority: high

Test real representative modpacks rather than only isolated toy examples.

## Current-State Execution Roadmap

Use the live Hydraulic repo as the control document for execution order.

The project is no longer at pure scaffolding stage. The repo now already contains:

- cached block-state resolution in `BlockMapping`, plus anchored rule indexes that reduce first-hit conditional rule scans while preserving rule precedence
- compact resolved block metadata exposed through `MappingResolver.ResolvedBlockState` for block consumers
- cached per-state `ModelDefinition` resolution in `StateDefinition`, reducing repeated blockstate variant and multipart matching during block conversion
- precompiled menu and block-entity patch templates in `MetadataIndex` and `MappingResolver` for runtime bridge lookups and analyzer checks
- cheaper startup block and item ownership lookup in `PackManager`, avoiding per-block multimap scans and unused path retrieval when indexed membership is sufficient
- per-mod resource indexing in `ModResourceIndex`
- performance artifacts through `performance-report.json`, now including startup indexed blockstate and item-asset totals in addition to timing snapshots
- a live `CapabilityAdapterRegistry`
- first metadata-backed menu and block-entity runtime bridges

That means the next roadmap should finish and generalize partially landed systems rather than re-plan them from zero.

### Execution order

1. refresh the baseline before each batch and treat the live repo plus runtime artifacts as authoritative over older notes
2. finish splitting flexible metadata loading from compact runtime metadata beyond the resolved block-state path, especially for patch-heavy and non-block consumers
3. finish compact resolved block-state answer caching and any remaining block hot-path compaction around the now-indexed rule matcher and cached model resolution path
4. complete startup indexing and eliminate remaining repeated probing by routing blockstate, item-definition, legacy-model, and model-provider lookups through indexes or prebuilt maps
5. make conversion selective, cacheable, and evidence-driven using indexed startup data, compatibility inventory, and strict invalidation keys
6. expand observability from the current performance artifact into a decision system with cache-hit, miss, and timing breakdowns plus snapshot-versus-history reporting
7. generalize the current runtime-bridge seams after the performance substrate is cheaper and measurable, extending menu, block-entity, fluid, container, and richer behavior support
8. add the first compatibility knowledge layer only after real generalized bridge seams exist
9. re-rank long-term breadth work after the missing external Copilot-share content is available and the performance substrate changes are landed

### Current execution anchors

- `shared/src/main/java/org/geysermc/hydraulic/metadata/BlockMapping.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/BlockStateRule.java`
- `shared/src/main/java/org/geysermc/hydraulic/metadata/MetadataLoader.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/MappingResolver.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/PackManager.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/ModResourceIndex.java`
- `shared/src/main/java/org/geysermc/hydraulic/pack/PerformanceReportTracker.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/adapter/CapabilityAdapterRegistry.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/runtime/MenuPatchTranslatorFactory.java`
- `shared/src/main/java/org/geysermc/hydraulic/compat/runtime/BlockEntityPatchTranslatorFactory.java`

### Execution rules

- preserve compatibility semantics first, then optimize
- prove each performance claim with focused measurements and runtime artifacts
- keep metadata expressive at load time but compact at runtime
- finish the partially landed performance substrate before widening broad runtime bridge scope
- keep README and this architecture plan aligned with what was actually validated

## External Tooling Deep Dive

The linked Forge/Fabric tooling is useful as pattern input, but it should change Hydraulic's implementation details more than its mission.

### Minecraft-Mod-Porter: useful patterns, not a direct Hydraulic dependency

Useful findings:

- use a canonical intermediate representation instead of pairwise rewrite logic
- keep version and platform knowledge in data files rather than hard-coded switch logic
- support alias and overlay datasets so closely related versions reuse mappings safely
- preserve unresolved cases and emit structured manual-action reports instead of guessing
- separate engine, CLI, and UI through a library-first backend and machine-readable reports

Important correction:

- the current README explicitly says Forge to Fabric cross-loader conversion is not supported today, so this should not be treated as a turnkey source-to-source loader converter for Hydraulic

Hydraulic implementation takeaway:

- add a compact compatibility IR for concepts Hydraulic already reasons about, such as menu archetypes, block-entity patch templates, item component families, storage and transfer capabilities, and interaction categories
- store compatibility knowledge as data with overlay support instead of spreading version, mod-family, or loader-family facts through runtime conditionals
- emit structured follow-up actions when automatic translation cannot safely decide, rather than silently downgrading intent

### Porting-Lib and Porting-Lib-Dash: modular shim design

Useful findings:

- the library is split into narrow modules like registry, transfer, fluids, gui utilities, config, tags, model loaders, and chunk loading instead of one monolithic compatibility layer
- related APIs are explicitly called out for capability replacement patterns, such as components instead of capabilities and accessories instead of Curios-style slots
- the maintained upstream matters more than stale forks; the Dash fork appears materially behind upstream and should not be treated as the baseline reference

Hydraulic implementation takeaway:

- keep capability adapters narrow, feature-scoped, and swappable instead of growing a single generic adapter abstraction that knows everything
- model machine, transfer, fluid, gui, and registry-style compatibility as separate bridge families with explicit requirements and bindings
- prefer optional bridge modules and targeted adapter surfaces over one large runtime emulation layer

### Kilt: runtime bridge lessons and non-goals

Useful findings:

- Kilt uses a staged runtime strategy: remap inputs, apply targeted fixers, then bridge APIs through explicit injects and workarounds
- the codebase keeps compatibility work visible through dedicated `compat` and `workarounds` areas rather than burying it in unrelated systems
- broad runtime compatibility requires many targeted fixups even when a large API surface is already bundled

Important constraint:

- Kilt describes itself as highly unstable and experimental, which reinforces how expensive full loader emulation is in practice

Hydraulic implementation takeaway:

- Hydraulic should borrow the shape of a remap and fixup pipeline for metadata patches, compatibility decisions, and targeted runtime bridge fixers
- Hydraulic should not attempt to recreate full Forge or NeoForge API emulation inside the Bedrock compatibility layer
- targeted workaround registries are appropriate; loader-wide reimplementation is not

### Modpack Converter: modpack-level intake patterns

Useful findings:

- pack conversion is a distinct problem from mod conversion or runtime compatibility
- the tool resolves pack manifests, downloads target-loader candidates, and emits a report split into converted, failed, and excluded entries
- optional external metadata sources improve resolution quality without being required for the baseline pipeline

Hydraulic implementation takeaway:

- Phase 10 should treat modpack intake as its own artifact pipeline with explicit converted, blocked, skipped, and unsupported categories
- Hydraulic's reports should separate content that converted cleanly from content that was intentionally excluded, unresolved, or requires a stronger adapter tier
- optional external indexes can improve prioritization, but the core compatibility verdicts should remain reproducible without them

### Decompilers: intake-only tooling

Useful findings:

- CFR and Fernflower are decompilers, not converters
- Fernflower and ForgeFlower remain useful for readable source recovery and identifier cleanup options
- CFR's regression-testing model is a good reminder to diff transformed output against expected artifacts instead of trusting one pass blindly

Hydraulic implementation takeaway:

- decompilation should be treated only as an optional analysis lane for closed-source mods when legally permitted
- decompiled output is not authoritative architecture data and should never be treated as automatic proof of behavior parity
- do not ingest recovered third-party code into Hydraulic; use it only to inform compatibility findings, metadata patches, and runtime bridge design where rights allow

## External Findings To Implement

These external projects suggest a concrete Hydraulic extension set:

1. add a canonical compatibility IR and overlayable knowledge datasets so Hydraulic can normalize menu, transfer, fluid, block-entity, and interaction concepts before deciding bridge strategy
2. extend reporting with explicit manual-action, approximated, blocked, excluded, and unsupported categories plus suggested resolutions and provenance
3. formalize a targeted fixer pipeline between analysis and runtime bridging so unsupported or partially supported mechanics can register narrow fixups without contaminating the whole conversion path
4. keep adapter and bridge families modular by domain, following the Porting-Lib module shape rather than building a single compatibility blob
5. add modpack-scale reporting and prioritization in Phase 10 so real pack audits distinguish converted content from missing upstream equivalents and Bedrock-only incompatibilities

## Recommended Immediate Next Slice

The best next implementation slice from the current repo state is:

1. split flexible metadata loading from compact runtime metadata so runtime consumers stop traversing nested rule and patch structures
2. define the first canonical compatibility IR slice for menu archetypes, block-entity patch templates, and transfer or interaction requirements so later bridges do not stay stringly typed
3. finish compact resolved block-state answer caching and any remaining block hot-path compaction around the now-indexed rule matcher and cached model resolution path
4. complete resource and model lookup indexing so startup and conversion stop paying repeated filesystem or flattened-pack search costs
5. promote runtime validation and performance validation from log-only checks into committed regression coverage plus artifact-backed measurement
6. once the performance substrate and first IR slice are proven, widen the existing menu and block-entity seams into the first more generic container or interaction bridge

This is the smallest next slice that lets both plans complete coherently: the architecture plan keeps its bridge-first long-term direction, while the current-state execution plan front-loads the substrate work needed to scale those bridges safely.

## What Not To Do
- Do not keep extending `BlockStateRule` with every future concern.
- Do not equate asset presence with compatibility success.
- Do not make menu or entity metadata look complete before runtime bridges exist.
- Do not pivot into hand-authoring hundreds of mod-specific JSON files as the main strategy.
- Do not let generated metadata overwrite server-owner or user intent.
- Do not let one failed asset or one unsupported mechanic abort the whole modpack conversion.
- Do not recreate Geyser's base knowledge when Hydraulic can consume it.
- Do not describe Hydraulic as a direct Forge jar to Fabric jar or cross-loader source converter.
- Do not attempt full Forge or NeoForge API emulation inside Hydraulic's Bedrock compatibility layer.
- Do not treat decompiled third-party code as a normal source input for Hydraulic implementation.

## Bottom Line
The fork is pointed in the right direction and is further along than the earlier assessment implied.

The architecture should no longer be described primarily as a future analyzer plus metadata expansion problem. The compatibility domains, capability model, support results, provenance, confidence, score, fingerprint, typed patch system, and current adapter bindings now exist in code. The correct next step is to convert those decisions into broader runtime bridges and wider capability-adapter coverage without regressing the current pack pipeline.

The scaling strategy remains:

```text
automatic
  -> metadata patch
  -> generic capability adapter
  -> mod-specific adapter
  -> unsupported
```

That is still the most realistic path to maximizing mod support without creating an unmaintainable pile of special-case code.
