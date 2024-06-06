package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.FishingBobberInWaterEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.features.fishing.FishingAPI.isBait
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object FishingBaitWarnings {

    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarnings

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
    }

    private var lastBait: String? = null
    private var wasUsingBait = true

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        lastBait = null
        wasUsingBait = true
    }

    @SubscribeEvent
    fun onBobberInWater(event: FishingBobberInWaterEvent) {
        DelayedRun.runDelayed(500.milliseconds) {
            checkBobber()
        }
    }

    private fun checkBobber() {
        val bobber = FishingAPI.bobber ?: return
        val bait = detectBait(bobber)
        if (bait == null) {
            if (config.noBaitWarning) {
                if (!wasUsingBait) {
                    showNoBaitWarning()
                }
            }
        } else {
            if (config.baitChangeWarning) {
                lastBait?.let {
                    if (it != bait) {
                        showBaitChangeWarning(it, bait)
                    }
                }
            }
        }
        wasUsingBait = bait != null
        lastBait = bait
    }

    private fun detectBait(bobber: EntityFishHook): String? {
        for (entity in EntityUtils.getEntitiesNearby<EntityItem>(bobber.getLorenzVec(), 6.0)) {
            val itemStack = entity.entityItem ?: continue
            if (!itemStack.isBait()) continue
            val ticksExisted = entity.ticksExisted
            if (ticksExisted in 6..15) {
                return itemStack.name
            }

            val distance = "distance: ${entity.getDistanceToEntity(bobber).addSeparators()}"
            ChatUtils.debug("fishing bait: ticksExisted: $ticksExisted, $distance")
        }
        return null
    }

    private fun showBaitChangeWarning(before: String, after: String) {
        SoundUtils.playClickSound()
        LorenzUtils.sendTitle("§eBait changed!", 2.seconds)
        ChatUtils.chat("Fishing Bait changed: $before §e-> $after")
    }

    private fun showNoBaitWarning() {
        SoundUtils.playErrorSound()
        LorenzUtils.sendTitle("§cNo bait is used!", 2.seconds)
        ChatUtils.chat("You're not using any fishing baits!")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && FishingAPI.isFishing() && !LorenzUtils.inKuudraFight
}
