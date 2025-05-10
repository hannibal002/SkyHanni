package at.hannibal2.skyhanni.features.mining

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.HotmApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.HotmData
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor

@SkyHanniModule
object CurrentPowderOnHotmPerk {

    private val config get() = SkyHanniMod.feature.mining.hotm

    @HandleEvent
    fun onToolTip(event: ToolTipEvent) {
        if (!isEnabled()) return

        val itemName = event.itemStack.displayName
        val perk = HotmData.getPerkByNameOrNull(itemName.removeColor()) ?: return

        if (perk.isMaxLevel || !perk.isUnlocked) return

        val powderType = determinePowderType(perk) ?: return
        val index = event.toolTip.indexOfFirst { it.contains("Cost") }

        event.toolTip.add(index + 2, "You have")
        event.toolTip.add(index + 3, "${powderType.color}${powderType.current.addSeparators()} ${powderType.displayName} Powder")
     }

    private val MITHRIL_PERKS = setOf(
        "MINING_SPEED", "MINING_SPEED_BOOST", "PRECISION_MINING", "MINING_FORTUNE",
        "TITANIUM_INSANIUM", "LUCK_OF_THE_CAVE", "EFFICIENT_MINER", "QUICK_FORGE"
    )

    private val GEMSTONE_PERKS = setOf(
        "OLD_SCHOOL", "PROFESSIONAL", "MOLE", "GEM_LOVER", "SEASONED_MINEMAN", "BLOCKHEAD", "SUBTERRANEAN_FISHER",
        "KEEP_IT_COOL", "LONESOME_MINER", "GREAT_EXPLORER", "SPEEDY_MINEMAN", "POWDER_BUFF", "FORTUNATE_MINEMAN"
    )

    private val GLACITE_PERKS = setOf(
        "NO_STONE_UNTURNED", "STRONG_ARM", "STEADY_HAND", "WARM_HEARTED", "SURVEYOR", "MINESHAFT_MAYHEM", "METAL_HEAD", "RAGS_TO_RICHES",
        "EAGER_ADVENTURER", "CRYSTALLINE", "GIFTS_FROM_THE_DEPARTED", "MINING_MASTER", "DEAD_MANS_CHEST", "VANGUARD_SEEKER"
    )

    private fun determinePowderType(perk: HotmData): HotmApi.PowderType? {
        return when (perk.name) {
            in MITHRIL_PERKS -> HotmApi.PowderType.MITHRIL
            in GEMSTONE_PERKS -> HotmApi.PowderType.GEMSTONE
            in GLACITE_PERKS -> HotmApi.PowderType.GLACITE
            else -> null
        }
    }

    private fun isEnabled() = SkyBlockUtils.inSkyBlock && HotmData.inInventory && config.currentPowder

}
