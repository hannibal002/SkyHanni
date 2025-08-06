package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.data.jsonobjects.repo.neu.NeuItemJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.UtilsPatterns
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Items
import net.minecraft.inventory.ContainerChest
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import java.util.regex.Matcher
//#if MC > 1.21
//$$ import net.minecraft.component.ComponentMap
//#endif

// Code taken from NotEnoughUpdates
class ItemResolutionQuery {

    //#if MC < 1.21
    private var compound: NBTTagCompound? = null

    //#else
    //$$ private var compound: ComponentMap? = null
    //#endif
    private var itemType: Item? = null
    private var knownInternalName: NeuInternalName? = null
    private var guiContext: GuiScreen? = null

    @SkyHanniModule
    companion object {

        private val petPattern = ".*(\\[Lvl .*] )§(.).*".toPattern()

        val petRarities = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")

        private val BAZAAR_ENCHANTMENT_PATTERN = "ENCHANTMENT_(\\D*)_(\\d+)".toPattern()

        private var renamedEnchantments: Map<String, String> = mapOf()
        private var shardNameOverrides: Map<String, String> = mapOf()

        @HandleEvent
        fun onRepoReload(event: RepositoryReloadEvent) {
            val data = event.getConstant<ItemsJson>("Items")
            renamedEnchantments = data.renamedEnchantments
            shardNameOverrides = data.shardNameOverrides
        }

        fun transformHypixelBazaarToNeuItemId(hypixelId: String): String {
            ItemUtils.bazaarOverrides[hypixelId]?.let {
                return it
            }
            val matcher = BAZAAR_ENCHANTMENT_PATTERN.matcher(hypixelId)
            if (matcher.matches()) {
                return matcher.group(1) + ";" + matcher.group(2)
            }
            return hypixelId.replace(":", "-")
        }

        fun findInternalNameByDisplayName(displayName: String, mayBeMangled: Boolean): NeuInternalName? {
            return filterInternalNameCandidates(
                findInternalNameCandidatesForDisplayName(displayName),
                displayName,
                mayBeMangled,
            )
        }

        private fun filterInternalNameCandidates(
            candidateInternalNames: Collection<String>,
            displayName: String,
            mayBeMangled: Boolean,
        ): NeuInternalName? {
            var itemName = displayName
            val isPet = itemName.contains("[Lvl ")
            var petRarity: String? = null
            if (isPet) {
                val matcher: Matcher = petPattern.matcher(itemName)
                if (matcher.matches()) {
                    itemName = itemName.replace(matcher.group(1), "").replace("✦", "").trim()
                    petRarity = matcher.group(2)
                }
            }
            val cleanDisplayName = itemName.removeColor()
            var bestMatch: NeuInternalName? = null
            var bestMatchLength = -1
            for (internalName in candidateInternalNames.map { it.toInternalName() }) {
                val unCleanItemDisplayName: String = EnoughUpdatesManager.getDisplayName(internalName)
                var cleanItemDisplayName = unCleanItemDisplayName.removeColor()
                if (cleanItemDisplayName.isEmpty()) continue
                if (isPet) {
                    if (!cleanItemDisplayName.contains("[Lvl {LVL}] ")) continue
                    cleanItemDisplayName = cleanItemDisplayName.replace("[Lvl {LVL}] ", "")
                    val matcher: Matcher = petPattern.matcher(unCleanItemDisplayName)
                    if (matcher.matches()) {
                        if (matcher.group(2) != petRarity) {
                            continue
                        }
                    }
                }

                val isMangledMatch = mayBeMangled && !cleanDisplayName.contains(cleanItemDisplayName)
                val isExactMatch = !mayBeMangled && cleanItemDisplayName != cleanDisplayName

                if (isMangledMatch || isExactMatch) {
                    continue
                }
                if (cleanItemDisplayName.length > bestMatchLength) {
                    bestMatchLength = cleanItemDisplayName.length
                    bestMatch = internalName
                }
            }
            return bestMatch
        }

        private fun findInternalNameCandidatesForDisplayName(displayName: String): Set<String> {
            val isPet = displayName.contains("[Lvl ")
            val cleanDisplayName = displayName.cleanString()
            val titleWordMap = EnoughUpdatesManager.titleWordMap
            val candidates = HashSet<String>()
            for (partialDisplayName in cleanDisplayName.split(" ")) {
                if (partialDisplayName.isEmpty()) continue
                if (!titleWordMap.containsKey(partialDisplayName)) continue
                val c: Set<String> = titleWordMap[partialDisplayName]?.keys ?: continue
                for (s in c) {
                    if (isPet && !s.contains(";")) continue
                    candidates.add(s)
                }
            }
            return candidates
        }

        fun resolveEnchantmentByName(displayName: String): NeuInternalName? =
            UtilsPatterns.enchantmentNamePattern.matchMatcher(displayName) {
                val name = group("name").trim().replace("'", "")
                val ultimate = group("format").lowercase().contains("§l")
                val prefix = if (ultimate && name != "Ultimate Wise" && name != "Ultimate Jerry") "ULTIMATE_" else ""
                val cleanedEnchantName = name.renamedEnchantmentCheck().replace(" ", "_").replace("-", "_").uppercase()
                "$prefix$cleanedEnchantName;${group("level").romanToDecimal()}".uppercase().toInternalName()
            }

        private fun String.renamedEnchantmentCheck(): String = renamedEnchantments[this] ?: this

        fun attributeNameToInternalName(attributeName: String): String? {
            var fixedAttributeName = attributeName.uppercase().replace(" ", "_")
            fixedAttributeName = shardNameOverrides[fixedAttributeName] ?: fixedAttributeName
            val shardName = "SHARD_$fixedAttributeName"
            return ItemUtils.bazaarOverrides[shardName]
        }
    }

    fun withItemStack(stack: ItemStack): ItemResolutionQuery {
        this.itemType = stack.item
        this.compound = stack.tagCompound
        return this
    }

    fun withKnownInternalName(internalName: NeuInternalName): ItemResolutionQuery {
        this.knownInternalName = internalName
        return this
    }

    fun withCurrentGuiContext(): ItemResolutionQuery {
        this.guiContext = Minecraft.getMinecraft().currentScreen
        return this
    }

    fun resolveInternalName(): NeuInternalName? {
        knownInternalName?.let { return it }
        val resolvedName = resolveFromSkyblock() ?: return resolveContextualName()
        return when (resolvedName.asString()) {
            "PET" -> resolvePetName()
            "RUNE", "UNIQUE_RUNE" -> resolveRuneName()
            "ENCHANTED_BOOK" -> resolveEnchantedBookNameFromNBT()
            "PARTY_HAT_CRAB", "PARTY_HAT_CRAB_ANIMATED" -> resolveCrabHatName()
            "ABICASE" -> resolvePhoneCase()
            "PARTY_HAT_SLOTH" -> resolveSlothHatName()
            "POTION" -> resolvePotionName()
            "BALLOON_HAT_2024", "BALLOON_HAT_2025" -> resolveBalloonHatName()
            "ATTRIBUTE_SHARD" -> resolveAttributeShardName()
            else -> resolvedName
        }
    }

    private fun resolvePetName(): NeuInternalName? {
        val petInfo = getExtraAttributes().getString("petInfo")
        if (petInfo.isNullOrEmpty()) return null
        try {
            val petInfoObject = ConfigManager.gson.fromJson(petInfo, JsonObject::class.java)
            val petId = petInfoObject["type"].asString
            val petTier = petInfoObject["tier"].asString
            val rarityIndex = petRarities.indexOf(petTier)
            val rawInternalName = petId.uppercase() + ";" + rarityIndex
            return rawInternalName.toInternalName()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error while resolving pet information",
                "petInfo" to petInfo,
            )
            return null
        }
    }

    private fun resolveRuneName(): NeuInternalName? {
        val runes = getExtraAttributes().getCompoundTag("runes")
        val runeName = runes.keySet.singleOrNull()
        if (runeName.isNullOrEmpty()) return null
        val rawInternalName = runeName.uppercase() + "_RUNE;" + runes.getInteger(runeName)
        return rawInternalName.toInternalName()
    }

    private fun resolveEnchantedBookNameFromNBT(): NeuInternalName? {
        val enchantments = getExtraAttributes().getCompoundTag("enchantments")
        val enchantName = enchantments.keySet.singleOrNull()
        if (enchantName.isNullOrEmpty()) return null
        val rawInternalName = enchantName.uppercase() + ";" + enchantments.getInteger(enchantName)
        return rawInternalName.toInternalName()
    }

    private fun resolveCrabHatName(): NeuInternalName {
        val crabHatYear = getExtraAttributes().getInteger("party_hat_year")
        val color = getExtraAttributes().getString("party_hat_color")
        val rawInternalName = "PARTY_HAT_CRAB_" + color.uppercase() + (if (crabHatYear == 2022) "_ANIMATED" else "")
        return rawInternalName.toInternalName()
    }

    private fun resolvePhoneCase(): NeuInternalName {
        val model = getExtraAttributes().getString("model")
        return ("ABICASE_" + model.uppercase()).toInternalName()
    }

    private fun resolveSlothHatName(): NeuInternalName {
        val emoji = getExtraAttributes().getString("party_hat_emoji")
        return ("PARTY_HAT_SLOTH_" + emoji.uppercase()).toInternalName()
    }

    private fun resolvePotionName(): NeuInternalName {
        val potion = getExtraAttributes().getString("potion")
        val potionLvl = getExtraAttributes().getInteger("potion_level")
        val potionName = getExtraAttributes().getString("potion_name").replace(" ", "_")
        val potionType = getExtraAttributes().getString("potion_type")
        val rawInternalName = if (potionName.isNotEmpty()) {
            "POTION_" + potionName.uppercase() + ";" + potionLvl
        } else if (!potion.isNullOrEmpty()) {
            "POTION_" + potion.uppercase() + ";" + potionLvl
        } else if (!potionType.isNullOrEmpty()) {
            "POTION_" + potionType.uppercase()
        } else {
            "WATER_BOTTLE"
        }
        return rawInternalName.toInternalName()
    }

    private fun resolveBalloonHatName(): NeuInternalName {
        val color = getExtraAttributes().getString("party_hat_color")
        val balloonHatYear = getExtraAttributes().getInteger("party_hat_year")
        val rawInternalName = "BALLOON_HAT_" + balloonHatYear + "_" + color.uppercase()
        return rawInternalName.toInternalName()
    }

    private fun resolveAttributeShardName(): NeuInternalName? {
        val attributes = getExtraAttributes().getCompoundTag("attributes")
        val attributeName = attributes.keySet.singleOrNull()
        if (attributeName.isNullOrEmpty()) return null
        val rawInternalName = "ATTRIBUTE_SHARD_" + attributeName.uppercase() + ";" + attributes.getInteger(attributeName)
        return rawInternalName.toInternalName()
    }

    private fun resolveItemInCatacombsRngMeter(): NeuInternalName? {
        val lore = compound.getLore()
        if (lore.size > 16) {
            val s = lore[15]
            if (s == "§7Selected Drop") {
                val displayName = lore[16]
                return findInternalNameByDisplayName(displayName, false)
            }
        }

        return null
    }

    private fun resolveItemInAttributeMenu(lore: List<String>): NeuInternalName? {
        UtilsPatterns.attributeSourcePattern.firstMatcher(lore) {
            return attributeNameToInternalName(group("source"))?.toInternalName()
        }
        return null
    }

    private fun resolveItemInHuntingBoxMenu(displayName: String): NeuInternalName? {
        return attributeNameToInternalName(displayName.removeColor())?.toInternalName()
    }

    private fun resolveContextualName(): NeuInternalName? {
        val chest = guiContext as? GuiChest ?: return null
        val inventorySlots = chest.inventorySlots as ContainerChest
        val guiName = InventoryUtils.openInventoryName()
        val isOnBazaar: Boolean = isBazaar(inventorySlots.lowerChestInventory)
        var displayName: String = ItemUtils.getDisplayName(compound) ?: return null
        displayName = displayName.removePrefix("§6§lSELL ").removePrefix("§a§lBUY ")
        if (itemType === Items.enchanted_book && isOnBazaar && compound != null) {
            return resolveEnchantmentByName(displayName)
        }
        if (itemType === Items.skull && displayName.contains("Essence")) {
            findInternalNameByDisplayName(displayName, false)?.let { return it }
        }

        return if (displayName.endsWith("Enchanted Book") && guiName.startsWith("Superpairs")) {
            var enchantmentIdCandidate: NeuInternalName? = null
            for (loreLine in compound.getLore()) {
                enchantmentIdCandidate = resolveEnchantmentByName(loreLine)
                if (enchantmentIdCandidate != null) break
            }
            enchantmentIdCandidate
        } else if (guiName == "Catacombs RNG Meter") {
            resolveItemInCatacombsRngMeter()
        } else if (guiName.startsWith("Choose Pet")) {
            findInternalNameByDisplayName(displayName, false)
        } else if (guiName.endsWith("Experimentation Table RNG")) {
            resolveEnchantmentByName(displayName)
        } else if (guiName == "Attribute Menu") {
            resolveItemInAttributeMenu(compound.getLore())
        } else if (guiName == "Hunting Box" || guiName == "Fusion Box" || guiName == "Shard Fusion") {
            resolveItemInHuntingBoxMenu(displayName)
        } else null
    }

    private fun isBazaar(chest: IInventory): Boolean {
        if (InventoryUtils.openInventoryName().startsWith("Bazaar ➜ ")) {
            return true
        }
        val bazaarSlot = chest.sizeInventory - 5
        if (bazaarSlot < 0) return false
        val stackInSlot = chest.getStackInSlot(bazaarSlot) ?: return false
        if (stackInSlot.stackSize == 0) return false

        val lore: List<String> = stackInSlot.getLore()
        return lore.contains("§7To Bazaar")
    }

    private fun getExtraAttributes(): NBTTagCompound = compound?.extraAttributes ?: NBTTagCompound()

    private fun resolveFromSkyblock(): NeuInternalName? {
        val internalName = getExtraAttributes().getString("id")
        if (internalName.isNullOrEmpty()) return null
        return internalName.uppercase().replace(":", "-").toInternalName()
    }

    private fun resolveToItemJson(): NeuItemJson? {
        val internalName = resolveInternalName() ?: return null
        return EnoughUpdatesManager.getItemById(internalName)
    }

    fun resolveToItemStack(): ItemStack? {
        val neuItem = resolveToItemJson() ?: return null
        return EnoughUpdatesManager.neuItemToStack(neuItem)
    }
}
