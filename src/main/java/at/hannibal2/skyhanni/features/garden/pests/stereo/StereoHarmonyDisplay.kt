package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.events.ConfigLoadEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.features.garden.GardenApi
import at.hannibal2.skyhanni.features.garden.pests.PestApi
import at.hannibal2.skyhanni.features.garden.pests.PestType
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ConditionalUtils
import at.hannibal2.skyhanni.utils.ItemCategory
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.RegexUtils.firstMatcher
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.SkullTextureHolder
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addItemStack
import at.hannibal2.skyhanni.utils.collection.RenderableCollectionUtils.addString
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.container.HorizontalContainerRenderable.Companion.horizontal
import at.hannibal2.skyhanni.utils.renderables.container.VerticalContainerRenderable.Companion.vertical
import at.hannibal2.skyhanni.utils.renderables.primitives.ItemStackRenderable.Companion.item
import at.hannibal2.skyhanni.utils.renderables.primitives.StringRenderable
import net.minecraft.world.item.ItemStack

@SkyHanniModule
object StereoHarmonyDisplay {

    private val config get() = PestApi.config.stereoHarmony

    private val gardenStorage get() = GardenApi.storage

    private fun VinylType.getPest(): PestType =
        PestType.filterableEntries.find { it.vinyl == this } ?: error("no PestType for VinylType $this")

    private var display = emptyList<Renderable>()

    private val questionMarkSkull by lazy {
        ItemUtils.createSkull(
            displayName = "§c?",
            uuid = "28aa984a-2077-40cc-8de7-e641adf2c497",
            value = SkullTextureHolder.getTexture("QUESTION_MARK"),
        )
    }

    private fun update() {
        display = drawDisplay()
    }

    // TODO cleanup: we don't want three nested buildList calls
    private fun drawDisplay() = buildList {
        val vinyl = gardenStorage?.activeVinyl ?: run {
            add(Renderable.item(questionMarkSkull))
            add(Renderable.vertical(listOf(StringRenderable("§ePlaying: §7Nothing")), verticalAlign = RenderUtils.VerticalAlignment.CENTER))
            return@buildList
        }
        val pest = vinyl.getPest()
        if (config.showHead.get()) {
            add(Renderable.item(pest.internalName, scale = 1.67))
        }
        val displayList = buildList {
            addString("§ePlaying: §a${vinyl.displayName}")
            val pestLine = buildList {
                addString("§ePest: §c${pest.displayName} ")
                pest.crop?.let {
                    if (config.showCrop.get()) addItemStack(it.icon)
                }
            }
            add(Renderable.horizontal(pestLine))
        }
        add(Renderable.vertical(displayList, verticalAlign = RenderUtils.VerticalAlignment.CENTER))
    }

    private fun updateActiveVinyl(stack: ItemStack?) {
        PestApi.stereoPlayingPattern.firstMatcher(stack?.getLore() ?: return) {
            gardenStorage?.activeVinyl = VinylType.getByName(group("vinyl").trim()).takeIf { it != VinylType.NONE }
            update()
        }
    }

    // NOTE: Do not mark this as Garden only, it is possible to change the active vinyl outside the Garden
    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (PestApi.stereoInventory.isInside()) {
            updateActiveVinyl(event.inventoryItemsWithNull[4])
        }
    }

    // NOTE: Do not mark this as Garden only, it is possible to change the active vinyl outside the Garden
    @HandleEvent(onlyOnSkyblock = true)
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (event.itemStack.getItemCategoryOrNull() == ItemCategory.VACUUM) {
            updateActiveVinyl(event.itemStack)
        }
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        if (!GardenApi.isCurrentlyFarming() && !config.alwaysShow) return

        if (gardenStorage?.activeVinyl == null && config.hideWhenNone) return
        else if (display.isEmpty()) update()
        if (display.isEmpty()) return
        val content = Renderable.horizontal(display, 1, verticalAlign = RenderUtils.VerticalAlignment.CENTER)
        val renderables = listOf(content)
        config.position.renderRenderables(renderables, posLabel = "Stereo Harmony Display")
    }

    @HandleEvent
    fun onWorldChange() {
        display = emptyList()
    }

    @HandleEvent(ConfigLoadEvent::class)
    fun onConfigLoad() {
        ConditionalUtils.onToggle(config.showHead, config.showCrop) { update() }
    }

    fun isEnabled() = GardenApi.inGarden() && config.displayEnabled
}
