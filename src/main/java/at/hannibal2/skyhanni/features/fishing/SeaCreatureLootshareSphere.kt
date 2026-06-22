package at.hannibal2.skyhanni.features.fishing

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.fishing.SeaCreatureEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.fishing.seaCreatureXMLGui.SeaCreatureSettings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.LootshareUtils
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawSphereWireframeInWorld

@SkyHanniModule
object SeaCreatureLootshareSphere {
    private val config get() = SkyHanniMod.feature.fishing

    private val seaCreatures = mutableListOf<LivingSeaCreatureData>()

    private val spherePositions = mutableListOf<LootshareSphere>()

    data class LootshareSphere(
        val position: LorenzVec? = null,
        var color: LorenzColor = LorenzColor.WHITE,
    )

    @HandleEvent
    fun onSeaCreatureSpawn(event: SeaCreatureEvent.Spawn) = addMob(event.seaCreature)

    @HandleEvent
    fun onSeaCreatureRemove(event: SeaCreatureEvent.Remove) = seaCreatures.remove(event.seaCreature)

    @HandleEvent(onlyOnSkyblock = true)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.lootshareRange) return
        scLoop@ for (lootshareSphere in spherePositions) {
            if (lootshareSphere.position == null) return
            event.drawSphereWireframeInWorld(lootshareSphere.color.toColor(), lootshareSphere.position, LootshareUtils.RANGE)
            spherePositions.remove(lootshareSphere)
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTick() {
        if (!config.lootshareRange) return
        val seaCreaturesToRender = mutableSetOf<LorenzVec>()
        for (seaCreature in seaCreatures) {
            if (!seaCreature.exists()) continue
            var otherNearbySpheres = 0
            val pos = seaCreature.pos ?: continue
            val color = if (seaCreature.isOwn || LootshareUtils.isInRange(pos)) LorenzColor.GREEN else LorenzColor.WHITE
            seaCreaturesToRender.forEach {
                if (it.distance(pos) < 10) otherNearbySpheres++
                if (otherNearbySpheres < 2) spherePositions.add(LootshareSphere(pos, color))
            }
        }
    }

    private fun addMob(seaCreature: LivingSeaCreatureData) {
        if (SeaCreatureSettings.getConfig(seaCreature)?.shouldRenderLootshare == true) seaCreatures.add(seaCreature)
    }
}
