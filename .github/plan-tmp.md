# Hydraulic-Phlodgate Compatibility Research and Implementation Plan

## Canonical Purpose

This file is the single canonical plan for the remaining Hydraulic-Phlodgate compatibility work. The project is not primarily missing more Bedrock content. It needs enough real-world behavioral evidence to recognize different Java mod architectures and bridge them through generic capabilities.

The target is not to copy third-party addon code or accumulate one adapter per mod. The target is a licensing-aware reference corpus and implementation pipeline that answers:

> Can Phlodgate recognize this capability, determine whether Bedrock can represent it, and compile the strongest available runtime bridge regardless of which mod implemented it?

The architecture is:

```text
Java mod discovery
     -> capability graph
     -> Java API normalization
     -> Bedrock feasibility analysis
     -> compatibility IR
     -> compiled runtime plan
     -> generic or mod-specific bridge
     -> Geyser transport
     -> Bedrock client
```

### Authority and evidence boundaries

- Mojang Bedrock Samples and Microsoft Creator documentation establish the supported Bedrock runtime and pack model.
- Bedrock-OSS documentation records practical limitations and version-specific workarounds.
- Bedrock-Core is the interoperability reference for discovery, replicated state, typed RPC, UI, and networking.
- Bedrock Energistics Core is the concrete machine, storage, energy, and fluid abstraction reference; Bedrock Energistics is its real addon consumer.
- Community projects are evidence and test cases, never an authority over official API behavior.
- CurseForge and direct-download pages are discovery sources only unless they link to inspectable source with admissible reuse terms.
- Hydraulic startup must never depend on live crawling, remote availability, or raw corpus access in hot runtime paths.

## Existing Hydraulic Integration Contract

The existing conversion pipeline remains the base engine:

```text
unified discovery/index
     -> compatibility analysis
     -> typed capability facts
     -> compiled compatibility/runtime plan
     -> resource generation and validation
     -> Hydraulic runtime bridges
     -> Geyser pack delivery and registration
```

The corpus enriches compatibility evidence and adapter selection. It must not directly change support decisions, create fake behavior, or bypass Java-server authority. Only validated, typed, compiled decisions may reach runtime dispatch.

## Canonical Capability Model

Every reference and implementation must be mapped to these capabilities:

| Phlodgate capability | Required semantics |
| --- | --- |
| `BLOCK_ENTITY_DATA` | State identity, serialization, synchronization, persistence, and safe patching |
| `MACHINE_INVENTORY` | Slot roles, capacity, sided access, filters, insertion, extraction, and simulation |
| `ITEM_TRANSFER` | Stack identity, count limits, insert/extract, simulation, sided access, and failure behavior |
| `FLUID_RUNTIME` | Fluid identity, block/item representation, tanks, capacity, and translation |
| `FLUID_TRANSFER` | Fill, drain, simulation, capacity, sided access, and cross-fluid rejection |
| `ENERGY_TRANSFER` | Capacity, receive/extract, rates, simulation, sided access, and persistence |
| `MENU_CONTAINER` | Slot semantics, transactions, menu type, interaction, and fallback behavior |
| `AUTOMATION_ACCESS` | Pipes, conveyors, routing, filtering, sided transfer, and network membership |
| `MACHINE_PROCESSING` | Recipes, inputs, outputs, catalysts, progress, energy/fluid consumption, and state |
| `PRESENTATION` | Models, textures, animations, geometry, particles, sounds, and state visuals |
| `NETWORK_SYNC` | Custom packets, clientbound state, serverbound interaction, and synchronization risk |

For each capability, classify the result as `NATIVE`, `AUTOMATIC`, `ADAPTED`, `APPROXIMATED`, `VISUAL_ONLY`, or `UNSUPPORTED`, and record evidence, limitations, persistence method, runtime hooks, and confidence. A score must never override a missing critical capability.

## Java Normalization Layer

Java APIs are inputs to one normalized model, not separate runtime implementations:

```text
Forge Capabilities       Fabric Transfer API       Mod-specific API
                    \                      |                      /
                     \                     |                     /
                                         Phlodgate normalizer
                                                             |
                         item / fluid / energy / machine
                                                             |
                                         compatibility plan
                                                             |
                                             Bedrock adapter
```

Primary Java references:

- Forge `IItemHandler`, `IFluidHandler`, and `IEnergyStorage`.
- Fabric item and fluid transfer abstractions.
- Botarium as a cross-loader item, fluid, and energy normalization reference.
- Existing Hydraulic typed bridge categories and dispatch contracts.

The normalizer must preserve simulation semantics, sided access, slot/tank identity, capacity, filtering, and failure isolation. Reflection or metadata may discover a capability, but a runtime plan must not advertise executable behavior without a concrete operation.

## Bedrock Runtime Strategy

Generated resource packs remain first-class. Bedrock behavior packs and scripts are optional supplements; Java server state remains authoritative.

The generated runtime should prefer current parameterized or flattened custom-component patterns over an obsolete static custom-component design. The intended shape is typed and generated, for example:

```text
phlodgate:machine
     machine_id
     runtime_id
     inventory
     energy
     fluid
     recipe
     interaction
```

Bedrock feasibility must be evaluated in this order:

```text
native Bedrock API
     -> supported Script API
     -> proven generic emulation
     -> approximation with explicit degradation
     -> unsupported
```

Do not present a visual object as gameplay-compatible merely because its model or texture converted.

## Execution Priorities

### 1. Item transfer and inventory

Implement and validate slot semantics, insertion, extraction, simulation, side restrictions, filters, stack handling, full-inventory behavior, and Java/Bedrock transaction boundaries. This is the gateway capability for automation and machines.

### 2. Fluid transfer

Normalize `FluidStack`, tanks, handlers, fill/drain, identity checks, capacity, side restrictions, bucket representation, and machine input/output. A bucket icon fallback is presentation only and must not imply fluid runtime support.

### 3. Machine execution

Execute the complete cycle: inputs available, input consumption, energy/fluid requirements, progress, outputs, output-capacity checks, reset behavior, and persisted state. Generic processing must precede mod-specific machine adapters.

### 4. Energy

Model source, network, storage, rates, sided access, machine consumption, simulation, and persistence. An energy bar without transfer semantics is not sufficient.

### 5. Menus, interaction, and block entities

Compile slot roles, menu archetypes, transactions, block-entity state, synchronization, and fallback behavior. Unsupported interaction must remain visible in diagnostics.

### 6. Automation and interoperability

Add generic routing, filtering, pipe/conveyor behavior, network membership, cross-addon communication, and typed synchronization only after the transfer contracts are executable.

## Reference Corpus Contract

Do not download all sources and copy code. Build a versioned, admissibility-checked capability corpus. Each record must include:

```text
source and license
Minecraft and Script API versions
capability
implementation pattern
limitations
performance characteristics
persistence method
runtime hooks
transfer semantics
UI method
reusability
Phlodgate adapter candidate
evidence pointers and confidence
```

Suggested corpus layout:

```text
bedrock-capability-corpus/
     item-transfer/
     inventory/
     fluid/
     energy/
     machine/
     automation/
     persistence/
     menus/
     ui/
     block-state/
     entity-state/
     networking/
     custom-components/
     addon-generation/
     interoperability/
```

The repository implementation should store only validated local snapshots under `config/hydraulic/corpus`, keep raw sources and generated knowledge separate, and expose corpus matches as advisory report evidence. Corpus changes must invalidate compatibility artifacts.

## Research Matrix

| Phlodgate capability | Research targets |
| --- | --- |
| `BLOCK_ENTITY_DATA` | Block runtime references, persistence references, and large open-source addons |
| `MACHINE_INVENTORY` | Block inventory APIs, industrial/machine projects, item-transfer projects |
| `ITEM_TRANSFER` | Item pipes, storage transfer, inventory manipulation, Fabric/Forge transfer APIs |
| `FLUID_RUNTIME` | Fluid systems, tanks, liquid machines, bucket and block representations |
| `FLUID_TRANSFER` | Fluid handlers, pipes, tanks, fill/drain and identity semantics |
| `ENERGY_TRANSFER` | Energy storage, power networks, generators, batteries, transfer APIs |
| `MENU_CONTAINER` | Block inventory APIs, UI frameworks, storage and furniture addons |
| `AUTOMATION_ACCESS` | Machines, pipes, conveyors, routing, filtering, and interoperability frameworks |
| Generated packs | Regolith, Bedrock Examples, Bedrock Boost, addon registries |
| Runtime components | Custom components, ADK-LIB, component registries, Script API samples |
| Persistent state | Dynamic properties, database projects, persistent block and machine data |
| Cross-addon communication | Bedrock-Core discovery, replicated state, typed RPC, UI, and network layers |

## Acceptance Criteria

Each capability slice is complete only when it has:

1. A typed normalized contract and validation rules.
2. A concrete compiled runtime-plan projection.
3. A production call path or an explicit, reportable unsupported result.
4. Unit tests for normal, simulation, boundary, malformed, and failure cases.
5. Integration coverage at the nearest Fabric/Geyser seam when applicable.
6. Persistence and cache invalidation coverage when state or corpus evidence changes.
7. Compatibility-report evidence that distinguishes visual, interaction, behavior, and network support.
8. Documentation of source provenance, version constraints, and licensing/admissibility.

No phase is complete because an interface exists. No adapter may return a successful result for an operation it cannot execute.

## Implementation Order

1. Preserve the unified index, metadata, cache, validation, and compiled dispatch foundations already in Hydraulic.
2. Finish normalized item transfer and real inventory execution.
3. Finish normalized fluid transfer and fluid runtime state.
4. Connect machine processing to energy/fluid requirements and persistent state.
5. Expand menu, block-entity, and interaction transaction bridges.
6. Add energy networks, automation access, and interoperability contracts.
7. Ingest the curated corpus offline and use it to rank generic adapter opportunities.
8. Add mod-specific adapters only where generic capability matching cannot close the gap.
9. Expand generated pack delivery, regression matrices, performance evidence, and release validation.

The broad source catalogue below is a discovery backlog, not permission to copy or redistribute third-party code/assets.

## Expanded Research Catalogue

The following catalogue folds in the original research inventory. Sources must be evaluated for version, license, provenance, implementation pattern, and limitations before they influence Hydraulic.

### 1. Official Bedrock runtime and API

Microsoft Minecraft Scripting Samples; Microsoft Minecraft Creator documentation; Mojang Bedrock Samples; Bedrock Wiki; Microsoft custom-component samples; Microsoft TypeScript starter; Microsoft How-to Gallery; Microsoft Build Challenge samples; Bedrock Script API documentation; Bedrock Script API GitHub documentation.

Use these sources to establish supported pack structure, Script API capabilities, custom components, event handling, commands, persistence, and version constraints.

### 2. Block runtime and inventory

BlockComponent documentation; BlockInventoryComponent; ItemComponentTypeMap; ItemComponentTypes; ItemComponent; ItemInventoryComponent; EntityInventoryComponent; ItemComponentRegistry; Scripting V2 overview; Bedrock block samples.

Prioritize concrete inventory access, slot manipulation, component registration, block interaction, and the limits of block-entity equivalents.

### 3. Industrial and machine systems

UtilityCraft Addon Template; DoriosStudios projects; public Bedrock machine repositories; Bedrock machines using Script API; technology, factory, processing-machine, generator, machine-TypeScript, and industrial addon repositories.

Extract machine identity, processing, upgrades, multiblocks, storage, automation, link-node ports, and state synchronization. UtilityCraft is a primary architecture-family reference, not a code dependency.

### 4. Energy systems

Bedrock energy, power, RF/FE, generator, energy-storage, energy-transfer, battery, power-cable, and energy-network repositories.

The research question is whether source, network, storage, transfer, machine consumption, rate limiting, and persistence can be represented, not merely whether an energy bar can be displayed.

### 5. Fluid systems

Bedrock fluid, fluid-addon, fluid-tank, fluid-pipe, liquid-transfer, fluid-transfer, tank-Script-API, liquid-machine, gas-system, and gas-pipe repositories.

Extract fluid identity, tank capacity, fill/drain semantics, sided access, bucket/item representation, machine interaction, and cross-fluid rejection.

### 6. Item transfer and logistics

Bedrock item-transfer, item-pipe, item-transport, item-logistics, item-network, conveyor, hopper-automation, item-filter, item-routing, and automation-network repositories.

Use these to test insertion, extraction, filtering, routing, stack handling, full inventories, and network behavior against the normalized `ITEM_TRANSFER` and `AUTOMATION_ACCESS` contracts.

### 7. Storage and persistence

Quick Item Database; Bedrock Database; Tendrock Database; dynamic-property repositories; database Script API projects; world-storage projects; persistent block-data and persistent-machine-data projects.

Extract durable state keys, lifecycle, migration, bounded caching, synchronization, corruption handling, and persistence guarantees for block entities and machines.

### 8. Component frameworks

ADK LIB; Microsoft custom components; Bedrock custom-component libraries; component frameworks; reusable components; component registries; Script API component projects; Bedrock block and item component projects.

Prefer current parameterized/flattened component patterns and record deprecated API assumptions explicitly.

### 9. Interoperability and addon frameworks

Bedrock-Core; Bedrock-Core server; Bedrock-Core UI; Bedrock-Core network; Bedrock-Core Regolith filters; community Bedrock APIs; BDS documentation; virtual APIs; API rules; environment types.

Use Bedrock-Core for discovery, replicated state, typed RPC, features, configuration, UI, networking, and cross-addon boundaries. It complements rather than replaces Bedrock Energistics Core.

### 10. Build systems and generated addon architecture

Regolith; Regolith filters; the Regolith VS Code extension; Regolith GitHub Action; Regolith schemas and filter resolver; Bedrock Examples; Bedrock Boost; Bedrock Addon Registry.

Study deterministic generation, schema validation, filters, generated identifiers, packaging, and reproducible output.

### 11. Large open-source Bedrock addons

Remon Furniture; Medieval Furniture Remastered; OriginsPE; ADK; UtilityCraft; DoriosLib; DoriosCore; SmokeyStack addon libraries; Shaiku projects; r4isen1920 projects.

Use large projects as architecture and regression cases for complex blocks, storage entities, cooking states, custom components, connected behavior, and generated content.

### 12. Discovery searches

Bedrock Script API repositories; Bedrock TypeScript addons; Bedrock JavaScript addons; Regolith projects; Bedrock addon libraries; Bedrock server frameworks; automation Script API; machine TypeScript; block-entity addon; and custom-UI addon searches.

Discovery results must be normalized into corpus records before use. Search results alone are not evidence of support or reuse rights.

## Curated Priority References

| Priority | Reference | Primary lesson |
| --- | --- | --- |
| S | Mojang Bedrock Samples and Microsoft Creator API | Supported Bedrock capability baseline |
| S | Bedrock-OSS Wiki | Practical limitations and workarounds |
| S | Bedrock Energistics Core | Generic machine, storage, energy, and fluid abstraction |
| S | Bedrock Energistics | Real consumer of the machine abstraction |
| S | UtilityCraft/DoriosCore | Complex automation architecture |
| S | Bedrock-Core | Interoperability, state replication, RPC, UI, and network |
| S | JaylyDev ScriptAPI | Community runtime implementations |
| S | Forge Capabilities | Java item/fluid/energy abstraction |
| S | Fabric Transfer API | Cross-mod transfer abstraction |
| A | Botarium | Cross-loader item/fluid/energy normalization |
| A | InvSee | Inventory runtime manipulation |
| A | AethelLib | Bedrock framework architecture |
| B | Storage Transfer | Real automation behavior and failure cases |
| B | Random open addons | Edge-case and compatibility regression cases |

## Corpus Extraction Template

For every accepted source, record:

```text
SOURCE
LICENSE AND ADMISSIBILITY
MINECRAFT VERSION
SCRIPT API VERSION
CAPABILITY
IMPLEMENTATION PATTERN
LIMITATIONS
PERFORMANCE CHARACTERISTICS
PERSISTENCE METHOD
RUNTIME HOOKS
TRANSFER SEMANTICS
UI METHOD
REUSABILITY
PHLODGATE ADAPTER CANDIDATE
EVIDENCE POINTERS
CONFIDENCE
```

The final engineering question for every record is:

> Does Bedrock have a viable runtime seam for this Java capability, and what is the strongest proven implementation pattern?

## Prohibited Shortcuts

- Do not copy or redistribute third-party code/assets without verified rights.
- Do not treat free downloads, CurseForge pages, or search results as reuse permission.
- Do not make raw corpus data a runtime dependency.
- Do not make behavior-pack execution the foundation of Java-server behavior.
- Do not advertise visual conversion as gameplay compatibility.
- Do not add mod-specific adapters before generic normalized capability contracts are executable.
- Do not hide missing critical behavior behind a weighted compatibility score.
- Do not leave unsupported, simulated, or diagnostic-only behavior indistinguishable in reports.

Phlodgate already has the right conceptual pipeline: discovery → compatibility analysis → adapter → compiled runtime plan → runtime bridge → Geyser → Bedrock. The biggest gaps it identifies are machines, automation, item/fluid transfer, energy, complex menus, block entities, and mod-specific interactions.

So this is the repository collection I would build around.

1. 🥇 Bedrock Energistics Core — highest priority

Bedrock Energistics Core — GitHub

This is almost tailor-made for Phlodgate.

It provides APIs for:

machines
machine networks
item storage
machine UI
custom storage types
fluids
energy
interoperability between separate Bedrock addons

It specifically exists so machines from different addons can work together.

Why I would study this first

Your biggest problem is:

What is the common abstraction between completely different machine implementations?

Bedrock Energistics gives you a real-world example of someone already attempting that abstraction.

I would extract from it:

Machine
├── identity
├── inventory
├── input
├── output
├── sided access
├── processing
├── recipes
├── energy
├── fluid/storage
├── network membership
├── UI
└── synchronization

Then compare that against Phlodgate's:

ITEM_TRANSFER
FLUID_TRANSFER
ENERGY_TRANSFER
MACHINE_INVENTORY
AUTOMATION_ACCESS

Those categories are already present in your architecture.

This should be one of your primary adapter test/reference implementations.

2. 🥇 Bedrock Energistics

Bedrock Energistics — CurseForge

Don't just study the Core library.

Study the actual addon built on top of it.

The current project has machines and custom UI and is built around Bedrock Energistics Core.

This gives Phlodgate two valuable things:

Core API

"What does a standardized machine interface look like?"

Real addon

"How does an actual content-heavy addon consume that interface?"

That distinction is extremely important for your adapter.

3. 🥇 DoriosCore / UtilityCraft

UtilityCraft Addon Template — GitHub

This is another extremely valuable compatibility target.

The template exposes:

machines
energy
liquids
gases
item integration
processing
multiblocks
upgrades
link-node ports
automation
crop systems
Bonsai integration

That's almost a checklist of the systems Phlodgate needs to understand.

I'd treat UtilityCraft as a second architecture family.

The important question for Phlodgate isn't:

"Can I support UtilityCraft?"

It's:

"Can I recognize UtilityCraft-like architecture automatically?"

That's much more powerful.

4. 🥇 Bedrock Script API ecosystem

Microsoft Bedrock Script API libraries

Bedrock-OSS Wiki — GitHub

This is your API capability reference.

The Bedrock Script API is the actual foundation Phlodgate has to target on the Bedrock side.

The Bedrock Wiki tracks the available Script API modules and their capabilities.

This should become part of your adapter's capability matrix.

For example:

Java capability
        ↓
Can Bedrock represent it?
        ↓
Native API
        ↓
Script API
        ↓
Emulation
        ↓
Approximation
        ↓
Impossible

That's much better than simply:

supported = true
5. 🥇 JaylyDev ScriptAPI

JaylyDev ScriptAPI — GitHub

This is particularly useful because it's a collection of real community implementations, rather than only documentation.

It contains community-driven Bedrock Script API examples covering a wide variety of behaviors.

For Phlodgate, I'd mine it for:

inventory manipulation
entities
block interaction
UI
custom systems
automation
event handling
persistence
scripting workarounds

This is useful for answering:

"Someone already solved this Bedrock-side problem. How?"

6. 🥇 Mojang Bedrock Samples

Mojang Bedrock Samples — GitHub

This is your ground truth.

Don't use community addons as the authority for what Bedrock can do.

Use Mojang's repository to establish the actual supported Bedrock resource/behavior architecture.

I would have Phlodgate's compatibility analyzer treat Mojang's supported structures as the baseline.

7. 🥇 Bedrock-OSS Wiki

Bedrock-OSS Bedrock Wiki — GitHub

This is probably the best community technical reference to pair with Mojang's official material.

The project is open source and specifically documents Bedrock addon development and Script API behavior.

This is particularly useful when determining:

"Can this Java behavior actually be represented on Bedrock?"

8. 🟢 Open-source inventory implementations

Your current architecture explicitly calls out:

ITEM_TRANSFER
MACHINE_INVENTORY
AUTOMATION_ACCESS

So I'd deliberately collect Bedrock projects that manipulate inventories.

One example:

InvSee — GitHub

It's useful because it demonstrates real Script API inventory manipulation rather than simply defining JSON content.

This category matters enormously for your adapter because inventory is the gateway to automation.

9. 🟢 Storage-transfer implementations

There are also useful projects that demonstrate actual item movement.

For example:

Storage Transfer — CurseForge

It implements moving nearby dropped items into storage.

It isn't open source, so don't copy its implementation.

But it is valuable as a behavioral test case:

World item
     ↓
Search
     ↓
Target storage
     ↓
Insertion
     ↓
Stack handling
     ↓
Failure/full inventory

That is exactly the kind of runtime behavior your generic transfer bridge needs to understand.

10. 🟢 AethelLib

AethelLib — CurseForge

This is worth investigating because it is a relatively modern Bedrock framework and is open source under AGPLv3.

For Phlodgate, frameworks are arguably more useful than individual addons because they reveal reusable architectural patterns.

11. 🔥 Your biggest opportunity: Java-side capability standards

This is where I think Phlodgate can become much more powerful.

Don't only collect Bedrock addons.

Collect Java mod APIs that represent the same concepts.

For example, Forge exposes:

IItemHandler
IFluidHandler
IEnergyStorage

through its capability system.

Fabric has its own transfer abstractions for:

item storage
fluid storage
transfer

And projects such as Botarium explicitly attempt to standardize item, fluid and energy storage/transfer.

That gives you a much better architecture:

                 JAVA
                   │
        ┌──────────┼──────────┐
        │          │          │
   Forge Caps   Fabric API  Mod API
        │          │          │
        └──────────┼──────────┘
                   ↓
          PHLODGATE NORMALIZER
                   ↓
       ┌───────────┼───────────┐
       │           │           │
      Item        Fluid       Energy
       │           │           │
       └───────────┼───────────┘
                   ↓
            RUNTIME PLAN
                   ↓
          BEDROCK ADAPTER

That is the adapter architecture I'd aim for.

The repositories I'd prioritize

If you're building a Phlodgate Adapter Research Corpus, I'd rank them:

Priority	Project	What it teaches Phlodgate
🔴 S	Bedrock Energistics Core	Machine/storage/energy/fluid abstraction
🔴 S	Bedrock Energistics	Real machine implementation
🔴 S	UtilityCraft/DoriosCore	Complex automation architecture
🔴 S	Mojang Bedrock Samples	Actual Bedrock capability baseline
🔴 S	Bedrock-OSS Wiki	Practical Bedrock limitations/workarounds
🔴 S	JaylyDev ScriptAPI	Real Script API implementations
🔴 S	Forge Capabilities	Java item/fluid/energy abstraction
🔴 S	Fabric Transfer API	Cross-mod transfer abstraction
🟠 A	Botarium	Cross-loader item/fluid/energy model
🟠 A	InvSee	Inventory runtime manipulation
🟠 A	AethelLib	Bedrock framework architecture
🟡 B	Storage Transfer	Real automation behavior
🟡 B	Random MCPEDL addons	Edge cases / compatibility testing
🟡 B	Random CurseForge addons	Edge cases / compatibility testing
But here's the important part

I would not tell a coding agent to simply download these projects and copy their code.

Instead, give Phlodgate a reference corpus.

For every project, extract:

Capability
├── Inventory
│   ├── slot access
│   ├── insert
│   ├── extract
│   ├── simulation
│   ├── sided access
│   └── filters
│
├── Fluid
│   ├── tank
│   ├── insert
│   ├── extract
│   ├── capacity
│   ├── fluid identity
│   └── sided access
│
├── Energy
│   ├── capacity
│   ├── receive
│   ├── extract
│   ├── rate
│   └── sided access
│
├── Machine
│   ├── recipe
│   ├── processing
│   ├── progress
│   ├── inputs
│   ├── outputs
│   ├── catalysts
│   └── state
│
├── Automation
│   ├── insertion
│   ├── extraction
│   ├── pipes
│   ├── conveyors
│   ├── filtering
│   ├── routing
│   └── network
│
└── Presentation
    ├── menu
    ├── block entity
    ├── model
    ├── animation
    └── state

Then map those into the runtime capabilities that Phlodgate already defines:

MENU_CONTAINER
BLOCK_ENTITY_DATA
FLUID_RUNTIME
ITEM_BEHAVIOR
ITEM_TRANSFER
FLUID_TRANSFER
ENERGY_TRANSFER
MACHINE_INVENTORY
AUTOMATION_ACCESS

And I would add one more layer

Your ultimate adapter should not be "Bedrock addon → Java mod."

It should be:

              MOD DISCOVERY
                    ↓
             CAPABILITY GRAPH
                    ↓
       ┌────────────┴────────────┐
       ↓                         ↓
   JAVA API                  MOD-SPECIFIC
       ↓                      ADAPTER
       └────────────┬────────────┘
                    ↓
          NORMALIZED CAPABILITY
                    ↓
           COMPATIBILITY PLAN
                    ↓
          RUNTIME EXECUTION
                    ↓
              GEYSER
                    ↓
               BEDROCK

That fits your existing architecture extremely well because Phlodgate already explicitly separates generic compatibility from known adapters, then compiles the result into a runtime plan.

The real target should therefore be:

"Can Phlodgate recognize the capability, regardless of which mod implemented it?"

rather than:

"Do we have an adapter for Mod X?"

That distinction is what could let Phlodgate eventually support hundreds or thousands of mods without thousands of hand-written adapters.

And the Bedrock Energistics + UtilityCraft + Forge Capabilities + Fabric Transfer API combination is probably the most valuable reference set for designing that generalized adapter.