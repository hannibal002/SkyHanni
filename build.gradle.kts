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
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import skyhannibuildsystem.ChangelogVerification
import skyhannibuildsystem.DownloadBackupRepo
import skyhannibuildsystem.PublishToModrinth
import skyhannibuildsystem.SetMixinRequired
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
val primaryTarget = ProjectTarget.MODERN_26200

apply(plugin = "net.fabricmc.fabric-loom")

// Manual accessors for the conditionally-applied loom plugin.
// These replace the typed accessors that Kotlin DSL would normally generate for
// plugins applied in the plugins block. Since both loom plugins are declared with
// apply false, no accessors are auto-generated, so we define them explicitly.
val loom: LoomGradleExtensionAPI get() = extensions.getByType(LoomGradleExtensionAPI::class.java)
fun dependencyNotation(dep: Any): Any = (dep as? Provider<*>)?.get() ?: dep
fun DependencyHandler.minecraft(dep: Any): Dependency? = add("minecraft", dep)
fun DependencyHandler.mappings(dep: Any): Dependency? = add("mappings", dep)
fun DependencyHandler.include(dep: Any): Dependency? = add("include", dependencyNotation(dep))
fun DependencyHandler.modImplementation(dep: Any): Dependency? = add("modImplementation", dependencyNotation(dep))
fun DependencyHandler.modImplementation(dep: Any, configure: ExternalModuleDependency.() -> Unit): Dependency? =
    add("modImplementation", dependencyNotation(dep)).also { (it as? ExternalModuleDependency)?.configure() }
fun DependencyHandler.modCompileOnly(dep: Any): Dependency? = add("modCompileOnly", dependencyNotation(dep))
fun DependencyHandler.modCompileOnly(dep: Any, configure: ExternalModuleDependency.() -> Unit): Dependency? =
    add("modCompileOnly", dependencyNotation(dep)).also { (it as? ExternalModuleDependency)?.configure() }
fun DependencyHandler.modRuntimeOnly(dep: Any): Dependency? = add("modRuntimeOnly", dependencyNotation(dep))
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

val shadowOnly: Configuration by configurations.creating

// The REI API artifact is a fake mod; keep it compile-only and load these only when a real REI jar is enabled.
val reiRunSupportMods: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
}

val runModsDirectory = rootProject.layout.projectDirectory.dir("versions/${target.projectName}/run/mods")

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

val skyHanniSystemProperties = providers.systemPropertiesPrefixedBy("skyhanni.")

val processedMixinConfig = layout.buildDirectory.file("resources/main/mixins.skyhanni.json")

val setDevelopmentMixinConfig by tasks.registering(SetMixinRequired::class) {
    dependsOn(tasks.processResources)
    mixinConfigFile.set(processedMixinConfig)
    required.set(true)
}

val setProductionMixinConfig by tasks.registering(SetMixinRequired::class) {
    dependsOn(tasks.processResources)
    mixinConfigFile.set(processedMixinConfig)
    required.set(false)
}

tasks.named<JavaExec>("runClient") {
    dependsOn(setDevelopmentMixinConfig)
    this.javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
    systemProperties(skyHanniSystemProperties.get())
    val runModsDirectoryFile = runModsDirectory.asFile
    val reiRunSupportModFiles = reiRunSupportMods.incoming.files
    inputs.files(fileTree(runModsDirectory.asFile) { include("*.jar") })
        .withPropertyName("enabledRunMods")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    doFirst {
        val enabledRunMods = runModsDirectoryFile.listFiles()
            ?.filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
            ?.map { it.name.lowercase() }
            .orEmpty()
        if (enabledRunMods.none { it.contains("roughlyenoughitems") }) return@doFirst

        val supportMods = reiRunSupportModFiles.files.filterNot { supportMod ->
            val supportModName = supportMod.name.lowercase()
            enabledRunMods.any { runModName ->
                when {
                    supportModName.contains("architectury") -> runModName.contains("architectury")
                    supportModName.contains("cloth-config") -> runModName.contains("cloth-config")
                    else -> false
                }
            }
        }
        if (supportMods.isEmpty()) return@doFirst

        val currentAddMods = systemProperties["fabric.addMods"]?.toString()?.takeIf { it.isNotBlank() }
        val reiSupportMods = supportMods.joinToString(File.pathSeparator) { it.absolutePath }
        systemProperty("fabric.addMods", listOfNotNull(currentAddMods, reiSupportMods).joinToString(File.pathSeparator))
    }
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

    compileOnly(libs.jbAnnotations)
    ksp(project(":annotation-processors"))?.let { compileOnly(it) }

    ksp(libs.autoservice.ksp)
    implementation(libs.autoservice.annotations)

    target.fabricLoaderVersion?.let {
        implementation(it)
        "productionRuntimeMods"(it)
    }
    target.fabricApiVersion?.let {
        implementation(it)
        "productionRuntimeMods"(it)
    }
    implementation(libs.fabricLanguageKotlin)
    "productionRuntimeMods"(libs.fabricLanguageKotlin)

    target.modMenuVersion?.let {
        implementation("maven.modrinth:modmenu:$it")
        "productionRuntimeMods"("maven.modrinth:modmenu:$it")
    }

    runtimeOnly(libs.devauth)
    "productionRuntimeMods"(libs.devauth)

    val moulconfigVersion = target.minecraftVersion.moulconfigMinecraftVersionOverride ?: target.minecraftVersion.versionName
    shadowImpl("org.notenoughupdates.moulconfig:modern-$moulconfigVersion:${libs.versions.moulconfig.get()}") {
        exclude("org.jetbrains.kotlin")
        exclude("org.jetbrains.kotlinx")
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

    implementation(target.hypixelModApiVersion)
    runtimeOnly(target.hypixelModApiFabricVersion)
    "productionRuntimeMods"(target.hypixelModApiFabricVersion)

    compileOnly(libs.roughlyenoughitems) { exclude(group = "net.fabricmc.fabric-api") }
    "minecraftTestClientRuntimeLibraries"(libs.roughlyenoughitems) {
        exclude(group = "net.fabricmc")
        exclude(group = "net.fabricmc.fabric-api")
    }
    compileOnly(libs.basicMath)
    "minecraftTestClientRuntimeLibraries"(libs.basicMath)
    val architecturyVersion = when (target) {
        ProjectTarget.MODERN_26200 -> "21.0.2"
        ProjectTarget.MODERN_26100 -> "20.0.6"
    }
    val clothConfigVersion = when (target) {
        ProjectTarget.MODERN_26200 -> "26.2.155"
        ProjectTarget.MODERN_26100 -> "26.1.154"
    }
    add(reiRunSupportMods.name, "dev.architectury:architectury-fabric:$architecturyVersion")
    add(reiRunSupportMods.name, "me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion")

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
    add("shadowImpl", dependencyNotation(dep)).also { (it as? ExternalModuleDependency)?.configure() }
}

afterEvaluate {
    loom.runs.named("client") {
        programArgs("--quickPlayMultiplayer", "hypixel.net")
    }

    ksp {
        arg("skyhanni.modver", version.toString())
        arg("skyhanni.mcver", target.minecraftVersion.versionName)
        //arg("skyhanni.cachedir", layout.buildDirectory.get().asFile.absolutePath)
        arg("skyhanni.buildpaths", project.file("buildpaths-excluded.txt").absolutePath)
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
            languageVersion = "2.3"
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
        dependsOn(setDevelopmentMixinConfig)
        dependsOn(tasks.named("configureLaunch"))
        val outputFile = project.file("build/regexes/constants.json")

        jvmArgs.add("-Dskyhanni.dumpRegex=${SHVersionInfo.gitHash}:${outputFile.absolutePath}")
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
excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.main)
excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.test)

tasks.withType<KotlinCompile> {
    compilerOptions {
        val jvmTargetStr = target.minecraftVersion.formattedKotlinJvmTarget
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetStr))
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.time.ExperimentalTime",
        )
        freeCompilerArgs.addAll(
            // 0 (all cores) triggers a race condition in JvmIrCodegenFactory's parallel codegen on Kotlin 2.3.x,
            // leaving corrupt .class files that break subsequent incremental builds.
            // see: https://youtrack.jetbrains.com/issue/KT-85498/
            "-Xbackend-threads=1",
            // This is so that workflows logs look cleaner, IntelliJ shows the warnings in the IDE anyway
            "-Xwarning-level=DEPRECATION:disabled",
            "-Xintrinsic-const-evaluation",
            "-Xcontext-sensitive-resolution"
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

tasks.shadowJar {
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
    archiveClassifier.set("")
    configurations = buildList {
        add(shadowImpl)
        add(shadowOnly)
    }
    exclude("META-INF/versions/**")
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "at.hannibal2.skyhanni.deps.libautoupdate")
    relocate("net.hypixel.modapi.tweaker", "at.hannibal2.skyhanni.deps.hypixel.modapi.tweaker")
    dependsOn(setProductionMixinConfig)
}
tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    dependsOn(setProductionMixinConfig)
}

tasks.assemble.get().dependsOn(tasks.shadowJar)

val sourcesJar by tasks.registering(Jar::class) {
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
    archiveClassifier.set("src")
    from(sourceSets.main.get().allSource)
}

publishing.publications {
    create<MavenPublication>("maven") {
        artifact(tasks.shadowJar)
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
