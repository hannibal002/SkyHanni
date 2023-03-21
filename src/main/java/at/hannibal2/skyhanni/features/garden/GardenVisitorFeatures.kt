package at.hannibal2.skyhanni.features.garden

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.ScoreboardData.Companion.sidebarLinesFormatted
import at.hannibal2.skyhanni.data.SendTitleHelper
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.SBInfo
import net.minecraft.client.Minecraft
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
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
    private val offerAcceptedPattern = Pattern.compile("§6§lOFFER ACCEPTED §r§8with §r(.*) §r.*")
    private val config get() = SkyHanniMod.feature.garden

    companion object {
        var inVisitorInventory = false
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryOpenEvent) {
        inVisitorInventory = false

        if (!GardenAPI.inGarden()) return
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

        if (!config.visitorNeedsDisplay && config.visitorHighlightStatus == 3) return

        var name = npcItem.name ?: return
        if (name.length == name.removeColor().length + 4) {
            name = name.substring(2)
        }
        val visitor = visitors[name]!!
        visitor.entityId = lastClickedNpc
        for (line in offerItem.getLore()) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val (itemName, amount) = ItemUtils.readItemAmount(line)
            if (itemName == null) continue
            val internalName = NEUItems.getInternalName(itemName)
            visitor.items[internalName] = amount
        }
        if (visitor.status == VisitorStatus.NEW) {
            visitor.status = VisitorStatus.WAITING
        }

        update()
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
            if (visitor.status != VisitorStatus.WAITING) continue

            val items = visitor.items
            if (items.isEmpty()) {
                newVisitors.add(visitorName)
            }
            for ((internalName, amount) in items) {
                val old = requiredItems.getOrDefault(internalName, 0)
                requiredItems[internalName] = old + amount
            }
        }
        if (requiredItems.isNotEmpty()) {
            newDisplay.add(Collections.singletonList("§7Visitor items needed:"))
            for ((internalName, amount) in requiredItems) {
                val name = NEUItems.getItemStack(internalName).name
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

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (!inVisitorInventory) return
        if (event.slotId != 33) return

        getVisitor(lastClickedNpc)?.let {
            it.status = VisitorStatus.REFUSED
            update()
        }
    }

    private fun getVisitor(id: Int) = visitors.map { it.value }.find { it.entityId == id }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!GardenAPI.inGarden()) return
        if (!onBarnPlot) return
        if (config.visitorHighlightStatus != 1 && config.visitorHighlightStatus != 2) return

        val entity = event.entity
        if (entity is EntityArmorStand) {
            if (entity.name == "§e§lCLICK") {
                event.isCanceled = true
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        if (!GardenAPI.inGarden()) return
        if (!onBarnPlot) return
        if (config.visitorHighlightStatus != 1 && config.visitorHighlightStatus != 2) return

        for (visitor in visitors.values) {
            visitor.getEntity()?.let {
                val location = it.getLorenzVec().add(0.0, 2.2, 0.0)
                val text = visitor.status.displayName
                event.drawString(location, text)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.inGarden()) return
        if (!inVisitorInventory) return
        val name = event.itemStack.name ?: return
        if (name != "§aAccept Offer") return

        val list = event.toolTip
        var totalPrice = 0.0
        var itemsCounter = 0
        var itemsWithSpeedCounter = 0
        var endReached = false
        for ((i, l) in list.toMutableList().withIndex()) {
            if (l.length < 4) continue

            val line = l.substring(4)
            if (line == "") {
                if (!endReached) {
                    if (config.visitorShowPrice) {
                        if (itemsCounter > 1) {
                            val format = NumberUtil.format(totalPrice)
                            list[1] = list[1] + "$line §7(§6Total §6$format§7)"
                        }
                    }
                    endReached = true
                }
            }

            // Items Required
            if (i > 1 && !endReached) {
                val (itemName, amount) = ItemUtils.readItemAmount(line)
                if (itemName != null) {
                    var internalName: String
                    try {
                        internalName = NEUItems.getInternalName(itemName)
                        // This fixes a NEU bug with §9Hay Bale (cosmetic item)
                        // TODO remove workaround when this is fixed in neu
                        if (internalName == "HAY_BALE") {
                            internalName = "HAY_BLOCK"
                        }
                    } catch (e: NullPointerException) {
                        val message = "internal name is null: '$itemName'"
                        println(message)
                        LorenzUtils.error(message)
                        e.printStackTrace()
                        return
                    }
                    val price = NEUItems.getPrice(internalName) * amount
                    totalPrice += price
                    if (config.visitorShowPrice) {
                        val format = NumberUtil.format(price)
                        list[i + itemsWithSpeedCounter] = "$line §7(§6$format§7)"
                    }
                    itemsCounter++

                    if (config.visitorExactAmountAndTime) {
                        val multiplier = NEUItems.getMultiplier(internalName)
                        val rawName = NEUItems.getItemStack(multiplier.first).name ?: continue
                        val crop = rawName.removeColor()
                        val cropAmount = multiplier.second.toLong() * amount
                        GardenAPI.getCropsPerSecond(crop)?.let {
                            val formatAmount = LorenzUtils.formatInteger(cropAmount)
                            val formatName = "§e${formatAmount}§7x $crop "
                            val formatSpeed = if (it != -1) {
                                val missingTimeSeconds = cropAmount / it
                                val duration = TimeUtils.formatDuration(missingTimeSeconds * 1000)
                                "in §b$duration"
                            } else {
                                "§cno speed data!"
                            }
                            itemsWithSpeedCounter++
                            list.add(i + itemsWithSpeedCounter, " §7- $formatName($formatSpeed§7)")
                        }
                    }
                }
            }

            if (config.visitorCopperPrice) {
                val matcher = copperPattern.matcher(line)
                if (matcher.matches()) {
                    val coppers = matcher.group(1).replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / coppers).toInt())
                    list[i + itemsWithSpeedCounter] = "$line §7(Copper price §6$pricePerCopper§7)"
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.visitorNeedsDisplay && config.visitorHighlightStatus == 3) return
        if (tick++ % 30 != 0) return

        onBarnPlot = sidebarLinesFormatted.contains(" §7⏣ §aThe Garden")

        if (onBarnPlot && config.visitorHighlightStatus != 3) {
            checkVisitorsReady()
        }
    }

    @SubscribeEvent
    fun onTabListUpdate(event: TabListUpdateEvent) {
        if (!GardenAPI.inGarden()) return
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
                var name = line.trim().replace("§r", "")
                if (!name.contains("§")) {
                    name = "§f$name"
                }
                visitorsInTab.add(name)
            }
        }
        if (visitors.keys.removeIf {
                val time = System.currentTimeMillis() - SBInfo.getInstance().joinedWorld
                it !in visitorsInTab && time > 2_000
            }) {
            updateDisplay()
        }
        for (name in visitorsInTab) {
            if (!visitors.containsKey(name)) {
                visitors[name] = Visitor(status = VisitorStatus.NEW)
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

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        val matcher = offerAcceptedPattern.matcher(event.message)
        if (!matcher.matches()) return

        val visitorName = matcher.group(1)
        for (visitor in visitors) {
            if (visitor.key == visitorName) {
                visitor.value.status = VisitorStatus.ACCEPTED
                update()
            }
        }
    }

    private fun update() {
        checkVisitorsReady()
        updateDisplay()
    }

    private fun checkVisitorsReady() {
        for ((visitorName, visitor) in visitors) {
            val entity = visitor.getEntity()
            if (entity == null) {
                findEntityByNametag(visitorName, visitor)
            }

            val status = visitor.status
            if (status == VisitorStatus.WAITING || status == VisitorStatus.READY) {
                visitor.status = if (isReady(visitor)) VisitorStatus.READY else VisitorStatus.WAITING
            }

            if (config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2) {
                if (entity is EntityLivingBase) {
                    val color = status.color
                    if (color != -1) {
                        RenderLivingEntityHelper.setEntityColor(
                            entity,
                            color
                        ) { config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2 }
                    } else {
                        RenderLivingEntityHelper.removeEntityColor(entity)
                    }
                }
            }
        }
    }

    private fun Visitor.getEntity() = Minecraft.getMinecraft().theWorld.getEntityByID(entityId)

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
        for ((internalName, need) in visitor.items) {
            val having = InventoryUtils.countItemsInLowerInventory { it.getInternalName() == internalName }
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
        if (!GardenAPI.inGarden()) return
        if (!config.visitorNeedsDisplay) return

        if (config.visitorNeedsOnlyWhenClose) {
            //TODO check if on barn plot (sidebar)
            if (!onBarnPlot) return
        }

        config.visitorNeedsPos.renderStringsAndItems(display)
    }

    class Visitor(
        var entityId: Int = -1,
        var status: VisitorStatus,
        val items: MutableMap<String, Int> = mutableMapOf(),
    )

    enum class VisitorStatus(val displayName: String, val color: Int) {
        NEW("§e§lNew", LorenzColor.YELLOW.toColor().withAlpha(100)),
        WAITING("§lWaiting", -1),
        READY("§a§lItems Ready", LorenzColor.GREEN.toColor().withAlpha(80)),
        ACCEPTED("§7§lAccepted", LorenzColor.DARK_GRAY.toColor().withAlpha(80)),
        REFUSED("§c§lRefused", LorenzColor.RED.toColor().withAlpha(60)),
    }
}

