package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.monster.EntityBlaze
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class MilleniaAgedBlazeColor {

    private var tick = 0
    private val blazes = mutableListOf<EntityBlaze>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 60 == 0) {
            Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityBlaze>()
                .filter { it !in blazes && it.hasMaxHealth(30_000_000) }.forEach { blazes.add(it) }
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in blazes) {
            event.color = LorenzColor.DARK_RED.toColor().withAlpha(60)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in blazes) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        blazes.clear()
    }

    private fun isEnabled() =
        LorenzUtils.inSkyblock && LorenzUtils.skyBlockIsland == "Crimson Isle" && SkyHanniMod.feature.misc.milleniaAgedBlazeColor
}