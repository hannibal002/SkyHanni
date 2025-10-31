package at.hannibal2.hanni.features.misc

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.ClickType
import at.hannibal2.hanni.events.BlockClickEvent
import at.hannibal2.hanni.features.fishing.FishingApi
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.NeuInternalName.Companion.toInternalNames

@HanniModule
object FixDoubleClicks {
    private val blazeDaggers = setOf(
        "FIREDUST_DAGGER", "BURSTFIRE_DAGGER", "HEARTFIRE_DAGGER",
        "MAWDUST_DAGGER", "BURSTMAW_DAGGER", "HEARTMAW_DAGGER",
    ).toInternalNames()

    @HandleEvent(onlyOnSkyblock = true)
    fun onBlockClick(event: BlockClickEvent) {
        if (!HanniMod.feature.misc.fixDoubleClicks) return
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val itemInHand = event.itemInHand ?: return
        val shouldPrevent = FishingApi.holdingRod || blazeDaggers.contains(itemInHand.getInternalName())

        if (shouldPrevent) event.cancel()
    }
}
