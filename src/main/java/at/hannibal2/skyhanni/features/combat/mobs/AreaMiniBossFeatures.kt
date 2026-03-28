package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ColorUtils.toChromaColor
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawDynamicText
import net.minecraft.world.phys.Vec3
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object AreaMiniBossFeatures {

    private val config get() = SkyHanniMod.feature.combat.mobs
    private var lastSpawnTime = SimpleTimeMark.farPast()
    private var miniBossType: AreaMiniBossType? = null
    private var respawnCooldown = 11.seconds
    val currentMobs = mutableSetOf<Mob>()

    @HandleEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val type = AreaMiniBossType.entries.find { it.displayName == event.mob.name } ?: return
        miniBossType = type
        val time = SimpleTimeMark.now()
        val diff = time - lastSpawnTime
        if (diff in 5.seconds..20.seconds) {
            respawnCooldown = diff
        }
        lastSpawnTime = time
        if (config.areaBossHighlight) {
            event.mob.highlight(type.color.addOpacity(type.colorTransparency).toChromaColor())
        }
        currentMobs.add(event.mob)
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        currentMobs.remove(event.mob)
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!SlayerApi.isInAnyArea) return
        if (!config.areaBossRespawnTimer) return

        val miniBoss = miniBossType ?: return

        val time = miniBoss.getTime()
        miniBoss.spawnLocations.filter { it.distanceToPlayer() < 15 }
            .forEach { event.drawDynamicText(it, time, 1.2, seeThroughBlocks = false) }
    }

    private fun AreaMiniBossType.getTime(): String {
        val spawnedSince = lastSpawnTime.passedSince()
        if (respawnCooldown <= spawnedSince) return "§c?"

        val estimatedTime = respawnCooldown - spawnedSince
        val format = estimatedTime.format(showMilliSeconds = true)
        return color.getChatColor() + format
    }

    @HandleEvent
    fun onWorldChange() {
        miniBossType = null
    }

    // TODO move to repo
    private enum class AreaMiniBossType(
        val displayName: String,
        val color: LorenzColor,
        val colorTransparency: Int,
        vararg val spawnLocations: Vec3,
    ) {
        GOLDEN_GHOUL(
            "Golden Ghoul", LorenzColor.YELLOW, 127,
            Vec3(-99.0, 39.0, -86.0),
            Vec3(-128.0, 42.0, -138.0),
        ),
        OLD_WOLF(
            "Old Wolf", LorenzColor.GOLD, 60,
            Vec3(-248.0, 123.0, 54.0),
            Vec3(-256.0, 105.0, 75.0),
            Vec3(-268.0, 90.0, 97.0),
            Vec3(-258.0, 94.0, 75.0),
            Vec3(-225.0, 92.0, 127.0),
        ),
        SOUL_OF_THE_ALPHA(
            "Soul of the Alpha", LorenzColor.GOLD, 60,
            Vec3(-381.0, 56.0, -94.0),
            Vec3(-394.0, 63.0, -52.0),
            Vec3(-386.0, 50.0, -2.0),
            Vec3(-396.0, 58.0, 29.0),
        ),
        VOIDLING_EXTREMIST(
            "Voidling Extremist", LorenzColor.LIGHT_PURPLE, 127,
            Vec3(-591.0, 10.0, -304.0),
            Vec3(-544.0, 21.0, -301.0),
            Vec3(-593.0, 26.0, -328.0),
            Vec3(-565.0, 41.0, -307.0),
            Vec3(-573.0, 51.0, -353.0),
        ),
        MILLENNIA_AGED_BLAZE(
            "Millennia-Aged Blaze", LorenzColor.DARK_RED, 60,
            Vec3(-292.0, 97.0, -999.0),
            Vec3(-232.0, 77.0, -951.0),
            Vec3(-304.0, 73.0, -952.0),
            Vec3(-281.0, 82.0, -1010.0),
            Vec3(-342.0, 86.0, -1035.0),
        ),
    }
}
