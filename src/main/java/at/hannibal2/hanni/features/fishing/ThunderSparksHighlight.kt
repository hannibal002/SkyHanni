package at.hannibal2.hanni.features.fishing

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.BlockUtils.getBlockAt
import at.hannibal2.hanni.utils.ColorUtils.toColor
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.hasSkullTexture
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.entity.item.EntityArmorStand

@HanniModule
object ThunderSparksHighlight {

    private val config get() = HanniMod.feature.fishing.thunderSpark
    private val THUNDER_SPARK_TEXTURE by lazy { SkullTextureHolder.getTexture("THUNDER_SPARK") }
    private val sparks = mutableListOf<EntityArmorStand>()

    @HandleEvent
    fun onTick() {
        if (!isEnabled()) return

        EntityUtils.getEntities<EntityArmorStand>().filter {
            it !in sparks && it.hasSkullTexture(THUNDER_SPARK_TEXTURE)
        }.forEach { sparks.add(it) }
    }

    @HandleEvent
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!isEnabled()) return

        val color = config.color.toColor()

        for (spark in sparks) {
            if (spark.isDead) continue
            val sparkLocation = spark.getLorenzVec()
            val block = sparkLocation.getBlockAt()
            val seeThroughBlocks = sparkLocation.distanceToPlayer() < 6 && (block in FishingApi.lavaBlocks)
            event.drawWaypointFilled(
                sparkLocation.add(-0.5, 0.0, -0.5), color, extraSize = -0.25, seeThroughBlocks = seeThroughBlocks,
            )
            if (sparkLocation.distanceToPlayer() < 10) {
                event.drawString(sparkLocation.up(1.5), "Thunder Spark", seeThroughBlocks = seeThroughBlocks)
            }
        }
    }

    @HandleEvent
    fun onWorldChange() {
        sparks.clear()
    }

    private fun isEnabled() =
        (IslandType.CRIMSON_ISLE.isCurrent() || SkyBlockUtils.isStrandedProfile) && config.highlight

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "fishing.thunderSparkHighlight", "fishing.thunderSpark.highlight")
        event.move(3, "fishing.thunderSparkColor", "fishing.thunderSpark.color")
    }
}
