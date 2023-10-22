package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FishingHookDisplay {
    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay
    private var bobber: EntityFishHook? = null
    private var armorStand: EntityArmorStand? = null
    private val potentionArmorStands = mutableListOf<EntityArmorStand>()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        bobber = null
        armorStand = null
        potentionArmorStands.clear()
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return

        if (armorStand == null) {
            val filter = potentionArmorStands.filter { it.hasCustomName() && it.hasCorrectName() }
            if (filter.size == 1) {
                armorStand = filter[0]
            }
        }

        if (event.isMod(5)) {
            val entities = EntityUtils.getEntities<EntityFishHook>()
            val foundBobber = entities.firstOrNull { it.angler is EntityPlayerSP }
            if (foundBobber != bobber) {
                bobber = foundBobber
                reset()
            }
        }

    }

    private fun reset() {
        potentionArmorStands.clear()
        armorStand = null
    }

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!isEnabled()) return
        val entity = event.entity ?: return
        if (entity !is EntityArmorStand) return

        potentionArmorStands.add(entity)
    }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!isEnabled()) return
        if (!config.hideArmorStand) return

        if (event.entity == armorStand) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!isEnabled()) return

        val armorStand = armorStand ?: return
        if (armorStand.isDead) {
            reset()
            return
        }
        if (!armorStand.hasCustomName()) return

        config.position.renderString(armorStand.name, posLabel = "Fishing Hook Display")
    }

    private fun EntityArmorStand.hasCorrectName(): Boolean {
        if (name == "§c§l!!!") {
            return true
        }
        if (name.startsWith("§e§l") && !name.contains("CLICK")) {
            return true
        }

        return false
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && FishingAPI.hasFishingRodInHand()
}
