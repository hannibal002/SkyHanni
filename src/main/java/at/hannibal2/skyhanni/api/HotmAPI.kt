package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

object HotmAPI {

    fun copyCurrentTree() = HotmData.storage?.deepCopy()

    val activeMiningAbility get() = HotmData.abilities.firstOrNull { it.enabled }

    enum class Powder() {
        MITHRIL,
        GEMSTONE,
        GLACITE,

        ;

        val lowName = name.lowercase().firstLetterUppercase()

        val heartPattern by RepoPattern.pattern(
            "inventory.${name.lowercase()}.heart",
            "§7$lowName Powder: §a§.(?<powder>[\\d,]+)"
        )
        val resetPattern by RepoPattern.pattern(
            "inventory.${name.lowercase()}.reset",
            "\\s+§8- §.(?<powder>[\\d,]+) $lowName Powder"
        )

        fun pattern(isHeart: Boolean) = if (isHeart) heartPattern else resetPattern

        fun getStorage() = ProfileStorageData.profileSpecific?.mining?.powder?.get(this)

        fun getCurrent() = getStorage()?.available ?: 0L

        fun setCurrent(value: Long) {
            getStorage()?.available = value
        }

        fun addCurrent(value: Long) {
            setCurrent(getCurrent() + value)
        }

        fun getTotal() = getStorage()?.total ?: 0L

        fun setTotal(value: Long) {
            getStorage()?.total = value
        }

        fun addTotal(value: Long) {
            setTotal(getTotal() + value)
        }

        /** Use when new powder gets collected*/ // TODO (future) use this for each Powder source
        fun gain(value: Long) {
            addTotal(value)
            addCurrent(value)
        }

        fun reset() {
            setCurrent(0)
            setTotal(0)
        }
    }
}
