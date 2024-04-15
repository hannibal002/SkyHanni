import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import xyz.wagyourtail.unimined.api.minecraft.MinecraftConfig
import xyz.wagyourtail.unimined.api.minecraft.task.RemapJarTask
import xyz.wagyourtail.unimined.api.runs.RunConfig
import java.io.ByteArrayOutputStream

plugins {
    idea
    java
    id("xyz.wagyourtail.unimined") version "1.2.0-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    kotlin("jvm") version "1.9.0"
    id("com.bnorm.power.kotlin-power-assert") version "0.13.0"
    `maven-publish`
    id("moe.nea.shot") version "1.0.0"
}

group = "at.hannibal2.skyhanni"
version = "0.25.Beta.9"

val gitHash by lazy {
    val baos = ByteArrayOutputStream()
    exec {
        standardOutput = baos
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }
    baos.toByteArray().decodeToString().trim()
}

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
    java.srcDir(layout.projectDirectory.dir("src/main/kotlin"))
    kotlin.destinationDirectory.set(java.destinationDirectory)
}

val patternSourceSet = sourceSets.create("pattern") {
    this.runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://jitpack.io") {
        content {
            includeGroupByRegex("com\\.github\\..*")
        }
    }
    maven("https://repo.nea.moe/releases")
    maven("https://maven.notenoughupdates.org/releases")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    ivy("https://repo1.maven.org/maven2/org/apache/logging/log4j") {
        this.content {
            this.includeGroup("log4jhack")
        }
        this.patternLayout {
            this.artifact("log4j-[artifact]/[revision]/log4j-[artifact]-[revision].[ext]")
        }
        this.metadataSources {
            this.artifact()
        }
    }
}


val devenvMod: Configuration by configurations.creating {
    isTransitive = false
    isVisible = false
}

val headlessLwjgl by configurations.creating {
    isTransitive = false
    isVisible = false
}

val shot = shots.shot("minecraft", project.file("shots.txt"))

val modRuntimeOnly by configurations.creating {
    configurations.runtimeOnly.get().extendsFrom(this)
}
val modCompileOnly by configurations.creating {
    configurations.compileOnly.get().extendsFrom(this)
}

fun RunConfig.setBaseConfig() {
    val runDir = file("run").absoluteFile
    this.javaVersion = JavaVersion.VERSION_1_8
    this.args.addAll(
        listOf(
            "--tweakClass",
            "org.spongepowered.asm.launch.MixinTweaker",
            "--tweakClass",
            "io.github.notenoughupdates.moulconfig.tweaker.DevelopmentResourceTweaker",
            "--mods",
            devenvMod.resolve().joinToString(",") { it.relativeTo(runDir).path }
        )
    )
    this.args.set(
        this.args.indexOf("--gameDir") + 1,
        runDir.absolutePath
    )
    this.jvmArgs.removeIf { it.startsWith("-Xmx") }
    this.jvmArgs.addAll(
        listOf(
            "-Xmx4G",
            "-Dmixin.debug=true",
            "-Dlog4j.configurationFile=${project.file("log4j2.xml").absolutePath}"
        )
    )
    this.workingDir = runDir
    if (System.getenv("repo_action") != "true") {
        this.jvmArgs.add("-Ddevauth.configdir=${rootProject.file(".devauth").absolutePath}")
    }
    this.env.putAll(parseEnvFile(file(".env")))
}

fun MinecraftConfig.defaultMinecraft() {
    this.version("1.8.9")
    this.mappings {
        this.searge()
        this.mcp("stable", "22-1.8.9")
    }
    this.minecraftForge {
        this.loader("11.15.1.2318-1.8.9")
        this.mixinConfig("mixins.skyhanni.json")
    }
}

unimined.minecraft {
    this.defaultMinecraft()
    this.runs {
        this.config("client") {
            this.setBaseConfig()
        }
    }
    this.mods {
        this.remap(modRuntimeOnly)
        this.remap(devenvMod)
        this.remap(modCompileOnly)
    }
}

unimined.minecraft(patternSourceSet) {
    this.defaultMinecraft()
    this.runs {
        this.config("client") {
            this.setBaseConfig()
            this.jvmArgs.addAll(
                listOf(
                    "-Dorg.lwjgl.opengl.Display.allowSoftwareOpenGL=true",
                    "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006",
                    "-javaagent:${headlessLwjgl.singleFile.absolutePath}"
                )
            )
            val outputFile = project.file("build/regexes/constants.json")
            this.env.put("SKYHANNI_DUMP_REGEXES", "${gitHash}:${outputFile.absolutePath}")
            this.env.put("SKYHANNI_DUMP_REGEXES_EXIT", "true")
        }
    }
}
unimined.minecraft(sourceSets.test.get()) {
    this.defaultMinecraft()
}
val modImplementation by configurations
val testModImplementation by configurations
val patternModImplementation by configurations

testModImplementation.extendsFrom(modImplementation)
patternModImplementation.extendsFrom(testModImplementation)

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadowModImpl: Configuration by configurations.creating {
    modImplementation.extendsFrom(this)

}

configurations.named("minecraftLibraries") {
    this.resolutionStrategy {
        this.force("org.apache.logging.log4j:log4j-core:2.8.1")
        this.force("org.apache.logging.log4j:log4j-api:2.8.1")
    }
}

dependencies {

    // Discord RPC client
    shadowImpl("com.github.NetheriteMiner:DiscordIPC:3106be5") {
        exclude(module = "log4j")
        because("Different version conflicts with Minecraft's Log4J")
        exclude(module = "gson")
        because("Different version conflicts with Minecraft's Log4j")
    }
    compileOnly(libs.jbAnnotations)

    headlessLwjgl(libs.headlessLwjgl)

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }

    implementation(kotlin("stdlib-jdk8"))
    shadowImpl("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") {
        exclude(group = "org.jetbrains.kotlin")
    }

    modRuntimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.1.0")

    modCompileOnly("com.github.hannibal002:notenoughupdates:4957f0b:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }
    devenvMod("com.github.NotEnoughUpdates:NotEnoughUpdates:6a661042b0:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }

    shadowModImpl(libs.moulconfig)
    shadowImpl(libs.libautoupdate)
    shadowImpl("org.jetbrains.kotlin:kotlin-reflect:1.9.0")
    implementation(libs.hotswapagentforge)

//    testImplementation(kotlin("test"))
    testImplementation("com.github.NotEnoughUpdates:NotEnoughUpdates:6a661042b0:all") {
        exclude(module = "unspecified")
        isTransitive = false
    }
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.12.5")

    implementation("net.hypixel:mod-api:0.3.1")

    runtimeOnly(libs.terminalConsoleAppender)
    // Manually load 2.0-beta.9 on the class path *after* loading 2.8.X, since forge uses some of the helper classes only available in this version.
    runtimeOnly("log4jhack:api:2.0-beta9")
    runtimeOnly("log4jhack:core:2.0-beta9")
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
    filesMatching("mcmod.info") {
        expand("version" to version)
    }
    this.filesMatching("mixins.skyhanni.json") {
        this.autoDiscoverMixins(sourceSets.main.get())
    }
}

val generateRepoPatterns by tasks.creating() {
    afterEvaluate { dependsOn(tasks["patternRunClient"]) }
}

tasks.compileJava {
    dependsOn(tasks.processResources)
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(Jar::class) {
    archiveBaseName.set("SkyHanni")
    if (this.name != "remapJar") {
        this.destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["Main-Class"] = "SkyHanniInstallerFrame"

        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["MixinConfigs"] = "mixins.skyhanni.json"
    }
}


val remapJar by tasks.named<RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.shadowJar {
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl, shadowModImpl)
    exclude("META-INF/versions/**")
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "at.hannibal2.skyhanni.deps.libautoupdate")
}
tasks.jar {
    archiveClassifier.set("nodeps")
}
tasks.assemble.get().dependsOn(remapJar)

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("src")
    from(sourceSets.main.get().allSource)
}

publishing.publications {
    create<MavenPublication>("maven") {
        artifact(remapJar)
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



