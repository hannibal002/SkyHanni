package at.hannibal2.skyhanni.data

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyFishManager
import at.hannibal2.skyhanni.features.fishing.trophy.TrophyRarity
import at.hannibal2.skyhanni.features.inventory.SackDisplay
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.formatNumber
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.annotations.Expose
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent


object SackAPI {
    private val sackDisplayConfig get() = SkyHanniMod.feature.inventory.sackDisplay
    private val chatConfig get() = SkyHanniMod.feature.chat
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
        0 -> (getPrice(true) * stored.formatNumber()).toLong().let { if (it < 0) 0L else it }

        1 -> try {
            val npcPrice = getNpcPriceOrNull() ?: 0.0
            (npcPrice * stored.formatNumber()).toLong()
        } catch (e: Exception) {
            0L
        }

        else -> 0L
    }

    fun getSacksData(savingSacks: Boolean) {
        if (savingSacks) sackData = ProfileStorageData.sackProfiles?.sackContents ?: return
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
                                    if (savingSacks) setSackItem(internalName, stored.formatNumber())
                                }

                                "Flawed" -> {
                                    gem.flawed = stored
                                    gem.flawedPrice = internalName.sackPrice(stored)
                                    if (savingSacks) setSackItem(internalName, stored.formatNumber())
                                }

                                "Fine" -> {
                                    gem.fine = stored
                                    gem.finePrice = internalName.sackPrice(stored)
                                    if (savingSacks) setSackItem(internalName, stored.formatNumber())
                                }

                                "Flawless" -> {
                                    gem.flawless = stored
                                    gem.flawlessPrice = internalName.sackPrice(stored)
                                    if (savingSacks) setSackItem(internalName, stored.formatNumber())
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
                        if (savingSacks) setSackItem(item.internalName, item.stored.formatNumber())
                        item.price = if (isTrophySack) {
                            val trophyName =
                                internalName.asString().lowercase().substringBeforeLast("_").replace("_", "")
                            val filletValue =
                                TrophyFishManager.getInfoByName(trophyName)?.getFilletValue(sackRarity!!) ?: 0
                            val storedNumber = stored.formatNumber()
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

        val otherItemsAdded = sackAddText.contains("other items")
        val otherItemsRemoved = sackRemoveText.contains("other items")

        val sackChanges = ArrayList<SackChange>()
        for (match in sackChangeRegex.findAll(sackChangeText)) {
            val delta = match.groups[1]!!.value.replace(",", "").toInt()
            val item = match.groups[2]!!.value
            val sacks = match.groups[3]!!.value.split(", ")

            val internalName = NEUInternalName.fromItemName(item)
            sackChanges.add(SackChange(delta, internalName, sacks))
        }
        SackChangeEvent(sackChanges, otherItemsAdded, otherItemsRemoved).postAndCatch()
        if (chatConfig.hideSacksChange) {
            event.blockedReason = "sacks_change"
        }
    }

    @SubscribeEvent
    fun sackChange(event: SackChangeEvent) {
        sackData = ProfileStorageData.sackProfiles?.sackContents ?: return

        // if it gets added and subtracted but only 1 shows it will be outdated
        val justChanged = mutableMapOf<NEUInternalName, Int>()

        for (change in event.sackChanges) {
            if (change.internalName in justChanged) {
                justChanged[change.internalName] = (justChanged[change.internalName] ?: 0) + change.delta
            } else {
                justChanged[change.internalName] = change.delta
            }
        }

        for (item in justChanged) {
            if (sackData.containsKey(item.key)) {
                val oldData = sackData[item.key]
                var newAmount = oldData!!.amount + item.value
                var changed = (newAmount - oldData.amount).toInt()
                if (newAmount < 0) {
                    newAmount = 0
                    changed = 0
                }
                sackData = sackData.editCopy { this[item.key] = SackItem(newAmount, changed, oldData.outdatedStatus) }
            } else {
                val newAmount = if (item.value > 0) item.value else 0
                sackData = sackData.editCopy { this[item.key] = SackItem(newAmount.toLong(), newAmount, 2) }
            }
        }

        if (event.otherItemsAdded || event.otherItemsRemoved) {
            for (item in sackData) {
                if (item.key in justChanged) continue
                val oldData = sackData[item.key]
                sackData = sackData.editCopy { this[item.key] = SackItem(oldData!!.amount, 0, 1) }
            }
        }
        saveSackData()
    }

    private fun setSackItem(item: NEUInternalName, amount: Long) {
        sackData = sackData.editCopy { this[item] = SackItem(amount, 0, 0) }
    }

    fun fetchSackItem(item: NEUInternalName): SackItem {
        sackData = ProfileStorageData.sackProfiles?.sackContents ?: return SackItem(0, 0, -1)

        if (sackData.containsKey(item)) {
            return sackData[item] ?: return SackItem(0, 0, -1)
        }

        sackData = sackData.editCopy { this[item] = SackItem(0, 0, 2) }
        return sackData[item] ?: return SackItem(0, 0, -1)
    }

    fun getFromSacks(item: String, amount: Int) = LorenzUtils.sendCommandToServer("gfs $item $amount")

    private fun saveSackData() {
        ProfileStorageData.sackProfiles?.sackContents = sackData
        SkyHanniMod.configManager.saveSackData("saving-data")
    }

    data class SackGemstone(
        var internalName: NEUInternalName = NEUInternalName.NONE,
        var rough: String = "0",
        var flawed: String = "0",
        var fine: String = "0",
        var flawless: String = "0",
        var roughPrice: Long = 0,
        var flawedPrice: Long = 0,
        var finePrice: Long = 0,
        var flawlessPrice: Long = 0,
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
        var price: Long = 0,
    )
}

// status -1 = fetching data failed, 0 = < 1% of being wrong, 1 = 10% of being wrong, 2 = is 100% wrong
// lastChange is set to 0 when value is refreshed in the sacks gui and when being set initially
// if it didn't change in an update the lastChange value will stay the same and not be set to 0
data class SackItem(
    @Expose val amount: Long,
    @Expose val lastChange: Int,
    @Expose val outdatedStatus: Int
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
