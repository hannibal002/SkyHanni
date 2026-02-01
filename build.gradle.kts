import at.skyhanni.sharedvariables.MultiVersionStage
import at.skyhanni.sharedvariables.ProjectTarget
import at.skyhanni.sharedvariables.SHVersionInfo
import at.skyhanni.sharedvariables.versionString
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import skyhannibuildsystem.ChangelogVerification
import skyhannibuildsystem.CleanupMappingFiles
import skyhannibuildsystem.DownloadBackupRepo
import skyhannibuildsystem.PublishToModrinth

plugins {
    idea
    java
    id("com.gradleup.shadow") version "9.3.1"
    id("net.fabricmc.fabric-loom-remap")
    kotlin("jvm")
    id("com.google.devtools.ksp")
    kotlin("plugin.power-assert")
    `maven-publish`
    id("io.gitlab.arturbosch.detekt")
}

val target = ProjectTarget.values().find { it.projectPath == project.path }!!

// Toolchains:
java {
    toolchain.languageVersion.set(target.minecraftVersion.javaLanguageVersion)
    // We specifically request ADOPTIUM because if we do not restrict the vendor DCEVM is a
    // possible candidate. Some DCEVMs are however incompatible with some things gradle is doing,
    // causing crashes during tests. You can still manually select DCEVM in the Minecraft Client
    // IntelliJ run configuration.
    toolchain.vendor.set(JvmVendorSpec.ADOPTIUM)
}
val runDirectory = rootProject.file("run")
runDirectory.mkdirs()

// Minecraft configuration:
loom {
    val accessWidenerFile = sc.process(rootProject.file("src/main/resources/skyhanni.accesswidener"), "build/accesswidener.access")

    if (accessWidenerFile.exists()) {
        accessWidenerPath = accessWidenerFile
    } else {
        println("No accesswidener file for ${target.minecraftVersion}")
    }
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runs {
        named("client") {
            isIdeConfigGenerated = true
            appendProjectPathToConfigName.set(true)
            this.runDir(rootProject.file("versions/${target.projectName}/run").relativeTo(projectDir).toString())
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

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val shadowModImpl: Configuration by configurations.creating {
    configurations.modImplementation.get().extendsFrom(this)
}

val includeBackupRepo by tasks.registering(DownloadBackupRepo::class) {
    this.user = "hannibal002"
    this.repo = "SkyHanni-Repo"
    this.branch = "main"
    this.resourcePath = "assets/skyhanni/repo.zip"
    this.outputDirectory.set(layout.buildDirectory.dir("downloadedRepo"))
}

val includeBackupNeuRepo by tasks.registering(DownloadBackupRepo::class) {
    this.user = "NotEnoughUpdates"
    this.repo = "NotEnoughUpdates-Repo"
    this.branch = "master"
    this.resourcePath = "assets/skyhanni/neu-repo.zip"
    this.outputDirectory.set(layout.buildDirectory.dir("downloadedNeuRepo"))
}

val publishToModrinth by tasks.registering(PublishToModrinth::class)

tasks.runClient {
    this.javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(target.minecraftVersion.javaLanguageVersion)
        },
    )
}

tasks.register("checkPrDescription", ChangelogVerification::class) {
    this.outputDirectory.set(layout.buildDirectory)
    this.prTitle = project.findProperty("prTitle") as? String ?: ""
    this.prBody = project.findProperty("prBody") as? String ?: ""
}

dependencies {
    val versionName = target.minecraftVersion.versionNameOverride ?: target.minecraftVersion.versionName
    minecraft("com.mojang:minecraft:$versionName")
    if (target.mappingDependency == "official") {
        mappings(loom.layered {
            officialMojangMappings()
            if (versionName == "1.21.10") {
                mappings("dev.lambdaurora:yalmm-mojbackward:1.21.10+build.3")
            }
        })
    } else {
        mappings(target.mappingDependency)
    }

    // Discord RPC client
    includeImplementation("com.github.caoimhebyrne:KDiscordIPC:0.2.3")
    compileOnly(libs.jbAnnotations)
    ksp(project(":annotation-processors"))?.let { compileOnly(it) }

    ksp(libs.autoservice.ksp)
    implementation(libs.autoservice.annotations)

    target.fabricLoaderVersion?.let { modImplementation(it) }
    target.fabricApiVersion?.let { modImplementation(it) }
    modImplementation(libs.fabricLanguageKotlin)
    target.modMenuVersion?.let { modImplementation("maven.modrinth:modmenu:$it") }

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")

    val moulconfigVersion = target.minecraftVersion.moulconfigMinecraftVersionOverride ?: target.minecraftVersion.versionName
    shadowModImpl("org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}") {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
    }
    include("org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}")

    @Suppress("UnstableApiUsage")
    shadowImpl(libs.libautoupdate) {
        exclude(module = "gson")
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:1.12.5")

    modImplementation(libs.hypixelmodapi)
    include(libs.hypixelmodapi.fabric)


    modCompileOnly(libs.roughlyenoughitems) {
        exclude(group = "net.fabricmc.fabric-api")
    }


    // getting clock offset
    includeImplementation("commons-net:commons-net:3.11.1")

    // Calculator
    includeImplementation("com.notkamui.libs:keval:1.1.1")

    detektPlugins("org.notenoughupdates:detektrules:1.0.0")
    detektPlugins(project(":detekt"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")

    if (target != ProjectTarget.MODERN_12110) shadowImpl("org.apache.httpcomponents:httpclient:4.5.14")
}

fun DependencyHandler.includeImplementation(dep: Any) {
    include(dep)
    modImplementation(dep)
}

afterEvaluate {
    loom.runs.named("client") {
        programArgs("--quickPlayMultiplayer", "hypixel.net")
    }

    ksp {
        arg("skyhanni.modver", version.toString())
        arg("skyhanni.mcver", target.minecraftVersion.versionName)
        arg("skyhanni.buildpaths", project.file("buildpaths-excluded.txt").absolutePath)
    }
}

tasks.withType(Test::class) {
    useJUnitPlatform()
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    workingDir(file(runDirectory))
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
    from(includeBackupRepo)
    from(includeBackupNeuRepo)
    val fapiVersion = target.fabricApiVersion?.split(":")?.last() ?: ""
    val props = buildMap {
        put("version", version)
        put("minecraft", target.minecraftVersion.versionName)
        put("fapi", fapiVersion)
    }

    props.forEach(inputs::property)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

if (target == ProjectTarget.MODERN_12110) {
    fabricApi {
        configureTests {
            modId = "skyhanni"
            enableGameTests = false // Server game tests
            enableClientGameTests = true
            eula = true
        }
    }
    tasks.register("generateRepoPatterns", ClientProductionRunTask::class.java).configure {
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
        dependsOn(tasks.configureLaunch)
        val outputFile = project.file("build/regexes/constants.json")
        mods.from(project.configurations.modImplementation.get())

        jvmArgs.add("-DSkyHanniDumpRegex.enabled=true")
        jvmArgs.add("-DSkyHanniDumpRegex=${SHVersionInfo.gitHash}:${outputFile.absolutePath}")
        jvmArgs.add("-Dfabric.client.gametest=true")
        useXVFB = true
    }
    loom.runs.removeIf { it.name == "clientGameTest" }
}

fun excludeBuildPaths(buildPathsFile: File, sourceSet: Provider<SourceSet>) {
    if (buildPathsFile.exists()) {
        sourceSet.get().apply {
            val buildPaths = buildPathsFile.readText().lineSequence()
                .map { it.substringBefore("#").trim().replace(Regex("\\.(?!kt|java|\\()"), "/") }
                .filter { it.isNotBlank() }
                .toSet()
            kotlin.exclude(buildPaths)
            java.exclude(buildPaths)
        }
    }
}
excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.main)
excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.test)

tasks.withType<KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(target.minecraftVersion.formattedJavaLanguageVersion))
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set("SkyHanni")
    archiveVersion.set("$version-mc${target.minecraftVersion.versionName}")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Why do we have this here? This only *hides* errors.
}

val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.get().archiveFile)
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("all-dev")
    configurations = listOf(shadowImpl, shadowModImpl)
    exclude("META-INF/versions/**")
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "at.hannibal2.skyhanni.deps.libautoupdate")
    relocate("net.hypixel.modapi.tweaker", "at.hannibal2.skyhanni.deps.hypixel.modapi.tweaker")
}
tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}
tasks.assemble.get().dependsOn(tasks.remapJar)

tasks.withType(KotlinCompile::class) {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(target.minecraftVersion.javaLanguageVersion.versionString()))
    }
}

if (!MultiVersionStage.activeState.shouldCompile(target)) {
    tasks.withType<JavaCompile> {
        onlyIf { false }
    }
    tasks.withType<KotlinCompile> {
        onlyIf { false }
    }
    tasks.withType<AbstractArchiveTask> {
        onlyIf { false }
    }
    tasks.withType<ProcessResources> {
        onlyIf { false }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
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

detekt {
    buildUponDefaultConfig = true // preconfigure defaults
    config.setFrom(rootProject.layout.projectDirectory.file("detekt/detekt.yml")) // point to your custom config defining rules to run, overwriting default behavior
    baseline = file(rootProject.layout.projectDirectory.file("detekt/baseline-main.xml")) // a way of suppressing issues before introducing detekt
    source.setFrom(project.sourceSets.named("main").map { it.allSource })
}

tasks.withType<Detekt>().configureEach {
    onlyIf {
        target == ProjectTarget.MODERN_12110 && project.findProperty("skipDetekt") != "true"
    }
    jvmTarget = target.minecraftVersion.formattedJavaLanguageVersion
    outputs.cacheIf { false } // Custom rules won't work if cached

    val isDetektMain = (this.name == "detektMain")
    val outputFileName = if (isDetektMain) "main" else "detekt"
    val detektDir = rootProject.layout.buildDirectory.dir("reports/detekt").get().asFile.absolutePath
    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
        html.outputLocation.set(file("$detektDir/$outputFileName.html"))
        xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
        xml.outputLocation.set(file("$detektDir/$outputFileName.xml"))
        sarif.required.set(true) // standardized SARIF format (https://sarifweb.azurewebsites.net/) to support integrations with GitHub Code Scanning
        sarif.outputLocation.set(file("$detektDir/$outputFileName.sarif"))
        md.required.set(true) // simple Markdown format
        md.outputLocation.set(file("$detektDir/$outputFileName.md"))
        txt.required.set(true)
        txt.outputLocation.set(file("$detektDir/$outputFileName.txt"))
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = target.minecraftVersion.formattedJavaLanguageVersion
    outputs.cacheIf { false } // Custom rules won't work if cached
    onlyIf {
        // We only need one baseline for the main source set
        target == ProjectTarget.MODERN_12110
    }

    val isMainBaseline = (this.name == "detektBaselineMain")
    val outputFileName = if (isMainBaseline) "baseline-main" else "baseline"
    baseline.set(file(rootProject.layout.projectDirectory.file("detekt/$outputFileName.xml")))
}
repositories {
    mavenCentral()
}