import at.skyhanni.sharedvariables.MappingStyle
import at.skyhanni.sharedvariables.ProjectTarget
import at.skyhanni.sharedvariables.SHVersionInfo
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.fabricmc.loom.task.RemapSourcesJarTask
import net.fabricmc.loom.task.ValidateAccessWidenerTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import skyhannibuildsystem.ChangelogVerification
import skyhannibuildsystem.DownloadBackupRepo
import skyhannibuildsystem.PublishToModrinth
import org.gradle.jvm.tasks.Jar as GradleJar

plugins {
    idea
    java
    alias(libs.plugins.shadow)
    id("net.fabricmc.fabric-loom-remap") apply false
    id("net.fabricmc.fabric-loom") apply false
    kotlin("jvm")
    id("com.google.devtools.ksp")
    kotlin("plugin.power-assert")
    `maven-publish`
    id("dev.detekt")
}

val target = ProjectTarget.entries.find { it.projectPath == project.path }!!
val primaryTarget = ProjectTarget.MODERN_26100
val isDeobf = target.mappingStyle == MappingStyle.NONE

if (isDeobf) apply(plugin = "net.fabricmc.fabric-loom")
else apply(plugin = "net.fabricmc.fabric-loom-remap")

// Manual accessors for the conditionally-applied loom plugin.
// These replace the typed accessors that Kotlin DSL would normally generate for
// plugins applied in the plugins block. Since both loom plugins are declared with
// apply false, no accessors are auto-generated, so we define them explicitly.
val loom: LoomGradleExtensionAPI get() = extensions.getByType(LoomGradleExtensionAPI::class.java)
fun DependencyHandler.minecraft(dep: Any): Dependency? = add("minecraft", dep)
fun DependencyHandler.mappings(dep: Any): Dependency? = add("mappings", dep)
fun DependencyHandler.include(dep: Any): Dependency? = add("include", dep)
fun DependencyHandler.modImplementation(dep: Any): Dependency? = add("modImplementation", dep)
fun DependencyHandler.modImplementation(dep: Any, configure: ExternalModuleDependency.() -> Unit): Dependency? =
    add("modImplementation", dep).also { (it as? ExternalModuleDependency)?.configure() }
fun DependencyHandler.modCompileOnly(dep: Any): Dependency? = add("modCompileOnly", dep)
fun DependencyHandler.modCompileOnly(dep: Any, configure: ExternalModuleDependency.() -> Unit): Dependency? =
    add("modCompileOnly", dep).also { (it as? ExternalModuleDependency)?.configure() }
fun DependencyHandler.modRuntimeOnly(dep: Any): Dependency? = add("modRuntimeOnly", dep)

// Toolchains:
java {
    toolchain.languageVersion.set(target.minecraftVersion.javaLanguageVersion)
}
val runDirectory = rootProject.file("run")
runDirectory.mkdirs()

// Minecraft configuration:
@OptIn(StonecutterExperimentalAPI::class)
loom.apply {
    val classTweakerFile = sc.process(
        rootProject.file("src/main/resources/skyhanni.classtweaker"),
        "build/skyhanni.classtweaker",
    )
    if (classTweakerFile.exists()) {
        accessWidenerPath = classTweakerFile
    } else {
        println("No classTweaker file for ${target.minecraftVersion}")
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
    if (!isDeobf) configurations.getByName("modImplementation").extendsFrom(this)
}

val shadowOnly: Configuration by configurations.creating

val includeBackupRepo by tasks.registering(DownloadBackupRepo::class) {
    this.user = "hannibal002"
    this.repo = "SkyHanni-Repo"
    this.branch = "main"
    this.resourcePath = "assets/skyhanni/repo.tar.gz"
    this.outputDirectory.set(layout.buildDirectory.dir("downloadedRepo"))
}

val includeBackupNeuRepo by tasks.registering(DownloadBackupRepo::class) {
    this.user = "NotEnoughUpdates"
    this.repo = "NotEnoughUpdates-Repo"
    this.branch = "master"
    this.resourcePath = "assets/skyhanni/neu-repo.tar.gz"
    this.outputDirectory.set(layout.buildDirectory.dir("downloadedNeuRepo"))
}

val publishToModrinth by tasks.registering(PublishToModrinth::class)

tasks.named<JavaExec>("runClient") {
    this.javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

tasks.register<ClientProductionRunTask>("prodClient") {
    notCompatibleWithConfigurationCache("Interactive client launches must start a new process every time.")
    outputs.upToDateWhen { false }
    runDir = file("run")
}

if (target == primaryTarget) {
    tasks.register("checkPrDescription", ChangelogVerification::class) {
        this.outputDirectory.set(layout.buildDirectory)
        this.prTitle = System.getenv("PR_TITLE") ?: project.findProperty("prTitle") as? String ?: ""
        this.prBody = System.getenv("PR_BODY") ?: project.findProperty("prBody") as? String ?: ""
    }
}

dependencies {
    val versionName = target.minecraftVersion.versionNameOverride ?: target.minecraftVersion.versionName
    minecraft("com.mojang:minecraft:$versionName")
    @Suppress("UnstableApiUsage")
    if (!isDeobf) {
        if (target.mappingDependency == "official") {
            mappings(loom.officialMojangMappings())
        } else {
            mappings(target.mappingDependency)
        }
    }

    compileOnly(libs.jbAnnotations)
    ksp(project(":annotation-processors"))?.let { compileOnly(it) }

    ksp(libs.autoservice.ksp)
    implementation(libs.autoservice.annotations)

    target.fabricLoaderVersion?.let {
        if (isDeobf) implementation(it) else modImplementation(it)
        "productionRuntimeMods"(it)
    }
    target.fabricApiVersion?.let {
        if (isDeobf) implementation(it) else modImplementation(it)
        "productionRuntimeMods"(it)
    }
    if (isDeobf) implementation(libs.fabricLanguageKotlin)
    else modImplementation(libs.fabricLanguageKotlin)
    "productionRuntimeMods"(libs.fabricLanguageKotlin)

    target.modMenuVersion?.let {
        if (isDeobf) implementation("maven.modrinth:modmenu:$it")
        else modImplementation("maven.modrinth:modmenu:$it")
    }

    if (isDeobf) runtimeOnly(libs.devauth)
    else modRuntimeOnly(libs.devauth)
    "productionRuntimeMods"(libs.devauth)

    val moulconfigVersion = target.minecraftVersion.moulconfigMinecraftVersionOverride ?: target.minecraftVersion.versionName
    if (isDeobf) {
        shadowImpl("org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}") {
            exclude("org.jetbrains.kotlin")
            exclude("org.jetbrains.kotlinx")
        }
    } else {
        shadowModImpl("org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}") {
            exclude("org.jetbrains.kotlin")
            exclude("org.jetbrains.kotlinx")
        }
        include("org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}")
    }
    "minecraftTestClientRuntimeLibraries"(
        "org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}"
    )

    shadowImpl(libs.libautoupdate) {
        exclude(module = "gson")
    }
    "minecraftTestClientRuntimeLibraries"(libs.libautoupdate)

    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.agent)

    if (isDeobf) {
        implementation(target.hypixelModApiVersion)
        runtimeOnly(target.hypixelModApiFabricVersion)
    } else {
        modImplementation(target.hypixelModApiVersion)
        modRuntimeOnly(target.hypixelModApiFabricVersion)
    }
    "productionRuntimeMods"(target.hypixelModApiFabricVersion)

    if (isDeobf) compileOnly(libs.roughlyenoughitems) { exclude(group = "net.fabricmc.fabric-api") }
    else modCompileOnly(libs.roughlyenoughitems) { exclude(group = "net.fabricmc.fabric-api") }

    // getting clock offset
    includeImplementation(libs.commons.net)
    "minecraftTestClientRuntimeLibraries"(libs.commons.net)

    // Calculator
    includeImplementation(libs.keval) {
        exclude(group = "org.jetbrains.kotlin")
    }
    "minecraftTestClientRuntimeLibraries"(libs.keval)

    detektPlugins(libs.detektrules.neu)
    detektPlugins(project(":detekt"))
    detektPlugins(libs.detektrules.ktlint)

    shadowImpl(libs.httpclient)
    "minecraftTestClientRuntimeLibraries"(libs.httpclient)
}

fun DependencyHandler.includeImplementation(dep: Any, configure: ExternalModuleDependency.() -> Unit = {}) {
    fun dependencyNotation(): Any = (dep as? Provider<*>)?.get() ?: dep

    if (isDeobf) {
        add("shadowImpl", dependencyNotation()).also { (it as? ExternalModuleDependency)?.configure() }
    } else {
        include(dependencyNotation()).also { (it as? ExternalModuleDependency)?.configure() }
        modImplementation(dependencyNotation()).also { (it as? ExternalModuleDependency)?.configure() }
    }
}

afterEvaluate {
    loom.runs.named("client") {
        programArgs("--quickPlayMultiplayer", "hypixel.net")
    }

    ksp {
        arg("skyhanni.modver", version.toString())
        arg("skyhanni.mcver", target.minecraftVersion.versionName)
        if (!isDeobf) {
            arg("skyhanni.cachedir", layout.buildDirectory.get().asFile.absolutePath)
        } else {
            arg("skyhanni.buildpaths", project.file("buildpaths-excluded.txt").absolutePath)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    workingDir(file(runDirectory))
    systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
    jvmArgs(
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/java.util=ALL-UNNAMED",
        "-XX:+EnableDynamicAgentLoading",
        // Tests start NPE-ing without this on Java 25
        "-Dnet.bytebuddy.experimental=true",
    )
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.2"
        }
    }
}

// Tasks:
tasks.processResources {
    from(includeBackupRepo)
    from(includeBackupNeuRepo)
    val fapiVersion = target.fabricApiVersion?.split(":")?.last() ?: ""
    val hypixelModApiVersion = target.hypixelModApiFabricVersion.split(":").last()
    val minecraftVersion = target.minecraftVersion.fabricModJsonVersion
    val props = buildMap {
        put("version", version)
        put("minecraft", minecraftVersion)
        put("fapi", fapiVersion)
        put("hypixelmodapi", hypixelModApiVersion)
    }

    props.forEach(inputs::property)

    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

@Suppress("UnstableApiUsage")
if (target == primaryTarget) {
    configure<FabricApiExtension> {
        configureTests {
            modId = "skyhanni"
            enableGameTests = false // Server game tests
            enableClientGameTests = true
            eula = true
        }
    }
    tasks.register("generateRepoPatterns", ClientProductionRunTask::class.java).configure {
        javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
        dependsOn(tasks.named("configureLaunch"))
        val outputFile = project.file("build/regexes/constants.json")

        jvmArgs.add("-DSkyHanniDumpRegex.enabled=true")
        jvmArgs.add("-DSkyHanniDumpRegex=${SHVersionInfo.gitHash}:${outputFile.absolutePath}")
        jvmArgs.add("-Dfabric.client.gametest=true")
        useXVFB = System.getProperty("os.name").startsWith("Linux", ignoreCase = true)
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
if (isDeobf) {
    excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.main)
    excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.test)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        val jvmTargetStr = if (isDeobf) target.minecraftVersion.formattedKotlinJvmTarget
                           else target.minecraftVersion.formattedJavaLanguageVersion
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetStr))
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.time.ExperimentalTime"
        )
        freeCompilerArgs.addAll(
            // 0 (all cores) triggers a race condition in JvmIrCodegenFactory's parallel codegen on Kotlin 2.3.x,
            // leaving corrupt .class files that break subsequent incremental builds.
            // see: https://youtrack.jetbrains.com/issue/KT-85498/
            "-Xbackend-threads=1",
            "-Xnested-type-aliases",
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<GradleJar> {
    archiveBaseName.set("SkyHanni")
    archiveVersion.set("$version-mc${target.minecraftVersion.versionName}")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE // Why do we have this here? This only *hides* errors.
}

if (!isDeobf) {
    val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
        archiveClassifier.set("")
        dependsOn(tasks.shadowJar)
        inputFile.set(tasks.shadowJar.get().archiveFile)
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    }
    tasks.assemble.get().dependsOn(remapJar)
}

tasks.shadowJar {
    if (isDeobf) {
        destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
        archiveClassifier.set("")
    } else {
        destinationDirectory.set(layout.buildDirectory.dir("badjars"))
        archiveClassifier.set("all-dev")
    }
    configurations = buildList {
        add(shadowImpl)
        if (!isDeobf) add(shadowModImpl)
        if (isDeobf) add(shadowOnly)
    }
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

if (isDeobf) {
    tasks.assemble.get().dependsOn(tasks.shadowJar)
}

val sourcesJar by tasks.registering(Jar::class) {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("src")
    from(sourceSets.main.get().allSource)
}

publishing.publications {
    create<MavenPublication>("maven") {
        if (!isDeobf) artifact(tasks.named("remapJar"))
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
    buildUponDefaultConfig = true
    config.setFrom(rootProject.layout.projectDirectory.file("detekt/detekt.yml"))
    baseline = file(rootProject.layout.projectDirectory.file("detekt/baseline-main.xml"))
    source.setFrom(project.sourceSets.named("main").map {
        it.allSource.matching {
            exclude { elem -> elem.file.absolutePath.replace('\\', '/').contains("/build/generated/") }
        }
    })
}

// Detekt is handled by a dedicated CI workflow; exclude it from the check/build lifecycle
// so it doesn't slow down normal builds. It still runs when invoked explicitly.
afterEvaluate {
    tasks.findByName("check")?.setDependsOn(
        tasks.getByName("check").dependsOn.filterNot { dep ->
            (dep is Task && dep.name.startsWith("detekt")) ||
            (dep is TaskProvider<*> && dep.name.startsWith("detekt"))
        }
    )
}

tasks.withType<Detekt>().configureEach {
    source = source.matching {
        exclude { it.file.absolutePath.replace('\\', '/').contains("/build/generated/") }
    }
    val isTargetVersion = target == primaryTarget
    val skipDetekt = project.findProperty("skipDetekt") == "true"
    onlyIf { isTargetVersion && !skipDetekt }

    val isDetektMain = name == "detektMain"
    val outputFileName = if (isDetektMain) "main" else "detekt"
    val detektDir = rootProject.layout.buildDirectory.dir("reports/detekt").get().asFile.absolutePath
    reports {
        html.required.set(true)
        html.outputLocation.set(file("$detektDir/$outputFileName.html"))
        sarif.required.set(true)
        sarif.outputLocation.set(file("$detektDir/$outputFileName.sarif"))
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    val isTargetVersion = target == primaryTarget
    jvmTarget = target.minecraftVersion.formattedJavaLanguageVersion
    outputs.cacheIf { false }
    onlyIf { isTargetVersion }

    val isMainBaseline = name == "detektBaselineMain"
    val outputFileName = if (isMainBaseline) "baseline-main" else "baseline"
    baseline.set(file(rootProject.layout.projectDirectory.file("detekt/$outputFileName.xml")))
}

tasks.withType<RemapSourcesJarTask>().configureEach {
    enabled = false
}

tasks.matching { it.name == "kspTestKotlin" || it.name == "kspTestJava" }.configureEach {
    enabled = false
}

tasks.withType<ValidateAccessWidenerTask>().configureEach {
    dependsOn("stonecutterPrepare")
}

repositories {
    mavenLocal()
    mavenCentral()
}
