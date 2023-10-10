package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.Storage
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.IslandChangeEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.SackChangeEvent
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPriceOrNull
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.format
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import net.minecraft.client.Minecraft
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EnderNodeTracker {
    private val config get() = SkyHanniMod.feature.combat.enderNodeTracker
    private val storage get() = ProfileStorageData.profileSpecific?.enderNodeTracker

    private var totalEnderArmor = 0
    private var miteGelInInventory = 0
    private var display = emptyList<List<Any>>()
    private var lootProfit = mapOf<EnderNode, Double>()

    private val enderNodeRegex = Regex("""ENDER NODE!.+You found (\d+x )?§r(.+)§r§f!""")
    private val endermanRegex = Regex("""(RARE|PET) DROP! §r(.+) §r§b\(""")

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!config.enabled) return
        if (!ProfileStorageData.loaded) return
        if (!isInTheEnd()) return

        // don't call removeColor because we want to distinguish enderman pet rarity
        val message = event.message.trim()
        var item: String? = null
        var amount = 1
        val storage = storage ?: return

        // check whether the loot is from an ender node or an enderman
        enderNodeRegex.find(message)?.let {
            storage.totalNodesMined++
            amount = it.groups[1]?.value?.substringBefore("x")?.toIntOrNull() ?: 1
            item = it.groups[2]?.value
        } ?: endermanRegex.find(message)?.let {
            amount = 1
            item = it.groups[2]?.value
        }

        when {
            item == null -> return
            isEnderArmor(item) -> totalEnderArmor++
            item == "§cEndermite Nest" -> {
                storage.totalEndermiteNests++
            }
        }

        // increment the count of the specific item found
        EnderNode.entries.find { it.displayName == item }?.let {
            val old = storage.lootCount[it] ?: 0
            storage.lootCount = storage.lootCount.editCopy {
                this[it] = old + amount
            }
        }
        update()
    }

    @SubscribeEvent
    fun onIslandChange(event: IslandChangeEvent) {
        if (!config.enabled) return
        if (event.newIsland != IslandType.THE_END) return
        miteGelInInventory = Minecraft.getMinecraft().thePlayer.inventory.mainInventory
            .filter { it?.getInternalNameOrNull() == EnderNode.MITE_GEL.internalName }
            .sumOf { it.stackSize }
    }

    @SubscribeEvent
    fun onSackChange(event: SackChangeEvent) {
        if (!config.enabled) return
        if (!ProfileStorageData.loaded) return
        if (!isInTheEnd()) return
        val storage = storage ?: return

        val change = event.sackChanges
            .firstOrNull { it.internalName == EnderNode.MITE_GEL.internalName && it.delta > 0 }
            ?: return
        val old = storage.lootCount[EnderNode.MITE_GEL] ?: 0
        storage.lootCount = storage.lootCount.editCopy {
            this[EnderNode.MITE_GEL] = old + change.delta
        }
        update()
    }

    @SubscribeEvent
    fun onInventoryUpdate(event: OwnInventoryItemUpdateEvent) {
        if (!config.enabled) return
        if (!isInTheEnd()) return
        if (!ProfileStorageData.loaded) return
        val storage = storage ?: return

        MinecraftExecutor.OnThread.execute {
            val newMiteGelInInventory = Minecraft.getMinecraft().thePlayer.inventory.mainInventory
                .filter { it?.getInternalNameOrNull() == EnderNode.MITE_GEL.internalName }
                .sumOf { it.stackSize }
            val change = newMiteGelInInventory - miteGelInInventory
            if (change > 0) {
                val old = storage.lootCount[EnderNode.MITE_GEL] ?: 0
                storage.lootCount = storage.lootCount.editCopy {
                    this[EnderNode.MITE_GEL] = old + change
                }
                update()
            }
            miteGelInInventory = newMiteGelInInventory
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent.GuiOverlayRenderEvent) {
        if (!config.enabled) return
        if (!isInTheEnd()) return
        config.position.renderStringsAndItems(display, posLabel = "Ender Node Tracker")
    }

    @SubscribeEvent
    fun onConfigLoad(event: ConfigLoadEvent) {
        config.textFormat.afterChange {
            update()
        }
        val storage = storage ?: return

        totalEnderArmor = storage.lootCount.filter { isEnderArmor(it.key.displayName) }
            .map { it.value }
            .sum()
        update()
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(2, "misc.enderNodeTracker", "combat.enderNodeTracker")
    }

    private fun calculateProfit(storage: Storage.ProfileSpecific.EnderNodeTracker): Map<EnderNode, Double> {
        if (!ProfileStorageData.loaded) return emptyMap()

        val newProfit = mutableMapOf<EnderNode, Double>()
        storage.lootCount.forEach { (item, amount) ->
            val price = if (isEnderArmor(item.displayName)) {
                10_000.0
            } else {
                (if (!LorenzUtils.noTradeMode) item.internalName.getPriceOrNull() else 0.0)
                    ?.coerceAtLeast(item.internalName.getNpcPriceOrNull() ?: 0.0)
                    ?.coerceAtLeast(georgePrice(item) ?: 0.0)
                    ?: 0.0
            }
            newProfit[item] = price * amount
        }
        return newProfit
    }

    private fun update() {
        val storage = storage ?: return
        lootProfit = calculateProfit(storage)
        display = formatDisplay(drawDisplay(storage))
    }

    private fun isInTheEnd() = LorenzUtils.skyBlockArea == "The End"

    private fun isEnderArmor(displayName: String?) = when (displayName) {
        EnderNode.END_HELMET.displayName,
        EnderNode.END_CHESTPLATE.displayName,
        EnderNode.END_LEGGINGS.displayName,
        EnderNode.END_BOOTS.displayName,
        EnderNode.ENDER_NECKLACE.displayName,
        EnderNode.ENDER_GAUNTLET.displayName -> true

        else -> false
    }

    private fun georgePrice(petRarity: EnderNode): Double? = when (petRarity) {
        EnderNode.COMMON_ENDERMAN_PET -> 100.0
        EnderNode.UNCOMMON_ENDERMAN_PET -> 500.0
        EnderNode.RARE_ENDERMAN_PET -> 2_000.0
        EnderNode.EPIC_ENDERMAN_PET -> 10_000.0
        EnderNode.LEGENDARY_ENDERMAN_PET -> 1_000_000.0
        else -> null
    }

    private fun drawDisplay(storage: Storage.ProfileSpecific.EnderNodeTracker) = buildList<List<Any>> {
        if (!ProfileStorageData.loaded) return emptyList<List<Any>>()

        addAsSingletonList("§5§lEnder Node Tracker")
        addAsSingletonList("§d${storage.totalNodesMined.addSeparators()} Ender Nodes mined")
        addAsSingletonList("§6${format(lootProfit.values.sum())} Coins made")
        addAsSingletonList(" ")
        addAsSingletonList("§b${storage.totalEndermiteNests.addSeparators()} §cEndermite Nest")

        for (item in EnderNode.entries.subList(0, 11)) {
            val count = (storage.lootCount[item] ?: 0).addSeparators()
            val profit = format(lootProfit[item] ?: 0.0)
            addAsSingletonList("§b$count ${item.displayName} §7(§6$profit§7)")
        }
        addAsSingletonList(" ")
        addAsSingletonList(
            "§b${totalEnderArmor.addSeparators()} §5Ender Armor " +
                    "§7(§6${format(totalEnderArmor * 10_000)}§7)"
        )
        for (item in EnderNode.entries.subList(11, 16)) {
            val count = (storage.lootCount[item] ?: 0).addSeparators()
            val profit = format(lootProfit[item] ?: 0.0)
            addAsSingletonList("§b$count ${item.displayName} §7(§6$profit§7)")
        }
        // enderman pet rarities
        val (c, u, r, e, l) = EnderNode.entries.subList(16, 21).map { (storage.lootCount[it] ?: 0).addSeparators() }
        val profit = format(EnderNode.entries.subList(16, 21).sumOf { lootProfit[it] ?: 0.0 })
        addAsSingletonList("§f$c§7-§a$u§7-§9$r§7-§5$e§7-§6$l §fEnderman Pet §7(§6$profit§7)")
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        if (!ProfileStorageData.loaded) return emptyList()

        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat.get()) {
            newList.add(map[index])
        }
        return newList
    }
}