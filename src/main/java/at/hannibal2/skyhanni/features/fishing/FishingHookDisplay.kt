package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityXPOrb
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FishingHookDisplay {
    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay
    private var bobberLocation: LorenzVec? = null
    private var armorStand: EntityArmorStand? = null
    private var display = ""

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        display = ""
        bobberLocation = null
        armorStand = null
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!isEnabled()) return
        if (!event.isMod(config.debugUpdateInterval)) return

        val entities = EntityUtils.getEntities<EntityFishHook>()
        bobberLocation = entities.firstOrNull { it.angler is EntityPlayerSP }?.getLorenzVec()
    }

    @SubscribeEvent
    fun onJoinWorld(event: EntityJoinWorldEvent) {
        if (!isEnabled()) return
        val entity = event.entity ?: return
        if (entity is EntityXPOrb) return
        val bobberLocation = bobberLocation ?: return

        val distance = entity.getLorenzVec().distance(bobberLocation)
        if (distance > config.debugMaxDistance) return
        if (entity is EntityArmorStand) {
            armorStand = entity
        }
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
        if (armorStand.isDead) return
        if (!armorStand.hasCustomName()) return

        config.position.renderString(armorStand.name, posLabel = "Fishing Hook Display")
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && FishingAPI.hasFishingRodInHand()
}
