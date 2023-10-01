import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.9.0"
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
}

group = "at.hannibal2.skyhanni"
version = "0.20"

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

sourceSets.main {
    output.setResourcesDir(file("$buildDir/classes/kotlin/main"))
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.spongepowered.org/maven/")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
    maven("https://repo.nea.moe/releases")
    maven("https://maven.notenoughupdates.org/releases")
}

val shadowImpl by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadowModImpl by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

val devenvMod by configurations.creating {
    isTransitive = false
    isVisible = false
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // Discord RPC client
    shadowImpl("com.github.ILikePlayingGames:DiscordIPC:f91ed4b") {
        exclude(module = "log4j")
        because("Different version conflicts with Minecraft's Log4J")
        exclude(module = "gson")
        because("Different version conflicts with Minecraft's Log4j")
    }


    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.4-SNAPSHOT")

    implementation(kotlin("stdlib-jdk8"))
    shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // If you don't want to log in with your real minecraft account, remove this line
    modRuntimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.0")

    @Suppress("VulnerableLibrariesLocal")
    modImplementation("com.github.hannibal002:notenoughupdates:4957f0b:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }
    @Suppress("VulnerableLibrariesLocal")
    devenvMod("com.github.hannibal002:notenoughupdates:4957f0b:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }

    shadowModImpl(libs.moulconfig)
    devenvMod(variantOf(libs.moulconfig) { classifier("test") })

    shadowImpl(libs.libautoupdate)
    shadowImpl("org.jetbrains.kotlin:kotlin-reflect:1.9.0")

//    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
}

tasks.withType(Test::class) {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    workingDir(file("run"))
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
            enableLanguageFeature("BreakContinueInInlineLambdas")
        }
    }
}

// Minecraft configuration:
loom {
    launchConfigs {
        "client" {
            // If you don't want mixins, remove these lines
            property("mixin.debug", "true")
            property("asmhelper.verbose", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
            arg("--mixin", "mixins.skyhanni.json")
            val modFiles = devenvMod
                .incoming.artifacts.resolvedArtifacts.get()
            arg("--mods", modFiles.joinToString(",") { it.file.relativeTo(file("run")).path })
        }
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        // If you don't want mixins, remove this lines
        mixinConfig("mixins.skyhanni.json")
    }
    // If you don't want mixins, remove these lines
    mixin {
        defaultRefmapName.set("mixins.skyhanni.refmap.json")
    }
    runConfigs {
        "server" {
            isIdeConfigGenerated = false
        }
    }
}

// Tasks:
tasks.processResources {
    inputs.property("version", version)
    filesMatching("mcmod.info") {
        expand("version" to version)
    }
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set("SkyHanni")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["Main-Class"] = "SkyHanniInstallerFrame"

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.skyhanni.json"
    }
}


val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl, shadowModImpl)
    doLast {
        configurations.forEach {
            println("Config: ${it.files}")
        }
    }
    exclude("META-INF/versions/**")

    relocate("io.github.moulberry.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "at.hannibal2.skyhanni.deps.libautoupdate")
}
tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}
tasks.assemble.get().dependsOn(tasks.remapJar)

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
