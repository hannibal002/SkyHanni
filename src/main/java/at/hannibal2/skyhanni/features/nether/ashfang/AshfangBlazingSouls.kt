package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.GriffinUtils.drawWaypointFilled
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.SpecialColour
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.awt.Color

class AshfangBlazingSouls {

    private val texture = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODI4N2IzOTdkYWY5NTE2YTBiZDc2ZjVmMWI3YmY5Nzk1MTVkZjNkNWQ4MzNlMDYzNWZhNjhiMzdlZTA4MjIxMiJ9fX0="
    private val souls = mutableListOf<EntityArmorStand>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        Minecraft.getMinecraft().theWorld.loadedEntityList
            .filter { it ->
                it is EntityArmorStand && it !in souls && it.inventory
                    .any { it != null && it.getSkullTexture() == texture }
            }.forEach { souls.add(it as EntityArmorStand) }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!isEnabled()) return

        val special = SkyHanniMod.feature.ashfang.blazingSoulsColor

        val color = Color(SpecialColour.specialToChromaRGB(special), true)

        val playerLocation = LocationUtils.playerLocation()
        for (orb in souls) {
            if (orb.isDead) continue
            val orbLocation = orb.getLorenzVec()
            event.drawWaypointFilled(orbLocation.add(-0.5, 1.25, -0.5), color, extraSize = -0.15)
            if (orbLocation.distance(playerLocation) < 10) {
                //TODO find way to dynamically change color
                event.drawString(orbLocation.add(0.0, 2.5, 0.0), "§bBlazing Soul")
            }
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: WorldEvent.Load) {
        souls.clear()
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.ashfang.blazingSouls
    }
}