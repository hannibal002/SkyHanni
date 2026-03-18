package at.hannibal2.skyhanni.utils.system

import at.hannibal2.skyhanni.utils.json.SkyHanniAdaptable

data class ModVersion(
    val stable: Int,
    val beta: Int,
    val bugfix: Int,
) : Comparable<ModVersion>, SkyHanniAdaptable<ModVersion> {

    val isBeta get() = beta != 0
    val isBackport get() = beta == 0 && bugfix != 0

    inline val asString get() = toString()
    override fun toString(): String = "$stable.$beta.$bugfix"
    override fun toJsonString(): String = toString()

    override fun compareTo(other: ModVersion): Int = when {
        stable != other.stable -> stable.compareTo(other.stable)
        beta != other.beta -> beta.compareTo(other.beta)
        else -> bugfix.compareTo(other.bugfix)
    }

    fun isValid() = stable != 0

    /** Inclusive for both borders */
    fun isInBetween(lower: ModVersion, higher: ModVersion): Boolean {
        return this in lower..higher
    }

    companion object : SkyHanniAdaptable.Factory<ModVersion> {
        fun fromString(version: String): ModVersion = version.split('.').let { parts ->
            ModVersion(
                parts.getOrNull(0)?.toIntOrNull() ?: 0,
                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                parts.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }

        override fun fromJsonString(json: String): ModVersion = fromString(json)
    }
}
