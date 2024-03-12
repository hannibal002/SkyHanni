package at.hannibal2.skyhanni.data.model

import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.TabWidgetUpdate
import at.hannibal2.skyhanni.utils.CollectionUtils.editCopy
import at.hannibal2.skyhanni.utils.CollectionUtils.getOrNull
import at.hannibal2.skyhanni.utils.ConditionalUtils.transformIf
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import io.github.moulberry.moulconfig.observer.Property
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

enum class TabWidget(
    pattern0: String
) {
    PLAYER_LIST("(?:§.)*Players (?:§.)*\\(\\d+\\)"),
    INFO("(?:§.)*Info"),
    AREA("(?:§.)*(Area|Dungeon): (?:§.)*(?<island>.*)"),
    SERVER(
        "Server: (?:§.)*(?<server>.*)"
    ),
    GEMS("Gems: (?:§.)*(?<gems>.*)"),
    FAIRY_SOULS("Fairy Souls: (?:§.)*(?<got>\\d+)(?:§.)*\\/(?:§.)*(?<max>\\d+)"),
    PROFILE("(?:§.)*Profile: (?:§.)*(?<profile>\\S+) .*"),
    SB_LEVEL(
        "SB Level(?:§.)*: (?:§.)*\\[(?:§.)*(?<level>\\d+)(?:§.)*\\] (?:§.)*(?<xp>\\d+).*"
    ),
    BANK("Bank: (?:§.)*(?<amount>[^§]+)(?:\\(?:§.)*(?:\\/(?:§.)*(?<personal>.*))?"),
    INTEREST("Interest: (?:§.)*(?<time>[^§]+)(?:§.)* \\((?<amount>[^)]+)\\)"),
    SOULFLOW("Soulflow: (?:§.)*(?<amount>.*)"),
    PET(
        "(?:§.)*Pet:"
    ),
    PET_TRANING("(?:§.)*Pet Training:"),

    PET_SITTER("Kat: .*"),
    FIRE_SALE("(?:§.)*Fire Sales: .*"),
    ELECTION("(?:§.)*Election: (?:§.)*(?<time>.*)"),
    EVENT("(?:§.)*Event: (?:§.)*(?<event>.*)"),
    SKILLS(
        "(?:§.)*Skills: ?(?<avg>.*)"
    ),
    STATS("(?:§.)*Stats:"),
    GUESTS("(?:§.)*Guests (?:§.)*.*"),
    COOP("(?:§.)*Coop (?:§.)*.*"),
    MINION("(?:§.)*Minions; (?:§.)*(?<used>\\d+)(?:§.)*\\/(?:§.)*(?<max>\\d+)"),
    JERRY_ISLAND_CLOSING("Island closes in: (?:§.)*(?<time>.*)"),
    NORTH_STARS("North Stars: (?:§.)*(?<amount>)"),
    COLLECTION("(?:§.)*Collection:"),
    JACOB_CONTEST("(?:§.)*Jacob's Contest: .*"),
    SLAYER("(?:§.)*Slayer:"),
    DAILY_QUESTS("(?:§.)*Daily Quests:"),
    ACTIVE_EFFECTS("(?:§.)*Active Effects: (?:§.)*\\((?<amount>\\d+)\\)"),
    BESTIARY("(?:§.)*Bestiary:"),
    ESSENCE("(?:§.)*Essence:.*"),
    FORGE("(?:§.)*Forges:"),
    TIMERS("(?:§.)*Timers:"),
    DUNGEON_STATS("(?:§.)*Dungeons:"),
    PARTY("(?:§.)*Party:.*"),
    TRAPPER("(?:§.)*Trapper:"),
    COMMISSIONS("(?:§.)*lCommissions:"),
    POWDER("(?:§.)*Powders:"),
    CRYSTAL("(?:§.)*Crystals:"),
    UNCLAIMED_CHESTS("Unclaimed chests: (?:§.)*(?<amount>\\d+)"),
    RAIN("(?<type>Thunder|Rain): (?:§.)*(?<time>.*)"),
    BROODMOTHER("Broodmother: (?:§.)*(?<time>.*)"),
    EYES_PLACED("Eyes placed: (?:§.)*(?<amount>\\d).*|(?:§.)*Dragon spawned!|(?:§.)*Egg respawning!"),
    PROTECTOR("Protector: (?:§.)*(?<time>.*)"),
    DRAGON("(?:§.)*Dragon: (?:§.)*\\((?<type>[^)])\\)"),
    VOLCANO("Volcano: (?:§.)*(?<time>.*)"),
    REPUTATION("(?:§.)*(Barbarian|Mage) Reputation:"),
    FACTION_QUESTS("(?:§.)*Faction Quests:"),
    TROPHY_FISH("(?:§.)*Trophy Fish:"),
    RIFT_INFO("(?:§.)*Good to know:"),
    RIFT_SHEN("(?:§.)*Shen: (?:§.)*\\((?<time>[^)])\\)"),
    RIFT_BARRY("(?:§.)*Advertisement:"),

    ;

    val pattern by RepoPattern.pattern("tab.widget.${name.replace("_", ".").lowercase()}", "\\s*$pattern0")

    fun postEvent(lines: List<String>) = TabWidgetUpdate(this, lines).postAndCatch()

    fun isEventForThis(event: TabWidgetUpdate) = event.widget == this

    var boundary = -1 to -1

    /** Do not get the value inside of [TabWidgetUpdate] since it will be wrong*/
    val isActive: Property<Boolean> = Property.of(false)

    private var activeAfterCheck = false

    private fun updateIsActive() {
        if (isActive.get() == activeAfterCheck) return
        isActive.set(activeAfterCheck)
    }

    companion object {

        private val separatorIndexes = mutableListOf<Pair<Int, TabWidget?>>()

        init {
            entries.forEach { it.pattern }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        fun onTabListUpdate(event: TabListUpdateEvent) {
            val tabList = filterTabList(event.tabList)

            separatorIndexes.forEach {
                it.second?.activeAfterCheck = false
            }
            separatorIndexes.clear()
            for ((index, line) in tabList.withIndex()) {
                val match = entries.firstOrNull { it.pattern.matches(line) } ?: continue
                separatorIndexes.add(index to match)
            }
            separatorIndexes.add(tabList.size to null)
            separatorIndexes.zipWithNext { (firstIndex, widget), (secondIndex, _) ->
                widget?.boundary = firstIndex to secondIndex - 1
                widget?.activeAfterCheck = true
                widget?.postEvent(tabList.subList(firstIndex, secondIndex).filter { it.isNotEmpty() })
            }
            entries.forEach { it.updateIsActive() }
        }

        private fun filterTabList(tabList: List<String>): List<String> {
            var playerListFound = false
            var infoFound = false

            val headers = generateSequence(0) { it + 20 }.take(4).map { it to tabList.getOrNull(it) }

            val removeIndexes = mutableListOf<Int>()

            for ((index, header) in headers) {
                when {
                    PLAYER_LIST.pattern.matches(header) -> if (playerListFound) removeIndexes.add(index - removeIndexes.size) else playerListFound =
                        true

                    INFO.pattern.matches(header) -> if (infoFound) removeIndexes.add(index - removeIndexes.size) else infoFound =
                        true
                }
            }

            return tabList.transformIf({ size > 81 }, { dropLast(size - 80) }).editCopy {
                removeIndexes.forEach {
                    removeAt(it)
                }
            }
        }
    }
}
