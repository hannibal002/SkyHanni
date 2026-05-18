package at.skyhanni.sharedvariables

import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * @param versionNameOverride Specify a version name override for stuff like snapshots
 * @param moulconfigMinecraftVersionOverride Specify a version of moulconfig that isn't the same
 * as the mc version for when you are still waiting for the next moulconfig release
 */
enum class MinecraftVersion(
    val versionName: String,
    val javaVersion: Int,
    val versionNameOverride: String? = null,
    val moulconfigMinecraftVersionOverride: String? = null,
) {
    MC12111("1.21.11", 21),
    MC26100("26.1", 25, versionNameOverride = "26.1.2"),
    ;

    val javaLanguageVersion = JavaLanguageVersion.of(javaVersion)

    val formattedJavaLanguageVersion: String
        get() = javaVersion.toString()

    val kotlinJvmTarget: Int get() = javaVersion
    val formattedKotlinJvmTarget: String get() = kotlinJvmTarget.toString()

    val versionNumber = run {
        val parts = versionName.split('.').mapTo(mutableListOf()) { it.toInt() }
        if (parts.size == 2) parts.add(0)
        require(parts.size == 3)
        parts[0] * 10000 + parts[1] * 100 + parts[2]
    }

    /**
     * The version string used in fabric.mod.json's minecraft dependency field.
     * For versions using the new 26.x+ versioning scheme, a tilde is prepended to allow compatible patch versions.
     */
    val fabricModJsonVersion: String
        get() = if (versionNumber >= 260100) "~$versionName" else versionName
}
