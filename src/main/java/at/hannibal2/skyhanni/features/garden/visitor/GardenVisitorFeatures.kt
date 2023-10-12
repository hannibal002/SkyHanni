package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.PacketEvent
import at.hannibal2.skyhanni.events.PreProfileSwitchEvent
import at.hannibal2.skyhanni.events.TabListLineRenderEvent
import at.hannibal2.skyhanni.events.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRenderEvent
import at.hannibal2.skyhanni.events.withAlpha
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
import at.hannibal2.skyhanni.utils.LorenzColor
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
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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
        if (config.visitorRewardWarning.showOverName) {
            visitor.hasReward()?.let { reward ->
                val name = reward.displayName

                event.parent.drawString(location.add(0.0, 2.73, 0.0), "§c!$name§c!")
            }
        }
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
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        val visitor = event.visitor
        val name = visitor.visitorName

        update()

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

    @SubscribeEvent
    fun onTabListText(event: TabListLineRenderEvent) {
        if (!GardenAPI.inGarden()) return
        if (!SkyHanniMod.feature.garden.visitorColoredName) return
        val text = event.text
        val replace = VisitorAPI.fromHypixelName(text)
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

