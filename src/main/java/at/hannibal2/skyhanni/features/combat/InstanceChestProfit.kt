package at.hannibal2.skyhanni.features.combat

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonApi
import at.hannibal2.skyhanni.features.misc.items.EstimatedItemValueCalculator
import at.hannibal2.skyhanni.features.nether.kuudra.KuudraApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemPriceUtils.formatCoin
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getPriceOrNull
import at.hannibal2.skyhanni.utils.ItemPriceUtils.getRawCraftCostOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.repoItemName
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.NeuInternalName
import at.hannibal2.skyhanni.utils.PetUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matchMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderable
import at.hannibal2.skyhanni.utils.collection.CollectionUtils.addOrPut
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.StringRenderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.item.ItemStack

@SkyHanniModule
object InstanceChestProfit {

    /**
     * REGEX-TEST: §6Kraken Shard §8x1
     */
    private val attributeShardPattern by RepoPattern.pattern(
        "combat.attributeshard",
        "§.(?<name>\\w+ Shard) §.x(?<count>\\d+)",
    )

    /**
     * REGEX-TEST: §dCrimson Essence §8x250
     */
    private val essencePattern by RepoPattern.pattern(
        "combat.essence",
        "§.(?<name>\\w+ Essence) §.x(?<count>\\d+)",
    )

    /**
     * REGEX-TEST: §6500, 000 Coins
     */
    private val coinsPattern by RepoPattern.pattern(
        "combat.coins",
        "§6(?<amount>.*) Coins",
    )

    /**
     * REGEX-TEST: §9Dungeon Chest Key
     */
    private val dungeonChestKey by RepoPattern.pattern(
        "combat.dungeonchestkey",
        "§9Dungeon Chest Key",
    )

    /**
     * REGEX-TEST: §6Infernal Kuudra Key
     */
    private val kuudraChestKey by RepoPattern.pattern(
        "combat.kuudrachestkey",
        "§.\\w+ Kuudra Key",
    )

    private val config get() = SkyHanniMod.feature.combat

    private var inDungeonChest = false
    private var inKuudraChest = false
    private var display: Renderable? = null

    @HandleEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!config.showInstanceChestProfit) return

        val name = event.inventoryName
        if (DungeonApi.DungeonChest.getByInventoryName(name) != null) {
            inDungeonChest = true
        } else if (KuudraApi.KuudraChest.getByInventoryName(name) != null) {
            inKuudraChest = true
        } else return

        createDisplay(event.inventoryItems)
    }

    @HandleEvent(InventoryCloseEvent::class)
    fun onInventoryClose() {
        inDungeonChest = false
        inKuudraChest = false
    }

    private fun createDisplay(items: Map<Int, ItemStack>) {
        val itemsWithCost: MutableMap<String, Double> = mutableMapOf()
        items.forEach {
            if (it.value.getInternalNameOrNull() != null) {
                val cost = EstimatedItemValueCalculator.getTotalPrice(it.value)
                if (cost != null) itemsWithCost.addOrPut(it.value.getInternalName().repoItemName, cost)
            }
            attributeShardPattern.matchMatcher(it.value.displayName) {
                val name = group("name")
                val count = group("count").toInt()
                val price = count * (NeuInternalName.fromItemName(name).getPriceOrNull() ?: 0.0)
                itemsWithCost.addOrPut(it.value.displayName, price)
            }
            essencePattern.matchMatcher(it.value.displayName) {
                val name = group("name")
                val rawCount = group("count").toInt()
                val count = if (name == "Crimson") rawCount * (1 + getKuudraEssenceBonus())
                else rawCount.toDouble()
                val price = count * (NeuInternalName.fromItemName(name).getPriceOrNull() ?: 0.0)
                itemsWithCost.addOrPut(it.value.displayName, price)
            }
        }

        // Slot 31 has the cost information for the chest
        items[31]?.getLore()?.forEach {
            coinsPattern.matchMatcher(it) {
                val amount = group("amount").replace(",", "").toInt()
                itemsWithCost.put(it, -amount.toDouble())
            }
            dungeonChestKey.matchMatcher(it) {
                val name = NeuInternalName.fromItemName(it)
                itemsWithCost.put(it, name.getPriceOrNull()?.times(-1) ?: 0.0)
            }
            kuudraChestKey.matchMatcher(it) {
                val name = NeuInternalName.fromItemName(it)
                itemsWithCost.put(it, name.getRawCraftCostOrNull()?.times(-1) ?: 0.0)
            }
        }

        val newDisplay = buildList {
            val chestName = if (inDungeonChest) "Dungeon"
            else if (inKuudraChest) "Kuudra"
            else ""
            add(listOf(StringRenderable("§d§l$chestName Chest Profit")))
            add(listOf(StringRenderable("")))

            var total = 0.0
            var displayedCost = false

            val revenue = itemsWithCost.values.filter { it > 0 }.sum()
            add(listOf(StringRenderable("§a§lTotal Revenue"), StringRenderable("§a${revenue.formatCoin()}")))

            itemsWithCost.forEach {
                val costColor = if (it.value < 0) "§c"
                else "§a"

                if (!displayedCost && it.value < 0) {
                    val cost = itemsWithCost.values.filter { cost -> cost < 0 }.sum()
                    add(listOf(StringRenderable("")))
                    add(listOf(StringRenderable("§c§lTotal Cost"), StringRenderable("§c${cost.formatCoin()}")))
                    displayedCost = true
                }

                val cost = "$costColor${it.value.formatCoin()}"

                total += it.value
                add(listOf(StringRenderable(it.key), StringRenderable(cost)))
            }

            val color = if (total < 0) "§c"
            else "§a"

            add(listOf(StringRenderable("")))
            add(listOf(StringRenderable("$color§lProfit"), StringRenderable("$color ${total.formatCoin()}")))
        }

        display = Renderable.table(newDisplay, yPadding = 1)
    }

    private fun getKuudraEssenceBonus(): Double {
        return ProfileStorageData.petProfiles?.pets?.filter { PetUtils.getPetProperName(it.fauxInternalName) == "KUUDRA" }
            ?.maxByOrNull { it.rarity.id }
            ?.let {
                when (it.rarity) {
                    LorenzRarity.RARE -> 0.15 / 100 * it.level
                    LorenzRarity.EPIC, LorenzRarity.LEGENDARY -> 0.2 / 100 * it.level
                    else -> 0.0
                }
            } ?: 0.0
    }

    @HandleEvent(GuiRenderEvent::class)
    fun onRenderOverlay() {
        if (!config.showInstanceChestProfit || (!inDungeonChest && !inKuudraChest)) return

        config.position.renderRenderable(display, posLabel = "Instance Chest Profit")
    }
}
