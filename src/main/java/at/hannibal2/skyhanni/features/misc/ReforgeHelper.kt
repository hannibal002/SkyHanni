package at.hannibal2.skyhanni.features.misc

import at.hannibal2.skyhanni.config.core.config.Position
import at.hannibal2.skyhanni.events.GuiContainerEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.RepositoryReloadEvent
import at.hannibal2.skyhanni.utils.DelayedRun
import at.hannibal2.skyhanni.utils.ItemUtils.getItemRarityOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzRarity
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.RenderUtils.renderRenderables
import at.hannibal2.skyhanni.utils.RenderUtils.renderStrings
import at.hannibal2.skyhanni.utils.SkyBlockItemModifierUtils.getReforgeName
import at.hannibal2.skyhanni.utils.StringUtils.matches
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import net.minecraft.client.Minecraft
import net.minecraft.inventory.Container
import net.minecraft.item.ItemStack
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ReforgeHelper {

    enum class ReforgeType {
        Sword, Ranged, Armor, Tool, Equipment
    }

    enum class StatType(val icon: String) {
        Strength("§c❁")
    }

    data class Stat(val amount: Int, val type: StatType) {
        override fun toString(): String {
            return amount.toString() + " " + type.icon
        }
    }

    class Reforge(val name: String, val type: ReforgeType, val stats: Map<LorenzRarity, List<Stat>>)

    private val reforges = listOf(
        Reforge("Clean", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(9, StatType.Strength)))),
        Reforge("Fierce", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(8, StatType.Strength)))),
        Reforge("Heavy", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(7, StatType.Strength)))),
        Reforge("Light", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(6, StatType.Strength)))),
        Reforge("Mythic", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(5, StatType.Strength)))),
        Reforge("Pure", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(4, StatType.Strength)))),
        Reforge("Smart", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(3, StatType.Strength)))),
        Reforge("Titanic", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(2, StatType.Strength)))),
        Reforge("Wise", ReforgeType.Armor, mapOf(LorenzRarity.COMMON to listOf(Stat(1, StatType.Strength)))),
    )

    val reforgeMenu by RepoPattern.pattern("menu.reforge", "Reforge Item")
    val reforgeHexMenu by RepoPattern.pattern("menu.reforge.hex", "The Hex ➜ Reforges")
    val reforgeChatMessage by RepoPattern.pattern("chat.reforge.message", "§aYou reforged your .* §r§ainto a .*!")
    val reforgeChatFail by RepoPattern.pattern("chat.reforge.fail", "§cWait a moment before reforging again!")

    var isInReforgeMenu = false
    var isInHexReforgeMenu = false

    fun isReforgeMenu(chestName: String) = reforgeMenu.matches(chestName)
    fun isHexReforgeMenu(chestName: String) = reforgeHexMenu.matches(chestName)

    val posList: Position = Position(-200, 85, true, true)
    val posCurrent: Position = Position(280, 45, true, true)

    fun enable() = LorenzUtils.inSkyBlock && isInReforgeMenu

    var item: ItemStack? = null
    var inventory: Container? = null

    var currentReforge: String = ""
        set(value) {
            field = value
            formattedCurrentReforge = if (value.isEmpty()) "" else "§7Now:  §3${value.replaceFirstChar { it.uppercase() }}"
        }
    var reforgeToSearch: String = ""
        set(value) {
            field = value
            formattedReforgeToSearch = if (value.isEmpty()) "" else "§7Goal: §9${value.replaceFirstChar { it.uppercase() }}"
        }

    var formattedCurrentReforge = ""
    var formattedReforgeToSearch = ""

    val reforgeItem get() = if (isInHexReforgeMenu) 19 else 13
    val reforgeButton get() = if (isInHexReforgeMenu) 48 else 22

    var waitForChat = AtomicBoolean(false)
    var waitDelay = false

    fun itemUpdate() {
        item = inventory?.getSlot(reforgeItem)?.stack
        currentReforge = item?.getReforgeName() ?: ""
    }

    @SubscribeEvent
    fun onClick(event: GuiContainerEvent.SlotClickEvent) {
        if (!enable()) return
        if (event.slot?.slotNumber == reforgeButton) {
            if (event.slot.stack?.name == "§eReforge Item" || event.slot.stack?.name == "§cError!") return
            if (currentReforge == reforgeToSearch) {
                event.isCanceled = true
                waitForChat.set(false)
                return
            }
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
            return
        }

        DelayedRun.runNextTick {
            itemUpdate()
            updateDisplay()
        }
    }

    inline val Int.tick get() = (this * 50 * 2).toDuration(DurationUnit.MILLISECONDS) // Remove the x2 when NeaTickEvent is implemented

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
                    updateDisplay()
                }
            }

            isReforgeMenu(event.inventoryName) -> {
                item = null
                currentReforge = ""
            }

            else -> return
        }
        isInReforgeMenu = true
        reforgeToSearch = ""
        waitForChat.set(false)
        DelayedRun.runNextTick {
            inventory = Minecraft.getMinecraft().thePlayer.openContainer
        }
    }

    @SubscribeEvent
    fun onClose(event: InventoryCloseEvent) {
        if (!enable()) return
        isInReforgeMenu = false
        isInHexReforgeMenu = false
    }

    fun generateDisplay() = buildList<Renderable> {
        this.add(Renderable.string("Reforge Overlay"))
        val item = item ?: return@buildList
        val itemType = ReforgeType.Armor
        val itemRarity = item.getItemRarityOrNull()
        val list = reforges.filter { it.type == itemType }.map { reforge ->
            Renderable.clickAndHover(reforge.name, itemRarity?.let { reforge.stats[it]?.map { if (it.amount < 0) "§c$reforge" else "§a+$reforge" } }
                ?: listOf("")) { reforgeToSearch = reforge.name.lowercase() }
        }
        this.addAll(list)
        if (itemType == null) {
            reforgeToSearch = ""
        }
    }

    fun updateDisplay() {
        display = generateDisplay()
    }

    var display: List<Renderable> = generateDisplay()

    @SubscribeEvent
    fun onRender(event: GuiRenderEvent.ChestGuiOverlayRenderEvent) {
        if (!enable()) return
        posCurrent.renderStrings(listOf(formattedReforgeToSearch, formattedCurrentReforge), posLabel = "Reforge Notify")
        posList.renderRenderables(display, posLabel = "Reforge Overlay")
    }

    @SubscribeEvent
    fun onRepo(event: RepositoryReloadEvent) {

    }
}
