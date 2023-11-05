package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRenderEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorToolTipEvent
import at.hannibal2.skyhanni.features.bazaar.BazaarApi
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemName
import at.hannibal2.skyhanni.utils.ItemUtils.getItemNameOrNull
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import io.github.moulberry.notenoughupdates.util.MinecraftExecutor
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

private val config get() = VisitorAPI.config

class GardenVisitorFeatures {
    private var display = emptyList<List<Any>>()
    private val newVisitorArrivedMessage = ".* §r§ehas arrived on your §r§bGarden§r§e!".toPattern()
    private val copperPattern = " §8\\+§c(?<amount>.*) Copper".toPattern()
    private val gardenExperiencePattern = " §8\\+§2(?<amount>.*) §7Garden Experience".toPattern()
    private val visitorChatMessagePattern = "§e\\[NPC] (§.)?(?<name>.*)§f: §r.*".toPattern()

    private val logger = LorenzLogger("garden/visitors")
    private var lastFullPrice = 0.0

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onVisitorOpen(event: VisitorOpenEvent) {
        val visitor = event.visitor
        val offerItem = visitor.offer!!.offerItem

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

        readToolTip(visitor, offerItem)

        if (visitor.status == VisitorAPI.VisitorStatus.NEW) {
            val alreadyReady = offerItem.getLore().any { it == "§eClick to give!" } == true
            if (alreadyReady) {
                VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.READY, "inSacks")
                visitor.inSacks = true
                update()
            } else {
                VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.WAITING, "firstContact")
            }
            update()
        }
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay(): List<List<Any>> {
        val newDisplay = mutableListOf<List<Any>>()
        if (!config.needs.display) return newDisplay

        val requiredItems = mutableMapOf<NEUInternalName, Int>()
        val newVisitors = mutableListOf<String>()
        for ((visitorName, visitor) in VisitorAPI.getVisitorsMap()) {
            if (visitor.status == VisitorAPI.VisitorStatus.ACCEPTED || visitor.status == VisitorAPI.VisitorStatus.REFUSED) continue

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

                if (config.needs.showPrice) {
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

                if (config.needs.itemPreview) {
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

    @SubscribeEvent
    fun onVisitorRefused(event: VisitorRefusedEvent) {
        update()
        GardenVisitorDropStatistics.deniedVisitors += 1
        GardenVisitorDropStatistics.saveAndUpdate()
    }

    @SubscribeEvent
    fun onVisitorAccepted(event: VisitorAcceptedEvent) {
        VisitorAcceptEvent(event.visitor).postAndCatch()
        update()
        GardenVisitorDropStatistics.coinsSpent += round(lastFullPrice).toLong()
        GardenVisitorDropStatistics.lastAccept = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun onVisitorRender(event: VisitorRenderEvent) {
        val visitor = event.visitor
        val text = visitor.status.displayName
        val location = event.location
        event.parent.drawString(location.add(0.0, 2.23, 0.0), text)
        if (config.rewardWarning.showOverName) {
            visitor.hasReward()?.let { reward ->
                val name = reward.displayName

                event.parent.drawString(location.add(0.0, 2.73, 0.0), "§c!$name§c!")
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onVisitorTooltip(event: VisitorToolTipEvent) {
        if (event.itemStack.name != "§aAccept Offer") return

        val visitor = event.visitor
        val toolTip = event.toolTip
        toolTip.clear()

        if (visitor.lastLore.isEmpty()) {
            readToolTip(visitor, event.itemStack)
            LorenzUtils.chat("§e[SkyHanni] Reloaded the visitor data of that inventory, this should not happen.")
        }

        toolTip.addAll(visitor.lastLore)
    }

    private fun readToolTip(visitor: VisitorAPI.Visitor, itemStack: ItemStack?) {
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
                    if (config.rewardWarning.notifyInChat) {
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
            if (config.inventory.experiencePrice) {
                gardenExperiencePattern.matchMatcher(formattedLine) {
                    val gardenExp = group("amount").replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / gardenExp).toInt())
                    finalList.set(index, "$formattedLine §7(§6$pricePerCopper §7per)")
                }
            }

            copperPattern.matchMatcher(formattedLine) {
                val copper = group("amount").replace(",", "").toInt()
                val pricePerCopper = NumberUtil.format((totalPrice / copper).toInt())
                visitor.pricePerCopper = (totalPrice / copper).toInt()
                val timePerCopper = TimeUtils.formatDuration((timeRequired / copper) * 1000)
                var copperLine = formattedLine
                if (config.inventory.copperPrice) copperLine += " §7(§6$pricePerCopper §7per)"
                if (config.inventory.copperTime) {
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

            if (config.inventory.showPrice) {
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
            if (config.inventory.exactAmountAndTime) {
                finalList.add(index + 1, "§7- $formattedName($formattedSpeed§7)")
                offset++
            }
        }
        visitor.lastLore = finalList
    }

    @SubscribeEvent
    fun onTick(event: LorenzTickEvent) {
        if (!GardenAPI.inGarden()) return
        if (!config.needs.display && config.highlightStatus == 3) return
        if (!event.isMod(10)) return

        if (GardenAPI.onBarnPlot && config.highlightStatus != 3) {
            checkVisitorsReady()
        }
    }

    @SubscribeEvent
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        val visitor = event.visitor
        val name = visitor.visitorName

        update()

        logger.log("New visitor detected: '$name'")

        if (config.notificationTitle && System.currentTimeMillis() > LorenzUtils.lastWorldSwitch + 2_000) {
            LorenzUtils.sendTitle("§eNew Visitor", 5.seconds)
        }
        if (config.notificationChat) {
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

    @SubscribeEvent
    fun onChatMessage(event: LorenzChatEvent) {
        if (config.hypixelArrivedMessage && newVisitorArrivedMessage.matcher(event.message).matches()) {
            event.blockedReason = "new_visitor_arrived"
        }

        if (GardenAPI.inGarden() && config.hideChat && hideVisitorMessage(event.message)) {
            event.blockedReason = "garden_visitor_message"
        }
    }

    private fun hideVisitorMessage(message: String) = visitorChatMessagePattern.matchMatcher(message) {
        val name = group("name")
        if (name == "Spaceman") return false
        if (name == "Beth") return false

        return VisitorAPI.getVisitorsMap().keys.any { it.removeColor() == name }
    } ?: false

    private fun update() {
        checkVisitorsReady()
        updateDisplay()
    }

    private fun checkVisitorsReady() {
        for (visitor in VisitorAPI.getVisitors()) {
            val visitorName = visitor.visitorName
            val entity = visitor.getEntity()
            if (entity == null) {
                findNametag(visitorName.removeColor())?.let {
                    findEntity(it, visitor)
                }
            }

            if (!visitor.inSacks) {
                val status = visitor.status
                if (status == VisitorAPI.VisitorStatus.WAITING || status == VisitorAPI.VisitorStatus.READY) {
                    val newStatus = if (hasItemsInInventory(visitor)) VisitorAPI.VisitorStatus.READY else VisitorAPI.VisitorStatus.WAITING
                    VisitorAPI.changeStatus(visitor, newStatus, "hasItemsInInventory")
                }
            }

            if ((config.highlightStatus == 0 || config.highlightStatus == 2) && entity is EntityLivingBase) {
                val color = visitor.status.color
                if (color != -1) {
                    RenderLivingEntityHelper.setEntityColor(
                        entity,
                        color
                    ) { config.highlightStatus == 0 || config.highlightStatus == 2 }
                }
                // Haven't gotten either of the known effected visitors (Vex and Leo) so can't test for sure
                if (color == -1 || !GardenAPI.inGarden()) RenderLivingEntityHelper.removeEntityColor(entity)
            }
        }
    }

    private fun findEntity(nameTag: EntityArmorStand, visitor: VisitorAPI.Visitor) {
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

    private fun hasItemsInInventory(visitor: VisitorAPI.Visitor): Boolean {
        var ready = true
        for ((internalName, need) in visitor.items) {
            val having = InventoryUtils.countItemsInLowerInventory { it.getInternalName() == internalName }
            if (having < need) {
                ready = false
            }
        }
        return ready
    }

    @SubscribeEvent
    fun onRenderInSigns(event: DrawScreenEvent.Post) {
        if (!GardenAPI.inGarden()) return
        if (!config.needs.display) return
        val gui = event.gui
        if (gui !is GuiEditSign) return

        if (config.needs.onlyWhenClose && !GardenAPI.onBarnPlot) return

        if (!hideExtraGuis()) {
            config.needs.pos.renderStringsAndItems(display, posLabel = "Visitor Items Needed")
        }
    }

    private fun hideExtraGuis() = GardenAPI.hideExtraGuis() && !VisitorAPI.inInventory

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!config.needs.display) return

        if (showGui()) {
            config.needs.pos.renderStringsAndItems(display, posLabel = "Visitor Items Needed")
        }
    }

    private fun showGui(): Boolean {
        if (config.needs.inBazaarAlley && LorenzUtils.skyBlockIsland == IslandType.HUB && LorenzUtils.skyBlockArea == "Bazaar Alley") {
            return true
        }

        if (hideExtraGuis()) return false
        if (GardenAPI.inGarden()) {
            if (GardenAPI.onBarnPlot) return true
            if (!config.needs.onlyWhenClose) return true
        }
        return false
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onRenderLiving(event: RenderLivingEvent.Specials.Pre<EntityLivingBase>) {
        if (!config.coloredName) return
        val entity = event.entity
        val entityId = entity.entityId
        for (visitor in VisitorAPI.getVisitors()) {
            if (visitor.nameTagEntityId == entityId) {
                entity.customNameTag = GardenVisitorColorNames.getColoredName(entity.name)
            }
        }
    }

    @SubscribeEvent
    fun onConfigFix(event: ConfigUpdaterMigrator.ConfigFixEvent) {
        event.move(3, "garden.visitorNeedsDisplay", "garden.visitors.needs.display")
        event.move(3, "garden.visitorNeedsPos", "garden.visitors.needs.pos")
        event.move(3, "garden.visitorNeedsOnlyWhenClose", "garden.visitors.needs.onlyWhenClose")
        event.move(3, "garden.visitorNeedsInBazaarAlley", "garden.visitors.needs.inBazaarAlley")
        event.move(3, "garden.visitorNeedsShowPrice", "garden.visitors.needs.showPrice")
        event.move(3, "garden.visitorItemPreview", "garden.visitors.needs.itemPreview")
        event.move(3, "garden.visitorShowPrice", "garden.visitors.inventory.showPrice")
        event.move(3, "garden.visitorExactAmountAndTime", "garden.visitors.inventory.exactAmountAndTime")
        event.move(3, "garden.visitorCopperPrice", "garden.visitors.inventory.copperPrice")
        event.move(3, "garden.visitorCopperTime", "garden.visitors.inventory.copperTime")
        event.move(3, "garden.visitorExperiencePrice", "garden.visitors.inventory.experiencePrice")
        event.move(3, "garden.visitorRewardWarning.notifyInChat", "garden.visitors.rewardWarning.notifyInChat")
        event.move(3, "garden.visitorRewardWarning.showOverName", "garden.visitors.rewardWarning.showOverName")
        event.move(3, "garden.visitorRewardWarning.preventRefusing", "garden.visitors.rewardWarning.preventRefusing")
        event.move(3, "garden.visitorRewardWarning.bypassKey", "garden.visitors.rewardWarning.bypassKey")
        event.move(3, "garden.visitorRewardWarning.drops", "garden.visitors.rewardWarning.drops")
        event.move(3, "garden.visitorNotificationChat", "garden.visitors.notificationChat")
        event.move(3, "garden.visitorNotificationTitle", "garden.visitors.notificationTitle")
        event.move(3, "garden.visitorHighlightStatus", "garden.visitors.highlightStatus")
        event.move(3, "garden.visitorColoredName", "garden.visitors.coloredName")
        event.move(3, "garden.visitorHypixelArrivedMessage", "garden.visitors.hypixelArrivedMessage")
        event.move(3, "garden.visitorHideChat", "garden.visitors.hideChat")
    }

}

