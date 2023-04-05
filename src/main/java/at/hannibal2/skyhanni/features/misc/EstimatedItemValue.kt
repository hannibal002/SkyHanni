package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.sortedDesc
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getAbilityScrolls
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDrillUpgrades
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getDye
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getEnchantments
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getFarmingForDummiesCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getGemstones
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHelmetSkin
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getHotPotatoCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getManaDisintegrators
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getMasterStars
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getPowerScroll
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getSilexCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getTransmissionTunerCount
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasArtOfPiece
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasArtOfWar
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasBookOfStats
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasEtherwarp
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasJalapenoBook
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.hasWoodSingularity
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.isRecombobulated
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.moulconfig.internal.KeybindHelper
import io.github.moulberry.notenoughupdates.util.Constants
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EstimatedItemValue {
    private val config get() = SkyHanniMod.feature.misc
    private var display = listOf<List<Any>>()
    private val cache = mutableMapOf<ItemStack, List<List<Any>>>()
    private var lastToolTipTime = 0L

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.ChestBackgroundRenderEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.estimatedIemValueEnabled) return
        if (!KeybindHelper.isKeyDown(config.estimatedItemValueHotkey) && !config.estimatedIemValueAlwaysEnabled) return
        if (System.currentTimeMillis() > lastToolTipTime + 200) return

        config.itemPriceDataPos.renderStringsAndItems(display, posLabel = "Estimated Item Value")
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        cache.clear()
    }

    @SubscribeEvent
    fun onItemTooltipLow(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.estimatedIemValueEnabled) return

        val item = event.itemStack
        val oldData = cache[item]
        if (oldData != null) {
            display = oldData
            lastToolTipTime = System.currentTimeMillis()
            return
        }

        val newDisplay = try {
            draw(item)
        } catch (e: Exception) {
            LorenzUtils.debug("Estimated Item Value error: ${e.message}")
            e.printStackTrace()
            listOf()
        }

        cache[item] = newDisplay
        display = newDisplay
        lastToolTipTime = System.currentTimeMillis()
    }

    private fun draw(stack: ItemStack): List<List<Any>> {
        val list = mutableListOf<String>()
        list.add("§aEstimated Item Value:")

        val internalName = stack.getInternalName()
        if (internalName == "") return listOf()

        //FIX neu item list
        if (internalName.startsWith("ULTIMATE_ULTIMATE_")) return listOf()
        // we don't need this feature to work on books at all
        if (stack.item == Items.enchanted_book) return listOf()
        // block catacombs items in mort inventory
        if (internalName.startsWith("CATACOMBS_PASS_") || internalName.startsWith("MASTER_CATACOMBS_PASS_")) return listOf()
        // blocks the dungeon map
        if (internalName.startsWith("MAP-")) return listOf()


        if (NEUItems.getItemStackOrNull(internalName) == null) {
            LorenzUtils.debug("Estimated Item Value is null for internal name: '$internalName'")
            return listOf()
        }

        var totalPrice = 0.0
        val basePrice = addBaseItem(stack, list)
        totalPrice += basePrice
        totalPrice += addReforgeStone(stack, list)

        // once
        totalPrice += addRecomb(stack, list)
        totalPrice += addArtOfWar(stack, list)
        totalPrice += addArtOfPiece(stack, list)
        totalPrice += addEtherwarp(stack, list)
        totalPrice += addPowerScrolls(stack, list)
        totalPrice += addWoodSingularity(stack, list)
        totalPrice += addJalapenoBook(stack, list)
        totalPrice += addStatsBook(stack, list)

        // counted
        totalPrice += addMasterStars(stack, list)
        totalPrice += addHotPotatoBooks(stack, list)
        totalPrice += addFarmingForDummies(stack, list)
        totalPrice += addSilex(stack, list)
        totalPrice += addTransmissionTuners(stack, list)
        totalPrice += addManaDisintegrators(stack, list)

        // dynamic
        totalPrice += addAbilityScrolls(stack, list)
        totalPrice += addDrillUpgrades(stack, list)
        totalPrice += addGemstones(stack, list)
        totalPrice += addEnchantments(stack, list)
        totalPrice += addHelmetSkin(stack, list)
        totalPrice += addDye(stack, list)

        if (basePrice == totalPrice) return listOf()

        list.add("§aTotal: §6§l" + NumberUtil.format(totalPrice))

        val newDisplay = mutableListOf<List<Any>>()
        for (line in list) {
            newDisplay.addAsSingletonList(line)
        }
        return newDisplay
    }

    private fun addReforgeStone(stack: ItemStack, list: MutableList<String>): Double {
        val rawReforgeName = stack.getReforgeName() ?: return 0.0

        for ((internalName, values) in Constants.REFORGESTONES.entrySet()) {
            val stone = values.asJsonObject
            val reforgeName = stone.get("reforgeName").asString
            if (rawReforgeName == reforgeName.lowercase()) {
                val price = NEUItems.getPrice(internalName)
                val name = NEUItems.getItemStack(internalName).name
                list.add("§7Reforge: §9$reforgeName")
                list.add("  §7($name §6" + NumberUtil.format(price) + "§7)")
                return price
            }
        }

        return 0.0
    }

    private fun addRecomb(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.isRecombobulated()) {
            val wtfHardcodedRecomb = "RECOMBOBULATOR_3000"
            val price = NEUItems.getPrice(wtfHardcodedRecomb)
            list.add("§7Recombobulated: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    private fun addJalapenoBook(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.hasJalapenoBook()) {
            val wtfHardcodedJalapeno = "JALAPENO_BOOK"

            val price = NEUItems.getPrice(wtfHardcodedJalapeno)
            list.add("§7Jalapeno Book: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    private fun addEtherwarp(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.hasEtherwarp()) {
            val wtfHardcodedConduit = "ETHERWARP_CONDUIT"
            val wtfHardcodedMerger = "ETHERWARP_MERGER"
            val price = NEUItems.getPrice(wtfHardcodedConduit) + NEUItems.getPrice(wtfHardcodedMerger)
            list.add("§7Etherwarp: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    private fun addWoodSingularity(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.hasWoodSingularity()) {
            val wtfHardcodedSingularity = "WOOD_SINGULARITY"
            val price = NEUItems.getPrice(wtfHardcodedSingularity)
            list.add("§7Wood Singularity: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    private fun addArtOfWar(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.hasArtOfWar()) {
            val ripTechno = "THE_ART_OF_WAR"
            val price = NEUItems.getPrice(ripTechno)
            list.add("§7The Art of War: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    private fun addStatsBook(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.hasBookOfStats()) {
            val ripTechno = "BOOK_OF_STATS"
            val price = NEUItems.getPrice(ripTechno)
            list.add("§7Book of Stats: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    // TODO untested
    private fun addArtOfPiece(stack: ItemStack, list: MutableList<String>): Double {
        if (stack.hasArtOfPiece()) {
            val ripTechno = "THE_ART_OF_PEACE"
            val price = NEUItems.getPrice(ripTechno)
            list.add("§7The Art Of Piece: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
            return price
        }

        return 0.0
    }

    private fun addHotPotatoBooks(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getHotPotatoCount()
        if (count == 0) return 0.0

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

        val wtfHardcodedHpb = "HOT_POTATO_BOOK"
        val hpbPrice = NEUItems.getPrice(wtfHardcodedHpb) * hpb
        list.add("§7HPB's: §e$hpb§7/§e10 §7(§6" + NumberUtil.format(hpbPrice) + "§7)")
        totalPrice += hpbPrice

        if (fuming > 0) {
            val wtfHardcodedFuming = "FUMING_POTATO_BOOK"
            val fumingPrice = NEUItems.getPrice(wtfHardcodedFuming) * fuming
            list.add("§7Fuming: §e$fuming§7/§e5 §7(§6" + NumberUtil.format(fumingPrice) + "§7)")
            totalPrice += fumingPrice
        }

        return totalPrice
    }

    private fun addFarmingForDummies(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getFarmingForDummiesCount()
        if (count == 0) return 0.0

        val wtfHardcodedDumbFarmers = "FARMING_FOR_DUMMIES"
        val price = NEUItems.getPrice(wtfHardcodedDumbFarmers) * count
        list.add("§7Farming for Dummies: §e$count§7/§e5 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addSilex(stack: ItemStack, list: MutableList<String>): Double {
        val tier = stack.getSilexCount()
        if (tier == 0) return 0.0

        val internalName = stack.getInternalName()
        val maxTier = if (internalName == "STONK_PICKAXE") 4 else 5

        val wtfHardcodedSilex = "SIL_EX"
        val price = NEUItems.getPrice(wtfHardcodedSilex) * tier
        list.add("§7Silex: §e$tier§7/§e$maxTier §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addTransmissionTuners(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getTransmissionTunerCount()
        if (count == 0) return 0.0

        val wtfHardcodedTuner = "TRANSMISSION_TUNER"
        val price = NEUItems.getPrice(wtfHardcodedTuner) * count
        list.add("§7Transmission Tuners: §e$count§7/§e4 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addManaDisintegrators(stack: ItemStack, list: MutableList<String>): Double {
        val count = stack.getManaDisintegrators()
        if (count == 0) return 0.0

        val wtfHardcodedTuner = "MANA_DISINTEGRATOR"
        val price = NEUItems.getPrice(wtfHardcodedTuner) * count
        list.add("§7Mana Disintegrators: §e$count§7/§e10 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addMasterStars(stack: ItemStack, list: MutableList<String>): Double {
        val masterStars = stack.getMasterStars()
        if (masterStars == 0) return 0.0

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
                val internalName = "${prefix}_MASTER_STAR"
                price += NEUItems.getPrice(internalName)
            }
        }

        list.add("§7Master Stars: §e$masterStars§7/§e5 §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addDrillUpgrades(stack: ItemStack, list: MutableList<String>): Double {
        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()
        for (internalName in stack.getDrillUpgrades()) {
            val name = NEUItems.getItemStackOrNull(internalName)!!.name
            val price = NEUItems.getPriceOrNull(internalName) ?: continue

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

        val price = NEUItems.getPrice(internalName)
        val name = NEUItems.getItemStack(internalName).name!!.removeColor()
        list.add("§7$name: §a§l✔ §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addHelmetSkin(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getHelmetSkin() ?: return 0.0

        val price = NEUItems.getPrice(internalName)
        val name = NEUItems.getItemStack(internalName).name
        list.add("§7Skin: $name §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addDye(stack: ItemStack, list: MutableList<String>): Double {
        val internalName = stack.getDye() ?: return 0.0

        val price = NEUItems.getPrice(internalName)
        val name = NEUItems.getItemStack(internalName).name
        list.add("§7Dye: $name §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addAbilityScrolls(stack: ItemStack, list: MutableList<String>): Double {
        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()
        for (internalName in stack.getAbilityScrolls()) {
            val name = NEUItems.getItemStackOrNull(internalName)!!.name
            val price = NEUItems.getPriceOrNull(internalName) ?: continue

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
        var price = NEUItems.getPrice(internalName)
        if (price == -1.0) {
            price = 0.0
        }

        val name = NEUItems.getItemStack(internalName).name
        if (internalName.startsWith("ENCHANTED_BOOK_BUNDLE_")) {
            list.add("§7Base item: $name")
            return 0.0
        }

        list.add("§7Base item: $name §7(§6" + NumberUtil.format(price) + "§7)")
        return price
    }

    private fun addEnchantments(stack: ItemStack, list: MutableList<String>): Double {
        var totalPrice = 0.0
        val map = mutableMapOf<String, Double>()

        val tieredEnchants = listOf("compact", "cultivating", "champion", "expertise", "hecatomb")
        val hasAlwaysScavenger = listOf(
            "CRYPT_DREADLORD_SWORD",
            "ZOMBIE_SOLDIER_CUTLASS",
            "CONJURING_SWORD",
            "EARTH_SHARD",
            "ZOMBIE_KNIGHT_SWORD",
            "SILENT_DEATH",
            "ZOMBIE_COMMANDER_WHIP",
        )

        val internalName = stack.getInternalName()
        for ((rawName, rawLevel) in stack.getEnchantments()) {
            // efficiency 1-5 is cheap, 6-10 is handled by silex
            if (rawName == "efficiency") continue

            if (rawName == "scavenger" && rawLevel == 5) {
                if (internalName in hasAlwaysScavenger) continue
            }

            var level = rawLevel
            var multiplier = 1
            if (rawName == "ultimate_chimera") {

                when (rawLevel) {
                    2 -> multiplier = 2
                    3 -> multiplier = 4
                    4 -> multiplier = 8
                    5 -> multiplier = 16
                }
                level = 1
                println("")
                println("rawName: $rawName")
                println("rawLevel: $rawLevel")
                println("multiplier: $multiplier")

            }
            if (internalName.startsWith("ENCHANTED_BOOK_BUNDLE_")) {
                multiplier = 5
            }
            if (rawName in tieredEnchants) level = 1

            val enchantmentName = "$rawName;$level".uppercase()
            val itemStack = NEUItems.getItemStackOrNull(enchantmentName) ?: continue
            val singlePrice = NEUItems.getPriceOrNull(enchantmentName) ?: continue


            var name = itemStack.getLore()[0]
            if (multiplier > 1) {
                name = "§8${multiplier}x $name"
            }
            val price = singlePrice * multiplier

            totalPrice += price
            val format = NumberUtil.format(price)


            map[" $name §7(§6$format§7)"] = price
        }
        if (map.isNotEmpty()) {
            list.add("§7Enchantments: §6" + NumberUtil.format(totalPrice))
            var i = 0
            val size = map.size
            for (entry in map.sortedDesc().keys) {
                if (i == 7) {
                    val missing = size - i
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
        val gemstones = stack.getGemstones()
        if (gemstones.isEmpty()) return 0.0

        var totalPrice = 0.0
        val counterMap = mutableMapOf<String, Int>()
        for (gemstone in gemstones) {
            val internalName = gemstone.getInternalName()
            val old = counterMap[internalName] ?: 0
            counterMap[internalName] = old + 1
        }

        val priceMap = mutableMapOf<String, Double>()
        for ((internalName, amount) in counterMap) {

            val name = NEUItems.getItemStack(internalName).name
            val price = NEUItems.getPrice(internalName) * amount

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

    class GemstoneSlot(val type: GemstoneType, val tier: GemstoneTier) {
        fun getInternalName() = "${tier}_${type}_GEM"
    }

    enum class GemstoneTier(val displayName: String) {
        ROUGH("Rough"),
        FLAWED("Flawed"),
        FINE("Fine"),
        FLAWLESS("Flawless"),
        PERFECT("Perfect"),
        ;

        companion object {
            fun getByName(name: String) = GemstoneTier.values().firstOrNull { it.name == name }
        }
    }

    enum class GemstoneType(val displayName: String) {
        JADE("Jade"),
        AMBER("Amber"),
        TOPAZ("Topaz"),
        SAPPHIRE("Sapphire"),
        AMETHYST("Amethyst"),
        JASPER("Jasper"),
        RUBY("Ruby"),
        OPAL("Opal"),
        ;

        companion object {
            fun getByName(name: String) = values().firstOrNull { it.name == name }
        }
    }
}