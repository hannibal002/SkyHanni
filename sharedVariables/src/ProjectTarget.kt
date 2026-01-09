package at.skyhanni.sharedvariables

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val mappingStyle: MappingStyle,
    val fabricLoaderVersion: String? = null,
    val fabricApiVersion: String? = null,
    val modMenuVersion: String? = null,
    val modrinthInfo: ModrinthInfo? = null,
    linkTo: String?,
) {
    MODERN_12105(
        "1.21.5",
        MinecraftVersion.MC12105,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.16.13",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.126.0+1.21.5",
        modMenuVersion = "14.0.0-rc.2",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_5,
        linkTo = "MODERN_12108",
    ),
    MODERN_12108(
        "1.21.8",
        MinecraftVersion.MC12108,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.17.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.134.0+1.21.8",
        modMenuVersion = "15.0.0-beta.3",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_8,
        linkTo = "MODERN_12110",
    ),
    MODERN_12110(
        "1.21.10",
        MinecraftVersion.MC12110,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.17.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.135.0+1.21.10",
        modMenuVersion = "16.0.0-rc.1",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_10,
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

    val projectPath get() = ":$projectName"

    companion object {
        fun activeVersions() = values().filter { MultiVersionStage.activeState.shouldCreateProject(it) }

        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return values().find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
