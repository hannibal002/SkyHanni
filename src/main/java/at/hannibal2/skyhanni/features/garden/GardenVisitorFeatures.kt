package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.config.features.Garden
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.ScoreboardData.Companion.sidebarLinesFormatted
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import java.util.*
import java.util.regex.Pattern

class GardenVisitorFeatures {

    private val visitors = mutableMapOf<String, Visitor>()
    private val display = mutableListOf<List<Any>>()
    private var lastClickedNpc = 0
    private var onBarnPlot = false
    private var tick = 0
    private val copperPattern = Pattern.compile(" §8\\+§c(.*) Copper")
    private val config: Garden get() = SkyHanniMod.feature.garden

    companion object {
        var inVisitorInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inVisitorInventory = false

        if (!isEnabled()) return
        val npcItem = event.inventoryItems[13] ?: return
        val lore = npcItem.getLore()
        var isVisitor = false
        if (lore.size == 4) {
            val line = lore[3]
            if (line.startsWith("§7Offers Accepted: §a")) {
                isVisitor = true
            }
        }
        if (!isVisitor) return

        val offerItem = event.inventoryItems[29] ?: return
        if (offerItem.name != "§aAccept Offer") return
        inVisitorInventory = true

        if (!config.visitorNeedsDisplay && !config.visitorHighlight) return

        val visitor = visitors[npcItem.name!!]!!
        visitor.entityId = lastClickedNpc
        for (line in offerItem.getLore()) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val (itemName, amount) = ItemUtils.readItemAmount(line)
            if (itemName == null) continue
            visitor.items[itemName] = amount
        }
        checkVisitorsReady()

        updateDisplay()
    }

    private fun updateDisplay() {
        val list = drawDisplay()
        display.clear()
        display.addAll(list)
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        if (!config.visitorNeedsDisplay) return newDisplay

        val requiredItems = mutableMapOf<String, Int>()
        val newVisitors = mutableListOf<String>()
        for ((visitorName, visitor) in visitors) {
            val items = visitor.items
            if (items.isEmpty()) {
                newVisitors.add(visitorName)
            }
            for ((itemName, amount) in items) {
                val old = requiredItems.getOrDefault(itemName, 0)
                requiredItems[itemName] = old + amount
            }
        }
        if (requiredItems.isNotEmpty()) {
            newDisplay.add(Collections.singletonList("§7Visitor items needed:"))
            for ((name, amount) in requiredItems) {
                val internalName: String
                try {
                    internalName = NEUItems.getInternalName(name)
                } catch (e: NullPointerException) {
                    val message = "internal name is null: '$name'"
                    println(message)
                    LorenzUtils.error(message)
                    e.printStackTrace()
                    continue
                }
                val itemStack = NEUItems.getItemStack(internalName)
                newDisplay.add(listOf(" §7- ", itemStack, "$name §8x$amount"))
            }
        }
        if (newVisitors.isNotEmpty()) {
            if (requiredItems.isNotEmpty()) {
                newDisplay.add(Collections.singletonList(""))
            }
            val amount = newVisitors.size
            val visitorLabel = if (amount == 1) "visitor" else "visitors"
            newDisplay.add(Collections.singletonList("§e$amount §7new $visitorLabel:"))
            for (visitor in newVisitors) {
                newDisplay.add(Collections.singletonList(" §7- $visitor"))
            }
        }

        return newDisplay
    }

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!isEnabled()) return
        if (!inVisitorInventory) return
        val name = event.itemStack.name ?: return
        if (name != "§aAccept Offer") return

        val list = event.toolTip
        var totalPrice = 0.0
        var itemsCounter = 0
        var endReached = false
        for ((i, l) in list.toMutableList().withIndex()) {
            val line = l.substring(4)
            if (line == "") {
                if (config.visitorShowPrice) {
                    if (itemsCounter > 1) {
                        val format = NumberUtil.format(totalPrice)
                        list[1] = list[1] + "$line §f(§6Total §6$format§f)"
                    }
                }
                endReached = true
            }

            // Items Required
            if (i > 1 && !endReached) {
                val (itemName, amount) = ItemUtils.readItemAmount(line)
                if (itemName != null) {
                    val internalName: String
                    try {
                        internalName = NEUItems.getInternalName(itemName)
                    } catch (e: NullPointerException) {
                        val message = "internal name is null: '$itemName'"
                        println(message)
                        LorenzUtils.error(message)
                        e.printStackTrace()
                        return
                    }
                    if (config.visitorShowPrice) {
                        val price = NEUItems.getPrice(internalName) * amount
                        totalPrice += price
                        val format = NumberUtil.format(price)
                        list[i] = "$line §7(§6$format§7)"
                    }
                    itemsCounter++

                    if (config.visitorExactAmountAndTime) {
                        val multiplier = NEUItems.getMultiplier(internalName)
                        val rawName = NEUItems.getItemStack(multiplier.first).name ?: continue
                        val crop = rawName.removeColor()
                        val cropAmount = multiplier.second.toLong() * amount
                        GardenCropMilestoneDisplay.cropsPerSecond[crop]?.let {
                            val formatAmount = LorenzUtils.formatInteger(cropAmount)
                            val formatName = "§e${formatAmount}§7x $crop "
                            val formatSpeed = if (it != -1) {
                                val missingTimeSeconds = cropAmount / it
                                val duration = TimeUtils.formatDuration(missingTimeSeconds * 1000)
                                "in §b$duration"
                            } else {
                                "§cno speed data!"
                            }
                            list.add(i + itemsCounter, " §7- $formatName($formatSpeed§7)")
                        }
                    }
                }
            }

            if (config.visitorCopperPrice) {
                val matcher = copperPattern.matcher(line)
                if (matcher.matches()) {
                    val coppers = matcher.group(1).replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / coppers).toInt())
                    list[i] = list[i] + " §f(§7Copper price §6$pricePerCopper§f)"
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!isEnabled()) return
        if (!config.visitorNeedsDisplay && !config.visitorHighlight) return
        if (tick++ % 30 != 0) return

        onBarnPlot = sidebarLinesFormatted.contains(" §7⏣ §aThe Garden")

        if (onBarnPlot && config.visitorHighlight) {
            checkVisitorsReady()
        }
    }

    @SubscribeEvent
    fun onTick(event: TabListUpdateEvent) {
        if (!isEnabled()) return
        var found = false
        val visitorsInTab = mutableListOf<String>()
        for (line in event.tabList) {
            if (line.startsWith("§b§lVisitors:")) {
                found = true
                continue
            }
            if (found) {
                if (line.isEmpty()) {
                    found = false
                    continue
                }
                val name = line.substring(3)
                visitorsInTab.add(name)
            }
        }
        if (visitors.keys.removeIf { it !in visitorsInTab }) {
            updateDisplay()
        }
        for (name in visitorsInTab) {
            if (!visitors.containsKey(name)) {
                visitors[name] = Visitor(-1)
                if (config.visitorNotificationTitle) {
                    SendTitleHelper.sendTitle("§eNew Visitor", 5_000)
                }
                if (config.visitorNotificationChat) {
                    LorenzUtils.chat("§e[SkyHanni] $name §eis visiting your garden!")
                }
                updateDisplay()
            }
        }
    }

    private fun checkVisitorsReady() {
        for ((visitorName, visitor) in visitors) {
            val entity = Minecraft.getMinecraft().theWorld.getEntityByID(visitor.entityId)
            if (entity == null) {
                findEntityByNametag(visitorName, visitor)
            }

            if (entity is EntityLivingBase) {
                if (visitor.items.isEmpty()) {
                    val color = LorenzColor.DARK_AQUA.toColor().withAlpha(120)
                    RenderLivingEntityHelper.setEntityColor(entity, color)
                    { config.visitorHighlight }
                } else if (isReady(visitor)) {
                    val color = LorenzColor.GREEN.toColor().withAlpha(120)
                    RenderLivingEntityHelper.setEntityColor(entity, color)
                    { config.visitorHighlight }
                } else {
                    RenderLivingEntityHelper.removeEntityColor(entity)
                }
            }
        }
    }

    private fun findEntityByNametag(visitorName: String, visitor: Visitor) {
        Minecraft.getMinecraft().theWorld.loadedEntityList
            .filter { it is EntityArmorStand && it.name == visitorName }
            .forEach { entity ->
                Minecraft.getMinecraft().theWorld.loadedEntityList
                    .filter { it !is EntityArmorStand }
                    .filter { entity.getLorenzVec().distanceIgnoreY(it.getLorenzVec()) == 0.0 }
                    .forEach { visitor.entityId = it?.entityId ?: 0 }
            }
    }

    private fun isReady(visitor: Visitor): Boolean {
        var ready = true
        for ((name, need) in visitor.items) {
            val cleanName = name.removeColor()
            val having = InventoryUtils.countItemsInLowerInventory { it.name?.contains(cleanName) ?: false }
            if (having < need) {
                ready = false
            }
        }
        return ready
    }

    // TODO make event
    @SubscribeEvent
    fun onSendEvent(event: PacketEvent.SendEvent) {
        val packet = event.packet
        if (packet !is C02PacketUseEntity) return

        val theWorld = Minecraft.getMinecraft().theWorld
        val entity = packet.getEntityFromWorld(theWorld) ?: return
        val entityId = entity.entityId

        lastClickedNpc = entityId
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!isEnabled()) return
        if (!config.visitorNeedsDisplay) return

        if (config.visitorNeedsOnlyWhenClose) {
            //TODO check if on barn plot (sidebar)
            if (!onBarnPlot) return
        }

        config.visitorNeedsPos.renderStringsAndItems(display)
    }

    class Visitor(var entityId: Int, val items: MutableMap<String, Int> = mutableMapOf())

    private fun isEnabled() = LorenzUtils.inSkyBlock && LorenzUtils.skyBlockIsland == IslandType.GARDEN
}