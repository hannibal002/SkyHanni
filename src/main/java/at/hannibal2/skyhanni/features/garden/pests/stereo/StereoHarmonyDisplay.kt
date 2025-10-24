package at.hannibal2.skyhanni.features.garden.pests.stereo

import at.hannibal2.skyhanni.api.event.HandleEvent
import at.hannibal2.skyhanni.data.ProfileStorageData
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
import net.minecraft.item.ItemStack

@SkyHanniModule
object StereoHarmonyDisplay {

    private val config get() = PestApi.config.stereoHarmony

    var activeVinyl: VinylType
        get() = ProfileStorageData.profileSpecific?.garden?.activeVinyl ?: VinylType.NONE
        private set(type) {
            ProfileStorageData.profileSpecific?.garden?.activeVinyl = type
            update()
        }

    private fun VinylType.getPest() = PestType.filterableEntries.find { it.vinyl == this }

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

    private fun drawDisplay() = buildList {
        val pest = activeVinyl.getPest()

        if (config.showHead.get()) {
            val itemScale = 1.67
            add(pest?.internalName?.let { Renderable.item(it, itemScale) } ?: Renderable.item(questionMarkSkull))
        }
        val displayList = buildList {
            val vinylName = activeVinyl.displayName
            val pestName = pest?.displayName ?: "None"
            addString("§ePlaying: §a$vinylName")
            val pestLine = buildList {
                addString("§ePest: §c$pestName ")
                if (pest?.crop != null && config.showCrop.get()) addItemStack(pest.crop.icon)
            }
            add(Renderable.horizontal(pestLine))
        }
        add(Renderable.vertical(displayList, verticalAlign = RenderUtils.VerticalAlignment.CENTER))
    }

    private fun updateActiveVinyl(stack: ItemStack) =
        PestApi.stereoPlayingPattern.firstMatcher(stack.getLore()) {
            val vinyl = group("vinyl").trim()
            activeVinyl = VinylType.getByNameOrNull(vinyl) ?: error("Unknown active vinyl: \"$vinyl\"")
        }

    // NOTE: Do not mark this as Garden only, it is possible to change the active vinyl outside the Garden
    @HandleEvent(onlyOnSkyblock = true)
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!PestApi.stereoInventory.isInside()) return
        event.inventoryItemsWithNull[4]?.let { updateActiveVinyl(it) }
    }

    // NOTE: Do not mark this as Garden only, it is possible to change the active vinyl outside the Garden
    @HandleEvent(onlyOnSkyblock = true)
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        val stack = event.itemStack
        if (stack.getItemCategoryOrNull() != ItemCategory.VACUUM) return
        updateActiveVinyl(stack)
    }

    @HandleEvent(GuiRenderEvent.GuiOverlayRenderEvent::class)
    fun onRenderOverlay() {
        if (!isEnabled()) return
        if (!GardenApi.isCurrentlyFarming() && !config.alwaysShow) return

        if (activeVinyl == VinylType.NONE && config.hideWhenNone) return
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
