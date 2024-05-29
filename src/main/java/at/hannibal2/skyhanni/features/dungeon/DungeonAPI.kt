package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.ScoreboardData
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.DungeonEnterEvent
import at.hannibal2.skyhanni.events.DungeonStartEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.LorenzWorldChangeEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.matchFirst
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TabListData
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object DungeonAPI {

    private val floorPattern = " §7⏣ §cThe Catacombs §7\\((?<floor>.*)\\)".toPattern()
    private val uniqueClassBonus =
        "^Your ([A-Za-z]+) stats are doubled because you are the only player using this class!$".toRegex()

    private val bossPattern =
        "View all your (?<name>\\w+) Collection".toPattern()
    private val levelPattern =
        " +(?<kills>\\d+).*".toPattern()
    private val killPattern = " +☠ Defeated (?<boss>\\w+).*".toPattern()
    private val totalKillsPattern = "§7Total Kills: §e(?<kills>.*)".toPattern()

    var dungeonFloor: String? = null
    var started = false
    var inBossRoom = false
    var playerClass: DungeonClass? = null
    var playerClassLevel = -1
    var isUniqueClass = false

    val bossStorage: MutableMap<DungeonFloor, Int>? get() = ProfileStorageData.profileSpecific?.dungeons?.bosses
    private val timePattern =
        "Time Elapsed:( )?(?:(?<minutes>\\d+)m)? (?<seconds>\\d+)s".toPattern() // Examples: Time Elapsed: 10m 10s, Time Elapsed: 2s

    private val patternGroup = RepoPattern.group("dungeon")

    private val dungeonComplete by patternGroup.pattern(
        "complete",
        "§.\\s+§.§.(?:The|Master Mode) Catacombs §.§.- §.§.(?:Floor )?(?<floor>M?[IV]{1,3}|Entrance)"
    )
    private val dungeonRoomPattern by patternGroup.pattern(
        "room",
        "§7\\d+\\/\\d+\\/\\d+ §\\w+ (?<roomId>[\\w,-]+)"
    )
    private val blessingPattern by patternGroup.pattern(
        "blessings",
        "§r§r§fBlessing of (?<type>\\w+) (?<amount>\\w+)§r"
    )
    private val noBlessingPattern by patternGroup.pattern(
        "noblessings",
        "§r§r§7No Buffs active. Find them by exploring the Dungeon!§r"
    )

    enum class DungeonBlessings(var power: Int) {
        LIFE(0),
        POWER(0),
        STONE(0),
        WISDOM(0),
        TIME(0);

        val displayName by lazy { name.firstLetterUppercase() }

        companion object {
            fun reset() {
                entries.forEach { it.power = 0 }
            }
        }
    }

    fun inDungeon() = IslandType.CATACOMBS.isInIsland()

    fun isOneOf(vararg floors: String) = dungeonFloor?.equalsOneOf(*floors) == true

    fun handleBossMessage(rawMessage: String) {
        if (!inDungeon()) return
        val message = rawMessage.removeColor()
        val bossName = message.substringAfter("[BOSS] ").substringBefore(":").trim()
        if ((bossName != "The Watcher") && dungeonFloor != null && checkBossName(bossName) && !inBossRoom) {
            DungeonBossRoomEnterEvent().postAndCatch()
            inBossRoom = true
        }
    }

    private fun checkBossName(bossName: String): Boolean {
        val correctBoss = when (dungeonFloor!!) {
            "E" -> "The Watcher"
            "F1", "M1" -> "Bonzo"
            "F2", "M2" -> "Scarf"
            "F3", "M3" -> "The Professor"
            "F4", "M4" -> "Thorn"
            "F5", "M5" -> "Livid"
            "F6", "M6" -> "Sadan"
            "F7", "M7" -> "Maxor"
            else -> null
        } ?: return false

        // Livid has a prefix in front of the name, so we check ends with to cover all the livids
        return bossName.endsWith(correctBoss)
    }

    fun getTime(): String {
        ScoreboardData.sidebarLinesFormatted.matchFirst(timePattern) {
            return "${group("minutes") ?: "00"}:${group("seconds")}" // 03:14
        }
        return ""
    }

    fun getCurrentBoss(): DungeonFloor? {
        val floor = dungeonFloor ?: return null
        return DungeonFloor.valueOf(floor.replace("M", "F"))
    }

    fun getRoomID(): String? {
        return ScoreboardData.sidebarLinesFormatted.matchFirst(dungeonRoomPattern) {
            group("roomId")
        }
    }

    fun getColor(level: Int): String = when {
        level >= 50 -> "§c§l"
        level >= 45 -> "§c"
        level >= 40 -> "§6"
        level >= 35 -> "§d"
        level >= 30 -> "§9"
        level >= 25 -> "§b"
        level >= 20 -> "§2"
        level >= 15 -> "§a"
        level >= 10 -> "§e"
        level >= 5 -> "§f"
        else -> "§7"
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (dungeonFloor == null) {
            ScoreboardData.sidebarLinesFormatted.matchFirst(floorPattern) {
                val floor = group("floor")
                dungeonFloor = floor
                DungeonEnterEvent(floor).postAndCatch()
            }
        }
        if (dungeonFloor != null && playerClass == null) {
            val playerTeam =
                TabListData.getTabList().firstOrNull {
                    it.contains(LorenzUtils.getPlayerName())
                }?.removeColor() ?: ""

            DungeonClass.entries.forEach {
                if (playerTeam.contains("(${it.scoreboardName} ")) {
                    val level = playerTeam.split(" ").last().trimEnd(')').romanToDecimalIfNecessary()
                    playerClass = it
                    playerClassLevel = level
                }
            }
        }
    }

    @SubscribeEvent
    fun onTabUpdate(event: TablistFooterUpdateEvent) {
        if (!inDungeon()) return
        val tabList = event.footer.split("\n")
        tabList.forEach {
            if (noBlessingPattern.matches(it)) {
                DungeonBlessings.reset()
                return
            }
            val matcher = blessingPattern.matcher(it)
            if (matcher.find()) {
                val type = matcher.group("type") ?: return@forEach
                val amount = matcher.group("amount").romanToDecimalIfNecessary()
                if (DungeonBlessings.valueOf(type.uppercase()).power != amount) {
                    DungeonBlessings.valueOf(type.uppercase()).power = amount
                }
            }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: LorenzWorldChangeEvent) {
        dungeonFloor = null
        started = false
        inBossRoom = false
        isUniqueClass = false
        playerClass = null
        playerClassLevel = -1
        DungeonBlessings.reset()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        val floor = dungeonFloor ?: return
        if (event.message == "§e[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon.") {
            started = true
            DungeonStartEvent(floor).postAndCatch()
        }
        if (event.message.removeColor().matches(uniqueClassBonus)) {
            isUniqueClass = true
        }

        if (!LorenzUtils.inSkyBlock) return
        killPattern.matchMatcher(event.message.removeColor()) {
            val bossCollections = bossStorage ?: return
            val boss = DungeonFloor.byBossName(group("boss"))
            if (matches() && boss != null && boss !in bossCollections) {
                bossCollections.addOrPut(boss, 1)
            }
            return
        }
        dungeonComplete.matchMatcher(event.message) {
            DungeonCompleteEvent(floor).postAndCatch()
            return
        }
    }

    // This returns a map of boss name to the integer for the amount of kills the user has in the collection
    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val bossCollections = bossStorage ?: return

        if (event.inventoryName == "Boss Collections") {
            readallCollections(bossCollections, event.inventoryItems)
        } else if (event.inventoryName.endsWith(" Collection")) {
            readOneMaxCollection(bossCollections, event.inventoryItems, event.inventoryName)
        }
    }

    private fun readOneMaxCollection(
        bossCollections: MutableMap<DungeonFloor, Int>,
        inventoryItems: Map<Int, ItemStack>,
        inventoryName: String,
    ) {
        inventoryItems[48]?.let { item ->
            if (item.name == "§aGo Back") {
                item.getLore().getOrNull(0)?.let { firstLine ->
                    if (firstLine == "§7To Boss Collections") {
                        val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                        val floor = DungeonFloor.byBossName(name) ?: return
                        val lore = inventoryItems[4]?.getLore() ?: return
                        val line = lore.find { it.contains("Total Kills:") } ?: return
                        val kills = totalKillsPattern.matchMatcher(line) {
                            group("kills").formatInt()
                        } ?: return
                        bossCollections[floor] = kills
                    }
                }
            }
        }
    }

    private fun readallCollections(
        bossCollections: MutableMap<DungeonFloor, Int>,
        inventoryItems: Map<Int, ItemStack>,
    ) {
        nextItem@ for (stack in inventoryItems.values) {
            var name = ""
            var kills = 0
            nextLine@ for (line in stack.getLore()) {
                val colorlessLine = line.removeColor()
                bossPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        name = group("name")
                    }
                }
                levelPattern.matchMatcher(colorlessLine) {
                    if (matches()) {
                        kills = group("kills").toInt()
                        break@nextLine
                    }
                }
            }
            val floor = DungeonFloor.byBossName(name) ?: continue
            bossCollections[floor] = kills
        }
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Dungeon")

        if (!inDungeon()) {
            event.addIrrelevant("not in dungeons")
            return
        }

        event.addData {
            add("dungeonFloor: $dungeonFloor")
            add("started: $started")
            add("getRoomID: ${getRoomID()}")
            add("inBossRoom: $inBossRoom")
            add("")
            add("playerClass: $playerClass")
            add("isUniqueClass: $isUniqueClass")
            add("playerClassLevel: $playerClassLevel")
            add("")
            add("Blessings: ")
            for (blessing in DungeonBlessings.entries) {
                add("  ${blessing.displayName} ${blessing.power}")
            }
        }
    }

    enum class DungeonClass(val scoreboardName: String) {
        ARCHER("Archer"),
        BERSERK("Berserk"),
        HEALER("Healer"),
        MAGE("Mage"),
        TANK("Tank")
    }

    enum class DungeonChest(val inventory: String) {
        WOOD("Wood Chest"),
        GOLD("Gold Chest"),
        DIAMOND("Diamond Chest"),
        EMERALD("Emerald Chest"),
        OBSIDIAN("Obsidian Chest"),
        BEDROCK("Bedrock Chest"),
        ;

        companion object {
            fun getByInventoryName(inventory: String) = entries.firstOrNull { it.inventory == inventory }
        }
    }
}
