package at.hannibal2.hanni.features.nether.ashfang

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.entity.EntityEnterWorldEvent
import at.hannibal2.hanni.events.entity.EntityLeaveWorldEvent
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ColorUtils
import at.hannibal2.hanni.utils.ColorUtils.getExtendedColorCode
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.DelayedRun
import at.hannibal2.hanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzColor
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawCylinderInWorld
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import at.hannibal2.hanni.utils.render.WorldRenderUtils.exactLocation
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object AshfangHighlights {

    private val config get() = AshfangManager.config

    private val BLAZING_SOUL by lazy { SkullTextureHolder.getTexture("ASHFANG_BLAZING_SOUL") }
    private val GRAVITY_ORB by lazy { SkullTextureHolder.getTexture("ASHFANG_GRAVITY_ORB") }
    private val blazingSouls = mutableSetOf<EntityArmorStand>()
    private val gravityOrbs = mutableSetOf<EntityArmorStand>()
    private const val MAX_DISTANCE = 15.0

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onEntityJoin(event: EntityEnterWorldEvent<EntityArmorStand>) {
        if (!AshfangManager.active) return
        val entity = event.entity
        DelayedRun.runNextTick {
            when {
                entity.wearingSkullTexture(BLAZING_SOUL) -> blazingSouls += entity
                entity.wearingSkullTexture(GRAVITY_ORB) -> gravityOrbs += entity
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onEntityLeave(event: EntityLeaveWorldEvent<EntityArmorStand>) {
        blazingSouls -= event.entity
        gravityOrbs -= event.entity
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!AshfangManager.active) return

        if (config.blazingSouls.enabled) {
            val color = config.blazingSouls.color.toColor()
            blazingSouls.forEach {
                val location = event.exactLocation(it)
                event.drawWaypointFilled(location.add(-0.5, 1.25, -0.5), color, extraSize = -0.15)
                event.drawBlendedColorString(location, "Blazing Soul")
            }
        }

        if (config.gravityOrbs.enabled) {
            val color = config.gravityOrbs.color.toColor()
            gravityOrbs.forEach {
                val location = event.exactLocation(it)
                event.drawCylinderInWorld(color, location.add(-0.5, -2.0, -0.5), 3.5f, 4.5f)
                event.drawBlendedColorString(location, "Gravity Orb")
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        blazingSouls.clear()
        gravityOrbs.clear()
    }

    private fun HanniRenderWorldEvent.drawBlendedColorString(location: LorenzVec, text: String) {
        val distance = location.distanceToPlayer()
        if (distance < MAX_DISTANCE) {
            val colorCode = getColorCode(distance)
            drawString(location.add(y = 2.5), colorCode + text)
        }
    }

    private fun getColorCode(distance: Double): String =
        ColorUtils.blendRGB(LorenzColor.GREEN.toColor(), LorenzColor.RED.toColor(), distance / MAX_DISTANCE).getExtendedColorCode()

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "ashfang.blazingSouls", "crimsonIsle.ashfang.blazingSouls.enabled")
        event.move(2, "ashfang.blazingSoulsColor", "crimsonIsle.ashfang.blazingSouls.color")

        event.move(1, "ashfang.gravityOrbs", "ashfang.gravityOrbs.enabled")
        event.move(1, "ashfang.gravityOrbsColor", "ashfang.gravityOrbs.color")
        event.move(2, "ashfang.gravityOrbs", "crimsonIsle.ashfang.gravityOrbs")
    }
}
