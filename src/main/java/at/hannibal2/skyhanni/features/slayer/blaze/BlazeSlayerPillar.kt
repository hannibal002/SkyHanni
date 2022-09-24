package at.hannibal2.skyhanni.features.slayer.blaze

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.features.damageindicator.BossType
import at.hannibal2.skyhanni.features.damageindicator.DamageIndicatorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.matchRegex
import at.hannibal2.skyhanni.utils.RenderUtils.renderString
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.SoundUtils.playSound
import at.hannibal2.skyhanni.utils.getLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.text.DecimalFormat
import java.util.regex.Pattern
import kotlin.math.roundToInt

class BlazeSlayerPillar {
    private var patternPillarExploded =
        Pattern.compile("§cYou took §r§f(.+) §r§ctrue damage from an exploding fire pillar!")
    private val pillarEntities = mutableListOf<EntityArmorStand>()
    private val pillarWarningTexture =
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWMyZTlkODM5NWNhY2Q5OTIyODY5YzE1MzczY2Y3Y2IxNmRhMGE1Y2U1ZjNjNjMyYjE5Y2ViMzkyOWM5YTExIn19fQ=="

    private var lastPillarSpawnTime = -1L
    private var lastSoundMoment = 0.0
    private var lastPillarBuildEntitiesFound = 0L
    private var pillarBuildEntityList = mutableListOf<EntityArmorStand>()

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return

        val list = mutableListOf<EntityArmorStand>()
        val playerLocation = LocationUtils.playerLocation()
        for (armorStand in Minecraft.getMinecraft().theWorld.loadedEntityList.filterIsInstance<EntityArmorStand>()) {
            val name = armorStand.name
            if (name.matchRegex("§6§l.s §c§l8 hits")) {
                if (armorStand !in pillarEntities) {
                    pillarEntities.add(armorStand)

                    val lastPillarEntities = System.currentTimeMillis() - lastPillarBuildEntitiesFound
                    if (lastPillarEntities in 2500..4500) {
                        lastPillarSpawnTime = System.currentTimeMillis()
                    }
                }
            }

            if (armorStand in pillarBuildEntityList || armorStand.inventory.any { it != null && it.getSkullTexture() == pillarWarningTexture }) {
                if (armorStand !in pillarBuildEntityList) {
                    pillarBuildEntityList.add(armorStand)
                    if (SkyHanniMod.feature.slayer.firePillarBuildHider) {
                        armorStand.inventory[0] = null
                        armorStand.inventory[1] = null
                        armorStand.inventory[2] = null
                        armorStand.inventory[3] = null
                    }
                }
                if (armorStand.getLorenzVec().distance(playerLocation) < 15) {
                    list.add(armorStand)
                }
            }
        }
        val size = list.size
        if (size == 0) return
        if (size % 12 == 0) {
            if (System.currentTimeMillis() > lastPillarBuildEntitiesFound + 10_000) {
                lastPillarBuildEntitiesFound = System.currentTimeMillis()
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
            SoundUtils.createSound("note.pling", 0.7f).playSound()
        }

        when (message) {
            "  §r§a§lSLAYER QUEST COMPLETE!",
            "  §r§c§lSLAYER QUEST FAILED!",
            "§eYour Slayer boss was despawned, but you have kept your quest progress!",
            -> lastPillarSpawnTime = -1L
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
        if (SkyHanniMod.feature.slayer.firePillarSound) {
            playSound(remaining)
        }

        if (SkyHanniMod.feature.slayer.firePillarDisplay) {
            val format = DecimalFormat("0.0").format(remaining + 0.1)
            SkyHanniMod.feature.slayer.firePillarPos.renderString("§cBlaze Pillar: §a${format}s")
        }
    }

    private fun playSound(remaining: Float) {
        val time = (remaining * 10).roundToInt().toDouble() / 10
        if (time == lastSoundMoment) return
        lastSoundMoment = time

        val playSound = if (time < 0) {
            false
        } else if (time <= 0.7) {
            true
        } else if (time <= 2.1) {
            when (time) {
                0.9 -> true
                1.2 -> true
                1.5 -> true
                1.8 -> true
                2.1 -> true
                else -> false
            }
        } else if (time <= 4 && time % 0.5 == 0.0) {
            true
        } else time % 1.0 == 0.0

        if (playSound) {
            SoundUtils.createSound("random.click", 1.3f).playSound()
        }
    }

    private fun isEnabled(): Boolean {
        return LorenzUtils.inSkyblock && DamageIndicatorManager.isBossSpawned(
            BossType.SLAYER_BLAZE_2,
            BossType.SLAYER_BLAZE_3,
            BossType.SLAYER_BLAZE_4,
            BossType.SLAYER_BLAZE_QUAZII_2,
            BossType.SLAYER_BLAZE_QUAZII_34,
            BossType.SLAYER_BLAZE_TYPHOEUS_2,
            BossType.SLAYER_BLAZE_TYPHOEUS_34,
        )
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        pillarEntities.clear()
        lastPillarSpawnTime = -1
    }

    @SubscribeEvent
    fun onSoundEvent(event: PlaySoundEvent) {
        if (!isEnabled()) return
        if (!SkyHanniMod.feature.slayer.firePillarBuildHider) return

        when (event.soundName) {
            "mob.chicken.plop",
            "mob.bat.takeoff",
            -> {
                event.isCanceled = true
            }
        }
    }
}