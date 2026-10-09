package at.hannibal2.skyhanni.features.combat.mobs

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.BestiaryApi
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.features.combat.mobs.BestiaryMobHighlight.Companion.toMarkedVariant
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.RenderUtils.highlight

@SkyHanniModule
object BestiaryMobHighlights {
    private val config get() = SkyHanniMod.feature.combat.mobs.bestiaryMobHighlights
    private val entries get() = config.highlights

    private var markedInventoryEntries = setOf<Int>()

    @HandleEvent
    private fun onConfigLoad() {
        ConditionalUtils.onToggle(
            config.enabled,
            config.highlightColor
        ) {
            BestiaryMobHighlightManager.forceApplyRules(glow = isEnabled())
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
        if (!config.enabled.get()) return
        val state = BestiaryApi.currentState as? BestiaryApi.BestiaryGuiState.Variants ?: return

        if (event.clickedButton != KeyboardManager.MIDDLE_MOUSE) return
        val slotIndex = event.slot?.index ?: return
        val variant = state.variants[slotIndex] ?: return
        val family = state.parentFamily?.cleanName ?: return

        event.cancel()

        val markedVariant = variant.toMarkedVariant(family)

        DelayedRun.runOrNextTick {
            BestiaryMobHighlightManager.toggle(markedVariant)
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
        markedInventoryEntries = when (val state = BestiaryApi.currentState) {
            is BestiaryApi.BestiaryGuiState.Variants -> {
                val family = state.parentFamily?.cleanName ?: return

                state.variants
                    .filterValues { variant ->
                        entries.any {
                            it.matchesVariant(family, variant)
                        }
                    }
                    .keys
            }
            is BestiaryApi.BestiaryGuiState.Mobs -> {
                state.mobs
                    .filterValues { mob ->
                        entries.any {
                            it.family == mob.cleanName
                        }
                    }
                    .keys
            }
            else -> emptySet()
        }
    }

    private fun isEnabled(): Boolean {
        val state = BestiaryApi.currentState
        return config.enabled.get() &&
            (state is BestiaryApi.BestiaryGuiState.Mobs || state is BestiaryApi.BestiaryGuiState.Variants)
    }
}
