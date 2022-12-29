package at.hannibal2.skyhanni.features.end

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.hasMaxHealth
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.monster.EntityEnderman
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class VoidlingExtremistColor {

    private var tick = 0
    private val extremists = mutableListOf<EntityEnderman>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 60 == 0) {
            Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityEnderman>()
                .filter { it !in extremists && it.hasMaxHealth(8_000_000) }.forEach { extremists.add(it) }
        }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in extremists) {
            event.color = LorenzColor.LIGHT_PURPLE.toColor().withAlpha(127)
        }
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtEvent) {
        if (!isEnabled()) return
        val entity = event.entity

        if (entity in extremists) {
            event.shouldReset = true
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        extremists.clear()
    }

    private fun isEnabled(): Boolean =
        LorenzUtils.inSkyblock && LorenzUtils.skyBlockIsland == "The End" && SkyHanniMod.feature.misc.voidlingExtremistColor
}