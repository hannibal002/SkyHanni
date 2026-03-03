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
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.cleanString
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.UtilsPatterns
import at.hannibal2.skyhanni.utils.compat.container
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
import net.minecraft.world.Container
import net.minecraft.world.inventory.ChestMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// Code taken from NotEnoughUpdates
class ItemResolutionQuery {

    private var compound: DataComponentMap? = null

    private var itemType: Item? = null
    private var knownInternalName: NeuInternalName? = null
    private var guiContext: Screen? = null

    @SkyHanniModule
    companion object {

        private val patternGroup = RepoPattern.group("misc.itemresolution")

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
            var petRarity: String? = null
            petPattern.matchMatcher(itemName) {
                itemName = group("name")
                petRarity = group("rarity")
            }
            val cleanDisplayName = itemName.removeColor()
            var bestMatch: NeuInternalName? = null
            var bestMatchLength = -1
            loop@ for (internalName in candidateInternalNames.map { it.toInternalName() }) {
                val unCleanItemDisplayName: String = EnoughUpdatesManager.getDisplayName(internalName)
                var cleanItemDisplayName = unCleanItemDisplayName.removeColor()
                if (cleanItemDisplayName.isEmpty()) continue
                if (petPattern.matches(itemName)) {
                    if (!cleanItemDisplayName.contains("[Lvl {LVL}] ")) continue
                    cleanItemDisplayName = cleanItemDisplayName.replace("[Lvl {LVL}] ", "")
                    petPattern.matchMatcher(unCleanItemDisplayName) {
                        if (group("rarity") != petRarity) continue@loop
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
            val isPet = petPattern.matches(displayName)
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
        this.compound = stack.components
        return this
    }

    fun withKnownInternalName(internalName: NeuInternalName): ItemResolutionQuery {
        this.knownInternalName = internalName
        return this
    }

    fun withCurrentGuiContext(): ItemResolutionQuery {
        this.guiContext = Minecraft.getInstance().screen
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
        val petInfo = getExtraAttributes().getStringOrDefault("petInfo")
        if (petInfo.isEmpty()) return null
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
        val runes = getExtraAttributes().getCompoundOrDefault("runes")
        val runeName = runes.keySet().singleOrNull()
        if (runeName.isNullOrEmpty()) return null
        val rawInternalName = runeName.uppercase() + "_RUNE;" + runes.getIntOrDefault(runeName)
        return rawInternalName.toInternalName()
    }

    private fun resolveEnchantedBookNameFromNBT(): NeuInternalName? {
        val enchantments = getExtraAttributes().getCompoundOrDefault("enchantments")
        val enchantName = enchantments.keySet().singleOrNull()
        if (enchantName.isNullOrEmpty()) return null
        val rawInternalName = enchantName.uppercase() + ";" + enchantments.getIntOrDefault(enchantName)
        return rawInternalName.toInternalName()
    }

    private fun resolveCrabHatName(): NeuInternalName {
        val crabHatYear = getExtraAttributes().getIntOrDefault("party_hat_year")
        val color = getExtraAttributes().getStringOrDefault("party_hat_color")
        val rawInternalName = "PARTY_HAT_CRAB_" + color.uppercase() + (if (crabHatYear == 2022) "_ANIMATED" else "")
        return rawInternalName.toInternalName()
    }

    private fun resolvePhoneCase(): NeuInternalName {
        val model = getExtraAttributes().getStringOrDefault("model")
        return ("ABICASE_" + model.uppercase()).toInternalName()
    }

    private fun resolveSlothHatName(): NeuInternalName {
        val emoji = getExtraAttributes().getStringOrDefault("party_hat_emoji")
        return ("PARTY_HAT_SLOTH_" + emoji.uppercase()).toInternalName()
    }

    private fun resolvePotionName(): NeuInternalName {
        val potion = getExtraAttributes().getStringOrDefault("potion")
        val potionLvl = getExtraAttributes().getIntOrDefault("potion_level")
        val potionName = getExtraAttributes().getStringOrDefault("potion_name").replace(" ", "_")
        val potionType = getExtraAttributes().getStringOrDefault("potion_type")
        val rawInternalName = if (potionName.isNotEmpty()) {
            "POTION_" + potionName.uppercase() + ";" + potionLvl
        } else if (potion.isNotEmpty()) {
            "POTION_" + potion.uppercase() + ";" + potionLvl
        } else if (potionType.isNotEmpty()) {
            "POTION_" + potionType.uppercase()
        } else {
            "WATER_BOTTLE"
        }
        return rawInternalName.toInternalName()
    }

    private fun resolveBalloonHatName(): NeuInternalName {
        val color = getExtraAttributes().getStringOrDefault("party_hat_color")
        val balloonHatYear = getExtraAttributes().getIntOrDefault("party_hat_year")
        val rawInternalName = "BALLOON_HAT_" + balloonHatYear + "_" + color.uppercase()
        return rawInternalName.toInternalName()
    }

    private fun resolveAttributeShardName(): NeuInternalName? {
        val attributes = getExtraAttributes().getCompoundOrDefault("attributes")
        val attributeName = attributes.keySet().singleOrNull()
        if (attributeName.isNullOrEmpty()) return null
        val rawInternalName = "ATTRIBUTE_SHARD_" + attributeName.uppercase() + ";" + attributes.getIntOrDefault(attributeName)
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
        } else if (guiName == "Confirm Fusion") {
            compound.getLore().firstOrNull()?.let {
                shardPattern.matchMatcher(it) {
                    resolveItemInHuntingBoxMenu(
                        group("name")
                    )
                }
            }
        } else if (guiName == "Dye Compendium") {
            findInternalNameByDisplayName(displayName, false)
        } else null
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

    private fun resolveFromSkyblock(): NeuInternalName? {
        val internalName = getExtraAttributes().getStringOrDefault("id")
        if (internalName.isEmpty()) return null
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
