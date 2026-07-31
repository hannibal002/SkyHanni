package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BestiaryApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.combat.mobs.MarkedMob.Companion.toMarkedVariant
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object MarkedBestiaryMob {

    private val config get() = SkyHanniMod.feature.combat.mobs.markedMobs
    private val entries get() = config.markedMobs

    private var markedInventoryEntries = setOf<Int>()

    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.enabled,
            config.highlightColor
        ) {
            MarkedMobManager.forceApplyRules(glow = isEnabled())
        }
    }

    @HandleEvent
    private fun onInventoryFullyOpened() {
        if (!isEnabled()) return

        DelayedRun.runOrNextTick {
            rebuildMarkedInventoryEntries()
        }
    }

    @HandleEvent
    private fun onSlotClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (!BestiaryApi.isMobVariants) return
        if (event.clickedButton != KeyboardManager.MIDDLE_MOUSE) return

        val slot = event.slot ?: return

        val variant = BestiaryApi.mobVariants
            .find { it.slot == slot.index }
            ?: return

        val family = BestiaryApi.currentFamily?.cleanName
            ?: return

        event.cancel()

        val markedVariant = variant.toMarkedVariant(family)

        DelayedRun.runOrNextTick {
            MarkedMobManager.toggle(markedVariant)
            rebuildMarkedInventoryEntries()
        }
    }

    @HandleEvent(priority = HandleEvent.LOW)
    private fun onBackgroundDrawn() {
        if (!isEnabled()) return

        markedInventoryEntries.forEach { slotIndex ->
            InventoryUtils.getSlotAtIndex(slotIndex)
                ?.highlight(config.highlightColor.get())
        }
    }

    private fun rebuildMarkedInventoryEntries() {
        markedInventoryEntries = when {
            BestiaryApi.isMobVariants -> {
                val family = BestiaryApi.currentFamily?.cleanName ?: return

                BestiaryApi.mobVariants
                    .filter { variant ->
                        entries.any {
                            it.matchesVariant(family, variant)
                        }
                    }
                    .mapNotNull { it.slot }
                    .toSet()
            }
            BestiaryApi.isCategoryOfMobs -> {
                BestiaryApi.mobList
                    .filter { mob ->
                        entries.any {
                            it.family == mob.cleanName
                        }
                    }
                    .mapNotNull { it.slot }
                    .toSet()
            }

            else -> emptySet()
        }
    }

    private fun isEnabled(): Boolean {
        return config.enabled.get() &&
            (BestiaryApi.isCategoryOfMobs || BestiaryApi.isMobVariants)
    }
}
