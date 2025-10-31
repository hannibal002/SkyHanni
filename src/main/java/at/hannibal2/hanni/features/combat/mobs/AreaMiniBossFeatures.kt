package at.hannibal2.hanni.features.combat.mobs

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.toChromaColor
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.TimeUtils.format
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawDynamicText
import kotlin.time.Duration.Companion.seconds

@HanniModule
object AreaMiniBossFeatures {

    private val config get() = HanniMod.feature.combat.mobs
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
            event.mob.highlight(type.color.addOpacity(type.colorOpacity).toChromaColor())
        }
        currentMobs.add(event.mob)
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        currentMobs.remove(event.mob)
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
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
        val colorOpacity: Int,
        vararg val spawnLocations: LorenzVec,
    ) {
        GOLDEN_GHOUL(
            "Golden Ghoul", LorenzColor.YELLOW, 127,
            LorenzVec(-99, 39, -86),
            LorenzVec(-128, 42, -138),
        ),
        OLD_WOLF(
            "Old Wolf", LorenzColor.GOLD, 60,
            LorenzVec(-248, 123, 54),
            LorenzVec(-256, 105, 75),
            LorenzVec(-268, 90, 97),
            LorenzVec(-258, 94, 75),
            LorenzVec(-225, 92, 127),
        ),
        SOUL_OF_THE_ALPHA(
            "Soul of the Alpha", LorenzColor.GOLD, 60,
            LorenzVec(-381, 56, -94),
            LorenzVec(-394, 63, -52),
            LorenzVec(-386, 50, -2),
            LorenzVec(-396, 58, 29),
        ),
        VOIDLING_EXTREMIST(
            "Voidling Extremist", LorenzColor.LIGHT_PURPLE, 127,
            LorenzVec(-591, 10, -304),
            LorenzVec(-544, 21, -301),
            LorenzVec(-593, 26, -328),
            LorenzVec(-565, 41, -307),
            LorenzVec(-573, 51, -353),
        ),
        MILLENNIA_AGED_BLAZE(
            "Millennia-Aged Blaze", LorenzColor.DARK_RED, 60,
            LorenzVec(-292, 97, -999),
            LorenzVec(-232, 77, -951),
            LorenzVec(-304, 73, -952),
            LorenzVec(-281, 82, -1010),
            LorenzVec(-342, 86, -1035),
        ),
    }
}
