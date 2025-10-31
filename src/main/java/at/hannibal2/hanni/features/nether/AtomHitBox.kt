package at.hannibal2.hanni.features.nether

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.data.IslandType
import at.hannibal2.hanni.events.minecraft.HanniRenderWorldEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.EntityUtils
import at.hannibal2.hanni.utils.EntityUtils.wearingSkullTexture
import at.hannibal2.hanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.hanni.utils.LorenzVec
import at.hannibal2.hanni.utils.SkullTextureHolder
import at.hannibal2.hanni.utils.collection.CollectionUtils.removeIfKey
import at.hannibal2.hanni.utils.getLorenzVec
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawHitbox
import at.hannibal2.hanni.utils.render.WorldRenderUtils.drawString
import net.minecraft.entity.item.EntityArmorStand
import java.awt.Color

@HanniModule
object AtomHitBox {

    private val config get() = HanniMod.feature.crimsonIsle.atomHitBox
    private val atomsList = mutableMapOf<EntityArmorStand, AtomType>()

    @HandleEvent(onlyOnIsland = IslandType.CRIMSON_ISLE)
    fun onRenderWorld(event: HanniRenderWorldEvent) {
        if (!config.enabled) return
        atomsList.removeIfKey { !it.isEntityAlive }
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
