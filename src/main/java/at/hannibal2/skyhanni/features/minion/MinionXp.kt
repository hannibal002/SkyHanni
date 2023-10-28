package at.hannibal2.skyhanni.features.minion

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.MinionOpenEvent
import at.hannibal2.skyhanni.events.MinionStorageOpenEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzVec
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUInternalName.Companion.asInternalName
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import net.minecraft.block.BlockChest
import net.minecraft.client.Minecraft
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.*

class MinionXp {

    private val config get() = SkyHanniMod.feature.minions

    private data class XpInfo(val type: XpType, val amount: Double)

    private data class MinionStorage(val position: LorenzVec, val xpList: EnumMap<XpType, Double>) {
        val timestamp: SimpleTimeMark = SimpleTimeMark.now()
    }

    private data class PrimitiveItemStack(val name: NEUInternalName, val stackSize: Int)

    private fun toPrimitiveItemStack(itemStack: ItemStack) =
        PrimitiveItemStack(itemStack.getInternalName(), itemStack.stackSize)

    enum class XpType {
        Farming,
        Mining,
        Combat,
        Foraging,
        Fishing,
        Alchemy
    }

    private var collectItem: Item? = null

    private val minionStorages = mutableListOf<MinionStorage>()

    @SubscribeEvent
    fun onMinionOpen(event: MinionOpenEvent) {
        if (!config.xpDisplay) return

        collectItem = event.inventoryItems[48]?.item
        collectItemXpList.clear()

        val xpTotal = handleItems(event.inventoryItems, true)

        val missesStorage = MinionFeatures.lastMinion?.let { minionPosition ->
            getStorageXpAndUpdateTotal(minionPosition, xpTotal)
        } ?: false

        collectItemXpList.addAll(xpTotal.map { (type, amount) -> collectMessage(type, amount) })
        if (missesStorage) {
            collectItemXpList.add("§4No Minion Storage Data")
            collectItemXpList.add("§6Open Storage to get Correct Value")
        }
    }

    private fun getStorageXpAndUpdateTotal(
        minionPosition: LorenzVec,
        xpTotal: EnumMap<XpType, Double>
    ): Boolean {
        if (!getHasStorage(minionPosition)) return false
        val storage = minionStorages.firstOrNull {
            it.position.distanceSq(minionPosition) <= 2.5 && it.timestamp.passedSince().inWholeMinutes < 20
        }

        return if (storage != null) {
            storage.xpList.forEach { (type, amount) ->
                xpTotal.compute(type) { _, currentAmount -> (currentAmount ?: 0.0) + amount }
            }
            false
        } else {
            true
        }

    }

    private fun handleItems(inventoryItems: Map<Int, ItemStack>, isMinion: Boolean): EnumMap<XpType, Double> {
        val xpTotal = EnumMap<XpType, Double>(XpType::class.java)
        inventoryItems.filter {
            it.value.getLore().isNotEmpty() && (!isMinion || it.key in listOf(21..26, 30..35, 39..44).flatten())
        }.forEach { (_, itemStack) ->
            val item = toPrimitiveItemStack(itemStack)
            val name = item.name
            val xp = xpInfoMap[name] ?: return@forEach

            //TODO add wisdom and Derpy to calculation and random extra Exp Events
            val xpAmount = xp.amount * item.stackSize

            xpItemMap[item] = collectMessage(xp.type, xpAmount)
            xpTotal.compute(xp.type) { _, currentAmount -> (currentAmount ?: 0.0) + xpAmount }
        }
        return xpTotal
    }

    @SubscribeEvent
    fun onMinionStorageOpen(event: MinionStorageOpenEvent) {
        if (!config.xpDisplay) return

        val xpTotal = handleItems(event.inventoryItems, false)

        if (event.position == null) return
        minionStorages.removeIf { it.position == event.position }
        minionStorages.add(MinionStorage(event.position, xpTotal))
    }

    private fun collectMessage(type: XpType, amount: Double) =
        "§7Collect to get: §b${String.format("%.1f", amount)} §e${type.name} Xp"

    private fun getHasStorage(minionPosition: LorenzVec): Boolean {
        val positionsToCheck = listOf(
            LorenzVec(1, 0, 0), LorenzVec(0, 0, 1),
            LorenzVec(-1, 0, 0), LorenzVec(0, 0, -1)
        )

        return positionsToCheck.any { position ->
            val pos = minionPosition.add(position).toBlocPos()
            val block = Minecraft.getMinecraft().theWorld.getBlockState(pos).block
            block is BlockChest
        }
    }

    @SubscribeEvent
    fun onItemTooltipEvent(event: ItemTooltipEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.xpDisplay) return
        when {
            MinionFeatures.minionInventoryOpen -> {
                addXpInfoToTooltip(event)
                if (collectItem == event.itemStack.item) {
                    collectItemXpList.forEachIndexed { i, it ->
                        event.toolTip.add(i + 1, it)
                    }
                }
            }

            MinionFeatures.minionStorageInventoryOpen -> {
                addXpInfoToTooltip(event)
            }
        }
    }

    private fun addXpInfoToTooltip(event: ItemTooltipEvent) {
        xpItemMap[toPrimitiveItemStack(event.itemStack)]?.let {
            event.toolTip.add(1, it)
        }
    }

    @SubscribeEvent
    fun onIslandChangeEvent(event: IslandChangeEvent) {
        minionStorages.clear()
        xpItemMap.clear()
        collectItemXpList.clear()
    }

    companion object {
        private val xpItemMap: MutableMap<PrimitiveItemStack, String> = mutableMapOf()
        private val collectItemXpList: MutableList<String> = mutableListOf()
        fun onMinionClose() {
            xpItemMap.clear()
            collectItemXpList.clear()
        }
    }

    private val xpInfoMap: HashMap<NEUInternalName, XpInfo> = hashMapOf(
        //TODO Flowers
        "ABSOLUTE_ENDER_PEARL".asInternalName() to XpInfo(XpType.Combat, 7680.0),
        "LOG-2".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "ALLIUM".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "AZURE_BLUET".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "LOG-2".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "BLAZE_ROD".asInternalName() to XpInfo(XpType.Combat, 0.3),
        "COAL_BLOCK".asInternalName() to XpInfo(XpType.Mining, 2.7),
        "DIAMOND_BLOCK".asInternalName() to XpInfo(XpType.Mining, 3.6),
        "EMERALD_BLOCK".asInternalName() to XpInfo(XpType.Mining, 3.6),
        "GOLD_BLOCK".asInternalName() to XpInfo(XpType.Mining, 3.6),
        "IRON_BLOCK".asInternalName() to XpInfo(XpType.Mining, 2.7),
        "QUARTZ_BLOCK".asInternalName() to XpInfo(XpType.Mining, 1.2),
        "BLOCK_OF_REDSTONE".asInternalName() to XpInfo(XpType.Mining, 1.8),
        "BLUE_ORCHID".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "BONE".asInternalName() to XpInfo(XpType.Combat, 0.2),
        "BOX_OF_SEEDS".asInternalName() to XpInfo(XpType.Farming, 2560.0),
        "BROWN_MUSHROOM".asInternalName() to XpInfo(XpType.Farming, 0.3),
        "BROWN_MUSHROOM_BLOCK".asInternalName() to XpInfo(XpType.Farming, 0.3),
        "CACTUS".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "CACTUS_GREEN".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "CARROT".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "CHILI_PEPPER".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "CLAY".asInternalName() to XpInfo(XpType.Fishing, 0.1),
        "CLAY_BLOCK".asInternalName() to XpInfo(XpType.Fishing, 0.4),
        "RAW_FISH-2".asInternalName() to XpInfo(XpType.Fishing, 2.0),
        "COAL".asInternalName() to XpInfo(XpType.Mining, 0.3),
        "COBBLESTONE".asInternalName() to XpInfo(XpType.Mining, 0.1),
        "COCOA_BEANS".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "CONCENTRATED_STONE".asInternalName() to XpInfo(XpType.Mining, 33177.6),
        "CRUDE_GABAGOOL".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "DANDELION".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "LOG_2-1".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "DIAMOND".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "EGG".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "EMERALD".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "ENCHANTED_ACACIA_LOG".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_BAKED_POTATO".asInternalName() to XpInfo(XpType.Farming, 2560.0),
        "ENCHANTED_BIRCH_LOG".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_BLAZE_POWDER".asInternalName() to XpInfo(XpType.Combat, 48.0),
        "ENCHANTED_BLAZE_ROD".asInternalName() to XpInfo(XpType.Combat, 7680.0),
        "ENCHANTED_BLOCK_OF_COAL".asInternalName() to XpInfo(XpType.Mining, 7680.0),
        "ENCHANTED_BONE".asInternalName() to XpInfo(XpType.Combat, 32.0),
        "ENCHANTED_BREAD".asInternalName() to XpInfo(XpType.Farming, 1.8),
        "ENCHANTED_BROWN_MUSHROOM".asInternalName() to XpInfo(XpType.Farming, 48.0),
        "ENCHANTED_CACTUS".asInternalName() to XpInfo(XpType.Farming, 12800.0),
        "ENCHANTED_CACTUS_GREEN".asInternalName() to XpInfo(XpType.Farming, 80.0),
        "ENCHANTED_CARROT".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_CLAY".asInternalName() to XpInfo(XpType.Fishing, 16.0),
        "ENCHANTED_CLOWNFISH".asInternalName() to XpInfo(XpType.Fishing, 320.0),
        "ENCHANTED_COAL".asInternalName() to XpInfo(XpType.Mining, 48.0),
        "ENCHANTED_COBBLESTONE".asInternalName() to XpInfo(XpType.Mining, 16.0),
        "ENCHANTED_COCOA_BEAN".asInternalName() to XpInfo(XpType.Farming, 32.0),
        "ENCHANTED_COOKED_FISH".asInternalName() to XpInfo(XpType.Fishing, 12800.0),
        "ENCHANTED_COOKED_MUTTON".asInternalName() to XpInfo(XpType.Farming, 2560.0),
        "ENCHANTED_COOKED_SALMON".asInternalName() to XpInfo(XpType.Fishing, 17920.0),
        "ENCHANTED_DANDELION".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_DARK_OAK_LOG".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_DIAMOND".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_DIAMOND_BLOCK".asInternalName() to XpInfo(XpType.Mining, 10240.0),
        "ENCHANTED_EGG".asInternalName() to XpInfo(XpType.Farming, 115.0),
        "ENCHANTED_EMERALD".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_EMERALD_BLOCK".asInternalName() to XpInfo(XpType.Mining, 10240.0),
        "ENCHANTED_ENDER_PEARL".asInternalName() to XpInfo(XpType.Combat, 48.0),
        "ENCHANTED_END_STONE".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_FEATHER".asInternalName() to XpInfo(XpType.Farming, 32.0),
        "ENCHANTED_FLINT".asInternalName() to XpInfo(XpType.Mining, 32.0),
        "ENCHANTED_GHAST_TEAR".asInternalName() to XpInfo(XpType.Combat, 75.0),
        "ENCHANTED_GLOWSTONE".asInternalName() to XpInfo(XpType.Mining, 6144.0),
        "ENCHANTED_GLOWSTONE_DUST".asInternalName() to XpInfo(XpType.Mining, 32.0),
        "ENCHANTED_GOLD".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_GOLD_BLOCK".asInternalName() to XpInfo(XpType.Mining, 10240.0),
        "ENCHANTED_GRILLED_PORK".asInternalName() to XpInfo(XpType.Farming, 5120.0),
        "ENCHANTED_GUNPOWDER".asInternalName() to XpInfo(XpType.Combat, 48.0),
        "ENCHANTED_HARD_STONE".asInternalName() to XpInfo(XpType.Mining, 57.6),
        "ENCHANTED_HAY_BLOCK".asInternalName() to XpInfo(XpType.Farming, 288.0),
        "ENCHANTED_ICE".asInternalName() to XpInfo(XpType.Mining, 80.0),
        "ENCHANTED_IRON".asInternalName() to XpInfo(XpType.Mining, 48.0),
        "ENCHANTED_IRON_BLOCK".asInternalName() to XpInfo(XpType.Mining, 7680.0),
        "ENCHANTED_JUNGLE_LOG".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_LAPIS_LAZULI_BLOCK".asInternalName() to XpInfo(XpType.Mining, 2560.0),
        "ENCHANTED_LAPIS_LAZULI".asInternalName() to XpInfo(XpType.Mining, 16.0),
        "ENCHANTED_LEATHER".asInternalName() to XpInfo(XpType.Farming, 115.0),
        "ENCHANTED_MAGMA_CREAM".asInternalName() to XpInfo(XpType.Combat, 32.0),
        "ENCHANTED_MELON".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_MELON_BLOCK".asInternalName() to XpInfo(XpType.Farming, 2560.0),
        "ENCHANTED_MITHRIL".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_MUTTON".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_MYCELIUM".asInternalName() to XpInfo(XpType.Mining, 32.0),
        "ENCHANTED_MYCELIUM_CUBE".asInternalName() to XpInfo(XpType.Mining, 5120.0),
        "ENCHANTED_NETHER_WART".asInternalName() to XpInfo(XpType.Farming, 48.0),
        "ENCHANTED_OAK_LOG".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_OBSIDIAN".asInternalName() to XpInfo(XpType.Combat, 64.0),
        "ENCHANTED_OBSIDIAN".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_PACKED_ICE".asInternalName() to XpInfo(XpType.Mining, 12800.0),
        "ENCHANTED_POISONOUS_POTATO".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "ENCHANTED_POPPY".asInternalName() to XpInfo(XpType.Foraging, 57.6),
        "ENCHANTED_PORK".asInternalName() to XpInfo(XpType.Farming, 32.0),
        "ENCHANTED_POTATO".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_PRISMARINE_CRYSTALS".asInternalName() to XpInfo(XpType.Fishing, 40.0),
        "ENCHANTED_PRISMARINE_SHARD".asInternalName() to XpInfo(XpType.Fishing, 40.0),
        "ENCHANTED_PUFFERFISH".asInternalName() to XpInfo(XpType.Fishing, 160.0),
        "ENCHANTED_PUMPKIN".asInternalName() to XpInfo(XpType.Farming, 48.0),
        "ENCHANTED_QUARTZ".asInternalName() to XpInfo(XpType.Combat, 48.0),
        "ENCHANTED_QUARTZ".asInternalName() to XpInfo(XpType.Mining, 48.0),
        "ENCHANTED_QUARTZ_BLOCK".asInternalName() to XpInfo(XpType.Combat, 7680.0),
        "ENCHANTED_QUARTZ_BLOCK".asInternalName() to XpInfo(XpType.Mining, 7680.0),
        "ENCHANTED_RABBIT_FOOT".asInternalName() to XpInfo(XpType.Farming, 32.0),
        "ENCHANTED_RABBIT_HIDE".asInternalName() to XpInfo(XpType.Farming, 115.0),
        "ENCHANTED_RAW_BEEF".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_RAW_CHICKEN".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_RAW_FISH".asInternalName() to XpInfo(XpType.Fishing, 80.0),
        "ENCHANTED_RAW_RABBIT".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_RAW_SALMON".asInternalName() to XpInfo(XpType.Fishing, 112.0),
        "ENCHANTED_REDSTONE".asInternalName() to XpInfo(XpType.Mining, 32.0),
        "ENCHANTED_REDSTONE_BLOCK".asInternalName() to XpInfo(XpType.Mining, 5120.0),
        "ENCHANTED_RED_MUSHROOM".asInternalName() to XpInfo(XpType.Farming, 48.0),
        "ENCHANTED_RED_SAND".asInternalName() to XpInfo(XpType.Mining, 32.0),
        "ENCHANTED_RED_SAND_CUBE".asInternalName() to XpInfo(XpType.Mining, 5120.0),
        "ENCHANTED_ROTTEN_FLESH".asInternalName() to XpInfo(XpType.Combat, 48.0),
        "ENCHANTED_ROTTEN_FLESH".asInternalName() to XpInfo(XpType.Mining, 48.0),
        "ENCHANTED_SAND".asInternalName() to XpInfo(XpType.Mining, 32.0),
        "ENCHANTED_SEEDS".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENCHANTED_SLIME_BALL".asInternalName() to XpInfo(XpType.Combat, 32.0),
        "ENCHANTED_SLIME_BLOCK".asInternalName() to XpInfo(XpType.Combat, 5120.0),
        "ENCHANTED_SNOW_BLOCK".asInternalName() to XpInfo(XpType.Mining, 64.0),
        "ENCHANTED_SPIDER_EYE".asInternalName() to XpInfo(XpType.Combat, 48.0),
        "ENCHANTED_SPONGE".asInternalName() to XpInfo(XpType.Fishing, 20.0),
        "ENCHANTED_SPRUCE_LOG".asInternalName() to XpInfo(XpType.Foraging, 16.0),
        "ENCHANTED_STRING".asInternalName() to XpInfo(XpType.Combat, 38.0),
        "ENCHANTED_SUGAR".asInternalName() to XpInfo(XpType.Alchemy, 16.0),
        "ENCHANTED_SUGAR_CANE".asInternalName() to XpInfo(XpType.Alchemy, 2560.0),
        "ENCHANTED_WET_SPONGE".asInternalName() to XpInfo(XpType.Fishing, 800.0),
        "ENCHANTED_WOOL".asInternalName() to XpInfo(XpType.Farming, 16.0),
        "ENDER_PEARL".asInternalName() to XpInfo(XpType.Combat, 0.3),
        "END_STONE".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "FEATHER".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "FLAME_DYE".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "FLINT".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "GABAGOOL_THE_FISH".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "GHAST_TEAR".asInternalName() to XpInfo(XpType.Combat, 0.5),
        "GLASS".asInternalName() to XpInfo(XpType.Mining, 0.0),
        "GLOWSTONE".asInternalName() to XpInfo(XpType.Mining, 0.8),
        "GLOWSTONE_DUST".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "GOLD_INGOT".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "GOLD_ORE".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "GRAVEL".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "GUNPOWDER".asInternalName() to XpInfo(XpType.Combat, 0.3),
        "HARD_STONE".asInternalName() to XpInfo(XpType.Mining, 0.1),
        "HAY_BLOCK".asInternalName() to XpInfo(XpType.Farming, 1.8),
        "HEMOBOMB".asInternalName() to XpInfo(XpType.Combat, 12000.0),
        "HEMOGLASS".asInternalName() to XpInfo(XpType.Combat, 800.0),
        "HEMOVIBE".asInternalName() to XpInfo(XpType.Combat, 5.0),
        "ICE".asInternalName() to XpInfo(XpType.Mining, 0.5),
        "INFERNO_APEX".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "INFERNO_VERTEX".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "IRON_INGOT".asInternalName() to XpInfo(XpType.Mining, 0.3),
        "IRON_ORE".asInternalName() to XpInfo(XpType.Mining, 0.3),
        "LOG-3".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "INK_SACK-4".asInternalName() to XpInfo(XpType.Mining, 0.1),
        "LAPIS_LAZULI_BLOCK".asInternalName() to XpInfo(XpType.Mining, 0.9),
        "LEATHER".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "LILAC".asInternalName() to XpInfo(XpType.Foraging, 0.2),
        "MAGMA_CREAM".asInternalName() to XpInfo(XpType.Combat, 0.2),
        "MELON".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "MELON_BLOCK".asInternalName() to XpInfo(XpType.Farming, 0.9),
        "MITHRIL".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "MUTANT_NETHER_WART".asInternalName() to XpInfo(XpType.Farming, 7680.0),
        "MYCELIUM".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "QUARTZ".asInternalName() to XpInfo(XpType.Mining, 0.3),
        "NETHER_WART".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "LOG".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "OBSIDIAN".asInternalName() to XpInfo(XpType.Combat, 0.4),
        "OBSIDIAN".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "OMEGA_ENCHANTED_EGG".asInternalName() to XpInfo(XpType.Farming, 149040.0),
        "ORANGE_TULIP".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "OXEYE_DAISY".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "PACKED_ICE".asInternalName() to XpInfo(XpType.Mining, 4.5),
        "PEONY".asInternalName() to XpInfo(XpType.Foraging, 0.2),
        "PINK_TULIP".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "POISONOUS_POTATO".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "POLISHED_PUMPKIN".asInternalName() to XpInfo(XpType.Farming, 7680.0),
        "POPPY".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "POTATO".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "PRISMARINE_CRYSTALS".asInternalName() to XpInfo(XpType.Fishing, 0.5),
        "PRISMARINE_SHARD".asInternalName() to XpInfo(XpType.Fishing, 0.5),
        "PUFFERFISH".asInternalName() to XpInfo(XpType.Fishing, 1.0),
        "PUMPKIN".asInternalName() to XpInfo(XpType.Farming, 0.3),
        "RABBIT'S_FOOT".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "RABBIT_HIDE".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "RAW_BEEF".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "RAW_CHICKEN".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "RAW_FISH".asInternalName() to XpInfo(XpType.Fishing, 0.5),
        "MUTTON".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "RAW_PORKCHOP".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "RAW_RABBIT".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "RAW_FISH-1".asInternalName() to XpInfo(XpType.Fishing, 0.7),
        "REAPER_PEPPER".asInternalName() to XpInfo(XpType.Combat, 0.0),
        "REDSTONE".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "RED_MUSHROOM".asInternalName() to XpInfo(XpType.Farming, 0.3),
        "RED_MUSHROOM_BLOCK".asInternalName() to XpInfo(XpType.Farming, 0.3),
        "RED_SAND".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "RED_TULIP".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "ROSE_BUSH".asInternalName() to XpInfo(XpType.Foraging, 0.2),
        "ROTTEN_FLESH".asInternalName() to XpInfo(XpType.Combat, 0.3),
        "SAND".asInternalName() to XpInfo(XpType.Mining, 0.2),
        "SEEDS".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "SLIME_BALL".asInternalName() to XpInfo(XpType.Combat, 0.2),
        "SLIME_BLOCK".asInternalName() to XpInfo(XpType.Combat, 1.8),
        "SNOWBALL".asInternalName() to XpInfo(XpType.Mining, 0.1),
        "SNOW_BLOCK".asInternalName() to XpInfo(XpType.Mining, 0.4),
        "SPIDER_EYE".asInternalName() to XpInfo(XpType.Combat, 0.3),
        "SPONGE".asInternalName() to XpInfo(XpType.Fishing, 0.5),
        "LOG-1".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "STONE".asInternalName() to XpInfo(XpType.Mining, 0.1),
        "STRING".asInternalName() to XpInfo(XpType.Combat, 0.2),
        "SUGAR_CANE".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "SUNFLOWER".asInternalName() to XpInfo(XpType.Foraging, 0.2),
        "SUPER_EGG".asInternalName() to XpInfo(XpType.Farming, 16560.0),
        "TIGHTLY_TIED_HAY_BALE".asInternalName() to XpInfo(XpType.Farming, 41472.0),
        "WHEAT".asInternalName() to XpInfo(XpType.Farming, 0.2),
        "WHITE_TULIP".asInternalName() to XpInfo(XpType.Foraging, 0.1),
        "WOOL".asInternalName() to XpInfo(XpType.Farming, 0.1),
        "LUSH_BERBERIS".asInternalName() to XpInfo(XpType.Farming, 0.1), //TODO
        "ENCHANTED_LUSH_BERBERIS".asInternalName() to XpInfo(XpType.Farming, 0.1), //TODO
    )
}
