package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.fishing.BaitUpdateEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberCastEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FishingBaitWarnings {

    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarnings

    private var lastBait: FishingApi.BaitType? = null
    private var wasUsingBait = true

    @HandleEvent
    fun onWorldChange() {
        lastBait = null
        wasUsingBait = true
    }

    @HandleEvent
    fun onBaitUpdate(event: BaitUpdateEvent) {
        if (!FishingApi.holdingRod) {
            wasUsingBait = false
            lastBait = null
            return
        }

        val bait = event.baitType

        lastBait?.let {
            if (it != bait && config.baitChangeWarning) {
                val beforeName = lastBait?.displayName ?: "None"
                val afterName = bait?.displayName ?: "None"
                showBaitChangeWarning(beforeName, afterName)
            }
        }
        wasUsingBait = bait != null
        lastBait = bait
    }

    @HandleEvent
    fun onBobberCast(event: FishingBobberCastEvent) {
        if (config.noBaitWarning && !wasUsingBait) showNoBaitWarning()
    }

    private fun showBaitChangeWarning(before: String, after: String) {
        SoundUtils.playClickSound()
        TitleManager.sendTitle("§eBait changed!", duration = 2.seconds)
        ChatUtils.chat("Fishing Bait changed: $before §e-> $after")
    }

    private fun showNoBaitWarning() {
        SoundUtils.playErrorSound()
        TitleManager.sendTitle("§cNo bait is used!", duration = 2.seconds)
        ChatUtils.chat("You're not using any fishing baits!")
    }
}
