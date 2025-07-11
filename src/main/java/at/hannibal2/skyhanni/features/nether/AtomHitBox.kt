package at.hannibal2.skyhanni.features.nether

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.removeIf
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.entity.item.EntityArmorStand
import java.awt.Color

@SkyHanniModule
object AtomHitBox {

    private val config get() = SkyHanniMod.feature.crimsonIsle.atomHitBox
    private val atomsList = mutableMapOf<EntityArmorStand, AtomType>()

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!config.enabled) return
        atomsList.removeIf { !it.key.isEntityAlive }
        for ((entity, atom) in atomsList) {
            if (entity.distanceToPlayer() > 50) continue
            event.drawHitbox(entity.entityBoundingBox, atom.color)
            event.drawString(entity.getLorenzVec() - LorenzVec(0, 1, 0), atom.displayName)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onTick() {
        if (!config.enabled) return

        for (entity in EntityUtils.getAllEntities().filterIsInstance<EntityArmorStand>()) {
            val atom = AtomType.entries.firstOrNull { entity.wearingSkullTexture(it.skullTexture) } ?: continue
            if (!atom.isSelected()) continue
            atomsList[entity] = atom
        }
    }

    @HandleEvent
    fun onWorldChange() {
        atomsList.clear()
    }

    enum class AtomType(
        val displayName: String,
        private val textureId: String,
        val color: Color,
    ) {
        EXE(
            "§aExe",
            "CRIMSON_ATOM_X",
            Color.GREEN,
        ),
        WAI(
            "§6Wai",
            "CRIMSON_ATOM_Y",
            Color.ORANGE,
        ),
        ZEE(
            "§5Zee",
            "CRIMSON_ATOM_Z",
            Color.MAGENTA,
        ),
        ;

        val skullTexture by lazy { SkullTextureHolder.getTexture(textureId) }

        override fun toString(): String = displayName
    }

    private fun AtomType.isSelected() = config.atomsEntries.contains(this)
}
