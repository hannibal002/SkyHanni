package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.inventory.SackDisplay
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object SackAPI {
    private val sackDisplayConfig get() = SkyHanniMod.feature.inventory.sackDisplay
    private var lastOpenedInventory = ""

    var inSackInventory = false
    private val sackPattern = "^(.* Sack|Enchanted .* Sack)$".toPattern()
    private val numPattern =
        "(?:(?:§[0-9a-f](?<level>I{1,3})§7:)?|(?:§7Stored:)?) (?<color>§[0-9a-f])(?<stored>[0-9.,kKmMbB]+)§7/(?<total>\\d+(?:[0-9.,]+)?[kKmMbB]?)".toPattern()
    private val gemstonePattern =
        " §[0-9a-f](?<gemrarity>[A-z]*): §[0-9a-f](?<stored>\\d+(?:\\.\\d+)?(?:(?:,\\d+)?)+[kKmM]?)(?: §[0-9a-f]\\(\\d+(?:\\.\\d+)?(?:(?:,\\d+)?)+[kKmM]?\\))?".toPattern()

    private var isRuneSack = false
    private var isGemstoneSack = false
    var isTrophySack = false
    private var sackRarity: TrophyRarity? = null

    val sackItem = mutableMapOf<String, SackOtherItem>()
    val runeItem = mutableMapOf<String, SackRune>()
    val gemstoneItem = mutableMapOf<String, SackGemstone>()
    private val stackList = mutableMapOf<Int, ItemStack>()

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inSackInventory = false
        isRuneSack = false
        isGemstoneSack = false
        isTrophySack = false
        runeItem.clear()
        gemstoneItem.clear()
        sackItem.clear()
        stackList.clear()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        val inventoryName = event.inventoryName
        val isNewInventory = inventoryName != lastOpenedInventory
        lastOpenedInventory = inventoryName
        val match = sackPattern.matcher(inventoryName).matches()
        if (!match) return
        val stacks = event.inventoryItems
        isRuneSack = inventoryName == "Runes Sack"
        isGemstoneSack = inventoryName == "Gemstones Sack"
        isTrophySack = inventoryName.contains("Trophy Fishing Sack")
        sackRarity = inventoryName.getTrophyRarity()
        inSackInventory = true
        stackList.putAll(stacks)
        SackDisplay.update(isNewInventory)
    }

    private fun String.getTrophyRarity(): TrophyRarity? {
        return if (this.startsWith("Bronze"))
            TrophyRarity.BRONZE
        else
            if (this.startsWith("Silver"))
                TrophyRarity.SILVER
            else null
    }

    private fun NEUInternalName.sackPrice(stored: String) = when (sackDisplayConfig.priceFrom) {
        0 -> (getPrice(true) * stored.formatNumber()).toInt().let { if (it < 0) 0 else it }

        1 -> try {
            val npcPrice = getBazaarData()?.npcPrice ?: 0.0
            (npcPrice * stored.formatNumber()).toInt()
        } catch (e: Exception) {
            0
        }

        else -> 0
    }

    fun getSacksData(savingSacks: Boolean) {
        if (savingSacks) sackData = ProfileStorageData.profileSpecific?.sacks?.sackContents ?: return
        for ((_, stack) in stackList) {
            val name = stack.name ?: continue
            val lore = stack.getLore()
            val gem = SackGemstone()
            val rune = SackRune()
            val item = SackOtherItem()
            loop@ for (line in lore) {
                if (isGemstoneSack) {
                    gemstonePattern.matchMatcher(line) {
                        val rarity = group("gemrarity")
                        val stored = group("stored")
                        gem.internalName = gemstoneMap[name.removeColor()] ?: NEUInternalName.NONE
                        if (gemstoneMap.containsKey(name.removeColor())) {
                            val internalName = "${rarity.uppercase()}_${
                                name.uppercase().split(" ")[0].removeColor()
                            }_GEM".asInternalName()

                            when (rarity) {
                                "Rough" -> {
                                    gem.rough = stored
                                    gem.roughPrice = internalName.sackPrice(stored)
                                }

                                "Flawed" -> {
                                    gem.flawed = stored
                                    gem.flawedPrice = internalName.sackPrice(stored)
                                }

                                "Fine" -> {
                                    gem.fine = stored
                                    gem.finePrice = internalName.sackPrice(stored)
                                }

                                "Flawless" -> {
                                    gem.flawless = stored
                                    gem.flawlessPrice = internalName.sackPrice(stored)
                                }
                            }
                            gemstoneItem[name] = gem
                        }
                    }
                } else {
                    numPattern.matchMatcher(line) {
                        val stored = group("stored")
                        val internalName = stack.getInternalName()
                        item.internalName = internalName
                        item.colorCode = group("color")
                        item.stored = stored
                        item.total = group("total")
                        if (savingSacks) setSackItem(item.internalName, item.stored.formatNumber().toInt())
                        item.price = if (isTrophySack) {
                            val trophyName =
                                internalName.asString().lowercase().substringBeforeLast("_").replace("_", "")
                            val filletValue =
                                TrophyFishManager.getInfoByName(trophyName)?.getFilletValue(sackRarity!!) ?: 0
                            val storedNumber = stored.formatNumber().toInt()
                            "MAGMA_FISH".asInternalName().sackPrice((filletValue * storedNumber).toString())
                        } else internalName.sackPrice(stored).coerceAtLeast(0)

                        if (isRuneSack) {
                            val level = group("level")
                            rune.stack = stack
                            if (level == "I") {
                                rune.lvl1 = stored
                                continue@loop
                            }
                            if (level == "II") {
                                rune.lvl2 = stored
                                continue@loop
                            }
                            if (level == "III") {
                                rune.lvl3 = stored
                            }
                            runeItem.put(name, rune)
                        } else {
                            sackItem.put(name, item)
                        }
                    }
                }
            }
        }
        if (savingSacks) saveSackData()
    }

    private var sackData = mapOf<NEUInternalName, SackItem>()

    data class SackChange(val delta: Int, val internalName: NEUInternalName, val sacks: List<String>)

    private val sackChangeRegex = Regex("""([+-][\d,]+) (.+) \((.+)\)""")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!event.message.removeColor().startsWith("[Sacks]")) return

        val sackChanges = ArrayList<SackChange>()

        val sackAddText = event.chatComponent.siblings.firstNotNullOfOrNull { sibling ->
            sibling.chatStyle?.chatHoverEvent?.value?.formattedText?.removeColor()?.takeIf {
                it.startsWith("Added")
            }
        } ?: ""
        val sackRemoveText = event.chatComponent.siblings.firstNotNullOfOrNull { sibling ->
            sibling.chatStyle?.chatHoverEvent?.value?.formattedText?.removeColor()?.takeIf {
                it.startsWith("Removed")
            }
        } ?: ""

        val sackChangeText = sackAddText + sackRemoveText
        if (sackChangeText.isEmpty()) return
        val isMissingInfo = sackChangeText.contains("other items")

        for (match in sackChangeRegex.findAll(sackChangeText)) {
            val delta = match.groups[1]!!.value.replace(",", "").toInt()
            val item = match.groups[2]!!.value
            val sacks = match.groups[3]!!.value.split(", ")

            val internalName = NEUInternalName.fromItemName(item)
            sackChanges.add(SackChange(delta, internalName, sacks))
        }
        SackChangeEvent(sackChanges, isMissingInfo).postAndCatch()
    }

    @SubscribeEvent
    fun sackChange(event: SackChangeEvent) {
        sackData = ProfileStorageData.profileSpecific?.sacks?.sackContents ?: return

        val justChanged = mutableListOf<NEUInternalName>()

        for (change in event.sackChanges) {
            justChanged.add(change.internalName)

            if (sackData.containsKey(change.internalName)) {
                val oldData = sackData[change.internalName]
                var newAmount = oldData!!.amount + change.delta
                if (newAmount < 0) newAmount = 0
                sackData = sackData.editCopy { this[change.internalName] = SackItem(newAmount, oldData.isOutdated) }
            } else {
                val newAmount = if (change.delta > 0) change.delta else 0
                sackData = sackData.editCopy { this[change.internalName] = SackItem(newAmount, true) }
            }
        }

        if (event.isMissingInfo) {
            for (item in sackData) {
                if (item.key in justChanged) continue
                val oldData = sackData[item.key]
                sackData = sackData.editCopy { this[item.key] = SackItem(oldData!!.amount, true) }
            }
        }
        saveSackData()
    }

    private fun setSackItem(item: NEUInternalName, amount: Int) {
        sackData = sackData.editCopy { this[item] = SackItem(amount, false) }
    }

    fun fetchSackItem(item: NEUInternalName): SackItem? {
        sackData = ProfileStorageData.profileSpecific?.sacks?.sackContents ?: return SackItem(-1, true)

        if (sackData.containsKey(item)) {
            return sackData[item]
        }

        sackData = sackData.editCopy { this[item] = SackItem(0, true) }
        saveSackData()
        return sackData[item]
    }

    private fun saveSackData() {
        ProfileStorageData.profileSpecific?.sacks?.sackContents = sackData
        println("saved sack data")
    }

    data class SackGemstone(
        var internalName: NEUInternalName = NEUInternalName.NONE,
        var rough: String = "0",
        var flawed: String = "0",
        var fine: String = "0",
        var flawless: String = "0",
        var roughPrice: Int = 0,
        var flawedPrice: Int = 0,
        var finePrice: Int = 0,
        var flawlessPrice: Int = 0,
    )

    data class SackRune(
        var stack: ItemStack? = null,
        var lvl1: String = "0",
        var lvl2: String = "0",
        var lvl3: String = "0",
    )

    data class SackOtherItem(
        var internalName: NEUInternalName = NEUInternalName.NONE,
        var colorCode: String = "",
        var stored: String = "0",
        var total: String = "0",
        var price: Int = 0,
    )
}

data class SackItem(
    @Expose val amount: Int,
    @Expose val isOutdated: Boolean
)

private val gemstoneMap = mapOf(
    "Jade Gemstones" to "ROUGH_JADE_GEM".asInternalName(),
    "Amber Gemstones" to "ROUGH_AMBER_GEM".asInternalName(),
    "Topaz Gemstones" to "ROUGH_TOPAZ_GEM".asInternalName(),
    "Sapphire Gemstones" to "ROUGH_SAPPHIRE_GEM".asInternalName(),
    "Amethyst Gemstones" to "ROUGH_AMETHYST_GEM".asInternalName(),
    "Jasper Gemstones" to "ROUGH_JASPER_GEM".asInternalName(),
    "Ruby Gemstones" to "ROUGH_RUBY_GEM".asInternalName(),
    "Opal Gemstones" to "ROUGH_OPAL_GEM".asInternalName(),
)
