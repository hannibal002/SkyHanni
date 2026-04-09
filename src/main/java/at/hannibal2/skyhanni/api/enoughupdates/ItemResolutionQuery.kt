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
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.ItemUtils.getLoreComponent
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalName
import at.hannibal2.skyhanni.utils.NeuInternalName.Companion.toInternalNames
import at.hannibal2.skyhanni.utils.NumberUtil.romanToDecimal
import at.hannibal2.skyhanni.utils.RegexUtils.anyMatches
import at.hannibal2.skyhanni.utils.RegexUtils.firstComponentMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.equalsOneOf
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.indexOfFirstOrNull
import at.hannibal2.skyhanni.utils.compat.container
import at.hannibal2.skyhanni.utils.compat.formattedTextCompatLeadingWhiteLessResets
import at.hannibal2.skyhanni.utils.compat.getCompoundOrDefault
import at.hannibal2.skyhanni.utils.compat.getIntOrDefault
import at.hannibal2.skyhanni.utils.compat.getStringOrDefault
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.core.component.DataComponentMap
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// Code taken from NotEnoughUpdates
class ItemResolutionQuery {

    private var compound = DataComponentMap.EMPTY

    private var itemType: Item? = null
    private var knownInternalName: NeuInternalName? = null
    private var guiContext: Screen? = null

    @SkyHanniModule
    companion object {

        private val patternGroup = RepoPattern.group("misc.itemresolution")

        // <editor-fold desc="Patterns">
        /**
         * REGEX-TEST: §r§7[Lvl 100] §r§6Scatha
         * REGEX-TEST: §r§7[Lvl 200] §r§6Golden Dragon§5 ✦
         */
        private val petPattern by patternGroup.pattern(
            "pet",
            "(?:§.)*\\[Lvl (?<level>\\d+)] (?:§.)*§(?<rarity>.)(?<name>[^§]+)(?:(?:§.)* ✦)?",
        )

        /**
         * REGEX-TEST: §aCondor
         * REGEX-TEST: §aCondor §d§lNEW SHARD
         */
        private val shardPattern by patternGroup.pattern(
            "shard",
            "(?<name>§.[^§]+)(?: §d§lNEW SHARD)?",
        )

        private val toBazaarPattern by patternGroup.pattern(
            "to-bazaar",
            "To Bazaar",
        )
        // </editor-fold>

        val petRarities = listOf("COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY", "MYTHIC")

        private val BAZAAR_ENCHANTMENT_PATTERN = "ENCHANTMENT_(\\D*)_(\\d+)".toPattern()

        private var renamedEnchantments = emptyMap<String, String>()
        private var shardNameOverrides = emptyMap<String, String>()

        @HandleEvent
        fun onRepoReload(event: RepositoryReloadEvent) {
            val data = event.getConstant<ItemsJson>("Items")
            renamedEnchantments = data.renamedEnchantments
            shardNameOverrides = data.shardNameOverrides
        }

        fun transformHypixelBazaarToNeuItemId(hypixelId: String): String {
            ItemUtils.bazaarOverrides[hypixelId]?.let { return it }
            BAZAAR_ENCHANTMENT_PATTERN.matchMatcher(hypixelId) {
                return "${group(1)};${group(2)}"
            }
            return hypixelId.replace(":", "-")
        }

        fun findInternalNameByDisplayName(
            displayName: String,
            mayBeMangled: Boolean,
        ): NeuInternalName? = filterInternalNameCandidates(
            findInternalNameCandidatesForDisplayName(displayName),
            displayName,
            mayBeMangled,
        )

        // TODO use components
        private fun filterInternalNameCandidates(
            candidateInternalNames: Collection<String>,
            displayName: String,
            mayBeMangled: Boolean,
        ): NeuInternalName? {
            val (itemName, petRarity) = petPattern.matchMatcher(displayName) {
                group("name") to group("rarity")
            } ?: (displayName to null)

            val cleanItemName = itemName.removeColor()
            var bestMatch: NeuInternalName? = null
            var bestMatchLength = -1

            loop@ for (internalName in candidateInternalNames.toInternalNames()) {
                val candidateName = EnoughUpdatesManager.getDisplayName(internalName)
                var cleanCandidateName = candidateName.removeColor()
                if (cleanCandidateName.isEmpty()) continue
                if (petPattern.matches(itemName)) {
                    if ("[Lvl {LVL}] " !in cleanCandidateName) continue
                    cleanCandidateName = cleanCandidateName.replace("[Lvl {LVL}] ", "")
                    petPattern.matchMatcher(candidateName) {
                        if (group("rarity") != petRarity) continue@loop
                    }
                }

                val isMangledMatch = mayBeMangled && cleanCandidateName !in cleanItemName
                val isExactMatch = !mayBeMangled && cleanCandidateName != cleanItemName
                if (isMangledMatch || isExactMatch) {
                    continue
                }

                if (cleanCandidateName.length > bestMatchLength) {
                    bestMatchLength = cleanCandidateName.length
                    bestMatch = internalName
                }
            }

            return bestMatch
        }

        // TODO use components
        private fun findInternalNameCandidatesForDisplayName(displayName: String): Set<String> {
            val isPet = petPattern.matches(displayName)
            val cleanDisplayName = displayName.cleanString()
            val titleWordMap = EnoughUpdatesManager.titleWordMap
            return buildSet {
                for (partialDisplayName in cleanDisplayName.split(" ")) {
                    if (partialDisplayName.isEmpty()) continue
                    if (partialDisplayName !in titleWordMap.keys) continue
                    val c = titleWordMap[partialDisplayName]?.keys ?: continue
                    for (s in c) {
                        if (isPet && ";" !in s) continue
                        add(s)
                    }
                }
            }
        }

        // TODO use components
        fun resolveEnchantmentByName(displayName: String): NeuInternalName? =
            UtilsPatterns.enchantmentNamePattern.matchMatcher(displayName) {
                val name = group("name").trim().replace("'", "")
                val ultimate = group("format").lowercase().contains("§l")
                val prefix = if (ultimate && !name.startsWith("Ultimate ")) "ULTIMATE_" else ""
                val cleanedEnchantName = name.renamedEnchantmentCheck().replace(" ", "_").replace("-", "_").uppercase()
                "$prefix$cleanedEnchantName;${group("level").romanToDecimal()}".uppercase().toInternalName()
            }

        // TODO use components
        private fun String.renamedEnchantmentCheck(): String = renamedEnchantments[this] ?: this

        fun attributeNameToInternalName(attributeName: String): NeuInternalName? {
            var fixedAttributeName = attributeName.uppercase().replace(" ", "_")
            fixedAttributeName = shardNameOverrides[fixedAttributeName] ?: fixedAttributeName
            val shardName = "SHARD_$fixedAttributeName"
            return ItemUtils.bazaarOverrides[shardName]?.toInternalName()
        }
    }

    fun withItemStack(stack: ItemStack): ItemResolutionQuery = apply {
        itemType = stack.item
        compound = stack.components
    }

    fun withKnownInternalName(internalName: NeuInternalName): ItemResolutionQuery = apply {
        knownInternalName = internalName
    }

    fun withCurrentGuiContext(): ItemResolutionQuery = apply {
        guiContext = Minecraft.getInstance().screen
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
        val petInfo = getExtraAttributes().getStringOrDefault("petInfo")
        if (petInfo.isEmpty()) return null
        try {
            val petInfoObject = ConfigManager.gson.fromJson(petInfo, JsonObject::class.java)
            val petId = petInfoObject["type"].asString
            val petTier = petInfoObject["tier"].asString
            val rarityIndex = petRarities.indexOf(petTier)
            return "${petId.uppercase()};$rarityIndex".toInternalName()
        } catch (e: Exception) {
            ErrorManager.logErrorWithData(
                e, "Error while resolving pet information",
                "petInfo" to petInfo,
            )
            return null
        }
    }

    private fun resolveRuneName(): NeuInternalName? {
        val runes = getExtraAttributes().getCompoundOrDefault("runes")
        val runeName = runes.keySet().singleOrNull()
        if (runeName.isNullOrEmpty()) return null
        val runeLevel = runes.getIntOrDefault(runeName)
        return "${runeName.uppercase()}_RUNE;$runeLevel".toInternalName()
    }

    private fun resolveEnchantedBookNameFromNBT(): NeuInternalName? {
        val enchantments = getExtraAttributes().getCompoundOrDefault("enchantments")
        val enchantName = enchantments.keySet().singleOrNull()
        if (enchantName.isNullOrEmpty()) return null
        val enchantLevel = enchantments.getIntOrDefault(enchantName)
        return "${enchantName.uppercase()};$enchantLevel".toInternalName()
    }

    private fun resolveCrabHatName(): NeuInternalName {
        val crabHatYear = getExtraAttributes().getIntOrDefault("party_hat_year")
        val color = getExtraAttributes().getStringOrDefault("party_hat_color")
        val suffix = if (crabHatYear == 2022) "_ANIMATED" else ""
        return "PARTY_HAT_CRAB_${color.uppercase()}$suffix".toInternalName()
    }

    private fun resolvePhoneCase(): NeuInternalName {
        val model = getExtraAttributes().getStringOrDefault("model")
        return "ABICASE_${model.uppercase()}".toInternalName()
    }

    private fun resolveSlothHatName(): NeuInternalName {
        val emoji = getExtraAttributes().getStringOrDefault("party_hat_emoji")
        return "PARTY_HAT_SLOTH_${emoji.uppercase()}".toInternalName()
    }

    private fun resolvePotionName(): NeuInternalName {
        val potion = getExtraAttributes().getStringOrDefault("potion")
        val potionLvl = getExtraAttributes().getIntOrDefault("potion_level")
        val potionName = getExtraAttributes().getStringOrDefault("potion_name").replace(" ", "_")
        val potionType = getExtraAttributes().getStringOrDefault("potion_type")

        return when {
            potionName.isNotEmpty() -> "POTION_${potionName.uppercase()};$potionLvl"
            potion.isNotEmpty() -> "POTION_${potion.uppercase()};$potionLvl"
            potionType.isNotEmpty() -> "POTION_${potionType.uppercase()}"
            else -> "WATER_BOTTLE"
        }.toInternalName()
    }

    private fun resolveBalloonHatName(): NeuInternalName {
        val color = getExtraAttributes().getStringOrDefault("party_hat_color")
        val balloonHatYear = getExtraAttributes().getIntOrDefault("party_hat_year")
        return "BALLOON_HAT_${balloonHatYear}_${color.uppercase()}".toInternalName()
    }

    private fun resolveAttributeShardName(): NeuInternalName? {
        val attributes = getExtraAttributes().getCompoundOrDefault("attributes")
        val attributeName = attributes.keySet().singleOrNull()
        if (attributeName.isNullOrEmpty()) return null
        val attributeLevel = attributes.getIntOrDefault(attributeName)
        return "ATTRIBUTE_SHARD_${attributeName.uppercase()};$attributeLevel".toInternalName()
    }

    private fun resolveItemInCatacombsRngMeter(): NeuInternalName? {
        val lore = compound.getLoreComponent()
        val index = lore.indexOfFirstOrNull { it.string == "Selected Drop" } ?: return null
        val displayName = lore.getOrNull(index + 1)?.formattedTextCompatLeadingWhiteLessResets()
            ?: return null
        return findInternalNameByDisplayName(displayName, false)
    }

    private fun resolveItemInAttributeMenu(lore: List<Component>): NeuInternalName? =
        UtilsPatterns.attributeSourcePattern.firstComponentMatcher(lore) {
            attributeNameToInternalName(group("source"))
        }

    // uses colorless name
    private fun resolveItemInHuntingBoxMenu(displayName: String): NeuInternalName? =
        attributeNameToInternalName(displayName)

    // TODO use components
    private fun resolveContextualName(): NeuInternalName? {
        val chest = guiContext as? ContainerScreen ?: return null
        val inventorySlots = chest.container as ChestMenu
        val guiName = InventoryUtils.openInventoryName()
        val isOnBazaar = isBazaar(inventorySlots.container)
        var displayName = ItemUtils.getDisplayName(compound) ?: return null
        displayName = displayName.removePrefix("§6§lSELL ").removePrefix("§a§lBUY ")

        if (itemType === Items.ENCHANTED_BOOK && isOnBazaar) {
            return resolveEnchantmentByName(displayName)
        }
        if (itemType === Items.PLAYER_HEAD && displayName.contains("Essence")) {
            findInternalNameByDisplayName(displayName, false)?.let { return it }
        }

        return when {
            displayName.endsWith("Enchanted Book") && guiName.startsWith("Superpairs") -> {
                compound.getLoreComponent().firstNotNullOfOrNull { loreLine ->
                    resolveEnchantmentByName(loreLine.string)
                }
            }

            guiName == "Catacombs RNG Meter" -> resolveItemInCatacombsRngMeter()

            guiName.startsWith("Choose Pet") -> findInternalNameByDisplayName(displayName, false)

            guiName.endsWith("Experimentation Table RNG") -> resolveEnchantmentByName(displayName)

            guiName == "Attribute Menu" -> resolveItemInAttributeMenu(compound.getLoreComponent())

            guiName.equalsOneOf("Hunting Box", "Fusion Box", "Shard Fusion") ->
                resolveItemInHuntingBoxMenu(displayName.removeColor())

            guiName == "Confirm Fusion" -> compound.getLoreComponent().firstOrNull()?.let {
                shardPattern.matchMatcher(it) {
                    resolveItemInHuntingBoxMenu(group("name").removeColor())
                }
            }

            guiName == "Dye Compendium" -> findInternalNameByDisplayName(displayName, false)

            else -> null
        }
    }

    private fun isBazaar(chest: Container): Boolean {
        if (InventoryUtils.openInventoryName().startsWith("Bazaar ➜ ")) {
            return true
        }
        val bazaarSlot = chest.containerSize - 5
        if (bazaarSlot < 0) return false
        val stackInSlot = chest.getItem(bazaarSlot) ?: return false
        if (stackInSlot.count == 0) return false

        return toBazaarPattern.anyMatches(stackInSlot.getCleanLore())
    }

    private fun getExtraAttributes(): CompoundTag = compound.extraAttributes

    private fun resolveFromSkyblock(): NeuInternalName? {
        val internalName = getExtraAttributes().getStringOrDefault("id")
        if (internalName.isEmpty()) return null
        return internalName.uppercase().replace(":", "-").toInternalName()
    }

    private fun resolveToItemJson(): NeuItemJson? =
        resolveInternalName()?.let(EnoughUpdatesManager::getItemById)

    fun resolveToItemStack(): ItemStack? =
        resolveToItemJson()?.let(EnoughUpdatesManager::neuItemToStack)
}
