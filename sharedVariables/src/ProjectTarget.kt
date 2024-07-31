package at.skyhanni.sharedvariables

private fun yarn(version: String): String = "net.fabricmc:yarn:${version}:v2"

enum class ProjectTarget(
    val projectName: String,
    val minecraftVersion: MinecraftVersion,
    val mappingDependency: String,
    val mappingStyle: MappingStyle,
    val forgeDep: String?,
    linkTo: String?,
) {
    MAIN(
        "1.8.9",
        MinecraftVersion.MC189,
        "de.oceanlabs.mcp:mcp_stable:22-1.8.9@zip",
        MappingStyle.SEARGE,
        "net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9",
        "BRIDGE_FORGE",
    ),
    BRIDGE_FORGE(
        "1.14.4-forge",
        MinecraftVersion.MC1144,
        "de.oceanlabs.mcp:mcp_stable:58-1.14.4@zip",
        MappingStyle.SEARGE,
        "net.minecraftforge:forge:1.14.4-28.1.113",
        "BRIDGE_FABRIC",
    ),
    BRIDGE_FABRIC(
        "1.14.4",
        MinecraftVersion.MC1144,
        yarn("1.14.4+build.1"),
        MappingStyle.YARN,
        null,
        "MODERN",
    ),
    MODERN(
        "1.21",
        MinecraftVersion.MC121,
        yarn("1.21+build.9"),
        MappingStyle.YARN,
        null,
        null,
    )
    ;

    val isBridge get() = this == BRIDGE_FABRIC || this == BRIDGE_FORGE

    val linkTo by lazy {
        if (linkTo == null) null
        else {
            ProjectTarget.values().find { it.name == linkTo }!!
        }
    }
    val isForge get() = forgeDep != null
    val isFabric get() = forgeDep == null

    val projectPath get() = ":$projectName"
}
