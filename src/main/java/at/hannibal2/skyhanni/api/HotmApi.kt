package at.hannibal2.skyhanni.api

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.hotx.HotmData
import at.hannibal2.skyhanni.data.hotx.HotxPatterns.asPatternId
import at.hannibal2.skyhanni.data.hotx.RotatingPerk
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.events.mining.PowderEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDrillUpgrades
import at.hannibal2.skyhanni.utils.collection.TimeLimitedCache
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import org.intellij.lang.annotations.Language
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object HotmApi {

    fun copyCurrentTree() = HotmData.storage?.deepCopy()

    val activeMiningAbility get() = HotmData.abilities.firstOrNull { it.enabled }

    private val blueGoblinEgg = "GOBLIN_OMELETTE_BLUE_CHEESE".toInternalName()

    private val blueEggCache = TimeLimitedCache<SafeItemStack, Boolean>(10.0.seconds, useWeakKeys = true)
    val isBlueEggActive
        get() = InventoryUtils.getItemInHand()?.let {
            blueEggCache.getOrPut(it) {
                it.getItemCategoryOrNull() == ItemCategory.DRILL &&
                    it.getDrillUpgrades()?.contains(blueGoblinEgg) == true
            }
        } == true

    enum class PowderType(val displayName: String, val color: String) {
        MITHRIL("Mithril", "§2"),
        GEMSTONE("Gemstone", "§d"),
        GLACITE("Glacite", "§b"),

        ;

        val heartPattern by RepoPattern.pattern(
            "inventory.${name.lowercase()}.heart",
            "$displayName Powder: (?<powder>[\\d,]+)",
        )
        val resetPattern by RepoPattern.pattern(
            "inventory.${name.lowercase()}.reset",
            "\\s+- (?<powder>[\\d,]+) $displayName Powder",
        )

        fun pattern(isHeart: Boolean) = if (isHeart) heartPattern else resetPattern

        private val storage: ProfileSpecificStorage.PowderStorage?
            get() = ProfileStorageData.profileSpecific?.mining?.powder?.getOrPut(this, ProfileSpecificStorage::PowderStorage)

        var current: Long
            get() = storage?.available ?: 0L
            private set(value) {
                storage?.available = value
            }

        var total: Long
            get() = storage?.total ?: 0L
            set(value) {
                storage?.total = value
            }

        fun setAmount(value: Long, postEvent: Boolean = false) {
            val diff = value - current
            if (diff == 0L) return
            total += diff
            current = value
            if (!postEvent) return
            if (diff > 0) {
                if (shouldSendDebug) ChatUtils.debug("Gained §a${diff.addSeparators()} $color$displayName Powder")
                PowderEvent.Gain(this, diff).post()
            } else {
                if (shouldSendDebug) ChatUtils.debug("Spent §a${diff.addSeparators()} $color$displayName Powder")
                PowderEvent.Spent(this, diff).post()
            }
        }

        fun resetTree() {
            current = total
            PowderEvent.Reset(this).post()
        }

        fun resetFull() {
            current = 0L
            total = 0L
            PowderEvent.Reset(this).post()
        }

        companion object {
            private val shouldSendDebug: Boolean get() = SkyHanniMod.feature.dev.debug.powderMessages
        }
    }

    var skymall: SkymallPerk? = null

    var mineshaftMayhem: MayhemPerk? = null

    enum class SkymallPerk(
        override val displayDescription: String,
        @field:Language("RegExp") val chatFallback: String,
        @field:Language("RegExp") val itemFallback: String,
    ) : RotatingPerk {
        MINING_SPEED(
            displayDescription = "§6+100${SkyblockStat.MINING_SPEED.hypixelIcon} Mining Speed",
            chatFallback = "Gain \\+100${SkyblockStat.MINING_SPEED.hypixelIcon} Mining Speed\\.",
            itemFallback = "Gain \\+100${SkyblockStat.MINING_SPEED.hypixelIcon} Mining Speed\\.",
        ),
        MINING_FORTUNE(
            displayDescription = "§6+50${SkyblockStat.MINING_FORTUNE.hypixelIcon} Mining Fortune",
            chatFallback = "Gain \\+50${SkyblockStat.MINING_FORTUNE.hypixelIcon} Mining Fortune\\.",
            itemFallback = "Gain \\+50${SkyblockStat.MINING_FORTUNE.hypixelIcon} Mining Fortune\\.",
        ),
        EXTRA_POWDER(
            displayDescription = "§a+15% §7more Powder",
            chatFallback = "Gain \\+15% more Powder while mining\\.",
            itemFallback = "Gain \\+15% more Powder while mining\\.",
        ),
        ABILITY_COOLDOWN(
            displayDescription = "§a-20% §7Pickaxe Ability cooldowns",
            chatFallback = "-20% Pickaxe Ability cooldowns\\.",
            itemFallback = "-20% Pickaxe Ability cooldowns\\.",
        ),
        GOBLIN_CHANCE(
            displayDescription = "§a10x §6Gold §7& §bDiamond §7Goblin chance",
            chatFallback = "10x chance to find Golden and Diamond Goblins\\.",
            itemFallback = "10x chance to find Golden and",
        ),
        TITANIUM(
            displayDescription = "§a5x §9Titanium §7drops",
            chatFallback = "Gain 5x Titanium drops",
            itemFallback = "Gain 5x Titanium drops\\.",
        ),
        ;

        private val basePath = "mining.hotm.skymall"
        override val chatPattern by RepoPattern.pattern("$basePath.chat.${asPatternId()}", chatFallback)
        override val itemPattern by RepoPattern.pattern("$basePath.item.${asPatternId()}", itemFallback)
    }

    @Suppress("MaxLineLength")
    enum class MayhemPerk(
        @field:Language("RegExp") val chatFallback: String,
    ) {
        SCRAP_CHANCE("Your Suspicious Scrap chance was buffed by your Mineshaft Mayhem perk!"),
        MINING_FORTUNE("You received a ${SkyblockStat.MINING_FORTUNE.hypixelIcon} Mining Fortune buff from your Mineshaft Mayhem perk!"),
        MINING_SPEED("You received a ${SkyblockStat.MINING_SPEED.hypixelIcon} Mining Speed buff from your Mineshaft Mayhem perk!"),
        COLD_RESISTANCE("You received a ${SkyblockStat.COLD_RESISTANCE.hypixelIcon} Cold Resistance buff from your Mineshaft Mayhem perk!"),
        ABILITY_COOLDOWN("Your Pickaxe Ability cooldown was reduced from your Mineshaft Mayhem perk!"),
        ;

        val chatPattern by RepoPattern.pattern("mining.hotm.mayhem.chat.${asPatternId()}", chatFallback)
    }
}
