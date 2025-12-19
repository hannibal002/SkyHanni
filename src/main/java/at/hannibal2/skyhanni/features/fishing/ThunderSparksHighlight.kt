package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.entity.EntityEquipmentChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ColorUtils.toColor
import at.hannibal2.skyhanni.utils.EntityUtils.getWornSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.compat.deceased
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawWaypointFilled
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object ThunderSparksHighlight {

    private val config get() = SkyHanniMod.feature.fishing.thunderSpark
    private val THUNDER_SPARK_TEXTURE by lazy { SkullTextureHolder.getTexture("THUNDER_SPARK") }
    private val sparks = mutableSetOf<ArmorStand>()

    @HandleEvent
    fun onEntityEquipmentChange(event: EntityEquipmentChangeEvent<ArmorStand>) {
        if (!isEnabled()) return
        val entity = event.entity
        if (entity.getWornSkullTexture() == THUNDER_SPARK_TEXTURE) sparks.add(entity)
    }

    @HandleEvent
    fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        sparks.remove(event.entity)
    }

    @HandleEvent
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        val color = config.color.toColor()

        for (spark in sparks) {
            if (spark.deceased) continue
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
