package at.hannibal2.skyhanni.features.dungeon

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ClickType
import at.hannibal2.skyhanni.data.ClickedBlockType
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
import at.hannibal2.skyhanni.events.minecraft.WorldChangeEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.BlockUtils
import at.hannibal2.skyhanni.utils.BlockUtils.getBlockAt
import at.hannibal2.skyhanni.utils.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.groupOrNull
import at.hannibal2.skyhanni.utils.RegexUtils.matchAll
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack

@Suppress("MemberVisibilityCanBePrivate")
@SkyHanniModule
object DungeonApi {

    private val floorPattern = " §7⏣ §cThe Catacombs §7\\((?<floor>.*)\\)".toPattern()
    private val uniqueClassBonus = "^Your ([A-Za-z]+) stats are doubled because you are the only player using this class!$".toRegex()

    private val bossPattern = "View all your (?<name>\\w+) Collection".toPattern()
    private val levelPattern = " +(?<kills>\\d+).*".toPattern()
    private val killPattern = " +☠ Defeated (?<boss>\\w+).*".toPattern()
    private val totalKillsPattern = "§7Total Kills: §e(?<kills>.*)".toPattern()

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

    val bossStorage: MutableMap<DungeonFloor, Int>? get() = ProfileStorageData.profileSpecific?.dungeons?.bosses

    private val patternGroup = RepoPattern.group("dungeon")
    private val WITHER_ESSENCE_TEXTURE by lazy { SkullTextureHolder.getTexture("WITHER_ESSENCE") }

    /**
     * REGEX-TEST: Time Elapsed: §a01m 17s
     * REGEX-TEST: Time Elapsed: §a14s
     */
    private val timePattern by patternGroup.pattern(
        "time",
        "Time Elapsed: §.(?:(?<minutes>\\d+)m )?(?<seconds>\\d+)s",
    )

    /**
     * REGEX-TEST: §f                §r§cMaster Mode The Catacombs §r§8- §r§eFloor VII
     * REGEX-TEST: §f                         §r§cThe Catacombs §r§8- §r§eFloor V
     */
    private val dungeonComplete by patternGroup.pattern(
        "complete",
        "§.\\s+§.§.(?:Master Mode )?The Catacombs §.§.- §.§.(?:Floor )?(?<floor>M?[IV]{1,3}|Entrance)",
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
        "blessings",
        "§r§r§fBlessing of (?<type>\\w+) (?<amount>\\w+)§r",
    )
    private val noBlessingPattern by patternGroup.pattern(
        "noblessings",
        "§r§r§7No Buffs active\\. Find them by exploring the Dungeon!§r",
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

    @HandleEvent
    fun onScoreboardUpdate(event: ScoreboardUpdateEvent) {
        // TODO: move this under inDungeon check when we use Hypixel's ModAPI for island detection
        floorPattern.firstMatcher(event.added) {
            val floor = group("floor")
            if (dungeonFloor == floor) return
            dungeonFloor = floor
            DungeonEnterEvent(floor).post()
            return
        }
        if (!inDungeon()) return
        dungeonRoomPattern.firstMatcher(event.added) {
            roomId = group("roomId")
            return
        }
        timePattern.firstMatcher(event.added) {
            time = "${groupOrNull("minutes") ?: "00"}:${group("seconds")}"
            return
        }
    }

    @HandleEvent
    fun onTablistChange(event: TabListUpdateEvent) {
        if (!inDungeon()) return
        if (dungeonFloor == null || playerClass != null) return

        val playerTeam = event.tabList.find { it.contains(LorenzUtils.getPlayerName()) }?.removeColor() ?: return
        for (dungeonClass in DungeonClass.entries) {
            if (playerTeam.contains("(${dungeonClass.scoreboardName} ")) {
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
        for (line in event.footer.split("\n")) {
            if (noBlessingPattern.matches(line)) {
                DungeonBlessings.reset()
                return
            }
            val matcher = blessingPattern.matcher(line)
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
    fun onWorldChange(event: WorldChangeEvent) {
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
    fun onChat(event: SkyHanniChatEvent) {
        val floor = dungeonFloor ?: return
        if (event.message == "§e[NPC] §bMort§f: §rHere, I found this map when I first entered the dungeon.") {
            started = true
            DungeonStartEvent(floor).post()
        }
        if (event.message.removeColor().matches(uniqueClassBonus)) {
            isUniqueClass = true
        }

        killPattern.matchMatcher(event.message.removeColor()) {
            val bossCollections = bossStorage ?: return
            val boss = DungeonFloor.byBossName(group("boss"))
            if (matches() && boss != null && boss !in bossCollections) {
                bossCollections.addOrPut(boss, 1)
            }
            return
        }
        dungeonComplete.matchMatcher(event.message) {
            completed = true
            DungeonCompleteEvent(floor).post()
            return
        }
    }

    // This returns a map of boss name to the integer for the amount of kills the user has in the collection
    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        val bossCollections = bossStorage ?: return

        if (event.inventoryName == "Boss Collections") {
            readAllCollections(bossCollections, event.inventoryItems)
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

    private fun readAllCollections(
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

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
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

    enum class DungeonClass(val scoreboardName: String) {
        ARCHER("Archer"),
        BERSERK("Berserk"),
        HEALER("Healer"),
        MAGE("Mage"),
        TANK("Tank"),
        ;


        companion object {
            fun getByClassName(className: String): DungeonClass? {
                return DungeonClass.entries.firstOrNull { it.scoreboardName.equals(className, ignoreCase = true) }
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
        if (event.clickType != ClickType.RIGHT_CLICK) return

        val position = event.position
        val blockType: ClickedBlockType = when (position.getBlockAt()) {
            Blocks.chest -> ClickedBlockType.CHEST
            Blocks.trapped_chest -> ClickedBlockType.TRAPPED_CHEST
            Blocks.lever -> ClickedBlockType.LEVER
            Blocks.skull -> {
                val blockTexture = BlockUtils.getTextureFromSkull(position)
                if (blockTexture == WITHER_ESSENCE_TEXTURE) {
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
        val dungeonClass: DungeonClass? = null,
        val classLevel: Int = 0,
        val playerDead: Boolean = false
    )

    private val playerTeamClasses: MutableList<TeamMember> = mutableListOf()

    fun getPlayerInfo(username: String): TeamMember =
        playerTeamClasses.find { it.username == username.removeColor() } ?: TeamMember(username)

    /**
     * REGEX-TEST: §r§b§lParty §r§f(4), §8[§r§f67§r§8] §r§7Ovi_1 §r§f(§r§dBerserk IX§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§732§r§8] §r§7Linkthe3god §r§f(§r§dBerserk VIII§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§e92§r§8] §r§bEREZ313_ §r§f(§r§dBerserk XXIV§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§f49§r§8] §r§7SebiTud11 §r§f(§r§d§r§cDEAD§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , , , ,        §r§2§lPlayer Stats, §a§lDowned: §r§f§r§7SebiTud11,  Time: §r§e25s,  Revive: §r§c20s, , §a§lTeam Deaths: §r§f4,  Team Damage Dealt: §r§c4.4M❤,  Team Healing Done: §r§c1k❤,  Your Milestone: §r§e☠⓿,  §r§7(30.6% to ❶), , §a§lDiscoveries: §r§f2,  Secrets Found: §r§b2,  Crypts: §r§60, , , , , , ,        §r§3§lDungeon Stats, §b§lDungeon: §r§7Catacombs,  Opened Rooms: §r§518,  Completed Rooms: §r§d12,  Secrets Found: §r§e7.4%,  Time: §r§603m 25s, , §b§lPuzzles: §r§f(2),  Creeper Beams: §r§7[§r§6§l✦§r§7] ,  ???: §r§7[§r§6§l✦§r§7], , , , , , , , , , ,        §r§6§lAccount Info, §e§lProfile: §r§aMango,  Pet Sitter: §r§bN/A,  Bank: §r§624.7M,  Interest: §r§e29 Hours, , §e§lSkills: §r§aForaging 16: §r§354.4%,  Speed: §r§f✦203,  Strength: §r§c❁641,  Crit Chance: §r§9☣108,  Crit Damage: §r§9☠535,  Attack Speed: §r§e⚔0, , §e§lEvent: §r§6Spooky Festival,  Starts In: §r§e17h, , §e§lElection: §r§b3d,  §r§eDiaz: §r§e|||||§r§f||||| §r§f(49%),  §r§dAatrox: §r§d||||§r§f|||||| §r§f(35%),  §r§cPaul: §r§c|§r§f||||||||| §r§f(9%)
     * REGEX-TEST: §r§b§lParty §r§f(5), §8[§r§f67§r§8] §r§7Ovi_1 §r§f(§r§dBerserk IX§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§f53§r§8] §r§aChrisShady0207 §r§7♲ §r§f(§r§dArcher VII§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§e105§r§8] §r§aAMIRAMSD2 §r§f(§r§dBerserk XXIV§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§e86§r§8] §r§bSw0rddancer §r§7§l⚝ §r§f(§r§dTank XI§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c0, , §8[§r§e108§r§8] §r§bCDEClovlyboy §r§f(§r§dBerserk XVI§r§f),  Ultimate: §r§e§r§aReady,  Revive Stones: §r§c1,        §r§2§lPlayer Stats, §a§lDowned: §r§7NONE,  Time: §r§eN/A,  Revive: §r§cN/A, , §a§lTeam Deaths: §r§f0,  Team Damage Dealt: §r§c1.5M❤,  Team Healing Done: §r§c428❤,  Your Milestone: §r§e☠⓿,  §r§7(0% to ❶), , §a§lDiscoveries: §r§f2,  Secrets Found: §r§b2,  Crypts: §r§60, , , , , , ,        §r§3§lDungeon Stats, §b§lDungeon: §r§7Catacombs,  Opened Rooms: §r§59,  Completed Rooms: §r§d9,  Secrets Found: §r§e8.3%,  Time: §r§632s, , §b§lPuzzles: §r§f(2),  ???: §r§7[§r§6§l✦§r§7],  ???: §r§7[§r§6§l✦§r§7], , , , , , , , , , ,        §r§6§lAccount Info, §e§lProfile: §r§aMango,  Pet Sitter: §r§bN/A,  Bank: §r§624.7M,  Interest: §r§e28 Hours, , §e§lSkills: §r§aForaging 16: §r§354.4%,  Speed: §r§f✦195,  Strength: §r§c❁552,  Crit Chance: §r§9☣108,  Crit Damage: §r§9☠466,  Attack Speed: §r§e⚔0, , §e§lEvent: §r§6Spooky Festival,  Starts In: §r§e17h, , §e§lElection: §r§b3d,  §r§eDiaz: §r§e|||||§r§f||||| §r§f(48%),  §r§dAatrox: §r§d||||§r§f|||||| §r§f(35%),  §r§cPaul: §r§c|§r§f||||||||| §r§f(9%)
     */
    @Suppress("MaxLineLength")
    private val playerClassRegex by patternGroup.pattern(
        "dungeon.tablist.playerteam",
        "§8\\[§.§.\\d+§.§.] §.§.(?<username>\\w+)[^(\\]]*§r§f\\(§r§d(?:§.§.)?(?<class>Archer|Mage|Berserk|Tank|Healer|DEAD)(?:\\s*(?<level>[IVXLCDM0]+)?§r§f)?\\)"
    )

    @HandleEvent
    fun onTabUpdate(event: TabListUpdateEvent) {
        if (!inDungeon() || !started || completed) return

        val updatedTeamMembers = mutableListOf<TeamMember>()

        playerClassRegex.matchAll(event.tabList) {
            val username = group("username")
            val dungeonClassName = group("class")
            val classLevel = group("level")
            val playerDead = dungeonClassName == "DEAD"
            val oldPlayerData = getPlayerInfo(username)
            val dungeonClass = if (playerDead) oldPlayerData.dungeonClass
            else DungeonClass.getByClassName(dungeonClassName) ?: oldPlayerData.dungeonClass
            val dungeonClassLevel = if (playerDead) oldPlayerData.classLevel else classLevel.romanToDecimalIfNecessary()

            updatedTeamMembers.add(
                TeamMember(
                    username = username,
                    dungeonClass = dungeonClass,
                    classLevel = dungeonClassLevel,
                    playerDead = playerDead
                )
            )
        }

        playerTeamClasses.apply {
            clear()
            addAll(updatedTeamMembers)
        }
    }
}
