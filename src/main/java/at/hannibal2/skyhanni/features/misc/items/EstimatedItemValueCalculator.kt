package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.CollectionUtils.sortedDesc
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.isRune
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.shortFormat
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAbilityScrolls
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getArmorDye
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getBookwormBookCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDrillUpgrades
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDungeonStarCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnrichment
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getExtraAttributes
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getGemstones
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHelmetSkin
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHotPotatoCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getManaDisintegrators
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPolarvoidBookCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPowerScroll
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getRune
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getSilexCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getTransmissionTunerCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasArtOfPeace
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasArtOfWar
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasBookOfStats
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasEtherwarp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasJalapenoBook
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasWoodSingularity
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.allLettersFirstUppercase
import io.github.moulberry.notenoughupdates.recipes.Ingredient
import net.minecraft.item.ItemStack
import java.util.Locale

object EstimatedItemValueCalculator {

    private val config get() = SkyHanniMod.feature.inventory.estimatedItemValues

    private val kuudraSets = listOf("AURORA", "CRIMSON", "TERROR", "HOLLOW", "FERVOR")

    private val additionalCostFunctions = listOf(
        ::addAttributeCost,
        ::addReforgeStone,

        // once
        ::addRecombobulator,
        ::addArtOfWar,
        ::addArtOfPeace,
        ::addEtherwarp,
        ::addPowerScrolls,
        ::addWoodSingularity,
        ::addJalapenoBook,
        ::addStatsBook,
        ::addEnrichment,

        // counted
        ::addMasterStars,
        ::addHotPotatoBooks,
        ::addFarmingForDummies,
        ::addSilex,
        ::addTransmissionTuners,
        ::addManaDisintegrators,
        ::addPolarvoidBook,
        ::addBookwormBook,

        // cosmetic
        ::addHelmetSkin,
        ::addArmorDye,
        ::addRune,

        // dynamic
        ::addAbilityScrolls,
        ::addDrillUpgrades,
        ::addGemstoneSlotUnlockCost,
        ::addGemstones,
        ::addEnchantments,
    )

    val farmingForDummies = "FARMING_FOR_DUMMIES".asInternalName()
    val etherwarpConduit = "ETHERWARP_CONDUIT".asInternalName()
    val etherwarpMerger = "ETHERWARP_MERGER".asInternalName()
    val fumingPotatoBook = "FUMING_POTATO_BOOK".asInternalName()
    val hotPotatoBook = "HOT_POTATO_BOOK".asInternalName()
    val silex = "SIL_EX".asInternalName()
    val transmissionTuner = "TRANSMISSION_TUNER".asInternalName()
    val manaDisintegrator = "MANA_DISINTEGRATOR".asInternalName()

    fun getTotalPrice(stack: ItemStack): Double = EstimatedItemValueCalculator.calculate(stack, mutableListOf()).first

    fun calculate(stack: ItemStack, list: MutableList<String>): Pair<Double, Double> {
        val basePrice = addBaseItem(stack, list)
        val totalPrice = additionalCostFunctions.fold(basePrice) { total, function -> total + function(stack, list) }
        return Pair(totalPrice, basePrice)
    }

    private fun isKuudraSet(internalName: String) =
        (kuudraSets.any { internalName.contains(it) } &&
            listOf("CHESTPLATE", "LEGGINGS", "HELMET", "BOOTS").any { internalName.endsWith(it) })

    private fun addAttributeCost(stack: ItemStack, list: MutableList<String>): Double {
        val attributes = stack.getAttributes() ?: return 0.0
        var internalName = removeKuudraArmorPrefix(stack.getInternalName().asString().removePrefix("VANQUISHED_"))
        var genericName = internalName
        if (isKuudraSet(internalName)) {
            genericName = kuudraSets.fold(internalName) { acc, part -> acc.replace(part, "GENERIC_KUUDRA") }
        }
        if (internalName == "ATTRIBUTE_SHARD" && attributes.size == 1) {
            val price = getPriceOrCompositePriceForAttribute(
                "ATTRIBUTE_SHARD+ATTRIBUTE_" + attributes[0].first,
                attributes[0].second,
            )
            if (price != null) {
                val name = attributes[0].first.fixMending().allLettersFirstUppercase()
                list.add("§7Attribute §9$name ${attributes[0].second}§7: (§6${price.shortFormat()}§7)",)
                return price
            }
        }
        if (attributes.size != 2) return 0.0
        val basePrice = internalName.asInternalName().getPriceOrNull() ?: 0.0
        var subTotal = 0.0
        val combo = ("$internalName+ATTRIBUTE_${attributes[0].first}+ATTRIBUTE_${attributes[1].first}")
        var comboPrice = combo.asInternalName().getPriceOrNull()

        if (comboPrice != null) {
            val useless = isUselessAttribute(combo)
            val color = if (comboPrice > basePrice && !useless) "§6" else "§7"
            list.add("§7Attribute Combo: ($color${comboPrice.shortFormat()}§7)")
            if (!useless) {
                subTotal += addAttributePrice(comboPrice, basePrice)
            }
        } else {
            list.add("§7Attributes:")
        }
        for (attr in attributes) {
            val attributeName = "$genericName+ATTRIBUTE_${attr.first}"
            val price =
                getPriceOrCompositePriceForAttribute(attributeName, attr.second)
            var priceColor = "§7"
            val useless = isUselessAttribute(attributeName)
            var nameColor = if (!useless) "§9" else "§7"
            if (price != null) {
                if (price > basePrice && !useless) {
                    subTotal += addAttributePrice(price, basePrice)
                    priceColor = "§6"
                }

            }
            val displayName = attr.first.fixMending()
            list.add(
                "  $nameColor${
                    displayName.allLettersFirstUppercase()
                } ${attr.second}§7: $priceColor${if (price != null) price.shortFormat() else "Unknown"}",
            )
        }
        // Adding 0.1 so that we always show the estimated item value overlay
        return subTotal + 0.1
    }

    private fun removeKuudraArmorPrefix(original: String): String {
        if (!isKuudraSet(original)) return original

        var internalName = original
        for (prefix in listOf("HOT_", "BURNING_", "FIERY_", "INFERNAL_")) {
            internalName = internalName.removePrefix(prefix)
        }
        return internalName
    }

    private fun addAttributePrice(attributePrice: Double, basePrice: Double): Double =
        if (attributePrice > basePrice) {
            attributePrice - basePrice
        } else {
            0.0
        }

    private fun isUselessAttribute(internalName: String): Boolean {
        if (internalName.contains("RESISTANCE")) return true
        if (internalName.contains("SPEED")) return true
        if (internalName.contains("EXPERIENCE")) return true
        if (internalName.contains("FORTITUDE")) return true
        if (internalName.contains("ENDER")) return true

        return false
    }

    private fun String.fixMending() = if (this == "MENDING") "VITALITY" else this

    private fun getPriceOrCompositePriceForAttribute(attributeName: String, level: Int): Double? {
        return (1..10).mapNotNull { lowerLevel ->
            "$attributeName;$lowerLevel".asInternalName().getPriceOrNull()
                ?.let { it / (1 shl lowerLevel) * (1 shl level).toDouble() }
        }.minOrNull()
    }

    private fun addReforgeStone(stack: ItemStack, list: MutableList<String>): Double {
        val rawReforgeName = stack.getReforgeName() ?: return 0.0

        val reforge = EstimatedItemValue.reforges.values.firstOrNull {
            rawReforgeName == it.reforgeName.lowercase() || rawReforgeName == it.internalName.asString().lowercase()
        } ?: return 0.0
        val internalName = reforge.internalName.asString().asInternalName()
        val reforgeStonePrice = internalName.getPrice()
        val reforgeStoneName = internalName.itemName
        val applyCost = getReforgeStoneApplyCost(stack, reforge.reforgeCosts, internalName) ?: return 0.0

        list.add("§7Reforge: §9${reforge.reforgeName}")
        list.add("  §7Stone $reforgeStoneName §7(§6" + reforgeStonePrice.shortFormat() + "§7)")
        list.add("  §7Apply cost: (§6" + applyCost.shortFormat() + "§7)")
        return reforgeStonePrice + applyCost
    }

    private fun getReforgeStoneApplyCost(
        stack: ItemStack,
        reforgeCosts: Map<LorenzRarity, Long>,
        reforgeStone: NEUInternalName,
    ): Int? {
        var itemRarity = stack.getItemRarityOrNull() ?: return null

        // Catch cases of special or very special
        if (itemRarity > LorenzRarity.MYTHIC) {
            itemRarity = LorenzRarity.LEGENDARY
        } else {
            if (stack.isRecombobulated()) {
                val oneBelow = itemRarity.oneBelow(logError = false)
                if (oneBelow == null) {
                    ErrorManager.logErrorStateWithData(
                        "Wrong item rarity detected in estimated item value for item ${stack.name}",
                        "Recombobulated item is common",
                        "internal name" to stack.getInternalName(),
                        "itemRarity" to itemRarity,
                        "item name" to stack.name,
                    )
                    return null
                }
                itemRarity = oneBelow
            }
        }

        return reforgeCosts[itemRarity]?.toInt() ?: run {
            ErrorManager.logErrorStateWithData(
                "Could not calculate reforge cost for item ${stack.name}",
                "Item not in NEU repo reforge cost",
                "reforgeCosts" to reforgeCosts,
                "itemRarity" to itemRarity,
                "internal name" to stack.getInternalName(),
                "item name" to stack.name,
                "reforgeStone" to reforgeStone,
            )
            null
        }
    }

    private fun addRecombobulator(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.isRecombobulated()) return 0.0

        val price = "RECOMBOBULATOR_3000".asInternalName().getPrice()
        list.add("§7Recombobulated: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addJalapenoBook(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasJalapenoBook()) return 0.0

        val price = "JALAPENO_BOOK".asInternalName().getPrice()
        list.add("§7Jalapeno Book: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addEtherwarp(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasEtherwarp()) return 0.0

        val price = etherwarpConduit.getPrice() + etherwarpMerger.getPrice()
        list.add("§7Etherwarp: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addWoodSingularity(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasWoodSingularity()) return 0.0

        val price = "WOOD_SINGULARITY".asInternalName().getPrice()
        list.add("§7Wood Singularity: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addArtOfWar(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasArtOfWar()) return 0.0

        val price = "THE_ART_OF_WAR".asInternalName().getPrice()
        list.add("§7The Art of War: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addStatsBook(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasBookOfStats()) return 0.0

        val price = "BOOK_OF_STATS".asInternalName().getPrice()
        list.add("§7Book of Stats: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    // TODO untested
    private fun addArtOfPeace(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasArtOfPeace()) return 0.0

        val price = "THE_ART_OF_PEACE".asInternalName().getPrice()
        list.add("§7The Art Of Peace: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addHotPotatoBooks(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getHotPotatoCount() ?: return 0.0

        val hpb: Int
        val fuming: Int
        if (count <= 10) {
            hpb = count
            fuming = 0
        } else {
            hpb = 10
            fuming = count - 10
        }

        var totalPrice = 0.0

        val hpbPrice = hotPotatoBook.getPrice() * hpb
        list.add("§7HPB's: §e$hpb§7/§e10 §7(§6" + hpbPrice.shortFormat() + "§7)")
        totalPrice += hpbPrice

        if (fuming > 0) {
            val fumingPrice = fumingPotatoBook.getPrice() * fuming
            list.add("§7Fuming: §e$fuming§7/§e5 §7(§6" + fumingPrice.shortFormat() + "§7)")
            totalPrice += fumingPrice
        }

        return totalPrice
    }

    private fun addFarmingForDummies(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getFarmingForDummiesCount() ?: return 0.0

        val price = farmingForDummies.getPrice() * count
        list.add("§7Farming for Dummies: §e$count§7/§e5 §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addPolarvoidBook(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getPolarvoidBookCount() ?: return 0.0

        val polarvoidBook = "POLARVOID_BOOK".asInternalName()
        val price = polarvoidBook.getPrice() * count
        list.add("§7Polarvoid: §e$count§7/§e5 §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addBookwormBook(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getBookwormBookCount() ?: return 0.0

        val tfHardcodedItemAgain = "BOOKWORM_BOOK".asInternalName()
        val price = tfHardcodedItemAgain.getPrice() * count
        list.add("§7Bookworm's Favorite Book: §e$count§7/§e5 §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addSilex(stack: ItemStack, list: MutableList<String>): Double {
        val tier = stack.getSilexCount() ?: return 0.0

        val internalName = stack.getInternalName()
        val maxTier = if (internalName == "STONK_PICKAXE".asInternalName()) 4 else 5

        val price = silex.getPrice() * tier
        list.add("§7Silex: §e$tier§7/§e$maxTier §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addTransmissionTuners(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getTransmissionTunerCount() ?: return 0.0

        val price = transmissionTuner.getPrice() * count
        list.add("§7Transmission Tuners: §e$count§7/§e4 §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addManaDisintegrators(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getManaDisintegrators() ?: return 0.0

        val price = manaDisintegrator.getPrice() * count
        list.add("§7Mana Disintegrators: §e$count§7/§e10 §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addMasterStars(stack: ItemStack, list: MutableList<String>): Double {
        val totalStars = stack.getDungeonStarCount() ?: return 0.0

        val masterStars = totalStars - 5
        if (masterStars < 1) return 0.0

        var price = 0.0

        val stars = mapOf(
            "FIRST" to 1,
            "SECOND" to 2,
            "THIRD" to 3,
            "FOURTH" to 4,
            "FIFTH" to 5,
        )

        for ((prefix, number) in stars) {
            if (masterStars >= number) {
                price += "${prefix}_MASTER_STAR".asInternalName().getPrice()
            }
        }

        list.add("§7Master Stars: §e$masterStars§7/§e5 §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addDrillUpgrades(stack: ItemStack, list: MutableList<String>): Double {
        val drillUpgrades = stack.getDrillUpgrades() ?: return 0.0

        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()
        for (internalName in drillUpgrades) {
            val name = internalName.itemName
            val price = internalName.getPriceOrNull() ?: continue

            totalPrice += price
            val format = price.shortFormat()
            map[" $name §7(§6$format§7)"] = price
        }
        if (map.isNotEmpty()) {
            list.add("§7Drill upgrades: §6" + totalPrice.shortFormat())
            list += map.sortedDesc().keys
        }
        return totalPrice
    }

    private fun addPowerScrolls(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getPowerScroll() ?: return 0.0

        val price = internalName.getPrice()
        val name = internalName.itemNameWithoutColor
        list.add("§7$name: §a§l✔ §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addHelmetSkin(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getHelmetSkin() ?: return 0.0
        return addCosmetic(internalName, list, "Skin", config.ignoreHelmetSkins)
    }

    private fun addArmorDye(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getArmorDye() ?: return 0.0
        return addCosmetic(internalName, list, "Dye", config.ignoreArmorDyes)
    }

    private fun addCosmetic(
        internalName: NEUInternalName,
        list: MutableList<String>,
        label: String,
        shouldIgnorePrice: Boolean,
    ): Double {
        val price = internalName.getPrice()
        val name = internalName.getNameOrRepoError()
        val displayName = name ?: "§c${internalName.asString()}"
        val color = if (shouldIgnorePrice) "§7" else "§6"
        list.add("§7$label: $displayName §7($color" + price.shortFormat() + "§7)")
        if (name == null) {
            list.add("   §8(Not yet in NEU Repo)")
        }

        return if (shouldIgnorePrice) 0.0 else price
    }

    private fun addEnrichment(stack: ItemStack, list: MutableList<String>): Double {

        val enrichmentName = stack.getEnrichment() ?: return 0.0
        val internalName = "TALISMAN_ENRICHMENT_$enrichmentName".asInternalName()

        val price = internalName.getPrice()
        val name = internalName.itemName
        list.add("§7Enrichment: $name §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    private fun addRune(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.getInternalName().isRune()) return 0.0
        val internalName = stack.getRune() ?: return 0.0

        return addCosmetic(internalName, list, "Rune", config.ignoreRunes)
    }

    private fun NEUInternalName.getNameOrRepoError(): String? = getItemStackOrNull()?.itemName

    private fun addAbilityScrolls(stack: ItemStack, list: MutableList<String>): Double {
        val abilityScrolls = stack.getAbilityScrolls() ?: return 0.0

        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()
        for (internalName in abilityScrolls) {
            val name = internalName.itemName
            val price = internalName.getPriceOrNull() ?: continue

            totalPrice += price
            val format = price.shortFormat()
            map[" $name §7(§6$format§7)"] = price
        }
        if (map.isNotEmpty()) {
            list.add("§7Ability Scrolls: §6" + totalPrice.shortFormat())
            list += map.sortedDesc().keys
        }
        return totalPrice
    }

    private fun addBaseItem(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = removeKuudraArmorPrefix(stack.getInternalName().asString()).asInternalName()
        var price = internalName.getPrice()
        if (price == -1.0) {
            price = 0.0
        }

        val name = internalName.itemName
        if (internalName.startsWith("ENCHANTED_BOOK_BUNDLE_")) {
            list.add("§7Base item: $name")
            return 0.0
        }

        list.add("§7Base item: $name §7(§6" + price.shortFormat() + "§7)")
        return price
    }

    // TODO repo
    private val hasAlwaysScavenger = listOf(
        "CRYPT_DREADLORD_SWORD".asInternalName(),
        "ZOMBIE_SOLDIER_CUTLASS".asInternalName(),
        "CONJURING_SWORD".asInternalName(),
        "EARTH_SHARD".asInternalName(),
        "ZOMBIE_KNIGHT_SWORD".asInternalName(),
        "SILENT_DEATH".asInternalName(),
        "ZOMBIE_COMMANDER_WHIP".asInternalName(),
        "ICE_SPRAY_WAND".asInternalName(),
    )

    private val hasAlwaysReplenish = listOf(
        "ADVANCED_GARDENING_HOE".asInternalName(),
        "ADVANCED_GARDENING_AXE".asInternalName(),
    )

    private fun addEnchantments(stack: ItemStack, list: MutableList<String>): Double {
        val enchantments = stack.getEnchantments() ?: return 0.0

        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()

        //todo use repo
        val tieredEnchants = listOf("compact", "cultivating", "champion", "expertise", "hecatomb")
        val onlyTierOnePrices =
            listOf("ultimate_chimera", "ultimate_fatal_tempo", "smoldering", "ultimate_flash", "divine_gift")

        val internalName = stack.getInternalName()
        for ((rawName, rawLevel) in enchantments) {
            // efficiency 1-5 is cheap, 6-10 is handled by silex
            if (rawName == "efficiency") continue

            if (rawName == "scavenger" && rawLevel == 5 && internalName in hasAlwaysScavenger) {
                continue
            }

            if (rawName == "replenish" && rawLevel == 1 && internalName in hasAlwaysReplenish) {
                continue
            }

            var level = rawLevel
            var multiplier = 1
            if (rawName in onlyTierOnePrices) {

                when (rawLevel) {
                    2 -> multiplier = 2
                    3 -> multiplier = 4
                    4 -> multiplier = 8
                    5 -> multiplier = 16
                }
                level = 1
            }
            if (internalName.startsWith("ENCHANTED_BOOK_BUNDLE_")) {
                multiplier = EstimatedItemValue.bookBundleAmount.getOrDefault(rawName, 5)
            }
            if (rawName in tieredEnchants) level = 1

            val enchantmentName = "$rawName;$level".uppercase().asInternalName()
            val itemStack = enchantmentName.getItemStackOrNull() ?: continue
            val singlePrice = enchantmentName.getPriceOrNull() ?: continue

            var name = itemStack.getLore()[0]
            if (multiplier > 1) {
                name = "§8${multiplier}x $name"
            }
            val price = singlePrice * multiplier

            totalPrice += price
            val format = price.shortFormat()


            map[" $name §7(§6$format§7)"] = price
        }
        val enchantmentsCap: Int = config.enchantmentsCap.get()
        if (map.isNotEmpty()) {
            list.add("§7Enchantments: §6" + totalPrice.shortFormat())
            var i = 0
            for (entry in map.sortedDesc().keys) {
                if (i == enchantmentsCap) {
                    val missing = map.size - enchantmentsCap
                    list.add(" §7§o$missing more enchantments..")
                    break
                }
                list.add(entry)
                i++
            }
        }
        return totalPrice
    }

    private fun addGemstones(stack: ItemStack, list: MutableList<String>): Double {
        val gemstones = stack.getGemstones() ?: return 0.0

        var totalPrice = 0.0
        val counterMap = mutableMapOf<NEUInternalName, Int>()
        for (gemstone in gemstones) {
            val internalName = gemstone.getInternalName()
            val old = counterMap[internalName] ?: 0
            counterMap[internalName] = old + 1
        }

        val priceMap = mutableMapOf<String, Double>()
        for ((internalName, amount) in counterMap) {

            val name = internalName.itemName
            val price = internalName.getPrice() * amount

            totalPrice += price
            val format = price.shortFormat()

            val text = if (amount == 1) {
                " $name §7(§6$format§7)"
            } else {
                " §8${amount}x $name §7(§6$format§7)"
            }
            priceMap[text] = price
        }

        if (priceMap.isNotEmpty()) {
            list.add("§7Gemstones: §6" + totalPrice.shortFormat())
            list += priceMap.sortedDesc().keys
        }
        return totalPrice
    }

    private fun addGemstoneSlotUnlockCost(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getInternalName()

        // item have to contains gems.unlocked_slots NBT array for unlocked slot detection
        val unlockedSlots =
            stack.getExtraAttributes()?.getCompoundTag("gems")?.getTag("unlocked_slots")?.toString() ?: return 0.0

        // TODO detection for old items which doesn't have gems.unlocked_slots NBT array
//        if (unlockedSlots == "null") return 0.0

        val priceMap = mutableMapOf<String, Double>()
        if (EstimatedItemValue.gemstoneUnlockCosts.isEmpty()) return 0.0

        if (internalName !in EstimatedItemValue.gemstoneUnlockCosts) {
            ErrorManager.logErrorStateWithData(
                "Could not find gemstone slot price for ${stack.name}",
                "EstimatedItemValue has no gemstoneUnlockCosts for $internalName",
                "internal name" to internalName,
                "gemstoneUnlockCosts" to EstimatedItemValue.gemstoneUnlockCosts,
                "item name" to stack.name,
            )
            return 0.0
        }

        var totalPrice = 0.0
        val slots = EstimatedItemValue.gemstoneUnlockCosts[internalName] ?: return 0.0
        for (slot in slots) {
            if (!unlockedSlots.contains(slot.key)) continue

            val previousTotal = totalPrice
            for (ingredients in slot.value) {
                val ingredient = Ingredient(NEUItems.manager, ingredients)

                totalPrice += if (ingredient.isCoins) {
                    ingredient.count
                } else {
                    ingredient.internalItemId.asInternalName().getPrice() * ingredient.count
                }
            }

            val splitSlot = slot.key.split("_") // eg. SAPPHIRE_1
            val colorCode = SkyBlockItemModifierUtils.GemstoneSlotType.getColorCode(splitSlot[0])
            val formattedPrice = (totalPrice - previousTotal).shortFormat()

            // eg. SAPPHIRE_1 -> Sapphire Slot 2
            val displayName = splitSlot[0].lowercase(Locale.ENGLISH).replaceFirstChar(Char::uppercase) + " Slot" +
                // If the slot index is 0, we don't need to specify
                if (splitSlot[1] != "0") " " + (splitSlot[1].toInt() + 1) else ""

            priceMap[" §$colorCode $displayName §7(§6$formattedPrice§7)"] = totalPrice - previousTotal
        }

        list.add("§7Gemstone Slot Unlock Cost: §6" + totalPrice.shortFormat())
        list += priceMap.sortedDesc().keys
        return totalPrice
    }
}
