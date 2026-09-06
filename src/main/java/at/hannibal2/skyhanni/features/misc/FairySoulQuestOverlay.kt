package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.RenderInventoryItemTipEvent
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getCleanLore
import at.hannibal2.skyhanni.utils.NumberUtil.formatLong
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkyBlockUtils
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object FairySoulQuestOverlay {
    val config get() = SkyHanniMod.feature.misc.fairySouls

    private var inInventory = false

    private var islandlist = mutableListOf<String>()
    private var displayList = emptyList<Renderable>()

    private var remainingMap = mutableMapOf<Int, Pair<Long, Long>>() // islandname -> (soulsfound, soulstotal)

    private val groupPattern = RepoPattern.group("misc.fairysoulquestoverlay")

    /**
     * REGEX-TEST: Fairy Souls: 1/5
     * REGEX-TEST: Fairy Souls: 11/11
     * REGEX-TEST: Fairy Souls: 0/8
     */
    private val visitedPattern by groupPattern.pattern(
        "souls.islandname",
        "Fairy Souls: (?<soulsfound>[0-9]+)/(?<soulstotal>[0-9]+)",
    )

    @HandleEvent(onlyOnSkyblock = true)
    private fun onInventoryFullyOpened(event: InventoryFullyOpenedEvent) {
        inInventory = event.inventoryName == "Fairy Souls Guide"

        for ((slot, item) in event.inventoryItems) {
            var soulstotal = 0L
            var soulsfound = 0L
            var soulsremaining = 0L
            val lore = item.getCleanLore()
            val island = item.cleanName

            visitedPattern.firstMatcher(lore) {
                soulsfound = group("soulsfound").formatLong()
                soulstotal = group("soulstotal").formatLong()
                soulsremaining = soulstotal - soulsfound

                remainingMap.put(slot, Pair(soulsfound, soulstotal))

                if (soulsremaining > 0) {
                    islandlist.add("§2$island§7: §e$soulsfound§7/§d$soulstotal")
                }
            }
        }
    }

    @HandleEvent
    private fun onRenderItemTip(event: RenderInventoryItemTipEvent) {
        if (!(inInventory && config.fairySoulStackSize)) return
        if (!(event.slot.index in remainingMap)) return
        event.stackTip = remainingMap[event.slot.index]?.let { (soulsfound, soulstotal) ->
            if ((soulstotal - soulsfound) > 0) {
                "§e${soulstotal - soulsfound}"
            } else ""
        } ?: return
    }

    @HandleEvent
    private fun onInventoryClose() {
        inInventory = false
        islandlist = mutableListOf()
    }

    @HandleEvent
    private fun onChestGuiRender() {
        if (!SkyBlockUtils.onHypixel) return
        if (!(inInventory && config.fairySoulOverlay)) return

        displayList = buildList {
            for (island in islandlist) {
                addString(island)
            }
        }

        if (true) {
            config.pos.renderRenderables(
                displayList,
                extraSpace = 1,
                posLabel = "Fairy Soul Quest Overlay",
            )
        }
    }
}
