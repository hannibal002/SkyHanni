package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.entity.EntityTransparencyActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.world.entity.LivingEntity

@SkyHanniModule
object HideIrrelevantMobsInSlayerArea {

    private val config get() = SlayerApi.config

    private var irrelevantMob: IrrelevantMob? = null

    @HandleEvent
    fun onEntityTransparencyActive(event: EntityTransparencyActiveEvent) {
        irrelevantMob = if (isActive() && config.hideIrrelevantMobsTransparency < 100) {
            IrrelevantMob.entries.find { it.isInArea() }
        } else null
        irrelevantMob?.let {
            event.setActive()
        }
    }

    @HandleEvent
    fun onEntityTransparencyTick(event: EntityTransparencyTickEvent<LivingEntity>) {
        val irrelevantMob = irrelevantMob ?: return
        if (event.entity.mob?.name in irrelevantMob.mobNames) {
            event.newTransparency = config.hideIrrelevantMobsTransparency
        }
    }

    enum class IrrelevantMob(val mobNames: Set<String>, val isInArea: () -> Boolean) {
        CRIMSON_MAGMA_CUBE(
            mobNames = setOf("Magma Cube", "Magma Cube Rider"),
            isInArea = { IslandType.CRIMSON_ISLE.isInIsland() && SkyBlockUtils.graphArea == "Burning Desert" },
        ),
    }

    private fun isActive() = SlayerApi.isInCorrectArea && config.hideIrrelevantMobs
}
