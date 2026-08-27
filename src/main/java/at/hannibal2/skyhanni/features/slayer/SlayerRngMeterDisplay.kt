package at.hannibal2.skyhanni.features.slayer

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.storage.ProfileSpecificStorage
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.data.SlayerApi
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuRNGScore
import at.hannibal2.skyhanni.data.title.TitleManager
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.events.chat.SkyHanniChatEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.NumberUtil.formatPercentage
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderDisplayHelper
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.StringUtils.removeWordsAtEnd
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.nextAfter
import at.hannibal2.skyhanni.utils.compat.appendWithColor
import at.hannibal2.skyhanni.utils.compat.componentBuilder
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import at.hannibal2.skyhanni.utils.renderables.primitives.empty
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.ChatFormatting
import kotlin.math.ceil
import kotlin.time.Duration.Companion.seconds

@SkyHanniModule
object SlayerRngMeterDisplay {

    private val config get() = SlayerApi.config.rngMeterDisplay

    private val patternGroup = RepoPattern.group("slayer.rngmeter")
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
    fun onSlayerChange() {
        update()
    }

    @HandleEvent
    fun onChat(event: SkyHanniChatEvent.Allow) {
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
                        "goal needed is -1, this should never be the case!",
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
                ChatUtils.chat(
                    componentBuilder {
                        appendWithColor("RNG Meter ", ChatFormatting.LIGHT_PURPLE)
                        withColor(ChatFormatting.GRAY)
                        append("dropped at ")
                        appendWithColor("$percentage ", ChatFormatting.YELLOW)
                        append("XP ($from/$to)")
                    },
                )
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

    private fun getStorage(): ProfileSpecificStorage.SlayerStorage.RngMeterStorage? {
        return ProfileStorageData.profileSpecific?.slayer?.rngMeter?.getOrPut(getCurrentSlayer()) {
            ProfileSpecificStorage.SlayerStorage.RngMeterStorage()
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
        val type = SlayerApi.rngMeterSlayerTypePattern.matchMatcher(event.inventoryName) {
            group("type")
        } ?: return

        if (type != getCurrentSlayer()) return

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

    private fun makeLink(content: Renderable) = Renderable.clickable(
        content,
        tips = listOf("§eClick to open RNG Meter Inventory."),
        onLeftClick = {
            HypixelCommands.showRng("slayer", SlayerApi.activeType?.rngName)
        },
    )

    fun drawDisplay(): Renderable {
        val storage = getStorage() ?: return Renderable.empty()

        if (SlayerApi.latestCategory.let { it.endsWith(" I") || it.endsWith(" II") }) {
            return Renderable.empty()
        }

        return Renderable.vertical(
            buildList {
                with(storage) {
                    if (itemGoal == "?") {
                        add(StringRenderable("§cOpen RNG Meter Inventory!"))
                        return@buildList
                    }
                    if (itemGoal == "") {
                        val msg = if (!lastItemDroppedTime.isFarPast()) "§a§lRNG Item dropped!" else "§eNo RNG Item selected!"
                        add(StringRenderable(msg))
                        return@buildList
                    }
                    if (currentMeter == -1L || gainPerBoss == -1L) {
                        add(StringRenderable("§cKill the slayer boss 2 times!"))
                        return@buildList
                    }

                    val missing = goalNeeded - currentMeter + gainPerBoss
                    var timesMissing = missing.toDouble() / gainPerBoss
                    if (timesMissing < 1) timesMissing = 1.0
                    timesMissing = ceil(timesMissing)

                    add(StringRenderable("$itemGoal §7in §e${timesMissing.toInt().addSeparators()} §7bosses!"))

                    if (config.coinsPerBoss) addNotNull(addCoinsPerBossLine(itemGoal, goalNeeded, gainPerBoss))
                }
            },
        )
    }

    init {
        RenderDisplayHelper(
            outsideInventory = true,
            inOwnInventory = true,
            condition = ::shouldShowDisplay,
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

    private fun addCoinsPerBossLine(itemGoal: String, goalNeeded: Long, gainPerBoss: Long): StringRenderable? {
        val internalName = NeuInternalName.fromItemNameOrNull(itemGoal.removeColor()) ?: return null
        val itemPrice = SlayerApi.getItemNameAndPrice(internalName, 1).second

        val bossesNeeded = ceil(goalNeeded.toDouble() / gainPerBoss).toInt().takeIf { it > 0 } ?: return null
        val slayerType = SlayerApi.activeType ?: return null
        val spawnCost = slayerType.calculateSpawnCost(SlayerApi.tier) ?: return null

        val profitPerBoss = SlayerRngMeterToolTipFeatures.calculateProfitPerBoss(bossesNeeded, spawnCost, itemPrice)

        return StringRenderable("§7Coins/Boss: $profitPerBoss")
    }

    fun isEnabled() = SkyBlockUtils.inSkyBlock && config.enabled

    @HandleEvent
    private fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(140, "#profile.slayerRngMeter", "#profile.slayer.rngMeter")
    }

}
