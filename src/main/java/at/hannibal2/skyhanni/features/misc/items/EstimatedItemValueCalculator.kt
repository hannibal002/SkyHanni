package at.hannibal2.skyhanni.features.misc.items

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.ItemUtils.nameWithEnchantment
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStackOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAbilityScrolls
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getArmorDye
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAttributes
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
import at.hannibal2.skyhanni.utils.StringUtils.firstLetterUppercase
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import com.google.gson.JsonObject
import io.github.moulberry.notenoughupdates.recipes.Ingredient
import io.github.moulberry.notenoughupdates.util.Constants
import net.minecraft.item.ItemStack
import java.util.Locale

object EstimatedItemValueCalculator {
    private val config get() = SkyHanniMod.feature.misc.estimatedItemValues
    private val additionalCostFunctions = listOf(
        ::addAttributeCost,
        ::addReforgeStone,

        // once
        ::addRecomb,
        ::addArtOfWar,
        ::addArtOfPiece,
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

        // cosmetic
        ::addHelmetSkin,
        ::addArmorDye,
        ::addRune,

        // dynamic
        ::addAbilityScrolls,
        ::addDrillUpgrades,
        ::addGemstoneSlotUnlockCost,
        ::addGemstones,
        ::addEnchantments
    )

    fun calculate(stack: ItemStack, list: MutableList<String>): Pair<Double, Double> {
        val basePrice = addBaseItem(stack, list)
        val totalPrice = additionalCostFunctions.fold(basePrice) { total, function -> total + function(stack, list) }
        return Pair(totalPrice, basePrice)
    }

    private fun addAttributeCost(stack: ItemStack, list: MutableList<String>): Double {
        val attributes = stack.getAttributes() ?: return 0.0
        var internalName = stack.getInternalName().asString().removePrefix("VANQUISHED_")
        val kuudraSets = listOf("AURORA", "CRIMSON", "TERROR", "HOLLOW")
        var genericName = internalName
        if (kuudraSets.any { internalName.contains(it) }
            && listOf("CHESTPLATE", "LEGGINGS", "HELMET", "BOOTS").any { internalName.endsWith(it) }) {
            for (prefix in listOf("HOT_", "BURNING_", "FIERY_", "INFERNAL_")) {
                internalName = internalName.removePrefix(prefix)
            }
            genericName = kuudraSets.fold(internalName) { acc, part -> acc.replace(part, "GENERIC_KUUDRA") }
        }
        if (internalName == "ATTRIBUTE_SHARD" && attributes.size == 1) {
            val price =
                getPriceOrCompositePriceForAttribute(
                    "ATTRIBUTE_SHARD+ATTRIBUTE_" + attributes[0].first,
                    attributes[0].second
                )
            if (price != null) {
                list.add(
                    "§7Attribute §9${
                        attributes[0].first.fixMending().split("_").joinToString(" ") { it.firstLetterUppercase() }
                    } ${attributes[0].second}§7: (§6${NumberUtil.format(price)}§7)"
                )
                return price
            }
        }
        if (attributes.size != 2) return 0.0
        val basePrice = internalName.asInternalName().getPriceOrNull() ?: 0.0
        var subTotal = 0.0
        val combo = ("$internalName+ATTRIBUTE_${attributes[0].first}+ATTRIBUTE_${attributes[1].first}").asInternalName()
        val comboPrice = combo.getPriceOrNull()
        if (comboPrice != null && comboPrice > basePrice) {
            list.add("§7Attribute Combo: (§6${NumberUtil.format(comboPrice)}§7)")
            subTotal += comboPrice - basePrice
        } else {
            list.add("§7Attributes:")
        }
        for (attr in attributes) {
            val price =
                getPriceOrCompositePriceForAttribute("$genericName+ATTRIBUTE_${attr.first}", attr.second)
            if (price != null) {
                subTotal += price
            }
            val displayName = attr.first.fixMending()
            list.add(
                "  §9${
                    displayName.split("_").joinToString(" ") { it.firstLetterUppercase() }
                } ${attr.second}§7: §6${if (price != null) NumberUtil.format(price) else "Unknown"}"
            )
        }
        return subTotal
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

        for ((rawInternalName, values) in Constants.REFORGESTONES.entrySet()) {
            val stoneJson = values.asJsonObject
            val reforgeName = stoneJson.get("reforgeName").asString
            if (rawReforgeName == reforgeName.lowercase() || rawReforgeName == rawInternalName.lowercase()) {
                val internalName = rawInternalName.asInternalName()
                val reforgeStonePrice = internalName.getPrice()
                val reforgeStoneName = internalName.getItemName()

                val reforgeCosts = stoneJson.get("reforgeCosts").asJsonObject
                val applyCost = getReforgeStoneApplyCost(stack, reforgeCosts, internalName) ?: return 0.0

                list.add("§7Reforge: §9$reforgeName")
                list.add("  §7Stone $reforgeStoneName §7(§6" + NumberUtil.format(reforgeStonePrice) + "§7)")
                list.add("  §7Apply cost: (§6" + NumberUtil.format(applyCost) + "§7)")
                return reforgeStonePrice + applyCost
            }
        }

        return 0.0
    }

    private fun getReforgeStoneApplyCost(
        stack: ItemStack,
        reforgeCosts: JsonObject,
        reforgeStone: NEUInternalName
    ): Int? {
        var itemRarity = stack.getItemRarityOrNull() ?: return null

        // Catch cases of special or very special
        if (itemRarity > LorenzRarity.MYTHIC) {
            itemRarity = LorenzRarity.LEGENDARY
        } else {
            if (stack.isRecombobulated()) {
                val oneBelow = itemRarity.oneBelow()
                if (oneBelow == null) {
                    ErrorManager.logErrorState(
                        "Wrong item rarity detected in estimated item value for item ${stack.name}",
                        "Recombobulated item is common: ${stack.getInternalName()}, name:${stack.name}"
                    )
                    return null
                }
                itemRarity = oneBelow
            }
        }
        val rarityName = itemRarity.name
        if (!reforgeCosts.has(rarityName)) {
            val reforgesFound = reforgeCosts.entrySet().map { it.key }
            ErrorManager.logErrorState(
                "Can not calculate reforge cost for item ${stack.name}",
                "item rarity '$itemRarity' is not in NEU repo reforge cost for reforge stone$reforgeStone ($reforgesFound)"
            )
            return null
        }

        return reforgeCosts[rarityName].asInt
    }

    private fun addRecomb(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.isRecombobulated()) return 0.0

        val price = "RECOMBOBULATOR_3000".asInternalName().getPrice()
        list.add("§7Recombobulated: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addJalapenoBook(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasJalapenoBook()) return 0.0

        val price = "JALAPENO_BOOK".asInternalName().getPrice()
        list.add("§7Jalapeno Book: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addEtherwarp(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasEtherwarp()) return 0.0

        val wtfHardcodedConduit = "ETHERWARP_CONDUIT".asInternalName()
        val wtfHardcodedMerger = "ETHERWARP_MERGER".asInternalName()
        val price = wtfHardcodedConduit.getPrice() + wtfHardcodedMerger.getPrice()
        list.add("§7Etherwarp: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addWoodSingularity(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasWoodSingularity()) return 0.0

        val price = "WOOD_SINGULARITY".asInternalName().getPrice()
        list.add("§7Wood Singularity: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addArtOfWar(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasArtOfWar()) return 0.0

        val price = "THE_ART_OF_WAR".asInternalName().getPrice()
        list.add("§7The Art of War: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addStatsBook(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasBookOfStats()) return 0.0

        val price = "BOOK_OF_STATS".asInternalName().getPrice()
        list.add("§7Book of Stats: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    // TODO untested
    private fun addArtOfPiece(stack: ItemStack, list: MutableList<String>): Double {
        if (!stack.hasArtOfPeace()) return 0.0

        val price = "THE_ART_OF_PEACE".asInternalName().getPrice()
        list.add("§7The Art Of Piece: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
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

        val wtfHardcodedHpb = "HOT_POTATO_BOOK".asInternalName()
        val hpbPrice = wtfHardcodedHpb.getPrice() * hpb
        list.add("§7HPB's: §e$hpb§7/§e10 §7(§6" + NumberUtil.format(hpbPrice) + "§7)")
        totalPrice += hpbPrice

        if (fuming > 0) {
            val wtfHardcodedFuming = "FUMING_POTATO_BOOK".asInternalName()
            val fumingPrice = wtfHardcodedFuming.getPrice() * fuming
            list.add("§7Fuming: §e$fuming§7/§e5 §7(§6" + NumberUtil.format(fumingPrice) + "§7)")
            totalPrice += fumingPrice
        }

        return totalPrice
    }

    private fun addFarmingForDummies(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getFarmingForDummiesCount() ?: return 0.0

        val wtfHardcodedDumbFarmers = "FARMING_FOR_DUMMIES".asInternalName()
        val price = wtfHardcodedDumbFarmers.getPrice() * count
        list.add("§7Farming for Dummies: §e$count§7/§e5 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addPolarvoidBook(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getPolarvoidBookCount() ?: return 0.0

        val broDilloMiningSoBad = "POLARVOID_BOOK".asInternalName()
        val price = broDilloMiningSoBad.getPrice() * count
        list.add("§7Polarvoid: §e$count§7/§e5 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addSilex(stack: ItemStack, list: MutableList<String>): Double {
        val tier = stack.getSilexCount() ?: return 0.0

        val internalName = stack.getInternalName()
        val maxTier = if (internalName == "STONK_PICKAXE".asInternalName()) 4 else 5

        val wtfHardcodedSilex = "SIL_EX".asInternalName()
        val price = wtfHardcodedSilex.getPrice() * tier
        list.add("§7Silex: §e$tier§7/§e$maxTier §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addTransmissionTuners(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getTransmissionTunerCount() ?: return 0.0

        val wtfHardcodedTuner = "TRANSMISSION_TUNER".asInternalName()
        val price = wtfHardcodedTuner.getPrice() * count
        list.add("§7Transmission Tuners: §e$count§7/§e4 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addManaDisintegrators(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getManaDisintegrators() ?: return 0.0

        val wtfHardcodedTuner = "MANA_DISINTEGRATOR".asInternalName()
        val price = wtfHardcodedTuner.getPrice() * count
        list.add("§7Mana Disintegrators: §e$count§7/§e10 §7(§6" + NumberUtil.format(price) + "§7)")
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

        list.add("§7Master Stars: §e$masterStars§7/§e5 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addDrillUpgrades(stack: ItemStack, list: MutableList<String>): Double {
        val drillUpgrades = stack.getDrillUpgrades() ?: return 0.0

        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()
        for (internalName in drillUpgrades) {
            val name = internalName.getItemName()
            val price = internalName.getPriceOrNull() ?: continue

            totalPrice += price
            val format = NumberUtil.format(price)
            map[" $name §7(§6$format§7)"] = price
        }
        if (map.isNotEmpty()) {
            list.add("§7Drill upgrades: §6" + NumberUtil.format(totalPrice))
            list += map.sortedDesc().keys
        }
        return totalPrice
    }

    private fun addPowerScrolls(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getPowerScroll() ?: return 0.0

        val price = internalName.getPrice()
        val name = internalName.getItemName().removeColor()
        list.add("§7$name: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addHelmetSkin(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getHelmetSkin() ?: return 0.0

        val price = internalName.getPrice()
        val name = internalName.getNameOrRepoError()
        val displayname = name ?: "§c${internalName.asString()}"
        list.add("§7Skin: $displayname §7(§6" + NumberUtil.format(price) + "§7)")
        if (name == null) {
            list.add("   §8(Not yet in NEU Repo)")
        }
        return price
    }

    private fun addArmorDye(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getArmorDye() ?: return 0.0

        val price = internalName.getPrice()
        val name = internalName.getNameOrRepoError()
        val displayname = name ?: "§c${internalName.asString()}"
        list.add("§7Dye: $displayname §7(§6" + NumberUtil.format(price) + "§7)")
        if (name == null) {
            list.add("   §8(Not yet in NEU Repo)")
        }
        return price
    }

    private fun addEnrichment(stack: ItemStack, list: MutableList<String>): Double {

        val enrichmentName = stack.getEnrichment() ?: return 0.0
        val internalName = "TALISMAN_ENRICHMENT_$enrichmentName".asInternalName()

        val price = internalName.getPrice()
        val name = internalName.getItemName()
        list.add("§7Enrichment: $name §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addRune(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getRune() ?: return 0.0

        val price = internalName.getPrice()
        val name = internalName.getItemNameOrNull()
        val displayname = name ?: "§c${internalName.asString()}"
        list.add("§7Rune: $displayname §7(§6" + NumberUtil.format(price) + "§7)")
        if (name == null) {
            list.add("   §8(Not yet in NEU Repo)")
        }
        return price
    }

    private fun NEUInternalName.getNameOrRepoError(): String? {
        val stack = getItemStackOrNull() ?: return null
        return stack.nameWithEnchantment ?: "§cItem Name Error"
    }

    private fun addAbilityScrolls(stack: ItemStack, list: MutableList<String>): Double {
        val abilityScrolls = stack.getAbilityScrolls() ?: return 0.0

        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()
        for (internalName in abilityScrolls) {
            val name = internalName.getItemName()
            val price = internalName.getPriceOrNull() ?: continue

            totalPrice += price
            val format = NumberUtil.format(price)
            map[" $name §7(§6$format§7)"] = price
        }
        if (map.isNotEmpty()) {
            list.add("§7Ability Scrolls: §6" + NumberUtil.format(totalPrice))
            list += map.sortedDesc().keys
        }
        return totalPrice
    }

    private fun addBaseItem(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getInternalName()
        var price = internalName.getPrice()
        if (price == -1.0) {
            price = 0.0
        }

        val name = internalName.getItemName()
        if (internalName.startsWith("ENCHANTED_BOOK_BUNDLE_")) {
            list.add("§7Base item: $name")
            return 0.0
        }

        list.add("§7Base item: $name §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private val hasAlwaysScavenger = listOf(
        "CRYPT_DREADLORD_SWORD".asInternalName(),
        "ZOMBIE_SOLDIER_CUTLASS".asInternalName(),
        "CONJURING_SWORD".asInternalName(),
        "EARTH_SHARD".asInternalName(),
        "ZOMBIE_KNIGHT_SWORD".asInternalName(),
        "SILENT_DEATH".asInternalName(),
        "ZOMBIE_COMMANDER_WHIP".asInternalName(),
    )

    private fun addEnchantments(stack: ItemStack, list: MutableList<String>): Double {
        val enchantments = stack.getEnchantments() ?: return 0.0

        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()

        val tieredEnchants = listOf("compact", "cultivating", "champion", "expertise", "hecatomb")

        val internalName = stack.getInternalName()
        for ((rawName, rawLevel) in enchantments) {
            // efficiency 1-5 is cheap, 6-10 is handled by silex
            if (rawName == "efficiency") continue

            if (rawName == "scavenger" && rawLevel == 5 && internalName in hasAlwaysScavenger) {
                continue
            }

            var level = rawLevel
            var multiplier = 1
            if (rawName == "ultimate_chimera" || rawName == "ultimate_fatal_tempo" || rawName == "smoldering") {

                when (rawLevel) {
                    2 -> multiplier = 2
                    3 -> multiplier = 4
                    4 -> multiplier = 8
                    5 -> multiplier = 16
                }
                level = 1

            }
            if (internalName.startsWith("ENCHANTED_BOOK_BUNDLE_")) {
                multiplier = 5
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
            val format = NumberUtil.format(price)


            map[" $name §7(§6$format§7)"] = price
        }
        val enchantmentsCap: Int = config.enchantmentsCap.get()
        if (map.isNotEmpty()) {
            list.add("§7Enchantments: §6" + NumberUtil.format(totalPrice))
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

            val name = internalName.getItemName()
            val price = internalName.getPrice() * amount

            totalPrice += price
            val format = NumberUtil.format(price)

            val text = if (amount == 1) {
                " $name §7(§6$format§7)"
            } else {
                " §8${amount}x $name §7(§6$format§7)"
            }
            priceMap[text] = price
        }

        if (priceMap.isNotEmpty()) {
            list.add("§7Gemstones: §6" + NumberUtil.format(totalPrice))
            list += priceMap.sortedDesc().keys
        }
        return totalPrice
    }

    private fun addGemstoneSlotUnlockCost(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getInternalName()

        // item have to contains gems.unlocked_slots NBT array for unlocked slot detection
        val unlockedSlots =
            stack.getExtraAttributes()?.getCompoundTag("gems")?.getTag("unlocked_slots")?.toString() ?: return 0.0

        // TODO detection for old items which doesnt have gems.unlocked_slots NBT array
//        if (unlockedSlots == "null") return 0.0

        val priceMap = mutableMapOf<String, Double>()
        if (EstimatedItemValue.gemstoneUnlockCosts.isEmpty()) return 0.0

        if (internalName !in EstimatedItemValue.gemstoneUnlockCosts) {
            ErrorManager.logErrorState(
                "Could not find gemstone slot price for ${stack.name}",
                "EstimatedItemValue has no gemstoneUnlockCosts for $internalName"
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
                    getPrice(ingredient.internalItemId) * ingredient.count
                }
            }

            val splitSlot = slot.key.split("_") // eg. SAPPHIRE_1
            val colorCode = SkyBlockItemModifierUtils.GemstoneSlotType.getColorCode(splitSlot[0])
            val formattedPrice = NumberUtil.format(totalPrice - previousTotal)

            // eg. SAPPHIRE_1 -> Sapphire Slot 2
            val displayName = splitSlot[0].lowercase(Locale.ENGLISH).replaceFirstChar(Char::uppercase) + " Slot" +
                // If the slot index is 0, we don't need to specify
                if (splitSlot[1] != "0") " " + (splitSlot[1].toInt() + 1) else ""

            priceMap[" §$colorCode $displayName §7(§6$formattedPrice§7)"] = totalPrice - previousTotal
        }

        list.add("§7Gemstone Slot Unlock Cost: §6" + NumberUtil.format(totalPrice))
        list += priceMap.sortedDesc().keys
        return totalPrice
    }
}
