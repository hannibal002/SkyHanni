package at.skyhanni.sharedvariables

enum class ModrinthInfo(
    val minecraftVersions: List<String>,
    val loader: String,
    val dependencies: Map<ModrinthDependency, DependencyType>,
) {
    SKYHANNI_189(
        minecraftVersions = listOf("1.8.9"),
        loader = "forge",
        dependencies = mapOf(
            ModrinthDependency.NOT_ENOUGH_UPDATES to DependencyType.OPTIONAL,
        ),
    ),
    SKYHANNI_12105(
        minecraftVersions = listOf("1.21.5"),
        loader = "fabric",
        dependencies = mapOf(
            ModrinthDependency.FABRIC_API to DependencyType.REQUIRED,
            ModrinthDependency.FABRIC_LANGUAGE_KOTLIN to DependencyType.REQUIRED,
        ),
    ),
}

enum class ModrinthDependency(
    val projectId: String,
) {
    NOT_ENOUGH_UPDATES("GGamhqbw"),
    FABRIC_API("P7dR8mSH"),
    FABRIC_LANGUAGE_KOTLIN("Ha28R6CL"),
}

enum class DependencyType {
    REQUIRED,
    OPTIONAL,
    EMBEDDED,
    INCOMPATIBLE,
    ;

    val apiName = name.lowercase()
}
