package at.skyhanni.sharedvariables

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val fabricLoaderVersion: String,
    val fabricApiVersion: String,
    val hypixelModApiVersion: String,
    val hypixelModApiFabricVersion: String,
    val modMenuVersion: String,
    val modrinthInfo: ModrinthInfo,
) {
    MODERN_26100(
        "26.1",
        MinecraftVersion.MC26100,
        "official",
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        // 0.154.0 bundles fabric-screen-api-v1 5.1.0, whose char events currently crash with
        // Architectury's synthetic REI input delegate screens.
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.153.0+26.1.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "18.0.0-beta.1",
        modrinthInfo = ModrinthInfo.FABRIC_26_1,
    ),
    MODERN_26200(
        "26.2",
        MinecraftVersion.MC26200,
        "official",
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        // 0.154.0 bundles fabric-screen-api-v1 5.1.0, whose char events currently crash with
        // Architectury's synthetic REI input delegate screens.
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.153.0+26.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "20.0.0-beta.4",
        modrinthInfo = ModrinthInfo.FABRIC_26_2,
    ),

    ;

    val projectPath get() = ":$projectName"

    companion object {
        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return entries.find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
