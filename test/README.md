# Hydraulic Test Mod
In this module contains the mod used the test Hydraulic's conversions. It is not designed for actual use on servers, since it contains some silly items.

## Building
Building the test mod is rather straight forward:
1. Complete project setup, see the main README for more details.
2. Run `./gradlew test:build` to compile a jar file.

## Bundled Metadata Fixture
The test mod now ships a committed Hydraulic metadata fixture at `test/src/main/resources/hydraulic/metadata/hydraulic_test_mod.golden_barrel.json`.

When the test mod initializes, it copies that file into Hydraulic's runtime config directory at `config/hydraulic/metadata/hydraulic_test_mod.golden_barrel.json` before Hydraulic reaches its `SERVER_STARTING` metadata load path. That keeps the `golden_barrel`, `barrel_pack`, `barrel_menu`, and `barrel_cube` compatibility/runtime validation fixture reproducible from tracked repository content instead of depending on a manually edited ignored file under `fabric/run/`.