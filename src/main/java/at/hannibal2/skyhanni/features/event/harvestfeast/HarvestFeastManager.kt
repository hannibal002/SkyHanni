package at.hannibal2.skyhanni.features.event.harvestfeast

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.SkyHanniMod.launchCoroutine
import at.hannibal2.skyhanni.api.EliteDevApi
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteFeastData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ClipboardUtils
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import kotlinx.coroutines.sync.Mutex
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object HarvestFeastManager {
    private val patternGroup = RepoPattern.group("event.harvestfeast")
    private val profileStorage get() = SkyHanniMod.feature.storage
    private val config get() = SkyHanniMod.feature.event.feast
    private val isCurrentOutdated get() = isOutdated(currentFeastData) && isDataAvailable()

    private val feastInventoryDetector by lazy { InventoryDetector(feastInventoryPattern) }

    private var currentFeastData: EliteFeastData? = null
    private var fetchedFromElite = false
    private var lastFetched = SimpleTimeMark.farPast()

    private val fetchingFeastDataMutex = Mutex()
    private val sendingFeastDataMutex = Mutex()

    /**
     * REGEX-TEST:
     */
    private val feastInventoryPattern by patternGroup.pattern(
        "main.menu",
        ""
    )

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Harvest Feast Data")

        if (currentFeastData == null) {
            val now = SkyBlockTime.now()
            event.addIrrelevant("Harvest Feast data is null for year ${now.year} and month ${now.month}.")
        }

        // TODO: Add debug
    }

    @HandleEvent(SecondPassedEvent::class)
    fun onSecondPassed() {
        if (feastInventoryDetector.isInside()) return
        fetch()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        currentFeastData = profileStorage.storedHarvestFeastData.takeUnless { isOutdated(it) }
    }

    private fun fetch() {
        if (!config.fetchAutomatically) return
        if (!isCurrentOutdated) return
        if (lastFetched.passedSince() < 10.minutes) return

        CoroutineSettings("harvest feast data fetch").withIOContext().withMutex(fetchingFeastDataMutex).launchCoroutine {
            currentFeastData = EliteDevApi.fetchHarvestFeastData().takeIf { it.complete && !isOutdated(it) }
            handleFeastData()
            lastFetched = SimpleTimeMark.now()
        }
    }

    private fun handleFeastData() {
        if (isCurrentOutdated) {
            ChatUtils.chat("Current Harvest Feast Data could not be loaded.")
        } else {
            ChatUtils.debug("Loaded Harvest Feast Data for year ${currentFeastData?.year}, month ${currentFeastData?.month}.")
            fetchedFromElite = true
            saveDataToStorage()
        }
    }

    private fun saveDataToStorage() {
        profileStorage.storedHarvestFeastData = currentFeastData
    }

    private fun isOutdated(data: EliteFeastData?): Boolean {
        val data = data ?: return true
        val now = SkyBlockTime.now()
        return data.year < now.year ||
            data.month < now.month ||
            (data.next.all { upcoming -> upcoming.value == null } && data.current.isEmpty()) // not sure if this is right, other two should be though
    }

    private fun isDataAvailable(): Boolean {
        val now = SkyBlockTime.now()
        return now.month in 7..9
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shprintcurrentfeastdata") {
            description = "Prints the current feast data to chat"
            category = CommandCategory.DEVELOPER_DEBUG
            simpleCallback {

                val builder = StringBuilder()
                builder.append("profile feast data(outdated: ${isOutdated(profileStorage.storedHarvestFeastData)}): ")
                builder.appendLine(ConfigManager.gson.toJson(profileStorage.storedHarvestFeastData))
                builder.append("current feast data (outdated: $isCurrentOutdated): ")
                builder.appendLine(ConfigManager.gson.toJson(currentFeastData))
                builder.appendLine("fetched from elite: $fetchedFromElite")
                builder.appendLine("last fetched: ${lastFetched.passedSince()}")

                ChatUtils.clickableChat("Click to copy data to clipboard.", onClick = {
                    CoroutineSettings("copy feast data to clipboard").withIOContext().launchCoroutine {
                        ClipboardUtils.copyToClipboardAsync(builder.toString()).await()
                    }
                })
            }
        }
    }
}
