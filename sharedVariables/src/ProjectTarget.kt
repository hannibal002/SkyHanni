package at.skyhanni.sharedvariables

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val mappingStyle: MappingStyle,
    val fabricLoaderVersion: String,
    val fabricApiVersion: String,
    val hypixelModApiVersion: String,
    val hypixelModApiFabricVersion: String,
    val modMenuVersion: String,
    val modrinthInfo: ModrinthInfo,
) {
    MODERN_12111(
        "1.21.11",
        MinecraftVersion.MC12111,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.1",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.1+build.1+mc1.21",
        modMenuVersion = "17.0.0",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_11,
    ),
    MODERN_26100(
        "26.1",
        MinecraftVersion.MC26100,
        "official",
        MappingStyle.NONE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.151.0+26.1.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "18.0.0-beta.1",
        modrinthInfo = ModrinthInfo.FABRIC_26_1,
    ),
    MODERN_26200(
        "26.2",
        MinecraftVersion.MC26200,
        "official",
        MappingStyle.NONE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.151.0+26.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "20.0.0-alpha.1",
        modrinthInfo = ModrinthInfo.FABRIC_26_2,
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
