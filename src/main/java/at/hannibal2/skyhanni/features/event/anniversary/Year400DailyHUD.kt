package at.hannibal2.skyhanni.features.event.anniversary

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.HypixelJoinEvent
import at.hannibal2.skyhanni.features.gui.PlayerTask
import at.hannibal2.skyhanni.features.gui.TaskHud
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.RegexUtils.matchGroup
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SimpleTimeMark.Companion.asTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.ScrollValue
import at.hannibal2.skyhanni.utils.repopatterns.RepoPatternGroup
import io.github.notenoughupdates.moulconfig.observer.Property
import net.minecraft.item.ItemStack
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.regex.Pattern
import kotlin.time.toKotlinDuration
import java.time.Duration as JDuration

@SkyHanniModule
object Year400DailyHUD : TaskHud<Year400DailyHUD.Task, Pair<String, Boolean>>(
    "Year 400 daily hud",
    Task.entries.toSet(),
) {
    private val config = SkyHanniMod.feature.event.anniversaryCelebration400

    private val patternGroup = RepoPatternGroup("event.anniversary.four")

    override val configToggle: Property<Boolean> = config.dailyTasksHud
    override val position: Position = config.dailyTaskPosition
    override val storage: MutableSet<Task> get() = playerStorage.anniversary400Dailies

    private val playerStorage get() = ProfileStorageData.playerSpecific ?: PlayerSpecificStorage()

    private var lastLogin
        get() = playerStorage.lastLoginInAnniversary400
        set(value) {
            playerStorage.lastLoginInAnniversary400 = value
        }

    override val inventoryPattern: Pattern by patternGroup.pattern("inventory", "Daily Tasks")
    private val incompletePattern by patternGroup.pattern("incomplete", "§c§lINCOMPLETE")

    /**REGEX-TEST: §6§lDAILY TASK! §eYou completed the §eRun the Nucleus §edaily task and earned §b+1 Raffle Ticket §eand a slice of cake!
     * REGEX-TEST: §6§lDAILY TASK! §eYou completed the §aReforger §edaily task and earned §b+1 Raffle Ticket §eand a slice of cake!
     * REGEX-TEST: §6§lDAILY TASK! §eYou completed the §aAlternate Dimension §edaily task and earned §b+1 Raffle Ticket §eand a slice of cake!
     */
    private val preChatPattern by patternGroup.pattern(
        "chat",
        "§6§lDAILY TASK! §eYou completed the (?<task>.*) §edaily task and earned §b\\+1 Raffle Ticket §eand a slice of cake!",
    )

    private val endTime = SkyBlockTime.fromSBYear(401).asTimeMark()

    override fun isEnabled(): Boolean {
        return super.isEnabled() && endTime.isInFuture()
    }

    private val scroll = ScrollValue()

    override fun createDisplay(data: Set<Task>): Renderable =
        Renderable.verticalContainer(
            listOf(
                Renderable.string("§6Year 400 Daily"),
                Renderable.scrollList(
                    data.sorted().map { it.render },
                    225,
                    velocity = 4.0,
                    scrollValue = scroll,
                    showScrollableTipsInList = true,
                ),
            ),
        )

    override fun preItemFilter(slot: Int, stack: ItemStack): Pair<String, Boolean>? {
        val lore = stack.getLore()
        if (lore.isEmpty()) return null
        val complete = !lore.any { incompletePattern.matches(it) }
        val name = stack.displayName.makeNameSave()
        return name to complete
    }

    private fun String.makeNameSave() = removeColor().uppercase().replace("[ \\-!&'?]".toRegex(), "_").trim()

    override fun chatFilter(msg: String): String? = preChatPattern.matchGroup(msg, "task")?.makeNameSave()

    private var wasLoggedIn = false

    @HandleEvent
    fun onHypixelJoin(event: HypixelJoinEvent) {
        resetSchedule()
        if (wasLoggedIn) return
        wasLoggedIn = true

        val current = getDate()

        val last = lastLogin ?: run {
            lastLogin = current
        }

        if (current != last) {
            reset()
        }

    }

    // Reset Time is 0:00 EST
    private fun getDate(): LocalDate = ZonedDateTime.now(TimeUtils.ZoneOffset_MDT).toLocalDate()

    override fun resetTime(): SimpleTimeMark {
        val now = ZonedDateTime.now(TimeUtils.ZoneOffset_MDT)
        val ourNow = SimpleTimeMark.now()

        val resetAt = now.toLocalDate().plusDays(1L).atTime(0, 0)

        val diff = JDuration.between(
            now.toLocalDateTime(),
            resetAt,
        )

        val kDiff = diff.toKotlinDuration()

        return ourNow + kDiff
    }

    enum class Task(display: String? = null) : PlayerTask<Pair<String, Boolean>>, Comparable<Task> {
        ENCHANTER("Enchant an item"),
        REFORGER("Reforge an item"),
        KA_CHING_("Sell to NPC"),
        CHING_KA_("Buy from NPC"),

        PARTY_TIME_("Eat Century Cake"),
        TIME_TO_CELEBRATE_("Activate Time Tower"),
        EXPERIMENTAL("Superpairs"),
        WITCH_IN_TRAINING("Brew level 3 potion"),

        ZOMBIE_KILLER("Kill Zombie"),
        SPIDER_KILLER("Kill Spider"),
        SKELETON_KILLER("Kill Skeleton"),
        WHO_S_THE_ALPHA_NOW_("Kill Soul of Alpha"),
        SPIDER_QUEEN("Summon Arachne"),

        ALTERNATE_DIMENSION("Enter Rift"),
        NOT_SO_SUPREME("Kill a Leach Supreme"),

        RANGER("Hunt a §f§lTRACKABLE§r"),
        RANGER_II("Hunt a §9§lUNDETECTED§r"),
        RANGER_III("Hunt a §6§lELUSIVE§r"),
        EXTERMINATOR("Vacuum up a Pest"),
        FARMHAND("Complete one Visitor"),
        PODIUM_PLACE("§l§cBRONZE §r§eJacob Contest"),
        CLIMBING_THE_PODIUM("§l§fSILVER §r§eJacob Contest"),

        TREASURE_HUNTER("Fish Treasure"),
        CATCH_OF_THE_DAY("Catch Sea Creature"),
        CATCH_OF_THE_WEEK("§5§lEPIC §r§eSea Creature"),
        CATCH_OF_THE_MONTH("§6§lLEGENDARY §r§eSea Creature"),
        FISHERMAN("§l§fSILVER §e§rTrophy Fish"),
        THERE_S_GOLD_IN_THEM_THERE_SEAS("§l§6GOLD §e§rTrophy Fish"),

        DWARVEN_MINES_COMMISSIONER("Commission §2Dwarven Mines"),
        CRYSTAL_HOLLOWS_COMMISSIONER("Commission §5Crystal Hollows"),
        GLACITE_TUNNELS_COMMISSIONER("Commission §bGlacite Tunnels"),
        CHEST_OPENER("Open a Treasure Chest"),
        SHOOTING_THE_STARS("Kill Star Sentry"),
        GEM_BUGS("Spawn Thyst"),
        UPRISING("Kill Automaton"),
        MOBBED("Kill Corleone"),
        RUN_THE_NUCLEUS("Nucleus Run"),
        GOBLIN_SUMMONER("Spawn Golden Goblin"),
        SHINY_GOBLIN_SUMMONER("Spawn Diamond Goblin"),
        FROZEN_MINESHAFT_DELVER("Spawn Mineshaft"),
        COLD_LOOT("Loot Umber/Tungsten Corp"),

        ENDERMAN_KILLER("Kill Enderman"),
        AN_EYE_FOR_AN_EYE("Spawn Special Zealot"),
        DRAGON_DOWN_("Kill Dragon"),
        PROTECTOR_OF_WHAT_("Kill Endstone Protector"),

        SLAYER_APPRENTICE("Kill Tier I Slayer"),
        SLAYER_INTERMEDIATE("Kill Tier III Slayer"),
        SLAYER_MASTER("Kill Tier V Slayer"),

        DETECTIVE("Secret in Dungeon"),
        DUNGEONEER("Normal Dungeon"),
        DUNGEONEER_II("Master Dungeon"),
        LOST___FOUND("Kill Lost Adventurer"),
        DUNGEON_LOOTER("Emerald Dungeon Chest"),
        DUNGEON_LOOTER_II("Bedrock Dungeon Chest"),

        FIRE_AWAY("Bezal Killer"),
        FLARING_UP("Kill Flare"),
        VANQUISHED("Spawn Vanquisher"),
        DUKE_IT_OUT("Kill Barbarian Duke X"),
        A_NEW_SHERIFF_IN_TOWN("Kill Mage Outlaw"),
        FINAL_CUT("Kill Bladesoul"),
        BEAT_THE_BOSS("Kill Magma Boss"),
        ASHES_TO_ASHES("Kill Ashfang"),
        KUUDRA_KILLER("Complete any Kuudra"),
        ;

        val render = Renderable.string(display ?: (LorenzColor.YELLOW.getChatColor() + name.allLettersFirstUppercase()))

        override fun checkChat(msg: String): Boolean? = name == msg
        override fun isTaskDoneViaItem(input: Pair<String, Boolean>): Boolean? {
            if (name != input.first) return false
            if (input.second) return true
            return null
        }
    }
}
