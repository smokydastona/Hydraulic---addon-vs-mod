val modId = project.property("mod_id") as String

architectury {
    platformSetupLoomIde()
    fabric()
}

fabricApi {
    configureDataGeneration() {
        client = true
        outputDirectory.set(layout.buildDirectory.file("generated/datagen"))
        addToResources.set(false)
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
    val stagedGeneratedResources = layout.buildDirectory.dir("generated/runtimeResources")

    val syncGeneratedResources = register<Copy>("syncGeneratedResources") {
        from(layout.buildDirectory.dir("generated/datagen"))
        into(stagedGeneratedResources)
        mustRunAfter(named("runDatagen"))
    }

    val prepareGeneratedResources = register("prepareGeneratedResources") {
        dependsOn(named("runDatagen"))
        dependsOn(syncGeneratedResources)
    }

    sourcesJar {
        dependsOn(prepareGeneratedResources) // Make sure the sources jar gets our generated files
        from(stagedGeneratedResources)
    }

    named<Jar>("jar") {
        dependsOn(syncGeneratedResources)
        from(stagedGeneratedResources)
    }

    named<Jar>("mergeShadowAndJarJar") {
        dependsOn(prepareGeneratedResources)
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
}

// Always ensure datagen is up to date before building
tasks.named("build") { dependsOn(tasks.named("prepareGeneratedResources")) }

dependencies {
    implementation(libs.fabric.loader)
    api(libs.fabric.api)
    compileOnly(project(":shared"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testCompileOnly(project(":shared"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
}

tasks.test {
    useJUnitPlatform()
}