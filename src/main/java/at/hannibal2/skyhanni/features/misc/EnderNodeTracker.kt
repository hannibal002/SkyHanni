package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.PlaySoundEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi.Companion.getBazaarData
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.afterChange
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUItems.getNpcPriceOrNull
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.format
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class EnderNodeTracker {
    private val config get() = SkyHanniMod.feature.misc.enderNodeTracker

    private var totalNodesMined = 0
    private var totalEndermiteNests = 0
    private var totalEnderArmor = 0
    private var display = emptyList<List<Any>>()
    private var lootCount = mapOf<EnderNode, Int>()
    private var lootProfit = mapOf<EnderNode, Double>()

    private val enderNodeRegex = Regex("""ENDER NODE!.+You found (\d+x )?§r(.+)§r§f!""")
    private val endermanRegex = Regex("""(RARE|PET) DROP! §r(.+) §r§b\(""")

    private var lastEndermiteTime = 0L

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!ProfileStorageData.loaded) return
        if (!isInTheEnd()) return

        // don't call removeColor because we want to distinguish enderman pet rarity
        val message = event.message.trim()
        var item: String? = null
        var amount = 1

        // check whether the loot is from an ender node or an enderman
        enderNodeRegex.find(message)?.let {
            totalNodesMined++
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
                lastEndermiteTime = System.currentTimeMillis()
                totalEndermiteNests++
            }
        }

        // increment the count of the specific item found
        EnderNode.entries.find { it.displayName == item }?.let {
            val old = lootCount[it] ?: 0
            lootCount = lootCount.editCopy {
                this[it] = old + amount
            }
        }
        saveAndUpdate()
    }

    @SubscribeEvent
    fun onSoundPlay(event: PlaySoundEvent) {
        if (!isInTheEnd()) return
        if (event.soundName != "mob.silverfish.kill") return
        if (event.distanceToPlayer > 15) return
        if (System.currentTimeMillis() - lastEndermiteTime > 7500) return

        // listen for nearby endermite death sounds within 7.5s of mining an endermite nest
        // this is a fairly accurate approximation for mite gel drops
        val oldEndStone = lootCount[EnderNode.ENCHANTED_ENDSTONE] ?: 0
        val oldMiteGel = lootCount[EnderNode.MITE_GEL] ?: 0
        lootCount = lootCount.editCopy {
            this[EnderNode.ENCHANTED_ENDSTONE] = oldEndStone + (1 + Math.random() * 2).toInt()
            this[EnderNode.MITE_GEL] = oldMiteGel + (1 + Math.random() * 2).toInt()
        }
        saveAndUpdate()
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
            saveAndUpdate()
        }

        val hidden = ProfileStorageData.profileSpecific?.enderNodeTracker ?: return
        totalNodesMined = hidden.totalNodesMined
        totalEndermiteNests = hidden.totalEndermiteNests
        lootCount = hidden.lootCount
        totalEnderArmor = hidden.lootCount.filter { isEnderArmor(it.key.displayName) }.map { it.value }.sum()
        saveAndUpdate()
    }

    private fun calculateProfit(): Map<EnderNode, Double> {
        val newProfit = mutableMapOf<EnderNode, Double>()
        lootCount.forEach { (key, _) ->
            val price = if (isEnderArmor(key.displayName)) {
                10_000.0
            } else {
                val internalName = key.internalName
                val npcPrice = internalName.getNpcPriceOrNull()
                val bazaarData = internalName.getBazaarData()
                if (LorenzUtils.noTradeMode || bazaarData == null) {
                    npcPrice ?: georgePrice(key) ?: 0.0
                } else {
                    npcPrice
                        ?.coerceAtLeast(bazaarData.sellPrice)
                        ?.coerceAtLeast(georgePrice(key) ?: 0.0)
                        ?: internalName.getPrice()
                }
            }
            newProfit[key] = price * (lootCount[key] ?: 0)
        }
        return newProfit
    }

    private fun saveAndUpdate() {
        val hidden = ProfileStorageData.profileSpecific?.enderNodeTracker ?: return
        hidden.totalNodesMined = totalNodesMined
        hidden.totalEndermiteNests = totalEndermiteNests
        hidden.lootCount = lootCount

        lootProfit = calculateProfit()
        display = formatDisplay(drawDisplay())
    }

    private fun isInTheEnd() = LorenzUtils.skyBlockArea == "The End"

    private fun isEnderArmor(displayName: String?) = when (displayName) {
        "§5Ender Helmet",
        "§5Ender Chestplate",
        "§5Ender Leggings",
        "§5Ender Boots",
        "§5Ender Necklace",
        "§5Ender Gauntlet" -> true

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

    private fun drawDisplay() = buildList<List<Any>> {
        addAsSingletonList("§5§lEnder Node Tracker")
        addAsSingletonList("§d${totalNodesMined.addSeparators()} Ender Nodes mined")
        addAsSingletonList("§6${format(lootProfit.values.sum())} Coins made")
        addAsSingletonList(" ")
        addAsSingletonList("§b${totalEndermiteNests.addSeparators()} §cEndermite Nest")

        for (item in EnderNode.entries.subList(0, 11)) {
            val count = (lootCount[item] ?: 0).addSeparators()
            val profit = format(lootProfit[item] ?: 0.0)
            addAsSingletonList("§b$count ${item.displayName} §7(§6$profit§7)")
        }
        addAsSingletonList(" ")
        addAsSingletonList(
            "§b${totalEnderArmor.addSeparators()} §5Ender Armor " +
                    "§7(§6${format(totalEnderArmor * 10_000)}§7)"
        )
        for (item in EnderNode.entries.subList(11, 16)) {
            val count = (lootCount[item] ?: 0).addSeparators()
            val profit = format(lootProfit[item] ?: 0.0)
            addAsSingletonList("§b$count ${item.displayName} §7(§6$profit§7)")
        }
        // enderman pet rarities
        val (c, u, r, e, l) = EnderNode.entries.subList(16, 21).map { (lootCount[it] ?: 0).addSeparators() }
        val profit = format(EnderNode.entries.subList(16, 21).sumOf { lootProfit[it] ?: 0.0 })
        addAsSingletonList("§f$c§7-§a$u§7-§9$r§7-§5$e§7-§6$l §fEnderman Pet §7(§6$profit§7)")
    }

    private fun formatDisplay(map: List<List<Any>>): List<List<Any>> {
        val newList = mutableListOf<List<Any>>()
        for (index in config.textFormat.get()) {
            newList.add(map[index])
        }
        return newList
    }
}
