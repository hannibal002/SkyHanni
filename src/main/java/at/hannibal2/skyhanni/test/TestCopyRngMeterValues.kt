package at.hannibal2.skyhanni.test

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object TestCopyRngMeterValues {

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!SkyHanniMod.feature.dev.debug.copyRngMeter) return

        val map = mutableMapOf<NEUInternalName, Long>()
        val slayerPattern = "§7Slayer XP: §d.*§5/§d(?<xp>.*)".toPattern()
        val dungeonPattern = "§7Dungeon Score: §d.*§5/§d(?<xp>.*)".toPattern()
        for (item in event.inventoryItems.values) {
            for (line in item.getLore()) {
                slayerPattern.matchMatcher(line) {
                    map[item.getInternalName()] = group("xp").formatNumber()
                }
                dungeonPattern.matchMatcher(line) {
                    map[item.getInternalName()] = group("xp").formatNumber()
                }
            }
        }
        if (map.isEmpty()) return

        OSUtils.copyToClipboard(ConfigManager.gson.toJson(map))
        LorenzUtils.debug("${map.size} items saved to clipboard.")
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(4, "dev.copyRngMeter", "dev.debug.copyRngMeter")
    }
}