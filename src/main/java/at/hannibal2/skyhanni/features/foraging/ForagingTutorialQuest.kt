package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.EntityUtils.cleanName
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatchers
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ForagingTutorialQuest {

    private val config get() = SkyHanniMod.feature.foraging.tutorialQuest

    private var lastParkWarpAttempt = SimpleTimeMark.farPast()
    private var lastSuggestion = SimpleTimeMark.farPast()

    private val patternGroup = RepoPattern.group("foraging.tutorial.quest")

    /**
     * REGEX-TEST: You don't have the requirements to use this warp!
     * REGEX-TEST: You haven't unlocked this fast travel destination!
     */
    private val lockedPattern by patternGroup.list(
        "warp.locked.list",
        "You don't have the requirements to use this warp!",
        "You haven't unlocked this fast travel destination!",
    )

    /**
     * REGEX-TEST: You must complete the Into the Woods Quest to use this!
     */
    private val needQuestPattern by patternGroup.pattern(
        "need-quest",
        "You must complete the (?<quest>.+) Quest to use this!",
    )

    /**
     * REGEX-TEST: Requires Into the Woods Quest
     */
    private val requiresQuestPattern by patternGroup.pattern(
        "requires-quest",
        "Requires (?<quest>.+) Quest",
    )

    private enum class Quest(val questName: String, val npcName: String, val npcLocation: LorenzVec) {
        FIRST("Foraging Tutorial", "Lumber Jack", LorenzVec(-123.5, 74.0, -30.0)),
        SECOND("Into the Woods", "Charlie", LorenzVec(-275.9, 80.0, -17.1)),
        THIRD("A Helping Hand", "Kelly", LorenzVec(-350.8, 94.0, 31.7)),
        FOURTH("The Campfire Cult", "Ryan", LorenzVec(-362.7, 102.0, -90.5)),
        FIFTH("The Rebuild", "Melody", LorenzVec(-412.3, 109.0, 70.2)),
    }

    @Suppress("MaxLineLength")
    private enum class NextQuest(val nextPortal: LorenzVec, val endingMessage: Pattern) {
        SECOND(
            LorenzVec(-312.1, 81.0, -9.0),
            "\\[NPC] Charlie: I wanted Kelly to get some Spruce Logs for us today, but I've not seen her in a while\\.\\.\\. Can you maybe look for her in the Spruce Woods\\?".toPattern(),
        ),
        THIRD(
            LorenzVec(-361.2, 90.0, -14.8),
            "\\[NPC] Kelly: I've heard some people are holding a Cult Meeting there RIGHT NOW!".toPattern(),
        ),
        FOURTH_LAST(
            LorenzVec(-397.3, 98.0, -37.5),
            "\\[NPC] Ryan: But be careful, though, as each trial burns a little hotter than the last!".toPattern(),
        ),
        FIFTH(LorenzVec(-436.4, 110.5, -14.4), "\\[NPC] Melody ♫: Talk to me again if you ever want to give my Harp a try!".toPattern()),
        SIXTH_FIRST(
            LorenzVec(-435.5, 110.0, -13.5),
            "\\[NPC] Molbert: It will take me some time to assemble them, so you should come back later\\.".toPattern(),
        ),
        SIXTH_SECOND(LorenzVec(-466.8, 120.0, -41.6), " +Talk to Molbert".toPattern()),
        SIXTH_THIRD(
            LorenzVec(-465.9, 119.0, -53.8),
            "\\[NPC] Molbert: The traps are ready for use; All that remains is to set them up in the right place\\. Once you find the ideal spots, go ahead and deploy them\\.".toPattern(),
        ),
        SIXTH_FOURTH(LorenzVec(-448.8, 120.0, -64.3), "Placed trap \\(1/3\\)".toPattern()),
        SIXTH_FIFTH(LorenzVec(-439.8, 122.0, -91.3), "Placed trap \\(2/3\\)".toPattern()),
        SIXTH_SIXTH(LorenzVec(-466.8, 120.0, -43.4), "Placed trap \\(3/3\\)".toPattern()),
        SIXTH_SEVENTH(
            LorenzVec(-435.5, 110.0, -13.5),
            "\\[NPC] Molbert: This might take some time, so you should come back later\\.".toPattern(),
        ),
        SEVEN(
            LorenzVec(-485.3, 116.5, -40.7),
            "\\[NPC] Molbert: I hope you forgive me after this and we can still be friends\\.".toPattern(),
        ),
    }

    @HandleEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        val message = event.message
        if (message.lowercase() == "/warp park") {
            lastParkWarpAttempt = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        lockedPattern.matchMatchers(event.cleanMessage) {
            if (lastParkWarpAttempt.passedSince() < 1.seconds) {
                EntityMovementData.onNextTeleport(IslandType.HUB) {
                    start(Quest.FIRST)
                }
            }
            return
        }
        if (IslandType.HUB.isInIsland() || IslandType.THE_PARK.isInIsland()) {
            needQuestPattern.matchMatcher(event.cleanMessage) {
                stepByName(group("quest"))
                return
            }
        }
        for (step in NextQuest.entries) {
            step.endingMessage.matchMatcher(event.cleanMessage) {
                goToNext(step)
                return
            }
        }
    }

    private fun stepByName(quest: String) {
        for (step in Quest.entries) {
            if (step.questName == quest) {
                start(step)
            }
        }
    }

    @HandleEvent
    fun onPlayerSpawn(event: MobEvent.Spawn.DisplayNpc) {
        requiresQuestPattern.matchMatcher(event.mob.baseEntity.cleanName()) {
            stepByName(group("quest"))
        }
    }

    private fun goToNext(quest: NextQuest) {
        if (!isEnabled()) return
        ChatUtils.chat("Go to next phase!")
        IslandGraphs.pathFind(
            quest.nextPortal,
            "Next Quest",
            condition = ::isEnabled,
        )
    }

    private fun start(step: Quest) {
        if (!isEnabled()) {
            suggest(step)
            return
        }
        ChatUtils.chat("Go to ${step.npcName} and start the ${step.questName} quest!")
        IslandGraphs.pathFind(
            step.npcLocation,
            step.npcName,
            condition = ::isEnabled,
        )
    }

    private fun suggest(step: Quest) {
        if (!config.suggestToEnable) return
        if (lastSuggestion.passedSince() < 10.minutes) return

        lastSuggestion = SimpleTimeMark.now()
        ChatUtils.clickableChat(
            "Do you want to have help solving the Foraging Tutorial Quest? Click here!",
            onClick = {
                config.enabled = true
                start(step)
            },
        )
        ChatUtils.clickableChat(
            "Never see this suggestion again? Click here!",
            onClick = {
                ChatUtils.chat("Disabled Foraging Tutorial Quest Suggestions.")
                config.suggestToEnable = false
            },
        )
    }

    private fun isEnabled() = config.enabled
}
