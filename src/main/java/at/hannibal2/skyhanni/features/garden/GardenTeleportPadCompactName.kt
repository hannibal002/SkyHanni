package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class GardenTeleportPadCompactName {
    private val patternName = "§.✦ §aWarp To (?<name>.*)".toPattern()
    private val patternNoName = "§.✦ §cNo Destination".toPattern()

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLivingB(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!SkyHanniMod.feature.garden.teleportPadsCompactName) return
        val entity = event.entity
        if (entity !is EntityArmorStand) return

        val name = entity.name

        patternNoName.matchMatcher(name) {
            event.isCanceled = true
        }

        patternName.matchMatcher(name) {
            entity.customNameTag = group("name")
        }
    }
}