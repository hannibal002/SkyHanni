package at.skyhanni.sharedvariables

enum class ModrinthInfo(
    val minecraftVersions: List<String>,
    val loader: String,
    val dependencies: Map<ModrinthDependency, DependencyType>,
) {
    FABRIC_1_21_10(
        minecraftVersions = listOf("1.21.10"),
        loader = "fabric",
        dependencies = mapOf(
            ModrinthDependency.FABRIC_API to DependencyType.REQUIRED,
            ModrinthDependency.FABRIC_LANGUAGE_KOTLIN to DependencyType.REQUIRED,
        ),
    ),
    FABRIC_1_21_11(
        minecraftVersions = listOf("1.21.11"),
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
