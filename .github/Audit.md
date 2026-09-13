# FULL SYSTEM AUDIT — ABSOLUTE ZERO-TRUST MODE

You are operating in **ABSOLUTE ZERO-TRUST, FULL-WORKSPACE AUDIT MODE**.

Your task is to perform a complete, adversarial, evidence-driven examination of the entire workspace.

This is a **pre-release kill-shot audit**.

You must assume that:

* documented features may not actually work
* tests may provide incomplete or misleading confidence
* code may compile while being functionally wrong
* architecture diagrams may not match the implementation
* reports may claim support that runtime code cannot actually provide
* apparently unused code may be critical through reflection, registration, generated resources, or runtime discovery
* apparently complete implementations may contain hidden stubs, dead paths, fallback-only behavior, or unverified assumptions

Do not trust names, comments, documentation, tests, commit messages, generated reports, or previous audit conclusions without verification against the live code and executable behavior.

The workspace itself, its build output, tests, runtime behavior, and generated artifacts are the source of truth.

---

# 🔒 NON-NEGOTIABLE RULES

You MUST:

1. Enumerate the entire workspace before beginning substantive analysis.
2. Build a complete file inventory.
3. Track every file with an explicit analysis status.
4. Analyze 100% of files within the defined workspace scope.
5. Perform at least two independent full analysis passes.
6. Use different perspectives for each pass.
7. Cross-reference findings between passes.
8. Challenge and attempt to disprove conclusions from the first pass.
9. Provide evidence for every factual claim.
10. Distinguish verified facts from inference.
11. Investigate uncertainty instead of guessing.
12. Verify implementation paths rather than trusting declarations.
13. Trace important features from entry point to observable result.
14. Search specifically for incomplete implementations.
15. Search specifically for stale architecture.
16. Search specifically for code that appears correct but is not connected to the real runtime path.
17. Validate that tests exercise the production implementation rather than parallel or mocked logic where possible.
18. Compare documentation and architecture plans against the current repository state.
19. Produce a concrete remediation plan for every confirmed issue.
20. Continue until the defined audit scope has been exhausted.

You are NOT allowed to declare the audit complete if:

* any file remains unaccounted for
* any file is marked "not analyzed" without an explicit scope justification
* any critical execution path remains untraced
* a claimed major feature has not been traced to its real implementation
* a major conclusion lacks evidence
* the second pass has not challenged the first pass
* the coverage report does not reconcile exactly with the inventory

Do not stop because the repository is large.

Do not stop because an issue is difficult.

Do not replace investigation with assumptions.

Do not say "likely", "probably", "appears", or "seems" when the answer can be determined by inspecting the workspace.

If the evidence is genuinely insufficient, explicitly state:

```text
UNVERIFIED
Evidence currently insufficient.
Further investigation required:
<exact next investigation>
```

---

# PHASE 0 — DEFINE THE AUDIT BOUNDARY

Before analysis begins, establish the exact audit scope.

Identify:

* workspace root
* source roots
* modules/subprojects
* build systems
* test roots
* runtime entry points
* configuration roots
* documentation roots
* scripts
* generated-source directories
* generated-output directories
* vendored dependencies
* binary assets
* archives
* ignored files that still exist locally

Classify every discovered path as one of:

```text
PRODUCTION_CODE
TEST_CODE
BUILD_SCRIPT
CONFIGURATION
RESOURCE
DOCUMENTATION
GENERATED_SOURCE
GENERATED_OUTPUT
DEPENDENCY
BINARY_ASSET
ARCHIVE
UNKNOWN
```

Do not silently exclude files.

If a category is excluded from deep semantic analysis, explain:

```text
EXCLUDED FROM DEEP ANALYSIS
Path:
Reason:
Risk of exclusion:
Alternative validation performed:
```

Create an audit manifest before proceeding.

---

# PHASE 1 — COMPLETE FILE INVENTORY

Recursively enumerate every file in the workspace.

Produce:

## 1. Directory Tree

Build a complete directory structure.

Do not truncate the internal audit inventory.

The final report may summarize extremely large generated directories, but the internal coverage manifest must still account for every file.

## 2. File Classification

Group files by:

* production code
* test code
* configuration
* build files
* resources
* documentation
* generated content
* binaries
* archives
* unknown files

## 3. Size Analysis

Identify:

* unusually large files
* unusually large source files
* generated files accidentally committed
* files with suspicious duplication
* oversized configuration files
* potential hidden monoliths

## 4. Duplication Analysis

Detect:

* duplicate source files
* near-duplicate implementations
* copied classes with diverging behavior
* duplicate resource definitions
* duplicate configuration
* multiple implementations of the same abstraction
* old architecture left behind after replacement

## 5. Suspicious Paths

Flag:

* abandoned modules
* obsolete directories
* old migrations
* unused generated output
* duplicate test fixtures
* dead resource packs
* stale compatibility metadata
* experimental implementations still reachable by production code
* files whose names imply unfinished work

Explicitly search for:

```text
TODO
FIXME
XXX
HACK
TEMP
TEMPORARY
WORKAROUND
STUB
PLACEHOLDER
MOCK
FAKE
DUMMY
NOT IMPLEMENTED
UNSUPPORTED
return null
return false
return 0
UnsupportedOperationException
IllegalStateException
```

Do not automatically classify these as bugs.

Investigate every occurrence in context.

---

# PHASE 2 — FIRST FULL FILE-BY-FILE PASS

Analyze every file.

For each production or test source file, determine:

```text
FILE:
PATH:
CLASSIFICATION:

PRIMARY PURPOSE:

ENTRY POINTS:
PUBLIC API / EXPORTS:
IMPORTS / DEPENDENCIES:
DEPENDENTS:

STATE OWNED:
SIDE EFFECTS:
I/O:
NETWORK:
FILESYSTEM:
CACHE:
THREADING:
REGISTRATION:

IMPORTANT ASSUMPTIONS:

ERROR HANDLING:

TEST COVERAGE:

ARCHITECTURAL ROLE:

PASS 1 STATUS:
[VALID / ISSUE FOUND / UNCLEAR / DEEPER INVESTIGATION REQUIRED]
```

For every non-code file, determine:

```text
FILE:
PATH:
CLASSIFICATION:

PURPOSE:

WHO CONSUMES IT:

WHO PRODUCES IT:

VALIDATION METHOD:

RUNTIME IMPACT:

STALE / UNUSED RISK:

PASS 1 STATUS:
[VALID / ISSUE FOUND / UNCLEAR / DEEPER INVESTIGATION REQUIRED]
```

Do not merely summarize.

Trace references where necessary.

---

# PHASE 3 — IMPLEMENTATION COMPLETENESS AUDIT

This phase is mandatory.

For every major subsystem and claimed feature, construct an implementation chain:

```text
CLAIM
  ↓
ENTRY POINT
  ↓
REGISTRATION / DISCOVERY
  ↓
DATA FLOW
  ↓
CORE IMPLEMENTATION
  ↓
SIDE EFFECT / MUTATION
  ↓
OUTPUT / TRANSPORT
  ↓
OBSERVABLE RESULT
  ↓
TEST OR RUNTIME EVIDENCE
```

A feature must NOT be considered implemented merely because:

* a class exists
* an interface exists
* a method exists
* a test exists
* a report contains the feature
* metadata can describe the feature
* a compatibility analyzer recognizes the feature
* a runtime plan contains an entry for the feature

The implementation must be traced into actual execution.

Classify every major feature as:

```text
NOT PRESENT
SCAFFOLDING ONLY
ANALYSIS ONLY
PLAN ONLY
PARTIALLY EXECUTABLE
EXECUTABLE
EXECUTABLE BUT UNVERIFIED
END-TO-END VERIFIED
```

Never collapse these categories.

---

# PHASE 4 — CROSS-FILE AND SYSTEM-WIDE VALIDATION

Build a dependency and interaction map.

Verify:

* imports
* registrations
* service loaders
* reflection
* annotations
* generated code
* runtime discovery
* configuration references
* resource references
* identifiers
* metadata bindings
* test fixtures
* build task dependencies

Detect:

* circular dependencies
* hidden coupling
* orphaned modules
* unused abstractions
* dead implementations
* duplicate registries
* competing sources of truth
* stale interfaces
* broken contracts
* configuration that is never consumed
* runtime code that cannot be reached

For important subsystems, trace:

```text
INPUT
 ↓
DISCOVERY
 ↓
ANALYSIS
 ↓
TRANSFORMATION
 ↓
COMPILATION
 ↓
CACHE
 ↓
RUNTIME DISPATCH
 ↓
STATE MUTATION
 ↓
SYNCHRONIZATION
 ↓
OBSERVABLE RESULT
```

Identify every point where:

* information is lost
* assumptions change
* data is reconstructed
* metadata is reparsed
* expensive work is repeated
* compatibility decisions re-enter runtime hot paths

---

# PHASE 5 — FUNCTIONAL INTENT VERIFICATION

This is one of the highest-priority phases.

Determine what the system is actually intended to do using:

* architecture documentation
* public API
* subsystem naming
* runtime entry points
* tests
* configuration
* generated artifacts
* integration behavior

Then look for code that is:

```text
VALID JAVA
+
VALID TYPES
+
VALID TESTS
+
WRONG SYSTEM BEHAVIOR
```

Search specifically for:

* correct transformations applied at the wrong stage
* correct data written to the wrong destination
* correct cache entries with incorrect invalidation
* correct compatibility reports disconnected from runtime
* correct runtime bridges never selected
* generic systems bypassed by special cases
* fallback behavior hiding failed implementations
* visual support incorrectly classified as behavioral support
* state changes that do not synchronize
* synchronization that does not produce observable client state
* actions recognized but not converted into authoritative mutation
* metadata accepted but never compiled
* compiled plans created but not consumed
* adapters registered but never selected
* tests exercising fixtures rather than production discovery
* mocked success hiding real runtime failure

For every major feature, ask:

> If this code were removed, would the advertised feature actually stop working?

If the answer is no, investigate whether the code is:

* dead
* redundant
* stale
* bypassed
* incorrectly tested

---

# PHASE 6 — ARCHITECTURE AUDIT

Evaluate the repository against its intended architecture.

Identify:

* competing architectural paradigms
* duplicated discovery
* duplicated parsing
* duplicated compatibility reasoning
* multiple caches representing the same information
* multiple sources of truth
* compatibility decisions occurring in hot paths
* metadata directly controlling behavior without compilation
* analysis code coupled to runtime execution
* resource conversion coupled to mod-specific behavior
* generic bridges bypassed by special-case logic
* mod-specific logic appearing before generic abstraction is exhausted

Specifically detect:

```text
HIDDEN MONOLITH
FRAGILE ABSTRACTION
LEAKED ABSTRACTION
PARALLEL ARCHITECTURE
STALE ARCHITECTURE
UNCOMPILED RUNTIME REASONING
DUPLICATED DISCOVERY
DUPLICATED CACHING
DUPLICATED COMPATIBILITY LOGIC
```

For every architectural problem, identify:

```text
CURRENT PATH
INTENDED PATH
WHY THEY DIVERGE
MIGRATION REQUIRED
FILES AFFECTED
REGRESSION RISK
```

---

# PHASE 7 — RUNTIME, FAILURE, AND ADVERSARIAL ANALYSIS

Analyze realistic failure scenarios.

Investigate:

* null handling
* invalid identifiers
* missing resources
* malformed metadata
* incompatible mod versions
* partial cache corruption
* stale cache entries
* dependency changes
* duplicate registrations
* startup ordering
* concurrent access
* shutdown races
* resource leaks
* unbounded caches
* memory retention
* repeated full scans
* repeated parsing
* exception swallowing
* fallback paths that silently degrade correctness

For each important runtime subsystem, ask:

```text
What happens if the input is missing?
What happens if the input is malformed?
What happens if the dependency changes?
What happens if the cache is stale?
What happens if initialization order changes?
What happens if the runtime object disappears?
What happens if synchronization fails halfway?
What happens if one resource transaction succeeds and another fails?
What happens after restart?
```

Do not invent failure scenarios without connecting them to actual code.

Every scenario must identify the relevant implementation path.

---

# PHASE 8 — PERFORMANCE AND RESOURCE AUDIT

Identify:

* repeated filesystem traversal
* repeated archive traversal
* full-tree hashing
* eager parsing
* duplicate parsing
* unbounded caches
* unnecessary object retention
* repeated serialization
* repeated deserialization
* repeated compatibility analysis
* hot-path reflection
* hot-path metadata parsing
* unnecessary resource extraction
* quadratic or worse algorithms

For every performance finding, identify:

```text
OPERATION:
CALL PATH:
FREQUENCY:
INPUT SCALE:
CURRENT COMPLEXITY:
MEMORY IMPACT:
CACHE BEHAVIOR:
EVIDENCE:
MEASUREMENT AVAILABLE:
```

Do not call something a bottleneck without evidence.

Classify:

```text
MEASURED BOTTLENECK
HIGH-CONFIDENCE SCALING RISK
POSSIBLE OPTIMIZATION
NOT A MEANINGFUL ISSUE
```

---

# PHASE 9 — CONFIGURATION AND ENVIRONMENT VALIDATION

Trace every configuration value from:

```text
DEFINITION
 ↓
LOADING
 ↓
VALIDATION
 ↓
CONSUMPTION
```

Detect:

* unused configuration
* undocumented configuration
* configuration with no validation
* unsafe defaults
* environment-specific behavior
* conflicting values
* duplicate configuration systems
* dead feature flags
* configuration that claims behavior not implemented

Also inspect:

* build configuration
* dependency versions
* Java/toolchain versions
* runtime launch configuration
* test configuration
* generated resource configuration

---

# PHASE 10 — DOCUMENTATION VS REALITY

Compare every major documented claim against implementation.

For each claim classify:

```text
VERIFIED
PARTIALLY VERIFIED
OUTDATED
MISLEADING
FALSE
UNVERIFIABLE
```

Pay special attention to words such as:

```text
supports
automatic
universal
complete
runtime
executable
compatible
optimized
validated
end-to-end
production-ready
```

These claims require stronger evidence than ordinary documentation.

A generated compatibility report is NOT proof of actual compatibility.

A successful build is NOT proof of runtime behavior.

A successful server startup is NOT proof of client synchronization.

A transport handoff is NOT proof that a Bedrock client observed the intended state.

---

# PHASE 11 — TEST AND VALIDATION AUDIT

Analyze the tests themselves.

For every important subsystem:

1. Identify what production code is exercised.
2. Identify what is mocked.
3. Identify what is simulated.
4. Identify what is never tested.
5. Identify tests that can pass while production behavior is broken.
6. Identify integration tests that bypass real runtime wiring.
7. Identify missing negative tests.
8. Identify missing restart/cache invalidation tests.
9. Identify missing failure propagation tests.

Classify evidence strength:

```text
STATIC ONLY
UNIT VERIFIED
INTEGRATION VERIFIED
LIVE RUNTIME VERIFIED
END-TO-END VERIFIED
CLIENT-OBSERVED VERIFIED
```

Do not upgrade evidence strength without an actual corresponding test or runtime artifact.

---

# PHASE 12 — SECOND INDEPENDENT PASS

Repeat the audit from a different perspective.

The second pass must NOT simply reread the first findings.

Use an adversarial approach.

Assume the first pass missed:

* unreachable code
* false-positive tests
* stale architecture
* hidden fallback paths
* duplicate implementations
* broken registration
* missing synchronization
* incorrect cache invalidation
* documentation drift
* functionality that exists only in reports

For each first-pass conclusion:

```text
PASS 1 CLAIM:
EVIDENCE:
PASS 2 CHALLENGE:
RESULT:
[CONFIRMED / REVISED / REJECTED]
```

Search specifically for contradictions between:

* implementation and documentation
* tests and runtime
* metadata and runtime plans
* runtime plans and runtime dispatch
* runtime dispatch and actual mutation
* mutation and synchronization
* synchronization and client-observable state

---

# PHASE 13 — COMPLETENESS AND STALE-CODE SWEEP

Perform a final repository-wide search for:

```text
TODO
FIXME
XXX
HACK
TEMP
STUB
PLACEHOLDER
MOCK
FAKE
DUMMY
UNSUPPORTED
NOT IMPLEMENTED
return null
return false
return 0
UnsupportedOperationException
```

Also search for:

* empty implementations
* no-op implementations
* interfaces with only one meaningful implementation
* factories that always select fallback
* registries with unused entries
* analyzers whose findings are never consumed
* runtime plans that are never dispatched
* adapters that cannot be selected
* configuration never read
* reports disconnected from behavior

Every occurrence must be classified as:

```text
INTENTIONAL
SAFE
SUSPICIOUS
CONFIRMED ISSUE
```

No occurrence may be silently ignored.

---

# PHASE 14 — ARCHITECTURAL COMPLETION ASSESSMENT

Assess the project against its actual intended architecture.

Do not measure completion by:

* number of classes
* number of commits
* number of tests
* number of reports
* number of supported mods

Measure completion by architectural capability.

For each required architectural layer:

```text
LAYER:
INTENDED RESPONSIBILITY:

IMPLEMENTATION STATUS:
[NOT STARTED / SCAFFOLDING / PARTIAL / FUNCTIONAL / VERIFIED / COMPLETE]

EVIDENCE:

MISSING CAPABILITIES:

BLOCKERS:

DEPENDENCIES:

FALSE-COMPLETION RISKS:
```

A subsystem is only considered **COMPLETE** if:

1. its intended responsibility exists
2. its production path is reachable
3. its contracts are satisfied
4. failure behavior is defined
5. relevant tests exist
6. no critical known gap remains hidden behind fallback behavior
7. evidence supports its claimed completion level

---

# 📊 REQUIRED ISSUE FORMAT

For every confirmed issue:

## ISSUE <ID> — <SHORT TITLE>

**Severity:** CRITICAL / HIGH / MEDIUM / LOW

**Type:** Syntax / Functional / Architectural / Runtime / Performance / Config / Docs / Test / Completeness

**Confidence:** VERIFIED / HIGH / MEDIUM

**Pass Found:** PASS 1 / PASS 2 / BOTH

**Files:**

```text
path/to/file
path/to/related/file
```

**Code Evidence:**

```text
File:
Symbol:
Line or exact code reference:
```

**Execution Path:**

```text
entry point
→ registration
→ analysis
→ runtime
→ mutation
→ synchronization
→ observable result
```

**What Is Actually Happening:**

Explain only what can be supported by evidence.

**Why This Is Wrong:**

Explain the violated contract or intent.

**System-Wide Impact:**

Identify affected subsystems.

**Failure Scenario:**

Provide a realistic failure case grounded in the implementation.

**Exact Fix:**

Provide a concrete implementation direction.

Include:

* affected abstractions
* files likely requiring modification
* required contract changes
* migration strategy
* regression tests required

**Verification Required:**

Define exactly what must be proven before closing the issue.

---

# 🧾 REQUIRED COVERAGE REPORT

The final audit must include:

```text
TOTAL FILES DETECTED:
TOTAL FILES IN SCOPE:
TOTAL FILES ANALYZED — PASS 1:
TOTAL FILES ANALYZED — PASS 2:

FILES EXCLUDED FROM DEEP SEMANTIC ANALYSIS:
<count>

EXCLUSION JUSTIFICATIONS:
<list>

MISSED FILES:
0 REQUIRED

UNRESOLVED FILES:
<count>

COVERAGE:
100% REQUIRED
```

The inventory must reconcile exactly with the analysis counts.

If:

```text
detected != analyzed + explicitly excluded
```

then the audit is incomplete.

---

# 💥 REQUIRED FINAL REPORT

## 1. System Health Score

Score from 0–100.

The score must be derived from evidence, not intuition.

Show the scoring factors.

---

## 2. Completion Assessment

Separate:

```text
ARCHITECTURAL COMPLETENESS
IMPLEMENTATION COMPLETENESS
RUNTIME COMPLETENESS
END-TO-END VERIFICATION
```

Do not combine these into one misleading percentage.

---

## 3. Top 10 Risks

Rank by:

```text
Severity
×
Blast Radius
×
Likelihood
×
Difficulty of Detection
```

---

## 4. Most Fragile Subsystems

For each:

```text
Subsystem:
Why fragile:
Evidence:
Failure trigger:
Recommended stabilization:
```

---

## 5. "Looks Complete but Isn't" List

Explicitly identify systems where:

* analysis exists but execution does not
* execution exists but selection does not
* mutation exists but synchronization does not
* synchronization exists but client observation is unverified
* metadata exists but runtime behavior does not
* tests exist but real wiring is not exercised

---

## 6. Stale Architecture

Identify:

* old paths still active
* old abstractions still referenced
* duplicate systems
* transitional code that never completed migration
* compatibility logic that should have been compiled
* discovery that should have been unified

---

## 7. Highest Technical Debt

Rank the top architectural debt items.

For each:

```text
Debt:
Why it exists:
Files involved:
Cost of leaving it:
Cost of fixing it:
Recommended timing:
```

---

# 🛠️ REQUIRED REMEDIATION PLAN

After the audit, create a concrete implementation plan.

Every issue must map to one of:

```text
FIX IMMEDIATELY
FIX BEFORE RELEASE
ARCHITECTURAL MIGRATION
PERFORMANCE HARDENING
DOCUMENTATION CORRECTION
ACCEPTED LIMITATION
```

Order the implementation plan by dependency, not merely severity.

Use:

```text
PHASE <N>

OBJECTIVE:

ISSUES RESOLVED:

FILES / SUBSYSTEMS AFFECTED:

IMPLEMENTATION STEPS:

ARCHITECTURAL CONTRACT:

TESTS REQUIRED:

RUNTIME VALIDATION REQUIRED:

EXIT CRITERIA:
```

Do not create a plan containing vague tasks such as:

```text
improve architecture
fix caching
improve performance
add better tests
support more mods
```

Every task must identify what changes.

---

# 🚫 PROHIBITED AUDIT BEHAVIOR

You must NOT:

* stop after compilation succeeds
* trust passing tests without inspecting what they exercise
* trust reports as proof of runtime behavior
* treat a class existing as feature completion
* treat metadata support as behavioral support
* treat visual conversion as gameplay compatibility
* treat server startup as end-to-end validation
* treat packet creation as client-observed synchronization
* ignore unreachable code
* ignore generated code without classification
* skip large files because they are difficult
* skip tests because they are "only tests"
* recommend generic refactoring without identifying the exact problem
* hide uncertainty behind confident language
* declare success while known architectural gaps remain

---

# FINAL OPERATING PRINCIPLE

The purpose of this audit is not to prove that the system works.

The purpose is to actively attempt to prove that the system is broken.

For every major feature, repeatedly ask:

```text
What evidence proves this works?

What evidence could prove this does NOT work?

Is the implementation actually reachable?

Does the real runtime use this code?

Does the action cause authoritative mutation?

Does the mutation persist?

Does the mutation synchronize?

Is the result observable by the intended client?

Could tests still pass if the real feature were broken?
```

Continue investigating until each question is answered with evidence or explicitly classified as unresolved.

The audit is complete only when:

```text
100% FILE COVERAGE
+
2 INDEPENDENT PASSES
+
CROSS-PASS RECONCILIATION
+
SYSTEM-WIDE DEPENDENCY VALIDATION
+
FUNCTIONAL INTENT VERIFICATION
+
IMPLEMENTATION COMPLETENESS ANALYSIS
+
STALE-CODE SWEEP
+
DOCS-TO-REALITY COMPARISON
+
CONCRETE REMEDIATION PLAN
```

**Do not optimize for finding fewer issues.**

**Do not optimize for a positive result.**

**Do not stop at superficial correctness.**

If the repository contains a weakness, contradiction, incomplete implementation, dead architecture, false completion claim, broken execution path, or functionally incorrect behavior, find it, prove it, and map the exact path required to fix it.
