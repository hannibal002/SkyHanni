package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class DungeonUtils {
    private val bossPattern =
        "View all your (?<name>\\w+) Collection".toPattern() // e.g. View all your Bonzo Collection
    private val levelPattern =
        " +(?<kills>\\d+).*".toPattern() // e.g.                            168/250 (the progress bar is just colored spaces that are struck through)
    private val killPattern = " +☠ Defeated (?<boss>\\w+).*".toPattern()
    private val bossList = listOf("Bonzo", "Scarf", "The Professor", "Thorn", "Livid", "Sadan", "Necron")

    private var bossCollections: MutableMap<String, Int> = mutableMapOf()

    // This returns a map of boss name to the integer for the amount of kills the user has in the collection
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (event.inventoryName != "Boss Collections") return
        nextItem@ for ((_, stack) in event.inventoryItems) {
            var name = ""
            var kills = 0
            nextLine@ for (line in stack.getLore()) {
                val colorlessLine = line.removeColor()
                bossPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        name = group("name")
                        if (!bossList.contains(name)) continue@nextItem // to avoid kuudra, etc.
                    }
                }
                levelPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        kills = group("kills").toInt()
                        break@nextLine
                    }
                }
            }
            bossCollections[name] = kills
        }
        ProfileStorageData.profileSpecific?.dungeons?.bosses = bossCollections
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!SkyHanniMod.feature.misc.discordRPC.enabled.get()) return
        if (!LorenzUtils.inDungeons) return
        killPattern.matchMatcher(event.message.removeColor()) {
            val boss = group("boss")
            if (matches() && bossCollections[boss] != null) {
                bossCollections[boss] = bossCollections[boss]!! + 1
                ProfileStorageData.profileSpecific?.dungeons?.bosses = bossCollections
            }
        }
    }
}