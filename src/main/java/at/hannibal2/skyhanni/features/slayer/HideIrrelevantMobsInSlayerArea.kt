package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.events.entity.EntityOpacityActiveEvent
import at.hannibal2.hanni.events.entity.EntityOpacityEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.MobUtils.mob
import at.hannibal2.hanni.utils.SkyBlockUtils
import net.minecraft.entity.EntityLivingBase

@HanniModule
object HideIrrelevantMobsInSlayerArea {

    private val config get() = SlayerApi.config

    private var irrelevantMob: IrrelevantMob? = null

    @HandleEvent
    fun onEntityOpacityActive(event: EntityOpacityActiveEvent) {
        irrelevantMob = if (isActive() && config.hideIrrelevantMobsOpacity < 100) {
            IrrelevantMob.entries.find { it.isInArea() }
        } else null
        irrelevantMob?.let {
            event.setActive()
        }
    }

    @HandleEvent
    fun onEntityOpacity(event: EntityOpacityEvent<EntityLivingBase>) {
        val irrelevantMob = irrelevantMob ?: return
        if (event.entity.mob?.name in irrelevantMob.mobNames) {
            event.opacity = config.hideIrrelevantMobsOpacity
        }
    }

    enum class IrrelevantMob(val mobNames: Set<String>, val isInArea: () -> Boolean) {
        CRIMSON_MAGMA_CUBE(
            mobNames = setOf("Magma Cube", "Magma Cube Rider"),
            isInArea = { IslandType.CRIMSON_ISLE.isCurrent() && SkyBlockUtils.graphArea == "Burning Desert" },
        ),
    }

    private fun isActive() = SlayerApi.isInCorrectArea && config.hideIrrelevantMobs
}
