package at.hannibal2.skyhanni.features.hunting

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ItemInHandChangeEvent
import at.hannibal2.skyhanni.events.entity.EntityCustomNameUpdateEvent
import at.hannibal2.skyhanni.events.entity.EntityRemovedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ComponentMatcherUtils.intoSpan
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.compat.MinecraftCompat.isLocalPlayer
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Leashable
import net.minecraft.world.entity.decoration.ArmorStand

@SkyHanniModule
object LassoDisplay {
    private const val NAMETAG_HEIGHT = 2.0
    private const val NAMETAG_INACCURACY = 2.0

    private val config get() = SkyHanniMod.feature.hunting

    private var holdingLasso = false

    /** Entity ID of every lasso nametag currently shown above a mob we lassoed, to the text it displays. */
    private val lassoNametags = mutableMapOf<Int, Component>()

    private var display: Renderable? = null

    @HandleEvent(onlyOnSkyblock = true)
    private fun onGuiRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.lassoDisplay) return
        display?.let {
            config.lassoDisplayPosition.renderRenderable(it, posLabel = "Lasso Display")
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onItemInHandChange(event: ItemInHandChangeEvent) {
        holdingLasso = event.newStack.getItemCategoryOrNull() == ItemCategory.LASSO
        if (!holdingLasso) reset()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityCustomNameUpdate(event: EntityCustomNameUpdateEvent<ArmorStand>) {
        if ((!config.lassoDisplay && !config.reelAlert) || !holdingLasso) return
        val entity = event.entity
        val name = event.newName?.takeIf { it.isLassoNametag() && entity.isAboveOwnLassoedMob() }
        if (name == null) {
            if (lassoNametags.remove(entity.id) == null) return
        } else {
            lassoNametags[entity.id] = name
            if (config.reelAlert && name.isReel()) {
                SoundUtils.playPlingSound()
            }
        }
        updateDisplay()
    }

    @HandleEvent(onlyOnSkyblock = true)
    private fun onEntityRemoved(event: EntityRemovedEvent<ArmorStand>) {
        if (lassoNametags.remove(event.entity.id) != null) updateDisplay()
    }

    @HandleEvent
    private fun onWorldChange() {
        reset()
    }

    @HandleEvent
    private fun onConfigFixEvent(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(100, "foraging.lassoDisplay", "hunting.lassoDisplay")
        event.move(100, "foraging.lassoDisplayPosition", "hunting.lassoDisplayPosition")
    }

    private fun reset() {
        lassoNametags.clear()
        display = null
    }

    private fun updateDisplay() {
        val names = lassoNametags.values
        display = when {
            names.any { it.isReel() } -> Renderable.text("§e§l          REEL          ")
            else -> names.lastOrNull()?.let { Renderable.text(it) }
        }
    }

    private fun Component.isReel() = string == "REEL"
    private fun Component.isProgress() =
        intoSpan().sampleStyleAtStart().let { it.isBold && it.isStrikethrough }
    private fun Component.isLassoNametag() = isReel() || isProgress()

    /**
     * Checks whether this nametag belongs to a mob the local player has lassoed, by looking for a mob leashed to us
     * roughly [NAMETAG_HEIGHT] blocks below it. The searched box is expanded by one block because the mob is matched
     * against its block position, which can be up to a block away from where its bounding box actually is.
     */
    private fun ArmorStand.isAboveOwnLassoedMob(): Boolean {
        val nametagPosition = getLorenzVec()
        val searchCenter = nametagPosition.down(NAMETAG_HEIGHT)
        return EntityUtils.getEntitiesInBox<Entity>(searchCenter, NAMETAG_INACCURACY + 1.0) { entity ->
            entity is Leashable && entity.leashHolder.isLocalPlayer &&
                entity.blockPosition().toLorenzVec().up(NAMETAG_HEIGHT).distance(nametagPosition) < NAMETAG_INACCURACY
        }.isNotEmpty()
    }
}
