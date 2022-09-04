package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.RenderMobColoredEvent
import at.hannibal2.skyhanni.events.ResetEntityHurtTimeEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.utils.EntityUtils.getAllNameTagsWith
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntityBlaze
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class AshfangHighlightBlazes {

    private val blazes = mutableMapOf<EntityBlaze, LorenzColor>()

    var nearAshfang = false
    var tick = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        if (tick++ % 20 == 0) {
            checkNearAshfang()
        } else {
            return
        }

        if (nearAshfang) {
            for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityBlaze>()
                .filter { it !in blazes.keys }) {
                val list = entity.getAllNameTagsWith(2, "Ashfang")
                if (list.size == 1) {
                    val armorStand = list[0]
                    if (armorStand.name.contains("Ashfang Follower")) {
                        blazes[entity] = LorenzColor.DARK_GRAY
                    } else if (armorStand.name.contains("Ashfang Underling")) {
                        blazes[entity] = LorenzColor.RED
                    } else if (armorStand.name.contains("Ashfang Acolyte")) {
                        blazes[entity] = LorenzColor.BLUE
                    }
                } else if (list.size > 1) {
                    println("found " + list.size + " name tags")
                }
            }
        }

    }

    private fun checkNearAshfang() {
        nearAshfang = Minecraft.getMinecraft().theWorld.loadedEntityList
            .any { it is EntityArmorStand && it.name.contains("Ashfang") }
    }

    @SubscribeEvent
    fun onRenderMobColored(event: RenderMobColoredEvent) {
        if (!isEnabled()) return
        val entity = event.entity
        event.color = blazes[entity]?.toColor()?.withAlpha(40) ?: 0
    }

    @SubscribeEvent
    fun onResetEntityHurtTime(event: ResetEntityHurtTimeEvent) {
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

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.highlightBlazes
    }
}