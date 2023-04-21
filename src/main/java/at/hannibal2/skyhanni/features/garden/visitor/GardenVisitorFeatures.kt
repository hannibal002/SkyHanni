package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.CropType
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.GardenAPI.getSpeed
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.*
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

class GardenVisitorFeatures {
    private val visitors = mutableMapOf<String, Visitor>()
    private var display = listOf<List<Any>>()
    private var lastClickedNpc = 0
    private var tick = 0
    private val newVisitorArrivedMessage = ".* §r§ehas arrived on your §r§bGarden§r§e!".toPattern()
    private val copperPattern = " §8\\+§c(.*) Copper".toPattern()
    private val gardenExperiencePattern = " §8\\+§2(.*) §7Garden Experience".toPattern()
    private val config get() = SkyHanniMod.feature.garden
    private val logger = LorenzLogger("garden/visitors")

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
            if (itemName == null) {
                LorenzUtils.error("§c[SkyHanni] Could not read item '$line'")
                continue
            }
            val internalName = NEUItems.getInternalName(itemName)
            visitor.items[internalName] = amount
        }
        if (visitor.status == VisitorStatus.NEW) {
            val alreadyReady = event.inventoryItems[29]?.getLore()?.any { it == "§eClick to give!" } == true
            if (alreadyReady) {
                changeStatus(visitor, VisitorStatus.READY, "inSacks")
                visitor.inSacks = true
                update()
            } else {
                val waiting = VisitorStatus.WAITING
                changeStatus(visitor, waiting, "firstContact")
            }
            update()
        }
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        if (!config.visitorNeedsDisplay) return newDisplay

        val requiredItems = mutableMapOf<String, Int>()
        val newVisitors = mutableListOf<String>()
        for ((visitorName, visitor) in visitors) {
            if (visitor.status == VisitorStatus.ACCEPTED || visitor.status == VisitorStatus.REFUSED) continue

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
            newDisplay.addAsSingletonList("§7Visitor items needed:")
            for ((internalName, amount) in requiredItems) {
                val name = NEUItems.getItemStack(internalName).name!!
                val itemStack = NEUItems.getItemStack(internalName)

                val list = mutableListOf<Any>()
                list.add(" §7- ")
                list.add(itemStack)

                list.add(Renderable.optionalLink("$name §8x${amount.addSeparators()}", {
                    if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                        LorenzUtils.setTextIntoSign("$amount")
                    } else if (!InventoryUtils.inStorage()) {
                        LorenzUtils.sendCommandToServer("bz ${name.removeColor()}")
                        OSUtils.copyToClipboard("$amount")
                    }
                }) { GardenAPI.inGarden() && !InventoryUtils.inStorage() })

                if (config.visitorNeedsShowPrice) {
                    val price = NEUItems.getPrice(internalName) * amount
                    val format = NumberUtil.format(price)
                    list.add(" §7(§6$format§7)")
                }

                newDisplay.add(list)
            }
        }
        if (newVisitors.isNotEmpty()) {
            if (requiredItems.isNotEmpty()) {
                newDisplay.addAsSingletonList("")
            }
            val amount = newVisitors.size
            val visitorLabel = if (amount == 1) "visitor" else "visitors"
            newDisplay.addAsSingletonList("§e$amount §7new $visitorLabel:")
            for (visitor in newVisitors) {
                val displayName = GardenVisitorColorNames.getColoredName(visitor)

                val list = mutableListOf<Any>()
                list.add(" §7- $displayName")

                if (config.visitorItemPreview) {
                    val items = GardenVisitorColorNames.visitorItems[visitor.removeColor()]
                    if (items == null) {
                        val text = "Visitor '$visitor' has no items in repo!"
                        logger.log(text)
                        LorenzUtils.debug(text)
                        list.add(" §7(§c?§7)")
                        continue
                    }
                    list.add(" ")
                    if (items.isEmpty()) {
                        list.add("§7(§fAny§7)")
                    } else {
                        for (item in items) {
                            val internalName = NEUItems.getInternalNameOrNull(item)
                            if (internalName != null) {
                                list.add(NEUItems.getItemStack(internalName))
                            } else {
                                list.add(" '$item' ")
                            }
                        }
                    }
                }

                newDisplay.add(list)
            }
        }

        return newDisplay
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventorItemUpdateEvent) {
        if (GardenAPI.onBarnPlot) {
            MinecraftExecutor.OnThread.execute {
                update()
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onStackClick(event: SlotClickEvent) {
        if (!inVisitorInventory) return
        if (event.clickType != 0) return

        val visitor = getVisitor(lastClickedNpc) ?: return

        if (event.slotId == 33) {
            if (event.slot.stack?.name != "§cRefuse Offer") return
            changeStatus(visitor, VisitorStatus.REFUSED, "refused")
            update()
            return
        }
        if (event.slotId == 29) {
            if (event.slot.stack?.getLore()?.any { it == "§eClick to give!" } == true) {
                changeStatus(visitor, VisitorStatus.ACCEPTED, "accepted")
                update()
                return
            }
        }
    }

    private fun getVisitor(id: Int) = visitors.map { it.value }.find { it.entityId == id }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.onBarnPlot) return
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
        if (!GardenAPI.onBarnPlot) return
        if (config.visitorHighlightStatus != 1 && config.visitorHighlightStatus != 2) return

        for (visitor in visitors.values) {
            visitor.getNameTagEntity()?.getLorenzVec()?.let {
                if (it.distanceToPlayer() < 15) {
                    val text = visitor.status.displayName
                    event.drawString(it.add(0.0, 2.23, 0.0), text)
                }
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
                    val internalName = NEUItems.getInternalNameOrNull(itemName)
                    if (internalName == null) {
                        val message = "internal name is null: '$itemName'"
                        println(message)
                        LorenzUtils.error(message)
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
                        val rawName = NEUItems.getItemStack(multiplier.first).name?.removeColor() ?: continue
                        CropType.getByItemName(rawName)?.let {
                            val speed = it.getSpeed()
                            val cropAmount = multiplier.second.toLong() * amount
                            val formatAmount = LorenzUtils.formatInteger(cropAmount)
                            val formatName = "§e$formatAmount§7x ${it.cropName} "
                            val formatSpeed = if (speed != -1) {
                                val missingTimeSeconds = cropAmount / speed
                                val duration = TimeUtils.formatDuration(missingTimeSeconds * 1000)
                                "in §b$duration"
                            } else {
                                "§cno speed data!"
                            }
                            itemsWithSpeedCounter++
                            list.add(i + itemsWithSpeedCounter, " §7- $formatName($formatSpeed§7)")
                        }
                    }
                } else {
                    LorenzUtils.error("§c[SkyHanni] Could not read item '$line'")
                }
            }

            if (config.visitorCopperPrice) {
                val matcher = copperPattern.matcher(line)
                if (matcher.matches()) {
                    val coppers = matcher.group(1).replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / coppers).toInt())
                    list[i + itemsWithSpeedCounter] = "$line §7(price per §6$pricePerCopper§7)"
                }
            }
            if (config.visitorExperiencePrice) {
                val matcher = gardenExperiencePattern.matcher(line)
                if (matcher.matches()) {
                    val gardenExp = matcher.group(1).replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / gardenExp).toInt())
                    list[i + itemsWithSpeedCounter] = "$line §7(price per §6$pricePerCopper§7)"
                }
            }
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.visitorNeedsDisplay && config.visitorHighlightStatus == 3) return
        if (tick++ % 10 != 0) return
//        if (tick++ % 300 != 0) return

        if (GardenAPI.onBarnPlot && config.visitorHighlightStatus != 3) {
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
                val name = fromHypixelName(line)

                // Hide hypixel watchdog entries
                if (name.contains("§c") && !name.contains("Spaceman") && !name.contains("Grandma Wolf")) {
                    logger.log("Ignore wrong red name: '$name'")
                    continue
                }


                //hide own player name
                if (name.contains(LorenzUtils.getPlayerName())) {
                    logger.log("Ignore wrong own name: '$name'")
                    continue
                }

                visitorsInTab.add(name)
            }
        }
        if (visitors.keys.removeIf {
                val time = System.currentTimeMillis() - LorenzUtils.lastWorldSwitch
                val removed = it !in visitorsInTab && time > 2_000
                if (removed) {
                    logger.log("Removed old visitor: '$it'")
                }
                removed
            }) {
            updateDisplay()
        }
        for (name in visitorsInTab) {
            if (!visitors.containsKey(name)) {
                addVisitor(name)
            }
        }
    }

    private fun addVisitor(name: String) {
        visitors[name] = Visitor(name, status = VisitorStatus.NEW)
        logger.log("New visitor detected: '$name'")

        if (config.visitorNotificationTitle) {
            TitleUtils.sendTitle("§eNew Visitor", 5_000)
        }
        if (config.visitorNotificationChat) {
            val displayName = GardenVisitorColorNames.getColoredName(name)
            LorenzUtils.chat("§e[SkyHanni] $displayName §eis visiting your garden!")
        }
        updateDisplay()

        if (System.currentTimeMillis() > LorenzUtils.lastWorldSwitch + 2_000) {
            if (name.removeColor().contains("Jerry")) {
                logger.log("Jerry!")
                ItemBlink.setBlink(NEUItems.getItemStackOrNull("JERRY;4"), 5_000)
            }
        }
    }

    private fun fromHypixelName(line: String): String {
        var name = line.trim().replace("§r", "").trim()
        if (!name.contains("§")) {
            name = "§f$name"
        }
        return name
    }

    @SubscribeEvent
    fun onTabListText(event: TabListLineRenderEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.visitorColoredName) return
        val text = event.text
        val replace = fromHypixelName(text)
        val visitor = visitors[replace]
        visitor?.let {
            event.text = " " + GardenVisitorColorNames.getColoredName(it.visitorName)
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (config.visitorHypixelArrivedMessage) {
            if (newVisitorArrivedMessage.matcher(event.message).matches()) {
                event.blockedReason = "new_visitor_arrived"
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
                findNametag(visitorName.removeColor())?.let {
                    findEntity(it, visitor)
                }
            }

            if (!visitor.inSacks) {
                val status = visitor.status
                if (status == VisitorStatus.WAITING || status == VisitorStatus.READY) {
                    val newStatus = if (hasItemsInInventory(visitor)) VisitorStatus.READY else VisitorStatus.WAITING
                    changeStatus(visitor, newStatus, "hasItemsInInventory")
                }
            }

            if (config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2) {
                if (entity is EntityLivingBase) {
                    val color = visitor.status.color
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

    private fun changeStatus(visitor: Visitor, newStatus: VisitorStatus, reason: String) {
        val old = visitor.status
        if (old == newStatus) return
        visitor.status = newStatus
        val name = visitor.visitorName.removeColor()
        logger.log("Visitor status change for '$name': $old -> $newStatus ($reason)")
    }

    private fun findEntity(nameTag: EntityArmorStand, visitor: Visitor) {
        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (entity is EntityArmorStand) continue
            if (entity.getLorenzVec().distanceIgnoreY(nameTag.getLorenzVec()) != 0.0) continue

            visitor.entityId = entity?.entityId ?: 0
            visitor.nameTagEntityId = nameTag.entityId
        }
    }

    private fun findNametag(visitorName: String): EntityArmorStand? {
        val foundVisitorNameTags = mutableListOf<EntityArmorStand>()
        for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
            if (entity !is EntityArmorStand) continue

            if (entity.name.removeColor() == visitorName) {
                foundVisitorNameTags.add(entity)
            }
        }

        if (visitorName in listOf("Jacob", "Anita")) {
            // Only detect jacob/anita npc if the "wrong" npc got found as well
            if (foundVisitorNameTags.size != 2) return null

            for (tag in foundVisitorNameTags.toMutableList()) {
                for (entity in Minecraft.getMinecraft().theWorld.loadedEntityList) {
                    if (entity !is EntityArmorStand) continue
                    if (entity in foundVisitorNameTags) continue
                    val distance = entity.getLorenzVec().distance(tag.getLorenzVec())
                    if (distance < 1.5 && entity.name == "§bSam") {
                        foundVisitorNameTags.remove(tag)
                    }
                }
            }
        }

        if (foundVisitorNameTags.size != 1) return null
        return foundVisitorNameTags[0]
    }

    private fun hasItemsInInventory(visitor: Visitor): Boolean {
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
    fun onRenderInSigns(event: DrawScreenEvent.Post) {
        if (!GardenAPI.inGarden()) return
        if (!config.visitorNeedsDisplay) return
        val gui = event.gui
        if (gui !is GuiEditSign) return

        if (config.visitorNeedsOnlyWhenClose && !GardenAPI.onBarnPlot) return

        if (!GardenAPI.hideExtraGuis()) {
            config.visitorNeedsPos.renderStringsAndItems(display, posLabel = "Visitor Items Needed")
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!config.visitorNeedsDisplay) return

        if (showGui()) {
            config.visitorNeedsPos.renderStringsAndItems(display, posLabel = "Visitor Items Needed")
        }
    }

    private fun showGui(): Boolean {
        if (config.visitorNeedsInBazaarAlley) {
            if (LorenzUtils.skyBlockIsland == IslandType.HUB && LorenzUtils.skyBlockArea == "Bazaar Alley") {
                return true
            }
        }

        if (GardenAPI.hideExtraGuis()) return false
        if (GardenAPI.inGarden()) {
            if (GardenAPI.onBarnPlot) return true
            if (!config.visitorNeedsOnlyWhenClose) return true
        }
        return false
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!SkyHanniMod.feature.garden.visitorColoredName) return
        val entity = event.entity
        val entityId = entity.entityId
        for (visitor in visitors.values) {
            if (visitor.nameTagEntityId == entityId) {
                entity.customNameTag = GardenVisitorColorNames.getColoredName(entity.name)
            }
        }
    }

    class Visitor(
        val visitorName: String,
        var entityId: Int = -1,
        var nameTagEntityId: Int = -1,
        var status: VisitorStatus,
        var inSacks: Boolean = false,
        val items: MutableMap<String, Int> = mutableMapOf(),
    ) {
        fun getEntity(): Entity? = Minecraft.getMinecraft().theWorld.getEntityByID(entityId)

        fun getNameTagEntity(): Entity? = Minecraft.getMinecraft().theWorld.getEntityByID(nameTagEntityId)
    }

    enum class VisitorStatus(val displayName: String, val color: Int) {
        NEW("§eNew", LorenzColor.YELLOW.toColor().withAlpha(100)),
        WAITING("Waiting", -1),
        READY("§aItems Ready", LorenzColor.GREEN.toColor().withAlpha(80)),
        ACCEPTED("§7Accepted", LorenzColor.DARK_GRAY.toColor().withAlpha(80)),
        REFUSED("§cRefused", LorenzColor.RED.toColor().withAlpha(60)),
    }
}

