package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.FishingBobberCastEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class FishingHookDisplay {

    private val config get() = SkyHanniMod.feature.fishing.fishingHookDisplay
    private var armorStand: EntityArmorStand? = null
    private val potentionArmorStands = mutableListOf<EntityArmorStand>()
    private val pattern = "§e§l(\\d+(\\.\\d+)?)".toPattern()

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        reset()
    }

    @SubscribeEvent
    fun onBobberThrow(event: FishingBobberCastEvent) {
        reset()
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
    }

    private fun reset() {
        potentionArmorStands.clear()
        armorStand = null
    }

    @SubscribeEvent
    fun onJoinWorld(event: EntityEnterWorldEvent) {
        if (!isEnabled()) return
        val entity = event.entity
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
        val alertText = if (armorStand.name == "§c§l!!!") config.customAlertText.replace("&", "§") else armorStand.name

        config.position.renderString(alertText, posLabel = "Fishing Hook Display")
    }

    private fun EntityArmorStand.hasCorrectName(): Boolean {
        if (name == "§c§l!!!") {
            return true
        }
        return pattern.matcher(name).matches()
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled && FishingAPI.holdingRod
}
