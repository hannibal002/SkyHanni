package at.hannibal2.skyhanni.utils

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.commands.CommandCategory
import at.hannibal2.skyhanni.config.commands.CommandRegistrationEvent
import at.hannibal2.skyhanni.config.commands.brigadier.BrigadierArguments
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
import at.hannibal2.skyhanni.utils.json.fromJson
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.NotEnoughUpdates
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
        PlatformUtils.isNeuLoaded() -> getNeuLowestBin(this)
        else -> getShLowestBin(this)
    }.takeIf { it != -1L }?.toDouble()

    private fun getNeuLowestBin(internalName: NeuInternalName) =
        NotEnoughUpdates.INSTANCE.manager.auctionManager.getLowestBin(internalName.asString())

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
    private fun getShLowestBin(internalName: NeuInternalName): Long = lowestBins[internalName] ?: -1L

    @HandleEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if (PlatformUtils.isNeuLoaded()) return
        if (ApiUtils.isMoulberryLowestBinDisabled()) return
        if (lastLowestBinRefresh.passedSince() < 2.minutes) return
        lastLowestBinRefresh = SimpleTimeMark.now()

        SkyHanniMod.launchIOCoroutine {
            val (_, data) = ApiUtils.getTypedJsonResponse<JsonObject>(lbinStatic).assertSuccessWithData() ?: return@launchIOCoroutine
            lowestBins = ConfigManager.gson.fromJson<Map<NeuInternalName, Long>>(data)
        }
    }

    private val lbinStatic = ApiStaticGetPath(
        "https://moulberry.codes/lowestbin.json.gz",
        "NEU Lowest Bin",
        tryForceGzip = true
    )

    fun NeuInternalName.getPriceName(amount: Number, pricePer: Double = getPrice()): String {
        val price = pricePer * amount.toDouble()
        if (this == SKYBLOCK_COIN) return " ${price.formatCoin()} coins"

        return " ${getNumberedName(amount)} ${price.formatCoinWithBrackets()}"
    }

    fun Number.formatCoinWithBrackets(gray: Boolean = false): String {
        return "§7(" + formatCoin(gray) + "§7)"
    }

    fun Number.formatCoin(gray: Boolean = false): String {
        val color = if (gray) "§7" else "§6"
        return color + shortFormat()
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
            simpleCallback {
                SkyHanniMod.launchIOCoroutine {
                    val timeNow = SimpleTimeMark.now()
                    val (_, fetchedLowestBins) = ApiUtils.getJsonResponse(lbinStatic).assertSuccessWithData()
                        ?: ErrorManager.skyHanniError("Failed to fetch Moulberry's lowest bin data!")
                    lowestBins = ConfigManager.gson.fromJson<Map<NeuInternalName, Long>>(fetchedLowestBins)
                    val formatString = buildString {
                        appendLine("§aFetched Moulberry's lowest bin data in §b${timeNow.passedSince().format()}§a!")
                        appendLine("    §7Total Items: §6${lowestBins.size}")
                    }
                    ChatUtils.chat(formatString, prefixColor = "§a")
                }
            }
        }
    }
}
