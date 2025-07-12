package at.skyhanni.sharedvariables

private fun yarn(version: String): String = "net.fabricmc:yarn:${version}:v2"

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val mappingStyle: MappingStyle,
    val forgeDep: String? = null,
    val fabricLoaderVersion: String? = null,
    val fabricApiVersion: String? = null,
    val modMenuVersion: String? = null,
    val isModern: Boolean = false,
    val modrinthInfo: ModrinthInfo? = null,
    linkTo: String?,
) {
    MAIN(
        "1.8.9",
        MinecraftVersion.MC189,
        "de.oceanlabs.mcp:mcp_stable:22-1.8.9@zip",
        MappingStyle.SEARGE,
        forgeDep = "net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9",
        modrinthInfo = ModrinthInfo.FORGE_1_8_9,
        linkTo = "BRIDGE116FORGE",
    ),
    BRIDGE116FORGE(
        "1.16.5-forge",
        MinecraftVersion.MC11605,
        "official",
        MappingStyle.SEARGE,
        forgeDep = "net.minecraftforge:forge:1.16.5-36.2.39",
        linkTo = "BRIDGE116FABRIC",
    ),
    BRIDGE116FABRIC(
        "1.16.5-fabric",
        MinecraftVersion.MC11605,
        yarn("1.16.5+build.10"),
        MappingStyle.YARN,
        linkTo = "MODERN_12105",
    ),
    MODERN_12105(
        "1.21.5",
        MinecraftVersion.MC12105,
        yarn("1.21.5+build.1"),
        MappingStyle.YARN,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.16.13",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.126.0+1.21.5",
        modMenuVersion = "14.0.0-rc.2",
        isModern = true,
        modrinthInfo = ModrinthInfo.FABRIC_1_21_5,
//         linkTo = "MODERN_12107",
//     ),
//     MODERN_12107(
//         "1.21.7",
//         MinecraftVersion.MC12107,
//         yarn("1.21.7+build.6"),
//         MappingStyle.YARN,
//         fabricLoaderVersion = "net.fabricmc:fabric-loader:0.16.14",
//         fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.128.2+1.21.6",
//         modMenuVersion = "15.0.0-beta.3",
//         isModern = true,
//         modrinthInfo = ModrinthInfo.FABRIC_1_21_7,
        linkTo = null,
    ),
    ;

    val isBridge get() = name.contains("bridge")

    val linkTo by lazy {
        if (linkTo == null) null
        else {
            ProjectTarget.values().find { it.name == linkTo }!!
        }
    }
    val parent by lazy {
        values().find { it.linkTo == this }
    }
    val isForge get() = forgeDep != null
    val isFabric get() = forgeDep == null

    val projectPath get() = ":$projectName"

    companion object {
        fun activeVersions() = values().filter { MultiVersionStage.activeState.shouldCreateProject(it) }

        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return values().find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
