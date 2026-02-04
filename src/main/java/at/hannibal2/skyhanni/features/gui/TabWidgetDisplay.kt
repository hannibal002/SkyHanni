package at.hannibal2.skyhanni.features.gui

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.model.TabWidgetComponent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text

enum class TabWidgetDisplay(
    private val configName: String?,
    vararg val widgets: TabWidgetComponent,
) {
    SOULFLOW(null, TabWidgetComponent.SOULFLOW),
    COINS("Bank and Interest", TabWidgetComponent.BANK, TabWidgetComponent.INTEREST),
    SB_LEVEL("Skyblock Level", TabWidgetComponent.SB_LEVEL),
    PROFILE(null, TabWidgetComponent.PROFILE),
    PLAYER_LIST("Players", TabWidgetComponent.PLAYER_LIST),
    PET(null, TabWidgetComponent.PET),
    PET_TRAINING("Pet Upgrade Info", TabWidgetComponent.PET_SITTER, TabWidgetComponent.PET_TRAINING),
    STATS(null, TabWidgetComponent.STATS, TabWidgetComponent.DUNGEON_SKILLS_AND_STATS),
    DUNGEON_TEAM("Dungeon Info about every person", TabWidgetComponent.DUNGEON_PARTY),
    DUNGEON_PUZZLE("Dungeon Info about puzzles", TabWidgetComponent.DUNGEON_PUZZLE),
    DUNGEON_OVERALL("Dungeon General Info (very long)", TabWidgetComponent.DUNGEON_STATS),
    BESTIARY(null, TabWidgetComponent.BESTIARY),
    DRAGON("Dragon Fight Info", TabWidgetComponent.DRAGON),
    PROTECTOR("Protector State", TabWidgetComponent.PROTECTOR),
    SHEN_RIFT("Shen's Auction inside the Rift", TabWidgetComponent.RIFT_SHEN),
    MINION("Minion Info", TabWidgetComponent.MINION),
    COLLECTION(null, TabWidgetComponent.COLLECTION),
    TIMERS(null, TabWidgetComponent.TIMERS),
    FIRE_SALE(null, TabWidgetComponent.FIRE_SALE),
    RAIN("Park Rain", TabWidgetComponent.RAIN),
    PEST_TRAPS("Pest Traps", TabWidgetComponent.PEST_TRAPS, TabWidgetComponent.FULL_TRAPS, TabWidgetComponent.NO_BAIT),
    FULL_PROFILE_WIDGET(
        "Profile Widget",
        TabWidgetComponent.PROFILE,
        TabWidgetComponent.SB_LEVEL,
        TabWidgetComponent.BANK,
        TabWidgetComponent.INTEREST,
        TabWidgetComponent.SOULFLOW,
        TabWidgetComponent.FAIRY_SOULS,
    ),
    EYES("Eyes placed", TabWidgetComponent.EYES_PLACED),
    MOONGLADE_BEACON("Moonglade Beacon", TabWidgetComponent.MOONGLADE_BEACON),
    STARBORN_TEMPLE("Starborn Temple", TabWidgetComponent.STARBORN_TEMPLE),
    SHARD_TRAPS("Shard Traps", TabWidgetComponent.SHARD_TRAPS),
    FOREST_WHISPERS("Forest Whispers", TabWidgetComponent.FOREST_WHISPERS),
    AGATHA_CONTEST("Agatha's Contest", TabWidgetComponent.AGATHA_CONTEST),
    COMMISSIONS("Mining Commissions", TabWidgetComponent.COMMISSIONS),
    SLAYER("Slayer", TabWidgetComponent.SLAYER),
    PITY("Pity", TabWidgetComponent.PITY),
    PICKAXE_COOLDOWN("Pickaxe Cooldown", TabWidgetComponent.PICKAXE_COOLDOWN),
    ;

    val position get() = config.displayPositions[ordinal]

    override fun toString(): String {
        return configName ?: name.lowercase().allLettersFirstUppercase()
    }

    @SkyHanniModule
    companion object {

        private val config get() = SkyHanniMod.feature.gui.tabWidget
        private fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

        @HandleEvent
        fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
            if (!isEnabled()) return
            if (config.displayPositions.isEmpty()) return
            config.display.get().forEach { widget ->
                widget.position.renderRenderables(
                    widget.widgets.flatMap { subWidget ->
                        subWidget.lines.map { Renderable.text(it) }
                    },
                    posLabel = "Display Widget: ${widget.name}",
                )
            }
        }

        @HandleEvent
        fun onJoin(event: ProfileJoinEvent) {
            // Validation that the displayPositions in the config is correct
            val sizeDiff = TabWidgetDisplay.entries.size - config.displayPositions.size
            if (sizeDiff == 0) return
            if (sizeDiff < 0) {
                ErrorManager.skyHanniError(
                    "Invalid State of config.displayPositions",
                    "Display" to TabWidgetDisplay.entries,
                    "Positions" to config.displayPositions,
                )
            } else {
                config.displayPositions.addAll(List(sizeDiff) { Position() })
            }
        }
    }
}
