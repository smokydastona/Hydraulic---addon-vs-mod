val modId = project.property("mod_id") as String

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    runs {
        named("server") {
            runDir("run")
        }
    }
}

val common = configurations.create("common")
val developmentFabric = configurations.named("developmentFabric").get()
val includeTransitive = configurations.named("includeTransitive").get()
val geyserCoreLocales = configurations.create("geyserCoreLocales")
val geyserRuntimeResourcesDir = layout.buildDirectory.dir("generated/geyser-runtime-resources")

configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentFabric.extendsFrom(configurations["common"])
}

tasks {
    val syncGeyserRuntimeResources = register<Sync>("syncGeyserRuntimeResources") {
        from({ geyserCoreLocales.resolve().map(::zipTree) })
        include("mappings/**")
        into(geyserRuntimeResourcesDir)
    }

    val syncGeyserLocales = register<Sync>("syncGeyserLocales") {
        from({ geyserCoreLocales.resolve().map(::zipTree) })
        include("languages/texts/*.properties")
        eachFile {
            path = name
        }
        includeEmptyDirs = false
        into(layout.projectDirectory.dir("run/config/Geyser-Fabric/languages"))
    }

    named<Jar>("mergeShadowAndJarJar") {
        from (
            zipTree( shadowJar.map { it.outputs.files.singleFile } ).matching {
                exclude("fabric.mod.json")
                exclude("LICENSE")
            },
            zipTree( jar.map { it.outputs.files.singleFile } ).matching {
                include("META-INF/jars/**")
                include("fabric.mod.json")
                include("LICENSE")
            }
        )
        archiveBaseName.set("${modId}-fabric")
    }

    shadowJar {
        archiveClassifier.set("dev-shadow")
        relocate("org.spongepowered.configurate", "org.geysermc.hydraulic.shaded.org.spongepowered.configurate")
    }

    jar {
        archiveClassifier.set("dev")
    }

    named<ProcessResources>("processResources") {
        dependsOn(syncGeyserRuntimeResources)
    }

    sourcesJar {
        dependsOn(syncGeyserRuntimeResources)
    }

    named("runServer") {
        dependsOn(syncGeyserLocales)
    }
}

dependencies {
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    common(project(":shared")) { isTransitive = false }
    compileOnly(libs.geyser.api)

    shadow(project(path = ":shared", configuration = "transformProductionFabric")) {
        isTransitive = false
    }

    runtimeOnly(libs.pack.converter)
    runtimeOnly(libs.examination.api)
    runtimeOnly(libs.examination.string)
    includeTransitive(libs.pack.converter)
    geyserCoreLocales(libs.geyser.core)

    localRuntime(libs.bundles.configurate)
    shadow(libs.bundles.configurate) { isTransitive = false }

    localRuntime(libs.geyser.fabric) {
        exclude(group = "io.netty")
        exclude(group = "io.netty.incubator")
        exclude(group = "org.incendo")
    }

    localRuntime(project(":test"))
}

sourceSets {
    main {
        resources {
            srcDirs(project(":shared").sourceSets["main"].resources.srcDirs)
            srcDir(geyserRuntimeResourcesDir)
        }
    }
}
