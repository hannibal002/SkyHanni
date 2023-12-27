package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.api.ReforgeAPI
import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.data.ItemRenderBackground.Companion.background
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemCategoryOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.tick
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.atomic.AtomicBoolean

fun Double.toStringWithPlus() = (if (this >= 0) "+" else "") + this.toString()

class ReforgeHelper {

    val reforgeMenu by RepoPattern.pattern("menu.reforge", "Reforge Item")
    val reforgeHexMenu by RepoPattern.pattern("menu.reforge.hex", "The Hex ➜ Reforges")
    val reforgeChatMessage by RepoPattern.pattern("chat.reforge.message", "§aYou reforged your .* §r§ainto a .*!|§aYou applied a .* §r§ato your .*!")
    val reforgeChatFail by RepoPattern.pattern("chat.reforge.fail", "§cWait a moment before reforging again!|§cWhoa! Slow down there!")

    var isInReforgeMenu = false
    var isInHexReforgeMenu = false

    fun isReforgeMenu(chestName: String) = reforgeMenu.matches(chestName)
    fun isHexReforgeMenu(chestName: String) = reforgeHexMenu.matches(chestName)

    val posList: Position = Position(-200, 85, true, true)
    val posCurrent: Position = Position(280, 45, true, true)

    fun enable() = LorenzUtils.inSkyBlock && isInReforgeMenu

    var item: ItemStack? = null
    var inventory: Container? = null

    var currentReforge: ReforgeAPI.Reforge? = null
        set(value) {
            field = value
            formattedCurrentReforge = if (value == null) "" else "§7Now:  §3${value.name}"
        }
    var reforgeToSearch: ReforgeAPI.Reforge? = null
        set(value) {
            field = value
            formattedReforgeToSearch = if (value == null) "" else "§7Goal: §9${value.name}"
        }

    var hoverdReforge: ReforgeAPI.Reforge? = null

    var formattedCurrentReforge = ""
    var formattedReforgeToSearch = ""

    val reforgeItem get() = if (isInHexReforgeMenu) 19 else 13
    val reforgeButton get() = if (isInHexReforgeMenu) 48 else 22

    val hexReforgeNextButton = 35

    var waitForChat = AtomicBoolean(false)
    var waitDelay = false

    var sortAfter: ReforgeAPI.StatType? = null

    var display: List<Renderable> = generateDisplay()

    val hoverColor = LorenzColor.GOLD.addOpacity(50).rgb
    val selectedColor = LorenzColor.BLUE.addOpacity(100).rgb
    val finishedColor = LorenzColor.GREEN.addOpacity(75).rgb
    val finishedColorLow = LorenzColor.GREEN.addOpacity(50).rgb

    fun itemUpdate() {
        val newItem = inventory?.getSlot(reforgeItem)?.stack
        if (newItem?.getInternalName() != item?.getInternalName()) {
            reforgeToSearch = null
        }
        item = newItem
        val newReforgeName = item?.getReforgeName() ?: ""
        if (newReforgeName == currentReforge?.lowercaseName) return
        currentReforge = ReforgeAPI.reforgeList.firstOrNull { it.lowercaseName == newReforgeName }
        updateDisplay()
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!enable()) return
        if (event.slot?.slotNumber == reforgeButton) {
            if (event.slot.stack?.name == "§eReforge Item" || event.slot.stack?.name == "§cError!") return
            if (currentReforge == reforgeToSearch) {
                event.isCanceled = true
                waitForChat.set(false)
            } else
                if (waitForChat.get()) {
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
        if (!enable()) return
        when {
            reforgeChatMessage.matches(event.message) -> {
                DelayedRun.runDelayed(2.tick) {
                    itemUpdate()
                    waitForChat.set(false)
                }
            }

            reforgeChatFail.matches(event.message) -> {
                DelayedRun.runDelayed(2.tick) {
                    waitForChat.set(false)
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
                DelayedRun.runDelayed(2.tick) {
                    itemUpdate()
                }
            }

            isReforgeMenu(event.inventoryName) -> {
                item = null
                currentReforge = null
            }

            else -> return
        }
        isInReforgeMenu = true
        waitForChat.set(false)
        DelayedRun.runNextTick {
            inventory = Minecraft.getMinecraft().thePlayer.openContainer
        }
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        if (!enable()) return
        reset()
    }

    fun reset() {
        isInReforgeMenu = false
        isInHexReforgeMenu = false
        reforgeToSearch = null
        currentReforge = null
        hoverdReforge = null
        sortAfter = null
        updateDisplay()
    }

    fun updateDisplay() {
        display = generateDisplay()
    }

    fun generateDisplay() = buildList<Renderable> {
        this.add(Renderable.string("§6Reforge Overlay"))
        val item = item ?: return@buildList
        val internalName = item.getInternalName()
        val itemType = item.getItemCategoryOrNull()
        val itemRarity = item.getItemRarityOrNull()
        val reforgeList = (if (isInHexReforgeMenu) ReforgeAPI.reforgeList else ReforgeAPI.nonePowerStoneReforge).filter { it.isValid(itemType, internalName) }
        val statTypes = reforgeList.mapNotNull { it.stats[itemRarity]?.map { it.key } }.flatten().toSet()
        val statButton = { it: ReforgeAPI.StatType? ->
            val string = Renderable.hoverTips(
                it?.icon ?: "§7D", listOf(
                "§6Sort after:",
                it?.iconWithName ?: "§7Default"
            )
            )
            if (sortAfter == it) {
                Renderable.underlined(string)
            } else {
                Renderable.clickable(string, { sortAfter = it; updateDisplay() })
            }
        }
        val statTypeButtons = (listOf(statButton.invoke(null)) + statTypes.map { statButton.invoke(it) }).chunked(9)
        this.add(Renderable.table(statTypeButtons, xPadding = 3, yPadding = 2))
        val sortSelector: (ReforgeAPI.Reforge) -> Comparable<Any?> = if (sortAfter != null) {
            { -(it.stats[itemRarity]?.get(sortAfter) ?: 0.0) as Comparable<Any?> }
        } else {
            { (it.isReforgeStone) as Comparable<Any?> }
        }
        val list = reforgeList.sortedBy(sortSelector).map { reforge ->
            Renderable.clickAndHover(
                (if (reforge.isReforgeStone) "§9" else "§7") + reforge.name,
                itemRarity?.let { rarity ->
                    if (currentReforge == reforge) listOf(Renderable.string("§3Reforge is currently applied!")) else
                        (reforge.stats[rarity]?.print(currentReforge?.stats?.get(rarity)) ?: emptyList()) +
                            (currentReforge?.extraProperty?.get(rarity)?.split('\n')?.map { Renderable.string(it) }?.let { listOf(Renderable.string("§cRemoves Effect:")) + it }
                                ?: emptyList()) +
                            (reforge.extraProperty?.get(rarity)?.split('\n')?.map { Renderable.string(it) }?.let { listOf(Renderable.string("§aAdds Effect:")) + it }
                                ?: emptyList())

                }
                    ?: listOf(""), onClick = { reforgeToSearch = reforge }, onHover = if (!isInHexReforgeMenu) {
                {}
            } else {
                { hoverdReforge = reforge }
            }
            )
        }
        this.addAll(list)
        if (itemType == null) {
            reforgeToSearch = null
        }
    }

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!enable()) return
        posCurrent.renderStrings(listOf(formattedReforgeToSearch, formattedCurrentReforge), posLabel = "Reforge Notify")
        posList.renderRenderables(display, posLabel = "Reforge Overlay")
        if (hoverdReforge != null) {
            if (hoverdReforge != currentReforge) {
                colorReforgeStone(hoverColor, hoverdReforge?.rawReforgeStoneName ?: "Random Basic Reforge")
            } else {
                inventory?.inventory?.get(reforgeItem)?.background = finishedColorLow
            }
            hoverdReforge = null
        }
        if (reforgeToSearch == null) return
        if (reforgeToSearch != currentReforge) {
            if (reforgeToSearch?.isReforgeStone == true) {
                colorReforgeStone(selectedColor, reforgeToSearch?.rawReforgeStoneName)
            } else {
                inventory?.inventory?.get(reforgeButton)?.background = selectedColor
            }
        } else {
            inventory?.inventory?.get(reforgeItem)?.background = finishedColor
        }
    }

    private fun colorReforgeStone(color: Int, reforgeStone: String?) {
        val itemStack = inventory?.inventory?.firstOrNull { it?.name?.removeColor() == reforgeStone }
        if (itemStack != null) {
            itemStack.background = color
        } else {
            inventory?.inventory?.get(hexReforgeNextButton)?.background = color
        }
    }

    private fun ReforgeAPI.StatList.print(current: ReforgeAPI.StatList?): List<Renderable> {
        val diff = current?.let { this - it }
        val pre = listOf(Renderable.string("§6Reforge Stats"))
        val main = ((diff ?: this).mapNotNull {
            val key = it.key
            val value = this[key]
            val diffValue = diff?.get(key)
            if (key == null || value == null) return@mapNotNull null
            buildList<Renderable> {
                add(Renderable.string("§9${value.toStringWithPlus()}"))
                diffValue?.let { add(Renderable.string((if (it < 0) "§c" else "§a") + it.toStringWithPlus())) }
                add(Renderable.string(key.iconWithName))
            }
        })
        val table = Renderable.table(main, 5)
        return pre + listOf(table)
    }
}

