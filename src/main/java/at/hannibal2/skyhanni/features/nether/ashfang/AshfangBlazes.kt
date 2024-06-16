package at.hannibal2.skyhanni.features.nether.ashfang

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.mob.Mob
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.SkyHanniRenderEntityEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.MobUtils.mob
import net.minecraft.entity.item.EntityArmorStand
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object AshfangBlazes {

    private val config get() = SkyHanniMod.feature.crimsonIsle.ashfang

    private val ashfangMobs = mutableSetOf<Mob>()
    var ashfang: Mob? = null

    fun isAshfangActive() = ashfang != null

    @SubscribeEvent
    fun onMobSpawn(event: MobEvent.Spawn.SkyblockMob) {
        if (!isEnabled()) return
        val mob = event.mob
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
        if (config.highlightBlazes) mob.highlight(color.toColor())
    }

    @SubscribeEvent
    fun onMobDeSpawn(event: MobEvent.DeSpawn.SkyblockMob) {
        val mob = event.mob
        ashfangMobs -= mob
        if (ashfang == mob) ashfang = null
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: SkyHanniRenderEntityEvent.Specials.Pre<EntityArmorStand>) {
        if (!isEnabled()) return
        if (!config.hide.fullNames) return
        if (event.entity.mob !in ashfangMobs) return
        event.cancel()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.nextResetCooldown", "crimsonIsle.ashfang.nextResetCooldown")
        event.move(2, "ashfang.highlightBlazes", "crimsonIsle.ashfang.highlightBlazes")
        event.move(2, "ashfang.hideNames", "crimsonIsle.ashfang.hide.fullNames")
    }

    private fun isEnabled() = IslandType.CRIMSON_ISLE.isInIsland()
}
