package at.hannibal2.skyhanni.features.foraging

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.EntityMovementData
import at.hannibal2.skyhanni.data.IslandGraphs
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.hypixel.chat.event.SystemMessageEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.events.MobEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object ForagingTutorialQuest {
    private val config get() = SkyHanniMod.feature.foraging.tutorialQuest

    private var lastParkWarpAttempt = SimpleTimeMark.farPast()
    private var lastSuggestion = SimpleTimeMark.farPast()

    private enum class Quest(val questName: String, val npcName: String, val npcLocation: LorenzVec) {
        FIRST("Foraging Tutorial", "Lumber Jack", LorenzVec(-112.2, 73.0, -36.9)),
        SECOND("Into the Woods", "Charlie", LorenzVec(-275.9, 80.0, -17.1)),
        THIRD("A Helping Hand", "Kelly", LorenzVec(-350.8, 94.0, 31.7)),
        FOURTH("The Campfire Cult", "Ryan", LorenzVec(-362.7, 102.0, -90.5)),
        FIFTH("The Rebuild", "Melody", LorenzVec(-412.3, 109.0, 70.2)),
    }

    private enum class NextQuest(val nextPortal: LorenzVec, val endingMessage: String) {
        SECOND(LorenzVec(-312.1, 81.0, -9.0), "Can you maybe look for her in the §aSpruce Woods§f?"),
        THIRD(LorenzVec(-361.2, 90.0, -14.8), "§cCult Meeting §fthere §c§lRIGHT NOW§f!"),
        FOURTH_LAST(LorenzVec(-397.3, 98.0, -37.5), "§rYou completed each §6Trial of Fire§f! §aCongratulations!"),
        FIFTH(LorenzVec(-436.4, 110.5, -14.4), " §rTalk to me again if you ever want to give my §dHarp §fa try!"),
        SIXTH_FIRST(LorenzVec(-435.5, 110.0, -13.5), "§r§fIt will take me some time to assemble them, so you should §acome back later§f."),
        SIXTH_SECOND(LorenzVec(-466.8, 120.0, -41.6), "  §r§fTalk to Molbert"),
        SIXTH_THIRD(LorenzVec(-465.9, 119.0, -53.8), "Once you find the ideal spots, go ahead and deploy them."),
        SIXTH_FOURTH(LorenzVec(-448.8, 120.0, -64.3), "§aPlaced trap §r§7(§r§e1§r§7/§r§a3§r§7)"),
        SIXTH_FIFTH(LorenzVec(-439.8, 122.0, -91.3), "LorenzVec(-448.8, 120.0, -64.3)"),
        SIXTH_SIXTH(LorenzVec(-466.8, 120.0, -43.4), "§aPlaced trap §r§7(§r§a3§r§7/§r§a3§r§7)"),
        SIXTH_SEVENTH(LorenzVec(-435.5, 110.0, -13.5), "/§r§a3§r§7)"),
        SIXTH_8(LorenzVec(-450.5, 120.0, -64.9), "  §r§fTalk to Molbert"),
        SEVEN(LorenzVec(-485.3, 116.5, -40.7), " §rI hope you forgive me after this and we can still be §6friends§f."),
    }

    @HandleEvent
    fun onMessageSendToServer(event: MessageSendToServerEvent) {
        val message = event.message
        if (message.lowercase() == "/warp park") {
            lastParkWarpAttempt = SimpleTimeMark.now()
        }
    }

    @HandleEvent
    fun onChat(event: SystemMessageEvent) {
        if (event.message == "§cYou don't have the requirements to use this warp!") {
            if (lastParkWarpAttempt.passedSince() < 1.seconds) {
                EntityMovementData.onNextTeleport(IslandType.HUB) {
                    start(Quest.FIRST)
                }
            }
        }
        if (IslandType.HUB.isCurrent() || IslandType.THE_PARK.isCurrent()) {
            "§cYou must complete the §r§6(?<quest>.*) Quest §r§cto use this!".toPattern().matchMatcher(event.message) {
                stepByName(group("quest"))
            }
        }
        for (step in NextQuest.entries) {
            if (event.message.endsWith(step.endingMessage)) {
                goToNext(step)
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
        "§cRequires §6(?<quest>.*) Quest".toPattern().matchMatcher(event.mob.name) {
            stepByName(group("quest"))
        }
    }

    private fun goToNext(quest: NextQuest) {
        if (!isEnabled()) return
        ChatUtils.chat("Go to next phase!")
        IslandGraphs.pathFind(
            quest.nextPortal,
            "Next Quest",
            condition = { isEnabled() },
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
            condition = { isEnabled() },
        )
    }

    private fun suggest(step: Quest) {
        if (!config.suggestToEnable) return
        if (lastSuggestion.passedSince() < 10.minutes) return

        lastSuggestion = SimpleTimeMark.now()
        ChatUtils.clickableChat(
            "Do you want to have help solving the Foraging Tutorial Quest? Click here!",
            onClick = {
                if (PlatformUtils.IS_LEGACY) {
                    ChatUtils.chat("§cYou need to be on a modern version of Minecraft to use this feature!")
                    return@clickableChat
                }
                config.enabled = true
                start(step)
            },
        )
        ChatUtils.clickableChat(
            "Never see this suggestion again? Click heere!",
            onClick = {
                ChatUtils.chat("Disabled Foraging Tutorial Quest Suggestions.")
                config.suggestToEnable = false
            },
        )
    }

    private fun isEnabled() = config.enabled && !PlatformUtils.IS_LEGACY
}
