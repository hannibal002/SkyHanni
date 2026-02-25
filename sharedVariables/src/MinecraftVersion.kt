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
    MC12110("1.21.10", 21),
    MC12111("1.21.11", 21),
    ;

    val javaLanguageVersion = JavaLanguageVersion.of(javaVersion)

    val formattedJavaLanguageVersion: String
        get() = javaVersion.toString()

    val versionNumber = run {
        val parts = versionName.split('.').mapTo(mutableListOf()) { it.toInt() }
        if (parts.size == 2) parts.add(0)
        require(parts.size == 3)
        parts[0] * 10000 + parts[1] * 100 + parts[2]
    }
}
