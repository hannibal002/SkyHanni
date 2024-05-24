package at.hannibal2.skyhanni.features.skillprogress

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.SkillAPI
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.skillprogress.EliteSkillsDisplayConfig.SkillDisplay
import at.hannibal2.skyhanni.data.jsonobjects.other.EliteLeaderboard
import at.hannibal2.skyhanni.data.jsonobjects.other.EliteSkillGraphEntry
import at.hannibal2.skyhanni.data.jsonobjects.repo.EliteAPISettingsJson
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.SkillExpGainEvent
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.APIUtil
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.OSUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.toDashlessUUID
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.fromJson
import at.hannibal2.skyhanni.utils.renderables.Renderable
import kotlinx.coroutines.launch
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object SkillRankDisplay {

    private val config get() = SkyHanniMod.feature.skillProgress.rankDisplay

    private var checkDuration = 10.minutes

    private val eliteCollectionApiGson by lazy {
        ConfigManager.createBaseGsonBuilder()
            .create()
    }

    private var profileID: UUID? = null
    private val skillPlacements = mutableMapOf<String, Map<Int, Long>>()
    private val skillRanks = mutableMapOf<String, Int>()
    private var currentSkills = mutableMapOf<String, Long>()

    private var lastSkillGained: String?
        get() = SkyHanniMod.feature.storage.lastSkillObtained
        set(value) {
            SkyHanniMod.feature.storage.lastSkillObtained = value
        }

    private var lastSkillFetched: String? = null
    private var lastLeaderboardFetch = SimpleTimeMark.farPast()
    private var lastXPGained = SimpleTimeMark.farPast()
    private var hasSkillsBeenFetched = false

    private var display = emptyList<Renderable>()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<EliteAPISettingsJson>("EliteAPISettings")
        checkDuration = data.refreshTime.minutes
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (GardenAPI.hideExtraGuis()) return
        if (!isEnabled()) return
        if (!config.alwaysShow && lastXPGained.passedSince() > config.alwaysShowTime.seconds) return

        config.pos.renderRenderables(display, posLabel = "Skill Rank Display")
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.skill.afterChange {
            lastLeaderboardFetch = SimpleTimeMark.farPast()
        }
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return
        if (profileID == null) return

        if (lastLeaderboardFetch.passedSince() > checkDuration) {
            lastLeaderboardFetch = SimpleTimeMark.now()
            val skill = if (config.skill.get() == SkillDisplay.AUTO) {
                lastSkillGained ?: "carpentry"
            } else {
                config.skill.get().skill
            }

            SkyHanniMod.coroutineScope.launch {
                skillPlacements.clear()
                skillRanks.clear()
                getRanksForSkill(skill)
            }
        }
        updateDisplay()
    }

    @SubscribeEvent
    fun onSkillGained(event: SkillExpGainEvent) {
        val skillName = event.skill.name.lowercase()
        println(SkillAPI.skillXPInfoMap)
        val skillInfo = SkillAPI.skillXPInfoMap[event.skill] ?: return

        println(lastSkillGained)
        println(skillName)
        if (lastSkillGained != skillName) {
            lastSkillGained = skillName

            println(skillName)

            SkyHanniMod.coroutineScope.launch {
                getRanksForSkill(skillName)
            }
        }
        lastXPGained = SimpleTimeMark.now()
        currentSkills[skillName] = skillInfo.lastTotalXp.toLong()
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (event.message.startsWith("§8Profile ID: ")) {
            val id = event.message.removePrefix("§8Profile ID: ")
            val newID = try {
                UUID.fromString(id)
            } catch (_: Exception) {
                null
            }
            if (profileID != newID) {
                resetData()
                profileID = newID
            }
        }
    }

    private fun resetData() {
        hasSkillsBeenFetched = false
        lastLeaderboardFetch = SimpleTimeMark.farPast()
        skillRanks.clear()
        skillPlacements.clear()
    }

    private fun updateDisplay() {
        if (lastSkillFetched == null) return
        if (skillPlacements.isEmpty()) return
        if (currentSkills.isEmpty()) {
            display = listOf(Renderable.wrappedString("§cCheck if your Skills \nAPI is enabled!", width = 200))
            return
        }

        val rank = skillRanks[lastSkillFetched] ?: return
        val nextRank = if (rank == -1) 5000 else rank - 1

        val placements = skillPlacements[lastSkillFetched] ?: return
        val skill = currentSkills[lastSkillFetched] ?: 0
        val amountToBeat = placements[nextRank] ?: 0

        val difference = amountToBeat - skill

        val displayPosition = if (config.showPosition && rank != -1) "§7[§b#$rank§7]" else ""


        val newDisplay = mutableListOf<Renderable>()
        newDisplay.add(
            Renderable.clickAndHover(
                "§6§l${lastSkillFetched?.firstLetterUppercase()}: §e${skill.addSeparators()} $displayPosition",
                listOf("§eClick to open your Elite Bot Profile."),
                onClick = {
                    OSUtils.openBrowser("https://elitebot.dev/@${LorenzUtils.getPlayerName()}/")
                    ChatUtils.chat("Opening Elite Bot Profile of player §b${LorenzUtils.getPlayerName()}")
                }
            )
        )
        val skillType = SkillType.getByNameOrNull(lastSkillFetched ?: "")
        if (config.showTimeUntilReached && skillType != null) {
            val speed = ((SkillAPI.skillXPInfoMap[skillType]?.xpGainHour ?: 0f) / 3600f).toInt()
            if (speed != 0) {
                val timeUntilReached = (difference / speed).seconds

                newDisplay.add(
                    Renderable.string("§7Time until reached: §b${timeUntilReached.format()}")
                )
            } else {
                newDisplay.add(
                    Renderable.string("§cPAUSED")
                )
            }
        }
        if (nextRank <= 0) {
            newDisplay.add(
                Renderable.string("§aNo players ahead of you!")
            )
        } else if (difference <= 0) {
            newDisplay.add(
                Renderable.clickAndHover(
                    "§7You have passed §b#${nextRank.addSeparators()}",
                    listOf("§bClick to refresh."),
                    onClick = {
                        lastLeaderboardFetch = SimpleTimeMark.farPast()
                        ChatUtils.chat("Skills leaderboard updating...")
                    }
                )
            )
        } else {
            newDisplay.add(
                Renderable.string("§e${difference.addSeparators()} §7behind §b#${nextRank.addSeparators()}")
            )
        }
        if (config.showTimeUntilRefresh) {
            val time = checkDuration - lastLeaderboardFetch.passedSince()
            val timedisplay = if (time.isNegative()) "Now" else time.format()

            newDisplay.add(
                Renderable.string("§7Refreshes in: §b$timedisplay")
            )
        }
        display = newDisplay
    }

    private fun getRanksForSkill(skill: String) {
        if (profileID == null) return
        val url =
            "https://api.elitebot.dev/Leaderboard/rank/$skill/${LorenzUtils.getPlayerUuid()}/${profileID!!.toDashlessUUID()}?includeUpcoming=true"
//         "https://api.elitebot.dev/Leaderboard/rank/$skill/5e22209be5864a088761aa6bde56a090/5825e8f071d04806b92687d79b733f30?includeUpcoming=true"

        val response = APIUtil.getJSONResponseAsElement(url)

        try {
            val data = eliteCollectionApiGson.fromJson<EliteLeaderboard>(response)

            skillPlacements.clear()

            skillRanks[skill] = data.rank
            val placements = mutableMapOf<Int, Long>()
            var rank = data.upcomingRank
            data.upcomingPlayers.forEach {
                //weight is amount
                placements[rank] = it.weight.toLong()
                rank--
            }
            skillPlacements[skill] = placements
            lastSkillFetched = skill
            if (data.amount != 0L) {
                currentSkills[skill] = data.amount
            }

            if (!hasSkillsBeenFetched && data.amount == 0L) {
                hasSkillsBeenFetched = true
                getCurrentSkills()
            }
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Error loading user skill leaderboard\n" +
                    "§eLoading the skill leaderboard data from elitebot.dev failed!\n" +
                    "§eYou can switch worlds to try to fix the problem.\n" +
                    "§cIf this message repeats, please report it on Discord!\n",
                "url" to url,
                "apiResponse" to response,
            )
        }
    }

    private fun getCurrentSkills() {
        if (profileID == null) return
        val url =
            "https://api.elitebot.dev/Graph/${LorenzUtils.getPlayerUuid()}/${profileID!!.toDashlessUUID()}/skills?days=1"
//         "https://api.elitebot.dev/Graph/5e22209be5864a088761aa6bde56a090/5825e8f071d04806b92687d79b733f30/skills?days=1"
        val response = APIUtil.getJSONResponseAsElement(url)

        try {
            val data = eliteCollectionApiGson.fromJson<Array<EliteSkillGraphEntry>>(response)

            data.sortBy { it.timestamp }
            currentSkills = data.lastOrNull()?.skills?.toMutableMap() ?: mutableMapOf()

        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e,
                "Error loading user skill\n" +
                    "§eLoading the skill data from elitebot.dev failed!\n" +
                    "§eYou can switch worlds to try to fix the problem.\n" +
                    "§cIf this message repeats, please report it on Discord!\n",
                "url" to url,
                "apiResponse" to response,
            )
        }
    }

    private fun isEnabled() = config.display && LorenzUtils.inSkyBlock && (GardenAPI.inGarden() || !config.showInGarden)
}
