package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames

@SkyHanniModule
object FixDoubleClicks {
    private val blazeDaggers = setOf(
        "FIREDUST_DAGGER", "BURSTFIRE_DAGGER", "HEARTFIRE_DAGGER",
        "MAWDUST_DAGGER", "BURSTMAW_DAGGER", "HEARTMAW_DAGGER",
    ).toInternalNames()

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockClick(event: BlockClickEvent) {
        if (!SkyHanniMod.feature.misc.fixDoubleClicks) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val itemInHand = event.itemInHand ?: return
        val shouldPrevent = FishingApi.holdingRod || blazeDaggers.contains(itemInHand.getInternalName())

        if (shouldPrevent) event.cancel()
    }
}
