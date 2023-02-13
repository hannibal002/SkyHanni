package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.TimeUnit
import at.hannibal2.skyhanni.utils.TimeUtils
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

class AshfangNextResetCooldown {

    private var spawnTime = 1L

    @SubscribeEvent
    fun renderOverlay(event: ClientTickEvent) {
        if (!isEnabled()) return

        if (Minecraft.getMinecraft().theWorld.loadedEntityList.any {
                it is EntityArmorStand && it.posY > 145 &&
                        (it.name.contains("§c§9Ashfang Acolyte§r") || it.name.contains("§c§cAshfang Underling§r"))
            }) {
            spawnTime = System.currentTimeMillis()
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!isEnabled()) return
        if (spawnTime == -1L) return

        val remainingTime = spawnTime + 46_100 - System.currentTimeMillis()
        if (remainingTime > 0) {
            val format = TimeUtils.formatDuration(remainingTime, TimeUnit.SECOND, showMilliSeconds = true)
            SkyHanniMod.feature.ashfang.nextResetCooldownPos.renderString("§cAshfang next reset in: §a$format")
        } else {
            spawnTime = -1
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        spawnTime = -1
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyBlock && SkyHanniMod.feature.ashfang.nextResetCooldown &&
                DamageIndicatorManager.isBossSpawned(BossType.NETHER_ASHFANG)
    }
}