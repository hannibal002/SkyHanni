package at.hannibal2.skyhanni.features.inventory

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.api.ReforgeAPI
import at.hannibal2.skyhanni.data.model.SkyblockStat
import at.hannibal2.skyhanni.data.model.SkyblockStatList
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.cleanName
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.NumberUtil.toStringWithPlus
import at.hannibal2.skyhanni.utils.RenderUtils
import at.hannibal2.skyhanni.utils.RenderUtils.highlight
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.TimeUtils.ticks
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Items
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.Color
import java.util.concurrent.atomic.AtomicBoolean
import at.hannibal2.skyhanni.utils.renderables.Renderable.Companion.string as rString

class ReforgeHelper {

    private val config get() = SkyHanniMod.feature.inventory.helper.reforge

    private val repoGroup = RepoPattern.group("reforge")

    private val reforgeMenu by repoGroup.pattern(
        "menu.blacksmith",
        "Reforge Item"
    )
    private val reforgeHexMenu by repoGroup.pattern(
        "menu.hex",
        "The Hex ➜ Reforges"
    )
    private val reforgeChatMessage by repoGroup.pattern(
        "chat.success",
        "§aYou reforged your .* §r§ainto a .*!|§aYou applied a .* §r§ato your .*!"
    )
    private val reforgeChatFail by repoGroup.pattern(
        "chat.fail",
        "§cWait a moment before reforging again!|§cWhoa! Slow down there!"
    )

    private var isInReforgeMenu = false
    private var isInHexReforgeMenu = false

    private fun isReforgeMenu(chestName: String) = reforgeMenu.matches(chestName)
    private fun isHexReforgeMenu(chestName: String) = reforgeHexMenu.matches(chestName)

    private fun isEnabled() = LorenzUtils.inSkyBlock && config.enable && isInReforgeMenu

    private var itemToReforge: ItemStack? = null
    private var inventoryContainer: Container? = null

    private var currentReforge: ReforgeAPI.Reforge? = null
        set(value) {
            field = value
            formattedCurrentReforge = if (value == null) "" else "§7Now:  §3${value.name}"
        }
    private var reforgeToSearch: ReforgeAPI.Reforge? = null
        set(value) {
            field = value
            formattedReforgeToSearch = if (value == null) "" else "§7Goal: §9${value.name}"
        }

    private var hoverdReforge: ReforgeAPI.Reforge? = null

    private var formattedCurrentReforge = ""
    private var formattedReforgeToSearch = ""

    private val reforgeItem get() = if (isInHexReforgeMenu) 19 else 13
    private val reforgeButton get() = if (isInHexReforgeMenu) 48 else 22

    private val hexReforgeNextDownButton = 35
    private val hexReforgeNextUpButton = 17

    private val exitButton = 40

    private var waitForChat = AtomicBoolean(false)

    /** Gatekeeps instant double switches of the state */
    private var waitDelay = false

    private var sortAfter: SkyblockStat? = null

    private var display: List<Renderable> = generateDisplay()

    private val hoverColor = LorenzColor.GOLD.addOpacity(50)
    private val selectedColor = LorenzColor.BLUE.addOpacity(100)
    private val finishedColor = LorenzColor.GREEN.addOpacity(75)

    private fun itemUpdate() {
        val newItem = inventoryContainer?.getSlot(reforgeItem)?.stack
        if (newItem?.getInternalName() != itemToReforge?.getInternalName()) {
            reforgeToSearch = null
        }
        itemToReforge = newItem
        val newReforgeName = itemToReforge?.getReforgeName() ?: ""
        if (newReforgeName == currentReforge?.lowercaseName) return
        currentReforge = ReforgeAPI.reforgeList.firstOrNull { it.lowercaseName == newReforgeName }
        updateDisplay()
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!isEnabled()) return
        if (event.slot?.slotNumber == reforgeButton) {
            if (event.slot.stack?.name == "§eReforge Item" || event.slot.stack?.name == "§cError!") return
            if (currentReforge == reforgeToSearch) {
                event.isCanceled = true
                waitForChat.set(false)
            } else if (waitForChat.get()) {
                waitDelay = true
                event.isCanceled = true
            } else {
                if (event.clickedButton == 2) return
                if (waitDelay) {
                    waitDelay = false
                } else {
                    waitForChat.set(true)
                }
            }
        }

        DelayedRun.runNextTick {
            itemUpdate()
        }
    }

    @SubscribeEvent
    fun onChat(event: LorenzChatEvent) {
        if (!isEnabled()) return
        when {
            reforgeChatMessage.matches(event.message) -> {
                DelayedRun.runDelayed(2.ticks) {
                    itemUpdate()
                    waitForChat.set(false)
                }
                if (config?.hideChat == true) {
                    event.blockedReason = "reforge_hide"
                }
            }

            reforgeChatFail.matches(event.message) -> {
                DelayedRun.runDelayed(2.ticks) {
                    waitForChat.set(false)
                }
                if (config?.hideChat == true) {
                    event.blockedReason = "reforge_hide"
                }
            }
        }
    }

    @SubscribeEvent
    fun onOpen(event: InventoryFullyOpenedEvent) {
        if (!LorenzUtils.inSkyBlock) return
        when {
            isHexReforgeMenu(event.inventoryName) -> {
                isInHexReforgeMenu = true
                DelayedRun.runDelayed(2.ticks) {
                    itemUpdate() // update since an item must already be in place
                }
            }

            isReforgeMenu(event.inventoryName) -> {
                itemToReforge = null
                currentReforge = null
            }

            else -> return
        }
        isInReforgeMenu = true
        waitForChat.set(false)
        DelayedRun.runNextTick {
            inventoryContainer = Minecraft.getMinecraft().thePlayer.openContainer
        }
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        if (!isEnabled()) return
        isInReforgeMenu = false
        isInHexReforgeMenu = false
        reforgeToSearch = null
        currentReforge = null
        hoverdReforge = null
        sortAfter = null
        itemToReforge = null
        updateDisplay()
    }

    private fun updateDisplay() {
        display = generateDisplay()
    }

    private fun generateDisplay() = buildList<Renderable> {
        this.add(rString("§6Reforge Overlay"))

        val item = itemToReforge ?: run {
            reforgeToSearch = null
            return@buildList
        }

        val internalName = item.getInternalName()
        val itemType = item.getItemCategoryOrNull()
        val itemRarity = item.getItemRarityOrNull() ?: return@buildList

        val rawReforgeList =
            if (!isInHexReforgeMenu && config.reforgeStonesOnlyHex) ReforgeAPI.nonePowerStoneReforge else ReforgeAPI.reforgeList
        val reforgeList = rawReforgeList.filter { it.isValid(itemType, internalName) }

        val statTypes = reforgeList.mapNotNull { it.stats[itemRarity]?.keys }.flatten().toSet()

        val statTypeButtons = (listOf(getStatButton(null)) + statTypes.map { getStatButton(it) }).chunked(9)
        this.add(Renderable.table(statTypeButtons, xPadding = 3, yPadding = 2))

        val list = reforgeList.sortedBy(getSortSelector(itemRarity, sortAfter)).map(getReforgeView(itemRarity))
        this.addAll(list)
    }

    private fun getReforgeView(itemRarity: LorenzRarity): (ReforgeAPI.Reforge) -> Renderable = { reforge ->
        val text = (if (reforge.isReforgeStone) "§9" else "§7") + reforge.name
        val tips = run {
            val pre: List<Renderable>
            val stats: List<Renderable>
            val removedEffect: List<Renderable>
            val addEffectText: String
            if (currentReforge == reforge) {
                pre = listOf(rString("§3Reforge is currently applied!"))
                stats = (currentReforge?.stats?.get(itemRarity)?.print() ?: emptyList())
                removedEffect = emptyList()
                addEffectText = "§aEffect:"
            } else {
                pre = listOf(rString("§6Reforge Stats"))
                stats = (reforge.stats[itemRarity]?.print(currentReforge?.stats?.get(itemRarity)) ?: emptyList())
                removedEffect = getReforgeEffect(
                    currentReforge,
                    itemRarity
                )?.let { listOf(rString("§cRemoves Effect:")) + it }
                    ?: emptyList()
                addEffectText = "§aAdds Effect:"
            }

            val addedEffect =
                (getReforgeEffect(reforge, itemRarity)?.let { listOf(rString(addEffectText)) + it }
                    ?: emptyList())

            return@run pre + stats + removedEffect + addedEffect
        }
        val onHover = if (!isInHexReforgeMenu) {
            {}
        } else {
            { hoverdReforge = reforge }
        }

        Renderable.clickAndHover(
            text, tips, onClick = { reforgeToSearch = reforge }, onHover = onHover
        )
    }

    private fun getReforgeEffect(reforge: ReforgeAPI.Reforge?, rarity: LorenzRarity) =
        reforge?.extraProperty?.get(rarity)?.let {
            Renderable.wrappedString(
                it,
                190,
                color = LorenzColor.GRAY.toColor()
            )
        }

    private fun getSortSelector(
        itemRarity: LorenzRarity,
        sorting: SkyblockStat?,
    ): (ReforgeAPI.Reforge) -> Comparable<Any?> =
        if (sorting != null) {
            { -(it.stats[itemRarity]?.get(sorting) ?: 0.0) as Comparable<Any?> }
        } else {
            { (it.isReforgeStone) as Comparable<Any?> }
        }

    private fun getStatButton(stat: SkyblockStat?): Renderable {
        val icon: String
        val tip: String
        if (stat == null) {
            icon = "§7D"
            tip = "§7Default"
        } else {
            icon = stat.icon
            tip = stat.iconWithName
        }

        val fieldColor = if (sortAfter == stat) LorenzColor.GRAY else LorenzColor.DARK_GRAY

        val sortField =
            Renderable.drawInsideRoundedRect(
                Renderable.hoverTips(
                    Renderable.fixedSizeLine(
                        rString(icon, horizontalAlign = RenderUtils.HorizontalAlignment.CENTER),
                        SkyblockStat.fontSizeOfLargestIcon
                    ), listOf("§6Sort after", tip)
                ), fieldColor.toColor(), radius = 15, padding = 1
            )
        return if (sortAfter == stat) {
            sortField
        } else {
            Renderable.clickable(sortField, {
                sortAfter = stat
                updateDisplay()
            })
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!isEnabled()) return
        config.posList.renderRenderables(display, posLabel = "Reforge Overlay")
    }

    @SubscribeEvent
    fun onGuiContainerForegroundDrawn(event: GuiContainerEvent.AfterDraw) {
        if (!isEnabled()) return
        GlStateManager.translate(0f, 0f, 10f)
        val position = if (isInHexReforgeMenu) config.posCurrentHex else config.posCurrent
        position.renderStrings(
            listOf(formattedReforgeToSearch, formattedCurrentReforge),
            posLabel = "Reforge Notify"
        )
        GlStateManager.translate(0f, 0f, -10f)
    }

    @SubscribeEvent
    fun onGuiContainerBackgroundDrawn(event: GuiContainerEvent.BackgroundDrawnEvent) {
        if (hoverdReforge != null && isInHexReforgeMenu) {
            if (hoverdReforge != currentReforge) {
                colorReforgeStone(hoverColor, hoverdReforge?.rawReforgeStoneName ?: "Random Basic Reforge")
            } else {
                inventoryContainer?.getSlot(reforgeItem)?.highlight(hoverColor)

                //?.get(reforgeItem)?. = hoverColor
            }
            hoverdReforge = null
        }

        if (reforgeToSearch == null) return
        if (reforgeToSearch != currentReforge) {
            colorSelected()
        } else {
            inventoryContainer?.getSlot(reforgeItem)?.highlight(finishedColor)
        }
    }

    private fun colorSelected() = if (reforgeToSearch?.isReforgeStone == true) {
        if (isInHexReforgeMenu) {
            colorReforgeStone(selectedColor, reforgeToSearch?.rawReforgeStoneName)
        } else {
            inventoryContainer?.getSlot(exitButton)?.highlight(selectedColor)
        }
    } else {
        inventoryContainer?.getSlot(reforgeButton)?.highlight(selectedColor)
    }

    private fun colorReforgeStone(color: Color, reforgeStone: String?) {
        val inventory = inventoryContainer?.inventorySlots ?: return
        val slot = inventory.firstOrNull { it?.stack?.cleanName() == reforgeStone }
        if (slot != null) {
            slot highlight color
        } else {
            inventory[hexReforgeNextDownButton]?.takeIf { it.stack.item == Items.skull }?.highlight(color)
            inventory[hexReforgeNextUpButton]?.takeIf { it.stack.item == Items.skull }?.highlight(color)
        }
    }

    private fun SkyblockStatList.print(appliedReforge: SkyblockStatList? = null): List<Renderable> {
        val diff = appliedReforge?.let { this - it }
        val main = ((diff ?: this).mapNotNull {
            val key = it.key
            val value = this[key] ?: return@mapNotNull null
            buildList<Renderable> {
                add(rString("§9${value.toStringWithPlus()}"))
                diff?.get(key)?.let { add(rString((if (it < 0) "§c" else "§a") + it.toStringWithPlus())) }
                add(rString(key.iconWithName))
            }
        })
        val table = Renderable.table(main, 5)
        return listOf(table)
    }
}

