package at.hannibal2.skyhanni.features.event.chocolatefactory

import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

class HoppityCollectionStats {

    private val config get() = ChocolateFactoryApi.config

    private val pagePattern by ChocolateFactoryApi.patternGroup.pattern(
        "page.current",
        "\\((?<page>\\d+)/(?<maxPage>\\d+)\\) Hoppity's Collection"
    )
    private val rabbitRarityPattern by ChocolateFactoryApi.patternGroup.pattern(
        "rabbit.rarity",
        "§.§L(?<rarity>\\w+) RABBIT"
    )

    private var display = emptyList<Renderable>()
    private val loggedRabbits = mutableMapOf<Int, List<RabbitCollectionInfo>>()
    private var inInventory = false
    private var currentPage = 0

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!isEnabled()) return
        if (!pagePattern.matches(event.inventoryName)) return

        inInventory = true


    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inInventory = false
    }

    @SubscribeEvent
    fun onProfileChange(event: ProfileJoinEvent) {
        display = emptyList()

        // todo clear the data

        currentPage = 0
        inInventory = false
    }

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.hoppityCollectionStats

    private data class RabbitCollectionInfo(
        var displayName: String = "",
        val rarity: RabbitCollectionRarity,
        val found: Boolean,
        val duplicates: Int
    )

    private enum class RabbitCollectionRarity(
        val displayName: String,
        val chocolatePerSecond: Int,
        val chocolateMultiplier: Double
    ) {
        COMMON("§fCommon", 1, 0.002),
        UNCOMMON("§aUncommon", 2, 0.003),
        RARE("§9Rare", 4, 0.004),
        EPIC("§5Epic", 10, 0.005),
        LEGENDARY("§6Legendary", 0, 0.02),
        MYTHIC("§dMythic", 0, 0.0),
        ;

        companion object {
            fun fromDisplayName(displayName: String) = entries.firstOrNull { it.name == displayName }
        }
    }
}
