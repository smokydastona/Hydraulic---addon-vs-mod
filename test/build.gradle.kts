val modId = project.property("mod_id") as String

architectury {
    platformSetupLoomIde()
    fabric()
}

fabricApi {
    configureDataGeneration() {
        client = true
    }
}

val common = configurations.create("common")
val developmentFabric = configurations.named("developmentFabric").get()

configurations {
    compileClasspath.get().extendsFrom(configurations["common"])
    runtimeClasspath.get().extendsFrom(configurations["common"])
    developmentFabric.extendsFrom(configurations["common"])
}

tasks {
    val syncGeneratedResources = register<Copy>("syncGeneratedResources") {
        dependsOn(named("runDatagen"))
        from("src/main/generated")
        into(layout.buildDirectory.dir("resources/main"))
    }

    sourcesJar {
        dependsOn(named("runDatagen")) // Make sure the sources jar gets our generated files
    }

    named<Jar>("jar") {
        dependsOn(syncGeneratedResources)
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
        archiveBaseName.set("${modId}-test-mod-fabric")
    }

    shadowJar {
        archiveClassifier.set("dev-shadow")
    }

    jar {
        archiveClassifier.set("dev")
    }

    named("runServer") {
        dependsOn(syncGeneratedResources)
    }

    named("runClient") {
        dependsOn(syncGeneratedResources)
    }
}

// Always ensure datagen is up to date before building
tasks.named("build") { dependsOn(tasks.named("runDatagen")) }

dependencies {
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
}