package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object TestCopyRngMeterValues {

    private val patternGroup = RepoPattern.group("test.dev.copyrng")

    /**
     * REGEX-TEST: Slayer XP: 20,625/7,917
     * REGEX-TEST: Dungeon Score: 489,850/75,000
     * REGEX-TEST: Frozen Corpse XP: 20,625/60,000
     * REGEX-TEST: Experimental XP: 20,105/150,000
     * REGEX-TEST: Nucleus XP: 202,105/320,000
     */
    private val rngScorePattern by patternGroup.pattern(
        "rngscore",
        "(?:(?:Slayer|Experimental|Nucleus|Frozen Corpse) XP|Dungeon Score): [\\d,.kM]+/(?<xp>[\\d,.kM]+)"
    )

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!SkyHanniMod.feature.dev.debug.copyRngMeter) return

        val map = mutableMapOf<NeuInternalName, Long>()
        for (item in event.inventoryItems.values) {
            for (line in item.getLoreComponent()) {
                rngScorePattern.matchMatcher(line) {
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
