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
