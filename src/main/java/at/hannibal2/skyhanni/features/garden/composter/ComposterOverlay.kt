package at.hannibal2.skyhanni.features.garden.composter

import at.hannibal2.skyhanni.api.ItemBuyApi.createBuyTipLine
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.composter.ComposterConfig.RetrieveFromEntry
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackApi.getAmountInSacksOrNull
import at.hannibal2.skyhanni.data.jsonobjects.repo.GardenJson
import at.hannibal2.skyhanni.data.model.ComposterUpgrade
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.NeuRepositoryReloadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.composter.ComposterApi.getLevel
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValue
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryDetector
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoinWithBrackets
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPrice
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemNameCompact
import at.hannibal2.skyhanni.utils.KeyboardManager
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.NONE
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems
import at.hannibal2.skyhanni.utils.NeuItems.getItemStack
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.NumberUtil.roundTo
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.SoundUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addNotNull
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addVerticalSpacer
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableString
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.addRenderableButton
import at.hannibal2.skyhanni.utils.renderables.addLine
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object ComposterOverlay {

    private var organicMatterFactors: Map<NeuInternalName, Double> = emptyMap()
    private var fuelFactors: Map<NeuInternalName, Double> = emptyMap()
    private var organicMatter: Map<NeuInternalName, Double> = emptyMap()

    private val config get() = GardenApi.config.composters
    private var organicMatterDisplay: Renderable? = null
    private var fuelExtraDisplay: Renderable? = null

    private var currentTimeType = TimeType.HOUR
    private val composterInventory = InventoryDetector { name -> name == "Composter" }
    private val composterUpgradesInventory = InventoryDetector { name -> name == "Composter Upgrades" }
    private var extraComposterUpgrade: ComposterUpgrade? = null
        set(value) {
            field = value
            lastHovered = SimpleTimeMark.now()
        }

    private var maxLevel = false
    private var lastHovered = SimpleTimeMark.farPast()
    private var lastAttemptTime = SimpleTimeMark.farPast()

    val inInventory get() = composterInventory.isInside() || composterUpgradesInventory.isInside()

    private var testOffset = 0

    var currentOrganicMatterItem: NeuInternalName?
        get() = GardenApi.storage?.composterCurrentOrganicMatterItem
        private set(value) {
            GardenApi.storage?.composterCurrentOrganicMatterItem = value
        }

    var currentFuelItem: NeuInternalName?
        get() = GardenApi.storage?.composterCurrentFuelItem
        private set(value) {
            GardenApi.storage?.composterCurrentFuelItem = value
        }

    fun onCommand(args: Array<String>) {
        if (args.size != 1) {
            ChatUtils.userError("Usage: /shtestcomposter <offset>")
            return
        }
        testOffset = args[0].toInt()
        ChatUtils.chat("Composter test offset set to $testOffset.")
    }

    private val COMPOST = "COMPOST".toInternalName()
    private val BIOFUEL = "BIOFUEL".toInternalName()
    private val VOLTA = "VOLTA".toInternalName()
    private val OIL_BARREL = "OIL_BARREL".toInternalName()

    @HandleEvent(TabListUpdateEvent::class, priority = HandleEvent.LOW)
    fun onTabListUpdate() {
        if (inInventory) {
            update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onTick() {
        if (composterUpgradesInventory.isInside() && extraComposterUpgrade != null && lastHovered.passedSince() > 200.milliseconds) {
            extraComposterUpgrade = null
            update()
        }
    }

    @HandleEvent(InventoryFullyOpenedEvent::class, onlyOnIsland = IslandType.GARDEN)
    fun onInventoryFullyOpened() {
        if (inInventory) {
            update()
        }
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onToolTip(event: ToolTipEvent) {
        if (!composterUpgradesInventory.isInside()) return
        for (upgrade in ComposterUpgrade.entries) {
            val name = event.itemStack.displayName
            if (name.contains(upgrade.displayName)) {
                maxLevel = ComposterUpgrade.regex.matchMatcher(name) {
                    group("level")?.romanToDecimalIfNecessary() ?: 0
                } == 25
                extraComposterUpgrade = upgrade
                update()
                return
            }
        }
        if (extraComposterUpgrade != null) {
            extraComposterUpgrade = null
            maxLevel = false
        }
        update()
    }

    private fun update() {
        if (!config.overlay) return
        val composterUpgrades = ComposterApi.composterUpgrades ?: return
        if (composterUpgrades.isEmpty()) {
            RenderableString("§cOpen Composter Upgrades!").let {
                organicMatterDisplay = it
                fuelExtraDisplay = it
            }
            return
        }
        if (organicMatterFactors.isEmpty()) {
            organicMatterDisplay = Renderable.vertical {
                addString("§cSkyHanni composter error:")
                addString("§cRepo data not loaded!")
                addString("§7(organicMatterFactors is empty)")
            }
            return
        }
        if (fuelFactors.isEmpty()) {
            organicMatterDisplay = Renderable.vertical {
                addString("§cSkyHanni composter error:")
                addString("§cRepo data not loaded!")
                addString("§7(fuelFactors is empty)")
            }
            return
        }
        if (currentOrganicMatterItem.let { it !in organicMatterFactors.keys && it != NONE }) {
            currentOrganicMatterItem = NONE
        }
        if (currentFuelItem.let { it !in fuelFactors.keys && it != NONE }) currentFuelItem = NONE

        if (composterInventory.isInside()) {
            organicMatterDisplay = drawOrganicMatterDisplay()
            fuelExtraDisplay = drawFuelExtraDisplay()
        } else if (this.composterUpgradesInventory.isInside()) {
            organicMatterDisplay = drawUpgradeStats()
            fuelExtraDisplay = null
        }
    }

    private fun preview(upgrade: ComposterUpgrade?): Renderable =
        if (upgrade == null) {
            RenderableString("§7Preview: Nothing")
        } else {
            val level = upgrade.getLevel(null)
            val nextLevel = if (maxLevel) "§6§lMAX" else "§c➜ §a" + (level + 1)
            val displayName = upgrade.displayName
            RenderableString("§7Preview §a$displayName§7: §a$level $nextLevel")
        }

    private fun drawUpgradeStats(): Renderable {
        val upgrade = if (!maxLevel) extraComposterUpgrade else null

        val maxOrganicMatter = ComposterApi.maxOrganicMatter(null)
        val maxOrganicMatterPreview = ComposterApi.maxOrganicMatter(upgrade)

        val matterPer = ComposterApi.organicMatterRequiredPer(null)
        val matterPerPreview = ComposterApi.organicMatterRequiredPer(upgrade)

        val organicMatterFormat = format(maxOrganicMatter, matterPer, upgrade, maxOrganicMatterPreview, matterPerPreview)

        val maxFuel = ComposterApi.maxFuel(null)
        val maxFuelPreview = ComposterApi.maxFuel(upgrade)

        val fuelRequiredPer = ComposterApi.fuelRequiredPer(null)
        val fuelRequiredPerPreview = ComposterApi.fuelRequiredPer(upgrade)

        val fuelFormat = format(maxFuel, fuelRequiredPer, upgrade, maxFuelPreview, fuelRequiredPerPreview)

        return Renderable.vertical {
            add(preview(extraComposterUpgrade))
            addVerticalSpacer()
            addNotNull(profitDisplay())
            addString(
                "§eOrganic Matter §7empty in §b$organicMatterFormat",
                tips = listOf(
                    "§7The full §eOrganic Matter §7storage would",
                    "§7become empty with your Composter Speed",
                    "§7in $organicMatterFormat",
                ),
            )
            addString(
                "§2Fuel §7empty in $fuelFormat",
                tips = listOf(
                    "§7The full §2Fuel §7storage would",
                    "§7become empty with your Composter Speed",
                    "§7in $fuelFormat",
                ),
            )
        }
    }

    private fun format(max: Int, per: Double, upgrade: ComposterUpgrade?, maxPreview: Int, perPreview: Double): String {
        val matterMaxDuration = ComposterApi.timePerCompost(null) * floor(max / per)
        val matterMaxDurationPreview =
            ComposterApi.timePerCompost(upgrade) * floor(maxPreview / perPreview)

        val format = formatTime(matterMaxDuration)
        val formatPreview = if (matterMaxDuration != matterMaxDurationPreview) " §c➜ " + formatTime(matterMaxDurationPreview) else ""

        return "$format$formatPreview"
    }

    private fun formatTime(duration: Duration) = "§b" + duration.format(maxUnits = 2)

    private fun drawOrganicMatterDisplay(): Renderable {
        val maxOrganicMatter = ComposterApi.maxOrganicMatter(if (maxLevel) null else extraComposterUpgrade)
        val currentOrganicMatter = ComposterApi.getOrganicMatter()
        val missingOrganicMatter = (maxOrganicMatter - currentOrganicMatter).toDouble()

        return Renderable.vertical {
            addString("§7Items needed to fill §eOrganic Matter")
            val fillList = fillList(organicMatterFactors, missingOrganicMatter, testOffset) {
                currentOrganicMatterItem = it
                update()
            }
            if (currentOrganicMatterItem == NONE) {
                currentOrganicMatterItem = fillList
                update()
            }
        }
    }

    private fun drawFuelExtraDisplay() = Renderable.vertical {
        addNotNull(profitDisplay())
        if (!composterInventory.isInside()) return@vertical
        addString("§7Items needed to fill §2Fuel")
        val maxFuel = ComposterApi.maxFuel(null)
        val currentFuel = ComposterApi.getFuel()
        val missingFuel = (maxFuel - currentFuel).toDouble()
        val fillList = fillList(fuelFactors, missingFuel) {
            currentFuelItem = it
            update()
        }
        if (currentFuelItem == NONE) {
            currentFuelItem = fillList
            update()
        }
    }

    private fun profitDisplay(): Renderable? {
        val organicMatterItem = currentOrganicMatterItem ?: return null
        val fuelItem = currentFuelItem ?: return null
        if (organicMatterItem == NONE || fuelItem == NONE) return null

        val timePerCompost = ComposterApi.timePerCompost(null)
        val upgrade = if (maxLevel) null else extraComposterUpgrade
        val timePerCompostPreview = ComposterApi.timePerCompost(upgrade)
        val format = timePerCompost.format()
        val formatPreview =
            if (timePerCompostPreview != timePerCompost) " §c➜ §b" + timePerCompostPreview.format() else ""

        val timeText = currentTimeType.display.lowercase()
        val timeMultiplier = if (currentTimeType != TimeType.COMPOST) {
            (currentTimeType.multiplier * 1000.0 / (timePerCompost.inWholeMilliseconds))
        } else 1.0
        val timeMultiplierPreview = if (currentTimeType != TimeType.COMPOST) {
            (currentTimeType.multiplier * 1000.0 / (timePerCompostPreview.inWholeMilliseconds))
        } else 1.0

        val multiDropFactor = ComposterApi.multiDropChance(null) + 1
        val multiDropFactorPreview = ComposterApi.multiDropChance(upgrade) + 1
        val multiplier = multiDropFactor * timeMultiplier
        val multiplierPreview = multiDropFactorPreview * timeMultiplierPreview
        val compostPerTitlePreview =
            if (multiplier != multiplierPreview) " §c➜ §e" + multiplierPreview.roundTo(2) else ""
        val compostPerTitle =
            if (currentTimeType == TimeType.COMPOST) "Compost factor" else "Composts per $timeText"

        val organicMatterPrice = getPrice(organicMatterItem)
        val organicMatterFactor = organicMatterFactors[organicMatterItem] ?: 1.0

        val organicMatterRequired = ComposterApi.organicMatterRequiredPer(null)
        val organicMatterRequiredPreview = ComposterApi.organicMatterRequiredPer(upgrade)

        val organicMatterPricePer = organicMatterPrice * (organicMatterRequired / organicMatterFactor)
        val organicMatterPricePerPreview = organicMatterPrice * (organicMatterRequiredPreview / organicMatterFactor)

        val fuelPrice = getPrice(fuelItem)
        val fuelFactor = fuelFactors[fuelItem] ?: 1.0

        val fuelRequired = ComposterApi.fuelRequiredPer(null)
        val fuelRequiredPreview = ComposterApi.fuelRequiredPer(upgrade)

        val fuelPricePer = fuelPrice * (fuelRequired / fuelFactor)
        val fuelPricePerPreview = fuelPrice * (fuelRequiredPreview / fuelFactor)

        val totalCost = (fuelPricePer + organicMatterPricePer) * timeMultiplier
        val totalCostPreview = (fuelPricePerPreview + organicMatterPricePerPreview) * timeMultiplierPreview

        val materialCostFormatPreview =
            if (totalCost != totalCostPreview) " §c➜ §6" + totalCostPreview.shortFormat() else ""

        val priceCompost = COMPOST.getPrice()
        val profit = ((priceCompost * multiDropFactor) - (fuelPricePer + organicMatterPricePer)) * timeMultiplier
        val profitPreview =
            ((priceCompost * multiDropFactorPreview) - (fuelPricePerPreview + organicMatterPricePerPreview)) * timeMultiplierPreview

        val profitFormatPreview = if (profit != profitPreview) " §c➜ §6" + profitPreview.shortFormat() else ""

        return Renderable.vertical {
            addRenderableButton<TimeType>(
                "Display",
                current = currentTimeType,
                onChange = {
                    currentTimeType = it
                    update()
                },
            )

            addLine(
                tips = listOf(
                    "§7The variables below are calcualted with",
                    "${organicMatterItem.repoItemName} §7and ${fuelItem.repoItemName}.",
                ),
            ) {
                addString("§7Using ")
                addItemStack(organicMatterItem)
                addString(" §7and ")
                addItemStack(fuelItem)
            }

            addString(
                text = " §7Time per Compost: §b$format$formatPreview",
                tips = listOf(
                    "§7It takes §b$format §7for",
                    "§7your composter to create one §aCompost.",
                ),
            )
            addString(
                " §7$compostPerTitle: §e${multiplier.roundTo(2).addSeparators()}$compostPerTitlePreview",
                tips = listOf(
                    "§7The §aCompost Factor §7is calcualted by adding",
                    "§aMulti Drop §7and §aComposter Speed §7together.",
                ),
            )
            addString(
                " §7Cost per $timeText: §6${totalCost.shortFormat()}$materialCostFormatPreview",
                tips = listOf(
                    "§7Shows how much you §cpay as cost §7in",
                    "§eOrganic Matter §7and §2Fuel§7.",
                ),
            )
            addString(
                " §7Profit per $timeText: §6${profit.shortFormat()}$profitFormatPreview",
                tips = listOf(
                    "§7Shows how much you make as §6profit §7from",
                    "§7selling §aCompsot §7after subtracting the §ccosts§6.",
                ),
            )
            addVerticalSpacer()
        }
    }

    private fun MutableList<Renderable>.fillList(
        factors: Map<NeuInternalName, Double>,
        missing: Double,
        testOffsetRec: Int = 0,
        onClick: (NeuInternalName) -> Unit,
    ): NeuInternalName {
        val map = mutableMapOf<NeuInternalName, Double>()
        for ((internalName, factor) in factors) {
            map[internalName] = factor / getPrice(internalName)
        }

        val testOffset = if (testOffsetRec > map.size) {
            ChatUtils.userError("Invalid Composter Overlay Offset! $testOffset cannot be greater than ${map.size}!")
            ComposterOverlay.testOffset = 0
            0
        } else testOffsetRec

        val first: NeuInternalName? = calculateFirst(map, testOffset, factors, missing, onClick)
        if (testOffset != 0) {
            add(
                Renderable.link("testOffset = $testOffset") {
                    ComposterOverlay.testOffset = 0
                    update()
                },
            )
        }

        return first ?: error("First is empty!")
    }

    private fun MutableList<Renderable>.calculateFirst(
        map: MutableMap<NeuInternalName, Double>,
        testOffset: Int,
        factors: Map<NeuInternalName, Double>,
        missing: Double,
        onClick: (NeuInternalName) -> Unit,
    ): NeuInternalName? {
        var i = 0
        var first: NeuInternalName? = null
        for (internalName in map.sortedDesc().keys) {
            i++
            if (i < testOffset) continue
            if (first == null) first = internalName
            val factor = factors[internalName] ?: 1.0

            val item = internalName.getItemStack()
            val itemsNeeded = if (config.roundDown) {
                val amount = missing / factor
                if (amount > .75 && amount < 1.0) {
                    1.0
                } else {
                    floor(amount)
                }
            } else {
                ceil(missing / factor)
            }.toInt()

            addLine {
                if (testOffset != 0) addString("#$i ")
                addItemStack(item)
                add(formatPrice(internalName, itemsNeeded, onClick, factor))
            }

            if (i == 10 + testOffset) break
        }
        return first
    }

    private fun formatPrice(
        internalName: NeuInternalName,
        itemsNeeded: Int,
        onClick: (NeuInternalName) -> Unit,
        factor: Double,
    ): Renderable {
        val pricePer = getPrice(internalName)
        val selected = internalName == currentOrganicMatterItem || internalName == currentFuelItem

        val displayType = when (internalName) {
            in organicMatterFactors -> "§eOrganic Matter"
            in fuelFactors -> "§2Fuel"
            else -> error("no type found: $internalName")
        }
        val totalPrice = itemsNeeded * pricePer

        val tips = buildList {
            add(internalName.repoItemName)
            add(" ")
            add("§7Price per: ${pricePer.formatCoin()}")
            add("$displayType §7per: §e${factor.addSeparators()}")
            add("§7Need §8$itemsNeeded §7items for max $displayType")
            add("§7Total price: ${totalPrice.formatCoin()}")

            add("")
            if (selected) {
                add(internalName.createBuyTipLine("Control + "))
            } else {
                add("§eClick to select for profit calculations!")
            }
        }

        val itemName = internalName.repoItemNameCompact
        val selectedFormat = if (selected) "§n" else ""
        val displayName = itemName.substring(0, 2) + selectedFormat + itemName.removeColor()

        return Renderable.clickable(
            text = "$displayName §8x${itemsNeeded.addSeparators()} ${totalPrice.formatCoinWithBrackets()}",
            onLeftClick = {
                onClick(internalName)
                if (KeyboardManager.isModifierKeyDown() && lastAttemptTime.passedSince() > 500.milliseconds) {
                    lastAttemptTime = SimpleTimeMark.now()
                    retrieveMaterials(internalName, itemName, itemsNeeded.toInt())
                }
            },
            tips = tips,
        )
    }

    private fun retrieveMaterials(internalName: NeuInternalName, itemName: String, itemsNeeded: Int) {
        if (itemsNeeded == 0) return
        if (config.retrieveFrom == RetrieveFromEntry.BAZAAR &&
            !SkyBlockUtils.noTradeMode && internalName != BIOFUEL
        ) {
            BazaarApi.searchForBazaarItem(itemName, itemsNeeded)
            return
        }
        val havingInInventory = internalName.getAmountInInventory()
        if (havingInInventory >= itemsNeeded) {
            ChatUtils.chat("$itemName §8x$itemsNeeded §ealready found in inventory!")
            return
        }

        val havingInSacks = internalName.getAmountInSacksOrNull() ?: run {
            HypixelCommands.getFromSacks(internalName.asString(), itemsNeeded - havingInInventory)
            // TODO Add sack type repo data

            val isDwarvenMineable = internalName.let { it == VOLTA || it == OIL_BARREL || it == BIOFUEL }
            val sackType = if (isDwarvenMineable) "Mining §eor §9Dwarven" else "Enchanted Agronomy"
            ChatUtils.clickableChat(
                "Sacks could not be loaded. Click here and open your §9$sackType Sack §eto update the data!",
                onClick = { HypixelCommands.sacks() },
                "§eClick to run /sax!",
                replaceSameMessage = true,
            )
            return
        }
        if (havingInSacks == 0) {
            SoundUtils.playErrorSound()
            if (SkyBlockUtils.noTradeMode) {
                ChatUtils.chat("No $itemName §efound in sacks.")
            } else {
                ChatUtils.chat("No $itemName §efound in sacks. Opening Bazaar.")
                BazaarApi.searchForBazaarItem(itemName, itemsNeeded)
            }
            return
        }

        HypixelCommands.getFromSacks(internalName.asString(), itemsNeeded - havingInInventory)
        val havingInTotal = havingInInventory + havingInSacks
        if (itemsNeeded >= havingInTotal) {
            if (SkyBlockUtils.noTradeMode) {
                ChatUtils.chat("You're out of $itemName §ein your sacks!")
            } else {
                ChatUtils.clickableChat(
                    // TODO Add this as a separate feature, and then don't send any msg if the feature is disabled
                    "You're out of $itemName §ein your sacks! Click here to buy more on the Bazaar!",
                    onClick = { HypixelCommands.bazaar(itemName.removeColor()) },
                    "§eClick find on the bazaar!",
                )
            }
        }
    }

    private fun getPrice(internalName: NeuInternalName): Double {
        val price = internalName.getPrice(config.priceSource)
        if (internalName == BIOFUEL && price > 20_000) return 20_000.0

        return price
    }

    @HandleEvent(NeuRepositoryReloadEvent::class)
    fun onNeuRepoReload() {
        updateOrganicMatterFactors()
    }

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val data = event.getConstant<GardenJson>("Garden")
        organicMatter = data.organicMatter
        fuelFactors = data.fuel
        updateOrganicMatterFactors()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        with(config) {
            ConditionalUtils.onToggle(minimumOrganicMatter) {
                updateOrganicMatterFactors()
            }
        }
    }

    private fun updateOrganicMatterFactors() {
        try {
            organicMatterFactors = updateOrganicMatterFactors(organicMatter)
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Failed to calculate composter overlay data",
                "organicMatter" to organicMatter,
            )
        }
    }

    private val blockedItems = listOf(
        "POTION_AFFINITY_TALISMAN",
        "CROPIE_TALISMAN",
        "SPEED_TALISMAN",
        "SIMPLE_CARROT_CANDY",
    )

    private fun isBlockedArmor(internalName: String): Boolean {
        return internalName.endsWith("_BOOTS") ||
            internalName.endsWith("_HELMET") ||
            internalName.endsWith("_CHESTPLATE") ||
            internalName.endsWith("_LEGGINGS")
    }

    private fun updateOrganicMatterFactors(baseValues: Map<NeuInternalName, Double>): Map<NeuInternalName, Double> {
        val map = mutableMapOf<NeuInternalName, Double>()
        for ((internalName, _) in NeuItems.allNeuRepoItems()) {
            if (blockedItems.contains(internalName) || isBlockedArmor(internalName)) continue

            var (newId, amount) = NeuItems.getPrimitiveMultiplier(internalName.toInternalName())
            if (internalName == "ENCHANTED_HUGE_MUSHROOM_1" || internalName == "ENCHANTED_HUGE_MUSHROOM_2") {
                //  160 * 8 * 4 is 5120 and not 5184, but hypixel made an error, so we have to copy the error
                amount = 5184
            }
            baseValues[newId]?.let {
                val totalOrganicMatter = it * amount
                if (totalOrganicMatter <= config.minimumOrganicMatter.get()) continue
                map[internalName.toInternalName()] = totalOrganicMatter
            }
        }
        return map
    }

    @HandleEvent(GuiRenderEvent.ChestGuiOverlayRenderEvent::class)
    fun onBackgroundDraw() {
        if (EstimatedItemValue.isCurrentlyShowing()) return

        if (!inInventory || !config.overlay) return
        config.overlayOrganicMatterPos.renderRenderable(
            organicMatterDisplay,
            posLabel = "Composter Overlay Organic Matter",
        )
        config.overlayFuelExtrasPos.renderRenderable(
            fuelExtraDisplay,
            posLabel = "Composter Overlay Fuel Extras",
        )
    }

    enum class TimeType(val display: String, val multiplier: Int) {
        COMPOST("Compost", 1),
        HOUR("Hour", 60 * 60),
        DAY("Day", 60 * 60 * 24),
        ;

        override fun toString(): String = display
    }

    @HandleEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.composterOverlay", "garden.composters.overlay")
        event.move(3, "garden.composterOverlayPriceType", "garden.composters.overlayPriceType")
        event.move(3, "garden.composterOverlayRetrieveFrom", "garden.composters.retrieveFrom")
        event.move(3, "garden.composterOverlayOrganicMatterPos", "garden.composters.overlayOrganicMatterPos")
        event.move(3, "garden.composterOverlayFuelExtrasPos", "garden.composters.overlayFuelExtrasPos")
        event.move(3, "garden.composterRoundDown", "garden.composters.roundDown")
        event.transform(15, "garden.composters.retrieveFrom") { element ->
            ConfigUtils.migrateIntToEnum(element, RetrieveFromEntry::class.java)
        }
    }

    @HandleEvent
    fun onDebug(event: DebugDataCollectEvent) {
        event.title("Garden Composter")

        event.addIrrelevant {
            add("currentOrganicMatterItem: $currentOrganicMatterItem")
            add("currentFuelItem: $currentFuelItem")

            add(" ")
            val composterUpgrades = ComposterApi.composterUpgrades
            if (composterUpgrades == null) {
                add("composterUpgrades is null")
            } else {
                for ((a, b) in composterUpgrades) {
                    add("upgrade $a: $b")
                }
            }

            add(" ")
            val tabListData = ComposterApi.tabListData
            for ((a, b) in tabListData) {
                add("tabListData $a: $b")
            }
        }
    }
}
