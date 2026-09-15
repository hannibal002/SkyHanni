package at.skyhanni.sharedvariables

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val fabricLoaderVersion: String,
    val fabricApiVersion: String,
    val hypixelModApiVersion: String,
    val hypixelModApiFabricVersion: String,
    val modMenuVersion: String,
    val renderChestVersion: String?,
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
        renderChestVersion = null,
        modrinthInfo = ModrinthInfo.FABRIC_26_1,
    ),
    MODERN_26200(
        "26.2",
        MinecraftVersion.MC26200,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.155.2+26.2",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        // There is no 26.2 version; the 26.1 version works on 26.2.
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "20.0.0-beta.4",
        renderChestVersion = "1.0.3+26.2",
        modrinthInfo = ModrinthInfo.FABRIC_26_2,
    ),
    MODERN_26300(
        "26.3",
        MinecraftVersion.MC26300,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.19.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.159.1+26.3",
        hypixelModApiVersion = "net.hypixel:mod-api:1.0.2",
        // There is no 26.3 version; the 26.1 version works on 26.3.
        hypixelModApiFabricVersion = "maven.modrinth:hypixel-mod-api:1.0.2+build.1+mc26.1",
        modMenuVersion = "21.0.0-alpha.1",
        renderChestVersion = null,
        modrinthInfo = ModrinthInfo.FABRIC_26_3,
    ),

    ;

    val projectPath get() = ":$projectName"

    companion object {
        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return entries.find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
