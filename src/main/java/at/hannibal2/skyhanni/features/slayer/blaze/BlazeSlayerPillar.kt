package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.text.DecimalFormat
import java.util.regex.Pattern

class BlazeSlayerPillar {

    private var patternPillarExploded = Pattern.compile("§cYou took §r§f(.+) §r§ctrue damage from an exploding fire pillar!")
    private var lastPillarSpawnTime = -1L
    private val pillarEntities = mutableListOf<EntityArmorStand>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        for (armorStand in Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>()) {
            val name = armorStand.name
            if (name.matchRegex("§6§l.s §c§l8 hits")) {
                if (armorStand !in pillarEntities) {
                    pillarEntities.add(armorStand)
                    lastPillarSpawnTime = System.currentTimeMillis()
                }
            }
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (!isEnabled()) return

        val message = event.message
        val matcher = patternPillarExploded.matcher(message)
        if (matcher.matches()) {
            lastPillarSpawnTime = -1L
        }
        if (message == "  §r§a§lSLAYER QUEST COMPLETE!") {
            lastPillarSpawnTime = -1L
        }

        if (message == "§eYour Slayer boss was despawned, but you have kept your quest progress!") {
            lastPillarSpawnTime = -1L
        }
    }

    @SubscribeEvent
    fun renderOverlay(event: RenderGameOverlayEvent.Post) {
        if (!isEnabled()) return
        if (lastPillarSpawnTime == -1L) return

        val duration = System.currentTimeMillis() - lastPillarSpawnTime
        val maxDuration = 7_000

        val remainingLong = maxDuration - duration
        val remaining = (remainingLong.toFloat() / 1000)
        val format = DecimalFormat("0.0").format(remaining + 0.1)
        SkyHanniMod.feature.slayer.firePillarsPos.renderString("§cBlaze Pillar: §a${format}s")
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && SkyHanniMod.feature.slayer.firePillars && DamageIndicatorManager.isBossSpawned(
            BossType.SLAYER_BLAZE_2, BossType.SLAYER_BLAZE_3, BossType.SLAYER_BLAZE_4
        )
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        pillarEntities.clear()
    }
}