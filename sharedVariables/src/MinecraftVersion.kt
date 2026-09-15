package at.skyhanni.sharedvariables

import org.gradle.jvm.toolchain.JavaLanguageVersion

/**
 * @param versionNameOverride Specify a version name override for stuff like snapshots
 * @param moulconfigMinecraftVersionOverride Specify a version of MoulConfig that isn't the same
 * as the Minecraft version for when you are still waiting for the next MoulConfig release
 */
enum class MinecraftVersion(
    val versionName: String,
    val javaVersion: Int,
    val versionNameOverride: String? = null,
    val moulconfigMinecraftVersionOverride: String? = null,
) {
    MC26100("26.1", 25),
    MC26200("26.2", 25),
    MC26300("26.3", 25, versionNameOverride = "26.3-pre-1", moulconfigMinecraftVersionOverride = "26.2"),
    ;

    val javaLanguageVersion = JavaLanguageVersion.of(javaVersion)
    val formattedJavaLanguageVersion: String get() = javaVersion.toString()

    val kotlinJvmTarget: Int get() = javaVersion
    val formattedKotlinJvmTarget: String get() = kotlinJvmTarget.toString()

    /**
     * The version string used in fabric.mod.json's minecraft dependency field.
     * For versions using the new 26.x+ versioning scheme, a tilde is prepended to allow compatible patch versions.
     */
    val fabricModJsonVersion: String get() = "~${(versionNameOverride ?: versionName).toSemVer()}"

    private fun String.toSemVer() = replace("-snapshot", "-alpha").replace(Regex("""-(\d+)$"""), ".$1")
}
