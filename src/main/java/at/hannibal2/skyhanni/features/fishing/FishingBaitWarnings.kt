package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.features.fishing.FishingAPI.isBait
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.seconds

class FishingBaitWarnings {
    private val config get() = SkyHanniMod.feature.fishing.fishingBaitWarnings
    private var bobber: EntityFishHook? = null
    private var lastBait: String? = null
    private var isUsingBait: Boolean = false

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        bobber = event.bobber
        isUsingBait = false
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        val bobber = bobber ?: return
        if (bobber.isDead) {
            this.bobber = null
            return
        }
        if (!event.isMod(5)) return
        if (FishingAPI.lastCastTime.passedSince() < 1.seconds) return

        val block = bobber.getLorenzVec().getBlockAt()
        if (block !in FishingAPI.getAllowedBlocks()) return

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
        EntityUtils.getEntitiesNearby<EntityItem>(bobber.getLorenzVec(), 1.5)
            .forEach { onBaitDetection(it.entityItem) }
    }

    private fun onBaitDetection(itemStack: ItemStack) {
        if (!itemStack.isBait()) return
        val name = itemStack.name?.removeColor() ?: return

        isUsingBait = true
        lastBait?.let {
            if (name == it) return
            showBaitChangeWarning(it, name)
        }
        lastBait = name
    }

    private fun showBaitChangeWarning(before: String, after: String) {
        SoundUtils.playClickSound()
        LorenzUtils.sendTitle("§eBait changed!", 2.seconds)
        LorenzUtils.chat("§e[SkyHanni] Fishing Bait changed: $before -> $after")
    }

    private fun showNoBaitWarning() {
        SoundUtils.playErrorSound()
        LorenzUtils.sendTitle("§cNo bait is used!", 2.seconds)
        LorenzUtils.chat("§e[SkyHanni] You do not use any fishing baits!")
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && FishingAPI.hasFishingRodInHand()
}
