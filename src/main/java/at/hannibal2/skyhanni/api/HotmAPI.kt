package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDrillUpgrades
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import kotlin.time.Duration.Companion.seconds

object HotmAPI {

    private val repoGroup = RepoPattern.group("data.hotmapi")

    private val hotmGuiPattern by repoGroup.pattern(
        "gui.name",
        "Heart of the Mountain"
    )

    fun copyCurrentTree() = HotmData.storage?.deepCopy()

    val activeMiningAbility get() = HotmData.abilities.firstOrNull { it.enabled }

    private val blueGoblinEgg = "GOBLIN_EGG_BLUE".asInternalName()

    private val blueEggCache = TimeLimitedCache<ItemStack, Boolean>(10.0.seconds)
    val isBlueEggActive
        get() = InventoryUtils.getItemInHand()?.let {
            blueEggCache.getOrPut(it) {
                it.getItemCategoryOrNull() == ItemCategory.DRILL && it.getDrillUpgrades()
                    ?.contains(blueGoblinEgg) == true
            }
        } == true

    enum class Powder() {
        MITHRIL,
        GEMSTONE,
        GLACITE,

        ;

        val lowName = name.lowercase().firstLetterUppercase()

        val heartPattern by repoGroup.pattern(
            "inventory.${name.lowercase()}.heart",
            "§7$lowName Powder: §a§.(?<powder>[\\d,]+)"
        )
        val resetPattern by repoGroup.pattern(
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
