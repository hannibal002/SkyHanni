package at.skyhanni.sharedvariables

enum class ModrinthInfo(
    val minecraftVersions: List<String>,
    val loader: String,
    val dependencies: Map<ModrinthDependency, DependencyType>,
) {
    FORGE_1_8_9(
        minecraftVersions = listOf("1.8.9"),
        loader = "forge",
        dependencies = mapOf(
            ModrinthDependency.NOT_ENOUGH_UPDATES to DependencyType.OPTIONAL,
        ),
    ),
    FABRIC_1_21_5(
        minecraftVersions = listOf("1.21.5"),
        loader = "fabric",
        dependencies = mapOf(
            ModrinthDependency.FABRIC_API to DependencyType.REQUIRED,
            ModrinthDependency.FABRIC_LANGUAGE_KOTLIN to DependencyType.REQUIRED,
        ),
    ),
    FABRIC_1_21_7(
        minecraftVersions = listOf("1.21.6", "1.21.7"),
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
    SKYHANNI("byNkmv5G"),
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
