package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyActiveEvent
import at.hannibal2.skyhanni.events.entity.EntityTransparencyTickEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.MobUtils.mob
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand

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
        if (shouldHideEntity(event.entity)) event.newTransparency = config.hideIrrelevantMobsTransparency
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onCheckRender(event: CheckRenderEntityEvent<ArmorStand>) {
        if (config.hideIrrelevantMobsNametags && shouldHideEntity(event.entity)) event.cancel()
    }

    private fun shouldHideEntity(entity: LivingEntity): Boolean {
        val irrelevantMob = irrelevantMob ?: return false
        return entity.mob?.name in irrelevantMob.mobNames
    }

    enum class IrrelevantMob(val mobNames: Set<String>, val isInArea: () -> Boolean) {
        CRIMSON_MAGMA_CUBE(
            mobNames = setOf("Magma Cube", "Magma Cube Rider"),
            isInArea = { IslandType.CRIMSON_ISLE.isInIsland() && SkyBlockUtils.graphArea == "Burning Desert" },
        ),
    }

    private fun isActive() = SlayerApi.isInCorrectArea && config.hideIrrelevantMobs
}
