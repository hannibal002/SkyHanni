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
    MAIN("1.8.9", MinecraftVersion.MC189, "de.oceanlabs.mcp:mcp_stable:22-1.8.9@zip", MappingStyle.SEARGE, null, null),
    ;

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
