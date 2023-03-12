import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    id("xyz.wagyourtail.unimined") version "0.4.1"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.8.20-RC"
}

group = "at.hannibal2.skyhanni"
version = "0.17.Beta.13.1"

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

sourceSets.main {
    output.setResourcesDir(file("$buildDir/classes/java/main"))
}

// Dependencies:
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://repo.nea.moe/releases")
    // If you don't want to log in with your real minecraft account, remove this line
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
}


minecraft {
    forge {
        it.mcpChannel = "stable"
        it.mcpVersion = "22-1.8.9"
        it.setDevFallbackNamespace("intermediary")
        it.setDevNamespace("yarn")
        it.mixinConfig = listOf("mixins.skyhanni.json")
    }
    mcRemapper.tinyRemapperConf = {
        it.ignoreFieldDesc(true)
        it.ignoreConflicts(true)
    }
}

val shadowImpl by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val devenvMod by configurations.creating {
    isTransitive = false
    isVisible = false
}

dependencies {
    minecraft("net.minecraft:minecraft:1.8.9")
    mappings("moe.nea.mcp:mcp-yarn:1.8.9")
    "forge"("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // Mixin
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }

    // Kotlin
    implementation(kotlin("stdlib-jdk8"))
    shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4") {
        exclude(group = "org.jetbrains.kotlin")
    }

    // NEU
    compileOnly("com.github.hannibal002:notenoughupdates:4957f0b:all") { isTransitive = false }
    devenvMod("com.github.hannibal002:notenoughupdates:4957f0b:deobf")

    // Dev Auth
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.0")
    devenvMod("me.djtheredstoner:DevAuth-forge-legacy:1.1.0") { isTransitive = false }
}


minecraft {
    launches.apply {
        setConfig("client") {
            this.args.add(0, "--tweakClass")
            this.args.add(1, "net.minecraftforge.fml.common.launcher.FMLTweaker")
            this.args.addAll(listOf(
                "--tweakClass",
                "org.spongepowered.asm.launch.MixinTweaker",
                "--mods",
                devenvMod.resolve().joinToString(",") { it.relativeTo(this.workingDir).path },
            ))
            this.args.replaceAll { if (it == "-mixin.config") "--mixin" else it }
            this.jvmArgs.addAll(listOf(
                "-Dmixin.debug=true",
                "-Dmixin.env.remapRefMap=true",
            ))
        }
    }
}

// Tasks:
tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set("SkyHanni")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"

        // If you don't want mixins, remove these lines
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.skyhanni.json"
    }
}

project.afterEvaluate {
    tasks.named("runClient", JavaExec::class) {
        this.javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    }
}

tasks.shadowJar {
    archiveClassifier.set("all")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("${(it as? Configuration)?.name ?: "Unknown"}: ${it.files}")
        }
    }
    from(tasks["remapJar"])
    // If you want to include other dependencies and shadow them, you can relocate them in here
    // fun relocate(name: String) = relocate(name, "com.examplemod.deps.$name")
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
