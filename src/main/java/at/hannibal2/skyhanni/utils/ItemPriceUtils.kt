package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod.launch
import at.hannibal2.skyhanni.api.enoughupdates.EnoughUpdatesManager
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteAuctionPricing
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteAuctionsResponse
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteLowestBinBase
import at.hannibal2.skyhanni.data.jsonobjects.elitedev.EliteVariedAuctionItem
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi.getBazaarData
import at.hannibal2.skyhanni.features.inventory.bazaar.HypixelItemApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getNumberedName
import at.hannibal2.skyhanni.utils.ItemUtils.getRecipePrice
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.SKYBLOCK_COIN
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuItems.getRecipes
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.api.ApiStaticGetPath
import at.hannibal2.skyhanni.utils.api.ApiUtils
import at.hannibal2.skyhanni.utils.coroutines.CoroutineSettings
import at.hannibal2.skyhanni.utils.json.fromJson
import com.google.gson.JsonObject
import kotlin.time.Duration.Companion.minutes

@SkyHanniModule
object ItemPriceUtils {

    private val JACK_O_LANTERN = "JACK_O_LANTERN".toInternalName()
    private val GOLDEN_CARROT = "GOLDEN_CARROT".toInternalName()

    fun NeuInternalName.getPrice(
        priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY,
        pastRecipes: List<PrimitiveRecipe> = emptyList(),
    ) = getPriceOrNull(priceSource, pastRecipes) ?: 0.0

    fun NeuInternalName.getPriceOrNull(
        priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY,
        pastRecipes: List<PrimitiveRecipe> = emptyList(),
    ): Double? {
        when (this) {
            SKYBLOCK_COIN -> return 1.0
            NeuInternalName.GEMSTONE_COLLECTION -> return 0.0
            NeuInternalName.JASPER_CRYSTAL -> return 0.0
            NeuInternalName.RUBY_CRYSTAL -> return 0.0
            NeuInternalName.WISP_POTION -> return 20_000.0
            NeuInternalName.ENCHANTED_HAY_BLOCK -> return 7_776.0
            NeuInternalName.TIGHTLY_TIED_HAY_BALE -> return 1_119_744.0
        }

        if (priceSource != ItemPriceSource.NPC_SELL) {
            getBazaarData()?.let {
                return if (priceSource == ItemPriceSource.BAZAAR_INSTANT_BUY) it.instantBuyPrice else it.instantSellPrice
            }

            getLowestBinOrNull()?.let {
                return it
            }

            if (this == JACK_O_LANTERN) {
                return "PUMPKIN".toInternalName().getPrice(priceSource) + 1
            }
        }
        if (this == GOLDEN_CARROT) {
            // 6.8 for some players
            return 7.0 // NPC price
        }

        return getNpcPriceOrNull()
            ?: getRawCraftCostOrNull(priceSource, pastRecipes).takeUnless { priceSource == ItemPriceSource.NPC_SELL }
    }

    fun NeuInternalName.isAuctionHouseItem(): Boolean = getLowestBinOrNull() != null

    private fun NeuInternalName.getLowestBinOrNull(): Double? = when {
        else -> getShLowestBin(this)
    }.takeIf { it != -1L }?.toDouble()

    // We can not use NEU craft cost, since we want to respect the price source choice
    // NEUItems.manager.auctionManager.getCraftCost(asString())?.craftCost
    fun NeuInternalName.getRawCraftCostOrNull(
        priceSource: ItemPriceSource = ItemPriceSource.BAZAAR_INSTANT_BUY,
        pastRecipes: List<PrimitiveRecipe> = emptyList(),
    ): Double? = getRecipes(this).filter { it !in pastRecipes }
        .map { it.getRecipePrice(priceSource, pastRecipes + it) }
        .filter { it > 0 }
        .minOrNull()

    fun NeuInternalName.getNpcPrice(): Double = getNpcPriceOrNull() ?: 0.0

    fun NeuInternalName.getNpcPriceOrNull(): Double? {
        if (this == NeuInternalName.WISP_POTION) {
            return 20_000.0
        }
        return HypixelItemApi.getNpcPrice(this)
    }

    private fun debugItemPrice(args: String?) {
        val internalName = getItemOrFromHand(args)
        if (internalName == null) {
            ChatUtils.userError("Hold an item in hand or do /shdebugprice <item name/id>")
            return
        }

        val defaultPrice = internalName.getPrice().addSeparators()
        val info = buildList {
            add("Debug Item Price for §6$internalName ")
            add("defaultPrice: §6$defaultPrice")

            add("#")
            for (source in ItemPriceSource.entries) {
                val price = internalName.getPrice(source)
                add("${source.displayName} price: §6${price.addSeparators()}")
            }
            add("#")

            add(" ")
            add("getLowestBinOrNull: §6${internalName.getLowestBinOrNull()?.addSeparators()}")

            internalName.getBazaarData().let {
                add("getBazaarData instantBuyPrice: §6${it?.instantBuyPrice?.addSeparators()}")
                add("getBazaarData instantSellPrice: §6${it?.instantSellPrice?.addSeparators()}")
            }

            add("getNpcPriceOrNull: §6${internalName.getNpcPriceOrNull()?.addSeparators()}")
            add("getRawCraftCostOrNull: §6${internalName.getRawCraftCostOrNull()?.addSeparators()}")
        }
        ChatUtils.clickToClipboard("${internalName.repoItemName}§f: §6$defaultPrice", info)
    }

    // TODO move either into inventory utils or new command utils
    private fun getItemOrFromHand(name: String?): NeuInternalName? {
        return if (name.isNullOrEmpty()) {
            InventoryUtils.getItemInHand()?.getInternalName()
        } else {
            NeuInternalName.fromItemNameOrInternalName(name)
        }
    }

    private var lastLowestBinRefresh = SimpleTimeMark.farPast()
    private var lowestBins: Map<NeuInternalName, Long> = mutableMapOf()
    private var eliteLowestBins: Map<NeuInternalName, EliteAuctionPricing> = mutableMapOf()
    private fun getShLowestBin(internalName: NeuInternalName): Long = lowestBins[internalName] ?: -1L

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (ApiUtils.isMoulberryLowestBinDisabled()) return
        if (lastLowestBinRefresh.passedSince() < 2.minutes) return
        lastLowestBinRefresh = SimpleTimeMark.now()

        lowBinCoroutine.launch {
            val (_, data) = ApiUtils.getTypedJsonResponse<JsonObject>(lowBinStatic).assertSuccessWithData() ?: return@launch
            lowestBins = ConfigManager.gson.fromJson<Map<NeuInternalName, Long>>(data)
        }
    }

    private val lowBinCoroutine = CoroutineSettings("neu lowest bin item price fetch", timeout = 1.minutes).withIOContext()
    private val lowBinStatic = ApiStaticGetPath(
        "https://moulberry.codes/lowestbin.json.gz",
        "NEU Lowest Bin",
        tryForceGzip = true,
    )

    private val eliteLbinCoroutine = CoroutineSettings("elite lowest bin item price fetch", timeout = 1.minutes).withIOContext()
    private val eliteLowBinStatic = ApiStaticGetPath(
        "https://api.eliteskyblock.com/resources/auctions",
        "Elite Lowest Bin",
    )

    private val compareLbinsCoroutine = CoroutineSettings("compare lowest bin sources", timeout = 2.minutes).withIOContext()

    fun NeuInternalName.getPriceName(amount: Number, pricePer: Double = getPrice()): String {
        val price = pricePer * amount.toDouble()
        if (this == SKYBLOCK_COIN) return "${price.formatCoin()} coins"

        return "${getNumberedName(amount)} ${price.formatCoinWithBrackets()}"
    }

    fun Number.formatCoinWithBrackets(gray: Boolean = false): String {
        return "§7(" + formatCoin(gray) + "§7)"
    }

    fun Number.formatCoin(gray: Boolean = false): String {
        val color = when {
            gray -> "§7"
            this.toDouble() < 0 -> "§c"
            else -> "§6"
        }
        return color + shortFormat()
    }

    private fun Map<NeuInternalName, List<EliteVariedAuctionItem>>.splitByVariedBy() = buildMap {
        val knownInternals = EnoughUpdatesManager.getInternalNames()
        this@splitByVariedBy.forEach { (internalName, variants) ->
            variants.forEach { variant ->
                val resolvedVariant = resolveVariantName(internalName, variant)
                if (resolvedVariant !in knownInternals) {
                    ChatUtils.debug("Resolved variant $variant to $resolvedVariant")
                }
                put(resolvedVariant, variant.toPricing())
            }
        }
    }

    private fun resolveVariantName(base: NeuInternalName, item: EliteVariedAuctionItem): NeuInternalName {
        // Pets are horribly complex, they get their own case
        if (item.variedBy.pet != null) return resolvePetVariantName(item)

        // Globally replace `:` with `-` as a fallback.
        val migratedBase = base.asString().replace(":", "-").toInternalName()

        val extra = item.variedBy.extra?.takeIf { it.isNotEmpty() } ?: return migratedBase
        return EliteVarianceType.entries.find { it.key in extra }?.let { type ->
            extra[type.key]?.let { typeData ->
                type.transform(base, typeData)
            }
        } ?: migratedBase
    }

    private fun resolvePetVariantName(item: EliteVariedAuctionItem): NeuInternalName {
        val petName = item.variedBy.pet ?: return item.internalName
        val rarityId = item.variedBy.rarity.id
        val internalNameLevel = when (val max = item.variedBy.petLevel?.max) {
            null, 99 -> ""
            199 -> "+100"
            else -> "+$max"
        }
        return "$petName;$rarityId$internalNameLevel".toInternalName()
    }

    private fun EliteLowestBinBase.toPricing() = EliteAuctionPricing(
        lowest = lowest,
        lowestVolume = lowestVolume,
        lowest3Day = lowest3Day,
        lowest3DayVolume = lowest3DayVolume,
        lowest7Day = lowest7Day,
        lowest7DayVolume = lowest7DayVolume,
        last = last,
        rawLowest = rawLowest,
    )

    /**
     * The different types of "varied by" keys that Elite returns.
     * @param key the JSON key inside the "variedBy.extra" object.
     * @param transform the transform to apply with the value of the key, to form a new unique internal name.
     */
    private enum class EliteVarianceType(
        val key: String,
        val transformIntermediary: String = "_",
        val valueTransform: (String) -> String = { it.replace(" ", "_").replace(":", "-").uppercase() },
        val transform: (NeuInternalName, String) -> NeuInternalName = { internalName, value ->
            "${internalName.asString()}${transformIntermediary}${valueTransform(value)}".toInternalName()
        },
    ) {
        ABIPHONE_MODEL("model"),
        SWORD_SCROLLS("scrolls"),
        PARTY_HAT_COLOR(
            "party_hat_color",
            transform = { internalName, value ->
                val internalString = internalName.asString()
                val fixedString = if (internalString.endsWith("_ANIMATED")) {
                    val clean = internalString.removeSuffix("_ANIMATED")
                    "${clean}_${value}_ANIMATED"
                } else "${internalString}_$value"
                fixedString.toInternalName()
            }
        ),
        PARTY_HAT_EMOJI("party_hat_emoji"),
        PARTY_HAT_YEAR(
            "party_hat_year",
            transform = { internalName, value -> internalName },
        ),
        STAT_BOOST_PERCENT(
            "baseStatBoostPercentage",
            transform = { internalName, value ->
                val boostInt = value.toIntOrNull() ?: 0
                val internalNameStr = internalName.asString()
                val fixedString = if (boostInt == 50) "$internalNameStr+PERFECT" else "$internalNameStr+$boostInt"
                fixedString.toInternalName()
            }
        ),
        NEW_YEAR_CAKE("new_years_cake", transformIntermediary = "+"),
        POTION("potion", valueTransform = { it.replace(" ", "_").replace(":", ";").uppercase() }),
        RUNE(
            "rune",
            transform = { internalName, value ->
                // Base name (internalName) from Elite is "RUNE", but internal name _ends_ with RUNE, so we "flip"
                // Value will be colon separated, e.g., "SNOW:2"
                val (runeName, runeLevel) = value.split(":")
                val new = "${runeName}_RUNE;$runeLevel".toInternalName()
                ChatUtils.debug("Transformed \"$internalName, $value\" to \"$new\"")
                new
            },
        ),
    }

    @HandleEvent
    fun onCommandRegistration(event: CommandRegistrationEvent) {
        event.registerBrigadier("shdebugprice") {
            description = "Debug different price sources for an item."
            category = CommandCategory.DEVELOPER_DEBUG
            arg("internalName", BrigadierArguments.string()) { internalName ->
                callback {
                    debugItemPrice(getArg(internalName))
                }
            }
            simpleCallback {
                debugItemPrice(null)
            }
        }
        event.registerBrigadier("shfetchmoulblbins") {
            description = "Test fetching Moulberry's lowest bin data."
            category = CommandCategory.DEVELOPER_DEBUG
            coroutineSimpleCallback(lowBinCoroutine) {
                val timeNow = SimpleTimeMark.now()
                val (_, fetchedLowestBins) = ApiUtils.getJsonResponse(lowBinStatic).assertSuccessWithData()
                    ?: ErrorManager.skyHanniError("Failed to fetch Moulberry's lowest bin data!")
                lowestBins = ConfigManager.gson.fromJson<Map<NeuInternalName, Long>>(fetchedLowestBins)
                val formatString = buildString {
                    appendLine("§aFetched Moulberry's lowest bin data in §b${timeNow.passedSince().format()}§a!")
                    appendLine("    §7Total Items: §6${lowestBins.size}")
                }
                ChatUtils.chat(formatString, prefixColor = "§a")
            }
        }
        event.registerBrigadier("shfetchelitelbins") {
            description = "Test fetching lowest bin data from Elite's API."
            category = CommandCategory.DEVELOPER_DEBUG
            coroutineSimpleCallback(eliteLbinCoroutine) {
                val timeNow = SimpleTimeMark.now()
                val (_, fetchedLowestBins) = ApiUtils.getJsonResponse(eliteLowBinStatic).assertSuccessWithData()
                    ?: ErrorManager.skyHanniError("Failed to fetch Elite's lowest bin data!")
                val variedEliteLowestBins = ConfigManager.gson.fromJson<EliteAuctionsResponse>(fetchedLowestBins).items

                eliteLowestBins = variedEliteLowestBins.splitByVariedBy()
                val formatString = buildString {
                    appendLine("§aFetched lowest bin data from Elite in §b${timeNow.passedSince().format()}§a!")
                    appendLine("    §7Total Entries: §6${variedEliteLowestBins.size}")
                    appendLine("    §7Total Items (after vary split): §6${eliteLowestBins.size}")
                }
                ChatUtils.chat(formatString, prefixColor = "§a")
            }
        }
        event.registerBrigadier("shcomparelowbins") {
            description = "Fetch both Moulberry and Elite lowest bin data and report size and key discrepancies."
            category = CommandCategory.DEVELOPER_DEBUG
            coroutineSimpleCallback(compareLbinsCoroutine) {
                val (_, moulberryRaw) = ApiUtils.getJsonResponse(lowBinStatic).assertSuccessWithData()
                    ?: ErrorManager.skyHanniError("Failed to fetch Moulberry's lowest bin data!")
                // Do not include "+ATTRIBUTE" items from moulberry; they're outdated, and thus Elite doesn't have them.
                val moulberryBins = ConfigManager.gson.fromJson<Map<NeuInternalName, Long>>(moulberryRaw).filter {
                    !it.key.contains("+ATTRIBUTE")
                }

                val (_, eliteRaw) = ApiUtils.getJsonResponse(eliteLowBinStatic).assertSuccessWithData()
                    ?: ErrorManager.skyHanniError("Failed to fetch Elite's lowest bin data!")
                val eliteBins = ConfigManager.gson.fromJson<EliteAuctionsResponse>(eliteRaw).items.splitByVariedBy()

                val onlyInMoulberry = (moulberryBins.keys - eliteBins.keys).sortedBy { it.asString() }
                val onlyInElite = (eliteBins.keys - moulberryBins.keys).sortedBy { it.asString() }

                val details = buildList {
                    add("Moulberry items: §6${moulberryBins.size}")
                    add("Elite items (after vary split): §6${eliteBins.size}")
                    add("#")
                    add("Only in Moulberry (${onlyInMoulberry.size}):")
                    onlyInMoulberry.forEach { add("  §c$it") }
                    add("#")
                    add("Only in Elite (${onlyInElite.size}):")
                    onlyInElite.forEach { add("  §e$it") }
                }
                val summary = "Moulberry §6${moulberryBins.size}§7 vs Elite §6${eliteBins.size}§7 | " +
                    "§conly-Moulberry: ${onlyInMoulberry.size}§7 | §eonly-Elite: ${onlyInElite.size}"
                ChatUtils.clickToClipboard(summary, details)
            }
        }
    }
}
