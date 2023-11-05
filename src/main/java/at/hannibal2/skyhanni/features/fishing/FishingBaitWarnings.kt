package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.fishing.FishingAPI.isBait
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class FishingBaitWarnings {
    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarnings
    private var bobber: EntityFishHook? = null
    private var lastBait: String? = null
    private var timeLastCast = SimpleTimeMark.farPast()
    private var isUsingBait: Boolean = false

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!isEnabled()) return
        val entity = event.entity ?: return
        if (entity !is EntityFishHook) return
        if (entity.angler != Minecraft.getMinecraft().thePlayer) return

        bobber = entity
        timeLastCast = SimpleTimeMark.now()
        isUsingBait = false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled() || bobber == null) return
        //Is there a way to get event sent time to be more accurate?
        if (timeLastCast.passedSince() < 1.seconds) return

        if (!isUsingBait && config.noBaitWarning) showNoBaitWarning()
        reset()
    }

    fun reset() {
        bobber = null
        isUsingBait = false
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!isEnabled() || !config.baitChangeWarning) return
        val bobber = bobber ?: return
        for (entityItem in EntityUtils.getEntitiesNearby<EntityItem>(bobber.getLorenzVec(), 1.5)) {
            val itemStack = entityItem.entityItem
            if (!itemStack.isBait()) continue
            val name = itemStack.name?.removeColor() ?: continue

            isUsingBait = true
            lastBait?.let {
                if (name == it) continue
                showBaitChangeWarning(it, name)
            }
            lastBait = name
        }
    }

    private fun showBaitChangeWarning(before: String, after: String) {
        SoundUtils.playClickSound()
        LorenzUtils.sendTitle("§eBait changed!", 2.seconds)
        LorenzUtils.chat("§e[SkyHanni] Fishing Bait change detected: $before -> $after")
    }

    private fun showNoBaitWarning() {
        SoundUtils.playErrorSound()
        LorenzUtils.sendTitle("§cNo bait is used!", 2.seconds)
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && FishingAPI.hasFishingRodInHand()
}
