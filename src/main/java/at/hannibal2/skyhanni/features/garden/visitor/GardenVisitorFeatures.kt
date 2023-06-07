package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.SkyHanniMod
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.TitleUtils
import at.hannibal2.skyhanni.events.*
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
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
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
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
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.entity.player.ItemTooltipEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.round

private val config get() = SkyHanniMod.feature.garden

class GardenVisitorFeatures {
    private val visitors = mutableMapOf<String, Visitor>()
    private var display = listOf<List<Any>>()
    private var lastClickedNpc = 0
    private var tick = 0
    private val newVisitorArrivedMessage = ".* §r§ehas arrived on your §r§bGarden§r§e!".toPattern()
    private val copperPattern = " §8\\+§c(?<amount>.*) Copper".toPattern()
    private val gardenExperiencePattern = " §8\\+§2(?<amount>.*) §7Garden Experience".toPattern()
    private val visitorChatMessagePattern = "§e\\[NPC] (§.)?(?<name>.*)§f: §r.*".toPattern()

    private val logger = LorenzLogger("garden/visitors")
    private var price = 0.0

    companion object {
        var inVisitorInventory = false
    }

    @SubscribeEvent
    fun onPreProfileSwitch(event: PreProfileSwitchEvent) {
        display = emptyList()
        visitors.clear()
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

        readReward(offerItem)?.let { reward ->
            if (visitor.reward == reward) return@let
            visitor.reward = reward
            visitor.hasReward()?.let {
                if (config.visitorRewardWarning.notifyInChat) {
                    LorenzUtils.chat("§e[SkyHanni] Found Visitor Reward ${it.displayName}§e!")
                }
            }
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

    private fun readReward(offerItem: ItemStack): VisitorReward? {
        for (line in offerItem.getLore()) {
            for (reward in VisitorReward.values()) {
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
            var totalPrice = 0.0
            newDisplay.addAsSingletonList("§7Visitor items needed:")
            for ((internalName, amount) in requiredItems) {
                val name = NEUItems.getItemStack(internalName).name!!
                val itemStack = NEUItems.getItemStack(internalName)

                val list = mutableListOf<Any>()
                list.add(" §7- ")
                list.add(itemStack)

                list.add(Renderable.optionalLink("$name §ex${amount.addSeparators()}", {
                    if (Minecraft.getMinecraft().currentScreen is GuiEditSign) {
                        LorenzUtils.setTextIntoSign("$amount")
                    } else if (!NEUItems.neuHasFocus() && !LorenzUtils.noTradeMode) {
                        LorenzUtils.sendCommandToServer("bz ${name.removeColor()}")
                        OSUtils.copyToClipboard("$amount")
                    }
                }) { GardenAPI.inGarden() && !NEUItems.neuHasFocus() })

                if (config.visitorNeedsShowPrice) {
                    val price = NEUItems.getPrice(internalName) * amount
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

            visitor.hasReward()?.let {
                if (config.visitorRewardWarning.preventRefusing) {
                    event.isCanceled = true
                    LorenzUtils.chat("§e[SkyHanni] §cBlocked refusing visitor ${visitor.visitorName} §7(${it.displayName}§7)")
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
        if (event.slotId == 29) {
            if (event.slot.stack?.getLore()?.any { it == "§eClick to give!" } == true) {
                changeStatus(visitor, VisitorStatus.ACCEPTED, "accepted")
                update()
                GardenVisitorDropStatistics.coinsSpent += round(price).toLong()
                GardenVisitorDropStatistics.lastAccept = System.currentTimeMillis()
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
                    if (config.visitorRewardWarning.showOverName) {
                        visitor.hasReward()?.let { reward ->
                            val name = reward.displayName
                            event.drawString(it.add(0.0, 2.73, 0.0), "§c!$name§c!")
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onTooltip(event: ItemTooltipEvent) {
        if (!GardenAPI.inBarn) return
        if (!inVisitorInventory) return
        if (event.itemStack.name != "§aAccept Offer") return

        var totalPrice = 0.0
        var timeRequired = -1L
        val iterator = event.toolTip.listIterator()
        for (line in iterator) {
            val formattedLine = line.substring(4)
            if (totalPrice == 0.0) {
                val (cropName, amount) = ItemUtils.readItemAmount(formattedLine)
                if (cropName != null) {
                    val internalName = NEUItems.getInternalNameOrNull(cropName) ?: continue
                    price = NEUItems.getPrice(internalName) * amount
                    totalPrice = price
                    if (config.visitorShowPrice) {
                        val format = NumberUtil.format(price)
                        iterator.set("$formattedLine §7(§6$format§7)")
                    }

                    val multiplier = NEUItems.getMultiplier(internalName)
                    val rawName = NEUItems.getItemStack(multiplier.first).name?.removeColor() ?: continue
                    getByNameOrNull(rawName)?.let {
                        val cropAmount = multiplier.second.toLong() * amount
                        val formattedAmount = LorenzUtils.formatInteger(cropAmount)
                        val formattedName = "§e$formattedAmount§7x ${it.cropName} "
                        val formattedSpeed = it.getSpeed()?.let { speed ->
                            timeRequired = cropAmount / speed
                            val duration = TimeUtils.formatDuration(timeRequired * 1000)
                            "in §b$duration"
                        } ?: "§cno speed data!"
                        if (config.visitorExactAmountAndTime) {
                            iterator.add("§7- $formattedName($formattedSpeed§7)")
                        }
                    }
                }
            }

            if (config.visitorExperiencePrice) {
                gardenExperiencePattern.matchMatcher(formattedLine) {
                    val gardenExp = group("amount").replace(",", "").toInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / gardenExp).toInt())
                    iterator.set("$formattedLine §7(§6$pricePerCopper §7per)")
                }
            }

            copperPattern.matchMatcher(formattedLine) {
                val copper = group("amount").replace(",", "").toInt()
                val pricePerCopper = NumberUtil.format((totalPrice / copper).toInt())
                val timePerCopper = TimeUtils.formatDuration((timeRequired/copper) * 1000)
                var copperLine = formattedLine
                if (config.visitorCopperPrice) copperLine += " §7(§6$pricePerCopper §7per)"
                if (config.visitorCopperTime) {
                    copperLine += if (timeRequired != -1L) " §7(§b$timePerCopper §7per)" else " §7(§cno speed data!§7)"
                }
                iterator.set(copperLine)
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
        val visitor = Visitor(name, status = VisitorStatus.NEW)
        visitors[name] = visitor
        VisitorArrivalEvent(visitor).postAndCatch()

        logger.log("New visitor detected: '$name'")

        if (config.visitorNotificationTitle && System.currentTimeMillis() > LorenzUtils.lastWorldSwitch + 2_000) {
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
        if (config.visitorHypixelArrivedMessage) {
            if (newVisitorArrivedMessage.matcher(event.message).matches()) {
                event.blockedReason = "new_visitor_arrived"
            }
        }

        if (GardenAPI.inGarden()) {
            if (config.visitorHideChat) {
                if (hideVisitorMessage(event.message)) {
                    event.blockedReason = "garden_visitor_message"
                }
            }
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

            if (config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2) {
                if (entity is EntityLivingBase) {
                    val color = visitor.status.color
                    if (color != -1) {
                        RenderLivingEntityHelper.setEntityColor(
                            entity,
                            color
                        ) { config.visitorHighlightStatus == 0 || config.visitorHighlightStatus == 2 }
                    }
                    if (color == -1 || !GardenAPI.inGarden()) RenderLivingEntityHelper.removeEntityColor(entity) // Have not gotten either of the known effected visitors (Vex and Leo) so cannot test for sure
                }
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
        var reward: VisitorReward? = null,
        val items: MutableMap<String, Int> = mutableMapOf(),
    ) {
        fun getEntity(): Entity? = Minecraft.getMinecraft().theWorld.getEntityByID(entityId)

        fun getNameTagEntity(): Entity? = Minecraft.getMinecraft().theWorld.getEntityByID(nameTagEntityId)

        fun hasReward() = reward?.let { if (config.visitorRewardWarning.drops.contains(it.ordinal)) it else null }
    }

    enum class VisitorStatus(val displayName: String, val color: Int) {
        NEW("§eNew", LorenzColor.YELLOW.toColor().withAlpha(100)),
        WAITING("Waiting", -1),
        READY("§aItems Ready", LorenzColor.GREEN.toColor().withAlpha(80)),
        ACCEPTED("§7Accepted", LorenzColor.DARK_GRAY.toColor().withAlpha(80)),
        REFUSED("§cRefused", LorenzColor.RED.toColor().withAlpha(60)),
    }
}

