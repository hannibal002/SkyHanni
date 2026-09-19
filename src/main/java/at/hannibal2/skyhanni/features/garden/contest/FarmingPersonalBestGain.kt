package at.hannibal2.skyhanni.features.garden.contest

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.model.SkyblockStat.FARMING_FORTUNE
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.NumberUtil.formatDouble
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting

@SkyHanniModule
object FarmingPersonalBestGain {

    private val config get() = GardenApi.config.jacobContest.personalBests

    private val patternGroup = RepoPattern.group("garden.contest.personal.best")

    // <editor-fold desc="Patterns">
    /**
     * REGEX-TEST: [NPC] Jacob: You collected 1,400,694 items! PERSONAL BEST!
     */
    private val newPattern by patternGroup.pattern(
        "collection.new.colorless",
        "\\[NPC] Jacob: You collected (?<collected>[\\d,]+) items! PERSONAL BEST!",
    )

    /**
     * REGEX-TEST: [NPC] Jacob: Your previous Personal Best was 1,176,372.
     */
    private val oldPattern by patternGroup.pattern(
        "collection.old.colorless",
        "\\[NPC] Jacob: Your previous Personal Best was (?<collected>[\\d,]+)\\.",
    )

    /**
     * REGEX-TEST: [NPC] Jacob: Your Personal Bests perk is now granting you +46.69 Potato Fortune!
     */
    private val newFFPattern by patternGroup.pattern(
        "ff.new.colorless",
        "\\[NPC] Jacob: Your Personal Bests perk is now granting you \\+(?<ff>.*)${FARMING_FORTUNE.hypixelIcon} (?<crop>.*) Fortune!",
    )

    /**
     * REGEX-TEST: §e[NPC] Jacob§f: §rYour §6Personal Bests §fperk is now granting you §6+46.69 Potato Fortune§f!
     */
    @Deprecated("Only exists for repo. Remove after 9.0.0.", level = DeprecationLevel.ERROR)
    @Suppress("MaxLineLength")
    private val unused by patternGroup.pattern(
        "ff.new",
        "§e\\[NPC] Jacob§f: §rYour §6Personal Bests §fperk is now granting you §6\\+(?<ff>.*)${FARMING_FORTUNE.hypixelIcon} (?<crop>.*) Fortune§f!",
    )
    // </editor-fold>

    private val repoReloadCoroutine = CoroutineSettings("farming personal best gain repo reload")

    private var personalBestIncrements = mapOf<CropType, Int>()

    var newCollected: Double? = null
    var oldCollected: Double? = null
    var newFF: Double? = null
    var crop: String? = null
    var cropType: CropType? = null

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) = repoReloadCoroutine.launch {
        val data = event.getConstantAsync<GardenJson>("Garden")
        personalBestIncrements = data.personalBestIncrement
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(68, "garden.contestPersonalBestIncreaseFF", "garden.personalBests.increaseFF")
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
        if (!isEnabled()) return

        newPattern.matchMatcher(event.message) {
            newCollected = group("collected").formatDouble()
            checkDelayed()
        }

        oldPattern.matchMatcher(event.message) {
            oldCollected = group("collected").formatDouble()
            checkDelayed()
        }
        newFFPattern.matchMatcher(event.message) {
            val cropName = group("crop")
            newFF = group("ff").formatDouble()
            val newFF = newFF ?: return
            crop = cropName
            cropType = CropType.getByName(cropName)
            val cropType = cropType ?: return
            GardenApi.storage?.let {
                it.personalBestFF[cropType] = newFF
            }
            checkDelayed()
        }
    }

    private fun checkDelayed() = DelayedRun.runNextTick { check() }

    private fun check() {
        val newCollected = newCollected ?: return
        val oldCollected = oldCollected ?: return
        val newFF = newFF ?: return
        val crop = crop ?: return
        this.newCollected = null
        this.oldCollected = null
        this.newFF = null
        this.crop = null

        val pbIncrement = personalBestIncrements[cropType] ?: return
        val oldFF = oldCollected / (pbIncrement * 100)
        val newOverflowFF = newCollected / (pbIncrement * 100)
        val ffDiff = newFF - oldFF
        val overflowFFDiff = newOverflowFF - oldFF

        if (oldFF < 100 && !config.overflow) {
            ChatUtils.chat(
                componentBuilder {
                    append("This is ")
                    append("${ffDiff.roundTo(2)}☘ $crop Fortune ") {
                        withColor(ChatFormatting.GOLD)
                    }
                    append("more than previously!")
                }
            )
        } else if (newOverflowFF > 100 && config.overflow) {
            ChatUtils.chat(
                componentBuilder {
                    append("You have ")
                    append("${overflowFFDiff.roundTo(2)}☘ $crop Fortune ") {
                        withColor(ChatFormatting.GOLD)
                    }
                    append("including overflow!")
                }
            )
            ChatUtils.chat(
                componentBuilder {
                    append("This is ")
                    append("${overflowFFDiff.roundTo(2)}☘ $crop Fortune ") {
                        withColor(ChatFormatting.GOLD)
                    }
                    append("more than previously!")
                }
            )
        }
    }

    fun isEnabled() = GardenApi.inGarden() && config.increaseFF
}
