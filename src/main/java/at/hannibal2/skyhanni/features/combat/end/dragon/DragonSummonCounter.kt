package at.hannibal2.skyhanni.features.combat.end.dragon

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.EndBoss
import at.hannibal2.skyhanni.events.EndBossSpawnEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatIntOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.primitives.text
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

/**
 * Counts the dragons the player helped summon. Deliberately its own overlay rather than a line in
 * one of the fight displays: the number is not tied to a running fight and stays relevant between
 * them.
 */
@SkyHanniModule
object DragonSummonCounter {

    private val config get() = SkyHanniMod.feature.combat.endIsland.summonCounter

    private val summoned: Int get() = ProfileStorageData.profileSpecific?.summonedDragons ?: 0

    private val repoGroup = RepoPattern.group("combat.end-dragon-guide")

    /**
     * Hypixel keeps the real total in the dragon guide. It is read once as a baseline, because it
     * covers every dragon summoned before this mod was installed - counting alone would start at
     * zero and never catch up.
     *
     * The percentile behind the number is optional - it only shows once the total is ranked.
     *
     * REGEX-TEST: Dragons Summoned: 494 (Top 0.01%)
     * REGEX-TEST: Dragons Summoned: 47
     */
    private val summonedPattern by repoGroup.pattern(
        "summoned",
        "\\s*Dragons? Summoned: (?<amount>[\\d,.]+)(?: \\(Top [\\d,.]+%\\))?",
    )

    /**
     * REGEX-TEST: Dragon Guide
     * REGEX-TEST: Dragon Spawning
     */
    private val guideTitlePattern by repoGroup.pattern(
        "title",
        "Dragon (?:Guide|Spawning)",
    )

    /** Set once the guide has been read, so the overlay knows whether its number means anything. */
    private val synced: Boolean get() = ProfileStorageData.profileSpecific?.summonedDragonsSynced == true

    /**
     * Opening the guide overwrites the counter with Hypixel's own total. From then on the local
     * count takes over, so the number stays right without reopening the menu.
     */
    @HandleEvent
    private fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName.contains("Dragon", ignoreCase = true)) {
            ChatUtils.consoleLog("[DragonSummonCounter] inventory: '${event.inventoryName}'")
        }
        if (!guideTitlePattern.matches(event.inventoryName)) return
        val storage = ProfileStorageData.profileSpecific ?: return
        for (item in event.inventoryItems.values) {
            for (line in item.getLore()) {
                summonedPattern.matchMatcher(line.removeColor()) {
                    val amount = group("amount").formatIntOrNull() ?: return@matchMatcher
                    storage.summonedDragons = amount
                    storage.summonedDragonsSynced = true
                    ChatUtils.chat("§eSynced your summoned dragons: §f${amount.addSeparators()}")
                    return
                }
            }
        }
    }

    /**
     * Counted on the spawn, and only when the player contributed an eye: standing at the nest
     * while somebody else summons is not the same thing.
     */
    @HandleEvent
    private fun onEndBossSpawn(event: EndBossSpawnEvent) {
        ChatUtils.consoleLog(
            "[DragonSummonCounter] spawn: ${event.boss}, eyes=${DragonFightState.eyesPlaced}",
        )
        if (event.boss != EndBoss.DRAGON) return
        if (DragonFightState.eyesPlaced <= 0) return
        val storage = ProfileStorageData.profileSpecific ?: return
        storage.summonedDragons++
    }

    @HandleEvent(onlyOnIsland = IslandType.THE_END)
    private fun onRender(event: GuiRenderEvent) {
        if (!config.enabled) return
        if (!synced) {
            config.position.renderRenderable(
                Renderable.text("§5Dragons Summoned: §c? §7(run /dragonguide)"),
                posLabel = "Dragons Summoned",
            )
            return
        }
        config.position.renderRenderable(
            Renderable.text("§5Dragons Summoned: §f${summoned.addSeparators()}"),
            posLabel = "Dragons Summoned",
        )
    }

    @HandleEvent
    private fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shresetdragonsummons") {
            description = "Reset the counter of dragons you have summoned."
            category = CommandCategory.USERS_RESET
            simpleCallback {
                ProfileStorageData.profileSpecific?.let {
                    it.summonedDragons = 0
                    it.summonedDragonsSynced = false
                }
                ChatUtils.chat("§eReset the summoned dragon counter.")
            }
        }
    }
}
