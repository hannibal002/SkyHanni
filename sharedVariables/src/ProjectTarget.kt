package at.skyhanni.sharedvariables

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
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
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.141.5+1.21.11",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.1",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.1+build.1+mc1.21",
        modMenuVersion = "17.0.0",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_11,
    ),
    MODERN_26100(
        "26.1",
        MinecraftVersion.MC26100,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.155.2+26.1.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "18.0.0",
        modrinthInfo = ModrinthInfo.FABRIC_26_1,
    ),
    ;

    val projectPath get() = ":$projectName"

    companion object {
        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return entries.find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
