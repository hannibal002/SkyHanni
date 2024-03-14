package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.TabWidgetUpdate
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternManager
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.regex.Pattern
// This group / prefix is not allowed to be used anywhere else (tab.widget is allowed as prefix)
private val repoGroup = RepoPattern.group("tab.widget.enum")

enum class TabWidget(
    pattern0: String
) {
    PLAYER_LIST(
        // language=RegExp
        "(?:§.)*Players (?:§.)*\\(\\d+\\)"
    ),
    INFO(
        // language=RegExp
        "(?:§.)*Info"
    ),
    AREA(
        // language=RegExp
        "(?:§.)*(Area|Dungeon): (?:§.)*(?<island>.*)"
    ),
    SERVER(
        // language=RegExp
        "Server: (?:§.)*(?<server>.*)"
    ),
    GEMS(
        // language=RegExp
        "Gems: (?:§.)*(?<gems>.*)"
    ),
    FAIRY_SOULS(
        // language=RegExp
        "Fairy Souls: (?:§.)*(?<got>\\d+)(?:§.)*\\/(?:§.)*(?<max>\\d+)"
    ),
    PROFILE(
        // language=RegExp
        "(?:§.)*Profile: (?:§.)*(?<profile>\\S+) .*"
    ),
    SB_LEVEL(
        // language=RegExp
        "SB Level(?:§.)*: (?:§.)*\\[(?:§.)*(?<level>\\d+)(?:§.)*\\] (?:§.)*(?<xp>\\d+).*"
    ),
    BANK(
        // language=RegExp
        "Bank: (?:§.)*(?<amount>[^§]+)(?:(?:§.)* \\/ (?:§.)*(?<personal>.*))?"
    ),
    INTEREST(
        // language=RegExp
        "Interest: (?:§.)*(?<time>[^§]+)(?:§.)* \\((?<amount>[^)]+)\\)"
    ),
    SOULFLOW(
        // language=RegExp
        "Soulflow: (?:§.)*(?<amount>.*)"
    ),
    PET(
        // language=RegExp
        "(?:§.)*Pet:"
    ),
    PET_TRANING(
        // language=RegExp
        "(?:§.)*Pet Training:"
    ),
    PET_SITTER(
        // language=RegExp
        "Kat: .*"
    ),
    FIRE_SALE(
        // language=RegExp
        "(?:§.)*Fire Sales: .*"
    ),
    ELECTION(
        // language=RegExp
        "(?:§.)*Election: (?:§.)*(?<time>.*)"
    ),
    EVENT(
        // language=RegExp
        "(?:§.)*Event: (?:§.)*(?<event>.*)"
    ),
    SKILLS(
        // language=RegExp
        "(?:§.)*Skills: ?(?<avg>.*)"
    ),
    STATS(
        // language=RegExp
        "(?:§.)*Stats:"
    ),
    GUESTS(
        // language=RegExp
        "(?:§.)*Guests (?:§.)*.*"
    ),
    COOP(
        // language=RegExp
        "(?:§.)*Coop (?:§.)*.*"
    ),
    MINION(
        // language=RegExp
        "(?:§.)*Minions; (?:§.)*(?<used>\\d+)(?:§.)*\\/(?:§.)*(?<max>\\d+)"
    ),
    JERRY_ISLAND_CLOSING(
        // language=RegExp
        "Island closes in: (?:§.)*(?<time>.*)"
    ),
    NORTH_STARS(
        // language=RegExp
        "North Stars: (?:§.)*(?<amount>\\d+)"
    ),
    COLLECTION(
        // language=RegExp
        "(?:§.)*Collection:"
    ),
    JACOB_CONTEST(
        // language=RegExp
        "(?:§.)*Jacob's Contest:.*"
    ),
    SLAYER(
        // language=RegExp
        "(?:§.)*Slayer:"
    ),
    DAILY_QUESTS(
        // language=RegExp
        "(?:§.)*Daily Quests:"
    ),
    ACTIVE_EFFECTS(
        // language=RegExp
        "(?:§.)*Active Effects: (?:§.)*\\((?<amount>\\d+)\\)"
    ),
    BESTIARY(
        // language=RegExp
        "(?:§.)*Bestiary:"
    ),
    ESSENCE(
        // language=RegExp
        "(?:§.)*Essence:.*"
    ),
    FORGE(
        // language=RegExp
        "(?:§.)*Forges:"
    ),
    TIMERS(
        // language=RegExp
        "(?:§.)*Timers:"
    ),
    DUNGEON_STATS(
        // language=RegExp
        "(?:§.)*Dungeons:"
    ),
    PARTY(
        // language=RegExp
        "(?:§.)*Party:.*"
    ),
    TRAPPER(
        // language=RegExp
        "(?:§.)*Trapper:"
    ),
    COMMISSIONS(
        // language=RegExp
        "(?:§.)*lCommissions:"
    ),
    POWDER(
        // language=RegExp
        "(?:§.)*Powders:"
    ),
    CRYSTAL(
        // language=RegExp
        "(?:§.)*Crystals:"
    ),
    UNCLAIMED_CHESTS(
        // language=RegExp
        "Unclaimed chests: (?:§.)*(?<amount>\\d+)"
    ),
    RAIN(
        // language=RegExp
        "(?<type>Thunder|Rain): (?:§.)*(?<time>.*)"
    ),
    BROODMOTHER(
        // language=RegExp
        "Broodmother: (?:§.)*(?<time>.*)"
    ),
    EYES_PLACED(
        // language=RegExp
        "Eyes placed: (?:§.)*(?<amount>\\d).*|(?:§.)*Dragon spawned!|(?:§.)*Egg respawning!"
    ),
    PROTECTOR(
        // language=RegExp
        "Protector: (?:§.)*(?<time>.*)"
    ),
    DRAGON(
        // language=RegExp
        "(?:§.)*Dragon: (?:§.)*\\((?<type>[^)])\\)"
    ),
    VOLCANO(
        // language=RegExp
        "Volcano: (?:§.)*(?<time>.*)"
    ),
    REPUTATION(
        // language=RegExp
        "(?:§.)*(Barbarian|Mage) Reputation:"
    ),
    FACTION_QUESTS(
        // language=RegExp
        "(?:§.)*Faction Quests:"
    ),
    TROPHY_FISH(
        // language=RegExp
        "(?:§.)*Trophy Fish:"
    ),
    RIFT_INFO(
        // language=RegExp
        "(?:§.)*Good to know:"
    ),
    RIFT_SHEN(
        // language=RegExp
        "(?:§.)*Shen: (?:§.)*\\((?<time>[^)])\\)"
    ),
    RIFT_BARRY(
        // language=RegExp
        "(?:§.)*Advertisement:"
    ),
    COMPOSTER(
        // language=RegExp
        "(?:§.)*Composter:"
    ),
    GARDEN_LEVEL(
        // language=RegExp
        "Garden Level: (?:§.)*(?<level>.*)"
    ),
    COPPER(
        // language=RegExp
        "Copper: (?:§.)*(?<amount>\\d+)"
    ),
    PESTS(
        // language=RegExp
        "(?:§.)*Pests:"
    ),
    VISITORS(
        // language=RegExp
        "(?:§.)*Visitors: (?:§.)*\\((?<count>\\d+)\\)"
    ),
    CROP_MILESTONE(
        // language=RegExp
        "(?:§.)*Crop Milestones:"
    ),
    PRIVATE_ISLAND_CRYSTALS(
        // language=RegExp
        "Crystals: (?:§.)*(?<count>\\d+)"
    )

    ;

    val pattern by repoGroup.pattern(name.replace("_", ".").lowercase(), "\\s*$pattern0")

    var lines: List<String> = emptyList()
        private set

    /** Both are inclusive */
    var boundary = -1 to -1

    var isActive: Boolean = false
        private set

    fun isEventForThis(event: TabWidgetUpdate) = event.widget == this

    private var gotChecked = false

    private fun postNewEvent(lines: List<String>) {
        // Prevent Post if lines are equal
        if (lines == this.lines) return
        this.lines = lines
        TabWidgetUpdate.NewValues(this, lines).postAndCatch()
    }

    private fun postClearEvent() {
        lines = emptyList()
        TabWidgetUpdate.Clear(this)
    }

    private fun updateIsActive() {
        if (isActive == gotChecked) return
        isActive = gotChecked
        if (!gotChecked) {
            postClearEvent()
        }
    }

    companion object {

        private val separatorIndexes = mutableListOf<Pair<Int, TabWidget?>>()

        private var extraPatterns: List<Pattern> = emptyList()

        init {
            entries.forEach { it.pattern }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        fun onTabListUpdate(event: TabListUpdateEvent) {
            val tabList = filterTabList(event.tabList)

            separatorIndexes.forEach {
                it.second?.gotChecked = false
            }

            separatorIndexes.clear()

            for ((index, line) in tabList.withIndex()) {
                val match = entries.firstOrNull { it.pattern.matches(line) }
                    ?: if (extraPatterns.any { it.matches(line) }) null else continue
                separatorIndexes.add(index to match)
            }
            separatorIndexes.add(tabList.size to null)

            separatorIndexes.zipWithNext { (firstIndex, widget), (secondIndex, _) ->
                widget?.boundary = firstIndex to secondIndex - 1
                widget?.gotChecked = true
                widget?.postNewEvent(tabList.subList(firstIndex, secondIndex).filter { it.isNotEmpty() })
            }

            entries.forEach { it.updateIsActive() }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        fun onRepoReload(event: RepositoryReloadEvent) {
            extraPatterns = RepoPatternManager.getUnusedPatterns(repoGroup.prefix)
        }

        private fun filterTabList(tabList: List<String>): List<String> {
            var playerListFound = false
            var infoFound = false

            val headers = generateSequence(0) { it + 20 }.take(4).map { it to tabList.getOrNull(it) }

            val removeIndexes = mutableListOf<Int>()

            for ((index, header) in headers) when {
                PLAYER_LIST.pattern.matches(header) -> if (playerListFound) removeIndexes.add(index - removeIndexes.size) else playerListFound =
                    true

                INFO.pattern.matches(header) -> if (infoFound) removeIndexes.add(index - removeIndexes.size) else infoFound =
                    true
            }

            return tabList.transformIf({ size > 81 }, { dropLast(size - 80) }).editCopy {
                removeIndexes.forEach {
                    removeAt(it)
                }
            }
        }
    }
}
