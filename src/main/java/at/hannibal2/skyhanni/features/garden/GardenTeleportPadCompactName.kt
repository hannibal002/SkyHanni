package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenTeleportPadCompactName {
    private val patternName = "§.✦ §aWarp To (.*)".toPattern()
    private val patternNoName = "§.✦ §cNo Destination".toPattern()

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLivingB(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!SkyHanniMod.feature.garden.teleportPadsCompactName) return
        val entity = event.entity
        if (entity !is EntityArmorStand) return

        val name = entity.name

        if (patternNoName.matcher(name).matches()) {
            event.isCanceled = true
        }

        val matcher = patternName.matcher(name)
        if (matcher.matches()) {
            entity.customNameTag = matcher.group(1)
        }
    }
}