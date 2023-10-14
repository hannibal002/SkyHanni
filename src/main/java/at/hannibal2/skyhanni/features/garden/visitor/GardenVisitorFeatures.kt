package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.CheckRenderEntityEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.InventoryCloseEvent
import at.hannibal2.skyhanni.events.InventoryFullyOpenedEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzRenderWorldEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.TabListLineRenderEvent
import at.hannibal2.skyhanni.events.TabListUpdateEvent
import at.hannibal2.skyhanni.events.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.withAlpha
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.command.CopyErrorCommand
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.KeyboardManager.isKeyHeld
import at.hannibal2.skyhanni.utils.LocationUtils.distanceToPlayer
import at.hannibal2.skyhanni.utils.LorenzColor
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.LorenzUtils.editCopy
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.exactLocation
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.events.SlotClickEvent
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

private val config get() = SkyHanniMod.feature.garden

class GardenVisitorFeatures {
    private var visitors = mapOf<String, Visitor>()
    private var display = emptyList<List<Any>>()
    private var lastClickedNpc = 0
    private val newVisitorArrivedMessage = ".* §r§ehas arrived on your §r§bGarden§r§e!".toPattern()
    private val copperPattern = " §8\\+§c(?<amount>.*) Copper".toPattern()
    private val gardenExperiencePattern = " §8\\+§2(?<amount>.*) §7Garden Experience".toPattern()
    private val visitorChatMessagePattern = "§e\\[NPC] (§.)?(?<name>.*)§f: §r.*".toPattern()

    private val logger = LorenzLogger("garden/visitors")
    private var lastFullPrice = 0.0

    companion object {
        var inVisitorInventory = false
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        visitors = emptyMap()
    }

    @SubscribeEvent
    fun onInventoryOpen(event: InventoryFullyOpenedEvent) {
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

        val visitor = getOrCreateVisitor(name) ?: return

        visitor.entityId = lastClickedNpc
        for (line in offerItem.getLore()) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val pair = ItemUtils.readItemAmount(line)
            if (pair == null) {
                LorenzUtils.error("§c[SkyHanni] Could not read item '$line'")
                continue
            }
            val (itemName, amount) = pair
            val internalName = NEUInternalName.fromItemName(itemName)
            visitor.items[internalName] = amount
        }
        readToolTip(visitor, event.inventoryItems[29])

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

    private fun getOrCreateVisitor(name: String): Visitor? {
        var visitor = visitors[name]
        if (visitor == null) {
            // workaround if the tab list has not yet updated when opening the visitor
            addVisitor(name)
            LorenzUtils.debug("Found visitor from npc that is not in tab list. Adding it still.")
            updateDisplay()
            visitor = visitors[name]
        }

        if (visitor != null) return visitor

        println("visitors: $visitors")
        println("name: $name")
        CopyErrorCommand.logErrorState(
            "Error finding the visitor `$name§c`. Try to reopen the inventory",
            "visitor is null! name='$name', visitors=`$visitors`"
        )
        return null
    }

    private fun readReward(offerItem: ItemStack): VisitorReward? {
        for (line in offerItem.getLore()) {
            for (reward in VisitorReward.entries) {
                if (line.contains(reward.displayName)) {
                    return reward
                }
            }
        }
        return null
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        if (!config.visitorNeedsDisplay) return newDisplay

        val requiredItems = mutableMapOf<NEUInternalName, Int>()
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
            var totalPrice = 0.0
            newDisplay.addAsSingletonList("§7Visitor items needed:")
            for ((internalName, amount) in requiredItems) {
                val name = internalName.getItemName()
                val itemStack = internalName.getItemStack()

                val list = mutableListOf<Any>()
                list.add(" §7- ")
                list.add(itemStack)

                list.add(Renderable.optionalLink("$name §ex${amount.addSeparators()}", {
                    if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                        LorenzUtils.setTextIntoSign("$amount")
                    } else {
                        BazaarApi.searchForBazaarItem(name, amount)
                    }
                }) { GardenAPI.inGarden() && !NEUItems.neuHasFocus() })

                if (config.visitorNeedsShowPrice) {
                    val price = internalName.getPrice() * amount
                    totalPrice += price
                    val format = NumberUtil.format(price)
                    list.add(" §7(§6$format§7)")
                }

                newDisplay.add(list)
            }
            if (totalPrice > 0) {
                val format = NumberUtil.format(totalPrice)
                newDisplay[0] = listOf("§7Visitor items needed: §7(§6$format§7)")
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
                                list.add(internalName.getItemStack())
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
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
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

            visitor.hasReward()?.let {
                if (config.visitorRewardWarning.preventRefusing) {
                    if (config.visitorRewardWarning.bypassKey.isKeyHeld()) {
                        LorenzUtils.chat("§e[SkyHanni] §cBypassed blocking refusal of visitor ${visitor.visitorName} §7(${it.displayName}§7)")
                        return
                    }
                    event.isCanceled = true
                    LorenzUtils.chat("§e[SkyHanni] §cBlocked refusing visitor ${visitor.visitorName} §7(${it.displayName}§7)")
                    if (config.visitorRewardWarning.bypassKey == Keyboard.KEY_NONE) {
                        LorenzUtils.clickableChat(
                            "§eIf you want to deny this visitor, set a keybind in §e/sh bypass",
                            "sh bypass"
                        )
                    }
                    Minecraft.getMinecraft().thePlayer.closeScreen()
                    return
                }
            }

            changeStatus(visitor, VisitorStatus.REFUSED, "refused")
            update()
            GardenVisitorDropStatistics.deniedVisitors += 1
            GardenVisitorDropStatistics.saveAndUpdate()
            return
        }
        if (event.slotId == 29 && event.slot.stack?.getLore()?.any { it == "§eClick to give!" } == true) {
            changeStatus(visitor, VisitorStatus.ACCEPTED, "accepted")
            acceptVisitor(visitor)
            update()
            GardenVisitorDropStatistics.coinsSpent += round(lastFullPrice).toLong()
            GardenVisitorDropStatistics.lastAccept = System.currentTimeMillis()
            return
        }
    }

    private fun acceptVisitor(visitor: Visitor) {
        VisitorAcceptEvent(visitor).postAndCatch()
    }

    private fun getVisitor(id: Int) = visitors.map { it.value }.find { it.entityId == id }

    @SubscribeEvent
    fun onCheckRender(event: CheckRenderEntityEvent<*>) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.onBarnPlot) return
        if (config.visitorHighlightStatus != 1 && config.visitorHighlightStatus != 2) return

        val entity = event.entity
        if (entity is EntityArmorStand && entity.name == "§e§lCLICK") {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onRenderWorld(event: LorenzRenderWorldEvent) {
        if (!GardenAPI.inGarden()) return
        if (!GardenAPI.onBarnPlot) return
        if (config.visitorHighlightStatus != 1 && config.visitorHighlightStatus != 2) return

        for (visitor in visitors.values) {
            visitor.getNameTagEntity()?.let {
                val location = event.exactLocation(it)
                if (it.distanceToPlayer() < 15) {
                    val text = visitor.status.displayName
                    event.drawString(location.add(0.0, 2.23, 0.0), text)
                    if (config.visitorRewardWarning.showOverName) {
                        visitor.hasReward()?.let { reward ->
                            val name = reward.displayName

                            event.drawString(location.add(0.0, 2.73, 0.0), "§c!$name§c!")
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.onBarnPlot) return
        if (!inVisitorInventory) return
        if (event.itemStack.name != "§aAccept Offer") return

        val visitor = getVisitor(lastClickedNpc) ?: return

        val toolTip = event.toolTip ?: return
        toolTip.clear()

        if (visitor.lastLore.isEmpty()) {
            readToolTip(visitor, event.itemStack)
            LorenzUtils.chat("§e[SkyHanni] Reloaded the visitor data of that inventory, this should not happen.")
        }

        toolTip.addAll(visitor.lastLore)
    }

    private fun readToolTip(visitor: Visitor, itemStack: ItemStack?) {
        val stack = itemStack ?: error("Accept offer item not found for visitor ${visitor.visitorName}")
        var totalPrice = 0.0
        var timeRequired = -1L
        var readingItemsNeeded = true
        lastFullPrice = 0.0
        val foundRewards = mutableListOf<NEUInternalName>()

        for (formattedLine in stack.getLore()) {
            if (formattedLine.contains("Rewards")) {
                readingItemsNeeded = false
            }

            val (itemName, amount) = ItemUtils.readItemAmount(formattedLine) ?: continue
            val internalName = NEUItems.getInternalNameOrNull(itemName)?.replace("◆_", "") ?: continue
            val price = internalName.getPrice() * amount

            if (readingItemsNeeded) {
                totalPrice += price
                lastFullPrice += price
            } else {
                foundRewards.add(internalName)
                totalPrice -= price
            }
        }
        if (totalPrice < 0) {
            totalPrice = 0.0
        }

        if (foundRewards.isNotEmpty()) {
            val wasEmpty = visitor.allRewards.isEmpty()
            visitor.allRewards = foundRewards
            if (wasEmpty) {
                visitor.hasReward()?.let { reward ->
                    if (config.visitorRewardWarning.notifyInChat) {
                        LorenzUtils.chat("§e[SkyHanni] Found Visitor Reward ${reward.displayName}§e!")
                    }
                }
            }
        }

        readingItemsNeeded = true
        val finalList = stack.getLore().toMutableList()
        var offset = 0
        for ((i, formattedLine) in finalList.toMutableList().withIndex()) {
            val index = i + offset
            if (config.visitorExperiencePrice) {
                gardenExperiencePattern.matchMatcher(formattedLine) {
                    val gardenExp = group("amount").replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / gardenExp).toInt())
                    finalList.set(index, "$formattedLine §7(§6$pricePerCopper §7per)")
                }
            }

            copperPattern.matchMatcher(formattedLine) {
                val copper = group("amount").replace(",", "").toInt()
                val pricePerCopper = NumberUtil.format((totalPrice / copper).toInt())
                val timePerCopper = TimeUtils.formatDuration((timeRequired / copper) * 1000)
                var copperLine = formattedLine
                if (config.visitorCopperPrice) copperLine += " §7(§6$pricePerCopper §7per)"
                if (config.visitorCopperTime) {
                    copperLine += if (timeRequired != -1L) " §7(§b$timePerCopper §7per)" else " §7(§cno speed data!§7)"
                }
                finalList.set(index, copperLine)
            }

            if (formattedLine.contains("Rewards")) {
                readingItemsNeeded = false
            }

            val (itemName, amount) = ItemUtils.readItemAmount(formattedLine) ?: continue
            val internalName = NEUItems.getInternalNameOrNull(itemName)?.replace("◆_", "") ?: continue
            val price = internalName.getPrice() * amount

            if (config.visitorShowPrice) {
                val format = NumberUtil.format(price)
                finalList[index] = "$formattedLine §7(§6$format§7)"
            }
            if (!readingItemsNeeded) continue
            val multiplier = NEUItems.getMultiplier(internalName)

            val rawName = multiplier.first.getItemNameOrNull()?.removeColor() ?: continue
            val cropType = getByNameOrNull(rawName) ?: continue

            val cropAmount = multiplier.second.toLong() * amount
            val formattedAmount = LorenzUtils.formatInteger(cropAmount)
            val formattedName = "§e$formattedAmount§7x ${cropType.cropName} "
            val formattedSpeed = cropType.getSpeed()?.let { speed ->
                timeRequired = cropAmount / speed
                val duration = TimeUtils.formatDuration(timeRequired * 1000)
                "in §b$duration"
            } ?: "§cno speed data!"
            if (config.visitorExactAmountAndTime) {
                finalList.add(index + 1, "§7- $formattedName($formattedSpeed§7)")
                offset++
            }
        }
        visitor.lastLore = finalList
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.visitorNeedsDisplay && config.visitorHighlightStatus == 3) return
        if (!event.isMod(10)) return

        if (GardenAPI.onBarnPlot && config.visitorHighlightStatus != 3) {
            checkVisitorsReady()
        }
    }

    @SubscribeEvent
    fun onInventoryClose(event: InventoryCloseEvent) {
        inVisitorInventory = false
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
        val removedVisitors = mutableListOf<String>()
        visitors.forEach {
            val name = it.key
            val time = System.currentTimeMillis() - LorenzUtils.lastWorldSwitch
            val removed = name !in visitorsInTab && time > 2_000
            if (removed) {
                logger.log("Removed old visitor: '$name'")
                removedVisitors.add(name)
            }
        }
        var dirty = false
        if (removedVisitors.isNotEmpty()) {
            visitors = visitors.editCopy {
                keys.removeIf { it in removedVisitors }
            }
            dirty = true
        }
        for (name in visitorsInTab) {
            if (!visitors.containsKey(name)) {
                addVisitor(name)
                dirty = true
            }
        }
        if (dirty) {
            updateDisplay()
        }
    }

    private fun addVisitor(name: String) {
        val visitor = Visitor(name, status = VisitorStatus.NEW)
        visitors = visitors.editCopy { this[name] = visitor }
        VisitorArrivalEvent(visitor).postAndCatch()

        logger.log("New visitor detected: '$name'")

        if (config.visitorNotificationTitle && System.currentTimeMillis() > LorenzUtils.lastWorldSwitch + 2_000) {
            LorenzUtils.sendTitle("§eNew Visitor", 5.seconds)
        }
        if (config.visitorNotificationChat) {
            val displayName = GardenVisitorColorNames.getColoredName(name)
            LorenzUtils.chat("§e[SkyHanni] $displayName §eis visiting your garden!")
        }

        if (System.currentTimeMillis() > LorenzUtils.lastWorldSwitch + 2_000) {
            if (name.removeColor().contains("Jerry")) {
                logger.log("Jerry!")
                ItemBlink.setBlink(NEUItems.getItemStackOrNull("JERRY;4"), 5_000)
            }
            if (name.removeColor().contains("Spaceman")) {
                logger.log("Spaceman!")
                ItemBlink.setBlink(NEUItems.getItemStackOrNull("DCTR_SPACE_HELM"), 5_000)
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
        if (config.visitorHypixelArrivedMessage && newVisitorArrivedMessage.matcher(event.message).matches()) {
            event.blockedReason = "new_visitor_arrived"
        }

        if (GardenAPI.inGarden() && config.visitorHideChat && hideVisitorMessage(event.message)) {
            event.blockedReason = "garden_visitor_message"
        }
    }

    private fun hideVisitorMessage(message: String) = visitorChatMessagePattern.matchMatcher(message) {
        val name = group("name")
        if (name == "Spaceman") return false
        if (name == "Beth") return false

        return visitors.keys.any { it.removeColor() == name }
    } ?: false

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

            if ((config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2) && entity is EntityLivingBase) {
                val color = visitor.status.color
                if (color != -1) {
                    RenderLivingEntityHelper.setEntityColor(
                        entity,
                        color
                    ) { config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2 }
                }
                // Haven't gotten either of the known effected visitors (Vex and Leo) so can't test for sure
                if (color == -1 || !GardenAPI.inGarden()) RenderLivingEntityHelper.removeEntityColor(entity)
            }
        }
    }

    private fun changeStatus(visitor: Visitor, newStatus: VisitorStatus, reason: String) {
        val old = visitor.status
        if (old == newStatus) return
        visitor.status = newStatus
        logger.log("Visitor status change for '${visitor.visitorName}': $old -> $newStatus ($reason)")
    }

    private fun findEntity(nameTag: EntityArmorStand, visitor: Visitor) {
        for (entity in EntityUtils.getAllEntities()) {
            if (entity is EntityArmorStand) continue
            if (entity.getLorenzVec().distanceIgnoreY(nameTag.getLorenzVec()) != 0.0) continue

            visitor.entityId = entity.entityId
            visitor.nameTagEntityId = nameTag.entityId
        }
    }

    private fun findNametag(visitorName: String): EntityArmorStand? {
        val foundVisitorNameTags = mutableListOf<EntityArmorStand>()
        for (entity in EntityUtils.getEntities<EntityArmorStand>()) {
            if (entity.name.removeColor() == visitorName) {
                foundVisitorNameTags.add(entity)
            }
        }

        if (visitorName in listOf("Jacob", "Anita")) {
            // Only detect jacob/anita npc if the "wrong" npc got found as well
            if (foundVisitorNameTags.size != 2) return null

            for (tag in foundVisitorNameTags.toMutableList()) {
                for (entity in EntityUtils.getEntities<EntityArmorStand>()) {
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
        if (config.visitorNeedsInBazaarAlley && LorenzUtils.skyBlockIsland == IslandType.HUB && LorenzUtils.skyBlockArea == "Bazaar Alley") {
            return true
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
        val items: MutableMap<NEUInternalName, Int> = mutableMapOf(),
    ) {

        var allRewards = listOf<NEUInternalName>()
        var lastLore = listOf<String>()
        fun getEntity(): Entity? = Minecraft.getMinecraft().theWorld.getEntityByID(entityId)

        fun getNameTagEntity(): Entity? = Minecraft.getMinecraft().theWorld.getEntityByID(nameTagEntityId)

        fun hasReward(): VisitorReward? {
            for (internalName in allRewards) {
                val reward = VisitorReward.getByInternalName(internalName) ?: continue

                if (config.visitorRewardWarning.drops.contains(reward.ordinal)) {
                    return reward
                }
            }

            return null
        }
    }

    enum class VisitorStatus(val displayName: String, val color: Int) {
        NEW("§eNew", LorenzColor.YELLOW.toColor().withAlpha(100)),
        WAITING("Waiting", -1),
        READY("§aItems Ready", LorenzColor.GREEN.toColor().withAlpha(80)),
        ACCEPTED("§7Accepted", LorenzColor.DARK_GRAY.toColor().withAlpha(80)),
        REFUSED("§cRefused", LorenzColor.RED.toColor().withAlpha(60)),
    }
}

