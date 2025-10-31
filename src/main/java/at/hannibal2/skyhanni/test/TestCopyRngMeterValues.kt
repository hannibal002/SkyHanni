package at.hannibal2.hanni.test

import at.hannibal2.hanni.HanniMod
import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.ConfigManager
import at.hannibal2.hanni.config.ConfigUpdaterMigrator
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.OSUtils
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.repopatterns.RepoPattern

@HanniModule
object TestCopyRngMeterValues {

    private val patternGroup = RepoPattern.group("test.dev.copyrng")

    /**
     * REGEX-TEST: §7§7Slayer XP: §d20,625§5/§d7,917
     */
    private val slayerPattern by patternGroup.pattern(
        "slayer",
        "§7§7Slayer XP: §d.*§5/§d(?<xp>.*)"
    )

    /**
     * REGEX-TEST: §7§7Dungeon Score: §d1,237§5/§d40,620
     */
    private val dungeonPattern by patternGroup.pattern(
        "dungeon",
        "§7§7Dungeon Score: §d.*§5/§d(?<xp>.*)"
    )

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!HanniMod.feature.dev.debug.copyRngMeter) return

        val map = mutableMapOf<NeuInternalName, Long>()
        for (item in event.inventoryItems.values) {
            for (line in item.getLore()) {
                slayerPattern.matchMatcher(line) {
                    map[item.getInternalName()] = group("xp").formatLong()
                }
                dungeonPattern.matchMatcher(line) {
                    map[item.getInternalName()] = group("xp").formatLong()
                }
            }
        }
        if (map.isEmpty()) return

        OSUtils.copyToClipboard(ConfigManager.gson.toJson(map))
        ChatUtils.debug("${map.size} items saved to clipboard.")
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "dev.copyRngMeter", "dev.debug.copyRngMeter")
    }
}
