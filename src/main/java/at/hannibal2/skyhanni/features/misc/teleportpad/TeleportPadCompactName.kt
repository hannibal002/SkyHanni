package at.hannibal2.skyhanni.features.misc.teleportpad

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class TeleportPadCompactName {
    // TODO USE SH-REPO
    private val patternName = "§.✦ §aWarp To (?<name>.*)".toPattern()
    private val patternNoName = "§.✦ §cNo Destination".toPattern()

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLivingB(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!SkyHanniMod.feature.misc.teleportPad.compactName) return
        if (LorenzUtils.skyBlockIsland != IslandType.PRIVATE_ISLAND) return
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