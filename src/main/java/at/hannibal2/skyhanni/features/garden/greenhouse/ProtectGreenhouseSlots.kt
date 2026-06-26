package at.hannibal2.skyhanni.features.garden.greenhouse

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.entity.EntityClickEvent
import at.hannibal2.skyhanni.events.minecraft.KeyDownEvent
import at.hannibal2.skyhanni.events.minecraft.SkyHanniRenderWorldEvent
import at.hannibal2.skyhanni.features.garden.GardenPlotApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.render.WorldRenderUtils.drawFilledBoundingBox
import at.hannibal2.skyhanni.utils.toLorenzVec
import net.minecraft.client.Minecraft
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult

@SkyHanniModule
object ProtectGreenhouseSlots {

    private const val MAX_PROTECTED_SLOTS = 300
    private const val MUTATION_Y_LEVEL = 74.0

    private val config get() = SkyHanniMod.feature.garden.greenhouse
    private val storage get() = ProfileStorageData.profileSpecific?.garden?.greenhouse

    @Suppress("ReturnCount")
    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onKeyDown(event: KeyDownEvent) {
        if (!isEnabled()) return
        if (Minecraft.getInstance().screen != null) return
        if (event.keyCode != config.protectGreenhouseSlotsToggleKey) return

        val hitResult = Minecraft.getInstance().hitResult ?: return
        val protectedSlots = storage?.protectedSlots ?: return

        val slot = when (hitResult.type) {
            HitResult.Type.BLOCK -> (hitResult as BlockHitResult).blockPos
            HitResult.Type.ENTITY -> (hitResult as EntityHitResult).entity.blockPosition()
            else -> return
        }.atY(0).toLorenzVec()

        if (!protectedSlots.remove(slot)) {

            if (protectedSlots.size >= MAX_PROTECTED_SLOTS) {
                ChatUtils.userError(
                    "You can protect a maximum of $MAX_PROTECTED_SLOTS slots. " +
                        "Run /shclearprotectedghslots to clear protected slots.",
                )
                return
            }

            protectedSlots.add(slot)
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onBlockClick(event: BlockClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != InteractClickType.LEFT_CLICK) return
        if (event.position.toBlockPos().atY(0).toLorenzVec() in storage?.protectedSlots.orEmpty()) {
            event.cancel()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onEntityClick(event: EntityClickEvent) {
        if (!isEnabled()) return
        if (event.clickType != InteractClickType.LEFT_CLICK) return
        if (event.clickedEntity.blockPosition().atY(0).toLorenzVec() in storage?.protectedSlots.orEmpty()) {
            event.cancel()
        }
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shclearprotectedghslots") {
            this.description = "Clears all protected greenhouse slots."
            this.category = CommandCategory.USERS_RESET
            simpleCallback {
                val protectedSlots = storage?.protectedSlots ?: return@simpleCallback
                protectedSlots.clear()
                ChatUtils.chat("Cleared protected greenhouse slots.")
            }
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onRenderWorld(event: SkyHanniRenderWorldEvent) {
        if (!isEnabled()) return

        for (slot in storage?.protectedSlots.orEmpty()) {
            event.drawFilledBoundingBox(
                AABB(slot.x, MUTATION_Y_LEVEL, slot.z, slot.x + 1, MUTATION_Y_LEVEL + 1, slot.z + 1),
                config.protectGreenhouseSlotsColor
            )
        }
    }

    private fun isEnabled(): Boolean = config.enableProtectGreenhouseSlots && GardenPlotApi.inGreenhouse()
}
