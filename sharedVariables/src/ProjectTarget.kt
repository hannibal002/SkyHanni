package at.skyhanni.sharedvariables

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val mappingStyle: MappingStyle,
    val fabricLoaderVersion: String? = null,
    val fabricApiVersion: String? = null,
    val hypixelModApiVersion: String = "net.hypixel:mod-api:1.0.1",
    val hypixelModApiFabricVersion: String = "maven.modrinth:hypixel-mod-api:1.0.1+build.1+mc1.21",
    val modMenuVersion: String? = null,
    val modrinthInfo: ModrinthInfo? = null,
) {
    MODERN_12111(
        "1.21.11",
        MinecraftVersion.MC12111,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.18.6",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.141.3+1.21.11",
        modMenuVersion = "17.0.0",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_11,
    ),
    MODERN_26100(
        "26.1",
        MinecraftVersion.MC26100,
        "official",
        MappingStyle.NONE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.18.6",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.147.0+26.1.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "18.0.0-alpha.8",
        modrinthInfo = ModrinthInfo.FABRIC_26_1,
    ),
    ;

    val projectPath get() = ":$projectName"

    companion object {
        fun activeVersions() = entries.filter { MultiVersionStage.activeState.shouldCreateProject(it) }

        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return entries.find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
