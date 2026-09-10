architectury {
    common("neoforge", "fabric")
}

dependencies {
    compileOnly(libs.mixin)
    compileOnly(libs.mixinextras)
    compileOnly(libs.geyser.api)
    compileOnly(libs.geyser.core) {
        exclude(group = "io.netty")
        exclude(group = "io.netty.incubator")
    }

    api(libs.pack.converter)
    compileOnly(libs.examination.api)

    implementation(libs.auto.service)
    annotationProcessor(libs.auto.service)

    annotationProcessor(libs.configurate.`interface`.ap)
    compileOnly(libs.bundles.configurate)

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testImplementation(libs.geyser.core) {
        exclude(group = "io.netty")
        exclude(group = "io.netty.incubator")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")

    // Only here to suppress "unknown enum constant EnvType.CLIENT" warnings.
    compileOnly(libs.fabric.loader)
}

tasks.test {
    useJUnitPlatform()
}

val mergedConfigurateInterfaceMappings = layout.projectDirectory.file("src/main/resources/org/spongepowered/configurate/interfaces/interface_mappings.properties")

fun writeMergedConfigurateInterfaceMappings(outputDir: File) {
    outputDir.mkdirs()
    copy {
        from(mergedConfigurateInterfaceMappings)
        into(outputDir)
    }
}

tasks.compileJava {
    doLast {
        writeMergedConfigurateInterfaceMappings(
            layout.buildDirectory.dir("classes/java/main/org/spongepowered/configurate/interfaces").get().asFile
        )
    }
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(mergedConfigurateInterfaceMappings) {
        into("org/spongepowered/configurate/interfaces")
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    exclude("org/spongepowered/configurate/interfaces/interface_mappings.properties")
    from(mergedConfigurateInterfaceMappings) {
        into("org/spongepowered/configurate/interfaces")
    }
}
