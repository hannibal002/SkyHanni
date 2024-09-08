package at.hannibal2.skyhanni.features.misc.compacttablist

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage.TabList
import at.hannibal2.skyhanni.config.storage.PlayerSpecificStorage.TabList.TabProfile
import at.hannibal2.skyhanni.data.ProfileStorageData
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryUpdatedEvent
import at.hannibal2.skyhanni.events.LorenzToolTipEvent
import at.hannibal2.skyhanni.events.MessageSendToServerEvent
import at.hannibal2.skyhanni.features.dungeon.DungeonAPI
import at.hannibal2.skyhanni.features.misc.TabWidgetSettings
import at.hannibal2.skyhanni.skyhannimodule.SkyHanniModule
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.ChatUtils.isCommand
import at.hannibal2.skyhanni.utils.ChatUtils.senderIsSkyhanni
import at.hannibal2.skyhanni.utils.CollectionUtils.toSingletonListOrEmpty
import at.hannibal2.skyhanni.utils.HypixelCommands
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RegexUtils.matches
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.SimpleTimeMark
import at.hannibal2.skyhanni.utils.renderables.DragItem
import at.hannibal2.skyhanni.utils.renderables.DragNDrop
import at.hannibal2.skyhanni.utils.renderables.Droppable
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderXYAligned
import at.hannibal2.skyhanni.utils.renderables.RenderableUtils.renderYAligned
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.time.Duration.Companion.milliseconds

@SkyHanniModule
object TabEditor {

    private val compactConfig get() = SkyHanniMod.feature.gui.compactTabList

    val storage: TabList get() = ProfileStorageData.playerSpecific?.tabList!!
    fun isEnabled() = LorenzUtils.inSkyBlock && !DungeonAPI.inDungeon()

    val globalProfile by lazy {
        storage.globaleTabProfile!!
    }

    private lateinit var activeStorage: TabProfile

    private var inEditor = false

    private fun updateStorage() {
        if (inEditor) return
        activeStorage = storage.tabSetting[LorenzUtils.skyBlockIsland] ?: run {
            val new = TabProfile()
            storage.tabSetting[LorenzUtils.skyBlockIsland] = new
            new
        }
        inEditor = true
    }

    @SubscribeEvent
    fun onMessageToServer(event: MessageSendToServerEvent) {
        if (!LorenzUtils.inSkyBlock) return
        if (!event.isCommand("/widget")) return
        if (event.senderIsSkyhanni()) return
        if (DungeonAPI.inDungeon()) {
            event.cancel()
            ChatUtils.clickableChat("Tab Editor not available in Dungeon", { HypixelCommands.widget() }, "§eOpen widget anyway.")
            return
        }
        updateStorage()
    }

    private var lastClicked = SimpleTimeMark.farPast()

    private fun createItem(stack: ItemStack?, slotId: Int, windowId: Int) = if (stack == null) Renderable.placeholder(
        0,
        0,
    ) else Renderable.multiClickAndHover(
        Renderable.drawInsideRoundedRect(
            Renderable.itemStack(stack),
            (TabWidgetSettings.highlights[slotId] ?: LorenzColor.GRAY).toColor(), // TODO
        ),
        toolTips[slotId]?.let { listOf(stack.displayName) + it } ?: listOf("NULL"),
        bypassChecks = true,
        click = mapOf(
            0 to {
                DragNDrop.setDrag(TabItem(stack, slotId))
                /* if (lastClicked.passedSince() > 500.milliseconds) {
                    Minecraft.getMinecraft().playerController.windowClick(
                        windowId, slot.slotIndex, 0, 0, Minecraft.getMinecraft().thePlayer,
                    )
                    lastClicked = SimpleTimeMark.now()
                } */
            },
            1 to {
                if (lastClicked.passedSince() > 500.milliseconds) {
                    Minecraft.getMinecraft().playerController.windowClick(
                        windowId, slotId, 1, 0, Minecraft.getMinecraft().thePlayer,
                    )
                    lastClicked = SimpleTimeMark.now()
                }
            },
            2 to {
                if (lastClicked.passedSince() > 500.milliseconds) {
                    Minecraft.getMinecraft().playerController.windowClick(
                        windowId, slotId, 0, 0, Minecraft.getMinecraft().thePlayer,
                    )
                    lastClicked = SimpleTimeMark.now()
                }
            },
        ),
    )

    private fun createItem(slot: Slot?, windowId: Int) = createItem(slot?.stack, slot?.slotNumber ?: 0, windowId)

    private var mainInv = mutableMapOf<Int, ItemStack>()
    private var secondInv = mutableMapOf<Int, ItemStack>()
    private var lastNameOfSecond = ""

    @SubscribeEvent
    fun onInventoryUpdated(event: InventoryUpdatedEvent) {
        if (!inEditor) return
        val cache = when {
            TabWidgetSettings.mainPageSettingPattern.matches(event.inventoryName) -> mainInv
            TabWidgetSettings.shownSettingPattern.matches(event.inventoryName) -> secondInv
            else -> return
        }
        if (cache === secondInv && event.inventoryName != lastNameOfSecond) {
            secondInv.clear()
        }
        for ((slotID, item) in event.inventoryItemsWithNull) {
            if (item == null) continue
            cache[slotID] = item
        }
    }

    private class TabTarget() : Droppable {
        override fun handle(drop: Any?) {
            val item = drop as TabItem
            if (lastClicked.passedSince() < 500.milliseconds) return
            InventoryUtils.clickSlot(item.slotId)
            lastClicked = SimpleTimeMark.now()
        }

        override fun validTarget(item: Any?): Boolean = item is TabItem
    }

    private class TabItem(val stack: ItemStack, val slotId: Int) : DragItem<TabItem> {
        override fun get(): TabItem = this

        override fun onRender(mouseX: Int, mouseY: Int) {
            Renderable.itemStack(stack).render(0, 0)
        }

    }

    @SubscribeEvent
    fun onDraw(event: GuiContainerEvent.PreDraw) {
        if (!inEditor) return
        val container = event.container
        val windowId = container.windowId
        mainInv.forEach { (slotNumber, stack) ->
            toolTips[slotNumber] = stack.getLore()
        }

        GlStateManager.pushMatrix()
        val pre = RenderUtils.absoluteTranslation
        GlStateManager.loadIdentity()
        val height = event.gui.height
        val width = event.gui.width
        val zLevel = pre.third + 270f
        GlStateManager.translate(0f, 0f, zLevel)
        event.drawDefaultBackground()
        Renderable.withMousePosition(event.mouseX, event.mouseY) {
            Renderable.horizontalContainer(
                createItem(mainInv[13], 13, windowId).toSingletonListOrEmpty(),
                verticalAlign = RenderUtils.VerticalAlignment.CENTER,
            ).renderYAligned(0, 0, height)

            Renderable.horizontalContainer(
                (19..39).mapNotNull {
                    val stack = mainInv[it] ?: return@mapNotNull null
                    if (stack.item == ItemBlock.getItemFromBlock(Blocks.stained_glass_pane)) return@mapNotNull null
                    it to stack
                }.sortedBy { it.second.cleanName().drop(2) }.map { createItem(it.second, it.first, windowId) },
                horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
                verticalAlign = RenderUtils.VerticalAlignment.BOTTOM,
            ).renderXYAligned(0, 0, width, height)

            drawTabList(width, height)
        }
        GlStateManager.popMatrix()
        event.cancel()
    }

    private val toolTips = mutableMapOf<Int, List<String>>()

    @SubscribeEvent
    fun onToolTip(event: LorenzToolTipEvent) {
        if (!inEditor) return
        //event.toolTip.clear() // TODO
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        if (event.reopenSameName) return
        //inEditor = false // TODO
    }

    private val hypixelTabDisplay
        get() = listOf(
            toolTips[3] ?: emptyList(), toolTips[4] ?: emptyList(), toolTips[5] ?: emptyList(),
        )

    private fun drawTabList(width: Int, height: Int) = if (compactConfig.enabled.get()) {
        TabListRenderer.drawTabList()
        Unit
    } else {
        Renderable.verticalContainer(
            listOf(
                Renderable.placeholder(0, 15),
                Renderable.drawInsideRoundedRect(
                    Renderable.horizontalContainer(
                        hypixelTabDisplay.map {
                            Renderable.verticalContainer(it.map { Renderable.string(it) })
                        },
                    ),
                    LorenzColor.DARK_GRAY.toColor(), radius = 0,
                ),
            ),
            horizontalAlign = RenderUtils.HorizontalAlignment.CENTER,
            verticalAlign = RenderUtils.VerticalAlignment.TOP,
        ).renderXYAligned(0, 0, width, height)
        Unit
    }
}
