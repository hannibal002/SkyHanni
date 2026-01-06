package at.hannibal2.skyhanni.features.misc.items.enchants

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.features.inventory.EnchantParsingConfig
import at.hannibal2.skyhanni.config.features.inventory.EnchantParsingConfig.CommaFormat
import at.hannibal2.skyhanni.events.ChatHoverEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.events.minecraft.ToolTipTextEvent
import at.hannibal2.skyhanni.features.chroma.ChromaManager
import at.hannibal2.skyhanni.mixins.hooks.GuiChatHook
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimalIfNecessary
import at.hannibal2.skyhanni.utils.OtherModsSettings
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHypixelEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.chat.TextHelper.asComponent
import at.hannibal2.skyhanni.utils.compat.append
import at.hannibal2.skyhanni.utils.compat.createHoverEvent
import at.hannibal2.skyhanni.utils.compat.formattedTextCompat
import at.hannibal2.skyhanni.utils.compat.unformattedTextCompat
import at.hannibal2.skyhanni.utils.compat.withColor
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import at.hannibal2.skyhanni.utils.system.PlatformUtils
import com.mojang.blaze3d.systems.RenderSystem
import java.util.TreeSet
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * Modified Enchant Parser from [SkyblockAddons](https://github.com/BiscuitDevelopment/SkyblockAddons/blob/main/src/main/java/codes/biscuit/skyblockaddons/features/enchants/EnchantManager.java)
 */
@SkyHanniModule
object EnchantParser {

    private val config get() = SkyHanniMod.feature.inventory.enchantParsing

    val patternGroup = RepoPattern.group("misc.items.enchantparsing")
    // Pattern to check that the line contains ONLY enchants (and the other bits that come with a valid enchant line)
    /**
     * REGEX-TEST: §9Champion VI §r§81.2M
     * REGEX-TEST: §9Cultivating VII §r§83,271,717
     * REGEX-TEST: §5§o§9Compact X
     * REGEX-TEST: §5§o§d§l§d§lChimera V§9, §9Champion X§9, §9Cleave VI
     * REGEX-TEST: §d§l§d§lWisdom V§9, §9Depth Strider III§9, §9Feather Falling X
     * REGEX-TEST: §9Compact X§9, §9Efficiency V§9, §9Experience IV
     * REGEX-TEST: §r§d§lUltimate Wise V§r§9, §r§9Champion X§r§9, §r§9Cleave V
     */
    val enchantmentExclusivePattern by patternGroup.pattern(
        "exclusive",
        "^(?:(?:§.)+[A-Za-z][A-Za-z '-]+ (?:[IVXLCDM]+|[0-9]+)(?:(?:§r)?§9, |\$| §r§8\\d{1,3}(?:[,.]\\d{1,3})*)[kKmMbB]?)+\$",
    )

    /**
     * REGEX-TEST: §9Champion VI §r§81.2M
     * REGEX-TEST: §9Cultivating VII §r§83,271,717
     * REGEX-TEST: §5§o§9Compact X
     * REGEX-TEST: §5§o§d§l§d§lChimera V§9, §9Champion X§9, §9Cleave VI
     * REGEX-TEST: §d§l§d§lWisdom V§9, §9Depth Strider III§9, §9Feather Falling X
     * REGEX-TEST: §9Compact X§9, §9Efficiency V§9, §9Experience IV
     * REGEX-TEST: §r§d§lUltimate Wise V§r§9, §r§9Champion X§r§9, §r§9Cleave V
     */
    @Suppress("MaxLineLength")
    val enchantmentPattern by patternGroup.pattern(
        "enchants.new",
        "(?:§7§l|§d§l|§9|§7)(?<enchant>[A-Za-z][A-Za-z '-]+) (?<levelNumeral>[IVXLCDM]+|[0-9]+)(?<stacking>(?:§r)?§9, |\$| §r§8\\d{1,3}(?:[,.]\\d{1,3})*[kKmMbB]?)",
    )

    private var currentItem: ItemStack? = null

    private var startEnchant = -1
    private var endEnchant = -1

    // Stacking enchants with their progress visible should have the
    // enchants stacked in a single column
    private var shouldBeSingleColumn = false

    private val stackingEnchants: MutableList<Enchant.Stacking> = mutableListOf()

    // Used to determine how many enchants are used on each line
    // for this particular item, since consistency is not Hypixel's strong point
    private var maxEnchantsPerLine = 0
    private var loreLines: MutableList<Component> = mutableListOf()
    private var orderedEnchants: TreeSet<FormattedEnchant> = TreeSet()

    private val loreCache: EnchantCache = EnchantCache()

    val isSbaLoaded by lazy { PlatformUtils.isModInstalled("skyblockaddons") }

    // Maps for all enchants
    private var enchants: EnchantsJson = EnchantsJson()

    @HandleEvent
    fun onRepoReload(event: RepositoryReloadEvent) {
        this.enchants = event.getConstant<EnchantsJson>("Enchants")
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        // Add observers to config options that would need us to mark cache dirty
        ConditionalUtils.onToggle(
            config.colorParsing,
            config.format,
            config.perfectEnchantColor,
            config.boldPerfectEnchant,
            config.greatEnchantColor,
            config.goodEnchantColor,
            config.poorEnchantColor,
            config.advancedEnchantColors.useAdvancedPerfectColor,
            config.advancedEnchantColors.advancedPerfectColor,
            config.advancedEnchantColors.useAdvancedGreatColor,
            config.advancedEnchantColors.advancedGreatColor,
            config.advancedEnchantColors.useAdvancedGoodColor,
            config.advancedEnchantColors.advancedGoodColor,
            config.advancedEnchantColors.useAdvancedPoorColor,
            config.advancedEnchantColors.advancedPoorColor,
            config.commaFormat,
            config.hideVanillaEnchants,
            config.hideEnchantDescriptions,
            ChromaManager.config.enabled,
        ) {
            markCacheDirty()
        }
    }

    @HandleEvent(onlyOnSkyblock = true)
    fun onTooltipEvent(event: ToolTipTextEvent) {
        // Only proceed if we are on the render thread as other mods could end up triggering
        // the ToolTipTextEvent from different threads which breaks the parser. (i.e. REI
        // during item searching since it needs to check tooltips for search queries)
        if (!RenderSystem.isOnRenderThread()) return

        // If enchants doesn't have any enchant data then we have no data to parse enchants correctly
        if (!this.enchants.hasEnchantData()) return

        currentItem = event.itemStack

        // The enchants we expect to find in the lore, found from the items NBT data
        val enchants = event.itemStack.getHypixelEnchantments() ?: return

        parseEnchants(event.toolTip, enchants, null)
    }

    /**
     * For tooltips that are shown when hovering over an item from /show
     */
    @HandleEvent
    fun onChatHoverEvent(event: ChatHoverEvent) {
        return // TODO: Deal with this method once Hypixel fixes /show
//         if (event.getHoverEvent().action() != HoverEvent.Action.SHOW_TEXT) return
//         if (!isEnabled() || !this.enchants.hasEnchantData()) return
//
//         currentItem = null
//
//         val lore = event.getHoverEvent().value().formattedTextCompat().split("\n").toMutableList()
//
//         // Since we don't get given an item stack from /show, we pass an empty enchants map and
//         // use all enchants from the Enchants class instead
//         parseEnchants(lore, mapOf(), event.component)
    }

    private fun warnAaronMaxEnchant() {
        if (!PlatformUtils.isModInstalled("aaron-mod")) return
        val aaron = OtherModsSettings.aaron()

        if (aaron.isEnabled("skyblock.enchantments.rainbowMaxEnchants")) {
            if (config.colorParsing.get()) {
                ChatUtils.clickToActionOrDisable(
                    "SkyHanni's enchant parsing breaks with Aaron's Mod's 'Rainbow Max Enchants'",
                    config::colorParsing,
                    "turn off Aaron's Mod's Rainbow Max Enchants",
                    { removeAaronMaxEnchant() }
                )
            }
            if (config.hideEnchantDescriptions.get()) {
                ChatUtils.clickToActionOrDisable(
                    "SkyHanni's hide enchant descriptions breaks with Aaron's Mod's 'Rainbow Max Enchants'",
                    config::hideEnchantDescriptions,
                    "turn off Aaron's Mod's Rainbow Max Enchants",
                    { removeAaronMaxEnchant() }
                )
            }
        }
    }

    private fun removeAaronMaxEnchant() {
        val aaron = OtherModsSettings.aaron()
        if (aaron.isEnabled("skyblock.enchantments.rainbowMaxEnchants")) {
            aaron.setBoolean("skyblock.enchantments.rainbowMaxEnchants", false)
            ChatUtils.chat("§aDisabled Aaron's Mod's Rainbow Max Enchants!")
        } else {
            ChatUtils.userError("Aaron's Mod's Rainbow Max Enchants is already disabled!")
        }
    }

    private fun parseEnchants(
        loreList: MutableList<Component>,
        enchants: Map<String, Int>,
        chatComponent: Component?,
    ) {
        // Check if the lore is already cached so continuous hover isn't 1 fps
        if (loreCache.isCached(loreList)) {
            loreList.clear()
            if (loreCache.cachedLoreAfter.isNotEmpty()) {
                loreList.addAll(loreCache.cachedLoreAfter)
            } else {
                loreList.addAll(loreCache.cachedLoreBefore)
            }
            // Need to still set replacement component even if its cached
            if (chatComponent != null) editChatComponent(chatComponent, loreList)
            return
        }
        loreCache.updateBefore(loreList)

        // Find where the enchants start and end
        enchantStartAndEnd(loreList, enchants)

        if (endEnchant == -1) {
            loreCache.updateAfter(loreList)
            return
        }

        stackingEnchants.clear()
        shouldBeSingleColumn = false
        loreLines = mutableListOf()
        orderedEnchants = TreeSet()
        maxEnchantsPerLine = 0

        // Order all enchants
        orderEnchants(loreList)

        if (orderedEnchants.isEmpty()) {
            loreCache.updateAfter(loreList)
            return
        }

        warnAaronMaxEnchant()

        // If we have color parsing off and hide enchant descriptions on, remove them and return from method
        if (!config.colorParsing.get()) {
            if (config.hideEnchantDescriptions.get()) {
                if (itemIsBook()) {
                    loreCache.updateAfter(loreList)
                    return
                }
                loreList.removeAll(loreLines)
                loreCache.updateAfter(loreList)
                if (chatComponent != null) editChatComponent(chatComponent, loreList)
                return
            }
            loreCache.updateAfter(loreList)
            return
        }

        val insertEnchants: MutableList<Component> = mutableListOf()

        // Format enchants based on format config option
        try {
            formatEnchants(insertEnchants)
        } catch (e: ArithmeticException) {
            ErrorManager.logErrorWithData(
                e,
                "Item has enchants in nbt but none were found?",
                "item" to currentItem,
                "loreList" to loreList,
                "nbt" to currentItem?.getExtraAttributes(),
            )
            return
        } catch (e: ConcurrentModificationException) {
            ErrorManager.logErrorWithData(
                e,
                "ConcurrentModificationException whilst formatting enchants",
                "loreList" to loreList,
                "format" to config.format.get(),
                "orderedEnchants" to orderedEnchants.toString(),
                "currentThread" to Thread.currentThread().name,
            )
        }

        // Remove enchantment lines so we can insert ours
        try {
            loreList.subList(startEnchant, endEnchant + 1).clear()
        } catch (e: IndexOutOfBoundsException) {
            ErrorManager.logErrorWithData(
                e,
                "Error parsing enchantment info from item",
                "item" to currentItem,
                "loreList" to loreList,
                "startEnchant" to startEnchant,
                "endEnchant" to endEnchant,
            )
            return
        }

        // Add our parsed enchants back into the lore
        loreList.addAll(startEnchant, insertEnchants)

        if (config.stackingEnchantProgress) {
            // TODO check if SBA's feature is enabled and show a chat prompt to decide what to disable. Maybe use OtherModsSettings.kt

            stackingEnchants.forEach { stacking ->
                currentItem?.let { item ->
                    loreList.add(loreList.size - 1, Component.literal(stacking.progressString(item)))
                }
            }
        }

        // Cache parsed lore
        loreCache.updateAfter(loreList)

        // Alter the chat component value if one was passed
        if (chatComponent != null) {
            editChatComponent(chatComponent, loreList)
        }
    }

    private fun enchantStartAndEnd(loreList: MutableList<Component>, enchants: Map<String, Int>) {
        var startEnchant = -1
        var endEnchant = -1

        for (i in 0 until loreList.size) {
            val strippedLine = loreList[i].unformattedTextCompat()

            if (startEnchant == -1) {
                if (this.enchants.containsEnchantment(enchants, loreList[i].formattedTextCompat())) startEnchant = i
            } else if (strippedLine.trim().isEmpty() && endEnchant == -1) endEnchant = i - 1
        }

        this.startEnchant = startEnchant
        this.endEnchant = endEnchant
    }

    private fun orderEnchants(loreList: MutableList<Component>) {
        var lastEnchant: FormattedEnchant? = null

        val isRoman = !SkyHanniMod.feature.misc.replaceRomanNumerals.get()
        val regex = "[\\d,.kKmMbB]+\$".toRegex()
        for (i in startEnchant..endEnchant) {
            val matcher = enchantmentPattern.matcher(loreList[i].formattedTextCompat())
            var containsEnchant = false
            var enchantsOnThisLine = 0

            while (matcher.find()) {
                // Pull enchant, enchant level and stacking amount if applicable
                val enchant = this.enchants.getFromLore(matcher.group("enchant"))
                val level = matcher.group("levelNumeral").romanToDecimalIfNecessary()
                val stacking = if (matcher.group("stacking").trimStart().removeColor().matches(regex)) {
                    shouldBeSingleColumn = true
                    matcher.group("stacking")
                } else "empty"

                if (enchant is Enchant.Stacking) {
                    stackingEnchants.add(enchant)
                }

                // Last found enchant
                lastEnchant = FormattedEnchant(enchant, level, stacking, isRoman)

                if (!orderedEnchants.add(lastEnchant)) {
                    for (formattedEnchant: FormattedEnchant in orderedEnchants) {
                        if (lastEnchant?.let { formattedEnchant.compareTo(it) } == 0) {
                            lastEnchant = formattedEnchant
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
    }

    private fun formatEnchants(insertEnchants: MutableList<Component>) {
        // Normal is leaving the formatting as Hypixel provides it
        if (config.format.get() == EnchantParsingConfig.EnchantFormat.NORMAL) {
            normalFormatting(insertEnchants)
            // Compressed is always forcing 3 enchants per line, except when there is stacking enchant progress visible
        } else if (config.format.get() == EnchantParsingConfig.EnchantFormat.COMPRESSED && !shouldBeSingleColumn) {
            compressedFormatting(insertEnchants)
            // Stacked is always forcing 1 enchant per line
        } else {
            stackedFormatting(insertEnchants)
        }
    }

    private fun normalFormatting(insertEnchants: MutableList<Component>) {
        val commaFormat = config.commaFormat.get()
        var component = Component.empty()

        val lastElement = orderedEnchants.last
        for ((i, orderedEnchant: FormattedEnchant) in orderedEnchants.withIndex()) {
            component = component.append(orderedEnchant.getComponent(currentItem))

            if (i % maxEnchantsPerLine != maxEnchantsPerLine - 1 && orderedEnchant != lastElement) {
                // Add comma
                if (commaFormat == CommaFormat.COPY_ENCHANT)
                    component.siblings.last().append(", ")
                else
                    component.append(Component.literal(", ").withColor(ChatFormatting.BLUE))
            } else {
                insertEnchants.add(component)

                // This will only add enchant descriptions if there were any to begin with
                if (!config.hideEnchantDescriptions.get() || itemIsBook()) insertEnchants.addAll(orderedEnchant.getLore())

                component = Component.empty()
            }
        }

        if (component != Component.empty()) insertEnchants.add(component)
    }

    private fun compressedFormatting(insertEnchants: MutableList<Component>) {
        val commaFormat = config.commaFormat.get()
        var component = Component.empty()

        val lastElement = orderedEnchants.last
        for ((i, orderedEnchant: FormattedEnchant) in orderedEnchants.withIndex()) {
            component = component.append(orderedEnchant.getComponent(currentItem))

            if (itemIsBook() && maxEnchantsPerLine == 1) {
                insertEnchants.add(component)
                insertEnchants.addAll(orderedEnchant.getLore())
                component = Component.empty()
            } else {
                if (i % 3 != 2 && orderedEnchant != lastElement) {
                    // Add comma
                    if (commaFormat == CommaFormat.COPY_ENCHANT)
                        component.siblings.last().append(", ")
                    else
                        component.append(Component.literal(", ").withColor(ChatFormatting.BLUE))
                } else {
                    insertEnchants.add(component)
                    component = Component.empty()
                }
            }
        }

        if (component != Component.empty()) insertEnchants.add(component)
    }

    private fun stackedFormatting(insertEnchants: MutableList<Component>) {
        if (!config.hideEnchantDescriptions.get() || itemIsBook()) {
            for (enchant: FormattedEnchant in orderedEnchants) {
                insertEnchants.add(enchant.getComponent(currentItem))
                insertEnchants.addAll(enchant.getLore())
            }
        } else {
            for (enchant: FormattedEnchant in orderedEnchants) {
                insertEnchants.add(enchant.getComponent(currentItem))
            }
        }
    }

    private fun editChatComponent(chatComponent: Component, loreList: MutableList<Component>) {
        val text = loreList.joinToString("\n").dropLast(2)

        // Just set the component text to the entire lore list instead of reconstructing the entire siblings tree
        val chatComponentText = text.asComponent()
        val hoverEvent = createHoverEvent(chatComponent.style.hoverEvent?.action(), chatComponentText) ?: return

        GuiChatHook.replaceOnlyHoverEvent(hoverEvent)
    }

    private fun itemIsBook(): Boolean {
        return currentItem?.getItemCategoryOrNull() == ItemCategory.ENCHANTED_BOOK
    }

    // We don't check if the main toggle here since we still need to go into
    // the parseEnchants method to deal with hiding vanilla enchants
    // and enchant descriptions
    fun isEnabled() = SkyBlockUtils.inSkyBlock

    private fun markCacheDirty() {
        loreCache.configChanged = true
    }
}
