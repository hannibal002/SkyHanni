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
) {
    MODERN_12110(
        "1.21.10",
        MinecraftVersion.MC12110,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.17.3",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.138.4+1.21.10",
        modMenuVersion = "16.0.0-rc.1",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_10,
    ),
    MODERN_12111(
        "1.21.11",
        MinecraftVersion.MC12111,
        "official",
        MappingStyle.SEARGE,
        fabricLoaderVersion = "net.fabricmc:fabric-loader:0.18.4",
        fabricApiVersion = "net.fabricmc.fabric-api:fabric-api:0.141.2+1.21.11",
        modMenuVersion = "17.0.0-beta.2",
        modrinthInfo = ModrinthInfo.FABRIC_1_21_11,
    ),
    ;

    val projectPath get() = ":$projectName"

    companion object {
        fun activeVersions() = values().filter { MultiVersionStage.activeState.shouldCreateProject(it) }

        fun findByMcVersion(mcVersion: String): ProjectTarget? {
            return values().find { it.minecraftVersion.versionName == mcVersion }
        }
    }
}
