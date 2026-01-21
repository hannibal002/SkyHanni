package at.hannibal2.skyhanni.api.enoughupdates

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.config.ConfigManager
import at.hannibal2.skyhanni.data.jsonobjects.repo.ItemsJson
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.extraAttributes
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.compat.container
import at.hannibal2.skyhanni.utils.compat.getCompoundOrDefault
import at.hannibal2.skyhanni.utils.compat.getIntOrDefault
import at.hannibal2.skyhanni.utils.compat.getStringOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.regex.Matcher

// Code taken from NotEnoughUpdates
class ItemResolutionQuery {

    private var compound: DataComponentMap? = null

    private var itemType: Item? = null
    private var knownInternalName: String? = null
    private var guiContext: Screen? = null

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

        fun findInternalNameByDisplayName(displayName: String, mayBeMangled: Boolean): String? {
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
        ): String? {
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
            var bestMatch: String? = null
            var bestMatchLength = -1
            for (internalName in candidateInternalNames) {
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

        fun resolveEnchantmentByName(displayName: String): String? =
            UtilsPatterns.enchantmentNamePattern.matchMatcher(displayName) {
                val name = group("name").trim().replace("'", "")
                val ultimate = group("format").lowercase().contains("§l")
                val prefix = if (ultimate && name != "Ultimate Wise" && name != "Ultimate Jerry") "ULTIMATE_" else ""
                val cleanedEnchantName = name.renamedEnchantmentCheck().replace(" ", "_").replace("-", "_").uppercase()
                "$prefix$cleanedEnchantName;${group("level").romanToDecimal()}".uppercase()
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
        this.compound = stack.components
        return this
    }

    fun withKnownInternalName(internalName: String): ItemResolutionQuery {
        this.knownInternalName = internalName
        return this
    }

    fun withCurrentGuiContext(): ItemResolutionQuery {
        this.guiContext = Minecraft.getInstance().screen
        return this
    }

    fun resolveInternalName(): String? {
        knownInternalName?.let {
            return it
        }
        var resolvedName = resolveFromSkyblock()
        resolvedName = if (resolvedName == null) {
            resolveContextualName()
        } else {
            when (resolvedName) {
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

        return resolvedName
    }

    private fun resolvePetName(): String? {
        val petInfo = getExtraAttributes().getStringOrDefault("petInfo")
        if (petInfo.isEmpty()) return null
        try {
            val petInfoObject = ConfigManager.gson.fromJson(petInfo, JsonObject::class.java)
            val petId = petInfoObject["type"].asString
            val petTier = petInfoObject["tier"].asString
            val rarityIndex = petRarities.indexOf(petTier)
            return petId.uppercase() + ";" + rarityIndex
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error while resolving pet information",
                "petInfo" to petInfo,
            )
            return null
        }
    }

    private fun resolveRuneName(): String? {
        val runes = getExtraAttributes().getCompoundOrDefault("runes")
        val runeName = runes.keySet().singleOrNull()
        if (runeName.isNullOrEmpty()) return null
        return runeName.uppercase() + "_RUNE;" + runes.getIntOrDefault(runeName)
    }

    private fun resolveEnchantedBookNameFromNBT(): String? {
        val enchantments = getExtraAttributes().getCompoundOrDefault("enchantments")
        val enchantName = enchantments.keySet().singleOrNull()
        if (enchantName.isNullOrEmpty()) return null
        return enchantName.uppercase() + ";" + enchantments.getIntOrDefault(enchantName)
    }

    private fun resolveCrabHatName(): String {
        val crabHatYear = getExtraAttributes().getIntOrDefault("party_hat_year")
        val color = getExtraAttributes().getStringOrDefault("party_hat_color")
        return "PARTY_HAT_CRAB_" + color.uppercase() + (if (crabHatYear == 2022) "_ANIMATED" else "")
    }

    private fun resolvePhoneCase(): String {
        val model = getExtraAttributes().getStringOrDefault("model")
        return "ABICASE_" + model.uppercase()
    }

    private fun resolveSlothHatName(): String {
        val emoji = getExtraAttributes().getStringOrDefault("party_hat_emoji")
        return "PARTY_HAT_SLOTH_" + emoji.uppercase()
    }

    private fun resolvePotionName(): String {
        val potion = getExtraAttributes().getStringOrDefault("potion")
        val potionLvl = getExtraAttributes().getIntOrDefault("potion_level")
        val potionName = getExtraAttributes().getStringOrDefault("potion_name").replace(" ", "_")
        val potionType = getExtraAttributes().getStringOrDefault("potion_type")
        return if (potionName.isNotEmpty()) {
            "POTION_" + potionName.uppercase() + ";" + potionLvl
        } else if (potion.isNotEmpty()) {
            "POTION_" + potion.uppercase() + ";" + potionLvl
        } else if (potionType.isNotEmpty()) {
            "POTION_" + potionType.uppercase()
        } else {
            "WATER_BOTTLE"
        }
    }

    private fun resolveBalloonHatName(): String {
        val color = getExtraAttributes().getStringOrDefault("party_hat_color")
        val balloonHatYear = getExtraAttributes().getIntOrDefault("party_hat_year")
        return "BALLOON_HAT_" + balloonHatYear + "_" + color.uppercase()
    }

    private fun resolveAttributeShardName(): String? {
        val attributes = getExtraAttributes().getCompoundOrDefault("attributes")
        val attributeName = attributes.keySet().singleOrNull()
        if (attributeName.isNullOrEmpty()) return null
        return "ATTRIBUTE_SHARD_" + attributeName.uppercase() + ";" + attributes.getIntOrDefault(attributeName)
    }

    private fun resolveItemInCatacombsRngMeter(): String? {
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

    private fun resolveItemInAttributeMenu(lore: List<String>): String? {
        UtilsPatterns.attributeSourcePattern.firstMatcher(lore) {
            return attributeNameToInternalName(group("source"))
        }
        return null
    }

    private fun resolveItemInHuntingBoxMenu(displayName: String): String? {
        return attributeNameToInternalName(displayName.removeColor())
    }

    private fun resolveContextualName(): String? {
        val chest = guiContext as? ContainerScreen ?: return null
        val inventorySlots = chest.container as ChestMenu
        val guiName = InventoryUtils.openInventoryName()
        val isOnBazaar: Boolean = isBazaar(inventorySlots.container)
        var displayName: String = ItemUtils.getDisplayName(compound) ?: return null
        displayName = displayName.removePrefix("§6§lSELL ").removePrefix("§a§lBUY ")
        if (itemType === Items.ENCHANTED_BOOK && isOnBazaar && compound != null) {
            return resolveEnchantmentByName(displayName)
        }
        if (itemType === Items.PLAYER_HEAD && displayName.contains("Essence")) {
            findInternalNameByDisplayName(displayName, false)?.let { return it }
        }
        if (displayName.endsWith("Enchanted Book") && guiName.startsWith("Superpairs")) {
            for (loreLine in compound.getLore()) {
                val enchantmentIdCandidate = resolveEnchantmentByName(loreLine)
                if (enchantmentIdCandidate != null) return enchantmentIdCandidate
            }
            return null
        }
        if (guiName == "Catacombs RNG Meter") {
            return resolveItemInCatacombsRngMeter()
        }
        if (guiName.startsWith("Choose Pet")) {
            return findInternalNameByDisplayName(displayName, false)
        }
        if (guiName.endsWith("Experimentation Table RNG")) {
            return resolveEnchantmentByName(displayName)
        }
        if (guiName == "Attribute Menu") {
            return resolveItemInAttributeMenu(compound.getLore())
        }
        if (guiName == "Hunting Box" || guiName == "Fusion Box" || guiName == "Shard Fusion") {
            return resolveItemInHuntingBoxMenu(displayName)
        }
        if (guiName == "Confirm Fusion") {
            return resolveItemInHuntingBoxMenu(compound.getLore().firstOrNull() ?: return null)
        }
        if (guiName == "Dye Compendium") {
            return findInternalNameByDisplayName(displayName, false)
        }
        return null
    }

    private fun isBazaar(chest: Container): Boolean {
        if (InventoryUtils.openInventoryName().startsWith("Bazaar ➜ ")) {
            return true
        }
        val bazaarSlot = chest.containerSize - 5
        if (bazaarSlot < 0) return false
        val stackInSlot = chest.getItem(bazaarSlot) ?: return false
        if (stackInSlot.count == 0) return false

        val lore: List<String> = stackInSlot.getLore()
        return lore.contains("§7To Bazaar")
    }

    private fun getExtraAttributes(): CompoundTag = compound?.extraAttributes ?: CompoundTag()

    private fun resolveFromSkyblock(): String? {
        val internalName = getExtraAttributes().getStringOrDefault("id")
        if (internalName.isEmpty()) return null
        return internalName.uppercase().replace(":", "-")
    }

    private fun resolveToItemListJson(): JsonObject? {
        val internalName = resolveInternalName() ?: return null
        return EnoughUpdatesManager.getItemById(internalName)
    }

    fun resolveToItemStack(): ItemStack? {
        val json = resolveToItemListJson() ?: return null
        return EnoughUpdatesManager.jsonToStack(json)
    }
}
