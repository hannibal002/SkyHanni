package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickedBlockType
import at.hannibal2.skyhanni.data.InteractClickType
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.BlockClickEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ScoreboardUpdateEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.TablistFooterUpdateEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBlockClickEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonBossRoomEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonCompleteEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonEnterEvent
import at.hannibal2.skyhanni.events.dungeon.DungeonStartEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.PlayerUtils
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAllComponents
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SafeItemStack
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.bold
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.level.block.Blocks

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object DungeonApi {
    val patternGroup = RepoPattern.group("dungeon")

    // TODO: move to SkyblockIcons class
    /**
     * WRAPPED-REGEX-TEST: " ⏣ The Catacombs (F7)"
     * WRAPPED-REGEX-TEST: "  The Catacombs (F7)"
     */
    private val floorPattern by patternGroup.pattern(
        "floor",
        " [⏣\uE067] The Catacombs \\((?<floor>.*)\\)",
    )

    /**
     * REGEX-TEST: Your Mage stats are doubled because you are the only player using this class!
     */
    private val uniqueClassBonus by patternGroup.pattern(
        "unique-class-bonus",
        "^Your (?<class>[A-Za-z]+) stats are doubled because you are the only player using this class!$",
    )

    /**
     * REGEX-TEST: View all your Bonzo Collection
     */
    private val bossPattern by patternGroup.pattern(
        "boss",
        "View all your (?<name>\\w+) Collection",
    )

    /**
     * WRAPPED-REGEX-TEST: " 1234"
     */
    private val levelPattern by patternGroup.pattern(
        "level",
        " +(?<kills>\\d+).*",
    )

    /**
     * WRAPPED-REGEX-TEST: " ☠ Defeated Bonzo"
     */
    private val killPattern by patternGroup.pattern(
        "kill",
        " +. Defeated (?<boss>\\w+).*",
    )

    /**
     * REGEX-TEST: Total Kills: 123
     */
    private val totalKillsPattern by patternGroup.pattern(
        "total-kills",
        "Total Kills: (?<kills>.*)",
    )

    var dungeonFloor: String? = null
        private set
    var started = false
        private set
    var completed = false
        private set
    var inBossRoom = false
        private set
    var playerClass: DungeonClass? = null
        private set
    var playerClassLevel: Int = -1
        private set
    var isUniqueClass = false
        private set
    var time: String = ""
        private set
    var roomId: String? = null
        private set
    val active get() = started && !completed

    val bossStorage: MutableMap<DungeonFloor, Int>? get() = ProfileStorageData.profileSpecific?.dungeons?.bosses

    private val WITHER_ESSENCE_TEXTURE by SkullTextureHolder.texture("WITHER_ESSENCE")

    /**
     * REGEX-TEST: Time Elapsed: §a01m 17s
     * REGEX-TEST: Time Elapsed: §a14s
     */
    private val timePattern by patternGroup.pattern(
        "time",
        "Time Elapsed: §.(?:(?<minutes>\\d+)m )?(?<seconds>\\d+)s",
    )

    /**
     * WRAPPED-REGEX-TEST: "                                 Master Mode The Catacombs - Floor V"
     */
    private val dungeonComplete by patternGroup.pattern(
        "completecolorless",
        "\\s+(?:Master Mode )?The Catacombs - (?:Floor [IV]{1,3}|Entrance)",
    )

    /**
     * REGEX-TEST: §711/15/24 §8m4F 830,-420
     */
    val dungeonRoomPattern by patternGroup.pattern(
        "room",
        "§7\\d+/\\d+/\\d+ §\\w+ (?<roomId>[\\w,-]+)",
    )

    /**
     * REGEX-TEST: §r§r§fBlessing of Power V§r
     */
    private val blessingPattern by patternGroup.pattern(
        "blessings.colorless",
        "Blessing of (?<type>\\w+) (?<amount>\\w+)",
    )
    private val noBlessingPattern by patternGroup.pattern(
        "noblessings.colorless",
        "No Buffs active\\. Find them by exploring the Dungeon!",
    )

    /**
     * REGEX-TEST: [319] Empa_ α (Mage XXXIV)
     * REGEX-TEST: [393] [YOUTUBE] Remittal Σ♲ (Mage XL)
     * REGEX-TEST: [273] Ovi_1 Ӄ (Mage XXXVI)
     * REGEX-TEST: [273] Ovi_1 Ӄ (DEAD)
     */
    @Suppress("MaxLineLength")
    val playerDungeonTeamPattern by patternGroup.pattern(
        "tablist.playerteam.colorless",
        "^(?<sbLevel>\\[\\d+]) (?<rank>\\[[^]]+])? ?(?<playerName>\\S+)\\s?(?<symbols>[^(]*) \\((?:(?<className>\\S+) (?<classLevel>[CLXVI0]+)|(?<playerDead>DEAD))\\)\$",
    )

    /**
     * REGEX-TEST: Boss Collections
     */
    val bossCollectionsInventoryPattern by patternGroup.pattern(
        "boss.collections.inventory",
        "Boss Collections",
    )

    enum class DungeonBlessings(var power: Int) {
        LIFE(0),
        POWER(0),
        STONE(0),
        WISDOM(0),
        TIME(0);

        val displayName = name.firstLetterUppercase()

        companion object {
            fun reset() {
                entries.forEach { it.power = 0 }
            }
        }
    }

    fun inDungeon(): Boolean = IslandType.CATACOMBS.isInIsland()

    fun isOneOf(vararg floors: String): Boolean = dungeonFloor?.equalsOneOf(*floors) == true

    fun handleBossMessage(rawMessage: String) {
        if (!inDungeon()) return
        val message = rawMessage.removeColor()
        val bossName = message.substringAfter("[BOSS] ").substringBefore(":").trim()
        if ((bossName != "The Watcher") && dungeonFloor != null && checkBossName(bossName) && !inBossRoom) {
            DungeonBossRoomEnterEvent.post()
            inBossRoom = true
        }
    }

    private fun checkBossName(bossName: String): Boolean {
        val correctBoss = when (dungeonFloor) {
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

    fun getCurrentBoss(): DungeonFloor? {
        val floor = dungeonFloor ?: return null
        return DungeonFloor.valueOf(floor.replace("M", "F"))
    }

    private const val WATER_ROOM_ID = "-60,-60"
    val inWaterRoom: Boolean get() = roomId == WATER_ROOM_ID

    @Deprecated("Use getLevelComponent instead for better formatting", replaceWith = ReplaceWith("getLevelComponent(level)"))
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

    fun getLevelComponent(level: Int): Component {
        val formatting: ChatFormatting = when {
            level >= 45 -> RED
            level >= 40 -> GOLD
            level >= 35 -> DARK_PURPLE
            level >= 30 -> BLUE
            level >= 25 -> AQUA
            level >= 20 -> DARK_GREEN
            level >= 15 -> GREEN
            level >= 10 -> YELLOW
            level >= 5 -> WHITE
            else -> GRAY
        }
        return componentBuilder {
            append("$level") {
                withColor(formatting)
                if (level >= 50) bold = true
            }
        }
    }

    @HandleEvent
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        val cleanAdded = event.added.map { it.removeColor() }
        // TODO: move this under inDungeon check when we use Hypixel's ModAPI for island detection
        floorPattern.firstMatcher(cleanAdded) {
            val floor = group("floor")
            if (dungeonFloor == floor) return
            dungeonFloor = floor
            DungeonEnterEvent(floor).post()
            return
        }
        if (!inDungeon()) return
        dungeonRoomPattern.firstMatcher(cleanAdded) {
            roomId = group("roomId")
            return
        }
        timePattern.firstMatcher(cleanAdded) {
            time = "${groupOrNull("minutes") ?: "00"}:${group("seconds")}"
            return
        }
    }

    @HandleEvent
    fun onTablistChange(event: TabListUpdateEvent) {
        if (!inDungeon()) return
        if (dungeonFloor == null || playerClass != null) return

        val playerTeam = event.tabList.find { it.string.contains(PlayerUtils.getName()) }?.string ?: return
        for (dungeonClass in DungeonClass.entries) {
            if (playerTeam.contains("(${dungeonClass.displayName} ")) {
                val level = playerTeam.split(" ").last().trimEnd(')').romanToDecimalIfNecessary()
                playerClass = dungeonClass
                playerClassLevel = level
                return
            }
        }
    }

    @HandleEvent
    fun onTabUpdate(event: TablistFooterUpdateEvent) {
        if (!inDungeon()) return
        for (line in event.footer) {
            if (noBlessingPattern.matches(line)) {
                DungeonBlessings.reset()
                return
            }
            val matcher = blessingPattern.matcher(line.string)
            if (matcher.find()) {
                val type = matcher.group("type") ?: continue
                val amount = matcher.group("amount").romanToDecimalIfNecessary()
                if (DungeonBlessings.valueOf(type.uppercase()).power != amount) {
                    DungeonBlessings.valueOf(type.uppercase()).power = amount
                }
            }
        }

    }

    @HandleEvent
    fun onWorldChange() {
        dungeonFloor = null
        started = false
        inBossRoom = false
        isUniqueClass = false
        playerClass = null
        playerClassLevel = -1
        completed = false
        playerTeamClasses.clear()
        time = ""
        roomId = null
        DungeonBlessings.reset()
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onChat(event: SkyHanniChatEvent.Allow) {
        val floor = dungeonFloor ?: return
        if (event.message == "§e[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon.") {
            started = true
            DungeonStartEvent(floor).post()
        }
        if (uniqueClassBonus.matches(event.cleanMessage)) {
            isUniqueClass = true
        }

        killPattern.matchMatcher(event.cleanMessage) {
            val bossCollections = bossStorage ?: return
            val boss = DungeonFloor.byBossName(group("boss"))
            if (matches() && boss != null && boss !in bossCollections) {
                bossCollections.addOrPut(boss, 1)
            }
            return
        }
        dungeonComplete.matchMatcher(event.cleanMessage) {
            completed = true
            DungeonCompleteEvent(floor).post()
            return
        }
    }

    // This returns a map of boss name to the integer for the amount of kills the user has in the collection
    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val bossCollections = bossStorage ?: return

        if (bossCollectionsInventoryPattern.matches(event.inventoryName)) {
            readAllCollections(bossCollections, event.inventoryItems)
        } else if (event.inventoryName.endsWith(" Collection")) {
            readOneMaxCollection(bossCollections, event.inventoryItems, event.inventoryName)
        }
    }

    private fun readOneMaxCollection(
        bossCollections: MutableMap<DungeonFloor, Int>,
        inventoryItems: Map<Int, SafeItemStack>,
        inventoryName: String,
    ) {
        inventoryItems[48]?.let { item ->
            if (item.cleanName == "Go Back") {
                item.getCleanLore().getOrNull(0)?.let { firstLine ->
                    if (firstLine == "To Boss Collections") {
                        val name = inventoryName.split(" ").dropLast(1).joinToString(" ")
                        val floor = DungeonFloor.byBossName(name) ?: return
                        val lore = inventoryItems[4]?.getCleanLore() ?: return
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

    private fun readAllCollections(
        bossCollections: MutableMap<DungeonFloor, Int>,
        inventoryItems: Map<Int, SafeItemStack>,
    ) {
        nextItem@ for (stack in inventoryItems.values) {
            var name = ""
            var kills = 0
            nextLine@ for (line in stack.getLoreComponent()) {
                val colorlessLine = line.string.removeColor()
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

    @HandleEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Dungeon")

        if (!inDungeon()) {
            event.addIrrelevant("not in dungeons")
            return
        }

        event.addData {
            add("dungeonFloor: $dungeonFloor")
            add("started: $started")
            add("getRoomID: $roomId")
            add("time: $time")
            add("inBossRoom: $inBossRoom")
            add("")
            add("playerClass: $playerClass")
            add("isUniqueClass: $isUniqueClass")
            add("playerClassLevel: $playerClassLevel")
            add("")
            add("Blessings: ")
            DungeonBlessings.entries.forEach {
                add("  ${it.displayName} ${it.power}")
            }
        }
    }

    enum class DungeonClass(val displayName: String) {
        ARCHER("Archer"),
        BERSERK("Berserk"),
        HEALER("Healer"),
        MAGE("Mage"),
        TANK("Tank"),
        ;

        companion object {
            fun getByClassName(className: String): DungeonClass? {
                return DungeonClass.entries.firstOrNull { it.displayName.equals(className, ignoreCase = true) }
            }
        }
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

    @HandleEvent(onlyOnIsland = IslandType.CATACOMBS)
    fun onBlockClick(event: BlockClickEvent) {
        if (event.clickType != InteractClickType.RIGHT_CLICK) return

        val position = event.position
        val blockType: ClickedBlockType = when (position.getBlockAt()) {
            Blocks.CHEST -> ClickedBlockType.CHEST
            Blocks.TRAPPED_CHEST -> ClickedBlockType.TRAPPED_CHEST
            Blocks.LEVER -> ClickedBlockType.LEVER
            Blocks.PLAYER_HEAD -> {
                val blockTexture = BlockUtils.getTextureFromSkull(position)
                if (WITHER_ESSENCE_TEXTURE != null && blockTexture == WITHER_ESSENCE_TEXTURE) {
                    ClickedBlockType.WITHER_ESSENCE
                } else {
                    return
                }
            }

            else -> return
        }
        DungeonBlockClickEvent(position, blockType).post()
    }

    data class TeamMember(
        val username: String,
        var dungeonClass: DungeonClass? = null,
        var classLevel: Int = 0,
        var playerDead: Boolean = false,
    )

    private val playerTeamClasses: MutableList<TeamMember> = mutableListOf()

    fun getPlayerInfo(username: String): TeamMember =
        playerTeamClasses.find { it.username == username.removeColor() } ?: TeamMember(username)

    @HandleEvent
    fun onTabUpdate(event: TabListUpdateEvent) {
        if (!inDungeon() || !started || completed) return

        playerDungeonTeamPattern.matchAllComponents(event.tabList) {
            val username = group("playerName").removeColor()
            val playerDead = group("playerDead") == "DEAD"

            val dungeonClassName = group("className")
            val dungeonClassLevel = group("classLevel")

            playerTeamClasses.find { teamClass -> teamClass.username == username }?.let { player ->
                player.playerDead = playerDead
                if (player.dungeonClass == null && !playerDead) {
                    player.dungeonClass = DungeonClass.getByClassName(dungeonClassName)
                    player.classLevel = dungeonClassLevel.romanToDecimalIfNecessary()
                }
            } ?: run {
                val dungeonClass = DungeonClass.getByClassName(dungeonClassName)
                val classLevel = dungeonClassLevel.romanToDecimalIfNecessary()

                playerTeamClasses.add(
                    TeamMember(
                        username = username,
                        dungeonClass = dungeonClass,
                        classLevel = classLevel,
                        playerDead = playerDead,
                    ),
                )
            }
        }
    }
}
