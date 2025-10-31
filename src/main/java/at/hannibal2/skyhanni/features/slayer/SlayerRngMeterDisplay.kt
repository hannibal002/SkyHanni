package at.hannibal2.hanni.features.slayer

import at.hannibal2.hanni.api.event.HandleEvent
import at.hannibal2.hanni.config.storage.ProfileSpecificStorage
import at.hannibal2.hanni.data.ProfileStorageData
import at.hannibal2.hanni.data.SlayerApi
import at.hannibal2.hanni.data.jsonobjects.repo.neu.NeuRNGScore
import at.hannibal2.hanni.data.title.TitleManager
import at.hannibal2.hanni.events.InventoryFullyOpenedEvent
import at.hannibal2.hanni.events.NeuRepositoryReloadEvent
import at.hannibal2.hanni.events.SecondPassedEvent
import at.hannibal2.hanni.events.chat.HanniChatEvent
import at.hannibal2.hanni.events.slayer.SlayerChangeEvent
import at.hannibal2.hanni.hannimodule.HanniModule
import at.hannibal2.hanni.test.command.ErrorManager
import at.hannibal2.hanni.utils.ChatUtils
import at.hannibal2.hanni.utils.HypixelCommands
import at.hannibal2.hanni.utils.ItemUtils.getInternalName
import at.hannibal2.hanni.utils.ItemUtils.getLore
import at.hannibal2.hanni.utils.ItemUtils.repoItemName
import at.hannibal2.hanni.utils.NeuInternalName
import at.hannibal2.hanni.utils.NumberUtil.addSeparators
import at.hannibal2.hanni.utils.NumberUtil.formatLong
import at.hannibal2.hanni.utils.NumberUtil.formatPercentage
import at.hannibal2.hanni.utils.RegexUtils.matchMatcher
import at.hannibal2.hanni.utils.RegexUtils.matches
import at.hannibal2.hanni.utils.RenderDisplayHelper
import at.hannibal2.hanni.utils.RenderUtils.renderRenderables
import at.hannibal2.hanni.utils.SimpleTimeMark
import at.hannibal2.hanni.utils.SkyBlockUtils
import at.hannibal2.hanni.utils.StringUtils.removeColor
import at.hannibal2.hanni.utils.StringUtils.removeWordsAtEnd
import at.hannibal2.hanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.hanni.utils.renderables.Renderable
import at.hannibal2.hanni.utils.repopatterns.RepoPattern
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

@HanniModule
object SlayerRngMeterDisplay {

    private val config get() = SlayerApi.config.rngMeterDisplay

    private val patternGroup = RepoPattern.group("slayer.rngmeter")
    private val inventoryNamePattern by patternGroup.pattern(
        "inventoryname",
        "(?<name>.*) RNG Meter",
    )
    private val slayerInventoryNamePattern by patternGroup.pattern(
        "inventoryname.slayer",
        "Slayer",
    )
    private val updatePattern by patternGroup.pattern(
        "update",
        " {3}§dRNG Meter §f- §d(?<exp>.*) Stored XP",
    )
    private val changedItemPattern by patternGroup.pattern(
        "changeditem",
        "§aYou set your §r.* RNG Meter §r§ato drop §r.*§a!",
    )

    /**
     * REGEX-TEST: §aEnchanted Book (§d§lDuplex I§a)
     */
    private val bookFormatPattern by patternGroup.pattern(
        "book.format",
        "§aEnchanted Book \\((?<name>.*)§a\\)",
    )

    private var display = emptyList<Renderable>()
    private var lastItemDroppedTime = SimpleTimeMark.farPast()
    private var lastRngMeterUpdate = SimpleTimeMark.farPast()
    private var timesUpdatedTotal = 0
    private var timesUpdatedSinceLastDrop = 0

    var rngScore = mapOf<String, Map<NeuInternalName, Long>>()

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (!isEnabled()) return

        if (!lastItemDroppedTime.isFarPast() && lastItemDroppedTime.passedSince() > 4.seconds) {
            lastItemDroppedTime = SimpleTimeMark.farPast()
            update()
        }
    }

    @HandleEvent
    fun onSlayerChange(event: SlayerChangeEvent) {
        update()
    }

    @HandleEvent
    fun onChat(event: HanniChatEvent) {
        if (!isEnabled()) return

        if (config.hideChat && SlayerApi.isInCorrectArea) {
            changedItemPattern.matchMatcher(event.message) {
                event.blockedReason = "slayer_rng_meter"
            }
        }

        val currentMeter = updatePattern.matchMatcher(event.message) {
            group("exp").formatLong()
        } ?: return
        timesUpdatedTotal++
        timesUpdatedSinceLastDrop++

        val storage = getStorage() ?: return
        val old = storage.currentMeter
        storage.currentMeter = currentMeter

        if (old != -1L) {
            val item = storage.itemGoal
            val hasItemSelected = item != "" && item != "?"
            if (!hasItemSelected && config.warnEmpty) {
                ChatUtils.userError("No Slayer RNG Meter Item selected!")
                TitleManager.sendTitle("§cNo RNG Meter Item!")
            }
            var blockChat = config.hideChat && hasItemSelected
            val diff = currentMeter - old
            if (diff > 0) {
                storage.gainPerBoss = diff
            } else {
                storage.currentMeter = 0
                blockChat = false
                val from = old.addSeparators()
                val to = storage.goalNeeded.addSeparators()

                var rawPercentage = old.toDouble() / storage.goalNeeded
                if (rawPercentage > 1) rawPercentage = 1.0
                val percentage = rawPercentage.formatPercentage()
                if (storage.goalNeeded == -1L) {
                    ErrorManager.logErrorStateWithData(
                        "Error Calculating Slayer RNG Meter",
                        "gaol needed is -1, this should never be the case!",
                        "goalNeeded" to storage.goalNeeded,
                        "currentMeter" to storage.currentMeter,
                        "gainPerBoss" to storage.gainPerBoss,
                        "itemGoal" to storage.itemGoal,
                        "rawPercentage" to rawPercentage,
                        "percentage" to percentage,
                        "old" to old,
                        "lastItemDroppedTime" to lastItemDroppedTime,
                        "lastRngMeterUpdate" to lastRngMeterUpdate,
                        "timesUpdatedTotal" to timesUpdatedTotal,
                        "timesUpdatedSinceLastDrop" to timesUpdatedSinceLastDrop,
                    )
                }
                ChatUtils.chat("§dRNG Meter §7dropped at §e$percentage §7XP ($from/$to§7)")
                lastItemDroppedTime = SimpleTimeMark.now()
                timesUpdatedSinceLastDrop = 0
            }
            if (blockChat) {
                event.blockedReason = "slayer_rng_meter"
            }
        }
        lastRngMeterUpdate = SimpleTimeMark.farPast()
        update()
    }

    private fun getStorage(): ProfileSpecificStorage.SlayerRngMeterStorage? {
        return ProfileStorageData.profileSpecific?.slayerRngMeter?.getOrPut(getCurrentSlayer()) {
            ProfileSpecificStorage.SlayerRngMeterStorage()
        }
    }

    private fun getCurrentSlayer() = SlayerApi.latestCategory.removeWordsAtEnd(1).removeColor()

    @HandleEvent
    fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return

        readRngMeterInventory(event)
        readSlayerInventory(event)
    }

    private fun readRngMeterInventory(event: InventoryFullyOpenedEvent) {
        val name = inventoryNamePattern.matchMatcher(event.inventoryName) {
            group("name")
        } ?: return

        if (name != getCurrentSlayer()) return

        val internalName = event.inventoryItems.values.find { item -> item.getLore().any { it.contains("§a§lSELECTED") } }
        setNewGoal(internalName?.getInternalName())
    }

    private fun readSlayerInventory(event: InventoryFullyOpenedEvent) {
        if (!slayerInventoryNamePattern.matches(event.inventoryName)) return
        val item = event.inventoryItems[35] ?: return
        val lore = item.getLore()
        val name = lore.firstOrNull()?.removeColor() ?: return

        if (name != getCurrentSlayer()) return

        val rawName = lore.nextAfter("§7Selected Drop") ?: return
        val itemName = bookFormatPattern.matchMatcher(rawName) {
            group("name")
        } ?: rawName
        val internalName = NeuInternalName.fromItemName(itemName)
        setNewGoal(internalName)
    }

    private fun setNewGoal(internalName: NeuInternalName?) {
        val storage = getStorage() ?: return
        if (internalName == null) {
            storage.itemGoal = ""
            storage.goalNeeded = -1
        } else {
            storage.itemGoal = internalName.repoItemName
            val currentSlayer = getCurrentSlayer()
            storage.goalNeeded = rngScore[currentSlayer]?.get(internalName) ?: run {
                ErrorManager.logErrorStateWithData(
                    "Failed reading RNG Meter goal needed amount",
                    "rngScore does not contain current slayer and current item data",
                    "internalName" to internalName,
                    "currentSlayer" to currentSlayer,
                    "rngScore" to rngScore,
                )
                -1
            }
        }
        update()
    }

    @HandleEvent
    fun onNeuRepoReload(event: NeuRepositoryReloadEvent) {
        rngScore = event.getConstant<NeuRNGScore>("rngscore").slayer
    }

    private fun update() {
        display = listOf(makeLink(drawDisplay()))
    }

    private fun makeLink(text: String) = Renderable.clickable(
        text,
        tips = listOf("§eClick to open RNG Meter Inventory."),
        onLeftClick = {
            HypixelCommands.showRng("slayer", SlayerApi.activeType?.rngName)
        },
    )

    fun drawDisplay(): String {
        val storage = getStorage() ?: return ""

        if (SlayerApi.latestCategory.let { it.endsWith(" I") || it.endsWith(" II") }) {
            return ""
        }

        with(storage) {
            if (itemGoal == "?") return "§cOpen RNG Meter Inventory!"
            if (itemGoal == "") {
                return if (!lastItemDroppedTime.isFarPast()) {
                    "§a§lRNG Item dropped!"
                } else {
                    "§eNo RNG Item selected!"
                }
            }
            if (currentMeter == -1L || gainPerBoss == -1L) return "§cKill the slayer boss 2 times!"

            val missing = goalNeeded - currentMeter + gainPerBoss
            var timesMissing = missing.toDouble() / gainPerBoss
            if (timesMissing < 1) timesMissing = 1.0
            timesMissing = ceil(timesMissing)

            return "$itemGoal §7in §e${timesMissing.toInt().addSeparators()} §7bosses!"
        }
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = { shouldShowDisplay() },
            onRender = {
                config.pos.renderRenderables(display, posLabel = "RNG Meter Display")
            },
        )
    }

    private fun shouldShowDisplay(): Boolean {
        if (!isEnabled()) return false
        if (!SlayerApi.isInCorrectArea) return false
        if (!SlayerApi.hasActiveQuest()) return false

        return true
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled
}
