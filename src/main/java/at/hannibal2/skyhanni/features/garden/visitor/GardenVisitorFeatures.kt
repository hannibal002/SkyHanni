package at.hannibal2.skyhanni.features.garden.visitor

import at.hannibal2.skyhanni.config.ConfigUpdaterMigrator
import at.hannibal2.skyhanni.config.features.garden.visitor.VisitorConfig.HighlightMode
import at.hannibal2.skyhanni.data.IslandType
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacks
import at.hannibal2.skyhanni.data.SackAPI.getAmountInSacksOrNull
import at.hannibal2.skyhanni.events.DebugDataCollectEvent
import at.hannibal2.skyhanni.events.GuiRenderEvent
import at.hannibal2.skyhanni.events.LorenzChatEvent
import at.hannibal2.skyhanni.events.LorenzTickEvent
import at.hannibal2.skyhanni.events.OwnInventoryItemUpdateEvent
import at.hannibal2.skyhanni.events.ProfileJoinEvent
import at.hannibal2.skyhanni.events.SackDataUpdateEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorAcceptedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorArrivalEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorOpenEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRefusedEvent
import at.hannibal2.skyhanni.events.garden.visitor.VisitorRenderEvent
import at.hannibal2.skyhanni.features.garden.CropType.Companion.getByNameOrNull
import at.hannibal2.skyhanni.features.garden.GardenAPI
import at.hannibal2.skyhanni.features.garden.farming.GardenCropSpeed.getSpeed
import at.hannibal2.skyhanni.features.inventory.bazaar.BazaarApi
import at.hannibal2.skyhanni.mixins.hooks.RenderLivingEntityHelper
import at.hannibal2.skyhanni.test.command.ErrorManager
import at.hannibal2.skyhanni.utils.ChatUtils
import at.hannibal2.skyhanni.utils.CollectionUtils.addAsSingletonList
import at.hannibal2.skyhanni.utils.ConfigUtils
import at.hannibal2.skyhanni.utils.EntityUtils
import at.hannibal2.skyhanni.utils.InventoryUtils.getAmountInInventory
import at.hannibal2.skyhanni.utils.ItemBlink
import at.hannibal2.skyhanni.utils.ItemUtils
import at.hannibal2.skyhanni.utils.ItemUtils.getInternalName
import at.hannibal2.skyhanni.utils.ItemUtils.getLore
import at.hannibal2.skyhanni.utils.ItemUtils.itemName
import at.hannibal2.skyhanni.utils.ItemUtils.itemNameWithoutColor
import at.hannibal2.skyhanni.utils.ItemUtils.name
import at.hannibal2.skyhanni.utils.LorenzLogger
import at.hannibal2.skyhanni.utils.LorenzUtils
import at.hannibal2.skyhanni.utils.LorenzUtils.isInIsland
import at.hannibal2.skyhanni.utils.NEUInternalName
import at.hannibal2.skyhanni.utils.NEUItems
import at.hannibal2.skyhanni.utils.NEUItems.getItemStack
import at.hannibal2.skyhanni.utils.NEUItems.getPrice
import at.hannibal2.skyhanni.utils.NumberUtil
import at.hannibal2.skyhanni.utils.NumberUtil.addSeparators
import at.hannibal2.skyhanni.utils.NumberUtil.formatInt
import at.hannibal2.skyhanni.utils.RenderUtils.drawString
import at.hannibal2.skyhanni.utils.RenderUtils.renderStringsAndItems
import at.hannibal2.skyhanni.utils.StringUtils.matchMatcher
import at.hannibal2.skyhanni.utils.StringUtils.removeColor
import at.hannibal2.skyhanni.utils.TimeUtils.format
import at.hannibal2.skyhanni.utils.getLorenzVec
import at.hannibal2.skyhanni.utils.renderables.Renderable
import at.hannibal2.skyhanni.utils.repopatterns.RepoPattern
import com.google.gson.JsonArray
import com.google.gson.JsonPrimitive
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiEditSign
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.round
import kotlin.time.Duration.Companion.seconds

private val config get() = VisitorAPI.config

object GardenVisitorFeatures {

    private var display = emptyList<List<Any>>()

    private val patternGroup = RepoPattern.group("garden.visitor")
    private val visitorArrivePattern by patternGroup.pattern(
        "visitorarrive",
        ".* §r§ehas arrived on your §r§bGarden§r§e!"
    )
    private val copperPattern by patternGroup.pattern(
        "copper",
        " §8\\+§c(?<amount>.*) Copper"
    )
    private val gardenExperiencePattern by patternGroup.pattern(
        "gardenexperience",
        " §8\\+§2(?<amount>.*) §7Garden Experience"
    )
    private val visitorChatMessagePattern by patternGroup.pattern(
        "visitorchat",
        "§e\\[NPC] (?<color>§.)?(?<name>.*)§f: §r.*"
    )
    private val partialAcceptedPattern by patternGroup.pattern(
        "partialaccepted",
        "§aYou gave some of the required items!"
    )

    private val logger = LorenzLogger("garden/visitors")
    private var lastFullPrice = 0.0

    @SubscribeEvent
    fun onProfileJoin(event: ProfileJoinEvent) {
        display = emptyList()
    }

    @SubscribeEvent
    fun onVisitorOpen(event: VisitorOpenEvent) {
        val visitor = event.visitor
        val offerItem = visitor.offer!!.offerItem

        val lore = offerItem.getLore()
        for (line in lore) {
            if (line == "§7Items Required:") continue
            if (line.isEmpty()) break

            val pair = ItemUtils.readItemAmount(line)
            if (pair == null) {
                ErrorManager.logErrorStateWithData(
                    "Could not read Shopping List in Visitor Inventory", "ItemUtils.readItemAmount returns null",
                    "line" to line,
                    "offerItem" to offerItem,
                    "lore" to lore,
                    "visitor" to visitor
                )
                continue
            }
            val (itemName, amount) = pair
            val internalName = NEUInternalName.fromItemName(itemName)
            visitor.shoppingList[internalName] = amount
        }

        readToolTip(visitor, offerItem)

        val alreadyReady = offerItem.getLore().any { it == "§eClick to give!" }
        if (alreadyReady) {
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.READY, "tooltipClickToGive")
        } else {
            VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.WAITING, "tooltipMissingItems")
        }
        update()
    }

    private fun updateDisplay() {
        display = drawDisplay()
    }

    private fun drawDisplay() = buildList {
        if (!config.shoppingList.display) return@buildList
        val (shoppingList, newVisitors) = prepareDrawingData()

        drawShoppingList(shoppingList)
        drawVisitors(newVisitors, shoppingList)
    }

    private fun prepareDrawingData(): Pair<MutableMap<NEUInternalName, Int>, MutableList<String>> {
        val globalShoppingList = mutableMapOf<NEUInternalName, Int>()
        val newVisitors = mutableListOf<String>()
        for ((visitorName, visitor) in VisitorAPI.getVisitorsMap()) {
            if (visitor.status == VisitorAPI.VisitorStatus.ACCEPTED || visitor.status == VisitorAPI.VisitorStatus.REFUSED) continue

            val shoppingList = visitor.shoppingList
            if (shoppingList.isEmpty()) {
                newVisitors.add(visitorName)
            }
            for ((internalName, amount) in shoppingList) {
                val old = globalShoppingList.getOrDefault(internalName, 0)
                globalShoppingList[internalName] = old + amount
            }
        }
        return globalShoppingList to newVisitors
    }

    private fun MutableList<List<Any>>.drawShoppingList(shoppingList: MutableMap<NEUInternalName, Int>) {
        if (shoppingList.isNotEmpty()) {
            var totalPrice = 0.0
            addAsSingletonList("§7Visitor Shopping List:")
            for ((internalName, amount) in shoppingList) {
                val name = internalName.itemName
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

                if (config.shoppingList.showPrice) {
                    val price = internalName.getPrice() * amount
                    totalPrice += price
                    val format = NumberUtil.format(price)
                    list.add(" §7(§6$format§7)")
                }

                if (config.shoppingList.showSackCount) {
                    internalName.getAmountInSacksOrNull()?.let {
                        val textColour = if (it >= amount) "a" else "e"
                        list.add(" §7(§${textColour}x${it.addSeparators()} §7in sacks)")
                    }
                }

                add(list)
            }
            if (totalPrice > 0) {
                val format = NumberUtil.format(totalPrice)
                this[0] = listOf("§7Visitor Shopping List: §7(§6$format§7)")
            }
        }
    }

    private fun MutableList<List<Any>>.drawVisitors(
        newVisitors: MutableList<String>,
        shoppingList: MutableMap<NEUInternalName, Int>,
    ) {
        if (newVisitors.isNotEmpty()) {
            if (shoppingList.isNotEmpty()) {
                addAsSingletonList("")
            }
            val amount = newVisitors.size
            val visitorLabel = if (amount == 1) "visitor" else "visitors"
            addAsSingletonList("§e$amount §7new $visitorLabel:")
            for (visitor in newVisitors) {
                val displayName = GardenVisitorColorNames.getColoredName(visitor)

                val list = mutableListOf<Any>()
                list.add(" §7- $displayName")

                if (config.shoppingList.itemPreview) {
                    val items = GardenVisitorColorNames.visitorItems[visitor.removeColor()]
                    if (items == null) {
                        val text = "Visitor '$visitor' has no items in repo!"
                        logger.log(text)
                        ChatUtils.debug(text)
                        list.add(" §7(§c?§7)")
                        continue
                    }
                    list.add(" ")
                    if (items.isEmpty()) {
                        list.add("§7(§fAny§7)")
                    } else {
                        for (item in items) {
                            list.add(NEUInternalName.fromItemName(item).getItemStack())
                        }
                    }
                }

                add(list)
            }
        }
    }

    @SubscribeEvent
    fun onOwnInventoryItemUpdate(event: OwnInventoryItemUpdateEvent) {
        if (GardenAPI.onBarnPlot) {
            update()
        }
    }

    @SubscribeEvent
    fun onSackUpdate(event: SackDataUpdateEvent) {
        update()
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
        event.parent.drawString(location.add(y = 2.23), text)
        if (config.rewardWarning.showOverName) {
            visitor.hasReward()?.let { reward ->
                val name = reward.displayName

                event.parent.drawString(location.add(y = 2.73), "§c!$name§c!")
            }
        }
    }

    fun onTooltip(visitor: VisitorAPI.Visitor, itemStack: ItemStack, toolTip: MutableList<String>) {
        if (itemStack.name != "§aAccept Offer") return

        toolTip.clear()

        if (visitor.lastLore.isEmpty()) {
            readToolTip(visitor, itemStack)
        }

        toolTip.addAll(visitor.lastLore)
    }

    private fun readToolTip(visitor: VisitorAPI.Visitor, itemStack: ItemStack?) {
        val stack = itemStack ?: error("Accept offer item not found for visitor ${visitor.visitorName}")
        var totalPrice = 0.0
        var farmingTimeRequired = 0.seconds
        var readingShoppingList = true
        lastFullPrice = 0.0
        val foundRewards = mutableListOf<NEUInternalName>()

        for (formattedLine in stack.getLore()) {
            if (formattedLine.contains("Rewards")) {
                readingShoppingList = false
            }

            val (itemName, amount) = ItemUtils.readItemAmount(formattedLine) ?: continue
            val internalName = NEUInternalName.fromItemNameOrNull(itemName)?.replace("◆_", "") ?: continue

            // Ignoring custom NEU items like copper
            if (internalName.startsWith("SKYBLOCK_")) continue
            val price = internalName.getPrice() * amount

            if (readingShoppingList) {
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
                        ChatUtils.chat("Found Visitor Reward ${reward.displayName}§e!")
                    }
                }
            }
        }

        readingShoppingList = true
        val finalList = stack.getLore().toMutableList()
        var offset = 0
        for ((i, formattedLine) in finalList.toMutableList().withIndex()) {
            val index = i + offset
            if (config.inventory.experiencePrice) {
                gardenExperiencePattern.matchMatcher(formattedLine) {
                    val gardenExp = group("amount").formatInt()
                    val pricePerCopper = NumberUtil.format((totalPrice / gardenExp).toInt())
                    finalList.set(index, "$formattedLine §7(§6$pricePerCopper §7per)")
                }
            }

            copperPattern.matchMatcher(formattedLine) {
                val copper = group("amount").formatInt()
                val pricePerCopper = NumberUtil.format((totalPrice / copper).toInt())
                val timePerCopper = (farmingTimeRequired / copper).format()
                var copperLine = formattedLine
                if (config.inventory.copperPrice) copperLine += " §7(§6$pricePerCopper §7per)"
                if (config.inventory.copperTime) {
                    copperLine += if (farmingTimeRequired != 0.seconds) " §7(§b$timePerCopper §7per)" else " §7(§cno speed data!§7)"
                }
                finalList.set(index, copperLine)
            }

            if (formattedLine.contains("Rewards")) {
                readingShoppingList = false
            }

            val (itemName, amount) = ItemUtils.readItemAmount(formattedLine) ?: continue
            val internalName = NEUInternalName.fromItemNameOrNull(itemName)?.replace("◆_", "") ?: continue

            // Ignoring custom NEU items like copper
            if (internalName.startsWith("SKYBLOCK_")) continue
            val price = internalName.getPrice() * amount

            if (config.inventory.showPrice) {
                val format = NumberUtil.format(price)
                finalList[index] = "$formattedLine §7(§6$format§7)"
            }
            if (!readingShoppingList) continue
            val multiplier = NEUItems.getMultiplier(internalName)

            val rawName = multiplier.first.itemNameWithoutColor
            val cropType = getByNameOrNull(rawName) ?: continue

            val cropAmount = multiplier.second.toLong() * amount
            val formattedName = "§e${cropAmount.addSeparators()}§7x ${cropType.cropName} "
            val formattedSpeed = cropType.getSpeed()?.let { speed ->
                val duration = (cropAmount / speed).seconds
                farmingTimeRequired += duration
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
        if (!config.shoppingList.display && config.highlightStatus == HighlightMode.DISABLED) return
        if (!event.isMod(10)) return

        if (GardenAPI.onBarnPlot && config.highlightStatus != HighlightMode.DISABLED) {
            checkVisitorsReady()
        }
    }

    @SubscribeEvent
    fun onVisitorArrival(event: VisitorArrivalEvent) {
        val visitor = event.visitor
        val name = visitor.visitorName

        update()

        logger.log("New visitor detected: '$name'")
        val recentWorldSwitch = LorenzUtils.lastWorldSwitch.passedSince() < 2.seconds

        if (config.notificationTitle && !recentWorldSwitch) {
            LorenzUtils.sendTitle("§eNew Visitor", 5.seconds)
        }
        if (config.notificationChat) {
            val displayName = GardenVisitorColorNames.getColoredName(name)
            ChatUtils.chat("$displayName §eis visiting your garden!")
        }

        if (!recentWorldSwitch) {
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
        if (config.hypixelArrivedMessage && visitorArrivePattern.matcher(event.message).matches()) {
            event.blockedReason = "new_visitor_arrived"
        }

        if (GardenAPI.inGarden() && config.hideChat && hideVisitorMessage(event.message)) {
            event.blockedReason = "garden_visitor_message"
        }

        if (config.shoppingList.display) {
            partialAcceptedPattern.matchMatcher(event.message) {
                ChatUtils.chat("Talk to the visitor again to update the number of items needed!")
            }
        }
    }

    private fun hideVisitorMessage(message: String) = visitorChatMessagePattern.matchMatcher(message) {
        val color = group("color")
        if (color == null || color == "§e") return false // Non-visitor NPC, probably Jacob

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
                NPCVisitorFix.findNametag(visitorName.removeColor())?.let {
                    findEntity(it, visitor)
                }
            }

            if (visitor.status in setOf(VisitorAPI.VisitorStatus.WAITING, VisitorAPI.VisitorStatus.READY)) {
                if (hasItems(visitor)) {
                    VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.READY, "hasItems")
                } else {
                    VisitorAPI.changeStatus(visitor, VisitorAPI.VisitorStatus.WAITING, "noLongerHasItems")
                }
            }

            if ((config.highlightStatus == HighlightMode.COLOR || config.highlightStatus == HighlightMode.BOTH) && entity is EntityLivingBase) {
                val color = visitor.status.color
                if (color != -1) {
                    RenderLivingEntityHelper.setEntityColor(
                        entity,
                        color
                    ) { config.highlightStatus == HighlightMode.COLOR || config.highlightStatus == HighlightMode.BOTH }
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

    private fun hasItems(visitor: VisitorAPI.Visitor): Boolean {
        var ready = true
        for ((internalName, required) in visitor.shoppingList) {
            val having = internalName.getAmountInInventory() + internalName.getAmountInSacks()
            if (having < required) {
                ready = false
            }
        }
        return ready
    }

    @SubscribeEvent
    fun onRenderInSigns(event: DrawScreenEvent.Post) {
        if (!GardenAPI.inGarden()) return
        if (!config.shoppingList.display) return
        val gui = event.gui
        if (gui !is GuiEditSign) return

        if (config.shoppingList.onlyWhenClose && !GardenAPI.onBarnPlot) return

        if (!hideExtraGuis()) {
            config.shoppingList.pos.renderStringsAndItems(display, posLabel = "Visitor Shopping List")
        }
    }

    private fun hideExtraGuis() = GardenAPI.hideExtraGuis() && !VisitorAPI.inInventory

    @SubscribeEvent
    fun onRenderOverlay(event: GuiRenderEvent) {
        if (!config.shoppingList.display) return

        if (showGui()) {
            config.shoppingList.pos.renderStringsAndItems(display, posLabel = "Visitor Shopping List")
        }
    }

    private fun showGui(): Boolean {
        if (IslandType.HUB.isInIsland()) {
            if (config.shoppingList.inBazaarAlley && LorenzUtils.skyBlockArea == "Bazaar Alley") {
                return true
            }
            if (config.shoppingList.inFarmingAreas && LorenzUtils.skyBlockArea == "Farm") {
                return true
            }
        }
        if (config.shoppingList.inFarmingAreas && IslandType.THE_FARMING_ISLANDS.isInIsland()) return true
        if (hideExtraGuis()) return false
        if (GardenAPI.inGarden()) {
            if (GardenAPI.onBarnPlot) return true
            if (!config.shoppingList.onlyWhenClose) return true
        }
        return false
    }

    @SubscribeEvent
    fun onDebugDataCollect(event: DebugDataCollectEvent) {
        event.title("Garden Visitor Stats")

        if (!GardenAPI.inGarden()) {
            event.addIrrelevant("not in garden")
            return
        }

        event.addData {
            val visitors = VisitorAPI.getVisitors()

            add("visitors: ${visitors.size}")

            for (visitor in visitors) {
                add(" ")
                add("visitorName: '${visitor.visitorName}'")
                add("status: '${visitor.status}'")
                if (visitor.shoppingList.isNotEmpty()) {
                    add("shoppingList: '${visitor.shoppingList}'")
                }
                visitor.offer?.offerItem?.getInternalName()?.let {
                    add("offer: '${it}'")
                }
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
        event.move(
            3,
            "garden.visitorRewardWarning.preventRefusing",
            "garden.visitors.rewardWarning.preventRefusing"
        )
        event.move(3, "garden.visitorRewardWarning.bypassKey", "garden.visitors.rewardWarning.bypassKey")
        event.move(3, "garden.visitorRewardWarning.drops", "garden.visitors.rewardWarning.drops")
        event.move(3, "garden.visitorNotificationChat", "garden.visitors.notificationChat")
        event.move(3, "garden.visitorNotificationTitle", "garden.visitors.notificationTitle")
        event.move(3, "garden.visitorHighlightStatus", "garden.visitors.highlightStatus")
        event.move(3, "garden.visitorColoredName", "garden.visitors.coloredName")
        event.move(3, "garden.visitorHypixelArrivedMessage", "garden.visitors.hypixelArrivedMessage")
        event.move(3, "garden.visitorHideChat", "garden.visitors.hideChat")
        event.transform(11, "garden.visitors.rewardWarning.drops") { element ->
            ConfigUtils.migrateIntArrayListToEnumArrayList(element, VisitorReward::class.java)
        }
        event.transform(12, "garden.visitors.rewardWarning.drops") { element ->
            val drops = JsonArray()
            for (jsonElement in element.asJsonArray) {
                val old = jsonElement.asString
                val new = VisitorReward.entries.firstOrNull { old.startsWith(it.name) }
                if (new == null) {
                    println("error with migrating old VisitorReward entity: '$old'")
                    continue
                }
                drops.add(JsonPrimitive(new.name))
            }

            drops
        }

        event.transform(15, "garden.visitors.highlightStatus") { element ->
            ConfigUtils.migrateIntToEnum(element, HighlightMode::class.java)
        }

        event.move(18, "garden.visitors.needs", "garden.visitors.shoppingList")
    }
}

