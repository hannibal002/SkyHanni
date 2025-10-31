package at.hannibal2.hanni.features.nether.ashfang

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.config.features.crimsonisle.ashfang.AshfangConfig
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.mob.Mob
import at.hannibal2.hanni.events.MobEvent
import at.hannibal2.hanni.events.HanniRenderEntityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils.addAlpha
import at.hannibal2.hanni.utils.ColorUtils.toChromaColor
import at.hannibal2.hanni.utils.EntityUtils.isAtFullHealth
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.MobUtils.mob
import at.hannibal2.hanni.utils.SimpleTimeMark
import net.minecraft.entity.item.EntityArmorStand
import kotlin.time.Duration.Companion.seconds

@HanniModule
object AshfangManager {

    val config: AshfangConfig get() = HanniMod.feature.crimsonIsle.ashfang

    private val ashfangMobs = mutableSetOf<Mob>()
    var ashfang: Mob? = null
        private set
    var lastSpawnTime = SimpleTimeMark.farPast()
        private set

    val active get() = ashfang != null

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        val mob = event.mob
        // TODO add config optioinhs for colors
        val color = when (mob.name) {
            "Ashfang Follower" -> LorenzColor.DARK_GRAY
            "Ashfang Underling" -> LorenzColor.RED
            "Ashfang Acolyte" -> LorenzColor.BLUE
            "Ashfang" -> {
                ashfang = mob
                return
            }

            else -> return
        }
        ashfangMobs += mob
        if (config.highlightBlazes) mob.highlight(color.toColor().addAlpha(40).toChromaColor())
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onMobFirstSeen(event: MobEvent.FirstSeen.SkyblockMob) {
        if (!event.mob.name.contains("Ashfang ")) return
        if (lastSpawnTime.passedSince() < 10.seconds) return
        lastSpawnTime = SimpleTimeMark.now()
    }

    @HandleEvent
    fun onMobDespawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        ashfangMobs -= mob
        if (ashfang == mob) {
            ashfang = null
            if (mob.isInRender()) lastSpawnTime = SimpleTimeMark.farPast()
        }
    }

    @HandleEvent(priority = HandleEvent.HIGH)
    fun onRenderLiving(event: HanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!active || !config.hide.fullNames) return
        val mob = event.entity.mob ?: return
        if (mob !in ashfangMobs) return
        if (mob.baseEntity.isAtFullHealth()) event.cancel()
    }

    @HandleEvent
    fun onWorldChange() {
        lastSpawnTime = SimpleTimeMark.farPast()
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.highlightBlazes", "crimsonIsle.ashfang.highlightBlazes")
        event.move(2, "ashfang.hideNames", "crimsonIsle.ashfang.hide.fullNames")
    }
}
