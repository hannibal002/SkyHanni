package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.minecraft.packet.PacketSentEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket

@SkyHanniModule
object FixDoubleClicks {
    // TODO: use repo
    private val blazeDaggers = setOf(
        "BURSTFIRE_DAGGER",
        "BURSTMAW_DAGGER",
        "FIREDUST_DAGGER",
        "HEARTFIRE_DAGGER",
        "HEARTMAW_DAGGER",
        "MAWDUST_DAGGER",
    ).toInternalNames()

    /**
     * IMPORTANT: This is intentionally written as a packet cancellation because
     * it is a workaround for a bug that is only fixable by cancelling part of
     * the interaction. Cancelling the entire interaction would prevent players
     * from being able to use these items at all.
     *
     * We believe this is currently safe and the bug has been acknowledged by
     * Hypixel staff but said to be difficult to fix on their end. However, use
     * caution when modifying this code, as improper cancellation of packets
     * could lead to Watchdog bans.
     */
    @HandleEvent(onlyOnSkyblock = true, priority = HandleEvent.HIGHEST)
    fun onPacketSent(event: PacketSentEvent) {
        if (event.packet !is PlayerInteractBlockC2SPacket) return
        if (!SkyHanniMod.feature.misc.fixDoubleClicks) return

        val itemInHand = InventoryUtils.getItemInHand() ?: return
        val shouldPrevent = FishingApi.holdingRod || itemInHand.getInternalName() in blazeDaggers

        if (shouldPrevent) event.cancel()
    }
}
