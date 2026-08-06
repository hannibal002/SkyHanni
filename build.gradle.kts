import at.skyhanni.sharedvariables.ProjectTarget
import at.skyhanni.sharedvariables.SHVersionInfo
import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.kikugie.stonecutter.StonecutterExperimentalAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.fabricmc.loom.task.RemapSourcesJarTask
import net.fabricmc.loom.task.ValidateAccessWidenerTask
import net.fabricmc.loom.task.prod.ClientProductionRunTask
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
    id("net.fabricmc.fabric-loom")
    kotlin("jvm")
    id("com.google.devtools.ksp")
    `maven-publish`
    id("dev.detekt")
}

val target = ProjectTarget.entries.find { it.projectPath == project.path }!!
val primaryTarget = ProjectTarget.MODERN_26100

fun dependencyNotation(dep: Any): Any = (dep as? Provider<*>)?.get() ?: dep

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
            appendProjectPathToDisplayName.set(true)
            runDirectory = rootProject.file("versions/${target.projectName}/run").relativeTo(projectDir)
            if (System.getenv("repo_action") != "true") {
                systemProperties.put("devauth.configDir", rootProject.file(".devauth").absolutePath)
            }
            jvmArguments.addAll("-Xmx4G", "-Dnarrator.none=true")
            programArguments.addAll(
                "--tweakClass", "at.hannibal2.skyhanni.tweaker.SkyHanniTweaker",
                "--tweakClass", "io.github.notenoughupdates.moulconfig.tweaker.DevelopmentResourceTweaker",
            )
        }
        removeIf { it.name == "server" }
    }
}

val shadowImpl: Configuration = configurations.create("shadowImpl") {
    configurations.implementation.get().extendsFrom(this)
}

val shadowOnly: Configuration = configurations.create("shadowOnly")

val mixinTestRuntime: Configuration = configurations.create("mixinTestRuntime") {
    isCanBeConsumed = false
    extendsFrom(configurations.testRuntimeClasspath.get())
}

val includeBackupRepo = tasks.register<DownloadBackupRepo>("includeBackupRepo") {
    description = "Includes the a backup of the SkyHanni repo into the final jar"
    this.user = "hannibal002"
    this.repo = "SkyHanni-Repo"
    this.branch = "main"
    this.resourcePath = "assets/skyhanni/repo.tar.gz"
    this.outputDirectory.set(layout.buildDirectory.dir("downloadedRepo"))
}

val includeBackupNeuRepo = tasks.register<DownloadBackupRepo>("includeBackupNeuRepo") {
    description = "Includes the a backup of the NotEnoughUpdates repo into the final jar"
    this.user = "NotEnoughUpdates"
    this.repo = "NotEnoughUpdates-Repo"
    this.branch = "master"
    this.resourcePath = "assets/skyhanni/neu-repo.tar.gz"
    this.outputDirectory.set(layout.buildDirectory.dir("downloadedNeuRepo"))
}

val publishToModrinth = tasks.register<PublishToModrinth>("publishToModrinth")

tasks.named<JavaExec>("runClient") {
    this.javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}

@Suppress("UnstableApiUsage")
tasks.register<ClientProductionRunTask>("prodClient") {
    description = "Runs the client in a production-like environment."
    notCompatibleWithConfigurationCache("Interactive client launches must start a new process every time.")
    outputs.upToDateWhen { false }
    runDir = file("run")
}

if (target == primaryTarget) {
    tasks.register("checkPrDescription", ChangelogVerification::class) {
        this.prTitle = System.getenv("PR_TITLE") ?: project.findProperty("prTitle") as? String ?: ""
        this.prBody = System.getenv("PR_BODY") ?: project.findProperty("prBody") as? String ?: ""
        this.outputDirectory.set(
            layout.buildDirectory.dir("changelog-verification")
        )
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
        mixinTestRuntime("net.fabricmc:fabric-loader-junit:${it.substringAfterLast(':')}")
    }
    target.fabricApiVersion?.let {
        implementation(it)
        "productionRuntimeMods"(it)
    }
    implementation(libs.fabricLanguageKotlin)
    "productionRuntimeMods"(libs.fabricLanguageKotlin)

    target.modMenuVersion?.let {
        implementation("maven.modrinth:modmenu:$it")
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

    val reiVersion = when (target) {
        ProjectTarget.MODERN_26100 -> "26.1.819"
    }
    val reiApi = "me.shedaniel:RoughlyEnoughItems-api:$reiVersion"
    compileOnly(reiApi) { isTransitive = false }
    "minecraftTestClientRuntimeLibraries"(reiApi) {
        isTransitive = false
    }
    compileOnly(libs.basicMath)
    "minecraftTestClientRuntimeLibraries"(libs.basicMath)

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
        programArguments.addAll("--quickPlayMultiplayer", "hypixel.net")
    }

    ksp {
        arg("skyhanni.modver", version.toString())
        arg("skyhanni.mcver", target.minecraftVersion.versionName)
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

val mixinTest = tasks.register<Test>("mixinTest") {
    description = "Audits mixin application under Fabric Loader."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().output + sourceSets.main.get().output + mixinTestRuntime
    filter {
        includeTestsMatching("at.hannibal2.skyhanni.test.MixinTest")
    }
}

tasks.test {
    dependsOn(mixinTest)
    exclude("at/hannibal2/skyhanni/test/MixinTest.class")
}

kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.4"
        }
    }
}

// Tasks:
tasks.processResources {
    from(includeBackupRepo)
    from(includeBackupNeuRepo)
    val fapiVersion = target.fabricApiVersion.split(":").last()
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
excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.main)
excludeBuildPaths(file("buildpaths-excluded.txt"), sourceSets.test)

tasks.withType<KotlinCompile> {
    compilerOptions {
        val jvmTargetStr = target.minecraftVersion.formattedKotlinJvmTarget
        jvmTarget.set(JvmTarget.fromTarget(jvmTargetStr))
        allWarningsAsErrors = true
        optIn.addAll(
            "kotlin.concurrent.atomics.ExperimentalAtomicApi",
            "kotlin.time.ExperimentalTime"
        )
        freeCompilerArgs.addAll(
            // 0 (all cores) triggers a race condition in JvmIrCodegenFactory's parallel codegen on Kotlin 2.4.x,
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
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations = buildList {
        add(shadowImpl)
        add(shadowOnly)
    }
    exclude("META-INF/versions/**")
    exclude("META-INF/*.kotlin_module")
    mergeServiceFiles()
    relocate("io.github.notenoughupdates.moulconfig", "at.hannibal2.skyhanni.deps.moulconfig")
    relocate("moe.nea.libautoupdate", "at.hannibal2.skyhanni.deps.libautoupdate")
    relocate("net.hypixel.modapi.tweaker", "at.hannibal2.skyhanni.deps.hypixel.modapi.tweaker")
}
tasks.jar {
    archiveClassifier.set("nodeps")
    destinationDirectory.set(layout.buildDirectory.dir("badjars"))
}

tasks.assemble.get().dependsOn(tasks.shadowJar)

val sourcesJar = tasks.register<Jar>("sourcesJar") {
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
