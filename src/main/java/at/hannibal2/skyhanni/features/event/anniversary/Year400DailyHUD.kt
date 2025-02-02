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
    private val preChatPattern by patternGroup.pattern(
        "chat",
        "§6§lDAILY TASK! §eYou completed the (?<task>.*) §edaily task and earned §b\\+1 Raffle Ticket §eand a slice of cake!",
    )

    private val scroll = ScrollValue()

    override fun createDisplay(data: Set<Task>): Renderable = Renderable.scrollList(
        data.sorted().map { it.render },
        225,
        velocity = 4.0,
        scrollValue = scroll,
        showScrollableTipsInList = true,
    )

    override fun preItemFilter(slot: Int, stack: ItemStack): Pair<String, Boolean>? {
        val lore = stack.getLore()
        if (lore.isEmpty()) return null
        val complete = !lore.any { incompletePattern.matches(it) }
        val name = stack.displayName.removeColor().uppercase().replace("[ \\-!&'?]".toRegex(), "_").trim()
        println(name)
        return name to complete
    }

    override fun chatFilter(msg: String): String? = preChatPattern.matchGroup(msg, "task")

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
        PARTY_TIME_,
        ZOMBIE_KILLER,
        SPIDER_KILLER,
        SKELETON_KILLER,
        ENDERMAN_KILLER,
        SLAYER_APPRENTICE,
        EXTERMINATOR,
        RANGER,
        CATCH_OF_THE_DAY,
        EXPERIMENTAL,
        DUNGEONEER,
        DWARVEN_MINES_COMMISSIONER,
        FARMHAND,
        TREASURE_HUNTER,
        ENCHANTER,
        REFORGER,
        DETECTIVE,
        KA_CHING_,
        CHING_KA_,
        ALTERNATE_DIMENSION,
        TIME_TO_CELEBRATE_,
        GO_BACK,
        NEXT_PAGE,
        UPRISING,
        SHOOTING_THE_STARS,
        FIRE_AWAY,
        RANGER_II,
        SLAYER_INTERMEDIATE,
        NOT_SO_SUPREME,
        SPIDER_QUEEN,
        LOST___FOUND,
        FLARING_UP,
        WHO_S_THE_ALPHA_NOW_,
        GOBLIN_SUMMONER,
        GEM_BUGS,
        KUUDRA_KILLER,
        CRYSTAL_HOLLOWS_COMMISSIONER,
        RUN_THE_NUCLEUS,
        FISHERMAN,
        CATCH_OF_THE_WEEK,
        PODIUM_PLACE,
        CHEST_OPENER,
        DUNGEON_LOOTER,
        WITCH_IN_TRAINING,
        FINAL_CUT,
        A_NEW_SHERIFF_IN_TOWN,
        DUKE_IT_OUT,
        ASHES_TO_ASHES,
        BEAT_THE_BOSS,
        MOBBED,
        DRAGON_DOWN_,
        RANGER_III,
        PROTECTOR_OF_WHAT_,
        SLAYER_MASTER,
        AN_EYE_FOR_AN_EYE,
        VANQUISHED,
        SHINY_GOBLIN_SUMMONER,
        FROZEN_MINESHAFT_DELVER,
        DUNGEONEER_II,
        GLACITE_TUNNELS_COMMISSIONER,
        THERE_S_GOLD_IN_THEM_THERE_SEAS,
        CATCH_OF_THE_MONTH,
        CLIMBING_THE_PODIUM,
        COLD_LOOT,
        DUNGEON_LOOTER_II,
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
