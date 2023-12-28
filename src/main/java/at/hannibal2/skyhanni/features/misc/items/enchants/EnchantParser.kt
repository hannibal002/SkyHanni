package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.config.features.enchantparsing.EnchantParsingConfig
import at.hannibal2.skyhanni.config.features.enchantparsing.EnchantParsingConfig.ColorEnchants.CommaFormat
import at.hannibal2.skyhanni.events.ChatHoverEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.ItemUtils.isEnchanted
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*
import java.util.regex.Pattern
import net.minecraft.event.HoverEvent
import net.minecraft.item.ItemStack
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

/**
 * Modified Enchant Parser from [SkyblockAddons](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/features/enchants/EnchantManager.java)
 */
object EnchantParser {

    private val config get() = SkyHanniMod.feature.enchantParsing

    val ENCHANTMENT_PATTERN: Pattern = Pattern.compile("(?<enchant>[A-Za-z][A-Za-z -]+) (?<levelNumeral>[IVXLCDM]+)(?<stacking>, |\$| \\d{1,3}(,\\d{3})*)")
    private val GRAY_ENCHANT_PATTERN = Pattern.compile("^(Respiration|Aqua Affinity|Depth Strider|Efficiency).*")

    private var indexOfLastGrayEnchant = -1
    private var loreLines: MutableList<String> = mutableListOf()

    private val gson = Gson()
    private val loreCache: Cache = Cache()
    // Maps for all enchants
    private var enchants: Enchants = Enchants()

    @SubscribeEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        val enchantsType = object : TypeToken<Enchants>(){}.type
        val inputStreamReader = InputStreamReader(
            FileInputStream(File(ConfigManager.configDirectory, "/repo/constants/Enchants.json")))
        val data = gson.fromJson<Enchants>(inputStreamReader, enchantsType)
        enchants = data
    }

    @SubscribeEvent
    fun onTooltipEvent(event: LorenzToolTipEvent) {
        // If enchants doesn't have any enchant data then we have no data to parse enchants correctly
        if (!isEnabled() || !enchants.hasEnchantData()) return

        // The enchants we expect to find in the lore, found from the items NBT data
        val enchants = event.itemStack.getEnchantments() ?: return

        // Check for any vanilla gray enchants at the top of the tooltip
        indexOfLastGrayEnchant = accountForAndRemoveGrayEnchants(event.toolTip, event.itemStack)

        parseEnchants(event.toolTip, enchants, null)
    }

    /**
     * For tooltips that are shown when hovering over an item from /show
     */
    @SubscribeEvent
    fun onChatHoverEvent(event: ChatHoverEvent) {
        if (event.action != HoverEvent.Action.SHOW_TEXT) return
        if (!isEnabled() || !enchants.hasEnchantData()) return

        val lore = event.component.formattedText.split("\n").toMutableList()

        // Since we don't get given an item stack from /show, we pass an empty enchants map and
        // use all enchants from the Enchants class instead
        parseEnchants(lore, mapOf(), event.component)
    }

    private fun parseEnchants(loreList: MutableList<String>, enchants: Map<String, Int>, chatComponent: IChatComponent?) {
        // Check if the lore is already cached so continuous hover isn't 1 fps
        if (loreCache.isCached(loreList)) {
            loreList.clear()
            loreList.addAll(loreCache.cachedLoreAfter)
            return
        }
        loreCache.updateBefore(loreList)

        var startEnchant = -1
        var endEnchant = -1

        // Find where the enchants start and end
        val startIndex = if (indexOfLastGrayEnchant == -1) 0 else indexOfLastGrayEnchant + 1
        for (i in startIndex until loreList.size) {
            val strippedLine = loreList[i].removeColor()

            if (startEnchant == -1) {
                if (this.enchants.containsEnchantment(enchants, strippedLine)) startEnchant = i
            } else if (strippedLine.trim().isEmpty() && endEnchant == -1) endEnchant = i - 1
        }

        if (endEnchant == -1) {
            loreCache.updateAfter(loreList)
            return
        }

        loreLines = mutableListOf()

        // Stacking enchants with their progress visible should have the
        // enchants stacked in a single column
        var shouldBeSingleColumn = false
        val orderedEnchants: TreeSet<FormattedEnchant> = TreeSet()
        var lastEnchant: FormattedEnchant? = null

        // Used to determine how many enchants are used on each line
        // for this particular item, since consistency is not Hypixel's strong point
        var maxEnchantsPerLine = 0

        // Order all enchants
        for (i in startEnchant..endEnchant) {
            val unformattedLine = loreList[i].removeColor()
            val matcher = ENCHANTMENT_PATTERN.matcher(unformattedLine)
            var containsEnchant = false
            var enchantsOnThisLine = 0

            while (matcher.find()) {
                // Pull enchant, enchant level and stacking amount if applicable
                val enchant = this.enchants.getFromLore(matcher.group("enchant"))
                val level = matcher.group("levelNumeral").romanToDecimal()
                val stacking = if (matcher.group("stacking").trimStart().matches("[\\d,]+\$".toRegex())) {
                    shouldBeSingleColumn = true
                    matcher.group("stacking")
                } else "empty"

                // Last found enchant
                lastEnchant = FormattedEnchant(enchant, level, stacking)

                if (!orderedEnchants.add(lastEnchant)) {
                    for (e: FormattedEnchant in orderedEnchants) {
                        if (lastEnchant?.let { e.compareTo(it) } == 0) {
                            lastEnchant = e
                            break
                        }
                    }
                }

                containsEnchant = true
                enchantsOnThisLine++
            }

            maxEnchantsPerLine = if (enchantsOnThisLine > maxEnchantsPerLine) enchantsOnThisLine else maxEnchantsPerLine

            if (!containsEnchant && lastEnchant != null) {
                lastEnchant.addLore(loreList[i])
                loreLines.add(loreList[i])
            }
        }

        if (orderedEnchants.isEmpty()) {
            loreCache.updateAfter(loreList)
            return
        }

        // If we have color parsing off and hide enchant descriptions on, remove them and return from method
        if (!config.colorEnchants.colorParsing) {
            if (config.hideEnchantDescriptions) {
                loreList.removeAll(loreLines)
                loreCache.updateAfter(loreList)
                if (chatComponent != null) editChatComponent(chatComponent, loreList)
                return
            }
            return
        }

        // Remove enchantment lines so we can insert ours
        loreList.subList(startEnchant, endEnchant + 1).clear()

        val insertEnchants: MutableList<String> = mutableListOf()
        val commaFormat = config.colorEnchants.commaFormat

        // Normal is leaving the formatting as Hypixel provides it
        if (config.format == EnchantParsingConfig.EnchantFormat.NORMAL) {
            var builder = StringBuilder()

            for ((i, orderedEnchant: FormattedEnchant) in orderedEnchants.withIndex()) {
                val comma = if (commaFormat == CommaFormat.COPY_ENCHANT) ", " else "§9, "

                builder.append(orderedEnchant.getFormattedString())
                if (i % maxEnchantsPerLine != maxEnchantsPerLine - 1) {
                    builder.append(comma)
                } else {
                    insertEnchants.add(builder.toString())

                    // This will only add enchant descriptions if there were any to begin with
                    if (!config.hideEnchantDescriptions) insertEnchants.addAll(orderedEnchant.getLore())

                    builder = StringBuilder()
                }
            }

            if (builder.isNotEmpty()) insertEnchants.add(builder.toString())

            // Check if there is a trailing space (therefore also a comma) and remove the last 2 chars
            if (insertEnchants.last().last() == ' ') {
                insertEnchants[insertEnchants.lastIndex] = insertEnchants.last().dropLast(if (commaFormat == CommaFormat.COPY_ENCHANT) 2 else 4)
            }

        // Compressed is always forcing 3 enchants per line, except when there is stacking enchant progress visible
        } else if (config.format == EnchantParsingConfig.EnchantFormat.COMPRESSED && !shouldBeSingleColumn) {
            var builder = StringBuilder()

            for ((i, orderedEnchant: FormattedEnchant) in orderedEnchants.withIndex()) {
                val comma = if (commaFormat == CommaFormat.COPY_ENCHANT) ", " else "§9, "

                builder.append(orderedEnchant.getFormattedString())
                if (i % 3 != 2) {
                    builder.append(comma)
                } else {
                    insertEnchants.add(builder.toString())
                    builder = StringBuilder()
                }
            }

            if (builder.isNotEmpty()) insertEnchants.add(builder.toString())

            // Check if there is a trailing space (therefore also a comma) and remove the last 2 chars
            if (insertEnchants.last().last() == ' ') {
                insertEnchants[insertEnchants.lastIndex] = insertEnchants.last().dropLast(if (commaFormat == CommaFormat.COPY_ENCHANT) 2 else 4)
            }

        // Stacked is always forcing 1 enchant per line
        } else {
            if (!config.hideEnchantDescriptions) {
                for (enchant: FormattedEnchant in orderedEnchants) {
                    insertEnchants.add(enchant.getFormattedString())
                    insertEnchants.addAll(enchant.getLore())
                }
            } else {
                for (enchant: FormattedEnchant in orderedEnchants) {
                    insertEnchants.add(enchant.getFormattedString())
                }
            }
        }

        // Add our parsed enchants back into the lore
        loreList.addAll(startEnchant, insertEnchants)
        // Cache parsed lore
        loreCache.updateAfter(loreList)

        // Alter the chat component value if one was passed
        if (chatComponent != null) {
            editChatComponent(chatComponent, loreList)
        }
    }

    private fun editChatComponent(chatComponent: IChatComponent, loreList: MutableList<String>) {
        // Drop 1st 2 and last 2 characters since when being rendered, the chat tooltip
        // adds formatting in front of, and at the end of the text, so we get rid of them
        // here and let Minecraft add them back, otherwise the text grows infinitely
        (chatComponent as ChatComponentText).text = loreList.joinToString("\n").drop(2).dropLast(2)

        val iterator = chatComponent.siblings.iterator()
        while (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }

    private fun accountForAndRemoveGrayEnchants(loreList: MutableList<String>, item: ItemStack) : Int {
        // If the item has no enchantmentTagList then there will be no gray enchants
        if (!item.isEnchanted() || item.enchantmentTagList.tagCount() == 0) return -1

        var lastGrayEnchant = -1
        val removeGrayEnchants = config.hideVanillaEnchants

        var i = 1
        for (total in 0 until (1 + item.enchantmentTagList.tagCount())) {
            val line = loreList[i]
            if (GRAY_ENCHANT_PATTERN.matcher(line).matches()) {
                lastGrayEnchant = i

                if (removeGrayEnchants) loreList.removeAt(i) else i++
            } else {
                i++
            }
        }

        return if (removeGrayEnchants) -1 else lastGrayEnchant
    }

    fun isEnabled() = LorenzUtils.inSkyBlock && config.enabled

    fun markCacheDirty() {
        loreCache.configChanged = true
    }

    class Cache {
        var cachedLoreBefore: List<String> = listOf()
        var cachedLoreAfter: List<String> = listOf()
        // So tooltip gets changed on the same item if the config was changed in the interim
        var configChanged = false

        fun updateBefore(loreBeforeModification: List<String>) {
            cachedLoreBefore = loreBeforeModification.toList()
        }

        fun updateAfter(loreAfterModification: List<String>) {
            cachedLoreAfter = loreAfterModification.toList()
            configChanged = false
        }

        fun isCached(loreBeforeModification: List<String>) : Boolean {
            if (configChanged || loreBeforeModification.size != cachedLoreBefore.size) return false

            for (i in loreBeforeModification.indices) {
                if (loreBeforeModification[i] != cachedLoreBefore[i]) return false
            }
            return true
        }
    }
}
