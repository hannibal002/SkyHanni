package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern

@SkyHanniModule
object GardenCharmedVisitors {

    private val config get() = VisitorApi.config.charmed
    private val storage get() = GardenApi.storage

    private val patternGroup = RepoPattern.group("garden.visitor.charmed")

    private const val VINYL_SLOT = 48

    /**
     * REGEX-TEST: This Visitor has been Charmed! ❤
     */
    private val charmedItemNamePattern by patternGroup.pattern(
        "name",
        "This Visitor has been Charmed!.*",
    )

    private var display = emptyList<Renderable>()
    private var openVisitor: VisitorApi.Visitor? = null

    @HandleEvent
    fun onVisitorOpen(event: VisitorOpenEvent) {
        val visitor = event.visitor
        openVisitor = visitor
        checkCharmed(visitor)
    }

    @HandleEvent(InventoryUpdatedEvent::class)
    fun onInventoryUpdate() {
        if (!VisitorApi.inInventory) return
        val visitor = openVisitor ?: return
        checkCharmed(visitor)
    }

    private fun checkCharmed(visitor: VisitorApi.Visitor) {
        val isCharmed = InventoryUtils.getItemAtSlotIndex(VINYL_SLOT)
            ?.let { charmedItemNamePattern.matches(it.hoverName) } == true

        if (isCharmed) {
            addCharmed(visitor)
        } else {
            removeCharmed(visitor)
        }
    }

    @HandleEvent
    fun onInventoryClose() {
        openVisitor = null
    }

    @HandleEvent
    fun onProfileJoin() {
        updateDisplay()
    }

    @HandleEvent(onlyOnIsland = IslandType.GARDEN)
    fun onGuiRender() {
        if (!config.enabled) return
        config.position.renderRenderables(display, posLabel = "Charmed Visitors")
    }

    private fun addCharmed(visitor: VisitorApi.Visitor) {
        val storage = storage ?: return
        val name = visitor.visitorName
        val changed = storage.charmedVisitors.add(name)
        if (!changed) return
        updateDisplay()
    }

    private fun removeCharmed(visitor: VisitorApi.Visitor) {
        val storage = storage ?: return
        val name = visitor.visitorName
        val changed = storage.charmedVisitors.remove(name)
        if (!changed) return
        updateDisplay()
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        val storage = storage ?: return@buildList
        val charmed = storage.charmedVisitors
        if (charmed.isEmpty()) return@buildList
        addString("§dCharmed Visitors §7(${charmed.size}):")
        for (name in charmed) {
            add(
                Renderable.clickable(
                    " §7- $name",
                    onLeftClick = {
                        storage.charmedVisitors.remove(name)
                        updateDisplay()
                    },
                    tips = listOf("§eClick to remove from charmed list"),
                ),
            )
        }
    }
}
