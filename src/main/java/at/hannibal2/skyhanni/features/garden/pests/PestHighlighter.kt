package at.hannibal2.skyhanni.features.garden.pests

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.mob.MobData
import at.hannibal2.skyhanni.events.RenderEntityOutlineEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceTo
import at.hannibal2.skyhanni.utils.compat.EntityCompat.getEquipmentSlots
import at.hannibal2.skyhanni.utils.compat.InventoryCompat.orNull
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.decoration.ArmorStand
import java.awt.Color

@SkyHanniModule
object PestHighlighter {

    private val config get() = PestApi.config
    private val highlightColor = Color.RED

    @HandleEvent
    fun onRenderEntityOutlines(event: RenderEntityOutlineEvent) {
        if (!GardenApi.inGarden() || !config.pestHighlight) return
        if (event.type !== RenderEntityOutlineEvent.Type.XRAY) return

        val pestMobs = MobData.entityToMob.values
            .filter { PestType.getByNameOrNull(it.name) != null }
            .distinct()

        val pestAnchors = pestMobs.flatMap { it.fullEntityList() }
        if (pestAnchors.isEmpty()) return

        event.queueEntitiesToOutline { entity ->
            when {
                entity.isKnownPestEntity() -> highlightColor
                entity is ArmorStand &&
                    entity.hasSkullModel() &&
                    pestAnchors.any { anchor -> entity.distanceTo(anchor) <= 3.0 } -> highlightColor
                else -> null
            }
        }
    }

    private fun Entity.isKnownPestEntity(): Boolean =
        MobData.entityToMob[this]?.let { PestType.getByNameOrNull(it.name) != null } == true

    private fun ArmorStand.hasSkullModel(): Boolean = getEquipmentSlots().values.any {
        it?.orNull()?.getSkullTexture() != null
    }
}
