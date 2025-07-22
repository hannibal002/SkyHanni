package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.events.fishing.FishingBobberInLiquidEvent
import at.hannibal2.skyhanni.features.fishing.FishingApi.isBait
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.collection.TimeLimitedSet
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityItem
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FishingBaitWarnings {

    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarnings

    private data class Bait(
        private val entity: EntityItem,
        val bobberDistance: Double,
        val name: String = entity.entityItem.displayName,
    )

    private var lastBait: String? = null
    private var wasUsingBait = true

    private val baitEntities = TimeLimitedSet<Bait>(4.seconds)

    @HandleEvent
    fun onWorldChange() {
        lastBait = null
        wasUsingBait = true
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onBobber(event: FishingBobberInLiquidEvent) {
        if (KuudraApi.inKuudra) return
        DelayedRun.runDelayed(300.milliseconds) {
            checkBait()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onEntityEnterWorld(event: EntityEnterWorldEvent<EntityItem>) {
        if (KuudraApi.inKuudra || !FishingApi.isFishing()) return
        val bobberDistance = event.entity.distanceTo(FishingApi.bobber?.getLorenzVec() ?: return)
        if (bobberDistance > 2) return
        DelayedRun.runNextTick {
            if (event.entity.entityItem.isBait()) {
                baitEntities += Bait(event.entity, bobberDistance)
            }
        }
    }

    private fun checkBait() {
        FishingApi.bobber ?: return
        // If user has no bait, but another player's bait spawns really close, it will be wrong.
        val bait = baitEntities.filter { it.bobberDistance < 2 }.minByOrNull { it.bobberDistance }?.name
        baitEntities.clear()

        if (bait == null) {
            if (config.noBaitWarning && !wasUsingBait) {
                showNoBaitWarning()
            }
        } else if (config.baitChangeWarning) {
            lastBait?.let {
                if (it != bait) {
                    showBaitChangeWarning(it, bait)
                }
            }
        }

        wasUsingBait = bait != null
        lastBait = bait
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
