import at.skyhanni.sharedvariables.MinecraftVersion
import at.skyhanni.sharedvariables.ProjectTarget
import at.skyhanni.sharedvariables.SHVersionInfo
import at.skyhanni.sharedvariables.versionString
import com.google.devtools.ksp.gradle.KspTaskJvm
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.replaymod.preprocess")
    id("gg.essential.loom")
    kotlin("jvm")
    id("com.google.devtools.ksp")
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
    `maven-publish`
    id("moe.nea.shot") version "1.0.0"
}

val target = ProjectTarget.values().find { it.projectPath == project.path }!!

// Toolchains:
java {
    toolchain.languageVersion.set(target.minecraftVersion.javaLanguageVersion)
}

// Minecraft configuration:
loom {
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.skyhanni.json")
    }
//     mixin.defaultRefmapName.set("mixins.skyhanni.refmap.json")
    runs {
        named("client") {
            property("mixin.debug", "true")
            if (System.getenv("repo_action") != "true") {
                property("devauth.configDir", rootProject.file(".devauth").absolutePath)
            }
            vmArgs("-Xmx4G")
            programArgs("--tweakClass", "at.hannibal2.skyhanni.tweaker.SkyHanniTweaker")
            programArgs("--tweakClass", "io.github.notenoughupdates.moulconfig.tweaker.DevelopmentResourceTweaker")
        }
        removeIf { it.name == "server" }
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
    java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
    kotlin.destinationDirectory.set(java.destinationDirectory)
    println(java.destinationDirectory.get().asFile)
    println(kotlin.destinationDirectory.get().asFile)
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadowModImpl: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

val devenvMod: Configuration by configurations.creating {
    isTransitive = false
    isVisible = false
}

val headlessLwjgl by configurations.creating {
    isTransitive = false
    isVisible = false
}

val shot = shots.shot("minecraft", rootProject.file("shots.txt"))

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9@zip")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    // Discord RPC client
    shadowImpl("com.jagrosh:DiscordIPC:0.5.3") {
        exclude(module = "log4j")
        because("Different version conflicts with Minecraft's Log4J")
        exclude(module = "gson")
        because("Different version conflicts with Minecraft's Log4j")
    }
    compileOnly(libs.jbAnnotations)

    headlessLwjgl(libs.headlessLwjgl)

    compileOnly(ksp(project(":annotation-processors"))!!)

    val mixinVersion = if (target.minecraftVersion >= MinecraftVersion.MC11200) "0.8.2" else "0.7.11-SNAPSHOT"

    if (!target.isFabric) {
        shadowImpl("org.spongepowered:mixin:$mixinVersion") {
            isTransitive = false
        }
    }
//     annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    implementation(kotlin("stdlib-jdk8"))
    shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") {
        exclude(group = "org.jetbrains.kotlin")
    }

    modRuntimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.0")

    modCompileOnly("com.github.hannibal002:notenoughupdates:4957f0b:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }
    // June 3, 2024, 9:30 PM AEST
    // https://github.com/NotEnoughUpdates/NotEnoughUpdates/tree/2.3.0
    devenvMod("com.github.NotEnoughUpdates:NotEnoughUpdates:2.3.0:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }

    shadowModImpl(libs.moulconfig)
    shadowImpl(libs.libautoupdate) {
        exclude(module = "gson")
    }
    shadowImpl("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation(libs.hotswapagentforge)

    testImplementation("com.github.NotEnoughUpdates:NotEnoughUpdates:faf22b5dd9:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.12.5")

    implementation("net.hypixel:mod-api:0.3.1")
}

loom.runs.named("client") {
    programArgs("--mods", devenvMod.resolve().joinToString(",") { it.relativeTo(file("run")).path })
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

// Tasks:
tasks.processResources {
    inputs.property("version", version)
    filesMatching(listOf("mcmod.info", "fabric.mod.json")) {
        expand("version" to version)
    }
    if (target.isFabric) {
        exclude("mcmod.info")
    } // else do NOT exclude fabric.mod.json. We use fabric.mod.json in order to show a logo in prism launcher.
}

if (target == ProjectTarget.MAIN) {
    val generateRepoPatterns by tasks.creating(JavaExec::class) {
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
        mainClass.set("net.fabricmc.devlaunchinjector.Main")
        workingDir(project.file("run"))
        classpath(sourceSets.main.map { it.runtimeClasspath }, sourceSets.main.map { it.output })
        jvmArgs(
            "-Dfabric.dli.config=${project.file(".gradle/loom-cache/launch.cfg").absolutePath}",
            "-Dfabric.dli.env=client",
            "-Dfabric.dli.main=net.minecraft.launchwrapper.Launch",
            "-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true",
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006",
            "-javaagent:${headlessLwjgl.singleFile.absolutePath}",
        )
        val outputFile = project.file("build/regexes/constants.json")
        environment("SKYHANNI_DUMP_REGEXES", "${SHVersionInfo.gitHash}:${outputFile.absolutePath}")
        environment("SKYHANNI_DUMP_REGEXES_EXIT", "true")
    }
}

tasks.compileJava {
    dependsOn(tasks.processResources)
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set("SkyHanni")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Why do we have this here? This only *hides* errors.
    manifest.attributes.run {
        this["Main-Class"] = "SkyHanniInstallerFrame"
        if (target == ProjectTarget.MAIN) {
            this["FMLCorePluginContainsFMLMod"] = "true"
            this["ForceLoadAsMod"] = "true"
            this["TweakClass"] = "at.hannibal2.skyhanni.tweaker.SkyHanniTweaker"
            this["MixinConfigs"] = "mixins.skyhanni.json"
        }
    }
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
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
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "at.hannibal2.skyhanni.deps.libautoupdate")
    relocate("com.jagrosh.discordipc", "at.hannibal2.skyhanni.deps.discordipc")
}
tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}
tasks.assemble.get().dependsOn(tasks.remapJar)

tasks.withType(KotlinCompile::class) {
    kotlinOptions.jvmTarget = target.minecraftVersion.javaLanguageVersion.versionString()
}

val sourcesJar by tasks.creating(Jar::class) {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("src")
    from(sourceSets.main.get().allSource)
}

publishing.publications {
    create<MavenPublication>("maven") {
        artifact(tasks.remapJar)
        artifact(sourcesJar) { classifier = "sources" }
        pom {
            name.set("SkyHanni")
            licenses {
                license {
                    name.set("GNU Lesser General Public License")
                    url.set("https://github.com/hannibal002/SkyHanni/blob/HEAD/LICENSE")
                }
            }
            developers {
                developer { name.set("hannibal002") }
                developer { name.set("The SkyHanni contributors") }
            }
        }
    }
}
