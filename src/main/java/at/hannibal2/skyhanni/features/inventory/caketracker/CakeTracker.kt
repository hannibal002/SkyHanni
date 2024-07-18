package at.hannibal2.skyhanni.features.inventory.caketracker

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.DrawScreenAfterEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.InventoryOpenEvent
import at.hannibal2.skyhanni.events.SecondPassedEvent
import at.hannibal2.skyhanni.features.inventory.caketracker.CakeTrackerConfig.CakeTrackerDisplayType
import at.hannibal2.skyhanni.features.inventory.patternGroup
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getInventoryName
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.SkyBlockTime
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.tracker.SkyHanniTracker
import at.hannibal2.skyhanni.utils.tracker.TrackerData
import com.google.gson.annotations.Expose
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.inventory.ContainerChest
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

@SkyHanniModule
object CakeTracker {

    fun getStorage() = ProfileStorageData.profileSpecific?.cakeTracker
    private val config get() = SkyHanniMod.feature.inventory.cakeTracker
    private var currentYear = SkyBlockTime.now().year

    /**
     * REGEX-TEST: §cNew Year Cake (Year 360)
     * REGEX-TEST: §cNew Year Cake (Year 1,000)
     */
    private val cakeNamePattern by patternGroup.pattern(
        "cake.name",
        "§cNew Year Cake \\(Year (?<year>[\\d,]*)\\)"
    )

    /**
     * REGEX-TEST: Ender Chest (2/9)
     * REGEX-TEST: Jumbo Backpack (Slot #6)
     * REGEX-TEST: New Year Cake Bag
     */
    private val cakeContainerPattern by patternGroup.pattern(
        "cake.container",
        "^(Ender Chest \\(\\d{1,2}/\\d{1,2}\\)|.*Backpack(?:§r)? \\(Slot #\\d{1,2}\\)|New Year Cake Bag)$"
    )

    /**
     * REGEX-TEST: New Year Cake Bag
     */
    private val cakeBagPattern by patternGroup.pattern(
        "cake.bag",
        "^New Year Cake Bag$"
    )

    private val tracker = SkyHanniTracker("New Year Cake Tracker", { Data() }, { it.cakeTracker })
    { drawDisplay(it) }

    class Data : TrackerData() {
        override fun reset(){
            cakesOwned.clear()
        }

        @Expose
        var cakesOwned: MutableList<Int> = mutableListOf()

        @Expose
        var cakesMissing: MutableList<Int> = mutableListOf()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!config.enabled) return
        val inventoryName = event.inventoryName
        if(!cakeContainerPattern.matches(inventoryName)) return;
        val items = event.inventoryItems.values.filter { cakeNamePattern.matches(it.displayName) }
        ChatUtils.chat("Cakes found: ${items.count()}")
    }

    @SubscribeEvent
    fun onSecondPassed(event: SecondPassedEvent) {
        if(!LorenzUtils.inSkyBlock) return
        if(!config.enabled) return
        checkYear()
    }

    private fun checkYear() {
        if(SkyBlockTime.now().year == currentYear) return
        val profileStorage = getStorage() ?: return
        currentYear = SkyBlockTime.now().year
        recalculateMissingCakes(profileStorage)
    }

    private fun recalculateMissingCakes(data: Data) {
        data.cakesMissing = (1..currentYear).filterNot { data.cakesOwned.contains(it) }.toMutableList()
    }

    private fun cakeYearsToRenderable(start: Int, end: Int = 0): Renderable {
        return Renderable.clickAndHover(
            if (end != 0) "§cYears $start-$end" else "§cYear $start",
            listOf("Click to search /ah"),
            onClick = {
                HypixelCommands.ahs("New Year Cake (Year $start)")
            },
        )
    }

    private fun drawDisplay(data: Data): List<List<Any>> = buildList {
        addAsSingletonList(
            Renderable.hoverTips(
                "§f§lYear §c§lCake §f§lTracker",
                tips = listOf("§aHave§7: §a${data.cakesOwned.count()}§7, §cMissing§7: §c${data.cakesMissing.count()}")
            ),
        )

        if (data.cakesMissing.isEmpty()) {
            addAsSingletonList("§aAll cakes owned!")
        } else {
            val sortedMissingCakes = when (config.displayType) {
                CakeTrackerDisplayType.OLDEST_FIRST -> data.cakesMissing.sorted()
                CakeTrackerDisplayType.NEWEST_FIRST -> data.cakesMissing.sortedDescending()
                null -> data.cakesMissing
            }

            // Combine consecutive missing years into ranges
            val combinedMissingYears = mutableListOf<Renderable>()
            var start = sortedMissingCakes.first()
            var end = start

            for (i in 1 until sortedMissingCakes.size) {
                if (sortedMissingCakes[i] == end + 1) end = sortedMissingCakes[i]
                else {
                    if (start != end) combinedMissingYears.add(cakeYearsToRenderable(start, end))
                    else combinedMissingYears.add(cakeYearsToRenderable(start))
                    start = sortedMissingCakes[i]
                    end = start
                }
            }

            if (start != end) combinedMissingYears.add(cakeYearsToRenderable(start, end))
            else combinedMissingYears.add(cakeYearsToRenderable(start))

            add(combinedMissingYears)
        }
    }
}
