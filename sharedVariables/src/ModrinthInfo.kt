package at.skyhanni.sharedvariables

enum class ModrinthInfo(
    val minecraftVersions: List<String>,
    val loader: String,
    val dependencies: Map<ModrinthDependency, DependencyType>,
) {
    FABRIC_26_1(
        minecraftVersions = listOf("26.1", "26.1.1", "26.1.2"),
        loader = "fabric",
        dependencies = mapOf(
            ModrinthDependency.FABRIC_API to DependencyType.REQUIRED,
            ModrinthDependency.FABRIC_LANGUAGE_KOTLIN to DependencyType.REQUIRED,
            ModrinthDependency.HYPIXEL_MOD_API to DependencyType.REQUIRED,
        )
    ),
    FABRIC_26_2(
        minecraftVersions = listOf("26.2"),
        loader = "fabric",
        dependencies = mapOf(
            ModrinthDependency.FABRIC_API to DependencyType.REQUIRED,
            ModrinthDependency.FABRIC_LANGUAGE_KOTLIN to DependencyType.REQUIRED,
            ModrinthDependency.HYPIXEL_MOD_API to DependencyType.REQUIRED,
        ),
    ),
    FABRIC_26_3(
        minecraftVersions = listOf("26.3"),
        loader = "fabric",
        dependencies = mapOf(
            ModrinthDependency.FABRIC_API to DependencyType.REQUIRED,
            ModrinthDependency.FABRIC_LANGUAGE_KOTLIN to DependencyType.REQUIRED,
            ModrinthDependency.HYPIXEL_MOD_API to DependencyType.REQUIRED,
        ),
    ),
}

enum class ModrinthDependency(
    val projectId: String,
) {
    SKYHANNI("byNkmv5G"),
    FABRIC_API("P7dR8mSH"),
    FABRIC_LANGUAGE_KOTLIN("Ha28R6CL"),
    HYPIXEL_MOD_API("1A2mKfBx"),
}

enum class DependencyType {
    REQUIRED,
    OPTIONAL,
    EMBEDDED,
    INCOMPATIBLE,
    ;

    val apiName = name.lowercase()
}
