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
            val atom = AtomType.entries.firstOrNull { entity.wearingSkullTexture(it.texture) } ?: continue
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
        val texture: String,
        val color: Color,
    ) {
        EXE(
            "§aExe",
            "ewogICJ0aW1lc3RhbXAiIDogMTY0NjA2NzI1MDA4NSwKICAicHJvZmlsZUlkIiA6ICJmNThkZWJkNTlmNTA0MjIyOGY2MDIyMjExZDRjMTQwYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJ1bnZlbnRpdmV0YWxlbnQiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWU0NmEzOWE4ZTYxYTRkYTA3YjE3YWRlZjBlZTIyMDlmNjRkYThhZWI5YTliZDYxMTBhNGUyNGUzNWVkNzRmMyIKICAgIH0KICB9Cn0=",
            Color.GREEN,
        ),
        WAI(
            "§6Wai",
            "ewogICJ0aW1lc3RhbXAiIDogMTY0NjA2NzE3MjQxNywKICAicHJvZmlsZUlkIiA6ICI0NDAzZGM1NDc1YmM0YjE1YTU0OGNmZGE2YjBlYjdkOSIsCiAgInByb2ZpbGVOYW1lIiA6ICJDaGFvc0NvbXB1dHJDbHViIiwKICAic2lnbmF0dXJlUmVxdWlyZWQiIDogdHJ1ZSwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2Y4M2ZkZGY3YWE2ZmYzYzUwYWJhYzdlNmE3Nzk2ZTE1NWNjYTRjY2ZjZTQ1NzM0ZjY2ZTQxMWNiY2E5ODlhMDQiCiAgICB9CiAgfQp9",
            Color.ORANGE,
        ),
        ZEE(
            "§5Zee",
            "ewogICJ0aW1lc3RhbXAiIDogMTY0NjA2NzIyODA3MywKICAicHJvZmlsZUlkIiA6ICJhOGJhMGY1YTFmNjQ0MTgzODZkZGI3OWExZmY5ZWRlYyIsCiAgInByb2ZpbGVOYW1lIiA6ICJDcmVlcGVyOTA3NSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS80ZThmMTlkZWYzMzA4NWRiNTY0OTA5ZmM4YzFiMmVhYmMwZWJjNTljZDg5NjY1ZjMxZTU4MGYxZmMyNDk3YjU4IgogICAgfQogIH0KfQ==",
            Color.MAGENTA,
        ),
        ;

        override fun toString(): String = displayName
    }

    private fun AtomType.isSelected() = config.atomsEntries.contains(this)
}
